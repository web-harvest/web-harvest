package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Contracts for TemplatePlugin language validation, empty body handling,
 * and optional result variable binding via {@code id}.
 */
public class TemplatePluginBranchContractTest {

    @Mock
    private DynamicScopeContext context;

    @Mock
    private PluginConfiguration configuration;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(configuration.hasChildren()).thenReturn(false);
        when(configuration.getChildren()).thenReturn(java.util.Collections.emptyList());
        when(context.getCharset()).thenReturn("UTF-8");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsUnsupportedLanguage() throws Exception {
        when(configuration.getProperty("language")).thenReturn("ruby");
        TemplatePlugin plugin = new TemplatePlugin();
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void validateAcceptsSupportedLanguagesAndMissingLanguage() throws Exception {
        TemplatePlugin plugin = new TemplatePlugin();
        when(configuration.getProperty("language")).thenReturn("javascript");
        plugin.validateConfiguration(configuration);

        when(configuration.getProperty("language")).thenReturn("groovy");
        plugin.validateConfiguration(configuration);

        when(configuration.getProperty("language")).thenReturn(null);
        plugin.validateConfiguration(configuration);

        when(configuration.getProperty("language")).thenReturn("");
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void executeReturnsEmptyWhenNoTemplateContentAvailable() throws Exception {
        when(configuration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(configuration.getProperty("id", "")).thenReturn("");
        when(configuration.getProperty("_textContent")).thenReturn(null);

        TemplatePlugin plugin = new TemplatePlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result instanceof EmptyVariable || result == null || result.isEmpty());
    }

    @Test
    public void executeBindsResultToIdWhenProvided() throws Exception {
        when(configuration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(configuration.getProperty("id", "")).thenReturn("out");
        when(configuration.getProperty("_textContent")).thenReturn("Hello Template");

        TemplatePlugin plugin = new TemplatePlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertNotNull(result);
        assertFalse(result instanceof EmptyVariable);
        verify(context).setLocalVar(eq("out"), any(Variable.class));
    }
}
