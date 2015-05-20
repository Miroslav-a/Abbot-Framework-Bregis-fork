package abbot.tester;


import java.awt.AWTException;
import java.awt.Button;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import junit.extensions.RepeatedTest;
import junit.extensions.abbot.TestHelper;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.junit.Assert;

import sun.awt.SunToolkit;


/**
 * This is a test that just uses awt.Robot directly to press
 * a button on a dialog, use as a test case to interact with the
 * JDK.
 */
public class RawButtonTesterTest extends TestCase {
    
    static java.awt.Robot robot;
        static {
        try {
            robot = new java.awt.Robot();
        } catch (AWTException e) {
        }
    }

    
    
    public void testClickButton() throws InterruptedException, InvocationTargetException {
        final Button b = new Button("Some Button Name");
        final String expected = "button clicked";
        
        final JFrame frame = new JFrame("Some title");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel pane = (JPanel)frame.getContentPane();
        pane.setBorder(new EmptyBorder(10, 10, 10, 10));
        pane.add(b);

        try
        {
            
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    
                    frame.pack();
                    // Make sure the window is positioned away from
                    // any toolbars around the display borders
                    frame.setLocation(100, 100);
                    frame.setVisible(true);


                    b.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            b.setLabel(expected);
                        }
                    });
                }
            });
           
            //
            
            robot.delay(100);
            robot.setAutoDelay(10);
            
          //  ((SunToolkit)Toolkit.getDefaultToolkit()).realSync();
            
            //
    
            // Risk non-thread safe version
//            Point point = AWT.getLocationOnScreen(b);
            Point point = new Point(b.getLocationOnScreen());


            Dimension dim = b.getSize();
            point.translate(dim.width/2, dim.height/2);
    
            robot.mouseMove(point.x, point.y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
    
    
//            robot.delay(100);
            
            //
            
            ((SunToolkit)Toolkit.getDefaultToolkit()).realSync();
    
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {

                    assertEqualsEventually("Button not clicked", expected, new Callable<String>() {
                        public String call() {
                            return b.getLabel();
                        }
                    });
                }
            });
    
    
//            assertEqualsEventually("Button not clicked", expected, new Callable<String>() {
//                public String call() {
//
//                    FutureTask<String> isOpen = new FutureTask<String>(
//                            new Callable<String>() {
//
//                                @Override
//                                public String call() throws Exception {
//                                    return b.getLabel();
//                                }
//                            });
//                    EventQueue.invokeLater(isOpen);
//
//                    try {
//                        return isOpen.get();
//                    } catch (Exception e) {
//                        return "Nothing";
//                    } 
//                }
//            });
        }
        finally {
            
            EventQueue.invokeAndWait(new Runnable()
                                     {
                @Override
                public void run() {
                    frame.setVisible(false);
                    frame.dispose();
                }
            });
                
        }
    }


    public RawButtonTesterTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, RawButtonTesterTest.class);
    }
    
    
    // assertTrueEventually
    

    /**
     * Waits until a value equals to the value produced by the closure
     * @param message The message is display if we fail
     * @param expectedValue The expected value
     * @param eventualValue The closure that fetched the value we are testing against
     */
    public static <T> void assertEqualsEventually(final String message, final T expectedValue, final Callable<T> eventualValue) {
        try {
            Condition condition = new Condition() {
                @Override
                public boolean test() {
                    try {
                        Assert.assertEquals(message, expectedValue, eventualValue.call());
                        return true;
                    } catch (Throwable e) {
                        return false;
                    }
                }
            };

            wait(condition);

        } 
        catch (WaitTimedOutException wtoe) {
            try {
                Assert.assertEquals(
                    message,
                    expectedValue,
                    eventualValue.call());
            } catch (Exception e) {
                AssertionFailedError ae = new AssertionFailedError("Failure to process assertion");
                ae.initCause(e);
                throw ae;
            }
        }
    }

    private static void wait(Condition condition) {
        long start = System.currentTimeMillis();
        while (!condition.test()) {
            if (System.currentTimeMillis() - start > 30000) {
                String msg = "Timed out waiting for " + condition;
                throw new WaitTimedOutException(msg);
            }
            robot.delay(10);
        }
    }
    
    
    
    //
    

    /** Abstract a condition test. */
    public interface Condition {
        /** Return the condition state. */
        boolean test();
        /** Return a description of what the condition is testing. */
        String toString();
    }
    
    
    public static class WaitTimedOutException extends RuntimeException {
    //    public WaitTimedOutError() {  }
        public WaitTimedOutException(String msg) { super(msg); }
    }
    
    // Run the tests lots of times
    //
    
    public static class RepeatHelper extends TestHelper {

        private static int repeatCount = 1;

        // no instantiations
        protected RepeatHelper() { }

        protected static String[] parseArgs(String[] args) {
            ArrayList list = new ArrayList();
            for (int i=0;i < args.length;i++) {
                try {
                    repeatCount = Integer.parseInt(args[i]);
                }
                catch(NumberFormatException e) {
                    list.add(args[i]);
                }
            }
            return (String[])list.toArray(new String[list.size()]);
        }

        public static void runTests(String[] args, Class testClass) {
            //args = Log.init(args);
            args = parseArgs(args);
            args = TestHelper.parseArgs(args);
            try {
                junit.framework.Test test = collectTests(args, testClass);
                if (repeatCount > 1)
                    test = new RepeatedTest(test, repeatCount);
                runTest(test);
            }
            catch(Exception e) {
                System.err.println(e.getMessage());
                System.exit(-2);
            }
        }
    }    
    
    
    
    
    
    
    
    
    
    
    ///
  
}
