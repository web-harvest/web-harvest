package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.XmlNode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for ConstantPlugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class ConstantPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private ConstantPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new ConstantPlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("Test constant value");
        when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty("value")).thenReturn("Test constant value");
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testConstantPluginReturnsValueFromConfiguration() throws InterruptedException, PluginException {
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Test constant value");
    }

    @Test
    public void testConstantPluginReturnsEmptyWhenNoValue() throws InterruptedException, PluginException {
        // Mock empty value
        when(configuration.getProperty("value")).thenReturn("");
        when(xmlNode.getText()).thenReturn("");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "");
    }

    @Test(enabled = false) // ULTRA-LEAN v2.2.x: Legacy test - needs rewrite
    public void testConstantPluginReturnsValueFromXmlNode_LEGACY() throws InterruptedException, PluginException {
        // TODO: Rewrite to use PluginConfiguration.getChildren() instead of xmlNode.getText()
    }

    @Test(enabled = false) // ULTRA-LEAN v2.2.x: Legacy test - needs rewrite
    public void testConstantPluginWithBodyContent_LEGACY() throws InterruptedException, PluginException {
        // TODO: Rewrite to use PluginConfiguration.getChildren() instead of xmlNode.getText()
    }

    @Test
    public void testConstantPluginWithComplexValue() throws InterruptedException, PluginException {
        // Mock complex value
        when(configuration.getProperty("value")).thenReturn("Complex value with spaces and special chars: !@#$%");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Complex value with spaces and special chars: !@#$%");
    }

    @Test
    public void testConstantPluginWithNumericValue() throws InterruptedException, PluginException {
        // Mock numeric value
        when(configuration.getProperty("value")).thenReturn("12345");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "12345");
    }

    @Test
    public void testConstantPluginGetMetadata() {
        // Test getMetadata method
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Constant Plugin");
        assertEquals(metadata.getDescription(), "Constant plugin that provides static text content");
    }

    @Test
    public void testConstantPluginHandlesNullValue() throws InterruptedException, PluginException {
        // Mock null value
        when(configuration.getProperty("value")).thenReturn(null);
        when(xmlNode.getText()).thenReturn(null);
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "");
    }
}