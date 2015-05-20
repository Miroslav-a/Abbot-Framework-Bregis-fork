package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;
import abbot.util.Bugs;

/**
 * Unit test to verify proper capture of user semantic events on a Choice.
 */
public class ChoiceRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private Frame frame;
    private Choice choice;
    private static final int MAX_ENTRIES = 20;

    // FIXME linux gets duplicate ITEM_STATE_CHANGED events (VM bug)
    public void testCaptureSelection() {
        showChoice();
        ChoiceTester tester = new ChoiceTester();

        startRecording();
        int idx = MAX_ENTRIES - 1;
        tester.actionSelectIndex(choice, idx);
        tester.delay(500);
        assertStep("SelectItem(.*,item " + idx + ")");
        if (Bugs.hasChoiceLockupBug()) {
            return;
        }

        startRecording();
        idx = MAX_ENTRIES/2-1;
        tester.actionSelectIndex(choice, idx);
        tester.delay(500);
        assertStep("SelectItem(.*,item " + idx + ")");

        startRecording();
        tester.actionSelectIndex(choice, 0);
        assertStep("SelectItem(.*,item 0)");
    }

    public void testESCToCancelSelection() {
        showChoice();
        startRecording();
        ComponentTester tester = ComponentTester.getTester(Component.class);
        tester.mousePress(choice);
        tester.actionKeyStroke(KeyEvent.VK_ESCAPE);
        assertNoStep();
    }

    public void testClickToCancelSelection() {
        showChoice();
        startRecording();
        ComponentTester tester = ComponentTester.getTester(Component.class);
        tester.mousePress(choice);
        JPanel pane = (JPanel)((JFrame)frame).getContentPane();
        tester.actionClick(pane, 0, 0);
        assertNoStep();
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new ChoiceRecorder(r);
    }

    private void showChoice() {
        choice = new Choice();
        for (int i=0;i < MAX_ENTRIES;i++) {
            choice.add("item " + i);
        }
        // Must have a listener or no events will be fired
        choice.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            }
        });
        frame = showFrame(choice);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, ChoiceRecorderTest.class);
    }
}
