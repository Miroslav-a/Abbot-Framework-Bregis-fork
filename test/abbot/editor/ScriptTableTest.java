package abbot.editor;

import java.awt.*;
import java.awt.event.InputEvent;

import junit.extensions.abbot.*;
import abbot.util.Bugs;
import abbot.script.*;
import abbot.tester.*;
import abbot.tester.Robot;

/** Verify operation of ScriptTable, and ScriptModel interactions. */

public class ScriptTableTest extends ComponentTestFixture {

    private Script script;
    private ScriptModel model;
    private ScriptTable table;
    private JTableTester tester;

    protected void setUp() {
        script = new Script(getHierarchy());
        model = new ScriptModel(script);
        table = new ScriptTable(model);
        tester = (JTableTester)ComponentTester.getTester(table);
    }

    public void testInsertStep() {
        assertEquals("Wrong row count", 0, table.getRowCount());
        assertEquals("Wrong default cursor row",
                     0, table.getCursorRow());
        model.insertStep(script, new Comment(script, "One"), 0);
        assertEquals("Wrong row count after insert",
                     1, table.getRowCount());
    }

    public void testToggle() {
        Sequence seq = new Sequence(script, getName());
        seq.addStep(new Comment(script, "One"));
        seq.addStep(new Comment(script, "Two"));
        seq.addStep(new Comment(script, "Three"));
        model.insertStep(script, seq, 0);
        assertEquals("Wrong row count after insert",
                     1, table.getRowCount());
        showFrame(table, new Dimension(200,200));
        // Double click to toggle
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertEquals("Wrong row count after double click to toggle open",
                     4, table.getRowCount());
        // Click on left end of cell to toggle
        openSequence(0);
        assertEquals("Wrong row count after click to toggle closed",
                     1, table.getRowCount());
    }

