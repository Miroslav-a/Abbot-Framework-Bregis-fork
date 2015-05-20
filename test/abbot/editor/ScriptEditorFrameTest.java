package abbot.editor;

import java.awt.*;
import javax.swing.ActionMap;
import java.io.File;

import junit.extensions.abbot.*;
import abbot.tester.*;

public class ScriptEditorFrameTest extends ComponentTestFixture {

    private ScriptEditorFrame editor;
    private String prefsFile = ".test-prefs";

    public void testTrackMoveAndResize() {
        showWindow(editor);
        WindowTester tester = new WindowTester();
        tester.actionMove(editor, 111, 111);
        tester.actionResize(editor, 444, 444);
        Dimension size = editor.getSize();
        Point where = editor.getLocation();
        getHierarchy().dispose(editor);

        ScriptEditorFrame editor2 = createEditor(new Preferences(prefsFile));
        showWindow(editor2);
        assertEquals("Location not preserved", where, editor2.getLocation());
        assertEquals("Size not preserved", size, editor2.getSize());
    }

    private ScriptEditorFrame createEditor(Preferences prefs) {
        return new ScriptEditorFrame(new String[][] { },
                                     new ActionMap(), null,
                                     getName(),
                                     new ScriptTable(), prefs);
    }

    protected void setUp() {
        File file = new File(new File(System.getProperty("user.home")),
                             prefsFile);
        file.deleteOnExit();
        editor = createEditor(new Preferences(prefsFile));
    }

    /** Construct a test case with the given name. */
    public ScriptEditorFrameTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptEditorFrameTest.class);
    }
}
