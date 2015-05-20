package abbot.util;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import junit.extensions.abbot.*;
import junit.framework.AssertionFailedError;
import abbot.Log;
import abbot.Platform;
import abbot.tester.ComponentTester;
import abbot.tester.Robot;
import abbot.tester.WindowTester;

public class AWTTest extends ComponentTestFixture {

    private static boolean ONLY_HEAVYWEIGHT_POPUPS = Platform.isOSX();

    public void testDefaultNames() throws Throwable {
        Class[] classes = {
            Button.class,
            Canvas.class,
            Checkbox.class,
            Choice.class,
            Frame.class,
            List.class,
            Label.class,
            Panel.class,
            Scrollbar.class,
            TextArea.class,
            TextField.class,
        };
        for (int i=0;i < classes.length;i++) {
            Component c = (Component)classes[i].newInstance();
            assertTrue("Expected default name on " + c,
                       AWT.hasDefaultName(c));
            c.setName("bobo");
            assertFalse("Expected no default name on " + c,
                        AWT.hasDefaultName(c));
        }
        Frame f = new Frame(getName());
        Component[] comps = {
            new Dialog(f),
            new Window(f),
            new FileDialog(f),
        };
        for (int i=0;i < comps.length;i++) {
            assertTrue("Expected default name on " + comps[i],
                       AWT.hasDefaultName(comps[i]));
            comps[i].setName("bobo");
            assertTrue("Expected no default name on " + comps[i],
                       !AWT.hasDefaultName(comps[i]));
        }
        JFrame jf = new JFrame(getName());
        JRootPane rp = (JRootPane)jf.getComponent(0);
        assertEquals("Unexpected root pane name", null, rp.getName());
        Component c = rp.getComponent(0);
        assertTrue("Expected default name on glass pane: " + c.getName(),
                   AWT.hasDefaultName(c));
        c = rp.getComponent(1);
        assertTrue("Expected default name on layered pane: " + c.getName(),
                   AWT.hasDefaultName(c));
        c = jf.getContentPane();
        assertTrue("Expected default name on content pane: " + c.getName(),
                   AWT.hasDefaultName(c));
    }

    public void testLightweightTransientPopups() {
        Frame frame = showFrame(new JLabel(getName()),
                                new Dimension(200, 200));
        JPanel pane = (JPanel)((JFrame)frame).getContentPane();
        JPopupMenu popup = new JPopupMenu("menu");
        popup.add(new JMenuItem("item 1"));
        popup.add(new JMenuItem("item 2"));
        showPopup(popup, pane, 1, 1);
        Component container = popup.getParent();
        Log.debug("Popup parent is " + Robot.toString(container)
                  + " (" + container.getClass() + ")");
        while (!AWT.isTransientPopup(container)) {
            if (container.getParent() == null)
                throw new AssertionFailedError("No transient popup ancestor to "
                                               + Robot.toString(popup)
                                               + " (ancestor " 
                                               + Robot.toString(container)
                                               + ")");
            container = container.getParent();
            Log.debug("Next ancestor is " + Robot.toString(container));
        }
        assertTrue("No transient popup ancestor detected for "
                   + Robot.toString(container),
                   AWT.isTransientPopup(container));
        // This depends on the platform
        if (ONLY_HEAVYWEIGHT_POPUPS) {
            assertTrue("Root expected to be heavyweight",
                       AWT.isHeavyweightPopup(container));
        }
        else {
            assertTrue("Root should be lightweight",
                       AWT.isLightweightPopup(container));
        }
    }
        
