package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import java.util.concurrent.Callable;

import junit.extensions.abbot.*;

public class ButtonTesterTest extends ComponentTestFixture {
    public void testClickButton() {
        final Button b = new Button(getName());
        showFrame(b);
        final String expected = "button clicked";
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b.setLabel(expected);
            }
        });
        ButtonTester tester = new ButtonTester();
        tester.actionClick(b);
        assertEqualsEventually("Button not clicked", expected, new Callable<String>() {
            public String call() {
                return b.getLabel();
            }
        });
    }

    public void testAWTModeButtonClick() {
        int lastMode = Robot.getEventMode();
        Robot.setEventMode(Robot.EM_AWT);
        try {
            final Button b = new Button(getName());
            showFrame(b);
            final String expected = "button clicked";
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    b.setLabel(expected);
                }
            });
            getRobot().click(b);
            getRobot().waitForIdle();
            assertTrue("Expect Button to not be clickable in AWT mode",
                       !expected.equals(b.getLabel()));

            assertTrueEventually("Expect Button to not be clickable in AWT mode", new Callable<Boolean>() {
                public Boolean call() {
                    return !expected.equals(b.getLabel());
                }
            });

        }
        finally {
            Robot.setEventMode(lastMode);
        }
    }

    public ButtonTesterTest(String name) { super(name); }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ButtonTesterTest.class);
    }

}
