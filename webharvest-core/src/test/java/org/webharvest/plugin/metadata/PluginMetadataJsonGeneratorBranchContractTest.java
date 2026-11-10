package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.Test;

/**
 * Generate/convert error paths and serialized structure checks for palette JSON.
 */
public class PluginMetadataJsonGeneratorBranchContractTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void generateRejectsBlankOutputPath() throws Exception {
        new PluginMetadataJsonGenerator().generate("   ");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void generateRejectsNullOutputPath() throws Exception {
        new PluginMetadataJsonGenerator().generate(null);
    }

    @Test
    public void generateCreatesParentDirectoriesAndSemanticsForHttpPlugin() throws Exception {
        Path tempDir = Files.createTempDirectory("wh-metadata-json-branch-");
        Path nested = tempDir.resolve("nested/deep/plugins.json");
        try {
            new PluginMetadataJsonGenerator().generate(nested.toString());
            assertTrue(Files.isRegularFile(nested), "Output file must exist");

            JsonObject root = JsonParser.parseString(Files.readString(nested)).getAsJsonObject();
            assertTrue(root.get("pluginCount").getAsInt() > 0);
            assertNotNull(root.get("generatedAt"));
            JsonArray plugins = root.getAsJsonArray("plugins");
            assertNotNull(plugins);

            JsonObject http = null;
            for (var element : plugins) {
                JsonObject obj = element.getAsJsonObject();
                if ("http".equals(obj.get("id").getAsString())) {
                    http = obj;
                    break;
                }
            }
            assertNotNull(http, "Palette JSON must contain http plugin entry");
            assertNotNull(http.get("semantics"), "HTTP entry should include resolved semantics");
            assertNotNull(http.get("inputPorts"));
            assertNotNull(http.get("outputPorts"));
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
}