    public void testHeavyweightTransientPopups() {
        Frame frame = showFrame(new JLabel(getName()),
                                new Dimension(200, 200));
        JPanel pane = (JPanel)((JFrame)frame).getContentPane();
        JPopupMenu popup = new JPopupMenu("menu");
        popup.add(new JMenuItem("item 1"));
        popup.add(new JMenuItem("item 2"));
        popup.add(new JMenuItem("item 3"));
        popup.add(new JMenuItem("item 4"));
        popup.add(new JMenuItem("item 5"));
        popup.add(new JMenuItem("item 6"));
        showPopup(popup, pane, 1, pane.getHeight()-5);

        Component container = popup.getParent();
        Log.debug("Popup parent is " + Robot.toString(container)
                  + " (" + container.getClass() + ")");
        while (!AWT.isTransientPopup(container)) {
            if (container.getParent() == null)
                throw new AssertionFailedError("No transient popup ancestor to "
                                               + Robot.toString(popup)
                                               + " (ancestor " 
                                               + Robot.toString(container)
                                               + ")");
            container = container.getParent();
            Log.debug("Next ancestor is " + Robot.toString(container));
        }
        assertTrue("No transient popup ancestor detected for "
                   + Robot.toString(container),
                   AWT.isTransientPopup(container));
        assertTrue("Container should be a window",
                   container instanceof Window);
        Window w = (Window)container.getParent();
        assertFalse("Ancestor " + Robot.toString(w)
                    + " of popup should not be transient",
                    AWT.isTransientPopup(w));
        assertTrue("Root should be heavyweight",
                   AWT.isHeavyweightPopup(container));
        assertFalse("Content pane should not think it's lightweight",
                    AWT.isLightweightPopup(((JWindow)container).getContentPane()));
    }

