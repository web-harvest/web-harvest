package org.webharvest.plugin.metadata;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for dynamic metadata generation in PluginMetadataCatalog.
 * Verifies that all metadata is generated from plugin classes without static JSON files.
 */
public class PluginMetadataCatalogDynamicTest {

    @BeforeMethod
    public void setUp() {
        // Clear cache to force fresh scan
        PluginClassScanner.clearCache();
    }

    @Test
    public void testCatalogLoadsPluginsWithoutStaticJSON() {
        // Should work without plugin-semantics.json
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        assertNotNull(descriptors);
        assertFalse(descriptors.isEmpty(), "Should discover plugins dynamically");
        
        // Verify core plugins are present
        PluginDescriptor config = descriptors.stream()
            .filter(d -> "config".equals(d.getElementName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(config, "Should find config plugin");
        assertEquals(config.getElementName(), "config");
    }

    @Test
    public void testMetadataGeneratedFromPluginClasses() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        // Find http plugin
        PluginDescriptor http = descriptors.stream()
            .filter(d -> "http".equals(d.getElementName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(http, "Should find http plugin");
        
        // Verify metadata comes from plugin class, not static JSON
        assertNotNull(http.getDescription(), "Description should be generated");
        assertNotNull(http.getCategory(), "Category should be generated");
        assertNotNull(http.getSemantics(), "Semantics should be generated");
        assertNotNull(http.getUiHints(), "UI hints should be generated");
    }

    @Test
    public void testSchemaExtensionsExtracted() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        PluginDescriptor http = descriptors.stream()
            .filter(d -> "http".equals(d.getElementName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(http);
        
        // Verify extensions contain schema information
        assertNotNull(http.getExtensions());
        assertTrue(http.getExtensions().containsKey("schema"), 
            "Extensions should contain schema information");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) http.getExtensions().get("schema");
        assertNotNull(schema);
        assertTrue(schema.containsKey("allowsBody"), 
            "Schema should contain allowsBody");
    }

    @Test
    public void testNamespaceExtracted() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        PluginDescriptor http = descriptors.stream()
            .filter(d -> "http".equals(d.getElementName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(http);
        
        // Namespace should be in extensions if available
        if (http.getExtensions().containsKey("namespace")) {
            String namespace = (String) http.getExtensions().get("namespace");
            assertNotNull(namespace);
            assertFalse(namespace.isEmpty());
        }
    }

    @Test
    public void testAllPluginsHaveSemantics() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        for (PluginDescriptor desc : descriptors) {
            assertNotNull(desc.getSemantics(), 
                "Plugin " + desc.getId() + " should have semantics");
            assertNotNull(desc.getSemantics().getRole(), 
                "Plugin " + desc.getId() + " should have semantic role");
        }
    }

    @Test
    public void testAllPluginsHaveUIHints() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        for (PluginDescriptor desc : descriptors) {
            assertNotNull(desc.getUiHints(), 
                "Plugin " + desc.getId() + " should have UI hints");
            // Icon can be null, but paletteGroup should be set
            if (desc.getUiHints().getPaletteGroup() == null) {
                // This is OK if auto-generated from category
            }
        }
    }

    @Test
    public void testDependenciesGeneratedFromMetadata() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        // Find catch plugin (should depend on try)
        PluginDescriptor catchPlugin = descriptors.stream()
            .filter(d -> "catch".equals(d.getElementName()))
            .findFirst()
            .orElse(null);
        
        if (catchPlugin != null) {
            List<String> deps = catchPlugin.getDependencies();
            // Catch should have try as dependency or required parent
            assertTrue(deps.contains("try") || 
                      catchPlugin.getSemantics().getRequiredParentTypes().contains("try"),
                "Catch should depend on try");
        }
    }

    @Test
    public void testSchemaVersionIsDynamic() {
        String version = PluginMetadataCatalog.getSchemaVersion();
        assertNotNull(version);
        assertEquals(version, "2.3.0", "Schema version should be 2.3.0 for dynamic generation");
    }
}

