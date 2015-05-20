package abbot.editor.editors;

import javax.swing.text.*;

import junit.extensions.abbot.*;
import abbot.script.*;
import abbot.tester.*;
import abbot.finder.matchers.*;

/** Verify ExpressionEditor operation. */

public class ExpressionEditorTest extends ResolverFixture {

    public void testExpressionField() throws Exception {
        Expression e = new Expression(getResolver(), getName());
        String EXPR = "hello = \"world\"";
        e.setExpression(EXPR);
        ExpressionEditor editor = new ExpressionEditor(e);
        showFrame(editor);

        JTextComponent tc = (JTextComponent)getFinder().
            find(editor, new NameMatcher("expression.text"));
        assertEquals("Wrong initial text", EXPR, tc.getText());

        String EXPR1 = "empty";
        JTextComponentTester tester = new JTextComponentTester();
        tester.actionEnterText(tc, EXPR1);
        assertEquals("Wrong changed text", EXPR1, e.getExpression());
    }

    /** Construct a test case with the given name. */
    public ExpressionEditorTest(String name) {
        super(name);
    }

    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ExpressionEditorTest.class);
    }
}
