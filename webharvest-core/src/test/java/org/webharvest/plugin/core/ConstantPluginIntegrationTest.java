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
 * Integration test for ConstantPlugin - verify it returns correct values
 */
public class ConstantPluginIntegrationTest {

    private ConstantPlugin plugin;
    private DynamicScopeContext context;

    @BeforeMethod
    public void setUp() {
        plugin = new ConstantPlugin();
        context = new ScraperContext();
        context.setScriptingLanguage(ScriptingLanguage.GROOVY);
    }

    @Test
    public void testConstantReturnsValue() throws Exception {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("value")).thenReturn("Test Content");
        
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(result.toString(), "Test Content", "Content should match");
        assertTrue(result.toString().length() > 0, "Content should not be empty");
    }

    @Test
    public void testConstantWithSpecialChars() throws Exception {
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.getProperty("value")).thenReturn("Special: <>&\"'");
        
        plugin.initialize(config);
        Variable result = plugin.execute(context);
        
        assertNotNull(result);
        assertTrue(result.toString().contains("Special"));
    }
}








