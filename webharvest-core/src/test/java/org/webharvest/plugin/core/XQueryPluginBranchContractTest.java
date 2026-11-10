package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.util.Arrays;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * XQuery execution and schema validation contracts via the public plugin API.
 */
public class XQueryPluginBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void missingExpressionReturnsEmptyVariable() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertSame(result, EmptyVariable.INSTANCE);
    }

    @Test
    public void integerParametersEvaluateArithmeticExpression() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $a as xs:integer external;"
                        + " declare variable $b as xs:integer external;"
                        + " <sum>{ $a + $b }</sum>");

        DefaultPluginConfiguration paramA = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        paramA.setProperty("name", "a");
        paramA.setProperty("type", "integer");
        paramA.setProperty("_textContent", "12");

        DefaultPluginConfiguration paramB = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        paramB.setProperty("name", "b");
        paramB.setProperty("type", "integer");
        paramB.setProperty("_textContent", "30");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, paramA, paramB));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertNotNull(result);
        assertTrue(result.toString().contains("42"), "Expected sum 42 in: " + result);
    }

    @Test
    public void booleanParameterBindsToExpression() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $flag as xs:boolean external;"
                        + " if ($flag) then <on/> else <off/>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "flag");
        param.setProperty("type", "boolean");
        param.setProperty("_textContent", "true");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("<on"), "Expected truthy branch: " + result);
    }

    @Test
    public void configurationSchemaRejectsNullConfiguration() {
        PluginConfigurationSchema schema = new XQueryPlugin().getConfigurationSchema();
        try {
            schema.validate(null);
            fail("Null configuration should throw PluginValidationException");
        } catch (PluginValidationException e) {
            assertEquals(e.getMessage(), "Configuration cannot be null");
        }
    }

    @Test
    public void configurationSchemaAcceptsMinimalMockConfiguration() throws Exception {
        PluginConfiguration config = org.mockito.Mockito.mock(PluginConfiguration.class);
        new XQueryPlugin().getConfigurationSchema().validate(config);
    }

    @Test
    public void stringParameterBindsToConcatenationExpression() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $label as xs:string external; <msg>{ concat('Hi ', $label) }</msg>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "label");
        param.setProperty("type", "string");
        param.setProperty("_textContent", "WebHarvest");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("Hi WebHarvest"), "Expected greeting in: " + result);
    }

    @Test
    public void falseBooleanParameterSelectsElseBranch() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $flag as xs:boolean external;"
                        + " if ($flag) then <on/> else <off/>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "flag");
        param.setProperty("type", "boolean");
        param.setProperty("_textContent", "false");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("<off"), "Expected falsy branch: " + result);
    }
}
