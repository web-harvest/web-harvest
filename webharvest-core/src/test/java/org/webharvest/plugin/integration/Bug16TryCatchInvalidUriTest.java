package org.webharvest.plugin.integration;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.*;

/**
 * Regression test for Bug #16: <try> cannot catch exception raised by invalid URI in <http>
 * 
 * **Status**: ✅ VERIFIED FIXED (Already working in v2.2.1)
 * 
 * **Original Issue** (from user report):
 * When an invalid URI is passed to <http>, the resulting IllegalArgumentException
 * stopped the scraper even if surrounded by a <try> block.
 * 
 * **Example** (from user):
 * <try>
 *   <body>
 *     <http url="http://web-harvest.sf.net/ê" />
 *   </body>
 *   <catch/>
 * </try>
 * 
 * **Expected**: Exception should be caught by <try>  
 * **Actual (original report)**: Scraper stopped with IllegalArgumentException  
 * **Current behavior**: ✅ Exception IS caught by <try> (FIXED)
 * 
 * **Why it's fixed**:
 * 1. HttpPlugin.doExecute() catches ALL exceptions and wraps in IllegalStateException (line 153)
 * 2. TryPlugin.doExecute() catches ALL Exception types including IllegalStateException (line 381)
 * 3. Proper exception chain: IllegalArgumentException → IllegalStateException → caught by <try>
 * 
 * **This test verifies**: Exception handling works correctly (regression test)
 */
public class Bug16TryCatchInvalidUriTest extends XmlBasedIntegrationTestBase {

    /**
     * Regression test for Bug #16: Invalid URI in <http> IS caught by <try>
     * 
     * Tests multiple invalid URI patterns to ensure <try> catches all exceptions:
     * - Space in URL
     * - Non-ASCII characters
     * - Malformed URIs
     */
    @Test(groups = {"integration", "bugs", "regression"})
    public void testBug16_TryCatchInvalidUri_VerifyFixed() throws Exception {
        System.out.println("\n### Bug #16 Regression: Try/Catch with invalid URI ###");
        
        String xmlPath = "integration-tests/bug16_try_catch_invalid_uri.xml";
        
        // Execute configuration with invalid URI (space in URL)
        executeXmlConfig(xmlPath);
        
        // If we reach here without exception, check results
        boolean caughtFileExists = Files.exists(Paths.get("target/test-output/bug16_caught.txt"));
        boolean successFileExists = Files.exists(Paths.get("target/test-output/bug16_success.txt"));
        
        System.out.println("  Caught file exists: " + caughtFileExists);
        System.out.println("  Success file exists: " + successFileExists);
        
        // Expected: Both files should exist (catch executed, then continued)
        assertTrue(successFileExists, 
            "Success file should exist - execution should continue after try/catch");
        
        assertTrue(caughtFileExists,
            "Catch file should exist - <try> should catch invalid URI exception");
        
        // Verify catch block was executed with error info
        String caughtContent = new String(Files.readAllBytes(
            Paths.get("target/test-output/bug16_caught.txt")));
        System.out.println("  Caught content: " + caughtContent);
        assertTrue(caughtContent.contains("Exception"), 
            "Catch block should contain error info");
        assertTrue(caughtContent.contains("IllegalStateException"), 
            "Should be IllegalStateException from HttpPlugin");
        
        System.out.println("✅ Bug #16 Regression: Exception properly caught by <try> (VERIFIED FIXED)");
    }

}

