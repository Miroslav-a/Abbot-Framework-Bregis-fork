package abbot.editor;

import javax.swing.*;
import java.awt.*;
import java.io.*;

import junit.extensions.abbot.*;
import junit.extensions.abbot.Timer;

public class HierarchyWriterTest extends ComponentTestFixture {

    public void xtestWriteHierarchy() throws IOException {
        Panel panel = new Panel();
        Panel p1 = new Panel();
        Panel p2 = new Panel();
        panel.add(p1);
        panel.add(p2);
        p1.add(new Button(getName()));
        Frame f = new Frame(getName());
        MenuBar mb = new MenuBar();
        Menu menu = new Menu("File");
        menu.add(new MenuItem("Open"));
        mb.add(menu);
        f.setMenuBar(mb);
        f.add(panel);
        // FIXME do regexp matches instead
        String expected =
            "<awtHierarchy>\r\n"
            + "  <component class=\"java.awt.Frame\" >\r\n"
            + "    <component class=\"java.awt.Panel\" index=\"0\" >\r\n"
            + "      <component class=\"java.awt.Panel\" index=\"0\" >\r\n"
            + "        <component class=\"java.awt.Button\" index=\"0\" />\r\n"
            + "      </component>\r\n"
            + "      <component class=\"java.awt.Panel\" index=\"1\" />\r\n"
            + "    </component>\r\n"
            + "  </component>\r\n"
            + "</awtHierarchy>\r\n";
        StringWriter writer = new StringWriter();
        HierarchyWriter hw = new HierarchyWriter(getHierarchy());
        hw.writeHierarchy(writer);
        assertEquals("Wrong hierarchy output", expected, writer.toString());
    }

    /** Should be able to export the complete hierarchy in minimal time. */ 
    public void testExportHierarchyPerformance() throws Exception {
        JPanel pane = new JPanel();
        int count = fill(pane, 8, 2) + 2;
        showFrame(pane);
        StringWriter writer = new StringWriter();
        HierarchyWriter hw = new HierarchyWriter(getHierarchy());
        Timer timer = new Timer();
        hw.writeHierarchy(writer);
        int MAX_TIME = 5000;
        assertTrue("Too long to save hierarchy: " + timer.elapsed() + "ms for "
                   + count + " references (max total time " + MAX_TIME + "ms)",
                   timer.elapsed() < MAX_TIME);
    }

    private int fill(JPanel pane, int level, int nsubs) {
        int count = 0;
        if (level > 0) {
            for (int i=0;i < nsubs;i++) {
                JPanel sub = new JPanel();
                count += fill(sub, level - 1, nsubs);
                pane.add(sub);
            }
        }
        else {
            pane.add(new JLabel(getName()));
            ++count;
        }
        return count;
    }

    public HierarchyWriterTest(String name) { super(name); }
    public static void main(String[] args) {
        TestHelper.runTests(args, HierarchyWriterTest.class);
    }
}
