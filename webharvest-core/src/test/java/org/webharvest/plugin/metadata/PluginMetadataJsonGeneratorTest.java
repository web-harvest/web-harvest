/*
 * Plugin Metadata JSON Generator Test (TDD - RED Phase)
 * 
 * Tests for JSON generation from VisualPluginMetadata objects.
 * 
 * RED PHASE: Tests should initially fail until JSON generator is implemented.
 * 
 * @author Robert Bala
 * @since 2.3.0
 */

package org.webharvest.plugin.metadata;

import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.annotations.CorePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import static org.testng.Assert.*;

/**
 * Test suite for plugin metadata JSON generator.
 * 
 * <p>This test suite validates that:</p>
 * <ul>
 *   <li>JSON generator can serialize VisualPluginMetadata to JSON</li>
 *   <li>Generated JSON contains all required fields</li>
 *   <li>JSON structure matches expected schema</li>
 *   <li>All user-facing plugins are included in JSON output</li>
 *   <li>System components are excluded from JSON output</li>
 * </ul>
 */
public class PluginMetadataJsonGeneratorTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMetadataJsonGeneratorTest.class);
    
    /**
     * Get all user-facing core plugins (excluding system components).
     * 
     * @return List of plugin element names that are user-facing (not system components)
     */
    private List<String> getUserFacingCorePlugins() {
        List<String> userPlugins = new ArrayList<>();
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        Collection<Plugin> allPlugins = registry.getAllPlugins();
        
        for (Plugin plugin : allPlugins) {
            if (plugin == null) continue;
            
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            if (annotation != null && !annotation.systemComponent() && annotation.paletteComponent()) {
                PluginMetadata metadata = plugin.getMetadata();
                if (metadata != null) {
                    // The registry is keyed by metadata ID, not XML element name.
                    userPlugins.add(metadata.getId());
                }
            }
        }
        
        return userPlugins;
    }
    
    @Test
    public void testLegacyProcessorsRemainRegisteredOutsidePalette() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        String[] legacyIds = {"var", "set-var"};
        for (String id : legacyIds) {
            Plugin plugin = registry.getPlugin(id);
            assertNotNull(plugin, "Legacy processor must remain registered: " + id);
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            assertNotNull(annotation, "Legacy processor must retain CorePlugin registration: " + id);
            assertFalse(annotation.paletteComponent(), "Legacy processor must not appear in visual palette: " + id);
            assertFalse(annotation.systemComponent(), "Legacy compatibility processor is not an internal system component: " + id);
        }
    }

    @Test
    public void testInternalProcessorsAreSystemAndNotPaletteComponents() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        String[] internalIds = {"config", "body-processor"};
        for (String id : internalIds) {
            Plugin plugin = registry.getPlugin(id);
            assertNotNull(plugin, "Internal processor must remain registered: " + id);
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            assertNotNull(annotation, "Internal processor must retain CorePlugin registration: " + id);
            assertTrue(annotation.systemComponent(), "Internal processor must be classified as system: " + id);
            assertFalse(annotation.paletteComponent(), "Internal processor must not appear in visual palette: " + id);
        }
    }

    @Test
    public void testJsonGeneratorExists() {
        // RED: This test will fail until PluginMetadataJsonGenerator class exists
        try {
            Class<?> generatorClass = Class.forName("org.webharvest.plugin.metadata.PluginMetadataJsonGenerator");
            assertNotNull(generatorClass, "PluginMetadataJsonGenerator class should exist");
        } catch (ClassNotFoundException e) {
            fail("PluginMetadataJsonGenerator class not found - this is expected in RED phase. " +
                 "Class should be created in Phase 3.1");
        }
    }
    
    @Test
    public void testJsonGeneratorCanGenerateJson() throws Exception {
        // RED: This test will fail until JSON generator is implemented
        
        // This test will fail until implementation exists
        try {
            Class<?> generatorClass = Class.forName("org.webharvest.plugin.metadata.PluginMetadataJsonGenerator");
            Object generator = generatorClass.getDeclaredConstructor().newInstance();
            
            // Try to call generate method (will fail if method doesn't exist)
            java.lang.reflect.Method generateMethod = generatorClass.getMethod("generate", String.class);
            
            // Create temporary output directory
            Path tempDir = Files.createTempDirectory("plugin-metadata-test");
            String outputPath = tempDir.resolve("plugins.json").toString();
            
            // Generate JSON
            generateMethod.invoke(generator, outputPath);
            
            // Verify file was created
            File outputFile = new File(outputPath);
            assertTrue(outputFile.exists(), "Generated JSON file should exist");
            assertTrue(outputFile.length() > 0, "Generated JSON file should not be empty");
            
            // Cleanup
            Files.deleteIfExists(Paths.get(outputPath));
            Files.deleteIfExists(tempDir);
            
        } catch (ClassNotFoundException e) {
            fail("PluginMetadataJsonGenerator class not found - implement in Phase 3.1");
        } catch (NoSuchMethodException e) {
            fail("PluginMetadataJsonGenerator.generate() method not found - implement in Phase 3.1");
        }
    }
    
    @Test
    public void testGeneratedJsonContainsAllPlugins() throws Exception {
        // RED: This test validates that all user-facing plugins are in JSON
        
        List<String> userPlugins = getUserFacingCorePlugins();
        assertFalse(userPlugins.isEmpty(), "Should have at least one user-facing plugin");
        
        try {
            Class<?> generatorClass = Class.forName("org.webharvest.plugin.metadata.PluginMetadataJsonGenerator");
            Object generator = generatorClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method generateMethod = generatorClass.getMethod("generate", String.class);
            
            Path tempDir = Files.createTempDirectory("plugin-metadata-test");
            String outputPath = tempDir.resolve("plugins.json").toString();
            
            generateMethod.invoke(generator, outputPath);
            
            // Read generated JSON
            String jsonContent = new String(Files.readAllBytes(Paths.get(outputPath)));
            assertNotNull(jsonContent, "JSON content should not be null");
            assertFalse(jsonContent.trim().isEmpty(), "JSON content should not be empty");
            
            // Verify all plugins are present (basic check - JSON contains element names)
            for (String pluginName : userPlugins) {
                assertTrue(jsonContent.contains(pluginName), 
                    "Generated JSON should contain plugin: " + pluginName);
            }
            
            // Cleanup
            Files.deleteIfExists(Paths.get(outputPath));
            Files.deleteIfExists(tempDir);
            
        } catch (ClassNotFoundException e) {
            fail("PluginMetadataJsonGenerator not implemented yet - Phase 3.1");
        }
    }
    
    @Test
    public void testGeneratedJsonIsValidJson() throws Exception {
        // RED: This test validates JSON syntax
        
        try {
            Class<?> generatorClass = Class.forName("org.webharvest.plugin.metadata.PluginMetadataJsonGenerator");
            Object generator = generatorClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method generateMethod = generatorClass.getMethod("generate", String.class);
            
            Path tempDir = Files.createTempDirectory("plugin-metadata-test");
            String outputPath = tempDir.resolve("plugins.json").toString();
            
            generateMethod.invoke(generator, outputPath);
            
            // Read and validate JSON
            String jsonContent = new String(Files.readAllBytes(Paths.get(outputPath)));
            
            // Try to parse JSON using Gson (should not throw exception)
            com.google.gson.Gson gson = new com.google.gson.Gson();
            try {
                Object parsed = gson.fromJson(jsonContent, Object.class);
                assertNotNull(parsed, "JSON should be parseable");
            } catch (com.google.gson.JsonSyntaxException e) {
                fail("Generated JSON is not valid JSON: " + e.getMessage());
            }
            
            // Cleanup
            Files.deleteIfExists(Paths.get(outputPath));
            Files.deleteIfExists(tempDir);
            
        } catch (ClassNotFoundException e) {
            fail("PluginMetadataJsonGenerator not implemented yet - Phase 3.1");
        }
    }
    
    @Test
    public void testGeneratedJsonContainsRequiredFields() throws Exception {
        // RED: This test validates JSON structure contains required VisualPluginMetadata fields
        
        try {
            Class<?> generatorClass = Class.forName("org.webharvest.plugin.metadata.PluginMetadataJsonGenerator");
            Object generator = generatorClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method generateMethod = generatorClass.getMethod("generate", String.class);
            
            Path tempDir = Files.createTempDirectory("plugin-metadata-test");
            String outputPath = tempDir.resolve("plugins.json").toString();
            
            generateMethod.invoke(generator, outputPath);
            
            String jsonContent = new String(Files.readAllBytes(Paths.get(outputPath)));
            
            // Check for required VisualPluginMetadata fields
            assertTrue(jsonContent.contains("icon") || jsonContent.contains("\"icon\""), 
                "JSON should contain 'icon' field");
            assertTrue(jsonContent.contains("color") || jsonContent.contains("\"color\""), 
                "JSON should contain 'color' field");
            assertTrue(jsonContent.contains("group") || jsonContent.contains("\"group\""), 
                "JSON should contain 'group' field");
            
            // Cleanup
            Files.deleteIfExists(Paths.get(outputPath));
            Files.deleteIfExists(tempDir);
            
        } catch (ClassNotFoundException e) {
            fail("PluginMetadataJsonGenerator not implemented yet - Phase 3.1");
        }
    }
}

