package junit.extensions.abbot;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.swing.*;

import abbot.tester.Robot;
import abbot.tester.ComponentTester;
import abbot.util.*;
import abbot.finder.matchers.*;
import abbot.Log;

/** 
 * Verify {@link ComponentTestFixture} operation.
 */
public class ComponentTestFixtureTest extends ComponentTestFixture {

    private class MouseWatcher extends MouseAdapter {
        private boolean mousePressed;
        public void mousePressed(MouseEvent e) {
            mousePressed = true;
        }
    }

    private Frame frame;
    private Component component;

    /** Used by {@link #testAutoDisposeFields}. */
    public void testWithShowingFrame() {
        frame = showFrame(component = new JLabel(getName()));
    }

    /** Ensure any fields derived from {@link Component} or
        {@link ComponentTester} are set to <code>null</code> to allow for
        proper GC of these objects.
    */
    public void testAutoDisposeFields() throws Throwable {
        ComponentTestFixtureTest test = new ComponentTestFixtureTest();
        test.setName("testWithShowingFrame");
        // This invokes setup/run/teardown
        test.runBare();
        assertNull("Member Frame should be reset to null after test",
                   test.frame);
        assertNull("Member Component should be reset to null after test",
                   test.component);
        assertNull("Member ComponentTester should be reset to null after test",
                   test.component);
    }

    /** Ensure any {@link Window}s shown during a test are disposed after the
        test completes.
    */
    public void testAutoDisposeWindows() throws Throwable {
        ComponentTestFixtureTest test = new ComponentTestFixtureTest();
        test.setName("testWithShowingFrame");
        List list = Arrays.asList(Frame.getFrames());
        // This invokes setup/run/teardown
        test.runBare();
        List list2 = new ArrayList(Arrays.asList(Frame.getFrames()));
        list2.removeAll(list);
        for (Iterator iter=list2.iterator();iter.hasNext();) {
            Window w = (Window)iter.next();
            assertFalse("All test windows should have been disposed",
                        w.isShowing());
        }        
    }

    /** Ensure any frame is ready to receive input after showFrame/Window. */
    public void testShowFrame() throws Throwable {
        // only required in robot mode
        if (Robot.getEventMode() != Robot.EM_ROBOT)
            return;

        int expected = 10;
        int count = 0;
        for (int i=0;i < expected;i++) {
            JList list = new JList(new String[] {
                "one", "two", "three", "four", "five"
            });
            MouseWatcher watcher = new MouseWatcher();
            list.addMouseListener(watcher);
            Frame f = showFrame(list, new Dimension(200, 200));
            java.awt.Robot robot = new java.awt.Robot();
            Point pt = f.getLocationOnScreen();
            robot.mouseMove(pt.x + f.getWidth()/2,
                            pt.y + f.getHeight()/2);
            robot.mousePress(MouseEvent.BUTTON1_MASK);
            robot.mouseRelease(MouseEvent.BUTTON1_MASK);
            robot.delay(Robot.getEventPostDelay());
            // Use the improved idle wait
            getRobot().waitForIdle();
            if (watcher.mousePressed) {
                ++count;
            }
            f.setVisible(false);
        }
        assertEquals("Missed some clicks", expected, count);
    }

    public void testShowHideShow() {
        Frame f = showFrame(new JLabel(getName()));
        assertTrue("Frame should be showing", f.isShowing());
        if (Robot.getEventMode() == Robot.EM_ROBOT) {
            assertTrue("Frame should be ready",
                       getWindowTracker().isWindowReady(f));
        }
        hideWindow(f);
        assertTrue("Frame should be hidden" , !f.isShowing());
        showWindow(f);
        assertTrue("Frame not showing after hide", f.isShowing());
    }

    // FIXME this fails on linux when run with the full suite
    public void testShowDisposeShow() {
        Frame f = showFrame(new JLabel(getName()));
        assertTrue("Frame should be showing", f.isShowing());
        if (Robot.getEventMode() == Robot.EM_ROBOT) {
            assertTrue("Frame should be ready",
                       getWindowTracker().isWindowReady(f));
        }
        Log.log("dispose");
        disposeWindow(f);
        assertTrue("Frame showing after dispose", !f.isShowing());
        Log.log("show");
        try {
            showWindow(f);
            Log.log("show done");
            assertTrue("Frame not showing after a previous dispose",
                       f.isShowing());
        }
        finally {
            // Have to do this manually, since the auto-dispose will
            // have already ignored this frame since we disposed it
            f.dispose();
        }
    }

    public void testIsShowingWithFrame() {
        Frame frame = showFrame(new JLabel(getName()));
        assertEquals("Frame was shown, but isShowing doesn't agree", 
                     true, isShowing(frame.getTitle()));
        hideWindow(frame);
        assertEquals("Frame was hidden, but isShowing doesn't agree",
                     false, isShowing(frame.getTitle()));
    }

