package abbot.script;

import abbot.finder.AWTHierarchy;
import abbot.finder.Hierarchy;

import java.awt.*;
import java.io.*;
import java.util.Iterator;

import javax.swing.*;
import java.util.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import org.jdom.Element;

import abbot.tester.Robot;

/** 
 * Verify the script works as advertised.
 */
public class ScriptTest extends ResolverFixture implements XMLConstants {

    private Script script;
    private File baseDir;
    private File otherRoot;    
    private static final String ENCODING = "UTF-8";

    protected void setUp() throws IOException {
        script = new Script(getHierarchy());
        
        // Create a base dir
        
        baseDir = File.createTempFile("baseDir","");
        baseDir.deleteOnExit();
        
        // Configure a file in another root to make sure that we get absolute 
        // paths
        
        if (System.getProperty("os.name").toLowerCase().startsWith("window")) {
            otherRoot = new File("w:\\root");
        }
        else {
            otherRoot = new File("/newroot");
        }
        
        // Finally set the location relative to location on the script
        // so all start at the same position
        //
        
        script.setRelativeTo(baseDir);
        
        //
        
        ComponentReference.cacheOnCreation = false;
    }

    protected void tearDown() {
        ComponentReference.cacheOnCreation = true;
    }

    public static Reader stringToReader(String script) throws Throwable {
        ByteArrayInputStream ba =
            new ByteArrayInputStream(script.getBytes(ENCODING));
        InputStreamReader reader = new InputStreamReader(ba, ENCODING);
        return reader;
    }

    public void testDefaultDescription() {
        String filename = script.getFile().getName();
        assertTrue("Default script description should omit the filename "
                   + "if the script is a temporary file: "
                   + script.getDescription(),
                   script.getDescription().indexOf(filename) == -1);
    }
    
    // need an automatic way to generate all component ref attributes
    // using a hand-coded array isn't any more reliable than the original
    // edits of abbot.xsd.
    /*
    public void testComponentValidation() throws Throwable {
        Frame f = new Frame(getName());
        ComponentReference ref = script.addComponent(f);
        Iterator iter = VALID_ATTRIBUTES.iterator();
        // Make sure our XSD has included ALL the valid cref attributes
        while (iter.hasNext()) {
            ref.setAttribute((String)iter.next(), "0");
        }
        script.save();
        script.load();
    }
    */

    public void testLaunchAsUIContext() {
        script.clear();
        Launch launch = new Launch(script, getName(), "any.cls.name", "method", new String[] { "[]" });
        script.addStep(launch);
        assertEquals("Launch should be UIContext", launch, script.getUIContext());
    }
    
    public void testFixtureAsUIContext() {
        script.clear();
        Fixture fixture = new Fixture(script, new HashMap());
        script.addStep(fixture);
        assertEquals("Fixture should be UIContext", fixture, script.getUIContext());
    }

    public void testCatchInvalidScript() throws Throwable {
        String bogus = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript desc=\"ignored\">\r\n"
            + "  <terminate/>\r\n"
            + "  <component id=\"id\"/>\r\n"
            + "</AWTTestScript>\r\n\r\n";
        try {
            script.load(stringToReader(bogus));
            fail("Expected an invalid script exception");
        }
        catch(InvalidScriptException ise) {
        }
    }

    public void testInValidLaunchLocation() throws Throwable {
        String valid = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript>\r\n"
            + "  <sequence/>\r\n"
            + "  <launch class=\"none\" method=\"main\" args=\"[]\"/>\r\n"
            + "</AWTTestScript>\r\n\r\n";
        try {
            script.load(stringToReader(valid));
            fail("A script with a launch step in other than position 0 should be detected as invalid");
        }
        catch(InvalidScriptException e) {
        }
    }

    public void testTerminatePositionValidation() throws Throwable {
        String valid = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript>\r\n"
            + "  <terminate/>\r\n"
            + "  <launch class=\"none\" method=\"main\" args=\"[]\"/>\r\n"
            + "</AWTTestScript>\r\n\r\n";
        try {
            script.load(stringToReader(valid));
            fail("A script with a terminate step in other than last position should be detected as invalid");
        }
        catch(InvalidScriptException e) {
        }
    }



