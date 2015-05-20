package abbot.util;

import java.awt.EventQueue;
import java.util.Properties;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class EventDispatchExceptionHandlerTest extends TestCase {

    private Runnable empty = new Runnable() { public void run(){ } };

    private Properties props;

    /** Preserve System properties across invocations. */
    protected void setUp() {
        props = (Properties)System.getProperties().clone();
    }

    /** Preserve System properties across invocations. */
    protected void tearDown() {
        System.setProperties(props);
    }

    public void testInstall() {
        try {
            new Catcher().install();
        }
        catch(RuntimeException e) {
            assertTrue("Handler could not be installed",
                       Catcher.isInstalled());
            throw e;
        }
    }

    /** Ensure we can install an instance and catch an exception. */
    public void testCatchException() throws Throwable {
        new Catcher().install();
        Catcher.throwable = null;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                throw new RuntimeException("Test exception for " + getName());
            }
        });
        EventQueue.invokeAndWait(empty);
        assertNotNull("No exception caught", Catcher.throwable);
    }

//    /** Ensure subsequently set handlers will get called. */
//    public void testForwardException() throws Throwable {
//        // ensure the standard handler is always installed first
//        new Catcher().install();
//        Catcher.throwable = null;
//        SampleCatcher.throwable = null;
//        System.setProperty(EventDispatchExceptionHandler.PROP_NAME,
//                           SampleCatcher.class.getName());
//        EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                throw new RuntimeException("Test exception for " + getName());
//            }
//        });
//        EventQueue.invokeAndWait(empty);
//        // Only one handler can be installed at a time, so if there was
//        // already a handler installed, this handler won't see the
//        // exception. 
//        //assertNotNull("No exception caught", Catcher.throwable);
//        assertNotNull("Exception not forwarded", SampleCatcher.throwable);
//    }
    
    public EventDispatchExceptionHandlerTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, EventDispatchExceptionHandlerTest.class);
    }

    public static class Catcher extends EventDispatchExceptionHandler {
        public static Throwable throwable = null;
        public void exceptionCaught(Throwable thr) {
            throwable = thr;
        }
    }

    public static class SampleCatcher {
        public static Throwable throwable = null;
        public void handle(Throwable thr) {
            throwable = thr;
        }
    }
}
