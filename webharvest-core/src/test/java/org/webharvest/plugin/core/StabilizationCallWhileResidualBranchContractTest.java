package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Additional Call/While/Include residual behavior for Gate C branch lift.
 */
public class StabilizationCallWhileResidualBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test(expectedExceptions = org.webharvest.plugin.exceptions.PluginValidationException.class)
    public void callWithBlankNameIsRejectedAtValidation() throws Exception {
        CallPlugin plugin = new CallPlugin();
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "call", NS, "call");
        configuration.setProperty("name", "   ");
        plugin.initialize(configuration);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void includeWithoutActiveConfigFailsExplicitly() throws Exception {
        IncludePlugin plugin = new IncludePlugin();
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "include", NS, "include");
        configuration.setProperty("path", "missing-but-accepted.xml");
        plugin.initialize(configuration);
        plugin.execute(new ScraperContext());
    }

    @Test
    public void whileFalseConditionProducesEmptyAggregation() throws Exception {
        WhilePlugin plugin = new WhilePlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("should-not-run");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", NS, "while");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "5");
        configuration.setProperty("index", "i");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertNotNull(result);
        assertFalse(result.toString().contains("should-not-run"), String.valueOf(result));
    }

    @Test
    public void callWithExplicitEmptyReturnRemainsEmpty() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"emptyBody\">\n"
                + "    <return></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"emptyBody\"/></def>\n"
                + "  <def var=\"ok\">after</def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
        assertEquals(ctx.getVar("ok").toString(), "after");
    }

    @Test
    public void callCollectsMultipleNamedParams() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"pair\">\n"
                + "    <return><template>${callParamLeft}:${callParamRight}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"pair\">\n"
                + "      <call-param name=\"left\">A</call-param>\n"
                + "      <call-param name=\"right\">B</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString().trim(), "A:B");
    }
}
