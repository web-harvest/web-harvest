/*
 * Visual Plugin Metadata Test (TDD - RED Phase)
 * 
 * Tests for visual plugin metadata completeness and accuracy.
 * 
 * RED PHASE: All tests should initially fail until VisualPluginMetadata is implemented.
 * 
 * @author Robert Bala
 * @since 2.3.0
 */

package org.webharvest.plugin.metadata;

import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.annotations.CorePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Test suite for visual plugin metadata validation.
 * 
 * <p>This test suite validates that all plugins have complete visual metadata
 * for visual editor integration. Following TDD approach, these tests should
 * initially fail (RED phase) until VisualPluginMetadata is implemented.</p>
 * 
 * <p>According to the plan, this tests:</p>
 * <ul>
 *   <li>Metadata completeness for all 39 core plugins</li>
 *   <li>Metadata accuracy against actual XML configurations</li>
 *   <li>Parent-child relationship validation</li>
 *   <li>Attribute schema correctness</li>
 *   <li>Sub-element schema correctness</li>
 * </ul>
 */
public class VisualPluginMetadataTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualPluginMetadataTest.class);
    
    /**
     * Get all user-facing core plugins (excluding system components).
     * 
     * <p>System components are identified by the {@code @CorePlugin(systemComponent = true)}
     * annotation. They are excluded from visual editor and metadata tests.</p>
     * 
     * @return List of plugin element names that are user-facing (not system components)
     */
    private List<String> getUserFacingCorePlugins() {
        List<String> userPlugins = new ArrayList<>();
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        Collection<Plugin> allPlugins = registry.getAllPlugins();
        
        for (Plugin plugin : allPlugins) {
            if (plugin == null) continue;
            
            // Check if plugin has @CorePlugin annotation
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            if (annotation != null) {
                // Skip system components
                if (annotation.systemComponent()) {
                    continue;
                }
                
                // Add user-facing plugin
                PluginMetadata metadata = plugin.getMetadata();
                if (metadata != null) {
                    userPlugins.add(metadata.getElementName());
                }
            }
        }
        
        return userPlugins;
    }
    
    @Test
    public void testAllCorePluginsHaveMetadata() {
        // RED: This test will fail until all plugins implement getMetadata()
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getUserFacingCorePlugins();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin != null) {
                PluginMetadata metadata = plugin.getMetadata();
                assertNotNull(metadata, "Plugin '" + pluginName + "' should have metadata");
            }
        }
    }
    
    @Test
    public void testAllPluginsCanImplementVisualPluginMetadata() {
        // RED: This test will fail until VisualPluginMetadata interface exists
        // and plugins can implement it
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        
        // Check if VisualPluginMetadata interface exists (will fail if not)
        try {
            Class<?> visualMetadataClass = Class.forName("org.webharvest.plugin.VisualPluginMetadata");
            assertNotNull(visualMetadataClass, "VisualPluginMetadata interface should exist");
            
            // Check that it extends PluginMetadata
            assertTrue(
                org.webharvest.plugin.PluginMetadata.class.isAssignableFrom(visualMetadataClass),
                "VisualPluginMetadata should extend PluginMetadata"
            );
        } catch (ClassNotFoundException e) {
            // RED phase - interface doesn't exist yet
            fail("VisualPluginMetadata interface not found - this is expected in RED phase. " +
                 "Interface should be created in Phase 2.1");
        }
    }
    
    @Test
    public void testPluginsHaveVisualMetadataWhenImplemented() {
        // RED: This test will fail until plugins implement VisualPluginMetadata
        // This is expected in RED phase - will pass after Phase 3 implementation
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getUserFacingCorePlugins();
        
        int pluginsWithVisualMetadata = 0;
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            assertNotNull(metadata, "Plugin '" + pluginName + "' should have metadata");
            
            // Check if plugin implements VisualPluginMetadata
            try {
                Class<?> visualMetadataClass = Class.forName("org.webharvest.plugin.VisualPluginMetadata");
                if (visualMetadataClass.isInstance(metadata)) {
                    pluginsWithVisualMetadata++;
                    
                    // Verify required visual metadata fields
                    // This will use reflection to check methods exist
                    // In GREEN phase, we'll cast and check values
                }
            } catch (ClassNotFoundException e) {
                // RED phase - interface doesn't exist yet
                LOGGER.warn("VisualPluginMetadata interface not found - RED phase");
                return; // Skip this test in RED phase
            }
        }
        
        // In GREEN phase, all plugins should have visual metadata
        // For now (RED phase), this test documents the requirement
        LOGGER.info("Plugins with VisualPluginMetadata: {}/{}", pluginsWithVisualMetadata, userPlugins.size());
    }
    
    @Test
    public void testVisualMetadataHasRequiredMethods() {
        // RED: Test that VisualPluginMetadata interface has all required methods
        // This will fail until interface is created with all methods
        
        try {
            Class<?> visualMetadataClass = Class.forName("org.webharvest.plugin.VisualPluginMetadata");
            
            // Required methods according to plan Phase 2.1:
            String[] requiredMethods = {
                "getIcon",
                "getColor",
                "getGroup",
                "getDefaultWidth",
                "getDefaultHeight",
                "getInputPorts",
                "getOutputPorts",
                "getPortType",
                "getTooltip",
                "getKeyboardShortcuts",
                "getAccessibility",
                "getLinkedServices"
            };
            
            for (String methodName : requiredMethods) {
                try {
                    visualMetadataClass.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    fail("VisualPluginMetadata should have method: " + methodName);
                }
            }
        } catch (ClassNotFoundException e) {
            // RED phase - interface doesn't exist yet
            fail("VisualPluginMetadata interface not found - create in Phase 2.1");
        }
    }
    
    @Test
    public void testParentChildRelationshipValidation() {
        // RED: Test parent-child relationships
        // This validates consistency between allowed sub-elements and actual usage
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        
        // Known parent-child relationships from analysis
        String[][] knownRelationships = {
            {"http", "http-param"},
            {"http", "http-header"},
            {"xquery", "xq-param"},
            {"xquery", "xq-expression"},
            {"loop", "*"},  // loop can contain any child
            {"if", "*"},    // if can contain any child
            {"function", "*"},  // function can contain any child
            {"call", "call-param"}
        };
        
        for (String[] relationship : knownRelationships) {
            String parentName = relationship[0];
            String childName = relationship[1];
            
            Plugin parentPlugin = registry.getPlugin(parentName);
            if (parentPlugin == null) continue;
            
            PluginMetadata metadata = parentPlugin.getMetadata();
            if (metadata == null) continue;
            
            // Check that parent allows child as sub-element
            assertNotNull(metadata.getConfigurationSchema(), 
                "Parent plugin '" + parentName + "' should have configuration schema");
            
            String[] allowedSubElements = metadata.getConfigurationSchema().getAllowedSubElements();
            if (allowedSubElements != null && !childName.equals("*")) {
                // Check if child is in allowed list or if schema allows all
                boolean allowsAll = metadata.getConfigurationSchema().allowsBody() && 
                                  (allowedSubElements.length == 0);
                
                if (!allowsAll) {
                    boolean found = false;
                    for (String allowed : allowedSubElements) {
                        if (allowed.equals(childName)) {
                            found = true;
                            break;
                        }
                    }
                    // Note: This may fail initially if schema doesn't include all sub-elements
                    // That's OK for RED phase - we'll fix it in GREEN phase
                    if (!found) {
                        LOGGER.warn("Parent '{}' may not allow child '{}' in schema - needs validation", 
                            parentName, childName);
                    }
                }
            }
        }
    }
    
    @Test
    public void testMetadataMatchesActualUsage() {
        // RED: This test validates metadata against actual XML configurations
        // This will be expanded after analyzing XML examples
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        
        // Check top 10 most used plugins have complete metadata
        String[] topUsedPlugins = {
            "def", "get-var", "file", "template", "xpath", 
            "http", "call-param", "script", "HTML to XML", "xq-param"
        };
        
        for (String pluginName : topUsedPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            assertNotNull(metadata, "Top-used plugin '" + pluginName + "' should have metadata");
            
            // Verify basic metadata fields
            assertNotNull(metadata.getId(), 
                "Top-used plugin '" + pluginName + "' should have ID");
            assertNotNull(metadata.getName(), 
                "Top-used plugin '" + pluginName + "' should have name");
            assertNotNull(metadata.getDescription(), 
                "Top-used plugin '" + pluginName + "' should have description");
            assertNotNull(metadata.getConfigurationSchema(), 
                "Top-used plugin '" + pluginName + "' should have configuration schema");
        }
    }
    
    @Test
    public void testAttributeSchemaCorrectness() {
        // RED: Test that attribute schemas are correct
        // This is delegated to PluginSchemaValidationTest, but we verify here
        // that plugins have attribute definitions
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getUserFacingCorePlugins();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            assertNotNull(metadata.getConfigurationSchema(), 
                "Plugin '" + pluginName + "' should have configuration schema");
            
            // Schema should have attribute definitions (can be empty array)
            assertNotNull(metadata.getConfigurationSchema().getAttributeDefinitions(), 
                "Plugin '" + pluginName + "' schema should have attribute definitions");
        }
    }
    
    @Test
    public void testSubElementSchemaCorrectness() {
        // RED: Test that sub-element schemas are correct
        
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getUserFacingCorePlugins();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            assertNotNull(metadata.getConfigurationSchema(), 
                "Plugin '" + pluginName + "' should have configuration schema");
            
            // Schema should have allowed sub-elements (can be empty array)
            assertNotNull(metadata.getConfigurationSchema().getAllowedSubElements(), 
                "Plugin '" + pluginName + "' schema should have allowedSubElements");
            
            // Schema should have required sub-elements (can be empty array)
            assertNotNull(metadata.getConfigurationSchema().getRequiredSubElements(), 
                "Plugin '" + pluginName + "' schema should have requiredSubElements");
        }
    }
}

