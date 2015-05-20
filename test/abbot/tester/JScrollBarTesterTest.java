package abbot.tester;

import javax.swing.*;

import junit.extensions.abbot.*;

/** Unit test to verify the JScrollBarTester class.<p> */

public class JScrollBarTesterTest extends ComponentTestFixture {

    private JScrollBarTester tester;
    private JScrollBar h, v;
    protected void setUp() {
        tester = new JScrollBarTester();
        h = new JScrollBar(JScrollBar.HORIZONTAL);
        v = new JScrollBar(JScrollBar.VERTICAL);
        JPanel p = new JPanel();
        p.add(h);
        p.add(v);
        showFrame(p);
    }

    public void testScrollUnit() {
        int value = h.getValue();
        tester.actionScrollUnitUp(h);
        assertEquals("Value not incremented",
                     value + h.getUnitIncrement(), h.getValue());
        tester.actionScrollUnitDown(h);
        assertEquals("Value not decremented",
                     value, h.getValue());

        tester.actionScrollUnitUp(v);
        assertEquals("Value not incremented",
                     value + v.getUnitIncrement(), v.getValue());
        tester.actionScrollUnitDown(v);
        assertEquals("Value not decremented",
                     value, v.getValue());
    }

    public void testScrollBlock() {
        int value = h.getValue();
        tester.actionScrollBlockUp(h);
        assertEquals("Value not incremented",
                     value + h.getBlockIncrement(), h.getValue());
        tester.actionScrollBlockDown(h);
        assertEquals("Value not decremented",
                     value, h.getValue());

        tester.actionScrollBlockUp(v);
        assertEquals("Value not incremented",
                     value + v.getBlockIncrement(), v.getValue());
        tester.actionScrollBlockDown(v);
        assertEquals("Value not decremented",
                     value, v.getValue());
    }
    
    public void testSetPosition() {
        int min = h.getMinimum();
        int max = h.getMaximum();
        int where = min + 3*(max-min)/4;
        tester.actionScrollTo(h, where);
        assertEquals("horizontal setPosition(" + where + ") failed",
                     where, h.getValue());
        tester.actionScrollTo(v, where);
        assertEquals("vertical setPosition(" + where + ") failed",
                     where, v.getValue());
    }


    public void testMinMaxPosition() {
        int min = h.getMinimum();
        int max = h.getMaximum() - h.getModel().getExtent();
        
        // Place not at max or min
        int where = min + (max-min)/2;
        tester.actionScrollTo(h, where);
        
        //
        
        tester.actionScrollToMinimum(h);
        
        assertEquals("horizontal setPosition(" + min + ") failed",
                     min, h.getValue());
        
        tester.actionScrollToMaximum(h);

        assertEquals("horizontal setPosition(" + max + ") failed",
                     max, h.getValue());
    }


    public static void main(String[] args) {
        RepeatHelper.runTests(args, JScrollBarTesterTest.class);
    }
}

