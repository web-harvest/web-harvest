package org.webharvest.plugin.core;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.integration.XmlBasedIntegrationTestBase;
import org.webharvest.plugin.metadata.PluginMetadataJsonGenerator;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Meaningful residual branch contracts for Core plugins still below Gate C
 * after the quality/cleanup tip. Each case asserts observable behavior
 * (validation rejection, empty results, return shaping, loop aggregation).
 */
public class StabilizationResidualBranchContractTest extends XmlBasedIntegrationTestBase {

    private static final String NS = "http://org.webharvest/schema/2.1/core";

    @Test(expectedExceptions = PluginValidationException.class)
    public void scriptValidateRejectsNullConfiguration() throws Exception {
        new ScriptPlugin().validateConfiguration(null);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void scriptValidateRejectsUnsupportedLanguage() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "script", NS, "script");
        configuration.setProperty("language", "not-a-real-language");
        new ScriptPlugin().validateConfiguration(configuration);
    }

    @Test
    public void scriptValidateAcceptsKnownLanguageAndBlankLanguage() throws Exception {
        DefaultPluginConfiguration groovy = new DefaultPluginConfiguration(
                "script", NS, "script");
        groovy.setProperty("language", "groovy");
        new ScriptPlugin().validateConfiguration(groovy);

        DefaultPluginConfiguration blank = new DefaultPluginConfiguration(
                "script", NS, "script");
        blank.setProperty("language", "   ");
        new ScriptPlugin().validateConfiguration(blank);

        DefaultPluginConfiguration omitted = new DefaultPluginConfiguration(
                "script", NS, "script");
        new ScriptPlugin().validateConfiguration(omitted);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void includeSchemaRejectsMissingPath() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "include", NS, "include");
        new IncludePlugin().getConfigurationSchema().validate(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void includeValidateRejectsBlankPath() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "include", NS, "include");
        configuration.setProperty("path", "  ");
        new IncludePlugin().validateConfiguration(configuration);
    }

    @Test
    public void includeValidateAcceptsConcretePath() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "include", NS, "include");
        configuration.setProperty("path", "fragments/shared.xml");
        new IncludePlugin().validateConfiguration(configuration);
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void getVarValidateRejectsMissingVarName() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "get", NS, "get");
        new GetVarPlugin().validateConfiguration(configuration);
    }

    @Test
    public void getVarReadsTemplatedVariableName() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <def var=\"targetName\">payload</def>\n"
                + "  <def var=\"payload\">stored-value</def>\n"
                + "  <def var=\"result\"><get var=\"${targetName}\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString(), "stored-value");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void getVarFailsWhenVariableIsUndefined() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "get", NS, "get");
        configuration.setProperty("var", "doesNotExist");
        GetVarPlugin plugin = new GetVarPlugin();
        plugin.initialize(configuration);
        plugin.execute(new ScraperContext());
    }

    @Test
    public void xmlPluginReturnsBodyContentAndEmptyBodyStaysEmpty() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "xml", NS, "xml");
        configuration.setProperty("validate", "false");

        XMLPlugin withContent = new XMLPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("<item id=\"1\"/>");
            }
        };
        withContent.initialize(configuration);
        Variable content = withContent.execute(new ScraperContext());
        assertTrue(content.toString().contains("item"), content.toString());

        XMLPlugin emptyBody = new XMLPlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return EmptyVariable.INSTANCE;
            }
        };
        emptyBody.initialize(configuration);
        Variable emptyResult = emptyBody.execute(new ScraperContext());
        assertTrue(emptyResult instanceof EmptyVariable || emptyResult.isEmpty(),
                String.valueOf(emptyResult));
    }

    @Test
    public void callMissingFunctionReturnsEmptyWithoutAbortingConfig() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <def var=\"result\"><call name=\"missingFunction\"/></def>\n"
                + "  <def var=\"after\">ok</def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertTrue(result instanceof EmptyVariable || result.isEmpty(), String.valueOf(result));
        assertEquals(ctx.getVar("after").toString(), "ok");
    }

    @Test
    public void callWithoutExplicitReturnUsesLastBodyExpression() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"lastExpr\">\n"
                + "    <template>from-body</template>\n"
                + "  </function>\n"
                + "  <def var=\"result\"><call name=\"lastExpr\"/></def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertTrue(ctx.getVar("result").toString().contains("from-body"),
                ctx.getVar("result").toString());
    }

    @Test
    public void callFindsNestedFunctionDefinitionsInConfigurationTree() throws Exception {
        org.webharvest.definition.PluginConfigurationTree root =
                new org.webharvest.definition.PluginConfigurationTree(
                        new DefaultPluginConfiguration("config", NS, "config"),
                        "config",
                        null);
        org.webharvest.definition.PluginConfigurationTree wrapper =
                new org.webharvest.definition.PluginConfigurationTree(
                        new DefaultPluginConfiguration("case", NS, "case"),
                        "case",
                        null);
        DefaultPluginConfiguration functionConfig = new DefaultPluginConfiguration(
                "function", NS, "function");
        functionConfig.setProperty("name", "nested");
        org.webharvest.definition.PluginConfigurationTree nested =
                new org.webharvest.definition.PluginConfigurationTree(
                        functionConfig, "function", null);
        wrapper.addChild(nested);
        root.addChild(wrapper);

        java.lang.reflect.Method finder = CallPlugin.class.getDeclaredMethod(
                "findFunctionConfiguration",
                org.webharvest.definition.PluginConfigurationTree.class,
                String.class);
        finder.setAccessible(true);
        Object found = finder.invoke(new CallPlugin(), root, "nested");
        assertNotNull(found, "Recursive tree search must locate nested function configs");
        assertNull(finder.invoke(new CallPlugin(), root, "missing"));
        assertNull(finder.invoke(new CallPlugin(), null, "nested"));
    }

    @Test
    public void callSingleCharacterParamNameIsCapitalizedForLookup() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <function name=\"echo\">\n"
                + "    <return><template>${callParamX}</template></return>\n"
                + "  </function>\n"
                + "  <def var=\"result\">\n"
                + "    <call name=\"echo\"><call-param name=\"x\">Z</call-param></call>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        assertEquals(ctx.getVar("result").toString().trim(), "Z");
    }

    @Test
    public void whileEmptyFlagSuppressesBodyAggregation() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<config xmlns=\"" + NS + "\">\n"
                + "  <def var=\"i\">0</def>\n"
                + "  <def var=\"result\">\n"
                + "    <while test=\"${i &lt; 2}\" maxloops=\"2\" index=\"i\" empty=\"true\">\n"
                + "      <template>tick</template>\n"
                + "    </while>\n"
                + "  </def>\n"
                + "</config>";
        DynamicScopeContext ctx = executeXmlString(xml);
        Variable result = ctx.getVar("result");
        assertNotNull(result);
        assertFalse(result.toString().contains("tick"),
                "empty=true must not aggregate body output: " + result);
    }

    @Test
    public void whileFallsBackFromTestToConditionAttribute() throws Exception {
        WhilePlugin plugin = new WhilePlugin() {
            @Override
            protected Variable executeBody(DynamicScopeContext context) {
                return new NodeVariable("once");
            }
        };
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", NS, "while");
        // Prefer legacy 'condition' when 'test' is absent/empty.
        configuration.setProperty("condition", "true");
        configuration.setProperty("maxloops", "1");
        plugin.initialize(configuration);
        Variable result = plugin.execute(new ScraperContext());
        assertTrue(result instanceof ListVariable || result.toString().contains("once"),
                String.valueOf(result));
    }

    @Test(expectedExceptions = PluginValidationException.class)
    public void whileValidateRejectsNonNumericMaxloops() throws Exception {
        DefaultPluginConfiguration configuration = new DefaultPluginConfiguration(
                "while", NS, "while");
        configuration.setProperty("maxloops", "abc");
        new WhilePlugin().validateConfiguration(configuration);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void jsonGeneratorRejectsNullOutputPath() throws Exception {
        new PluginMetadataJsonGenerator().generate(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void jsonGeneratorRejectsBlankOutputPath() throws Exception {
        new PluginMetadataJsonGenerator().generate("   ");
    }

    @Test
    public void normalizeEntirelyBracketedStringStripsOuterBrackets() throws Exception {
        java.lang.reflect.Method normalize = CallPlugin.class.getDeclaredMethod(
                "normalizeReturnValue", Variable.class);
        normalize.setAccessible(true);
        Variable result = (Variable) normalize.invoke(
                new CallPlugin(), new NodeVariable("[wrapped-value]"));
        assertEquals(result.toString(), "wrapped-value");
    }
}
