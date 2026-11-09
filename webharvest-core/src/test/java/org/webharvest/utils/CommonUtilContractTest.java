package org.webharvest.utils;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Public compatibility contracts for CommonUtil helpers.
 *
 * <p>These tests characterize existing behavior. They intentionally avoid
 * changing production semantics or depending on external network services.</p>
 */
public class CommonUtilContractTest {

    @Test
    public void xmlAndJavaIdentifierHelpersHaveExplicitBoundaries() {
        assertTrue(CommonUtil.isValidXmlIdentifier("node"));
        assertTrue(CommonUtil.isValidXmlIdentifier("node-1"));
        assertFalse(CommonUtil.isValidXmlIdentifier(null));
        assertFalse(CommonUtil.isValidXmlIdentifier(""));
        assertFalse(CommonUtil.isValidXmlIdentifier("1node"));
        assertFalse(CommonUtil.isValidXmlIdentifier("node.name"));

        assertEquals(CommonUtil.getValidIdentifier(null), "_");
        assertEquals(CommonUtil.getValidIdentifier(""), "_");
        assertEquals(CommonUtil.getValidIdentifier("1value"), "_value");
        assertEquals(CommonUtil.getValidIdentifier("value with spaces"), "value_with_spaces");
        assertEquals(CommonUtil.getValidIdentifier("valid_name"), "valid_name");
    }

    @Test
    public void pathHelpersDistinguishAbsoluteAndRelativePaths() {
        assertFalse(CommonUtil.isPathAbsolute(null));
        assertFalse(CommonUtil.isPathAbsolute("relative/file.xml"));
        assertTrue(CommonUtil.isPathAbsolute("/var/tmp/file.xml"));
        assertTrue(CommonUtil.isPathAbsolute("C:\\temp\\file.xml"));

        assertEquals(CommonUtil.getAbsoluteFilename("/workspace/", "config/file.xml"),
                "/workspace/config/file.xml");
        assertEquals(CommonUtil.getAbsoluteFilename("/workspace", "C:\\temp\\file.xml"),
                "C:/temp/file.xml");
        assertEquals(CommonUtil.getDirectoryFromPath("/var/tmp/file.xml"), "/var/tmp");
        assertEquals(CommonUtil.getFileFromPath("/var/tmp/file.xml"), "file.xml");
        assertEquals(CommonUtil.getFileFromPath("C:\\temp\\file.xml"), "file.xml");
    }

    @Test
    public void booleanAndDoubleParsingPreserveDefaults() {
        assertTrue(CommonUtil.isBooleanTrue("true"));
        assertTrue(CommonUtil.isBooleanTrue("YES"));
        assertTrue(CommonUtil.isBooleanTrue("1"));
        assertFalse(CommonUtil.isBooleanTrue("no"));
        assertFalse(CommonUtil.isBooleanTrue(null));

        assertEquals(CommonUtil.getBooleanValue("no", Boolean.TRUE), Boolean.FALSE);
        assertEquals(CommonUtil.getBooleanValue("unknown", Boolean.TRUE), Boolean.TRUE);
        assertEquals(CommonUtil.getDoubleValue("1.25e2", -1.0d), 125.0d, 0.000001d);
        assertEquals(CommonUtil.getDoubleValue("not-a-number", -1.0d), -1.0d, 0.000001d);
        assertEquals(CommonUtil.getDoubleValue(null, 7.5d), 7.5d, 0.000001d);
    }

    @Test
    public void fileHelpersRoundTripUtf8AndHandleEmptyOrMissingFiles() throws IOException {
        Path file = Files.createTempFile("webharvest-commonutil-", ".txt");
        try {
            String expected = "Zażółć gęślą jaźń — Pogórze";
            CommonUtil.saveStringToFile(file.toFile(), expected, StandardCharsets.UTF_8.name());

            assertEquals(CommonUtil.readStringFromFile(file.toFile(), StandardCharsets.UTF_8.name()), expected);
            assertEquals(new String(CommonUtil.readBytesFromFile(file.toFile()), StandardCharsets.UTF_8), expected);

            Files.write(file, new byte[0]);
            assertEquals(CommonUtil.readStringFromFile(file.toFile(), StandardCharsets.UTF_8.name()), "");
            assertEquals(CommonUtil.readBytesFromFile(file.toFile()).length, 0);
        } finally {
            Files.deleteIfExists(file);
        }

        Path missing = Files.createTempFile("webharvest-commonutil-missing-", ".txt");
        Files.deleteIfExists(missing);
        assertThrows(IOException.class,
                () -> CommonUtil.readStringFromFile(missing.toFile(), StandardCharsets.UTF_8.name()));
    }

    @Test
    public void tokenizerSupportsCharacterSetsTrimmingAndEmptyTokenPolicy() {
        assertEquals(CommonUtil.tokenize("alpha,beta;gamma", ",;"),
                new String[] {"alpha", "beta", "gamma"});
        assertEquals(CommonUtil.tokenize("alpha,,beta,", ",", false, false),
                new String[] {"alpha", "beta"});
        assertEquals(CommonUtil.tokenize(" one , ,two,", ",", true, true),
                new String[] {"one", "", "two", ""});
        assertEquals(CommonUtil.tokenize(null, ",").length, 0);
        assertEquals(CommonUtil.tokenize(null, ",", true, true).length, 0);
    }

    @Test
    public void characterCountingAndCollectionLookupRespectBoundsAndNulls() {
        assertEquals(CommonUtil.countChars("banana", 'a', 1, 4), 2);
        assertEquals(CommonUtil.countChars("banana", 'a', -5, 99), 3);
        assertEquals(CommonUtil.countChars("banana", 'z', 0, 5), 0);
        assertEquals(CommonUtil.countChars("banana", 'a', 4, 1), 0);

        List<String> values = new ArrayList<>(Arrays.asList("first", null, "last"));
        assertEquals(CommonUtil.findValueInCollection(values, "last"), 2);
        assertEquals(CommonUtil.findValueInCollection(values, null), 1);
        assertEquals(CommonUtil.findValueInCollection(values, "missing"), -1);
    }

    @Test
    public void stringArrayLookupSupportsCaseInsensitiveMode() {
        assertTrue(CommonUtil.existsInStringArray(new String[] {"alpha", "beta"}, "ALPHA", false));
        assertTrue(CommonUtil.existsInStringArray(new String[] {"alpha", "beta"}, "beta", true));
        assertFalse(CommonUtil.existsInStringArray(new String[] {"alpha", "beta"}, "gamma", false));
        assertFalse(CommonUtil.existsInStringArray(null, "alpha", false));
        assertFalse(CommonUtil.existsInStringArray(new String[] {"alpha"}, null, false));
    }
}