    public void testUpwardSelections() {
        Sequence seq1 = new Sequence(script, getName());
        seq1.addStep(new Comment(script, "One"));
        seq1.addStep(new Comment(script, "Two"));
        model.insertStep(script, seq1, 0);
        showFrame(table, new Dimension(200, 400));
        openSequence(0);
        assertEquals("All rows should be visible", 3, table.getRowCount());
        tester.actionSelectCell(table, 1, 0);
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK
                           |InputEvent.SHIFT_MASK);
        assertEquals("Adding an ancestor to the selection should also select all the ancestor's children", 
                     3, table.getSelectedRowCount());
    }

    // FIXME interrmittent failure on linux 1.4.2_04 
    public void testDownwardSelections() {
        Sequence seq1 = new Sequence(script, getName());
        Sequence seq2 = new Sequence(script, getName());
        seq1.addStep(new Comment(script, "One"));
        seq1.addStep(seq2);
        seq2.addStep(new Comment(script, "Two"));
        seq2.addStep(new Comment(script, "Two and one half"));
        seq1.addStep(new Comment(script, "Three"));
        model.insertStep(script, seq1, 0);
        showFrame(table, new Dimension(200, 400));
        openSequence(0);
        assertEquals("All rows should be showing", 4, table.getRowCount());

        // Selecting the open sequence should result in all children selected
        tester.actionSelectCell(table, 0, 0);
        assertEquals("Full sequence should be selected when any one child is selected in conjunction with the parent",
                     4, table.getSelectedRowCount());

        tester.actionSelectCell(table, 1, 0);
        int mask = InputEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK;
        tester.actionClick(table, new JTableLocation(2, 0), mask);
        assertEquals("Sibling step selection should have no side effects",
                     2, table.getSelectedRowCount());

        openSequence(2);
        tester.actionSelectCell(table, 1, 0);
        tester.actionClick(table, new JTableLocation(2, 0), mask);
        assertEquals("Sequence and all descendents should be selected when sequence is opened and it is not the only selection",
                     4, table.getSelectedRowCount());
    }

    // FIXME sporadic w32 error
    public void testDragSingleStep() {
        if (Bugs.dragDropRequiresNativeEvents()
            && Robot.getEventMode() != Robot.EM_ROBOT) {
            System.err.println("Skipping " + getName());
            return;
        }

        Sequence seq = new Sequence(script, getName());
        seq.addStep(new Comment(script, "One"));
        seq.addStep(new Comment(script, "Two"));
        seq.addStep(new Comment(script, "Three"));
        seq.addStep(new Sequence(script, getName() + " inner"));
        model.insertStep(script, seq, 0);
        Comment c1 = new Comment(script, "first");
        Comment c2 = new Comment(script, "second");
        model.insertStep(script, c1, 1);
        model.insertStep(script, c2, 2);
        assertEquals("Wrong row count", 3, table.getRowCount());
        showFrame(table, new Dimension(200,200));
        Rectangle rect = table.getCellRect(0, 0, false);
        tester.actionDrag(table, new JTableLocation(2, 0));
        tester.actionDrop(table, rect.x,
                          rect.y + rect.height*3/4);
        assertEquals("Last step incorrectly dragged",
                     c2, model.getStepAt(1));
        assertEquals("Displaced step incorrectly moved",
                     c1, model.getStepAt(2));
    }

    public void testMoveUpSibling() {
        Step s1 = new Comment(script, "One");
        Step s2 = new Comment(script, "Two");
        model.insertStep(script, s1, 0);
        model.insertStep(script, s2, 1);
        table.setRowSelectionInterval(1, 1);
        table.moveUp();
        assertEquals("Step not moved", s2, table.getValueAt(0, 0));
        assertEquals("Wrong selection after move", 0, table.getSelectedRow());
        assertEquals("Wrong selection size after move", 1, table.getSelectedRowCount());
    }

    // FIXME sporadic w32 failure
    public void testMoveUpOpenSequenceEmpty() {
        Sequence seq = new Sequence(script, getName());
        Step s1 = new Comment(script, "One");
        model.insertStep(script, seq, 0);
        model.insertStep(script, s1, 1);
        showFrame(table, new Dimension(200, 400));
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        tester.actionSelectCell(table, 1, 0);
        table.moveUp();
        assertEquals("Step not moved into empty sequence", 1, seq.size());
        assertEquals("Wrong selection after move", 1, table.getSelectedRow());
        assertEquals("Wrong selection size after move", 1, table.getSelectedRowCount());
    }

    public void testMoveUpOpenSequence() {
        Sequence seq = new Sequence(script, getName());
        Step s1 = new Comment(script, "One");
        Step s2 = new Comment(script, "Two");
        seq.addStep(s2);
        model.insertStep(script, seq, 0);
        model.insertStep(script, s1, 1);

        showFrame(table, new Dimension(200, 400));
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        tester.actionSelectCell(table, 2, 0);

        table.moveUp();
        assertEquals("Step not moved into sequence", 2, seq.size());
        assertEquals("Wrong selection after move", 2, table.getSelectedRow());
        assertEquals("Wrong selection size after move", 1, table.getSelectedRowCount());
        assertEquals("Wrong index in sequence", 1, seq.indexOf(s1));
    }

    public void testMoveUpOut() {
        Sequence seq = new Sequence(script, getName());
        Step s1 = new Comment(script, "One");
        seq.addStep(s1);
        model.insertStep(script, seq, 0);

        showFrame(table, new Dimension(200, 400));
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        tester.actionSelectCell(table, 1, 0);

        table.moveUp();
        assertEquals("Step not moved out of sequence", 0, seq.size());
        assertEquals("Wrong selection after move", 0, table.getSelectedRow());
        assertEquals("Wrong selection size after move", 1, table.getSelectedRowCount());
        assertEquals("Wrong index in script", 0, script.indexOf(s1));
    }

    public void testMoveDownSibling() {
        Step s1 = new Comment(script, "One");
        Step s2 = new Comment(script, "Two");
        model.insertStep(script, s1, 0);
        model.insertStep(script, s2, 1);
        table.setRowSelectionInterval(0, 0);
        table.moveDown();
        assertEquals("Step not moved", s1, table.getValueAt(1, 0));
        assertEquals("Step not selected", 1, table.getSelectedRow());
        assertEquals("Wrong selection size", 1, table.getSelectedRowCount());
    }

    public void testMoveDownOpenSequence() {
        Step s1 = new Comment(script, "One");
        Sequence seq = new Sequence(script, getName());
        model.insertStep(script, s1, 0);
        model.insertStep(script, seq, 1);

        showFrame(table, new Dimension(200, 400));
        tester.actionClick(table, new JTableLocation(1, 0),
                           InputEvent.BUTTON1_MASK, 2);
        tester.actionSelectCell(table, 0, 0);

        table.moveDown();
        assertEquals("Stop should have moved into sequence", 1, seq.size());
        assertEquals("Step not moved", s1, table.getValueAt(1, 0));
        assertEquals("Step not selected", 1, table.getSelectedRow());
        assertEquals("Wrong selection size", 1, table.getSelectedRowCount());
    }

    public void testSetCursorPrevious() {
        Step s1 = new Comment(script, "One");
        Step s2 = new Comment(script, "Two");
        model.insertStep(script, s1, 0);
        model.insertStep(script, s2, 1);
        showFrame(table, new Dimension(200, 400));
        Rectangle rect = table.getCellRect(1, 0, false);
        tester.actionClick(table, rect.x, rect.y);
        assertEquals("Wrong cursor row", 1, table.getCursorRow());
        assertEquals("Wrong cursor parent", script, table.getCursorParent());
        assertEquals("Wrong cursor parent index", 1, table.getCursorParentIndex());
        rect = table.getCellRect(0, 0, false);
        tester.actionClick(table, rect.x, rect.y);
        assertEquals("Wrong cursor row", 0, table.getCursorRow());
        assertEquals("Wrong cursor parent", script, table.getCursorParent());
        assertEquals("Wrong cursor parent index", 0, table.getCursorParentIndex());
    }

    public void testSetCursorNext() {
        Step s1 = new Comment(script, "One");
        Step s2 = new Comment(script, "Two");
        model.insertStep(script, s1, 0);
        model.insertStep(script, s2, 1);
        showFrame(table, new Dimension(200, 400));
        Rectangle rect = table.getCellRect(1, 0, false);
        tester.actionClick(table, rect.x, rect.y + rect.height*3/4);
        assertEquals("Wrong cursor row", 2, table.getCursorRow());
        assertEquals("Wrong cursor parent", script, table.getCursorParent());
        assertEquals("Wrong cursor parent index", 2, table.getCursorParentIndex());
        rect = table.getCellRect(0, 0, false);
        tester.actionClick(table, rect.x, rect.y + rect.height*3/4);
        assertEquals("Wrong cursor row", 1, table.getCursorRow());
        assertEquals("Wrong cursor parent", script, table.getCursorParent());
        assertEquals("Wrong cursor parent index", 1, table.getCursorParentIndex());
    }

    public void testDoubleClickOpenClose() {
        Sequence seq1 = new Sequence(script, "One");
        Sequence seq2 = new Sequence(script, "Two");
        seq1.addStep(seq2);
        Step s1 = new Comment(script, "A");
        model.insertStep(script, seq1, 0);
        model.insertStep(script, s1, 1);
        showFrame(table, new Dimension(200, 400));

        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertTrue("First sequence not expanded on double click",
                   model.isOpen(seq1));
        assertEquals("Expanded sequence not reflected in table",
                     3, table.getRowCount());
        assertTrue("Second sequence should be closed", !model.isOpen(seq2));

        tester.actionClick(table, new JTableLocation(1, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertTrue("Second sequence should have been opened on double click",
                   model.isOpen(seq2));

        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertTrue("First sequence not closed on double click",
                   !model.isOpen(seq1));
        assertEquals("Closed sequence not reflected in table",
                     2, table.getRowCount());
        assertTrue("Second sequence should still be open",
                   model.isOpen(seq2));
    }

    public void testClickOpenClose() {
        Sequence seq1 = new Sequence(script, "One");
        Sequence seq2 = new Sequence(script, "Two");
        seq1.addStep(seq2);
        Step s1 = new Comment(script, "A");
        model.insertStep(script, seq1, 0);
        model.insertStep(script, s1, 1);
        showFrame(table, new Dimension(200, 400));
        Rectangle rect = table.getCellRect(0, 0, false);
        tester.actionClick(table, rect.x, rect.y + rect.height/2);
        assertTrue("First sequence not expanded on icon click",
                   model.isOpen(seq1));
        assertEquals("Expanded sequence not reflected in table",
                     3, table.getRowCount());
        Rectangle rect2 = table.getCellRect(1, 0, false);
        tester.actionClick(table, rect2.x, rect2.y + rect2.height/2);
        assertTrue("Second sequence not opened on icon click",
                   model.isOpen(seq2));

        tester.actionClick(table, rect.x, rect.y + rect.height/2);
        assertTrue("First sequence not closed on icon click",
                   !model.isOpen(seq1));
        assertEquals("Closed sequence not reflected in table",
                     2, table.getRowCount());
        assertTrue("Second sequence should still be open",
                   model.isOpen(seq2));
    }

    public void testSetCursorNested() {
        Sequence seq1 = new Sequence(script, "One");
        Sequence seq2 = new Sequence(script, "Two");
        seq1.addStep(seq2);
        Step s1 = new Comment(script, "a");
        model.insertStep(script, seq1, 0);
        model.insertStep(script, s1, 1);
        showFrame(table, new Dimension(200, 400));
        tester.actionClick(table, new JTableLocation(0, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertTrue("First sequence not expanded", model.isOpen(seq1));
        assertEquals("Expanded sequence not reflected in table",
                     3, table.getRowCount());
        tester.actionClick(table, new JTableLocation(1, 0),
                           InputEvent.BUTTON1_MASK, 2);
        assertTrue("Second sequence not opened", model.isOpen(seq2));

        // (script)
        // sequence 1
        //   sequence 2
        //     (3)
        //   (2)
        // (1)
        // comment
        //
        // Should be three possible cursor positions on row 2
        // 1) sibling to seq1
        // 2) sibling to seq2
        // 3) child of seq2
        Rectangle rect = table.getCellRect(1, 0, false);
        Point target = new Point(table.getDepthIndentation(0),
                                 rect.y + rect.height * 7 / 8);

        table.setCursorLocation(target);
        assertEquals("Leftmost cursor should target script as parent",
                     script, table.getCursorParent());
        assertEquals("Wrong target index in parent",
                     1, table.getCursorParentIndex());
        assertEquals("Wrong cursor row",
                     2, table.getCursorRow());

        target.x = table.getDepthIndentation(1);
        table.setCursorLocation(target);
        assertEquals("Point at " + target + " should target outer sequence",
                     seq1, table.getCursorParent());
        assertEquals("Wrong target index in parent",
                     1, table.getCursorParentIndex());
        assertEquals("Wrong cursor row",
                     2, table.getCursorRow());

        target.x = table.getDepthIndentation(2);
        table.setCursorLocation(target);
        assertEquals("Point at " + target + " should target inner sequence",
                     seq2, table.getCursorParent());
        assertEquals("Wrong target index in parent",
                     0, table.getCursorParentIndex());
        assertEquals("Wrong cursor row",
                     2, table.getCursorRow());
    }

    // Showed up as a bug in 1.4.2_01 (w32)
    public void testHideSequence() {
        Sequence s1 = new Sequence(script, "Hide me");
        for (int i=0;i < 100;i++) {
            s1.addStep(new Comment(script, String.valueOf(i)));
        }
        model.insertStep(script, s1, 0);
        showFrame(table, new Dimension(200, 400));
        Rectangle rect = table.getCellRect(0, 0, false);
        tester.actionClick(table, rect.x, rect.y + rect.height/2);
        // ensure it's not a double click
        tester.delay(500);
        tester.actionClick(table, rect.x, rect.y + rect.height/2);
    }

    private void openSequence(int row) {
        Rectangle rect = table.getCellRect(row, 0, false);
        tester.actionClick(table, rect.x + table.getIndentation(0),
                           rect.y + rect.height / 2);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptTableTest.class);
    }
}
