package abbot.script;

import java.io.*;
import java.util.ArrayList;

import junit.extensions.abbot.*;
import junit.framework.*;
import abbot.Log;
import abbot.script.ForkedStepRunner.*;
import abbot.util.ProcessOutputHandler;

/** 
 * Verify the sequence works as advertised.
 */
 // FIXME verify forked version has same classpath!
public class ForkedStepRunnerTest extends ResolverFixture {

    private final static String LS = System.getProperty("line.separator");
    private final static String OUTPUT_TEXT = "This is the output stream";
    private final static String ERROR_TEXT = "This is the error stream";

    /** Steps are identified across processes by their encoded locations
     * within the script.
     */
    public void testStepEncoding() throws Throwable {
        String dummyStep = "<assert class=\"java.lang.Boolean\" "
            + "method=\"getBoolean\" "
            + "value=\"false\" args=\"no-such-property\"/>";
        String src = "<AWTTestScript>"
            + "  <sequence><!--0-->" /* A */
            + "    <sequence><!--0,1-->"
            + "      " + dummyStep + "<!--0,1,1-->" /* B */
            + "    </sequence>"
            + "    <sequence><!--0,2-->"
            + "    </sequence>"
            + "  </sequence>"
            + "  <sequence><!--1-->"
            + "    <sequence><!--1,1-->"
            + "      " + dummyStep + "<!--1,1,1-->"
            + "      " + dummyStep + "<!--1,1,2-->" /* C */
            + "    </sequence>"
            + "    <sequence><!--1,2-->" /* D */
            + "    </sequence>"
            + "  </sequence>"
            + "</AWTTestScript>";
        Script script = loadScript(src);
        // A
        Step step = script.getStep(0);
        String code = ForkedStepRunner.encodeStep(script, step);
        assertEquals("Wrong encoding", "0", code);
        assertEquals("Wrong decoding",
                     step, ForkedStepRunner.decodeStep(script, code));
        // B
        step = ((Sequence)((Sequence)script.getStep(0)).getStep(1)).getStep(1);
        code = ForkedStepRunner.encodeStep(script, step);
        assertEquals("Wrong nested encoding", "0,1,1", code);
        assertEquals("Wrong nested decoding",
                     step, ForkedStepRunner.decodeStep(script, code));
        // C
        step = ((Sequence)((Sequence)script.getStep(1)).getStep(1)).getStep(2);
        code = ForkedStepRunner.encodeStep(script, step);
        assertEquals("Wrong nested encoding", "1,1,2", code);
        assertEquals("Wrong nested decoding",
                     step, ForkedStepRunner.decodeStep(script, code));
        // D
        step = ((Sequence)script.getStep(1)).getStep(2);
        code = ForkedStepRunner.encodeStep(script, step);
        assertEquals("Wrong nested encoding", "1,2", code);
        assertEquals("Wrong nested decoding",
                     step, ForkedStepRunner.decodeStep(script, code));

        step = script;
        code = ForkedStepRunner.encodeStep(script, step);
        assertEquals("Wrong nested encoding", "-1", code);
        assertEquals("Wrong nested decoding",
                     step, ForkedStepRunner.decodeStep(script, code));

    }

    public void testFork() throws Throwable {
        ForkedStepRunner fs = new ForkedStepRunner();
        String[] args = {
            getClass().getName(), "fork",
        };
        final StringBuffer sb = new StringBuffer();
        final StringBuffer sbe = new StringBuffer();
        Process p;
        try {
            p = fs.fork(null, args);
        }
        catch(IOException io) {
            throw new AssertionFailedError("VM fork failed");
        }
        try {
            ProcessOutputHandler handler = new ProcessOutputHandler(p) {
                protected void handleOutput(byte[] buf, int len) {
                    sb.append(new String(buf, 0, len));
                }
                protected void handleError(byte[] buf, int len) {
                    sbe.append(new String(buf, 0, len));
                }
            };
            // FIXME add a timeout
            p.waitFor();
            assertEquals("Wrong exit value", 0, p.exitValue());
            handler.waitFor();
            assertEquals("Wrong output from subprocess",
                         OUTPUT_TEXT + LS, sb.toString());
            assertEquals("Wrong error from subprocess",
                         ERROR_TEXT + LS, sbe.toString());
        }
        finally {
            p.destroy();
        }
    }

    /** Run a script to completion. */
    public void testForkedStepRunner() throws Throwable {
        // Empty script that does nothing
        String src = "<AWTTestScript></AWTTestScript>";
        Script script = loadScript(src);
        final ArrayList events = new ArrayList();
        ForkedStepRunner fs = new ForkedStepRunner();
        fs.addStepListener(new StepListener() {
            public void stateChanged(StepEvent event) {
                events.add(event);
            }
        });
        fs.run(script);
        assertEquals("Wrong number of events", 2, events.size());
        assertEquals("Wrong step source, event 0",
                     script, ((StepEvent)events.get(0)).getStep());
        assertEquals("Wrong step id, event 0",
                     StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Wrong step source, event 1",
                     script, ((StepEvent)events.get(1)).getStep());
        assertEquals("Wrong step id, event 1",
                     StepEvent.STEP_END,
                     ((StepEvent)events.get(1)).getType());
    }

