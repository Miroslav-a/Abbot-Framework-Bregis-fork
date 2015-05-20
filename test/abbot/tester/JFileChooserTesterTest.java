package abbot.tester;

import java.awt.*;
import java.io.File;
import javax.swing.*;

import junit.extensions.abbot.*;

public class JFileChooserTesterTest extends ComponentTestFixture {

    private JFileChooserTester tester;
    private JFileChooser chooser;
    private int result;

    private static final int ANY = 0;
    private static final int OPEN = 1;
    private static final int SAVE = 2;

    protected void setUp() throws Exception {
        tester = new JFileChooserTester();
        chooser = new JFileChooser();
        show(ANY);
    }

    protected Dialog show(final int which) throws Exception {
        return showModalDialog(new Runnable() { 
            public void run() {
                switch(which) {
                case ANY:
                    result = chooser.showDialog(null, "<accept text>");
                    break;
                case OPEN:
                    result = chooser.showOpenDialog(null);
                    break;
                case SAVE:
                    result = chooser.showSaveDialog(null);
                    break;
                }
            }
        });
    }

    public void testSetFilenameAndApprove() throws Exception {
        String FILENAME = "goobler";
        tester.actionSetFilename(chooser, FILENAME);
        tester.actionApprove(chooser);
        assertEquals("File should have been approved",
                     JFileChooser.APPROVE_OPTION, result);
        assertTrue("No file selected",
                   chooser.getSelectedFile() != null);
        assertEquals("Wrong file selected",
                     FILENAME, chooser.getSelectedFile().getName());
    }

    // sporadic OSX exception thrown on EDT (OSX bug)
    public void testSetDirectory() throws Exception {
        getRobot().delay(10000);
        File dir = new File(System.getProperty("user.home"));
        File pwd = chooser.getCurrentDirectory();
        // Make sure the directory actually gets changed
        if (pwd.equals(dir)) {
            File tmp = File.createTempFile(getName(), ".tmp");
            tmp.deleteOnExit();
            dir = tmp.getParentFile();
        }
        tester.actionSetDirectory(chooser, dir.getAbsolutePath());
        assertEquals("Directory not selected",
                     dir, chooser.getCurrentDirectory());
    }

    public void testCancel() throws Exception {
        tester.actionCancel(chooser);
        assertEquals("File should have been canceled", 
                     JFileChooser.CANCEL_OPTION, result);
    }

    /** Create a new test case with the given name. */
    public JFileChooserTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JFileChooserTesterTest.class);
    }
}

