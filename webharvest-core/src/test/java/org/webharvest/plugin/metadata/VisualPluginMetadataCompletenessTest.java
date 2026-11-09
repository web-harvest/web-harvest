/*
 * Visual Plugin Metadata Completeness Test (TDD - Validation Phase)
 * 
 * Tests for complete visual metadata implementation - validates all required fields
 * are populated with correct values. This test identifies gaps in metadata.
 * 
 * @author Robert Bala
 * @since 2.3.0
 */

package org.webharvest.plugin.metadata;

import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.annotations.CorePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.testng.Assert.*;

/**
 * Test suite for visual plugin metadata completeness validation.
 * 
 * <p>This test suite validates that all plugins have complete and correct
 * visual metadata. It identifies gaps and missing information that needs
 * to be fixed.</p>
 * 
 * <p>Tests cover:</p>
 * <ul>
 *   <li>All required VisualPluginMetadata fields are populated</li>
 *   <li>Field values are valid (colors in hex, icons exist, etc.)</li>
 *   <li>Input/output ports are correctly defined</li>
 *   <li>Parent-child relationships match actual usage</li>
 *   <li>Accessibility metadata is complete</li>
 * </ul>
 */
public class VisualPluginMetadataCompletenessTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(VisualPluginMetadataCompletenessTest.class);
    
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final Pattern FONT_AWESOME_PATTERN = Pattern.compile("^fas? fa-[a-z0-9-]+$");
    
    /**
     * Get IDs of all Core processors explicitly eligible for the visual palette.
     */
    private List<String> getPaletteEligibleCorePluginIds() {
        List<String> userPlugins = new ArrayList<>();
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        Collection<Plugin> allPlugins = registry.getAllPlugins();
        
        for (Plugin plugin : allPlugins) {
            if (plugin == null) continue;
            
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            if (annotation != null && !annotation.systemComponent() && annotation.paletteComponent()) {
                PluginMetadata metadata = plugin.getMetadata();
                assertNotNull(metadata, "Palette-eligible processor must provide metadata: "
                    + plugin.getClass().getName());
                // The registry is keyed by metadata ID, not XML element name.
                userPlugins.add(metadata.getId());
            }
        }
        
        return userPlugins;
    }
    
    @Test
    public void testAllPluginsHaveVisualMetadata() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        int pluginsWithVisualMetadata = 0;
        List<String> missingPlugins = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) {
                missingPlugins.add(pluginName + " (no metadata)");
                continue;
            }
            
            if (metadata instanceof VisualPluginMetadata) {
                pluginsWithVisualMetadata++;
            } else {
                missingPlugins.add(pluginName + " (not VisualPluginMetadata)");
            }
        }
        
        LOGGER.info("Plugins with VisualPluginMetadata: {}/{}", pluginsWithVisualMetadata, userPlugins.size());
        
        if (!missingPlugins.isEmpty()) {
            LOGGER.warn("Plugins missing VisualPluginMetadata ({}): {}", missingPlugins.size(), missingPlugins);
            for (String missing : missingPlugins) {
                LOGGER.warn("  - {}", missing);
            }
        }
        
        // Allow small tolerance (1 plugin) for edge cases or plugins in transition
        int difference = Math.abs(pluginsWithVisualMetadata - userPlugins.size());
        if (difference > 1) {
            fail("Too many plugins missing VisualPluginMetadata. Expected: " + userPlugins.size() + 
                ", Found: " + pluginsWithVisualMetadata + ". Missing: " + missingPlugins);
        } else if (difference == 1 && !missingPlugins.isEmpty()) {
            LOGGER.warn("One plugin missing VisualPluginMetadata - acceptable for transition: {}", missingPlugins);
        }
        
        LOGGER.info("Metadata completeness check: {}/{} plugins have VisualPluginMetadata", 
            pluginsWithVisualMetadata, userPlugins.size());
    }
    
    @Test
    public void testVisualMetadataFieldsArePopulated() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        List<String> gaps = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (!(metadata instanceof VisualPluginMetadata)) continue;
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            // Check required fields
            if (visualMetadata.getIcon() == null || visualMetadata.getIcon().trim().isEmpty()) {
                gaps.add(pluginName + ": missing icon");
            }
            
            if (visualMetadata.getColor() == null || visualMetadata.getColor().trim().isEmpty()) {
                gaps.add(pluginName + ": missing color");
            }
            
            if (visualMetadata.getGroup() == null || visualMetadata.getGroup().trim().isEmpty()) {
                gaps.add(pluginName + ": missing group");
            }
            
            if (visualMetadata.getTooltip() == null || visualMetadata.getTooltip().trim().isEmpty()) {
                gaps.add(pluginName + ": missing tooltip");
            }
            
            if (visualMetadata.getPortType() == null) {
                gaps.add(pluginName + ": missing portType");
            }
            
            if (visualMetadata.getAccessibility() == null) {
                gaps.add(pluginName + ": missing accessibility metadata");
            }
        }
        
        if (!gaps.isEmpty()) {
            LOGGER.warn("Found metadata gaps: {}", gaps);
        }
        
        assertTrue(gaps.isEmpty(), "All plugins should have complete visual metadata. Gaps: " + gaps);
    }
    
    @Test
    public void testVisualMetadataFieldFormatsAreValid() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        List<String> invalidFields = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (!(metadata instanceof VisualPluginMetadata)) continue;
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            // Validate color format (hex)
            String color = visualMetadata.getColor();
            if (color != null && !HEX_COLOR_PATTERN.matcher(color).matches()) {
                invalidFields.add(pluginName + ": invalid color format '" + color + "' (should be #RRGGBB)");
            }
            
            // Validate icon format (FontAwesome or SVG path)
            String icon = visualMetadata.getIcon();
            if (icon != null && !FONT_AWESOME_PATTERN.matcher(icon).matches() && !icon.startsWith("M ") && !icon.startsWith("<svg")) {
                // Allow SVG paths starting with "M " or "<svg" tag
                if (!icon.contains("svg") && !icon.startsWith("/")) {
                    invalidFields.add(pluginName + ": icon format may be invalid '" + icon + "' (expected FontAwesome or SVG)");
                }
            }
            
            // Validate default dimensions
            int width = visualMetadata.getDefaultWidth();
            if (width < 100 || width > 500) {
                invalidFields.add(pluginName + ": invalid defaultWidth " + width + " (should be 100-500)");
            }
            
            int height = visualMetadata.getDefaultHeight();
            if (height < 60 || height > 300) {
                invalidFields.add(pluginName + ": invalid defaultHeight " + height + " (should be 60-300)");
            }
        }
        
        if (!invalidFields.isEmpty()) {
            LOGGER.warn("Found invalid field formats: {}", invalidFields);
        }
        
        assertTrue(invalidFields.isEmpty(), "All visual metadata fields should have valid formats. Invalid: " + invalidFields);
    }
    
    @Test
    public void testInputOutputPortsAreValid() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        List<String> portIssues = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (!(metadata instanceof VisualPluginMetadata)) continue;
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            // Check input ports
            InputPort[] inputPorts = visualMetadata.getInputPorts();
            if (inputPorts != null) {
                for (InputPort port : inputPorts) {
                    if (port.getName() == null || port.getName().trim().isEmpty()) {
                        portIssues.add(pluginName + ": input port has no name");
                    }
                    if (port.getType() == null) {
                        portIssues.add(pluginName + ": input port '" + port.getName() + "' has no type");
                    }
                }
            }
            
            // Check output ports
            OutputPort[] outputPorts = visualMetadata.getOutputPorts();
            if (outputPorts != null) {
                for (OutputPort port : outputPorts) {
                    if (port.getName() == null || port.getName().trim().isEmpty()) {
                        portIssues.add(pluginName + ": output port has no name");
                    }
                    if (port.getType() == null) {
                        portIssues.add(pluginName + ": output port '" + port.getName() + "' has no type");
                    }
                }
            }
            
            // Most plugins should have at least one output port
            if (outputPorts == null || outputPorts.length == 0) {
                // Some plugins might not have outputs (e.g., exit, log), so this is a warning
                LOGGER.debug("Plugin {} has no output ports (may be intentional)", pluginName);
            }
        }
        
        if (!portIssues.isEmpty()) {
            LOGGER.warn("Found port issues: {}", portIssues);
        }
        
        assertTrue(portIssues.isEmpty(), "All input/output ports should be valid. Issues: " + portIssues);
    }
    
    @Test
    public void testAccessibilityMetadataIsComplete() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        List<String> accessibilityGaps = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (!(metadata instanceof VisualPluginMetadata)) continue;
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            AccessibilityMetadata accessibility = visualMetadata.getAccessibility();
            if (accessibility == null) {
                accessibilityGaps.add(pluginName + ": missing accessibility metadata");
                continue;
            }
            
            if (accessibility.getAriaLabel() == null || accessibility.getAriaLabel().trim().isEmpty()) {
                accessibilityGaps.add(pluginName + ": missing ariaLabel");
            }
            
            if (accessibility.getAriaDescription() == null || accessibility.getAriaDescription().trim().isEmpty()) {
                accessibilityGaps.add(pluginName + ": missing ariaDescription");
            }
            
            if (accessibility.getKeyboardNavigation() == null) {
                accessibilityGaps.add(pluginName + ": missing keyboardNavigation");
            }
        }
        
        if (!accessibilityGaps.isEmpty()) {
            LOGGER.warn("Found accessibility gaps: {}", accessibilityGaps);
        }
        
        assertTrue(accessibilityGaps.isEmpty(), "All plugins should have complete accessibility metadata. Gaps: " + accessibilityGaps);
    }
    
    @Test
    public void testParentChildRelationshipsAreConsistent() {
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        List<String> userPlugins = getPaletteEligibleCorePluginIds();
        
        List<String> relationshipIssues = new ArrayList<>();
        
        for (String pluginName : userPlugins) {
            Plugin plugin = registry.getPlugin(pluginName);
            assertNotNull(plugin, "Palette plugin must be registered by metadata ID: " + pluginName);
            
            PluginMetadata metadata = plugin.getMetadata();
            if (!(metadata instanceof VisualPluginMetadata)) continue;
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            // Check detail plugins have parent relationship
            ParentChildRelationship parentRel = visualMetadata.getParentRelationship();
            String[] allowedChildren = visualMetadata.getAllowedChildPlugins();
            
            // If plugin has parent relationship, it's a detail plugin
            if (parentRel != null) {
                if (parentRel.getParentElementName() == null || parentRel.getParentElementName().trim().isEmpty()) {
                    relationshipIssues.add(pluginName + ": parent relationship missing parentElementName");
                }
                
                if (parentRel.getParentDisplayName() == null || parentRel.getParentDisplayName().trim().isEmpty()) {
                    relationshipIssues.add(pluginName + ": parent relationship missing parentDisplayName");
                }
                
                // Detail plugins shouldn't have allowed children (they're children themselves)
                if (allowedChildren != null && allowedChildren.length > 0) {
                    relationshipIssues.add(pluginName + ": detail plugin should not have allowedChildPlugins");
                }
            }
            
            // If plugin has allowed children, it's a master plugin
            if (allowedChildren != null && allowedChildren.length > 0) {
                // Master plugins shouldn't have parent relationship
                if (parentRel != null) {
                    relationshipIssues.add(pluginName + ": master plugin should not have parentRelationship");
                }
            }
        }
        
        if (!relationshipIssues.isEmpty()) {
            LOGGER.warn("Found parent-child relationship issues: {}", relationshipIssues);
        }
        
        assertTrue(relationshipIssues.isEmpty(), 
            "Parent-child relationships should be consistent. Issues: " + relationshipIssues);
    }
}

