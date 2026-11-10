package org.webharvest.plugin;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;

import com.google.inject.Guice;

/**
 * Regression coverage for validation failures that must abort body execution.
 */
public class DefaultPluginBodyExecutorValidationTest {

    @BeforeMethod
    public void setUp() {
        Guice.createInjector(new org.webharvest.ioc.ScraperModule("."));
        CorePluginsRegistrar.registerCorePlugins();
    }

    @Test
    public void executeBodyPropagatesSetVarValidationFailures() throws Exception {
        DefaultPluginConfiguration conflicting = new DefaultPluginConfiguration(
                "set-var",
                "http://org.webharvest/schema/2.1/core",
                "set-var",
                Map.of("name", "legacy", "var", "modern", "value", "x"));
        DefaultPluginConfiguration root = new DefaultPluginConfiguration(
                "config",
                "http://org.webharvest/schema/2.1/core",
                "config",
                Map.of());
        root.setChildren(List.of(conflicting));

        DynamicScopeContext context = Guice.createInjector(new org.webharvest.ioc.ScraperModule("."))
                .getInstance(ScraperContext.class);

        try {
            new DefaultPluginBodyExecutor().executeBody(root, context);
            fail("Conflicting set-var attributes must abort body execution");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getCause() instanceof PluginValidationException);
            String combined = (expected.getMessage() + " " + expected.getCause().getMessage()).toLowerCase();
            assertTrue(combined.contains("conflict"));
            assertTrue(combined.contains("name"));
            assertTrue(combined.contains("var"));
        }
    }
}
