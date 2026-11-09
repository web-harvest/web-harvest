package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;

import static org.testng.Assert.*;

/**
 * XML-runtime integration tests for WhilePlugin variable and index semantics.
 *
 * These tests deliberately use the real parser, plugin registry, context and body
 * executor. The former tests attempted Mockito stubbing/verifying on a real
 * ScraperContext and invoked SetVarPlugin outside the while body.
 */
public class WhilePluginIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test
    public void testWhilePluginWithSetVarPluginCounter() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <def var=\"counter\" value=\"0\"/>" +
            "  <while test=\"${counter &lt; 3}\" maxloops=\"5\">" +
            "    <set-var name=\"counter\" value=\"${counter + 1}\"/>" +
            "  </while>" +
            "</config>");
        assertEquals(result.getVar("counter").toString(), "3");
    }

    @Test
    public void testWhilePluginWithSetVarPluginIndex() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <def var=\"count\" value=\"0\"/>" +
            "  <def var=\"lastIndex\" value=\"-1\"/>" +
            "  <while test=\"${count &lt; 3}\" index=\"i\" maxloops=\"5\">" +
            "    <set-var name=\"lastIndex\" value=\"${i}\"/>" +
            "    <set-var name=\"count\" value=\"${count + 1}\"/>" +
            "  </while>" +
            "</config>");
        assertEquals(result.getVar("count").toString(), "3");
        assertEquals(result.getVar("i").toString(), "3");
        assertEquals(result.getVar("lastIndex").toString(), "2",
            "The loop body must observe zero-based indices 0, 1, 2");
    }

    @Test
    public void testWhilePluginWithSetVarPluginMultipleVariables() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <def var=\"count\" value=\"0\"/>" +
            "  <def var=\"total\" value=\"0\"/>" +
            "  <while test=\"${count &lt; 2}\" maxloops=\"5\">" +
            "    <set-var name=\"count\" value=\"${count + 1}\"/>" +
            "    <set-var name=\"total\" value=\"${total + 10}\"/>" +
            "  </while>" +
            "</config>");
        assertEquals(result.getVar("count").toString(), "2");
        assertEquals(result.getVar("total").toString(), "20");
    }

    @Test
    public void testWhilePluginWithSetVarPluginEmptyBody() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <while test=\"true\" index=\"i\" maxloops=\"2\"/>" +
            "</config>");
        assertEquals(result.getVar("i").toString(), "2");
    }

    @Test
    public void testWhileWithoutExplicitMaxloopsStopsAfterOneIteration() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <while test=\"true\" index=\"i\"/>" +
            "</config>");
        assertEquals(result.getVar("i").toString(), "1");
    }

    @Test
    public void testWhilePluginWithSetVarPluginConditionalLogic() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <def var=\"flag\" value=\"true\"/>" +
            "  <while test=\"${flag} == true\" maxloops=\"5\">" +
            "    <set-var name=\"flag\" value=\"false\"/>" +
            "  </while>" +
            "</config>");
        assertEquals(result.getVar("flag").toString(), "false");
    }

    @Test
    public void testNewSetVarCreatedInsideLoopRemainsScopedToIteration() throws Exception {
        DynamicScopeContext result = executeXmlString(
            "<config xmlns=\"" + NS + "\">" +
            "  <while test=\"true\" maxloops=\"1\">" +
            "    <set-var name=\"iterationOnly\" value=\"temporary\"/>" +
            "  </while>" +
            "</config>");
        assertNull(result.getVar("iterationOnly"),
            "A variable first created inside an iteration must not leak into the outer scope");
    }
}
