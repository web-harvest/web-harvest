package org.webharvest.plugin.integration;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.core.ScriptPlugin;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Characterizes the real ScriptPlugin -> injected JSR-223 Groovy execution path.
 *
 * This is a production runtime contract for JSR-223 discovery, Java interop and script results.
 */
public class GroovyScriptRuntimeIntegrationTest extends XmlBasedIntegrationTestBase {

    @Test(groups = {"integration", "script"})
    public void executesGroovyThroughInjectedJsr223FactoryWithJavaInterop()
            throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("language")).thenReturn("groovy");
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("return")).thenReturn(null);

        String script = "import java.util.UUID\n"
                + "def stableId = UUID.fromString('123e4567-e89b-12d3-a456-426614174000')\n"
                + "41 + (stableId.toString().length() == 36 ? 1 : 1000)";
        PluginConfiguration scriptBody = mock(PluginConfiguration.class);
        when(scriptBody.getProperty("_textContent")).thenReturn(script);
        when(scriptBody.getElementName()).thenReturn("constant");
        when(configuration.hasChildren()).thenReturn(true);
        when(configuration.getChildren()).thenReturn(Collections.singletonList(scriptBody));

        ScriptPlugin plugin = new ScriptPlugin();
        plugin.initialize(configuration);
        injector.injectMembers(plugin);

        Variable result = plugin.execute(new ScraperContext());

        assertNotNull(result, "The real ScriptPlugin must return a JSR-223 result");
        assertEquals(result.toString(), "42",
                "Groovy must retain Java interop and expression result behavior");
    }
}
