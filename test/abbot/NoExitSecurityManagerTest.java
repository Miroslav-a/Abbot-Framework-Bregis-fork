package abbot;

import javax.swing.JFrame;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class NoExitSecurityManagerTest extends TestCase {

    private SecurityManager oldsm;
    protected void setUp() {
        oldsm = System.getSecurityManager();
        System.setSecurityManager(new NoExitSecurityManager() {
            public void exitCalled(int code) {
            }
        });
    }

    protected void tearDown() {
        System.setSecurityManager(oldsm);
    }

    public void testExitPrevented() {
        try {
            System.exit(0);
        }
        catch(ExitException ee) {
            assertEquals("Wrong exit code", 0, ee.getStatus());
        }
        try {
            System.exit(1);
        }
        catch(ExitException ee) {
            assertEquals("Wrong exit code", 1, ee.getStatus());
        }
    }

    public void testNonExitAllowed() {
        JFrame frame = new JFrame(getName());
        try {
            // Somebody used to call checkExit on this
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            //Runtime.getRuntime().runFinalizersOnExit(true);
        }
        catch(SecurityException se) {
            fail("Code should be allowed to set the default frame operation.");
        }
    }

    public NoExitSecurityManagerTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, NoExitSecurityManagerTest.class);
    }
}
