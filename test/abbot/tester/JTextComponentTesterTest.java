package abbot.tester;

import java.awt.*;

import java.util.concurrent.TimeUnit;

import javax.swing.*;
import javax.swing.text.JTextComponent;

import junit.extensions.abbot.*;

/** Unit test to verify the JTextComponentTester class.<p> */

public class JTextComponentTesterTest extends ComponentTestFixture {

    private JTextComponentTester tester;
    private JTextComponent tc;
    protected void setUp() {
        JTextField tf = new JTextField();
        tf.setColumns(10);
        tc = tf;
        tester = new JTextComponentTester();
    }

    public void testActionEnterText() {
        tc.setText("Some initial text to slow things down");
        showFrame(tc);
        String text = "short";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong short text typed,", text, tc.getText());
        text = "longer";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong replacement text,",
                     text, tc.getText());
        text = "Some longer text that will surely exceed the field width";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong long replacement text,",
                     text, tc.getText());
        text = "shorter";
        tester.actionEnterText(tc, text);
        assertEquals("Wrong shorter replacement text,",
                     text, tc.getText());

    }

    public void testActionEnterTextWithEmptyString() {
        tc.setText("Some initial text to slow things down");
        showFrame(tc);
        String EXPECTED = "";
        tester.actionEnterText(tc, EXPECTED);
        assertEquals("Text should be cleared", EXPECTED, tc.getText());
    }

    public void testClick() {
        showFrame(tc);
        String text = "Some somewhat long text which exceeds the component size";
        tc.setText(text);
        tester.waitForIdle();

        tester.actionClick(tc, 0);
        assertEquals("Wrong location", 0, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click
        
        tester.actionClick(tc, text.length() / 2);
        assertEquals("Wrong location",
                     text.length() / 2, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click

        tester.actionClick(tc, text.length());
        assertEquals("Wrong location", text.length(), tc.getCaretPosition());
    }


    public void testClickWithDelayedString() {
        showFrame(tc);
        final String text = "Some somewhat long text which exceeds the component size";

        new Thread() {
            public void run() {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                            try {
                                TimeUnit.SECONDS.sleep(5);
                            } catch (InterruptedException e) {
                            }
                            tc.setText(text);
                        }
                });
            }
        }.start();
        
        
        tester.waitForIdle();

        tester.actionClick(tc, 0);
        assertEquals("Wrong location", 0, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click
        
        tester.actionClick(tc, text.length() / 2);
        assertEquals("Wrong location",
                     text.length() / 2, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click

        tester.actionClick(tc, text.length());
        assertEquals("Wrong location", text.length(), tc.getCaretPosition());
    }



    public void testClickText() throws InterruptedException {
        showFrame(tc);
        String text = "Some somewhat long text which exceeds the component size";
        tc.setText(text);
        tester.waitForIdle();
        
        tester.actionClick(tc, "Some");
        assertEquals("Wrong location", 2, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click

        tester.actionClick(tc, "text");
        assertEquals("Wrong location",
                     21, tc.getCaretPosition());
        tester.delay(AWTConstants.MULTI_CLICK_INTERVAL); // We are only testing single click

        tester.actionClick(tc, "size");
        assertEquals("Wrong location", 54, tc.getCaretPosition());
    }

    public void testDoubleClickText() throws InterruptedException {
        showFrame(tc);
        String text = "Some somewhat long text which exceeds the component size";
        tc.setText(text);
        tester.waitForIdle();
        
        tester.actionClick(tc, "Some");
        tester.actionClick(tc, "Some");
        assertEquals("Wrong location", 4, tc.getCaretPosition());

        tester.actionClick(tc, "text");
        tester.actionClick(tc, "text");
        assertEquals("Wrong location",
                     23, tc.getCaretPosition());

        // A triple click required as the text is no intially visible?
        tester.actionClick(tc, "size");
        tester.actionClick(tc, "size");
        tester.actionClick(tc, "size");
        assertEquals("Wrong location", 56, tc.getCaretPosition());

    }

    public void testSelectVisibleText() {
        showFrame(tc);
        final String text = "short";
        tester.invokeAndWait(new Runnable() {
            public void run() { tc.setText(text); }
        });
        
        tester.actionSelectText(tc, text.length()-1, 1);
        assertEquals("Wrong selection start",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length()-1, tc.getSelectionEnd());
        tester.actionSelectText(tc, text.length(), 0);
        assertEquals("Wrong selection start",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length(), tc.getSelectionEnd());
        tester.actionSelectText(tc, 0, text.length());
        assertEquals("Wrong selection start",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length(), tc.getSelectionEnd());
        tester.actionSelectText(tc, 1, text.length()-1);
        assertEquals("Wrong selection start",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length()-1, tc.getSelectionEnd());
    }
    
    public void testSelectObscuredText() {
        showFrame(tc);
        // Try a long selection which exceeds the visible area
        String text = "The quick brown fox jumped over the lazy dog";
        tc.setText(text);
        tester.waitForIdle();
        tester.actionSelectText(tc, text.length(), 0);
        assertEquals("Wrong selection start",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end (hidden)",
                     text.length(), tc.getSelectionEnd());

        tester.actionSelectText(tc, 1, text.length()-1);
        assertEquals("Wrong selection start (hidden)",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length()-1, tc.getSelectionEnd());

        tester.actionSelectText(tc, 0, text.length());
        assertEquals("Wrong selection start",
                     0, tc.getSelectionStart());
        assertEquals("Wrong selection end (hidden)",
                     text.length(), tc.getSelectionEnd());

        tester.actionSelectText(tc, text.length()-1, 1);
        assertEquals("Wrong selection start (hidden)",
                     1, tc.getSelectionStart());
        assertEquals("Wrong selection end",
                     text.length()-1, tc.getSelectionEnd());
    }

    public void testSelectByText() {
        showFrame(tc);
        // Try a long selection and select a particular string
        String text = "The quick brown fox jumped over the lazy dog";
        tc.setText(text);
        tester.waitForIdle();
        tester.actionSelectText(tc, "quick");
        tester.waitForIdle();

        assertEquals("Wrong selection start",
                     4, tc.getSelectionStart());
        assertEquals("Wrong selection end (hidden)",
                     9, tc.getSelectionEnd());
    }



    /**
     * So this test appears to be failing because there is no drag operaion
     * going on when the text is moved underneath the cursor, in the real
     * screen the x,y co-ords are not changing so I suspect this is causing
     * the code not to see this as a selection operation, I suspect we need to 
     * generate a mouse move event or some kind to have this work but
     * we might need to provide a different implementation
     * @exception
     */
    public void testSelectByTextWithScrolling() {
        showFrame(tc);
        // Try a long selection and select a particular string
        String text = "The quick brown fox jumped over the lazy dog";
        tc.setText(text);
        tester.waitForIdle();
        tester.actionSelectText(tc, "lazy");
        tester.waitForIdle();

        assertEquals("Wrong selection start",
                     36, tc.getSelectionStart());
        assertEquals("Wrong selection end (hidden)",
                     40, tc.getSelectionEnd());
    }

    public void testScrollToVisible() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(200, 400));
        JPanel scrolled = new JPanel(new BorderLayout());
        scrolled.add(p);
        scrolled.add(tc, BorderLayout.SOUTH);
        showFrame(new JScrollPane(scrolled), new Dimension(200, 200));

        Rectangle visible = tc.getVisibleRect();
        Rectangle empty = new Rectangle(0,0,0,0);
        assertTrue("Text should not be visible",
                   visible == null || visible.equals(empty));
        
        tester.actionScrollToVisible(tc, new ComponentLocation());
        visible = tc.getVisibleRect();
        assertFalse("Text should be visible",
                    visible == null || visible.equals(empty));
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTextComponentTesterTest.class);
    }
}

