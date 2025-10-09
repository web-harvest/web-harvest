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
import org.webharvest.definition.IElementDef;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class VarPluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    @Mock
    private IElementDef elementDef;

    private VarPlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        plugin = new VarPlugin();
        
        // Mock elementDef
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        // Mock context
        doNothing().when(context).setLocalVar(anyString(), any());
    }

    @Test
    public void testGetMetadata() {
        VarPlugin testPlugin = new VarPlugin();
        assertNotNull(testPlugin.getMetadata());
        assertEquals(testPlugin.getMetadata().getId(), "var");
    }

    @Test
    public void testExecuteWithExistingVariable() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("myValue"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "myValue");
    }

    @Test
    public void testExecuteWithNonExistentVariable() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithEmptyVariable() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(EmptyVariable.INSTANCE);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithEmptyVariableName() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullVariableName() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithVariableContainingDot() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("user.name");
        when(context.getVar("user.name")).thenReturn(new NodeVariable("John"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "John");
    }

    @Test
    public void testExecuteWithVariableContainingHyphen() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("user-name");
        when(context.getVar("user-name")).thenReturn(new NodeVariable("John"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "John");
    }

    @Test
    public void testExecuteWithVariableContainingUnderscore() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("user_name");
        when(context.getVar("user_name")).thenReturn(new NodeVariable("John"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "John");
    }

    @Test
    public void testExecuteWithVariableContainingNumbers() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("user123");
        when(context.getVar("user123")).thenReturn(new NodeVariable("John"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "John");
    }

    @Test
    public void testExecuteWithComplexValue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("Complex Value with ${variable}"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Complex Value with ${variable}");
    }

    @Test
    public void testExecuteWithMultilineValue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("Line1\nLine2\nLine3"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Line1\nLine2\nLine3");
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("Special <>&\"'"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Special <>&\"'");
    }

    @Test
    public void testExecuteWithUnicode() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("Unicode: \u00E9\u00E8\u00EA"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "Unicode: \u00E9\u00E8\u00EA");
    }

    @Test
    public void testExecuteWithMultipleExecutions() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("myValue"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof NodeVariable);
        assertTrue(result2 instanceof NodeVariable);
        assertTrue(result3 instanceof NodeVariable);
        assertEquals(result1.toString(), "myValue");
        assertEquals(result2.toString(), "myValue");
        assertEquals(result3.toString(), "myValue");
    }

    @Test
    public void testExecuteWithVariableReassignment() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("value1"))
            .thenReturn(new NodeVariable("value2"))
            .thenReturn(new NodeVariable("value3"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof NodeVariable);
        assertTrue(result2 instanceof NodeVariable);
        assertTrue(result3 instanceof NodeVariable);
    }

    @Test
    public void testExecuteWithJSONValue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("{\"key\":\"value\"}"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "{\"key\":\"value\"}");
    }

    @Test
    public void testExecuteWithXMLValue() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("myVar");
        when(context.getVar("myVar")).thenReturn(new NodeVariable("<root><child>value</child></root>"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "<root><child>value</child></root>");
    }

    @Test
    public void testExecuteWithHyphenatedVariableName() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("my-var-name");
        when(context.getVar("my-var-name")).thenReturn(new NodeVariable("value"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "value");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testVarPluginValidateWithEmptyName() throws PluginValidationException {
        // Given
        PluginMetadata metadata = new VarPlugin().getMetadata();
        PluginConfigurationSchema schema = metadata.getConfigurationSchema();
        
        PluginConfiguration emptyConfig = mock(PluginConfiguration.class);
        when(emptyConfig.getProperty("name", "")).thenReturn("");
        
        // When
        schema.validate(emptyConfig);
        
        // Then - should throw PluginValidationException
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertNotNull(metadata.getCategory());
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
        
        for (PluginConfigurationSchema.AttributeDefinition attr : attrs) {
            assertNotNull(attr.getName());
            assertNotNull(attr.getType());
        }
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
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
    public void testExecuteReturnsEmptyVariableWhenNameIsEmpty() throws Exception {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then - should return EmptyVariable when name is empty
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullVariableInContext() throws Exception {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("nullVar");
        when(context.getVar("nullVar")).thenReturn(null);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyVariableInContext() throws Exception {
        // Given
        when(configuration.getProperty("name", "")).thenReturn("emptyVar");
        when(context.getVar("emptyVar")).thenReturn(EmptyVariable.INSTANCE);
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithNumericVarName() throws Exception {
        when(configuration.getProperty("name", "")).thenReturn("var123");
        when(context.getVar("var123")).thenReturn(new NodeVariable("value123"));
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertEquals(result.toString(), "value123");
    }

    @Test
    public void testExecuteMultipleCalls() throws Exception {
        when(configuration.getProperty("name", "")).thenReturn("testVar");
        when(context.getVar("testVar")).thenReturn(new NodeVariable("testValue"));
        
        plugin.initialize(configuration);
        
        for (int i = 0; i < 5; i++) {
            Variable result = plugin.execute(context);
            assertNotNull(result);
        }
    }
}


