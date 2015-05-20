package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.InputEvent;
import javax.swing.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;

import org.junit.Test;

/**
 * Unit test to verify proper capture of user semantic events on a JTree.
 */
public class JTreeRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JTreeTester tester;
    private TestJTree tree;

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JTreeRecorder(r);
    }

    public void testCaptureRowSelection() {
        startRecording();
        tester.actionSelectRow(tree, 1);
        assertStep("SelectRow\\(.*\"\\[JTree, colors\\]\"\\)");
    }

    public void testCaptureRowSelectionWithComma() {
        startRecording();
        tester.actionSelectRow(tree, 4);
        assertStep("SelectRow\\(.*\"\\[JTree, child%2c 2\\]\"\\)");
    }


    public void testCaptureRowSelectionHiddenRoot() {
        tree.setRootVisible(false);
        startRecording();
        tester.actionSelectRow(tree, 1);
        assertStep("SelectRow\\(.*\"\\[sports\\]\"\\)");
    }

    /**
     * Here we have a specific failure case where the root element
     * has no render, or value that can easily be converted into a string
     */
    public void testCaptureRowSelectionHiddenRootNoRenderer() {
        tree.setRootVisible(false);
        tree.setNullForRoot();
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
                                 public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                                               boolean sel,
                                                                               boolean expanded,
                                                                               boolean leaf, int row,
                                                                               boolean hasFocus) {
                                     Component comp = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                                     if (row==-1) {
                                         setText(null);
                                     }

                                     return comp;
                                 }
                             });
        ((DefaultMutableTreeNode)((DefaultTreeModel)tree.getModel()).getRoot()).setUserObject(new Object());
        startRecording();
        tester.actionSelectRow(tree, 1);
        assertStep("SelectRow\\(.*\"\\[sports\\]\"\\)");
    }

    public void testCaptureMultipleClick() {
        startRecording();
        int row = 2;
        tester.actionClick(tree, new JTreeLocation(row),
                           InputEvent.BUTTON1_MASK, 2);
        assertStep("Click\\(.*,\"\\[JTree, sports\\]\",BUTTON1_MASK,2\\)");
    }

    public void testCaptureToggleRow() {
        startRecording();
        int row = 2;
        Rectangle bounds = new JTreeLocation(row).getBounds(tree);
        // Make a reasonable guess of the proper location.
        Point pt = new Point(bounds.x - bounds.height/2,
                             bounds.y + bounds.height/2);
        tester.actionClick(tree, pt.x, pt.y);
        assertStep("ToggleRow\\(.*,\"\\[JTree, sports\\]\"\\)");
    }

    public void testCapturePopup() {
        final String NAME = "item 1";
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem(NAME));
        addPopup(tree, menu);
        startRecording();
        tester.actionSelectPopupMenuItem(tree, 0, 0, NAME);
        assertStep("SelectPopupMenuItem\\(.*,\"\\[JTree\\]\","
                   + NAME + "\\)");
    }

    protected void setUp() {
        tree = new TestJTree();
        tester = (JTreeTester)ComponentTester.getTester(tree);
        showFrame(tree);
    }

    public JTreeRecorderTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTreeRecorderTest.class);
    }
    
    
    /**
     * Special JTree instance that will return a null string for a node
     * where required by circumstance.
     */
    
    private class TestJTree extends JTree {
        
        boolean nullForRoot = false;
        
        
        {
            DefaultTreeModel model = ((DefaultTreeModel)getModel());
            model.insertNodeInto(
                new DefaultMutableTreeNode("child, 2"), 
                (MutableTreeNode)model.getRoot(), 3);
        }
        
        public void setNullForRoot() {
            nullForRoot = true;
        }
        

        public String convertValueToText(Object value, boolean selected,
                                         boolean expanded, boolean leaf,
                                         int row, boolean hasFocus) {
            if (row==-1 && nullForRoot) {
                return null;
            }
            else
            {
                return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
            }
        }
    }
    
}
