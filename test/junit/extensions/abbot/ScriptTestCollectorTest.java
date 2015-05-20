package junit.extensions.abbot;

import junit.framework.*;
import java.util.*;
import java.io.File;
import abbot.util.PathClassLoader;

public class ScriptTestCollectorTest extends TestCase {

    public void testListClasses() {
        String[] required = {
            "build/abbot.jar",
            "build/example.jar",
        };
        String path = "";
        String PS = "";
        for (int i=0;i < required.length;i++) {
            assertTrue("Missing file: " + required[i],
                       new File(required[i]).exists());
            path += PS + required[i];
            PS = System.getProperty("path.separator");
        }
        ScriptTestCollector tc =
            new ScriptTestCollector(new PathClassLoader(path));
        ArrayList list = new ArrayList();
        Enumeration en = tc.collectTests();
        while (en.hasMoreElements()) {
            list.add(en.nextElement());
        }
        assertTrue("Some tests should have been found", list.size() > 0);
        assertFalse("Should ignore framework classes",
                    list.contains("junit.extensions.abbot.ScriptFixture"));
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptTestCollectorTest.class);
    }
}
