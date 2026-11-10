package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Residual WhilePlugin contracts for non-boolean condition results, schema attribute
 * lookup, and maxloops boundary validation.
 */
public class WhilePluginResidualBranchContractTest {

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
    public void numericOneConditionIsTreatedAsTruthyNonBooleanResult() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        // BeanShell yields an Integer, not a Boolean; While must coerce via isBooleanTrue.
        configuration.setProperty("condition", "1");
        configuration.setProperty("maxloops", "1");

        WhilePlugin plugin = pluginWithConfig(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result instanceof ListVariable, "truthy non-boolean condition should iterate: " + result);
        assertEquals(result.toString(), "tick");
    }

    @Test
    public void schemaGetAttributeReturnsDefinitionOrNull() {
        PluginConfigurationSchema schema = new WhilePlugin().getConfigurationSchema();
        assertNotNull(schema.getAttribute("condition"));
        assertNull(schema.getAttribute("does-not-exist"));
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void schemaValidateRejectsNullConfiguration() throws Exception {
        new WhilePlugin().getConfigurationSchema().validate(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void schemaValidateRejectsMaxloopsBelowOne() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "0");
        new WhilePlugin().getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void schemaValidateRejectsMaxloopsAboveSafetyCap() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "1000001");
        new WhilePlugin().getConfigurationSchema().validate(configuration);
    }

    @Test
    public void schemaValidateAcceptsEmptyMaxloops() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", "http://org.webharvest/schema/2.1/core", "while");
        configuration.setProperty("condition", "false");
        configuration.setProperty("maxloops", "  ");
        new WhilePlugin().getConfigurationSchema().validate(configuration);
    }
}
