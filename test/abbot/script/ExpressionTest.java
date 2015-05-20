package abbot.script;

import java.io.File;

import java.io.FileOutputStream;
import java.io.OutputStream;

import junit.extensions.abbot.*;

public class ExpressionTest extends ResolverFixture {

    public void testFromXML() throws Throwable {
        String EXPR = "System.out.println(\"hello\");";
        String XML = "<expression><![CDATA[" + EXPR + "]]></expression>";

        Expression e = (Expression)Step.createStep(getResolver(), XML);
        assertEquals("Wrong expression parsed", EXPR, e.getExpression());

        assertEquals("Wrong encoding", XML, Step.toXMLString(e));
    }

    public void testSimpleExpression() throws Throwable {
        Expression e = new Expression(getResolver(), getName());
        assertEquals("Default expression should be empty",
                     "", e.getExpression());

        e.setExpression("i=10;");
        e.runStep();
        Interpreter sh = (Interpreter)
            getResolver().getProperty(Script.INTERPRETER);
        assertEquals("Wrong value for expression",
                     new Integer(10), sh.get("i"));
    }
    
    public void testVariableExpansion() throws Throwable {
        Expression e = new Expression(getResolver(), getName());
        assertEquals("Default expression should be empty",
                     "", e.getExpression());

        final String NAME = "bloody hell";
        getResolver().setProperty("name", NAME);
        e.setExpression("name=\"${name}\";");
        e.runStep();
        Interpreter sh = (Interpreter)
            getResolver().getProperty(Script.INTERPRETER);
        assertEquals("Wrong value for expression",
                     NAME, sh.get("name"));
    }

    /**
     * Verify bug fix 3441155
     */
    public void testSourceScript() throws Throwable {

        File tempF = File.createTempFile("script", "location");
        tempF.delete();
        File temporaryDirectory = new File(tempF.getPath() + "/");
        temporaryDirectory.mkdir();
        File script = new File(temporaryDirectory, "script.xml");
        File groovy = new File(temporaryDirectory, "simple.groovy");
        
        OutputStream scriptOutput = new FileOutputStream(groovy, true);
        scriptOutput.write(
            "println \"Hello World\";\nexample = \"Example\";".getBytes());
        scriptOutput.close();
        

        Script resolver = (Script)getResolver();
        resolver.setFile(script);
        
        Expression e = new Expression(resolver, getName());

        e.setExpression("source(\"simple.groovy\");");
        e.runStep();
        
        Interpreter sh = (Interpreter)
            getResolver().getProperty(Script.INTERPRETER);
        
        // Check that the script actually worked
        assertEquals("Wrong value for expression",
                     "Example", sh.get("example"));
    }



    public ExpressionTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, ExpressionTest.class);
    }
}

