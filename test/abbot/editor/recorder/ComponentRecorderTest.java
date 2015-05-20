package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.Log;
import abbot.Platform;
import abbot.util.AWT;
import abbot.util.WeakAWTEventListener;
import abbot.script.*;
import abbot.tester.*;

/**
 * Unit test to verify proper capture of basic user semantic events.  A
 * ComponentTester is used to generate the appropriate events, which is
 * significantly easier than reproducing the steps manually.  These tests
 * should be run after the ComponentTester tests.
 */
public class ComponentRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private ComponentTester tester;

    protected void setUp() {
        tester = new ComponentTester();
    }

    protected void tearDown() {
        tester = null;
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new ComponentRecorder(r);
    }

    // Open events to capture (those known to generate unique sequences of
    // events):
    // First window (open)
    // Subsequent window (open')
    // Window (close)

    public void testNamedWindowShow() {
        Frame frame = showFrame(new JLabel(getName()));
        JWindow win = new JWindow(frame);
        win.getContentPane().add(new JLabel(getName()));
        String NAME = "MyWindow";
        win.setName(NAME);
        startRecording();
        showWindow(win);
        assertStep("Wait for ComponentShowing\\(" + NAME + "\\)");
        assertNoStep();

        win.dispose();
        assertStep("Wait for !ComponentShowing\\(" + NAME + "\\)");
    }

    public void testFrameShowDispose() {
        startRecording();
        Frame frame = showFrame(new JLabel(getName()));
        // FIXME 1.4 produces additional RESIZE and SHOWING events prior to
        // WINDOW_OPEN -- need to catch these

        // Must catch an initial window open.  First open produces
        // WINDOW_OPENED.   
        assertStep("Wait for ComponentShowing\\(" + getName() + "\\)");

        // Dispose produces WINDOW_CLOSED, without a COMPONENT_HIDDEN
        // 1.4.1 adds a HIERARCHY_CHANGED !SHOWING prior to CLOSED
        frame.dispose(); 
        assertStep("Wait for !ComponentShowing\\(" + getName() + "\\)");

        // Shouldn't record multiple WINDOW_CLOSED events (pre-1.4)
        frame.dispose();
        assertTrue("Multiple CLOSE events generated", !hasRecorder());
    }

    public void testFrameShowHideShow() {
        Frame frame = showFrame(new JLabel(getName()));
        hideWindow(frame);
        startRecording();
        showWindow(frame);
        assertStep("Wait for ComponentShowing\\(" + getName());

        hideWindow(frame);
        assertStep("Wait for !ComponentShowing\\(" + getName());

        showWindow(frame);
        assertStep("Wait for ComponentShowing\\(" + getName());
    }

    public void testDialogShowDispose() {
        // Showing a dialog produces a WINDOW_OPEN, but no SHOWING
        Frame frame = showFrame(new JLabel(getName()));
        startRecording();
        final JDialog dialog = new JDialog(frame, "Dialog");
        showWindow(dialog);
        assertStep("Wait for ComponentShowing");
        
        // avoid extraneous INPUT_METHOD_TEXT_CHANGED events on linux
        startRecording();
        dialog.dispose();
        assertStep("Wait for !ComponentShowing");
        
        // Shouldn't record multiple WINDOW_CLOSED events (pre-1.4)
        dialog.dispose();
        assertTrue("Multiple CLOSE events generated", !hasRecorder());
    }

    public void testDialogShowHideShow() {
        // Showing a dialog produces a WINDOW_OPEN, but no SHOWING
        Frame frame = showFrame(new JLabel(getName()));
        startRecording();
        JDialog dialog = new JDialog(frame, "Dialog");
        dialog.getContentPane().add(new JLabel(getName() + " Dialog"));
        showWindow(dialog);
        assertStep("Wait for ComponentShowing");
        
        // avoid extraneous INPUT_METHOD_TEXT_CHANGED events on linux
        startRecording();
        hideWindow(dialog);
        assertStep("Wait for !ComponentShowing");
        
        // avoid extraneous INPUT_METHOD_TEXT_CHANGED events on linux
        startRecording();
        showWindow(dialog);
        assertStep("Wait for ComponentShowing");
    }

    public void testShowDialogOnSameNamedFrame() {
        Frame frame = showFrame(new JLabel(getName()));
        startRecording();
        JDialog dialog = new JDialog(frame, getName());
        dialog.getContentPane().add(new JLabel(getName() + " Dialog"));
        showWindow(dialog);
        assertStep("Wait for ComponentShowing");
    }

    private boolean itemOne = false;
    private boolean itemTwo = false;
    private boolean subItemOne = false;
    private boolean subItemTwo = false;
    private JPanel initPopup() {
        final JPopupMenu popup = new JPopupMenu();
        popup.add(new JMenuItem(new AbstractAction("Item One") {
            public void actionPerformed(ActionEvent ev) {
                itemOne = true;
            }
        }));
        popup.add(new JMenuItem(new AbstractAction("Item Two") {
            public void actionPerformed(ActionEvent ev) {
                itemTwo = true;
            }
        }));
        JMenu submenu = new JMenu("Submenu");
        submenu.add(new JMenuItem(new AbstractAction("Subitem One") {
            public void actionPerformed(ActionEvent ev) {
                subItemOne = true;
            }
        }));
        submenu.add(new JMenuItem(new AbstractAction("Subitem Two") {
            public void actionPerformed(ActionEvent ev) {
                subItemTwo = true;
            }
        }));
        popup.add(submenu);
        JPanel pane = new JPanel() {
            public Dimension getPreferredSize() {
                return new Dimension(100, 100);
            }
        };
        addPopup(pane, popup);
        return pane;
    }

    private void triggerPopup(Component pane) {
        Point where = new Point(pane.getWidth()/2, pane.getHeight()/2);
        if (Platform.isLinux()) {
            tester.mousePress(pane, where.x, where.y, AWTConstants.POPUP_MASK);
        }
        else {
            // Use click instead of actionClick to avoid event queue blockage
            // on w32 when popup is visible 
            tester.click(pane, where.x, where.y, AWTConstants.POPUP_MASK);
        }
        // OSX needs this
        tester.delay(200);
        where.translate(10, 10);
        if (Platform.isLinux()) {
            tester.mousePress(pane, where.x, where.y, InputEvent.BUTTON1_MASK);
            tester.mouseRelease(InputEvent.BUTTON1_MASK|AWTConstants.POPUP_MASK);
            tester.waitForIdle();
        }
        else {
            tester.actionClick(pane, where.x, where.y);
        }
        // OSX needs this
        tester.delay(200);
    }

    // Care must be taken when activating an AWT PopupMenu, since it holds the
    // AWTTreeLock while the popup is showing.  Since the normal
    // abbot.tester.Robot.mouseMove(Component,int,int) method calls
    // Component.getLocationOnScreen, it can't be used to position the pointer
    // for a click on the popup.
    public void testCaptureAWTPopupMenuSelection() throws Exception {
        Panel pane = new Panel();
        final PopupMenu popup = new PopupMenu();
        popup.add(new MenuItem("popup action"));
        popup.add(new MenuItem("one"));
        popup.add(new MenuItem("two"));
        popup.add(new CheckboxMenuItem("three"));
        addPopup(pane, popup);
        showFrame(pane, new Dimension(400, 400));
        startRecording();

        triggerPopup(pane);

        assertStep("SelectAWTPopupMenuItem\\(.*,popup action\\)");
    }

    public void testCapturePopup() {
        JPanel pane = initPopup();
        showFrame(pane);

        startRecording();
        tester.actionSelectPopupMenuItem(pane, "Item One");
        assertStep("SelectPopupMenuItem");
    }

    public void testCapturePopupSubmenu() {
        JPanel pane = initPopup();
        showFrame(pane);

        startRecording();
        tester.actionSelectPopupMenuItem(pane, "Subitem Two");
        assertStep("SelectPopupMenuItem");
    }

    public void testCapturePopupSubmenuPath() {
        JPanel pane = initPopup();
        showFrame(pane);

        startRecording();
        tester.actionSelectPopupMenuItem(pane, "Submenu|Subitem Two");
        assertStep("SelectPopupMenuItem");
    }

    /** If no popup exists, a regular click should be recorded instead, using
     * the appropriate mask.
     */
    public void testCaptureNoPopup() {
        JLabel label = new JLabel("No Popup Here");
        showFrame(label);
        startRecording();
        tester.actionClick(label,
                           label.getWidth()/2,
                           label.getHeight()/2,
                           AWTConstants.POPUP_MASK);
        assertStep("Click\\(.*,POPUP_MASK\\)");
    }

    // See comments for testCaptureAWTPopupMenuSelection
    public void testCaptureAWTMenuSelection() throws Exception {
        MenuBar mb = new MenuBar();
        Menu menu1 = new Menu("File"); mb.add(menu1);
        Menu menu2 = new Menu("Edit"); mb.add(menu2);
        MenuItem mi = new MenuItem("Open");
        class Flag { volatile boolean flag; }
        menu1.add(mi);
        Frame frame = new Frame(getName());
        Label label = new Label(getName());
        frame.add(label);
        frame.setMenuBar(mb);
        showWindow(frame);

        startRecording();
        if (Platform.isOSX()) {
            // might have useScreenMenuBar enabled, so punt
            tester.actionSelectAWTMenuItem(frame, mi.getLabel());
        }
        else {
            // Guess the positioning for the menu bar and its popup
            tester.actionClick(label, 10, -10);
            tester.actionClick(label, 10, 10);
        }
        assertStep("SelectAWTMenuItem\\(.*,"
                   + "File|" + mi.getLabel() + "\\)");
    }

    public void testCaptureMenuBarMenuSelection() {
        JMenuBar menubar = new JMenuBar();
        JMenu menu1 = new JMenu("File"); menubar.add(menu1);
        JMenu menu2 = new JMenu("Edit"); menubar.add(menu2);
        JMenuItem mi = new JMenuItem("Copy");
        menu2.add(mi);
        
        JMenu subMenu = new JMenu("sub"); menu2.add(subMenu);
        JMenuItem mi2 = new JMenuItem("Paste");
        subMenu.add(mi2);
        
        
        JFrame frame = new JFrame("frame");
        frame.setJMenuBar(menubar);
        showWindow(frame);

        startRecording();
        tester.actionSelectMenuItem(mi);
        assertStep("SelectMenuItem.frame,Edit|Copy.");
        stopRecording();
  
        startRecording();
        tester.actionSelectMenuItem(mi2);
        assertStep("SelectMenuItem.frame,Edit|sub|Paste.");
        stopRecording();
    }

    public void testCaptureClick() {
        Frame frame = showFrame(new JLabel(getName()));
        startRecording();
        tester.actionClick(frame);
        assertStep("Click");
    }

    /** If the step we're recording results in the target component being
     * hidden (or in general if the component gets hidden before the step is
     * finished recording), the step should still be recorded properly.
     */
    // NOTE: this doesn't test the most general case of an arbitrary component
    // being hidden.
    // FIXME consistent linux failure here
    public void testCaptureWithAHiddenComponent() {
        JButton close = new JButton("close");

        JButton push = new JButton("push");
        Frame frame = showFrame(push);
        
        final JDialog dialog =
            new JDialog(frame, getName() + " Dialog");
        dialog.getContentPane().add(close);
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                dialog.dispose();
            }
        });
        push.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                dialog.pack(); dialog.setVisible(true);
            }
        });

        tester.actionClick(push);
        startRecording();
        assertEquals("Dialog prematurely gone",
                     1, frame.getOwnedWindows().length);
        assertEquals("Frame should own dialog",
                     dialog, frame.getOwnedWindows()[0]);
        assertEquals("Wrong dialog parent",
                     frame, dialog.getParent());
        tester.actionClick(close);
        assertStep("Click");
    }

    private void showTextField() {
        JTextField tf = new JTextField();
        tf.setColumns(10);
        showFrame(tf);
        // make sure text field has focus
        tester.actionFocus(tf);
    }

    public void testCaptureKeyStroke() {
        showTextField();
        startRecording();
        tester.actionKeyString("a");
        assertStep("KeyString\\(.*,a\\)");
    }

    public void testCaptureShiftedKeyStroke() {
        showTextField();
        startRecording();
        tester.actionKeyString("A");
        assertStep("KeyString\\(.*,A\\)");
    }

    public void testCaptureSpecialKeys() {
        showTextField();
        startRecording();
        int[] keys = {
            KeyEvent.VK_ENTER,
            KeyEvent.VK_TAB,
            KeyEvent.VK_BACK_SPACE,
            KeyEvent.VK_DELETE,
            KeyEvent.VK_ESCAPE
        };
        for (int i=0;i < keys.length;i++) {
            tester.actionKeyStroke(keys[i]);
            // NOTE: not every platform generates KEY_TYPED events; if not,
            // then there is no problem. (w2k 1.3.1 gets no DELETE)
            if (hasRecorder())
                assertStep("KeyStroke\\(.*,"
                           + AWT.getKeyCode(keys[i]) + "\\)");
        }
    }

    public void testCaptureMacInputMethod() {
        if (Platform.isMacintosh()) {
            showTextField();
            tester.actionKeyStroke(KeyEvent.VK_E, KeyEvent.ALT_MASK);
            startRecording();
            tester.actionKeyStroke(KeyEvent.VK_E);
            assertStep("KeyString\\(.*,\u00e9\\)");
        }
    }

    // FIXME only do this where InputMethod is supported -- how do we detect
    // that? 
    public void xtestCaptureInputMethod() {
        showTextField();
        startRecording();
        try {
            tester.actionKeyStroke(KeyEvent.VK_HALF_WIDTH);
        }
        catch(ArrayIndexOutOfBoundsException aio) {
            if (Platform.isMacintosh())
                return;
            throw aio;
        }
        catch(IllegalArgumentException iae) {
            if (Platform.isLinux()) 
                return;
            throw iae;
        }
        tester.actionKeyStroke(KeyEvent.VK_K);
        tester.actionKeyStroke(KeyEvent.VK_A);
        tester.actionKeyStroke(KeyEvent.VK_N);
        tester.actionKeyStroke(KeyEvent.VK_J);
        tester.actionKeyStroke(KeyEvent.VK_I);
        tester.actionKeyStroke(KeyEvent.VK_FULL_WIDTH);
        assertStep("Some text"); // FIXME
        Step step = getStep();
        assertTrue("Should have recorded a sequence",
                   step instanceof Sequence);
        assertEquals("Wrong number of keystrokes", 7, ((Sequence)step).size());
    }

    // sporadic failures on Linux 2.4.20/1.4.1_02
    public void testCaptureMultipleClick() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        startRecording();
        tester.actionClick(label, label.getWidth()/2,
                           label.getHeight()/2,
                           InputEvent.BUTTON1_MASK, 2);
        assertStep("Click\\(.*,2\\)");
    }
    
    public void testCaptureClickWithComponentDisposal() {
        JButton button = new JButton(getName());
        final Frame frame = showFrame(button);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
        startRecording();
        tester.actionClick(button);
        assertStep("Click");
    }

    public void testCaptureMultiClickWithComponentDisposal() {
        // switch panes in response to a double click
        // the original bug was found when changing to a new applet page via
        // javascript in response to a double click (the double click was not
        // recorded). 
        final JLabel label = new JLabel("Next Page");
        final Frame frame = showFrame(label);
        final JPanel replacement = new JPanel();
        replacement.add(new JButton(getName() + " replacement"));
        label.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Force a mouse exit event 
                    getRobot().mouseMove(frame, 1, 1);
                    ((JFrame)frame).setContentPane(replacement);
                    frame.invalidate();
                }
            }
        });
        startRecording();
        tester.actionClick(label, label.getWidth()/2, label.getHeight()/2,
                           InputEvent.BUTTON1_MASK, 2);
        assertStep("Click\\(.*,2\\)");
    }

    private class ToolTipWatcher implements AWTEventListener {
        public volatile boolean opened = false;
        public void eventDispatched(AWTEvent ev) {
            if (ev.getID() == WindowEvent.WINDOW_OPENED) {
                opened = true;
                Toolkit.getDefaultToolkit().
                    removeAWTEventListener(ToolTipWatcher.this);
            }
        }
    }

    /** Tooltip displays should not be recorded. */
    public void testCaptureNoToolTips() {
        JLabel label = new JLabel(getName());
        ToolTipWatcher tw = new ToolTipWatcher();
        label.setToolTipText("A really long label that is sure to create a window that exceeds the original frame bounds");
        showFrame(label);
        new WeakAWTEventListener(tw, WindowEvent.WINDOW_EVENT_MASK);
        startRecording();
        tester.mouseMove(label, 0, 0);
        tester.mouseMove(label);
        Timer timer = new Timer();
        while (!tw.opened) {
            if (timer.elapsed() > 5000)
                throw new RuntimeException("No tooltip appeared");
        }
        assertNoStep();
    }

    public void testCaptureTableHeaderClick() {
        int MAX_ENTRIES = 4;
        String[][] cells = new String[MAX_ENTRIES][MAX_ENTRIES];
        for (int i=0;i < MAX_ENTRIES;i++) {
            for (int j=0;j < MAX_ENTRIES;j++) {
                cells[i][j] = "cell " + i + "," + j;
            }
        }
        String[] names = new String[MAX_ENTRIES];
        for (int i=0;i < MAX_ENTRIES;i++) {
            names[i] = "col " + i;
        }
        JTable table = new JTable(cells, names);
        showFrame(new JScrollPane(table));

        startRecording();
        JTableHeader header = table.getTableHeader();
        tester.actionClick(header, new JTableHeaderLocation(1));
        assertStep("Click\\(.*,\"col 1\"\\)");
    }

    // If duplicate labels are found among available popup menus, ensure that
    // additional information is provided to the invocation.
    public void testCaptureAWTPopupMenuSelectionWithTwoSimilarPopups() {
        Frame frame = new Frame("frame");
        Panel pane = new Panel();
        Label label1 = new Label("left");
        Label label2 = new Label("right");
        pane.add(label1);
        pane.add(label2);
        frame.add(pane);
        PopupMenu p1 = new PopupMenu();
        MenuItem m1 = new MenuItem("item");
        p1.add(m1);
        PopupMenu p2 = new PopupMenu();
        MenuItem m2 = new MenuItem("item");
        p2.add(m2);

        addPopup(label1, p1);
        addPopup(label2, p2);
        // Make both popups have the same invoker
        frame.add(p1);
        frame.add(p2);

        showWindow(frame);

        startRecording(false);

        triggerPopup(label1);

        String id = frame.getTitle(); // should be cref id
        assertStep("SelectAWTPopupMenuItem\\("
                   + id + "," + AWT.getPath(m1) + "\\)");
        stopRecording();

        startRecording(false);

        triggerPopup(label2);
        assertStep("SelectAWTPopupMenuItem\\("
                   + id + "," + AWT.getPath(m2) + "\\)");
    }

    /** Focus accelerators should not be recorded as typed events, but rather
     * as basic keystrokes.
     */
    // FIXME sporadic linux failure here
    public void testCaptureFocusAccelerator() {
        // Mac doesn't support focus accelerators
        if (Platform.isOSX()) 
            return;
        
        JTextField tf1 = new JTextField("1"); tf1.setName("TF1");
        JTextField tf2 = new JTextField("2"); tf2.setName("TF2");
        tf1.setFocusAccelerator('a');
        tf2.setFocusAccelerator('b');
        JPanel p = new JPanel();
        p.add(tf1);
        p.add(tf2);
        showFrame(p);
        assertTrue("First text field should have focus", tf1.hasFocus());

        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK);
        assertTrue("Focus accelerator didn't work", tf2.hasFocus());
        assertNoStep();

        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_MASK);
        assertTrue("Focus accelerator didn't work", tf1.hasFocus());
        assertNoStep();
    }

    // This would be a nice test, but it'd have to be run on every 
    // keyboard hardware to actually test it.  For now, ad hoc discard what we
    // know to be irrelevant
    //public void testWhichKeyTypedEventsAreIgnored() { }

    /** Create a new test case with the given name. */
    public ComponentRecorderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ComponentRecorderTest.class);
    }
}
