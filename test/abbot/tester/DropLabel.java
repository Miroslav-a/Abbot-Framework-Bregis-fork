package abbot.tester;

import java.awt.*;
import java.awt.dnd.*;
import javax.swing.*;

import abbot.Log;

public class DropLabel extends JLabel {
    /** Target received drag. */
    public volatile boolean dragEntered = false;
    /** Target accepted the drop. */
    public volatile boolean dropAccepted = false;
    private DropTarget dropTarget = null;
    private DropTargetListener dtl = null;
    private boolean acceptDrops = false;
    public DropLabel(String name) { this(name, true); }
    public DropLabel(String name, boolean accept) {
        super(name);
        setName("DropLabel");
        acceptDrops = accept;
        dtl = new DropTargetListener() {
                public void dragEnter(DropTargetDragEvent e) {
                    Log.debug("Drag enter (target) "
                              + DropLabel.this.getName());
                    dragEntered = true;
                    if (acceptDrops) {
                        setForeground(Color.blue);
                        paintImmediately(getBounds());
                    }
                }
                public void dragOver(DropTargetDragEvent e) {
                    Log.debug("Drag over (target) "
                              + DropLabel.this.getName());
                    if (acceptDrops)
                        e.acceptDrag(e.getDropAction());
                }
                public void dragExit(DropTargetEvent e) {
                    Log.debug("Drag exit (target)" 
                              + DropLabel.this.getName());
                    if (acceptDrops) {
                        setForeground(Color.black);
                        paintImmediately(getBounds());
                    }
                }
                public void dropActionChanged(DropTargetDragEvent e) {
                    Log.debug("Drop action changed (target)");
                    if (acceptDrops)
                        e.acceptDrag(e.getDropAction());
                }
                public void drop(DropTargetDropEvent e) {
                    Log.debug("Drop accepted (target)");
                    if (acceptDrops) {
                        e.acceptDrop(e.getDropAction());
                        e.dropComplete(true);
                        dropAccepted = true;
                        setForeground(Color.black);
                        paintImmediately(getBounds());
                    }
                }
            };
        dropTarget = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE,
                                    dtl, true);
    }

}

