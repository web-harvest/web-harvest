package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Unit tests for TemplatePlugin - variable substitution and template processing.
 * Used in examples for generating JSON reports and formatted output.
 */
public class TemplatePluginTest {

    private TemplatePlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new TemplatePlugin();
        context = new ScraperContext();
    }

    @Test(description = "Test template plugin metadata")
    public void testTemplatePluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Template", 
                    "Plugin name should be 'Template Plugin'");
    }
}
