package abbot.tester;


import abbot.WaitTimedOutException;

import java.awt.event.ActionEvent;

import java.awt.*;
import java.awt.event.ActionListener;

import java.awt.event.MouseAdapter;

import java.awt.event.MouseEvent;

import java.util.concurrent.atomic.AtomicBoolean;

import java.util.regex.Matcher;

import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.tree.*;

import junit.extensions.abbot.ComponentTestFixture;
import junit.extensions.abbot.RepeatHelper;

/** Unit test to verify the JTreeTester class.<p> */
// TODO add test for lazy-loading children, i.e. load on node select
public class JTreeTesterTest extends ComponentTestFixture
{

    private JTreeTester tester;
    private JTree tree;
    private JScrollPane scrollPane;

    protected void setUp() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        DefaultMutableTreeNode parent, child, leaf;
        int COUNT = 5;
        for (int i=0;i < COUNT;i++) {
            parent = new DefaultMutableTreeNode("parent " + i);
            root.add(parent);
            for (int j=0;j < COUNT;j++) {
                child = new DefaultMutableTreeNode("child " + j);
                parent.add(child);
                for (int k=0;k < COUNT;k++) {
                    leaf = new DefaultMutableTreeNode("leaf " + k, false);
                    child.add(leaf);
                }
            }
        }
        tree = new JTree(root);
        scrollPane = new JScrollPane(tree);
        tester = new JTreeTester();
    }

    public void testFindPathForStringPath() {
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 2", "child 2"
        });
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        assertNotNull("Couldn't convert path", path);
        assertEquals("Wrong path",
                     "[root, parent 2, child 2]", path.toString());
    }

    // FIXME sporadic linux (1.4.2) failures
    public void testMakeVisibleWithStringPath() {
        showFrame(scrollPane);
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 4", "child 3"
        });
        tester.actionMakeVisible(tree, stringPath);
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        assertTrue("Path not visible", tree.isVisible(path));
    }

    public void testMakeVisibleWithRealPath() {
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 4", "child 3", "leaf 1"
        });
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        tester.actionMakeVisible(tree, path);
        assertTrue("Path not visible", tree.isVisible(path));
    }

    public void testMakeVisibleWithHiddenRoot() {
        tree.setRootVisible(false);
        tester.actionWaitForIdle();
        TreePath stringPath = new TreePath(new String[] {
            "parent 2", "child 1", "leaf 2"
        });
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        tester.actionMakeVisible(tree, path);
        assertTrue("Path not visible", tree.isVisible(path));
    }

    public void testMakeVisibleMissingPath() {
        showFrame(scrollPane);
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 4", "child x"
        });
        try {
            tester.actionMakeVisible(tree, stringPath);
            fail("Should throw an exception when path doesn't match");
        }
        catch(LocationUnavailableException e) {
        }
    }
    
 
    public void testSelectPath() {
        showFrame(scrollPane);
        // Select using tree location path
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 2", "child 2" 
        });
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        assertNotNull("Couldn't convert path", path);

        tester.actionSelectRow(tree, new JTreeLocation(stringPath));
        TreePath[] paths = tree.getSelectionPaths();
        assertTrue("Paths not visible", tree.isVisible(path));
        assertTrue("No paths selected", paths != null);
        assertEquals("Too many selected", 1, paths.length);
        assertEquals("Wrong path selected", stringPath.toString(), paths[0].toString());
    }

    public void testSelectPathHiddenRoot() {
        showFrame(scrollPane);
        // root is optional if it is hidden
        TreePath stringPath = new TreePath(new String[] {
            "root", "parent 2", "child 2" 
        });
        TreePath stringPath2 = new TreePath(new String[] {
            "parent 2", "child 1" 
        });
        tree.setRootVisible(false);
        TreePath path = JTreeLocation.findMatchingPath(tree, stringPath);
        assertNotNull("Couldn't convert path", path);

        tester.actionSelectRow(tree, new JTreeLocation(stringPath));
        TreePath[] paths = tree.getSelectionPaths();
        assertTrue("Paths not visible", tree.isVisible(path));
        assertTrue("No paths selected", paths != null);
        assertEquals("Too many selected", 1, paths.length);
        assertEquals("Wrong path selected", stringPath.toString(), paths[0].toString());

        path = JTreeLocation.findMatchingPath(tree, stringPath2);
        tester.actionSelectRow(tree, new JTreeLocation(stringPath2));
        paths = tree.getSelectionPaths();
        assertTrue("Paths not visible", tree.isVisible(path));
        assertTrue("No paths selected", paths != null);
        assertEquals("Too many selected", 1, paths.length);
        assertEquals("Wrong path selected",
                     "[root, parent 2, child 1]", paths[0].toString());
    }

    public void testSelectRow() {
        int row = 1;
        showFrame(scrollPane);
        tester.actionSelectRow(tree, new JTreeLocation(row));
        TreePath[] paths = tree.getSelectionPaths();
        assertTrue("No paths selected", paths != null);
        assertEquals("Wrong row count selected", 1, paths.length);
        assertEquals("Wrong row selected", tree.getPathForRow(row), paths[0]);
    }

    /**
     * Check that the second select row is a no-op
     */
    public void testSelectRowTwice() {
        int row = 1;
        showFrame(scrollPane);
        tester.actionSelectRow(tree, new JTreeLocation(row));
        tester.actionSelectRow(tree, new JTreeLocation(row));
        TreePath[] paths = tree.getSelectionPaths();
        assertTrue("No paths selected", paths != null);
        assertEquals("Wrong row count selected", 1, paths.length);
        assertEquals("Wrong row selected", tree.getPathForRow(row), paths[0]);
    }


    public void testSelectRowGlassPane() {
        int row = 1;
        
        // Set the glass pane, should make the select row fail
        final JFrame frame = (JFrame)showFrame(scrollPane);
        
        getRobot().invokeAndWait(new Runnable() {
                                     public void run() {
                                         JPanel pane = (JPanel)frame.getGlassPane();
                                         pane.setVisible(true);
                                         pane.setLayout(new BorderLayout());
                                         pane.setBackground(Color.RED);
                                         pane.add(new JButton("Big Button"),BorderLayout.CENTER);
                                     }
                                 });
            
        try
        {
            tester.actionSelectRow(tree, new JTreeLocation(row));

            assertTrue("Should fail as row is not selected", false);
        }
        catch (WaitTimedOutException wte)
        {
            TreePath[] paths = tree.getSelectionPaths();
            assertTrue("Paths selected", paths == null);
        }
    }

    public void testSelectInvisibleRow() {
        showFrame(scrollPane);
        try {
            tester.actionSelectRow(tree, new JTreeLocation(200));
            fail("Should not successfully select an unavailable row");
        }
        catch(ActionFailedException e) {
        }
    }

    public void testToggleRow() {
        int row = 2;
        showFrame(scrollPane);
        tester.actionToggleRow(tree, new JTreeLocation(row));
        assertTrue("Node should have expanded", tree.isExpanded(row));
        // Avoid the next action being interpreted as the same click
        tester.delay(500);
        tester.actionToggleRow(tree, new JTreeLocation(row));
        // Make sure events have fired
        assertTrue("Node should not still be expanded", !tree.isExpanded(row));
    }


    public void testToggleRowDontTriggerDoubleClick() {
        final AtomicBoolean triggered = new AtomicBoolean(false);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    triggered.set(true);
                }
            }
        });
        int row = 2;
        showFrame(scrollPane);
        tester.actionToggleRow(tree, new JTreeLocation(row));
        assertTrue("Node should have expanded", tree.isExpanded(row));
        assertTrue("Double click shouldn't have been triggered", !triggered.get());
    }


    public void testGetLocation() {
        showFrame(tree, new Dimension(200, 450));
        // position in expand control
        Rectangle rect = tree.getRowBounds(1);
        Point expansion = new Point(rect.x/2, rect.y + rect.height/2);
        tester.actionClick(tree, new JTreeLocation(expansion));
        assertTrue("Node should have expanded", tree.isExpanded(1));
        ComponentLocation loc = tester.getLocation(tree, expansion);
        Point where = loc.getPoint(tree);
        assertTrue("Row expansion click should designate row",
                   rect.contains(where));

        Point midRow = new Point(rect.x + rect.width/2,
                                 rect.y + rect.height/2);
        loc = tester.getLocation(tree, midRow);
        where = loc.getPoint(tree);
        assertTrue("Mid-row click should designate row",
                   rect.contains(where));

        rect = tree.getRowBounds(tree.getRowCount()-1);
        Point belowTree = new Point(rect.x + rect.width/2,
                                    rect.y + rect.height + 10);
        loc = tester.getLocation(tree, belowTree);
        where = loc.getPoint(tree);
        assertEquals("Point outside tree should store raw point",
                     belowTree, where);
    }

    static int count = 0;
    public void testConvertPathToStringPath() {
        class Foo { }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new Foo());
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(new Foo());
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new Foo());
        root.add(parent);
        parent.add(child);
        JTree tree = new JTree(root);
        TreePath path = new TreePath(new Object[] { root, parent, child });
        TreePath strPath = JTreeTester.pathToStringPath(tree, path);
        assertNull("Should not use default toString() value as path: "
                   + strPath, strPath);

        count = 0;
        class Bar {
            private int id = count++;
            public String toString() { return String.valueOf(id); }
        }
        root = new DefaultMutableTreeNode(new Bar());
        parent = new DefaultMutableTreeNode(new Bar());
        child = new DefaultMutableTreeNode(new Bar());
        root.add(parent);
        parent.add(child);
        tree = new JTree(root);
        showFrame(new JScrollPane(tree));
        path = new TreePath(new Object[] { root, parent, child });
        assertEquals("Path should be stringifiable",
                     "[0, 1, 2]",
                     JTreeTester.pathToStringPath(tree, path).toString());
    }

    public void testAssertPathExists() {
        TreePath path = new TreePath(new String[] {
            "root", "parent 3", "child 1",
        });
        assertTrue("Path should exist",
                   tester.assertPathExists(tree, path));
        path = new TreePath(new String[] {
            "root", "parent 2", "child y",
        });
        
        long startTime = System.currentTimeMillis();
        
        assertFalse("Path should not exist",
                    tester.assertPathExists(tree, path, 0, false)); // Use short time out version
        assertTrue("Path should not exist",
                    tester.assertPathExists(tree, path, true));

        final long time = System.currentTimeMillis() - startTime;
        assertTrue("Should be relatively quick", time < 1000);
    }


    /**
     * This is an alternative version of the lazy loading case where the
     * node in question is replaced, required a slightly different fix.
     */
    public void testLazyLoadingReplaceNode() {
        
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        final TreePath path = new TreePath(new String[]
        {
          "root", "parent", "child"
        });
        final TreePath path2 = new TreePath(new String[]
        {
          "root", "parent", "child2"
        });        
        
        final JTree lazyTree = new JTree(root);
        showFrame(lazyTree, new Dimension(200, 450));

        // Lazily add children to the tree to check that lazy loading is working
        // properly
        //

        final Timer timer = new Timer(2000, new ActionListener()
        {       
            DefaultMutableTreeNode last = root;
            int counter = 1;

                public void actionPerformed(ActionEvent e) {

                    DefaultMutableTreeNode next;

                    if (counter == path.getPathCount()) {
                        next = new DefaultMutableTreeNode(path2.getPathComponent(2));
                        ((DefaultMutableTreeNode)last.getParent()).add(next);
                    } else {
                        
                        // Replace last node with a new one to 
                        // show lazy loading with repalcement node
                        if (last.getParent()!=null)
                        {
                            DefaultMutableTreeNode newLast = new DefaultMutableTreeNode(
                               last.getUserObject());
                            MutableTreeNode parent = ((MutableTreeNode)last.getParent());
                            parent.remove(0);
                            parent.insert(newLast, 0);
                            last = newLast;
                        }

                        next = new DefaultMutableTreeNode(path.getPathComponent(counter));
                        last.add(next);
                    }


                    lazyTree.setModel(new DefaultTreeModel(root, false));

                    counter++;
                    last = next;
                    if (counter > path.getPathCount()) {
                        ((Timer)e.getSource()).stop();
                        ;
                    }
                }
            });

        timer.start();
        
        // Test for delayed expansion
        
        tester.actionSelectRow(lazyTree, 
                              new JTreeLocation(path));        

        // Test for delayed addition

        tester.actionSelectRow(lazyTree, 
                              new JTreeLocation(path2));        
        
    }
    
    
    
    public void testLazyLoading() {
        
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        final TreePath path = new TreePath(new String[]
        {
          "root", "parent", "child"
        });
        final TreePath path2 = new TreePath(new String[]
        {
          "root", "parent", "child2"
        });        final JTree lazyTree = new JTree(root);
        showFrame(lazyTree, new Dimension(200, 450));

        // Lazily add children to the tree to check that lazy loading is working
        // properly
        //

        final Timer timer = new Timer(2000, new ActionListener()
        {       
            DefaultMutableTreeNode last = root;
            int counter = 1;
            public void actionPerformed(ActionEvent e) {
                
                DefaultMutableTreeNode next;
                
                if (counter==path.getPathCount())
                {
                  next = new DefaultMutableTreeNode(path2.getPathComponent(2));
                  ((DefaultMutableTreeNode)last.getParent()).add(next);
                }
                else
                {
                  next = new DefaultMutableTreeNode(path.getPathComponent(counter));
                  last.add(next);
                }
                
                
                lazyTree.setModel(
                    new DefaultTreeModel(root, false));
                    
                counter ++;
                last = next;
                if (counter > path.getPathCount()) {
                    ((Timer)e.getSource()).stop();;
                }
            }
        });
        
        timer.start();
        
        // Test for delayed expansion
        
        tester.actionSelectRow(lazyTree, 
                              new JTreeLocation(path));        

        // Test for delayed addition

        tester.actionSelectRow(lazyTree, 
                              new JTreeLocation(path2));        
        
    }
    
    
    /**
     * Ensure that we don't see null pointer exceptions when recording with
     * abbot for poorley implemented renders and model.
     */
    
    public void testDumbRenderer() {
        
        class DumbRenderer extends Component
          implements TreeCellRenderer {

            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean selected,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                return this;
            }
        }
        
        class DumbObject {
            public DumbObject(String obj) {
                
            }
        }


        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new DumbObject("root"));
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode(new DumbObject("child 1"));
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode(new DumbObject("child 2"));
        root.add(child1);
        root.add(child2);
        
        tree = new JTree(root);
        tree.setCellRenderer(new DumbRenderer());

        assertEquals(
            "Must not return null in simple first child case",
            JTreeTester.valueToString(
               tree , new TreePath(new Object[] {root, child1})),
            "[0]");

        assertEquals(
            "Must not return null in simple second child case",
            JTreeTester.valueToString(
               tree , new TreePath(new Object[] {root, child2})),
            "[1]");
    }
    
    
    /**
     * Ensure that we try to use a "getText" method for non JLabel renderers
     */
    
    public void testGetTextRenderer() {
        
        class OtherObject {
            
            public String _obj;
            
            public OtherObject(String obj) {
                _obj = obj;
            }
        }
        
        
        class OtherRenderer extends JTextField
          implements TreeCellRenderer {

            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean selected,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode)value;
                setText(((OtherObject)n.getUserObject())._obj);
                return this;
            }
        }
        



        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new OtherObject("root"));
        DefaultMutableTreeNode child1 = new DefaultMutableTreeNode(new OtherObject("child 1"));
        DefaultMutableTreeNode child2 = new DefaultMutableTreeNode(new OtherObject("child 2"));
        root.add(child1);
        root.add(child2);
        
        tree = new JTree(root);
        tree.setCellRenderer(new OtherRenderer());

        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JTreeTester.valueToString(
               tree , new TreePath(new Object[] {root, child1})),
            "child 1");

        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JTreeTester.valueToString(
               tree , new TreePath(new Object[] {root, child2})),
            "child 2");
    }
    
    
    
    public void testDumpTree() {
        
        String tree = JTreeTester.dumpTree(this.tree);
//        System.out.println(tree);

        String[] split = tree.split("\n");
        int count = split.length;
        
        assertEquals("Should contain a  lot of lines", 156, count);
        assertEquals("First line should be root", "root", split[0]);
        
        assertTrue("Should contain nested lines", tree.contains("--child 4"));
    }
    

    /** Create a new test case with the given name. */
    public JTreeTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTreeTesterTest.class);
    }
}

