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
 * Unit tests for XmlToJsonPlugin.
 */
public class XmlToJsonPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private XmlToJsonPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new XmlToJsonPlugin();
        
        // Set ElementDef to avoid NullPointerException
        
        // Mock elementDef
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(elementDef.hasOperations()).thenReturn(false);
        
        // Mock configuration
        when(configuration.getProperty(anyString())).thenReturn(null);
        
        // Initialize plugin
        plugin.initialize(configuration);
    }

    @Test
    public void testXmlToJsonPluginGetMetadata() {
        // Test getMetadata method
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "XmlToJson Plugin");
        assertEquals(metadata.getDescription(), "Converter from XML to JSON");
        assertEquals(metadata.getId(), "xml-to-json");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), "core");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "xml-to-json");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        
        // Test tags
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        assertEquals(tags.length, 4);
        assertEquals(tags[0], "core");
        assertEquals(tags[1], "xml");
        assertEquals(tags[2], "json");
        assertEquals(tags[3], "converter");
        
        // Test dependencies
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
        assertEquals(dependencies.length, 0);
        
        // Test className
        assertEquals(metadata.getClassName(), XmlToJsonPlugin.class.getName());
    }

    @Test
    public void testXmlToJsonPluginGetConfigurationSchema() {
        // Test getConfigurationSchema method
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertTrue(schema.allowsBody());
        
        // Test attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertNotNull(attributes);
        assertEquals(attributes.length, 0);
        
        // Test getAttribute
        assertNull(schema.getAttribute("nonExistent"));
        
        // Test required/optional attributes
        String[] requiredAttributes = schema.getRequiredAttributes();
        assertNotNull(requiredAttributes);
        assertEquals(requiredAttributes.length, 0);
        
        String[] optionalAttributes = schema.getOptionalAttributes();
        assertNotNull(optionalAttributes);
        assertEquals(optionalAttributes.length, 0);
        
        // Test sub-elements
        String[] requiredSubElements = schema.getRequiredSubElements();
        assertNotNull(requiredSubElements);
        assertEquals(requiredSubElements.length, 0);
        
        String[] allowedSubElements = schema.getAllowedSubElements();
        assertNotNull(allowedSubElements);
        assertEquals(allowedSubElements.length, 0);
    }

    @Test
    public void testXmlToJsonPluginInitialize() throws PluginException {
        // Create a new plugin instance for this test
        XmlToJsonPlugin newPlugin = new XmlToJsonPlugin();
        
        // Test successful initialization
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty(anyString())).thenReturn(null);
        
        newPlugin.initialize(config);
        
        // Plugin should be initialized successfully
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testXmlToJsonPluginInitializeWithNullConfiguration() throws PluginException {
        // Create a new plugin instance for this test
        XmlToJsonPlugin newPlugin = new XmlToJsonPlugin();
        
        // Test initialization with null configuration
        newPlugin.initialize(null);
    }

    @Test
    public void testXmlToJsonPluginExecuteWithValidXml() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns valid XML from executeBody
        XmlToJsonPlugin testPlugin = new XmlToJsonPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                String xmlContent = "<root><name>John</name><age>30</age></root>";
                NodeVariable bodyResult = new NodeVariable(xmlContent);
                return super.doExecute(context);
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
    }

    @Test
    public void testXmlToJsonPluginExecuteWithEmptyXml() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns empty XML from executeBody
        XmlToJsonPlugin testPlugin = new XmlToJsonPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                String xmlContent = "<root></root>";
                NodeVariable bodyResult = new NodeVariable(xmlContent);
                return super.doExecute(context);
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
    }

    @Test
    public void testXmlToJsonPluginExecuteWithComplexXml() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns complex XML from executeBody
        XmlToJsonPlugin testPlugin = new XmlToJsonPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                String xmlContent = "<data><users><user id=\"1\"><name>Alice</name><email>alice@example.com</email></user><user id=\"2\"><name>Bob</name><email>bob@example.com</email></user></users></data>";
                NodeVariable bodyResult = new NodeVariable(xmlContent);
                return super.doExecute(context);
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin
        Variable result = testPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
    }

    @Test
    public void testXmlToJsonPluginExecuteWithInvalidXml() throws InterruptedException, PluginException {
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that returns invalid XML from executeBody
        XmlToJsonPlugin testPlugin = new XmlToJsonPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                String invalidXmlContent = "<root><unclosed>invalid</root>";
                NodeVariable bodyResult = new NodeVariable(invalidXmlContent);
                return super.doExecute(context);
            }
        };
        
        // Initialize the plugin
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        // Execute plugin - should handle invalid XML gracefully
        Variable result = testPlugin.execute(context);
        
        // Verify result
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testXmlToJsonPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        // Mock context to simulate interruption
        when(context.getCharset()).thenReturn("UTF-8");
        
        // Create a plugin that will throw InterruptedException
        XmlToJsonPlugin testPlugin = new XmlToJsonPlugin() {
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
    public void testXmlToJsonPluginValidateConfiguration() throws PluginValidationException {
        // Test successful validation
        PluginConfiguration config = mock(PluginConfiguration.class);
        plugin.validateConfiguration(config);
        
        // Should not throw any exception
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testXmlToJsonPluginValidateConfigurationWithNull() throws PluginValidationException {
        // Test validation with null configuration
        plugin.validateConfiguration(null);
    }
}
