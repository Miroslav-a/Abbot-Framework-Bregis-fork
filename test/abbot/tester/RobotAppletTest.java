package abbot.tester;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import sun.awt.AppContext;
import sun.awt.SunToolkit;
import sun.applet.AppletPanel;

import abbot.*;
import abbot.util.AWT;
import abbot.script.*;
import abbot.tester.Robot;
import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;

/** Unit test to verify Robot operation on applets.<p> */

public class RobotAppletTest extends ComponentTestFixture {

    private static boolean manual;

    // TODO: figure out a way to kill the event dispatch threads started by
    // each instantiation of AppletViewer; they should die on their own but
    // something seems to be resurrecting them (Robot.waitForIdle may be
    // partly responsible).

    private Robot robot;

    protected void setUp() {
        robot = getRobot();
        appletStartCount = 0;
    }

    // Until applet disposal is worked out, only run these tests manually
    public void runBare() throws Throwable {
        if (manual) {
            super.runBare();
        }
    }

    // Ensure the applet is properly shut down before automatic window
    // disposal happens, or we're left with lingering EDTs and event queues.
    protected void tearDown() throws Exception {
        // must close via menu "File|Quit" (not dispose)
        // to properly get rid of appletviewer
        for (Iterator i = getHierarchy().getRoots().iterator();i.hasNext();) {
            Window w = (Window)i.next();
            if (w instanceof Frame 
                && "sun.applet.AppletViewer".equals(w.getClass().getName())) {
                robot.selectAWTMenuItem((Frame)w, "Applet|Close");
            }
        }
        while (System.getSecurityManager() != oldsm) {
            Thread.sleep(10);
        }
        oldsm = null;
    }

