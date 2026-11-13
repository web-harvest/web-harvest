package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;

/**
 * Palette JSON contracts for visual extras projected from live plugin metadata
 * (accessibility, parent relationships, empty keyboard shortcut arrays).
 */
public class PluginMetadataJsonGeneratorVisualExtrasBranchContractTest {

    @Test
    public void generateProjectsAccessibilityAndParentRelationshipForXqExpression() throws Exception {
        Path tempDir = Files.createTempDirectory("wh-metadata-json-visual-");
        Path output = tempDir.resolve("plugins.json");
        try {
            new PluginMetadataJsonGenerator().generate(output.toString());
            JsonObject root = JsonParser.parseString(Files.readString(output)).getAsJsonObject();
            JsonArray plugins = root.getAsJsonArray("plugins");
            assertNotNull(plugins);

            JsonObject xqExpression = findById(plugins, "xq-expression");
            if (xqExpression == null) {
                xqExpression = findByElementName(plugins, "xq-expression");
            }
            assertNotNull(xqExpression, "Palette JSON must contain xq-expression");

            assertTrue(xqExpression.has("accessibility"),
                    "xq-expression should project accessibility metadata");
            JsonObject accessibility = xqExpression.getAsJsonObject("accessibility");
            assertTrue(accessibility.has("ariaLabel") || accessibility.has("ariaDescription"));

            assertTrue(xqExpression.has("parentRelationship"),
                    "xq-expression should project parent relationship to xquery");
            JsonObject parent = xqExpression.getAsJsonObject("parentRelationship");
            assertEquals(parent.get("parentElementName").getAsString(), "xquery");
            assertTrue(parent.get("required").getAsBoolean());

            if (xqExpression.has("keyboardShortcuts")) {
                assertTrue(xqExpression.get("keyboardShortcuts").isJsonArray());
            }
            if (xqExpression.has("linkedServices")) {
                assertTrue(xqExpression.get("linkedServices").isJsonArray());
            }
        } finally {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                            // best-effort cleanup
                        }
                    });
        }
    }

    private static JsonObject findById(JsonArray plugins, String id) {
        for (JsonElement element : plugins) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("id") && id.equals(obj.get("id").getAsString())) {
                return obj;
            }
        }
        return null;
    }

    private static JsonObject findByElementName(JsonArray plugins, String elementName) {
        for (JsonElement element : plugins) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("elementName") && elementName.equals(obj.get("elementName").getAsString())) {
                return obj;
            }
        }
        return null;
    }
}
