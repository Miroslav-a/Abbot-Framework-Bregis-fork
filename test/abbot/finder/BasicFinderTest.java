package abbot.finder;

import javax.swing.*;
import junit.extensions.abbot.*;

/** Check version settings, and platform or JVM-specific bugs. */
public class BasicFinderTest extends ComponentTestFixture {

    public void testFindIconifiedJInternalFrame() throws Exception {
        JDesktopPane p = new JDesktopPane();
        JInternalFrame f = new JInternalFrame(getName());
        p.add(f);
        f.setIcon(true);
        showFrame(p);
        getFinder().find(new ComponentMatcher(f));
    }

    public void testPopupReachableInHierarchy() throws Exception {
        final JPopupMenu light = new JPopupMenu();
        JMenuItem mi1, mi2;
        light.add(mi1 = new JMenuItem("item"));
        final JPopupMenu heavy = new JPopupMenu();
        heavy.add(mi2 = new JMenuItem("item 1"));
        heavy.add(new JMenuItem("item 2"));
        heavy.add(new JMenuItem("item 3"));
        heavy.add(new JMenuItem("item 4"));
        heavy.add(new JMenuItem("item 5"));
        heavy.add(new JMenuItem("item 6"));
        final JLabel label1 = new JLabel("light");
        final JLabel label2 = new JLabel("heavy");
        installPopup(label1, light);
        installPopup(label2, heavy);

        JPanel pane = new JPanel();
        pane.add(label1);
        pane.add(label2);
        showFrame(pane);
        getRobot().showPopupMenu(label1);
        Matcher lightweightMatcher = new ComponentMatcher(light);
        getFinder().find(lightweightMatcher);

        getRobot().click(mi1);
        getRobot().waitForIdle();
        try {
            getFinder().find(lightweightMatcher);
            fail("Should not find lightweight popup in hierarchy after it is hidden");
        }
        catch(ComponentNotFoundException e) {
        }

        getRobot().showPopupMenu(label2);
        Matcher heavyMatcher = new ComponentMatcher(heavy);
        getFinder().find(heavyMatcher);
        getRobot().click(mi2);
        getRobot().waitForIdle();
        try {
            getFinder().find(heavyMatcher);
            fail("Should not find heavyweight popup in hierarchy after it is hidden");
        }
        catch(ComponentNotFoundException e) {
        }
    }

    public BasicFinderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, BasicFinderTest.class);
    }
}
