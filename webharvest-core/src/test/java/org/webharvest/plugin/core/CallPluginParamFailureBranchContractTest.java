package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

/**
 * CallPlugin call-param collection edges: blank names are ignored and empty
 * call-param bodies do not abort the surrounding call.
 */
public class CallPluginParamFailureBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test
    public void blankCallParamNamesAreIgnoredDuringCollection() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"greet\">\n"
                + "    <return><template>Hi ${callParamWho}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"greet\">\n"
                + "      <call-param name=\"\">ignored</call-param>\n"
                + "      <call-param name=\"who\">Pat</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertTrue(ctx.getVar("result").toString().contains("Pat"));
    }

    @Test
    public void emptyCallParamBodyDoesNotAbortSurroundingCall() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"echo\">\n"
                + "    <return><template>[${callParamBroken}][${callParamOk}]</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"echo\">\n"
                + "      <call-param name=\"broken\">\n"
                + "        <xpath expression=\"///broken[[[\"/>\n"
                + "      </call-param>\n"
                + "      <call-param name=\"ok\">kept</call-param>\n"
                + "    </call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        String text = result.toString();
        assertTrue(text.contains("kept"), text);
        assertTrue(text.contains("[]") || text.contains("[ ]") || text.matches(".*\\[\\].*"),
                "Broken/empty call-param should contribute an empty value: " + text);
    }
}
