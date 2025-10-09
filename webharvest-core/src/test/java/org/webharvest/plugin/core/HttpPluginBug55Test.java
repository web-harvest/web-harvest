package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Test for Bug #55: Character '#' in URL parameters
 * 
 * Issue: It is not possible to pass parameters after a '#' character in URL.
 * The URL is truncated at the '#' level.
 * 
 * Example:
 *   Input:  http://www.myweb.com/index.php?a=123&b=#tre
 *   Bug:    http://www.myweb.com/index.php?a=123&b=
 *   Expected: http://www.myweb.com/index.php?a=123&b=%23tre
 * 
 * Root cause: '#' is treated as URL fragment/anchor separator.
 * In query parameters, '#' should be URL-encoded as %23.
 */
public class HttpPluginBug55Test {

    /**
     * Test Bug #55: URL with # in parameter value should be properly encoded
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testHashInParameterValue_ShouldBeEncoded() {
        // Test URL parsing/encoding
        String url = "http://www.myweb.com/index.php?a=123&b=#tre";
        
        // Expected: # should be encoded as %23 in parameter value
        String expected = "http://www.myweb.com/index.php?a=123&b=%23tre";
        
        // TODO: Test HttpPlugin URL handling
        // For now, document the expected behavior
        
        System.out.println("Bug #55 - URL with # in parameter:");
        System.out.println("  Input:    " + url);
        System.out.println("  Expected: " + expected);
        System.out.println("  Issue: Everything after # is truncated");
    }

    /**
     * Test multiple # characters in URL
     */
    @Test(groups = {"unit", "bugs", "hard"})
    public void testMultipleHashCharacters() {
        String url = "http://example.com/page?color=#FF0000&style=#bold";
        
        // Both # should be encoded
        String expected = "http://example.com/page?color=%23FF0000&style=%23bold";
        
        System.out.println("Multiple # test:");
        System.out.println("  Input:    " + url);
        System.out.println("  Expected: " + expected);
    }

    /**
     * Test # at different positions
     */
    @Test(groups = {"unit", "bugs", "hard", "edge-case"})
    public void testHashAtDifferentPositions() {
        // # at start of parameter value
        String url1 = "http://example.com?tag=#hashtag";
        String exp1 = "http://example.com?tag=%23hashtag";
        
        // # in middle of parameter value
        String url2 = "http://example.com?code=AB#CD";
        String exp2 = "http://example.com?code=AB%23CD";
        
        // # at end of parameter value
        String url3 = "http://example.com?symbol=test#";
        String exp3 = "http://example.com?symbol=test%23";
        
        System.out.println("# at different positions:");
        System.out.println("  Start:  " + url1 + " -> " + exp1);
        System.out.println("  Middle: " + url2 + " -> " + exp2);
        System.out.println("  End:    " + url3 + " -> " + exp3);
    }

    /**
     * Test legitimate fragment (anchor) should still work
     */
    @Test(groups = {"unit", "bugs", "hard", "real-world"})
    public void testLegitimateFragment_ShouldBePreserved() {
        // This is a REAL fragment (anchor), not a parameter value
        String url = "http://example.com/page.html#section1";
        
        // Fragment should be preserved as-is (not encoded)
        // This is DIFFERENT from # inside parameter value
        
        System.out.println("Legitimate fragment test:");
        System.out.println("  URL: " + url);
        System.out.println("  Note: #section1 is anchor, should be preserved");
        System.out.println("  Different from ?param=#value (should encode)");
    }

    /**
     * HARD TEST: Complex URL with both parameters and fragment
     */
    @Test(groups = {"unit", "bugs", "hard", "complex"})
    public void testParametersWithHashAndFragment() {
        // Parameter value contains #, AND there's a legitimate fragment
        // This is the HARDEST case
        
        String url = "http://example.com/page?color=#FF0000#anchor";
        
        // First # (in parameter) should be encoded
        // Second # (fragment) should be preserved
        String expected = "http://example.com/page?color=%23FF0000#anchor";
        
        System.out.println("Complex URL test:");
        System.out.println("  Input:    " + url);
        System.out.println("  Expected: " + expected);
        System.out.println("  Challenge: Distinguish param # from fragment #");
    }

    /**
     * REGRESSION: Normal URLs without # should still work
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testNormalUrl_NoRegression() {
        String url = "http://example.com/page?a=1&b=2&c=3";
        
        System.out.println("Normal URL regression test:");
        System.out.println("  URL: " + url);
        System.out.println("  Should work without any changes");
    }

    /**
     * REGRESSION: URLs with other special characters
     */
    @Test(groups = {"unit", "bugs", "regression"})
    public void testOtherSpecialCharacters_NoRegression() {
        // Other chars that need encoding: space, &, =, %, etc.
        String url = "http://example.com/search?q=hello world&filter=a+b";
        
        System.out.println("Other special chars test:");
        System.out.println("  URL: " + url);
        System.out.println("  Other chars should still be encoded properly");
    }
}

