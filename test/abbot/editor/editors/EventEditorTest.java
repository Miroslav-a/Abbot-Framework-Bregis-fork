package abbot.editor.editors;

import java.awt.event.*;

import javax.swing.JLabel;

import junit.extensions.abbot.*;
import abbot.script.*;

/** Verify EventEditor operation. */

public class EventEditorTest extends ResolverFixture {

    private Event event;
    private EventEditor editor;

    public void testMouseEvent() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        MouseEvent me =
            new MouseEvent(label, MouseEvent.MOUSE_MOVED,
                           System.currentTimeMillis(),
                           MouseEvent.BUTTON1_MASK, 2, 3, 1, false);
        event = new Event(getResolver(), "mouse press", me);
        String refid = event.getComponentID();
        editor = new EventEditor(event);
        // expect description, type, kind, component, x, y
        assertEquals("Wrong number of fields", 6, editor.getComponents().length/4);
        assertTrue("Type field should not be enabled",
                   !editor.type.isEnabled());
        assertTrue("Kind field should not be enabled",
                   !editor.kind.isEnabled());

        assertEquals("Wrong Component", refid,
                     editor.cref.getSelectedItem());

    }

    public void testKeyEvent() {
        JLabel label = new JLabel(getName());
        showFrame(label);
        KeyEvent ke =
            new KeyEvent(label, KeyEvent.KEY_PRESSED,
                         System.currentTimeMillis(),
                         0, KeyEvent.VK_A, KeyEvent.CHAR_UNDEFINED);
        event = new Event(getResolver(), "key press", ke);
        String refid = event.getComponentID();
        editor = new EventEditor(event);
        // expect description, type, kind, component, keycode
        assertEquals("Wrong number of fields", 5, editor.getComponents().length/4);
        assertTrue("Type field should not be enabled",
                   !editor.type.isEnabled());
        assertTrue("Kind field should not be editable",
                   !editor.kind.isEditable());

        assertEquals("Wrong Component", refid,
                     editor.cref.getSelectedItem());
    }

    /** Construct a test case with the given name. */
    public EventEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, EventEditorTest.class);
    }
}
