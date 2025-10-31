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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.XmlNode;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for ListPlugin.
 */
public class ListPluginTest {

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private CorePluginDef elementDef;
    // @Mock
    // private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private ListPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new ListPlugin();
        
        // v2.2.x: elementDef/xmlNode removed - no longer needed
        // when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        // when(elementDef.getNode()).thenReturn(xmlNode);
        // when(xmlNode.getText()).thenReturn("Test list content");
        // when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty("id")).thenReturn("testList");
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testListPluginGetMetadata() {
        // Test getMetadata method
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "List Plugin");
        assertEquals(metadata.getDescription(), "List plugin that provides list functionality");
        assertEquals(metadata.getId(), "list");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), expectedCategory(ListPlugin.class));
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "list");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        assertEquals(metadata.getClassName(), ListPlugin.class.getName());
        
        // Test tags
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        assertEquals(tags.length, 4);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "list");
        assertEquals(tags[2], "collection");
        assertEquals(tags[3], "variable");
        
        // Test dependencies
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
        assertEquals(dependencies.length, 0);
    }

    @Test
    public void testListPluginGetConfigurationSchema() {
        // Test getConfigurationSchema method
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        
        // Test attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertNotNull(attributes);
        assertEquals(attributes.length, 1);
        
        PluginConfigurationSchema.AttributeDefinition idAttr = attributes[0];
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getDescription(), "Optional identifier for the list element");
        assertFalse(idAttr.isRequired());
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        
        // Test sub-elements
        String[] requiredSubElements = schema.getRequiredSubElements();
        assertNotNull(requiredSubElements);
        assertEquals(requiredSubElements.length, 0);
        
        String[] allowedSubElements = schema.getAllowedSubElements();
        assertNotNull(allowedSubElements);
        assertEquals(allowedSubElements.length, 0);
        
        // Test attributes
        String[] optionalAttributes = schema.getOptionalAttributes();
        assertNotNull(optionalAttributes);
        assertEquals(optionalAttributes.length, 1);
        assertEquals(optionalAttributes[0], "id");
        
        String[] requiredAttributes = schema.getRequiredAttributes();
        assertNotNull(requiredAttributes);
        assertEquals(requiredAttributes.length, 0);
    }

    @Test
    public void testListPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        ListPlugin newPlugin = new ListPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("testList");
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testListPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        ListPlugin newPlugin = new ListPlugin();
        
        // Test initialization with null configuration should throw exception
        newPlugin.initialize(null);
    }

    @Test
    public void testListPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testListPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration should throw exception
        plugin.validateConfiguration(null);
    }

    @Test
    public void testListPluginExecute() throws InterruptedException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Verify result is not null
        assertNotNull(result);
        
        // Verify that variable was set in context
        verify(context).setLocalVar(eq("listResult"), any(Variable.class));
    }

    @Test
    public void testListPluginExecuteWithException() throws InterruptedException {
        // Mock context to throw exception
        when(context.getCharset()).thenThrow(new RuntimeException("Test exception"));
        
        // Execute plugin
        Variable result = plugin.execute(context);
        
        // Should return EmptyVariable on exception
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testListPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        ListPlugin testPlugin = new ListPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin - should throw RuntimeException wrapping InterruptedException
        testPlugin.execute(context);
    }

    @Test
    public void testListPluginConfigurationSchemaValidation() throws PluginValidationException {
        // Test configuration schema validation
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        PluginConfiguration config = mock(PluginConfiguration.class);
        
        // Should not throw exception for valid configuration
        schema.validate(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testListPluginConfigurationSchemaValidationWithNull() throws PluginValidationException {
        // Test configuration schema validation with null configuration
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        // Should throw exception for null configuration
        schema.validate(null);
    }
}
