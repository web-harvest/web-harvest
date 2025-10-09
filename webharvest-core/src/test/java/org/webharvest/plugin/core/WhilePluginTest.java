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
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.IElementDef;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class WhilePluginTest {

    @Mock
    private PluginConfiguration configuration;

    @Mock
    private DynamicScopeContext context;

    @Mock
    private IElementDef elementDef;

    private WhilePlugin plugin;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        plugin = new WhilePlugin();
        
        // Mock elementDef
        when(elementDef.hasOperations()).thenReturn(false);
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        
        // Mock context
        when(context.getCharset()).thenReturn("UTF-8");
        doNothing().when(context).setLocalVar(anyString(), any());
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "while");
        assertEquals(plugin.getMetadata().getName(), "While Plugin");
    }

    @Test
    public void testGetConfigurationSchema() {
        assertNotNull(plugin.getConfigurationSchema());
        assertTrue(plugin.getConfigurationSchema().allowsBody());
        assertEquals(plugin.getConfigurationSchema().getRequiredAttributes().length, 1);
    }

    @Test
    public void testExecuteWithEmptyCondition() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("");
        when(configuration.getProperty("condition", "")).thenReturn("");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithFalseCondition() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithMaxloops() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("true");
        when(configuration.getProperty("condition", "")).thenReturn("true");
        when(configuration.getProperty("maxloops", "")).thenReturn("2");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithIndexVariable() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("i");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithEmptyResult() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("empty", "false")).thenReturn("true");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithFilter() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("filter", "")).thenReturn("test");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteMultipleTimes() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        // Then
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithInvalidMaxloops() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("true");
        when(configuration.getProperty("condition", "")).thenReturn("true");
        when(configuration.getProperty("maxloops", "")).thenReturn("invalid");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then - WhilePlugin handles invalid maxloops gracefully by using default value
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNullConfiguration() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("");
        when(configuration.getProperty("condition", "")).thenReturn("");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithWhitespaceCondition() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("   ");
        when(configuration.getProperty("condition", "")).thenReturn("   ");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithSpecialCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("i");
        when(configuration.getProperty("filter", "")).thenReturn("test@#$%");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithUnicodeCharacters() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("索引");
        when(configuration.getProperty("filter", "")).thenReturn("测试");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithLongValues() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("very_long_variable_name_that_might_cause_issues");
        when(configuration.getProperty("filter", "")).thenReturn("very_long_filter_value_that_might_cause_issues_with_processing");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNumericValues() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("maxloops", "")).thenReturn("50");
        when(configuration.getProperty("index", "")).thenReturn("counter");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithMixedCaseValues() throws PluginException, InterruptedException {
        // Given
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("Index");
        when(configuration.getProperty("filter", "")).thenReturn("Filter");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithCounterVariable() throws PluginException, InterruptedException {
        // Given - condition that uses 'counter' variable
        when(configuration.getProperty("test", "")).thenReturn("${counter < 3}");
        when(configuration.getProperty("condition", "")).thenReturn("${counter < 3}");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("5");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        // Mock counter variable
        when(context.getVar("counter")).thenReturn(new NodeVariable("0"))
            .thenReturn(new NodeVariable("1"))
            .thenReturn(new NodeVariable("2"))
            .thenReturn(new NodeVariable("3"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof ListVariable || result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithInvalidCounterFormat() throws PluginException, InterruptedException {
        // Given - condition with counter, but counter is not a number
        when(configuration.getProperty("test", "")).thenReturn("${counter < 3}");
        when(configuration.getProperty("condition", "")).thenReturn("${counter < 3}");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("2");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        // Mock counter with invalid format
        when(context.getVar("counter")).thenReturn(new NodeVariable("not-a-number"));
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof ListVariable || result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithConditionEvaluationError() throws PluginException, InterruptedException {
        // Given - invalid condition that will throw exception during re-evaluation
        when(configuration.getProperty("test", "")).thenReturn("${invalid.syntax");
        when(configuration.getProperty("condition", "")).thenReturn("${invalid.syntax");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("2");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteWithNonBooleanResult() throws PluginException, InterruptedException {
        // Given - condition that returns non-boolean
        when(configuration.getProperty("test", "")).thenReturn("'yes'");
        when(configuration.getProperty("condition", "")).thenReturn("'yes'");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("1");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        // When
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof ListVariable || result instanceof EmptyVariable);
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        
        String[] dependencies = metadata.getDependencies();
        assertNotNull(dependencies);
    }

    @Test
    public void testSchemaAllowsBody() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertTrue(schema.allowsBody());
    }

    @Test
    public void testExecuteWithMaxLoopsZero() throws Exception {
        when(configuration.getProperty("test", "")).thenReturn("true");
        when(configuration.getProperty("condition", "")).thenReturn("true");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("0");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithMaxLoopsNegative() throws Exception {
        when(configuration.getProperty("test", "")).thenReturn("true");
        when(configuration.getProperty("condition", "")).thenReturn("true");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("-1");
        when(configuration.getProperty("empty", "false")).thenReturn("false");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testExecuteWithEmptyTrue() throws Exception {
        when(configuration.getProperty("test", "")).thenReturn("false");
        when(configuration.getProperty("condition", "")).thenReturn("false");
        when(configuration.getProperty("index", "")).thenReturn("idx");
        when(configuration.getProperty("filter", "")).thenReturn("");
        when(configuration.getProperty("maxloops", "")).thenReturn("10");
        when(configuration.getProperty("empty", "false")).thenReturn("true");
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
    }
}
