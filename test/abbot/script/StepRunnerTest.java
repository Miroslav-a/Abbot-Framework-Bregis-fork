package abbot.script;

import java.io.*;
import java.util.ArrayList;
import javax.swing.*;

import junit.extensions.abbot.*;
import abbot.AssertionFailedError;

public class StepRunnerTest extends ResolverFixture {

    private StepRunner runner;
    private ArrayList events;
    private Resolver resolver;

    protected void setUp() {
        events = new ArrayList();
        resolver = getResolver();
        runner = new StepRunner();
        runner.addStepListener(new StepListener() {
            public void stateChanged(StepEvent ev) {
                events.add(ev);
            }
        });
    }

    protected void tearDown() {
        runner.terminate();
        runner = null;
        events = null;
        resolver = null;
    }

    public void testRunStep() throws Throwable {
        runner.runStep(new DefaultStep("simple step"));
        assertEquals("Wrong number of step events generated",
                     2, events.size());
        assertEquals("Missing step start",
                     StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Missing step end",
                     StepEvent.STEP_END,
                     ((StepEvent)events.get(1)).getType());
    }

    public void testRunSequence() throws Throwable {
        Sequence seq = new Sequence(getResolver(), "sequence of 2", null);
        Step st1 = new DefaultStep("step 1");
        seq.addStep(st1);
        Step st2 = new DefaultStep("step 2");
        seq.addStep(st2);
        runner.runStep(seq);
        assertEquals("Wrong number of step events generated",
                     6, events.size());
        assertEquals("Missing sequence start",
                     StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Expected sequence start, wrong source",
                     seq, ((StepEvent)events.get(0)).getStep());
        assertEquals("Missing step 1 start",
                     StepEvent.STEP_START,
                     ((StepEvent)events.get(1)).getType());
        assertEquals("Wrong source",
                     st1, ((StepEvent)events.get(1)).getStep());
        assertEquals("Missing step 1 end",
                     StepEvent.STEP_END,
                     ((StepEvent)events.get(2)).getType());
        assertEquals("Wrong source",
                     st2, ((StepEvent)events.get(3)).getStep());
        assertEquals("Missing step 2 start",
                     StepEvent.STEP_START,
                     ((StepEvent)events.get(3)).getType());
        assertEquals("Missing step 2 end",
                     StepEvent.STEP_END,
                     ((StepEvent)events.get(4)).getType());
        assertEquals("Missing sequence end",
                     StepEvent.STEP_END,
                     ((StepEvent)events.get(5)).getType());
    }

    public void testRunFailedStep() throws Throwable {
        Step step = new FailingStep("fail");
        Throwable failure = null;
        try {
            runner.runStep(step);
        }
        catch(AssertionFailedError afe) {
            failure = afe;
        }
        assertTrue("step failure should have been caught",
                   runner.getError(step) != null);
        assertEquals("wrong failure", failure, runner.getError(step));
        assertEquals("Wrong number of events", 2, events.size());
        assertEquals("Missing step start", StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Missing step failure", StepEvent.STEP_FAILURE,
                     ((StepEvent)events.get(1)).getType());
    }

    public void testRunFailedSequence() throws Throwable {
        Sequence seq = new Sequence(getResolver(), "sequence of 1", null);
        Step step = new FailingStep("fail");
        seq.addStep(step);
        Throwable failure = null;
        try {
            runner.runStep(seq);
        }
        catch(AssertionFailedError afe) {
            failure = afe;
        }
        assertTrue("step failure should have been caught",
                   runner.getError(step) != null);
        assertEquals("wrong failure", failure, runner.getError(step));
        assertEquals("Wrong number of events", 4, events.size());
        assertEquals("Missing sequence start", StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Wrong event source", seq, 
                     ((StepEvent)events.get(0)).getStep());
        assertEquals("Missing step start", StepEvent.STEP_START,
                     ((StepEvent)events.get(1)).getType());
        assertEquals("Wrong event source", step, 
                     ((StepEvent)events.get(1)).getStep());
        assertEquals("Missing step failure", StepEvent.STEP_FAILURE,
                     ((StepEvent)events.get(2)).getType());
        assertEquals("Wrong event source", step, 
                     ((StepEvent)events.get(2)).getStep());
        assertEquals("Missing step failure", StepEvent.STEP_FAILURE,
                     ((StepEvent)events.get(3)).getType());
        assertEquals("Wrong event source", seq, 
                     ((StepEvent)events.get(3)).getStep());
    }

    public void testRunErrorStep() {
        Step step = new ErrorStep("error");
        Throwable error = null;
        try {
            runner.runStep(step);
        }
        catch(Throwable thr) {
            error = thr;
        }
        assertNotNull("step error should have been caught", error);
        assertNotNull("step error should be stored with runner",
                      runner.getError(step));
        assertEquals("wrong error", error, runner.getError(step));
        assertEquals("Wrong number of events", 2, events.size());
        assertEquals("Missing step start", StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Missing step failure", StepEvent.STEP_ERROR,
                     ((StepEvent)events.get(1)).getType());
    }

