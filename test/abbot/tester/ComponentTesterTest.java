package abbot.tester;

import java.lang.reflect.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import junit.extensions.abbot.*;
import abbot.DynamicLoadingConstants;
import abbot.finder.ComponentSearchException;
import abbot.finder.matchers.ClassMatcher;
import abbot.util.*;
import abbot.util.Bugs;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/** Unit test to verify the base ComponentTester class.<p>

    <ul>
    <li>Test all exported actions.
    </ul>
 */
// OSX uses a button/modifier combo to indicate MB2/3...ick
// w32 uses a button/modifier combo to indicate MB3...ick
public class ComponentTesterTest extends ComponentTestFixture implements DynamicLoadingConstants {

    private ComponentTester tester;

    protected void setUp() {
        tester = new ComponentTester();
    }
    protected void tearDown() {
        tester = null;
    }

    private class ActionFlag extends AbstractAction {
        public volatile boolean gotAction;
        public ActionFlag(String label) { super(label); }
        public void actionPerformed(ActionEvent ev) {
            gotAction = true;
        }
    }

    public void testSelectPopupMenuItem() {
        JPanel pane = new JPanel();
        ActionFlag item1 = new ActionFlag("Item One");
        ActionFlag item2 = new ActionFlag("Item Two");
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(item1));
        popup.add(new JMenuItem(item2));

        installPopup(pane, popup);
        showFrame(pane, new Dimension(200, 200));

        // This implicitly tests "actionShowPopupMenu"
        tester.actionSelectPopupMenuItem(pane, "Item One");
        assertTrue("First popup menu item not selected", item1.gotAction);

