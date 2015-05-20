package abbot.tester;

import java.awt.Point;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class ComponentLocationTest extends TestCase {

    public void testParsePoint() {
        ComponentLocation loc = new ComponentLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new ComponentLocation(new Point(1,1)),
                     loc.parse(parse)); 
        parse = " ( 10 , 20 ) ";
        assertEquals("Badly parsed: " + parse,
                     new ComponentLocation(new Point(10,20)),
                     loc.parse(parse)); 
    }

    public ComponentLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentLocationTest.class);
    }
}
