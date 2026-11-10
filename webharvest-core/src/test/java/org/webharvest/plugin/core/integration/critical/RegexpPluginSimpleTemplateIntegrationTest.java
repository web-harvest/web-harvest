package org.webharvest.plugin.core.integration.critical;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Covers the regexp-result simple {@code <var name='...'/>} template fallback path.
 */
public class RegexpPluginSimpleTemplateIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test
    public void testRegexpResultUsesSimpleVarTemplateFallback() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <def var=\"matched\">\n"
                + "    <regexp>\n"
                + "      <regexp-pattern>(\\w+)-(\\d+)</regexp-pattern>\n"
                + "      <regexp-source>item-42</regexp-source>\n"
                + "      <regexp-result>&lt;var name='group1'/&gt;-&lt;var name='group2'/&gt;</regexp-result>\n"
                + "    </regexp>\n"
                + "  </def>\n"
                + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable matched = context.getVar("matched");
        assertNotNull(matched, "Regexp result variable must exist");
        assertEquals(matched.toString().trim(), "item-42",
                "Simple var template fallback must substitute capture groups");
    }

    @Test
    public void testRegexpResultMissingGroupVariableBecomesEmpty() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <def var=\"matched\">\n"
                + "    <regexp>\n"
                + "      <regexp-pattern>(abc)</regexp-pattern>\n"
                + "      <regexp-source>abc</regexp-source>\n"
                + "      <regexp-result>prefix-&lt;var name='group9'/&gt;-suffix</regexp-result>\n"
                + "    </regexp>\n"
                + "  </def>\n"
                + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable matched = context.getVar("matched");
        assertNotNull(matched);
        assertEquals(matched.toString().trim(), "prefix--suffix",
                "Missing group variables must substitute as empty strings");
    }
}
