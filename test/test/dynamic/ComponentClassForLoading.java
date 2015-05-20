package test.dynamic;

/** Simple class with some static initialization that can be observed without
 * actually operating on any of the class's interface.
 */
public class ComponentClassForLoading extends java.awt.Component {
    private static int instanceCount = 0;

    public ComponentClassForLoading() {
        synchronized(getClass()) {
            ++instanceCount;
        }
    }

    public String toString() {
        synchronized(getClass()) {
            return String.valueOf(instanceCount);
        }
    }
}
