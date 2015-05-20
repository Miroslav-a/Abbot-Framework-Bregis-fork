package abbot.editor.recorder;

import java.util.Vector;

import javax.swing.JList;

import junit.extensions.abbot.RepeatHelper;
import junit.framework.*;
import abbot.script.Resolver;
import abbot.tester.*;

/**
 * Unit test to verify proper capture of user semantic events on a JList.
 */
public class JListRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JListTester tester;
    private JList list;
    private static final int MAX_ENTRIES = 5;

    public JListRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JListRecorder(r);
    }

    private void showList() {
        Vector els = new Vector();
        for (int i=0;i < MAX_ENTRIES;i++) {
            els.add("list item " + i);
        }
        list = new JList(els);
        tester = (JListTester)ComponentTester.getTester(list);
        showFrame(list);
    }

    public void testCaptureSelection() {
        showList();
        startRecording();
        for (int i=MAX_ENTRIES-1;i >= 0; i--) {
            tester.actionSelectIndex(list, i);
            assertStep("SelectRow\\(.*,\"list item " + i + "\"\\)");
        }
    }

    /** Return the default test suite. */
    public static Test suite() {
        return new TestSuite(JListRecorderTest.class);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JListRecorderTest.class);
    }
}
