package abbot.editor.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.tester.*;
import abbot.finder.*;
import abbot.finder.matchers.*;

public class ArrayEditorTest extends ComponentTestFixture {

    private class EditorMatcher extends ClassMatcher {
        private int currentRow = 0;
        private int row;
        public EditorMatcher(int row) {
            super(JTextField.class);
            this.row = row;
        }
        public boolean matches(Component c) {
            return super.matches(c) && currentRow++ == row;
        }
    }

    private class ButtonMatcher extends ClassMatcher {
        private int currentRow = 0;
        private int row;
        private String text;
        public ButtonMatcher(int row, String text) {
            super(JButton.class);
            this.text = text;
            this.row = row;
        }
        public boolean matches(Component c) {
            return super.matches(c) && ((JButton)c).getText().equals(text)
                && currentRow++ == row;
        }
    }

    private JTextField getEditorAt(ArrayEditor ed, int row) {
        try {
            return (JTextField)getFinder().find(ed, new EditorMatcher(row));
        }
        catch(ComponentSearchException e) {
            return null;
        }
    }

    private JButton getButtonAt(ArrayEditor ed, int row, String text) {
        try {
            return (JButton)getFinder().find(ed, new ButtonMatcher(row, text));
        }
        catch(ComponentSearchException e) {
            return null;
        }
    }

    public void testCreateEmptyList() {
        ArrayEditor ed = new ArrayEditor();
        assertEquals("Wrong row count", 0, ed.getRowCount());
        assertEquals("Should have some contents", 1, ed.getComponentCount());
    }

    public void testPopulated() {
        String[] values = { "one", "two", "three", "four" };
        ArrayEditor ed = new ArrayEditor(values);
        assertEquals("Wrong row count", values.length, ed.getRowCount());
        for (int i=0;i < values.length;i++) {
            assertEquals("Wrong value", values[i], ed.getValueAt(i));
        }
    }

    public void testInsertRow() {
        ArrayEditor ed = new ArrayEditor();
        assertTrue("text should be disabled when empty",
                   !getEditorAt(ed, 0).isEnabled());
        assertTrue("remove should be disabled when empty",
                   !getButtonAt(ed, 0, "-").isEnabled());
        assertTrue("add should be disabled when empty",
                   getButtonAt(ed, 0, "+").isEnabled());

        ed.insertRow(0);
        assertEquals("Wrong row count", 1, ed.getRowCount());
        assertEquals("Wrong component count", 1, ed.getComponentCount());
        assertEquals("Expect empty initial value", "", ed.getValueAt(0));
        assertTrue("text should be enabled when non-empty",
                   getEditorAt(ed, 0).isEnabled());
        assertTrue("remove should be enabled when non-empty",
                   getButtonAt(ed, 0, "-").isEnabled());
        assertTrue("add should be enabled when non-empty",
                   getButtonAt(ed, 0, "+").isEnabled());

        assertEquals("Wrong value", "", ed.getValueAt(0));
        assertEquals("Value mismatch at 0", "",
                     getEditorAt(ed, 0).getText());


        ed.insertRow(ed.getRowCount(), "bubba");
        assertEquals("Wrong row count", 2, ed.getRowCount());
        assertEquals("Wrong value", "bubba", ed.getValueAt(1));
        assertEquals("Value mismatch at 1", "bubba",
                     getEditorAt(ed, 1).getText());

        ed.insertRow(ed.getRowCount(), "shrimp");
        assertEquals("Wrong row count", 3, ed.getRowCount());
        assertEquals("Wrong value", "shrimp", ed.getValueAt(2));
        assertEquals("Value mismatch at 2", "shrimp",
                     getEditorAt(ed, 2).getText());

        // insert into middle
        ed.insertRow(ed.getRowCount()-1, "gump");
        assertEquals("Wrong row count", 4, ed.getRowCount());
        assertEquals("Wrong value", "gump", ed.getValueAt(2));
        assertEquals("Value mismatch at 2", "gump",
                     getEditorAt(ed, 2).getText());
        assertEquals("Wrong value", "shrimp", ed.getValueAt(3));
        assertEquals("Value mismatch at 3", "shrimp",
                     getEditorAt(ed, 3).getText());

        String value = "salvation";
        ed.insertRow(-1, value);
        assertEquals("Insert with negative adds to end",
                     value, ed.getValueAt(ed.getRowCount()-1));

        // Use the button to add a row
        showFrame(ed);
        int count = ed.getRowCount();
        tester.actionClick(getButtonAt(ed, 1, "+"));
        assertEquals("Row not inserted", count+1, ed.getRowCount());
        assertEquals("Wrong insertion point", "", ed.getValueAt(2));
    }