    public void testLightweightPopup() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem("item"));
        JLabel label = new JLabel(getName());
        showFrame(label, new Dimension(200, 200));
        showPopup(menu, label);
        if (ONLY_HEAVYWEIGHT_POPUPS) {
            Window w = SwingUtilities.getWindowAncestor(menu);
            assertTrue("Popup menu expected on a heavyweight popup: "
                       + w, AWT.isHeavyweightPopup(w));
        }
        else {
            assertTrue("Popup menu parent should be detected as lightweight: "
                       + menu.getParent(),
                       AWT.isLightweightPopup(menu.getParent()));
        }
    }

    public void testIsContentPane() {
        JFrame f = new JFrame(getName());
        f.getContentPane().add(new JLabel(getName()));
        JMenuBar mb = new JMenuBar();
        mb.add(new JMenu("file"));
        f.setJMenuBar(mb);
        showWindow(f);
        assertTrue("Content pane not detected",
                   AWT.isContentPane(f.getContentPane()));
        assertFalse("Layered pane is not the content pane",
                    AWT.isContentPane(f.getContentPane().getParent()));
        assertFalse("Menu bar is not the content pane",
                    AWT.isContentPane(mb));
    }

    private void checkChildren(Container c, boolean expected) {
        Component[] kids = c.getComponents();
        // All descendents of the root pane are not decorations
        if (c instanceof JRootPane)
            expected = false;
        for (int i=0;i < kids.length;i++) {
            if (kids[i] instanceof Container) {
                checkChildren((Container)kids[i], expected);
            }
        }
        if (expected && !(c instanceof JInternalFrame)) 
            assertTrue("Decoration not detected: " + c,
                       AWT.isInternalFrameDecoration(c));
        else
            assertFalse("Not a decoration: " + c,
                        AWT.isInternalFrameDecoration(c));
    }

    public void testInternalFrameDecorationCheck() {
        JInternalFrame frame = new JInternalFrame();
        JLabel label = new JLabel(getName());
        frame.getContentPane().add(label);
        checkChildren(frame, true);
    }

    public void testEventTypeEnabled() {
        JFrame frame = new JFrame(getName());
        Container c = frame.getContentPane();
        JLabel label = new JLabel(getName());
        c.add(label);
        assertFalse("JLabel should not be enabled w/o listeners",
                    AWT.eventTypeEnabled(label, MouseEvent.MOUSE_PRESSED));
        label.addMouseListener(new MouseAdapter() { });
        assertTrue("JLabel should be enabled w/listeners",
                   AWT.eventTypeEnabled(label, MouseEvent.MOUSE_PRESSED));
    }

    public void testDetectTransientDialogComponents() {
        JOptionPane pane = new JOptionPane("Dialog", JOptionPane.INFORMATION_MESSAGE);
        Dialog d = pane.createDialog(null, "Dialog");
        assertTrue("Dialog is a JOptionPane dialog",
                   AWT.isTransientDialog(d));
        assertTrue("Content pane of transient dialog should be transient",
                   AWT.isTransientDialog(((JDialog)d).getContentPane()));
        assertFalse("Option pane should not be transient",
                    AWT.isTransientDialog(pane));
    }

    public void testDetectNonTransientDialog() {
        Frame frame = showFrame(new JLabel(getName()));
        JDialog dialog = new JDialog(frame, "dialog");
        showWindow(dialog);
        assertTrue("Dialog is not transient", !AWT.isTransientDialog(dialog));
    }

    public void testInvokerAndWindow() {
        JFrame frame = new JFrame(getName());
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenu submenu = new JMenu("submenu");
        JMenuItem mi = new JMenuItem("Item");
        menu.add(mi);
        menu.add(submenu);
        assertEquals("Wrong invoker", menu, AWT.getInvoker(mi));
        assertEquals("Wrong invoker", menu, AWT.getInvoker(submenu));
        assertEquals("Wrong invoker", null, AWT.getInvoker(menu));
        assertEquals("Wrong window", null, AWT.getWindow(mi));
        assertEquals("Wrong window", null, AWT.getWindow(submenu));
        assertEquals("Wrong window", null, AWT.getWindow(menu));

        mb.add(menu);
        frame.setJMenuBar(mb);
        showWindow(frame);
        assertEquals("Wrong window", frame, AWT.getWindow(mi));
        assertEquals("Wrong window", frame, AWT.getWindow(submenu));
        assertEquals("Wrong window", frame, AWT.getWindow(menu));
    }

    public void testAWTMenuItemPath() {
        Frame frame = new Frame(getName());
        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File");
        Menu menu2 = new Menu("Edit");
        MenuItem dupMB1 = new MenuItem("Open");
        MenuItem dupMB2 = new MenuItem("Open");
        MenuItem uniqueMB = new MenuItem("Unique");
        menu.add(dupMB1);
        menu.add(uniqueMB);
        menu2.add(dupMB2);
        mb.add(menu);
        mb.add(menu2);
        frame.setMenuBar(mb);
        PopupMenu p1 = new PopupMenu();
        MenuItem dup = new MenuItem("Open");
        MenuItem uniquePopup = new MenuItem("Close");
        p1.add(dup);
        p1.add(uniquePopup);
        PopupMenu p2 = new PopupMenu();
        MenuItem dup2 = new MenuItem("Open");
        p2.add(dup2);
        frame.add(p1);
        frame.add(p2);

        assertFalse("Identical labels on different popups not unique: "
                    + AWT.getPath(dup),
                    AWT.getPath(dup).equals(AWT.getPath(dup2)));
        assertFalse("Identical labels on different menus not unique: "
                    + AWT.getPath(dupMB1),
                    AWT.getPath(dupMB1).equals(AWT.getPath(dupMB2)));
        assertEquals("Unique Menubar item includes full path",
                     "File|" + uniqueMB.getLabel(), AWT.getPath(uniqueMB));
        assertEquals("Unique popup item includes full path",
                     uniquePopup.getLabel(), AWT.getPath(uniquePopup));

        MenuItem[] items = AWT.findAWTPopupMenuItems(frame, AWT.getPath(dup));
        assertEquals("Duplicate popup item not uniquely found",
                     1, items.length);
        assertEquals("Wrong duplicate popup item found", dup, items[0]);

        items = AWT.findAWTPopupMenuItems(frame, AWT.getPath(dup2));
        assertEquals("Duplicate popup item not uniquely found (2)",
                     1, items.length);
        assertEquals("Wrong duplicate popup item found (2)", dup2, items[0]);
    }

    public void testAWTGetInvoker() {
        Frame frame = new Frame(getName());
        PopupMenu popup = new PopupMenu();
        MenuItem mi = new MenuItem("Open");
        popup.add(mi);
        Label label = new Label(getName());
        frame.add(label);
        label.add(popup);

        assertEquals("Wrong invoker", label, AWT.getInvoker(mi));

        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File");
        menu.add(mi);
        mb.add(menu);
        frame.setMenuBar(mb);

        assertNull("Invoker should be null for menubar-based items",
                   AWT.getInvoker(mi));
    }

    public void testAWTIsOnPopup() {
        Frame frame = new Frame(getName());
        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File");
        MenuItem mi = new MenuItem("Open");
        menu.add(mi);
        mb.add(menu);
        frame.setMenuBar(mb);
        assertTrue("Menu item should not register as on a popup",
                   !AWT.isOnPopup(mi));

        PopupMenu popup = new PopupMenu();
        popup.add(menu);
        frame.add(popup);
        assertTrue("Menu item should register as on a popup", 
                   AWT.isOnPopup(mi));
    }

    public void testFindAWTPopupMenuItems() throws Exception {
        Frame frame = new Frame(getName());
        PopupMenu p1 = new PopupMenu();
        PopupMenu p2 = new PopupMenu();
        MenuItem mi = new MenuItem("Open");
        MenuItem mi2 = new MenuItem("Open");
        p1.add(mi);
        p1.add(new MenuItem("Close"));
        p2.add(mi2);
        final Label label = new Label(getName());
        frame.add(label);
        label.add(p1);
        label.add(p2);

        MenuItem[] items = AWT.findAWTPopupMenuItems(label, "Open");
        assertEquals("Wrong number of items found", 2, items.length);
        assertEquals("Wrong item #1", mi, items[0]);
        assertEquals("Wrong item #2", mi2, items[1]);
    }

    public void testFindAWTMenuItems() throws Exception {
        Frame frame = new Frame(getName());
        MenuBar mb = new MenuBar();
        Menu fileMenu = new Menu("File");
        Menu editMenu = new Menu("Edit");
        MenuItem open = new MenuItem("Open");
        fileMenu.add(open);
        MenuItem close = new MenuItem("Close");
        fileMenu.add(close);
        mb.add(fileMenu);
        MenuItem open2 = new MenuItem("Open");
        editMenu.add(open2);
        mb.add(editMenu);
        frame.setMenuBar(mb);

        final Label label = new Label(getName());
        frame.add(label);

        MenuItem[] items = AWT.findAWTMenuItems(frame, "Open");
        assertEquals("Wrong number of items found", 2, items.length);
        assertEquals("Wrong item #1", open, items[0]);
        assertEquals("Wrong item #2", open2, items[1]);

        String PATH = "Edit|Open";
        items = AWT.findAWTMenuItems(frame, PATH);
        assertEquals("Wrong number of items found for " + PATH,
                     1, items.length);
        assertEquals("Wrong item found for " + PATH, open2, items[0]);

        PATH = "File|Close";
        items = AWT.findAWTMenuItems(frame, PATH);
        assertEquals("Wrong number of items found for " + PATH,
                     1, items.length);
        assertEquals("Wrong item found for " + PATH, close, items[0]);
    }

    // FIXME fails on linux/1.4.2 only when run w/full suite
    public void testAWTPopupControl() throws Exception {
        boolean blocks = Bugs.showAWTPopupMenuBlocks();
        if (Robot.getEventMode() == Robot.EM_AWT
            && blocks) {
            return;
        }

        Frame frame = new Frame(getName());
        final PopupMenu popup = new PopupMenu();
        MenuItem mi = new MenuItem("Open");
        popup.add(mi);
        final Label label = new Label(getName());
        frame.add(label);
        label.add(popup);
        showWindow(frame);
        assertFalse("Incorrect initial AWT popup blocking state",
                    AWT.isAWTPopupMenuBlocking());
        try {
            Runnable show = new Runnable() {
                public void run() {
                    popup.show(label, 0, label.getHeight());
                }
            };
            SwingUtilities.invokeLater(show);

            // Give the popup time to show; waitforidle would block
            Thread.sleep(200);
            if (blocks) {
                assertEquals("Incorrect AWT popup blocking state",
                             true, AWT.isAWTPopupMenuBlocking());
            }

            AWT.dismissAWTPopup();
            assertFalse("AWT Popup could not be dismissed",
                        AWT.isAWTPopupMenuBlocking());
        }
        finally {
            // have to close the popup before doing any waitforidle or
            // anything that requires the AWT tree lock
            AWT.dismissAWTPopup();
        }
    }

    public void testRootFrameCheck() {
        Frame f = JOptionPane.getRootFrame();
        assertTrue("Wrong class name for root frame: actual=" + f.getClass(),
                   f.getClass().getName().startsWith(AWT.ROOT_FRAME_CLASSNAME));
        assertTrue("Test for root name should pass for root frame",
                   AWT.isSharedInvisibleFrame(f));
        assertFalse("Test for root name should not pass on something else",
                    AWT.isSharedInvisibleFrame(new JFrame(getName())));
    }

    public void testGetWindow() {
        JFrame frame = new JFrame(getName());
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("file");
        JMenu submenu = new JMenu("open");
        JMenuItem item = new JMenuItem("open");
        submenu.add(item);
        menu.add(submenu);
        bar.add(menu);
        frame.setJMenuBar(bar);
        assertEquals("Wrong window for menu item", frame, AWT.getWindow(item));
        assertEquals("Wrong window for submenu", frame, AWT.getWindow(submenu));
        assertEquals("Wrong window for menu", frame, AWT.getWindow(menu));
        assertEquals("Wrong window for menu bar", frame, AWT.getWindow(bar));
        assertEquals("Wrong window for frame", frame, AWT.getWindow(frame));
    }
    
    public void testMenuActive() {
        JLabel label = new JLabel(getName());
        JFrame frame = new JFrame(getName());
        frame.getContentPane().add(label);
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem menuItem = new JMenuItem("Open");
        menu.add(menuItem);
        mb.add(menu);
        frame.setJMenuBar(mb);
        showWindow(frame);

        ComponentTester tester = new ComponentTester();
        tester.actionClick(menu);
        assertTrue("Should detect an active menu", AWT.isMenuActive(label));
    }
    
    public void testEnsureOnScreen() {
        final Frame f = showFrame(new JLabel(getName()));
        WindowTester tester = new WindowTester();
        Rectangle bounds = f.getGraphicsConfiguration().getBounds();
        tester.actionMove(f, bounds.x + bounds.width + 100, f.getY());
        
        invokeAndWait(new Runnable() { public void run() {
            AWT.ensureOnScreen(f);
        }});
        assertTrue("Frame not moved on screen", bounds.contains(f.getLocation()));
        
        tester.actionMove(f, -f.getWidth()-10, f.getY());
        invokeAndWait(new Runnable() { public void run() {
            AWT.ensureOnScreen(f);
        }});
        assertTrue("Frame not moved on screen", 
                   bounds.contains(new Point(f.getX() + f.getWidth(), f.getY())));
    }

    public void testEncoding() throws Exception {
        String utf8 = "\u0444\u0438\u0441\u0432\u0443";
        JTextField tf = new JTextField(utf8);
        showFrame(tf);
        assertEquals("Wrong text", utf8, tf.getText());
        /*
        class Flag { boolean flag; }
        final Flag flag = new Flag();
        tf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flag.flag = true;
            }
        });
        while (!flag.flag) {
            Thread.sleep(100);
        }
        utf8 = tf.getText();
        File file = new File("native.txt");
        FileOutputStream os = new FileOutputStream(file);
        os.write(utf8.getBytes("utf8"));
        os.close();
        */
    }
    
    public static void main(String[] args) {
        RepeatHelper.runTests(args, AWTTest.class);
    }
}
