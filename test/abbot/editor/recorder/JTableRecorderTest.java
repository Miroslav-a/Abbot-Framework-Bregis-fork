package abbot.editor.recorder;

import java.awt.event.InputEvent;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import junit.extensions.abbot.RepeatHelper;
import abbot.script.Resolver;
import abbot.tester.*;

/**
 * Unit test to verify proper capture of user semantic events on a JTable.
 */
public class JTableRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JTableTester tester;
    private JTable table;
    private static final int MAX_ENTRIES = 4;

    public JTableRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JTableRecorder(r);
    }

    private void showTable() {
        String[][] cells = new String[MAX_ENTRIES][MAX_ENTRIES];
        for (int i=0;i < MAX_ENTRIES;i++) {
            for (int j=0;j < MAX_ENTRIES;j++) {
                if (i == 0 && j == 0)
                    cells[i][j] = null;
                else
                    cells[i][j] = "cell " + i + "," + j;
            }
        }
        String[] names = new String[MAX_ENTRIES];
        for (int i=0;i < MAX_ENTRIES;i++) {
            names[i] = "col " + i;
        }
        table = new JTable(cells, names);
        tester = (JTableTester)ComponentTester.getTester(table);
        showFrame(new JScrollPane(table));
    }

    public void testCaptureCellSelection() {
        showTable();
        startRecording();
        for (int i=MAX_ENTRIES-1;i >= 0; i--) {
            for (int j=MAX_ENTRIES-1; j >= 0;j--) {
                tester.actionSelectCell(table, i, j);
                if (i == 0 && j == 0)
                    assertStep("SelectCell\\(.*,\\[0,0\\]\\)");
                else
                    assertStep("SelectCell\\(.*,\"cell " + i + "," + j + "\"\\)");
            }
        }
    }

    public void testCaptureMultipleClick() {
        showTable();
        startRecording();
        int row = MAX_ENTRIES/2;
        int col = MAX_ENTRIES/2;
        tester.actionClick(table, new JTableLocation(row, col),
                           InputEvent.BUTTON1_MASK, 2);
        assertStep("Click\\(.*,\"cell " + row + "," + col + "\",BUTTON1_MASK,2\\)");
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTableRecorderTest.class);
    }
}
