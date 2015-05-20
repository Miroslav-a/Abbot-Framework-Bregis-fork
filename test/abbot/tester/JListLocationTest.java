package abbot.tester;

import java.awt.Point;

import junit.extensions.abbot.RepeatHelper;

import junit.framework.TestCase;

public class JListLocationTest extends TestCase {

    public void testParsePoint() {
        JListLocation loc = new JListLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new JListLocation(new Point(1,1)),
                     loc.parse(parse)); 
    }

    public void testParseRow() {
        JListLocation loc = new JListLocation();
        String parse = "[1]";
        assertEquals("Badly parsed: " + parse,
                     new JListLocation(1),
                     loc.parse(parse)); 
        parse = " [ 10 ] ";
        assertEquals("Badly parsed: " + parse,
                     new JListLocation(10),
                     loc.parse(parse)); 
    }

    public void testParseValue() {
        JListLocation loc = new JListLocation();
        String parse = "\"some value\"";
        assertEquals("Badly parsed: " + parse,
                     new JListLocation(parse.substring(1, parse.length()-1)),
                     loc.parse(parse)); 
    }

    public JListLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JListLocationTest.class);
    }
}
