package abbot.editor;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.*;

import abbot.Platform;
import abbot.i18n.Strings;

/** Simple splash screen for the script editor. */
public class Costello {

    private static final String BUNDLE = "abbot.editor.i18n.costello";

    static {
        // Don't need robot verification
        System.setProperty("abbot.robot.verify", "false");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "Costello");
        if (Platform.JAVA_VERSION < Platform.JAVA_1_4) {
            // Mac OSX setup stuff
            System.setProperty("com.apple.mrj.application.growbox.intrudes",
                               "true");
            System.setProperty("com.apple.macos.use-file-dialog-packages", "true");
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
        }
        else {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.showGrowBox", "true");
        }
        Strings.addBundle(BUNDLE);
    }

    private static class SplashScreen extends JWindow {
        boolean disposeOK = false;
        /** @deprecated */
        public void hide() {
            if (disposeOK) {
                super.hide();
            }
        }
        public void dispose() {
            if (disposeOK) {
                super.dispose();
            }
        }
    }

    private static SplashScreen splash = null;
    public static Window getSplashScreen() {
        return splash;
    }

    /** Note that this "main" is typical of many Swing apps, in that it does
        window showing and disposal directly from the main thread.  Running
        the editor under itself should provide a reasonable test for handling
        that scenario.
    */
    public static void main(String[] args) {
        EditorContext ec = new EditorContext(args);
        showCostello(ec);
    }

    /** Invoke the costello editor with an editor configuration to give more fine
        grained control
     */
    public static void showCostello(EditorContext ec) {

        // In non embedded case set look and feel
        if (!ec.isEmbedded())
        {
            try {
                String lafClass =
                    System.getProperty("abbot.editor.look_and_feel", "system");
                if ("system".equals(lafClass))
                    lafClass = UIManager.getSystemLookAndFeelClassName();
                if (lafClass != null
                    && !"".equals(lafClass)
                    && !"default".equals(lafClass))
                    UIManager.setLookAndFeel(lafClass);
            }
            catch(Exception e) {
            }
        }

        splash = new SplashScreen();
        JLabel label = new LogoLabel();
        // Add a beveled border on non-Mac platforms
        if (!Platform.isOSX()) {
            Border b =
                new CompoundBorder(new SoftBevelBorder(BevelBorder.RAISED),
                                   label.getBorder());
            label.setBorder(b);
        }
        splash.getContentPane().add(label);
        splash.pack();
        Rectangle screen = splash.getGraphicsConfiguration().getBounds();
        Dimension size = splash.getSize();
        Point loc = new Point(screen.x + (screen.width - size.width) / 2,
                              screen.y + (screen.height - size.height) / 2);
        splash.setLocation(loc);
        splash.setVisible(true);

        try {
            ScriptEditor.showEditor(ec);
        }
        finally {
            splash.disposeOK = true;
            // NOTE: disposal in main still screws us up, because the dispose
            // does an invokeAndWait...
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() { splash.dispose(); splash = null; }
            });
        }
    }
}
