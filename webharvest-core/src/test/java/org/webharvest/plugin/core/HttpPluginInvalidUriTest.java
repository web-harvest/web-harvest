package org.webharvest.plugin.core;

import org.apache.commons.httpclient.methods.GetMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Test to identify which URI patterns actually throw IllegalArgumentException
 * in HttpClient's GetMethod/PostMethod constructors.
 * 
 * This helps understand Bug #16 - when exactly does <try> fail to catch exceptions.
 */
public class HttpPluginInvalidUriTest {

    /**
     * Test various invalid URI patterns to see which throw IllegalArgumentException
     */
    @Test(groups = {"unit", "bugs"})
    public void testInvalidUriPatterns() {
        System.out.println("\n### Testing which URI patterns throw IllegalArgumentException ###\n");
        
        // Test 1: Non-ASCII character (user's example)
        testUri("http://web-harvest.sf.net/ê", "Non-ASCII char");
        
        // Test 2: Space in URL
        testUri("http://example.com/path with spaces", "Space in path");
        
        // Test 3: Invalid characters
        testUri("http://example.com/<invalid>", "Angle brackets");
        testUri("http://example.com/|pipe", "Pipe character");
        testUri("http://example.com/\\backslash", "Backslash");
        
        // Test 4: Invalid protocol
        testUri("htp://example.com", "Invalid protocol");
        
        // Test 5: Missing protocol
        testUri("example.com", "No protocol");
        
        // Test 6: Invalid format
        testUri("http://", "Protocol only");
        testUri("http:// ", "Protocol + space");
        testUri("http://[invalid", "Malformed");
        
        // Test 7: Control characters
        testUri("http://example.com/path\nwith\nnewlines", "Newlines");
        testUri("http://example.com/path\twith\ttabs", "Tabs");
    }
    
    private void testUri(String url, String description) {
        try {
            GetMethod method = new GetMethod(url);
            System.out.println("✅ " + description + ": NO exception");
            System.out.println("   URL: " + url);
            System.out.println("   GetMethod created successfully");
            method.releaseConnection();
        } catch (IllegalArgumentException e) {
            System.out.println("❌ " + description + ": IllegalArgumentException!");
            System.out.println("   URL: " + url);
            System.out.println("   Exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("⚠️  " + description + ": Other exception");
            System.out.println("   URL: " + url);
            System.out.println("   Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println();
    }
}

