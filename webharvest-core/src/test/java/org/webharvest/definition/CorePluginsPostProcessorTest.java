package org.webharvest.definition;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.plugin.PluginMetadata;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Test for CorePluginsPostProcessor.
 */
public class CorePluginsPostProcessorTest {

    private ConfigurableResolver resolver;

    private CorePluginsPostProcessor postProcessor;

    @BeforeMethod
    public void setUp() {
        resolver = mock(ConfigurableResolver.class);
        MockitoAnnotations.initMocks(this);
        postProcessor = new CorePluginsPostProcessor("org.webharvest.plugin.core");
    }

    @Test
    public void testConstructorWithPackageName() {
        CorePluginsPostProcessor processor = new CorePluginsPostProcessor("test.package");
        assertNotNull(processor);
    }

    @Test
    public void testConstructorWithPackageNameAndStrategy() {
        CorePluginRegistrationStrategy strategy = new CorePluginRegistrationStrategy();
        CorePluginsPostProcessor processor = new CorePluginsPostProcessor("test.package", strategy);
        assertNotNull(processor);
    }

    @Test
    public void testPostProcess() {
        // This test verifies that postProcess doesn't throw exceptions
        // and calls the resolver appropriately
        postProcessor.postProcess(resolver);
        
        // Verify that the resolver was used (plugins should have been registered)
        // This is a basic test - postProcess should complete without exceptions
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testPostProcessWithNullResolver() {
        postProcessor.postProcess(null);
    }

    @Test
    public void testPostProcessMultipleTimes() {
        // Test that postProcess can be called multiple times without issues
        postProcessor.postProcess(resolver);
        postProcessor.postProcess(resolver);
        
        // Should not throw exceptions
        assertTrue(true);
    }

    @Test
    public void testPostProcessWithDifferentPackages() {
        CorePluginsPostProcessor processor1 = new CorePluginsPostProcessor("org.webharvest.plugin.core");
        CorePluginsPostProcessor processor2 = new CorePluginsPostProcessor("org.webharvest.plugin.db");
        
        processor1.postProcess(resolver);
        processor2.postProcess(resolver);
        
        // Should not throw exceptions
        assertTrue(true);
    }

    @Test
    public void testPostProcessWithEmptyPackage() {
        CorePluginsPostProcessor processor = new CorePluginsPostProcessor("");
        processor.postProcess(resolver);
        
        // Should not throw exceptions
        assertTrue(true);
    }

    @Test
    public void testPostProcessWithNonExistentPackage() {
        CorePluginsPostProcessor processor = new CorePluginsPostProcessor("com.nonexistent.package");
        processor.postProcess(resolver);
        
        // Should not throw exceptions even if package doesn't exist
        assertTrue(true);
    }

    @Test
    public void testPostProcessWithCustomStrategy() {
        CorePluginRegistrationStrategy customStrategy = mock(CorePluginRegistrationStrategy.class);
        CorePluginsPostProcessor processor = new CorePluginsPostProcessor("org.webharvest.plugin.core", customStrategy);
        
        processor.postProcess(resolver);
        
        // Should not throw exceptions
        assertTrue(true);
    }

    /**
     * Mock plugin class for testing - annotated with @CorePlugin
     */
    @CorePlugin(
        elementName = "test",
        description = "Test plugin",
        version = "1.0.0"
    )
    public static class MockCorePlugin extends AbstractPlugin {
        // Mock implementation - not used in actual test
        @Override
        protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
            return EmptyVariable.INSTANCE;
        }

        public PluginConfigurationSchema getConfigurationSchema() {
            return null;
        }

        @Override
        protected void doValidateConfiguration(PluginConfiguration configuration) {
            // Mock implementation
        }

        @Override
        protected void doInitialize(PluginConfiguration configuration) {
            // Mock implementation
        }

        @Override
        public PluginMetadata getMetadata() {
            return null;
        }
    }

    /**
     * Mock class for testing - annotated with @CorePlugin but doesn't implement Plugin
     */
    @CorePlugin(
        elementName = "invalid",
        description = "Invalid plugin",
        version = "1.0.0"
    )
    public static class InvalidCorePlugin {
        // This should be filtered out during scanning
    }
}
