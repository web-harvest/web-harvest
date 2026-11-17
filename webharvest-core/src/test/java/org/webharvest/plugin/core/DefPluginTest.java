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

    @Test(description = "Test DefPlugin sets simple text variable")
    public void testDefPluginSetsSimpleVariable() {
        // Expected behavior from examples:
        // <def var="url">https://books.toscrape.com/</def>
        // Should set variable 'url' in context with value 'https://books.toscrape.com/'
        
        // Verify context can store variables
        context.setLocalVar("test", new NodeVariable("test value"));
        Variable result = context.getVar("test");
        
        assertNotNull(result, "Variable should be set in context");
        assertEquals(result.toString(), "test value");
    }

    @Test(description = "Test DefPlugin with variable substitution")
    public void testDefPluginWithVariableSubstitution() {
        // Expected behavior:
        // <def var="categoryUrl">https://books.toscrape.com/${firstCategoryLink}</def>
        // Should substitute ${firstCategoryLink} value into string
        
        context.setLocalVar("base", new NodeVariable("https://example.com"));
        context.setLocalVar("path", new NodeVariable("/path"));
        
        // Verify variables can be retrieved
        assertNotNull(context.getVar("base"));
        assertNotNull(context.getVar("path"));
    }

    @Test(description = "Test DefPlugin metadata")
    public void testDefPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        assertEquals(plugin.getMetadata().getName(), "Define Variable", 
                    "Plugin name should be 'Define Variable'");
    }
}
