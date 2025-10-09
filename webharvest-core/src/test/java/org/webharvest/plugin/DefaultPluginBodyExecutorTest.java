package org.webharvest.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.definition.DefinitionResolver;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.core.EmptyPlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit tests for DefaultPluginBodyExecutor.
 * Target coverage: 55%
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class DefaultPluginBodyExecutorTest {
    
    private DefaultPluginBodyExecutor executor;
    private DynamicScopeContext context;
    private CorePluginRegistry registry;
    
    @BeforeMethod
    public void setUp() {
        executor = new DefaultPluginBodyExecutor();
        
        // Initialize registry
        Injector injector = Guice.createInjector(new ScraperModule("."));
        DefinitionResolver resolver = injector.getInstance(DefinitionResolver.class);
        
        registry = CorePluginRegistry.getInstance();
        
        // Create context using injector (ScraperModule should provide ScraperContext)
        context = injector.getInstance(org.webharvest.runtime.ScraperContext.class);
    }
    
    @Test
    public void testExecuteBody_withNullConfiguration() throws InterruptedException {
        Variable result = executor.executeBody(null, context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
    
    @Test
    public void testExecuteBody_withNoChildren() throws InterruptedException {
        PluginConfiguration config = new DefaultPluginConfiguration("test", "http://test", "test");
        
        Variable result = executor.executeBody(config, context);
        
        assertNotNull(result);
        assertTrue(result instanceof EmptyVariable);
    }
    
    @Test
    public void testExecuteBody_withOneChild() throws InterruptedException {
        // Create parent configuration
        DefaultPluginConfiguration parentConfig = new DefaultPluginConfiguration("parent", "http://org.webharvest/schema/2.1/core", "config");
        
        // Create child configuration for empty plugin
        DefaultPluginConfiguration childConfig = new DefaultPluginConfiguration("child", "http://org.webharvest/schema/2.1/core", "empty");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(childConfig);
        parentConfig.setChildren(children);
        
        Variable result = executor.executeBody(parentConfig, context);
        
        assertNotNull(result);
        // EmptyPlugin returns EmptyVariable, so result should be EmptyVariable
        assertTrue(result instanceof EmptyVariable || result instanceof ListVariable);
    }
    
    @Test
    public void testExecuteBody_withMultipleChildren() throws InterruptedException {
        DefaultPluginConfiguration parentConfig = new DefaultPluginConfiguration("parent", "http://org.webharvest/schema/2.1/core", "config");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration("child1", "http://org.webharvest/schema/2.1/core", "empty"));
        children.add(new DefaultPluginConfiguration("child2", "http://org.webharvest/schema/2.1/core", "empty"));
        children.add(new DefaultPluginConfiguration("child3", "http://org.webharvest/schema/2.1/core", "empty"));
        
        parentConfig.setChildren(children);
        
        Variable result = executor.executeBody(parentConfig, context);
        
        assertNotNull(result);
    }
    
    @Test
    public void testExecuteBody_withNonExistentPlugin() throws InterruptedException {
        DefaultPluginConfiguration parentConfig = new DefaultPluginConfiguration("parent", "http://test", "config");
        
        List<PluginConfiguration> children = new ArrayList<>();
        // Use a plugin name that doesn't exist
        children.add(new DefaultPluginConfiguration("nonexistent", "http://test", "nonexistent-plugin"));
        
        parentConfig.setChildren(children);
        
        Variable result = executor.executeBody(parentConfig, context);
        
        // Should handle gracefully and return ListVariable (even if empty)
        assertNotNull(result);
        assertTrue(result instanceof ListVariable);
    }
    
    @Test
    public void testGetBodyTextContent_withNoChildren() throws InterruptedException {
        PluginConfiguration config = new DefaultPluginConfiguration("test", "http://test", "test");
        
        Variable result = executor.getBodyTextContent(config, context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
        assertEquals(result.toString(), "");
    }
    
    @Test
    public void testGetBodyTextContent_withChildren() throws InterruptedException {
        DefaultPluginConfiguration parentConfig = new DefaultPluginConfiguration("parent", "http://org.webharvest/schema/2.1/core", "config");
        
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration("child", "http://org.webharvest/schema/2.1/core", "empty"));
        
        parentConfig.setChildren(children);
        
        Variable result = executor.getBodyTextContent(parentConfig, context);
        
        assertNotNull(result);
        assertTrue(result instanceof NodeVariable);
    }
    
    @Test
    public void testExecuteBody_handlesExceptionGracefully() throws InterruptedException {
        DefaultPluginConfiguration parentConfig = new DefaultPluginConfiguration("parent", "http://test", "config");
        
        List<PluginConfiguration> children = new ArrayList<>();
        // Add multiple children, some might fail
        children.add(new DefaultPluginConfiguration("c1", "http://test", "invalid1"));
        children.add(new DefaultPluginConfiguration("c2", "http://org.webharvest/schema/2.1/core", "empty"));
        children.add(new DefaultPluginConfiguration("c3", "http://test", "invalid2"));
        
        parentConfig.setChildren(children);
        
        // Should not throw exception, should handle failures gracefully
        Variable result = executor.executeBody(parentConfig, context);
        
        assertNotNull(result);
    }
    
    @Test
    public void testExecuteBody_withConfigurationHasChildren() throws InterruptedException {
        DefaultPluginConfiguration config = new DefaultPluginConfiguration("test", "http://test", "test");
        
        // Initially has no children
        assertFalse(config.hasChildren());
        Variable result1 = executor.executeBody(config, context);
        assertTrue(result1 instanceof EmptyVariable);
        
        // Add children
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration("child", "http://org.webharvest/schema/2.1/core", "empty"));
        config.setChildren(children);
        
        // Now has children
        assertTrue(config.hasChildren());
        Variable result2 = executor.executeBody(config, context);
        assertNotNull(result2);
    }
}

