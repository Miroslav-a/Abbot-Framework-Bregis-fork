package abbot.tester;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import abbot.Log;

public class DragLabel extends DropLabel {
    private class DragData implements Transferable {
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] {
                DataFlavor.stringFlavor
            };
        }
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return true;
        }
        public Object getTransferData(DataFlavor flavor) {
            return getName();
        }
    }

    /** Drag gesture was recognized. */
    public volatile boolean dragStarted = false;
    /** Drag has left the building, er, Component. */
    public volatile boolean dragExited = false;
    /** Source registered a successful drop. */
    public volatile boolean dropSuccessful = false;
    /** Source got an indication the drag ended. */
    public volatile boolean dragEnded = false;
    public Exception exception = null;
    private DragGestureListener dgl = null;
    private DragSourceListener dsl = null;
    private DragSource dragSource = null;
    private int acceptedActions = DnDConstants.ACTION_COPY_OR_MOVE;
    public DragLabel(String name) { this(name, true); }
    public DragLabel(String name, final boolean acceptDrops) { 
        super(name, acceptDrops);
        setName("DragLabel (" + name + ")");
        dragSource = DragSource.getDefaultDragSource();
        dgl = new DragGestureListener() {
                public void dragGestureRecognized(DragGestureEvent e) {
                    Log.debug("Recognizing gesture");
                    if ((e.getDragAction() & acceptedActions) == 0)
                        return;
                    Log.debug("Drag started (listener)");
                    dragStarted = true;
                    try {
                        e.startDrag(acceptDrops
                                    ? DragSource.DefaultCopyDrop
                                    : DragSource.DefaultCopyNoDrop,
                                    new DragData(), dsl);
                        setForeground(Color.red);
                        paintImmediately(getBounds());
                    }
                    catch(InvalidDnDOperationException idoe) {
                        exception = idoe;
                    }
                }
            };
        dsl = new DragSourceListener() {
                public void dragDropEnd(DragSourceDropEvent e) {
                    Log.debug("Drag ended (success="
                              + e.getDropSuccess() + ")");
                    dropSuccessful = e.getDropSuccess();
                    dragEnded = true;
                    setForeground(Color.black);
                    paintImmediately(getBounds());
                }
                public void dragEnter(DragSourceDragEvent e) {
                    Log.debug("Drag enter (source)");
                }
                public void dragOver(DragSourceDragEvent e) {
                    Log.debug("Drag over (source)");
                }
                public void dragExit(DragSourceEvent e) {
                    Log.debug("Drag exit (source)");
                    dragExited = true;
                }
                public void dropActionChanged(DragSourceDragEvent e) {
                    Log.debug("Drop action changed (source)");
                }
            };
        dragSource.
            createDefaultDragGestureRecognizer(this, acceptedActions, dgl);
    }
}

