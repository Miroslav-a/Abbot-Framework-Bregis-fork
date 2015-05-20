package abbot.tester;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.*;

import java.lang.reflect.InvocationTargetException;

import java.util.Vector;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.locks.Lock;

import java.util.concurrent.locks.ReentrantLock;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import junit.extensions.abbot.*;

/** Unit test to verify supported JComboBox operations.<p> */

public class JComboBoxTesterTest extends ComponentTestFixture {

    private JComboBoxTester tester;
    private JComboBox box;
    private Vector list;
    private static final int MAX_ENTRIES = 100;

    /** Create a new test case with the given name. */
    public JComboBoxTesterTest(String name) {
        super(name);
    }

    private volatile String selectedItem;
    private volatile int selectedIndex;
    protected void setUp() {
        tester = (JComboBoxTester)ComponentTester.getTester(JComboBox.class);
        list = new Vector();
        for (int i=0;i < MAX_ENTRIES;i++) {
            list.add("item " + i);
        }
        box = new JComboBox(list);
        box.setSelectedIndex(-1);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                JComboBox cb = (JComboBox)ev.getSource();
                selectedItem = (String)cb.getSelectedItem();
                selectedIndex = cb.getSelectedIndex();
            }
        });
    }

    // FIXME: occasionally see failures on OSX
    // (selected index = -1 @ 50, -1 @ 80)
    public void testSelectIndex() {
        showFrame(box);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedIndex = -1;
            tester.actionSelectIndex(box, i);
            assertEquals("Wrong index selected", i, selectedIndex);
        }
    }

    public void testSelectItem() {
        showFrame(box);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedItem = null;
            String item = "item " + i;
            tester.actionSelectItem(box, item);
            assertEquals("Wrong item selected", item, selectedItem);
        }
    }

    public void testSelectItemLazy() throws InterruptedException {
        final DefaultComboBoxModel boxModel = new DefaultComboBoxModel();
        // Add the first item as it doesn't result in a selection change
        boxModel.addElement("Empty");
        box.setModel(boxModel);
        box.setSelectedItem("Empty");
        
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                
                for (int i = 0; i < list.size(); i++) {
                    
                    final Object next = list.get(i);
                    
                    Robot.delay(50);
                        
            
                    while (Robot.callAndWait(box, new Callable<Boolean>() {

                        public Boolean call()  {
                            if (box==null) {
                                // Test is probably finished
                                return true;
                            }
                            if (box.isPopupVisible()) {
                                return true;
                            }
                            else {
                                boxModel.addElement(next);
                                return false;
                            }
                        }
                    })) {
                        Robot.delay(20);
                    }
                }
                
            }
        });
        
        try
        {
            t.start();
            
            
            showFrame(box);
            
            // I couldn't get this to work reliably with multiple updates,
            // so until then we will just test with one 
            for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
                selectedItem = null;
                String item = "item " + i;
                tester.actionSelectItem(box, item);
                assertEquals("Wrong item selected", item, selectedItem);
            }
            
            
            // Make sure we wait until the thread has finished
        }
        finally {
            t.join();
        }
    }


    /**
     * Check we can still find the items if there is a custom cell renderer
     */
    public void testSelectItemCustomRenderer() {
        
        box.setRenderer(new ListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList list, final Object value, int index, boolean isSelected,
                                                          boolean cellHasFocus) {
                
                JPanel p = new JPanel()
                    {
                        public String getText() {
                            return value!=null ? value.toString() : "[Null]";
                        }
                    };
                p.setBackground(index % 2 == 0 ? Color.RED : Color.BLUE);
                return p;
            }
        });
        
        showFrame(box);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedItem = null;
            String item = "item " + i;
            tester.actionSelectItem(box, item);
            assertEquals("Wrong item selected", item, selectedItem);
        }
    }


    public void testSelectIndexEditable() {
        box.setEditable(true);
        showFrame(box);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedIndex = -1;
            tester.actionSelectIndex(box, i);
            assertEquals("Wrong index selected (editable)", i, selectedIndex);
        }
    }

    public void testSelectItemEditable() {
        box.setEditable(true);
        showFrame(box);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedItem = null;
            String item = "item " + i;
            tester.actionSelectItem(box, item);
            assertEquals("Wrong item selected (editable)", item, selectedItem);
        }
    }

    /**
     * Check we get something sensible out
     */
    public void testDumpList() {
        
        String result = tester.dumpList(box); 
        
        assertTrue("Wrong start", result.startsWith("[item 0, item 1, item 2, item 3, item 4, item 5, "));
        assertTrue("Wrong end", result.endsWith(", item 96, item 97, item 98, item 99]"));
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JComboBoxTesterTest.class);
    }
}
