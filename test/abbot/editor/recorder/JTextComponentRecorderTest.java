package abbot.editor.recorder;

import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.JTextComponentTester;

/** Test recording of JTextComponent-specific events. */
public class JTextComponentRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JTextComponentTester tester;

    protected void setUp() throws Exception {
        super.setUp();
        tester = new JTextComponentTester();
    }

    // FIXME select action failure on first run on Linux/1.4.2
    public void testCaptureSelection(){ 
        String text = "Select some text";
        JTextField tf = new JTextField(text);
        JPanel p = new JPanel();
        p.add(tf);
        p.add(new JLabel(getName()));
        showFrame(p);
        startRecording();
        tester.actionSelectText(tf, 1, text.length()-1);
        assertStep("SelectText\\(.*,1," + (text.length()-1) + "\\)");
        tester.actionSelectText(tf, 0, 5);
        assertStep("SelectText\\(.*,0,5\\)");
        tester.actionSelectText(tf, 4, text.length());
        assertStep("SelectText\\(.*,4," + text.length() + "\\)");
        tester.actionSelectText(tf, 0, text.length());
        assertStep("SelectText\\(.*,0," + text.length() + "\\)");
    }

    public void testCaptureReverseSelection() {
        String text = "Select me backwards";
        JTextField tf = new JTextField(text);
        JPanel p = new JPanel();
        p.add(tf);
        p.add(new JLabel(getName()));
        showFrame(p);
        startRecording();
        
        
        tester.actionSelectText(tf, text.length()-1, 1);
        assertStep("SelectText\\(.*," + (text.length()-1) + ",1\\)");

        tester.actionSelectText(tf, 5, 0);
        assertStep("SelectText\\(.*,5,0\\)");
        tester.actionSelectText(tf, text.length(), 4);
        assertStep("SelectText\\(.*," + text.length() + ",4\\)");
        tester.actionSelectText(tf, text.length(), 0);
        assertStep("SelectText\\(.*," + text.length() + ",0\\)");
    }

    // FIXME NPE due to improper drag/drop recording on linux/1.4.2
    public void testCaptureScrollingSelection(){ 
        String text = "Select some text";
        JTextField tf = new JTextField(text);
        JPanel p = new JPanel();
        p.add(tf);
        p.add(new JLabel(getName()));
        showFrame(p);
        tester.actionSelectText(tf, 1, text.length()-1);
        startRecording();
        tester.actionDrag(tf, 1, tf.getHeight()/2);
        tester.actionDrop(tf, tf.getWidth() + 100, tf.getHeight()/2);
        assertStep("SelectText\\(.*,0," + text.length() + "\\)");
    }

    public void testCaptureSetCaretPosition() {
        String text = "Set the caret position";
        JTextField tf = new JTextField(text);
        showFrame(tf);
        startRecording();
        tester.actionClick(tf, 0);
        assertStep("Click\\(.*,0\\)");

        tester.actionClick(tf, text.length()-1);
        assertStep("Click\\(.*," + (text.length()-1) + "\\)");

        tester.actionClick(tf, text.length()/2);
        assertStep("Click\\(.*," + (text.length()/2) + "\\)");
    }

    /** Create a new test case with the given name. */
    public JTextComponentRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JTextComponentRecorder(r);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTextComponentRecorderTest.class);
    }
}
