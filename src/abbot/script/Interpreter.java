package abbot.script;

import abbot.Log;

import abbot.finder.BasicFinder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


/** Provides a Groovy interpreter customized for the Costello scripting
 * environment.
 */
public class Interpreter  {
    
    private static ScriptEngineManager MANAGER = new ScriptEngineManager();
    private ScriptEngine engine;


    public Interpreter(Resolver r) {
       
        
       
        try {

            engine = MANAGER.getEngineByName("groovy");
            //Bindings bindings = se.getBindings(ScriptEngine.)
            
            engine.put("finder", new BasicFinder(r.getHierarchy()));
            engine.put("resolver", r);
            engine.put("script", r);
            engine.put("engine", engine);
            InputStream is = getClass().getResourceAsStream("init.groovy");
            engine.eval(new BufferedReader(new InputStreamReader(is)));
        }
        catch(ScriptException e) {
            Log.warn("Error initializing interpreter: ",e);
        }
    }
    
    /**
     * Evaluate the groovy expression
     * @throws ScriptException
     */
    public Object eval(String expression) throws ScriptException {
        return engine.eval(expression);
    }
    
    /**
     * @return A bound value
     */
    public Object get(String value) {
        return engine.get(value);
    }
}
