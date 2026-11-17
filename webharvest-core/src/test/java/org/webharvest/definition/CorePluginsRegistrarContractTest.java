package org.webharvest.definition;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.core.*;

import static org.testng.Assert.assertEquals;

/**
 * Exhaustive contract for the XML element names registered by the default Core runtime.
 *
 * <p>This intentionally verifies the executable registrar, not the separate metadata
 * scanner/catalog. The expected set includes the legacy {@code set-var} alias.</p>
 */
public class CorePluginsRegistrarContractTest {

    @BeforeMethod
    public void setUp() {
        CorePluginsRegistrar.reset();
        CorePluginsRegistrar.registerCorePlugins();
    }

    @AfterMethod
    public void tearDown() {
        CorePluginsRegistrar.reset();
    }

    @Test
    public void registersExactlyTheExpectedCoreXmlMappings() {
        Map<String, Class<? extends Plugin>> expected = expectedMappings();
        CorePluginRegistry registry = CorePluginRegistry.getInstance();

        assertEquals(expected.size(), 44, "The reviewed Core XML mapping baseline contains 44 entries");
        Set<String> registeredNames = registry.getRegisteredElementNames();
        assertEquals(registeredNames, expected.keySet(),
                "The default registrar must register exactly the reviewed XML element names");

        for (Map.Entry<String, Class<? extends Plugin>> mapping : expected.entrySet()) {
            assertEquals(registry.getPluginClass(mapping.getKey()), mapping.getValue(),
                    "Unexpected implementation registered for XML element <" + mapping.getKey() + ">");
        }
    }

    private Map<String, Class<? extends Plugin>> expectedMappings() {
        Map<String, Class<? extends Plugin>> mappings = new LinkedHashMap<>();
        mappings.put("call", CallPlugin.class);
        mappings.put("call-param", CallParamPlugin.class);
        mappings.put("case", CasePlugin.class);
        mappings.put("catch", CatchPlugin.class);
        mappings.put("constant", ConstantPlugin.class);
        mappings.put("def", DefPlugin.class);
        mappings.put("else", ElsePlugin.class);
        mappings.put("empty", EmptyPlugin.class);
        mappings.put("exit", ExitPlugin.class);
        mappings.put("file", FilePlugin.class);
        mappings.put("function", FunctionPlugin.class);
        mappings.put("get", GetVarPlugin.class);
        mappings.put("html-to-xml", HtmlToXmlPlugin.class);
        mappings.put("http", HttpPlugin.class);
        mappings.put("http-header", HttpHeaderPlugin.class);
        mappings.put("http-param", HttpParamPlugin.class);
        mappings.put("if", IfPlugin.class);
        mappings.put("include", IncludePlugin.class);
        mappings.put("json-to-xml", JsonToXmlPlugin.class);
        mappings.put("loop", LoopPlugin.class);
        mappings.put("regexp", RegexpPlugin.class);
        mappings.put("regexp-pattern", RegexpPatternPlugin.class);
        mappings.put("regexp-result", RegexpResultPlugin.class);
        mappings.put("regexp-source", RegexpSourcePlugin.class);
        mappings.put("return", ReturnPlugin.class);
        mappings.put("script", ScriptPlugin.class);
        mappings.put("list", ListPlugin.class);
        mappings.put("log", LogPlugin.class);
        mappings.put("value-of", ValueOfPlugin.class);
        mappings.put("xml", XMLPlugin.class);
        mappings.put("set-var", SetVarPlugin.class);
        mappings.put("sleep", SleepPlugin.class);
        mappings.put("stylesheet", StylesheetPlugin.class);
        mappings.put("template", TemplatePlugin.class);
        mappings.put("text", TextPlugin.class);
        mappings.put("tokenize", TokenizePlugin.class);
        mappings.put("try", TryPlugin.class);
        mappings.put("while", WhilePlugin.class);
        mappings.put("xml-to-json", XmlToJsonPlugin.class);
        mappings.put("xpath", XPathPlugin.class);
        mappings.put("xquery", XQueryPlugin.class);
        mappings.put("xq-expression", XQExpressionPlugin.class);
        mappings.put("xq-param", XQParamPlugin.class);
        mappings.put("xslt", XsltPlugin.class);
        return Collections.unmodifiableMap(mappings);
    }
}
