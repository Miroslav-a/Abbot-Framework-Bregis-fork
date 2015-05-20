package abbot.tester;

import java.awt.*;
import java.awt.event.*;

import java.util.concurrent.Callable;

import javax.swing.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import junit.framework.*;

/** Unit test to verify the AbstractButtonTester class.<p>
    
    <ul>
    <li>Test all exported actions.
    </ul>
 */

public class AbstractButtonTesterTest extends ComponentTestFixture {

    private AbstractButtonTester tester;

    /** Create a new test case with the given name. */
    public AbstractButtonTesterTest(String name) {
        super(name);
    }

    protected void setUp() {
        tester = (AbstractButtonTester)
            ComponentTester.getTester(AbstractButton.class);
    }

    private JButton findButton(Component comp, String text) {
        if (comp instanceof JButton) {
            if (((JButton)comp).getText().equals(text)) 
                return (JButton)comp;
        }
        else if (comp instanceof Container) {
            Component[] children = ((Container)comp).getComponents();
            for (int i=0;i < children.length;i++) {
                JButton b = findButton(children[i], text);
                if (b != null)
                    return b;
            }
        }
        return null;
    }

    private JButton findButton(String text) {
        Frame[] frames = Frame.getFrames();
        for (int i=0;i < frames.length;i++) {
            if (frames[i].isShowing()) {
                JButton button = findButton(frames[i], text);
                if (button != null)
                    return button;
            }
            Window[] subs = frames[i].getOwnedWindows();
            for (int j=0;j < subs.length;j++) {
                if (subs[j].isShowing()) {
                    JButton button = findButton(subs[j], text);
                    if (button != null)
                        return button;
                }
            }
        }
        return null;
    }

    private class ButtonWatcher implements ActionListener {
        public volatile boolean gotAction = false;
        public void actionPerformed(ActionEvent ev) {
            gotAction = true;
        }
    }
    public void testClick() {
        final JButton button = new JButton("Hit me");
        String[] values = { "one", "two", "three" };
        final JList list = new JList(values);
        final ButtonWatcher bw = new ButtonWatcher() {
            public void actionPerformed(ActionEvent ev) {
                super.actionPerformed(ev);
                JOptionPane.showInputDialog(button, list);
            }
        };
        button.addActionListener(bw);
        showFrame(button);
        tester.actionClick(button);
        assertTrueEventually("Button not pressed", 
                             new Callable<Boolean>() {
                                public Boolean call() {
                                    return bw.gotAction;
                                }
                             });

        assertTrueEventually("Timed out waiting for input dialog", 
                             new Callable<Boolean>() {
                                public Boolean call() {
                                    return isShowing("Input");
                                }
                             });
        
        
        tester.actionClick(list);
        tester.reset();
        JButton ok = findButton("OK");
        assertNotNull("Couldn't find OK button", ok);
        final ButtonWatcher bw2 = new ButtonWatcher();
        ok.addActionListener(bw2);
        tester.actionClick(ok);
//        timer.reset();

        assertTrueEventually("Timed out waiting for dialog to close", 
                             new Callable<Boolean>() {
                                public Boolean call() {
                                    return !isShowing("Input");
                                }
                             });


        assertTrueEventually("OK button not pressed", 
                             new Callable<Boolean>() {
                                public Boolean call() {
                                    return bw2.gotAction;
                                }
                             });
    }
    
    
    public void testJCheckBox() {
        
        
        final JCheckBox checkBox = new JCheckBox("Box with long name");
        showFrame(checkBox);
        ButtonWatcher bw = new ButtonWatcher();
        checkBox.addActionListener(bw);
        tester.actionClick(checkBox);
        assertTrue("Button not pressed", bw.gotAction);
        assertTrue("Button not selected", checkBox.isSelected());
    }

    /** Return the default test suite. */
    public static Test suite() {
        return new TestSuite(AbstractButtonTesterTest.class);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, AbstractButtonTesterTest.class);
    }
}

