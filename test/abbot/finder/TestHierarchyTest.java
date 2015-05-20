package abbot.finder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.Log;
import abbot.tester.*;
import abbot.util.AWT;
import abbot.finder.matchers.*;

public class TestHierarchyTest extends ComponentTestFixture {

    private TestHierarchy hierarchy;

    protected Hierarchy createHierarchy() { 
        return new TestHierarchy(); 
    }

    protected void setUp() {
        hierarchy = (TestHierarchy)getHierarchy();
        Log.addDebugClass(TestHierarchy.class);
        Log.setShowThreads(true);
    }
    
    protected void hideWindow(Window w) {
        // Explicitly wait for the window close event
        // before checking for results to avoid timing errors; not sure why
        // there's a delay though (waitForIdle doesn't work in this case).
        class Flag { volatile boolean closed; }
        final Flag flag = new Flag();
        w.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                e.getWindow().removeWindowListener(this);
                flag.closed = true;
            }
        });
        super.hideWindow(w);
        while (!flag.closed) {
            getRobot().sleep();
        }
    }

    public void testFilterNewIgnoredWindows() throws Exception {
        TestHierarchy hierarchy = new TestHierarchy();
        Frame f = showFrame(new JLabel(getName()));
        assertTrue("Frame should be in hierarchy", hierarchy.contains(f));

        hierarchy.dispose(f);
        showWindow(f);
        assertTrue("Frame should no longer appear in hierarchy",
                   !hierarchy.contains(f));

        JDialog d = new JDialog(f, getName());
        d.getContentPane().add(new JLabel("dialog"));
        showWindow(d);
        assertTrue("Filtered frame dialog should not appear in hierarchy",
                   !hierarchy.contains(d));

        disposeWindow(d);
        assertTrue("Disposed dialog should not appear in hierarhcy",
                   !hierarchy.contains(d));

        showWindow(d);
        assertTrue("Redisplayed dialog should not appear in hierarchy",
                   !hierarchy.contains(d));
    }

    public void testAutoFilterDisposedWindows() throws Exception {
        JButton openButton = new JButton("open");
        final Frame f = showFrame(openButton);
        class Flag { volatile boolean flag = true; }
        final Flag flag = new Flag();
        final String CLOSE = "close";
        class TestDialog extends JDialog {
            public TestDialog(final boolean dispose) {
                super(f, TestHierarchyTest.this.getName());
                JButton close = new JButton(CLOSE);
                close.setName(CLOSE);
                getContentPane().add(close);
                close.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (dispose)
                            TestDialog.this.dispose();
                        else
                            TestDialog.this.setVisible(false);
                    }
                });
                setModal(true);
            }
        }
        openButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog d = new TestDialog(flag.flag);
                d.pack();
                d.setVisible(true);
            }
        });
        ComponentTester tester = new ComponentTester();
        tester.actionClick(openButton);
        JButton closeButton = (JButton)
            getFinder().find(new NameMatcher(CLOSE));
        tester.actionClick(closeButton);

        // This will bring up a new dialog; matching should match the new
        // dialog 
        tester.actionClick(openButton);
        JButton closeButton2 = (JButton)
            getFinder().find(new NameMatcher(CLOSE));
        assertTrue("Should pick up new button, not old one: " + closeButton,
                   !closeButton2.equals(closeButton));
        tester.actionClick(closeButton2);

        // Now don't do the dispose, and we expect to always get the same match
        flag.flag = false;
        tester.actionClick(openButton);
        closeButton = (JButton)
            getFinder().find(new NameMatcher(CLOSE));
        tester.actionClick(closeButton);

        tester.actionClick(openButton);
        closeButton2 = (JButton)
            getFinder().find(new NameMatcher(CLOSE));
        assertEquals("Second lookup should match first",
                     closeButton, closeButton2);
    }
    

    public void testFilterMenuItem() throws Exception {
        JFrame frame = new JFrame(getName());
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem mi = new JMenuItem("Item");
        menu.add(mi);
        mb.add(menu);
        frame.setJMenuBar(mb);
        showWindow(frame);
        hierarchy.dispose(frame);
        assertTrue("Hierarchy should no longer contain frame",
                   !hierarchy.contains(frame));
        assertTrue("Hierarchy should no longer contain menu bar",
                   !hierarchy.contains(mb));
        assertTrue("Hierarchy should no longer contain menu",
                   !hierarchy.contains(menu));
        assertTrue("Hierarchy should no longer contain menu item",
                   !hierarchy.contains(mi));
    }

    public void testAutoRemoveFileChooserDialogs() throws Exception {
        final JFileChooser chooser = new JFileChooser();
        final Frame frame = showFrame(new JLabel(getName()));
        Dialog d1 = showModalDialog(new Runnable() {
            public void run() {
                chooser.showOpenDialog(frame);
            }
        });
        hideWindow(d1);
        assertTrue("Transient file chooser dialog should now be filtered: "
                   + d1.getName(),
                   hierarchy.isFiltered(d1));
        assertTrue("Transient file chooser dialog should no longer be in the hierarchy",
                   !hierarchy.contains(d1));
    }

    public void testAutoRemoveJOptionPaneShowConfirm() throws Exception {
        final JLabel confirm = new JLabel("confirm");
        Runnable r = new Runnable() {
            public void run() {
                JOptionPane.showConfirmDialog(null, confirm);
            }
        };
        Dialog d = showModalDialog(r);
        hideWindow(d);
        assertTrue("Transient option pane dialog should now be filtered: "
                   + d.getName(),
                   hierarchy.isFiltered(d));
        assertTrue("Transient option pane dialog should no longer be in the hierarchy",
                   !hierarchy.contains(d));
    }

    public void testAutoRemoveJOptionPaneShowInput() throws Exception {
        final JLabel input = new JLabel("input");
        Runnable r = new Runnable() {
            public void run() {
                JOptionPane.showInputDialog(null, input);
            }
        };
        Dialog d = showModalDialog(r);
        hideWindow(d);
        assertTrue("Transient option pane dialog should now be filtered: "
                   + d.getName(),
                   hierarchy.isFiltered(d));
        assertTrue("Transient option pane dialog should no longer be in the hierarchy",
                   !hierarchy.contains(d));
    }

    public void testAutoRemoveJOptionPaneShowMessage() throws Exception {
        final JLabel message = new JLabel("message");
        Runnable r = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, message);
            }
        };
        Dialog d = showModalDialog(r);
        hideWindow(d);
        assertTrue("Transient option pane dialog should now be filtered: "
                   + d.getName(),
                   hierarchy.isFiltered(d));
        assertTrue("Transient option pane dialog should no longer be in the hierarchy",
                   !hierarchy.contains(d));
    }

    // 1.4+ only
    private Popup p1, p2;
    public void testFindReusedPopup() {
        final JFrame frame = (JFrame)showFrame(new JTextField(getName()));
        final JLabel message = new JLabel(getName());
        message.setSize(message.getPreferredSize());
        final PopupFactory f = PopupFactory.getSharedInstance();
        invokeAndWait(new Runnable() { public void run() {
            p1 = f.getPopup(frame.getContentPane(), message, 
                            frame.getX() + frame.getWidth(), frame.getY());
            p1.show();
        }});
        try { 
            Component c = getFinder().find(new ClassMatcher(JLabel.class));
            assertEquals("Wrong component found", message, c);
        }
        catch(Exception e) {
            fail("Popup and contents not found");
        }
        // force a redisplay before the dispose of the first gets processed
        invokeAndWait(new Runnable() { public void run() {
            p1.hide();
            p2 = f.getPopup(frame.getContentPane(), message, 
                            frame.getX()+frame.getWidth(), frame.getY());
            p2.show();
        }});
        //getRobot().delay(300000);
        try {
            Component c = getFinder().find(new ClassMatcher(JLabel.class));
            assertEquals("Wrong component found after hide/show", message, c);
        }
        catch(Exception e) {
            fail("Popup and contents not found after hide/show");
        }
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, TestHierarchyTest.class);
    }
}
