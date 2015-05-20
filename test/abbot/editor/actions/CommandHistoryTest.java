package abbot.editor.actions;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

public class CommandHistoryTest extends TestCase {

    private UndoableCommand undoable = new UndoableCommand() {
        public void execute() {
            commandRun = true;
        }
        public void undo() {
            commandRun = false;
        }
        public String toString() { return "set true"; }
    };
    private Command command = new Command() {
        public void execute() {
            commandCount++;
        }
        public String toString() { return "increment"; }
    };

    private boolean commandRun = false;
    private int commandCount = 0;

    public void testUndo() throws Throwable {
        commandRun = false;
        CommandHistory history = new CommandHistory();
        assertTrue("Should not be able to undo", !history.canUndo());

        // Command 1
        command.execute();
        history.add(command);
        assertEquals("Command should have been run", 1, commandCount);
        assertTrue("Should not be able to undo", !history.canUndo());

        // Command 2
        undoable.execute();
        history.add(undoable);
        assertTrue("Command should have been run", commandRun);
        assertTrue("Should be able to undo", history.canUndo());

        // Undo Command 2 (becomes Command 3)
        history.undo();
        assertTrue("Command should have been undone", !commandRun);
        assertTrue("Should be no further undo information", !history.canUndo());
        try {
            history.undo();
            fail("Expected an exception with no further undo information");
        }
        catch(NoUndoException nue) {
            assertTrue("Should be no change", !commandRun);
            assertTrue("Undo should now be available", history.canUndo());
        }

        // History should now wrap back to the end
        // Undo Command 3 (becomes Command 4)
        history.undo();
        assertTrue("Command should have been undone", commandRun);
        assertTrue("Should be able to undo", history.canUndo());
        // Undo Command 2 (becomes Command 5)
        history.undo();
        assertTrue("Command should have been undone", !commandRun);
        assertTrue("Should not be able to undo", !history.canUndo());

        // An undoable command should flush the history
        command.execute();
        history.add(command);
        assertTrue("Should not be able to undo", !history.canUndo());
    }

    public CommandHistoryTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, CommandHistoryTest.class);
    }
}
