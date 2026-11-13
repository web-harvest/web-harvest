package org.webharvest.utils;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * Residual {@link CommonUtil#fullUrl} / normalizePath edges: current-directory
 * segments, excess parent traversal, and hosts without a path.
 */
public class CommonUtilNormalizePathResidualBranchContractTest {

    @Test
    public void fullUrlResolvesCurrentDirectorySegments() {
        assertEquals(
                CommonUtil.fullUrl("http://example.com/a/b/", "././c.txt"),
                "http://example.com/a/b/c.txt");
    }

    @Test
    public void fullUrlIgnoresParentTraversalBeyondRoot() {
        assertEquals(
                CommonUtil.fullUrl("http://example.com/only/", "../../../../../x.txt"),
                "http://example.com/x.txt");
    }

    @Test
    public void fullUrlLeavesHostOnlyBaseWithRelativeLinkWithoutProtocolPathSplitIssues() {
        String result = CommonUtil.fullUrl("http://example.com", "page.html");
        assertTrue(result.startsWith("http://example.com/"), result);
        assertTrue(result.endsWith("page.html"), result);
    }

    @Test
    public void createVariableWrapsObjectArraysAndTreatsNullAsEmpty() {
        assertTrue(CommonUtil.createVariable(null).isEmpty());
        assertTrue(CommonUtil.createVariable(new Object[] {"a", "b"}).toString().contains("a"));
        assertEquals(CommonUtil.createVariable("plain-text").toString(), "plain-text");
    }
}
