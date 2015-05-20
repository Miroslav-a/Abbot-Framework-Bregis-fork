package abbot.tester;

import javax.swing.*;
import junit.extensions.abbot.*;

/** Unit test to verify the JSpinnerTester class.<p> */

public class JSpinnerTesterTest extends ComponentTestFixture {

    public void testIncrement() {
        showFrame(numeric);
        tester.actionIncrement(numeric);
        assertEquals("Wrong value", new Integer(1), numeric.getValue());
    }

    public void testDecrement() {
        showFrame(numeric);
        tester.actionDecrement(numeric);
        assertEquals("Wrong value", new Integer(-1), numeric.getValue());
    }

    public void testSetValue() {
        int VALUE = 199;
        showFrame(numeric);
        tester.actionSetValue(numeric, String.valueOf(VALUE));
        assertEquals("Wrong value", new Integer(VALUE), numeric.getValue());
    }

    /** Create a new test case with the given name. */
    public JSpinnerTesterTest(String name) {
        super(name);
    }

    private JSpinnerTester tester;
    private JSpinner numeric;
    protected void setUp() {
        tester = (JSpinnerTester)ComponentTester.getTester(JSpinner.class);
        numeric = new JSpinner();
        numeric.setValue(new Integer(0));
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JSpinnerTesterTest.class);
    }
}
