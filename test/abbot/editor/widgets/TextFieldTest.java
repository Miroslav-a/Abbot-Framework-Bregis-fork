package abbot.editor.widgets;

import java.awt.event.*;
import junit.extensions.abbot.*;
import abbot.tester.*;

import java.util.concurrent.Callable;

public class TextFieldTest extends ComponentTestFixture {

    private TextField textField;
    private JTextComponentTester tester;

    protected void setUp() {
        tester = new JTextComponentTester();
        textField = new TextField("Basic Field");
        showFrame(textField);
    }

    public void testReplaceText() {
        tester.actionActionMap(textField, "select-all");
        tester.actionKeyString("replaced");
        String text = textField.getText();
        assertEquals("Text not replaced", "replaced", text);
    }

    public void testRevert() {
        class Flag { volatile boolean flag; String command; }
        final Flag fired = new Flag();
        String orig = textField.getText();
        tester.actionEnterText(textField, "new text");
        assertTrue("Text not entered", !textField.getText().equals(orig));
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent     e) {
                    if (TextField.ACTION_TEXT_REVERTED.equals(e.getActionCommand())) {
                        fired.flag = true;
                        fired.command = e.getActionCommand();
                    }
                }
        });
        tester.actionKeyStroke(textField, KeyEvent.VK_ESCAPE);
        assertEquals("Text not reverted", orig, textField.getText());
        assertTrue("No action fired on revert", fired.flag);
        assertEquals("Wrong action command",
                     TextField.ACTION_TEXT_REVERTED, fired.command);
    }

    public void testSelectAllOnEnter() {
        String text = "new text";
        tester.actionEnterText(textField, text);
        tester.actionKeyStroke(textField, KeyEvent.VK_ENTER);
        assertEquals("Text should be selected",
                     text, textField.getSelectedText());
    }


    //
    // Versions with the message delay built in
    //

    public void testDelayedReplaceText() {
        textField.putClientProperty(TextField.DELAYED_EVENTS_CLIENT_PROPERTY, true);
        tester.actionActionMap(textField, "select-all");
        tester.actionKeyString("replaced");
        
        assertEqualsEventually("Text not replaced", "replaced", 
                               new Callable<String>() { public String call() { return textField.getText(); } });
    }

    public void testdDelayedRevert() {
        textField.putClientProperty(TextField.DELAYED_EVENTS_CLIENT_PROPERTY, true);
        class Flag { volatile boolean flag; String command; }
        final Flag fired = new Flag();
        String orig = textField.getText();
        tester.actionEnterText(textField, "new text");
        assertTrue("Text not entered", !textField.getText().equals(orig));
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent     e) {
                if (TextField.ACTION_TEXT_REVERTED.equals(e.getActionCommand())) {
                    fired.flag = true;
                    fired.command = e.getActionCommand();
                }
            }
        });

        tester.actionKeyStroke(textField, KeyEvent.VK_ESCAPE);


        assertEqualsEventually("Text not reverted", orig, 
                               new Callable<String>() { public String call() { return textField.getText(); } });
        // Event can be fired after a slight delay
        assertTrueEventually("No action fired on revert", 
                              new Callable<Boolean>() { public Boolean call() { return fired.flag; } });
        assertEquals("Wrong action command",
                     TextField.ACTION_TEXT_REVERTED, fired.command);
    }

    public void testDelayedSelectAllOnEnter() {
        textField.putClientProperty(TextField.DELAYED_EVENTS_CLIENT_PROPERTY, true);
        String text = "new text";
        tester.actionEnterText(textField, text);
        tester.actionKeyStroke(textField, KeyEvent.VK_ENTER);
        assertEqualsEventually("Text should be selected",
                     text, 
                     new Callable<String>() { public String call() { return textField.getSelectedText(); } });
    }


    /** Construct a test case with the given name. */
    public TextFieldTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, TextFieldTest.class);
    }
}
