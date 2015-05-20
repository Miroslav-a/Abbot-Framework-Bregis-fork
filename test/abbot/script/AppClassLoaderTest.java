package abbot.script;

import abbot.WaitTimedOutException;

import abbot.tester.Robot;

import javax.swing.SwingUtilities;

import junit.extensions.abbot.TestHelper;
import junit.framework.*;
import abbot.util.NonDelegatingClassLoader;
import java.lang.ref.WeakReference;

import java.util.concurrent.Callable;

import junit.extensions.abbot.ComponentTestFixture;

/**
 * Verify AppClassLoader operation.
 */

// Additional tests:
// A class loaded by both system and app loader should differ
// load from different class path
// load resource from different class path
// load from manifest class path in jar in class path
public class AppClassLoaderTest extends TestCase {

    private AppClassLoader acl;

    /** Ensure the class loader is GC'd after uninstall. */
    public void testGC() throws Exception {
        acl = new AppClassLoader();
        final WeakReference ref = new WeakReference(acl);
        class Flag { volatile boolean flag; }
        final Flag flag = new Flag();

        acl.install();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                flag.flag = acl.isEventDispatchThread();
            }
        });

        assertTrue("Custom event queue not installed", flag.flag);

        acl.uninstall();
        acl = null;

        // Might take more than one cc
        try
        {
            Robot.wait(new abbot.util.Condition()
                       {
                @Override
                public boolean test() {
                    System.gc();
                    AppClassLoader test = (AppClassLoader)ref.get();
                    return test==null;
                }
    
                @Override
                public String toString() {
                    return null;
                }
            }, 10000, 200);
        }
        catch (WaitTimedOutException wteo) {
            
        }
        
        acl = (AppClassLoader)ref.get();
        assertTrue("Class loader reference should be GC'd", acl == null);
    }

    /** Ensure the bootstrap classes are always loaded by the bootstrap
     * loader, except in cases where we specifically want preloading.
     */
    public void testAppletViewerExclusion() throws Throwable {
        // applet viewer should get the custom loader
        AppClassLoader dcl = new AppClassLoader();

        Class appletClass = Class.forName("sun.applet.AppletViewer", true, dcl);
        assertTrue("AppletViewer class loading should *not* be delegated",
                   appletClass.getClassLoader() instanceof NonDelegatingClassLoader);

        Class orbClass = Class.forName("org.omg.CORBA.ORB", true, dcl);
        assertTrue("org.omg class loading should always be delegated",
                   !(orbClass.getClassLoader() instanceof NonDelegatingClassLoader));

        Class sliderClass = Class.forName("javax.swing.JSlider", true, dcl);
        assertTrue("javax.swing class loading should always be delegated",
                   !(sliderClass.getClassLoader() instanceof NonDelegatingClassLoader));
    }

    private ClassLoader eventClassLoader = null;
    public void testContextInstallUninstall() throws Throwable {
        final AppClassLoader dcl = new AppClassLoader();
        ClassLoader original = getClass().getClassLoader();
        ClassLoader origContext =
            Thread.currentThread().getContextClassLoader();
        try {
            dcl.install();
            eventClassLoader = null;
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    eventClassLoader =
                        Thread.currentThread().getContextClassLoader();
                }
            });
            assertEquals("Context not installed on event thread",
                         dcl, eventClassLoader);
            dcl.uninstall();
            

            ComponentTestFixture.assertTrueEventually("Context not uninstalled from event thread",
                       new Callable<Boolean>()
                       {
                            @Override
                            public Boolean call() {
                                eventClassLoader =
                                    Thread.currentThread().getContextClassLoader();
                                return eventClassLoader != dcl;
                            }
                       });
            
            
            assertEquals("Original class loader not restored", 
                         getClass().getClassLoader(), original);
            assertEquals("Original context not restored", 
                         Thread.currentThread().getContextClassLoader(), origContext);
        }
        finally {
            // Make sure it's uninstalled in case something bad happened
            dcl.uninstall();
        }
    }

    public void testLoadAppletViewer() throws Throwable {
        String pathName = "lib/example.jar";
        String className = "sun.applet.AppletViewer";
        ClassLoader cl1 = new AppClassLoader(pathName);
        ClassLoader cl2 = new AppClassLoader(pathName);
        try {
            Class cls1 = Class.forName(className, true, cl1);
            Class cls2 = Class.forName(className, true, cl2);
            assertTrue("Class loader should define the class package",
                       cls1.getPackage() != null);
            assertEquals("Class loader defined the wrong package",
                         "sun.applet", cls1.getPackage().getName());

            Class cls3 = Class.forName(className);
            assertTrue("Core classes should be different when "
                       + "loaded dynamically", !cls1.equals(cls2));
            assertTrue("Class loader should never delegate AppletViewer class",
                       !cls3.equals(cls1) && !cls3.equals(cls2));
        }
        catch(ClassNotFoundException cnf) {
            fail("App class loader failed to load " + className
                 + " from path " + pathName);
        }
    }

    // this behavior results in too many complications; it's inevitably
    // impossible for the framework ensure it loads from the right class
    // loader at the right time, since the exact same classes are being loaded.
    /*
    public void testDirectBaseTesterLoading() throws Throwable {
        AppClassLoader dcl =
            new AppClassLoader("lib/abbot.jar");
        Class testerClass = 
            Class.forName("abbot.tester.ComponentTester", true, dcl);
        assertEquals("Custom class loader should be used to load a framework class when the framework is in the custom loader's class path",
                     dcl, testerClass.getClassLoader());
        Class compClass = Class.forName("javax.swing.JSlider", true, dcl);
        assertTrue("Component class should not be loaded by custom loader, "
                   + "not by " + dcl,
                   !dcl.equals(compClass.getClass().getClassLoader()));

        // A new reloading class loader should still get its own copy
        AppClassLoader dcl2 =
            new AppClassLoader("lib/abbot.jar");
        Class testerClass2 = 
            Class.forName("abbot.tester.ComponentTester", true, dcl2);
        assertEquals("Basic tester should be reloadable",
                     dcl2, testerClass2.getClassLoader());
        compClass = Class.forName("javax.swing.JSlider", true, dcl);
        assertTrue("Component class should not be loaded by custom loader, "
                   + "not by " + dcl2,
                   !dcl2.equals(compClass.getClass().getClassLoader()));

        // If the framework is *not* in the classpath, should always get the
        // same class.
        AppClassLoader dcl3 = new AppClassLoader("classes");
        Class testerClass3 =
            Class.forName("abbot.tester.ComponentTester", true, dcl3);
        assertTrue("Basic tester should be loaded from framework path, not by "
                   + dcl3,
                   !dcl3.equals(testerClass3.getClassLoader()));
    }
    */

    /** Load a custom tester and a tester and ensure the class hierarchy is
     * appropriate.
     */
    public void testExtensionTesterLoading() throws Throwable {
        AppClassLoader dcl =
            new AppClassLoader("lib/example.jar");
        Class customTesterClass = 
            Class.forName("abbot.tester.extensions.ArrowButtonTester",
                          true, dcl);
        ClassLoader ccl = customTesterClass.getClassLoader();
        ClassLoader fcl = getClass().getClassLoader();
        assertTrue("Class loader for extension tester should allow reloading: "
                   + ccl, ccl instanceof NonDelegatingClassLoader);

        assertTrue("Class loader for extension tester should differ from the "
                   + "framework class loader " + fcl,
                   !ccl.equals(fcl));

        assertEquals("Class loader for base tester class "
                     + "ancestor of the extension tester "
                     + "should match that of the test environment",
                     getClass().getClassLoader(),
                     customTesterClass.getSuperclass().getClassLoader());

        assertEquals("Actual base tester class ancestor "
                     + "of the extension should match that of "
                     + "the framework environment", 
                     abbot.tester.ComponentTester.class,
                     customTesterClass.getSuperclass());
    }

    /** Extensions not in the app loader class path but in the framework
     * class path should still be loaded by the AppClassLoader, not the system
     * class loader.
     */
    public void testExtensionsNotInTestCodeClassPath() throws Throwable {
        AppClassLoader dcl =
            new AppClassLoader("lib/example.jar");
        try {
            Class customTesterClass = 
                Class.forName("abbot.tester.extensions.DummyTester",
                              true, dcl);
            ClassLoader ccl = customTesterClass.getClassLoader();
            ClassLoader fcl = getClass().getClassLoader();

            assertTrue("Wrong class loader for custom tester",
                       ccl instanceof NonDelegatingClassLoader);
            assertTrue("Extension should not be found in the test code path",
                       !dcl.equals(ccl));

            // The parent class of the custom class should not come from the
            // app class loader
            assertEquals("Tester superclass should match the test environment",
                         fcl, 
                         customTesterClass.getSuperclass().getClassLoader());
        }
        catch(ClassNotFoundException cnf) {
            String msg = "The class loader should revert to java.class.path "
                + "if the extension is not found in the app class loader "
                + "classpath.  The extension was not found.";
            throw new AssertionFailedError(msg);
        }
    }

    public AppClassLoaderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, AppClassLoaderTest.class);
    }
}

