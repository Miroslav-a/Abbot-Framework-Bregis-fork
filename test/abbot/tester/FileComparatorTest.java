package abbot.tester;

import java.io.*;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

/** Test FileComparator. */

public class FileComparatorTest extends TestCase {

    private FileComparator fc;

    private File generateFile(String contents, int length) throws Throwable {
        File file = File.createTempFile(getName(), null);
        file.deleteOnExit();
        FileOutputStream os = new FileOutputStream(file);
        while (length > 0) {
            if (length > contents.length()) {
                os.write(contents.getBytes());
                length -= contents.length();
            }
            else {
                os.write(contents.getBytes(), 0, length);
                length = 0;
            }
        }
        os.close();
        return file;
    }

    protected void setUp() {
        fc = new FileComparator();
    }

    public void testCompareWithNull() throws Throwable {
        File f1 = generateFile("a", 10);
        File f2 = null;
        assertTrue("Shouldn't match null", fc.compare(f1, f2) > 0);
        assertTrue("Shouldn't match null", fc.compare(f2, f1) < 0);
    }

    public void testCompareSameFiles() throws Throwable {
        File f1 = generateFile("a", 10);
        File f2 = f1;
        assertEquals("Same files, a==b", 0, fc.compare(f1, f2));
        assertEquals("Same files, b==a", 0, fc.compare(f2, f1));
    }

    public void testCompareIdenticalFiles() throws Throwable {
        File f1 = generateFile("A", 10);
        File f2 = generateFile("A", 10);
        assertEquals("Identical files, a==b", 0, fc.compare(f1, f2));
        assertEquals("Identical files, b==a", 0, fc.compare(f2, f1));
    }

    public void testCompareDifferentFiles() throws Throwable {
        File f1 = generateFile("A", 10);
        File f2 = generateFile("A", 20);
        assertTrue("Differing files, a < b", fc.compare(f1, f2) < 0);
        assertTrue("Differing files, a > b", fc.compare(f2, f1) > 0);

        File f3 = generateFile("B", 10);
        assertTrue("Differing files, a < b", fc.compare(f1, f3) < 0);
        assertTrue("Differing files, a > b", fc.compare(f3, f1) > 0);
    }

    public void testComparNonFiles() {
        File f1 = new File("tmp.txt");
        Object obj = new Object();
        try {
            fc.compare(f1, obj);
            fail("Should have thrown an exception");
        }
        catch(IllegalArgumentException iae) {
        }
        try {
            fc.compare(obj, f1);
            fail("Should have thrown an exception");
        }
        catch(IllegalArgumentException iae) {
        }
    }

    /** Create a new test case with the given name. */
    public FileComparatorTest(String name) {
        super(name);
    }

    /** Return the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, FileComparatorTest.class);
    }
}