    public void testForkedApplicationPrematureExit() throws Throwable {
        String src = "<AWTTestScript><call method=\"exit\" class=\""
            + Exit.class.getName() + "\"/></AWTTestScript>";
        Script script = loadScript(src);
        final ArrayList events = new ArrayList();
        ForkedStepRunner fs = new ForkedStepRunner();
        fs.addStepListener(new StepListener() {
            public void stateChanged(StepEvent event) {
                events.add(event);
            }
        });
        try {
            fs.run(script);
            fail("Expected a forked error");
        }
        catch(ForkedFailure e) {
            fail("Expected a forked error, not a failure: " + e);
        }
        catch(ForkedError e) {
        }
    }

    /** Should be able to catch a failure from a forked script exactly like
     * you would from a regular one.
     */
    public void testForkedFailure() throws Throwable {
        String src = "<AWTTestScript>"
            + "<assert method=\"assertFrameShowing\" args=\"no such window\"/>"
            + "</AWTTestScript>";
        ForkedStepRunner fs = new ForkedStepRunner();
        Script script = loadScript(src);
        try {
            fs.run(script);
            fail("The failure was not propagated");
        }
        catch(ForkedFailure ff) {
            assertEquals("Error not set in runner",
                         ff, fs.getError(script));
            StringWriter s = new StringWriter();
            ff.printStackTrace(new PrintWriter(s));
            String trace = s.toString();
            assertTrue("No stack trace in failure: " + trace,
                       trace.indexOf("at ") != -1);
        }
        catch(Throwable t) {
            fail("Wrong exception thrown: " + t);
        }
        finally {
            fs.terminate();
        }
    }

    /** Should be able to catch an error from a forked script exactly like
     * you would from a regular one.
     */
    public void testForkedError() throws Throwable {
        String src = "<AWTTestScript>"
            + "<launch class=\"nonsense class\" method=\"main\" args=\"[]\"/>"
            + "</AWTTestScript>";
        ForkedStepRunner fs = new ForkedStepRunner();
        Script script = loadScript(src);
        try {
            fs.run(script);
            fail("No error propagated");
        }
        catch(ForkedError fe) {
            assertEquals("Error not set in runner",
                         fe, fs.getError(script));
            String expected = "java.lang.ClassNotFoundException";
            assertTrue("Wrong error: " + fe,
                       fe.toString().startsWith(expected));
            StringWriter s = new StringWriter();
            fe.printStackTrace(new PrintWriter(s));
            String trace = s.toString();
            assertTrue("Error is missing stack trace: " + trace,
                       trace.indexOf("at ") != -1);
        }
        catch(Throwable thr) {
            fail("Wrong error: " + thr);
        }
        finally {
            fs.terminate();
        }
    }

    /** Ensure the forked script has the same directory root as the
     * original.
     */ 
    public void testForkedScriptDirectory() throws Throwable {
        String included = getName() + "2";
        String src = "<AWTTestScript>"
            + "  <script filename=\"" + included + "\"/>"
            + "  <terminate/>"
            + "</AWTTestScript>";
        // Create the included script first so that the including one doesn't
        // barf on load
        Script script2 = new Script(getHierarchy());
        File dir = new File(script2.getDirectory(), getName());
        if (!dir.mkdir() && !dir.exists() && !dir.isDirectory())
            fail("Could not create temporary directory");
        dir.deleteOnExit();
        File s2 = new File(dir, included);
        s2.deleteOnExit();
        script2.setFile(s2);
        script2.save();
        ForkedStepRunner fs = new ForkedStepRunner();
        Script script = loadScript(src);
        script.setFile(new File(dir, getName()));
        fs.run(script);
        assertEquals("No errors expected", null, fs.getError(script));
    }

    private Script loadScript(String src) throws Throwable {
        StringReader reader = new StringReader(src);
        Script script = new Script(getHierarchy());
        script.load(reader);
        return script;
    }

    public ForkedStepRunnerTest(String name) {
        super(name);
    }

    public static class Exit {
        public static void exit() {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        args = Log.init(args);
        // special case for self-test
        if (args.length == 1 && args[0].equals("fork")) {
            System.out.println(OUTPUT_TEXT);
            System.err.println(ERROR_TEXT);
            System.exit(0);
        }
        TestHelper.runTests(args, ForkedStepRunnerTest.class);
    }
}
