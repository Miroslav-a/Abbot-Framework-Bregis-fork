package abbot.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import junit.extensions.abbot.*;
import abbot.finder.matchers.*;

public class ComponentNodeTest extends ComponentTestFixture {

    private CompactHierarchy hierarchy;

    protected void setUp() {
        hierarchy = new CompactHierarchy(getHierarchy());
    }

    public void testGetPath() {
        ComponentNode root = new ComponentNode(hierarchy);
        JTree tree = new JTree(root);
        JFrame f = new JFrame(getName());
        f.getContentPane().add(tree);
        showWindow(f);
        root.reload();
        Object[] path = root.getNode(tree).getPath();
        Component[] expected = new Component[] {
            null, f, f.getContentPane(), tree
        };
        for (int i=0;i < path.length;i++) {
            assertEquals("Wrong path element " + i + " on tree's path",
                         expected[i], ((ComponentNode)path[i]).getComponent());
        }
    }

    public void testGetTableHeaderPath() throws Exception {
        Object[][] data = { { "one", "two" }, { "three", "four" } };
        String[] columns = { "one", "two" };
        JTable table = new JTable(data, columns);
        JScrollPane sp = new JScrollPane(table);
        showFrame(sp);
        ComponentNode root = new ComponentNode(hierarchy);
        JTableHeader header = (JTableHeader)getFinder().
            find(new ClassMatcher(JTableHeader.class));
        Object[] path = root.getNode(header).getPath();

        ComponentNode node = (ComponentNode)path[path.length-1];
        assertEquals("Header should be last path element",
                     header, node.getComponent());
    }

    public void testPopupPath() {
        JLabel label = new JLabel(getName());
        JFrame f = new JFrame(getName());
        f.getContentPane().add(label);
        showWindow(f, new Dimension(200, 200));

        JPopupMenu light = new JPopupMenu();
        light.setName("light");
        JMenu sublight = new JMenu("sublight");
        sublight.add(new JMenuItem("subitem"));
        light.add(new JMenuItem("item"));
        light.add(sublight);
        JPopupMenu heavy = new JPopupMenu();
        heavy.setName("heavy");
        for (int i=0;i < 10;i++) {
            heavy.add(new JMenuItem("item " + i));
        }
        JMenu subheavy = new JMenu("subheavy");
        subheavy.add(new JMenuItem("subitem"));
        heavy.add(subheavy);

        showPopup(light, label, 10, 10);
        getRobot().mouseMove(sublight);
        ComponentNode root = new ComponentNode(hierarchy);
        Object[] path = root.getNode(sublight).getPath();
        Component[] expected = new Component[] { 
            null, f, f.getContentPane(), label, light, sublight
        };
        for (int i=0;i < expected.length;i++) {
            assertEquals("Wrong path element " + i + " on LW",
                         expected[i], ((ComponentNode)path[i]).getComponent());
        }

        showPopup(heavy, label);
        getRobot().mouseMove(subheavy);
        root.reload();
        path = root.getNode(subheavy).getPath();
        expected = new Component[] { 
            null, f, f.getContentPane(), label, heavy, subheavy
        };
        for (int i=0;i < expected.length;i++) {
            assertEquals("Wrong path element " + i + " on LW",
                         expected[i], ((ComponentNode)path[i]).getComponent());
        }
    }

    public ComponentNodeTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentNodeTest.class);
    }
}
