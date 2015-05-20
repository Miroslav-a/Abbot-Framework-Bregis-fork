package abbot.tester;

import java.net.URL;

import javax.swing.event.MenuEvent;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.*;
import abbot.util.AWT;
import abbot.util.WeakAWTEventListener;
import abbot.util.Bugs;

import javax.xml.ws.Holder;

/** Unit test to verify Robot operation. */

public class RobotTest extends ComponentTestFixture {

    class Flag { volatile boolean flag; }

    public void testWaitForIdle() {
        final Flag flag = new Flag();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try { Thread.sleep(1000); }
                catch(InterruptedException e) { }
                flag.flag = true;
            }
        });
        getRobot().waitForIdle();
        assertTrue("Did not wait for event dispatch to finish", flag.flag);
    }

    /**
     * Verify bug fix 3476008 for JDK 7
     */
    public void testWaitForIdleWithNestedEventQueue() {
        
        class PoppyQueue extends EventQueue {
           public void pop() {
               super.pop();
           }
        }
        
        final PoppyQueue queue = new PoppyQueue();
        final Holder<JFrame> hello = new Holder<JFrame>();
        
        try
        {
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    hello.value = new JFrame("Hello");
                    hello.value.setSize(200,200);
                    Toolkit.getDefaultToolkit().getSystemEventQueue().push(queue);
                    hello.value.setVisible(true);      
                }
            });
            
            assertTrue("First wait for idle should return true", getRobot().waitForIdle());
            assertTrue("Second wait for idle shoudl also return true, but used to fail under JDK7", getRobot().waitForIdle());

        }
        finally {
            queue.pop();
            
            if (hello.value!=null)
            {
                hello.value.setVisible(false);
                hello.value.dispose();
            }
        }
    }


    private volatile boolean pump;
    private volatile boolean waitStarted;
    private volatile boolean waitTerminated;
    private volatile boolean gotTimeout;
    // FIXME: on fast CPUs running linux (up through 1.5),
    // it has been found that waitForIdle will wait forever
    // when a dialog is shown.  need to reproduce that scenario
    // here to ensure we don't hang.
    public void xtestWaitForIdleTimeout() throws Exception {
        Robot robot = new Robot() {
            protected boolean isQueueBlocked(EventQueue q) {
                return false;
            }
            protected boolean postInvocationEvent(EventQueue q,
                                                  Toolkit toolkit,
                                                  long timeout) {
                if (super.postInvocationEvent(q, toolkit, timeout)) {
                    System.err.println("x");
                    pump = false;
                    gotTimeout = true;
                    return true;
                }
                return false;
            }
        };

        final Runnable EMPTY = new Runnable() {
            public void run() {
                if (pump) {
                    System.err.print(".");
                    SwingUtilities.invokeLater(this);
                }
            }
        };
        JFrame frame = new JFrame(getName());
        showWindow(frame);

        pump = true;
        // Put wait for idle on a different thread in case it blocks
        new Thread("wait for idle") {
            public void run() {
                waitStarted = true;
                System.err.println("start pump");
                try {
                    SwingUtilities.invokeAndWait(EMPTY);
                }
                catch(Throwable t) { }
                System.err.println("waitForIdle");
                getRobot().waitForIdle();
                waitTerminated = true;
            }
        }.start();

        while (!waitStarted) {
            Thread.sleep(100);
        }
        System.err.println("test thread wait");
        Timer timer = new Timer();
        while (!waitTerminated) {
            if (timer.elapsed() > 10000) {
                fail("Idle wait should not block unreasonably long");
            }
            System.err.print("+");
            Thread.sleep(10);
        }
        pump = false;
        assertTrue("Idle wait should time out", gotTimeout);
    }

    public void testEventPostDelay() throws Exception {
        if (Robot.getEventMode() != Robot.EM_ROBOT)
            return;
        JLabel label = new JLabel(getName());
        MouseWatcher ml = new MouseWatcher();
        label.addMouseListener(ml);
        showFrame(label);
        java.awt.Robot r = Robot.getRobot();
        Point pt = label.getLocationOnScreen();
        r.mouseMove(pt.x + label.getWidth()/2,
                    pt.y + label.getHeight()/2);
        r.mousePress(MouseEvent.BUTTON1_MASK);
        r.mouseRelease(MouseEvent.BUTTON1_MASK);
        r.delay(Robot.getEventPostDelay());
        long now = System.currentTimeMillis();
        robot.waitForIdle();
        if (!ml.gotPress) {
            Timer timer = new Timer();
            while (!ml.gotPress) {
                robot.delay(5);
                if (timer.elapsed() > 5000)
                    fail("Mouse press never registered");
            }
            long arrived = System.currentTimeMillis();
            fail("MOUSE_PRESSED event not yet generated, "
                 + "after " + Robot.getEventPostDelay()
                 + "ms, actual additional delay was " 
                 + (arrived - now) + "ms");
        }
    }

    /** Ensure image capture gets the right image. */
    public void testImageCapture() throws Throwable {
        // Don't need to test this under AWT mode, where we might get a false
        // negative anyway
        if (Robot.getEventMode() == Robot.EM_AWT)
            return;

        final int X = 100;
        final int Y = 100;
        URL gif = RobotTest.class.getResource("image.png");
        ImageIcon icon = new ImageIcon(gif);
        
        JLabel label1 = new JLabel(icon);
        JLabel label2 = new JLabel(icon);
        final JFrame frame = new JFrame(getName());
        frame.setLocation(X, Y);
        JPanel pane = (JPanel)frame.getContentPane();
        pane.setLayout(new BoxLayout(pane, BoxLayout.X_AXIS));
        //Ensure OSX growbox doesn't obscure the picture
        pane.setBorder(new EmptyBorder(20,20,20,20));
        pane.add(label1);
        pane.add(label2);
        frame.pack();
        frame.setResizable(false);
        frame.setLocation(X, Y);
        showWindow(frame);
        robot.focus(frame, true);
        
        ImageComparator ic = new ImageComparator();
        int status = icon.getImageLoadStatus();
        int DONE =
            MediaTracker.ERRORED|MediaTracker.ABORTED|MediaTracker.COMPLETE;
        Timer timer = new Timer();
        while ((status & DONE) == 0) {
            if (timer.elapsed() > 5000)
                fail("Icon image failed to load or error within 5s");
            robot.sleep();
            status = icon.getImageLoadStatus();
        }
        if ((status & DONE) != MediaTracker.COMPLETE) {
            fail("Icon image load failed: status=" + status);
        }
        robot.activate(frame);
        robot.waitForIdle();

        Image image = robot.capture(label1);
        assertEquals("Captured wrong image (1)", 0, ic.compare(image, gif));
        image = robot.capture(label2);
        assertEquals("Captured wrong image (2)", 0, ic.compare(image, gif));
    }

    private class MenuListener implements ActionListener {
        public int actionCount = 0;
        public boolean gotAction = false;
        public void actionPerformed(ActionEvent ev) {
            ++actionCount;
            gotAction = true;
        }
    }

    public void testSelectAWTMenuItem() {
        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File");
        menu.add(new MenuItem("One"));
        MenuItem mi = new MenuItem("Two");
        MenuListener ml1 = new MenuListener();
        MenuListener ml2 = new MenuListener();
        mi.addActionListener(ml1);
        menu.add(mi);
        Menu sub = new Menu("Submenu");
        sub.add(new MenuItem("Sub one"));
        MenuItem mi2 = new MenuItem("Sub two");
        mi2.addActionListener(ml2);
        sub.add(mi2);
        menu.add(sub);
        mb.add(menu);
        JFrame frame = new JFrame(getName());
        frame.setMenuBar(mb);
        // Ensure we have a proper size for linux
        showWindow(frame, new Dimension(200, 100));
        /*
        robot.selectAWTMenuItem(mi);
        robot.waitForIdle();
        assertTrue("AWT Menu item in menubar menu not hit", ml1.gotAction);
        robot.selectAWTMenuItem(mi2);
        robot.waitForIdle();
        assertTrue("AWT Menu item in menubar submenu not hit", ml2.gotAction);

        ml1.gotAction = ml2.gotAction = false;
        robot.selectAWTMenuItem(frame, mi.getLabel());
        robot.waitForIdle();
        assertTrue("AWT Menu item in menubar menu not hit", ml1.gotAction);
        robot.selectAWTMenuItem(frame, mi2.getLabel());
        robot.waitForIdle();
        assertTrue("AWT Menu item in menubar submenu not hit", ml2.gotAction);
        */
        ml1.gotAction = false;
        String PATH = "File|Two";
        robot.selectAWTMenuItem(frame, PATH);
        robot.waitForIdle();
        assertTrue("AWT Menu item identified by path '" + PATH + "' not hit",
                   ml1.gotAction);
    }

    public void testAWTPopupMenuSelection() {
        if (Robot.getEventMode() == Robot.EM_AWT
            && Bugs.showAWTPopupMenuBlocks()) {
            //fail("This test would block");
            return;
        }

        PopupMenu popup = new PopupMenu();
        popup.add(new MenuItem("One"));
        MenuItem mi = new MenuItem("Two");
        MenuListener ml1 = new MenuListener();
        MenuListener ml2 = new MenuListener();
        mi.addActionListener(ml1);
        popup.add(mi);
        Menu sub = new Menu("Submenu");
        sub.add(new MenuItem("Sub one"));
        MenuItem mi2 = new MenuItem("Sub two");
        mi2.addActionListener(ml2);
        sub.add(mi2);
        popup.add(sub);
        JFrame frame = new JFrame(getName());
        frame.add(popup);
        showWindow(frame, new Dimension(200, 200));

        robot.selectAWTPopupMenuItem(mi);
        robot.waitForIdle();
        assertTrue("AWT PopupMenu item not hit", ml1.gotAction);
        robot.selectAWTPopupMenuItem(mi2);
        robot.waitForIdle();
        assertTrue("AWT PopupMenu item in submenu not hit", ml2.gotAction);

        ml1.gotAction = ml2.gotAction = false;
        robot.selectAWTPopupMenuItem(frame, mi.getLabel());
        robot.waitForIdle();
        assertTrue("AWT PopupMenu item not hit", ml1.gotAction);
        robot.selectAWTPopupMenuItem(frame, mi2.getLabel());
        robot.waitForIdle();
        assertTrue("AWT PopupMenu item in submenu not hit", ml2.gotAction);
    }

    public void testSelectMenuItem() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem mi0 = new JMenuItem("One");
        MenuListener ml0 = new MenuListener();
        mi0.addActionListener(ml0);
        menu.add(mi0);
        JMenuItem mi1 = new JMenuItem("Two");
        MenuListener ml1 = new MenuListener();
        mi1.addActionListener(ml1);
        menu.add(mi1);
        JMenu sub = new JMenu("Submenu");
        sub.add(new JMenuItem("Sub one"));
        JMenuItem mi2 = new JMenuItem("Sub two");
        MenuListener ml2 = new MenuListener();
        mi2.addActionListener(ml2);
        sub.add(mi2);
        menu.add(sub);
        mb.add(menu);
        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        robot.selectMenuItem(mi0);
        robot.waitForIdle();
        assertTrue("Standard menu item 0 not selected", ml0.gotAction);

        robot.selectMenuItem(mi1);
        robot.waitForIdle();
        assertTrue("Standard menu item 1 not selected", ml1.gotAction);
    }

    public void testSelectMenuItemByPath() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");

        JMenuItem mi1 = new JMenuItem("Two");
        JMenu sub = new JMenu("Submenu");
        JMenuItem mi2 = new JMenuItem("File");
        MenuListener ml = new MenuListener();
        mi2.addActionListener(ml);

        mb.add(menu);
        menu.add(new JMenuItem("One"));
        menu.add(sub);
        sub.add(new JMenuItem("Sub one"));
        sub.add(mi2);

        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        String path =
            menu.getText() + "|" + sub.getText() + "|" + mi2.getText();
        robot.selectMenuItem(frame, path);
        assertTrue("Select menu item by path failed", ml.gotAction);
    }
    
    public void testSelectMenuItemByPathLazyLoad() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");

        JMenuItem mi1 = new JMenuItem("Two");
        final JMenu sub = new JMenu("Submenu");
        final JMenuItem mi2 = new JMenuItem("File");
        MenuListener ml = new MenuListener();
        mi2.addActionListener(ml);

        mb.add(menu);
        menu.add(new JMenuItem("One"));
        menu.add(sub);
        
        sub.addMenuListener(new javax.swing.event.MenuListener() {                                                                     
            public void menuCanceled(MenuEvent e) {
            }
            
            public void menuDeselected(MenuEvent e) {
            }
            
            public void menuSelected(MenuEvent e) {
                sub.add(new JMenuItem("Sub one"));
                sub.add(mi2);
            }
        });
        

        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        String path =
            menu.getText() + "|" + sub.getText() + "|" + mi2.getText();
        robot.selectMenuItem(frame, path);
        assertTrue("Select menu item by path failed", ml.gotAction);
    }    

    public void testSelectPopupMenuItemByPathLazyLoad() {
        final JPopupMenu popup = new JPopupMenu();
        JMenu menu = new JMenu("File");

        JMenuItem mi1 = new JMenuItem("Two");
        final JMenu sub = new JMenu("Submenu");
        final JMenuItem mi2 = new JMenuItem("File");
        MenuListener ml = new MenuListener();
        mi2.addActionListener(ml);

        popup.add(menu);
        menu.add(new JMenuItem("One"));
        menu.add(sub);
        
        sub.addMenuListener(new javax.swing.event.MenuListener() {                                                                     
            public void menuCanceled(MenuEvent e) {
            }
            
            public void menuDeselected(MenuEvent e) {
            }
            
            public void menuSelected(MenuEvent e) {
                sub.add(new JMenuItem("Sub one"));
                sub.add(mi2);
            }
        });
        

        JFrame frame = new JFrame(getName());
        JLabel component = new JLabel(getName());
        component.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) { test(e);}
            public void mousePressed(MouseEvent e) { test(e);}
            public void mouseReleased(MouseEvent e) { test(e);}
            
            private void test(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(
                        (Component)e.getSource(),
                        e.getX(),
                        e.getY());
                }
            }
            
        });
        frame.getContentPane().add(component);
        showWindow(frame);
        
        

        String path =
            menu.getText() + "|" + sub.getText() + "|" + mi2.getText();
        robot.selectPopupMenuItem(component,
                                  new ComponentLocation(new Point(10,10)), path);
        assertTrue("Select menu item by path failed", ml.gotAction);
    }    


    public void testSelectMenuItemLongItemTraversal() {
        // The following should not cause problems:
        // ------------
        // |menu|menu1|
        // ------------------------
        // |really long menu item |
        // ------------------------
        JMenuBar mb = new JMenuBar();
        JMenu menu1 = new JMenu("A");
        JMenuItem mi = new JMenuItem("This is a really long menu item such that the center of the item is way out there");
        menu1.add(mi);
        JMenu menu2 = new JMenu("B");
        menu2.add(new JMenuItem("About"));
        mb.add(menu1);
        mb.add(menu2);
        MenuListener ml = new MenuListener();
        mi.addActionListener(ml);
        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        robot.selectMenuItem(mi);
        assertTrue("Long menu item missed", ml.gotAction);
    }

    public void testSelectSubmenuItem() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.add(new JMenuItem("One"));
        JMenuItem mi1 = new JMenuItem("Two");
        MenuListener ml1 = new MenuListener();
        mi1.addActionListener(ml1);
        menu.add(mi1);
        JMenu sub = new JMenu("Submenu");
        sub.add(new JMenuItem("Sub one"));
        JMenuItem mi2 = new JMenuItem("Sub two");
        MenuListener ml2 = new MenuListener();
        mi2.addActionListener(ml2);
        sub.add(mi2);
        menu.add(sub);
        mb.add(menu);
        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        robot.selectMenuItem(mi2);
        assertTrue("Submenu item not selected", ml2.gotAction);
    }

    public void testMenuSelectionWithParentShowing() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.add(new JMenuItem("One"));
        JMenuItem mi1 = new JMenuItem("Two");
        MenuListener ml1 = new MenuListener();
        mi1.addActionListener(ml1);
        menu.add(mi1);
        mb.add(menu);

        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(new JLabel(getName()));
        frame.setJMenuBar(mb);
        showWindow(frame);

        robot.click(menu);
        robot.waitForIdle();
        robot.selectMenuItem(mi1);
        robot.waitForIdle();
        assertTrue("Menu it main menu not hit when parent already open",
                   ml1.gotAction);
    }

    public void testSelectNestedMenuItem() {
        // Repeatedly select a nested menu item
        JMenuBar mb = new JMenuBar();
        mb.add(new JMenu("Action Management"));
        JMenu menu = new JMenu("Risk Management");
        mb.add(menu);
        mb.add(new JMenu("BPComs"));
        mb.add(new JMenu("Tools"));
        mb.add(new JMenu("Actions"));
        mb.add(new JMenu("Window"));
        mb.add(new JMenu("Help"));
        JMenu sub1 = new JMenu("Position Enquiry");
        menu.add(sub1);
        menu.add(new JMenu("Static Data Management"));
        menu.add(new JMenu("Excession Management"));
        sub1.add(new JMenu("Cash Account"));
        JMenu sub2 = new JMenu("Safekeeping Account");
        sub1.add(sub2);
        sub1.add(new JMenu("Client"));
        sub1.add(new JMenu("RCA"));
        JMenuItem mi = new JMenuItem("Search...");
        sub2.add(mi);
        MenuListener ml = new MenuListener();
        mi.addActionListener(ml);
        JFrame frame = new JFrame(getName());
        frame.setJMenuBar(mb);
        showWindow(frame);

        int REPEAT = 5;
        for (int i=0;i < REPEAT;i++) { 
            robot.selectMenuItem(mi);
            robot.waitForIdle();
        }
        assertEquals("Some menu activations failed", REPEAT, ml.actionCount);
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

    /** Verify proper click operation.  You may need to set
        Robot.mouseReleaseDelay if this fails intermittently. */
    public void testClick() {
        MouseWatcher mw = new MouseWatcher();
        JFrame frame = new JFrame(getName());
        frame.addMouseListener(mw);
        showWindow(frame, new Dimension(200, 200), true);
        robot.click(frame);
        robot.waitForIdle();
        assertTrue("Never received press", mw.gotPress);
        assertEquals("Wrong event source", frame, mw.source);
        assertTrue("Never received release", mw.gotRelease);
        assertTrue("Never received click", mw.gotClick);
        assertEquals("No modifiers expected", 
                     AWT.getMouseModifiers(MouseEvent.BUTTON1_MASK), 
                     AWT.getMouseModifiers(mw.modifiers));
        assertTrue("Unexpected popup trigger", !mw.popupTrigger);
    }

    /** Double clicks. */
    public void testMultipleClicks() {
        MouseWatcher mw = new MouseWatcher();
        JFrame frame = new JFrame(getName());
        showWindow(frame, new Dimension(200, 200));
        JPanel pane = (JPanel)frame.getContentPane();
        pane.addMouseListener(mw);
        robot.click(pane, 
                    pane.getWidth()/2,
                    pane.getHeight()/2,
                    InputEvent.BUTTON1_MASK, 2);
        Timer timer = new Timer();
        while (mw.clickCount < 2) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never received a double click");
            robot.sleep();
        }
    }

    public void testClickAt() {
        MouseWatcher mw = new MouseWatcher();
        JFrame frame = new JFrame(getName());
        frame.addMouseListener(mw);
        showWindow(frame, new Dimension(200, 200));
        // Make sure we don't click in the title or border...
        Insets insets = frame.getInsets();
        Point at = new Point(insets.left,insets.top);
        robot.click(frame, at.x, at.y);
        robot.waitForIdle();
        assertTrue("Never received press", mw.gotPress);
        assertEquals("Wrong event source", frame, mw.source);
        assertTrue("Never received release", mw.gotRelease);
        assertTrue("Never received click", mw.gotClick);
        assertEquals("Wrong mouse coordinates", at, mw.where);
        assertEquals("No modifiers expected", 
                     AWT.getMouseModifiers(MouseEvent.BUTTON1_MASK), 
                     AWT.getMouseModifiers(mw.modifiers));
        assertTrue("Unexpected popup trigger", !mw.popupTrigger);
    }

    public void testClickPopup() {
        MouseWatcher mw = new MouseWatcher();
        JFrame frame = new JFrame(getName());
        frame.addMouseListener(mw);
        showWindow(frame, new Dimension(200, 200));
        robot.click(frame, AWTConstants.POPUP_MASK);
        robot.waitForIdle();
        assertTrue("Never received press", mw.gotPress);
        assertEquals("Wrong event source", frame, mw.source);
        assertTrue("Never received release", mw.gotRelease);
        assertTrue("Never received click", mw.gotClick);
        assertEquals("Wrong modifiers", 
                     AWT.getMouseModifiers(AWTConstants.POPUP_MASK), 
                     AWT.getMouseModifiers(mw.modifiers));
        assertTrue("Should be popup trigger", mw.popupTrigger);
    }

    public void testClickTertiary() {
        MouseWatcher mw = new MouseWatcher();
        JFrame frame = new JFrame(getName());
        frame.addMouseListener(mw);
        showWindow(frame, new Dimension(200, 200));
        int mask = AWTConstants.TERTIARY_MASK;
        robot.click(frame, mask);
        robot.waitForIdle();
        // w32 uses CTRL as a modifier prior to 1.4, I guess in case you don't
        // have a 3-button mouse.
        if (Platform.isWindows()
            && Platform.JAVA_VERSION < Platform.JAVA_1_4) {
            mask |= InputEvent.CTRL_MASK;
        }

        assertTrue("Never received press", mw.gotPress);
        assertEquals("Wrong event source", frame, mw.source);
        assertTrue("Never received release", mw.gotRelease);
        assertTrue("Never received click", mw.gotClick);
        assertEquals("Wrong modifiers", 
                     AWT.getMouseModifiers(mask),
                     AWT.getMouseModifiers(mw.modifiers));
        assertTrue("Unexpected popup trigger", !mw.popupTrigger);
    }

    public void testFocusSingleFrame() {
        JTextField tf = new JTextField("Default focus goes here");
        JTextField tf2 = new JTextField("Requested focus goes here");
        JPanel pane = new JPanel();
        pane.add(tf);
        pane.add(tf2);
        showFrame(pane);
        robot.focus(tf2, true);
        assertTrue("Default TextField should not have focus", !tf.hasFocus());
        assertTrue("Next TextField should have focus", tf2.hasFocus());
        robot.focus(tf, true);
        assertTrue("First TextField should have focus", tf.hasFocus());
    }

    // FIXME sporadic failures on linux (AWT mode)
    public void testFocusMultipleFrames() {
        JTextField tf = new JTextField("Default focus goes here");
        JTextField tf2 = new JTextField("Requested focus goes here");
        JTextField tf3 = new JTextField("Next it goes here");
        JPanel pane = new JPanel();
        pane.add(tf);
        pane.add(tf2);
        showFrame(pane);
        JFrame frame = new JFrame(getName() + " 2");
        frame.getContentPane().add(tf3);
        showWindow(frame);
        robot.focus(tf2, true);
        assertTrue("Second text field didn't get focus", tf2.hasFocus());
        robot.focus(tf3, true);
        assertTrue("Third text field didn't get focus", tf3.hasFocus());
    }

    private class KeyWatcher extends KeyAdapter {
        public volatile boolean gotPress = false;
        public volatile boolean gotRelease = false;
        public volatile boolean gotTyped = false;
        public volatile int keyCode = KeyEvent.VK_UNDEFINED;
        public volatile int modifiers = 0;
        public void keyPressed(KeyEvent ke) {
            gotPress = true;
            keyCode = ke.getKeyCode();
            modifiers = ke.getModifiers();
        }
        public void keyReleased(KeyEvent ke) {
            gotRelease = true;
        }
        public void keyTyped(KeyEvent ke) {
            gotTyped = true;
        }
    }

    public void testPlainKey() {
        KeyWatcher kw = new KeyWatcher();
        JTextField tf = new JTextField();
        tf.setColumns(10);
        tf.addKeyListener(kw);
        JPanel pane = new JPanel();
        pane.add(tf);
        showFrame(pane);
        robot.focus(tf, true);
        int code = KeyEvent.VK_A;
        robot.key(code);
        robot.waitForIdle();
        assertTrue("Never received key press", kw.gotPress);
        assertTrue("Never received release", kw.gotRelease);
        assertTrue("Never received type", kw.gotTyped);
        assertEquals("Wrong key code", code, kw.keyCode);
        assertEquals("Wrong modifiers", 0, kw.modifiers);
    }

    public void testKey() {
        JTextField tf1 = new JTextField();
        KeyWatcher kw = new KeyWatcher();
        tf1.addKeyListener(kw);
        showFrame(tf1);
        robot.focus(tf1, true);
        robot.key(KeyEvent.VK_A);
        robot.waitForIdle();
        assertTrue("Never received key press", kw.gotPress);
        assertEquals("Wrong key code", KeyEvent.VK_A, kw.keyCode);
        assertEquals("Wrong modifiers", 0, kw.modifiers);

        kw.gotPress = false;
        robot.key(KeyEvent.VK_A, KeyEvent.SHIFT_MASK);
        robot.waitForIdle();
        assertTrue("Never received key press", kw.gotPress);
        assertEquals("Wrong key code", KeyEvent.VK_A, kw.keyCode);
        assertEquals("Wrong modifiers", KeyEvent.SHIFT_MASK, kw.modifiers);
    }

    /** Generate some things which we know we don't have key mappings for. */
    public void testKeyString() {
        JPanel pane = new JPanel();
        JTextField tf1 = new JTextField();
        JTextField tf2 = new JTextField();
        tf1.setColumns(10);
        tf2.setColumns(10);
        pane.add(tf1);
        pane.add(tf2);
        Frame frame = showFrame(pane);
        robot.activate(frame);
        robot.waitForIdle();
        robot.focus(tf1, true);
        String keymap = "The quick brown fox jumped over the lazy dog"
            + "`1234567890-=~!@#$%^&*()_+[]\\{}|;':\",./<>?";
        robot.keyString(keymap);
        robot.waitForIdle();
        assertEquals("Wrong text typed", keymap, tf1.getText());

        // Throw in some Unicode; should work just as well for any other...
        String string = "Un \u00e9l\u00e9ment gr\u00e2ce \u00e0 l'index";
        robot.focus(tf2, true);
        robot.keyString(string);
        robot.waitForIdle();
        assertEquals("Wrong non-keymap text typed", string, tf2.getText());
    }

    public void testEndKey() {
        TextField tf = new TextField(getName());
        KeyWatcher watcher = new KeyWatcher();
        tf.addKeyListener(watcher);
        showFrame(tf);
        robot.focus(tf, true);
        robot.key(KeyEvent.VK_END);
        robot.waitForIdle();
        Timer timer = new Timer();
        while (!watcher.gotPress) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never got key press");
            robot.sleep();
        }
        assertEquals("Wrong keycode was generated",
                     KeyEvent.VK_END, watcher.keyCode);
        while (!watcher.gotRelease) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Never got key release");
            robot.sleep();
        }
    }

    public void testFindFocusOwner() {
        JPanel pane = new JPanel();
        JTextField tf1 = new JTextField("tf 1");
        tf1.setName("tf 1");
        JTextField tf2 = new JTextField("tf 2");
        tf2.setName("tf 2");
        pane.add(tf1);
        pane.add(tf2);
        showFrame(pane);
        robot.focus(tf2, true);
        assertEquals("Wrong focus detected",
                     Robot.toString(tf2),
                     Robot.toString(robot.findFocusOwner()));
    }

    public void testIconify() throws Throwable {
        Frame frame = showFrame(new JLabel(getName()));
        robot.iconify(frame);
        // Give the WM time to put the window away
        Timer timer = new Timer();
        while (frame.getState() != Frame.ICONIFIED) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Frame not iconified, state " + frame.getState());
            robot.sleep();
        }
        robot.deiconify(frame);
        timer.reset();
        while (frame.getState() != Frame.NORMAL) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Frame not restored, state " + frame.getState());
            robot.sleep();
        }
    }

    public void testMaximize() {
        Frame frame = showFrame(new JLabel(getName()));
        Dimension size = frame.getSize();
        robot.maximize(frame);
        Timer timer = new Timer();
        while (frame.getSize().equals(size)) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Frame size not changed");
            robot.sleep();
        }
        robot.normalize(frame);
        // Don't bother testing normalize (it won't work on 1.3.1)
    }

    private class EnterExitListener implements AWTEventListener {
        public AWTEvent exited;
        public AWTEvent entered;
        public void eventDispatched(AWTEvent event) {
            if (event.getID() == MouseEvent.MOUSE_ENTERED) {
                entered = event;
            }
            else if (event.getID() == MouseEvent.MOUSE_EXITED) {
                exited = event;
            }
        }
    }

    public void testEnterExitGeneration() {
        JPanel panel = new JPanel();
        JLabel label1 = new JLabel("Source"); label1.setName("label 1");
        JLabel label2 = new JLabel("Destination"); label2.setName("label 2");

        panel.add(label1);
        panel.add(label2);
        Frame f = showFrame(panel, new Dimension(200, 200));
        f.setName(getName());
        // Ensure we have a state to start with
        robot.mouseMove(label1);
        robot.waitForIdle();
        // Add the listener *after* the mouse is within the first component
        EnterExitListener eel = new EnterExitListener();
        new WeakAWTEventListener(eel, MouseEvent.MOUSE_EVENT_MASK);
        robot.mouseMove(label2);
        robot.waitForIdle();

        assertEquals("Expect no exit event w/o motion listeners",
                     null, eel.exited);
        assertEquals("Expect no enter event w/o motion listeners",
                     null, eel.entered);

        MouseListener ml = new MouseAdapter() { };
        label1.addMouseListener(ml);
        label2.addMouseListener(ml);

        robot.mouseMove(label1);
        robot.waitForIdle();

        assertEquals("Expect no exit event (last component was frame)",
                     null, eel.exited);
        assertEquals("Enter event was expected",
                     label1, eel.entered.getSource());

        eel.entered = eel.exited = null;

        robot.mouseMove(label2);
        robot.waitForIdle();
        assertEquals("Exit event was expected", 
                     label1, eel.exited.getSource());
        assertEquals("Enter event was expected",
                     label2, eel.entered.getSource());
    }

    public void testGetModifiers() {
        Object[][] modifiers = {
            { "ALT", new Integer(InputEvent.ALT_MASK) },
            { "ALT_GRAPH", new Integer(InputEvent.ALT_GRAPH_MASK) },
            { "SHIFT", new Integer(InputEvent.SHIFT_MASK) },
            { "CTRL", new Integer(InputEvent.CTRL_MASK) },
            { "META", new Integer(InputEvent.META_MASK) },
            { "BUTTON1", new Integer(InputEvent.BUTTON1_MASK) },
            { "BUTTON2", new Integer(InputEvent.BUTTON2_MASK) },
            { "BUTTON3", new Integer(InputEvent.BUTTON3_MASK) },
            { "POPUP", new Integer(AWTConstants.POPUP_MASK) },
            { "TERTIARY", new Integer(AWTConstants.TERTIARY_MASK) },
            { "ALT|CTRL|SHIFT", new Integer(InputEvent.ALT_MASK
                                            |InputEvent.CTRL_MASK
                                            |InputEvent.SHIFT_MASK) },
        };
        for (int i=0;i < modifiers.length;i++) {
            String mod = (String)modifiers[i][0];
            assertEquals("Wrong value for string modifier " + mod, 
                         modifiers[i][1],
                         new Integer(AWT.getModifiers(mod)));
        }
        for (int i=0;i < modifiers.length;i++) {
            String mod = modifiers[i][0].toString() + "_MASK";
            assertEquals("Wrong value for string modifier " + mod,
                         modifiers[i][1],
                         new Integer(AWT.getModifiers(mod)));
        }
    }

    public void testMouselessModifierMask() {
        final JButton b = new JButton("Push Me");
        b.setMnemonic(KeyEvent.VK_P);
        showFrame(b);
        final Flag flag = new Flag();
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flag.flag = true;
            }
        });
        invokeAndWait(new Runnable() { public void run() {
            b.requestFocus();
        }});
        robot.key(KeyEvent.VK_P, Robot.MOUSELESS_MODIFIER_MASK);
        robot.waitForIdle();
        assertTrue("Button not activated, probably incorrect mouseless modifier, or not supported",
                   flag.flag);
    }

    // FIXME X11 hangs here when run with all tests on a non-primary display
    // 6 errors on non-primary display, 3 on primary display
    public void testSampleComponentLocation() {
        // Sampling requires java.awt.Robot
        if (Robot.getEventMode() != Robot.EM_ROBOT)
            return;

        if (!Platform.isX11()) {
            JLabel label = new JLabel("       ");
            label.setBackground(Color.red);
            label.setOpaque(true);
            showFrame(label);
            assertEquals("Sample of component location failed",
                         Color.red, robot.sample(label, label.getWidth()/2, 
                                                 label.getHeight()/2));
        }
    }

    /** Create a new test case with the given name. */
    public RobotTest(String name) { super(name); }
    /** Note the event mode when reporting this test's name. */
    public String getName() { 
        return Robot.getEventMode() == Robot.EM_AWT
            ? super.getName() + " (AWT mode)"
            : super.getName();
    }

    private Robot robot;
    protected void setUp() {
        robot = getRobot();
    }


//    /** Provide for repetitive testing on individual tests. */
//    public static void main(String[] args) {
//        RepeatHelper.runTests(args, RobotTest.class);
//    }
}
