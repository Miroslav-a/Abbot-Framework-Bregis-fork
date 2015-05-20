package abbot.script;

import java.lang.reflect.Method;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.JList;

import junit.extensions.abbot.*;
import abbot.finder.Hierarchy;
import abbot.tester.*;

import javax.swing.text.JTextComponent;

public class ActionTest extends ResolverFixture {

    public void testMethodFixing() throws Throwable {
        Action action = new Action(getResolver(), getName(),
                                   "Click", new String[0]);
        assertEquals("Method name should be fixed",
                     "actionClick", action.getMethodName());
    }

    private class FakeRef extends ComponentReference {
        public FakeRef(Resolver r) {
            this(r, Component.class);
        }
        public FakeRef(Resolver r, Class cls) {
            super(r, cls, new HashMap());
        }
        // Fake the lookup; we don't care about the result
        public Component getComponent(Hierarchy h) { return null; }
    }

    private class MyAction extends Action {
        public MyAction(Resolver r, String method, String[] args, Class cls) {
            super(r, getName(), method, args, cls);
        }
        public MyAction(Resolver r, String method, String[] args) {
            this(r, method, args, Component.class);
        }
        public Object[] getParameters() throws Exception {
            return evaluateParameters(getMethod(), getArguments());
        }

        @Override
        protected Method[] getMethods() throws ClassNotFoundException, NoSuchMethodException {
            return super.getMethods();
        }
    }

    public void testVKPlusModifier() throws Exception {
        ComponentReference ref = new FakeRef(getResolver());
        MyAction action = new MyAction(getResolver(), "actionKeyStroke", 
                                       new String[] {
                                           ref.getID(), "VK_A", "SHIFT_MASK",
                                       });
        Object[] params = action.getParameters();
        assertEquals("Second parameter should be an integer",
                     new Integer(KeyEvent.VK_A), params[1]);
        assertEquals("Third parameter should be an integer",
                     new Integer(KeyEvent.SHIFT_MASK), params[2]);
    }


    /**
     * Test for method ordering ala bug 3524725 in JDK 7
     */
    public void testSortedMethodsWithPrimitivesFirst() throws Exception {
        ComponentReference ref = new FakeRef(getResolver());
        MyAction action = new MyAction(getResolver(), "actionClick", 
                                       new String[] {
                                           ref.getID(), "0",
                                       }, JTextComponent.class);
        
        Method[] methods = action.getMethods();

        assertEquals("Second parameter of first method should be an int",
                     methods[0].getParameterTypes()[1], int.class);
        assertEquals("Second parameter of second method should be an String",
                     methods[1].getParameterTypes()[1], String.class);
    }


    public void testComponentPlusVK() throws Exception {
        ComponentReference ref = new ComponentReference(getResolver(),
                                                        Component.class,
                                                        new HashMap());
        MyAction action = new MyAction(getResolver(), "actionKeyStroke", 
                                       new String[] {
                                           ref.getID(), "VK_X",
                                       });
        Method method = action.getMethod();
        Class[] params = method.getParameterTypes();
        assertEquals("First parameter should be a Component",
                     Component.class, params[0]);
        assertEquals("Second parameter should be an integer",
                     int.class, params[1]);
    }

    public void testMultipleModifiers() throws Exception {
        MyAction action = new MyAction(getResolver(), "actionKeyStroke", 
                                       new String[] { 
                                           "VK_B",
                                           "SHIFT_MASK|ALT_MASK",
                                       });
        Object[] params = action.getParameters();
        assertEquals("First parameter should be an integer",
                     new Integer(KeyEvent.VK_B), params[0]);
        assertEquals("Second parameter should be an integer",
                     new Integer(KeyEvent.SHIFT_MASK|KeyEvent.ALT_MASK),
                     params[1]);
    }

    public void testVKArgument() throws Exception {
        MyAction action = new MyAction(getResolver(), "actionKeyStroke", 
                              new String[] { "VK_C", });
        Object[] params = action.getParameters();
        assertEquals("First parameter should be an integer",
                     new Integer(KeyEvent.VK_C), params[0]);
    }

    public void testComponentLocationMethod() throws Throwable {
        ComponentReference ref = new FakeRef(getResolver());
        MyAction action = new MyAction(getResolver(), "Click", new String[] {
            ref.getID(), "(0,0)"
        });
        String[] args = action.getArguments();
        assertEquals("Wrong component reference ID",
                     ref.getID(), args[0]);
        Object[] params = action.getParameters();
        assertEquals("Second parameter should be a ComponentLocation",
                     new ComponentLocation(new Point(0, 0)), params[1]);
    }

    public void testComponentLocationWithSubstitution() throws Exception {
        final String PROPNAME = "user.name";
        ComponentReference ref = new FakeRef(getResolver(), JList.class);
        MyAction action = new MyAction(getResolver(), "Click", new String[] {
            ref.getID(), "\"${" + PROPNAME + "}\""
        }, JListTester.class);
        String[] args = action.getArguments();
        Object[] params = action.getParameters();
        assertEquals("Wrong target class", 
                     JListTester.class, action.getTargetClass());
        assertEquals("Second parameter should be a ComponentLocation",
                     new JListLocation(System.getProperty(PROPNAME)), 
                     params[1]);
    }

    public ActionTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, ActionTest.class);
    }
}

