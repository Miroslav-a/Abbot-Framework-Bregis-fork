package abbot.script;

import java.awt.*;

import javax.swing.JLabel;
import javax.swing.JTree;

import abbot.AssertionFailedError;
import junit.extensions.abbot.*;

public class AssertTest extends ResolverFixture {

    public void testAssertTreePath() throws Throwable {
        JTree tree = new JTree();
        tree.setSelectionInterval(1, 1);
        showFrame(tree);
        ComponentReference ref = getResolver().addComponent(tree);
        Assert step = new Assert(getResolver(), null,
                                 "getSelectionPath", ref.getID(),
                                 "[JTree, colors]", false);
        step.runStep();
    }

    public void testAssertNullValue() throws Throwable {
        JLabel label = new JLabel(getName());
        Frame frame = showFrame(label);
        ComponentReference ref1 = getResolver().addComponent(label);
        ComponentReference ref2 = getResolver().addComponent(frame);
        Assert step1 = new Assert(getResolver(), "label.getName() == null",
                                 "getName", ref1.getID(),
                                  ArgumentParser.NULL, false);
        step1.runStep();
        label.setName("my label");
        try {
            step1.runStep();
            fail("test getName() == null should fail if name is not null");
        }
        catch(AssertionFailedError e) {
        }
        
        Assert step2 = new Assert(getResolver(), "frame.getName() != null",
                                  "getName", ref2.getID(),
                                  ArgumentParser.NULL, true);
        step2.runStep();
        frame.setName(null);
        try {
            step2.runStep();
            fail("test getName() != null should fail if name is null");
        }
        catch(AssertionFailedError e) {
        }

    }

    /** Verify expressions are always reduced to a canonical boolean form. */
    public void testCanonicalBooleans() {
        String[] args = { };
        Assert step = new Assert(getResolver(), null,
                                 "isSomething", args,
                                 Component.class, "true", false);
        assertEquals("Should expect 'true'",
                     "true", step.getExpectedResult()); 
        assertTrue("Should not be inverted", !step.isInverted());

        step = new Assert(getResolver(), null,
                          "isSomething", args,
                          Component.class, "true", true);
        assertEquals("Should expect 'true'",
                     "true", step.getExpectedResult()); 
        assertTrue("Should be inverted", step.isInverted());

        step = new Assert(getResolver(), null,
                          "isSomething", args,
                          Component.class, "false", false);
        assertEquals("Should expect 'true'",
                     "true", step.getExpectedResult()); 
        assertTrue("Should be inverted", step.isInverted());

        step = new Assert(getResolver(), null,
                          "isSomething", args,
                          Component.class, "false", true);
        assertEquals("Should expect 'true'",
                     "true", step.getExpectedResult()); 
        assertTrue("Should not be inverted", !step.isInverted());
    }

    public void testAssertFrameShowing() throws Throwable {
        Frame f = showFrame(new JLabel(getName()));
        f.setTitle(getName());
        String a1 = "<assert method=\"assertFrameShowing\" args=\""
            + getName() + "\"/>";
        Step s1 = Step.createStep(getResolver(), a1);
        s1.runStep();
    }

    public void testExpectedResult() throws Throwable {
        Frame f = showFrame(new JLabel(getName()));
        f.setName(getName());
        ComponentReference ref = getResolver().addComponent(f);
        Color c = f.getBackground();
        String expected = c.toString();
        Assert s = new Assert(getResolver(), null, "getBackground",
                              ref.getID(), expected, false);
        assertEquals("Wrong expected result", expected, s.getExpectedResult());
        s.runStep();
    }

    /** Create the assert step without having its class loading context. */
    public void testCreateWithNoContext() throws Throwable {
        new Assert(getResolver(), getName(), "unresolvedMethod", 
                   "someComponent", "true", false) {
            protected void setScriptError(Throwable thrown) {
                fail("Assert should load without error");
            }
        };
    }

    public AssertTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, AssertTest.class);
    }
}

