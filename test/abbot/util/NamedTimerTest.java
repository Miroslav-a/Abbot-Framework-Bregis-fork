package abbot.util;

import java.util.*;
import junit.extensions.abbot.Timer;
import junit.extensions.abbot.TestHelper;
import junit.framework.*;

public class NamedTimerTest extends TestCase {

    private NamedTimer timer;
    protected void setUp() {
        timer = new NamedTimer(getName(), true) {
            protected void handleException(Throwable t) { }
        };
    }

    protected void tearDown() {
        timer.cancel();
    }

    public void testSetName() throws Exception {
        class Flag { volatile String name; }
        final Flag flag = new Flag();
        timer.schedule(new TimerTask() {
            public void run() {
                flag.name = Thread.currentThread().getName();
            }
        }, 0);
        Timer t = new Timer();
        while (flag.name == null) {
            if (t.elapsed() > 5000)
                fail("Task never ran");
            try { Thread.sleep(50); } catch(InterruptedException e) { }
            Thread.yield();
        }
        assertEquals("Thread not properly named", getName(), flag.name);
    }

    public void testExceptionThrowingTimerTask() throws Exception {
        class Flag { volatile boolean taskRan; }
        final Flag flag = new Flag();
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    throw new RuntimeException("Purposely throwing");
                }
                finally {
                    flag.taskRan = true;
                }
            }
        };
        timer.schedule(task, 0);
        Timer t = new Timer();
        while (!flag.taskRan) {
            if (t.elapsed() > 5000)
                fail("Task never ran");
            Thread.yield();
        }
        // This will throw an exception if the Timer was canceled
        timer.schedule(new TimerTask() { public void run() { }}, 0);
    }

    public void testCancelTask() throws Exception {
        class Flag { volatile boolean taskRan; }
        final Flag flag = new Flag();
        TimerTask task = new TimerTask() {
            public void run() {
                flag.taskRan = true;
            }
        };
        timer.schedule(task, 100);
        task.cancel();
        Thread.sleep(200);
        assertTrue("Task should not have run", !flag.taskRan);
    }

    public NamedTimerTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, NamedTimerTest.class);
    }
}