    public void testRemoveRow() {
        String[] values = { "one", "two", "three", "four" };
        ArrayEditor ed = new ArrayEditor(values);
        assertEquals("Wrong row count", values.length, ed.getRowCount());

        // remove middle
        ed.removeRow(1);
        assertEquals("Row not removed", values.length-1, ed.getRowCount());
        assertEquals("Wrong values remaining", values[0], ed.getValueAt(0));
        assertEquals("Value mismatch at 0", values[0],
                     getEditorAt(ed, 0).getText());
        assertEquals("Wrong values remaining", values[2], ed.getValueAt(1));
        assertEquals("Value mismatch at 1", values[2],
                     getEditorAt(ed, 1).getText());

        // remove last
        ed.removeRow(ed.getRowCount()-1);
        assertEquals("Wrong row count" , 2, ed.getRowCount());
        assertEquals("Wrong value", "three", ed.getValueAt(1));
        assertEquals("Value mismatch at 1", "three",
                     getEditorAt(ed, 1).getText());
        ed.removeRow(ed.getRowCount()-1);
        assertEquals("Wrong row count", 1, ed.getRowCount());
        assertEquals("Wrong value", "one", ed.getValueAt(0));
        assertEquals("Value mismatch at 0", "one",
                     getEditorAt(ed, 0).getText());
        ed.removeRow(0);
        assertEquals("Should be empty", 0, ed.getRowCount());

        assertTrue("text should be disabled when empty",
                   !getEditorAt(ed, 0).isEnabled());
        assertTrue("remove should be disabled when empty",
                   !getButtonAt(ed, 0, "-").isEnabled());
        assertTrue("add should be disabled when empty",
                   getButtonAt(ed, 0, "+").isEnabled());

        // Use the button to remove one
        ed.setValues(values);
        showFrame(ed);
        int count = ed.getRowCount();
        tester.actionClick(getButtonAt(ed, count-1, "-"));
        assertEquals("Row not removed", count-1, ed.getRowCount());
        tester.actionClick(getButtonAt(ed, 0, "-"));
        assertEquals("Row not removed", count-2, ed.getRowCount());
        assertEquals("Wrong row removed", values[1], ed.getValueAt(0));
    }

    public void testChangeRow() {
        String[] values = { "one", "two", "three" };
        ArrayEditor ed = new ArrayEditor(values);
        showFrame(ed);
        tester.actionSetCaretPosition(getEditorAt(ed, 0), 0);
        tester.actionKeyStroke(getEditorAt(ed, 0), KeyEvent.VK_A);
        assertEquals("Wrong text", "aone", ed.getValueAt(0));
        tester.actionKeyStroke(getEditorAt(ed, 0), KeyEvent.VK_B);
        assertEquals("Wrong text", "abone", ed.getValueAt(0));
        tester.actionSetCaretPosition(getEditorAt(ed, 1), 0);
        tester.actionKeyStroke(getEditorAt(ed, 1), KeyEvent.VK_A);
        assertEquals("Wrong text", "atwo", ed.getValueAt(1));
    }

    private class Listener implements ActionListener {
        public boolean triggered;
        public String event;
        public int index;
        public void actionPerformed(ActionEvent e) {
            event = e.getActionCommand();
            index = e.getID();
            triggered = true;
        }
    }
    public void testListeners() {
        ArrayEditor ed = new ArrayEditor();
        Listener l = new Listener();
        ed.addActionListener(l);

        l.triggered = false;
        ed.insertRow(0, "new");
        assertTrue("No event triggered", l.triggered);
        assertEquals("Wrong event type",
                     ArrayEditor.ACTION_ITEM_INSERTED, l.event);
        assertEquals("Wrong index", 0, l.index);

        l.triggered = false;
        ed.setValueAt(0, "old");
        assertTrue("No event triggered", l.triggered);
        assertEquals("Wrong event type",
                     ArrayEditor.ACTION_ITEM_CHANGED, l.event);
        assertEquals("Wrong index", 0, l.index);

        l.triggered = false;
        ed.setValues(new String[] { "old", "new" });
        assertTrue("No event triggered", l.triggered);
        assertEquals("Wrong event type",
                     ArrayEditor.ACTION_LIST_CHANGED, l.event);
        assertEquals("Wrong index", -1, l.index);

        l.triggered = false;
        ed.removeRow(0);
        assertTrue("No event triggered", l.triggered);
        assertEquals("Wrong event type",
                     ArrayEditor.ACTION_ITEM_DELETED, l.event);
        assertEquals("Wrong index", 0, l.index);
    }

    public void testRemoveBadIndex() {
        ArrayEditor ed = new ArrayEditor();
        try {
            ed.removeRow(10);
            fail("Should throw an exception when arg out of range");
        }
        catch(IllegalArgumentException e) {
        }
    }

    private JTextComponentTester tester;
    protected void setUp() {
        tester = new JTextComponentTester();
    }

    /** Construct a test case with the given name. */
    public ArrayEditorTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ArrayEditorTest.class);
    }
}
