package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import junit.extensions.abbot.*;

/** Unit test to verify the ListTester class.<p> */
public class ListTesterTest extends ComponentTestFixture {

    private ListTester tester;
    private List list;

    private String[] data = { "zero", "one", "two", "three", "four", 
                              "five", "six", "seven", "eight"};

    protected void setUp() {
        tester = new ListTester();
        list = new List(data.length);
        for (int i=0;i < data.length;i++) {
            list.add(data[i]);
        }
        showFrame(list);
    }

    public void testSelectRow() {
        class Listener implements ItemListener {
            int index = -1;
            boolean selected;
            public void itemStateChanged(ItemEvent e) {
                index = ((List)e.getSource()).getSelectedIndex();
                selected = e.getStateChange() == ItemEvent.SELECTED;
            }
        }
        Listener listener = new Listener();
        list.addItemListener(listener);
        for (int i=0;i < data.length;i++) {
            listener.selected = false;
            tester.actionSelectRow(list, new ListLocation(i));
            assertTrue("No select fired", listener.selected);
            assertEquals("Incorrect selection", i, listener.index);
        }
    }

    /** Create a new test case with the given name. */
    public ListTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ListTesterTest.class);
    }
}

