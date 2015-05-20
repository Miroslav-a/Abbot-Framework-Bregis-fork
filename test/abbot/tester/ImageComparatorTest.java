package abbot.tester;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.*;

import junit.extensions.abbot.*;
import junit.framework.TestCase;
/** Test ImageComparator. */

public class ImageComparatorTest extends TestCase {

    private ComponentTester tester;

    protected void setUp() {
        tester = new ComponentTester();
    }

    public void testImageCompare() throws Throwable {

        File gif = new File("test/abbot/tester/image.png");
        ImageIcon icon = new ImageIcon(gif.toURL());
        ImageIcon icon2 = new ImageIcon(gif.toURL());
        
        ImageComparator ic = new ImageComparator();
        assertTrue("Images should not differ", 
                   ic.compare(icon.getImage(), icon2.getImage()) == 0);
    }

    /** Return the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ImageComparatorTest.class);
    }
}
