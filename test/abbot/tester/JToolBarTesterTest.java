package abbot.tester;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import junit.extensions.abbot.*;

public class JToolBarTesterTest extends ComponentTestFixture {
    public static void main(String[] args) {
        RepeatHelper.runTests(args, JToolBarTesterTest.class);
    }

    private JToolBar bar;
    private JPanel dock;
    private JLabel contents;
    private JToolBarTester tester;
    protected void setUp() {
        bar = new JToolBar();
        bar.add(new JButton(getName()));
        tester = new JToolBarTester();
        dock = new JPanel(new BorderLayout());
        dock.add(contents = new JLabel(getName()), BorderLayout.CENTER);
        dock.add(bar, BorderLayout.NORTH);
    }
    
    public void testActionFloatLocation() {
        Frame f = showFrame(dock);
        int x = f.getX() + f.getWidth();
        int y = f.getY() + f.getHeight();
        tester.actionFloat(bar, x, y);
        Window w = SwingUtilities.getWindowAncestor(bar);
        assertTrue("Not floated", tester.isFloating(bar));
        assertTrue("Floated in wrong location",
                   w.getBounds().contains(x, y));
    }

    public void testActionFloat() {
        Frame f = showFrame(dock);
        tester.actionFloat(bar);
        assertTrue("Bar should be floating", tester.isFloating(bar)); 
        
        tester.actionUnfloat(bar);
        assertEquals("Bar should not be floating",
                     f, SwingUtilities.getWindowAncestor(bar));
    }

    // sporadic linux (1.4.2) failures
    public void testActionUnfloatLocation() {
        Frame f = showFrame(dock);
        int x = f.getX() + f.getWidth();
        int y = f.getY() + f.getHeight();
        
        tester.actionFloat(bar, x, y);
        tester.actionUnfloat(bar, BorderLayout.SOUTH);
        assertEquals("Wrong orientation after SOUTH drop",
                     SwingConstants.HORIZONTAL, bar.getOrientation());
        assertTrue("Not docked SOUTH: bar y=" + bar.getY() 
                   + " vs. " + contents.getY(),
                   bar.getY() > contents.getY());
        
        tester.actionFloat(bar, x, y);
        tester.actionUnfloat(bar, BorderLayout.WEST);
        assertEquals("Wrong orientation after WEST drop",
                     SwingConstants.VERTICAL, bar.getOrientation());
        assertTrue("Not docked WEST: bar x=" + bar.getX() 
                   + " vs. " + contents.getX(),
                   bar.getX() < contents.getX());
                   
        tester.actionFloat(bar, x, y);
        tester.actionUnfloat(bar, BorderLayout.NORTH);
        assertEquals("Wrong orientation after NORTH drop",
                     SwingConstants.HORIZONTAL, bar.getOrientation());
        assertTrue("Not docked NORTH: bar y=" + bar.getY() 
                   + " vs. " + contents.getY(),
                   bar.getY() < contents.getY());

        tester.actionFloat(bar, x, y);
        tester.actionUnfloat(bar, BorderLayout.EAST);
        assertEquals("Wrong orientation after EAST drop",
                     SwingConstants.VERTICAL, bar.getOrientation());
        assertTrue("Not docked EAST: bar x=" + bar.getX() 
                   + " vs. " + contents.getX(),
                   bar.getX() > contents.getX());
    
    }
    
    
    
}
