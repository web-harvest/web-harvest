package org.webharvest.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;
// REMOVED in v2.2.x - old architecture classes no longer exist
// import org.webharvest.definition.DefinitionResolver;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.core.EmptyPlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
    
    @AfterMethod(alwaysRun = true)
    public void clearInterruptFlagAfterTest() {
        Thread.interrupted();
    }

    @BeforeMethod
    public void setUp() {
        executor = new DefaultPluginBodyExecutor();
        
        // Initialize registry
        // Use target/ as working directory - all generated files will go to target/
        String targetDir = "target";
        new java.io.File(targetDir).mkdirs();
        Injector injector = Guice.createInjector(new ScraperModule(targetDir));
        // v2.2.x: DefinitionResolver removed
        // DefinitionResolver resolver = injector.getInstance(DefinitionResolver.class);
        
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
    @Test
    public void testExecuteBodyPropagatesInterruptionAndStopsFollowingSiblings() throws Exception {
        Thread.interrupted();
        InterruptingTestPlugin.cleanupCalled.set(false);
        InterruptingTestPlugin.cleanupObservedInterrupt.set(true);
        FollowingSiblingTestPlugin.executions.set(0);

        registry.registerMapping(null, InterruptingTestPlugin.class, INTERRUPTING_PLUGIN);
        registry.registerMapping(null, FollowingSiblingTestPlugin.class, FOLLOWING_SIBLING_PLUGIN);

        DefaultPluginConfiguration parent = new DefaultPluginConfiguration(
            "parent", "http://org.webharvest/schema/2.1/core", "config");
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration(
            "interrupting", "http://org.webharvest/schema/2.1/core", INTERRUPTING_PLUGIN));
        children.add(new DefaultPluginConfiguration(
            "following", "http://org.webharvest/schema/2.1/core", FOLLOWING_SIBLING_PLUGIN));
        parent.setChildren(children);

        try {
            executor.executeBody(parent, context);
            fail("An interrupted child must abort body execution");
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "child cancellation");
            assertTrue(InterruptingTestPlugin.cleanupCalled.get(),
                "The child plugin lifecycle must still perform cleanup");
            assertFalse(InterruptingTestPlugin.cleanupObservedInterrupt.get(),
                "The interrupt flag must be restored after child cleanup");
            assertTrue(Thread.currentThread().isInterrupted(),
                "The lifecycle must restore the interrupt flag after cleanup");
        } finally {
            Thread.interrupted();
        }

        assertEquals(FollowingSiblingTestPlugin.executions.get(), 0,
            "Later sibling plugins must not execute after interruption");
    }

    private static final String INTERRUPTING_PLUGIN = "test-interrupting-plugin-issue105";
    private static final String FOLLOWING_SIBLING_PLUGIN = "test-following-sibling-plugin-issue105";

    public static class InterruptingTestPlugin extends AbstractPlugin {
        private static final AtomicBoolean cleanupCalled = new AtomicBoolean();
        private static final AtomicBoolean cleanupObservedInterrupt = new AtomicBoolean();

        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(DynamicScopeContext executionContext) throws InterruptedException {
            throw new InterruptedException("child cancellation");
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No configuration constraints.
        }

        @Override
        protected void doCleanup() {
            cleanupObservedInterrupt.set(Thread.currentThread().isInterrupted());
            cleanupCalled.set(true);
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }

    public static class FollowingSiblingTestPlugin extends AbstractPlugin {
        private static final AtomicInteger executions = new AtomicInteger();

        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(DynamicScopeContext executionContext) {
            executions.incrementAndGet();
            return new NodeVariable("following sibling executed");
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No configuration constraints.
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }

    @Test
    public void testAbstractPluginBodyHelpersPropagateInterruption() throws Exception {
        registry.registerMapping(null, InterruptingTestPlugin.class, INTERRUPTING_PLUGIN);
        DefaultPluginConfiguration parent = new DefaultPluginConfiguration(
            "body-helper-parent", "http://org.webharvest/schema/2.1/core", "config");
        parent.setChildren(java.util.Collections.<PluginConfiguration>singletonList(
            new DefaultPluginConfiguration(
                "interrupting", "http://org.webharvest/schema/2.1/core", INTERRUPTING_PLUGIN)));

        BodyHelperTestPlugin helper = new BodyHelperTestPlugin();
        helper.initialize(parent);

        Thread.interrupted();
        try {
            helper.invokeExecuteBody(context);
            fail("executeBody must propagate child interruption");
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "child cancellation");
            assertTrue(Thread.currentThread().isInterrupted(),
                "executeBody must preserve the child lifecycle interrupt status");
        } finally {
            Thread.interrupted();
        }

        Thread.interrupted();
        try {
            helper.invokeGetBodyTextContent(context);
            fail("getBodyTextContent must propagate child interruption");
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "child cancellation");
            assertTrue(Thread.currentThread().isInterrupted(),
                "getBodyTextContent must preserve the child lifecycle interrupt status");
        } finally {
            Thread.interrupted();
        }
    }

    public static class BodyHelperTestPlugin extends AbstractPlugin {
        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(DynamicScopeContext executionContext) {
            return EmptyVariable.INSTANCE;
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No configuration constraints.
        }

        public Variable invokeExecuteBody(DynamicScopeContext executionContext) throws InterruptedException {
            return executeBody(executionContext);
        }

        public Variable invokeGetBodyTextContent(DynamicScopeContext executionContext) throws InterruptedException {
            return getBodyTextContent(executionContext);
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }

    @Test
    public void testExecuteBodyContinuesAfterOrdinaryRuntimeFailure() throws Exception {
        FollowingSiblingTestPlugin.executions.set(0);
        registry.registerMapping(null, RuntimeFailingTestPlugin.class, RUNTIME_FAILING_PLUGIN);
        registry.registerMapping(null, FollowingSiblingTestPlugin.class, FOLLOWING_SIBLING_PLUGIN);

        DefaultPluginConfiguration parent = new DefaultPluginConfiguration(
            "parent", "http://org.webharvest/schema/2.1/core", "config");
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration(
            "failing", "http://org.webharvest/schema/2.1/core", RUNTIME_FAILING_PLUGIN));
        children.add(new DefaultPluginConfiguration(
            "following", "http://org.webharvest/schema/2.1/core", FOLLOWING_SIBLING_PLUGIN));
        parent.setChildren(children);

        Variable result = executor.executeBody(parent, context);

        assertNotNull(result);
        assertEquals(FollowingSiblingTestPlugin.executions.get(), 1,
            "Ordinary runtime failures must retain continue-on-sibling behavior");
    }

    private static final String RUNTIME_FAILING_PLUGIN = "test-runtime-failing-plugin-issue105";

    public static class RuntimeFailingTestPlugin extends AbstractPlugin {
        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(DynamicScopeContext executionContext) {
            throw new IllegalStateException("ordinary plugin failure");
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No configuration constraints.
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }

    @Test(description = "Verify checked cancellation across Try, List, Def and XPath wrappers")
    public void testCorePluginWrappersPropagateChildInterruption() throws Exception {
        registry.registerMapping(null, InterruptingTestPlugin.class, INTERRUPTING_PLUGIN);

        FollowingSiblingTestPlugin.executions.set(0);
        DefaultPluginConfiguration tryConfiguration =
            wrapperConfiguration("try", new java.util.HashMap<String, String>());
        tryConfiguration.setChildren(java.util.Arrays.<PluginConfiguration>asList(
            new DefaultPluginConfiguration(
                "interrupting-child", "http://org.webharvest/schema/2.1/core", INTERRUPTING_PLUGIN),
            new DefaultPluginConfiguration(
                "following-child", "http://org.webharvest/schema/2.1/core", FOLLOWING_SIBLING_PLUGIN)));
        registry.registerMapping(null, FollowingSiblingTestPlugin.class, FOLLOWING_SIBLING_PLUGIN);
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.TryPlugin(),
            tryConfiguration);
        assertEquals(FollowingSiblingTestPlugin.executions.get(), 0,
            "Try must not execute later siblings or catch handling after cancellation");

        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.ListPlugin(),
            wrapperConfiguration("list", new java.util.HashMap<String, String>()));

        java.util.Map<String, String> defProperties = new java.util.HashMap<>();
        defProperties.put("var", "result");
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.DefPlugin(),
            wrapperConfiguration("def", defProperties));

        java.util.Map<String, String> xpathProperties = new java.util.HashMap<>();
        xpathProperties.put("expression", "//root");
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.XPathPlugin(),
            wrapperConfiguration("xpath", xpathProperties));
    }

    private DefaultPluginConfiguration wrapperConfiguration(
            String elementName, java.util.Map<String, String> properties) {
        DefaultPluginConfiguration wrapper = new DefaultPluginConfiguration(
            elementName, "http://org.webharvest/schema/2.1/core", elementName, properties);
        wrapper.setChildren(java.util.Collections.<PluginConfiguration>singletonList(
            new DefaultPluginConfiguration(
                "interrupting-child", "http://org.webharvest/schema/2.1/core", INTERRUPTING_PLUGIN)));
        return wrapper;
    }

    private void assertWrapperPropagatesInterruption(
            AbstractPlugin plugin, PluginConfiguration configuration) throws Exception {
        InterruptingTestPlugin.cleanupCalled.set(false);
        InterruptingTestPlugin.cleanupObservedInterrupt.set(true);
        Thread.interrupted();
        plugin.initialize(configuration);

        try {
            plugin.execute(context);
            fail(plugin.getClass().getSimpleName()
                + " must propagate child interruption instead of swallowing or wrapping it");
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "child cancellation");
            assertTrue(InterruptingTestPlugin.cleanupCalled.get(),
                "The interrupted child must still complete its cleanup");
            assertFalse(InterruptingTestPlugin.cleanupObservedInterrupt.get(),
                "The child cleanup must run before interrupt restoration");
            assertTrue(Thread.currentThread().isInterrupted(),
                "The wrapper lifecycle must restore interrupt status after cleanup");
        } finally {
            Thread.interrupted();
        }
    }

    @Test(description = "Constant, Template, Var, XML, XQExpression and HttpHeader propagate cancellation")
    public void testSecondaryCorePluginWrappersPropagateChildInterruption() throws Exception {
        registry.registerMapping(null, InterruptingTestPlugin.class, INTERRUPTING_PLUGIN);

        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.ConstantPlugin(),
            wrapperConfiguration("constant", new java.util.HashMap<String, String>()));
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.TemplatePlugin(),
            wrapperConfiguration("template", new java.util.HashMap<String, String>()));

        java.util.Map<String, String> varProperties = new java.util.HashMap<>();
        varProperties.put("name", "result");
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.VarPlugin(),
            wrapperConfiguration("var", varProperties));

        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.XMLPlugin(),
            wrapperConfiguration("xml", new java.util.HashMap<String, String>()));
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.XQExpressionPlugin(),
            wrapperConfiguration("xq-expression", new java.util.HashMap<String, String>()));

        java.util.Map<String, String> headerProperties = new java.util.HashMap<>();
        headerProperties.put("name", "X-Test");
        assertWrapperPropagatesInterruption(new org.webharvest.plugin.core.HttpHeaderPlugin(),
            wrapperConfiguration("http-header", headerProperties));
    }

    @Test
    public void testCreateChildPluginRejectsUnknownElement() {
        DefaultPluginConfiguration unknown = new DefaultPluginConfiguration(
            "unknown", "http://org.webharvest/schema/2.1/core", "missing-plugin-for-regression");

        try {
            executor.createChildPlugin(unknown);
            fail("Unknown plugin elements must not be represented as an empty result");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("missing-plugin-for-regression"));
        }
    }

    @Test
    public void testCreateChildPluginPropagatesInitializationFailure() {
        final String elementName = "test-initialization-failing-plugin";
        registry.registerMapping(null, InitializationFailingTestPlugin.class, elementName);
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
            "initialization-failure", "http://org.webharvest/schema/2.1/core", elementName);

        try {
            executor.createChildPlugin(configuration);
            fail("Plugin initialization failures must not be converted into null");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getCause() instanceof PluginException,
                "The original PluginException must remain available as the cause");
            assertTrue(expected.getMessage().contains(elementName));
        }
    }

    public static class InitializationFailingTestPlugin extends AbstractPlugin {
        @Override
        protected void doInitialize(PluginConfiguration configuration) throws PluginException {
            throw new PluginException("intentional initialization failure");
        }

        @Override
        protected Variable doExecute(DynamicScopeContext executionContext) {
            return EmptyVariable.INSTANCE;
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // No configuration constraints.
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }


    @Test
    public void testExecuteBodyRejectsOrphanElseBeforeExecutingEarlierSiblings() throws Exception {
        FollowingSiblingTestPlugin.executions.set(0);
        registry.registerMapping(null, FollowingSiblingTestPlugin.class, FOLLOWING_SIBLING_PLUGIN);

        DefaultPluginConfiguration parent = new DefaultPluginConfiguration(
            "malformed-orphan-else", "http://org.webharvest/schema/2.1/core", "config");
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration(
            "earlier-child", "http://org.webharvest/schema/2.1/core", FOLLOWING_SIBLING_PLUGIN));
        children.add(new DefaultPluginConfiguration(
            "orphan-else", "http://org.webharvest/schema/2.1/core", "else"));
        parent.setChildren(children);

        try {
            executor.executeChildren(children, context);
            fail("An Else without an immediately preceding sibling If must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Else must immediately follow"));
        }

        assertEquals(FollowingSiblingTestPlugin.executions.get(), 0,
            "Malformed branch structure must be rejected before any sibling executes");
    }

    @Test
    public void testExecuteBodyRejectsMultipleElseSiblingsForOneIf() throws Exception {
        DefaultPluginConfiguration parent = new DefaultPluginConfiguration(
            "malformed-duplicate-else", "http://org.webharvest/schema/2.1/core", "config");
        List<PluginConfiguration> children = new ArrayList<>();
        children.add(new DefaultPluginConfiguration(
            "if-branch", "http://org.webharvest/schema/2.1/core", "if"));
        children.add(new DefaultPluginConfiguration(
            "first-else", "http://org.webharvest/schema/2.1/core", "else"));
        children.add(new DefaultPluginConfiguration(
            "second-else", "http://org.webharvest/schema/2.1/core", "else"));
        parent.setChildren(children);

        try {
            executor.executeChildren(children, context);
            fail("A second Else cannot reuse the same preceding If branch");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Else must immediately follow"));
        }
    }


}

