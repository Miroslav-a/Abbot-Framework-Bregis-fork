package abbot.editor.recorder;

import java.awt.*;

import javax.swing.JLabel;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;
import abbot.tester.Robot;

/**
 * Unit test to verify proper capture of user semantic events on a Window.
 */
public class WindowRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private Window window;
    private WindowTester tester;

    public WindowRecorderTest(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        super.setUp();
        tester = new WindowTester();
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new WindowRecorder(r);
    }

    protected Frame showWindow() {
        Frame frame = showFrame(new JLabel(getName()));
        window = new Window(frame);
        window.add(new JLabel("One big label"));
        showWindow(window);
        return frame;
    }

    public void testCaptureClose() {
        showWindow();
        startRecording();
        tester.actionClose(window);
        assertStep("Close\\(.*\\)");
    }

    public void testCaptureResize() {
        showWindow();
        startRecording();
        try {
            tester.actionResize(window, 200, 200);
            assertEquals("Window not resized", new Dimension(200, 200),
                         window.getSize());
            assertStep("Resize\\(.*\\)");
        }
        catch(ActionFailedException afe) {
            assertTrue("Resizing should only be disallowed on mac/w32",
                       !Robot.canResizeWindows());
        }
    }

    // Linux/1.4.1_02 fails when this is run with the other tests on a VNC
    // connection; INPUT_METHOD_TEXT_CHANGED gets generated, which stops the
    // recorder from receiving the COMPONENT_MOVED event.
    public void testCaptureMove() {
        // Move a frame, since windows aren't always moveable
        Frame f = showFrame(new JLabel(getName()));
        startRecording();
        tester.actionMove(f, 200, 200);
        assertEquals("Frame not moved", new Point(200, 200),
                     f.getLocationOnScreen());
        assertStep("Move\\(.*\\)");
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, WindowRecorderTest.class);
    }
}
