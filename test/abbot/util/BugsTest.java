/*
 * Copyright 2002-2005 Timothy Wall
 *
 */
package abbot.util;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.tester.Robot;
import abbot.tester.ComponentTester;
import abbot.Platform;

import java.util.concurrent.Callable;

/**
 Provides platform bugs detection.  Also serves as a repository of tests for
 all VM or OS-related bugs which affect Abbot operation.  A boolean test for
 each of these may appear in <code>abbot.util.Bugs</code>. <p>
 Add tests that show the bug, then disable them only for the specific
 platform/VM versions that are known to display the bug (i.e. use
 Bugs.hasXXX() to avoid running the test).  This ensures that the bug
 is detected/tested for on new platforms/VMs, but doesn't needlessly clutter
 test results with known failures.  Define abbot.test.show_bugs=true in order
 to see the failures when running tests.

 @author twall
 */
public class BugsTest extends ComponentTestFixture {

    /** Indicates whether to test for bugs on a platform where they are known
     * to exist, as opposed to silently ignoring them.
     */
    private static final boolean SHOW_BUGS =
        Boolean.getBoolean("abbot.test.show_bugs");
    private java.awt.Robot robot;
    protected void setUp() throws Exception {
        robot = new java.awt.Robot();
    }
    
    // OSX 1.4+
    public void testWindowMotionBug() throws Exception {
        // NOTE: this test fails in AWT mode
        if (Robot.getEventMode() == Robot.EM_AWT)
            return;

        if (!SHOW_BUGS && Bugs.hasMissingWindowMouseMotion())
            return;

        Frame f = new Frame(getName());
        Window w = new Window(f);
        showWindow(w, new Dimension(100, 100));
        class Flag { volatile boolean flag; }
        final Flag flag1 = new Flag();
        final Flag flag2 = new Flag();
        AWTEventListener listener = new AWTEventListener() {
            public void eventDispatched(AWTEvent e) {
                if (e.getID() == MouseEvent.MOUSE_MOVED) {
                    flag1.flag = true;
                }
            }
        };
        w.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) { flag2.flag = true; }
        });
        new WeakAWTEventListener(listener, MouseEvent.MOUSE_MOTION_EVENT_MASK);

        int x = w.getX() + w.getWidth()/2;
        int y = w.getY() + w.getHeight()/2;
        robot.mouseMove(x, y);
        robot.mouseMove(x, y+1);
        Timer timer = new Timer();
        while (!flag1.flag || !flag2.flag) {
            if (timer.elapsed() > 5000) {
                fail("No motion event received: AWTEventListener=" + flag1.flag
                     + ", MouseListener=" + flag2.flag);
            }
        }
    }

    // OSX 1.4+
    public void testButtonSwapBug() throws Throwable {
        // NOTE: this test fails in AWT mode
        if (Robot.getEventMode() == Robot.EM_AWT)
            return;
        if (!SHOW_BUGS && Bugs.hasRobotButtonsSwapped())
            return;
        javax.swing.JLabel label = new javax.swing.JLabel(getName());
        MouseWatcher watcher = new MouseWatcher();
        label.addMouseListener(watcher);
        showFrame(label);
        java.awt.Point pt = label.getLocationOnScreen();
        robot.mouseMove(pt.x + label.getWidth()/2, pt.y + label.getHeight()/2);
        robot.mousePress(MouseEvent.BUTTON2_MASK);
        robot.mouseRelease(MouseEvent.BUTTON2_MASK);
        Timer timer = new Timer();
        while (!watcher.gotClick) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never received button 2 click event");
            robot.delay(200);
        }
        assertEquals("This platform has bad robot button 2 mask mapping",
                     MouseEvent.BUTTON2_MASK, watcher.modifiers);

        watcher.gotClick = false;
        robot.mousePress(MouseEvent.BUTTON3_MASK);
        robot.mouseRelease(MouseEvent.BUTTON3_MASK);
        timer.reset();
        while (!watcher.gotClick) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never received button 3 click event");
            robot.delay(200);
        }
        assertEquals("This platform has bad robot button 3 mask mapping",
                     MouseEvent.BUTTON3_MASK, watcher.modifiers);
    }

    // Found on several platforms.  A subsequent mouse click on an entirely
    // different component is considered to have a click count of 2 if it
    // occurs within the double-click interval.
    public void testMultiClickFrameBug() {
        if (!SHOW_BUGS && Bugs.hasMultiClickFrameBug())
            return;
        String[] data = { "one", "two", "three", "four", "five" };
        JFrame frame1 = new JFrame(getName() + "1");
        JList list1 = new JList(data);
        list1.setName("List 1");
        frame1.getContentPane().add(list1);
        
        JFrame frame2 = new JFrame(getName() + "2");
        JList list2 = new JList(data);
        list2.setName("List 2");
        frame2.getContentPane().add(list2);
        
        MouseWatcher mw1 = new MouseWatcher();
        MouseWatcher mw2 = new MouseWatcher();
        list1.addMouseListener(mw1);
        list2.addMouseListener(mw2);
        showWindow(frame1);

        Point pt = list1.getLocationOnScreen();
        robot.mouseMove(pt.x + list1.getWidth()/2, pt.y + list1.getHeight()/2);
        robot.mousePress(MouseEvent.BUTTON1_MASK);
        robot.mouseRelease(MouseEvent.BUTTON1_MASK);
        robot.waitForIdle();

        frame1.setVisible(false);
        showWindow(frame2);
        pt = list2.getLocationOnScreen();
        robot.mouseMove(pt.x + list2.getWidth()/2, pt.y + list2.getHeight()/2);
        robot.mousePress(MouseEvent.BUTTON1_MASK);
        robot.mouseRelease(MouseEvent.BUTTON1_MASK);
        robot.waitForIdle();
        Timer timer = new Timer();
        while (!mw1.gotClick) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never received click event on first frame");
            robot.delay(200);
        }
        timer.reset();
        while (!mw2.gotClick) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never received click event on second frame");
            robot.delay(200);
        }
        assertEquals("Multi-click counting should not span frames",
                     1, mw1.clickCount);
        assertEquals("Multi-click counting should not span frames",
                     1, mw2.clickCount);
    }

    /** W32 systems block on AWT PopupMenu.show.  If the method is invoked on
        the EDT, the result is that Robot.waitForIdle will lock up and events
        posted by Robot.postEvent will not be processed until the AWT popup
        goes away.  
    */
    public void testShowAWTPopupMenuBlocks() throws Exception {
        boolean blocks = Bugs.showAWTPopupMenuBlocks();
        if (!SHOW_BUGS && blocks)
            return;

        Frame frame = new Frame(getName());
        final PopupMenu popup = new PopupMenu();
        MenuItem mi = new MenuItem("Open");
        popup.add(mi);
        final Label label = new Label(getName());
        frame.add(label);
        label.add(popup);
        showWindow(frame);
        assertEquals("Detected AWT popup state",
                     false, AWT.isAWTPopupMenuBlocking());
        try {
            class Flag { volatile boolean flag; }
            final Flag flag = new Flag();
            Runnable show = new Runnable() {
                public void run() {
                    popup.show(label, 0, label.getHeight());
                }
            };
            SwingUtilities.invokeLater(show);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    flag.flag = true;
                }
            });
            Timer timer = new Timer();
            while (!flag.flag) {
                if (timer.elapsed() > 5000)
                    fail("An active AWT Popup blocks the event queue");
                Thread.sleep(10);
            }
        }
        finally {
            AWT.dismissAWTPopup();
        }
    }

    /** Robot won't generate an ESC on OSX 1.3.1 (Apple VM bug). */
    public void testEscapeKeyGenerationFailure() throws Exception {
        final JTextField tf = new JTextField();
        tf.setColumns(10);
        KeyWatcher kw = new KeyWatcher();
        tf.addKeyListener(kw);
        showFrame(tf);
	ComponentTester tester = new ComponentTester();
	tester.actionFocus(tf);
        int code = KeyEvent.VK_ESCAPE;
        int mods = 0;
	tester.actionKeyStroke(code, mods);
        assertTrue("Never received key press", kw.gotPress);
        assertTrue("Never received release", kw.gotRelease);
        assertEquals("Wrong key code", code, kw.code);
        assertEquals("Wrong modifiers", mods, kw.modifiers);
    }

    public void testReportsIncorrectLockingKeyState() throws Throwable {
        // NOTE: this test fails in AWT mode
        if (Robot.getEventMode() == Robot.EM_AWT)
            return;

        if (!SHOW_BUGS && Bugs.reportsIncorrectLockingKeyState())
            return;

        // On w32, locking key state requires a non-disposed frame to work
        // properly! Not sure if this is a VM bug or not.
        // FIXME still gets sporadic failures on w32
        if (Platform.isWindows()) {
            showFrame(new JLabel(getName()));
        }

        String[] KEYS = {
            "CAPS",
            "NUM",
            "SCROLL",
            "KANA",
        };
        final int[] CODES = { 
            KeyEvent.VK_CAPS_LOCK,
            KeyEvent.VK_NUM_LOCK,
            KeyEvent.VK_SCROLL_LOCK,
            KeyEvent.VK_KANA_LOCK,
        };
	ComponentTester tester = new ComponentTester();
        for (int i=0;i < CODES.length;i++) {
            try {
                final int nextKey = CODES[i];
                boolean state = Toolkit.getDefaultToolkit().
                    getLockingKeyState(nextKey);
                try {
                    tester.actionKeyStroke(nextKey);
                    
                    ComponentTestFixture.assertEqualsEventually("Reported state of locking key '" + KEYS[i]
                                 + "' did not change",
                                 !state, new Callable<Boolean>() {

                            @Override
                            public Boolean call() throws Exception {
                                return Toolkit.getDefaultToolkit().
                                    getLockingKeyState(nextKey);
                            }
                        });

                }
                finally {
                    tester.actionKeyStroke(nextKey);
                }
            }
            catch(UnsupportedOperationException e) {
                // ignore
            }
            
        }
    }

    public void testFileDialogMisreportsBounds() throws Exception {
        if (!SHOW_BUGS && Bugs.fileDialogMisreportsBounds())
            return;
        JLabel label = new JLabel(getName());
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        Frame f = showFrame(label, size);
        getRobot().move(f, 0, 0);
        getRobot().waitForIdle();
        FileDialog d = new FileDialog(f, getName(), FileDialog.LOAD);
        class Flag { volatile boolean flag; }
        final Flag flag = new Flag();
        FocusListener listener = new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                flag.flag = true;
            }
        };
        size = new Dimension(400, 400);
        showWindow(d, size, false);
        getRobot().move(d, 100, 100);
        getRobot().waitForIdle();

        robot.delay(500);
        getRobot().waitForIdle();
        Point loc = d.getLocation();
        Dimension dsize = d.getSize();
        Rectangle actual =
            new Rectangle(loc.x, loc.y, dsize.width, dsize.height);
        assertEquals("FileDialog reports wrong location or size",
                     new Rectangle(100, 100, 400, 400), actual);

        // Click inside the dialog; if the click misses, the frame will get
        // focus. 
        f.addFocusListener(listener);
        robot.mouseMove(loc.x + 50, loc.y + 50);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(500);
        robot.waitForIdle();
        // Note that no FOCUS_LOST or WINDOW_ACTIVATED is posted on the
        // FileDialog. 
        assertFalse("Mouse click should not go to frame", flag.flag);
    }

    public void testFileDialogRequiresDismiss() throws Exception {
        if (!SHOW_BUGS && Bugs.fileDialogRequiresDismiss())
            return;

        JLabel label = new JLabel(getName());
        Frame f = showFrame(label);

        FileDialog d = new FileDialog(f, getName(), FileDialog.LOAD);
        showWindow(d, null, false);
        // FIXME what was this for?  It sometimes causes the frame to be moved
        // on linux
        /*
        getRobot().move(f, (size.width - f.getWidth())/2,
                        (size.height - f.getHeight())/2);
        */
        getRobot().waitForIdle();
        d.dispose();
        assertFalse("Dispose should hide Dialog", d.isShowing());

        // Try to click on the main frame; if the dialog is still showing,
        // it will swallow the event
        MouseWatcher mw = new MouseWatcher();
        label.addMouseListener(mw);

        // Ensure dialog has time to go away
        getRobot().delay(500);
        getRobot().click(label);
        getRobot().waitForIdle();
        assertTrue("File Dialog is still in front of main frame", mw.gotPress);
    }

    public void testHasKeyInputDelay() {
        if (!SHOW_BUGS && Bugs.hasKeyInputDelay())
            return;

        //fail("No automated test yet");
    }
    
    public void testNoAWTInputOnTextField() {
        if (!SHOW_BUGS && Bugs.hasNoAWTInputOnTextFieldBug())
            return;
        
        // X11 fails to generate TextField input in AWT mode (fixed in 1.5).
        // Use AWT_TOOLKIT=XToolkit to fix (post-1.5)
        // So does OSX (1.6)
        TextField tf = new TextField();
        tf.setColumns(40);
        showFrame(tf);
        int old = Robot.getEventMode();
        Robot.setEventMode(Robot.EM_AWT);
        try {
            ComponentTester tester = new ComponentTester();
            final String TEXT = "The quick brown fox";
            tester.actionKeyString(tf, TEXT);
            assertEquals("AWT mode did not generate text in TextField", TEXT, tf.getText());
        }
        finally {
            Robot.setEventMode(old);
        }
    }

    // Make this last, 'cuz it nukes the robot
    public void testLocksUpOnScreenCapture() throws Exception {
        if (!SHOW_BUGS && Bugs.locksUpOnScreenCapture())
            return;
        
        // this happens in ComponentTestFixture.fixtureSetup, but
        // make it explicit for the purposes of illustrating the failure
        final java.awt.Robot robot = new java.awt.Robot();
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        
        final Image[] ref = { null };
        Thread t = new Thread("Screen capture") {
            public void run() {
                Rectangle rect = new Rectangle(0, 0, 10, 10);
                ref[0] = robot.createScreenCapture(rect);
            }
        };
        t.start();
        t.join(5000);
        assertNotNull("Screen capture is hung", ref[0]);
    }

    private class MouseWatcher extends MouseAdapter {
        public volatile boolean gotPress = false;
        public volatile boolean gotRelease = false;
        public volatile boolean gotClick = false;
        public volatile int clickCount = 0;
        public volatile boolean popupTrigger = false;
        public volatile Component source = null;
        public volatile int modifiers = 0;
        public volatile Point where = null;
        public void mousePressed(MouseEvent me) {
            gotPress = true;
            source = me.getComponent();
            popupTrigger = me.isPopupTrigger();
            modifiers = me.getModifiers();
            where = me.getPoint();
        }
        public void mouseReleased(MouseEvent me) {
            gotRelease = true;
            popupTrigger = popupTrigger || me.isPopupTrigger();
        }
        public void mouseClicked(MouseEvent me) {
            gotClick = true;
            clickCount = me.getClickCount();
        }
    }

    private class KeyWatcher extends KeyAdapter {
        public volatile int code = KeyEvent.VK_UNDEFINED;
        public volatile int modifiers = 0;
        public volatile boolean gotPress = false;
        public volatile boolean gotRelease = false;
        public void keyPressed(KeyEvent ke) {
            gotPress = true;
            code = ke.getKeyCode();
            modifiers = ke.getModifiers();
        }
        public void keyReleased(KeyEvent ke) {
            gotRelease = true;
        }
    }

    public static void main(String[] args) {
        if (!SHOW_BUGS) {
            System.out.println("Use -Dabbot.test.show_bugs=true to run tests "
                               + "for known bugs on "
                               + System.getProperty("os.name") + ".");
            System.out.println("By default, such tests are skipped.");
        }
        RepeatHelper.runTests(args, BugsTest.class);
    }
}
