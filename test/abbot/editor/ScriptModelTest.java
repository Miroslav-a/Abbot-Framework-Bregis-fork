package abbot.editor;

import junit.extensions.abbot.*;
import abbot.script.*;

/** Verify operation of ScriptModel. */

public class ScriptModelTest extends ResolverFixture {

    private Script script;
    private ScriptModel model;

    public void testInsertBefore() {
        Sequence s1 = new Sequence(script, "sequence 1", null);
        Step s2 = new Comment(script, "step 2");
        Step s3 = new Comment(script, "step 3");
        script.addStep(s1);
        script.addStep(s2);
        script.addStep(s3);
        // 1 2 3
        model.setScript(script);
        Step s4 = new Comment(script, "step 4");
        model.insertStep(script, s4, 0);
        // 4 1 2 3
        assertEquals("Step 4 in wrong location", 0, script.indexOf(s4));
        Step s5 = new Comment(script, "step 5");
        model.toggle(1);
        model.insertStep(script, s5, 1);
        // 4 5 1 2 3
        assertEquals("Step 5 in wrong location", 1, script.indexOf(s5));
    }

    public void testInsertAfter() {
        Sequence s1 = new Sequence(script, "sequence 1", null);
        Step s2 = new Comment(script, "step 2");
        Step s3 = new Comment(script, "step 3");
        script.addStep(s1);
        script.addStep(s2);
        script.addStep(s3);
        // 1 2 3
        model.setScript(script);
        Step s4 = new Comment(script, "step 4");
        model.insertStep(script, s4, 1);
        // 1 4 2 3
        assertEquals("Step 4 in wrong location", 1, script.indexOf(s4));
        Step s5 = new Comment(script, "step 5");
        model.toggle(0);
        assertTrue("Sequence 1 should be open", model.isOpen(0));
        model.insertStep(s1, s5, 0);
        // 1(5) 4 2 3
        assertEquals("Step 5 should be moved out of script",
                     -1, script.indexOf(s5));
        assertEquals("Step 5 should be within sequence 1",
                     0, s1.indexOf(s5));
    }

    protected void setUp() throws Exception {
        script = new Script(getHierarchy());
        script.setDescription(getName());
        model = new ScriptModel();
    }

    /** Construct a test case with the given name. */
    public ScriptModelTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptModelTest.class);
    }
}
