package abbot.tester;

import abbot.WaitTimedOutException;

import java.awt.event.*;

import javax.swing.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.util.ExtendedComparator;

import java.awt.Component;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/** Unit test to verify the JListTester class.<p> */

public class JListTesterTest extends ComponentTestFixture {

    private JListTester tester;
    private JList list;
    private JScrollPane scrollPane;

    private String[] data = { "zero", "one", "two", "three", "four", 
                              "five", "six", "seven", "eight"};

    protected void setUp() {
        tester = new JListTester();
        list = new JList(data);
    }

    public void testContents() {
        showFrame(list);
        assertTrue("Wrong contents returned", 
                   ExtendedComparator.equals(data, tester.getContents(list)));
    }

    private class MouseWatcher extends MouseAdapter {
        public volatile int clickCount = 0;
        public void mouseClicked(MouseEvent ev) {
            clickCount = ev.getClickCount();
        }
    }

    public void testSelectFirstValue() {
        list.setSelectedIndex(1);
        MouseWatcher mw = new MouseWatcher();
        list.addMouseListener(mw);
        showFrame(list);
        tester.actionSelectRow(list, new JListLocation(data[0]));
        assertEquals("Wrong item selected", 0, list.getSelectedIndex());
        Timer timer = new Timer();
        while (mw.clickCount == 0) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                throw new RuntimeException("Timed out waiting for clicks");
            tester.sleep();
        }
        assertEquals("Too many clicks", 1, mw.clickCount);
    }

    public void testSelectFirstIndex() {
        list.setSelectedIndex(1);
        MouseWatcher mw = new MouseWatcher();
        list.addMouseListener(mw);
        showFrame(list);
        tester.actionSelectIndex(list, 0);
        assertEquals("Wrong item selected", 0, list.getSelectedIndex());
        Timer timer = new Timer();
        while (mw.clickCount == 0) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                throw new RuntimeException("Timed out waiting for clicks");
            tester.sleep();
        }
        assertEquals("Too many clicks", 1, mw.clickCount);
    }

    /**
     * Uses a JList in a JScrollPane to test the scrollToVisible feature of
     * JComponentTester.
     */
    public void testScrollToVisibleIndex()
    {
        list.setVisibleRowCount(4);
        scrollPane = new JScrollPane(list);
        showFrame(scrollPane);
        tester.actionSelectIndex(list, 0);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 0);
        tester.actionSelectIndex(list, 7);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 7);
        tester.actionSelectIndex(list, 6);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 6);
        tester.actionSelectIndex(list, 2);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 2);
        tester.actionSelectIndex(list, 8);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 8);
        tester.actionSelectIndex(list, 5);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 5);
        tester.actionSelectIndex(list, 1);
        assertEquals("Wrong Cell Selected", list.getSelectedIndex(), 1);
        try {
            tester.actionSelectIndex(list, -1);
            fail("Action with out-of-bounds index should fail");
        }
        catch(ActionFailedException a) { }
        try {
            tester.actionSelectIndex(list, data.length, 1000); // Use shorted delay to speed test run
            fail("Action with out-of-bounds index should fail");
        }
        catch(ActionFailedException a) { }
        catch(WaitTimedOutException w) {}
    }

    /**
     * Uses a JList in a JScrollPane to test the scrollToVisible feature of
     * JComponentTester.
     */
    public void testLazyLoad()
    {
        list.setVisibleRowCount(4);
        scrollPane = new JScrollPane(list);
        showFrame(scrollPane);
        final DefaultListModel dlm = new DefaultListModel();
        list.setModel(dlm);
        
        // Slight later on add some entries
        //
        
        new Thread(new Runnable() {                                                 
            public void run() {
                try {
                    Thread.sleep(2000);
                    dlm.addElement("Zero");
                    Thread.sleep(1000);
                    dlm.addElement("One");
                } catch (Exception e) {
                    
                }
                
            }
        }).start();
        
        
        
        tester.actionSelectIndex(list, 0);
        assertEquals("Cell Not Selected", list.getSelectedIndex(), 0);
        tester.actionSelectIndex(list, 1);
        assertEquals("Cell Not Selected", list.getSelectedIndex(), 1);
    }
    
    
    public void testGetTextRenderer() {
        
        class OtherRenderer extends JTextField
          implements ListCellRenderer {

            public Component getListCellRendererComponent(JList list,
                                                          Object value,
                                                          int index,
                                                          boolean isSelected,
                                                          boolean cellHasFocus) {

                setText(value.toString());
                return this;
            }
        }
        
        //
        
        
        list.setCellRenderer(new OtherRenderer());
        
        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JListTester.valueToString(
               list , 0),
            "zero");
        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JListTester.valueToString(
               list , 1),
            "one");
        
        
    }

        
    public static void main(String[] args) {
        RepeatHelper.runTests(args, JListTesterTest.class);
    }
}

