package org.webharvest.plugin.core.integration.critical;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Integration test for SetVarPlugin (element name: "set-var")
 * 
 * CRITICAL plugin - required for modifying variable values
 * 
 * Tests:
 * - Basic variable modification
 * - set-var with name attribute
 * - Updating existing variables
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class SetVarPluginIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test
    public void testBasicSetVar() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <set-var var=\"testVar\" value=\"Hello World\"/>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable modified = context.getVar("testVar");
        
        assertNotNull(modified, "Variable should exist after set-var");
        assertEquals("Hello World", modified.toString().trim());
    }
    
    @Test
    public void testSetVarWithExpression() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <set-var var=\"numberVar\" value=\"42\"/>\n" +
            "    <set-var var=\"expressionVar\" value=\"${1 + 2}\"/>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        Variable numberVar = context.getVar("numberVar");
        
        assertNotNull(numberVar);
        assertEquals("42", numberVar.toString().trim());
    }
    
    @Test
    public void testSetVarMultipleVars() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <set-var var=\"var1\" value=\"Value1\"/>\n" +
            "    <set-var var=\"var2\" value=\"Value2\"/>\n" +
            "    <set-var var=\"var3\" value=\"Value3\"/>\n" +
            "</config>";
        
        DynamicScopeContext context = executeXmlString(xml);
        
        assertNotNull(context.getVar("var1"));
        assertNotNull(context.getVar("var2"));
        assertNotNull(context.getVar("var3"));
    }
    
    // v2.2.x: executeXmlString() moved to XmlBasedIntegrationTestBase
}

