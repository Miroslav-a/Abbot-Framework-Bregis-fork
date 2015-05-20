package abbot.tester;

import java.awt.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.tree.*;

import abbot.Log;

public class DropTree extends JTree {
    /** Target received drag. */
    public volatile boolean dragEntered = false;
    /** Target accepted the drop. */
    public volatile boolean dropAccepted = false;
    private DropTarget dropTarget = null;
    private DropTargetListener dtl = null;
    private int dropRow = -1;
    public DropTree() {
        setName("DropTree");
        setCellRenderer(new DefaultTreeCellRenderer() {
            private Font originalFont;
            private Color originalColor;
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean sel,
                                                          boolean exp,
                                                          boolean leaf,
                                                          int row, 
                                                          boolean focus) {
                Component c = super.
                    getTreeCellRendererComponent(tree, value, sel, exp,
                                                 leaf, row, focus);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    if (originalFont == null) {
                        originalFont = label.getFont();
                        originalColor = label.getForeground();
                    }
                    if (row == dropRow) {
                        label.setForeground(Color.blue);
                        label.setFont(label.getFont().deriveFont(Font.BOLD));
                    }
                    else {
                        label.setForeground(originalColor);
                        label.setFont(originalFont);
                    }
                }
                return c;
            }
        });
        dtl = new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent e) {
                Log.debug("Drag enter (target) "
                          + DropTree.this.getName());
                dragEntered = true;
                Point where = e.getLocation();
                int row = getRowForLocation(where.x, where.y);
                dropRow = row;
                if (row != -1) 
                    paintImmediately(getRowBounds(row));
            }
            public void dragOver(DropTargetDragEvent e) {
                Log.debug("Drag over (target) "
                          + DropTree.this.getName());
                e.acceptDrag(e.getDropAction());
            }
            public void dragExit(DropTargetEvent e) {
                Log.debug("Drag exit (target)" 
                          + DropTree.this.getName());
                if (dropRow != -1) {
                    int repaint = dropRow;
                    dropRow = -1;
                    paintImmediately(getRowBounds(repaint));
                }
            }
            public void dropActionChanged(DropTargetDragEvent e) {
                Log.debug("Drop action changed (target)");
                e.acceptDrag(e.getDropAction());
            }
            public void drop(DropTargetDropEvent e) {
                Log.debug("Drop accepted (target)");
                e.acceptDrop(e.getDropAction());
                e.dropComplete(true);
                dropAccepted = true;
                if (dropRow != -1) {
                    int repaint = dropRow;
                    dropRow = -1;
                    paintImmediately(getRowBounds(repaint));
                }
            }
        };
        dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                                    dtl, true);
    }

}

