/**
 * Name........: LaunchEditor 
 * Description.: Provide convenient editing of a launch step.
 * Author......: Timothy Wall 
 * E-Mail......: twall@users.sourceforge.net 
 */
package abbot.editor.editors;

import abbot.editor.widgets.*;
import abbot.script.*;
import abbot.i18n.*;
import abbot.util.*;

import javax.swing.*;

import java.awt.event.*;

import java.lang.reflect.*;

import java.util.*;

/**
 * Provide convenient editing of a launch step.
 */
public class LaunchEditor extends CallEditor {

    public static final String HELP_DESC = Strings.get("FixClassname");

    private Launch launch;

    protected ArrayEditor classpath;

    private JCheckBox thread;

    private JButton importeclipse;


    /**
     * Initializes this editor which is used to edit the supplied step.
     * 
     * @param launch
     *            The launch step that will be edited.
     */
    public LaunchEditor(Launch launch) {

        super(launch);

        this.launch = launch;

        String[] paths = PathClassLoader.convertPathToFilenames(launch
                .getClasspath());

        // FIXME extend ArrayEditor to use file choosing buttons instead of
        // text fields alone
        classpath = addArrayEditor(Strings.get("Classpath"), paths);
        classpath.setName(TAG_CLASSPATH);

        thread = addCheckBox(Strings.get("Thread"), launch.isThreaded());
        thread.setName(TAG_THREADED);

        // the stuff used to import an eclipse launch configuration
        try {
			Class classobj = Class.forName("abbot.editor.editors.ImportButton");
			Constructor constructor = classobj.getConstructor(new Class[] {LaunchEditor.class, Launch.class});
	        importeclipse = (JButton) constructor.newInstance(new Object[] {this, launch});
	        add(importeclipse);
		} catch (Throwable t) {
			// just don't embed the import button
		}

    }

    /**
     * Display only the public static member functions.
     */
    protected String[] getMethodNames(Method[] mlist) {
        ArrayList list = new ArrayList();
        int mask = Modifier.PUBLIC | Modifier.STATIC;
        for (int i = 0; i < mlist.length; i++) {
            if ((mlist[i].getModifiers() & mask) == mask) {
                list.add(mlist[i].getName());
            }
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent evt) {
        Object src = evt.getSource();
        if (src == classpath) {
            Object[] values = classpath.getValues();
            String cp = null;
            if (values.length > 0) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) {
                        buf.append(System.getProperty("path.separator"));
                    }
                    String path = (String) values[i];
                    if ("".equals(path)) {
                        path = ".";
                    }
                    buf.append(path);
                }
                cp = buf.toString();
            }
            launch.setClasspath(cp);
            // Changing the classpath may affect whether the class/method are
            // valid.
            validateTargetClass();
            validateMethod();
            fireStepChanged();
        } else if (src == thread) {
            launch.setThreaded(!launch.isThreaded());
            fireStepChanged();
        } else {
            super.actionPerformed(evt);
        }

        // Remove the default placeholder description
        if (HELP_DESC.equals(launch.getDescription())) {
            launch.setDescription(null);
            fireStepChanged();
        }

    }


} /* ENDCLASS */
