package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;
import abbot.util.AWT;

/**
 * Unit test to verify proper capture of user semantic events on a JComboBox.
 */
public class JComboBoxRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private Frame frame;
    private JComboBoxTester tester;
    private JComboBox combo;
    private static final int MAX_ENTRIES = 20;

    // FIXME sporadic failure on linux 1.4.1_02
    public void testCaptureMousePressDragReleaseSelect() {
        showComboBox();

        startRecording();
        tester.drag(combo, combo.getWidth()/2, combo.getHeight()/2);
        tester.waitForIdle();

        // FIXME this is just an estimate of the right position
        tester.drop(combo, combo.getWidth()/2,
                    combo.getHeight() * 4/2);
        tester.waitForIdle();
        assertStep("SelectItem\\(.*,item 1\\)");
    }

    public void testCaptureClickMoveClickSelection() {
        showComboBox();
        startRecording();
        tester.actionSelectIndex(combo, MAX_ENTRIES - 1);
        assertStep("SelectItem(.*,item "
                   + (MAX_ENTRIES - 1) + ")");
        tester.actionSelectIndex(combo, MAX_ENTRIES / 2 - 1);
        assertStep("SelectItem(.*,item "
                   + (MAX_ENTRIES/2 - 1) + ")");
        tester.actionSelectIndex(combo, 0);
        assertStep("SelectItem(.*,item 0)");
    }

    public void testCaptureCanceledSelection() {
        showComboBox();
        startRecording();
        tester.actionClick(combo);
        tester.actionKeyStroke(KeyEvent.VK_ESCAPE);
        assertNoStep();
    }

    public void testCaptureSelectionWithExtraEvents() {
        showComboBox();
        startRecording();
        tester.actionClick(combo);
        Component popup = AWT.findActivePopupMenu();
        assertNotNull("Combo should have an associated popup when active",
                      popup);
        // FIXME need to add some intervening events
    }

    public void testCaptureClickCancelSelection() {
        showComboBox();
        startRecording();
        tester.actionClick(combo);
        JPanel pane = (JPanel)((JFrame)frame).getContentPane();
        tester.actionClick(pane, 1, 1);
        assertNoStep();
    }

    public JComboBoxRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JComboBoxRecorder(r);
    }

    private void showComboBox() {
        Vector list = new Vector();
        for (int i=0;i < MAX_ENTRIES;i++) {
            list.add("item " + i);
        }
        combo = new JComboBox(list);
        tester = (JComboBoxTester)ComponentTester.getTester(combo);
        frame = showFrame(combo);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JComboBoxRecorderTest.class);
    }
}
