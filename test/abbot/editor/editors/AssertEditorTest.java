package abbot.editor.editors;

import java.util.ArrayList;

import java.awt.Frame;
import java.awt.event.KeyEvent;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.finder.matchers.*;
import abbot.script.*;
import abbot.tester.*;

/** Verify AssertEditor operation. */

public class AssertEditorTest
    extends ResolverFixture implements XMLConstants {

    private Assert step;
    private AssertEditor editor;
    private JComponentTester tester;

    public void setUp() { 
        step = new Assert(getResolver(), getName(),
                          ComponentTester.class.getName(),
                          "assertFrameShowing", new String[0],
                          "true", false);
        editor = new AssertEditor(step);
        tester = new JComponentTester();
    }

    public void testWaitOptions() throws Exception {
        showFrame(editor);
        JCheckBox waitBox = (JCheckBox)getFinder().
            find(editor, new NameMatcher(TAG_WAIT));

        int count = editor.getComponentCount();
        tester.actionClick(waitBox);
        assertTrue("No components were added",
                   count < editor.getComponentCount());

        tester.actionClick(waitBox);
        assertEquals("Wait option components not properly removed",
                     count, editor.getComponentCount());
    }

    public void testClearTimeout() throws Exception {
        // When a required field is cleared, its contents should be restored
        // to the default value on ENTER
        final Frame frame = showFrame(editor);
        JCheckBox wait = (JCheckBox)getFinder().
            find(editor, new NameMatcher(TAG_WAIT));
        tester.actionClick(wait);
        invokeAndWait(new Runnable() { public void run() {
            frame.setSize(frame.getPreferredSize());
        }});
        editor.revalidate();
        editor.repaint();

        JTextField timeout = (JTextField)getFinder().
            find(editor, new NameMatcher(TAG_TIMEOUT));
        tester.actionActionMap(timeout, "select-all");
        tester.actionKeyStroke(timeout, KeyEvent.VK_DELETE);
        tester.waitForIdle();
        assertEquals("Text field should be empty on delete contents",
                     "", timeout.getText());
        tester.actionKeyStroke(timeout, KeyEvent.VK_ENTER);
        assertEquals("Text field should hold default value after Enter",
                     String.valueOf(step.getTimeout()), timeout.getText());
    }


    public void testTesterMethodList() throws Throwable {
        showFrame(editor);

        JComboBox cb = editor.method;
        String[] expected = {
            "assertComponentShowing",
            "assertFrameShowing",
            "assertImage",
            "getBorderTitle",
            "getDescriptiveName",
            "getIconName",
            "getIndex",
            "getLabel",
            "getName",
            "getText",
            "getTitle"
        };
        
        assertTrue("Too few methods in combo box: " + cb.getItemCount(),
                   cb.getItemCount() >= expected.length);
        for (int i=0;i < cb.getItemCount();i++) {
            assertTrue("Too many combo box items (next is "
                       + cb.getItemAt(i) + ")", i < expected.length);
            assertEquals("Wrong method in combo list",
                         expected[i], cb.getItemAt(i));
        }
        assertEquals("Wrong number of combo box items",
                     expected.length, cb.getItemCount());
    }
    
    public void testComponentMethodList() throws Throwable {
        JComboBox methodChooser = (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_METHOD));

        showFrame(editor);
        // Make sure we have at least one reference in the list
        ComponentReference cr = step.getResolver().addComponent(methodChooser);

        JComboBoxTester tester = new JComboBoxTester();
        JComboBox refChooser = (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_COMPONENT));

        // Alter the step to refer to a property on a component
        tester.actionSelectItem(refChooser, cr.getID());
        tester.actionSelectItem(methodChooser, "getItemCount");
        assertEquals("Wrong method selected",
                     "getItemCount", editor.method.getSelectedItem());

        ArrayList list = new ArrayList();
        for (int i=0;i < methodChooser.getItemCount();i++) {
            list.add(methodChooser.getItemAt(i));
        }
        assertEquals("Wrong target class",
                     JComboBox.class, step.getTargetClass());
        assertTrue("No items in the list", methodChooser.getItemCount() > 0);
        String[] expected = {
            "getParent",
            "isOpaque",
            "hasFocus",
            // From the JComboBoxTester
            "getContents",
        };
        for (int i=0;i < expected.length;i++) {
            assertTrue(expected[i] + " method missing",
                       list.contains(expected[i]));
        }
    }

    public void testSwitchFromTesterToComponentMethod() throws Exception {
        Frame f = showFrame(editor);
        JComboBox methodChooser = (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_METHOD));

        ComponentReference cr = step.getResolver().addComponent(methodChooser);
        hideWindow(f);

        step = new Assert(getResolver(), getName(),
                          JComboBoxTester.class.getName(),
                          "getContents", new String[] { cr.getID() },
                          "[one,two,three]", false);

        editor = new AssertEditor(step);
        showFrame(editor);
        methodChooser = (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_METHOD));

        JComboBoxTester tester = new JComboBoxTester();
        // FIXME why do we need to do this twice?
        tester.actionSelectItem(methodChooser, "getToolkit");
        tester.actionSelectItem(methodChooser, "getToolkit");
        assertEquals("Component method not selected",
                     "getToolkit", methodChooser.getSelectedItem());

        // FIXME check other parts of editor
    }

    /** Construct a test case with the given name. */
    public AssertEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, AssertEditorTest.class);
    }
}
