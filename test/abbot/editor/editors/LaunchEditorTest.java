package abbot.editor.editors;

import java.awt.Color;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.DynamicLoadingConstants;
import abbot.script.*;
import abbot.tester.*;
import abbot.finder.matchers.*;
import abbot.editor.widgets.*;

/** Verify LaunchEditor operation. */

public class LaunchEditorTest
    extends ResolverFixture implements XMLConstants, DynamicLoadingConstants {

    private Launch launch;
    private LaunchEditor editor;
    private ComponentTester tester;

    protected void setUp() {
        launch = new Launch(getResolver(), LaunchEditor.HELP_DESC,
                            "java.lang.Object", "hashCode", null);
        editor = new LaunchEditor(launch);
        tester = new ComponentTester();
    }

    public void testRemoveHelpDescription() throws Exception {
        showFrame(editor);
        JCheckBox cb = (JCheckBox)getFinder().
            find(new ClassMatcher(JCheckBox.class));
        assertEquals("Should start with help description",
                     LaunchEditor.HELP_DESC, launch.getDescription());
        tester.actionClick(cb);
        assertFalse("Help description should be changed on edit of other prop",
                    LaunchEditor.HELP_DESC.equals(launch.getDescription()));
    }

    public void testClasspathAffectsClassAndMethod() throws Exception {
        String PATH = DYNAMIC_CLASSPATH;
        String CLASSNAME = DYNAMIC_CLASSNAME;
        String METHOD = "main";
        launch.setTargetClassName(CLASSNAME);
        launch.setMethodName(METHOD);
        launch.setArguments("[]");
        launch.setClasspath(PATH);
        editor = new LaunchEditor(launch);
        showFrame(editor);

        ArrayEditor array = (ArrayEditor)getFinder().
            find(editor, new NameMatcher(TAG_CLASSPATH));
        JTextField tf = (JTextField)getFinder().
            find(array, new NameMatcher("editor"));
        JTextField className = (JTextField)getFinder().
            find(editor, new NameMatcher(TAG_CLASS));
        JComboBox methodCombo = (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_METHOD));
        JTextField method = (JTextField)getFinder().
            find(methodCombo, new ClassMatcher(JTextField.class));
        Color normal = className.getForeground();

        assertEquals("Wrong class name", CLASSNAME, className.getText());
        assertEquals("Wrong method name", METHOD, method.getText());
        assertEquals("Wrong number of path elements",
                     1, array.getValues().length);
        assertEquals("Wrong path", PATH, array.getValues()[0]);

        JTextComponentTester textTester = new JTextComponentTester();
        textTester.actionEnterText(tf, "no.such.path");

        assertTrue("Class name field should show invalid: "
                   + className.getForeground(),
                   !normal.equals(className.getForeground()));
        assertTrue("Method name field should show invalid"
                   + method.getForeground(),
                   !normal.equals(method.getForeground()));

        textTester.actionEnterText(tf, PATH);
        assertEquals("Class name should have reverted to normal foreground",
                     normal, className.getForeground());
        assertEquals("Method name should have reverted to normal foreground",
                     normal, method.getForeground());
    }

    public void testChangeClasspath() throws Exception {
        String PATH = "/some/path";
        launch.setClasspath(PATH);
        editor = new LaunchEditor(launch);
        showFrame(editor);
        ArrayEditor array = (ArrayEditor)getFinder().
            find(editor, new NameMatcher(TAG_CLASSPATH));

        Object[] path = array.getValues();
        assertEquals("Path element not added", 1, path.length);

        JTextField tf = (JTextField)getFinder().
            find(array, new NameMatcher("editor"));
        assertEquals("Incorrect editor text", PATH, tf.getText());

        path = array.getValues();
        assertEquals("Path not changed", PATH, path[0]);
    }

    public void testAddClasspath() throws Exception {
        showFrame(editor);
        ArrayEditor array = (ArrayEditor)getFinder().
            find(editor, new NameMatcher(TAG_CLASSPATH));
        JButton button = (JButton)getFinder().
            find(array, new NameMatcher("add"));
        tester.actionClick(button);
        Object[] path = array.getValues();
        assertEquals("Should have added a path", 1, path.length);
        tester.actionClick(button);
        path = array.getValues();
        assertEquals("Should have added a path", 2, path.length);
    }

    /** Construct a test case with the given name. */
    public LaunchEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, LaunchEditorTest.class);
    }
}
