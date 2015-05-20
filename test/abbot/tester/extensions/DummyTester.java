package abbot.tester.extensions;

class DummyTester extends abbot.tester.ComponentTester {
    protected String deriveTag() {
        return "This is a dummy extension tester for class loader tests";
    }
}
