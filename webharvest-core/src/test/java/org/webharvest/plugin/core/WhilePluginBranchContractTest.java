package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Runtime loop semantics for {@link WhilePlugin}: attribute precedence, limits, and output modes.
 */
public class WhilePluginBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    private WhilePlugin pluginWithConfig(DefaultPluginConfiguration configuration) throws Exception {
        WhilePlugin plugin = new WhilePlugin() {
            @Override
            protected Variable executeBody(org.webharvest.runtime.DynamicScopeContext ctx) {
                return new NodeVariable("tick");
            }
        };
        plugin.initialize(configuration);
        return plugin;
    }

    @Test
    public void falseConditionSkipsBodyAndReturnsEmpty() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "false");

        WhilePlugin plugin = pluginWithConfig(configuration);
        Variable result = plugin.execute(context);
        assertSame(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void testAttributeTakesPrecedenceOverCondition() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("test", "true");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "1");

        WhilePlugin plugin = pluginWithConfig(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result instanceof ListVariable);
        assertEquals(result.toString(), "tick");
    }

    @Test
    public void indexVariableTracksIterationsInParentScope() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "true");
        configuration.setProperty("index", "i");
        configuration.setProperty("maxloops", "2");

        WhilePlugin plugin = pluginWithConfig(configuration);
        plugin.execute(context);
        assertEquals(context.getVar("i").toString(), "1");
    }

    @Test
    public void emptyFlagSuppressesAggregatedOutput() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "true");
        configuration.setProperty("empty", "true");
        configuration.setProperty("maxloops", "2");

        WhilePlugin plugin = pluginWithConfig(configuration);
        assertSame(plugin.execute(context), EmptyVariable.INSTANCE);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void initializeRejectsNonNumericMaxloops() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "unbounded");
        new WhilePlugin().initialize(configuration);
    }

    @Test
    public void stringTrueConditionRunsSingleIterationInTestMode() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "true");

        WhilePlugin plugin = pluginWithConfig(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "tick");
    }
}