    public void testRunEDTError() {
        Step step = new EDTErrorStep();
        Throwable error = null;
        try {
            runner.runStep(step);
        }
        catch(Throwable t) {
            error = t;
        }
        assertNotNull("EDT error should have been thrown by runner", error);
        assertNotNull("EDT error should be stored with runner",
                      runner.getError(step));
    }

    public void testRunErrorSequence() throws Throwable {
        Sequence seq = new Sequence(getResolver(), "sequence of 1", null);
        Step step = new ErrorStep("error");
        seq.addStep(step);
        Throwable failure = null;
        try {
            runner.runStep(seq);
        }
        catch(Throwable thr) {
            failure = thr;
        }
        assertTrue("step error should have been caught",
                   runner.getError(step) != null);
        assertEquals("wrong failure", failure, runner.getError(step));
        assertEquals("Wrong number of events", 4, events.size());
        assertEquals("Missing sequence start", StepEvent.STEP_START,
                     ((StepEvent)events.get(0)).getType());
        assertEquals("Wrong event source", seq, 
                     ((StepEvent)events.get(0)).getStep());
        assertEquals("Missing step start", StepEvent.STEP_START,
                     ((StepEvent)events.get(1)).getType());
        assertEquals("Wrong event source", step, 
                     ((StepEvent)events.get(1)).getStep());
        assertEquals("Missing step failure", StepEvent.STEP_ERROR,
                     ((StepEvent)events.get(2)).getType());
        assertEquals("Wrong event source", step, 
                     ((StepEvent)events.get(2)).getStep());
        assertEquals("Missing step failure", StepEvent.STEP_ERROR,
                     ((StepEvent)events.get(3)).getType());
        assertEquals("Wrong event source", seq, 
                     ((StepEvent)events.get(3)).getStep());
    }

    /** If the script is set to run to completion, make sure it throws an
     * error at the end.
     */
    public void testThrowMultipleError() throws Throwable {
        Sequence seq = new Sequence(getResolver(), "sequence of 2", null);
        Step error1 = new ErrorStep("error 1");
        seq.addStep(error1);
        Step error2 = new ErrorStep("error 2");
        seq.addStep(error2);
        runner.setStopOnError(false);
        try {
            runner.runStep(seq);
        }
        catch(Throwable thr) {
            StepEvent lastEvent = (StepEvent)events.get(events.size()-1);
            assertEquals("No error on event", thr, lastEvent.getError());
            assertEquals("Wrong event source", seq, lastEvent.getStep());
        }
    }

    public void testNoTerminate() throws Throwable {
        Script script = new Script(getHierarchy());
        final JFrame f = new JFrame(getName());
        f.getContentPane().add(new JLabel(getName()));
        script.addStep(new DefaultStep("Window Shower") {
            public void runStep() {
                f.setVisible(true);
            }
        });
        runner.run(script);
        assertTrue("Frame should not be disposed when script has no terminate",
                   f.isShowing());
    }

    // StepRunner.stop should leave the SUT in its state at the time of stop.
    public void testRunToStep() throws Throwable {
        Script script = new Script(getHierarchy());
        Step one = new DefaultStep("one");
        final Step two = new DefaultStep("two");
        script.addStep(one);
        script.addStep(two);
        final TestStepRunner runner = new TestStepRunner();
        runner.addStepListener(new StepListener() {
            public void stateChanged(StepEvent ev) {
                if (ev.getSource() == two)
                    runner.stop();
            }
        });
        try {
            runner.run(script);
        }
        finally {
            runner.realTerminate();
        }
    }

    /** If a nested file does not exist, an exception should be thrown. */
    public void testScriptNotFound() throws Throwable {
        Script script = new Script(getHierarchy());
        Script nested = new Script(getHierarchy());
        nested.setFile(new File("somewhere/relative/file.xml"));
        script.addStep(nested);
        try {
            runner.run(script);
            fail("Exception should have been thrown");
        }
        catch(InvalidScriptException ise) {
        }
    }

    private class DefaultStep extends Step {
        public DefaultStep(String desc) {
            super(resolver, desc);
        }
        public void runStep() throws Throwable { }
        public String getXMLTag() { return ""; }
        public String getUsage() { return ""; }
        public String getDefaultDescription() { return ""; }
    }

    private class FailingStep extends DefaultStep {
        public FailingStep(String desc) {
            super(desc);
        }
        public void runStep() throws Throwable {
            throw new AssertionFailedError(getDescription());
        }
    }

    private class ErrorStep extends DefaultStep {
        public ErrorStep(String desc) {
            super(desc);
        }
        public void runStep() throws Throwable {
            throw new Error(getDescription());
        }
    }

    private class EDTErrorStep extends DefaultStep {
        public EDTErrorStep() {
            super("EDT error");
        }
        public void runStep() throws Throwable {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    throw new TestEDTError();
                }
            });
            SwingUtilities.invokeAndWait(new Runnable() { 
                public void run() { }
            });
        }
    }

    private class TestStepRunner extends StepRunner {
        public void terminate() {
            fail("Step runner should not terminate when stopped");
        }
        public void realTerminate() {
            super.terminate();
        }
    }

    private class TestEDTError extends Error {
        public TestEDTError() { super("Test EDT Error"); }
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, StepRunnerTest.class);
    }
}
