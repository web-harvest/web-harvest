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
 * Test for BodyProcessorPlugin
 */
public class BodyProcessorPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private BodyProcessorPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new BodyProcessorPlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("Test body processor value");
        when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        // BodyProcessorPlugin doesn't have specific configuration properties
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "body-processor");
        assertEquals(metadata.getDescription(), "Plugin which executes only body and returns variables list");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "body-processor");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 0);
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "1.0.0");
    }

    @Test
    public void testBodyProcessorPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        BodyProcessorPlugin newPlugin = new BodyProcessorPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testBodyProcessorPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        BodyProcessorPlugin newPlugin = new BodyProcessorPlugin();
        
        // Test initialization with null configuration
        newPlugin.initialize(null);
    }

    @Test
    public void testBodyProcessorPluginExecute() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns a specific result from doExecute
        BodyProcessorPlugin testPlugin = new BodyProcessorPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                // Mock the super.executeBody call
                NodeVariable bodyResult = new NodeVariable("body processor content");
                return bodyResult;
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result - BodyProcessorPlugin should return the result of executeBody
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "body processor content");
    }

    @Test
    public void testBodyProcessorPluginExecuteWithEmptyBody() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns null from doExecute
        BodyProcessorPlugin testPlugin = new BodyProcessorPlugin() {
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
    public void testBodyProcessorPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        BodyProcessorPlugin testPlugin = new BodyProcessorPlugin() {
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
    public void testBodyProcessorPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testBodyProcessorPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration
        plugin.validateConfiguration(null);
    }
}
