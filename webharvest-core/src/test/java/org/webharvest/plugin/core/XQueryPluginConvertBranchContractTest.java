package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.util.Arrays;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.Variable;

/**
 * Additional XQuery scalar/sequence conversion contracts (float/long/boolean+).
 */
public class XQueryPluginConvertBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void floatParameterSupportsComparison() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $x as xs:double external; if ($x < 1.0) then <small/> else <big/>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "x");
        param.setProperty("type", "float");
        param.setProperty("_textContent", "0.25");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("<small"), result.toString());
    }

    @Test
    public void longParameterSupportsEquality() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $id as xs:long external; <id>{$id}</id>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "id");
        param.setProperty("type", "long");
        param.setProperty("_textContent", "42");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("42"), result.toString());
    }

    @Test
    public void booleanPlusSequenceCountsTrueValues() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $flags as xs:boolean+ external; <n>{count($flags[. = true()])}</n>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "flags");
        param.setProperty("type", "boolean+");
        param.setProperty("_textContent", "true false true");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("2"), "Expected two true flags, got: " + result);
    }
}
