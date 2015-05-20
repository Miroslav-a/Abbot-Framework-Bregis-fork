package abbot.editor.recorder;

import java.awt.*;
import java.io.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.Platform;
import abbot.script.*;
import abbot.tester.FileDialogTester;

/**
 * Unit test to verify proper capture of user semantic events on a FileDialog.
 */
public class FileDialogRecorderTest extends AbstractSemanticRecorderFixture {

    private FileDialog dialog;
    private FileDialogTester tester;

    public void testCaptureFile() {
        String fn = "blahblahblah";
        startRecording();
        showWindow(dialog);
        tester.actionSetFile(dialog, fn);
        tester.actionAccept(dialog);
        Step step = getStep();
        assertTrue("Expected a sequence", step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(2, seq);
        assertStep("SetFile\\(.*," + fn + "\\)", seq.getStep(0));
        assertStep("Accept\\(.*\\)", seq.getStep(1));
    }

    public void testCaptureDirectoryChange() throws IOException {
        File file = File.createTempFile(getName(), ".test");
        String dn = file.getParent();
        String fn = "blah";
        startRecording();
        showWindow(dialog);
        tester.actionSetDirectory(dialog, dn);
        tester.actionSetFile(dialog, fn);
        tester.actionAccept(dialog);
        
        Step step = getStep();
        assertTrue("Expected a sequence", step instanceof Sequence);
        Sequence seq = (Sequence)step;
        assertStepCount(3, seq);
        
        // On JDK 7 the path returned doesn't have a file sep at the end so
        // we add the "?" regex to match for both
        assertStep("SetDirectory\\(.*," + dn.replace('\\', '.') + File.separator + "?" +  "\\)",
                   seq.getStep(0));
        assertStep("SetFile\\(.*," + fn + "\\)", seq.getStep(1));
        assertStep("Accept\\(.*\\)", seq.getStep(2));
    }

    public void testCaptureCancel() {
        String fn = "foo";
        startRecording();
        showWindow(dialog);
        tester.actionSetFile(dialog, fn);
        tester.actionCancel(dialog);
        assertStep("Cancel\\(.*\\)");
    }

    protected void setUp() {
        Frame frame = new Frame(getName());
        // Workaround for w32 bug
        if (Platform.isWindows())
            showWindow(frame);
        dialog = new FileDialog(frame, getName(), FileDialog.SAVE);
        tester = new FileDialogTester();
    }

    public FileDialogRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new FileDialogRecorder(r);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, FileDialogRecorderTest.class);
    }
}
