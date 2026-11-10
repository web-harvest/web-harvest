package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Additional CallPlugin normalization/resolution contracts for Surefire/JaCoCo.
 */
public class CallPluginMoreNormalizeBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test
    public void bracketedTemplateFragmentsAreFlattened() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"brackets\">\n"
                + "    <return><template>Hello [Alice]</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"brackets\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertTrue(result.toString().contains("Alice"), result.toString());
        assertFalse(result.toString().contains("[Alice]"), result.toString());
    }

    @Test
    public void explicitEmptyReturnRemainsEmpty() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"emptyRet\"><return/></function>\n"
                + "  <def var=\"result\"><call name=\"emptyRet\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
    }

    @Test
    public void callParamBlankNameIsIgnoredAndValidParamUsed() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"greet\">\n"
                + "    <return><template>Hi ${callParamWho}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"greet\">\n"
                + "      <call-param name=\"\">ignored</call-param>\n"
                + "      <call-param name=\"who\">Bob</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result.toString().contains("Bob"), result.toString());
    }

    @Test(expectedExceptions = Exception.class)
    public void validateRejectsMissingCallName() throws Exception {
        org.webharvest.plugin.DefaultPluginConfiguration configuration =
                new org.webharvest.plugin.DefaultPluginConfiguration(
                        "call", NS, "call");
        new CallPlugin().validateConfiguration(configuration);
    }
}
