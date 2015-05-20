package junit.extensions.abbot;

import java.awt.Window;
import java.util.Iterator;

import junit.framework.TestCase;
import abbot.Log;
import abbot.finder.BasicFinder;
import abbot.script.Resolver;
import abbot.script.Script;

/** Simple wrapper for testing objects which require a Resolver. */

public abstract class ResolverFixture extends ComponentTestFixture {

    private Resolver resolver;
    
    /** Obtain a consistent resolver. */
    protected Resolver getResolver() { return resolver; }

    /** Fixture setup is performed here, to avoid problems should a derived
        class define its own setUp and neglect to invoke the superclass
        method. 
    */
    protected void fixtureSetUp() throws Throwable {
        super.fixtureSetUp();
        // FIXME kind of a hack, but Script is the only implementation of
        // Resolver we've got at the moment.
        resolver = new Script(getHierarchy());
    }

    /** Fixture teardown is performed here, to avoid problems should a derived
        class define its own tearDown and neglect to invoke the superclass
        method.  
    */
    protected void fixtureTearDown() throws Throwable {
        super.fixtureTearDown();
        resolver = null;
    }

    /** Construct a test case with the given name.  */
    public ResolverFixture(String name) {
        super(name);
    }

    /** Default Constructor.  The name will be automatically set from the
        selected test method.
    */ 
    public ResolverFixture() { }
}
