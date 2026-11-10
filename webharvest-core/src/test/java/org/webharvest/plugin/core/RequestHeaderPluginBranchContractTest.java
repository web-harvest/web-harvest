package org.webharvest.plugin.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Behavioral contracts for request-header plugin validation and execute paths.
 * Filename avoids Surefire {@code *Http*} exclusion.
 */
public class RequestHeaderPluginBranchContractTest {

    @Test(expectedExceptions = PluginException.class)
    public void initializeRejectsNullConfiguration() throws Exception {
        new HttpHeaderPlugin().initialize(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void initializeRejectsBlankName() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("  ");
        when(configuration.getProperty(eq("name"), anyString())).thenReturn("  ");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);
        new HttpHeaderPlugin().initialize(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void initializeRejectsInvalidHeaderName() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("Bad Name!");
        when(configuration.getProperty(eq("name"), anyString())).thenReturn("Bad Name!");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);
        new HttpHeaderPlugin().initialize(configuration);
    }

    @Test
    public void executeReturnsEmptyWhenNamePresentAndBodyRuns() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("Accept");
        when(configuration.getProperty("name", "")).thenReturn("Accept");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getProperty("_textContent")).thenReturn("application/json");

        HttpHeaderPlugin plugin = new HttpHeaderPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertSame(result, EmptyVariable.INSTANCE);
        assertNotNull(plugin.getMetadata());
        assertNotNull(plugin.getConfigurationSchema());
    }

    @Test
    public void executeWarnsAndReturnsEmptyWhenNameMissingAtRuntime() throws Exception {
        PluginConfiguration configuration = mock(PluginConfiguration.class);
        when(configuration.getProperty("name")).thenReturn("X-Trace");
        when(configuration.getProperty("name", "")).thenReturn("");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.hasChildren()).thenReturn(false);

        HttpHeaderPlugin plugin = new HttpHeaderPlugin();
        plugin.initialize(configuration);
        assertSame(plugin.execute(new ScraperContext()), EmptyVariable.INSTANCE);
    }
}
