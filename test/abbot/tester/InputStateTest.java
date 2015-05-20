package abbot.tester;

import java.awt.*;
import java.awt.event.*;
import java.lang.ref.WeakReference;

import javax.swing.*;

import junit.extensions.abbot.*;

/** Verify proper operation of tracking input state. */
public class InputStateTest extends ComponentTestFixture {

    private Robot robot = null;
    private InputState state = null;

    // sporadic w32 failures (mouse component not null after exit)
    public void testTrackMouse() {
        assertNull("Should be no mouse component w/no components",
                   state.getMouseComponent());
        JButton button = new JButton(getName());
        Frame frame = showFrame(button);
        Point center = new Point(button.getWidth()/2, button.getHeight()/2);
        robot.mouseMove(button, center.x, center.y);
        robot.waitForIdle();
        assertEquals("Wrong mouse component",
                     Robot.toString(button),
                     Robot.toString(state.getMouseComponent()));
        assertEquals("Wrong relative location",
                     center, state.getMouseLocation());
        Point loc = button.getLocationOnScreen();
        loc.translate(center.x, center.y);
        assertEquals("Wrong screen location",
                     loc, state.getMouseLocationOnScreen());

        // FIXME w/o the second wait, w32 fails this test about 1 in 4
        // (26/100 when repeated).  Not sure why the state is delayed in
        // updating its internal state.
        robot.mouseMove(frame, -1, -1);
        robot.waitForIdle();
        robot.waitForIdle();

        assertNull("Should be no mouse component after exit",
                   state.getMouseComponent());
        assertNull("Should be no relative location",
                   state.getMouseLocation());
        assertTrue("Should be a last known screen location",
                   state.getMouseLocationOnScreen() != null);
    }

    public void testTrackNonNativeDrag() {
        JButton one = new JButton("One");
        JButton two = new JButton("Two");
        JPanel pane = new JPanel();
        pane.add(one);
        pane.add(two);
        Frame frame = showFrame(pane);
        frame.addMouseListener(new MouseAdapter() { });
        frame.addMouseMotionListener(new MouseMotionAdapter() { });
        robot.mouseMove(one);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(one);
        robot.waitForIdle();
        assertEquals("Wrong drag start component",
                     Robot.toString(one),
                     Robot.toString(state.getDragSource()));
        assertEquals("Wrong mouse component",
                     Robot.toString(one),
                     Robot.toString(state.getMouseComponent()));

        robot.mouseMove(two);
        robot.waitForIdle();
        assertEquals("Containing component should be tracked during drag",
                     Robot.toString(two),
                     Robot.toString(state.getMouseComponent()));

        // latest revs of 1.3, 1.4 don't provide enter/exit events while
        // dragging
        /*
        if (Robot.getEventMode() == Robot.EM_ROBOT) {
            robot.mouseMove(frame, frame.getWidth() * 2,
                            frame.getHeight() * 2);
            robot.mouseMove(frame, frame.getWidth() * 2 + 1,
                            frame.getHeight() * 2);
            robot.waitForIdle();
            assertEquals("Drag outside should leave no current mouse component",
                         null, state.getMouseComponent());
        }
        */
    }

    /** Not all components track mouse events. */
    public void testMouseComponent() {
        JLabel label = new JLabel(getName());
        Frame f = showFrame(label);
        robot.click(label);
        robot.waitForIdle();
        assertEquals("Wrong mouse component",
                     f, state.getMouseComponent());
        assertEquals("Wrong ultimate mouse component",
                     label, state.getUltimateMouseComponent());
    }

    public void testNoReferencesHeld() {
        JLabel label = new JLabel(getName());
        Frame f = showFrame(label);
        WeakReference ref = new WeakReference(label);
        robot.click(label);
        robot.waitForIdle();

        // Ensure things get properly GCed
        f.dispose();
        f.remove(label);
        f = null;
        label = null;
        System.gc();
        assertNull("Someone still references the label", ref.get());

        assertTrue("Should be no mouse component",
                   state.getMouseComponent() == null);
    }

    protected void setUp() {
        state = new InputState();
        robot = getRobot();
    }

    protected void tearDown() {
        state.dispose();
        state = null;
        robot = null;
    }

    public InputStateTest(String name) { super(name); }
    public static void main(String[] args) {
        RepeatHelper.runTests(args, InputStateTest.class);
    }
}
