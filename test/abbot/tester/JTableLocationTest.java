package abbot.tester;

import java.awt.Point;
import javax.swing.JTable;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class JTableLocationTest extends TestCase {

    public void testParsePoint() {
        JTableLocation loc = new JTableLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new JTableLocation(new Point(1,1)),
                     loc.parse(parse)); 
    }

    public void testParseCell() {
        JTableLocation loc = new JTableLocation();
        String parse = "[1,2]";
        assertEquals("Badly parsed: " + parse,
                     new JTableLocation(1,2),
                     loc.parse(parse)); 
        parse = " [ 10 , 20 ] ";
        assertEquals("Badly parsed: " + parse,
                     new JTableLocation(10, 20),
                     loc.parse(parse)); 
    }

    public void testParseValue() {
        JTableLocation loc = new JTableLocation();
        String parse = "\"some value\"";
        assertEquals("Badly parsed: " + parse,
                     new JTableLocation(parse.substring(1, parse.length()-1)),
                     loc.parse(parse)); 
    }

    public void testLookupNonexistentValue() {
        JTableLocation loc = new JTableLocation("green");
        try {
            String[][] data = new String[][] {
                { "0 one", "0 two", },
                { "1 one", "1 two", },
            };
            String[] names = { "one", "two", };
            loc.getPoint(new JTable(data, names));
            fail("Exception should be thrown when value does not exist");
        }
        catch(LocationUnavailableException e) {
        }
    }

    public void testLookupNonexistentCell() {
        JTableLocation loc = new JTableLocation(10, 10);
        try {
            String[][] data = new String[][] {
                { "0 one", "0 two", },
                { "1 one", "1 two", },
            };
            String[] names = { "one", "two", };
            loc.getPoint(new JTable(data, names));
            fail("Exception should be thrown when cell does not exist");
        }
        catch(LocationUnavailableException e) {
        }
    }

    public JTableLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, JTableLocationTest.class);
    }
}
