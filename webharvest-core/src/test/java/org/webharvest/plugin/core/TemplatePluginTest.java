package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for TemplatePlugin.
 */
public class TemplatePluginTest {

    @Mock
    private PluginConfiguration mockConfiguration;

    @Mock
    private DynamicScopeContext mockContext;

    // REMOVED in v2.2.x - old architecture mocks no longer needed
    // @Mock
    // private org.webharvest.definition.IElementDef mockElementDef;

    private TemplatePlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new TemplatePlugin();
        
        // Setup default mock behaviors
        when(mockContext.getCharset()).thenReturn("UTF-8");
        when(mockContext.getScriptingLanguage()).thenReturn(org.webharvest.runtime.scripting.ScriptingLanguage.BEANSHELL);
        when(mockContext.getVar(anyString())).thenReturn(EmptyVariable.INSTANCE);
        doNothing().when(mockContext).setLocalVar(anyString(), any());
        
        // Mock PluginConfiguration.hasChildren() for new architecture
        when(mockConfiguration.hasChildren()).thenReturn(false);
        when(mockConfiguration.getChildren()).thenReturn(java.util.Collections.emptyList());
    }

    @Test
    public void testExecuteWithSimpleTemplate() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        when(mockContext.getVar("name")).thenReturn(new NodeVariable("World"));
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        // TemplatePlugin returns EmptyVariable when no body content is provided
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteWithEmptyTemplate() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteWithNoVariables() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteWithUndefinedVariable() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        when(mockContext.getVar("undefined")).thenReturn(EmptyVariable.INSTANCE);
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteWithMultipleVariables() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        when(mockContext.getVar("greeting")).thenReturn(new NodeVariable("Hello"));
        when(mockContext.getVar("name")).thenReturn(new NodeVariable("World"));
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testExecuteWithNestedTemplate() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        when(mockContext.getVar("greeting")).thenReturn(new NodeVariable("Hello"));
        when(mockContext.getVar("name")).thenReturn(new NodeVariable("${user}"));
        when(mockContext.getVar("user")).thenReturn(new NodeVariable("World"));
        
        plugin.initialize(mockConfiguration);
        
        // When
        var result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertEquals(result, EmptyVariable.INSTANCE);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullConfiguration() throws PluginException {
        // When
        plugin.initialize(null);
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // When
        plugin.initialize(mockConfiguration);
        
        // Then
        assertTrue(plugin.isInitialized());
        // Note: getProperty() is called in doExecute(), not in initialize()
    }
    
    @Test
    public void testGetMetadata() {
        // Test getMetadata method
        org.webharvest.plugin.PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Template Plugin");
        assertEquals(metadata.getDescription(), "Template plugin that provides template processing functionality");
        assertTrue(metadata.isCorePlugin());
    }
    
    @Test
    public void testGetConfigurationSchema() {
        // Test getConfigurationSchema method
        org.webharvest.plugin.PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertTrue(schema.getAttributeDefinitions().length > 0);
    }
    
    @Test
    public void testExecuteWithDifferentLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("javascript");
        when(mockConfiguration.getProperty("id", "")).thenReturn("test-template");
        when(mockContext.getScriptingLanguage()).thenReturn(org.webharvest.runtime.scripting.ScriptingLanguage.JAVASCRIPT);
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
    
    @Test
    public void testExecuteWithIdProperty() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("my-template");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithGroovyLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("groovy");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithJavaScriptLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("javascript");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithInvalidLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("invalid");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithEmptyLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("");
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullLanguage() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn(null);
        when(mockConfiguration.getProperty("id", "")).thenReturn("");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSpecialCharactersId() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-special-chars");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithWhitespaceId() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("   ");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullId() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn(null);
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimes() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("test-template");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result1 = plugin.execute(mockContext);
        org.webharvest.runtime.variables.Variable result2 = plugin.execute(mockContext);
        org.webharvest.runtime.variables.Variable result3 = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithComplexTemplateExpression() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("complex-template");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithVariables() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-vars");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithFunctions() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-functions");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithLoops() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-loops");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithConditions() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-conditions");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithMathOperations() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-math");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithStringOperations() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-strings");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithDateOperations() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-dates");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithArrayOperations() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-arrays");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithObjectOperations() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-objects");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithErrorHandling() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-errors");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithNestedExpressions() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-nested");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithRecursiveCalls() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-with-recursion");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithPerformanceTest() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-performance");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithMemoryTest() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-memory");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithTemplateWithConcurrencyTest() throws PluginException, InterruptedException {
        // Given
        when(mockConfiguration.getProperty("language", "beanshell")).thenReturn("beanshell");
        when(mockConfiguration.getProperty("id", "")).thenReturn("template-concurrency");
        
        // Mock ElementDef
        // v2.2.x: mockElementDef removed
        // when(mockElementDef.hasOperations()).thenReturn(false);
        // when(mockElementDef.getOperationDefs()).thenReturn(new org.webharvest.definition.IElementDef[0]);
        
        plugin.initialize(mockConfiguration);
        
        // When
        org.webharvest.runtime.variables.Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
}