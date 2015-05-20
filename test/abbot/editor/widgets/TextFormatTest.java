package abbot.editor.widgets;

import junit.framework.*;
import junit.extensions.abbot.*;

public class TextFormatTest extends TestCase {

    public void testWordBreak() {
        String[][] words = {
            { "Some", "Some" },
            { "SomeMore", "Some More" },
            { "andAnother", "and Another" },
            { "AAAMotorClub", "AAA Motor Club" },
        };
        for (int i=0;i < words.length;i++) {
            assertEquals("Didn't break up '" + words[i][0] + "'",
                         words[i][1], TextFormat.wordBreak(words[i][0]));
        }
    }

    public void testLongLine() {
        String original = "onenonwrap twononwrap";
        String expected = "onenonwrap<br>twononwrap";
        assertEquals("Improper long-line wrapping",
                     expected, TextFormat.wordWrap(original, 5, "<br>"));
    }

    public void testTrimLeadingWhitespace() {
        String original = "  line 1 and      line 2";
        String expected = "line 1 and<br>line 2";
        assertEquals("Should strip whitespace from BOL",
                     expected, TextFormat.wordWrap(original, 10, "<br>"));
    }

    public void testWordWrap() {
        String original = "The current JVM release incorrectly generates events for mouse buttons 2 and 3.  There is no workaround.  Please file a bug with Apple at http://www.bugreporter.apple.com.";
        String expected = "The current JVM release incorrectly generates events for<br>mouse buttons 2 and 3.  There is no workaround.  Please file<br>a bug with Apple at http://www.bugreporter.apple.com.";
        assertEquals("Bad word wrapping", expected,
                     TextFormat.wordWrap(original,
                                         TextFormat.DIALOG_WRAP,
                                         "<br>"));
    }
    
    /** Construct a test case with the given name. */
    public TextFormatTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, TextFormatTest.class);
    }
}
