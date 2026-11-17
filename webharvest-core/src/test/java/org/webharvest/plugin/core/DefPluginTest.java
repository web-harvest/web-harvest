package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Unit tests for DefPlugin - variable definition and assignment.
 * Critical plugin used in ALL examples for setting variables.
 */
public class DefPluginTest {

    private DefPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new DefPlugin();
        context = new ScraperContext();
    }

    @Test(description = "Test DefPlugin metadata")
    public void testDefPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Define Variable", 
                    "Plugin name should be 'Define Variable'");
    }
}