    static Object lock = new Object();
    private static final String TAG = " (applet test)";
    public static class TestApplet extends Applet {
        public static volatile Applet testInstance;
        public void start() { ++appletStartCount; }
        public void init() {
            String name = Thread.currentThread().getName();
            Thread.currentThread().setName(name + TAG);
            testInstance = this;
            System.out.println("applet context=" + sun.awt.AppContext.getAppContext());
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    Thread thread = Thread.currentThread();
                    String name = thread.getName();
                    if (name.indexOf(TAG) == -1) {
                        thread.setName(name + TAG);
                    }
                }
            });
        }
    }

    public static class ColoredApplet extends TestApplet {
        public TextField text;
        public ColoredApplet() { setName(getClass().getName()); }
        public ColoredApplet(Color c) {
            setBackground(c);
        }
        public void init() {
            super.init();
            text = new TextField(ColoredApplet.this.getName());
            text.setBackground(getBackground());
            add(text);
        }
    }
    public static class RedApplet extends ColoredApplet {
        public static volatile ColoredApplet instance = null;
        public static volatile boolean destroyed = false;
        public RedApplet() { super(Color.red); }
        public void start() { super.start(); instance = this; }
        public void destroy() { super.destroy(); instance = null; }
    }
    public static class GreenApplet extends ColoredApplet {
        public static volatile ColoredApplet instance = null;
        public static volatile boolean destroyed = false;
        public GreenApplet() { super(Color.green); }
        public void start() { super.start(); instance = this; }
        public void destroy() { super.destroy(); instance = null; }
    }
    private class FocusWatcher extends FocusAdapter {
        public volatile boolean gotFocus = false;
        public void focusGained(FocusEvent f) {
            gotFocus = true;
            Log.log("AppContext=" + AWT.getAppContext(f.getComponent()).hashCode());
        }
    }

    private volatile int appletCount;
    private volatile static int appletStartCount;
    private File htmlFile;
    private SecurityManager oldsm;
    private AppContext appletContext;

    private void launch(String[] classNames) throws Exception {
        appletCount = classNames.length;
        StringBuffer html = new StringBuffer();
        for (int i=0;i < classNames.length;i++) {
            html.append("<html><applet codebase=\"build/classes\" code=\"");
            html.append(classNames[i]);
            html.append("\" width=\"100\" height=\"100\"></applet>");
        }
        html.append("</html>");
        // HTML file must be a relative path
        File dir = new File(System.getProperty("user.dir"));
        htmlFile = File.createTempFile(getName(), ".html", dir);
        htmlFile.deleteOnExit();
        FileOutputStream os = new FileOutputStream(htmlFile);
        os.write(html.toString().getBytes());
        os.close();
        // The appletviewer and security manager must share a class loader
        // We use AppClassLoader b/c it knows how to reload AppletViewer
        final AppClassLoader cl = new AppClassLoader(".");
        oldsm = System.getSecurityManager();
        // Make sure we catch the appletviewer exit exception
        String tgname = getName() + " Thread Group for catching exit";
        final ThreadGroup group = new ThreadGroup(tgname) {
            public void uncaughtException(Thread t, Throwable thrown) {
                if (!(thrown instanceof ExitException)
                    && !(thrown instanceof ThreadDeath))
                    Log.warn(thrown);
            }
        };
        Thread thread = new Thread(group, "AppletViewer Launcher") {
            public void run() {
                //appletContext = SunToolkit.createNewAppContext();
                try {
                    Class cls =
                        Class.forName("abbot.script.AppletSecurityManager",
                                      true, cl);
                    Constructor ctor = cls.getConstructor(new Class[] {
                        SecurityManager.class, boolean.class,
                    });
                    SecurityManager asm = (SecurityManager)
                        ctor.newInstance(new Object[] {
                            oldsm, Boolean.TRUE,
                        });
                    // Ensure AppletViewer is loaded by an instance of
                    // AppClassLoader, which can ensure the class gets 
                    // discarded after the test. 
                    cls = Class.forName("sun.applet.Main", true, cl);
                    System.setSecurityManager(asm);
                    cls.getMethod("main", new Class[] { String[].class }).
                        invoke(null, new Object[] {
                            new String[] { htmlFile.getName() }
                        });
                }
                catch(Exception e) {
                    Log.warn(e);
                }
            }
        };
        thread.setContextClassLoader(cl);
        thread.start();

        getWindowTracker();
        Timer timer = new Timer();
        while (appletStartCount < appletCount) {
            if (timer.elapsed() > 60000) {
                fail("AppletViewer failed to launch");
            }
            robot.sleep();
        }
    }

    // NOTE: maybe abstract this to an applet test fixture, which takes the
    // applet class (or a list of them) as an argument and makes appletviewer
    // display them
    public void testFocusApplet() throws Exception {
        launch(new String[] {
            RedApplet.class.getName(),
            GreenApplet.class.getName()
        });
        ColoredApplet green = GreenApplet.instance;
        ColoredApplet red = RedApplet.instance;

        FocusWatcher fw = new FocusWatcher();
        red.text.addFocusListener(fw);
        robot.focus(red.text, true);
        Timer timer = new Timer();
        while (!fw.gotFocus) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                Log.log("Failing applet focus");
                fail("Red applet text field never received focus");
            }
            robot.sleep();
        }
        fw = new FocusWatcher();
        green.text.addFocusListener(fw);
        robot.focus(green.text, true);
        timer.reset();
        while (!fw.gotFocus) {
            if (timer.elapsed() > EVENT_GENERATION_DELAY) {
                Log.log("Failing text field focus");
                fail("Green applet text field never received focus");
            }
            robot.sleep();
        }
    }

    private static abstract class AbstractComponentApplet extends TestApplet {
        public static Component component;
        protected abstract Component getComponent();
        public void init() {
            super.init();
            add(component = getComponent());
        }
    }
    public static class TextFieldApplet extends AbstractComponentApplet {
        protected Component getComponent() {
            return new TextField("wider field");
        }
    }
    public static class JTextFieldApplet extends AbstractComponentApplet {
        protected Component getComponent() {
            return new JTextField("wider field");
        }
    }

    // FIXME fails on linux 1.4.2_04, AWT mode, pointer focus
    // Can't stuff a TextField with AWT events
    public void testTextFieldEntry() throws Exception {
        launch(new String[] { TextFieldApplet.class.getName() });
        String TEXT = "a few more";
        TextField tf = (TextField)TextFieldApplet.component;
        tf.setText("");
        robot.waitForIdle();
        robot.focus(tf, true);
        robot.keyString(TEXT);
        robot.waitForIdle();
        assertEquals("Text not entered", TEXT, tf.getText());
    }

    // This fails on linux/twm/pointer focus/1.4.1_02; the text
    // isn't directed to the proper window
    public void testJTextFieldEntry() throws Exception {
        launch(new String[] { JTextFieldApplet.class.getName() });
        JTextField tf = (JTextField)JTextFieldApplet.component;
        tf.setText("");
        robot.waitForIdle();
        robot.focus(tf, true);
        String TEXT = "a few more";
        robot.keyString(TEXT);
        robot.waitForIdle();
        assertEquals("Text not entered", TEXT, tf.getText());
    }

    public static class JButtonApplet extends AbstractComponentApplet {
        protected Component getComponent() {
            return new JButton("button");
        }
    }

    public void testJButtonPress() throws Exception {
        launch(new String[] { JButtonApplet.class.getName() });
        final JButton b = (JButton)JButtonApplet.component;
        final String expected = "Text changed";
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b.setText(expected);
            }
        });
        robot.click(b);
        robot.waitForIdle();
        assertEquals("Button not hit", expected, b.getText());
    }

    private void closeAll(Window w) {
        Window[] owned = w.getOwnedWindows();
        for (int i=0;i < owned.length;i++) {
            closeAll(owned[i]);
        }
        robot.close(w);
    }
    /*
    public void testZZZ() { 
        System.out.println("Get stack trace now");
        while(true);
    }
    */
    /** Create a new test case with the given name. */
    public RobotAppletTest(String name) { super(name); }

    /** Provide for repetitive testing on individual tests. */
    public static void main(String[] args) {
        manual = true;
        RepeatHelper.runTests(args, RobotAppletTest.class);
    }
}
