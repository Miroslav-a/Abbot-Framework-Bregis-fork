package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import java.io.PrintStream;

import java.lang.reflect.Field;

import javax.swing.*;
import junit.framework.*;
import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;

public class WindowTrackerTest extends ComponentTestFixture {
    
    private class MouseWatcher extends MouseAdapter {
        private boolean gotClick = false;
        public void mousePressed(MouseEvent e) {
            gotClick = true;
        }
    }

    private WindowTracker tracker;

    private void wait(Window w, boolean state, String msg) {
        Timer timer = new Timer();
        while (tracker.isWindowReady(w) != state) {
            if (timer.elapsed() > WindowTracker.WINDOW_READY_DELAY+500)
                fail(msg);
            try { Thread.sleep(10); } catch(InterruptedException e) { }
        }
    }

    protected void setUp() {
        tracker = new WindowTracker();
    }

    protected void tearDown() {
        tracker.dispose();
        tracker = null;
    }

    private class MissedShow1 extends AssertionFailedError {
        public MissedShow1() {
            super("Window not ready after initial show");
        }
    }
    private class MissedShow2 extends AssertionFailedError {
        public MissedShow2() {
            super("Window not ready after subsequent show");
        }
    }

    public void testTrackWindowState() throws Throwable {
        Frame w = new Frame(getName());
        w.pack();
        w.setVisible(true);
        wait(w, true, "WindowTracker didn't catch initial Window.show()");
        assertTrue("Window should be showing", w.isShowing());
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));

        w.setVisible(false);
        wait(w, false, "WindowTracker didn't catch Window.hide()");
        assertTrue("Window should not be showing", !w.isShowing());
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));

        w.setVisible(true);
        wait(w, true, "WindowTracker didn't catch Window.show() after hide");
        assertTrue("Window should be showing", w.isShowing());
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));

        // Generates WINDOW_CLOSE, may or may not generate COMPONENT_HIDDEN
        w.dispose();
        wait(w, false, "WindowTracker didn't catch Window.dispose()");
        assertTrue("Window should not be showing", !w.isShowing());
        assertEquals("Trackers should have been removed",0, countWindowAdapters(w));
    }

    public void testSingleWindowReady() throws Throwable {
        final JFrame f = new JFrame(getName());
        MouseWatcher watcher = new MouseWatcher();
        f.addMouseListener(watcher);
        f.getContentPane().add(new JLabel(getName()));
        f.pack();
        f.setSize(100, 100);
        f.setVisible(true);
        try {
            wait(f, true, "WindowTracker didn't catch initial Window.show");
            assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(f));

            getRobot().click(f);
            getRobot().waitForIdle();
            if (!watcher.gotClick)
                throw new MissedShow1();
            
            watcher.gotClick = false;
            f.setVisible(false);
            wait(f, false, "WindowTracker didn't catch Window.hide after show");
            assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(f));
            
            f.setVisible(true);
            wait(f, true, "WindowTracker didn't catch Window.show after hide");
            assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(f));
            assertTrue("Window not ready", tracker.isWindowReady(f));
            getRobot().click(f);
            getRobot().waitForIdle();
            if (!watcher.gotClick)
                throw new MissedShow2();
        }
        finally {
            // Do on the event queue otherwise the following tests will fail
            getRobot().invokeAndWait(new Runnable() { public void run() {
                f.dispose();
            }});

            assertEquals("Trackers should be removed at this point",0, countWindowAdapters(f));
        }
    }

    public void testComplexWindowReady() throws Throwable {
        JList list = new JList(new String[] {
            "one", "two", "three", "four", "five"
        });
        JFrame f = new JFrame(getName() + "1");
        f.getContentPane().add(list);
        MouseWatcher watcher = new MouseWatcher();
        list.addMouseListener(watcher);
        f.pack();
        f.setLocation(100, 100);
        f.setVisible(true);
        try {
            wait(f, true, "WindowTracker didn't catch window");
            
            getRobot().click(list);
            getRobot().waitForIdle();
            
            assertTrue("Window not ready for click", watcher.gotClick);

            assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(f));
        }
        finally {
            f.dispose();
        }
    }

    // Not required when using mouse motion to detect window readiness
    /*
    public void testTrackRepeatedWindowReady() throws Throwable {
        // This test is only required in Robot mode
        if (Robot.getEventMode() != Robot.EM_ROBOT)
            return;
        int EXPECTED = 100;
        int count1 = EXPECTED;
        int count2 = EXPECTED;
        int count3 = EXPECTED;
        for (int i=0;i < EXPECTED;i++) {
            try {
                testSingleWindowReady();
            }
            catch(MissedShow1 e) {
                --count1;
            }
            catch(MissedShow2 e) {
                --count2;
            }
            try {
                testComplexWindowReady();
            }
            catch(AssertionFailedError e) {
                --count3;
            }
        }
        assertEquals("Missed some clicks on initial show", EXPECTED, count1);
        assertEquals("Missed some clicks on subsequent show", EXPECTED, count2);
        assertEquals("Missed clicks on complex window", EXPECTED, count3);
    }
    */
    public void testTrackWindowReadyAfterDispose() throws Exception {
        final Frame w = new Frame(getName());
        getRobot().invokeAndWait(new Runnable() { public void run() {
            w.pack();
            w.setVisible(true);
        }});
        wait(w, true, "Didn't catch initial Window.show()");
        assertTrue("Window showing", w.isShowing());
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));

        getRobot().invokeAndWait(new Runnable() { public void run() {
            w.dispose();
        }});
        wait(w, false, "Didn't catch Window.dispose()");
        assertTrue("Window not hidden", !w.isShowing());
        assertEquals("Should be zero after a dispoe",0, countWindowAdapters(w));
        
        getRobot().invokeAndWait(new Runnable() { public void run() {
            w.pack();
            w.setVisible(true);
        }});
        wait(w, true, "Didn't catch Window.show() after dispose(), pack()");
        assertTrue("Window not showing", w.isShowing());
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));
    }
    
    public void testDetectEmptyDialog() {
        final Frame w = new Frame(getName());
        final Dialog d = new Dialog(w, getName());
        getRobot().invokeAndWait(new Runnable() { public void run() {
            w.pack();
            w.setVisible(true);
            d.pack();
            d.setVisible(true);
        }});
        wait(d, true, "Didn't catch empty dialog");
        assertTrue("Dialog not showing", d.isShowing());

        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(w));
        assertEquals("Should only be one adapter per window, but in this test there is another tracker",2, countWindowAdapters(d));
    }
    
    
    
    
    private int countWindowAdapters(Window win) {
        try
        {
            Field $windowListener = Window.class.getDeclaredField("windowListener");
            $windowListener.setAccessible(true);
            WindowListener aem = (WindowListener) $windowListener.get(win);
            WindowListener[] listeners = AWTEventMulticaster.getListeners(aem, WindowListener.class);

            int count = 0;            
            for (WindowListener wl : listeners)
            {
               if (wl instanceof WindowTracker.WindowWatcher) {
                   count ++;
               }
            }
            
            return count;
        }
        catch (Throwable th) {
            throw new RuntimeException(th);
        }
    }
    
    

    public static void main(String[] args) {
        RepeatHelper.runTests(args, WindowTrackerTest.class);
    }
}
