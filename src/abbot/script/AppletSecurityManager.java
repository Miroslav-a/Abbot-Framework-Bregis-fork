package abbot.script;

import abbot.ExitException;
import javax.swing.SwingUtilities;

/** This security manager extends sun.applet.AppletSecurity b/c
 * AppletViewer does some casts that assume that is the only security
 * manager that will be installed.  It has to permit everything, though, or
 * the framework will be hampered.  Because of this, it isn't a reliable test
 * of an applet responding well to restricted permissions.
 */
// FIXME need to determine what causes the class circularity errors and then
// defer to the AppletSecurity
// NOTE: don't see the class circularity any more, maybe the class loading
// restructuring in 0.9/0.10 has fixed it?
public class AppletSecurityManager extends sun.applet.AppletSecurity {

    SecurityManager parent;
    boolean removeOnExit;
    public AppletSecurityManager(SecurityManager sm) {
        this(sm, false);
    }

    public AppletSecurityManager(SecurityManager sm, boolean removeOnExit) {
        parent = sm;
        this.removeOnExit = removeOnExit;
    }

    public void checkPermission(java.security.Permission perm,
                                Object context) {
        if (parent != null) {
            parent.checkPermission(perm, context);
        }
    }
    public void checkPermission(java.security.Permission perm) {
        if (parent != null) {
            parent.checkPermission(perm);
        }
    }

    public void checkExit(int status) {
        if (removeOnExit) {
            System.setSecurityManager(parent);
        }
        throw new ExitException("Applet System.exit disallowed on " 
                                + Thread.currentThread(), status);
    }
}
