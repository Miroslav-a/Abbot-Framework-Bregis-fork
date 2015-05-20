package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import junit.extensions.abbot.*;

public class CheckboxTesterTest extends ComponentTestFixture {
    public void testClickCheckbox() {
        final Checkbox b = new Checkbox(getName());
        showFrame(b);
        final String expected = "button clicked";
        b.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                b.setLabel(expected);
            }
        });
        CheckboxTester tester = new CheckboxTester();
        tester.actionClick(b);
        assertEquals("Checkbox not clicked", expected, b.getLabel());
        assertEquals("Wrong state", true, b.getState());
        tester.actionClick(b);
        assertEquals("Wrong state", false, b.getState());
    }

    public void testAWTModeCheckboxClick() {
        int lastMode = Robot.getEventMode();
        Robot.setEventMode(Robot.EM_AWT);
        try {
            final Checkbox b = new Checkbox(getName());
            showFrame(b);
            final String expected = "button clicked";
            b.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    b.setLabel(expected);
                }
            });
            getRobot().click(b);
            getRobot().waitForIdle();
            assertTrue("Expect Checkbox to not be clickable in AWT mode",
                       !expected.equals(b.getLabel()));
            assertEquals("Expect no state change", false, b.getState());
        }
        finally {
            Robot.setEventMode(lastMode);
        }
    }

    public CheckboxTesterTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, CheckboxTesterTest.class);
    }

}
