package abbot;

import java.util.*;
import java.io.File;
import junit.framework.*;
import junit.extensions.abbot.TestHelper;

public class UnitTestSuite extends junit.framework.TestSuite {

    private static TestSuite createSuite(Class cls) {
        try {
            java.lang.reflect.Method suiteMethod =
                cls.getMethod("suite", null);
            return (TestSuite)suiteMethod.invoke(null, null);
        }
        catch(Exception e) {
            return new TestSuite(cls);
        }
    }

    private static Map findTestClasses(File file, String name) {
        if ("test".equals(name))
            return new HashMap();

        Map map = new HashMap();
        File[] files = file.listFiles();
        if (files == null) {
            if (name.endsWith("Test.class")) {
                String className = name.substring(0, name.length()-6);
                try {
                    map.put(className, Class.forName(className));
                }
                catch(ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            for (int i=0;i < files.length;i++) {
                map.putAll(findTestClasses(files[i],
                                           "".equals(name)
                                           ? files[i].getName()
                                           : name + "." + files[i].getName()));
            }
        }
        return map;
    }

    public static Test suite() {
        String filter = System.getProperty("abbot.test.filter");
        if (filter == null)
            filter = "";

        // Any classes which should be run first or in some particular order
        // should be put here.  Otherwise test case suites are run in the
        // order the classes are added. 
        String[] priority = new String[] {
            "abbot.util.",
            "abbot.tester.WindowTrackerTest",
            "junit.extensions.abbot.",
            "abbot.tester.RobotTest",
            "abbot.tester.RobotAWTModeTest",
            "abbot.tester.RobotDragDropTest",
            "abbot.tester.RobotAppletTest",
            "abbot.tester.ComponentTesterTest",
            "abbot.tester.",
        };
        // This group has some strange interactions...
        /*
          abbot.editor.ComponentBrowserTest.class, //
          abbot.editor.ComponentNodeTest.class, //
          abbot.editor.ComponentTreeTest.class, //
          abbot.editor.recorder.JInternalFrameRecorderTest.class, //
          abbot.finder.BasicFinderTest.class, //
          abbot.tester.JInternalFrameTesterTest.class, //
        */
        // Now add everything else
        File file = new File(System.getProperty("user.dir")
                             + File.separator + "build"
                             + File.separator + "test-classes");
        TestSuite suite = new TestSuite();
        Map scanned = new HashMap();
        scanned.putAll(findTestClasses(file, ""));
        for (Iterator i=scanned.keySet().iterator();i.hasNext();) {
            String name = (String)i.next();
            if (name.indexOf(filter) == -1) {
                i.remove();
            }
        }

        for (int i=0;i < priority.length;i++) {
            String name = priority[i];
            if (scanned.containsKey(name)) {
                suite.addTest(createSuite((Class)scanned.get(name)));
                scanned.remove(name);
            }
            else {
                for (Iterator iter=scanned.keySet().iterator();iter.hasNext();) {
                    String cname = (String)iter.next();
                    if (cname.startsWith(name)) {
                        suite.addTest(createSuite((Class)scanned.get(cname)));
                        iter.remove();
                    }
                }
            }
        }

        for (Iterator i=scanned.values().iterator();i.hasNext();) {
            suite.addTest(createSuite((Class)i.next()));
        }
        return suite;
    }

    public UnitTestSuite(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, UnitTestSuite.class);
    }
}
