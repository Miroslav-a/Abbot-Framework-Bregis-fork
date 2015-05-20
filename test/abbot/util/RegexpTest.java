package abbot.util;

import junit.framework.*;
import junit.extensions.abbot.*;

public class RegexpTest extends TestCase {

    public void testMultiLineMatch() {
        assertTrue("Multi-line match failed, LF",
                   Regexp.stringMatch("(?m)Bangalore.*India",
                                      "Bangalore\nIndia"));
        assertTrue("Multi-line match failed, CRLF",
                   Regexp.stringMatch("(?m)Bangalore.*India",
                                      "Bangalore\r\nIndia"));
        assertFalse("Shouldn't multi-line match w/o trigger, LF",
                    Regexp.stringMatch("^Bangalore.*India",
                                       "Bangalore\nIndia"));
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, RegexpTest.class);
    }
}
