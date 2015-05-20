package abbot.editor.recorder;

import java.awt.Frame;

import javax.swing.JLabel;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.FrameTester;

/**
 * Unit test to verify proper capture of user semantic events on a Frame.
 */
public class FrameRecorderTest extends AbstractSemanticRecorderFixture {

    private FrameTester tester;

    public FrameRecorderTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tester = new FrameTester();
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new FrameRecorder(r);
    }

    public void testNoCaptureResize() {
        final Frame f = new Frame(getName());
        f.add(new JLabel(getName()));
        f.setResizable(false);
        showWindow(f);
        startRecording();
        tester.resize(f, f.getWidth()*2, f.getHeight());
        assertNoStep();
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, FrameRecorderTest.class);
    }
}