    public void testDirty() throws Throwable {
        script.save();
        script.getFile().deleteOnExit();
        script.load();
        assertTrue("Script should not be dirty immediately after load",
                   !script.isDirty());
        script.addStep(new Script(getHierarchy()));
        assertTrue("Script should be dirty after modifications",
                   script.isDirty());
        script.removeStep(0);
        assertTrue("Script should not be dirty", !script.isDirty());
    }

    static final String ID = "button";
    static final String TEXT = "\u65E5\u008Aencoded";
    static final String CLASS = "javax.swing.JButton";
    private static final String unicodeData = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
        + "<AWTTestScript>\r\n"
        + "  <component class=\"" + CLASS + "\" id=\""
        + ID + "\" text=\"" + TEXT + "\" />\r\n"
        + "  <terminate />\r\n"
        + "</AWTTestScript>\r\n\r\n";
    public void testWrite() throws Throwable {
        StringWriter writer = new StringWriter();
        Element el = new Element(XMLConstants.TAG_COMPONENT).
            setAttribute(XMLConstants.TAG_ID, ID).
            setAttribute(XMLConstants.TAG_TEXT, TEXT).
            setAttribute(XMLConstants.TAG_CLASS, CLASS);
        ComponentReference ref = script.addComponentReference(el);
        assertEquals("Incorrect tag encoding",
                     TEXT, ref.getAttribute(XMLConstants.TAG_TEXT));
        script.addStep(new Terminate(script, (String)null));
        script.save(writer);

        // Note that this is all Unicode, NOT UTF-8
        assertEquals("Incorrect XML written", unicodeData, writer.toString());
    }

    public void testRead() throws Throwable {
        try {
            script.load(stringToReader(unicodeData));
            assertEquals("Wrong number of references",
                         1, script.getComponentReferences().size());
            assertEquals("Wrong number of steps", 1, script.size());
            ComponentReference ref = (ComponentReference)
                script.getComponentReferences().iterator().next();
            // Ensure all attributes are saved with unicode
            assertEquals("Wrong tag",
                         TEXT, ref.getAttribute(XMLConstants.TAG_TEXT));
        }
        catch(InvalidScriptException ise) {
            fail(ise.getMessage());
        }
    }

    /** Ensure that a nested script maintains its own context. */
    public void testNestedReference() throws Throwable {
        Script nested = new Script(getHierarchy());
        nested.load(stringToReader(unicodeData));
        script.addStep(nested);
        assertEquals("Wrong resolver", script, script.getResolver());
        assertEquals("Wrong nested resolver", nested, nested.getResolver());
        assertEquals("Wrong resolver for nested step",
                     nested, ((Step)nested.steps().get(0)).getResolver());

        String nestedScriptData = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript>\r\n"
            + "  <script filename=\"nested.xml\" >\r\n"
            + "  </script>\r\n"
            + "</AWTTestScript>\r\n\r\n";
        script.load(stringToReader(nestedScriptData));
        nested = (Script)script.steps().get(0);
        assertEquals("Wrong resolver for nested script",
                     nested, nested.getResolver());
    }

    /** A nested script should have *only* its filename attribute saved. */
    public void testSaveNestedScript() throws Throwable {
        String fullNestedScript = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript>\r\n"
            + "  <script filename=\"nested.xml\" >\r\n"
            + "    <comment desc=\"Filler\" />\r\n"
            + "  </script>\r\n"
            + "</AWTTestScript>\r\n\r\n";
        String savedNestedScript = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            + "<AWTTestScript>\r\n"
            + "  <script filename=\"nested.xml\" />\r\n"
            + "</AWTTestScript>\r\n\r\n";
        script.load(stringToReader(fullNestedScript));
        StringWriter writer = new StringWriter();
        script.save(writer);
        assertEquals("Incorrectly saved",
                     savedNestedScript, writer.toString());
        Script nested = (Script)script.steps().get(0);
        assertEquals("Wrong relative directory", 
                     script.getDirectory(), nested.getRelativeTo());
    }


