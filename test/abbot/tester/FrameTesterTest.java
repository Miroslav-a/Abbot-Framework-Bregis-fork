package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import javax.swing.JLabel;
import javax.swing.JFrame;

import junit.extensions.abbot.*;

/** Unit test to verify the FrameTester class.<p> */

public class FrameTesterTest extends ComponentTestFixture {

    private FrameTester tester;

    private static final int DX = 300;
    private static final int DY = 240;

    /** Create a new test case with the given name. */
    public FrameTesterTest(String name) {
        super(name);
    }

    protected void setUp() {
        tester = new FrameTester();
    }

    public void testMoveBy() {
        Frame frame = showFrame(new JLabel(getName()));
        Point loc = frame.getLocationOnScreen();
        loc.x += DX;
        loc.y += DY;
        tester.actionMoveBy(frame, DX, DY);
        assertEquals("Frame not moved to the desired location",
                     loc, frame.getLocationOnScreen());

        // FIXME +/- coords
        // FIXME scale 5, 10, 50, 100, 500
    }

    public void testNoResize() {
        Frame frame = showFrame(new JLabel(getName()));
        frame.setResizable(false);
        Dimension size = frame.getSize();
        size.width += DX;
        size.height += DY;
        try {
            tester.actionResizeBy(frame, DX, DY);
            fail("Resizing of non-resizable frames should not be allowed");
        }
        catch(ActionFailedException e) {
        }
    }

    // FIXME intermittent failure on linux 1.4.2_04
    public void testResizeBy() {
        Frame frame = showFrame(new JLabel(getName()));
        Dimension size = frame.getSize();
        size.width += DX;
        size.height += DY;
        tester.actionResizeBy(frame, DX, DY);
        assertEquals("Frame not resized to the desired size",
                     size, frame.getSize());
    }

    private boolean gotClose = false;
    public void testClose() {
        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent ev) {
                gotClose = true;
            }
        });
        showWindow(frame);
        tester.actionClose(frame);
        assertTrue("No WINDOW_CLOSING event generated", gotClose);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, FrameTesterTest.class);
    }
}

