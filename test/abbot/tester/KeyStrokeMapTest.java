package abbot.tester;

import java.awt.event.*;

import javax.swing.*;

import junit.extensions.abbot.*;

/** Unit test to verify basic key stroke mappings. <p>
 */

public class KeyStrokeMapTest extends ComponentTestFixture {

    private JTextField tf;
    private Robot robot;

    /** Create a new test case with the given name. */
    public KeyStrokeMapTest(String name) {
        super(name);
    }

    protected void setUp() {
        tf = new JTextField();
        tf.setColumns(50);
        robot = getRobot();
    }

    private static final String charList =
        " !\"#$%&'()*+,-./0123456789:;<=>?@"
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_"
        + "`abcdefghijklmnopqrstuvwxyz{|}~";
    /** Test the basic ASCII set. */
    public void testCharacterGeneration() {
        showFrame(tf);
        robot.focus(tf);
        robot.waitForIdle();
        robot.keyString(charList);
        robot.waitForIdle();
        assertEquals("Not all ascii characters were produced properly",
                     charList, tf.getText());
    }

    public void testBackSpace() {
        showFrame(tf);
        robot.focus(tf);
        robot.waitForIdle();
        robot.keyString("HELO\bLO");
        robot.waitForIdle();
        assertEquals("Backspace not generated", "HELLO", tf.getText());
    }

    private boolean flag = false;
    public void testEnter() {
        tf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                flag = true;
            }
        });
        showFrame(tf);
        robot.focus(tf);
        robot.waitForIdle();
        robot.keyString("HELLO\n");
        robot.waitForIdle();
        assertTrue("'Enter' not generated", flag);
    }

    /** Check proper generation of keystrokes given key codes. */
    public void testKeyStrokeParsing() {
        assertEquals("Normal 'a'", 
                     KeyStroke.getKeyStroke(KeyEvent.VK_A, 0),
                     KeyStrokeMap.getKeyStroke('a'));
        assertEquals("Shifted 'a'", 
                     KeyStroke.getKeyStroke(KeyEvent.VK_A, 
                                            InputEvent.SHIFT_MASK),
                     KeyStrokeMap.getKeyStroke('A'));
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, KeyStrokeMapTest.class);
    }
}
