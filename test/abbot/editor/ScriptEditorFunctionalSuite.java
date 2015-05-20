package abbot.editor;

import junit.extensions.abbot.*;
import junit.framework.Test;

/** Provides aggregation of all scripts for testing the Script Editor.
    Assumes tests are available in a subdirectory "test/scripts/editor". */

public class ScriptEditorFunctionalSuite extends ScriptFixture {

    /** Construct a test case with the given name. */
    public ScriptEditorFunctionalSuite(String name) { super(name); }

    /** Return the full suite of this test case's variations. */
    public static Test suite() {
        return new ScriptTestSuite(ScriptEditorFunctionalSuite.class,
                                   "test/scripts/editor");
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ScriptEditorFunctionalSuite.class);
    }
}
