package org.webharvest.utils;

import static org.testng.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.testng.annotations.Test;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Residual CommonUtil contracts for file IO edges and query-string fullUrl shaping.
 */
public class CommonUtilIoAndUrlResidualBranchContractTest {

    @Test(expectedExceptions = IOException.class)
    public void readStringFromFileRejectsMissingFile() throws Exception {
        CommonUtil.readStringFromFile(new File("definitely-missing-webharvest-fixture.bin"), "UTF-8");
    }

    @Test
    public void readStringFromFileReturnsEmptyStringForEmptyFile() throws Exception {
        File empty = Files.createTempFile("wh-empty-", ".txt").toFile();
        try {
            assertEquals(CommonUtil.readStringFromFile(empty, "UTF-8"), "");
        } finally {
            empty.delete();
        }
    }

    @Test
    public void readStringFromFileLoadsUtf8Content() throws Exception {
        File file = Files.createTempFile("wh-content-", ".txt").toFile();
        try {
            Files.write(file.toPath(), "hello-webharvest".getBytes(StandardCharsets.UTF_8));
            assertEquals(CommonUtil.readStringFromFile(file, "UTF-8"), "hello-webharvest");
        } finally {
            file.delete();
        }
    }

    @Test
    public void readBytesFromFileReadsExactContents() throws Exception {
        File file = Files.createTempFile("wh-bytes-", ".bin").toFile();
        try {
            byte[] payload = new byte[] {1, 2, 3, 9};
            Files.write(file.toPath(), payload);
            assertEquals(CommonUtil.readBytesFromFile(file), payload);
        } finally {
            file.delete();
        }
    }

    @Test
    public void fullUrlAppendsQueryWhenBaseHasNoQuery() {
        assertEquals(CommonUtil.fullUrl("http://example.com/path", "?a=1"),
                "http://example.com/path?a=1");
    }

    @Test
    public void fullUrlReplacesTrailingQuestionMarkWhenAppendingQuery() {
        assertEquals(CommonUtil.fullUrl("http://example.com/path?", "?a=1"),
                "http://example.com/path?a=1");
    }

    @Test
    public void fullUrlMergesQueryWhenBaseAlreadyHasParameters() {
        assertEquals(CommonUtil.fullUrl("http://example.com/path?a=1", "?b=2"),
                "http://example.com/path?a=1&b=2");
    }

    @Test
    public void createVariablePassesThroughNonEmptyVariableInstances() {
        Variable original = new NodeVariable("keep-me");
        assertSame(CommonUtil.createVariable(original), original);
        assertSame(CommonUtil.createVariable(EmptyVariable.INSTANCE), EmptyVariable.INSTANCE);
        assertTrue(CommonUtil.createVariable("").isEmpty());
    }

    @Test
    public void normalizeViaFullUrlCollapsesParentSegmentsToRootSlash() {
        String result = CommonUtil.fullUrl("http://example.com/a/b/", "../../");
        assertEquals(result, "http://example.com/");
    }
}