    public void testForceUseForwardSlashesInSavedPaths() 
      throws IOException
    {
        // Configure the script to be in a sub dir with user.home as 
        // relative to location making use of forward slashes
        //
        
        File userDir = new File(System.getProperty("user.home"));
        File scriptLocation = new File(userDir, "relative\\example.xml");
        File relativeLocation = new File(userDir, "");
        
        //
        
        script.setRelativeTo(relativeLocation);
        script.setFile(scriptLocation);
        
        //

        assertTrue("Relative path contains backslashes",
                   script.getFilename().indexOf("\\") == -1);
        
        assertEquals("Relative path is incorrect",
                     "relative/example.xml", script.getFilename());
        
    }


    public void testRelativeSimple() {
        
        File relative = new File(baseDir, "relative.xml");
        script.setRelativeTo(baseDir);
        script.setFile(relative);
        
        assertTrue("Script file should be absolute",
                   script.getFile().isAbsolute());
        assertEquals("Script filename should now be relative",
                   script.getFilename(), "relative.xml");
    }
    
    public void testSimpleAbsolute() {
        
        File relative = new File(baseDir, "relative.xml");
        script.setRelativeTo(otherRoot);
        script.setFile(relative);
        
        assertTrue("Script file should be absolute",
                   script.getFile().isAbsolute());
        assertEquals("Script filename should now be relative",
                   script.getFilename(), relative.getPath());
    }


    public void testRelativeParent() {
        
        File relative = new File(baseDir, "relative.xml");
        script.setRelativeTo(new File(baseDir, "child"));
        script.setFile(relative);
        
        assertTrue("Script file should be absolute",
                   script.getFile().isAbsolute());
        assertEquals("Script filename should now be relative",
                   script.getFilename(), "../relative.xml");
    }

    public void testRelativeChild() {
        
        File relative = new File(baseDir, "child/relative.xml");
        script.setRelativeTo(baseDir);
        script.setFile(relative);
        
        assertTrue("Script file should be absolute",
                   script.getFile().isAbsolute());
        assertEquals("Script filename should now be relative",
                   script.getFilename(), "child/relative.xml");
    }

    public void testFarTooRelative() {
        File relative = new File(otherRoot, "../../../../..");
        try {
            Script.resolveRelativeReferences(relative);
            fail("Should have throw exception to consuming too much of the path");
        }
        catch (IllegalArgumentException ise) {
            // Do nothing, this is fine
        }
    }


    public void testMoveRelativeParentParent() throws IOException {

        String relativePath = "one/relative.xml";
        String subSubPath = "../../relative.xml";
        
        File oneDir = new File(baseDir, "one");
        File twoDir = new File(oneDir, "two");
        File threeDir = new File(twoDir, "three");
                
        
        
        // Make the root of the script something sensible, as on
        // some platform this could be just any old tmp directory
        //

        File targetFile = new File(baseDir, relativePath);
        script.setFile(targetFile);

        // R: baseDir/one/two/three
        // FI: baseDir/one/relative.xml
        // FO: ../../relative.xml


        script.setRelativeTo(threeDir);
        assertEquals("Wrong relative filename (parent parent)",
                     subSubPath, script.getFilename());
        assertEquals("Wrong relative dir (parent parent)",
                     threeDir, script.getRelativeTo());
        assertEquals("File should be the same (parent parent)",
                     targetFile, script.getFile());        

 

    }
    

    public void testMoveRelativeParent() throws IOException {

        String relativePath = "one/relative.xml";
        String subPath = "../relative.xml";
        
        File oneDir = new File(baseDir, "one");
        File twoDir = new File(oneDir, "two");
                
        
        
        // Make the root of the script something sensible, as on
        // some platform this could be just any old tmp directory
        //

        File targetFile = new File(baseDir, relativePath);
        script.setFile(targetFile);


        // R: baseDir/one/two
        // FI: baseDir/one/relative.xml
        // FO: ../relative.xml
        
        
        script.setRelativeTo(twoDir);
        assertEquals("Wrong relative filename (parent)",
                     subPath, 
                     script.getFilename());
        assertEquals("Wrong relative dir (parent)",
                     twoDir,
                     script.getRelativeTo());
        assertEquals("File should be the same  (parent)",
                     targetFile,
                     script.getFile());




    }
    

