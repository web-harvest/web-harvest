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
 * Test for RegexpSourcePlugin
 */
public class RegexpSourcePluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private RegexpSourcePlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new RegexpSourcePlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        // v2.2.x: elementDef removed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test regexp source value");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty("id")).thenReturn(null); // Optional attribute
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Regexp Source Plugin");
        assertEquals(metadata.getDescription(), "Regexp source plugin that provides regexp source functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "regexp-source");
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
    public void testRegexpSourcePluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        RegexpSourcePlugin newPlugin = new RegexpSourcePlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("sourceId");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testRegexpSourcePluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        RegexpSourcePlugin newPlugin = new RegexpSourcePlugin();
        
        // Test initialization with null configuration
        newPlugin.initialize(null);
    }

    @Test
    public void testRegexpSourcePluginExecute() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result - RegexpSourcePlugin returns EmptyVariable.INSTANCE
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testRegexpSourcePluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        RegexpSourcePlugin testPlugin = new RegexpSourcePlugin() {
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
    public void testRegexpSourcePluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testRegexpSourcePluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration
        plugin.validateConfiguration(null);
    }

    @Test
    public void testRegexpSourcePluginWithIdAttribute() throws PluginException, InterruptedException {
        // Create a new plugin instance for this test
        RegexpSourcePlugin newPlugin = new RegexpSourcePlugin();
        
        // Mock configuration with 'id' attribute
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("sourceId");
        
        // Initialize plugin with mocked configuration
        newPlugin.initialize(config);
        
        // Execute plugin
        Variable result = newPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
}
