package abbot.editor;

import java.awt.Component;
import java.lang.reflect.Method;
import java.util.*;

import junit.extensions.abbot.*;

public class ComponentPropertyModelTest extends ComponentTestFixture {

    public void testFilterTesterMethods() {
        ComponentPropertyModel model = new ComponentPropertyModel();
        Method[] methods = model.getPropertyMethods(Component.class, false);
        List list = Arrays.asList(methods);
        String[] filtered = { "getTag", "getTester" };
        for (int i=0;i < filtered.length;i++) {
            assertTrue(filtered[i] + " not filtered",
                       !list.contains(filtered[i]));
        }
    }
    
    /** Run the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ComponentPropertyModelTest.class);
    }
}
