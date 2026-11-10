package org.webharvest.plugin.core;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Runtime contracts for {@link SetVarPlugin} arithmetic increment paths and
 * in-place updates when the target variable already exists.
 */
public class SetVarPluginBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        context = spy(new ScraperContext());
    }

    @Test
    public void arithmeticBodyIncrementsContextVariable() throws Exception {
        context.setLocalVar("counter", new NodeVariable("2"));

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "counter");
        configuration.setProperty("_textContent", "${counter + 1}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertEquals(result.toString(), "3");
        assertEquals(context.getVar("counter").toString(), "3");
        verify(context).replaceExistingVar(eq("counter"), any(Variable.class));
    }

    @Test
    public void existingVariableIsUpdatedInPlace() throws Exception {
        context.setLocalVar("flag", new NodeVariable("old"));

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("name", "flag");
        configuration.setProperty("value", "new");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        verify(context).replaceExistingVar(eq("flag"), any(Variable.class));
        assertEquals(context.getVar("flag").toString(), "new");
    }

    @Test
    public void arithmeticSubtractionUsesContextValue() throws Exception {
        context.setLocalVar("steps", new NodeVariable("5"));

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "steps");
        configuration.setProperty("_textContent", "${steps - 2}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);

        assertEquals(result.toString(), "3");
    }

    @Test
    public void newVariableUsesLocalScopeAssignment() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "fresh");
        configuration.setProperty("value", "created");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        verify(context, never()).replaceExistingVar(eq("fresh"), any());
        verify(context).setLocalVar(eq("fresh"), any());
        assertEquals(context.getVar("fresh").toString(), "created");
    }

    @Test
    public void legacyNameAttributeIsAccepted() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("name", "legacy");
        configuration.setProperty("value", "v1");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        plugin.execute(context);

        assertEquals(context.getVar("legacy").toString(), "v1");
    }

    @Test(expectedExceptions = org.webharvest.plugin.exceptions.PluginValidationException.class)
    public void conflictingNameAndVarAreRejectedAtValidation() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("name", "a");
        configuration.setProperty("var", "b");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
    }

    @Test
    public void nonNumericOperandsDefaultToZeroInAddition() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "SetVarPlugin", "http://org.webharvest/schema/2.1/core", "set-var");
        configuration.setProperty("var", "sum");
        configuration.setProperty("_textContent", "${nope + xy}");

        SetVarPlugin plugin = new SetVarPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertEquals(result.toString(), "0");
    }
}
