package abbot.script;

import junit.extensions.abbot.*;

public class SampleTest extends ResolverFixture {

    public void testSaveProperty() throws Throwable {
        String PROP_NAME = "my.int";
        abbot.script.Resolver r = getResolver();
        Sample call = new Sample(r, "Test property from Sample",
                                 CPTestClass.class.getName(),
                                 "intReturningMethod", new String[0], null);
        call.setPropertyName(PROP_NAME);
        call.run();
        assertEquals("Property should be set to the return value",
                     new Integer(CPTestClass.intReturningMethod()),
                     r.getProperty(PROP_NAME));
    }

    public SampleTest(String name) { super(name); }

    public static void main(String[] args) {
        TestHelper.runTests(args, SampleTest.class);
    }
}

class CPTestClass {
    public static int intReturningMethod() {
        return 0;
    }
}
