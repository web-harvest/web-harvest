package org.webharvest.utils;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/**
 * URL resolution and path normalization branches in {@link CommonUtil#fullUrl}.
 */
public class CommonUtilFullUrlBranchContractTest {

    @Test
    public void fullUrlNormalizesParentDirectorySegments() {
        assertEquals(
                CommonUtil.fullUrl("http://example.com/foo/bar/page.html", "../sibling.txt"),
                "http://example.com/foo/sibling.txt");
        assertEquals(
                CommonUtil.fullUrl("http://example.com/a/b/c", "../../d.txt"),
                "http://example.com/d.txt");
    }

    @Test
    public void fullUrlAppendsQueryWhenPageHasOrLacksQuestionMark() {
        assertEquals(
                CommonUtil.fullUrl("http://example.com/page", "?q=1"),
                "http://example.com/page?q=1");
        assertEquals(
                CommonUtil.fullUrl("http://example.com/page?", "?q=1"),
                "http://example.com/page?q=1");
        assertEquals(
                CommonUtil.fullUrl("http://example.com/page?x=1", "?y=2"),
                "http://example.com/page?x=1&y=2");
    }

    @Test
    public void fullUrlPrefixesSchemeForRelativeHosts() {
        assertEquals(
                CommonUtil.fullUrl("example.com/dir/", "file.txt"),
                "http://example.com/dir/file.txt");
        assertEquals(
                CommonUtil.fullUrl("example.com", "/absolute.txt"),
                "http://example.com/absolute.txt");
    }

    @Test
    public void normalizePathSkipsWhenNoTraversalTokens() {
        assertEquals(
                CommonUtil.fullUrl("http://host/base/", "plain.txt"),
                "http://host/base/plain.txt");
    }
}
