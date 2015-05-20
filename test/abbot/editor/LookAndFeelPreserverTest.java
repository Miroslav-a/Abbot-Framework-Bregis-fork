package abbot.editor;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;

import junit.extensions.abbot.*;

/** Verify operation of LookAndFeelPreserver, and ScriptModel interactions. */

public class LookAndFeelPreserverTest extends ComponentTestFixture {

    private String alternate;

    protected void setUp() throws Exception {
        LookAndFeel laf = UIManager.getLookAndFeel();
        UIManager.LookAndFeelInfo[] infos =
            UIManager.getInstalledLookAndFeels();
        for (int i=0;i < infos.length;i++) {
            String cname = infos[i].getClassName();
            if (!laf.getClass().getName().equals(cname)) {
                alternate = cname;
                return;
            }
        }
        throw new RuntimeException("No alternate LAF available");
    }

    public void testPreserveGlobalLAFChange() throws Exception {
        JFrame f = new JFrame(getName());
        new LookAndFeelPreserver(f);

        JLabel label = new JLabel(getName());
        f.getContentPane().add(label);
        LabelUI ui = label.getUI();
        UIManager.setLookAndFeel(alternate);
        Frame[] frames = Frame.getFrames();
        for (int i=0;i < frames.length;i++) {
            SwingUtilities.updateComponentTreeUI(frames[i]);
        }

        getRobot().waitForIdle();
        LabelUI ui2 = label.getUI();
        assertEquals("UI should revert after attempt at global change",
                     ui, ui2);
    }

    public void testPreserveWithinHierarchy() throws Exception {
        JFrame f = new JFrame(getName());
        new LookAndFeelPreserver(f);

        JLabel label = new JLabel("original LAF");
        f.getContentPane().add(label);
        LabelUI ui = label.getUI();
        UIManager.setLookAndFeel(alternate);

        JLabel label2 = new JLabel("LAF changed");
        f.getContentPane().add(label2);

        getRobot().waitForIdle();
        LabelUI ui2 = label2.getUI();
        assertEquals("Should use original LAF for new hierarchy members",
                     ui, ui2);
    }

    /** Construct a test case with the given name. */
    public LookAndFeelPreserverTest(String name) { super(name); }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, LookAndFeelPreserverTest.class);
    }
}
