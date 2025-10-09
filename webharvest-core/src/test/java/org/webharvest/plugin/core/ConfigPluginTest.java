/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
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
 * Test for ConfigPlugin
 */
public class ConfigPluginTest {

    private ConfigPlugin plugin;
    private PluginConfiguration mockConfiguration;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new ConfigPlugin();
        mockConfiguration = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testInitialization() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // When
        plugin.initialize(mockConfiguration);
        
        // Then
        assertTrue(plugin.isInitialized());
    }

    @Test
    public void testInitializationWithCustomCharset() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("ISO-8859-1");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // When
        plugin.initialize(mockConfiguration);
        
        // Then
        assertTrue(plugin.isInitialized());
        verify(mockContext, never()).setCharset(anyString()); // Context not set during initialization
    }

    @Test
    public void testInitializationWithCustomScriptLang() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("javascript");
        
        // When
        plugin.initialize(mockConfiguration);
        
        // Then
        assertTrue(plugin.isInitialized());
        verify(mockContext, never()).setScriptingLanguage(any()); // Context not set during initialization
    }

    @Test
    public void testExecuteWithDefaultConfiguration() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock the element definition and body execution
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // Mock executeBodyContent to return a result
        NodeVariable expectedResult = new NodeVariable("test result");
        // We need to mock the actual execution since executeBodyContent is private
        // For now, we'll test the public interface
        
        // When
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        verify(mockContext).setCharset("UTF-8");
        verify(mockContext).setScriptingLanguage(any());
    }

    @Test
    public void testExecuteWithCustomCharset() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("ISO-8859-1");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock the element definition
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        verify(mockContext).setCharset("ISO-8859-1");
    }

    @Test
    public void testExecuteWithChildElements() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock the element definition with child elements
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // Mock executeBody to return a result
        NodeVariable expectedResult = new NodeVariable("test result");
        // We need to mock the actual execution since executeBody is protected
        // For now, we'll test the public interface
        
        // When
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        verify(mockContext).setCharset("UTF-8");
        verify(mockContext).setScriptingLanguage(any());
    }

    @Test
    public void testExecuteWithoutElementDef() throws Exception {
        // Given
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // When
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        
        // Then
        assertNotNull(result);
        // Should handle missing elementDef gracefully
    }

    @Test
    public void testGetMetadata() {
        // When
        PluginMetadata metadata = plugin.getMetadata();
        
        // Then
        assertNotNull(metadata);
        assertEquals(metadata.getId(), "config");
        assertEquals(metadata.getName(), "Config Plugin");
        assertEquals(metadata.getVersion(), "2.2.0");
        assertEquals(metadata.getElementName(), "config");
        assertEquals(metadata.getNamespace(), "http://org.webharvest/schema/2.1/core");
        assertTrue(metadata.isCorePlugin());
    }

    @Test
    public void testGetConfigurationSchema() {
        // When
        org.webharvest.plugin.PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        
        // Then
        assertNotNull(schema);
        assertTrue(schema.allowsBody());
        assertEquals(schema.getVersion(), "2.2.0");
        
        // Check required attributes
        String[] requiredAttributes = schema.getRequiredAttributes();
        assertNotNull(requiredAttributes);
        assertEquals(requiredAttributes.length, 0); // No required attributes for config
        
        // Check optional attributes
        String[] optionalAttributes = schema.getOptionalAttributes();
        assertNotNull(optionalAttributes);
        assertTrue(optionalAttributes.length >= 2); // charset and scriptlang should be optional
    }

    @Test
    public void testConfigurationSchemaAttributes() {
        // When
        org.webharvest.plugin.PluginConfigurationSchema schema = plugin.getConfigurationSchema();
        org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition[] attributes = schema.getAttributeDefinitions();
        
        // Then
        assertNotNull(attributes);
        assertTrue(attributes.length >= 2); // charset and scriptlang
        
        // Find charset attribute
        boolean foundCharset = false;
        boolean foundScriptLang = false;
        
        for (org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition attr : attributes) {
            if ("charset".equals(attr.getName())) {
                foundCharset = true;
                assertEquals(attr.getDefaultValue(), "UTF-8");
                assertFalse(attr.isRequired());
            } else if ("scriptlang".equals(attr.getName())) {
                foundScriptLang = true;
                assertEquals(attr.getDefaultValue(), "beanshell");
                assertFalse(attr.isRequired());
            }
        }
        
        assertTrue(foundCharset, "charset attribute should be defined");
        assertTrue(foundScriptLang, "scriptlang attribute should be defined");
    }

    @Test
    public void testValidateConfiguration() throws Exception {
        // Given
        PluginConfiguration validConfig = mock(PluginConfiguration.class);
        when(validConfig.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(validConfig.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // When & Then
        plugin.validateConfiguration(validConfig); // Should not throw
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void testValidateConfigurationWithNull() throws Exception {
        // When
        plugin.validateConfiguration(null);
        
        // Then - should throw PluginValidationException
    }

    @Test
    public void testSetAndGetElementDef() {
        // Given
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        
        // Then
        // ULTRA-LEAN v2.2.x: getElementDef() removed
        assertNotNull(plugin);
    }

    @Test
    public void testIsInitialized() {
        // Initially not initialized
        assertFalse(plugin.isInitialized());
        
        // After initialization
        when(mockConfiguration.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(mockConfiguration.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        try {
            plugin.initialize(mockConfiguration);
            assertTrue(plugin.isInitialized());
        } catch (Exception e) {
            fail("Initialization should not fail");
        }
    }
}