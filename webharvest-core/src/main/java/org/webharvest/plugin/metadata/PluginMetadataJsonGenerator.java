/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.semantic.PluginSemanticsResolver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON generator for plugin metadata.
 * 
 * <p>Generates JSON representation of VisualPluginMetadata for all user-facing
 * core plugins. The JSON is used by the frontend visual editor to display
 * plugins in the palette and provide visual metadata.</p>
 * 
 * <p>The generated JSON structure:</p>
 * <pre>{@code
 * {
 *   "version": "2.3.0",
 *   "generatedAt": "2025-11-04T20:00:00Z",
 *   "plugins": [
 *     {
 *       "id": "http",
 *       "name": "HTTP Request",
 *       "elementName": "http",
 *       "namespace": "http://org.webharvest/schema/2.1/core",
 *       "version": "2.3.0",
 *       "description": "...",
 *       "category": "HTTP & Network",
 *       "icon": "fas fa-globe",
 *       "color": "#3B82F6",
 *       "group": "HTTP & Network",
 *       "defaultWidth": 220,
 *       "defaultHeight": 140,
 *       "inputPorts": [...],
 *       "outputPorts": [...],
 *       "portType": "MIXED",
 *       "tooltip": "...",
 *       "keyboardShortcuts": [...],
 *       "accessibility": {...},
 *       "linkedServices": [...],
 *       "parentRelationship": {...},
 *       "allowedChildPlugins": [...]
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 * 
 * <p>Only processors explicitly eligible for the visual palette are serialized.
 * Runtime registration and palette eligibility are separate contracts.</p>
 * 
 * <p>Usage:</p>
 * <pre>{@code
 * PluginMetadataJsonGenerator generator = new PluginMetadataJsonGenerator();
 * generator.generate("webharvest-ide/src/main/resources/webapp/static/metadata/plugins.json");
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.3.0
 */
public class PluginMetadataJsonGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginMetadataJsonGenerator.class);
    
    /**
     * Generates JSON metadata file for all user-facing core plugins.
     * 
     * <p>Discovers registered Core processors, excludes system and non-palette
     * components, and requires every palette-eligible processor to provide
     * VisualPluginMetadata.</p>
     * 
     * @param outputPath path to output JSON file (will create parent directories if needed)
     * @throws IOException if file operations fail
     * @throws IllegalStateException if PluginRegistry is not initialized
     */
    public void generate(String outputPath) throws IOException {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Output path cannot be null or empty");
        }
        
        LOGGER.info("Starting JSON metadata generation to: " + outputPath);
        
        // Get plugin registry
        PluginRegistry registry = PluginRegistryHolder.getInstance();
        if (registry == null) {
            throw new IllegalStateException("PluginRegistry is not initialized");
        }
        
        // Collect all user-facing plugins with VisualPluginMetadata
        List<PluginMetadataJson> pluginsJson = new ArrayList<>();
        Collection<Plugin> allPlugins = registry.getAllPlugins();
        
        int processedCount = 0;
        int skippedNonPaletteComponents = 0;
        
        for (Plugin plugin : allPlugins) {
            if (plugin == null) continue;
            
            // Check if plugin has @CorePlugin annotation
            CorePlugin annotation = plugin.getClass().getAnnotation(CorePlugin.class);
            if (annotation == null) {
                continue; // Skip non-core plugins
            }
            
            // Effective eligibility requires both conditions: not a system component,
            // and explicitly allowed in the visual palette. Runtime registration is separate.
            if (annotation.systemComponent() || !annotation.paletteComponent()) {
                skippedNonPaletteComponents++;
                LOGGER.debug("Skipping non-palette processor: " + plugin.getClass().getSimpleName());
                continue;
            }
            
            // Get metadata
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) {
                throw new IllegalStateException("Palette-eligible plugin "
                    + plugin.getClass().getName() + " has no metadata");
            }
            
            // Check if metadata implements VisualPluginMetadata
            if (!(metadata instanceof VisualPluginMetadata)) {
                throw new IllegalStateException("Palette-eligible plugin " + metadata.getElementName()
                    + " does not provide VisualPluginMetadata");
            }
            
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
            
            // Convert to JSON representation
            PluginMetadataJson pluginJson = convertToJson(visualMetadata);
            pluginsJson.add(pluginJson);
            processedCount++;
        }
        
        LOGGER.info(String.format("Processed %d palette plugins, skipped %d non-palette processors",
            processedCount, skippedNonPaletteComponents));
        
        // Create output structure
        Map<String, Object> output = new HashMap<>();
        output.put("version", "2.3.0");
        output.put("generatedAt", java.time.Instant.now().toString());
        output.put("pluginCount", pluginsJson.size());
        output.put("plugins", pluginsJson);
        
        // Serialize to JSON
        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
        
        String json = gson.toJson(output);
        
        // Write to file
        Path outputFile = Paths.get(outputPath);
        Path parentDir = outputFile.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }
        
        try (Writer writer = new FileWriter(outputFile.toFile(), false)) {
            writer.write(json);
        }
        
        LOGGER.info(String.format("Successfully generated JSON metadata file: %s (%d plugins)", 
            outputPath, pluginsJson.size()));
    }
    
    /**
     * Converts VisualPluginMetadata to JSON representation.
     * 
     * @param metadata visual plugin metadata
     * @return JSON representation
     */
    private PluginMetadataJson convertToJson(VisualPluginMetadata metadata) {
        PluginMetadataJson json = new PluginMetadataJson();
        
        // Basic metadata
        json.id = metadata.getId();
        json.name = metadata.getName();
        json.elementName = metadata.getElementName();
        json.namespace = metadata.getNamespace();
        json.version = metadata.getVersion();
        json.description = metadata.getDescription();
        json.author = metadata.getAuthor();
        json.category = metadata.getCategory();
        json.tags = metadata.getTags();
        json.dependencies = metadata.getDependencies();
        json.className = metadata.getClassName();
        json.corePlugin = metadata.isCorePlugin();
        
        // Visual metadata
        json.icon = metadata.getIcon();
        json.color = metadata.getColor();
        json.group = metadata.getGroup();
        json.defaultWidth = metadata.getDefaultWidth();
        json.defaultHeight = metadata.getDefaultHeight();
        json.tooltip = metadata.getTooltip();
        json.portType = metadata.getPortType() != null ? metadata.getPortType().name() : null;
        
        // Input ports
        if (metadata.getInputPorts() != null) {
            json.inputPorts = new ArrayList<>();
            for (org.webharvest.plugin.InputPort port : metadata.getInputPorts()) {
                InputPortJson portJson = new InputPortJson();
                portJson.name = port.getName();
                portJson.description = port.getDescription();
                portJson.type = port.getType() != null ? port.getType().name() : null;
                portJson.required = port.isRequired();
                json.inputPorts.add(portJson);
            }
        }
        
        // Output ports
        if (metadata.getOutputPorts() != null) {
            json.outputPorts = new ArrayList<>();
            for (org.webharvest.plugin.OutputPort port : metadata.getOutputPorts()) {
                OutputPortJson portJson = new OutputPortJson();
                portJson.name = port.getName();
                portJson.description = port.getDescription();
                portJson.type = port.getType() != null ? port.getType().name() : null;
                json.outputPorts.add(portJson);
            }
        }
        
        // Keyboard shortcuts
        if (metadata.getKeyboardShortcuts() != null) {
            json.keyboardShortcuts = new ArrayList<>();
            for (org.webharvest.plugin.KeyboardShortcut shortcut : metadata.getKeyboardShortcuts()) {
                KeyboardShortcutJson shortcutJson = new KeyboardShortcutJson();
                shortcutJson.key = shortcut.getKey();
                shortcutJson.action = shortcut.getAction();
                shortcutJson.isGlobal = shortcut.isGlobal();
                json.keyboardShortcuts.add(shortcutJson);
            }
        }
        
        // Accessibility metadata
        if (metadata.getAccessibility() != null) {
            AccessibilityMetadataJson accJson = new AccessibilityMetadataJson();
            accJson.ariaLabel = metadata.getAccessibility().getAriaLabel();
            accJson.ariaDescription = metadata.getAccessibility().getAriaDescription();
            accJson.keyboardNavigation = metadata.getAccessibility().getKeyboardNavigation();
            json.accessibility = accJson;
        }
        
        // Linked services
        if (metadata.getLinkedServices() != null) {
            json.linkedServices = new ArrayList<>();
            for (org.webharvest.plugin.LinkedServiceMetadata service : metadata.getLinkedServices()) {
                LinkedServiceMetadataJson serviceJson = new LinkedServiceMetadataJson();
                serviceJson.serviceType = service.getServiceType();
                serviceJson.serviceName = service.getServiceName();
                serviceJson.description = service.getDescription();
                serviceJson.required = service.isRequired();
                json.linkedServices.add(serviceJson);
            }
        }
        
        // Parent-child relationship
        if (metadata.getParentRelationship() != null) {
            ParentChildRelationshipJson parentJson = new ParentChildRelationshipJson();
            parentJson.parentElementName = metadata.getParentRelationship().getParentElementName();
            parentJson.required = metadata.getParentRelationship().isRequired();
            parentJson.parentDisplayName = metadata.getParentRelationship().getParentDisplayName();
            json.parentRelationship = parentJson;
        }
        
        // Allowed child plugins
        json.allowedChildPlugins = metadata.getAllowedChildPlugins();

        // Semantic metadata
        PluginSemanticsJson semanticsJson = null;
        try {
            Class<? extends Plugin> pluginClass = Class.forName(metadata.getClassName()).asSubclass(Plugin.class);
            org.webharvest.plugin.semantic.PluginSemantics runtimeSemantics =
                    PluginSemanticsResolver.resolve(pluginClass, metadata);

            semanticsJson = new PluginSemanticsJson();
            semanticsJson.roles = runtimeSemantics.getRoles().stream()
                    .map(Enum::name)
                    .collect(Collectors.toList());
            semanticsJson.canAppearAtRoot = !runtimeSemantics.requiresParent();
            semanticsJson.allowsChildren = runtimeSemantics.allowsChildPlugins();
            semanticsJson.acceptsAnyChild = runtimeSemantics.allowsAnyChild();
            semanticsJson.allowBody = runtimeSemantics.allowsTextContent();
            semanticsJson.allowTextContent = runtimeSemantics.allowsTextContent();
            semanticsJson.allowedChildTypes = new ArrayList<>(runtimeSemantics.getAllowedChildElements());
            semanticsJson.requiredParentTypes = new ArrayList<>(runtimeSemantics.getRequiredParentElements());
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Unable to load plugin class {} for semantics generation", metadata.getClassName(), e);
        }

        json.semantics = semanticsJson;
        
        return json;
    }
    
    // JSON DTO classes for Gson serialization
    
    /**
     * JSON representation of plugin metadata.
     */
    private static class PluginMetadataJson {
        String id;
        String name;
        String elementName;
        String namespace;
        String version;
        String description;
        String author;
        String category;
        String[] tags;
        String[] dependencies;
        String className;
        boolean corePlugin;
        
        // Visual metadata
        String icon;
        String color;
        String group;
        int defaultWidth;
        int defaultHeight;
        String tooltip;
        String portType;
        
        List<InputPortJson> inputPorts;
        List<OutputPortJson> outputPorts;
        List<KeyboardShortcutJson> keyboardShortcuts;
        AccessibilityMetadataJson accessibility;
        List<LinkedServiceMetadataJson> linkedServices;
        ParentChildRelationshipJson parentRelationship;
        String[] allowedChildPlugins;
        PluginSemanticsJson semantics;
    }
    
    /**
     * JSON representation of input port.
     */
    private static class InputPortJson {
        String name;
        String description;
        String type;
        boolean required;
    }
    
    /**
     * JSON representation of output port.
     */
    private static class OutputPortJson {
        String name;
        String description;
        String type;
    }
    
    /**
     * JSON representation of keyboard shortcut.
     */
    private static class KeyboardShortcutJson {
        String key;
        String action;
        boolean isGlobal;
    }
    
    /**
     * JSON representation of accessibility metadata.
     */
    private static class AccessibilityMetadataJson {
        String ariaLabel;
        String ariaDescription;
        String[] keyboardNavigation;
    }
    
    /**
     * JSON representation of linked service metadata.
     */
    private static class LinkedServiceMetadataJson {
        String serviceType;
        String serviceName;
        String description;
        boolean required;
    }
    
    /**
     * JSON representation of parent-child relationship.
     */
    private static class ParentChildRelationshipJson {
        String parentElementName;
        boolean required;
        String parentDisplayName;
    }

    /**
     * JSON representation of plugin semantics.
     */
    private static class PluginSemanticsJson {
        List<String> roles;
        boolean canAppearAtRoot;
        boolean allowsChildren;
        boolean acceptsAnyChild;
        boolean allowBody;
        boolean allowTextContent;
        List<String> allowedChildTypes;
        List<String> requiredParentTypes;
    }
    
    /**
     * Main method for command-line execution.
     * 
     * <p>Usage: java PluginMetadataJsonGenerator [output-path]</p>
     * <p>Default output: target/classes/webapp/static/metadata/plugins.json</p>
     * 
     * @param args command-line arguments (optional output path)
     */
    public static void main(String[] args) {
        String outputPath;
        if (args.length > 0) {
            outputPath = args[0];
        } else {
            // Default output path
            outputPath = "target/classes/webapp/static/metadata/plugins.json";
        }
        
        try {
            PluginMetadataJsonGenerator generator = new PluginMetadataJsonGenerator();
            generator.generate(outputPath);
            System.out.println("JSON metadata generated successfully: " + outputPath);
        } catch (Exception e) {
            System.err.println("Failed to generate JSON metadata: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

