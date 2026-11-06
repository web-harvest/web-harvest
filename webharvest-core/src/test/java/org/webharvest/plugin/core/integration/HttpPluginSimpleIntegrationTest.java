package org.webharvest.plugin.core.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/** Integration tests for HttpPlugin using the deterministic HTTP fixtures in the base test module. */
public class HttpPluginSimpleIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test
    public void testHttpPluginDirectCall() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <http url=\"https://httpbin.org/json\"/>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("configResult");

        assertNotNull(result, "HTTP result should exist in configResult");
        assertTrue(result.toString().contains("Sample Slide Deck"),
            "HTTP response should contain the deterministic JSON fixture");
    }

    @Test
    public void testHttpPluginViaDefVar() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"https://httpbin.org/json\"/>\n" +
            "    </def>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable response = context.getVar("response");

        assertNotNull(response, "HTTP response should be stored in response variable");
        assertTrue(response.toString().contains("Sample Slide Deck"),
            "HTTP response should contain the deterministic JSON fixture");
    }
}
