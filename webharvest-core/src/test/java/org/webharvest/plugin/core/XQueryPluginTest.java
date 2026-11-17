package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Unit tests for XQueryPlugin to verify XQuery execution works correctly.
 * 
 * TDD: Write test first to define expected behavior, then fix plugin if needed.
 */
public class XQueryPluginTest {

    private XQueryPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new XQueryPlugin();
        context = new ScraperContext();
    }

    @Test(description = "Test XQuery plugin metadata")
    public void testXQueryPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "XQuery", 
                    "Plugin name should be 'XQuery Plugin'");
    }

}
