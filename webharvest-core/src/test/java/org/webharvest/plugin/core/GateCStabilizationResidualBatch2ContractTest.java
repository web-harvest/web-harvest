package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Gate C residual contracts targeting remaining high-yield branch gaps:
 * If condition evaluation matrix, JsonToXml payload shapes, XPath CSS/empty
 * routing, While empty aggregation / literal-true safety break, Template
 * language conversion, and Call unknown-function fallback.
 */
public class GateCStabilizationResidualBatch2ContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test(expectedExceptions = PluginValidationException.class)
    public void ifSchemaRejectsNullConfiguration() throws Exception {
        new IfPlugin().getConfigurationSchema().validate(null);
    }

    @Test
    public void ifEmptyConditionIsFalseAndSkipsBody() throws Exception {
        IfPlugin plugin = new IfPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("should-not-run");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration("if", NS, "if");
        configuration.setProperty("condition", "");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
    }

    @Test
    public void ifZeroAndFalseLiteralsAreFalsy() throws Exception {
        for (String condition : new String[] {"0", "false"}) {
            IfPlugin plugin = new IfPlugin() {
                @Override
                protected Variable executeBody(DynamicScopeContext context) {
                    return new NodeVariable("body");
                }
            };
            DefaultPluginConfiguration configuration = new DefaultPluginConfiguration("if", NS, "if");
            configuration.setProperty("condition", condition);
            plugin.initialize(configuration);
            Variable result = plugin.execute(new ScraperContext());
            assertTrue(result instanceof EmptyVariable || result.isEmpty(),
                    "condition=" + condition + " result=" + result);
        }
    }

    @Test
    public void ifTemplateInequalityComparesContextVariable() throws Exception {
        ScraperContext context = new ScraperContext();
        context.setLocalVar("status", new NodeVariable("active"));

        IfPlugin hit = bodyCapturingIf();
        DefaultPluginConfiguration hitConfig = new DefaultPluginConfiguration("if", NS, "if");
        hitConfig.setProperty("condition", "${status != 'inactive'}");
        hit.initialize(hitConfig);
        assertEquals(hit.execute(context).toString(), "yes");

        IfPlugin miss = bodyCapturingIf();
        DefaultPluginConfiguration missConfig = new DefaultPluginConfiguration("if", NS, "if");
        missConfig.setProperty("condition", "${status != 'active'}");
        miss.initialize(missConfig);
        Variable missResult = miss.execute(context);
        assertTrue(missResult instanceof EmptyVariable || missResult.isEmpty(), String.valueOf(missResult));
    }

    @Test
    public void ifTemplateInequalitySupportsDoubleQuotedAndUnquotedLiterals() throws Exception {
        ScraperContext context = new ScraperContext();
        context.setLocalVar("status", new NodeVariable("active"));

        for (String condition : new String[] {
                "${status != \"inactive\"}",
                "${status != inactive}"}) {
            IfPlugin plugin = bodyCapturingIf();
            DefaultPluginConfiguration configuration =
                    new DefaultPluginConfiguration("if", NS, "if");
            configuration.setProperty("condition", condition);
            plugin.initialize(configuration);

            assertEquals(plugin.execute(context).toString(), "yes",
                    "Inequality must preserve historical quoted and unquoted literal support");
        }
    }

    @Test
    public void ifTemplateEqualityAndSimpleVariableReference() throws Exception {
        ScraperContext context = new ScraperContext();
        context.setLocalVar("ready", new NodeVariable("true"));

        IfPlugin eq = bodyCapturingIf();
        DefaultPluginConfiguration eqConfig = new DefaultPluginConfiguration("if", NS, "if");
        eqConfig.setProperty("condition", "${ready == 'true'}");
        eq.initialize(eqConfig);
        assertEquals(eq.execute(context).toString(), "matched");

        IfPlugin flag = bodyCapturingIf();
        DefaultPluginConfiguration flagConfig = new DefaultPluginConfiguration("if", NS, "if");
        flagConfig.setProperty("condition", "${ready}");
        flag.initialize(flagConfig);
        assertEquals(flag.execute(context).toString(), "flagged");
    }

    private static IfPlugin bodyCapturingIf() {
        return new IfPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                String condition = getConfiguration().getProperty("condition", "");
                if (condition.contains("!=")) {
                    return new NodeVariable("yes");
                }
                if (condition.contains("==")) {
                    return new NodeVariable("matched");
                }
                return new NodeVariable("flagged");
            }
        };
    }

    @Test
    public void jsonToXmlConvertsArrayWithCustomRootTag() throws Exception {
        JsonToXmlPlugin plugin = new JsonToXmlPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("[{\"n\":1},{\"n\":2}]");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "json-to-xml", NS, "json-to-xml");
        configuration.setProperty("tag", "items");
        plugin.initialize(configuration);
        String xml = plugin.execute(new ScraperContext()).toString();
        assertTrue(xml.contains("items"), xml);
        assertTrue(xml.contains("n"), xml);
    }

    @Test
    public void jsonToXmlWrapsScalarPayloadAndFallsBackOnInvalidJson() throws Exception {
        JsonToXmlPlugin scalarPlugin = new JsonToXmlPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("42");
            }
        };
        DefaultPluginConfiguration scalarConfig = new DefaultPluginConfiguration(
                "json-to-xml", NS, "json-to-xml");
        scalarPlugin.initialize(scalarConfig);
        String scalarXml = scalarPlugin.execute(new ScraperContext()).toString();
        assertTrue(scalarXml.contains("data"), scalarXml);

        JsonToXmlPlugin invalidPlugin = new JsonToXmlPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("{not-json");
            }
        };
        DefaultPluginConfiguration invalidConfig = new DefaultPluginConfiguration(
                "json-to-xml", NS, "json-to-xml");
        invalidConfig.setProperty("tag", "payload");
        invalidPlugin.initialize(invalidConfig);
        String fallback = invalidPlugin.execute(new ScraperContext()).toString();
        assertTrue(fallback.contains("<payload>"), fallback);
        assertTrue(fallback.contains("CDATA") || fallback.contains("<![CDATA["), fallback);
    }

    @Test
    public void jsonToXmlEmptyBodyUsesDefaultRootWhenTagBlank() throws Exception {
        JsonToXmlPlugin plugin = new JsonToXmlPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return EmptyVariable.INSTANCE;
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "json-to-xml", NS, "json-to-xml");
        configuration.setProperty("tag", "");
        plugin.initialize(configuration);
        assertEquals(plugin.execute(new ScraperContext()).toString(), "<empty/>");
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void xpathSchemaRejectsMissingExpressionAndVariables() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xpath", NS, "xpath");
        new XPathPlugin().getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void xpathSchemaRejectsNonVariableAttributesWithoutExpression() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xpath", NS, "xpath");
        configuration.setProperty("id", "xpath-1");
        configuration.setProperty("type", "xpath");
        new XPathPlugin().getConfigurationSchema().validate(configuration);
    }

    @Test
    public void xpathSchemaAcceptsVariablePrefixedAttributeWithoutExpression() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xpath", NS, "xpath");
        configuration.setProperty("v:doc", "payload");
        new XPathPlugin().getConfigurationSchema().validate(configuration);
    }

    @Test
    public void xpathEmptyExpressionReturnsEmptyWhenOnlyVariableAttributePresent() throws Exception {
        XPathPlugin plugin = new XPathPlugin();
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xpath", NS, "xpath");
        // Schema accepts variable-prefixed attributes; blank expression is still empty at execute.
        configuration.setProperty("v:doc", "payload");
        configuration.setProperty("expression", "   ");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
    }

    @Test
    public void xpathCssTypeWithEmptyBodyReturnsEmpty() throws Exception {
        XPathPlugin plugin = new XPathPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return EmptyVariable.INSTANCE;
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xpath", NS, "xpath");
        configuration.setProperty("expression", "div.title");
        configuration.setProperty("type", "css");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
    }

    @Test
    public void whileEmptyFlagReturnsEmptyEvenWhenBodyProducesValues() throws Exception {
        WhilePlugin plugin = new WhilePlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("tick");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", NS, "while");
        configuration.setProperty("condition", "true");
        configuration.setProperty("maxloops", "2");
        configuration.setProperty("empty", "true");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
    }

    @Test
    public void whileLiteralTrueWithoutMaxloopsStopsAfterSafetyBreak() throws Exception {
        WhilePlugin plugin = new WhilePlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("once");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", NS, "while");
        configuration.setProperty("condition", "true");
        // intentionally omit maxloops to exercise the literal-true safety break
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertNotNull(result);
        assertEquals(result.toString(), "once");
    }

    @Test
    public void templateLanguagesResolveThroughExecutePath() throws Exception {
        for (String language : new String[] {"beanshell", "javascript", "groovy", "unknown-lang", ""}) {
            TemplatePlugin plugin = new TemplatePlugin();
            DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                    "template", NS, "template");
            configuration.setProperty("language", language);
            configuration.setProperty("_textContent", "hello-" + language);
            if ("unknown-lang".equals(language)) {
                // validation rejects unknown languages; initialize with blank then set property
                configuration.setProperty("language", "");
                plugin.initialize(configuration);
                configuration.setProperty("language", "unknown-lang");
            } else {
                plugin.initialize(configuration);
            }
            Variable result = plugin.execute(new ScraperContext());
            assertNotNull(result, "language=" + language);
            assertTrue(result.toString().contains("hello"),
                    "language=" + language + " result=" + result);
        }
    }

    @Test
    public void callUnknownFunctionReturnsEmptyWithoutAbortingConfig() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <def var=\"result\"><call name=\"doesNotExist\"/></def>\n"
                + "  <def var=\"ok\">after</def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertTrue(ctx.getVar("result") instanceof EmptyVariable
                || ctx.getVar("result").toString().trim().isEmpty(),
                String.valueOf(ctx.getVar("result")));
        assertEquals(ctx.getVar("ok").toString(), "after");
    }

    @Test
    public void setVarEmptyBodyProducesEmptyVariable() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <set-var name=\"blank\"></set-var>\n"
                + "  <def var=\"blankSnapshot\"><get-var var=\"blank\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertTrue(ctx.getVar("blank") instanceof EmptyVariable
                || ctx.getVar("blank").toString().isEmpty()
                || ctx.getVar("blankSnapshot").toString().isEmpty(),
                String.valueOf(ctx.getVar("blank")));
    }

    @Test
    public void setVarValueAttributeExpressionIsEvaluated() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <set-var var=\"sum\" value=\"${1 + 4}\"/>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("sum").toString().trim(), "5");
    }
}
