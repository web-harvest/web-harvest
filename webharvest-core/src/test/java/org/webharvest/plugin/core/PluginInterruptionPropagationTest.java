package org.webharvest.plugin.core;

import java.util.Collections;

import org.testng.annotations.Test;
import org.webharvest.definition.CorePluginsRegistrar;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/** Regression tests for nested plugin interruption propagation through Core wrappers. */
public class PluginInterruptionPropagationTest {

    private static final String CORE_NAMESPACE = "http://org.webharvest/schema/2.1/core";

    @Test
    public void xqueryParameterDoesNotConvertInterruptedChildToEmptyValue() throws Exception {
        CorePluginsRegistrar.registerCorePlugins();

        DefaultPluginConfiguration xquery = new DefaultPluginConfiguration("xquery", CORE_NAMESPACE, "xquery");
        DefaultPluginConfiguration parameter = new DefaultPluginConfiguration("xq-param", CORE_NAMESPACE, "xq-param");
        parameter.setProperty("name", "input");
        DefaultPluginConfiguration sleep = sleepChild();
        parameter.setChildren(Collections.singletonList(sleep));
        xquery.setChildren(Collections.singletonList(parameter));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(xquery);
        assertInterrupted(plugin);
    }

    @Test
    public void regexpResultDoesNotConvertInterruptedChildToTemplateFallback() throws Exception {
        CorePluginsRegistrar.registerCorePlugins();

        DefaultPluginConfiguration regexp = new DefaultPluginConfiguration("regexp", CORE_NAMESPACE, "regexp");
        regexp.setProperty("pattern", "x");
        regexp.setProperty("source", "x");

        DefaultPluginConfiguration result = new DefaultPluginConfiguration(
                "regexp-result", CORE_NAMESPACE, "regexp-result");
        result.setChildren(Collections.singletonList(sleepChild()));
        regexp.setChildren(Collections.singletonList(result));

        RegexpPlugin plugin = new RegexpPlugin();
        plugin.initialize(regexp);
        assertInterrupted(plugin);
    }

    private static DefaultPluginConfiguration sleepChild() {
        DefaultPluginConfiguration sleep = new DefaultPluginConfiguration("sleep", CORE_NAMESPACE, "sleep");
        sleep.setProperty("milliseconds", "5000");
        return sleep;
    }

    private static void assertInterrupted(org.webharvest.plugin.Plugin plugin) throws Exception {
        Thread.currentThread().interrupt();
        try {
            plugin.execute(new ScraperContext());
            fail("An interrupted nested plugin must propagate InterruptedException");
        } catch (InterruptedException expected) {
            assertTrue(Thread.currentThread().isInterrupted(),
                    "The interrupt flag must be restored after plugin cleanup");
        } finally {
            Thread.interrupted();
        }
    }
}
