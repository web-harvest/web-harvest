package org.webharvest.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test for Bug #7: CommonUtils.fullUrl: ".." is not handled correctly
 * 
 * Issue: fullUrl() doesn't normalize parent directory references ("..").
 * Example: 
 *   pageUrl = "http://www.example.com/foo/"
 *   link = "../bar.htm"
 *   Result: "http://www.example.com/foo/../bar.htm" (wrong!)
 *   Expected: "http://www.example.com/bar.htm" (normalized)
 * 
 * Problem: IIS servers don't like ".." in URLs and close connections.
 * Browsers normalize URLs automatically, but Web-Harvest didn't.
 * 
 * Fix: Normalize path by resolving ".." segments.
 */
public class CommonUtilBug7Test {

    /**
     * Test Bug #7: User's exact example - "../" should be normalized
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testBug7_ParentDirectoryReference_Normalized() {
        System.out.println("\n### Bug #7: Parent directory normalization ###");
        
        String pageUrl = "http://www.example.com/foo/";
        String link = "../bar.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        System.out.println("  Page URL: " + pageUrl);
        System.out.println("  Link: " + link);
        System.out.println("  Result: " + result);
        
        // Should normalize to remove "../"
        assertEquals(result, "http://www.example.com/bar.htm",
            "Should normalize ../ to parent directory");
        
        // Should NOT contain ".."
        assertFalse(result.contains(".."),
            "Normalized URL should not contain ..");
        
        System.out.println("✅ Bug #7: ../ normalized correctly");
    }

    /**
     * Test multiple levels of parent directory references
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testMultipleParentReferences() {
        System.out.println("\n### Bug #7: Multiple ../ references ###");
        
        // From /foo/bar/baz/ go up two levels
        String pageUrl = "http://www.example.com/foo/bar/baz/";
        String link = "../../file.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        System.out.println("  Page: " + pageUrl);
        System.out.println("  Link: " + link);
        System.out.println("  Result: " + result);
        
        assertEquals(result, "http://www.example.com/foo/file.htm",
            "Should go up two directory levels");
        assertFalse(result.contains(".."));
        
        System.out.println("✅ Multiple ../ handled");
    }

    /**
     * Test "../" in middle of path
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testParentReference_InMiddleOfPath() {
        String pageUrl = "http://www.example.com/foo/bar/";
        String link = "baz/../file.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        System.out.println("  Page: " + pageUrl);
        System.out.println("  Link: " + link);
        System.out.println("  Result: " + result);
        
        // baz/../file.htm should become just file.htm
        assertEquals(result, "http://www.example.com/foo/bar/file.htm");
        assertFalse(result.contains(".."));
        
        System.out.println("✅ ../ in middle of path normalized");
    }

    /**
     * Edge case: Too many "../" going above root
     */
    @Test(groups = {"unit", "bugs", "edge-case"})
    public void testTooManyParentReferences_StopsAtRoot() {
        String pageUrl = "http://www.example.com/foo/";
        String link = "../../../bar.htm";  // Too many "../"
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        System.out.println("  Page: " + pageUrl);
        System.out.println("  Link: " + link);
        System.out.println("  Result: " + result);
        
        // Should not go above domain root
        assertTrue(result.startsWith("http://www.example.com/"),
            "Should not go above domain root");
        assertFalse(result.contains(".."));
        
        System.out.println("✅ Too many ../ handled gracefully");
    }

    /**
     * REGRESSION: Normal relative links should still work
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testNormalRelativeLink_NoRegression() {
        String pageUrl = "http://www.example.com/foo/";
        String link = "bar.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        assertEquals(result, "http://www.example.com/foo/bar.htm",
            "Normal relative links should still work");
        
        System.out.println("✅ Normal relative links work (no regression)");
    }

    /**
     * REGRESSION: Absolute links should still work
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testAbsoluteLink_NoRegression() {
        String pageUrl = "http://www.example.com/foo/";
        String link = "/absolute/path.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        assertEquals(result, "http://www.example.com/absolute/path.htm",
            "Absolute links should still work");
        
        System.out.println("✅ Absolute links work (no regression)");
    }

    /**
     * REGRESSION: Full URLs should be returned as-is
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testFullUrl_NoRegression() {
        String pageUrl = "http://www.example.com/foo/";
        String link = "http://other.com/file.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        assertEquals(result, "http://other.com/file.htm",
            "Full URLs should be returned as-is");
        
        System.out.println("✅ Full URLs work (no regression)");
    }

    /**
     * Edge case: Current directory reference "./" 
     */
    @Test(groups = {"unit", "bugs", "edge-case"})
    public void testCurrentDirectoryReference() {
        String pageUrl = "http://www.example.com/foo/";
        String link = "./bar.htm";
        
        String result = CommonUtil.fullUrl(pageUrl, link);
        
        // "./" should be removed (current directory)
        assertEquals(result, "http://www.example.com/foo/bar.htm");
        assertFalse(result.contains("./"));
        
        System.out.println("✅ ./ (current directory) normalized");
    }
}

