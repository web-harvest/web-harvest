package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * SetVar arithmetic contracts when operands are missing or non-numeric.
 */
public class SetVarArithmeticNonNumericBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void additionTreatsNonNumericLeftOperandAsZero() throws Exception {
        context.setLocalVar("counter", new NodeVariable("abc"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter + 4}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "4");
        assertEquals(context.getVar("out").toString(), "4");
    }

    @Test
    public void additionTreatsNonNumericRightOperandAsZero() throws Exception {
        context.setLocalVar("counter", new NodeVariable("7"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter + xyz}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "7");
    }

    @Test
    public void subtractionUsesZeroWhenLeftVariableMissing() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${missing - 3}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "-3");
    }

    @Test
    public void subtractionTreatsNonNumericRightOperandAsZero() throws Exception {
        context.setLocalVar("counter", new NodeVariable("9"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter - nope}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "9");
    }
}
