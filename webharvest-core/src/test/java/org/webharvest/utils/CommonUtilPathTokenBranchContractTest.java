package org.webharvest.utils;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;

/**
 * Path helpers, tokenizers, identifiers, and IntPair parsing branches on {@link CommonUtil}.
 */
public class CommonUtilPathTokenBranchContractTest {

    @Test
    public void pathHelpersDistinguishAbsoluteAndRelativePaths() {
        assertFalse(CommonUtil.isPathAbsolute(null));
        assertTrue(CommonUtil.isPathAbsolute("/tmp/data"));
        assertTrue(CommonUtil.isPathAbsolute("C:/work/file.xml"));
        assertFalse(CommonUtil.isPathAbsolute("relative/dir"));

        assertEquals(CommonUtil.adaptFilename("a\\b"), "a/b");
        assertEquals(CommonUtil.getAbsoluteFilename("/work", "child.txt"), "/work/child.txt");
        assertEquals(CommonUtil.getAbsoluteFilename("/work/", "child.txt"), "/work/child.txt");
        assertEquals(CommonUtil.getDirectoryFromPath("/work/sub/file.txt"), "/work/sub");
        assertEquals(CommonUtil.getFileFromPath("/work/sub/file.txt"), "file.txt");
        assertEquals(CommonUtil.getFileFromPath("C:\\dir\\file.txt"), "file.txt");
    }

    @Test
    public void xmlIdentifierValidationRejectsLeadingDigitsAndHyphenEdges() {
        assertTrue(CommonUtil.isValidXmlIdentifier("valid-name"));
        assertFalse(CommonUtil.isValidXmlIdentifier(""));
        assertFalse(CommonUtil.isValidXmlIdentifier("1bad"));
        assertFalse(CommonUtil.isValidXmlIdentifier("bad@name"));
    }

    @Test
    public void intPairDefineFromStringCoversSeparatorBranches() {
        CommonUtil.IntPair pair = new CommonUtil.IntPair();
        pair.defineFromString("5", ':', 99);
        assertEquals(pair.x, 5);
        assertEquals(pair.y, 5);

        pair.defineFromString(":7", ':', 20);
        assertEquals(pair.x, 1);
        assertEquals(pair.y, 7);

        pair.defineFromString("3:", ':', 20);
        assertEquals(pair.x, 3);
        assertEquals(pair.y, 20);

        pair.defineFromString("2:9", ':', 20);
        assertEquals(pair.x, 2);
        assertEquals(pair.y, 9);
    }

    @Test
    public void tokenizeHonorsTrimAndEmptyTokenFlags() {
        assertEquals(CommonUtil.tokenize(null, ",").length, 0);
        assertEquals(CommonUtil.tokenize("a,b,c", ","), new String[] {"a", "b", "c"});

        String[] extended = CommonUtil.tokenize("a,,b", ",", true, true);
        assertEquals(extended.length, 3);
        assertEquals(extended[0], "a");
        assertEquals(extended[1], "");
        assertEquals(extended[2], "b");
    }

    @Test
    public void getValidIdentifierReplacesIllegalLeadingCharacters() {
        assertEquals(CommonUtil.getValidIdentifier(null), "_");
        assertEquals(CommonUtil.getValidIdentifier("9start"), "_start");
        assertEquals(CommonUtil.getValidIdentifier("ok_name"), "ok_name");
    }

    @Test
    public void fileIoHelpersRoundTripText() throws IOException {
        File file = File.createTempFile("wh-common-io-", ".txt");
        try {
            CommonUtil.saveStringToFile(file, "persisted", StandardCharsets.UTF_8.name());
            assertEquals(CommonUtil.readStringFromFile(file, StandardCharsets.UTF_8.name()), "persisted");
            assertTrue(CommonUtil.readBytesFromFile(file).length > 0);
        } finally {
            assertTrue(file.delete());
        }
    }

    @Test
    public void existsInStringArrayMatchesIgnoreCaseEvenWhenSensitiveFlagSet() {
        String[] values = {"Alpha", "beta"};
        assertTrue(CommonUtil.existsInStringArray(values, "alpha", false));
        assertTrue(CommonUtil.existsInStringArray(values, "alpha", true));
        assertFalse(CommonUtil.existsInStringArray(values, "gamma", true));
        assertFalse(CommonUtil.existsInStringArray(null, "x", true));
    }

    @Test
    public void countCharsRespectsBounds() {
        assertEquals(CommonUtil.countChars("ababa", 'a', 0, 4), 3);
        assertEquals(CommonUtil.countChars("ababa", 'a', -5, 100), 3);
    }

    @Test
    public void nvlAndEmptyStringHelpers() {
        assertEquals(CommonUtil.nvl(null, "default"), "default");
        assertTrue(CommonUtil.isEmptyString("   "));
        assertTrue(CommonUtil.isEmpty(null));
    }

    @Test
    public void isBooleanTrueAcceptsAffirmativeStrings() {
        assertTrue(CommonUtil.isBooleanTrue("yes"));
        assertTrue(CommonUtil.isBooleanTrue("TRUE"));
        assertFalse(CommonUtil.isBooleanTrue("maybe"));
    }

    @Test
    public void getIntAndDoubleValueParseOrDefault() {
        assertEquals(CommonUtil.getIntValue("42", 0), 42);
        assertEquals(CommonUtil.getIntValue("nope", 7), 7);
        assertEquals(CommonUtil.getDoubleValue("3.5", 1.0), 3.5, 0.0001);
        assertEquals(CommonUtil.getDoubleValue("", 2.0), 2.0, 0.0001);
    }

    @Test
    public void createVariableWrapsScalarsAndPreservesNodePayload() {
        org.webharvest.runtime.variables.Variable node = CommonUtil.createVariable(new NodeVariable("payload"));
        assertEquals(node.toString(), "payload");
        org.webharvest.runtime.variables.Variable scalar = CommonUtil.createVariable(99);
        assertEquals(scalar.toString(), "99");
        assertSame(CommonUtil.createVariable(EmptyVariable.INSTANCE), EmptyVariable.INSTANCE);
    }
}
