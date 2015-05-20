package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.*;
import abbot.script.*;
import abbot.tester.*;
import abbot.tester.Robot;

/**
 * Unit test to verify proper capture of multiple user semantic events when
 * parsed from a continuous event stream.
 * TODO: feed the event recorder one or more canned event sequences with the 
 * expected results.  Input streams may vary by platform/VM version.
 * Need a separate test of the canned stream itself against robot-generated
 * events to ensure things don't change.  Currently the input event stream is
 * not explicit in the tests and is thus resistant to change.
 * <p>
 * Feeding specific event sequences avoids having to throw up an
 * IF during testing.
 * <p>
 * Needs much more testing on multi-step recording.
 * All recording should be tested here since EventRecorder is the only context
 * in which the others are used, and individual tests are prone to missing
 * things at the start or end of a recording.
 */
public class EventRecorderTest extends AbstractRecorderFixture {

    private EventRecorder recorder;
    private JTextComponentTester tester;
    private Step step;

    // Provide this so we can test as if we were a platform that doesn't
    // generate them
    private boolean suppressKeyTypedEvents = false;
        
    protected Recorder getRecorder() {
        step = null;
        // Add a little extra to optionally suppress KEY_TYPED events
        recorder = new EventRecorder(getResolver(), false) {
            protected void recordEvent(AWTEvent event) 
                throws RecordingFailedException {
                if (!suppressKeyTypedEvents
                    || event.getID() != KeyEvent.KEY_TYPED
                    || !(event instanceof ComponentEvent)) {
                    Log.debug("ERT: " + Robot.toString(event));
                    super.recordEvent(event);
                }
            }
        };
        recorder.addActionListener(new ActionListener() {
            /** Will be invoked when the recorder posts status messages. */
            public void actionPerformed(ActionEvent ev) {
                Log.debug(ev.getActionCommand());
            }
        });
        return recorder;
    }

    protected void setUp() {
        tester = new JTextComponentTester();
    }

    protected void tearDown() {
        recorder = null;
        tester = null;
        step = null;
    }

    private void waitForStep() {
        tester.waitForIdle();
        step = recorder.getStep();
    }

    private Step getStep() {
        if (step == null) 
            step = recorder.getStep();
        return step;
    }

