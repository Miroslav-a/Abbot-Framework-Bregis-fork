package abbot.editor;

import java.io.File;

/**
 * This class is provided as part of the EditorContext object and allows the
 * editor to make certain request of the ownining context.
 */

public class FileSystemHelper {
    
    
    /**
     * When the script editor is saving scripts it uses this function to ensure
     * that the file is writable. This is usefull in the context of source control
     * system that require a checkout operation before a file can be written to.
     * By default this method does nothing as the actions permitted by {@link File} are
     * rather limited. 
     * 
     * @param file The file that is being requested to make writable
     * @return Whether the file can be written so, by default just return the file state
     */
    
    public boolean makeWritable(File file) {
       return file.exists() ? file.canWrite() : true;
    }
    
}
