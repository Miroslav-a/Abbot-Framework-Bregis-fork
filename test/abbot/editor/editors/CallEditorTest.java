package abbot.editor.editors;

import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.script.*;
import abbot.tester.JTextComponentTester;
import abbot.finder.matchers.*;

import java.util.concurrent.Callable;

/** Verify CallEditor operation. */

public class CallEditorTest
    extends ResolverFixture implements XMLConstants {

    private Call call;
    private CallEditor editor;
    private JTextComponentTester tester;

    protected void setUp() {
        call = new Call(getResolver(), null,
                        "java.lang.Object", "hashCode", null);
        editor = new CallEditor(call);
        tester = new JTextComponentTester();
    }

    public void testMethodList() throws Throwable {
        showFrame(editor);
        JComboBox cb = getMethodComboBox(editor);
        assertEquals("Wrong method selected",
                     cb.getSelectedItem(), "hashCode");
        String[] expected = {
            "equals",
            "getClass",
            "hashCode",
            "notify",
            "notifyAll",
            "toString",
            "wait",
        };
        assertEquals("Wrong number of methods",
                     expected.length, cb.getItemCount());
        for (int i=0;i < cb.getItemCount();i++) {
            assertEquals("Wrong method in combo list",
                         expected[i], cb.getItemAt(i));
        }
    }

    public void testMethodListUpdateOnClassChange() throws Exception {
        showFrame(editor);
        final JTextField tf = getTargetClassBox(editor);
        final JComboBox cb = getMethodComboBox(editor);
        final int count = cb.getItemCount();

        // Type some text
        //

        String TEXT = "abbot.tester.ComponentTester";
        tester.actionEnterText(tf, TEXT);
        
        // The field are lazily updated so that we need to use these special versions
        // of the assertion
        ComponentTestFixture.assertEqualsEventually("Text entered improperly", TEXT, 
                               new Callable<String>() { public String call() { return tf.getText(); } });
        ComponentTestFixture.assertTrueEventually("Method list was not updated", 
                              new Callable<Boolean>() { public Boolean call() { return count != cb.getItemCount();}});
    }

    protected JTextField getTargetClassBox(CallEditor editor)
        throws Exception
    {
        return (JTextField)getFinder().
            find(editor, new NameMatcher(TAG_CLASS));
    }

    protected JComboBox getMethodComboBox(CallEditor editor) throws Exception {
        return (JComboBox)getFinder().
            find(editor, new NameMatcher(TAG_METHOD));
    }

    /** Construct a test case with the given name. */
    public CallEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        RepeatHelper.runTests(args, CallEditorTest.class);
    }
}
