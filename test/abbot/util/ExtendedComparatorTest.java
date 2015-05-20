package abbot.util;

import junit.extensions.abbot.TestHelper;
import junit.framework.TestCase;

/** Test ArgumentParser functions. */

public class ExtendedComparatorTest extends TestCase {

    public void testCompareNull() {
        assertTrue("Compare against null should fail",
                   !ExtendedComparator.equals("this", null));
        assertTrue("Compare against null should fail",
                   !ExtendedComparator.equals(null, "this"));
    }

    public void testCompareStrings() {
        String[][] matchValues = { 
            { "Orange", "Orange" },
            { "/O.*/", "Orange" },
            { "/^Or....$/", "Orange" },
            { "/One|Two/", "One" },
            { "/One|Two/", "Two" },
            { "/", "/" },
            { "///", "/" },
        };
        for (int i=0;i < matchValues.length;i++) {
            assertTrue("String <" + matchValues[i][0]
                       + "> should match <" + matchValues[i][1] + ">",
                       ExtendedComparator.
                       stringsMatch(matchValues[i][0], matchValues[i][1]));
        }
        String[][] nonmatchValues = { 
            { "Black", "Orange" },
            { "Ora", "Orange" },
            { "O.*", "XOrange" },
            { "/^Or....$/", "XOrange" },
            { "///", "No slash" },
        };
        for (int i=0;i < nonmatchValues.length;i++) {
            assertTrue("String <" + nonmatchValues[i][0]
                       + "> should not match <" + nonmatchValues[i][1] + ">",
                       !ExtendedComparator.
                       stringsMatch(nonmatchValues[i][0],
                                    nonmatchValues[i][1]));
        }
        // I can't get multi-line matches to work, either with gnu regexp or
        // java.util.regex.*; 
        /*
        String[][] multiValues = {
            { "/(?m)One$Two/", "One\nTwo" },
        };
        for (int i=0;i < multiValues.length;i++) {
            assertTrue("String <" + multiValues[i][0]
                       + "> should match <" + multiValues[i][1] + ">",
                       ExtendedComparator.
                       stringsMatch(multiValues[i][0],
                                    multiValues[i][1]));
        }
        String[][] nmultiValues = {
            { "/^Two$/", "One\nTwo\nThree" },
        };
        for (int i=0;i < nmultiValues.length;i++) {
            assertTrue("String <" + nmultiValues[i][0]
                       + "> should not match <" + nmultiValues[i][1] + ">",
                       !ExtendedComparator.
                       stringsMatch(nmultiValues[i][0],
                                    nmultiValues[i][1]));
        }
        */
    }

    /** Checks array comparisons. */
    public void testArrayEquals() {
        String[] array0 = { };
        String[] array0a = { };
        String[] array1 = { "one" };
        String[] array1a = { "one" };
        String[] array1b = { "six" };
        String[] array2 = { "one", "two" };
        String[] array2a = { "one", "two" };
        String[] array5 = { "one", "two", "three", "four", "five" };
        String[] array5a = { "one", "two", "three", "four", "five" };
        String[] array6 = { "/[0-9]* count/", "Orange", "Black" };
        String[] array6a = { "99 count", "Orange", "Black" };
        String[] array6b = { "99 count", "XOrange", "Black" };

        assertTrue("Array size 0 comparison failed", 
                   ExtendedComparator.equals(array0, array0a));
        assertTrue("Array size 0 discomparison failed", 
                   !ExtendedComparator.equals(array0, array1));
        assertTrue("Array size 1 comparison failed", 
                   ExtendedComparator.equals(array1, array1a));
        assertTrue("Array size 1 discomparison failed", 
                   !ExtendedComparator.equals(array1, array1b));
        assertTrue("Array size 1 discomparison failed", 
                   !ExtendedComparator.equals(array1, array2));
        assertTrue("Array size 2 comparison failed", 
                   ExtendedComparator.equals(array2, array2a));
        assertTrue("Array size 5 comparison failed", 
                   ExtendedComparator.equals(array5, array5a));
        assertTrue("Array/object discomparison failed",
                   !ExtendedComparator.equals(array5, new Integer(0)));
        assertTrue("Arrays of different types discomparison failed",
                   !ExtendedComparator.equals(array5, new Integer[5]));
        assertTrue("String array with regexp pattern should match",
                   ExtendedComparator.equals(array6, array6a));
        assertTrue("String array with regexp pattern should not match",
                   !ExtendedComparator.equals(array6, array6b));
    }

    /** Create a new test case with the given name. */
    public ExtendedComparatorTest(String name) {
        super(name);
    }

    /** Return the default test suite. */
    public static void main(String[] args) {
        TestHelper.runTests(args, ExtendedComparatorTest.class);
    }
}
