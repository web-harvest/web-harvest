package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Residual CallPlugin execute contracts: body-only returns, single-char params,
 * and non-param children ignored during call-param collection.
 */
public class CallPluginExecuteResidualBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test
    public void functionWithoutReturnUsesLastBodyExpression() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"bodyOnly\">\n"
                + "    <template>from-body</template>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"bodyOnly\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "from-body");
    }

    @Test
    public void singleCharacterCallParamIsCapitalizedInScope() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"oneChar\">\n"
                + "    <return><template>${callParamX}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"oneChar\">\n"
                + "      <call-param name=\"x\">Z</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "Z");
    }

    @Test
    public void nonCallParamChildrenAreIgnoredWhileCollectingParams() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"mixed\">\n"
                + "    <return><template>Hi ${callParamWho}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"mixed\">\n"
                + "      <text>noise</text>\n"
                + "      <call-param name=\"who\">Pat</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result.toString().contains("Pat"), result.toString());
    }
}
