/*
 Copyright (c) 2006-2025 the original author or authors.
 */

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
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.XmlNode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for BodyPlugin
 */
public class BodyPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private BodyPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new BodyPlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("Test body content");
        when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty("id")).thenReturn(null); // Optional attribute
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Body Plugin");
        assertEquals(metadata.getDescription(), "Body plugin that provides body functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "body");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        // Check 'id' attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testBodyPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        BodyPlugin newPlugin = new BodyPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("bodyId");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testBodyPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        BodyPlugin newPlugin = new BodyPlugin();
        
        // Test initialization with null configuration
        newPlugin.initialize(null);
    }

    @Test
    public void testBodyPluginExecute() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns a specific result from doExecute
        BodyPlugin testPlugin = new BodyPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                // Mock the super.executeBody call
                NodeVariable bodyResult = new NodeVariable("body content");
                return bodyResult;
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result - BodyPlugin should return the result of executeBody
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "body content");
    }

    @Test
    public void testBodyPluginExecuteWithEmptyBody() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns null from doExecute
        BodyPlugin testPlugin = new BodyPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                // Mock the super.executeBody call to return null
                return null;
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result - should return null when body is empty
        assertNull(result);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testBodyPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        BodyPlugin testPlugin = new BodyPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        testPlugin.execute(context);
    }

    @Test
    public void testBodyPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testBodyPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration
        plugin.validateConfiguration(null);
    }

    @Test
    public void testBodyPluginWithIdAttribute() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        BodyPlugin newPlugin = new BodyPlugin();
        
        // Mock configuration with 'id' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("bodyId");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Create a plugin that returns a specific result from doExecute
        BodyPlugin testPlugin = new BodyPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                // Mock the super.executeBody call
                NodeVariable bodyResult = new NodeVariable("body content");
                return bodyResult;
            }
        };
        
        // Initialize the plugin
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "body content");
    }
}
