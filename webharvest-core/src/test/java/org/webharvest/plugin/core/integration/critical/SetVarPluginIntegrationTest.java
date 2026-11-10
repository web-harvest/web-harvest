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
    public void testSetVarWithWebHarvest10NameAttribute() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"counter\" value=\"0\"/>\n" +
            "    <set-var name=\"counter\" value=\"1\"/>\n" +
            "</config>";
        DynamicScopeContext context = executeXmlString(xml);
        Variable counter = context.getVar("counter");
        assertNotNull(counter, "Legacy name attribute should update the variable");
        assertEquals("1", counter.toString().trim());
    }

    @Test
    public void testSetVarAcceptsMatchingNameAndVarAttributes() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <set-var name=\"sharedName\" var=\"sharedName\" value=\"compatible\"/>\n" +
            "</config>";
        DynamicScopeContext context = executeXmlString(xml);
        Variable value = context.getVar("sharedName");
        assertNotNull(value);
        assertEquals("compatible", value.toString().trim());
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

    @Test
    public void testSetVarBodyArithmeticAddition() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"counter\" value=\"3\"/>\n" +
            "    <set-var var=\"counter\">${counter + 2}</set-var>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable counter = context.getVar("counter");
        assertNotNull(counter);
        assertEquals(counter.toString().trim(), "5",
                "Body arithmetic ${counter + 2} must update the existing variable");
    }

    @Test
    public void testSetVarBodyArithmeticSubtractionAndNonNumericFallback() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"counter\" value=\"abc\"/>\n" +
            "    <set-var var=\"counter\">${counter - 1}</set-var>\n" +
            "    <def var=\"offset\" value=\"7\"/>\n" +
            "    <set-var var=\"offset\">${offset - xyz}</set-var>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        assertEquals(context.getVar("counter").toString().trim(), "-1",
                "Non-numeric left operand falls back to 0, so ${counter - 1} becomes -1");
        assertEquals(context.getVar("offset").toString().trim(), "7",
                "Non-numeric right operand falls back to 0, so ${offset - xyz} remains 7");
    }

    @Test
    public void testSetVarRejectsConflictingNameAndVarAttributes() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <set-var name=\"legacy\" var=\"modern\" value=\"x\"/>\n" +
            "</config>";

        try {
            executeXmlString(xml);
            fail("Conflicting name/var attributes must fail validation or execution");
        } catch (Exception expected) {
            StringBuilder messages = new StringBuilder();
            for (Throwable cursor = expected; cursor != null; cursor = cursor.getCause()) {
                if (cursor.getMessage() != null) {
                    messages.append(' ').append(cursor.getMessage());
                }
            }
            String combined = messages.toString().toLowerCase();
            assertTrue(combined.contains("name") && combined.contains("var") && combined.contains("conflict"),
                    "Failure must mention the conflicting name/var attributes: " + messages);
        }
    }
    
    // v2.2.x: executeXmlString() moved to XmlBasedIntegrationTestBase
}

