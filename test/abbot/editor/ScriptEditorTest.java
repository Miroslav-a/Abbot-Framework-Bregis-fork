package abbot.editor;

import abbot.finder.BasicFinder;

import abbot.finder.matchers.ClassMatcher;

import java.io.*;

import junit.extensions.abbot.*;
import abbot.script.Script;


import abbot.util.AWT;

import java.awt.EventQueue;

import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

/** Tests for the script editor proper. */

public class ScriptEditorTest extends ComponentTestFixture {

    ScriptEditor editor;

    protected void setUp() {
        editor = new ScriptEditor(new EditorContext()) {
            protected boolean checkSaveBeforeClose() {
                return true;
            }
        };
    }

    protected void tearDown() {
        if (editor != null) {
            editor.dispose();
        }
        // Make sure we don't have any security managers left around
        // preventing exits
        System.setSecurityManager(null);
    }

    public void testSaveNestedScripts() throws Throwable {
        Script s1 = new Script(getHierarchy());
        Script s2 = new Script(getHierarchy());
        Script s3 = new Script(getHierarchy());
        s1.addStep(s2);
        s2.addStep(s3);
        s3.addStep(new Script(getHierarchy()));
        assertTrue("Script 1 should be marked dirty", s1.isDirty());
        assertTrue("Script 2 should be marked dirty", s2.isDirty());
        assertTrue("Script 3 should be marked dirty", s3.isDirty());
        editor.saveNestedScripts(s1);
        assertTrue("Script 1 should have been saved", !s1.isDirty());
        assertTrue("Script 2 should have been saved", !s2.isDirty());
        assertTrue("Script 3 should have been saved", !s3.isDirty());
    }

    /** Open a new, empty script. */
    public void testNewScript() throws Throwable {
        // Try a file that exists
        final File file = File.createTempFile(getName(), ".xml");
        file.deleteOnExit();
        editor.newScript(file, false);
        // Default script should always get a launch + terminate
        Script script = (Script)editor.getResolverContext();
        assertTrue("Script should have a launch step",
                   script.hasLaunch());
        assertTrue("Script should have a terminate step",
                   script.hasTerminate());

        // Try a file that doesn't exist
        final File newFile = File.createTempFile(getName(), ".xml");
        newFile.deleteOnExit();
        newFile.delete();

        // Test was failing due to this EDT violation
        AWT.invokeAndWait(new Runnable() {
            public void run() {
                editor.newScript(newFile, false);
            }
        });
            
        
        script = (Script)editor.getResolverContext();
        assertTrue("Script should have a launch step",
                   script.hasLaunch());
        assertTrue("Script should have a terminate step",
                   script.hasTerminate());
    }


    /** Open a new, empty script. */
    public void testBrokenScript() throws Throwable {
        // Try a file that exists
        final File file = File.createTempFile(getName(), ".xml");
        file.deleteOnExit();
        PrintWriter pw = new PrintWriter(file);
        pw.println("This is not a valid script");
        pw.close();
        
        
        // Just run this from the event queue
        EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    editor.openScript(file);
                }
            });
         
        // Check this pops up a JOptionPane
        
        JOptionPane pane = (JOptionPane)BasicFinder.getDefault().find(new ClassMatcher(JOptionPane.class));
        
        assertNotNull("Script pop up an error dialog, not found", pane);

        // Get rid of the panel
        //
        pane.setVisible(false);

        // Check some of the properties
        //
        assertEquals("Script pop up an error dialog, wrong type", JOptionPane.ERROR_MESSAGE,  pane.getMessageType());
        
        
    }


    /** Construct a test case with the given name. */
    public ScriptEditorTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptEditorTest.class);
    }
}
