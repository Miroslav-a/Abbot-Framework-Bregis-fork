package abbot.editor.widgets;

import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.border.*;
import junit.extensions.abbot.ComponentTestFixture;
import junit.extensions.abbot.TestHelper;
import junit.extensions.abbot.Timer;
import abbot.tester.FrameTester;
import abbot.tester.JTabbedPaneLocation;
import abbot.tester.JTabbedPaneTester;
import abbot.tester.JTextComponentTester;
import abbot.Log;

public class AbstractComponentDecoratorTest extends ComponentTestFixture {

    private static final int SIZE = 100;
    private static final Color COLOR = Color.red;
    private static final Color BICOLOR = Color.black;
    private static final Point COLORED = new Point(1, 1);

    private void wait(Component c, int x, int y, Color color) {
        // X11/Linux is a little slow in painting
        long start = System.currentTimeMillis();
        Color actual = null;
        do {
            actual = getRobot().sample(c, x, y);
            if (System.currentTimeMillis() - start >  2000)
                break;
        } while (!actual.equals(color));
    }
    
    private void checkDecorationBounds(String message, Component comp) {
        checkDecorationBounds(message, comp, COLOR);
    }
    
    private void checkDecorationBounds(String message, 
                                       Component comp, 
                                       Color expected) {
        // Paint one pixel in from the edges, or of dimension SIZExSIZE,
        // whichever is smaller
        int h = Math.min(comp.getHeight()-1, SIZE);
        int w = Math.min(comp.getWidth()-1, SIZE);
        Point[] CORNERS = {
            new Point(COLORED.x, COLORED.y),
            new Point(COLORED.x, COLORED.y + h - 1),
            new Point(COLORED.x + w-1, COLORED.y + h-1),
            new Point(COLORED.x + w-1, COLORED.y),
        };
        Point[] XCORNERS = {
            new Point(0, 0),
            new Point(0, comp.getHeight()-1),
            new Point(comp.getWidth()-1, comp.getHeight()-1),
            new Point(comp.getWidth()-1, 0),
        };
        wait(comp, CORNERS[0].x, CORNERS[0].y, expected);
        
        for (int i=0;i < CORNERS.length;i++) {
            Color actual = getRobot().sample(comp, CORNERS[i].x, CORNERS[i].y);
            assertEquals(message + "@" + CORNERS[i], expected, actual);
        }
        for (int i=0;i < XCORNERS.length;i++) {
            Color actual = getRobot().sample(comp, XCORNERS[i].x, XCORNERS[i].y);
            if (expected == COLOR) {
                assertFalse(message + "@" + XCORNERS[i], 
                            expected.equals(actual)); 
            }
            else {
                assertEquals(message + "@" + XCORNERS[i], expected, actual);
            }
        }
    }


    /**
     * It turns out due to anti-aliasing the sample colour around the
     * text can be out a little so this is a bit of a hack to ignore these
     * differences.
     * @param message
     * @param expected
     * @param actual
     */
    private static void assertEquals(String message, Color expected, Color actual) {
        
        int difference = Math.abs(expected.getBlue() - actual.getBlue())
            + Math.abs(expected.getGreen() - actual.getGreen())
            + Math.abs(expected.getRed() - actual.getRed());
        
        assertTrue(message, difference < 12);
    }

    private class Decorator extends AbstractComponentDecorator {
        public boolean bicolor;
        public Decorator(JComponent c) { this(c, 1); }
        public Decorator(JComponent c, int offset) { 
            this(c, offset, TOP); 
        }
        public Decorator(JComponent c, int offset, int position) { 
            super(c, offset, position);
            c.putClientProperty("decorator", this);
        }
        /** Decorate all but the outermost pixels (up to SIZE max w/h). */
        protected Rectangle getDecorationBounds() {
            Rectangle r = new Rectangle(super.getDecorationBounds());
            r.setLocation(COLORED.x, COLORED.y);
            r.width = Math.min(getComponent().getWidth()-1, SIZE);
            r.height = Math.min(getComponent().getHeight()-1, SIZE);
            return r;
        }
        /** Fill the decoration bounds. */
        public void paint(Graphics g) {
            Rectangle b = getDecorationBounds();
            g.setColor(COLOR);
            g.fillRect(b.x, b.y, b.width, b.height);
            g.drawRect(b.x, b.y, b.width-1, b.height-1);
            if (bicolor) {
                g.setColor(BICOLOR);
                g.fillRect(b.x + b.width/2, b.y + b.height/2, 
                           b.width/2, b.height/2);
                g.drawRect(b.x + b.width/2, b.y + b.height/2, 
                           b.width/2-1, b.height/2-1);
            }
        }
        public Object getCursor() {
            return getPainter().getCursor();
        }
    }

