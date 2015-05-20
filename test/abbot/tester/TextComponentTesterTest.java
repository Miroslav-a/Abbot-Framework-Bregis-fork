package abbot.tester;

import java.awt.TextField;
import java.awt.TextComponent;

import junit.extensions.abbot.*;

/** Unit test to verify the TextComponentTester class.<p> */

public class TextComponentTesterTest extends ComponentTestFixture {

    private TextComponentTester tester;
    private TextComponent tc;
    protected void setUp() {
        TextField tf = new TextField();
        tf.setColumns(10);
        tc = tf;
        tester = new TextComponentTester();
    }

    /** Wholesale text replacement by typing. */
    // FIXME sporadic w32 misses first character
    public void testSetText() {
        showFrame(tc);
        String text = "short";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong short text typed,", text, tc.getText());
        text = "longer";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong replacement text,",
                     text, tc.getText());
        text = "Some longer text that will surely exceed the field width";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong long replacement text,",
                     text, tc.getText());
        text = "shorter";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong shorter replacement text,",
                     text, tc.getText());

    }

    /** Verify we can select arbitrary bits of text. */
    public void testSelect() {
        showFrame(tc);
        String text = "short";
        tc.setText(text);
        tester.waitForIdle();
        tester.actionSelectText(tc, 0, text.length());
        assertEquals("Wrong selection start (full)",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end (full)",
                     text.length(), tc.getSelectionEnd());

        tester.actionSelectText(tc, 1, text.length()-1);
        assertEquals("Wrong selection start (mid)",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end (mid)",
                     text.length()-1, tc.getSelectionEnd());
    }
    
    /** Select text that goes beyond the visible display area. */
    public void testLongSelection() {
        showFrame(tc);
        String text = "The quick brown fox jumped over the lazy dog";
        tc.setText(text);
        tester.waitForIdle();
        tester.actionSelectText(tc, 0, text.length());
        assertEquals("Wrong selection start (full)",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end, end hidden (full)",
                     text.length(), tc.getSelectionEnd());
        tester.actionSelectText(tc, 1, text.length()-1);
        assertEquals("Wrong selection start, start hidden, (mid)",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end (mid)",
                     text.length()-1, tc.getSelectionEnd());
    }

    /** Create a new test case with the given name. */
    public TextComponentTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, TextComponentTesterTest.class);
    }
}

