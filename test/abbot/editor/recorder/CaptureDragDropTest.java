package abbot.editor.recorder;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.JTableHeader;

import junit.extensions.abbot.*;
import abbot.Log;
import abbot.script.*;
import abbot.tester.*;

/**
 * Verify capture of various Drag/Drop operations.
 */
public class CaptureDragDropTest 
    extends AbstractSemanticRecorderFixture {

    /** Create a new test case with the given name. */
    public CaptureDragDropTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new ComponentRecorder(r);
    }

    private ComponentTester tester;
    protected void setUp() {
        tester = new ComponentTester();
    }
    private Frame frame;
    protected Frame showFrame(Component c) {
        return frame = super.showFrame(c);
    }
    protected void tearDown() throws Exception {
        if (frame != null) {
            // ensure DnD state gets reset
            Log.log("DnD cleanup");
            tester.click(frame);
            frame = null;
        }
        tester = null;
    }

    // FIXME sporadic linux failure (misses release)
    public void testCaptureTableHeaderDragDrop() {
        // Use a table with draggable headers as a default drag & drop test
        // Drag the first column header over the second and verify they
        // switched positions
        Object[][] values = {
            { "green", "ugly" },
            { "red", "beautiful" },
        };
        Object[] names = { "Color", "Type" };
        JTable table = new JTable(values, names);
        showFrame(new JScrollPane(table));
        JTableHeader header = table.getTableHeader();
        startRecording();
        int width = header.getWidth();
        int height = header.getHeight();
        tester.actionDrag(header, width / 4, height/2);
        assertStep("Drag");
        tester.actionDrop(header, width * 3 / 4, height/2);
        assertStep("Drop");
        assertEquals("Column header not moved", 
                     1, table.convertColumnIndexToView(0));
    }
    
    // This fails on 1.3, because we can't determine the drop location
    public void testCaptureLabelDragDrop() {
        DropLabel drop = new DropLabel("drop");
        DragLabel drag = new DragLabel("drag");
        JPanel pane = new JPanel();
        pane.add(drag);
        pane.add(drop);
        showFrame(pane);

        startRecording();
        tester.actionDrag(drag);
        assertStep("Drag\\(DragLabel");
        // ensure we ignore enter/exit events by dragging outside the pane
        tester.dragOver(pane, pane.getWidth() + 1, pane.getHeight() + 1);
        tester.actionDrop(drop);
        assertStep("Drop\\(DropLabel");
    }

    public void testCaptureDragDropToJTree() {
        DragLabel drag = new DragLabel("drag");
        DropTree drop = new DropTree();
        JPanel pane = new JPanel();
        pane.add(drag);
        pane.add(drop);
        showFrame(pane);
        startRecording();
        tester.actionDrag(drag);
        assertStep("Drag\\(DragLabel");
        tester.dragOver(pane, pane.getWidth() + 1, pane.getHeight() + 1);
        tester.actionDrop(drop, new JTreeLocation(2));
        assertStep("Drop\\(DropTree,\"\\[.*\\]\"");
    }

    public void testCaptureDragDropToJTable() {
        DragLabel drag = new DragLabel("drag");
        DropTable drop = new DropTable();
        JPanel pane = new JPanel();
        pane.add(drag);
        pane.add(drop);
        showFrame(pane);
        startRecording();
        tester.actionDrag(drag);
        assertStep("Drag\\(DragLabel");
        tester.dragOver(pane, pane.getWidth() + 1, pane.getHeight() + 1);
        tester.actionDrop(drop, new JTableLocation(1, 1));
        assertStep("Drop\\(DropTable,\"" + drop.getValueAt(1, 1) + "\"");
    }

    public void testCaptureNoDrag() {
        JButton button = new JButton(getName());
        showFrame(button);
        startRecording();
        tester.mousePress(button, button.getWidth()/2, button.getHeight()/2);
        tester.mouseMove(button, button.getWidth()/2+4, button.getHeight()/2);
        tester.mouseRelease();
        tester.waitForIdle();
        assertStep("Click");
    }
    
    public static void main(String[] args) {
        RepeatHelper.runTests(args, CaptureDragDropTest.class);
    }
}
