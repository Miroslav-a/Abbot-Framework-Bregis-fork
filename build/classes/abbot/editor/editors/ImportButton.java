/**
 * Name........: ImportButton 
 * Description.: Button allowing to import an Eclipse project configuration.
 * Author......: Daniel Kasmeroglu 
 * E-Mail......: costamojan@users.sourceforge.net 
 */
package abbot.editor.editors;


import net.sf.ant4eclipse.tools.resolver.*;
import net.sf.ant4eclipse.tools.*;

import net.sf.ant4eclipse.model.project.*;
import net.sf.ant4eclipse.model.launch.*;
import net.sf.ant4eclipse.model.*;


import abbot.script.*;
import abbot.i18n.*;

import javax.swing.*;

import java.awt.event.*;

import java.io.*;


/**
 * Button allowing to import an Eclipse project configuration.
 */
public class ImportButton extends JButton implements ActionListener {

	
    private JFileChooser launchfilechooser;
    private JFileChooser workspacefilechooser;
    private LaunchFileFilter launchfilefilter;
    private WorkspaceFileFilter workspacefilefilter;
	private LaunchEditor editor;
	private Launch launch;
	
	
	/**
	 * Initialises this button which will be placed on the supplied editor.
	 * 
	 * @param launcheditor   
	 *             The editor which receives this button.
	 * @param launchstep     
	 *             The step that will be edited.
	 */
	public ImportButton(LaunchEditor launcheditor, Launch launchstep) {
		super(Strings.get("eclipse.import"));
		editor = launcheditor;
		launch = launchstep;
        launchfilechooser = new JFileChooser();
        workspacefilechooser = new JFileChooser();
        launchfilefilter = new LaunchFileFilter();
        launchfilechooser.setFileFilter(launchfilefilter);
        workspacefilechooser.setFileFilter(workspacefilefilter);
        workspacefilechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        addActionListener(this);
	}

	
	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed( ActionEvent evt ) {
        if (importEclipse()) {
            editor.fireStepChanged();
        }
	}
	
	
    /**
     * Enforces the user to select the workspace.
     * 
     * @param projectname
     *            The name of the project used within the launch configuration.
     * @return The location of the workspace or null if none has been selected.
     */
    private File selectWorkspace(String projectname) {
        workspacefilefilter.setProjectName(projectname);
        File result = null;
        int option = workspacefilechooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            result = workspacefilechooser.getSelectedFile();
        }
        return (result);
    }

    /**
     * Locates the workspace directory which may be parental to the Java launch
     * configuration.
     * 
     * @param file
     *            The directory to start from.
     * @return The location of the workspace directory or null.
     */
    private File locateWorkspace(File file) {
        if (file == null) {
            return (null);
        }
        File metadata = new File(file, ".metadata");
        if (metadata.isDirectory()) {
            return (file);
        }
        return (locateWorkspace(file.getParentFile()));
    }
    
    
    /**
     * Runs the import process of an Eclipse launch configuration.
     * 
     * @return true <=> A launch configuration could be imported.
     */
    private boolean importEclipse() {

        boolean result = false;

        // load the launch configuration first
        JavaApplicationLaunchConfiguration launchconfig = selectLaunchConfiguration();
        if (launchconfig != null) {

            // try to locate the workspace (potentially a parental directory)
            File workspacedir = locateWorkspace(launchconfig.getLaunchFile().getParentFile());
            if (workspacedir == null) {
                // the user must select the workspace directory
                workspacedir = selectWorkspace(launchconfig.getProjectName());
            }

            if (workspacedir != null) {

                // try to resolve the classpath
                Workspace workspace = Workspace.getWorkspace(workspacedir);
                String cp = loadClasspath(workspace, launchconfig);
                File projectlocation = workspace.getChild(launchconfig
                        .getProjectName());

                String progargs = launchconfig.getProgramArguments();
                if (progargs == null) {
                	progargs = "";
                }
                String[] splitted = progargs.split(" ");
                StringBuffer argsbuffer = new StringBuffer();
                for (int i = 0; i < splitted.length; i++) {
                    if (i > 0) {
                    	argsbuffer.append(" ");
                    }
                    File file = new File(projectlocation, splitted[i]);
                    if (file.exists()) {
                        splitted[i] = "[" + file.getAbsolutePath() + "]";
                    } else {
                        splitted[i] = "[" + splitted[i] + "]";
                    }
                    argsbuffer.append(splitted[i]);
                }

                // modify the step
                launch.setArguments(argsbuffer.toString());
                launch.setClasspath(cp);
                launch.setTargetClassName(launchconfig.getMainType());

                // ant4eclipse assures the File.pathSeparator
                editor.classpath.setValues(cp.split(File.pathSeparator));
                editor.target.setText(launchconfig.getMainType());
                editor.arguments.setValues(splitted);

                result = true;

            }

        }

        return (result);

    }    
    
    /**
     * Resolves the classpath from a Java launch configuration.
     * 
     * @param workspace
     *            The workspace which is used for the Eclipse project.
     * @param launchconfig
     *            The launch configuration providing the necessary data.
     * @return The resolved classpath.
     * @pre workspace != null
     * @pre launchconfig != null
     * @post result != null
     */
    private String loadClasspath(Workspace workspace,
            JavaApplicationLaunchConfiguration launchconfig) {
        ResolvedPathEntry[] entries = null;
        try {
            entries = RuntimeClasspathResolver.resolveRuntimeClasspath(
                    workspace, launchconfig, false);
        } catch (FileParserException ex) {
            /**
             * @todo [15-Apr-2006:KASI] There should be a proper error message
             *       here.
             */
            return ("");
        }
        StringBuffer buffer = new StringBuffer();
        boolean added = false;
        for (int i = 0; i < entries.length; i++) {
            // we're checking for resolved entries since containers like JRE
            // don't need to be added here, since Abbot doesn't support specific
            // runtime information
            if (entries[i].isResolved()) {
                if (added) {
                    buffer.append(File.pathSeparator);
                }
                buffer.append(entries[i].getResolvedEntryAsFile()
                        .getAbsolutePath());
                added = true;
            }
        }
        return (buffer.toString());
    }    

    /**
     * Selects and loads launch configuration file.
     * 
     * @return The launching configuration information.
     */
    private JavaApplicationLaunchConfiguration selectLaunchConfiguration() {
        JavaApplicationLaunchConfiguration result = null;
        int option = launchfilechooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            File location = launchfilechooser.getSelectedFile();
            if (location.isFile()) {
                try {
                    LaunchFileParser parser = new LaunchFileParser(location);
                    AbstractLaunchConfiguration config = parser
                            .getLaunchConfiguration();
                    if (config instanceof JavaApplicationLaunchConfiguration) {
                        result = (JavaApplicationLaunchConfiguration) config;
                    }
                } catch (FileParserException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return (result);
    }
    
	
    /**
     * FileFilter implementation used to select Eclipse launch configuration
     * files.
     */
    private static class LaunchFileFilter extends
            javax.swing.filechooser.FileFilter {

        /**
         * {@inheritDoc}
         */
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return (true);
            }
            /**
             * @todo [13-Apr-2006:KASI] There should be OS specific comparison
             *       (upper/lower/ignore).
             */
            String name = file.getName();
            return (name.endsWith(".launch"));
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return ("Eclipse Launch Configuration (*.launch)");
        }

    } /* ENDCLASS */

    /**
     * FileFilter implementation used to select an Eclipse workspace.
     */
    private static class WorkspaceFileFilter extends
            javax.swing.filechooser.FileFilter {

        private String project;

        private String relative;

        /**
         * Initializes this FileFilter which is used to select a Workspace.
         */
        public WorkspaceFileFilter() {
            project = null;
            relative = null;
        }

        /**
         * Changes the name of the currently used projectname.
         * 
         * @param newproject
         *            The name of the currently used projectname.
         * @pre newproject.length() > 0
         */
        public void setProjectName(String newproject) {
            project = newproject;
            relative = ".plugins/org.eclipse.core.resources/.projects/"
                    + project;
        }

        /**
         * {@inheritDoc}
         */
        public boolean accept(File file) {
            if (file.isDirectory()) {
                File child = new File(file, ".metadata");
                if (child.isDirectory()) {
                    File projectdir = new File(child, relative);
                    return (projectdir.isDirectory());
                }
            }
            return (false);
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return ("Eclipse Workspace");
        }

    } /* ENDCLASS */
	
	
} /* ENDCLASS */
