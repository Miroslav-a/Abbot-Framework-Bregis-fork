package abbot.util;

import junit.extensions.abbot.*;
import junit.framework.TestCase;

public class ThreadTerminatingSecurityManagerTest extends TestCase {

    public void testThreadsTerminated() {
        ThreadGroup group = new ThreadGroup("AUT Group") {
            public void uncaughtException(Thread t, Throwable thrown) {
            }
        };
        // Normally, this thread would never exit
        Thread t1 = new Thread(group, getName() + "1") {
            public void run() {
                while (true) {
                    currentThread().setContextClassLoader(null);
                    try { sleep(1000); } catch(InterruptedException ie) { }
                }
            }
        };
        t1.start();
        sm.terminateThreads(group);
        Timer timer = new Timer();
        while (t1.isAlive()) {
            if (timer.elapsed() > 5000)
                fail("Thread not terminated");
            try { Thread.sleep(100); }
            catch(InterruptedException e) { }
        }
    }

    private SecurityManager oldsm;
    private ThreadTerminatingSecurityManager sm;
    protected void setUp() {
        oldsm = System.getSecurityManager();
        sm = new ThreadTerminatingSecurityManager() {
            public void exitCalled(int status) {
            }
        };
        System.setSecurityManager(sm);
    }

    protected void tearDown() {
        System.setSecurityManager(oldsm);
    }

    public ThreadTerminatingSecurityManagerTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ThreadTerminatingSecurityManagerTest.class);
    }
}
