package abbot.editor.recorder;

import java.awt.*;
import java.util.*;

import abbot.Log;
import abbot.WaitTimedOutException;

import abbot.script.*;
import abbot.tester.Robot;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;

/**
 * All SemanticRecorder tests should derive from this class.
 * Provides a framework similar to EventRecorder which creates appropriate
 * semantic recorders based on incoming events.<p>
 * Will continue recording past the first semantic recorder used, preserving
 * the first one until an invocation of assert[No]Step, which resets the
 * preserved recorder, replacing it with the current one when appropriate.
 * This allows a more or less continuous stream of actions alternating with
 * assert[No]Step calls.<p>
 * NOTE: it would probably be simpler to not do this and require explicit
 * startRecording steps, although there is some benefit to checking recorders
 * back to back (it more closely simulates the EventRecorder behavior).
 */
// FIXME still needs some refactoring
// Needs clearer design on when to stop recording and examination of trailing
// events that are consumed/not consumed
public abstract class AbstractSemanticRecorderFixture
    extends AbstractRecorderFixture {

    private Queue<Step> steps;
    private Map<Step,String> stepEvents;
    private SemanticRecorder currentRecorder;
    private AWTEvent trailingEvent;

    public AbstractSemanticRecorderFixture() {
    }

    /** Create a new test case with the given name. */
    public AbstractSemanticRecorderFixture(String name) {
        super(name);
    }

    protected void fixtureSetUp() throws Throwable {
        super.fixtureSetUp();
        steps = new ConcurrentLinkedQueue<Step>();
        stepEvents = new ConcurrentHashMap<Step,String>();
    }

    protected void fixtureTearDown() throws Throwable {
        currentRecorder = null;
        trailingEvent = null;
        steps.clear();
        stepEvents.clear();
        super.fixtureTearDown();
    }

    /** Each Recorder subclass test should implement this method to return the
        SemanticRecorder to be tested. */
    protected abstract SemanticRecorder createSemanticRecorder(Resolver r);

    protected void startRecording() {
        startRecording(true);
    }

    protected void startRecording(boolean waitForIdle) {
        // Don't pick up any lingering events
        if (waitForIdle)
            getRobot().waitForIdle();
        trailingEvent = null;
        super.startRecording();
    }

    private class DefaultRecorder extends Recorder {
        public DefaultRecorder(Resolver r) { super(r); }
        protected void recordEvent(AWTEvent event) {
            AbstractSemanticRecorderFixture.this.recordEvent(event);
        }
        protected Step createStep() {
            return AbstractSemanticRecorderFixture.this.getStep();
        }
        public long getEventMask() {
            return EventRecorder.RECORDING_EVENT_MASK;
        }
        public void terminate() { }
    }

    /** Clear the current state of all steps and active recorder. */
    protected void clear() {
        currentRecorder = null;
        super.clear();
        steps.clear();
    }

    /** Provide a recorder which acts sort of like EventRecorder, but will
        always pass events on to the same SemanticRecorder obtained with
        createRecorder, rather than dynamically determining which recorder to
        create. */ 
    protected Recorder getRecorder() {
        clear();
        return new DefaultRecorder(getResolver());
    }

    // FIXME is this the best way to handle multiple recorders?
    private void saveCurrentStep() {
        if (currentRecorder != null) {
            Step step = currentRecorder.getStep();
            if (step == null)
                Log.debug("null step!");
            if (steps.size() > 0)
                Log.debug("Adding step " + step + " to list of "
                          + steps.size());
            if (step!=null)
            {
                steps.add(step);
                stepEvents.put(step, listEvents());
                currentRecorder = null;
            }
        }
    }

    // This dispatching is the canonical usage for a semantic recorder; cf
    // EventRecorder's lookup and dispatch
    // FIXME maybe make a semantic controller object that both this test and
    // event recorder use?
    // FIXME use a different synchronization lock
    protected void recordEvent(AWTEvent event) {
        if (Boolean.getBoolean("abbot.recorder.log_events"))
            Log.log("SREC: " + Robot.toString(event));

        if (currentRecorder == null) {
            // ordinarily, you'd get an appropriate recorder based on the
            // event type
            SemanticRecorder cr = createSemanticRecorder(getResolver());
            if (cr.accept(event)) {
                currentRecorder = cr;
                Log.log("New recorder, event accepted");
            }
        }

        // Don't bother passing on events if the recorder has captured its
        // target event
        if (currentRecorder != null) {
            SemanticRecorder cr = currentRecorder;
            if (cr.isFinished()) {
                if (trailingEvent == null) {
                    trailingEvent = event;
                    Log.log("Unconsumed trailing event");
                }
                return;
            }
            boolean consumed;
            boolean finished;
            Log.debug("Recording with " + cr);
            consumed = cr.record(event);
            finished = cr.isFinished();
            if (finished) {
                Log.log("Recorder is finished");
                saveCurrentStep();
                clearEvents();
            }
            if (!consumed) {
                Log.log("Re-sending event");
                recordEvent(event);
            }
        }
    }

    /** Verify that no unconsumed events were seen since the last recorded
        step.
    */
    protected void assertNoTrailingEvents() {
        getRobot().waitForIdle();
        synchronized(this) {
            AWTEvent e = trailingEvent;
            trailingEvent = null;
            if (e != null) {
                String bugInfo = bugInfo(listEvents());
                fail("Unconsumed trailing events starting at "
                     + Robot.toString(e) + " " + bugInfo);
            }
        }
    }

    protected void assertNoStep() {
        getRobot().waitForIdle();
        synchronized(this) {
            Step step = getStep();
            String bugInfo = step != null
                ? bugInfo((String)stepEvents.get(step)) : "";
            assertTrue("Recorder should not have recorded the step " + step
                       + " " + bugInfo, step == null);
        }
    }

    /** Make sure the step created matches the given pattern.  The recorder
     * may not nececessarily have detected the end of the pattern.
     */
    protected void assertStep(String pattern) {
        assertStep(pattern, false);
    }

    public void assertStep(String pattern, Step step) {
        assertStep(pattern, step, (String)stepEvents.get(step));
    }

    /** Make sure the recorder created step matches the given pattern,
     * optionally requiring the recorder to have detected the end of the
     * semantic event.
     */
    // FIXME this depends on the description that is generated; it should
    // check the actual step structure instead
    protected void assertStep(String pattern, boolean mustBeFinished) {
        // Make sure all events have been processed
        getRobot().waitForIdle();
        //synchronized(this) 
        {
            String bugInfo = bugInfo(listEvents());
            Log.log("Verifying recorder state (" + pattern + ")");
            try {
                assertTrue("No recorder instantiated " + bugInfo,
                           currentRecorder != null || steps.size() > 0);
                if (mustBeFinished) {
                    assertTrue("Semantic recorder should have detected the "
                               + "end of this event sequence " + bugInfo, 
                               (currentRecorder != null
                                && currentRecorder.isFinished())
                               || steps.size() > 0);
                }
                Step step = getStep();
                assertStep(pattern, step, (String)stepEvents.get(step));
            }
            catch(RuntimeException thr) {
                Log.log("** FAILED ***");
                throw thr;
            }
        }
    }

    // FIXME this methods purpose is unclear
    protected synchronized boolean hasRecorder() {
        return currentRecorder != null || steps.size() > 0;
    }

    protected  Step getStep() {
        
        // Wait around for a step
        try
        {
            Robot.wait(new abbot.util.Condition()
                {
                    @Override
                    public boolean test() {
                        return !steps.isEmpty();
                    }
                }, TimeUnit.SECONDS.toMillis(10), 1000);
        }
        catch (WaitTimedOutException wte) {
            // Just ignore
        }
        
        
        Step step = steps.poll();
        if (step !=null) {
            Log.log("retrieved saved step");
        }
        else if (currentRecorder != null) {
            Log.log("asking recorder for step");
            step = currentRecorder.getStep();
            currentRecorder = null;
            if (step != null) {
                stepEvents.put(step, listEvents());
                super.clear();
            }
        }
        else { 
            Log.log("No steps and no recorder");
        }
        return step;
    }
    
    /**
     * @return A sequence based on the current list of step events, could be in odd order
     */
    protected  Sequence getSequence() {
        
        Sequence seq = new Sequence(getResolver(), "Steps recorded so far");
        
        for (Step step : stepEvents.keySet()) {
            seq.addStep(step);
        }
        
        return seq;
        
    }
    
}
