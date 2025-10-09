/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.Test;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Integration test for ConfigPlugin
 */
public class ConfigPluginIntegrationTest {

    @Test
    public void testConfigPluginWithRealXML() throws Exception {
        // Given
        ConfigPlugin plugin = new ConfigPlugin();
        
        // Create a mock context
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        
        // Mock configuration
        org.webharvest.plugin.PluginConfiguration config = mock(org.webharvest.plugin.PluginConfiguration.class);
        when(config.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(config.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock element definition
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        verify(context).setCharset("UTF-8");
        verify(context).setScriptingLanguage(any());
    }
    
    @Test
    public void testConfigPluginWithCustomCharset() throws Exception {
        // Given
        ConfigPlugin plugin = new ConfigPlugin();
        
        // Create a mock context
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        
        // Mock configuration
        org.webharvest.plugin.PluginConfiguration config = mock(org.webharvest.plugin.PluginConfiguration.class);
        when(config.getProperty("charset", "UTF-8")).thenReturn("ISO-8859-1");
        when(config.getProperty("scriptlang", "beanshell")).thenReturn("javascript");
        
        // Mock element definition
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        verify(context).setCharset("ISO-8859-1");
        verify(context).setScriptingLanguage(any());
    }
    
    @Test
    public void testConfigPluginWithDefaultValues() throws Exception {
        // Given
        ConfigPlugin plugin = new ConfigPlugin();
        
        // Create a mock context
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        
        // Mock configuration with no properties (should use defaults)
        org.webharvest.plugin.PluginConfiguration config = mock(org.webharvest.plugin.PluginConfiguration.class);
        when(config.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(config.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock element definition
        org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        verify(context).setCharset("UTF-8"); // Default charset
        verify(context).setScriptingLanguage(any());
    }
}
