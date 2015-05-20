package abbot.editor.editors;

import java.awt.event.*;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.finder.matchers.*;
import abbot.script.*;
import abbot.tester.*;

/** Verify StepEditor operation. */

public class StepEditorTest extends ResolverFixture {

    private Step step;
    private DefaultStepEditor editor;
    private String description = "default value";
    private JTextComponentTester tester = new JTextComponentTester();
    private JTextField textField;

    private class DefaultStepEditor extends StepEditor {
        JTextField text;
        public DefaultStepEditor(Step s) {
            super(s);
            text = addTextField("dummy field", "empty");
        }
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == text) {
                StepEditorTest.this.description = text.getText();
                // tell the editor the underlying step data has changed
                fireStepChanged();
            }
            else {
                super.actionPerformed(e);
            }
        }
        public void descriptionChanged() {
            fireStepChanged();
        }
    }

    protected void setUp() throws Exception {
        step = new Step(getResolver(), (String)null) {
            public String getDefaultDescription() {
                return description;
            }
            public String getUsage() { return null; }
            public String getXMLTag() { return "step"; }
            public void runStep() { }
        };
        editor = new DefaultStepEditor(step);
        textField = (JTextField)getFinder().
            find(editor, new ClassMatcher(JTextField.class));
    }

    public void testInit() {
        showFrame(editor);
        assertEquals("Step should match text field",
                     step.getDescription(), textField.getText());
    }

    public void testRevertDescription() {
        showFrame(editor);
        textField.setText("");
        String text = step.getDescription();
        tester.actionEnterText(textField, "dummy");
        tester.actionKeyStroke(textField, KeyEvent.VK_ESCAPE);
        assertEquals("Text not reverted", text, textField.getText());
        assertEquals("Description not reverted", text, step.getDescription());
    }

    public void testSetDescription() {
        showFrame(editor);
        assertEquals("Wrong initial description",
                     description, textField.getText());
        textField.setText("");
        tester.actionEnterText(textField, "hello");
        assertEquals("Text entry did not change step data",
                     "hello", step.getDescription());

        // Clearing it should result in the default description
        description = "default";
        textField.setText("");
        tester.actionKeyStroke(textField, KeyEvent.VK_ENTER);
        assertEquals("Step should revert to default when cleared",
                     description, step.getDescription());
        assertEquals("Text field should revert to default when cleared",
                     description, textField.getText());

        // Now edit with key input
        tester.actionSetCaretPosition(textField, textField.getText().length());
        tester.actionKeyStroke(textField, KeyEvent.VK_BACK_SPACE);
        assertEquals("Step should be properly edited",
                     description.substring(0, description.length()-1),
                     textField.getText());
    }
    
    public void testClearDescription() {
        // clearing followed by enter or focus change should revert to default
        showFrame(editor);
        tester.actionSelectText(textField,
                                0, step.getDescription().length());
        tester.actionKeyStroke(textField, KeyEvent.VK_BACK_SPACE);
        tester.actionKeyStroke(textField, KeyEvent.VK_ENTER);
        assertEquals("Step should revert to default description",
                     step.getDefaultDescription(), step.getDescription());
        assertEquals("Text field should revert to default description",
                     step.getDefaultDescription(),
                     textField.getText());
    }

    public void testUpdateDefaultDescription() throws Throwable {
        showFrame(editor);
        // Change from underlying step data
        description = "something else";
        editor.descriptionChanged();
        assertEquals("Editor didn't pick up underlying data change",
                     description, textField.getText());

        // Change from other field which affects the description should cause
        // the description field to update if the description is the default
        tester.actionKeyString(editor.text, "hello");
        assertEquals("Description  didn't change in response to step data",
                     description, textField.getText());
        // Remove the description, it should be restored
        textField.setText("");
        tester.actionKeyStroke(editor.text, KeyEvent.VK_ENTER);
        assertEquals("Description should revert to default when removed",
                     description, textField.getText());
    }

    /** Construct a test case with the given name. */
    public StepEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, StepEditorTest.class);
    }
}
