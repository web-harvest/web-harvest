package org.webharvest.plugin.core.integration.critical;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration test for HttpPlugin (element name: "http")
 * 
 * CRITICAL plugin - required for web scraping
 * 
 * Tests:
 * - Basic HTTP GET
 * - HTTP with URL attribute
 * - HTTP with variable interpolation
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class HttpPluginBasicIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testBasicHttpGet() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"content\">\n" +
            "        <http url=\"https://httpbin.org/html\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable content = context.getVar("content");
        
        assertNotNull(content, "HTTP response should be stored in variable");
        String responseText = content.toString();
        assertTrue(responseText.contains("<html") || responseText.contains("HTML"), 
                   "Response should contain HTML content");
    }
    
    @Test
    public void testHttpWithVariableUrl() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"url\">https://httpbin.org/get</def>\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable content = context.getVar("content");
        
        assertNotNull(content);
        assertTrue(content.toString().contains("<html") || content.toString().contains("HTML"));
    }
    
    @Test
    public void testHttpSimpleUrl() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"https://httpbin.org/status/200\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable response = context.getVar("response");
        
        assertNotNull(response, "HTTP response should exist");
    }
    
    private DynamicScopeContext executeXmlString(String xmlContent) throws Exception {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-config-", ".xml");
        java.nio.file.Files.writeString(tempFile, xmlContent);
        
        try {
            return executeXmlConfig(tempFile.toString());
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }
}

