/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for TryPlugin
 */
public class TryPluginTest {

    @Mock
    private CorePluginDef elementDef;
    
    @Mock
    private XmlNode xmlNode;
    
    @Mock
    private DynamicScopeContext context;
    
    @Mock
    private PluginConfiguration configuration;
    
    private TryPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new TryPlugin();
        
        plugin.setElementDef(elementDef);
        
        when(elementDef.getOperationDefs()).thenReturn(new IElementDef[0]);
        when(elementDef.getNode()).thenReturn(xmlNode);
        when(xmlNode.getText()).thenReturn("Test try content");
        when(elementDef.hasOperations()).thenReturn(false);
        
        when(configuration.getProperty("id")).thenReturn(null);
        
        plugin.initialize(configuration);
    }

    @Test
    public void testGetMetadata() {
        PluginMetadata metadata = plugin.getMetadata();
        assertNotNull(metadata);
        assertEquals(metadata.getName(), "Try Plugin");
        assertEquals(metadata.getDescription(), "Try plugin that provides exception handling with try-catch functionality");
        assertTrue(metadata.isCorePlugin());
        assertEquals(metadata.getElementName(), "try");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
    }

    @Test
    public void testGetConfigurationSchema() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        assertNotNull(schema);
        assertNotNull(schema.getAttributeDefinitions());
        assertEquals(schema.getAttributeDefinitions().length, 1);
        
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttributeDefinitions()[0];
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        assertFalse(idAttr.isRequired());
        
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.1.0");
    }

    @Test
    public void testTryPluginInitialize() throws PluginException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("tryId");
        
        newPlugin.initialize(config);
        
        assertNotNull(newPlugin);
    }

    @Test(expectedExceptions = PluginException.class)
    public void testTryPluginInitializeWithNullConfiguration() throws PluginException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        newPlugin.initialize(null);
    }

    @Test
    public void testTryPluginExecute() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testTryPluginExecuteWithInterruptedException() throws InterruptedException, PluginException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        TryPlugin testPlugin = new TryPlugin() {
            @Override
            protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
                throw new InterruptedException("Test interruption");
            }
        };
        testPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        testPlugin.initialize(config);
        
        testPlugin.execute(context);
    }

    @Test
    public void testTryPluginValidateConfiguration() throws PluginValidationException {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("tryId");
        plugin.validateConfiguration(config);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testTryPluginValidateConfigurationWithNull() throws PluginValidationException {
        plugin.validateConfiguration(null);
    }

    @Test
    public void testTryPluginWithIdAttribute() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("tryId");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginExecuteMultipleTimes() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithSpecialCharactersInId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try@#$");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithUnicodeInId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("尝试");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithVeryLongId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("very-long-try-plugin-id-with-many-characters-to-test-system");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithNumericId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try123");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithHyphenatedId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try-test-id-with-hyphens");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithUnderscoreId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try_test_id");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithMixedCaseId() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("TryTestId");
        
        newPlugin.initialize(config);
        
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithDifferentCharsets() throws PluginException, InterruptedException {
        when(context.getCharset()).thenReturn("ISO-8859-1").thenReturn("UTF-16").thenReturn("UTF-8");
        
        Variable result1 = plugin.execute(context);
        Variable result2 = plugin.execute(context);
        Variable result3 = plugin.execute(context);
        
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result3);
        assertTrue(result1 instanceof EmptyVariable);
        assertTrue(result2 instanceof EmptyVariable);
        assertTrue(result3 instanceof EmptyVariable);
    }

    @Test
    public void testTryPluginWithVariousIDs() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try-123-test");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testTryPluginWithNumericID() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("999");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testTryPluginWithUnicodeID() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("测试中文");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testTryPluginMetadataDetails() {
        PluginMetadata metadata = plugin.getMetadata();
        
        assertNotNull(metadata.getId());
        assertNotNull(metadata.getAuthor());
        assertNotNull(metadata.getCategory());
        assertNotNull(metadata.getVersion());
        assertNotNull(metadata.getTags());
        assertNotNull(metadata.getDependencies());
    }

    @Test
    public void testTryPluginSchemaDetails() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        
        assertNotNull(schema.getRequiredSubElements());
        assertNotNull(schema.getAllowedSubElements());
        assertNotNull(schema.getOptionalAttributes());
        assertNotNull(schema.getRequiredAttributes());
    }

    @Test
    public void testTryPluginGetAttribute() {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        
        PluginConfigurationSchema.AttributeDefinition idAttr = schema.getAttribute("id");
        assertNotNull(idAttr);
        assertEquals(idAttr.getName(), "id");
        
        PluginConfigurationSchema.AttributeDefinition nonExistent = schema.getAttribute("nonexistent");
        assertNull(nonExistent);
    }

    @Test
    public void testTryPluginMultipleOperations() throws InterruptedException {
        when(context.getCharset()).thenReturn("UTF-8");
        
        for (int i = 0; i < 5; i++) {
            Variable result = plugin.execute(context);
            assertNotNull(result);
        }
    }

    @Test
    public void testTryPluginWithSpecialCharID() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("try-<>&\"'");
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testTryPluginWithLongID() throws PluginException, InterruptedException {
        TryPlugin newPlugin = new TryPlugin();
        newPlugin.setElementDef(elementDef);
        
        StringBuilder longId = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longId.append("try-id-").append(i).append("-");
        }
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn(longId.toString());
        
        newPlugin.initialize(config);
        when(context.getCharset()).thenReturn("UTF-8");
        
        Variable result = newPlugin.execute(context);
        
        assertNotNull(result);
    }

    @Test
    public void testTryPluginSchemaValidation() throws PluginValidationException {
        PluginConfigurationSchema schema = plugin.getMetadata().getConfigurationSchema();
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("id")).thenReturn("test");
        
        schema.validate(config);
    }
}

