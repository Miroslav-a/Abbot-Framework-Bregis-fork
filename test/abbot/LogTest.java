// $Id: LogTest.java 1932 2006-08-01 02:46:10Z twall $
// Copyright (c) Oculus Technologies Corporation, all rights reserved
// ----------------------------------------------------------------------------
package abbot;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import junit.extensions.abbot.*;
import junit.framework.TestCase;

/** Testing for the Log class.

    @author twall@users.sf.net
*/

public class LogTest extends TestCase {

    private String stackNest(int nest, int lines) {
        if (nest <= 0)
            return Log.getStack(lines);
        return stackNest(nest - 1, lines);
    }

    private String className() {
        return getClass().getName();
    }
    
    private ByteArrayOutputStream logContents;
    private PrintStream original;
    protected void setUp() {
        logContents = new ByteArrayOutputStream();
        original = Log.getLog();
        Log.setEchoToConsole(false);
        Log.flush();
        Log.setDestination(new PrintStream(logContents));
    }
    protected void tearDown() {
        logContents = null;
        Log.flush();
        Log.setDestination(original);
        Log.setEchoToConsole(true);
    }

    /** Verify that the stack dump is accurate. */
    public void testStackLines() throws Exception {
        String dump = stackNest(3, Log.FULL_STACK);
        String atLine = "\\s*at " + className() + ".stackNest\\(.*\\)$";
        assertTrue("Full stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + atLine + atLine + atLine 
                                + "\\s*at " + className() + ".testStackLines\\(.*\\)$.*"));
        dump = stackNest(3, 1);
        assertTrue("Stack trace mismatch, 1 line: '" + dump + "'",
                   dump.matches("^\\s*" + className() + ".stackNest.*$"));
        dump = stackNest(3, 2);
        assertTrue("Stack trace mismatch, 2 lines: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest.*$"
                                + atLine + ".*"));
        dump = stackNest(3, 3);
        assertTrue("Stack trace mismatch, 3 lines: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest.*$"
                                + atLine + atLine + ".*"));
    }

    /** Verify we get a proper stack trace. */
    public void testStackTrace() throws Exception {
        String dump = stackNest(4, Log.FULL_STACK);
        String atLine = "\\s*at " + className() + ".stackNest\\(.*\\)$";
        // We need to check multiple lines...
        assertTrue("Level 4 stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + atLine + atLine + atLine + atLine
                                + "\\s*at " + className() + ".testStackTrace\\(.*\\)$.*"));
        dump = stackNest(3, Log.FULL_STACK);
        assertTrue("Level 3 stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + atLine + atLine + atLine
                                + "\\s*at " + className() + ".testStackTrace\\(.*\\)$.*"));
        dump = stackNest(2, Log.FULL_STACK);
        assertTrue("Level 2 stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + atLine + atLine
                                + "\\s*at " + className() + ".testStackTrace\\(.*\\)$.*"));
        dump = stackNest(1, Log.FULL_STACK);
        assertTrue("Level 1 stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + atLine
                                + "\\s*at " + className() + ".testStackTrace\\(.*\\)$.*"));
        dump = stackNest(0, Log.FULL_STACK);
        assertTrue("Level 0 stack trace mismatch: '" + dump + "'",
                   dump.matches("(?sm)\\s*" + className() + ".stackNest\\(.*\\)$"
                                + "\\s*at " + className() + ".testStackTrace\\(.*\\)$.*"));
    }

    /** Test class-from-string generation. */
    public void testDebugEnabling() {
        try {
            try {
                Class c = Class.forName(className());
                assertEquals("Wrong class found by forName for " + className(), 
                             LogTest.class, c);
                Log.addDebugClass(LogTest.class);
                assertTrue("Log class debugging not enabled after explicit enable",
                           Log.isClassDebugEnabled(LogTest.class));
                
                Log.removeDebugClass(LogTest.class.getName());
                assertFalse("Log class debugging enabled after explicit disable",
                            Log.isClassDebugEnabled(LogTest.class));
                
                Log.addDebugClass(LogTest.class.getName(), Log.FULL_STACK);
                assertTrue("Log class debugging should be enabled",
                           Log.isClassDebugEnabled(LogTest.class));
                assertEquals("Wrong log stack depth", 
                             Log.FULL_STACK,
                             Log.getClassStackDepth(LogTest.class.getName()));
            }
            catch(ClassNotFoundException exc) {
                fail("Can't derive class from " + className() + ": " + exc);
            }
        }
        finally {
            Log.removeDebugClass(LogTest.class);
        }
    }
    
    public void testSuppressUnenabledDebugOutput() {
        Log.debug("A message");
        Log.flush();
        assertEquals("Expect no debug output", "", logContents.toString());
    }
    
    public void testEnableDebugOutput() {
        String EXPECTED = "A message";
        Log.addDebugClass(LogTest.class);
        Log.debug(EXPECTED);
        Log.flush();
        try {
            assertFalse("Missing debug output", 
                        "".equals(logContents.toString()));
            assertTrue("Incorrect debug output",
                       logContents.toString().indexOf(EXPECTED) != -1);
        }
        finally {
            Log.removeDebugClass(LogTest.class);
        }
    }
    
    public void testSetDebugOutputDepth() {
        Log.addDebugClass(LogTest.class.getName() + ":" + 2);
        Log.debug("anything");
        Log.flush();
        try {
            assertEquals("Expect two lines of stack trace: " 
                         + logContents.toString(),
                         2, logContents.toString().split("\tat ").length);
        }
        finally {
            Log.removeDebugClass(LogTest.class);
        }
    }
    
    public void testWarnThrowable() {
        try {
            throw new RuntimeException("rte");
        }
        catch(RuntimeException e) {
            Log.warn(e);
            Log.flush();
            String output = logContents.toString();
            assertFalse("No log output", "".equals(output));
            assertTrue("Missng exception stack trace: " + output,
                       output.indexOf("Exception thrown at") != -1);
            assertTrue("Missing 'caught at': " + output,
                       output.indexOf("caught at") != -1);
            assertTrue("Missing default message: " + output,
                       output.indexOf(e.toString()) != -1);
        }
    }
    public void testLogThrowable() {
        try {
            throw new RuntimeException("logged exception");
        }
        catch(RuntimeException e) {
            Log.log("Exception thrown", e);
            Log.flush();
            String output = logContents.toString();
            assertFalse("No log output", "".equals(output));
            assertTrue("Missng exception stack trace: " + output,
                       output.indexOf("Exception thrown at") != -1);
            assertTrue("Missing 'caught at': " + output,
                       output.indexOf("caught at") != -1);
            assertTrue("Missing default message: " + output,
                       output.indexOf(e.toString()) != -1);
        }
    }
    public void testDebugThrowable() {
        Log.addDebugClass(LogTest.class);
        try {
            throw new RuntimeException("rte");
        }
        catch(RuntimeException e) {
            Log.debug("debug message", e);
            Log.flush();
            String output = logContents.toString();
            assertFalse("No log output", "".equals(output));
            assertTrue("Missng exception stack trace: " + output,
                       output.indexOf("Exception thrown at") != -1);
            assertTrue("Missing 'caught at': " + output,
                       output.indexOf("caught at") != -1);
            assertTrue("Missing default message: " + output,
                       output.indexOf(e.toString()) != -1);
        }
        finally {
            Log.removeDebugClass(LogTest.class);
        }
    }

    public void testLogRepeatedMessages() {
        final String MESSAGE = "message";
        Log.warn(MESSAGE);
        Log.warn(MESSAGE);
        Log.warn(MESSAGE);
        Log.warn("something else");
        Log.flush();
        String output = logContents.toString();
        assertTrue("Should indicate a message repeat count: " + output, 
                   output.indexOf("repeated") != -1);
    }
    
    // 39 ms for 1000 invocations
    public void testDebugSpeedWithNoDebugEnabled() {
        final int REPEAT = 1000;
        Timer timer = new Timer();
        for (int i=0;i < REPEAT;i++) {
        }
        long base = timer.elapsed()+1;

        timer.reset();
        for (int i=0;i < REPEAT;i++) {
            Log.debug("i=" + i);
        }
        long debugged = timer.elapsed();
        long delta = debugged - base;
        int MAX_US_PER_DEBUG = 500;
        assertTrue("Expected debug overhead exceeded: "
                   + delta*1000/REPEAT + "us >= " + MAX_US_PER_DEBUG + "us",
                   delta*1000/REPEAT < MAX_US_PER_DEBUG);
    }

    public void testLogSpeed() {
        final int REPEAT = 1000;
        Timer timer = new Timer();
        for (int i=0;i < REPEAT;i++) {
        }
        long base = timer.elapsed()+1;

        timer.reset();
        for (int i=0;i < REPEAT;i++) {
            Log.log("i=" + i);
        }
        long debugged = timer.elapsed();
        long delta = debugged - base;
        final int MAX_US_PER_DEBUG = 500;
        assertTrue("Expected log overhead exceeded: "
                   + delta*1000/REPEAT + " >= " + MAX_US_PER_DEBUG + "us",
                   delta*1000/REPEAT < MAX_US_PER_DEBUG);
        
        timer.reset();
        Log.flush();
        long elapsed = timer.elapsed();

        final int MAX_US_PER_MSG = 500;
        assertTrue("Too long flushing " + REPEAT + " log messages: " 
                   + elapsed + "ms", elapsed*1000/REPEAT < MAX_US_PER_MSG); 
    }
    
    // flush: ~500ms for 1000 messages
    public void testDebugMemoryUse() {
        final int REPEAT = 1000;
        long delta = 0;
        long flush = 0;
        {
            Log.addDebugClass(LogTest.class);
            long before = SizeOf.getMemoryUse();
            for (int i=0;i < REPEAT;i++) {
                Log.debug("i=" + i);
            }
            Timer timer = new Timer();
            Log.flush();
            flush = timer.elapsed();
            delta = SizeOf.getMemoryUse() - before;
            Log.removeDebugClass(LogTest.class);
        }
        System.out.println("Memory increase: " + delta/1024 + "Kb");
        System.out.println("Flush time: " + flush + "ms for " 
                           + REPEAT + " msgs");
    }
    
    /**
     * Run the main suite of this test case.
     */
    public static void main(String[] args) {
        TestHelper.runTests(args, LogTest.class);
    }
}
