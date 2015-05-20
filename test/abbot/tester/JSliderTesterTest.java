package abbot.tester;

import javax.swing.*;
import junit.extensions.abbot.*;

/** Unit test to verify the JSliderTester class.<p> */

public class JSliderTesterTest extends ComponentTestFixture {

    public void testIncrement() {
        for (int i=0;i < sliders.length;i++) {
            int value = sliders[i].getValue();
            tester.actionIncrement(sliders[i]);
            assertTrue("Not incremented: " + sliders[i],
                       sliders[i].getValue() > value);
        }
    }

    public void testDecrement() {
        for (int i=0;i < sliders.length;i++) {
            int value = sliders[i].getValue();
            tester.actionDecrement(sliders[i]);
            assertTrue("Not decremented: " + sliders[i],
                       sliders[i].getValue() < value);
        }
    }

    // FIXME intermittent failure, Linux 1.4.2_04
    public void testSlide() {
        int VALUE = 25;
        for (int i=0;i < sliders.length;i++) {
            tester.actionSlide(sliders[i], VALUE);
            assertEquals("Wrong value on " + sliders[i], 
                         VALUE, sliders[i].getValue());
        }
    }

    public void testSlideMaximum() {
        for (int i=0;i < sliders.length;i++) {
            int VALUE = sliders[i].getMaximum();
            tester.actionSlideMaximum(sliders[i]);
            assertEquals("Wrong value on " + sliders[i], 
                         VALUE, sliders[i].getValue());
        }
    }

    public void testSlideMinimum() {
        for (int i=0;i < sliders.length;i++) {
            int VALUE = sliders[i].getMinimum();
            tester.actionSlideMinimum(sliders[i]);
            assertEquals("Wrong value on " + sliders[i], 
                         VALUE, sliders[i].getValue());
        }
    }

    /** Create a new test case with the given name. */
    public JSliderTesterTest(String name) {
        super(name);
    }

    private JSliderTester tester;
    private JSlider[] sliders;
    protected void setUp() {
        tester = (JSliderTester)ComponentTester.getTester(JSlider.class);
        sliders = new JSlider[4];
        sliders[0] = new JSlider();
        sliders[1] = new JSlider();
        sliders[1].setInverted(true);
        sliders[2] = new JSlider(JSlider.VERTICAL); 
        sliders[3] = new JSlider(JSlider.VERTICAL);
        sliders[3].setInverted(true);
        JPanel p = new JPanel();
        p.add(sliders[0]);
        p.add(sliders[1]);
        p.add(sliders[2]);
        p.add(sliders[3]);
        showFrame(p);
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JSliderTesterTest.class);
    }
}
