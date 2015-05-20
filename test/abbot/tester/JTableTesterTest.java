package abbot.tester;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Vector;

import java.util.concurrent.TimeUnit;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import junit.extensions.abbot.ComponentTestFixture;
import junit.extensions.abbot.RepeatHelper;


/** Unit test to verify the JTableTester class.<p> */

public class JTableTesterTest extends ComponentTestFixture {

    private int ROWS = 4;
    private int COLS = 4;
    private JTableTester tester;
    private JTable table;
    private JScrollPane scroll;

    /** Create a new test case with the given name. */
    public JTableTesterTest(String name) {
        super(name);
    }

    protected void setUp() {
        String[][] data = new String[][] {
            { "0 one", "0 two", "0 three", "0 four" },
            { "1 one", "1 two", "1 three", "1 four" },
            { "2 one", "2 two", "2 three", "2 four" },
            { "3 one", "3 two", "3 three", "3 four" },
        };
        String[] names = { "one", "two", "three", "four" };
        table = new JTable(new DefaultTableModel(data, names));
        scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tester = new JTableTester();
    }

    public void testSelectCells() {
        showFrame(table);
        for (int row=0;row < ROWS;row++) {
            for (int col=0;col < COLS;col++) {
                tester.actionClick(table, new JTableLocation(row, col));
                assertEquals("Wrong row selected", row, 
                             table.getSelectedRow());
                assertEquals("Wrong column selected", col, 
                             table.getSelectedColumn());
            }
        }
    }
    
    public void testLazySelectCells() {
        showFrame(scroll);
        
        final DefaultTableModel dtm = (DefaultTableModel)table.getModel();

        // Add an item two seconds later
        //
        
        final Timer timer = new Timer(2000, new ActionListener()
        {       
            public void actionPerformed(ActionEvent e) {
                dtm.addRow(new Object[] {"delayed value", "", "", ""});
            }
        });
        timer.start();
        
        // Now try to select
        //
                        
        
        tester.actionSelectCell(
            table,
            new JTableLocation("delayed value"));
        
        assertEquals("Wrong row selected", 4, table.getSelectedRow());
        assertEquals("Wrong column selected", 0, table.getSelectedColumn());
        
    }

    /**
     * Verify we can deal with the case where the string exceeds the visible space
     */
    public void testSelectHugeString() throws Exception {
        
        // Make sure that the viewport is much bigger than the table cell in question
        table.setMinimumSize(new Dimension(700,300));
        JPanel largePanel = new JPanel(new FlowLayout());
        largePanel.add(table);
        scroll.setViewportView(largePanel);
        
        // Show the frame
        showFrame(scroll);
        
        final DefaultTableModel dtm = (DefaultTableModel)table.getModel();

        // Add an item two seconds later
        //

        final String longString = "this is a really long section of text that means that the middle co-ordinate just isn't ever going to be visible";

        // Intermittent failure as being done after frame is shown so EDT violation
        EventQueue.invokeAndWait(new Runnable() {
                                     public void run() {
                                         dtm.removeRow(0);
                                         dtm.removeRow(0);
                                         dtm.removeRow(0);
                                         dtm.removeRow(0);
                                         dtm.addRow(new Object[] {"new value", "", "",  longString });
                                         
                                         table.getTableHeader().getColumnModel().getColumn(3)
                                             .setMinWidth(500);
                                         table.invalidate();
                                     }
                                 });
        
        // Now try to select
        //
                        
        
        tester.actionSelectCell(
            table,
            new JTableLocation(longString));
        
        assertEquals("Wrong row selected", 0, table.getSelectedRow());
        assertEquals("Wrong column selected", 3, table.getSelectedColumn());
        
    }


    public void testAssertCellsPresent() {
        showFrame(scroll);
        
        final DefaultTableModel dtm = (DefaultTableModel)table.getModel();

        // Now try to select
        //
                        
        
        
        assertTrue("Should have found 0 one", tester.assertCellExists(table, new JTableLocation("0 one")));
        assertTrue("Should have found 3 four", tester.assertCellExists(table, new JTableLocation("3 four")));
        
        assertFalse("Shouldn't have found 4 five", tester.assertCellExists(table, new JTableLocation("4 five"), 100));
        assertFalse("Shouldn't have found dog", tester.assertCellExists(table, new JTableLocation("dog"), 100));

    }
    

    /** Ensure scrolling works. */
    public void testScrollToVisible()
    {
        Vector data = new Vector();
        Vector columnNames = new Vector();

        int ROWS = 100;
        int COLS = 4;
        for (int row=0;row < ROWS;row++) {
            Vector rowv = new Vector();
            for (int col=0;col < COLS;col++) {
                rowv.add(String.valueOf(row+1) + "," + String.valueOf(col+1));
            }
            data.add(rowv);
        }
        for (int col=0;col < COLS;col++) {
            columnNames.add(String.valueOf(col));
        }
        table = new JTable(data, columnNames);
        Dimension size = new Dimension(150, 50);
        table.setPreferredScrollableViewportSize(size); 
        JScrollPane scrollPane = new JScrollPane(table);
        showFrame(scrollPane);
        tester.actionClick(table, new JTableLocation(0, 0));
        tester.waitForIdle();
        assertEquals("Wrong row selected", 0, table.getSelectedRow());
        assertEquals("Wrong column selected", 0, table.getSelectedColumn());
        tester.actionClick(table, new JTableLocation(ROWS-1, COLS-1));
        tester.waitForIdle();
        assertEquals("Wrong row selected", ROWS-1, table.getSelectedRow());
        assertEquals("Wrong column selected", COLS-1, table.getSelectedColumn());
        tester.actionClick(table, new JTableLocation(ROWS-1, 0));
        tester.waitForIdle();
        assertEquals("Wrong row selected", ROWS-1, table.getSelectedRow());
        assertEquals("Wrong column selected", 0, table.getSelectedColumn());
        tester.actionClick(table, new JTableLocation(0, COLS-1));
        tester.waitForIdle();
        assertEquals("Wrong row selected", 0, table.getSelectedRow());
        assertEquals("Wrong column selected", COLS-1, table.getSelectedColumn());
        tester.actionClick(table, new JTableLocation(0, 0));
        tester.waitForIdle();
        assertEquals("Wrong row selected", 0, table.getSelectedRow());
        assertEquals("Wrong column selected", 0, table.getSelectedColumn());
    }
    
    
    /** Ensure custom renderers work **/
    public void testGetTextRenderer() {
        
        class OtherRenderer extends JTextField
          implements TableCellRenderer {

            public Component getTableCellRendererComponent(JTable table,
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row,
                                                           int column) {
                setText(value.toString());
                return this;
            }
        }
        
        //
        
        
        table.setDefaultRenderer(
            String.class, new OtherRenderer());
        
        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JTableTester.valueToString(
               table , 0 , 0),
            "0 one");
        assertEquals(
            "Must return the correct child text for non JLabel renderers",
            JTableTester.valueToString(
               table , 3,3),
            "3 four");
        
        
    }
    
        
    public static void main(String[] args) {
        RepeatHelper.runTests(args, JTableTesterTest.class);
    }
}

