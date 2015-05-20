package abbot.editor;

import java.awt.*;
import javax.swing.*;
import java.util.*;

import junit.extensions.abbot.*;

public class CompactHierarchyTest extends ComponentTestFixture {

    private CompactHierarchy hierarchy;

    protected void setUp() {
        hierarchy = new CompactHierarchy(getHierarchy());
    }

    public void testElideScrollPaneChildren() {
        Object[][] data = { { "one", "two" }, { "three", "four" } };
        String[] columns = { "one", "two" };
        JTable table = new JTable(data, columns);
        JScrollPane sp = new JScrollPane(table);
        showFrame(sp);
        Collection kids = hierarchy.getComponents(sp);
        assertEquals("Scroll pane should show two children", 2, kids.size());
        assertTrue("One child should be the table header",
                   kids.contains(table.getTableHeader()));
        assertTrue("One child should be the table",
                   kids.contains(table));
    }

    public void testElidePopups() {
        JLabel label = new JLabel(getName());
        JFrame f = new JFrame(getName());
        f.getContentPane().add(label);
        showWindow(f, new Dimension(200, 200));
        JPopupMenu light = new JPopupMenu();
        light.add(new JMenuItem("item"));
        JPopupMenu heavy = new JPopupMenu();
        for (int i=0;i < 10;i++) {
            heavy.add(new JMenuItem("item " + i));
        }

        showPopup(light, label);
        assertEquals("LW Popup parent should be its invoker",
                     label, hierarchy.getParent(light));
        Collection kids = hierarchy.getComponents(label);
        assertEquals("Label should have LW popup child: " + kids,
                     1, kids.size());
        assertEquals("Label child should be the LW popup",
                     light, kids.iterator().next());
        // Normally, a heavyweight popup will be a child of the frame's
        // layered pane.  Make sure it's not represented that way
        kids = hierarchy.getComponents(f.getLayeredPane());
        assertEquals("Frame should have a single child: " + kids,
                     f.getContentPane(), kids.iterator().next());


        showPopup(heavy, label);
        assertEquals("HW Popup parent should be its invoker",
                     label, hierarchy.getParent(heavy));
        kids = hierarchy.getComponents(label);
        assertEquals("Label should have HW popup child: " + kids,
                     1, kids.size());
        assertEquals("Label child should be the HW popup",
                     heavy, kids.iterator().next());
        // Normally, a heavyweight popup will be a sub-window of the frame
        // Make sure it's not represented that way
        kids = hierarchy.getComponents(f);
        assertEquals("Frame should have a single child: " + kids,
                     f.getContentPane(), kids.iterator().next());
    }

    public void testElideMenuContents() {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("file");
        JMenuItem item = new JMenuItem("open");
        menu.add(item);
        mb.add(menu);
        JFrame f = new JFrame(getName());
        f.setJMenuBar(mb);
        showWindow(f);

        Collection kids = hierarchy.getComponents(menu);
        assertEquals("Menu should have one child: " + kids, 1, kids.size());
        assertEquals("Menu child should be menu item", 
                     item, kids.iterator().next());
    }

    public void testElideRootPaneContainer() {
        JFrame f = (JFrame)showFrame(new JLabel(getName())); 

        Collection kids = hierarchy.getComponents(f);
        assertEquals("Frame should have a single child: " + kids,
                     1, kids.size());
        assertEquals("Content pane should appear as child of frame",
                     f.getContentPane(), kids.iterator().next());
    }

    public CompactHierarchyTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, CompactHierarchyTest.class);
    }
}
