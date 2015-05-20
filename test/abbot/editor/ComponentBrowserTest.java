package abbot.editor;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import junit.extensions.abbot.*;
import abbot.finder.matchers.*;

public class ComponentBrowserTest extends ResolverFixture {

    private ComponentBrowser browser;
    protected void setUp() {
        browser = new ComponentBrowser(getResolver(), getHierarchy());
    }
    protected void tearDown() {
        Container c = browser.getParent();
        if (c != null)
            c.remove(browser);
        browser = null;
    }

    public void testPreserveComponentSelection() throws Throwable {
        JFrame frame = new JFrame(getName());
        final JLabel label = new JLabel(getName());
        frame.getContentPane().add(label);
        showFrame(browser);
	SwingUtilities.invokeAndWait(new Runnable() {
	    public void run() {
		browser.setSelectedComponent(label);
	    }
	});
        assertEquals("Wrong selected component",
                     label, browser.getSelectedComponent());
        browser.refresh();
        assertEquals("Wrong selected component after reload",
                     label, browser.getSelectedComponent());
    }

    public void testUncompactOnSelectCompactedComponent() throws Exception {
        ArrayList data = new ArrayList();
        for (int i=0;i < 100;i++) {
            data.add("Row " + i);
        }
        JList list = new JList(data.toArray());
        JScrollPane sp = new JScrollPane(list);
        showFrame(sp);
        JScrollBar sb = (JScrollBar)getFinder().
            find(new ClassMatcher(JScrollBar.class));
        browser.setCompactDisplay(true);
        browser.setSelectedComponent(sb);
        assertTrue("Browser should un-compact when selected component is compacted",
                   !browser.isCompactDisplay());
    }

    public ComponentBrowserTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentBrowserTest.class);
    }
}
