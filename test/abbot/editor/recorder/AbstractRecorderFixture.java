package abbot.editor.recorder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import junit.extensions.abbot.ComponentTestFixture;
import junit.extensions.abbot.ResolverFixture;
import abbot.*;
import abbot.script.*;
import abbot.tester.*;
import abbot.tester.Robot;
import abbot.util.*;

/** Provide generic support for checking contents of recorded steps. */
// FIXME still needs some refactoring
public abstract class AbstractRecorderFixture extends ResolverFixture {

    private EventWatcher watcher;
    private Recorder recorder;
    private RecordingFailedException failure;

    public AbstractRecorderFixture() { }

    public AbstractRecorderFixture(String name) {
        super(name);
    }

    /** Provide a recorder to handle the input stream. */
    protected abstract Recorder getRecorder();

    public void runBare() throws Throwable {
        // Skip all recorder tests if not in robot mode, since there's no point
        // in recording AWT-stuffed events.
        if (Robot.getEventMode() == Robot.EM_ROBOT) {
            try {
                super.runBare();
            }
            finally {
                if (failure != null) {
                    throw failure.getCause() != null
                        ? failure.getCause() : failure;
                }
            }
        }
    }

    protected void fixtureTearDown() throws Throwable {
        stopRecording();
        recorder = null;
        if (watcher != null) {
            watcher.clear();
            watcher = null;
        }
        super.fixtureTearDown();
    }

    protected void startRecording() {
        Log.debug("start recording");
        recorder = getRecorder();
        watcher = new EventWatcher();
        watcher.startListening(recorder.getEventMask());
    }

    protected void stopRecording() {
        EventWatcher w = watcher;
        if (w != null)
            w.stopListening();
    }

    protected String bugInfo(String msg) {
        return "(" + new BugReport(msg) + ")";
    }

    public void assertStep(String pattern, Step step) {
        assertStep(pattern, step, null);
    }

    /** Clear the current state. */
    protected void clear() {
        clearEvents();
    }

    /** Clear the event history. */
    protected synchronized void clearEvents() {
        if (watcher != null)
            watcher.clear();
    }

    /** Allow derived classes to insert an event into the event stream to be
        recorded. */
    protected synchronized void insertEvent(AWTEvent e) {
        if (watcher != null)
            watcher.eventDispatched(e);
    }

    protected synchronized String listEvents() {
        return watcher != null ? watcher.listEvents() : "<empty>";
    }

    public synchronized void assertStep(String pattern, Step step,
                                        String events) {
        String bugInfo;
        if (events == null) {
            bugInfo = bugInfo(listEvents());
            clearEvents();
        }
        else {
            bugInfo = bugInfo(events);
        }
        Log.log(step != null ? ("Examining step: " + step) 
                : "No step available (expected '" + pattern + "')");
        assertTrue("Expected <" + pattern + ">, but no step was captured "
                   + bugInfo, step != null);
        assertTrue("Incorrect step, expected <" + pattern + ">, but got <"
                   + step + "> " + bugInfo, 
                   Regexp.stringContainsMatch(pattern, step.toString()));
    }

    public String toString(Sequence seq) {
        String steps = "The recorded steps were the following:";
        for (int i=0;i < seq.size();i++) {
            steps += "\n" + seq.getStep(i);
        }
        return steps;
    }

    public void assertStepCount(int count, Sequence seq) {
        String steps = toString(seq);
        String bugInfo = bugInfo(listEvents() + "\n\n" + steps);
        assertTrue("Wrong number of steps captured, expected <" + count
                   + ">, but got <" + seq.size() + "> " + bugInfo, 
                   count == seq.size());
    }

    protected class EventWatcher implements AWTEventListener {
        protected ArrayList events = new ArrayList();
        private EventNormalizer normalizer = new EventNormalizer(true);
        
        public void startListening(long mask) {
            Log.debug("start listening, mask=0x" + Integer.toHexString((int)mask));
            clear();
            Log.debug("events cleared");
            normalizer.startListening(this, mask);
        }
        
        public void stopListening() {
            normalizer.stopListening();
        }
        
        public void eventDispatched(AWTEvent event) {
            if (Boolean.getBoolean("abbot.recorder.log_events"))
                Log.log(Robot.toString(event));
            synchronized(events) {
                events.add(event);
            }
            Recorder r = recorder;
            if (r != null) {
                try {
                    r.record(event);
                }
                catch(RecordingFailedException e) {
                    failure = e;
                }
            }
        }
        
        public void clear() {
            synchronized(events) {
                events.clear();
            }
        }

        public String listEvents() {
            String msg = "The generated event stream was the following:";
            Iterator iter;
            synchronized(events) {
                iter = new ArrayList(events).iterator();
            }
            if (!iter.hasNext()) {
                msg += "\n(No events were captured)";
            }
            else while (iter.hasNext()) {
                AWTEvent ev = (AWTEvent)iter.next();
                msg += "\n" + ComponentTester.toString(ev);
            }
            return msg;
        }
    }

    private abstract class PopupListener extends MouseAdapter {
        protected abstract void showPopup(MouseEvent e);
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) 
                showPopup(e);
        }
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger())
                showPopup(e);
        }
    }

    /** Hook up the popup to be displayed on the given component. */
    protected void addPopup(Component c, final PopupMenu popup) {
        c.add(popup);
        c.addMouseListener(new PopupListener() {
            protected void showPopup(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    /** Hook up the popup to be displayed on the given component. */
    protected void addPopup(Component c, final JPopupMenu popup) {
        c.addMouseListener(new PopupListener() {
            protected void showPopup(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
}
