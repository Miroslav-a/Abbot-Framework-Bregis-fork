package abbot.script;

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import abbot.Log;
import abbot.finder.*;
import abbot.finder.matchers.*;

/** 
 * Verify various ComponentReference requirements.
 */
// TODO: test new refs aren't added to Resolver
// TODO: new refs all get unique IDs
// TODO: more clearly delineate rules instead of random tests
public class ComponentReferenceTest
    extends ResolverFixture implements XMLConstants {

    private Map newRefs;
    private Resolver resolver;
    protected void setUp() {
        newRefs = new HashMap();
        resolver = getResolver();
        // allow easy creation of references, but force initial lookups
        ComponentReference.cacheOnCreation = false;
    }
    protected void tearDown() {
        newRefs = null;
        resolver = null;
        ComponentReference.cacheOnCreation = true;
    }

    public void testInnerClassMatch() {
        ComponentReference cr =
            new ComponentReference(resolver, Component.class, new HashMap());
        assertTrue("Anonymous inner class not detected",
                   cr.expressionMatch(ComponentReference.ANON_INNER_CLASS,
                                      "my.class.Name$12"));
        assertTrue("Anonymous inner class falsely detected",
                   !cr.expressionMatch(ComponentReference.ANON_INNER_CLASS,
                                       "my.class.Name$InnerClass"));
    }

    /** Saved class name should not be an anonymous or inner class. */
    public void testUseCanonicalClass() {
        ComponentReference cr =
            new ComponentReference(resolver, new Frame(getName()) {
                public String toString() { return getName(); }
            });
        assertEquals("Wrong class saved", Frame.class.getName(),
                     cr.getRefClassName());
    }

    /** Any newly created reference should match against the component it was
     * created from.
     */
    public void testAddComponent() throws Throwable {
        JTextField tf = new JTextField();
        tf.setColumns(20);
        showFrame(tf);
        ComponentReference ref = resolver.addComponent(tf);
        assertEquals("Newly created component reference does not match",
                     tf, ref.getComponent());
    }

    /** Create a reference for a dialog child of a dialog.
        Bug reported by vladgur.
    */
    public void testAddSubDialog() {
        JFrame frame = new JFrame(getName());
        JDialog d1 = new JDialog(frame, "Modal", true);
        d1.getContentPane().add(new JLabel("Modal Dialog"));
        JDialog d2 = new JDialog(d1, "Non-Modal", false);
        d2.getContentPane().add(new JLabel("Non-Modal Dialog"));
        showWindow(d1);
        showWindow(d2);
        ComponentReference ref =
            new ComponentReference(resolver, d2.getContentPane(), newRefs);
        assertNotNull("Can't create subdialog reference", ref);
    }

    /**
     * Test to see if the label and index can identify a component
     */
    public void testSiblings() {
        JTextField tf1 = new JTextField();
        JTextField tf2 = new JTextField();
        tf1.setColumns(10);
        tf2.setColumns(10);
        JPanel pane = new JPanel();
        pane.add(tf1);
        pane.add(tf2);

        showFrame(pane);

        ComponentReference cr1 = resolver.addComponent(tf1);
        ComponentReference cr2 = resolver.addComponent(tf2);
        assertTrue("Should get two unique references", cr1 != cr2);

        try {
            assertEquals("Incorrect sibling 1 lookup with " + cr1,
                         tf1, cr1.getComponent());
            assertEquals("Incorrect sibling 2 lookup with " + cr2,
                         tf2, cr2.getComponent());
        }
        catch(ComponentNotFoundException cnf) {
            fail(cnf.toString());
        }
        catch(MultipleComponentsFoundException mcf) {
            fail(mcf.toString());
        }
    }


    /**
     * Verify two identical sibling components can be distinguished.
     */
    public void testIdenticalSiblings() {
        JButton button1 = new JButton("Button");
        JButton button2 = new JButton("Button");
        JPanel pane = new JPanel();
        pane.add(button1);
        pane.add(button2);

        showFrame(pane);

        ComponentReference cr1 = resolver.addComponent(button1);
        ComponentReference cr2 = resolver.addComponent(button2);
        assertTrue("Should get two unique references", cr1 != cr2);

        try {
            assertEquals("Incorrect sibling button 1 lookup with " + cr1,
                         button1, cr1.getComponent());
            assertEquals("Incorrect sibling button 2 lookup with " + cr2,
                         button2, cr2.getComponent());
        }
        catch(ComponentNotFoundException cnf) {
            fail(cnf.toString());
        }
        catch(MultipleComponentsFoundException mcf) {
            fail(mcf.toString());
        }
    }

    
    /**
     * Verify two identical cousin (sibling parents) components can be
     * distinguished. 
     */
    public void testIdenticalCousins() {
        JButton button1 = new JButton("Button");
        JButton button2 = new JButton("Button");
        JPanel  panel1 = new JPanel();
        JPanel  panel2 = new JPanel();
        panel1.add(button1);
        panel2.add(button2);

        JPanel pane = new JPanel();
        pane.add(panel1);
        pane.add(panel2);

        showFrame(pane);

        ComponentReference cr1 = resolver.addComponent(button1);
        ComponentReference cr2 = resolver.addComponent(button2);
        assertTrue("Should get two unique references", cr1 != cr2);

        try {
            assertEquals("Incorrect cousin button 1 lookup with " + cr1,
                         button1, cr1.getComponent());
            assertEquals("Incorrect cousin button 2 lookup with " + cr2,
                         button2, cr2.getComponent());
        }
        catch(ComponentNotFoundException cnf) {
            fail(cnf.toString());
        }
        catch(MultipleComponentsFoundException mcf) {
            fail(mcf.toString());
        }
    }

    /**
     * Test to see if two buttons which are identical except for their name
     * and index can be found
     */
    public void testTwoNamedButtons() {
        JButton button1 = new JButton("Button");
        JButton button2 = new JButton("Button");
        JPanel  panel1 = new JPanel();
        JPanel  panel2 = new JPanel();
        
        button1.setName("Button1");
        button2.setName("Button2");

        panel1.add(button1);
        panel2.add(button2);
        JPanel pane = new JPanel();
        pane.add(panel1);
        pane.add(panel2);

        showFrame(pane);

        ComponentReference cr1 = resolver.addComponent(button1);
        ComponentReference cr2 = resolver.addComponent(button2);
        assertTrue("Should get two unique references", cr1 != cr2);

        try {
            assertEquals("Button 1 not found", button1, cr1.getComponent());
            assertEquals("Button 2 not found", button2, cr2.getComponent());
        }
        catch(ComponentNotFoundException cnf) {
            fail(cnf.toString());
        }
        catch(MultipleComponentsFoundException mcf) {
            fail(mcf.toString());
        }
    }

    /**
     * Test to ensure that index can be used on otherwise indistingushable 
     * components
     */
    public void testIdenticalLabelGroup() {
        
        final int NUM_LABELS = 10;

        ArrayList labels = new ArrayList();
        ArrayList refs = new ArrayList();

        JPanel pane = new JPanel();
        showFrame(pane);
        for (int i=0; i < NUM_LABELS; ++i){
            JLabel label = new JLabel("Empty");
            labels.add(label);
            pane.add(label);
            refs.add(resolver.addComponent(label));
        }
        
        for (int i=0;i < NUM_LABELS - 1; ++i) {
            for (int next=i+1;next < NUM_LABELS;next++) {
                Component c1 = (Component)labels.get(i);
                Component c2 = (Component)labels.get(next);
                ComponentReference cr1 = (ComponentReference)refs.get(i);
                ComponentReference cr2 = (ComponentReference)refs.get(next);

                try {
                    assertEquals("label " + i + " not found", c1, cr1.getComponent());
                    assertEquals("label " + next + " not found", c2, cr2.getComponent());
                }
                catch(ComponentNotFoundException cnf) {
                    fail(cnf.toString());
                }
                catch(MultipleComponentsFoundException mcf) {
                    fail(mcf.toString());
                }
            }
        }
    }

    /** Ensure that find window works with JOptionPane-generated
     * components. */
    public void testDuplicateJOptionPanes() throws Throwable {
        JOptionPane pane = new JOptionPane("A message", JOptionPane.
                                           INFORMATION_MESSAGE);
        JFrame frame = new JFrame(getName());
        Dialog dialog = pane.createDialog(frame, "Dialog");
        showWindow(dialog);
        dialog.setVisible(false);
        Timer timer = new Timer();
        while (dialog.isShowing()) {
            if (timer.elapsed() > 5000)
                throw new RuntimeException("Timed out waiting for dialog to hide");
            getRobot().sleep();
        }
        JOptionPane pane2 = new JOptionPane("A message", JOptionPane.
                                            INFORMATION_MESSAGE);
        Dialog d2 = pane2.createDialog(frame, "Dialog 2");
        showWindow(d2);
        Component comp = getFinder().find(new WindowMatcher(d2.getTitle()));
        assertEquals("Wrong JOptionPane found", d2, comp);
    }

    private class ButtonMatcher implements Matcher {
        private String text = null;
        public ButtonMatcher() {
            this(null);
        }
        public ButtonMatcher(String text) {
            this.text = text;
        }
        public boolean matches(Component c) {
            return c instanceof JButton
                && (text == null || ((JButton)c).getText().equals(text));
        }
    }

    /** Component reference matching one dialog should match an identical
     * subsequent one using the same JOptionPane.
     */
    public void testSubsequentDialogContentsLookup() throws Throwable {
        ComponentReference ref =
            new ComponentReference(resolver, javax.swing.JButton.class,
                                   new String[][] {{
                                       XMLConstants.TAG_TEXT, "OK" }});
        Dialog d1 = null;
        Dialog d2 = null;
        JFrame frame = new JFrame(getName());
        JOptionPane pane = new JOptionPane("Dialog", JOptionPane.
                                           INFORMATION_MESSAGE);
        d1 = pane.createDialog(frame, "Dialog");
        showWindow(d1);
        JButton button = (JButton)getFinder().find(d1, new ButtonMatcher());
        assertNotNull("Button shouldn't be null", button);
        assertEquals("Wrong component found",
                     button, ref.getComponent(getHierarchy()));
        d1.setVisible(false);

        d2 = pane.createDialog(frame, "Dialog");
        showWindow(d2);
        JButton button2 = (JButton)getFinder().find(d2, new ButtonMatcher());
        assertNotNull("Second button lookup shouldn't be null", button2);
        assertEquals("Same button should be used", button2, button);
        assertEquals("Wrong component found with " + ref,
                     button2, ref.getComponent(getHierarchy()));
    }

    public void testJFileChooserReferences() throws Exception {
        final JFileChooser chooser = new JFileChooser();
        final Frame frame = showFrame(new JLabel(getName()));
        Dialog d1 = showModalDialog(new Runnable() {
            public void run() {
                chooser.showOpenDialog(frame);
            }
        });
        // Don't care if we get Open or Cancel
        JButton button = (JButton)getFinder().
            find(new ClassMatcher(JButton.class, true));

        ComponentReference ref = resolver.addComponent(d1);
        ComponentReference bref = resolver.addComponent(button);
                                                         
        assertEquals("First file dialog not found", d1, ref.getComponent());
        assertEquals("Dialog button not found", button, bref.getComponent());
        // must dispose, not hide; otherwise it will still match
        disposeWindow(d1);

        Dialog d2 = showModalDialog(new Runnable() {
            public void run() {
                chooser.showOpenDialog(frame);
            }
        });
        assertTrue("JFileChooser presented same dialog", d1 != d2);
        assertEquals("Incorrect subsequent file dialog found with " + ref,
                     d2, ref.getComponent());
        assertEquals("Same button should be used on both dialogs",
                     button, bref.getComponent());
    }

    public void testFindMenuItem() throws Throwable {
        JMenuItem mi = new JMenuItem("item");
        JMenu menu = new JMenu("menu");
        JMenuBar mb = new JMenuBar();
        mb.add(menu);
        menu.add(mi);
        JFrame frame = new JFrame(getName());
        frame.setJMenuBar(mb);
        showWindow(frame);
        ComponentReference ref = resolver.addComponent(mi);
        assertEquals("Menu item not found: " + ref,
                     mi, ref.getComponent());
    }

    public void testConsistentDefaultFrameReference() throws Throwable {
        // Default frame should always get the same reference
        JOptionPane pane = new JOptionPane("Dialog", JOptionPane.
                                           INFORMATION_MESSAGE);
        Dialog d1 = pane.createDialog(null, "Dialog");
        showWindow(d1);
        ComponentReference ref1 = resolver.addComponent(d1);
        d1 = pane.createDialog(null, "Dialog2");
        showWindow(d1);
        ComponentReference ref2 = resolver.addComponent(d1);
        assertEquals("Option pane dialogs should share same default frame",
                     ref1.getAttribute(TAG_PARENT),
                     ref2.getAttribute(TAG_PARENT));
        assertEquals("Incorrect ID for shared frame instance",
                     ComponentReference.SHARED_FRAME_ID,
                     ref1.getAttribute(TAG_PARENT));
    }

    public void testRootReference() {
        JFrame frame = new JFrame(getName());
        showWindow(frame);
        Dialog d = new JDialog(frame, getName() + " Dialog");
        showWindow(d);
        ComponentReference ref = resolver.addComponent(frame);
        ComponentReference ref1 = resolver.addComponent(d);
        assertEquals("Component should be root",
                     "true", ref.getAttribute(TAG_ROOT));
        assertNull("Component should not be root",
                   ref1.getAttribute(TAG_ROOT));
    }

    public void testFindDialogWithHiddenFrame() throws Throwable {
        JFrame frame = new JFrame(getName());
        Dialog d1 = new JDialog(frame, getName() + " Dialog");
        showWindow(d1);
        ComponentReference ref = resolver.addComponent(d1);
        assertEquals("Dialog not found", d1, ref.getComponent());
    }

    public void testIndexlessHorizontalOrdering() throws Throwable {
        // Ensure that several components differing only by horizontal
        // location can be distinguished.  Use Frames, so that the parent
        // index does not come into play.
        // Note that this ordering is only really meaningful to embedded
        // frames (and thus applets) which can not otherwise be moved or
        // resized independengly of one another.
        Component c1 = new InvisibleShowingFrame("Same");
        Component c2 = new InvisibleShowingFrame("Same");
        Component c3 = new InvisibleShowingFrame("Same");
        c1.setLocation(100, 100); c1.setSize(100, 100);
        c2.setLocation(200, 100); c2.setSize(100, 100);
        c3.setLocation(300, 100); c3.setSize(100, 100);

        ComponentReference cr1 = resolver.addComponent(c1);
        ComponentReference cr2 = resolver.addComponent(c2);
        ComponentReference cr3 = resolver.addComponent(c3);

        assertNotNull("Leftmost frame not found", cr1.getComponent());
        assertNotNull("Middle frame not found", cr2.getComponent());
        assertNotNull("Rightmost frame not found", cr3.getComponent());

        c1.setLocation(200, 100); 
        c2.setLocation(300, 100); 
        c3.setLocation(400, 100); 

        assertNotNull("Leftmost frame not found", cr1.getComponent());
        assertNotNull("Middle frame not found", cr2.getComponent());
        assertNotNull("Rightmost frame not found", cr3.getComponent());
    }

    public void testDescendentIndexlessHorizontalOrdering() throws Throwable {
        // Similar to the IndexlessHorizontalOrdering, except that children
        // are checked, where the only difference is the window reference.
        Frame f1 = new InvisibleShowingFrame("Same");
        Frame f2 = new InvisibleShowingFrame("Same");
        Component c1 = new JLabel(getName());
        Component c2 = new JLabel(getName());
        f1.add(c1);
        f2.add(c2);
        f1.setLocation(100, 100); f1.setSize(100, 100);
        f2.setLocation(200, 100); f2.setSize(100, 100);
        ComponentReference cr1 = resolver.addComponent(c1);
        ComponentReference cr2 = resolver.addComponent(c2);
        assertNotNull("Leftmost component not found", cr1.getComponent());
        assertNotNull("Rightmost component not found", cr2.getComponent());
    }

    public void testIndexlessVerticalOrdering() throws Throwable {
        // Ensure that several components differing only by vertical
        // location can be distinguished.  Use Frames, so that the parent
        // index does not come into play.
        // Note that this ordering is only really meaningful to embedded
        // frames (and thus applets) which can not otherwise be moved or
        // resized independengly of one another.
        Component c1 = new InvisibleShowingFrame("Same");
        Component c2 = new InvisibleShowingFrame("Same");
        Component c3 = new InvisibleShowingFrame("Same");
        c1.setLocation(100, 100); c1.setSize(100, 100);
        c2.setLocation(100, 200); c2.setSize(100, 100);
        c3.setLocation(100, 300); c3.setSize(100, 100);

        ComponentReference cr1 = resolver.addComponent(c1);
        ComponentReference cr2 = resolver.addComponent(c2);
        ComponentReference cr3 = resolver.addComponent(c3);

        assertNotNull("Top frame not found", cr1.getComponent());
        assertNotNull("Middle frame not found", cr2.getComponent());
        assertNotNull("Bottom frame not found", cr3.getComponent());

        c1.setLocation(100, 200); 
        c2.setLocation(100, 300); 
        c3.setLocation(100, 400); 

        assertNotNull("Top frame not found", cr1.getComponent());
        assertNotNull("Middle frame not found", cr2.getComponent());
        assertNotNull("Bottom frame not found", cr3.getComponent());
    }

    private class InvisibleShowingFrame extends Frame {
        private Point location;
        public InvisibleShowingFrame(String title) { super(title); }
        public boolean isShowing() { return true; }
        // All other methods defer to this one
        public void setBounds(int x, int y, int width, int height) {
            super.setBounds(x, y, width, height);
            if (location == null)
                location = new Point(x, y);
            else
                location.setLocation(x, y);
        }
        public Point getLocationOnScreen() { return getLocation(); }
        public Point getLocation() {
            if (location == null)
                location = new Point(0, 0);
            return location;
        }
    }

    public void testOrder() {
        Component c1 = new InvisibleShowingFrame("Same");
        Component c2 = new InvisibleShowingFrame("Same");
        Component c3 = new InvisibleShowingFrame("Same");
        c1.setLocation(100, 100); c1.setSize(100, 100);
        c2.setLocation(100, 200); c2.setSize(100, 100);
        c3.setLocation(200, 200); c3.setSize(100, 100);
        Component[] all = { c1, c2, c3 };
        assertEquals("Wrong leftmost order",
                     "0", ComponentReference.getOrder(c1, all, true));
        assertEquals("Wrong middle order",
                     "0", ComponentReference.getOrder(c2, all, true));
        assertEquals("Wrong rightmost order",
                     "1", ComponentReference.getOrder(c3, all, true));
        assertEquals("Wrong top order",
                     "0", ComponentReference.getOrder(c1, all, false));
        assertEquals("Wrong middle order",
                     "1", ComponentReference.getOrder(c2, all, false));
        assertEquals("Wrong bottom order",
                     "1", ComponentReference.getOrder(c3, all, false));

    }

    private static final int MAX_CREATION_TIME = 10000; //ms
    public void testBasicReferenceCreationPerformance() throws Throwable {
        // Make sure caching is on, since that's what's being measured
        ComponentReference.cacheOnCreation = true;
        // NOTE: need for this test was triggered by slowness in recording.
        // Scrollpanes (maybe w/all their buttons?) cause significant
        // slowdown. 
        JPanel p1 = new JPanel(); 
        JButton b1 = new JButton("Label");
        p1.add(b1);
        p1.add(new JScrollPane(new JButton("Label"))); 
        p1.add(new JScrollPane(new JTextField("Label")));
        p1.add(new JScrollPane(new JComboBox())); 
        p1.add(new JScrollPane(new JTree())); 
        Frame f1 = showFrame(p1);
        getRobot().move(f1, 100, 100);
        getRobot().waitForIdle();

        Timer timer = new Timer();
        ComponentReference ref1 = resolver.addComponent(b1);
        long elapsed = timer.elapsed();
        assertTrue("Too long to create reference: " + elapsed + "ms",
                   elapsed < MAX_CREATION_TIME);
        if (elapsed > MAX_CREATION_TIME / 5)
            Log.warn("Slow cref creation time: " + elapsed + "ms");

        JPanel p2 = new JPanel(); 
        JButton b2 = new JButton("Label");
        p2.add(b2);
        p2.add(new JScrollPane(new JButton("Label")));
        p2.add(new JScrollPane(new JTextField("Label")));
        p2.add(new JScrollPane(new JComboBox()));
        p2.add(new JScrollPane(new JTree()));
        Frame f2 = showFrame(p2);
        getRobot().move(f2, 101, 100);
        getRobot().waitForIdle();

        timer.reset();
        ComponentReference ref2 = resolver.addComponent(b2);
        elapsed = timer.elapsed();
        assertTrue("Too long to create reference (using ordering): " 
                   + elapsed, elapsed < MAX_CREATION_TIME);
        if (elapsed > MAX_CREATION_TIME / 5)
            Log.warn("Slow cref creation time: " + elapsed + "ms");
    }

    private static final int DEPTH = 10; 
    private class NestedPanel extends JPanel {
        public NestedPanel(String name, int level) {
            setBorder(new EmptyBorder(0,0,0,0));
            if (level > 0)
                add(new NestedPanel(name, --level));
            else {
                JButton b = new JButton(name);
                add(b);
                b.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JOptionPane.
                            showMessageDialog(null, "message", null,
                                              JOptionPane.PLAIN_MESSAGE);
                    }
                });
            }
        }
    }
    private class MyFrame extends JFrame {
        public MyFrame(String name) {
            super(name);
            JPanel p = new JPanel();
            p.add(new NestedPanel("Submit " + name, DEPTH));
            p.add(new NestedPanel("Retrieve " + name, DEPTH));
            p.add(new NestedPanel("New " + name, DEPTH));
            p.add(new NestedPanel("Edit " + name, DEPTH));
            p.add(new NestedPanel("Search " + name, DEPTH));
            setContentPane(p);
        }
    }

    /** Create a number of references which have no current match, then
        test creation of a new reference based on an existing component.
    */
    public void testReferenceCreationPerformanceWithMatchFailures()
        throws Exception {
        // Make sure caching is on, since that's what's being measured
        ComponentReference.cacheOnCreation = true;

        // Numbers in parens are with cacheing of unresolved ref results
        // enabled; cacheing multiple match failures as well as not found
        // failures results in a reasonable creation time. (winxp, 1.2GHz)
        // 0=19s (4s), gets exponentially worse
        // 1=70s (15s)
        // 2=176s (38s)
        //
        // With full cacheing enabled:
        //  N=time  component count
        // ------------------------
        // 10=<1s    197
        // 20=1.2s   347
        // 40=3.6s   647
        // 80=15s   1247 
        //
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(null, "cref", null, 
                                              JOptionPane.PLAIN_MESSAGE);
            }
        });
        Timer timer = new Timer();
        while (true) {
            try {
                JButton button = (JButton)getFinder().
                    find(new ClassMatcher(JButton.class));
                // Create a reference, then dispose of its match
                ComponentReference existing = getResolver().addComponent(button);
                getHierarchy().dispose(SwingUtilities.getWindowAncestor(button));
                break;
            }
            catch(ComponentNotFoundException e) {
                getRobot().waitForIdle();
            }
            if (timer.elapsed() > 10000)
                fail("Timed out waiting for transient dialog to show");
        }

        Frame fa = new MyFrame("A");
        Frame fb = new MyFrame("B");
        Frame fc = new MyFrame("C");
        showWindow(fa);
        showWindow(fb);
        showWindow(fc);

        // find a button, any button will do
        JButton button2 = (JButton)getFinder().
            find(fa, new ClassMatcher(JButton.class));
        
        timer.reset();
        ComponentReference cref = resolver.addComponent(button2);

        assertTrue("Maximum reference creation time exceeded, elapsed time="
                   + timer.elapsed() + "ms",
                   timer.elapsed() < MAX_CREATION_TIME);
        if (timer.elapsed() > MAX_CREATION_TIME / 5)
            Log.warn("Slow cref creation time: " + timer.elapsed() + "ms");
    }

    /** References which match a non-showing component still have to
     * re-match.
     */
    public void testReferenceCreationPerformanceWithNonShowingMatches() 
        throws Exception {
        // Make sure caching is on, since that's what's being measured
        ComponentReference.cacheOnCreation = true;

        Frame fa = new MyFrame("A");
        Frame fb = new MyFrame("B");
        Frame fc = new MyFrame("C");
        showWindow(fa);
        showWindow(fb);
        showWindow(fc);

        // find a button, any button will do
        JButton button2 = (JButton)getFinder().
            find(fa, new ClassMatcher(JButton.class));
        JButton button3 = (JButton)getFinder().
            find(fb, new ClassMatcher(JButton.class));
        
        ComponentReference cref = resolver.addComponent(button2);
        fa.setVisible(false);
        Timer timer = new Timer();
        ComponentReference cref2 = resolver.addComponent(button3);

        assertTrue("Maximum reference creation time exceeded, elapsed time="
                   + timer.elapsed() + "ms",
                   timer.elapsed() < MAX_CREATION_TIME);
        if (timer.elapsed() > MAX_CREATION_TIME / 5)
            Log.warn("Slow cref creation time: " + timer.elapsed() + "ms");
    }

    private static int appletCount = 0;
    private class InvisibleApplet extends Applet {
        public InvisibleApplet(final URL url,
                               final String p1, final String p2) {
            setStub(new AppletStub() {
                public void appletResize(int w, int h) { }
                public AppletContext getAppletContext() {
                    return null;
                }
                public URL getCodeBase() { return null; }
                public URL getDocumentBase() { return url; }
                public String getParameter(String name) {
                    return "p1".equals(name) ? p1 : p2;
                }
                public boolean isActive() { return true; }
            });
            // Install into a frame so that it can be found
            // Note that all applet frames have a unique location
            Frame f = new InvisibleShowingFrame("Dummy Applet Frame");
            f.add(InvisibleApplet.this);
            f.setLocation(appletCount++, 0);
        }
        public String[][] getParameterInfo() {
            String[][] info = {
                { "p1", "" },
                { "p2", "" },
            };
            return info;
        }
        public boolean isShowing() { return true; }
    }

    public void testFindAppletsByParameter() throws Throwable {
        // Several identical applets with different parameters
        URL url = new URL("http://somewhere.com");
        Component c1 = new InvisibleApplet(url, "true", "true");
        Component c2 = new InvisibleApplet(url, "true", "false");
        ComponentReference cr1 = resolver.addComponent(c1);
        ComponentReference cr2 = resolver.addComponent(c2);
        assertNotNull("Could not resolve applet 1 by parameter value",
                      cr1.getComponent());
        assertNotNull("Could not resolve applet 2 by parameter value",
                      cr2.getComponent());
    }

    public void testFindAppletsByDocumentBase() throws Throwable {
        // Several identical applets with different document base
        URL url1 = new URL("http://somewhere.com");
        URL url2 = new URL("http://somewhereelse.com");
        Component c1 = new InvisibleApplet(url1, "true", "true");
        Component c2 = new InvisibleApplet(url2, "true", "true");
        ComponentReference cr1 = resolver.addComponent(c1);
        ComponentReference cr2 = resolver.addComponent(c2);
        assertNotNull("Could not resolve applet 1 by doc base", cr1.getComponent());
        assertNotNull("Could not resolve applet 2 by doc base", cr2.getComponent());
    }

    public void testDynamicButtonLabel() throws Throwable {
        JPanel pane = new JPanel();
        JPanel pane1 = new JPanel();
        JPanel root = new JPanel();
        final JButton button = new JButton("Open");
        pane.add(button);
        pane.add(new JButton("Wink"));
        pane.add(new JButton("Yank"));
        root.add(pane);

        pane1.add(new JButton("This"));
        pane1.add(new JButton("That"));
        root.add(pane1);
        showFrame(root);
        ComponentReference ref = resolver.addComponent(button);
        getRobot().click(button);
        getRobot().waitForIdle();
        button.setText("Close");

        ref.setAttribute(XMLConstants.TAG_TEXT, "/Open|Close/");
        assertNotNull("Could not find button after label change",
                      ref.getComponent());
    }

    // These factory-created methods check for factory-created components
    // where a subsequent version is shown without necessarily disposing the
    // previous one

    public void testFactoryWithUnparentPrevious() throws Exception {
        JButton ok1 = new JButton("ok") { public String toString() {
            return "ok 1"; }};
        ok1.setName("ok");
        JButton ok2 = new JButton("ok") { public String toString() {
            return "ok 2"; }};
        ok2.setName("ok");
        Frame f1 = showFrame(ok1);
        ComponentReference ref = getResolver().addComponent(ok1);
        assertEquals("Initial lookup failed for " + ref,
                     ok1, ref.getComponent());
        // disposal by removing references
        hideWindow(f1);
        f1.removeAll();

        Frame f2 = showFrame(ok2);
        assertEquals("Cached value should not be used if it has no Window",
                     ok2, ref.getComponent());
    }

    public void testFactoryPreferShowing() throws Exception {
        JButton ok1 = new JButton("ok") { public String toString() {
            return "ok 1"; }};
        ok1.setName("ok");
        JButton ok2 = new JButton("ok") { public String toString() {
            return "ok 2"; }};
        ok2.setName("ok");
        Frame f1 = showFrame(ok1);
        ComponentReference ref = getResolver().addComponent(ok1);
        assertEquals("Initial lookup failed for " + ref,
                     ok1, ref.getComponent());
        // simulate "I forgot to dispose"
        hideWindow(f1);

        Frame f2 = showFrame(ok2);
        assertEquals("Showing component should be preferred for " + ref,
                     ok2, ref.getComponent());
    }

    public void testFactorySelectAmongHidden() throws Exception {
        JButton ok1 = new JButton("ok") { public String toString() {
            return "ok 1"; }};
        ok1.setName("ok");
        JButton ok2 = new JButton("ok") { public String toString() {
            return "ok 2"; }};
        ok2.setName("ok");
        Frame f1 = showFrame(ok1);
        ComponentReference ref = getResolver().addComponent(ok1);
        assertEquals("Initial lookup failed for " + ref,
                     ok1, ref.getComponent());
        hideWindow(f1);

        Frame f2 = showFrame(ok2);
        hideWindow(f2);

        assertEquals("Cached result should be used if all options are hidden",
                     ok1, ref.getComponent());
    }

    /** Thanks to jimdoyle@users.sf.net for tracking this down. */
    public void testFindJTabbedPaneDescendant() {
        JTabbedPane tabs = new JTabbedPane();
        JButton button = null;
        int NEST = 10; // 2->750ms, 3->1.4s, 4->7s, 5->142s
        int TABS = 2;
        for(int t=0;t < TABS;t++) {
            Container parent = tabs;
            JPanel panel;
            for (int i=0;i < NEST;i++) {
                panel = new JPanel();
                parent.add(panel);
                parent = panel;
            }
            parent.add(button = new JButton("tab " + t));
        }
        showFrame(tabs);
        Timer timer = new Timer();
        ComponentReference ref = resolver.addComponent(button);
        long elapsed = timer.elapsed();
        if (elapsed > 500) 
            fail("Creation of non-visible tabbed pane component took "
                 + timer.elapsed() + "ms");
    }

    public void testFindJScrollBar() {
        JScrollPane pane = new JScrollPane(new JLabel(getName()));
        pane.setHorizontalScrollBarPolicy(JScrollPane.
                                          HORIZONTAL_SCROLLBAR_NEVER);
        showFrame(pane);
        ComponentReference ref =resolver.addComponent(pane.getHorizontalScrollBar());
        assertTrue("Could not create ref", ref != null);
    }

    public void testBestExistingMatch() throws Exception {
        JPanel panel1 = new JPanel();
        JButton b1 = new JButton("b1");
        panel1.add(b1);
        JPanel panel2 = new JPanel();
        JButton b2 = new JButton("b2");
        panel2.add(b2);
        JPanel top = new JPanel();
        top.add(panel1); top.add(panel2);
        showFrame(top);

        ComponentReference ref1 = resolver.addComponent(b1);
        ComponentReference ref2 = resolver.addComponent(b2);

        Collection refs = resolver.getComponentReferences();
        assertEquals("Incorrect first match", ref1, 
                     ComponentReference.matchExisting(b1, refs));
        assertEquals("Incorrect second match", ref2, 
                     ComponentReference.matchExisting(b2, refs));
    }


    public void testMatchOrphanedComponent() throws Exception {
        final JPanel panel = new JPanel();
        final ArrayList list = new ArrayList();
        for (int i=0;i < 2;i++) {
            list.add(new JButton("button " + i));
        }
        Component c = (Component)list.get(0);
        Component c2 = (Component)list.get(1);
        JButton button = new JButton("Change");
        button.addActionListener(new ActionListener() {
            private int index;
            public void actionPerformed(ActionEvent e) {
                panel.remove((Component)list.get(index));
                if (++index == list.size())
                    index = 0;
                panel.add((Component)list.get(index));
                panel.revalidate();
                panel.repaint();
            }
        });
        panel.add(button);
        panel.add((Component)list.get(0));
        showFrame(panel);

        ComponentReference ref = resolver.addComponent(c);
        Collection refs = resolver.getComponentReferences();
        assertEquals("Component should match existing reference", 
                     ref, ComponentReference.matchExisting(c, refs));

        // makes second button display
        button.doClick();
        // components are not guaranteed to match if they are no longer in the
        // hierarchy. 
        /*
        assertEquals("First component should still match when excised",
                     ref, ComponentReference.matchExisting(c, refs));
        */

        // NOTE: force a reference creation; otherwise fuzzy matching will
        // cause the second component to match the first reference (as it
        // should). 
        ComponentReference ref2 =
            new ComponentReference(resolver, c2, newRefs);
        Iterator iter = newRefs.keySet().iterator();
        while (iter.hasNext()) {
            ComponentReference tmp = (ComponentReference)
                newRefs.get(iter.next());
            resolver.addComponentReference(tmp);
        }

        // NOTE: this is a slight variation of "best existing match".  Ensure
        // that we get the best match when more than one ref would pass a
        // fuzzy match.
        refs = resolver.getComponentReferences();
        assertEquals("Second component should match proper existing ref", 
                     ref2, ComponentReference.matchExisting(c2, refs));

        // Make first button display
        button.doClick();
        assertEquals("First component should match existing ref when restored",
                     ref, ComponentReference.matchExisting(c, refs));
        // components are not guaranteed to match if they are no longer in the
        // hierarchy. 
        /*
        assertEquals("Second component should still match when excised",
                     ref2, ComponentReference.matchExisting(c2, refs));
        */
    }

    public ComponentReferenceTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentReferenceTest.class);
    }
}
