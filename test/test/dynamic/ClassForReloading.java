package test.dynamic;

/** Simple class with some static initialization that can be observed without
 * actually operating on any of the class's methods.
 */
public class ClassForReloading {
    private static int instanceCount = 0;

    public ClassForReloading() {
        synchronized(getClass()) {
            ++instanceCount;
        }
    }

    public String toString() {
        return String.valueOf(instanceCount);
    }

    public static void main(String[] args) {
        //System.out.println("Main routine of dynamically loaded class");
    }
}
