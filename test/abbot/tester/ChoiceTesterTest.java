package abbot.tester;

import java.awt.Choice;
import java.awt.event.*;

import junit.extensions.abbot.*;

/** Unit test to verify supported operations on the Choice class.<p> */

public class ChoiceTesterTest extends ComponentTestFixture {

    private ChoiceTester tester;
    private Choice choice;
    private static final int MAX_ENTRIES = 100;

    private String selectedItem = null;
    private int selectedIndex = -1;
    protected void setUp() {
        tester = (ChoiceTester)ComponentTester.getTester(Choice.class);
        choice = new Choice();
        for (int i=0;i < MAX_ENTRIES;i++) {
            choice.add("item " + i);
        }
        choice.select(MAX_ENTRIES-1);
        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent ev) {
                abbot.Log.debug("Got " + ev);
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    selectedIndex = choice.getSelectedIndex();
                    selectedItem = choice.getItem(selectedIndex);
                }
            }
        });
    }

    public void testSelectIndex() {
        showFrame(choice);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedIndex = -1;
            tester.actionSelectIndex(choice, i);
            assertEquals("Listener not notified", i, selectedIndex);
            assertEquals("Component state not properly set",
                         i, choice.getSelectedIndex());
        }
    }

    public void testSelectItem() {
        showFrame(choice);
        for (int i=0;i < MAX_ENTRIES;i += MAX_ENTRIES / 10) {
            selectedItem = null;
            String item = "item " + i;
            tester.actionSelectItem(choice, item);
            assertEquals("Listener not notified", item, selectedItem);
            assertEquals("Component state not properly set",
                         item, choice.getSelectedObjects()[0]);
        }
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ChoiceTesterTest.class);
    }
}
