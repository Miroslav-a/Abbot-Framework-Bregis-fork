package abbot.editor.recorder;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;

/** Test recording of JComponent-specific events. */
public class JComponentRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JComponentTester tester;

    protected void setUp() throws Exception {
        super.setUp();
        tester = new JComponentTester();
    }

    public void testCaptureActionMap() {
        JTextField tf = new JTextField("Operate here");
        showFrame(tf);
        tester.actionFocus(tf);
        startRecording();

        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        tester.key(KeyEvent.VK_A, mask);
        assertStep("ActionMap\\(.*,select-all\\)");
        assertNoStep();
        assertNoTrailingEvents();
    }

    public void testSwallowKeyTyped() {
        JTextField tf = new JTextField("Press Enter");
        showFrame(tf);
        tester.actionFocus(tf);
        startRecording();

        tester.key(KeyEvent.VK_ENTER);
        assertStep("ActionMap\\(.*,notify-field-accept\\)");
        assertNoStep();
        assertNoTrailingEvents();
    }

    public void testCaptureUnnamedActions() {
        JTree tree = new JTree();
        showFrame(tree);
        tester.actionFocus(tree);
        startRecording();
        // FIXME ensure the action is unnamed; LAFs may differ
        tester.key(KeyEvent.VK_LEFT);
        assertStep("ActionMap\\(.*,(selectParent|aquaCollapseNode)\\)");
    }

    /** Create a new test case with the given name. */
    public JComponentRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JComponentRecorder(r);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JComponentRecorderTest.class);
    }
}
