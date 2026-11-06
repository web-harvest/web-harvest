package org.webharvest.plugin.core.integration.critical;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/** Integration tests for the critical HttpPlugin using deterministic HTTP fixtures. */
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
        assertTrue(content.toString().contains("<html"),
            "Response should contain the deterministic HTML fixture");
    }

    @Test
    public void testHttpWithVariableUrl() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"url\">https://httpbin.org/json</def>\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"${url}\"/>\n" +
            "    </def>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable response = context.getVar("response");

        assertNotNull(response, "HTTP response should be stored in response variable");
        assertTrue(response.toString().contains("Sample Slide Deck"),
            "Interpolated URL should resolve to the deterministic JSON fixture");
    }

    @Test
    public void testHttpSimpleUrl() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n" +
            "    <def var=\"response\">\n" +
            "        <http url=\"https://httpbin.org/json\"/>\n" +
            "    </def>\n" +
            "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable response = context.getVar("response");

        assertNotNull(response, "HTTP response should exist");
        assertTrue(response.toString().contains("Sample Slide Deck"),
            "HTTP response should contain the deterministic JSON fixture");
    }
}
