/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.CorePluginDef;
// import org.webharvest.definition.IElementDef;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.XmlNode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for CallParamPlugin
 */
public class CallParamPluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private CallParamPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new CallParamPlugin();
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test param value");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration with required 'name' attribute
        when(configuration.getProperty("name")).thenReturn("testParam");
        when(configuration.getProperty("id")).thenReturn(null); // Optional attribute
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "CallParam Plugin");
        assertEquals(metadata.getDescription(), "CallParam plugin that provides function call parameter functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "call-param");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 2);
        
        // Check 'id' attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        // Check 'name' attribute
        PluginConfigurationSchema.AttributeDefinition nameAttr = schema.getAttribute("name");
        assertNotNull(nameAttr);
        assertEquals(nameAttr.getName(), "name");
        assertTrue(nameAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testCallParamPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        CallParamPlugin newPlugin = new CallParamPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("testParam");
        when(config.getProperty("id")).thenReturn("paramId");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCallParamPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        CallParamPlugin newPlugin = new CallParamPlugin();
        
        // Test initialization with null configuration
        newPlugin.initialize(null);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCallParamPluginInitializeWithMissingName() throws PluginException {
        // Create a new plugin instance for this test
        CallParamPlugin newPlugin = new CallParamPlugin();
        
        // Test initialization with missing required 'name' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn(null);
        
        newPlugin.initialize(config);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testCallParamPluginInitializeWithEmptyName() throws PluginException {
        // Create a new plugin instance for this test
        CallParamPlugin newPlugin = new CallParamPlugin();
        
        // Test initialization with empty 'name' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("   "); // Empty/whitespace name
        
        newPlugin.initialize(config);
    }

    @Test
    public void testCallParamPluginExecute() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result - CallParamPlugin returns EmptyVariable.INSTANCE
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testCallParamPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        CallParamPlugin testPlugin = new CallParamPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("testParam");
        testPlugin.initialize(config);
        
        // Execute plugin
        testPlugin.execute(context);
    }

    @Test
    public void testCallParamPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("testParam");
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testCallParamPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testCallParamPluginValidateConfigurationWithMissingName() throws PluginValidationException {
        // Test validation with missing required 'name' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn(null);
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testCallParamPluginValidateConfigurationWithEmptyName() throws PluginValidationException {
        // Test validation with empty 'name' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("");
        plugin.validateConfiguration(config);
    }

    @Test
    public void testCallParamPluginWithIdAttribute() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        CallParamPlugin newPlugin = new CallParamPlugin();
        
        // Mock configuration with both 'name' and 'id' attributes
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("name")).thenReturn("testParam");
        when(config.getProperty("id")).thenReturn("paramId");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
}


