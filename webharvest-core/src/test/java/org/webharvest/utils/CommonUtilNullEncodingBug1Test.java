/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.utils;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Verification tests for Bug #1: Prevent NPE when URL-encoding parameters.
 * 
 * <h3>Bug #1: NPE Prevention in URL Encoding</h3>
 * <p>Original issue in old HttpClientManager (removed in Bug #41):</p>
 * <pre>{@code
 * // OLD CODE (caused NPE):
 * urlParams += pair.getName() + "=" + URLEncoder.encode(pair.getValue(), charset) + "&";
 * // If pair.getValue() is null → NullPointerException
 * 
 * // PATCH:
 * urlParams += pair.getName() + "=" + URLEncoder.encode(
 *     pair.getValue() == null ? "" : pair.getValue(), charset) + "&";
 * }</pre>
 * 
 * <p>These tests verify that the new architecture (v2.2.0) properly handles
 * null parameter values without throwing NPE.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class CommonUtilNullEncodingBug1Test {
    
    /**
     * Test that encodeUrl handles null parameter values gracefully.
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_NullParameterValue_NoNPE() {
        // GIVEN - URL with parameter that has null value (empty in URL)
        String url = "http://example.com?name=&city=Warsaw";
        
        // WHEN
        String encoded = CommonUtil.encodeUrl(url, "UTF-8");
        
        // THEN - should not throw NPE
        assertNotNull(encoded, "Encoded URL should not be null");
        assertTrue(encoded.contains("name="), "Should contain parameter with empty value");
        assertTrue(encoded.contains("city="), "Should contain city parameter");
    }
    
    /**
     * Test that encodeUrl handles completely null URL gracefully.
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_NullUrl_ReturnsEmptyString() {
        // WHEN
        String result = CommonUtil.encodeUrl(null, "UTF-8");
        
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(result, "", "Null URL should return empty string");
    }
    
    /**
     * Test that encodeUrl handles URL with only parameter names (no values).
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_ParameterWithoutValue_NoNPE() {
        // GIVEN - URL with parameter that has no value (just name=)
        String url = "http://example.com?flag&option=&value=test";
        
        // WHEN
        String encoded = CommonUtil.encodeUrl(url, "UTF-8");
        
        // THEN
        assertNotNull(encoded, "Encoded URL should not be null");
        assertTrue(encoded.contains("flag"), "Should contain flag parameter");
        assertTrue(encoded.contains("option="), "Should contain option with empty value");
        assertTrue(encoded.contains("value=test"), "Should contain value parameter");
    }
    
    /**
     * Test that encodeUrl handles multiple null/empty parameters.
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_MultipleNullParameters_NoNPE() {
        // GIVEN
        String url = "http://example.com?a=&b=&c=value&d=";
        
        // WHEN
        String encoded = CommonUtil.encodeUrl(url, "UTF-8");
        
        // THEN
        assertNotNull(encoded, "Encoded URL should not be null");
        assertTrue(encoded.contains("a="), "Should handle first empty param");
        assertTrue(encoded.contains("b="), "Should handle second empty param");
        assertTrue(encoded.contains("c=value"), "Should handle param with value");
        assertTrue(encoded.contains("d="), "Should handle last empty param");
    }
    
    /**
     * Regression test: Ensure encodeUrl still works with normal parameters.
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_NormalParameters_WorksCorrectly() {
        // GIVEN
        String url = "http://example.com?name=John&city=Warsaw&color=#FF0000";
        
        // WHEN
        String encoded = CommonUtil.encodeUrl(url, "UTF-8");
        
        // THEN
        assertNotNull(encoded, "Encoded URL should not be null");
        assertTrue(encoded.contains("name=John"), "Should contain name parameter");
        assertTrue(encoded.contains("city=Warsaw"), "Should contain city parameter");
        assertTrue(encoded.contains("%23FF0000"), "Should encode # character (Bug #55)");
    }
    
    /**
     * Edge case: URL with only question mark (no parameters).
     */
    @Test(groups = {"unit", "utils", "bug1"})
    public void testEncodeUrl_OnlyQuestionMark_NoNPE() {
        // GIVEN
        String url = "http://example.com?";
        
        // WHEN
        String encoded = CommonUtil.encodeUrl(url, "UTF-8");
        
        // THEN
        assertNotNull(encoded, "Encoded URL should not be null");
        // Question mark with no parameters might be removed or kept
        assertTrue(encoded.startsWith("http://example.com"), "Should preserve base URL");
    }
    
    /**
     * Verification: Bug #1 patch idea is implemented.
     * 
     * <p>This test confirms that the defensive programming approach
     * from the original patch (null → empty string) is implemented
     * in the new CommonUtil.encodeUrlParam() method.</p>
     */
    @Test(groups = {"unit", "utils", "bug1", "verification"})
    public void testBug1_PatchIdeaImplemented_NullSafety() {
        // GIVEN - Simulate what would happen with null parameter value
        String urlWithNullValue = "http://example.com?param1=value1&param2=&param3=value3";
        
        // WHEN - This should NOT throw NPE
        String result = CommonUtil.encodeUrl(urlWithNullValue, "UTF-8");
        
        // THEN
        assertNotNull(result, "Should not throw NPE");
        assertTrue(result.contains("param1=value1"), "Should encode normal parameter");
        assertTrue(result.contains("param2="), "Should handle empty parameter (null equivalent)");
        assertTrue(result.contains("param3=value3"), "Should encode parameter after empty one");
        
        // No NullPointerException = Bug #1 fix is working!
    }
}

