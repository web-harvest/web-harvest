package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.util.Arrays;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

/**
 * XQuery parameter type-conversion contracts (integer/double/boolean/sequence).
 *
 * <p>Note: XML fragments without an explicit supported scalar type are bound as
 * strings by {@code convertParamValue}; they are not auto-promoted to nodes.</p>
 */
public class XQueryPluginTypeBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void doubleParameterSupportsNumericComparison() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $threshold as xs:double external;"
                        + " if ($threshold > 1.5) then <high/> else <low/>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "threshold");
        param.setProperty("type", "double");
        param.setProperty("_textContent", "2.5");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("<high"), "Expected high branch: " + result);
    }

    @Test
    public void integerSequenceParameterBindsWhitespaceSeparatedValues() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $nums as xs:integer* external; <sum>{sum($nums)}</sum>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "nums");
        param.setProperty("type", "integer*");
        param.setProperty("_textContent", "1 2 3");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("6"), "Expected sequence sum 6, got: " + result);
    }

    @Test
    public void invalidNumericTypeFallsBackWithoutAbortingQuery() throws Exception {
        DefaultPluginConfiguration expressionChild = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expressionChild.setProperty("_textContent",
                "declare variable $label as xs:string external; <v>{$label}</v>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "label");
        param.setProperty("type", "integer");
        param.setProperty("_textContent", "not-a-number");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expressionChild, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("not-a-number"),
                "Invalid integer conversion should fall back to string binding: " + result);
    }
}
