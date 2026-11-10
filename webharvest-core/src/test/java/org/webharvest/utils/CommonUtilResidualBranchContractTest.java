package org.webharvest.utils;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.testng.annotations.Test;

/**
 * Residual CommonUtil contracts for absolute-path detection, URL path
 * normalization edge cases, and empty-file IO.
 */
public class CommonUtilResidualBranchContractTest {

    @Test
    public void isPathAbsoluteCoversDriveLetterAndRelativeEdges() {
        assertFalse(CommonUtil.isPathAbsolute(""));
        assertFalse(CommonUtil.isPathAbsolute("a"));
        assertFalse(CommonUtil.isPathAbsolute("1:not-drive"));
        assertTrue(CommonUtil.isPathAbsolute("\\windows\\path"));
        assertTrue(CommonUtil.isPathAbsolute("d:\\data\\file.txt"));
        assertFalse(CommonUtil.isPathAbsolute("rel\\nested"));
    }

    @Test
    public void fullUrlNormalizesParentAndCurrentSegments() {
        assertEquals(CommonUtil.fullUrl("http://example.com/a/b/", "./c.txt"),
                "http://example.com/a/b/c.txt");
        assertEquals(CommonUtil.fullUrl("http://example.com/a/b/", "../../"),
                "http://example.com/");
        assertEquals(CommonUtil.fullUrl("http://example.com", "no-slash"),
                "http://example.com/no-slash");
        assertEquals(CommonUtil.fullUrl("http://example.com/only", "../x"),
                "http://example.com/x");
    }

    @Test
    public void readStringFromEmptyFileReturnsEmptyString() throws Exception {
        File empty = Files.createTempFile("wh-empty", ".txt").toFile();
        try {
            assertEquals(CommonUtil.readStringFromFile(empty, StandardCharsets.UTF_8.name()), "");
        } finally {
            empty.delete();
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void readStringFromMissingFileFailsFast() throws Exception {
        CommonUtil.readStringFromFile(new File("definitely-missing-" + System.nanoTime() + ".txt"),
                StandardCharsets.UTF_8.name());
    }
}
