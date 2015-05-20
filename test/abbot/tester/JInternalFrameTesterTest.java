package abbot.tester;

import java.awt.*;

import javax.swing.*;
import javax.swing.event.*;

import junit.extensions.abbot.*;

/** Unit test to verify the JInternalFrameTester class.<p> */

public class JInternalFrameTesterTest extends ComponentTestFixture {

    private JInternalFrameTester tester;
    private JInternalFrame frame;
    
    
    
    

    public void testNormalizeIcon() {
        tester.actionIconify(frame);
        try {
            tester.actionNormalize(frame);
            fail("Normalizing an icon should fail");
        }
        catch(ActionFailedException e) {
        }
    }

    public void testDeiconifyFrame() {
        tester.actionDeiconify(frame);
    }

    public void testMaximizeIcon() {
        Dimension size = frame.getSize();
        tester.actionIconify(frame);
        tester.actionMaximize(frame);
        assertTrue("Frame not maximized", !size.equals(frame.getSize()));
    }

    public void testMaximizeNormalize() {
        Dimension size = frame.getSize();
        tester.actionMaximize(frame);
        assertTrue("Frame not maximized", !size.equals(frame.getSize()));
        tester.actionNormalize(frame);
        assertEquals("Frame not restored", size, frame.getSize());
    }

    public void testIconifyDeiconify() {
        tester.actionIconify(frame);
        assertTrue("Frame not iconified", frame.isIcon());
        tester.actionDeiconify(frame);
        assertTrue("Frame still iconified", !frame.isIcon());
    }

    public void testMove() {
        Point loc = new Point(100, 100);
        tester.actionMove(frame, loc.x, loc.y);
        assertEquals("Frame not moved", loc, frame.getLocation());
    }

    public void testResize() {
        Dimension size = new Dimension(200, 200);
        tester.actionResize(frame, size.width, size.height);
        assertEquals("Frame not resized", size, frame.getSize());
    }

    private boolean gotClosing = false;
    public void testClose() {
        frame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosing(InternalFrameEvent ev) {
                gotClosing = true;
            }
        });
        tester.actionClose(frame);
        assertTrue("No INTERNAL_FRAME_CLOSING event generated", gotClosing);
    }

    protected void setUp() throws Exception {
        
        tester = new JInternalFrameTester();
        
        JDesktopPane dtpane = new JDesktopPane();
        frame = new JInternalFrame(getName(), true, true, true, true);
        frame.getContentPane().add(new JLabel(getName()));

        dtpane.add(frame);
        showFrame(dtpane, new Dimension(400, 400));
        frame.setLocation(10, 10);
        frame.setSize(300, 300);
        EventQueue.invokeAndWait(new Runnable() {
            public void run() { frame.show(); }
        });
    }

    /** Create a new test case with the given name. */
    public JInternalFrameTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JInternalFrameTesterTest.class);
    }
}

