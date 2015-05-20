package abbot.tester;

import java.awt.*;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.Log;

/** Unit test to verify the JComponentTester class.<p> */

public class JComponentTesterTest extends ComponentTestFixture {

    private JComponentTester tester;

    /** Create a new test case with the given name. */
    public JComponentTesterTest(String name) {
        super(name);
    }

    protected void setUp() {
        tester = (JComponentTester)ComponentTester.getTester(JComponent.class);
    }

    private void scrollAndAssertVisible(JList list, int row) {
        Rectangle rect = list.getCellBounds(row, row);
        tester.actionScrollToVisible(list, rect.x, rect.y,
                                     rect.width, rect.height);
        tester.actionWaitForIdle();
        Rectangle visible = list.getVisibleRect();
        Log.debug("Visible rect: " + visible);
        assertTrue("Horizontal scroll to element " + row + " failed ("
                   + visible.x + " <= " + rect.x + " < " + (visible.x +
                                                            visible.width)
                   + ")",
                   visible.x <= rect.x
                   && rect.x < visible.x + visible.width);
        assertTrue("Vertical scroll to element " + row + " failed ("
                   + visible.y + " <= " + rect.y + " < " + (visible.y +
                                                            visible.height)
                   + ")",
                   visible.y <= rect.y
                   && rect.y < visible.y + visible.height);
    }

    /** Test scrolling for JComponents within a scroll pane. */
    // FIXME really needs to scroll horizontally as well
    public void testScroll() {
        String[] data = {
            "zero", "one", "two", "three", "four", 
            "five", "six", "seven", "eight", "nine",
            "zero", "one", "two", "three", "four", 
            "five", "six", "seven", "eight", "nine",
            "zero", "one", "two", "three", "four", 
            "five", "six", "seven", "eight", "nine",
            "zero", "one", "two", "three", "four", 
            "five", "six", "seven", "eight", "nine",
            "zero", "one", "two", "three", "four", 
            "five", "six", "seven", "eight", "nine",
        };
        JList list = new JList(data);
        JScrollPane scroll = new JScrollPane(list);
        showFrame(scroll);

        scrollAndAssertVisible(list, 0);
        scrollAndAssertVisible(list, data.length / 4);
        scrollAndAssertVisible(list, data.length / 2);
        scrollAndAssertVisible(list, data.length * 3 / 4);
        scrollAndAssertVisible(list, data.length / 2);
        scrollAndAssertVisible(list, data.length / 4);
        scrollAndAssertVisible(list, 0);
    }

    public void testActionMap() {
        // Use something for which we know the action/mappings
        JTextField tf = new JTextField("Some text");
        showFrame(tf);
        tester.actionFocus(tf);
        tester.actionActionMap(tf, "select-all");
        assertEquals("Selection should start at beginning of text",
                     0, tf.getSelectionStart());
        assertEquals("Selection should end at end of text",
                     tf.getText().length(), tf.getSelectionEnd());
    }

    public void testActionOnObscured() {
        JPanel panel = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(400, 400);
            }
        };
        showFrame(panel, new Dimension(100, 100));
        try {
            tester.actionClick(panel, 300, 300);
            fail("Action should fail if outside of JComponent visible rect");
        }
        catch(ActionFailedException e) {
        }
    }

    public void testScrollToVisible() {
        JLabel label = new JLabel(getName());
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(200, 400));
        JPanel scrolled = new JPanel(new BorderLayout());
        scrolled.add(p);
        scrolled.add(label, BorderLayout.SOUTH);
        showFrame(new JScrollPane(scrolled), new Dimension(200, 200));

        Rectangle visible = label.getVisibleRect();
        Rectangle empty = new Rectangle(0,0,0,0);
        assertTrue("Component should not be visible",
                   visible == null || visible.equals(empty));
        
        tester.actionScrollToVisible(label, new ComponentLocation());
        visible = label.getVisibleRect();
        assertFalse("Component should be visible",
                    visible == null || visible.equals(empty));
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JComponentTesterTest.class);
    }
}