    public void testisShowingWithFrameAndDialog() {
        final String title = "Not me";
        final String title2 = "Wait for me";
        Frame frame = showFrame(new Label(getName()), new Dimension(200, 200));
        JDialog d1 = new JDialog(frame, title, true);
        d1.getContentPane().add(new JLabel("Modal dialog"));
        JDialog d2 = new JDialog(d1, title2, false);
        d2.getContentPane().add(new JLabel("Non-modal subdialog"));
        showWindow(d1);
        showWindow(d2);
        assertTrue("Second dialog should have been detected",
                   isShowing(title2));
    }

    private void checkDialog(Frame parent, String title) {
        // Now try it with a dialog
        JDialog d = new JDialog(parent, title);
        d.getContentPane().add(new JLabel("I'm a dialog, and I'm OK"));

        showWindow(d);
        assertTrue("Dialog should be showing", d.isShowing());
        assertTrue("Dialog '" + title + "' wasn't showing or wasn't detected",
                   isShowing(title));

        hideWindow(d);
        assertFalse("Dialog should not be showing after hideWindow", d.isShowing());
	assertFalse("Dialog '" + title + "' wasn't hidden", isShowing(title)); 
    }

    public void testIsShowingWithDialog() {
        Frame frame = showFrame(new JPanel(), new Dimension(200, 200));
        String title = frame.getTitle();
        assertTrue("Frame '" + title + "' wasn't showing or wasn't detected",
                   isShowing(title));

        // Try a dialog with a non-null parent
        //System.out.println("show/hide w/frame");
        checkDialog(frame, "Dialog under " + frame.getTitle());

        // Now try one with a null parent
        //System.out.println("show/hide w/o frame");
        checkDialog(null, "Dialog under null frame");
        Iterator iter = getHierarchy().getRoots().iterator();
        while (iter.hasNext())
            getHierarchy().dispose((Window)iter.next());

        // And make sure we can find an identical one the next time it's shown
        //System.out.println("show/hide w/o frame, again");
        checkDialog(null, "Dialog under null frame");
    }

    public void testModalDialogWontBlock() throws Exception {
        JButton b = new JButton("Push me");
        Frame f = showFrame(b);
        final JDialog d = new JDialog(f, "Modal Dialog", true);
        d.getContentPane().add(new JLabel("Modal Dialog Contents"));
        d.getContentPane().setBackground(Color.red);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                d.pack();
                d.setVisible(true);
            }
        });
        getRobot().click(b);
        getRobot().waitForIdle();
        assertTrue("Dialog is showing and test thread not blocked",
                   d.isShowing());
        JDialog found = (JDialog)
            getFinder().find(new ClassMatcher(JDialog.class, true));
        assertEquals("Wrong dialog found", d, found);
        assertEquals("Wrong-colored dialog is showing",
                     Color.red, found.getContentPane().getBackground());
    }

    public static class Fixture extends ComponentTestFixture {
        private RuntimeException edtException;
        private RuntimeException mainException;
        public Fixture(RuntimeException edtException) {
            this(edtException, null);
        }
        public Fixture(RuntimeException edtException,
                       RuntimeException mainException) {
            super("testCatchEventThreadException");
            this.edtException = edtException;
            this.mainException = mainException;
        }
        public void testCatchEventThreadException() throws Throwable {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                        throw edtException;
                }
            });
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() { }
            });
            if (mainException != null)
                throw mainException;
        }
    }

    public void testFailWithEventThreadException() throws Throwable {
        class TestException extends RuntimeException {
            public TestException() { super("Test exception"); }
        }
        final TestException e = new TestException();
        ComponentTestFixture fixture = new Fixture(e);
        try {
            fixture.runBare();
            fail("Test should re-throw the event dispatch exception");
        }
        catch(ComponentTestFixture.EventDispatchException thrown) {
            assertEquals("EDT exception should be thrown",
                         e, thrown.getTargetException());
            EDTExceptionCatcher.clear();
        }
    }

    public void testFailOnFirstExceptionThrown() throws Throwable {
        class TestException extends RuntimeException {
            public TestException() { super("Test exception"); }
        }
        final TestException e = new TestException();
        final TestException main = new TestException();
        ComponentTestFixture fixture = new Fixture(e, main);
        try {
            fixture.runBare();
            fail("Test should re-throw the event dispatch exception");
        }
        catch(ComponentTestFixture.EventDispatchException thrown) {
            assertEquals("Exception on EDT should be thrown",
                         e, thrown.getTargetException());
            EDTExceptionCatcher.clear();
        }
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ComponentTestFixtureTest.class);
    }
}
