package abbot.script;

import junit.extensions.abbot.*;
import abbot.script.parsers.Parser;

/** Test ArgumentParser functions. */

public class ArgumentParserTest extends ResolverFixture {

    public void testParseArgumentList() {
        String[][] expected = {
            { "one" }, // single
            { "one", "" }, // trailing empty
            { "", "two" }, // leading empty
            { "", "" }, // two empty
            { "one", "two", "three" },  // multiple
            { null, "one", " two " }, // spaces
            { "one", " two", " \"three, four, five\"" }, // quotes+commas
            { "one", "two", "[three,three]",
              "[four,four]", "[five%2cfive,six]" }, // arrays
            { // comma should get unescaped in final arg
                "one,one", 
                // brackets should be left unescaped after parsing
                "[two]", 
                // comma within array should be left escaped
                "[three%2cthree,three+]",
                // shouldn't get confused by trailing brackets
                "four,]",
                // use quoting to protect arbitrary characters (strings or ids)
                "\"[five,\"", },
        };
        String[] input = {
            "one",
            "one,",
            ",two",
            ",",
            "one,two,three",
            "null,one, two ",
            "one, two, \"three, four, five\"",
            "one,two,[three,three],[four,four],[five\\,five,six]",
            "one\\,one,[two],[three%2cthree,three+],four\\,],\"[five,\""
        };
        for (int i=0;i < input.length;i++) {
            String[] args = ArgumentParser.parseArgumentList(input[i]);
            assertEquals("Wrong number of arguments parsed from '"
                         + input[i] + "'",
                         expected[i].length, args.length);
            for (int j=0;j < args.length;j++) {
                assertEquals("Badly parsed", expected[i][j], args[j]);
            }
        }
    }

    public void testSubstitution() {
        // value shorter than name
        String PROP_NAME = "my.prop";
        String PROP_VALUE = "value";
        // value longer than name
        String PROP2_NAME = "p2";
        String PROP2_VALUE = "${my.prop}${my.prop}";
        getResolver().setProperty(PROP_NAME, PROP_VALUE);
        getResolver().setProperty(PROP2_NAME, PROP2_VALUE);
        String[][] args = {
            { "${my.prop}", PROP_VALUE },
            { "X${my.prop}", "X" + PROP_VALUE },
            { "${my.prop}X", PROP_VALUE + "X" },
            { "${my.prop", "${my.prop" },
            { "${my.prop}${my.prop}", PROP_VALUE + PROP_VALUE },
            { "A${my.prop}:${my.prop}B${",
              "A" + PROP_VALUE + ":" + PROP_VALUE + "B${" },
            { "${no.prop}${my.prop}", "${no.prop}" + PROP_VALUE },
            { "${p2}", PROP2_VALUE }, // no recursion
        };
        for (int i=0;i < args.length;i++) {
            assertEquals("Bad substitution", args[i][1],
                         ArgumentParser.substitute(getResolver(), args[i][0]));
        }
    }

    public void testEvalWithSubstitution() throws Throwable {
        getResolver().setProperty("my.prop", "value");
        assertEquals("Bad substitution", "value",
                     ArgumentParser.eval(getResolver(), "${my.prop}",
                                         String.class));
    }

    public void testEncodeDecodeArguments() {
        String[] args = {
            "one", "[two, two+]", "three%2cthree", "four,four"
        };
        String encoded = ArgumentParser.encodeArguments(args);
        assertEquals("Wrong encoding",
                     "one,[two, two+],three%%2Cthree,four%2cfour", encoded);
        String[] decoded = ArgumentParser.parseArgumentList(encoded);
        for (int i=0;i < args.length;i++) {
            assertEquals("Wrong decode arg " + i, args[i], decoded[i]);
        }
    }

    public void testReplace() {
        String s1 = "A string with some stuff to replace";
        String s2 = "A XXring with some XXuff to replace";
        assertEquals("Bad replacement", s2,
                     ArgumentParser.replace(s1, "st", "XX"));
    }

    public void testLoadParser() {
        Parser cvt = ArgumentParser.getParser(java.io.File.class);
        assertEquals("Wrong class loaded",
                     abbot.script.parsers.FileParser.class, cvt.getClass());
    }

    public void testEvalInteger() throws Throwable {
        abbot.script.Resolver resolver = getResolver();
        assertEquals("Can't convert String to integer", 
                     new Integer(1),
                     ArgumentParser.eval(resolver, "1", int.class));
        assertEquals("Can't convert String to Integer", 
                     new Integer(1),
                     ArgumentParser.eval(resolver, "1", Integer.class));
    }

    public void testToStringArray() throws Exception {
        Object[][] samples = {
            { new String[] { "one", "two", "three", "four" }, 
              "[one,two,three,four]" },
            { new int[] { 1, 2, 3, 4 },
              "[1,2,3,4]" }
        };
        for (int i=0;i < samples.length;i++) {
            Object array = samples[i][0];
            assertEquals("Wrong array conversion sample " + i,
                         samples[i][1],
                         ArgumentParser.toString(array));
        }
    }

    public void testToString() {
        Object o = new Object();
        Object o2 = new Object() {
            public String toString() {
                return null;
            }
        };
        assertEquals("should be encoded default",
                     ArgumentParser.DEFAULT_TOSTRING,
                     ArgumentParser.toString(o));
        assertEquals("should be encoded null",
                     ArgumentParser.NULL,
                     ArgumentParser.toString(o2));
    }

    public void testIsDefaultToString() {
        assertFalse("null is not a default toString",
                    ArgumentParser.isDefaultToString(null));
    }

    /** Create a new test case with the given name. */
    public ArgumentParserTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        TestHelper.runTests(args, ArgumentParserTest.class);
    }
}
