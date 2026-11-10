package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Extra SetVar arithmetic/validation edges: missing name, incomplete operators,
 * and non-template literal fallback.
 */
public class SetVarArithmeticFallbackBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRequiresVariableName() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        new SetVarPlugin().validateConfiguration(configuration);
    }

    @Test
    public void nonTemplateBodyUsesLiteralFallback() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "literal-value");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "literal-value");
        assertEquals(context.getVar("out").toString(), "literal-value");
    }

    @Test
    public void incompleteAdditionExpressionDoesNotCrash() throws Exception {
        context.setLocalVar("counter", new NodeVariable("5"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter + }");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
    }
}
