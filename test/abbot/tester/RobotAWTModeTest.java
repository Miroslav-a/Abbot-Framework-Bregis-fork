package abbot.tester;

import java.awt.*;
import java.awt.event.*;
import junit.extensions.abbot.*;
import junit.framework.*;


public class RobotAWTModeTest extends ComponentTestFixture {

    // avoid warning about no tests in the suite
    public void testStub() { }
    
    /** Wrap the standard robot tests in AWT mode. */
    private static class AWTMode extends TestSuite {
        public AWTMode(Class cls) { super(cls); }
        public void addTest(Test test) {
            // ignore drag/drop tests, which won't work
            if (test.toString().indexOf("Drag") == -1)
                super.addTest(test);
        }
        public void run(TestResult result) {
            int lastMode = Robot.getEventMode();
            Robot.setEventMode(Robot.EM_AWT);
            try {
                super.run(result);
            }
            finally {
                Robot.setEventMode(lastMode);
            }
        }
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(RobotAWTModeTest.class);
        // Only add in the AWT version of the basic robot tests if
        // we're in robot mode; otherwise we're just duplicating effort.
        if (Robot.getEventMode() == Robot.EM_ROBOT) {
            suite.addTest(new AWTMode(RobotTest.class));
            //suite.addTest(new AWTMode(RobotDragDropTest.class));
            suite.addTest(new AWTMode(RobotAppletTest.class));
        }
        return suite;
    }

    private int lastMode;
    /** All these tests run in AWT mode. */
    protected void setUp() {
        lastMode = Robot.getEventMode();
        Robot.setEventMode(Robot.EM_AWT);
    }

    /** Restore the robot event generation mode. */
    protected void tearDown() {
        Robot.setEventMode(lastMode);
    }

    /** Provide for repetitive testing on individual tests. */
    public static void main(String[] args) {
        RepeatHelper.runTests(args, RobotAWTModeTest.class);
    }
}
