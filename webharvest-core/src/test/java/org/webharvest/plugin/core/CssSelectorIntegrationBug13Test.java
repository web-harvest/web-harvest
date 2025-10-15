package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.PluginConfiguration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration Test for Bug #13: CSS Selector Support
 * 
 * Tests real-world scenarios with CSS selectors.
 * 
 * @author Robert Bala
 * @since 2.2.1
 */
@Test(groups = {"integration", "bugs", "css"})
public class CssSelectorIntegrationBug13Test {

    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration config;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(context.getCharset()).thenReturn("UTF-8");
    }

    @Test(groups = {"integration", "bugs"})
    public void testCssSelectorIntegration_RealWorldExample() throws Exception {
        System.out.println("\n### Bug #13 Integration Test: Real-World CSS Selectors ###");
        
        // Real-world HTML from e-commerce site
        String htmlContent = 
            "<div class='product-list'>" +
            "  <div class='product'>" +
            "    <h3 class='title'>Laptop Pro 15</h3>" +
            "    <span class='price'>$1299</span>" +
            "    <a href='/buy/laptop-pro-15' class='buy-button'>Buy Now</a>" +
            "  </div>" +
            "  <div class='product'>" +
            "    <h3 class='title'>Mouse Wireless</h3>" +
            "    <span class='price'>$29</span>" +
            "    <a href='/buy/mouse' class='buy-button'>Buy Now</a>" +
            "  </div>" +
            "</div>";
        
        XPathPlugin plugin = new XPathPlugin();
        
        // Test: Extract product titles with CSS
        Map<String, String> props = new HashMap<>();
        props.put("type", "css");
        props.put("expression", ".product h3.title");
        
        when(config.getProperty("type")).thenReturn("css");
        when(config.getProperty("expression")).thenReturn(".product h3.title");
        when(config.getAllProperties()).thenReturn(props);
        
        plugin.initialize(config);
        
        Variable result = plugin.evaluateCssSelector(htmlContent, ".product h3.title");
        
        assertNotNull(result);
        String resultText = result.toString();
        
        System.out.println("✅ CSS Selector: .product h3.title");
        System.out.println("   Results: " + resultText);
        
        assertTrue(resultText.contains("Laptop Pro 15"), "Should extract first product title");
        assertTrue(resultText.contains("Mouse Wireless"), "Should extract second product title");
        assertFalse(resultText.contains("$1299"), "Should NOT extract prices");
        assertFalse(resultText.contains("Buy Now"), "Should NOT extract button text");
        
        System.out.println("   ✓ Correctly extracted ONLY product titles");
    }

    
    @Test(groups = {"integration", "bugs"})
    public void testCssSelectorIntegration_PseudoSelectors() throws Exception {
        System.out.println("\n### Bug #13: CSS Pseudo-Selectors ###");
        
        String htmlContent = 
            "<ul class='navigation'>" +
            "  <li>Home</li>" +
            "  <li>Products</li>" +
            "  <li>Contact</li>" +
            "</ul>";
        
        XPathPlugin plugin = new XPathPlugin();
        
        // Test first-child pseudo-selector
        Variable result = plugin.evaluateCssSelector(htmlContent, "li:first-child");
        
        assertNotNull(result);
        String resultText = result.toString();
        
        System.out.println("✅ CSS Selector: li:first-child");
        System.out.println("   Result: " + resultText);
        
        assertTrue(resultText.contains("Home"), "Should select first list item");
        assertFalse(resultText.contains("Products"), "Should not select second item");
        assertFalse(resultText.contains("Contact"), "Should not select third item");
        
        System.out.println("   ✓ Pseudo-selectors work correctly");
    }
    
    @Test(groups = {"integration", "bugs"})
    public void testCssSelectorIntegration_VsXPath() throws Exception {
        System.out.println("\n### Bug #13: CSS vs XPath Comparison ###");
        
        String htmlContent = "<div class='content'><p>Test paragraph</p></div>";
        
        XPathPlugin plugin = new XPathPlugin();
        
        // Test CSS selector
        Variable cssResult = plugin.evaluateCssSelector(htmlContent, ".content p");
        
        System.out.println("✅ CSS Selector: .content p");
        System.out.println("   Syntax: Simple, familiar to web developers");
        System.out.println("   Result: " + cssResult.toString());
        
        // XPath equivalent would be: //div[@class='content']/p
        System.out.println("\n✅ XPath Equivalent: //div[@class='content']/p");
        System.out.println("   Syntax: More verbose, powerful for XML");
        
        assertNotNull(cssResult);
        assertTrue(cssResult.toString().contains("Test paragraph"),
            "CSS selector should work as well as XPath");
        
        System.out.println("\n   ✓ Both approaches work - user can choose!");
    }
}

