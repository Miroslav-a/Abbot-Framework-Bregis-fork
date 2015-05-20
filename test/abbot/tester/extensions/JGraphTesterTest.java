package abbot.tester.extensions;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import java.util.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;
import junit.framework.*;
import abbot.util.ExtendedComparator;

import org.jgraph.JGraph;
import org.jgraph.graph.*;

/** Unit test to verify the JGraphTester class. */

public class JGraphTesterTest extends ComponentTestFixture {

    private static final int COUNT = 2;

    private JGraphTester tester;
    private JGraph graph;
    private List cells;

    protected void setUp() {
        tester = new JGraphTester();
        graph = new JGraph();
        cells = new ArrayList();
        GraphModel m = new DefaultGraphModel();
        Map attributes = new HashMap();
        for (int i=0;i < COUNT;i++) {
            DefaultGraphCell cell = new DefaultGraphCell("cell" + i);
            Map map = new HashMap();
            GraphConstants.setBounds(map, new Rectangle(10, 40*i, 40, 30));
            cells.add(cell);
            attributes.put(cell, map);
        }
        m.insert(cells.toArray(), attributes, null, null, null);
        graph.setModel(m);
        showFrame(new JScrollPane(graph), new Dimension(200, 200));
    }

    public void testSelectCell() throws Exception {
        assertTrue("Initial selection should be empty",
                   graph.getSelectionCell() == null);
        tester.actionSelectCell(graph, new JGraphLocation(0));
        assertEquals("Wrong selection", cells.get(0),
                     graph.getSelectionCell());

        tester.actionSelectCell(graph, new JGraphLocation(1));
        assertEquals("Wrong selection (2)", cells.get(1),
                     graph.getSelectionCell());

        tester.actionSelectCell(graph, new JGraphLocation(0));
        assertEquals("Wrong selection (3)", cells.get(0),
                     graph.getSelectionCell());
    }

    public void testSelectCellScaled() throws Exception {
        assertTrue("Initial selection should be empty",
                   graph.getSelectionCell() == null);
        graph.setScale(4.0d);

        tester.actionSelectCell(graph, new JGraphLocation(0));
        assertEquals("Wrong selection", cells.get(0),
                     graph.getSelectionCell());

        tester.actionSelectCell(graph, new JGraphLocation(1));
        assertEquals("Wrong selection (2)", cells.get(1),
                     graph.getSelectionCell());

        tester.actionSelectCell(graph, new JGraphLocation(0));
        assertEquals("Wrong selection (3)", cells.get(0),
                     graph.getSelectionCell());
    }

    public static void main(String[] args) {
        RepeatHelper.runTests(args, JGraphTesterTest.class);
    }
}

