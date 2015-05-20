package abbot.tester;

import javax.swing.*;

import junit.extensions.abbot.*;

/** Unit test to verify the JSplitPaneTester class.<p> */

public class JSplitPaneTesterTest extends ComponentTestFixture {

    private JSplitPaneTester tester;
    private JSplitPane h, v;
    protected void setUp() {
        tester = new JSplitPaneTester();
        h = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        h.add(new JTree());
        h.add(new JTree());
        v = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        v.add(new JTree());
        v.add(new JTree());
        JPanel p = new JPanel();
        p.add(h);
        p.add(v);
        showFrame(p);
    }

    public void testMoveDividerVerticalMax() {
        tester.actionMoveDivider(h, 1.0);
        assertEquals("Vertical divider should be moved to max position",
                     h.getMaximumDividerLocation(),
                     h.getDividerLocation());
    }

    public void testMoveDividerHorizontalMax() {
        tester.actionMoveDivider(v, 1.0);
        assertEquals("Horizontal divider should be moved to max position",
                     v.getMaximumDividerLocation(),
                     v.getDividerLocation());
    }

    public void testMoveDividerVerticalCenter() {

        // Alter ordering as min/max value alters as the UI
        // updates making it hard to have the test past.
        int min = h.getMinimumDividerLocation();
        int max = h.getMaximumDividerLocation();
        int mid = (int)((max - min) / 2f + min);
        
        tester.actionMoveDivider(h, 0.5);
                
        assertEquals("Vertical divider should be moved to center: "
                     + h.getDividerLocation() + "/" 
                     + h.getMaximumDividerLocation(),
                     mid, h.getDividerLocation());
    }

    public void testMoveDividerHorizontalCenter() {

        // Alter ordering as min/max value alters as the UI
        // updates making it hard to have the test past.
        int min = v.getMinimumDividerLocation();
        int max = v.getMaximumDividerLocation();
        int mid = (int)((max - min) / 2f + min);

        tester.actionMoveDivider(v, 0.5);
        assertEquals("Horizontal divider should be moved to center: "
                     + v.getDividerLocation() + "/"
                     + v.getMaximumDividerLocation(),
                     mid, v.getDividerLocation());
    }

    public void testMoveDividerVerticalMin() {
        tester.actionMoveDivider(h, 1.0);
        tester.actionMoveDivider(h, 0.0);
        assertEquals("Vertical divider should be moved to min position",
                     h.getMinimumDividerLocation(), h.getDividerLocation());
    }

    public void testMoveDividerHorizontalMin() {
        tester.actionMoveDivider(v, 1.0);
        tester.actionMoveDivider(v, 0.0);
        assertEquals("Horizontal divider should be moved to min position",
                     v.getMinimumDividerLocation(), v.getDividerLocation());
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JSplitPaneTesterTest.class);
    }
}

