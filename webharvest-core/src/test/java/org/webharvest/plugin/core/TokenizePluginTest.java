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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;
import static org.webharvest.plugin.core.MetadataTestUtils.expectedCategory;

/**
 * Unit tests for TokenizePlugin.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class TokenizePluginTest {

    @Mock
    private PluginConfiguration configuration;
    
    @Mock
    private DynamicScopeContext context;
    
    private TokenizePlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        plugin = new TokenizePlugin();
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "tokenize");
        assertEquals(metadata.getName(), "Tokenize");
        assertEquals(metadata.getVersion(), "2.3.0");
        assertNotNull(metadata.getDescription()); assertTrue(metadata.getDescription().contains("token") || metadata.getDescription().contains("Token"));
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertEquals(metadata.getCategory(), expectedCategory(TokenizePlugin.class));
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "tokenize");
        assertEquals(plugin.getMetadata().getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.3.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 0);
        assertEquals(schema.getOptionalAttributes().length, 3);
        
        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 3);
        
        // Check delimiters attribute
        PluginConfigurationSchema.AttributeDefinition delimitersAttr = schema.getAttribute("delimiters");
        assertNotNull(delimitersAttr);
        assertEquals(delimitersAttr.getName(), "delimiters");
        assertEquals(delimitersAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertFalse(delimitersAttr.isRequired());
        assertEquals(delimitersAttr.getDefaultValue(), "\\n\\r");
        
        // Check trimtokens attribute
        PluginConfigurationSchema.AttributeDefinition trimTokensAttr = schema.getAttribute("trimtokens");
        assertNotNull(trimTokensAttr);
        assertEquals(trimTokensAttr.getName(), "trimtokens");
        assertEquals(trimTokensAttr.getType(), PluginConfigurationSchema.AttributeType.BOOLEAN);
        assertFalse(trimTokensAttr.isRequired());
        assertEquals(trimTokensAttr.getDefaultValue(), "true");
        
        // Check allowemptytokens attribute
        PluginConfigurationSchema.AttributeDefinition allowEmptyTokensAttr = schema.getAttribute("allowemptytokens");
        assertNotNull(allowEmptyTokensAttr);
        assertEquals(allowEmptyTokensAttr.getName(), "allowemptytokens");
        assertEquals(allowEmptyTokensAttr.getType(), PluginConfigurationSchema.AttributeType.BOOLEAN);
        assertFalse(allowEmptyTokensAttr.isRequired());
        assertEquals(allowEmptyTokensAttr.getDefaultValue(), "false");
    }

    @Test
    public void testInitializeWithValidConfiguration() throws PluginException {
        plugin.initialize(configuration);
        
        assertTrue(plugin.isInitialized(), "Valid configuration should initialize the plugin");
    }

    @Test(expectedExceptions = PluginException.class)
    public void testInitializeWithNullConfiguration() throws PluginException {
        plugin.initialize(null);
    }

    @Test
    public void testValidateConfigurationWithValidConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(configuration);
        
        // Successful return confirms the valid configuration is accepted.
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testExecuteWithDefaultDelimiters() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithCustomDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithTrimTokensDisabled() throws InterruptedException, PluginException {
        when(configuration.getProperty("trimtokens")).thenReturn("false");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithAllowEmptyTokens() throws InterruptedException, PluginException {
        when(configuration.getProperty("allowemptytokens")).thenReturn("true");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithEmptyBody() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-tokenize");
        
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
    public void testExecuteWithComplexDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("|;,");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithWhitespaceDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(" \t\n");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithSpecialCharacterDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("@#$%");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithUnicodeDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("，。；");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithSingleCharDelimiter() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("|");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithVeryLongDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("||||||||||||||||||||||||||||");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteMultipleSequential() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        
        plugin.initialize(configuration);
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        // Results depend on executeBody implementation
    }

    @Test
    public void testExecuteWithNumericDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("123");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithMixedDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("a1,b2;c3");
        
        plugin.initialize(configuration);
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        // Result depends on executeBody implementation
    }

    @Test
    public void testExecuteWithTrimTokensTrue() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(true);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }

    @Test
    public void testExecuteWithTrimTokensFalse() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(false);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }

    @Test
    public void testExecuteWithAllowEmptyTokensTrue() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(true);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(true);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }

    @Test
    public void testExecuteWithAllowEmptyTokensFalse() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(",");
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(true);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }

    @Test
    public void testExecuteWithNullDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn(null);
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(true);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }

    @Test
    public void testExecuteWithEmptyDelimiters() throws InterruptedException, PluginException {
        when(configuration.getProperty("delimiters")).thenReturn("");
        when(configuration.getBooleanProperty("trimtokens", true)).thenReturn(true);
        when(configuration.getBooleanProperty("allowemptytokens", false)).thenReturn(false);
        
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
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
}


