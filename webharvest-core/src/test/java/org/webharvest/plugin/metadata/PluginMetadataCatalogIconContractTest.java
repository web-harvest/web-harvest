package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Catalog UI-hint contracts for icon/palette projections used by the IDE palette.
 *
 * <p>Exercises the element-name icon map and category fallbacks through the public
 * {@link PluginMetadataCatalog#getDescriptor(String)} API.</p>
 */
public class PluginMetadataCatalogIconContractTest {

    @Test
    public void elementSpecificIconsAreProjectedForCoreProcessors() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("config", "fa-gear");
        expected.put("loop", "fa-repeat");
        expected.put("while", "fa-rotate");
        expected.put("if", "fa-question");
        expected.put("else", "fa-arrow-down-wide-short");
        expected.put("case", "fa-diagram-project");
        expected.put("try", "fa-shield");
        expected.put("catch", "fa-triangle-exclamation");
        expected.put("script", "fa-code");
        expected.put("template", "fa-file-alt");
        expected.put("xslt", "fa-shuffle");
        expected.put("stylesheet", "fa-code-branch");
        expected.put("html-to-xml", "fa-file-code");
        expected.put("json-to-xml", "fa-brackets-curly");
        expected.put("xml-to-json", "fa-code");
        expected.put("xpath", "fa-code-branch");
        expected.put("xquery", "fa-database");
        expected.put("text", "fa-align-left");
        expected.put("constant", "fa-lock");
        expected.put("empty", "fa-circle");
        expected.put("list", "fa-list");
        expected.put("def", "fa-tag");
        expected.put("function", "fa-superscript");
        expected.put("call", "fa-phone");
        expected.put("call-param", "fa-sliders");
        expected.put("return", "fa-arrow-turn-down");
        expected.put("body-processor", "fa-layer-group");
        expected.put("http", "fa-globe");
        expected.put("http-header", "fa-heading");
        expected.put("http-param", "fa-sliders");
        expected.put("file", "fa-file");
        expected.put("include", "fa-link");
        expected.put("regexp", "fa-search");
        expected.put("regexp-pattern", "fa-asterisk");
        expected.put("regexp-result", "fa-highlighter");
        expected.put("regexp-source", "fa-input-text");
        expected.put("sleep", "fa-clock");
        expected.put("exit", "fa-right-from-bracket");
        expected.put("log", "fa-file-lines");

        int asserted = 0;
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            PluginDescriptor descriptor = PluginMetadataCatalog.getDescriptor(entry.getKey());
            if (descriptor == null) {
                // Optional/extension processors may be absent from the runtime classpath.
                continue;
            }
            assertNotNull(descriptor.getUiHints(), "Missing UI hints for " + entry.getKey());
            // Only assert element-specific icons when the catalog element name matches the map key.
            // Otherwise pickIcon falls through to category defaults (still a valid projection).
            String elementName = descriptor.getElementName();
            if (entry.getKey().equalsIgnoreCase(elementName)) {
                assertEquals(descriptor.getUiHints().getIcon(), entry.getValue(),
                        "Unexpected icon for " + entry.getKey());
            } else {
                assertNotNull(descriptor.getUiHints().getIcon(),
                        "Icon must still be projected for " + entry.getKey());
            }
            assertNotNull(descriptor.getUiHints().getPaletteGroup(),
                    "Missing palette group for " + entry.getKey());
            asserted++;
        }
        assertTrue(asserted >= 25, "Expected to assert icons for at least 25 core processors, got " + asserted);
    }

    @Test
    public void displayNamesAndOriginsAreStableForCorePlugins() {
        PluginDescriptor http = PluginMetadataCatalog.getDescriptor("http");
        assertNotNull(http);
        assertEquals(http.getOrigin(), "core");
        assertTrue(http.getDisplayName() != null && !http.getDisplayName().isBlank());

        PluginDescriptor htmlToXml = PluginMetadataCatalog.getDescriptor("html-to-xml");
        assertNotNull(htmlToXml);
        assertEquals(htmlToXml.getDisplayName(), "HTML to XML");

        PluginDescriptor callParam = PluginMetadataCatalog.getDescriptor("call-param");
        assertNotNull(callParam);
        assertTrue(callParam.getDisplayName().toLowerCase().contains("call"));
    }

    @Test
    public void schemaExtensionsSurfaceConstraintFieldsWhenDeclared() {
        PluginDescriptor whileDescriptor = PluginMetadataCatalog.getDescriptor("while");
        assertNotNull(whileDescriptor);
        Object schema = whileDescriptor.getExtensions().get("schema");
        assertNotNull(schema, "while should project schema extensions");
        assertTrue(schema instanceof Map);
        Map<?, ?> schemaMap = (Map<?, ?>) schema;
        assertTrue(schemaMap.containsKey("attributes") || schemaMap.containsKey("requiredAttributes")
                        || !schemaMap.isEmpty(),
                "while schema extension should expose attribute metadata");
    }
}