    public void testMoveRelative() throws IOException {

        String name = "relative.xml";
        String relativePath = "one/relative.xml";
        
        File oneDir = new File(baseDir, "one");
        
        // Make the root of the script something sensible, as on
        // some platform this could be just any old tmp directory
        //

        File targetFile = new File(baseDir, relativePath);
        script.setFile(targetFile);



        // R: baseDir/one
        // FI: baseDir/one/relative.xml
        // FO: relative.xml
        
        script.setRelativeTo(oneDir);
        assertEquals("Wrong relative filename (sameDir)",
                     name, script.getFilename());
        assertEquals("Wrong relative dir (sameDir)",
                     oneDir, script.getRelativeTo());
        assertEquals("File should be the same  (sameDir)",
                     targetFile, script.getFile());


    }
    


    public void testMoveRelativeChild() throws IOException {

        String relativePath = "one/relative.xml";
        
        // Make the root of the script something sensible, as on
        // some platform this could be just any old tmp directory
        //

        File targetFile = new File(baseDir, relativePath);
        script.setFile(targetFile);


        // R: baseDir
        // FI: baseDir/one/relative
        // FO: one/relative.xml


        script.setRelativeTo(baseDir);
        assertEquals("Wrong relative filename (child)",
                     relativePath, script.getFilename());
        assertEquals("Wrong relative dir (child)",
                     baseDir, script.getRelativeTo());
        assertEquals("File should be the same  (child)",
                     targetFile, script.getFile());



    }
    
    /** Any given component, if added multiple times, should result in the
     * same component reference.
     */
    public void testReferenceReuse() throws Throwable {
        JTextField tf = new JTextField();
        showFrame(tf);

        ComponentReference ref = script.addComponent(tf);
        assertEquals("New reference does not match the component"
                     + " from which it was created", tf, ref.getComponent());

        ComponentReference ref2 = script.addComponent(tf);
        assertEquals("Same component should always result in the same ref",
                     ref, ref2);

        assertEquals("Reverse lookup of reference by component failed",
                     ref, script.getComponentReference(tf));
    }

    /** Any number of unique components should all result in unique
     * references.
     */
    public void testUniqueReferences() {
        JTextField tf1 = new JTextField();
        JTextField tf2 = new JTextField();
        JButton b1 = new JButton("Button");
        JButton b2 = new JButton("Button");
        JPanel pane = new JPanel();
        pane.add(tf1);
        pane.add(tf2);
        pane.add(b1);
        pane.add(b2);
        showFrame(pane);

        ComponentReference cr1 = script.addComponent(tf1);
        ComponentReference cr2 = script.addComponent(tf2);
        assertTrue("Should get two unique text field references", cr1 != cr2);

        ComponentReference cr3 = script.addComponent(b1);
        ComponentReference cr4 = script.addComponent(b2);
        assertTrue("Should get two unique button references", cr3 != cr4);
    }

    public void testReferenceOrdering() throws Throwable {
        HashMap map = new HashMap();
        ComponentReference cr1 =
            new ComponentReference(getResolver(), JButton.class, map);
        ComponentReference cr2 =
            new ComponentReference(getResolver(), JButton.class, map);
        ComponentReference cr3 =
            new ComponentReference(getResolver(), JButton.class, map);
        Script s1 = new Script(getHierarchy());
        Script s2 = new Script(getHierarchy());
        s1.addComponentReference(cr1);
        s1.addComponentReference(cr2);
        s1.addComponentReference(cr3);
        s2.addComponentReference(cr3);
        s2.addComponentReference(cr2);
        s2.addComponentReference(cr1);

        Iterator iter1 = s1.getComponentReferences().iterator();
        Iterator iter2 = s2.getComponentReferences().iterator();
        while (iter1.hasNext()) {
            assertTrue("Reference counts don't match", iter2.hasNext());
            assertEquals("Reference ordering not maintained",
                         iter1.next(), iter2.next());
        }
    }

