package org.webharvest.plugin.core.integration.critical;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration test for GetVarPlugin (element name: "get")
 * 
 * CRITICAL plugin - required for retrieving variable values
 * 
 * Tests:
 * - Basic variable retrieval
 * - Attribute: var
 * - Usage in nested contexts
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class GetVarPluginIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testBasicGetVar() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"message\">Hello from GetVar</def>\n" +
            "    <def var=\"retrieved\">\n" +
            "        <get var=\"message\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable retrieved = context.getVar("retrieved");
        
        assertNotNull(retrieved, "Retrieved variable should exist");
        assertEquals("Hello from GetVar", retrieved.toString().trim());
    }
    
    @Test
    public void testGetVarInXPath() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"xmlData\"><root><item>Test</item></root></def>\n" +
            "    <def var=\"extracted\">\n" +
            "        <xpath expression=\"//item\">\n" +
            "            <get var=\"xmlData\"/>\n" +
            "        </xpath>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable extracted = context.getVar("extracted");
        
        assertNotNull(extracted);
        assertTrue(extracted.toString().contains("Test"));
    }
    
    @Test
    public void testGetVarMultiple() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"a\">AAA</def>\n" +
            "    <def var=\"b\">BBB</def>\n" +
            "    <def var=\"combined\">\n" +
            "        <get var=\"a\"/> - <get var=\"b\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable combined = context.getVar("combined");
        
        assertNotNull(combined);
        String value = combined.toString();
        assertTrue(value.contains("AAA"), "Should contain first variable: " + value);
        assertTrue(value.contains("BBB"), "Should contain second variable: " + value);
    }
    
    @Test
    public void testGetSystemVariable() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"sysInfo\">\n" +
            "        <get var=\"sys\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable sysInfo = context.getVar("sysInfo");
        
        assertNotNull(sysInfo, "System variable 'sys' should be accessible");
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

