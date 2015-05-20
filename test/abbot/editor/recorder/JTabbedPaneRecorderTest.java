package abbot.editor.recorder;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;

import junit.extensions.abbot.RepeatHelper;
import abbot.Platform;
import abbot.script.Resolver;
import abbot.tester.*;
import abbot.util.AWT;

/**
 * Unit test to verify proper capture of user semantic events on a JTabbedPane.
 */
public class JTabbedPaneRecorderTest 
    extends AbstractSemanticRecorderFixture {

    private JTabbedPaneTester tester;
    private JTabbedPane tabbedPane;
    private static final int MAX_ENTRIES = 10;

    public JTabbedPaneRecorderTest(String name) {
        super(name);
    }

    protected SemanticRecorder createSemanticRecorder(Resolver r) {
        return new JTabbedPaneRecorder(r);
    }

    private void showTabbedPane() {
        tabbedPane = new JTabbedPane();
        for (int i=0;i < MAX_ENTRIES;i++) {
            tabbedPane.addTab("Tab " + i, new JLabel("This is pane #" + i) {
                public Dimension getPreferredSize() {
                    return new Dimension(300, 175);
                }
            });
        }
        tester = (JTabbedPaneTester)ComponentTester.getTester(tabbedPane);
        showFrame(tabbedPane);
    }

    public void testCaptureSelection() {
        showTabbedPane();
        startRecording();
        TabbedPaneUI ui = tabbedPane.getUI();
        Rectangle tab0 = ui.getTabBounds(tabbedPane, 0);
        for (int i=MAX_ENTRIES-1;i >= 0; i--) {
            JTabbedPaneLocation loc = new JTabbedPaneLocation(i);
            try {
                loc.getPoint(tabbedPane);
                tester.actionSelectTab(tabbedPane, loc);
            }
            catch(LocationUnavailableException e) {
                if (!Platform.isOSX()) {
                    throw e;
                }
                // Select from the popup menu
                Point where = new Point(tabbedPane.getWidth()
                                        - tab0.x - tab0.height/2,
                                        tab0.height/2);
                tester.actionClick(tabbedPane, where.x, where.y);
                JPopupMenu menu = AWT.getActivePopupMenu();
                Component c = menu.getComponent(i);
                tester.actionSelectMenuItem(c);

            }
            assertStep("SelectTab\\(.*,\"Tab " + i + "\"\\)");
        }
    }

    /*
    public void testCaptureSelectionBug() {
        showTabbedPane();
        startRecording();
        tester.mouseMove(tabbedPane, 20, 20);
        tester.delay(1000);
        tester.mousePress(InputEvent.BUTTON1_MASK);
        tester.delay(1000);
        assertStep("SelectTab\\(.*,Tab 4\\)", false);
    }
    */

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTabbedPaneRecorderTest.class);
    }
}