    private class MouseWatcher 
        extends MouseAdapter implements MouseMotionListener {
        public boolean mousePressed;
        public boolean mouseMoved;
        public void mousePressed(MouseEvent e) {
            mousePressed = true;
        }
        public void mouseDragged(MouseEvent e) {
        }
        public void mouseMoved(MouseEvent e) {
            mouseMoved = true;
        }
    }

    protected void setUp(){
        //Log.addDebugClass(AbstractComponentDecorator.class);
        //Log.setSynchronous(true);
    }
    
    public void testLayeredPaneBounds() {
        JLabel label = new JLabel(getName());
        Frame frame = showFrame(label);
        JLayeredPane lp = label.getRootPane().getLayeredPane();
        Rectangle visible = lp.getVisibleRect();
        Rectangle bounds = new Rectangle(0, 0, lp.getWidth(), lp.getHeight());
        
        assertEquals("Layered pane is entirely visible", bounds, visible);
        Insets insets = frame.getInsets();
        assertEquals("Layered pane is within Frame width", 
                     frame.getWidth() - insets.left - insets.right, 
                     lp.getWidth());
        assertEquals("Layered pane is within Frame height", 
                     frame.getHeight() - insets.top - insets.bottom, 
                     lp.getHeight());
                     
    }
    
    public void testIsVisible() {
        JLabel label = new JLabel(getName());
        Decorator d = new Decorator(label) {
            public Rectangle getDecorationBounds() {
                return new Rectangle(1, 1, 10, 10);
            }
        };
        showFrame(label);
        assertTrue("Decoration should report  visible when decoration " +
                    "lies inside of the layered pane", d.isVisible()); 
        // Check visibility
        // don't set cursor on main when not visible (prevents flicker
        // in aux ghost frames)
        // still need to check primary for flicker
        
    }


    public void testIsNotVisible() {
        JLabel label = new JLabel(getName());
        Decorator d = new Decorator(label) {
            public Rectangle getDecorationBounds() {
                return new Rectangle(-100, -100, 1, 1);
            }
        };
        showFrame(label);
        assertFalse("Decoration should report not visible when decoration " +
                    "lies outside of the layered pane", d.isVisible()); 
        // Check visibility
        // don't set cursor on main when not visible (prevents flicker
        // in aux ghost frames)
        // still need to check primary for flicker
        
    }
    
