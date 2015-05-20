package abbot.finder;

import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.finder.matchers.*;

public class AWTHierarchyTest extends ComponentTestFixture {

    protected Hierarchy createHierarchy() { 
        return new TestHierarchy(); 
    }
    
    public void testFindScrollBars() throws Exception {
        JScrollPane pane = new JScrollPane(new JLabel(getName()));
        try {
            getFinder().find(pane, new ClassMatcher(JScrollBar.class));
        }
        catch(ComponentNotFoundException e) {
            fail("ScrollBar not reachable");
        }
    }

    public AWTHierarchyTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, AWTHierarchyTest.class);
    }
}
