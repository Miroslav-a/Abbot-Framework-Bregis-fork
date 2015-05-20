package abbot.i18n;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import junit.extensions.abbot.TestHelper;

/** Test Resource loading. */
public class StringsTest extends junit.framework.TestCase {

    public void testMissingResource() throws Throwable {
        File dir = TestHelper.getClasspathDirectory(StringsTest.class);
        Properties p = new Properties();
        p.put("default.properties.loaded", "yes");
        p.store(new FileOutputStream(new File(dir, "testMissingResource.properties")), "testMissingResource");
        Strings.addBundle("testMissingResource");
        Locale oldLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.FRENCH);
            String name = "NoSuchResource";
            assertEquals("Missing resource will be a flagged key",
                         "#" + name + "#", Strings.get(name));
            
            assertEquals("Missing i18n resource should get default value",
                         "yes", Strings.get("default.properties.loaded"));
        }
        finally {
            Locale.setDefault(oldLocale);
        }
    }

    public void testMissingPropertyFile() throws Throwable {
        try {
            Strings.addBundle("missing", getClass().getClassLoader());
            fail("Expected a MissingResourceException");
        }
        catch(MissingResourceException e) {
        }
    }
    
    public void testAlternateResource() throws Throwable {
        File dir = TestHelper.getClasspathDirectory(StringsTest.class);
        Properties p = new Properties();
        p.put("test.property", "some value");
        p.store(new FileOutputStream(new File(dir, "testAlternateResource.properties")), "testAlternateResource");
        Strings.addBundle("testAlternateResource", getClass().getClassLoader());
        assertTrue("Should be able to load additional properties",
                   Strings.get("test.property") != null);
                   
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, StringsTest.class);
    }
}
