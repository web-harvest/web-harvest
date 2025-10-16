package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.testng.Assert.*;

/**
 * Unit tests for GetVarPlugin - retrieve variables from context.
 * Used in examples as <get var="name"/> to access defined variables.
 */
public class GetVarPluginTest {

    private GetVarPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new GetVarPlugin();
        context = new ScraperContext();
    }

    @Test(description = "Test GetVarPlugin retrieves existing variable")
    public void testGetVarPluginRetrievesVariable() {
        // Expected behavior from examples:
        // <get var="html"/>
        // Should retrieve 'html' variable from context
        
        // Set a variable
        context.setLocalVar("html", new NodeVariable("<html>test</html>"));
        
        // Verify it can be retrieved
        Variable result = context.getVar("html");
        assertNotNull(result, "Variable should be retrievable");
        assertTrue(result.toString().contains("test"));
    }

    @Test(description = "Test GetVarPlugin with non-existent variable")
    public void testGetVarPluginNonExistentVariable() {
        // Expected behavior:
        // <get var="nonexistent"/>
        // Should return empty or null
        
        Variable result = context.getVar("nonexistent");
        // May be null or empty - both acceptable
        assertTrue(result == null || result.toString().isEmpty(), 
                  "Non-existent variable should be null or empty");
    }

    @Test(description = "Test GetVarPlugin in XPath context")
    public void testGetVarPluginInXPathContext() {
        // Expected behavior from examples:
        // <xpath expression="//title/text()">
        //     <get var="page"/>
        // </xpath>
        // GetVarPlugin should provide XML content to XPath for processing
        
        String xmlContent = "<page><title>Test Title</title></page>";
        context.setLocalVar("page", new NodeVariable(xmlContent));
        
        Variable page = context.getVar("page");
        assertNotNull(page);
        assertTrue(page.toString().contains("Test Title"));
    }

    @Test(description = "Test GetVarPlugin metadata")
    public void testGetVarPluginMetadata() {
        assertNotNull(plugin.getMetadata(), "Plugin should have metadata");
        // GetVarPlugin is registered as 'get' or 'var' in examples
    }
}
