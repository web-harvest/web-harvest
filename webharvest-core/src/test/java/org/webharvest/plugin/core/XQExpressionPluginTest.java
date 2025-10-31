package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for XQExpressionPlugin.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class XQExpressionPluginTest {

    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private DynamicScopeContext context;
    
    private XQExpressionPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new XQExpressionPlugin();
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "xq-expression");
        assertEquals(metadata.getName(), "XQExpression Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "XQExpression plugin that provides XQuery expression evaluation functionality");
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), expectedCategory(XQExpressionPlugin.class));
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "xq-expression");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 2);
        assertEquals(schema.getAllowedSubElements().length, 2);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes().length, 1);
        
        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 1);
        
        // Check id attribute
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertEquals(idAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(idAttr.isRequired());
        assertNull(idAttr.getDefaultValue());
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullConfiguration() throws PluginException {
        plugin.initialize(null);
    }

    @Test
    public void testValidateConfigurationWithValidConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(configuration);
        
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecuteWithValidConfiguration() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-xqexpression");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        // Results depend on executeBody implementation
    }

    @Test
    public void testExecuteWithEmptyBody() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithComplexConfiguration() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("complex-xqexpression");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithNullConfiguration() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xqexpr-with-special@#$");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithUnicodeCharacters() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xqexpr-测试");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithLongId() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("very-long-xqexpression-id-with-many-characters-to-test-system");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteMultipleTimesInSequence() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("xqexpr-test");
        
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        // Results depend on executeBody implementation
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertNotNull(metadata.getCategory());
        assertEquals(metadata.getCategory(), expectedCategory(XQExpressionPlugin.class));
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        assertTrue(tags.length > 0);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testSchemaAttributeDefinitions() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        assertNotNull(attrs);
        assertTrue(attrs.length >= 1);
        
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            assertNotNull(attr.getName());
            assertNotNull(attr.getType());
        }
    }

    @Test
    public void testSchemaOptionalAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] optionalAttrs = schema.getOptionalAttributes();
        
        assertNotNull(optionalAttrs);
    }

    @Test
    public void testSchemaRequiredAttributes() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        String[] requiredAttrs = schema.getRequiredAttributes();
        
        assertNotNull(requiredAttrs);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testAttributeDefinitionProperties() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        PluginConfigurationSchema.AttributeDefinition[] attrs = schema.getAttributeDefinitions();
        
        boolean hasIdAttr = false;
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            if ("id".equals(attr.getName())) {
                hasIdAttr = true;
                // Id attribute may or may not be required
                assertNotNull(attr.getType());
            }
        }
        assertTrue(hasIdAttr);
    }

    @Test
    public void testExecuteWithDifferentIds() throws Exception {
        XQExpressionPlugin newPlugin = new XQExpressionPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("expr_123");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNumericId() throws Exception {
        XQExpressionPlugin newPlugin = new XQExpressionPlugin();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("999");
        
        newPlugin.initialize(config);
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }
}
