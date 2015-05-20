package abbot.script;

import java.lang.reflect.Method;

import junit.extensions.abbot.*;

public class CallTest extends ResolverFixture {

    private class TargetedCall extends Call {
        public Object target;
        public TargetedCall(Resolver r, String d,
                            String c, String m, String[] args) {
            super(r, d, c, m, args);
        }
        protected Object getTarget(Method m) throws Throwable {
            return target = getTargetClass().newInstance();
        }
    }

    public void testResolveInheritedMethods() throws Throwable {
        TargetedCall call =
            new TargetedCall(getResolver(), "Test Call",
                             CallTestClass.class.getName(),
                             "whichClass", new String[0]);

        call.run();
        assertEquals("Wrong method invoked for whichClass",
                     CallTestClass.class.getName(),
                     ((CallTestClass)call.target).which);
    }

    public void testResolveDuplicateMethods() throws Throwable {
        // As per bug 3524725 we need to look to whether
        // we match the correct versions of the method depending on
        // the method type
        TargetedCall call =
            new TargetedCall(getResolver(), "Test Call",
                             CallTestClass.class.getName(),
                             "someMethodDuplicatedSameReturn",
                             new String[] { "1" });
        // Invoke the int version
        call.run();
        // Invoke the string version
        call.setArguments("string arg");
        call.run();
    }

    public void testCall() throws Throwable {
        TargetedCall call =
            new TargetedCall(getResolver(), "Test Call",
                             CallTestClass.class.getName(),
                             "someMethod", new String[0]);
        call.run();
        assertTrue("Flag should be set after to invocation",
                   ((CallTestClass)call.target).derivedWasCalled);
    }

    public void testReturnType() throws Throwable {
        TargetedCall call = new TargetedCall(getResolver(), "Test Call",
                             CallTestClass.class.getName(),
                             "intReturningMethod", new String[0]);
        call.run();
        assertTrue("Flag should be set after to invocation",
                   ((CallTestClass)call.target).derivedWasCalled);
    }

    public static class CallTestBase {
        public boolean baseWasCalled = false;
        public String which = null;
        public String whichClass() {
            return which = CallTestBase.class.getName();
        }
    }

    public static class CallTestClass extends CallTestBase {
        public boolean derivedWasCalled = false;
        public void someMethod() {
            derivedWasCalled = true;
        }


        public void someMethodDuplicatedSameReturn(int arg) {
        }
        public void someMethodDuplicatedSameReturn(String arg) {
        }


        public int someMethodDuplicated(int arg) {
            return 0;
        }
        public String someMethodDuplicated(String arg) {
            return "String";
        }
        public Object someMethodDuplicated(Object arg) {
            return new Object();
        }
        public int intReturningMethod() {
            derivedWasCalled = true;
            return 1;
        }
        public String whichClass() {
            return which = CallTestClass.class.getName();
        }
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, CallTest.class);
    }
}

