package abbot;

import abbot.tester.Robot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;

import java.util.concurrent.Callable;

import junit.framework.TestCase;


/**
 * This might be best done with a fixture but it would mean modifying all the tests
 * but this basically forces a window to be open during all of the tests to prevent
 * the X11 disconnect errors we sometimes seem when running lots of tests
 */
public class AAEnv extends TestCase {


    public void testTicker() {

        if (Platform.isX11()) {

            // When running a load of tests on X11 Java sometimes get stopps being able
            // to reconnect between windows, so this code just displays a simple window that
            // keeps the connection to X11 going

            class RobotWindow extends Frame {
                public RobotWindow() {
                    super("RobotX11Tickler");
                    setBackground(Color.RED);
                    setSize(10, 10);
                }
            }

            final Frame window = new RobotWindow();
            Robot.callAndWait(window, new Callable<Void>() {

                @Override
                public Void call() throws Exception {

                    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
                    window.setLocation(dim.width - 10, 0);

                    window.setVisible(true);
                    window.setFocusable(false);
                    window.setState(Frame.ICONIFIED);

                    return null;
                }
            });
        }


    }
}
