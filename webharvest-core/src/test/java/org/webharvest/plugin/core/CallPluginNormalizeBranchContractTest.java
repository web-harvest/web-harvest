package org.webharvest.plugin.core;

import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Surefire-visible CallPlugin contracts for return normalization and explicit
 * {@code <return>} handling. Mirrors the normalization matrix from
 * {@link FunctionCallIntegrationTest} so JaCoCo sees CallPlugin branches during unit test runs.
 */
public class CallPluginNormalizeBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test
    public void multiItemXPathResultNormalizesToLastNonEmptyItem() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"lastItem\">\n"
                + "    <xpath expression=\"//item/text()\">\n"
                + "      <template><![CDATA[<root><item>first</item><item>last</item></root>]]></template>\n"
                + "    </xpath>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"lastItem\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "last");
    }

    @Test
    public void emptyXPathResultRemainsEmptyAfterCall() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"noMatch\">\n"
                + "    <xpath expression=\"//missing/text()\">\n"
                + "      <template><![CDATA[<root><item>present</item></root>]]></template>\n"
                + "    </xpath>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"noMatch\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result instanceof EmptyVariable || result.isEmpty());
    }

    @Test
    public void bracketedTemplateFragmentsNormalizeToSpacedText() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"bracketed\"><template>Hello [Alice] [Bob]</template></function>\n"
                + "  <def var=\"result\"><call name=\"bracketed\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "Hello Alice Bob");
    }

    @Test
    public void singleItemListUnwrapsToScalarValue() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"singleItem\">\n"
                + "    <xpath expression=\"//item/text()\">\n"
                + "      <template><![CDATA[<root><item>only</item></root>]]></template>\n"
                + "    </xpath>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"singleItem\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "only");
    }

    @Test
    public void nestedBracketWrappersAreStrippedFromReturn() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"nestedBrackets\"><template>[[alpha]]</template></function>\n"
                + "  <def var=\"result\"><call name=\"nestedBrackets\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString().trim(), "alpha");
    }

    @Test
    public void explicitEmptyReturnOverridesEarlierBodyExpression() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"explicitEmpty\">\n"
                + "    <template>body value must not be returned</template>\n"
                + "    <return>\n"
                + "      <xpath expression=\"//missing/text()\"><template><![CDATA[<root/>]]></template></xpath>\n"
                + "    </return>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"explicitEmpty\"/></def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result instanceof EmptyVariable || result.isEmpty());
    }

    @Test
    public void callParamWithEmptyNameIsIgnoredWithoutFailingCall() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"withParam\">\n"
                + "    <return>\n"
                + "      <template>${callParamLabel}</template>\n"
                + "    </return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"withParam\">\n"
                + "      <call-param name=\"\">ignored</call-param>\n"
                + "      <call-param name=\"label\">ok</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";

        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "ok");
    }
}
