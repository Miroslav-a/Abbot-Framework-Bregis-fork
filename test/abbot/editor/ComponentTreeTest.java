package abbot.editor;

import java.awt.*;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.tree.*;

import junit.extensions.abbot.*;

public class ComponentTreeTest extends ComponentTestFixture {

    private ComponentTree tree;

    protected void setUp() {
        tree = new ComponentTree(new CompactHierarchy(getHierarchy()));
    }

    protected void tearDown() {
        Container c = tree.getParent();
        if (c != null)
            c.remove(tree);
        tree = null;
        // Get rid of the event listener
        System.gc();
    }

    private void assertPath(final Object[] expected, final Callable<Object[]> pathToBe) {
        
        assertTrueEventually("Incompatible paths",
                             new Callable<Boolean>() {
                    
                                @Override
                                public Boolean call() throws Exception {
                                    
                                    Object[] path = pathToBe.call();
                                    
                                    //
                                    
                                    for (int i=0;i < expected.length && i < path.length;i++) {
                                        assertEquals("Wrong path component " + i,
                                                     expected[i], ((ComponentNode)path[i]).getComponent());
                                    }
                                    assertEquals("Wrong path length", expected.length, path.length);
                                    
                                    return true;
                                }
                             });

    }

    public void testDisplayNewFrames() {
        Frame f = showFrame(new JLabel(getName()));
        final TreePath path = tree.getPath(f);
        assertPath(new Object[] { null, f }, 
                   new Callable<Object[]>() {

                        @Override
                        public Object[] call() throws Exception {
                            return path.getPath();
                        }
                    });
    }

    public void testRefreshOnPopup() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        JPopupMenu popup = new JPopupMenu();        
        popup.add(new JMenuItem("nothing to see"));
        installPopup(label, popup);
        getRobot().showPopupMenu(label);
        assertTrue("Tree didn't update after popup shown",
                   tree.getPath(popup) != null);
    }

    public void testPreserveSelection() {
        JLabel label = new JLabel(getName());
        JFrame f = new JFrame(getName());
        f.getContentPane().add(label);
        showWindow(f);
        ComponentNode root = (ComponentNode)
            ((DefaultTreeModel)tree.getModel()).getRoot();
        TreePath path = root.getPath(label);
        tree.setSelectionPath(path);
        tree.reload();
        final TreePath newPath = tree.getSelectionPath();
        assertTrue("Tree should still have a selection", newPath != null);
        assertTrue("path should be visible", tree.isVisible(newPath));
        assertPath(new Object[] { null, f, f.getContentPane(), label },
                           new Callable<Object[]>() {

                                @Override
                                public Object[] call() throws Exception {
                                    return newPath.getPath();
                                }
                            });
    }

    public void testRefreshOnWindowEvents() throws InterruptedException {
        final Frame f = showFrame(new JLabel(getName()));
        assertPath(new Object[] { null, f }, 
                   new Callable<Object[]>() {

                        @Override
                        public Object[] call() throws Exception {
                            return tree.getPath(f).getPath();
                        }
                    });
        final Frame f2 = new JFrame(getName() + "2");
        showWindow(f);
        
        assertPath(new Object[] { null, f2 }, 
                   new Callable<Object[]>() {

                        @Override
                        public Object[] call() throws Exception {
                            return  tree.getPath(f2).getPath();
                        }
                    });
    }

    public ComponentTreeTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentTreeTest.class);
    }
}
