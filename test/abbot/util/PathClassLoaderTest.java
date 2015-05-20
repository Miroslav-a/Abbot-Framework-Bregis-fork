package abbot.util;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;
import abbot.Platform;

public class PathClassLoaderTest extends TestCase {

    public void testConvertPathToFilesW32() {
        if (Platform.isWindows()) {
            String path = "c:\\;c:\\mydir;.";
            String[] names =
                PathClassLoader.convertPathToFilenames(path, ":;");
            String[] expected = {
                "c:\\", "c:\\mydir", "."
            };
            assertEquals("Wrong number of files",
                         expected.length, names.length);
            for (int i=0;i < names.length;i++) {
                assertEquals("Wrong path", expected[i], names[i]);
            }
        }
    }

    public void testConvertPathToFiles() {

        String path = "/:/tmp:/tmp/mydir:.";
        String[] names = PathClassLoader.convertPathToFilenames(path, ":;");
        String[] expected = {
            "/", "/tmp", "/tmp/mydir", "."
        };
        assertEquals("Wrong number of files",
                     expected.length, names.length);
        for (int i=0;i < names.length;i++) {
            assertEquals("Wrong path", expected[i], names[i]);
        }
    }

    /** We assume the existence of "lib/example.jar" and a resource
     * example/logo32.gif within it.
     */ 
    public void testLoadResource() throws Throwable {
        String pathName = "lib/example.jar";
        String rsrc = "/example/logo32.gif";
        String sysPath = System.getProperty("java.class.path");
        assertTrue("Can't test, path " + pathName
                   + " is already in classpath " + sysPath,
                   sysPath.indexOf(pathName) == -1);

        ClassLoader cl = new PathClassLoader(pathName,
                                             getClass().getClassLoader());
        try {
            cl.getResourceAsStream(rsrc);
        }
        catch(Exception exc) {
            fail("Resource not found: " + exc.getMessage());
        }
    }

    /** We assume the existence of "lib/example.jar" and
        example/FontChooser.class within it.
    */
    public void testLoadClassFromPath() throws Throwable {
        String pathName = "lib/example.jar";
        String className = "example.FontChooser";
        String sysPath = System.getProperty("java.class.path");
        assertTrue("Can't test, path " + pathName
                   + " is already in classpath " + sysPath,
                   sysPath.indexOf(pathName) == -1);
        ClassLoader cl = new PathClassLoader(pathName,
                                             getClass().getClassLoader());
        try {
            Class.forName(className, true, cl);
        }
        catch(ClassNotFoundException cnf) {
            fail("Path class loader failed to load " + className
                 + " from path " + pathName);
        }
    }

    public PathClassLoaderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, PathClassLoaderTest.class);
    }
}
