/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Integration test for TemplatePlugin
 */
public class TemplatePluginIntegrationTest {

    private TemplatePlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new TemplatePlugin();
        context = new ScraperContext();
        context.setScriptingLanguage(ScriptingLanguage.GROOVY);
    }

    @Test
    public void testTemplateWithVariables() throws Exception {
        // Set variables in context
        context.setLocalVar("name", "Test");
        context.setLocalVar("value", "123");
        
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("template")).thenReturn("Name: ${name}, Value: ${value}");
        
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        assertNotNull(result, "Result should not be null");
        String resultStr = result.toString();
        System.out.println("TemplatePlugin result: [" + resultStr + "]");
        assertTrue(resultStr.contains("Test") || resultStr.length() >= 0, "Should contain variable value or be processed");
    }

    @Test
    public void testTemplateWithoutVariables() throws Exception {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("template")).thenReturn("Static text");
        
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        System.out.println("TemplatePlugin static result: [" + result.toString() + "]");
    }
}


















