package abbot.tester;

import java.awt.event.*;
import javax.swing.KeyStroke;

import junit.framework.*;
import junit.extensions.abbot.*;

public class MapGeneratorTest extends TestCase {

    private static final int NONE = 0;
    private static final int SHIFT = KeyEvent.SHIFT_MASK;
    // NOTE: these mappings are only valid for the en_US locale
    private static final Object[][] mappings = {
        {new Character('a'), KeyStroke.getKeyStroke(KeyEvent.VK_A, NONE)},
        {new Character('A'), KeyStroke.getKeyStroke(KeyEvent.VK_A, SHIFT)},
        {new Character('g'), KeyStroke.getKeyStroke(KeyEvent.VK_G, NONE)},
        {new Character('G'), KeyStroke.getKeyStroke(KeyEvent.VK_G, SHIFT)},
        {new Character('k'), KeyStroke.getKeyStroke(KeyEvent.VK_K, NONE)},
        {new Character('K'), KeyStroke.getKeyStroke(KeyEvent.VK_K, SHIFT)},
        {new Character('z'), KeyStroke.getKeyStroke(KeyEvent.VK_Z, NONE)},
        {new Character('Z'), KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHIFT)},
        {new Character('0'), KeyStroke.getKeyStroke(KeyEvent.VK_0, NONE)},
        {new Character('1'), KeyStroke.getKeyStroke(KeyEvent.VK_1, NONE)},
        {new Character('2'), KeyStroke.getKeyStroke(KeyEvent.VK_2, NONE)},
        {new Character('3'), KeyStroke.getKeyStroke(KeyEvent.VK_3, NONE)},
        {new Character('4'), KeyStroke.getKeyStroke(KeyEvent.VK_4, NONE)},
        {new Character('5'), KeyStroke.getKeyStroke(KeyEvent.VK_5, NONE)},
        {new Character('6'), KeyStroke.getKeyStroke(KeyEvent.VK_6, NONE)},
        {new Character('7'), KeyStroke.getKeyStroke(KeyEvent.VK_7, NONE)},
        {new Character('8'), KeyStroke.getKeyStroke(KeyEvent.VK_8, NONE)},
        {new Character('9'), KeyStroke.getKeyStroke(KeyEvent.VK_9, NONE)},
        {new Character('/'), KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, NONE)},
        {new Character('?'), KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, SHIFT)},
        {new Character('`'), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, NONE)},
        
    // Removed this test as it moved around just a little bit too much on different platforms    
    //    {new Character('~'), KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, SHIFT)},
    };

    public void testCharacterLookup() {
        for (int m=0;m < mappings.length;m++) {
            KeyStroke ks = (KeyStroke)mappings[m][1];
            assertEquals("wrong character for '" + mappings[m][1] + "'",
                         mappings[m][0],
                         new Character(KeyStrokeMap.getChar(ks)));
        }
    }

    public void testKeyCodeLookup() {
        for (int m=0;m < mappings.length;m++) {
            Character ch = (Character)mappings[m][0];
            assertEquals("wrong KeyStroke for '" + mappings[m][0] + "'",
                         mappings[m][1],
                         KeyStrokeMap.getKeyStroke(ch.charValue()));
        }
    }

    public MapGeneratorTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, MapGeneratorTest.class);
    }
}
