package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Characterizes the real XML -> ScriptPlugin -> JSR-223 Groovy execution path.
 *
 * This is a compatibility baseline for a future Groovy runtime migration.
 */
public class GroovyScriptRuntimeIntegrationTest extends XmlBasedIntegrationTestBase {

    private static final String NAMESPACE = "http://org.webharvest/schema/2.1/core";

    @Test(groups = {"integration", "script"})
    public void executesGroovyThroughXmlScriptPluginWithContextBindingsAndJavaInterop()
            throws Exception {
        String xml = "<config xmlns=\"" + NAMESPACE + "\">"
                + "<def var=\"input\" value=\"41\"/>"
                + "<def var=\"groovyResult\">"
                + "  <script language=\"groovy\"><![CDATA["
                + "    import java.util.UUID\n"
                + "    def number = Integer.parseInt(context.getVar('input').toString())\n"
                + "    def stableId = UUID.fromString('123e4567-e89b-12d3-a456-426614174000')\n"
                + "    number + 1 + (stableId.toString().length() == 36 ? 0 : 1000)\n"
                + "  ]]></script>"
                + "</def>"
                + "</config>";

        DynamicScopeContext context = executeXmlString(xml);
        Variable result = context.getVar("groovyResult");

        assertNotNull(result, "Groovy result must be stored through the real ScriptPlugin");
        assertEquals(result.toString(), "42",
                "Groovy must retain context binding, Java interop and expression result behavior");
    }
}