        tester.actionSelectPopupMenuItem(pane, "Item Two");
        assertTrue("Second popup menu item not selected", item2.gotAction);
    }

    // Broken in 1.5 on linux; popup submenu gets dismissed if mouse button
    // is not depressed
    public void testSelectPopupSubMenuItem() {
        JPanel pane = new JPanel();
        ActionFlag item1 = new ActionFlag("Item One");
        ActionFlag item2 = new ActionFlag("Item Two");
        ActionFlag sub1 = new ActionFlag("Subitem One");
        ActionFlag sub2 = new ActionFlag("Subitem Two");
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(item1));
        popup.add(new JMenuItem(item2));
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem(sub1));
        submenu.add(new JMenuItem(sub2));
        popup.add(submenu);

        installPopup(pane, popup);
        showFrame(pane, new Dimension(200, 200));

        tester.actionSelectPopupMenuItem(pane, "Subitem One");
        assertTrue("First popup submenu item not selected", sub1.gotAction);

        tester.actionSelectPopupMenuItem(pane, "Subitem Two");
        assertTrue("Second popup submenu item not selected", sub2.gotAction);
    }

    public void testSelectPopupMenuItemByPath() {
        JPanel pane = new JPanel();
        ActionFlag item1 = new ActionFlag("Item One");
        ActionFlag item2 = new ActionFlag("Item Two");
        ActionFlag sub1 = new ActionFlag("Duplicate");
        ActionFlag sub2 = new ActionFlag("Duplicate");
        JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(item1));
        popup.add(new JMenuItem(item2));
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem(sub1));
        popup.add(submenu);
        JMenu submenu2 = new JMenu("Submenu2");
        submenu2.add(new JMenuItem(sub2));
        popup.add(submenu2);

        installPopup(pane, popup);
        showFrame(pane, new Dimension(200, 200));

        // This implicitly tests "actionShowPopupMenu"
        tester.actionSelectPopupMenuItem(pane, "Submenu|Duplicate");
        assertTrue("First popup submenu item not selected", sub1.gotAction);
        tester.actionSelectPopupMenuItem(pane, "Submenu2|Duplicate");
        assertTrue("Second popup submenu item not selected", sub2.gotAction);
        try {
            tester.actionSelectPopupMenuItem(pane, "Ziggy");
            fail("Selecting nonexistent item should fail");
        }
        catch(ActionFailedException e) {
            // expected
        }
        try {
            tester.actionSelectPopupMenuItem(pane, "Item One|Duplicate");
            fail("Selecting nonexistent path should fail");
        }
        catch(ActionFailedException e) {
            // expected
        }
    }

    /** Check translation between event IDs and integer values. */
    public void testEventID() {
        assertEquals("mouse press", MouseEvent.MOUSE_PRESSED,
                     Robot.getEventID(MouseEvent.class,
                                       "MOUSE_PRESSED"));
        assertEquals("mouse release", MouseEvent.MOUSE_RELEASED,
                     Robot.getEventID(MouseEvent.class,
                                       "MOUSE_RELEASED"));
        assertEquals("mouse click", MouseEvent.MOUSE_CLICKED,
                     Robot.getEventID(MouseEvent.class,
                                       "MOUSE_CLICKED"));
    }

    /** Verify forward and back parsing of modifiers. */
    public void testModifiers() {
        // NOTE: As of Java 1.3, Meta and Control also map to button 2/3.
        // Currently the symbolic button masks are preferred to the symbolic
        // key masks. 
        assertEquals("shift", InputEvent.SHIFT_MASK,
                     AWT.getModifiers("SHIFT_MASK"));
        assertEquals("button 1", InputEvent.BUTTON1_MASK,
                     AWT.getModifiers("BUTTON1_MASK"));
        assertEquals("button 2", InputEvent.BUTTON2_MASK,
                     AWT.getModifiers("BUTTON2_MASK"));
        assertEquals("button 3", InputEvent.BUTTON3_MASK,
                     AWT.getModifiers("BUTTON3_MASK"));
        // combinations
        assertEquals("shift + button 1", 
                     InputEvent.SHIFT_MASK | InputEvent.BUTTON1_MASK,
                     AWT.getModifiers("SHIFT_MASK | BUTTON1_MASK"));
    }

    /** Check virtual keycode parsing. */
    public void testKeyCodeParsing() {
        assertEquals("key 'A'", KeyEvent.VK_A,
                     AWT.getKeyCode("VK_A"));
        assertEquals("key '0'", KeyEvent.VK_0,
                     AWT.getKeyCode("VK_0"));
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

    public void testKeyStroke() {
        JTextField tf = new JTextField();
        KeyWatcher kw = new KeyWatcher();
        tf.addKeyListener(kw);
        tf.setColumns(10);
        showFrame(tf);
        tester.actionFocus(tf);
        int code = KeyEvent.VK_A;
        int mods = KeyEvent.SHIFT_MASK;
        tester.actionKeyStroke(code, mods);
        assertTrue("Never received key press", kw.gotPress);
        assertEquals("Wrong key code", code, kw.keyCode);
        assertEquals("Wrong modifiers", AWT.getKeyModifiers(mods), 
                     AWT.getKeyModifiers(kw.modifiers));
        assertTrue("Never received release", kw.gotRelease);
        assertTrue("Never received type", kw.gotTyped);
    }

    private class MouseWatcher extends MouseAdapter {
        public volatile boolean gotClick;
        public volatile int clickCount;
        public volatile int modifiers;
        public void mousePressed(MouseEvent me) {
            modifiers = me.getModifiers();
        }
        public void mouseClicked(MouseEvent me) {
            gotClick = true;
            clickCount = me.getClickCount();
            modifiers = me.getModifiers();
        }
    }
    /** Ensure the advertised multiple click method works. */
    // FIXME sporadic OSX failures
    public void testDoubleClick() {
        MouseWatcher mw = new MouseWatcher();
        JTextField tf = new JTextField("SelectMe");
        tf.addMouseListener(mw);
        showFrame(tf);
        tester.actionFocus(tf);
        tester.actionClick(tf, tf.getWidth()/2, tf.getHeight()/2,
                           InputEvent.BUTTON1_MASK, 2);
        assertEquals("Wrong number of clicks", 2, mw.clickCount);
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL + 10);
    }
    
    /** Ensure that we can send clicks close enough together to register as a
        double click. */
    public void testTwoClicks() {
        final MouseWatcher mw = new MouseWatcher();
        JTextField tf = new JTextField("SelectMe");
        tf.addMouseListener(mw);
        showFrame(tf);
        tester.actionFocus(tf);
        tester.actionClick(tf);
        tester.actionClick(tf);
        assertEqualsEventually("Two clicks should make a double click",
                               2, new Callable<Integer>() { public Integer call()  {return mw.clickCount;}});
    }

    /** 
     * Ensure that we can send clicks the distance of MULTI_CLICK_INTERVAL apart
     * that they don't count as a double click
     */
    public void testTwoClicksWithInterval() {
        MouseWatcher mw = new MouseWatcher();
        JTextField tf = new JTextField("SelectMe");
        tf.addMouseListener(mw);
        showFrame(tf);
        tester.actionFocus(tf);
        tester.actionClick(tf);
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL);
        tester.actionClick(tf); 
        assertEquals("Two clicks with this gap should make a double click",
                     1, mw.clickCount);
    }

    public void testClickWithControlModifier() {
        MouseWatcher mw = new MouseWatcher();
        JTextField tf = new JTextField("SelectMe");
        tf.addMouseListener(mw);
        showFrame(tf);
        tester.actionFocus(tf);
        tester.actionClick(tf, tf.getWidth()/2, tf.getHeight()/2,
                           InputEvent.BUTTON1_MASK|InputEvent.CTRL_MASK);
        assertEquals("Wrong modifiers",
                     InputEvent.BUTTON1_MASK|InputEvent.CTRL_MASK, mw.modifiers);
    }


    // obsolete functionality
    public void testDeriveTag() {
        Button button = new Button("AWT Button");
        assertEquals("Tag should be the Button's label",
                     button.getLabel(), tester.deriveTag(button));
        JButton jbutton = new JButton("JButton");
        assertEquals("Tag should be the JButton's text",
                     jbutton.getText(), tester.deriveTag(jbutton));
        JFrame frame = new JFrame("JFrame");
        assertEquals("Tag should be the JFrame's title",
                     frame.getTitle(), tester.deriveTag(frame));
    }

    public void testStripHTML() {
        String html1 = "<html>simple</html>";
        String strip1 = "simple";
        String html2 = "<html><font color=\"red\" size=\"-1\">complex</font></html>";
        String strip2 = "complex";
        assertEquals("Simple html was not stripped",
                     strip1, ComponentTester.stripHTML(html1));
        assertEquals("Complex html was not stripped",
                     strip2, ComponentTester.stripHTML(html2));
    }

    public void testGetDefaultTester() throws Throwable {
        String cname = DYNAMIC_COMPONENT_CLASSNAME;
        ClassLoader cl = 
            new abbot.util.NonDelegatingClassLoader(DYNAMIC_CLASSPATH,
                                                    getClass().getClassLoader());
        Class cls = Class.forName(cname, true, cl);
        assertEquals("Wrong class loader for test class",
                     cl, cls.getClassLoader());
        ComponentTester customTester = ComponentTester.getTester(cls);
        ComponentTester tester =
            ComponentTester.getTester(java.awt.Component.class);
        assertEquals("Wrong tester loaded", tester, customTester);
        assertTrue("Class loaders should be different",
                   !cls.getClassLoader().equals(tester.getClass().getClassLoader()));
    }

    public void testFindTester() throws Throwable {
        class Dialog extends JPanel { }
        ComponentTester tester =
            ComponentTester.getTester(Dialog.class);
        assertEquals("Wrong tester class loaded", 
                     JComponentTester.class, tester.getClass());
    }

    public void testEndKeyKeyStroke() {
        TextField tf = new TextField(getName());
        KeyWatcher kw = new KeyWatcher();
        tf.addKeyListener(kw);
        showFrame(tf);
        tester.actionKeyStroke(tf, KeyEvent.VK_END);
        assertTrue("Never received key press", kw.gotPress);
        assertEquals("Wrong key code", KeyEvent.VK_END, kw.keyCode);
        assertTrue("Never received key release", kw.gotRelease);
    }

    // Make sure the select menu item action works on a popup regardless of
    // how it was triggered.
    public void testSelectFromButtonTriggeredPopup() {
        final JButton button = new JButton(getName());
        ActionFlag item1 = new ActionFlag("Item One");
        ActionFlag item2 = new ActionFlag("Item Two");
        ActionFlag sub1 = new ActionFlag("Subitem One");
        ActionFlag sub2 = new ActionFlag("Subitem Two");
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi1, mi2;
        popup.add(new JMenuItem(item1));
        popup.add(mi1 = new JMenuItem(item2));
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem(sub1));
        submenu.add(mi2 = new JMenuItem(sub2));
        popup.add(submenu);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });
        showFrame(button, new Dimension(300, 200));

        tester.actionClick(button);
        tester.actionSelectMenuItem(mi1);
        assertTrue("First-level item on popup not selected", item2.gotAction);

        tester.actionClick(button);
        tester.actionSelectMenuItem(mi2);
        assertTrue("2nd-level item on popup not selected", sub2.gotAction);
    }

    public void testSelectFromButtonTriggeredAWTPopup() {
        if (Robot.getEventMode() == Robot.EM_AWT
            && Bugs.showAWTPopupMenuBlocks()) {
            //fail("This test would block");
            return;
        }

        final JButton button = new JButton(getName());
        ActionFlag item2 = new ActionFlag("Item Two");
        ActionFlag sub2 = new ActionFlag("Subitem Two");
        final PopupMenu popup = new PopupMenu();
        MenuItem mi1, mi2;
        popup.add(new MenuItem("Item 1"));
        popup.add(mi1 = new MenuItem("Item Two"));
        Menu submenu = new Menu("Submenu");
        submenu.add(new MenuItem("Subitem One"));
        submenu.add(mi2 = new MenuItem("Subitem Two"));
        popup.add(submenu);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });
        Frame frame = showFrame(button, new Dimension(300, 200));
        frame.add(popup);
        mi1.addActionListener(item2);
        mi2.addActionListener(sub2);

        tester.click(button);
        tester.actionSelectAWTPopupMenuItem(frame, mi1.getLabel());
        assertTrue("First-level item on popup not selected", item2.gotAction);

        tester.click(button);
        tester.actionSelectAWTPopupMenuItem(frame, mi2.getLabel());
        assertTrue("2nd-level item on popup not selected", sub2.gotAction);
    }

    public void testGetComponentActions() {
        Method[] methods = tester.getComponentActions();
        for (int i=0;i < methods.length;i++) {
            if (methods[i].getName().endsWith("ByLabel"))
                fail("Deprecated method should be omitted");
        }
    }

    public void testGetPropertyMethods() {
        Method[] methods = tester.getPropertyMethods();
        for (int i=0;i < methods.length;i++) {
            String name = methods[i].getName();
            if (name.equals("getTag")
                || name.equals("getTester")) {
                fail("Non-property method should be omitted");
            }
        }
    }

    public void testNoBlockOnKeyStrokeToActivateModalDialog() throws Exception {
        final KeyStroke ks = 
            KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_MASK);

        final JMenuItem b = new JMenuItem();
        Action action = new AbstractAction("Save As") {
            public void actionPerformed(ActionEvent e) {
                new JFileChooser().showOpenDialog(b);
            }
        };
        b.setAction(action);
        JFrame f = new JFrame(getName());
        JMenuBar mb = new JMenuBar();
        JMenu file = new JMenu("File");
        mb.add(file);
        file.add(b);
        f.setJMenuBar(mb);
        f.getContentPane().add(new JLabel("test"));
        b.setAccelerator(ks);
        showWindow(f);
        tester.actionKeyStroke(ks.getKeyCode(), ks.getModifiers());
        try {
            getFinder().find(new ClassMatcher(JFileChooser.class));
        }
        catch(ComponentSearchException e) {
            throw new RuntimeException("Dialog not opened on keystroke");
        }
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ComponentTesterTest.class);
    }
}

