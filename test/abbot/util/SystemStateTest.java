package abbot.util;

import java.io.*;

import junit.framework.*;
import junit.extensions.abbot.*;

public class SystemStateTest extends TestCase {

    public void testPreserveProperties() throws Throwable {
        final String nullProperty = "null.property";
        final String knownProperty = "os.name";
        String nullValue = System.getProperty(nullProperty);
        String knownValue = System.getProperty(knownProperty);
        SystemState state = new SystemState();

        System.setProperty(nullProperty, "non-null");
        System.setProperty(knownProperty, "dummy value");

        state.restore();
        assertEquals("Null property not restored",
                     nullValue, System.getProperty(nullProperty));
        assertEquals("Known property not restored",
                     knownValue, System.getProperty(knownProperty));
    }

    public void testPreserveStreams() throws Throwable {
        PrintStream out = System.out;
        PrintStream err = System.err;
        SystemState state = new SystemState();

        System.out.close();
        System.err.close();
        System.setOut(new PrintStream(new OutputStream() {
            public void write(int b) { }
        }));
        System.setErr(new PrintStream(new OutputStream() {
           public void write(int b) { }
        }));
        System.out.println(getName());
        System.err.println(getName());

        state.restore();

        assertEquals("System.out not preserved", out, System.out);
        assertEquals("System.err not preserved", err, System.err);
        assertTrue("System.out was closed", !System.out.checkError());
        assertTrue("System.err was closed", !System.err.checkError());
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, SystemStateTest.class);
    }
}
