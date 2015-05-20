package abbot.script;

import java.util.Iterator;
import java.util.Map;

import javax.script.ScriptException;

import org.jdom.CDATA;
import org.jdom.Element;


/** Provides evaluation of arbitrary Java expressions.  Any Java expression is
    supported, with a more loose syntax if desired.  See the
    <a href="http://groovy.codehaus.org">groovey documentation</a> for
    complete details of the extended features available in this evaluator.
    <p>
    Note that any variables declared or assigned will be available to any
    subsequent steps in the same Script.
 */

public class Expression extends Step {

    public static final String TAG_EXPRESSION = "expression";

    private static final String USAGE = "<expression>{java/groovey expression}</expression>";
    private String expression = "";

    public Expression(Resolver resolver, Element el, Map attributes) {
        super(resolver, attributes);

        String expr = null;
        Iterator iter = el.getContent().iterator();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof CDATA) {
                expr = ((CDATA)o).getText();
                break;
            }
        }
        if (expr == null)
            expr = el.getText();
        setExpression(expr);
    }

    public Expression(Resolver resolver, String description) {
        super(resolver, description);
    }

    public String getDefaultDescription() { 
        return getExpression();
    }
    public String getUsage() { return USAGE; }
    public String getXMLTag() { return TAG_EXPRESSION; }

    protected Element addContent(Element el) {
        return el.addContent(new CDATA(getExpression()));
    }

    public void setExpression(String text) {
        expression = text;
    }

    public String getExpression() {
        return expression;
    }

    /** Evaluates the expression. */
    protected void runStep() throws Throwable {
        Interpreter sh = (Interpreter)
            getResolver().getProperty(Script.INTERPRETER);
        try {  
            sh.eval(ArgumentParser.substitute(getResolver(), getExpression()));
        }
        catch(ScriptException e) {
            throw e;
        }
    }
}
