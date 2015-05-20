package abbot.editor.recorder;

import java.awt.*;

import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.Platform;
import abbot.script.Resolver;
import abbot.tester.JInternalFrameTester;

/**
 * Unit test to verify proper capture of user semantic events on a Window.
 */
public class JInternalFrameRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JInternalFrame frame;
    private JInternalFrameTester tester;

    /** Special adapter to catch events on JInternalFrame instances. */
    private class InternalFrameWatcher 
        extends AbstractInternalFrameWatcher {
        public InternalFrameWatcher(JInternalFrame frame) { super(frame); }
        protected void dispatch(AWTEvent e) {
            insertEvent(e);
        }
    }

    public void testCaptureShowHide() throws Throwable {
        startRecording();
        showInternalFrame();
        assertStep("Wait for ComponentShowing");

        frame.setVisible(false);
        assertStep("Wait for !ComponentShowing");
    }

    /*
    public void testCaptureMaximizeNormalize() throws Throwable {
        showInternalFrame();
        startRecording();
        tester.actionMaximize(frame);
        assertStep("Maximize\\(.*\\)");
        tester.actionNormalize(frame);
        assertStep("Normalize\\(.*\\)");
    }
    */

    public void testCaptureIconifyDeiconify() throws Throwable {
        showInternalFrame();
        startRecording();
        tester.actionIconify(frame);
        assertStep("Iconify\\(.*\\)");
        tester.actionDeiconify(frame);
        assertStep("Deiconify\\(.*\\)");
    }

    public void testCaptureMove() throws Throwable {
        showInternalFrame();
        startRecording();
        Point p = frame.getLocation();
        tester.actionMove(frame, p.x + 10, p.y + 10);
        assertStep("Move\\(.*\\)");
    }

    public void testCaptureResize() throws Throwable {
        showInternalFrame();
        startRecording();
        Dimension size = frame.getSize();
        tester.actionResize(frame, size.width + 10, size.height + 10);
        assertStep("Resize\\(.*\\)");
    }

    public void testCaptureClose() throws Throwable {
        showInternalFrame();
        startRecording();
        tester.actionClose(frame);
        assertStep("Close\\(.*\\)");
    }

    private void showInternalFrame() throws Throwable {
        EventQueue.invokeAndWait(new Runnable() {
            public void run() { frame.show(); }
        });
    }

    protected void setUp() {
        if (Platform.JAVA_VERSION < Platform.JAVA_1_4) {
            fail("Internal frame tracking not supported prior to 1.4");
        }

        JDesktopPane dtpane = new JDesktopPane();
        frame = new JInternalFrame(getName(), true, true, true, true);
        frame.getContentPane().add(new JLabel(getName()));

        dtpane.add(frame);
        showFrame(dtpane, new Dimension(400, 400));
        frame.setLocation(10, 10);
        frame.setSize(300, 300);

        new InternalFrameWatcher(frame);
        tester = new JInternalFrameTester();
    }

    public JInternalFrameRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JInternalFrameRecorder(r);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JInternalFrameRecorderTest.class);
    }
}
