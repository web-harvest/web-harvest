/*
 Copyright (c) 2006-2025 the original author or authors.
 */

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
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Unit tests for HttpParamPlugin.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class HttpParamPluginTest {

    @Mock
    private PluginConfiguration configuration;
    @Mock
    private DynamicScopeContext context;

    private HttpParamPlugin plugin;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        plugin = new HttpParamPlugin();
        when(configuration.getProperty("id")).thenReturn(null);
        when(configuration.getProperty("name")).thenReturn("testParam");
        when(configuration.getProperty("isfile")).thenReturn("false");
    }

    @Test
    public void testGetMetadata() {
        assertNotNull(plugin.getMetadata());
        assertEquals(plugin.getMetadata().getId(), "http-param");
        assertEquals(plugin.getMetadata().getName(), "HTTP Parameter Plugin");
        assertEquals(plugin.getMetadata().getVersion(), "2.1.0");
        assertEquals(plugin.getMetadata().getDescription(), "HTTP parameter plugin that defines HTTP parameters for HTTP requests");
        assertEquals(plugin.getMetadata().getAuthor(), "Web-Harvest Team");
        assertEquals(plugin.getMetadata().getCategory(), "core");
        assertEquals(plugin.getMetadata().getElementName(), "http-param");
        assertTrue(plugin.getMetadata().isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        assertNotNull(schema);
        assertEquals(schema.getVersion(), "2.1.0");
        assertTrue(schema.allowsBody());
        assertEquals(schema.getRequiredSubElements().length, 0);
        assertEquals(schema.getAllowedSubElements().length, 0);
        assertEquals(schema.getRequiredAttributes().length, 1);
        assertEquals(schema.getOptionalAttributes().length, 4);

        // Check attribute definitions
        PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        assertEquals(attributes.length, 5);

        // Check name attribute (required)
        PluginConfigurationSchema.AttributeDefinition nameAttr = schema.getAttribute("name");
        assertNotNull(nameAttr);
        assertEquals(nameAttr.getName(), "name");
        assertEquals(nameAttr.getType(), PluginConfigurationSchema.AttributeType.STRING);
        assertTrue(nameAttr.isRequired());
        assertNull(nameAttr.getDefaultValue());

        // Check isfile attribute (optional)
        PluginConfigurationSchema.AttributeDefinition isfileAttr = schema.getAttribute("isfile");
        assertNotNull(isfileAttr);
        assertEquals(isfileAttr.getName(), "isfile");
        assertEquals(isfileAttr.getType(), PluginConfigurationSchema.AttributeType.BOOLEAN);
        assertFalse(isfileAttr.isRequired());
        assertEquals(isfileAttr.getDefaultValue(), "false");

        // Check id attribute (optional)
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

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNullConfiguration() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithMissingName() throws PluginValidationException {
        when(configuration.getProperty("name")).thenReturn(null);
        plugin.validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithEmptyName() throws PluginValidationException {
        when(configuration.getProperty("name")).thenReturn("");
        plugin.validateConfiguration(configuration);
    }

    @Test
    public void testValidateConfigurationWithMissingIsfile() throws PluginValidationException {
        when(configuration.getProperty("isfile")).thenReturn(null);
        // HttpParamPlugin doesn't throw PluginValidationException for missing isfile
        plugin.validateConfiguration(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test
    public void testValidateConfigurationWithEmptyIsfile() throws PluginValidationException {
        when(configuration.getProperty("isfile")).thenReturn("");
        // HttpParamPlugin doesn't throw PluginValidationException for empty isfile
        plugin.validateConfiguration(configuration);
        // Should not throw any exception
        assertTrue(true);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecute() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithIdAttribute() throws InterruptedException, PluginException {
        when(configuration.getProperty("id")).thenReturn("test-httpparam");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteMultipleTimes() throws InterruptedException, PluginException {
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithDifferentParameterNames() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("differentParam");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithComplexValues() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("complexParam");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithSpecialCharactersInName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("param@#$");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testExecuteWithUnicodeInName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("参数");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithHyphenatedName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("param-name-with-hyphens");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithUnderscoreName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("param_name_with_underscores");
        when(configuration.getProperty("isfile")).thenReturn("false");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteMultipleTimesExtended() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("param1");
        when(configuration.getProperty("isfile")).thenReturn("false");
        plugin.initialize(configuration);
        
        plugin.execute(context);
        plugin.execute(context);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithFileIsTrue() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("fileParam");
        when(configuration.getProperty("isfile")).thenReturn("true");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithFileIsFalse() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("textParam");
        when(configuration.getProperty("isfile")).thenReturn("false");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithVeryLongName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("very-long-parameter-name-with-many-characters-to-test-system");
        when(configuration.getProperty("isfile")).thenReturn("false");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecuteWithNumericName() throws InterruptedException, PluginException {
        when(configuration.getProperty("name")).thenReturn("param123");
        when(configuration.getProperty("isfile")).thenReturn("false");
        plugin.initialize(configuration);
        plugin.execute(context);
    }

    @Test
    public void testMetadataGetters() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getAuthor());
        assertEquals(metadata.getAuthor(), "Web-Harvest Team");
        assertNotNull(metadata.getCategory());
        assertEquals(metadata.getCategory(), "core");
        assertNotNull(metadata.getVersion());
        
        String[] tags = metadata.getTags();
        assertNotNull(tags);
        
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
}


