package junit.extensions.abbot;

import abbot.script.XMLConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import java.io.IOException;

import java.util.*;

import junit.framework.*;

/** 
 * Verify we can extend a ScriptTestSuite.
 */
public class ScriptTestSuiteTest extends TestCase {

    private static int runs;
    public static class MyScriptFixture extends ScriptFixture {
        public MyScriptFixture(String filename) {
            super(filename);
        }
        protected void runTest() throws Throwable {
            ++runs;
        }
    }

    /** Ensure that a suite generated with a derived class actually
        instantiates those classes.
    */
    public void testDerivedClass() throws Throwable {
        runs = 0;
        Test suite = new ScriptTestSuite(MyScriptFixture.class, "src/example");
        suite.run(new TestResult());
        assertTrue("Derived test fixture class was never run", runs != 0);
    }

    /** Create a few temp files, the test suite should pick them up. */
    public void testGenerateFilenames() throws Throwable {
        File tmp = File.createTempFile(getName(), "-dummy");
        File tmpdir = tmp.getParentFile();
        tmp.delete();
        File[] files = tmpdir.listFiles();
        for (int i=0;i < files.length;i++) {
            if (files[i].getName().startsWith(getName())
                && files[i].getName().endsWith(".tst")) {
                files[i].delete();
            }
        }

        File f1 = File.createTempFile(getName(), ".tst");
        writeString(f1);
        f1.deleteOnExit();
        File f2 = File.createTempFile(getName(), ".tst");
        writeString(f2);
        f2.deleteOnExit();
        File f3 = File.createTempFile(getName(), ".tst");
        writeString(f3);
        f3.deleteOnExit();

        String[] flist = ScriptTestSuite.
            findFilenames(f1.getParentFile().getAbsolutePath(), false);
        List list = new ArrayList();
        for (int i=0;i < flist.length;i++) {
            String fn = flist[i];
            if (fn.indexOf(getName()) != -1 && fn.endsWith(".tst")) {
                list.add(fn);
            }
        }
        assertEquals("Wrong number of files", 3, list.size());
        assertTrue("Missing f1", list.contains(f1.getAbsolutePath()));
        assertTrue("Missing f2", list.contains(f2.getAbsolutePath()));
        assertTrue("Missing f3", list.contains(f3.getAbsolutePath()));
    }
    
    
    private void writeString(File f) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        try {
            fos.write(XMLConstants.TAG_AWTTESTSCRIPT.getBytes("UTF-8"));
        }
        finally {
            fos.close();
        }
            
    }
    

    public ScriptTestSuiteTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptTestSuiteTest.class);
    }
}
