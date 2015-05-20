package abbot.editor.editors;

import java.util.ArrayList;

import javax.swing.JComboBox;

import junit.extensions.abbot.*;
import abbot.Log;
import abbot.script.*;
import abbot.finder.matchers.ClassMatcher;

/** Verify ActionEditor operation. */

public class ActionEditorTest extends ResolverFixture {

    private Action action;
    private ActionEditor editor;

    protected void setUp() {
        action = new Action(getResolver(), null,
                            "actionClick", new String[] {
                            "MethodSelector"
                            }, JComboBox.class);
        editor = new ActionEditor(action);
    }

    public void testComponentTesterMethodList() throws Throwable {
        showFrame(editor);

        JComboBox cb = (JComboBox)getFinder().
            find(new ClassMatcher(JComboBox.class));
        action.getResolver().addComponent(cb);
        
        assertEquals("Wrong method selected",
                     "actionClick", cb.getSelectedItem());
        ArrayList list = new ArrayList();
        for (int i=0;i < cb.getItemCount();i++) {
            Log.debug("Got " + cb.getItemAt(i));
            list.add(cb.getItemAt(i));
        }
        assertTrue("No items in the list", cb.getItemCount() > 0);
        String[] expected = {
            "actionClick",
            "actionFocus",
            "actionKeyStroke",
            // Combo box methods
            "actionSelectIndex",
            "actionSelectItem",
        };
        for (int i=0;i < expected.length;i++) {
            assertTrue(expected[i] + " missing", list.contains(expected[i]));
        }
    }

    /** Construct a test case with the given name. */
    public ActionEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ActionEditorTest.class);
    }
}
