package abbot.tester.extensions;

import java.awt.Point;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class JGraphLocationTest extends TestCase {

    public void testParsePoint() {
        JGraphLocation loc = new JGraphLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new JGraphLocation(new Point(1,1)),
                     loc.parse(parse)); 
    }

    public void testParseRow() {
        JGraphLocation loc = new JGraphLocation();
        String parse = "[1]";
        assertEquals("Badly parsed: " + parse,
                     new JGraphLocation(1),
                     loc.parse(parse)); 
        parse = " [ 10 ] ";
        assertEquals("Badly parsed: " + parse,
                     new JGraphLocation(10),
                     loc.parse(parse)); 
    }

    /*
    public void testParseValue() {
        JGraphLocation loc = new JGraphLocation();
        String parse = "\"some value\"";
        assertEquals("Badly parsed: " + parse,
                     new JGraphLocation(parse.substring(1, parse.length()-1)),
                     loc.parse(parse)); 
    }
    */

    public JGraphLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, JGraphLocationTest.class);
    }
}
