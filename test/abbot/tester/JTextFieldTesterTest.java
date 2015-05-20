package abbot.tester;

import java.awt.event.*;
import javax.swing.JTextField;

import junit.extensions.abbot.*;

/** Unit test to verify the JTextFieldTester class.<p> */

public class JTextFieldTesterTest extends ComponentTestFixture {

    private JTextFieldTester tester;
    private JTextField tf;
    private Listener listener;

    private class Listener implements ActionListener {
        private String actionCommand;
        private String text;
        private JTextField field;
        public Listener(JTextField f) {
            this.field = f;
            f.addActionListener(this);
        }
        public void actionPerformed(ActionEvent e) {
            actionCommand = e.getActionCommand();
            text = field.getText();
        }
    }


    protected void setUp() {
        tf = new JTextField(getName());
        tf.setColumns(10);
        tester = new JTextFieldTester();
        listener = new Listener(tf);
    }

    public void testActionCommitText() {
        final String EXPECTED = "Some new text";
        showFrame(tf);
        tester.actionCommitText(tf, EXPECTED);
        assertEquals("Text not entered", EXPECTED, tf.getText());
        assertEquals("Notification didn't fire", EXPECTED, listener.text);
    }

    public void testActionCommit() {
        showFrame(tf);
        final String EXPECTED = tf.getText();
        tester.actionCommit(tf);
        assertEquals("Notification didn't fire", EXPECTED, listener.text);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTextFieldTesterTest.class);
    }
}

