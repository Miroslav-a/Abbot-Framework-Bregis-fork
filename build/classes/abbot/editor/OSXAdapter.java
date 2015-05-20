package abbot.editor;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Action;
import abbot.Log;

/** Provide access to OSX application hooks.  Compilable on all platforms. */
public class OSXAdapter implements InvocationHandler {

    private static Object APPLICATION;
    private static synchronized Object application() {
        if (APPLICATION == null) {
            String cname = "com.apple.eawt.Application";
            try {
                APPLICATION = Class.forName(cname).newInstance();
            }
            catch(Exception e) {
                throw new Error("Can't load class " + cname + ": " + e);
            }
        }
        return APPLICATION;
    }

    /** Values are auto-generated proxy OSX listener. */
    private static Map adapters = new WeakHashMap();
    private WeakReference source;
    private Action about;
    private Action prefs;
    private Action quit;
    private Object listener;
    
    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().startsWith("mac");
    }

    private OSXAdapter(Frame context, Action quit, Action about, Action prefs) {
        this.source = new WeakReference(context);
        this.quit = quit;
        this.about = about;
        this.prefs = prefs;
        try {
            Object app = application();
            Class iface = Class.forName("com.apple.eawt.ApplicationListener");
            listener = 
                Proxy.newProxyInstance(OSXAdapter.class.getClassLoader(),
                                       new Class[] { iface }, this); 
            Method m = app.getClass().getDeclaredMethod("addApplicationListener",
                                                        new Class[] { iface });
            m.invoke(app, new Object[] { listener });
            enablePrefs(prefs != null);
            Object old = adapters.put(context, this);
            if (old instanceof OSXAdapter) {
                ((OSXAdapter)old).unregister();
            }
        }
        catch(Exception e) { 
            Log.warn(e);
        }
    }
    
    private void unregister() {
        try {
            Object app = application();
            Class iface = Class.forName("com.apple.eawt.ApplicationListener");
            Method m = app.getClass().getDeclaredMethod("removeApplicationListener",
                                                        new Class[] { iface });
            m.invoke(app, new Object[] { listener });
        }
        catch(Exception e) {
            Log.warn(e);
        }
    }
        
    private static final Class[] BOOL_ARGS = new Class[] {
        boolean.class,
    };
    private void setHandled(Object event, boolean handled) {
        try { 
            Method m = 
                event.getClass().getDeclaredMethod("setHandled", BOOL_ARGS);
            m.invoke(event, new Object[] { new Boolean(handled) });
        }
        catch(Exception e) { 
            Log.warn(e);
        }
    }

    /** Returns non-null only if the owner frame is active or if there
     * are no active frames.
     */
    private Object getSource() {
        Object src = source.get();
        if (src == null) {
            unregister();
        }
        else {
            Frame[] frames = Frame.getFrames();
            for (int i=0;i < frames.length;i++) {
                if (frames[i].isActive()) {
                    if (src == frames[i]) {
                        return src;
                    }
                }
            }
        }
        // It wasn't for our frame, so don't process the event
        return null;
    }
    
    private ActionEvent createEvent(Object src, Action action) {
        return new ActionEvent(src, ActionEvent.ACTION_PERFORMED,
                               (String)action.getValue(Action.NAME),
                               System.currentTimeMillis(), 0);
    }
    
    public void handleAbout(Object e) {
        setHandled(e, true);
        if (about != null) {
            Object src = getSource();
            if (src != null) {
                about.actionPerformed(createEvent(src, about));
            }
        }
    }
	
    public void handlePreferences(Object e) {
        setHandled(e, true);
        if (prefs!= null) {
            Object src = getSource();
            if (src != null) {
                prefs.actionPerformed(createEvent(src, prefs));
            }
        }
    }
	
    public void handleQuit(Object e) {
        // You MUST setHandled(false) if you want to delay or cancel the quit.
        // This is important for cross-platform development -- have a
        // universal quit routine that chooses whether or not to quit, so the
        // functionality is identical on all platforms.  This example simply
        // cancels the AppleEvent-based quit and defers to that universal
        // method. 
        setHandled(e, false);
        Object src = getSource();
        if (src != null) {
            quit.actionPerformed(createEvent(src, quit));
        }
    }
	
    /** User clicked on application in the Dock. */
    public void handleReOpenApplication(Object e) { }
    public void handleOpenApplication(Object e) { }
    public void handleOpenFile(Object e) { }
    public void handlePrintFile(Object e) { }

    /** Register the given special frame actions with OSX.  About and prefs 
     * actions may be null, but quit must be non-null.
     */
    public static void register(Frame owner, Action quit, Action about, Action prefs) {
        if (isMac()) {
            if (owner == null)
                throw new NullPointerException("Parent Frame may not be null");
            if (quit == null)
                throw new NullPointerException("Quit action may not be null");
            new OSXAdapter(owner, quit, about, prefs);
        }
    }

    /** Unregister the given frame's actions. */
    public static void unregister(Frame owner) { 
        if (isMac()) {
            Object listener = adapters.get(owner);
            if (listener != null) {
                Object adapter = Proxy.getInvocationHandler(listener);
                if (adapter instanceof OSXAdapter) {
                    ((OSXAdapter)adapter).unregister();
                }
            }
        }
    }
    
    /** Another static entry point for EAWT functionality.  Sets the enabled 
     * stste of the "Preferences..." menu item in the application menu. 
     */
    public static void enablePrefs(boolean enabled) {
        if (isMac()) {
            try {
                Object app = application();
                Method m = app.getClass().getDeclaredMethod("setEnabledPreferencesMenu",
                                                            BOOL_ARGS);
                m.invoke(app, new Object[] { new Boolean(enabled) });
            }
            catch(Exception e) { 
                Log.warn(e);
            }
        }
    }

    /** Handle all calls from the host OS. */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if ("handleAbout".equals(name)) {
            handleAbout(args[0]);
        }
        else if ("handlePreferences".equals(name)) {
            handlePreferences(args[0]);
        }
        else if ("handleQuit".equals(name)) {
            handleQuit(args[0]);
        }
        else if ("handleReOpenApplication".equals(name)) {
            handleReOpenApplication(args[0]);
        }
        else if ("handleOpenApplication".equals(name)) {
            handleOpenApplication(args[0]);
        }
        else if ("handleOpenFile".equals(name)) {
            handleOpenFile(args[0]);
        }
        else if ("handlePrintFile".equals(name)) {
            handlePrintFile(args[0]);
        }
        else {
            throw new Error("Unimplemented method " + method);
        }
        return null;
    }
}

