package abbot.tester;

import java.awt.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.table.*;

import abbot.Log;

public class DropTable extends JTable {
    private static final String DATA[][] = {
        { "one", "red", "basketball", },
        { "two", "green", "football", },
        { "three", "blue", "rugby", },
        { "four", "yellow", "badminton", },
    };
    private static final String COLUMN_NAMES[] = {
        "numbers", "colors",
    };

    /** Target received drag. */
    public volatile boolean dragEntered = false;
    /** Target accepted the drop. */
    public volatile boolean dropAccepted = false;
    private DropTarget dropTarget = null;
    private DropTargetListener dtl = null;
    private int dropRow = -1;
    private int dropCol = -1;
    public DropTable() {
        super(DATA, COLUMN_NAMES);
        setName("DropTable");
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private Font originalFont;
            private Color originalColor;
            public Component getTableCellRendererComponent(JTable table,
                                                          Object value,
                                                          boolean sel,
                                                          boolean focus,
                                                          int row, int col) {
                Component c = super.
                    getTableCellRendererComponent(table, value, sel, focus,
                                                 row, col);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    if (originalFont == null) {
                        originalFont = label.getFont();
                        originalColor = label.getForeground();
                    }
                    if (row == dropRow && col == dropCol) {
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
                          + DropTable.this.getName());
                dragEntered = true;
                dropRow = rowAtPoint(e.getLocation());
                dropCol = columnAtPoint(e.getLocation());
                if (dropRow != -1 && dropCol != -1) 
                    paintImmediately(getCellRect(dropRow, dropCol, false));
            }
            public void dragOver(DropTargetDragEvent e) {
                Log.debug("Drag over (target) "
                          + DropTable.this.getName());
                e.acceptDrag(e.getDropAction());
            }
            public void dragExit(DropTargetEvent e) {
                Log.debug("Drag exit (target)" 
                          + DropTable.this.getName());
                if (dropRow != -1 && dropCol != -1) {
                    int row = dropRow;
                    int col = dropCol;
                    dropRow = -1;
                    dropCol = -1;
                    paintImmediately(getCellRect(row, col, false));
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
                if (dropRow != -1 && dropCol != -1) {
                    int row = dropRow;
                    int col = dropCol;
                    dropRow = -1;
                    dropCol = -1;
                    paintImmediately(getCellRect(row, col, false));
                }
            }
        };
        dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                                    dtl, true);
    }

}

