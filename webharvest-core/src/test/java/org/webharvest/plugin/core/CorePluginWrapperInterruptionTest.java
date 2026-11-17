package org.webharvest.plugin.core;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.ioc.ScraperModule;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;

/**
 * TDD contract tests for core plugins that wrap child/body execution.
 */
public class CorePluginWrapperInterruptionTest {

    private static final String NAMESPACE = "http://org.webharvest/schema/2.1/core";
    private static final String INTERRUPTING_PLUGIN = "interrupting-wrapper-test-issue111";

    private DynamicScopeContext context;
    private CorePluginRegistry registry;

    @BeforeMethod
    public void setUp() {
        final Injector injector = Guice.createInjector(new ScraperModule("target"));
        context = injector.getInstance(ScraperContext.class);
        registry = CorePluginRegistry.getInstance();
        registry.registerMapping(null, InterruptingTestPlugin.class, INTERRUPTING_PLUGIN);
        InterruptingTestPlugin.cleanupCalled.set(false);
        InterruptingTestPlugin.cleanupObservedInterrupt.set(true);
    }

    @AfterMethod(alwaysRun = true)
    public void clearInterruptFlagAfterTest() {
        Thread.interrupted();
    }

    @Test
    public void testTryPluginPropagatesChildInterruptionInsteadOfHandlingItAsAnOrdinaryFailure()
            throws Exception {
        assertInterruptionPropagates(new TryPlugin(), parent("try", Collections.<String, String>emptyMap()));
    }

    @Test
    public void testListPluginPropagatesChildInterruption() throws Exception {
        assertInterruptionPropagates(new ListPlugin(), parent("list", Collections.<String, String>emptyMap()));
    }

    @Test
    public void testXPathPluginPropagatesBodyInterruption() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put("expression", "//entry");
        assertInterruptionPropagates(new XPathPlugin(), parent("xpath", properties));
    }

    @Test
    public void testDefPluginPropagatesChildInterruptionWithoutWrappingIt() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put("var", "definedValue");
        assertInterruptionPropagates(new DefPlugin(), parent("def", properties));
    }

    private void assertInterruptionPropagates(final Plugin plugin,
                                              final DefaultPluginConfiguration configuration)
            throws Exception {
        final PluginConfiguration child = new DefaultPluginConfiguration(
            "interrupting-child", NAMESPACE, INTERRUPTING_PLUGIN);
        configuration.setChildren(Collections.singletonList(child));
        plugin.initialize(configuration);

        try {
            plugin.execute(context);
            fail("The original InterruptedException must propagate through " + plugin.getClass().getSimpleName());
        } catch (InterruptedException expected) {
            assertEquals(expected.getMessage(), "wrapper cancellation");
            assertTrue(InterruptingTestPlugin.cleanupCalled.get(),
                "The interrupted child must still execute lifecycle cleanup");
            assertFalse(InterruptingTestPlugin.cleanupObservedInterrupt.get(),
                "Child cleanup must run before the interrupt flag is restored");
            assertTrue(Thread.currentThread().isInterrupted(),
                "The enclosing plugin lifecycle must restore the interrupt flag after cleanup");
        } finally {
            Thread.interrupted();
        }
    }

    private DefaultPluginConfiguration parent(final String elementName,
                                               final Map<String, String> properties) {
        return new DefaultPluginConfiguration("parent-" + elementName, NAMESPACE,
            elementName, properties);
    }

    public static class InterruptingTestPlugin extends AbstractPlugin {
        private static final AtomicBoolean cleanupCalled = new AtomicBoolean();
        private static final AtomicBoolean cleanupObservedInterrupt = new AtomicBoolean();

        @Override
        protected void doInitialize(final PluginConfiguration configuration) {
            // No initialization required by this test plugin.
        }

        @Override
        protected Variable doExecute(final DynamicScopeContext executionContext)
                throws InterruptedException {
            throw new InterruptedException("wrapper cancellation");
        }

        @Override
        protected void doValidateConfiguration(final PluginConfiguration configuration)
                throws PluginValidationException {
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
}
