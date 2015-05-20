package abbot.editor.widgets;

import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;

import junit.framework.*;
import junit.extensions.abbot.*;

import abbot.i18n.Strings;

public class MnemonicTest extends TestCase {

    public void testGetMnemonic() throws Exception {
        Properties p = new Properties();
        p.setProperty("MNEMONIC_\u00d6", "O");
        p.store(new FileOutputStream("build/test-classes/abbot/editor/widgets/mnemonics.properties"), "test");
        Strings.addBundle("abbot.editor.widgets.mnemonics");
        Object[][] strings = {
            { "None", "None",
              new Integer(KeyEvent.VK_UNDEFINED), new Integer(-1) },
            { "&File", "File",
              new Integer(KeyEvent.VK_F), new Integer(0) },
            { "&lower", "lower",
              new Integer(KeyEvent.VK_L), new Integer(0) },
            { "Last&", "Last&",
              new Integer(KeyEvent.VK_UNDEFINED), new Integer(-1) },
            { "&TooMan&y", "TooMan&y",
              new Integer(KeyEvent.VK_T), new Integer(0) },
            { "About &Al", "About Al",
              new Integer(KeyEvent.VK_A), new Integer(6) },
            { "Rock && Roll", "Rock & Roll",
              new Integer(KeyEvent.VK_UNDEFINED), new Integer(-1) },
            { "&Rock && Roll", "Rock & Roll",
              new Integer(KeyEvent.VK_R), new Integer(0) },
            { "Rock && &Roll", "Rock & Roll",
              new Integer(KeyEvent.VK_R), new Integer(7) },
            { "Rock & Roll", "Rock & Roll",
              new Integer(KeyEvent.VK_UNDEFINED), new Integer(-1) },
            { "Sample '&' &character", "Sample '&' character",
              new Integer(KeyEvent.VK_C), new Integer(11) },
            { "Chara&cteristic '&'", "Characteristic '&'",
              new Integer(KeyEvent.VK_C), new Integer(5) },
            // Looked up via localized mapping
            { "Unicode &\u00d6", "Unicode \u00d6",
              new Integer(KeyEvent.VK_O), new Integer(8) },
            // Looked up via keymap
            { "Symbol&=", "Symbol=",
              new Integer(KeyEvent.VK_EQUALS), new Integer(6) },
        };
        for (int i=0;i < strings.length;i++) {
            String str = (String)strings[i][0];
            Mnemonic mnemonic = Mnemonic.getMnemonic(str);
            assertEquals("Incorrect label for '" + str + "'",
                         strings[i][1], mnemonic.text);
            if (Mnemonic.useMnemonics()) {
                assertEquals("Incorrect mnemonic keycode for '" + str + "'",
                             strings[i][2], new Integer(mnemonic.keycode));
                assertEquals("Incorrect display index for '" + str + "'",
                             strings[i][3], new Integer(mnemonic.index));
            }
        }
    }

    /** Construct a test case with the given name. */
    public MnemonicTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, MnemonicTest.class);
    }
}
