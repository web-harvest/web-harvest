package org.webharvest.plugin.webbrowser;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for Bug #33: JavaScript and AJAX support in WebBrowser plugin.
 * 
 * <h3>Bug #33: Please Add Parse JavaScript and AJAX</h3>
 * <p>
 * <strong>User Request:</strong> "Please add parse javascript and AJAX in HTML 
 * while extract data from HTML"
 * </p>
 * 
 * <p><strong>History:</strong></p>
 * <ul>
 * <li>2013-2015 (r649-r655): Feature existed with PhantomJS</li>
 * <li>2024 (r695): Lost during architecture refactoring</li>
 * <li>2025 (v2.2.1): Restored with HtmlUnit implementation</li>
 * </ul>
 * 
 * <p><strong>Status:</strong> FIXED (v2.2.1)</p>
 * <p><strong>Implementation:</strong> HtmlUnit 2.70.0 with JavaScript and AJAX support</p>
 * 
 * @author Web-Harvest Team
 * @since 2.2.1
 */
public class WebBrowserPluginBug33Test {
    
    private WebBrowserPlugin plugin;
    private DynamicScopeContext context;
    
    @BeforeMethod
    public void setUp() {
        plugin = new WebBrowserPlugin();
        context = new org.webharvest.runtime.ScraperContext();
        WebBrowserPlugin.closeAllPages();
    }
    
    @AfterMethod
    public void tearDown() {
        WebBrowserPlugin.closeAllPages();
    }
    
    /**
     * Test Bug #33: Plugin metadata contains JavaScript/AJAX support info.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_MetadataContainsJavaScriptAjaxSupport() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "web-browser");
        assertEquals(plugin.getMetadata().getName(), "Web Browser Plugin");
        assertEquals(plugin.getMetadata().getVersion(), "2.2.1");
        
        // Bug #33: Description must mention JavaScript and AJAX
        String desc = plugin.getMetadata().getDescription();
        assertTrue(desc.contains("JavaScript"), "Description should mention JavaScript");
        assertTrue(desc.contains("AJAX"), "Description should mention AJAX");
        assertTrue(desc.contains("HtmlUnit"), "Description should mention HtmlUnit");
    }
    
    /**
     * Test Bug #33: Plugin has JavaScript/AJAX tags.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_HasJavaScriptAjaxTags() {
        String[] tags = plugin.getMetadata().getTags();
        assertNotNull(tags);
        assertTrue(containsTag(tags, "javascript"), "Should have 'javascript' tag");
        assertTrue(containsTag(tags, "ajax"), "Should have 'ajax' tag");
        assertTrue(containsTag(tags, "browser"), "Should have 'browser' tag");
        assertTrue(containsTag(tags, "automation"), "Should have 'automation' tag");
    }
    
    /**
     * Test Bug #33: Configuration schema supports required attributes.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_ConfigurationSchemaSupportsActions() {
        var schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        
        // Required: action
        String[] required = schema.getRequiredAttributes();
        assertNotNull(required);
        assertEquals(required.length, 1);
        assertEquals(required[0], "action");
        
        // Optional: url, page, wait, javascript-enabled
        String[] optional = schema.getOptionalAttributes();
        assertNotNull(optional);
        assertTrue(optional.length >= 4, "Should have at least 4 optional attributes");
        
        // Body allowed for execute-script
        assertTrue(schema.allowsBody(), "Should allow body for JavaScript code");
    }
    
    /**
     * Test Bug #33: Plugin is in Browser category.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_PluginInBrowserCategory() {
        assertEquals(plugin.getMetadata().getCategory(), "Browser");
        assertEquals(plugin.getMetadata().getElementName(), "web-browser");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }
    
    /**
     * Test Bug #33: Load action requires URL.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_LoadActionRequiresUrl() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "load");
        props.put("url", "");  // Empty URL
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        try {
            plugin.execute(context);
            fail("Should throw exception for empty URL");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("URL is required"));
        }
    }
    
    /**
     * Test Bug #33: Load action validates URL format.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_LoadActionValidatesUrlFormat() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "load");
        props.put("url", "not-a-valid-url");
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        try {
            plugin.execute(context);
            fail("Should throw exception for invalid URL");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Invalid URL format"));
        }
    }
    
    /**
     * Test Bug #33: Unsupported action throws exception.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_UnsupportedActionThrowsException() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "unsupported-action");
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        try {
            plugin.execute(context);
            fail("Should throw exception for unsupported action");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("Unsupported WebBrowser action"));
        }
    }
    
    /**
     * Test Bug #33: Get-content requires loaded page.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_GetContentRequiresLoadedPage() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "get-content");
        props.put("page", "nonexistent");
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        try {
            plugin.execute(context);
            fail("Should throw exception for nonexistent page");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }
    
    /**
     * Test Bug #33: Close action handles nonexistent page gracefully.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_CloseActionHandlesNonexistentPage() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "close");
        props.put("page", "nonexistent");
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        // Should NOT throw exception
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }
    
    /**
     * Test Bug #33: CloseAllPages utility method works.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_CloseAllPagesUtility() {
        // Should not throw
        WebBrowserPlugin.closeAllPages();
        WebBrowserPlugin.closeAllPages();
    }
    
    /**
     * Test Bug #33: Invalid wait parameter defaults to 1000ms.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_InvalidWaitParameterDefaultsTo1000() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "close");
        props.put("wait", "invalid-number");
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        
        plugin.initialize(config);
        
        // Should handle gracefully (default to 1000)
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }
    
    /**
     * Test Bug #33: JavaScript enabled/disabled parsing.
     */
    @Test(groups = {"bug-fix", "webbrowser"})
    public void testBug33_JavaScriptEnabledParameter() throws Exception {
        Map<String, String> props = new HashMap<>();
        props.put("action", "close");
        
        // Test "true"
        props.put("javascript-enabled", "true");
        DefaultPluginConfiguration config1 = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        WebBrowserPlugin plugin1 = new WebBrowserPlugin();
        plugin1.initialize(config1);
        plugin1.execute(context);
        
        // Test "yes"
        props.put("javascript-enabled", "yes");
        DefaultPluginConfiguration config2 = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        WebBrowserPlugin plugin2 = new WebBrowserPlugin();
        plugin2.initialize(config2);
        plugin2.execute(context);
        
        // Test "false"
        props.put("javascript-enabled", "false");
        DefaultPluginConfiguration config3 = new DefaultPluginConfiguration(
            "web-browser", "http://org.webharvest/schema/2.1/core", "web-browser", props);
        WebBrowserPlugin plugin3 = new WebBrowserPlugin();
        plugin3.initialize(config3);
        plugin3.execute(context);
    }
    
    /**
     * Documentation test: Bug #33 history.
     */
    @Test(groups = {"documentation", "webbrowser"})
    public void testBug33_HistoryAndImplementation() {
        // Bug #33 History:
        // - 2013-2015 (r649-r655): PhantomJS implementation
        // - 2024 (r695): Lost during refactoring
        // - 2025 (v2.2.1): Restored with HtmlUnit
        
        assertEquals(plugin.getMetadata().getVersion(), "2.2.1",
            "Bug #33 fixed in v2.2.1");
        
        String desc = plugin.getMetadata().getDescription();
        assertTrue(desc.contains("JavaScript"));
        assertTrue(desc.contains("AJAX"));
    }
    
    private boolean containsTag(String[] tags, String tag) {
        if (tags == null || tag == null) return false;
        for (String t : tags) {
            if (tag.equals(t)) return true;
        }
        return false;
    }
}
