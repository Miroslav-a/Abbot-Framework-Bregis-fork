package abbot.util;

import abbot.DynamicLoadingConstants;
import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class NonDelegatingClassLoaderTest extends TestCase implements DynamicLoadingConstants {

    public void testNoDelegation() throws Throwable {
        // Use a shared parent class loader that has the given class in its
        // path to ensure that the nondelegating loader loads the class and
        // not its parent.
        PathClassLoader pcl = new PathClassLoader(DYNAMIC_CLASSPATH,
                                                  getClass().getClassLoader());
        String className = DYNAMIC_CLASSNAME;
        ClassLoader cl = new NonDelegatingClassLoader(DYNAMIC_CLASSPATH, pcl);
        Class cls = Class.forName(className, true, cl);
        assertTrue("Class loader should define the class package (it is null)",
                   cls.getPackage() != null);
        assertEquals("Class loader defined the wrong package",
                     "test.dynamic", cls.getPackage().getName());

        Object obj = cls.newInstance();
        assertEquals("Wrong initial instance count on first load",
                     "1", obj.toString());

        cl = new NonDelegatingClassLoader(DYNAMIC_CLASSPATH, pcl);
        cls = Class.forName(className, true, cl);
        obj = cls.newInstance();
        assertEquals("Wrong initial instance count when not delegating",
                     "1", obj.toString());
    }

    private class DelegatingClassLoader extends NonDelegatingClassLoader {
        public DelegatingClassLoader(String path, ClassLoader parent) {
            super(path, parent);
        }
        protected boolean shouldDelegate(String cname) { return true; }
    }

    public void testDelegation() throws Throwable {
        String className = DYNAMIC_CLASSNAME;
        ClassLoader parent = new PathClassLoader(DYNAMIC_CLASSPATH,
                                                 getClass().getClassLoader());
        ClassLoader cl = new DelegatingClassLoader(DYNAMIC_CLASSPATH, parent);
        Class cls = Class.forName(className, true, cl);
        Object obj = cls.newInstance();
        assertEquals("Wrong initial instance count on first load",
                     "1", obj.toString());

        cl = new DelegatingClassLoader(DYNAMIC_CLASSPATH, parent);
        cls = Class.forName(className, true, cl);
        obj = cls.newInstance();
        assertEquals("Wrong instance count when delegating",
                     "2", obj.toString());
    }

    public NonDelegatingClassLoaderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, NonDelegatingClassLoaderTest.class);
    }
}
