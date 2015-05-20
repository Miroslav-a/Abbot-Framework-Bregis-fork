package abbot;

import junit.extensions.abbot.*;

/** Check version settings.  */
public class PlatformTest extends ComponentTestFixture {

    public void testVersionParsing() {
        String[] versions = {
            "1.2",
            "1.2.0_01",
            "1.3",
            "1.3.1_06",
            "1.4.0_02",
            "1.4.1_01",
        };
        int[] values = { 0x1200, 0x1201, 0x1300, 0x1316, 0x1402, 0x1411 };
        for (int i=0;i < versions.length;i++) {
            int version = Platform.parse(versions[i]);
            assertEquals("Wrong version from '" + versions[i] + "'",
                         Integer.toHexString(values[i]),
                         Integer.toHexString(version));
        }
        assertTrue("Wrong version "
                   + Integer.toHexString(Platform.JAVA_VERSION),
                   Platform.JAVA_VERSION > Platform.JAVA_1_1);
    }


    public PlatformTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, PlatformTest.class);
    }
}
