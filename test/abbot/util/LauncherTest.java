package abbot.util;

import junit.framework.*;
import junit.extensions.abbot.*;

// TODO: need some form of auto-verification
public class LauncherTest extends TestCase {

    // only run these tests manually, since we can't control the results
    private static boolean run = false;

    public void testMailTo() throws Exception {
        if (run)
            Launcher.mail("email@address", "subject here & some",
                          "message body with escape chars <[%]>",
                          "copies@here", "blindcopies@here");
    }

    public void testBrowseTo() throws Exception {
        if (run)
            Launcher.open("http://abbot.sf.net");
    }

    public LauncherTest(String name) { super(name); }

    public static void main(String[] args) {
        run = true;
        TestHelper.runTests(args, LauncherTest.class);
    }
}
