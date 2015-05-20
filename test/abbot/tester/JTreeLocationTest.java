package abbot.tester;

import abbot.Platform;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTree;
import javax.swing.tree.*;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;
import abbot.script.parsers.TreePathParser;

public class JTreeLocationTest extends TestCase {

    protected JTree createTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode("parent");
        DefaultMutableTreeNode child = new DefaultMutableTreeNode("child");
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode("child, 2");
        root.add(parent);
        parent.add(child);
        parent.add(child2);
        return new JTree(root);
    }

    public void testRowPoint() {
        JTree tree = createTree();
        Point p = new JTreeLocation(0).getPoint(tree);
        Rectangle rect = tree.getRowBounds(0);
        assertTrue("Row point should be within row", rect.contains(p));

        p = new JTreeLocation(0, true).getPoint(tree);
        assertTrue("Row point should be in expansion control",
                   JTreeTester.isLocationInExpandControl(tree, p.x, p.y));
    }

    public void testPathPoint() {
        JTree tree = createTree();
        TreePath path = new TreePath(tree.getModel().getRoot());
        Point p = new JTreeLocation(path).getPoint(tree);
        Rectangle rect = tree.getRowBounds(0);
        assertTrue("Path point should be within path's row", rect.contains(p));

        p = new JTreeLocation(path, true).getPoint(tree);
        assertTrue("Path point should be in expansion control",
                   JTreeTester.isLocationInExpandControl(tree, p.x, p.y));
    }

    public void testParsePoint() {
        JTreeLocation loc = new JTreeLocation();
        String parse = "(1,1)";
        assertEquals("Badly parsed: " + parse,
                     new JTreeLocation(new Point(1,1)),
                     loc.parse(parse)); 
    }

    public void testParseRow() {
        JTreeLocation loc = new JTreeLocation();
        String parse = "[1]";
        JTreeLocation loc2 = new JTreeLocation(1);
        assertEquals("Badly parsed: " + parse,
                     loc2, loc.parse(parse)); 
        assertEquals("Wrong row-based toString",
                     parse, loc.toString());

        parse = " [ 10 ] ";
        loc2 = new JTreeLocation(10);
        assertEquals("Badly parsed: " + parse,
                     loc2, loc.parse(parse)); 
        assertEquals("Wrong row-based toString",
                     "[10]", loc.toString());

        parse = "+[10]";
        loc2 = new JTreeLocation(10, true);
        assertEquals("Badly parsed: " + parse, 
                     loc2, loc.parse(parse));

        assertEquals("Wrong row-based toString",
                     parse, loc.toString());
    }

    public void testParsePath() {
        JTreeLocation loc = new JTreeLocation();
        String parse = "[root, parent, child]";
        TreePath path = (TreePath)new TreePathParser().parse(parse);
        JTreeLocation loc2 = new JTreeLocation(path);
        assertEquals("Badly parsed: " + parse,
                     loc2, loc.parse("\"" + parse + "\"")); 
        assertEquals("Wrong path-based toString",
                     "\"" + parse + "\"", loc.toString());

        loc2.setInExpansion(true);
        assertEquals("Badly parsed: +" + parse,
                     loc2, loc.parse("+\"" + parse + "\"")); 
        assertEquals("Wrong path-based toString",
                     "+\"" + parse + "\"", loc.toString());
    }

    public void testParsePathWithComma() {
        JTreeLocation loc = new JTreeLocation();
        String parse = "[root, parent, child%2c 2]";
        TreePath path = (TreePath)new TreePathParser().parse(parse);
        JTreeLocation loc2 = new JTreeLocation(path);
        assertEquals("Badly parsed: " + parse,
                     loc2, loc.parse("\"" + parse + "\"")); 
        assertEquals("Wrong path-based toString",
                     "\"" + parse + "\"", loc.toString());

        loc2.setInExpansion(true);
        assertEquals("Badly parsed: +" + parse,
                     loc2, loc.parse("+\"" + parse + "\"")); 
        assertEquals("Wrong path-based toString",
                     "+\"" + parse + "\"", loc.toString());
    }



    public void testParsePathHiddenRoot() {
        JTreeLocation loc = new JTreeLocation();
        String parse = "[null, parent, child]";
        try
        {
            assertEquals("Badly parsed: " + parse,
                         new JTreeLocation((TreePath)new TreePathParser().
                             parse(parse)),
                         loc.parse("\"" + parse + "\""));
            
            assertTrue("You cannot have a null path element after JDK 6",Platform.is6OrAfter());
        }
        catch (IllegalArgumentException iae) {
            // This is expected as path elements cannot be null 
            // as of JDK 7

            assertTrue("Shouldn't hit this test before JDK 7",Platform.is7OrAfter());
        }
    }

    public void testConvertHiddenRoot() {
        // Should be able to use path with or without the root reference
        JTree tree = createTree();
        tree.setRootVisible(false);
        Object root = tree.getModel().getRoot();
        Object parent = tree.getModel().getChild(root, 0);
        Object child = tree.getModel().getChild(parent, 0);
        TreePath p1 = (TreePath)
            new TreePathParser().parse("[parent, child]"); // Cannot have null elements as of JDK 7 "[null, parent, child]");
        TreePath p2 = JTreeLocation.findMatchingPath(tree, p1);
        assertTrue("Got a null path", p2 != null);
        assertEquals("Wrong root", root, p2.getPathComponent(0));
        assertEquals("Wrong parent", parent, p2.getPathComponent(1));
        assertEquals("Wrong child", child, p2.getPathComponent(2));

        // Remove as we can no longer perform [null,... test in JDK 7
//        p1 = (TreePath)new TreePathParser().parse("[parent, child]");
//        p2 = JTreeLocation.findMatchingPath(tree, p1);
//        assertTrue("Got a null path", p2 != null);
//        assertEquals("Wrong root", root, p2.getPathComponent(0));
//        assertEquals("Wrong parent", parent, p2.getPathComponent(1));
//        assertEquals("Wrong child", child, p2.getPathComponent(2));
    }

    private static int count=0;
    public void testFindMatchingPath() {
        class Bar {
            private int id = count++;
            public String toString() { return String.valueOf(id); }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new Bar());
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(new Bar());
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new Bar());
        root.add(parent);
        parent.add(child);
        JTree tree = new JTree(root);
        TreePath path = new TreePath(new Object[] { root, parent, child });
        TreePath strPath = 
            new TreePath(new String[] { "0", "1", "2" });
        JTreeLocation loc = new JTreeLocation(path);
        assertEquals("String path should be converted to real TreePath",
                     path, JTreeLocation.findMatchingPath(tree, strPath));

        class Foo { }
        root = new DefaultMutableTreeNode(new Foo());
        parent = new DefaultMutableTreeNode(new Foo());
        child = new DefaultMutableTreeNode(new Foo());
        root.add(parent);
        parent.add(child);
        tree = new JTree(root);
        
        path = new TreePath(new Object[] { root, parent, child });
        loc = new JTreeLocation(path);
        assertEquals("Should return original tree path if it's not a "
                     + "string-based path", 
                     path, JTreeLocation.findMatchingPath(tree, path));
    }

    public void testFindDuplicateMatchingPath() {
        class Bar {
            private int id;
            public Bar(int id) { this.id = id; }
            public String toString() { return String.valueOf(id); }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new Bar(0));
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(new Bar(0));
        DefaultMutableTreeNode parent2 = new DefaultMutableTreeNode(new Bar(0));
        // Make these have the same string value
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new Bar(1));
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode(new Bar(1));
        DefaultMutableTreeNode child3 = new DefaultMutableTreeNode(new Bar(1));
        DefaultMutableTreeNode child4 = new DefaultMutableTreeNode(new Bar(1));
        root.add(parent);
        root.add(parent2);
        parent.add(child);
        parent.add(child2);
        parent2.add(child3);
        parent2.add(child4);
        JTree tree = new JTree(root);
        TreePath path = new TreePath(new Object[] { root, parent, child2 });
        TreePath strpath = new TreePath(new String[] { "0", "0", "1[1]" });
        JTreeLocation loc = new JTreeLocation(path);
        assertEquals("String path should be converted to real TreePath",
                     path, JTreeLocation.findMatchingPath(tree, strpath));

        path = new TreePath(new Object[] { root, parent, child });
        strpath = new TreePath(new String[] { "0", "0", "1" });
        loc = new JTreeLocation(path);
        assertEquals("String path should be converted to real TreePath",
                     path, JTreeLocation.findMatchingPath(tree, strpath));

        path = new TreePath(new Object[] { root, parent2, child3 });
        strpath = new TreePath(new String[] { "0", "0[1]", "1" });
        loc = new JTreeLocation(path);
        assertEquals("String path should be converted to real TreePath",
                     path, JTreeLocation.findMatchingPath(tree, strpath));

        path = new TreePath(new Object[] { root, parent2, child4 });
        strpath = new TreePath(new String[] { "0", "0[1]", "1[1]" });
        loc = new JTreeLocation(path);
        assertEquals("String path should be converted to real TreePath",
                     path, JTreeLocation.findMatchingPath(tree, strpath));

    }
    // TODO: add recorder test as well
    public JTreeLocationTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, JTreeLocationTest.class);
    }
}
