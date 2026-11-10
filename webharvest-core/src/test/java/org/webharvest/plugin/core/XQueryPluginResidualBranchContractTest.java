package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import java.util.Arrays;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Residual XQuery contracts: node() params, blank names, and null validation.
 */
public class XQueryPluginResidualBranchContractTest {

    private ScraperContext context;

    @BeforeMethod
    public void setUp() {
        context = new ScraperContext();
    }

    @Test
    public void nodeTypedParameterBindsXmlFragment() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $doc as node() external; <out>{$doc//item/text()}</out>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "doc");
        param.setProperty("type", "node()");
        param.setProperty("_textContent", "<root><item>alpha</item></root>");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("alpha"), result.toString());
    }

    @Test
    public void blankParamNameIsIgnoredWithoutFailingQuery() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent", "<ok/>");

        DefaultPluginConfiguration blank = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        blank.setProperty("name", "");
        blank.setProperty("type", "string");
        blank.setProperty("_textContent", "ignored");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, blank));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("ok"), result.toString());
    }

    @Test
    public void emptyParamBodyFallsBackToEmptyBindingForStringType() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $label as xs:string external; <msg>{string-length($label)}</msg>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "label");
        param.setProperty("type", "string");
        param.setProperty("_textContent", "");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("0"), result.toString());
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void validateRejectsNullConfiguration() throws Exception {
        new XQueryPlugin().validateConfiguration(null);
    }

    @Test
    public void longSequenceParameterJoinsValues() throws Exception {
        DefaultPluginConfiguration expression = new DefaultPluginConfiguration(
                "xq-expression", "http://org.webharvest/schema/2.1/core", "xq-expression");
        expression.setProperty("_textContent",
                "declare variable $ids as xs:long+ external; <n>{count($ids)}</n>");

        DefaultPluginConfiguration param = new DefaultPluginConfiguration(
                "xq-param", "http://org.webharvest/schema/2.1/core", "xq-param");
        param.setProperty("name", "ids");
        param.setProperty("type", "long+");
        param.setProperty("_textContent", "1 2 3");

        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xquery", "http://org.webharvest/schema/2.1/core", "xquery");
        configuration.setChildren(Arrays.asList(expression, param));

        XQueryPlugin plugin = new XQueryPlugin();
        plugin.initialize(configuration);
        Variable result = plugin.execute(context);
        assertTrue(result.toString().contains("3"), result.toString());
        assertFalse(result instanceof EmptyVariable);
    }
}