    /** When referencing a new reference's parent, make sure we don't map it
        inappropriately to an existing reference.
    */
    public void testCreateProperWindowAncestor() {
        JOptionPane pane = new JOptionPane();
        Dialog d = pane.createDialog(null, getName());
        showWindow(d);
        ComponentReference ref1 = script.addComponent(d);
        hideWindow(d);
        final JFileChooser chooser = new JFileChooser();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chooser.showOpenDialog(null);
            }
        });
        // Wait for the dialog to post
        Window w;
        Timer timer = new Timer();
        while ((w = SwingUtilities.getWindowAncestor(chooser)) == null) {
            if (timer.elapsed() > 5000)
                fail("File chooser has no window");
            getRobot().sleep();
        }
        assertTrue("Wrong window selected: " + d, d != w);

        timer.reset();
        while (!w.isShowing()) {
            if (timer.elapsed() > 5000)
                fail("File chooser window is not showing");
            getRobot().sleep();
        }

        // NOTE: making the JFileChooser reference triggered a bug where this
        // reference's dialog/window would mistakenly use the existing dialog
        // reference (ref1) instead of creating a new one.
        ComponentReference ref2 = script.addComponent(chooser);
        ComponentReference ref3 = script.addComponent(w);
        assertTrue("Dialog for file chooser " + ref2
                   + " should get a unique reference for " 
                   + Robot.toString(w) + ", instead got " + ref3, 
                   !ref1.equals(ref3));
    }

    private class DefaultStep extends Step {
        public DefaultStep(Resolver r) { super(r, new HashMap()); }
        public void runStep() { }
        public String getXMLTag() { return "defaultstep"; }
        public String getUsage() { return "blah"; }
        public String getDefaultDescription() {
            return "dummy step for " + getName();
        }
    }

    public void testLineReporting() {
        Script script = new Script(getHierarchy());
        Step step = new DefaultStep(script);
        script.addStep(step);
        assertEquals("Wrong file reported for first step",
                     script.getFile(), Script.getFile(step));
        assertEquals("Wrong line number for first step",
                     3, Script.getLine(step));
        
        HashMap map = new HashMap();
        ComponentReference cr1 =
            new ComponentReference(script, JButton.class, map);
        ComponentReference cr2 =
            new ComponentReference(script, JButton.class, map);
        assertEquals("Wrong line number after refs added",
                     5, Script.getLine(step));

        Sequence seq = new Sequence(script, "");
        script.addStep(seq);
        assertEquals("Wrong line number for added sequence",
                     6, Script.getLine(seq));

        step = new DefaultStep(script);
        seq.addStep(step);
        assertEquals("Wrong line number for step within sequence",
                     7, Script.getLine(step));

        seq = new Sequence(script, "");
        script.addStep(seq);
        assertEquals("Wrong line number for step after sequence",
                     9, Script.getLine(seq));

        step = new DefaultStep(script);
        script.addStep(step);
        assertEquals("Wrong line number for step after empty sequence",
                     10, Script.getLine(step));
    }

    public void testIDChangeDetection() {
        Script script = new Script(getHierarchy());
        JLabel label = new JLabel(getName());
        showFrame(label);
        ComponentReference ref = script.addComponent(label);
        assertEquals("Default id lookup failed with " + ref.getID(),
                     ref, script.getComponentReference(ref.getID()));

        ref.setAttribute(XMLConstants.TAG_ID, "goober");
        assertEquals("Changed ref id not detected: " + ref.getID(),
                     ref, script.getComponentReference(ref.getID()));
    }



    public void testBug1922380() {
        
        // Test script constructor with null parent
        // but relative path
        //
        
        Map attributes = new HashMap();
        attributes.put(Script.TAG_FILENAME, ".." + File.separatorChar + "fish.xml");
        
        Script s = new Script(
                       null, attributes);
        
    }

    /**
     * Smoke test all of the basic constructors
     */

    public void testOtherConstructors() {
        
        // With a Hierarchy
        //
        
        Script s = new Script( 
                       new AWTHierarchy());
        
        // With another script as parent
        //
        
        s = new Script(
                script, new HashMap());

        // With no script as parent
        //
        
        s = new Script(
                null, new HashMap());

        // With a file and parent
        
        s = new Script(
              "/odd/job/job.xml",
              new AWTHierarchy());
        
    }


    public ScriptTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptTest.class);
    }
}
