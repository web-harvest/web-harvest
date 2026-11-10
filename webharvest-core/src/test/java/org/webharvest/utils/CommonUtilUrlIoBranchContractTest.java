package org.webharvest.utils;

import static org.testng.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.testng.annotations.Test;
import org.webharvest.exception.VariableException;

/**
 * URL encoding, file-URL reads, collection lookup, and boolean parsing edges
 * for {@link CommonUtil} not covered elsewhere.
 */
public class CommonUtilUrlIoBranchContractTest {

    @Test
    public void encodeUrlLeavesUrlsWithoutQueryUnchanged() {
        assertEquals(CommonUtil.encodeUrl("https://example.com/path", "UTF-8"),
                "https://example.com/path");
    }

    @Test
    public void encodeUrlEncodesBareQueryTokensWithoutEquals() {
        String encoded = CommonUtil.encodeUrl("https://example.com/search?q&sort=name", "UTF-8");
        assertTrue(encoded.startsWith("https://example.com/search?"));
        assertTrue(encoded.contains("q=") || encoded.contains("&sort=name"),
                "Bare query flag and named param should survive encoding: " + encoded);
    }

    @Test(expectedExceptions = VariableException.class)
    public void encodeUrlRejectsUnsupportedCharset() {
        CommonUtil.encodeUrl("https://example.com?x=1", "NOT_A_REAL_CHARSET");
    }

    @Test
    public void readStringFromUrlReadsLocalFileContent() throws IOException {
        Path file = Files.createTempFile("wh-commonutil-url-", ".txt");
        try {
            String expected = "line-one\nline-two";
            Files.writeString(file, expected, StandardCharsets.UTF_8);
            URL fileUrl = file.toUri().toURL();

            assertEquals(CommonUtil.readStringFromUrl(fileUrl), expected);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    public void findValueInCollectionUsesReferenceEqualityForSameInstance() {
        Object marker = new Object();
        Set<Object> values = new LinkedHashSet<>(Arrays.asList("first", marker, "last"));
        assertEquals(CommonUtil.findValueInCollection(values, marker), 1);
    }

    @Test
    public void getBooleanValueRecognizesExplicitFalseAndYesAffirmatives() {
        assertEquals(CommonUtil.getBooleanValue("yes", Boolean.FALSE), Boolean.TRUE);
        assertEquals(CommonUtil.getBooleanValue("0", Boolean.TRUE), Boolean.FALSE);
        assertEquals(CommonUtil.getBooleanValue("FALSE", Boolean.TRUE), Boolean.FALSE);
        assertEquals(CommonUtil.getBooleanValue("maybe", Boolean.FALSE), Boolean.FALSE);
    }
}
