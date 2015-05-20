package abbot.tester;

import java.awt.Dimension;

import javax.swing.*;

import junit.extensions.abbot.*;
import junit.framework.*;

/** Unit test to verify the JTabbedPaneTester class.<p> */

public class JTabbedPaneTesterTest extends ComponentTestFixture {

    private JTabbedPaneTester tester;
    private JTabbedPane tabbedPane;
    private final int MAX = 10;

    /** Create a new test case with the given name. */
    public JTabbedPaneTesterTest(String name) {
        super(name);
    }

    protected void setUp() {
        tester = (JTabbedPaneTester)
            ComponentTester.getTester(JTabbedPane.class);
        tabbedPane = new JTabbedPane();
        for (int i=0;i < MAX;i++) {
            tabbedPane.addTab("Tab " + i, new JLabel("Pane #" + i) {
                public Dimension getPreferredSize() {
                    return new Dimension(300,175);
                }
            });
        }
    }

    public void testSelectTabByIndex() {
        showFrame(tabbedPane);
        for (int i=0;i < MAX;i++) {
            tester.actionSelectTab(tabbedPane, new JTabbedPaneLocation(i));
            assertEquals("Wrong tab selected", i, 
                         tabbedPane.getSelectedIndex());
        }
    }

    public void testSelectTabByName() {
        showFrame(tabbedPane);
        for (int i=0;i < MAX;i++) {
            String title = "Tab " + i;
            tester.actionSelectTab(tabbedPane, new JTabbedPaneLocation(title));
            assertEquals("Wrong tab selected",
                         title, 
                         tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
        }
    }
        
    /** Return the default test suite. */
    public static Test suite() {
        return new TestSuite(JTabbedPaneTesterTest.class);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTabbedPaneTesterTest.class);
    }
}

