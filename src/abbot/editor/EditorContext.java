package abbot.editor;

import abbot.finder.TestHierarchy;

import abbot.script.Script;

import java.awt.Component;

import java.io.File;

import java.util.HashSet;
import java.util.Set;

/**
 * Provide a transport for the editor context. This is usefull in cases where
 * the costello editor is invoked inside of another application. It can also
 * be used to provide further editor customization, for example a new file
 * template, depending on the context.
 */

public class EditorContext {

    private String _arguments[];
    private boolean _embedded;
    private TestHierarchy _hierarchy = null;
    private SecurityManager _securityManager = null;
    private File _newFileTemplate = null;
    private FileSystemHelper _fileSystemHelper = new FileSystemHelper();

    public EditorContext(String[] arguments) {
        _arguments = arguments;
    }

    public EditorContext() {
        _arguments = new String[0];
    }


    //
    // Getters and setters
    //

    public String[] getArguments() {
        return (String[])_arguments.clone();
    }

    public void setArguments(String[] arguments) {
        _arguments = arguments;
    }

    /**
     * @return Whether the costello editor is being embdeed in another application
     */
    public

    boolean isEmbedded() {
        return _embedded;
    }

    /**
     * @param embedded Whether costello is embedded in another application
     */
    public void setEmbedded(boolean embedded) {
        _embedded = embedded;
    }

    /**
     * @return A hierarchy that can preconfigured to filter certain components
     * ignoreExisting call. This is usefull in the embedded case.
     */
    public TestHierarchy getTestHierarchy() {
        return _hierarchy;
    }

    /**
     * @param hierarchy A hierarchy that can preconfigured to filter certain components
     * @throws IllegalStateException if we are not in an embedded mode
     */
    public void setTestHierarchy(TestHierarchy hierarchy) {
        if (!isEmbedded()) {
            throw new IllegalStateException("Only value when Costello is to be embedded");
        }
            
        _hierarchy = hierarchy;
    }

    /**
     * @param newFileTemplate The file to be used when creating new
     * scripts.
     */
    public void setNewFileTemplate(File newFileTemplate) {
        if (!newFileTemplate.exists()) {
            throw new IllegalArgumentException("File doesn't exist");
        }
        if (!Script.isScript(newFileTemplate)) {
            throw new IllegalArgumentException("File doesn't appears to be a script");
        }

        _newFileTemplate = newFileTemplate;
    }

    /**
     * @return The file to be used
     */
    public File getNewFileTemplate() {
        return _newFileTemplate;
    }

    /**
     * @param securityManager The secutiry manager for this context
     */

    public void setSecurityManager(SecurityManager securityManager) {
        _securityManager = securityManager;
    }
    
    /**
     * @return The security manage for this context
     */

    public SecurityManager getSecurityManager() {
        return _securityManager;
    }
    
    
    /**
     * @param helper A file system helper for this context, usefull for the
     *   case where the costello editor is emdedded in an IDE
     */
    public void setFileSysteHelper(FileSystemHelper helper) {
        _fileSystemHelper = helper; 
    }
    
    /**
     * @return A file system helper for this context
     */
    
    public FileSystemHelper getFileSystemHelper() {
        return _fileSystemHelper;
    }

}
