package org.webharvest.plugin.core;

import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * CallPlugin behavioral contracts for missing function names, unknown functions,
 * multi-value return normalization, and call-param propagation.
 */
public class CallPluginResolutionBranchContractTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "call"})
    public void emptyCallNameYieldsEmptyResult() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <def var=\"result\"><call name=\"\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable || result.isEmpty(),
                "Empty call name must not invent a return value");
    }

    @Test(groups = {"integration", "call"})
    public void unknownFunctionYieldsEmptyResult() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <def var=\"result\"><call name=\"missingFunction\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable || result.isEmpty(),
                "Unknown function must return empty rather than throw");
    }

    @Test(groups = {"integration", "call"})
    public void callParamValuesAreVisibleInsideFunction() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <function name=\"greet\">\n"
                + "    <return>\n"
                + "      <template>Hello ${callParamName}</template>\n"
                + "    </return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"greet\">\n"
                + "      <call-param name=\"name\">Alice</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertTrue(result.toString().contains("Alice"),
                "Expected parameterized greeting, got: " + result);
    }

    @Test(groups = {"integration", "call"})
    public void nestedFunctionDefinitionsAreDiscoverable() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"http://org.webharvest/schema/2.1/core\">\n"
                + "  <def var=\"wrapper\">\n"
                + "    <function name=\"nestedFn\">\n"
                + "      <return><template>nested-ok</template></return>\n"
                + "    </function>\n"
                + "  </def>\n"
                + "  <def var=\"result\"><call name=\"nestedFn\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertEquals(result.toString(), "nested-ok");
    }
}
