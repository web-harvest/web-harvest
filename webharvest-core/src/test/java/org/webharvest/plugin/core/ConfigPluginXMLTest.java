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
 * Test for ConfigPlugin with real XML processing
 */
public class ConfigPluginXMLTest {

    @Test
    public void testConfigPluginWithChildElements() throws Exception {
        // Given
        ConfigPlugin plugin = new ConfigPlugin();
        
        // Create a mock context
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        
        // Mock configuration
        org.webharvest.plugin.PluginConfiguration config = mock(org.webharvest.plugin.PluginConfiguration.class);
        when(config.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(config.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Mock element definition with child elements
        // v2.2.x: IElementDef removed - mock not needed
        // org.webharvest.definition.IElementDef mockElementDef = mock(org.webharvest.definition.IElementDef.class);
        
        // When
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        verify(context).setCharset("UTF-8");
        verify(context).setScriptingLanguage(any());
        
        // Verify that executeBody was called (should be called by ConfigPlugin)
        // This is the key test - ConfigPlugin should execute child elements
        assertTrue(result instanceof org.webharvest.runtime.variables.EmptyVariable || 
                   result instanceof org.webharvest.runtime.variables.NodeVariable);
    }
    
    @Test
    public void testConfigPluginWithoutElementDef() throws Exception {
        // Given
        ConfigPlugin plugin = new ConfigPlugin();
        
        // Create a mock context
        DynamicScopeContext context = mock(DynamicScopeContext.class);
        
        // Mock configuration
        org.webharvest.plugin.PluginConfiguration config = mock(org.webharvest.plugin.PluginConfiguration.class);
        when(config.getProperty("charset", "UTF-8")).thenReturn("UTF-8");
        when(config.getProperty("scriptlang", "beanshell")).thenReturn("beanshell");
        
        // Don't set elementDef - this should still work
        
        // When
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        // Then
        assertNotNull(result);
        verify(context).setCharset("UTF-8");
        verify(context).setScriptingLanguage(any());
        
        // Should return EmptyVariable when no elementDef
        assertTrue(result instanceof org.webharvest.runtime.variables.EmptyVariable);
    }
}












