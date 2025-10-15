package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.PluginConfiguration;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * TDD Test for Bug #13: CSS Selector support in XPathPlugin
 * 
 * RED/GREEN/REFACTOR cycle for adding CSS selector functionality
 * 
 * PLAN:
 * 1. RED: type="css" attribute recognition  
 * 2. GREEN: Add type field to XPathPlugin
 * 3. RED: CSS selector parsing
 * 4. GREEN: Integrate jsoup for CSS selectors
 * 5. REFACTOR: Extract CSS logic to helper method
 */
public class XPathCssSelectorBug13Test {

    private XPathPlugin plugin;
    private DynamicScopeContext context;
    private PluginConfiguration config;

    @BeforeMethod
    public void setUp() throws Exception {
        plugin = new XPathPlugin();
        context = mock(DynamicScopeContext.class);
        config = mock(PluginConfiguration.class);
        
        when(context.getCharset()).thenReturn("UTF-8");
    }

    @Test
    public void testCycle1_CssSelectorTypeAttribute() throws Exception {
        // TDD Cycle 1: RED - Test that type="css" is recognized during initialization
        
        Map<String, String> properties = new HashMap<>();
        properties.put("type", "css");
        properties.put("expression", "a.link");
        
        when(config.getProperty("type")).thenReturn("css");
        when(config.getProperty("expression")).thenReturn("a.link");
        when(config.getAllProperties()).thenReturn(properties);
        
        // This should not throw exception - type="css" should be valid
        try {
            plugin.initialize(config);
            assertTrue(true, "Plugin accepts type='css' without throwing exception");
        } catch (Exception e) {
            fail("Plugin should accept type='css' but threw: " + e.getMessage());
        }
    }

    
    @Test
    public void testCycle2_CssSelectorHelperMethodExists() throws Exception {
        // TDD Cycle 2: RED - Test that plugin can evaluate CSS selectors
        // Simplest possible test - just check if helper method exists
        
        String htmlContent = "<html><body><a class='link'>Test</a></body></html>";
        String cssSelector = "a.link";
        
        try {
            // This method doesn't exist yet - test should fail compilation or runtime
            java.lang.reflect.Method method = XPathPlugin.class.getDeclaredMethod(
                "evaluateCssSelector", String.class, String.class
            );
            
            // If method exists, try to invoke it
            method.setAccessible(true);
            Object result = method.invoke(plugin, htmlContent, cssSelector);
            
            assertNotNull(result, "CSS selector evaluation should return result");
        } catch (NoSuchMethodException e) {
            fail("XPathPlugin should have evaluateCssSelector() method - TDD RED phase OK");
        }
    }
    
    @Test
    public void testCycle3_RealCssSelectorWithJsoup() throws Exception {
        // TDD Cycle 3: RED - Test actual CSS selector using jsoup
        
        String htmlContent = "<html><body>" +
            "<a href='page1.html' class='link'>Link 1</a>" +
            "<a href='page2.html' class='link'>Link 2</a>" +
            "<a href='page3.html' class='other'>Other</a>" +
            "</body></html>";
        String cssSelector = "a.link";  // Select links with class='link'
        
        Variable result = plugin.evaluateCssSelector(htmlContent, cssSelector);
        
        assertNotNull(result, "Result should not be null");
        String resultText = result.toString();
        
        // Should find text from matching elements
        assertTrue(resultText.contains("Link 1") || resultText.contains("Link 2"),
            "Should contain text from elements with class='link'");
        assertFalse(resultText.contains("Other"),
            "Should NOT contain text from elements without class='link'");
    }
}

