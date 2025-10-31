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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for ValueOfPlugin.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class ValueOfPluginTest {

    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private DynamicScopeContext context;
    
    private ValueOfPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new ValueOfPlugin();
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "value-of");
        assertEquals(metadata.getName(), "ValueOf Plugin");
        assertEquals(metadata.getVersion(), "2.1.0");
        assertEquals(metadata.getDescription(), "ValueOf plugin that evaluates expressions and returns their values");
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), expectedCategory(ValueOfPlugin.class));
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "value-of");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertFalse(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 1);
        assertEquals(schema.getOptionalAttributes().length, 0);
        
        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 1);
        
        // Check expr attribute
        PluginConfigurationSchema.AttributeDefinition exprAttr = schema.getAttribute("expr");
        assertNotNull(exprAttr);
        assertEquals(exprAttr.getName(), "expr");
        assertEquals(exprAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertTrue(exprAttr.isRequired());
        assertNull(exprAttr.getDefaultValue());
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
        when(configuration.getProperty("expr")).thenReturn("test expression");
        
        plugin.validateConfiguration(configuration);
        
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithMissingExpression() throws PluginValidationException {
        when(configuration.getProperty("expr")).thenReturn(null);
        
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyExpression() throws PluginValidationException {
        when(configuration.getProperty("expr")).thenReturn("");
        
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithWhitespaceExpression() throws PluginValidationException {
        when(configuration.getProperty("expr")).thenReturn("   ");
        
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testExecuteWithValidExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("test expression");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on BaseTemplater.evaluateToVariable implementation
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithNullExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        plugin.execute(context);
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-valueof");
        when(configuration.getProperty("expr")).thenReturn("expression with ID");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("multiple executions");
        
        plugin.initialize(configuration);
        
        // Execute multiple times
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
    }

    @Test
    public void testExecuteWithComplexExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("${variable1} + ${variable2}");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNumericExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("42");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithBooleanExpression() throws InterruptedException, PluginException {
        when(configuration.getProperty("expr")).thenReturn("true");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }
}
