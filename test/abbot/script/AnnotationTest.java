package abbot.script;

import java.awt.*;
import javax.swing.JButton;

import abbot.finder.matchers.*;
import abbot.tester.ComponentTester;
import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;

public class AnnotationTest extends ResolverFixture {

    private Timer timer = new Timer();

    private class MyAnnotation extends Annotation {
        public volatile boolean isRunning;
        public MyAnnotation() {
            super(AnnotationTest.this.getResolver(), getName());
        }
        public void runStep() throws Throwable {
            isRunning = true;
            try {
                super.runStep();
            }
            finally {
                isRunning = false;
            }
        }
    }

    private Throwable stepFailure = null;
    // FIXME sporadic linux failure when run with full suite
    public void testShowUserDismiss() throws Throwable {
        final MyAnnotation ann = new MyAnnotation();
        ann.setTitle(getName());
        ann.setText("<html>This is a user-dismissed annotation.<br>It will remain until dismissed.</html>");
        ann.setUserDismiss(true);
        new Thread(getName()) {
            public void run() {
                try {
                    ann.run();
                }
                catch(Throwable thrown) { 
                    stepFailure = thrown;
                }
            }
        }.start();
        timer.reset();
        Window w = wait(ann);
        while (!w.isShowing()) {
            if (timer.elapsed() > 10000)
                fail("Annotation window never showed");
            getRobot().sleep();
        }
        while (w.isShowing()) {
            if (timer.elapsed() > ann.getDelayTime() + 1000)
                break;
            getRobot().sleep();
        }
        assertTrue("Annotation should not automatically close",
                   ann.isShowing());
        assertTrue("Annotation should block script execution while displaying",
                   ann.isRunning);
        ComponentTester tester = new ComponentTester();
        Component button =
            getFinder().find(w, new ClassMatcher(JButton.class));
        tester.actionClick(button);
        // Give the annotation thread time to clean up
        Timer timer = new Timer();
        while (ann.isShowing()) {
            getRobot().sleep();
            if (timer.elapsed() > 1000)
                fail("Annotation should be hidden after dismiss button pressed");
        }
        timer.reset();
        while (ann.isRunning) {
            getRobot().sleep();
            if (timer.elapsed() > 1000)
                fail("Annotation step should have finished");
        }
        assertEquals("No exception should be thrown",
                     (Throwable)null, stepFailure);
    }

    public void testShow() throws Throwable {
        final Annotation ann = new Annotation(getResolver(), getName());
        ann.setTitle(getName());
        ann.setText("<html>This is an <b>example</b> of an auto-closing annotation</html>");
        new Thread(getName()) { 
            public void run() {
                try { ann.run(); }
                catch(Throwable thr) { stepFailure = thr; }
            }
        }.start();
        timer.reset();
        Window w = wait(ann);
        while (!w.isShowing()) {
            if (timer.elapsed() > 10000) 
                fail("Annotation window never showed");
            getRobot().sleep();
        }
        while (w.isShowing()) {
            if (timer.elapsed() > ann.getDelayTime() + 1000)
                fail("Annotation window should have closed automatically");
            getRobot().sleep();
        }
        assertEquals("No exception should be thrown",
                     (Throwable)null, stepFailure);
    }

    private Window wait(Annotation ann) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < ann.getDelayTime()) {
            try {
                return (Window)getFinder().
                    find(new ClassMatcher(Annotation.AnnotationWindow.class) {
                        public boolean matches(Component c) {
                            return super.matches(c)
                                && ((Window)c).isShowing();
                        }
                    });
            }
            catch(Exception e) {
            }
        }
        throw new RuntimeException("Window never showed");
    }

    public AnnotationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, AnnotationTest.class);
    }
}

