package abbot.tester;

import java.awt.Point;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class JTabbedPaneLocationTest extends TestCase {

    public void testParsePoint() {
        JTabbedPaneLocation loc = new JTabbedPaneLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new JTabbedPaneLocation(new Point(1,1)),
                     loc.parse(parse)); 
    }

    public void testParseIndex() {
        JTabbedPaneLocation loc = new JTabbedPaneLocation();
        String parse = "[1]";
        assertEquals("Badly parsed: " + parse,
                     new JTabbedPaneLocation(1),
                     loc.parse(parse)); 
        parse = " [ 10 ] ";
        assertEquals("Badly parsed: " + parse,
                     new JTabbedPaneLocation(10),
                     loc.parse(parse)); 
    }

    public void testParseTabName() {
        JTabbedPaneLocation loc = new JTabbedPaneLocation();
        String expected = "tab name";
        String parse = "\"" + expected + "\"";
        assertEquals("Badly parsed: " + parse,
                     new JTabbedPaneLocation(expected), 
                     loc.parse(parse)); 
    }

    public JTabbedPaneLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, JTabbedPaneLocationTest.class);
    }
}
