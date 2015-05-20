package abbot.tester;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import junit.extensions.abbot.*;
import abbot.*;
import abbot.util.Bugs;

/** Unit test to verify the FileDialogTester class.<p> */

public class FileDialogTesterTest extends ComponentTestFixture {

    private File tmpFile;
    private FileDialogTester tester;
    private FileDialog dialog;

    protected void setUp() throws IOException {
        tester = new FileDialogTester();
        Frame frame = new Frame(getName() + " Frame");
        if (Bugs.fileDialogRequiresVisibleFrame()) {
            showWindow(frame);
        }
        dialog = new FileDialog(frame, getName(), FileDialog.LOAD);
        tmpFile = File.createTempFile(getName(), ".test");
        tmpFile.deleteOnExit();
    }

    protected void tearDown() {
        if (dialog.isShowing() && Bugs.fileDialogRequiresDismiss()) {
            tester.actionKeyStroke(KeyEvent.VK_ESCAPE);
        }
        dialog = null;
        tmpFile.delete();
        tmpFile = null;
    }

    public void testSetDirectory() {
        showWindow(dialog);
        String dir = tmpFile.getParentFile().getPath();
        tester.actionSetDirectory(dialog, dir);
        String ddir = dialog.getDirectory();
        if (ddir.endsWith(File.separator))
            ddir = ddir.substring(0, ddir.length()-1);
        if (dir.endsWith(File.separator))
            dir = dir.substring(0, dir.length()-1);
        assertEquals("Directory not set", dir, ddir);
    }

    public void testSetFileAndAccept() {
        showWindow(dialog);
        tester.actionSetFile(dialog, tmpFile.getPath());
        assertEquals("File not set", tmpFile.getPath(), dialog.getFile());
        tester.actionAccept(dialog);
        Timer timer = new Timer();
        while (dialog.isShowing()) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Dialog should have closed on accept");
            tester.sleep();
        }
        assertEquals("File not set", tmpFile.getPath(), dialog.getFile());
        assertTrue("Dialog should have been hidden", !dialog.isShowing());
    }

    public void testCancel() {
        showWindow(dialog);
        tester.actionSetFile(dialog, tmpFile.getPath());
        tester.actionCancel(dialog);
        Timer timer = new Timer();
        while (dialog.isShowing()) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY)
                fail("Dialog should have closed on cancel");
            tester.sleep();
        }
        assertEquals("File should not be set", null, dialog.getFile());
    }

    /** Simulate actual code that shows a dialog and checks the results. */
    private class ShowDialog implements Runnable {
        private boolean onDispatch = false;
        public volatile String fileResult = null;
        public ShowDialog(boolean onDispatch) { this.onDispatch = onDispatch; }
        private void doShow() {
            FileDialog d = dialog;
            d.pack();
            d.setVisible(true);
            fileResult = d.getFile();
        }
        public void run() {
            if (onDispatch) {
                tester.invokeLater(new Runnable() {
                    public void run() { doShow(); }
                });
            }
            else {
                doShow();
            }
        }
    }

    /** Ensure a dialog shown on the event dispatch 
        thread gets the proper result from getFile after FileDialog.show
        returns. */ 
    public void testThreadAcceptDispatch() throws Throwable {
        ShowDialog sd1 = new ShowDialog(true);
        Thread t1 = new Thread(sd1, "event dispatch");
        t1.start();
        waitForWindow(dialog, true);
        tester.actionSetFile(dialog, tmpFile.getPath());
        tester.actionAccept(dialog);
        t1.join();
        assertEquals("File should be set after show on dispatch thread",
                     tmpFile.getPath(), sd1.fileResult);
        assertTrue("Dialog should have been hidden", !dialog.isShowing());
    }

    /** Ensure a dialog shown off the event dispatch 
        thread gets the proper result from getFile after FileDialog.show
        returns. */ 
    public void testThreadAcceptNonDispatch() throws Throwable {
        ShowDialog sd2 = new ShowDialog(false);
        Thread t2 = new Thread(sd2, "non-event dispatch");
        t2.start();
        waitForWindow(dialog, true);
        tester.actionSetFile(dialog, tmpFile.getPath());
        tester.actionAccept(dialog);
        t2.join();
        // NOTE: this may fail due to race conditions
        assertEquals("File should be set after show from non-dispatch thread",
                     tmpFile.getPath(), sd2.fileResult);
        assertTrue("Dialog should have been hidden", !dialog.isShowing());
    }

    public void testThreadCancelDispatch() throws Throwable {
        ShowDialog sd1 = new ShowDialog(true);
        Thread t1 = new Thread(sd1, "event dispatch");
        t1.start();
        waitForWindow(dialog, true);
        tester.actionSetFile(dialog, tmpFile.getPath());
        tester.actionCancel(dialog);
        t1.join();
        assertNull("File should not be set after show on dispatch thread",
                   sd1.fileResult);
        // More OSX bugs?
        if (Platform.isOSX())
            tester.delay(2000);
        assertTrue("Dialog should have been hidden", !dialog.isShowing());
    }

    public void testThreadCancelNonDispatch() throws Throwable {
        ShowDialog sd2 = new ShowDialog(false);
        Thread t2 = new Thread(sd2, "non-event dispatch");
        t2.start();
        waitForWindow(dialog, true);
        tester.actionSetFile(dialog, tmpFile.getPath());
        tester.actionCancel(dialog);
        t2.join();
        assertNull("File should not be set after show from non-dispatch thread",
                   sd2.fileResult);
        assertTrue("Dialog should have been hidden", !dialog.isShowing());
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, FileDialogTesterTest.class);
    }
}

