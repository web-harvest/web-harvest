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
 * Residual SetVar contracts for multiply/divide body detection, name/var conflicts,
 * and empty-body assignment.
 */
public class SetVarResidualOpsBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void multiplyBodyExpressionIsEvaluatedThroughTemplaterFallback() throws Exception {
        context.setLocalVar("counter", new NodeVariable("6"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter * 3}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertFalse(result.toString().isBlank());
        assertEquals(context.getVar("out").toString(), result.toString());
    }

    @Test
    public void divideBodyExpressionIsEvaluatedThroughTemplaterFallback() throws Exception {
        context.setLocalVar("counter", new NodeVariable("8"));
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "${counter / 2}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertEquals(context.getVar("out").toString(), result.toString());
    }

    @Test
    public void valueAttributeTakesPrecedenceOverBodyContent() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("value", "from-attr");
        configuration.setProperty("_textContent", "from-body");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "from-attr");
        assertEquals(context.getVar("out").toString(), "from-attr");
    }

    @Test
    public void emptyBodyAssignsEmptyVariable() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "out");
        configuration.setProperty("_textContent", "   ");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "");
        assertNotNull(context.getVar("out"));
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void initializeRejectsConflictingNameAndVar() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("name", "legacy");
        configuration.setProperty("var", "modern");
        new SetVarPlugin().initialize(configuration);
    }

    @Test
    public void matchingNameAndVarAreAccepted() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "set-var", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("name", "same");
        configuration.setProperty("var", "same");
        configuration.setProperty("value", "ok");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "ok");
        assertEquals(context.getVar("same").toString(), "ok");
    }
}
