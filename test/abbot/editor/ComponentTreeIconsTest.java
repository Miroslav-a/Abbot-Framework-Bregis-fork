package abbot.editor;

import java.awt.Component;
import javax.swing.*;
import junit.framework.*;
import junit.extensions.abbot.*;

public class ComponentTreeIconsTest extends TestCase {

    public void testBasicComponents() {
        Component[] comps = {
            new JFrame(getName()),
            new JWindow(new JFrame()),
            new JDialog(new JFrame())
        };
        for (int i=0;i < comps.length;i++) {
            assertTrue("No icon found for " + comps[i].getClass(),
                       icons.getIcon(comps[i].getClass()) != null);
        }
    }

    private ComponentTreeIcons icons;
    protected void setUp() {
        icons = new ComponentTreeIcons();
    }
    public ComponentTreeIconsTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentTreeIconsTest.class);
    }
}
