package abbot.editor.recorder;

import java.awt.*;

import javax.swing.JLabel;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.DialogTester;

/**
 * Unit test to verify proper capture of user semantic events on a Dialog.
 */
public class DialogRecorderTest extends AbstractSemanticRecorderFixture {

    private DialogTester tester;

    public DialogRecorderTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tester = new DialogTester();
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new DialogRecorder(r);
    }

    public void testNoCaptureResizeIfNotResizable() {
        final Dialog d = new Dialog(new Frame(getName()), getName());
        d.add(new JLabel(getName()));
        d.setResizable(false);
        showWindow(d);
        startRecording();
        // NOTE: don't use tester resize/actionResize, since it checks the
        // dialog for resizability.  
        invokeAndWait(new Runnable() { public void run() {
            d.setSize(d.getWidth()*2, d.getHeight());
        }});
        assertNoStep();
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, DialogRecorderTest.class);
    }
}
