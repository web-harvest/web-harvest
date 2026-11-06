package org.webharvest.plugin.core.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * TDD Integration Test for HttpPlugin.
 * 
 * PROBLEM: HttpPlugin returns EMPTY content in integration tests!
 * Unit tests PASS (HttpService works, HttpPlugin works with DI).
 * 
 * This test uses XmlBasedIntegrationTestBase.executeXmlString()
 * to verify if problem is in integration environment.
 * 
 * Expected: If this FAILS, problem is in integration test setup
 * Expected: If this PASS, problem is in HttpPluginBasicIntegrationTest
 */
public class HttpPluginSimpleIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testHttpPluginDirectCall() throws Exception {
        System.out.println("=== TEST: HttpPlugin direct call via XML ===");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <http url=\"https://httpbin.org/json\"/>\n" +
            "</config>";
        
        System.out.println("XML: " + xml);
        
        DynamicScopeContext context = executeXmlString(xml);
        
        System.out.println("Context variables:");
        int count = 0;
        for (org.webharvest.utils.KeyValuePair<Variable> pair : context) {
            System.out.println("  " + pair.getKey() + " = " + pair.getValue());
            count++;
        }
        System.out.println("Total variables: " + count);
        
        // HttpPlugin should return result, but where is it stored?
        // Try to find it in context or as configResult
        Variable configResult = context.getVar("configResult");
        System.out.println("configResult = " + configResult);
        
        if (configResult != null) {
            String content = configResult.toString();
            System.out.println("configResult content length: " + content.length());
            System.out.println("configResult content: [" + content + "]");
            
            assertNotNull(content, "HTTP response should exist in configResult");
            assertTrue(content.length() > 0, 
                "HTTP response should not be empty, but got: [" + content + "]");
        } else {
            fail("configResult is null - where is HttpPlugin result?");
        }
    }
    
    @Test
    public void testHttpPluginViaDefVar() throws Exception {
        System.out.println("=== TEST: HttpPlugin via <def var> ===");
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"https://httpbin.org/json\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        System.out.println("XML: " + xml);
        
        DynamicScopeContext context = executeXmlString(xml);
        
        System.out.println("Context variables:");
        for (org.webharvest.utils.KeyValuePair<Variable> pair : context) {
            System.out.println("  " + pair.getKey() + " = " + pair.getValue());
        }
        
        Variable response = context.getVar("response");
        System.out.println("response variable = " + response);
        
        assertNotNull(response, "HTTP response should be stored in 'response' variable");
        
        String content = response.toString();
        System.out.println("response content length: " + content.length());
        System.out.println("response content: [" + content + "]");
        
        assertTrue(content.length() > 0, 
            "HTTP response should not be empty, but got: [" + content + "]");
        assertTrue(content.contains("\"") || content.contains("{"), 
            "Response should contain JSON content from httpbin.org/get");
    }
}