    public void testCaptureMnemonic() {
        JButton button = new JButton("Push Me");
        button.setMnemonic(KeyEvent.VK_P);
        class Flag { volatile boolean flag; }
        final Flag flag = new Flag();
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                flag.flag = true;
            }
        });
        showFrame(button);
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_P, Robot.MOUSELESS_MODIFIER_MASK);
        assertTrue("Mnemonic had no effect, check mask ("
                   + Robot.MOUSELESS_MODIFIER + ")", flag.flag);
        Sequence seq = (Sequence)getStep();
        assertStepCount(1, seq);
        assertStep("KeyStroke\\(.*,VK_P,", seq.getStep(0));
    }

    // FIXME NPE on X11, OSX, as if tracking a drag
    public void testCaptureTextSelection() {
        String text = "Select some text";
        JTextField tf = new JTextField(text);
        JPanel p = new JPanel();
        p.add(tf);
        p.add(new JLabel(getName()));
        showFrame(p);

        // Ensure the drag exits the component
        startRecording();
        tester.actionDrag(tf, 1, tf.getHeight()/2);
        tester.mouseMove(tf, tf.getWidth()/2, tf.getHeight()/2);
        tester.mouseMove(tf, tf.getWidth()/2, tf.getHeight()/2+1);
        tester.actionDrop(tf, tf.getWidth() + 100, tf.getHeight()/2);
        Sequence seq = (Sequence)getStep();
        assertStep("SelectText\\(.*,0," + text.length() + "\\)",
                   seq.getStep(0));
    }

    public void testCaptureActivateDialogFromButton() {
        // clicking a button that results in a change of focus to that button
        // shouldn't leave a trailing MOUSE_RELEASED after the Click is parsed
        // are there other possible intervening events?
        // 
        // In addition, make sure the resulting dialog only results in a
        // single "wait for frame showing"
        JButton button = new JButton("Push me");
        final Frame frame = showFrame(button);

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                JDialog dialog = new JDialog(frame, "Dialog", true);
                dialog.getContentPane().add(new JButton("OK"));
                dialog.pack();
                dialog.setVisible(true);
            }
        });
        startRecording();
        tester.actionClick(button);
        stopRecording();
        
        assertTrue("Event recorder should return a Sequence", 
                   getStep() instanceof Sequence);
        Sequence seq = (Sequence)getStep();
        assertStepCount(2, seq);
        assertStep("Click", seq.getStep(0));
        assertStep("Wait for ComponentShowing", seq.getStep(1));
    }

    /** Display a text field, already focused. */
    private JTextField showTextField() {
        JTextField tf = new JTextField();
        tf.setColumns(10);
        showFrame(tf);
        tester.actionFocus(tf);
        return tf;
    }

    /** Ensure that a single key press results in a single keystroke event. */
    // Sometimes fails on linux with pointer focus
    // w32 sporadic failure
    public void testCaptureSingleKey() {
        // Some VM versions produce dual key_typed events for non-text
        // components; make sure we save only one.  If a text field is used,
        // only one key_typed event is received.
        JButton button = new JButton("Focus here");
        JTextField tf = new JTextField("Focus here next");
        JPanel pane = new JPanel();
        pane.add(button);
        pane.add(tf);
        showFrame(pane);

        // Don't want to capture any actions related to focus.
        tester.actionFocus(button);
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_A);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   getStep() instanceof Sequence);
        Sequence seq = (Sequence)getStep();
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,a\\)", seq.getStep(0));

        // Now send to a text field
        tester.actionFocus(tf);
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_B);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   getStep() instanceof Sequence);
        seq = (Sequence)getStep();
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,b\\)", seq.getStep(0));
    }

    /** Prefer capturing numeric keypad keystrokes to the characters that are
     * produced, since keypad operation may produce different behavior than
     * entering numeric characters.
     */
    // FIXME linux only types first 6 keys
    public void testCaptureNumPadKeys() {
        JTextField tf = showTextField();

        final int NUMKEYS = 10;
        boolean needLock = false;
        try {
            needLock = !Toolkit.getDefaultToolkit().
                getLockingKeyState(KeyEvent.VK_NUM_LOCK);
        }
        catch(UnsupportedOperationException e) {
            // Type a numpad key and see what we get
            tester.actionKeyStroke(tf, KeyEvent.VK_NUMPAD4);
            needLock = !tf.getText().equals("4");
        }
        if (needLock) {
            tester.actionKeyStroke(KeyEvent.VK_NUM_LOCK);
            tester.actionActionMap(tf, "select-all");
            tester.actionKeyStroke(KeyEvent.VK_NUMPAD4);
            assertEquals("NumLock not set, can't generate numpad keys",
                         "4", tf.getText());
        }
        // Don't want any special key mappings; this prevents the keys from
        // appearing in the text field, but still allows the recorder to run
        // as expected. 
        //tf.setInputMap(JComponent.WHEN_FOCUSED, new InputMap());

        startRecording();
        try {
            for (int i=0;i < NUMKEYS;i++) {
                tester.actionKeyStroke(tf, KeyEvent.VK_NUMPAD0 + i);
            }
            stopRecording();
            waitForStep();
            assertTrue("Event recorder should return a Sequence",
                       getStep() instanceof Sequence);
            Sequence seq = (Sequence)getStep();
            assertTrue("No steps recorded", seq.size() > 0);
            assertStepCount(NUMKEYS, seq);
            for (int i=0;i < NUMKEYS;i++) {
                String expected = "VK_NUMPAD" + i;
                assertStep("KeyStroke\\(.*," + expected + "\\)",
                           seq.getStep(i));
            }
        }
        finally {
            if (needLock) 
                tester.actionKeyStroke(KeyEvent.VK_NUM_LOCK);
        }
    }

    /** Should save no individual key press/release events. */
    public void testCaptureKeyString() {
        showTextField();
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_T);
        tester.actionKeyStroke(KeyEvent.VK_E);
        tester.actionKeyStroke(KeyEvent.VK_X);
        tester.actionKeyStroke(KeyEvent.VK_T);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)getStep();
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,text\\)", seq.getStep(0));
    }

    /** Should save no modifiers or individual key press/release. */
    public void testCaptureKeyStringShifted() {
        showTextField();
        startRecording();
        tester.actionKeyPress(KeyEvent.VK_SHIFT);
        tester.actionKeyStroke(KeyEvent.VK_T);
        tester.actionKeyStroke(KeyEvent.VK_E);
        tester.actionKeyStroke(KeyEvent.VK_X);
        tester.actionKeyStroke(KeyEvent.VK_T);
        tester.actionKeyRelease(KeyEvent.VK_SHIFT);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,TEXT\\)", seq.getStep(0));
    }

    /** Should save no modifiers, since the modifier gets incorporated into
     * each key stroke.
     */
    public void testCaptureKeyStringPartiallyShifted() {
        showTextField();
        startRecording();
        tester.actionKeyPress(KeyEvent.VK_SHIFT);
        tester.actionKeyStroke(KeyEvent.VK_T);
        tester.actionKeyRelease(KeyEvent.VK_SHIFT);
        tester.actionKeyStroke(KeyEvent.VK_E);
        tester.actionKeyStroke(KeyEvent.VK_X);
        tester.actionKeyStroke(KeyEvent.VK_T);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,Text\\)", seq.getStep(0));
    }

    /** A modifier down/up should be captured that way. */
    public void testCaptureModifier() {
        showTextField();
        startRecording();
        tester.actionKeyPress(KeyEvent.VK_SHIFT);
        tester.actionKeyRelease(KeyEvent.VK_SHIFT);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(2, seq);
        assertStep("KeyEvent.KEY_PRESSED \\(VK_SHIFT\\) on \\$\\{JTextField Instance\\}", 
                     seq.getStep(0));
        assertStep("KeyEvent.KEY_RELEASED \\(VK_SHIFT\\) on \\$\\{JTextField Instance\\}", 
                     seq.getStep(1));
    }

    public void testCaptureWithNoKeyTypedEvents() {
        suppressKeyTypedEvents = true;
        showTextField();
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_T);
        tester.actionKeyStroke(KeyEvent.VK_E);
        tester.actionKeyStroke(KeyEvent.VK_X);
        tester.actionKeyStroke(KeyEvent.VK_T);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(4, seq);
        assertStep("KeyStroke\\(.*,VK_T\\)", seq.getStep(0));
        assertStep("KeyStroke\\(.*,VK_E\\)", seq.getStep(1));
        assertStep("KeyStroke\\(.*,VK_X\\)", seq.getStep(2));
        assertStep("KeyStroke\\(.*,VK_T\\)", seq.getStep(3));
        suppressKeyTypedEvents = false;
    }

    /** Test an event stream from rapidly typed keys. */
    public void testCaptureJumbledKeyEvents() {
        showTextField();
        startRecording();
        // Actual event sequence seen when typing "SHIFt" rapidly
        tester.actionKeyPress(KeyEvent.VK_SHIFT);
        tester.actionKeyPress(KeyEvent.VK_S);
        tester.actionKeyRelease(KeyEvent.VK_S);
        tester.actionKeyPress(KeyEvent.VK_H);
        tester.actionKeyPress(KeyEvent.VK_I);
        tester.actionKeyRelease(KeyEvent.VK_H);
        tester.actionKeyPress(KeyEvent.VK_F);
        tester.actionKeyRelease(KeyEvent.VK_I);
        tester.actionKeyRelease(KeyEvent.VK_F);
        tester.actionKeyRelease(KeyEvent.VK_SHIFT);
        tester.actionKeyStroke(KeyEvent.VK_T);
        stopRecording();
        waitForStep();
        // Should capture exactly 5 steps, one for each letter typed
        assertTrue("Event recorder should return a Sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,SHIFt\\)", seq.getStep(0));
    }

    /** Tabs are used for focus traversal. */
    public void testCaptureFocusTraversal() {
        JTextField comp1, comp2, comp3, comp4;
        JPanel pane = new JPanel();
        pane.add(comp1 = new JTextField("Field 1"));
        pane.add(comp2 = new JTextField("Field 2"));
        pane.add(comp3 = new JTextField("Field 3"));
        pane.add(comp4 = new JTextField("Field 4"));
        showFrame(pane);
        tester.actionFocus(comp1);
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_TAB);
        tester.actionKeyStroke(KeyEvent.VK_TAB);
        tester.actionKeyStroke(KeyEvent.VK_TAB, KeyEvent.SHIFT_MASK);
        long start = System.currentTimeMillis();
        while (!comp2.hasFocus()) {
            if (System.currentTimeMillis() - start > 5000) {
                throw new Error("Timed out waiting for proper focus on "
                                + Robot.toString(comp2));
            }
        }
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(3, seq);
        toString(seq);
        assertStep("KeyStroke\\(.*,VK_TAB\\)1*", seq.getStep(0));
        assertStep("KeyStroke\\(.*,VK_TAB\\)2*", seq.getStep(1));
        assertStep("KeyStroke\\(.*,VK_TAB,SHIFT_MASK\\)", seq.getStep(2));
        assertEquals("No text expected", "Field 1", comp1.getText());
        assertEquals("No text expected", "Field 2", comp2.getText());
        assertEquals("No text expected", "Field 3", comp3.getText());
        assertEquals("No text expected", "Field 4", comp4.getText());
    }

    public void testMacModifierStripping() {
        // Ensure the input method modifiers are stripped
        if (!Platform.isMacintosh()) {
            return;
        }
        showTextField();
        startRecording();
        tester.keyPress(KeyEvent.VK_ALT);
        tester.keyPress(KeyEvent.VK_E);
        tester.keyRelease(KeyEvent.VK_E);
        tester.keyRelease(KeyEvent.VK_ALT);
        tester.keyPress(KeyEvent.VK_E);
        tester.keyRelease(KeyEvent.VK_E);
        tester.waitForIdle();
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(1, seq);
        assertStep("KeyString\\(.*,\u00e9\\)", seq.getStep(0));
    }

    public void testMouseThenKey() {
        JButton button = new JButton("Press");
        showFrame(button);
        startRecording();
        tester.actionClick(button);
        tester.actionKeyStroke(KeyEvent.VK_A);
        stopRecording();
        waitForStep();
        assertTrue("Event recorder should return a sequence",
                   step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(2, seq);
        assertStep("Click\\(.*\\)", seq.getStep(0));
        assertStep("KeyString\\(.*,a\\)", seq.getStep(1));
    }

    public void testStripShortcutKey() {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        JTextField tf = new JTextField(getName());
        showFrame(tf);
        tester.actionFocus(tf);
        startRecording();
        tester.actionKeyStroke(KeyEvent.VK_A, mask);
        tester.actionKeyStroke(KeyEvent.VK_X, mask);
        tester.actionKeyStroke(KeyEvent.VK_V, mask);
        tester.actionKeyPress(KeyEvent.VK_CONTROL);
        tester.actionKeyStroke(KeyEvent.VK_A);
        tester.actionKeyStroke(KeyEvent.VK_X);
        tester.actionKeyStroke(KeyEvent.VK_V);
        tester.actionKeyRelease(KeyEvent.VK_CONTROL);
        stopRecording();
        waitForStep();
        assertTrue("Expected a sequence", step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(6, seq);
        assertStep("ActionMap\\(.*,select-all\\)", seq.getStep(0));
        assertStep("ActionMap\\(.*,cut-to-clipboard\\)", seq.getStep(1));
        assertStep("ActionMap\\(.*,paste-from-clipboard\\)", seq.getStep(2));
        assertStep("ActionMap\\(.*,select-all\\)", seq.getStep(0));
        assertStep("ActionMap\\(.*,cut-to-clipboard\\)", seq.getStep(1));
        assertStep("ActionMap\\(.*,paste-from-clipboard\\)", seq.getStep(2));
    }

    // Popups may be displayed in response to any stimulus. */
    public void testCapturePopupMenuFromButton() {
        final JButton button = new JButton(getName());
        final JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem("File"); popup.add(mi);
        JMenuItem mi2 = new JMenuItem("Edit"); popup.add(mi2);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });
        showFrame(button);

        startRecording();
        tester.actionClick(button);
        tester.actionClick(mi2);

        waitForStep();
        assertTrue("Expected a sequence: " + step, step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(2, seq);
        assertStep("Click\\(.*\\)", seq.getStep(0));
        assertStep("SelectMenuItem\\(" + mi2.getText() + "\\)",
                   seq.getStep(1));
    }

    // AWT popups are a bit more tricky
    public void testCaptureAWTPopupMenuFromButton() throws Exception {
        final JButton button = new JButton(getName());
        final PopupMenu popup = new PopupMenu();
        MenuItem mi = new MenuItem("File"); popup.add(mi);
        MenuItem mi2 = new MenuItem("Edit"); popup.add(mi2);
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                popup.show(button, 0, button.getHeight());
            }
        });
        button.add(popup);
        showFrame(button);

        startRecording();

        // waitForIdle times out if a w32 AWT popup is showing, 
        // so use plain old click
        ComponentTester tester = new ComponentTester();
        tester.click(button);
        tester.delay(100);
        tester.click(button, 10, button.getHeight() + 10);
        // FIXME figure out why this delay is necessary
        // Note: the framework doesn't use events to trigger AWT menu
        // selections, so this is not normally an issue.
        if (Platform.isOSX()) // Consider Bugs.XXX
            tester.delay(500);
        waitForStep();
        assertTrue("Expected a sequence: " + step, step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(2, seq);
        assertStep("Click\\(.*\\)", seq.getStep(0));
        assertStep("SelectAWTPopupMenuItem\\(.*,"
                   + mi.getLabel() + "\\)",
                   seq.getStep(1));
    }

    /** Create a new test case with the given name. */
    public EventRecorderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, EventRecorderTest.class);
    }
}
