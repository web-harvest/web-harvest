/*
 Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.core;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for XsltPlugin
 */
public class XsltPluginTest {

    private XsltPlugin plugin;
    private PluginConfiguration mockConfiguration;
    private DynamicScopeContext mockContext;

    @BeforeMethod
    public void setUp() {
        plugin = new XsltPlugin();
        mockConfiguration = mock(PluginConfiguration.class);
        mockContext = mock(DynamicScopeContext.class);
    }

    @Test
    public void testExecute() throws Exception {
        plugin.initialize(mockConfiguration);
        Variable result = plugin.execute(mockContext);
        assertNotNull(result);
    }
    
    @Test
    public void testInitialization() throws Exception {
        plugin.initialize(mockConfiguration);
        assertNotNull(plugin);
    }
}


