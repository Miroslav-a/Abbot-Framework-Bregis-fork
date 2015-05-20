package abbot.script;

import java.awt.*;
import java.applet.*;
import java.util.*;

import junit.extensions.abbot.*;
import abbot.finder.matchers.*;

public class AppletviewerTest extends ResolverFixture {

    private static boolean manual;
    private TestAppletviewer viewer;

    public static class TestApplet extends Applet {
        public static volatile Applet instance = null;
        public void init() {
            setBackground(java.awt.Color.green);
            instance = this;
        }
        public void destroy() {
            instance = null;
        }
    }

    // Until applet disposal is worked out, only run these tests manually
    public void runBare() throws Throwable {
        if (manual) {
            super.runBare();
        }
    }

    protected void tearDown() throws Exception {
        // ensure appletviewer instances get disposed of properly
        if (viewer != null) {
            viewer.dispose();
        }
        viewer = null;
    }

    public void testLaunch() throws Throwable {
        viewer = new TestAppletviewer(new HashMap());
        viewer.run();
        // Step should not return until the applet is visible
        assertTrue("Test applet should be instantiated after step run",
                   TestApplet.instance != null);
        assertTrue("Test applet should be visible",
                   TestApplet.instance.isShowing());
    }

    public void testGeneratedHTML() {
        HashMap map = new HashMap();
        map.put("name", "value");
        viewer = new TestAppletviewer(map);

        String html = viewer.generateHTML();
        assertTrue("Missing html tag: " + html,
                   html.startsWith("<html>"));
        assertTrue("Missing applet tag: " + html,
                   html.indexOf("<applet") != -1);
        assertTrue("Code param missing: " + html,
                   html.indexOf("code=\"" + TestApplet.class.getName()) != -1);
        assertTrue("Params missing: " + html,
                   html.indexOf("><param name=\"name\" value=\"value\"") != -1);
    }

    public void testClassLoader() throws Throwable {
        viewer = new TestAppletviewer(new HashMap());
        viewer.run();
        Component c =
            getFinder().find(new ClassMatcher(Applet.class, true));
        assertEquals("Appletviewer should provide the Applet's class loader",
                     c.getClass().getClassLoader(),
                     viewer.getContextClassLoader());
    }

    public void testDocumentBase() throws Throwable {
        viewer = new TestAppletviewer(new HashMap());
        viewer.run();
        Applet applet = (Applet)getFinder().
            find(new ClassMatcher(Applet.class, true));
        
        assertNotNull("Document base should not be null",
                      applet.getDocumentBase());
    }

    public void testThreadsDisposed() throws Throwable {
        viewer = new TestAppletviewer(new HashMap());
        viewer.run();
        viewer.run();
        viewer.run();
        Iterator iter = getHierarchy().getRoots().iterator();
        while (iter.hasNext()) {
            getHierarchy().dispose((Window)iter.next());
        }

        ThreadGroup group = Thread.currentThread().getThreadGroup();
        while (group.getParent() != null) {
            group = group.getParent();
        }
        int count = group.activeCount();
        Thread[] list = new Thread[count];
        count = group.enumerate(list);

        for (int i=0;i < list.length;i++) {
            Thread thread = list[i];
            if (thread != null) {
                String name = thread.getName();
                if (name != null && name.indexOf("EventQueue") != -1
                    && name.indexOf("(abbot)") != -1) {
                    //Log.debug("Thread " + thread + " still running");
                    assertFalse("Event dispatch thread " + thread
                                + " should not still be running",
                                thread.isAlive());
                }
            }
        }
    }

    private class TestAppletviewer extends Appletviewer {
        public TestAppletviewer(Map map) {
            this(map, "CODEBASE", "ARCHIVE");
        }
        public TestAppletviewer(Map map, String codebase, String archive) {
            super(AppletviewerTest.this.getResolver(), getName(), 
                  TestApplet.class.getName(),
                  map, codebase, archive, null);
        }
        public void runStep() throws Throwable {
            super.runStep();
            getFrame().setName(getName());
            getFrame().setTitle(getName());
        }
        public String generateHTML() { return super.generateHTML(); }
        protected boolean removeSMOnExit() { return true; }
        public void dispose() {
            terminate();
        }
    }

    public static void main(String[] args) {
        manual = true;
        TestHelper.runTests(args, AppletviewerTest.class);
    }
}