    public void testClipToLayeredPane() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        JLayeredPane lp = label.getRootPane().getLayeredPane();
        final Decorator d = new Decorator(label);
        final Point p = SwingUtilities.convertPoint(lp, 0, 0, label);
        Point p2 = SwingUtilities.convertPoint(lp, lp.getWidth(), lp.getHeight(), label);
        p.x -= 10;
        p.y -= 10;
        final int w = p2.x - p.x;
        final int h = p2.y - p.y;
        invokeAndWait(new Runnable() { public void run() {
            d.setDecorationBounds(p.x, p.y, w, h);
            d.repaint();
        }});
        Rectangle bounds = new Rectangle(0, 0, lp.getWidth(), lp.getHeight());
        assertTrue("Painter bounds should be clipped to the layered pane: lp="
                   + bounds + ", painter=" + d.getPainter().getBounds(),
                   bounds.contains(d.getPainter().getBounds()));
    }
    
    public void testNoEffectOnInputEvents() {
        MouseWatcher watcher = new MouseWatcher();
        JLabel label = new JLabel("input");
        showFrame(label);
        getRobot().mouseMove(label);
        getRobot().waitForIdle();
        label.addMouseListener(watcher);
        label.addMouseMotionListener(watcher);
        new Decorator(label);
        getRobot().waitForIdle();
        getRobot().click(label);
        getRobot().waitForIdle();
        assertTrue("Input not passed to label", watcher.mousePressed);
        getRobot().mouseMove(label, COLORED.x, COLORED.y);
        assertTrue("Motion not passed to label", watcher.mouseMoved);
    }
    
    public void testSynchCursor() {
        JLabel label = new JLabel(getName());
        Decorator d = new Decorator(label);
        showFrame(label);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        assertEquals("Decorator cursor should match",
                     label.getCursor(), d.getCursor());
                     
    }

    public void testDecorateBeforeShow() {
        JLabel label = new JLabel(getName());
        new Decorator(label);
        showFrame(label);
        checkDecorationBounds("Decorator added before show didn't take effect",
                              label);
    }
    
    // TODO: BOTTOM position doesn't work
    /*
    public void testDecorateBeforeShowBottom() {
        JLabel label = new JLabel(getName());
        //new Decorator(label, 1, Decorator.BOTTOM);
        new Decorator(label, 1, 0);
        showFrame(label);
        getRobot().delay(60000);
        checkDecorationBounds("Decorator (bottom) added before show didn't take effect",
                              label);
    }
    */
    public void testDecorateAfterShow() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        Decorator decorator = new Decorator(label);
        getRobot().waitForIdle();
        checkDecorationBounds("Decoration not applied when component already showing", label);
        decorator.dispose();
        getRobot().waitForIdle();
        Color actual = getRobot().sample(label, COLORED.x, COLORED.y);
        assertFalse("Decoration not removed", COLOR.equals(actual));
    }

    public void testDecorateAfterComponentRepaint() {
        JLabel label = new JLabel(getName());
        label.setBackground(Color.gray);
        label.setOpaque(true);
        showFrame(label);
        
        Decorator decorator = new Decorator(label);
        getRobot().waitForIdle();
        checkDecorationBounds("Decoration not initially applied", 
                              label);
        
        label.repaint();
        getRobot().waitForIdle();
        checkDecorationBounds("Decoration not reapplied after component repaint()", 
                              label);
        
        decorator.dispose();
        decorator = null;
        getRobot().waitForIdle();
        checkDecorationBounds("Decoration not removed after dispose", 
                              label, label.getBackground());
    }

    public void testDecorateAfterLayerChange() {
        final JLabel label = new JLabel(getName());
        label.setBackground(Color.gray);
        label.setOpaque(true);
        new Decorator(label);
        Frame frame = showFrame(label);
        assertTrue("Wrong frame type", frame instanceof RootPaneContainer);
        final JLayeredPane lp = ((RootPaneContainer)frame).getLayeredPane();
        final Integer[] LAYERS = {
            JLayeredPane.DEFAULT_LAYER,
            JLayeredPane.PALETTE_LAYER,
            JLayeredPane.MODAL_LAYER,
            JLayeredPane.POPUP_LAYER,
            JLayeredPane.DRAG_LAYER,
        };
        final String[] NAMES = {
            "default", "palette", "modal", "popup", "drag",
        };
        final Container content = ((RootPaneContainer)frame).getContentPane();
        checkDecorationBounds("Initial decoration missing", label);
        for (int i=0;i < LAYERS.length;i++) {
            final int idx = i;
            invokeAndWait(new Runnable() { public void run() {
                label.setText("Decorated on " + NAMES[idx]
                              + " layer (" + LAYERS[idx] + ")");
                lp.setLayer(content, LAYERS[idx].intValue());
            }});
            checkDecorationBounds("Decorator missing when component layer "
                                  + "changed to " + NAMES[i]
                                  + " layer (" + LAYERS[idx] + ")",
                                  label);
        }
    }

    public void testDecorateAfterHierarchyChange() {
        JLabel label = new JLabel(getName());
        JPanel p1 = new JPanel();
        JPanel p2 = new JPanel();
        p2.add(p1);
        p1.add(label);

        new Decorator(label);
        Frame f1 = showFrame(p2);
        JPanel p3 = new JPanel();
        showFrame(p3, f1.getSize());
        p3.add(p1);
        p3.revalidate();
        getRobot().waitForIdle();
        checkDecorationBounds("Decoration not applied after hierarchy change", 
                              label);
    }
    
    public void testDecorateScrolledComponent() {
        Object data[][] = { 
            { "one", "two" }, 
            {"three", "four" },
            { "x", "y" }, 
            {"three", "four" },
            { "one", "two" }, 
            {"z", "w" },
            { "one", "two" }, 
            {"three", "four" },
            { "one", "two" }, 
            {"a", "1" },
            {"b", "2" },
            {"c", "3" },
            {"d", "4" },
            {"e", "5" },
            {"f", "6" },
            {"g", "7" },
            {"h", "8" },
            {"i", "9" },
            {"j", "10" },
        };
        String[] cols = { "column 1", "column 2" };
        final JTable table = new JTable(data, cols);
        JScrollPane pane = new JScrollPane(table);

        showFrame(pane, new Dimension(200, 200));
        new Decorator(table);
        Rectangle rect = table.getParent().getBounds();
        wait(table, COLORED.x, COLORED.y, COLOR);
        Color actual = getRobot().sample(table, COLORED.x, COLORED.y);
        assertEquals("Decoration should be applied to component within viewport",
                     COLOR, actual);
        getRobot().invokeAndWait(new Runnable() { public void run() {
            table.scrollRectToVisible(table.getCellRect(table.getRowCount()-1, 0, true));
        }});
        actual = getRobot().sample(pane, rect.x, rect.y);
        assertNotSame("Decoration should scroll with component", 
                      COLOR, actual);
        getRobot().invokeAndWait(new Runnable() { public void run() {
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }});
        actual = getRobot().sample(table, COLORED.x, COLORED.y);
        assertEquals("Decoration should be reapplied to component after scroll",
                     COLOR, actual);
    }
    
    public void testDecorateResizedViewport() {
        final JTextArea p = new JTextArea(getName());
        p.setName("**TEST**");
        p.setPreferredSize(new Dimension(200, 200));
        new AbstractComponentDecorator(p) {
            public void paint(Graphics g) {
                Rectangle b = getDecorationBounds();
                g.setColor(Color.red);
                g.fillRect(b.x, b.y, b.width, b.height);
            }
        };
        Frame f = showFrame(new JScrollPane(p), new Dimension(100, 100));
        getRobot().invokeAndWait(new Runnable() { public void run() {
            p.setSize(500, 100);
        }});
        FrameTester ft = new FrameTester();
        ft.actionResizeBy(f, 100, 0);
        Rectangle rect = p.getVisibleRect();
        assertEquals("Newly exposed region should be colored",
                     COLOR, getRobot().sample(p, rect.width-1, rect.height-1));
    }
    
    // BOTTOM doesn't currently work
    /*
    public void testDecorateBottomAcrossScrollBarAddition() {
        final JTextArea text = new JTextArea(getName() + "\n" + getName());
        showFrame(new JScrollPane(text), new Dimension(400, 200));
        new AbstractComponentDecorator(text, AbstractComponentDecorator.BOTTOM) {
            public void paint(Graphics g) {
                Rectangle b = getDecorationBounds();
                g.setColor(COLOR);
                g.fillRect(b.x, b.y, b.width, b.height);
            }
        };
        text.setText(text.getText() + "\n\n\n\n\n\n\n");
        text.setCaretPosition(text.getText().length());
        getRobot().waitForIdle();
        Rectangle rect = text.getVisibleRect();
        assertEquals("Component should still be fully decorated", COLOR,
                     getRobot().sample(text, rect.width-1, rect.height-1));
    }
    */
    public void testDispose() throws Throwable {
        Map map = new WeakHashMap();
        JLabel label = new JLabel(getName());
        Frame frame = showFrame(label);
        Decorator decorator = new Decorator(label);
        map.put(decorator, Boolean.TRUE);
        map.put(label, Boolean.TRUE);
        decorator = null;
        frame.remove(label);
        assertNull("Label should have no parent", label.getParent());
        label = null;
        getRobot().waitForIdle();
        Timer timer = new Timer();
        while (map.size() > 0 && timer.elapsed() < 5000) {
            // This should get rid of the component and its decorator
            System.gc();
            Thread.sleep(10);
        }
        assertEquals("Objects still referenced after GC: " + map.keySet(), 
                     0, map.size());
    }
    
    public void testHideWhenComponentHidden() throws Exception {
        JLabel label = new JLabel(getName());
        label.setBorder(new EmptyBorder(20,20,20,20));
        JTabbedPane pane = new JTabbedPane();
        // explicitly set bg to avoid patterned bg on OSX
        label.setBackground(Color.gray);
        label.setOpaque(true);
        pane.add("one", label);
        JLabel label2 = new JLabel(getName());
        label2.setBorder(new EmptyBorder(20,20,20,20));
        label2.setBackground(Color.gray);
        label2.setOpaque(true);
        pane.add("two", label2);
        showFrame(pane);
        new Decorator(label);
        JTabbedPaneTester tester = new JTabbedPaneTester();
        tester.actionSelectTab(pane, new JTabbedPaneLocation(1));
        checkDecorationBounds("Decoration not hidden when target hidden",
                              label2, label2.getBackground());
    }

    public void testViewportClipping() throws Exception {
        final JLabel label = new JLabel(getName());
        final int SIZE = 200;
        label.setPreferredSize(new Dimension(SIZE, SIZE));
        label.setBorder(new LineBorder(Color.black));
        final Rectangle DB = new Rectangle(SIZE/4, SIZE/4, SIZE/2, SIZE/2); 
        Decorator d = new Decorator(label) {
            protected Rectangle getDecorationBounds() {
                return DB;
            }
        };
        d.bicolor = true;
        showFrame(new JScrollPane(label), new Dimension(200, 200));
        getRobot().invokeAndWait(new Runnable() { public void run() {
            label.scrollRectToVisible(DB);
        }});
        Color sample1 = getRobot().sample(label, SIZE/4, SIZE/4);
        assertEquals("Incorrect upper left", COLOR, sample1);
        sample1 = getRobot().sample(label, SIZE/2-1, SIZE/2-1);
        assertEquals("Incorrect center", COLOR, sample1);
        Color sample2 = getRobot().sample(label, SIZE/4+SIZE/2-1, SIZE/4+SIZE/2-1);
        assertEquals("Incorrect lower right", BICOLOR, sample2);
    }
    
    // Put the decorator on the background
    public void testDecorateBackground() {
        JLabel centerLabel = new JLabel("decorated");
        Font font = centerLabel.getFont();
        centerLabel.setFont(font.deriveFont(Font.BOLD, font.getSize()*4));
        // TODO: handle decorated components with solid bg+ opaque (bg != null)
        //jc.setBackground(Color.green);

        AbstractComponentDecorator d = new AbstractComponentDecorator(centerLabel, -1) {
            public void paint(Graphics g) {
                Rectangle r = getDecorationBounds();
                g.setColor(Color.red);
                g.fillRect(r.x, r.y, r.width, r.height);
                //g.setColor(Color.white);
                //g.drawString("(black fg, default bg)", 20, 30);
            }
            public Rectangle getDecorationBounds() {
                Rectangle r = super.getDecorationBounds();
                r.x = 1;
                r.y = 1;
                r.width /= 2;
                r.height /= 2;
                return r;
            }
        };
        AbstractComponentDecorator d2 = new AbstractComponentDecorator(centerLabel, -2) {
            public void paint(Graphics g) {
                Rectangle r = getDecorationBounds();
                g.setColor(Color.green);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
            public Rectangle getDecorationBounds() {
                Rectangle r = super.getDecorationBounds();
                r.x += r.width / 3;
                r.y += r.height / 6;
                r.width /= 2;
                r.height /= 2;
                return r;
            }
        };

        JPanel p = new JPanel(new BorderLayout());
        p.setName("Content");
        p.setBackground(Color.yellow);
        p.add(new JLabel("Label on an opaque JPanel"), BorderLayout.NORTH);
        p.add(centerLabel, BorderLayout.CENTER);
        p.add(new JLabel("Another Label"), BorderLayout.SOUTH);
        showFrame(p);

        wait(centerLabel, 0, 0, Color.yellow);
        //getRobot().delay(60000);
        Color sample = getRobot().sample(centerLabel, 0, 0);
        assertEquals("Component bg not properly exposesd", 
                     Color.yellow, sample);
        sample = getRobot().sample(centerLabel, centerLabel.getWidth() / 3,
                                   centerLabel.getHeight() / 6);
        assertEquals("-1 bg decorator should obscure -2", Color.red, sample);
        sample = getRobot().sample(centerLabel, centerLabel.getWidth()/3
                                   + centerLabel.getWidth()/2-1,
                                   centerLabel.getHeight()/6
                                   + centerLabel.getHeight()/2-1);
        assertEquals("Second decorator not applied", Color.green, sample);
    }
    
    public static void main(String[] args) {
        TestHelper.runTests(args, AbstractComponentDecoratorTest.class);
    }
}



