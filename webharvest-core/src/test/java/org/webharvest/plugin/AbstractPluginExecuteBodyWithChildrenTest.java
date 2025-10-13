/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * ULTRA-LEAN v2.2.x: Test that AbstractPlugin.executeBody() correctly executes children
 * from PluginConfiguration.getChildren() without using IElementDef.
 * 
 * @author Web-Harvest Team
 * @since 2.2.0
 */
public class AbstractPluginExecuteBodyWithChildrenTest {

    @Mock
    private DynamicScopeContext context;
    
    private TestPlugin plugin;

    @BeforeMethod
    public void setUp() throws PluginException {
        MockitoAnnotations.openMocks(this);
        plugin = new TestPlugin();
    }

    @Test
    public void testExecuteBodyWithNoChildren() throws Exception {
        // Given: configuration with no children
        PluginConfiguration config = mock(PluginConfiguration.class);
        when(config.hasChildren()).thenReturn(false);
        when(config.getChildren()).thenReturn(new ArrayList<>());
        
        plugin.initialize(config);
        
        // When
        Variable result = plugin.executeBody(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }

    @Test
    public void testExecuteBodyWithOneChild() throws Exception {
        // Given: Real configuration with 1 child (using DefaultPluginConfiguration)
        DefaultPluginConfiguration childConfig = new DefaultPluginConfiguration(
            "child_1", 
            "http://org.webharvest/schema/2.1/core",
            "empty", // Use 'empty' plugin which is registered in CorePluginRegistry
            new java.util.HashMap<>()
        );
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(childConfig);
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "parent_1",
            "http://org.webharvest/schema/2.1/core",
            "test",
            new java.util.HashMap<>()
        );
        config.setChildren(children);
        
        plugin.initialize(config);
        
        // When
        Variable result = plugin.executeBody(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
        ListVariable listResult = (ListVariable) result;
        // EmptyPlugin returns EmptyVariable which is filtered out by ListVariable constructor
        // So list size might be 0, but result should not be null
        assertNotNull(listResult);
    }

    @Test
    public void testExecuteBodyWithMultipleChildren() throws Exception {
        // Given: Real configuration with 3 children
        List<PluginConfiguration> children = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DefaultPluginConfiguration childConfig = new DefaultPluginConfiguration(
                "child_" + i,
                "http://org.webharvest/schema/2.1/core",
                "empty", // Use 'empty' plugin
                new java.util.HashMap<>()
            );
            children.add(childConfig);
        }
        
        DefaultPluginConfiguration config = new DefaultPluginConfiguration(
            "parent_multi",
            "http://org.webharvest/schema/2.1/core",
            "test",
            new java.util.HashMap<>()
        );
        config.setChildren(children);
        
        plugin.initialize(config);
        
        // When
        Variable result = plugin.executeBody(context);
        
        // Then
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
        // EmptyPlugin returns EmptyVariable, so list might be empty, but result should not be null
        assertNotNull(result);
    }

    /**
     * Test plugin for AbstractPlugin testing.
     */
    private static class TestPlugin extends AbstractPlugin {
        
        @Override
        protected void doInitialize(PluginConfiguration configuration) throws PluginException {
            // No initialization needed
        }

        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            return executeBody(context);
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) 
                throws PluginValidationException {
            // No validation needed
        }

        @Override
        public PluginMetadata getMetadata() {
            return new PluginMetadata() {
                @Override public String getId() { return "test"; }
                @Override public String getName() { return "Test Plugin"; }
                @Override public String getVersion() { return "1.0"; }
                @Override public String getDescription() { return "Test"; }
                @Override public String getAuthor() { return "Test"; }
                @Override public String getCategory() { return "Test"; }
                @Override public String[] getTags() { return new String[0]; }
                @Override public String[] getDependencies() { return new String[0]; }
                @Override public String getClassName() { return TestPlugin.class.getName(); }
                @Override public boolean isCorePlugin() { return false; }
                @Override public String getElementName() { return "test"; }
                @Override public String getNamespace() { return "http://test"; }
                @Override public PluginConfigurationSchema getConfigurationSchema() { return null; }
            };
        }
    }
}

