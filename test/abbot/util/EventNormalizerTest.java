package abbot.util;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.*;
import abbot.tester.JTreeLocation;
import abbot.tester.JTreeTester;

import junit.extensions.abbot.*;
import java.util.List;
public class EventNormalizerTest extends ComponentTestFixture {

    private class Listener implements AWTEventListener {
        public int disposedCount = 0;
        public List events = new ArrayList();
        public void eventDispatched(AWTEvent e) {
            if (e.getID() == WindowEvent.WINDOW_CLOSED) {
                ++disposedCount;
            }
            events.add(e);
        }
    }

    private AWTEvent findEvent(List events, Object source, int id) {
        for (Iterator i=events.iterator();i.hasNext();) {
            AWTEvent e = (AWTEvent)i.next();
            if (e.getID() == id && e.getSource() == source) {
                return e;
            }
        }
        return null;
    }

    public void testForwardNativeDragEvents() {
        EventNormalizer n = new EventNormalizer(true);
        Listener x = new Listener();
        n.startListening(x, InputEvent.MOUSE_EVENT_MASK|InputEvent.MOUSE_MOTION_EVENT_MASK);
        JTree tree = new JTree();
        // 1.4 only
        tree.setDragEnabled(true);
        JTextField text = new JTextField(40);
        JPanel p = new JPanel(new BorderLayout());
        p.add(tree, BorderLayout.CENTER);
        p.add(text, BorderLayout.NORTH);
        showFrame(p);
        JTreeTester tester = new JTreeTester();
        tester.actionClick(tree, new JTreeLocation(0));
        tester.actionDrag(tree, new JTreeLocation(0));
        tester.actionDrop(text);

        assertEquals("Drag/drop operation did not complete",
                     "JTree", text.getText());
        assertNotNull("Expected a 'drag' event on tree: " + events(x.events),
                      findEvent(x.events, tree, MouseEvent.MOUSE_DRAGGED));
        assertNotNull("Expected a 'drag' event on text: " + events(x.events),
                   findEvent(x.events, text, MouseEvent.MOUSE_DRAGGED));
        assertNotNull("Expected a 'drop' event on text: " + events(x.events), 
                      findEvent(x.events, text, MouseEvent.MOUSE_RELEASED));
    }
    
    private String events(List x) {
        StringBuffer buf = new StringBuffer();
        for (Iterator i=x.iterator();i.hasNext();) {
            buf.append("\n\t" + String.valueOf(i.next()));
        }
        return buf.toString();
    }

    public void testFilterDuplicateDispose() {
        EventNormalizer n = new EventNormalizer();
        Listener l = new Listener();
        n.startListening(l, WindowEvent.WINDOW_EVENT_MASK);
        Frame f = showFrame(new JLabel(getName()));
        f.dispose();
        getRobot().waitForIdle();
        assertEquals("One call to dispose should result in one event (1)",
                     1, l.disposedCount);
        f.dispose();
        getRobot().waitForIdle();
        assertEquals("Multiple dispose events should be filtered (1)",
                     1, l.disposedCount);

        l.disposedCount = 0;
        showWindow(f);
        f.dispose();
        // wait for dispose to finish
        getRobot().waitForIdle();
        assertEquals("One call to dispose should result in one event "
                     + "(component disposed and shown)",
                     1, l.disposedCount);
        f.dispose();
        getRobot().waitForIdle();
        assertEquals("Multiple dispose events should be filtered "
                     + "(component disposed and shown)",
                     1, l.disposedCount);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, EventNormalizerTest.class);
    }
}
