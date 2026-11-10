package org.webharvest.plugin.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Behavioral contracts for {@link GetVarPlugin} validation and retrieval paths.
 */
public class GetVarPluginBranchContractTest {

    @Test(expectedExceptions = PluginException.class)
    public void initializeRejectsNullConfiguration() throws Exception {
        new GetVarPlugin().initialize(null);
    }

    @Test
    public void executeRejectsEmptyVarName() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty(eq("var"), anyString())).thenReturn("");
        when(configuration.getProperty("var")).thenReturn("");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);

        GetVarPlugin plugin = new GetVarPlugin();
        plugin.initialize(configuration);
        try {
            plugin.execute(new ScraperContext());
            fail("empty var");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("variable name"));
        }
    }

    @Test
    public void retrievesDefinedVariableAndFailsWhenMissing() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("var", "")).thenReturn("token");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);

        GetVarPlugin plugin = new GetVarPlugin();
        plugin.initialize(configuration);

        ScraperContext context = new ScraperContext();
        context.setLocalVar("token", new NodeVariable("secret"));
        Variable found = plugin.execute(context);
        assertEquals(found.toString(), "secret");

        try {
            plugin.execute(new ScraperContext());
            fail("missing variable");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("token"));
        }
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsNullConfiguration() throws Exception {
        new GetVarPlugin().validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsEmptyVarName() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty(eq("var"), anyString())).thenReturn("");
        new GetVarPlugin().validateConfiguration(configuration);
    }

    @Test
    public void pluginSchemaResolvesVarAttribute() {
        assertNotNull(new GetVarPlugin().getConfigurationSchema().getAttribute("var"));
        assertNull(new GetVarPlugin().getConfigurationSchema().getAttribute("missing"));
        assertNotNull(new GetVarPlugin().getMetadata());
    }
}
