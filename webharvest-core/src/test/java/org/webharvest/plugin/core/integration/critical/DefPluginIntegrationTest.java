package org.webharvest.plugin.core.integration.critical;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.KeyValuePair;

import static org.testng.Assert.*;

/**
 * Integration test for DefPlugin (element name: "def")
 * 
 * CRITICAL plugin - required for basic variable definition
 * 
 * Tests:
 * - Basic variable definition
 * - Nested content
 * - Variable interpolation
 * - Multiple variables
 * - Empty values
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class DefPluginIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testBasicDefVar() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"greeting\">Hello World</def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable greeting = context.getVar("greeting");
        
        assertNotNull(greeting, "Variable 'greeting' should be defined");
        assertEquals("Hello World", greeting.toString().trim());
    }
    
    @Test
    public void testDefVarWithNestedHttp() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"content\">\n" +
            "        <http url=\"https://httpbin.org/html\"/>\n" +
            "    </def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable content = context.getVar("content");
        
        assertNotNull(content, "Variable 'content' should be defined");
        assertTrue(content.toString().contains("<html"), 
                   "Content should contain HTML");
    }
    
    @Test
    public void testDefVarWithInterpolation() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"name\">World</def>\n" +
            "    <def var=\"greeting\">Hello ${name}!</def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable greeting = context.getVar("greeting");
        
        assertNotNull(greeting);
        assertTrue(greeting.toString().contains("World"), 
                   "Should interpolate variable: " + greeting.toString());
    }
    
    @Test
    public void testMultipleDefVars() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"var1\">Value 1</def>\n" +
            "    <def var=\"var2\">Value 2</def>\n" +
            "    <def var=\"var3\">Value 3</def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        assertNotNull(context.getVar("var1"));
        assertNotNull(context.getVar("var2"));
        assertNotNull(context.getVar("var3"));
        
        assertEquals("Value 1", context.getVar("var1").toString().trim());
        assertEquals("Value 2", context.getVar("var2").toString().trim());
        assertEquals("Value 3", context.getVar("var3").toString().trim());
    }
    
    @Test
    public void testDefVarEmpty() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"empty\"></def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable empty = context.getVar("empty");
        
        assertNotNull(empty, "Empty variable should still be defined");
    }
    
    @Test
    public void testDefVarOverwrite() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"test\">Original</def>\n" +
            "    <def var=\"test\">Overwritten</def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable test = context.getVar("test");
        
        assertNotNull(test);
        assertEquals("Overwritten", test.toString().trim());
    }
    
    @Test
    public void testDefVarIterator() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"a\">AAA</def>\n" +
            "    <def var=\"b\">BBB</def>\n" +
            "    <def var=\"c\">CCC</def>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        // Verify we can iterate over variables
        int count = 0;
        for (KeyValuePair<Variable> pair : context) {
            assertNotNull(pair.getKey());
            assertNotNull(pair.getValue());
            count++;
        }
        
        assertTrue(count >= 3, "Should have at least 3 user-defined variables (plus system vars)");
    }
    
    // v2.2.x: executeXmlString() moved to XmlBasedIntegrationTestBase
}

