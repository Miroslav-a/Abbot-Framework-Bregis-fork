package abbot.util;

import java.io.*;
import junit.framework.*;
import junit.extensions.abbot.*;

public class ProcessOutputHandlerTest extends TestCase {

    public void testExec() throws Exception {
        String[] cmd = { "echo", "hello" };
        String out = ProcessOutputHandler.exec(cmd);
        assertEquals("Wrong output", "hello\n", out);

        try {
            out = ProcessOutputHandler.exec(new String[] { "no such command" });
            fail("Non-existent command should throw an IOException");
        }
        catch(IOException e) {
        }

        try {
            out = ProcessOutputHandler.exec(new String[] {
                "java", "-no-such-option"
            });
            fail("Expect exception with error output on non-zero exit");
        }
        catch(IOException e) {
            assertTrue("Error output should have been thrown",
                       e.getMessage().indexOf("exited") != -1);
        }
    }

    public ProcessOutputHandlerTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, ProcessOutputHandlerTest.class);
    }
}
