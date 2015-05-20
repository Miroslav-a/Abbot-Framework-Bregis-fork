package abbot.tester;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.util.Bugs;

/** Unit test to verify Robot drag/drop operation.  Include "Native" in 
 * the test name if the test depends on native drag and drop. 
 */
// FIXME w32 has sporadic errors (4 failures on a repeat of 50) where the drag
// gesture is not recognized (it might be that post-press mouse move is missed)
// The failure is also present on non-native drags
public class RobotDragDropTest extends ComponentTestFixture {

    /** Ensure drag gestures are recognized. */
    public void testNativeDragDrop() throws Exception {
        DragLabel c = new DragLabel(getName());
        showFrame(c);
        robot.drag(c, c.getWidth()/2, c.getHeight()/2);
        Timer timer = new Timer();
        while (!c.dragStarted) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                fail("Drag gesture not recognized");
            }
            if (c.exception != null)
                throw c.exception;
            robot.sleep();
        }

        robot.drop(c, 1, 1);
        timer.reset();
        while (!c.dropAccepted || !c.dragEnded) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                assertTrue("Drag never ended", c.dragEnded);
                assertTrue("Drag not accepted", c.dropAccepted);
            }
            robot.sleep();
        }
        // Fails on OSX 1.3.1, 1.4.1 (VM bug)
        assertTrue("Drag ended, but drop failed", c.dropSuccessful);
    }

    public void testNativeDragDropAcrossComponents() throws Exception {
        DragLabel c1 = new DragLabel("Start drag here", false);
        DropLabel c2 = new DropLabel("Drop here");
        JPanel pane = new JPanel();
        pane.add(c1);
        pane.add(c2);

        showFrame(pane);
        robot.drag(c1, c1.getWidth()/2, c1.getHeight()/2);
        Timer timer = new Timer();
        while (!c1.dragStarted) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                fail("Drag gesture not recognized");
            }
            if (c1.exception != null)
                throw c1.exception;
            robot.sleep();
        }
        robot.drop(c2, c2.getWidth()/2, c2.getHeight()/2);
        timer.reset();
        // source drag exit is not reliable; w32 sends it after drop accepted,
        // OSX never sends it
        while (!c2.dragEntered || !c2.dropAccepted || !c1.dragEnded) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                assertTrue("Never received drag enter on target", c2.dragEntered);
                assertTrue("Never received drag end", c1.dragEnded);
                assertTrue("Drop not accepted", c2.dropAccepted);
            }
            robot.sleep();
        }
        // Fails on OSX 1.3.1, 1.4.1 (VM bug)
        assertTrue("Drag ended, but drop failed", c1.dropSuccessful);
    }

    public void testNativeDragDropToJTree() throws Exception {
        DragLabel label = new DragLabel("Start drag here", false);
        DropTree tree = new DropTree();
        JPanel pane = new JPanel();
        pane.add(label);
        pane.add(tree);
        showFrame(pane);
        robot.drag(label, label.getWidth()/2, label.getHeight()/2);
        Timer timer = new Timer();
        while (!label.dragStarted) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                fail("Drag gesture not recognized");
            }
            if (label.exception != null)
                throw label.exception;
            robot.sleep();
        }
        Rectangle rect = tree.getRowBounds(2);
        robot.drop(tree, rect.x + rect.width/2, rect.y + rect.height/2);
        timer.reset();
        while (!tree.dragEntered
               || !tree.dropAccepted || !label.dragEnded) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                assertTrue("Drag never entered drop target",
                           tree.dragEntered);
                assertTrue("Drag never ended", label.dragEnded);
                assertTrue("Drag not accepted", tree.dropAccepted);
            }
            robot.sleep();
        }
        // Fails on OSX 1.3.1, 1.4.1 (VM bug)
        assertTrue("Drag ended, but drop failed", label.dropSuccessful);
    }

    public void testNativeDragDropToJTable() throws Exception {
        DragLabel label = new DragLabel("Start drag here", false);
        DropTable table = new DropTable();
        JPanel pane = new JPanel();
        pane.add(label);
        pane.add(table);
        showFrame(pane);
        robot.drag(label, label.getWidth()/2, label.getHeight()/2);
        Timer timer = new Timer();
        while (!label.dragStarted) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                fail("Drag gesture not recognized");
            }
            if (label.exception != null)
                throw label.exception;
            robot.sleep();
        }
        Rectangle rect = table.getCellRect(1, 1, false);
        robot.drop(table, rect.x + rect.width/2, rect.y + rect.height/2);
        timer.reset();
        while (!table.dragEntered
               || !table.dropAccepted || !label.dragEnded) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                assertTrue("Drag never entered drop target",
                           table.dragEntered);
                assertTrue("Drag never ended", label.dragEnded);
                assertTrue("Drag not accepted", table.dropAccepted);
            }
            robot.sleep();
        }
        // Fails on OSX 1.3.1, 1.4.1 (VM bug)
        assertTrue("Drag ended, but drop failed", label.dropSuccessful);
    }

    public void testJavaDragDrop() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        class DragListener extends MouseInputAdapter {
            public boolean gotDrag = false;
            public void mouseDragged(MouseEvent me){ gotDrag = true; }
        }
        DragListener ma = new DragListener();
        label.addMouseListener(ma);
        label.addMouseMotionListener(ma);
        getRobot().drag(label, 1, 1);
        getRobot().waitForIdle();
        getRobot().drop(label, label.getWidth()/2,
                        label.getHeight()/2);
        getRobot().waitForIdle();
        assertTrue("Should have seen a drag event", ma.gotDrag);
    }
    
    /** Note the event mode when reporting this test's name. */
    public String getName() { 
        return Robot.getEventMode() == Robot.EM_AWT
            ? super.getName() + " (AWT mode)"
            : super.getName();
    }

    public void runBare() throws Throwable {
        if (!Bugs.dragDropRequiresNativeEvents()
            || Robot.getEventMode() == Robot.EM_ROBOT
            || getName().indexOf("Native") == -1) {
            super.runBare();
        }
        else {
            System.err.println("Skipping test " + getName());
        }
    }

    private Frame frame;
    protected Frame showFrame(Component c) {
        return frame = super.showFrame(c);
    }

    private Robot robot;
    protected void setUp() {
        robot = getRobot();
    }

    protected void tearDown() {
        if (frame != null) {
            // Clear the DnD state
            robot.click(frame);
        }
    }

    /** Provide for repetitive testing on individual tests. */
    public static void main(String[] args) {
        RepeatHelper.runTests(args, RobotDragDropTest.class);
    }
}
