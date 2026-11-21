/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * [BSD 3-Clause License text...]
 */

package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import org.webharvest.plugin.metadata.PluginMetadataCatalog;
import org.webharvest.plugin.metadata.PluginDescriptor;
import org.webharvest.plugin.metadata.PluginDocumentation;
import org.webharvest.plugin.metadata.PluginSemantics;
import org.webharvest.plugin.metadata.PluginUIHints;
import org.webharvest.plugin.metadata.PluginSemanticRole;
import org.webharvest.plugin.metadata.XSDGenerator;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;

/**
 * Servlet providing plugin metadata and XSD schemas for the Visual Designer.
 * 
 * Endpoints:
 * - GET /api/plugins/metadata - Returns JSON with all registered plugins, their schemas, and metadata
 * - GET /api/plugins/xsd - Returns generated XSD schema for default namespace
 * - GET /api/plugins/xsd/{version} - Returns generated XSD schema for specific version/namespace
 * 
 * This endpoint is used by the visual designer frontend to populate
 * the plugin palette, generate property panels, and validate configurations.
 * 
 * @author WebHarvest Team
 * @since 2.2.0
 */
@WebServlet(name = "PluginMetadataServlet", urlPatterns = {
    "/api/plugins/metadata",
    "/api/plugins/metadata/version",
    "/api/plugins/ontology",
    "/api/plugins/xsd",
    "/api/plugins/xsd/*"
})
public class PluginMetadataServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataServlet.class);
    private static final String DOCUMENTATION_BASE_URL =
            "https://github.com/web-harvest/web-harvest/blob/main/docs/web/documentation/2.2.0/";
    private static final Set<String> CORE_DOCUMENTATION_PAGES = Set.of(
            "body-processor", "call-param", "call", "case", "catch", "config", "constant",
            "def", "else", "empty", "exit", "file", "function", "get-var", "html-to-xml",
            "http-header", "http-param", "http", "if", "include", "json-to-xml", "list",
            "log", "loop", "regexp-pattern", "regexp-result", "regexp-source", "regexp",
            "return", "script", "set-var", "sleep", "stylesheet", "template", "text",
            "tokenize", "try", "value-of", "var", "while", "xml-to-json", "xml",
            "xpath", "xq-expression", "xq-param", "xquery", "xslt");
    private static final Map<String, String> EXTERNAL_DOCUMENTATION_PAGES = Map.of(
            "ftp", "external/ftp.html",
            "ftp-get", "external/ftp-get.html",
            "ftp-list", "external/ftp-list.html",
            "ftp-put", "external/ftp-put.html",
            "mail", "external/mail.html",
            "webbrowser", "external/web-browser.html",
            "zip", "external/zip.html",
            "zip-entry", "external/zip-entry.html");
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    private static final String VISUAL_API_VERSION = "2.0";
    private static final List<String> VISUAL_CAPABILITIES = List.of(
        "schema-driven-fields",
        "ui-tools",
        "master-detail-constraints",
        "legacy-compatibility"
    );
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        String pathInfo = req.getPathInfo();
        String requestURI = req.getRequestURI();
        
        // Handle version endpoint
        if (requestURI.endsWith("/version") || requestURI.endsWith("/version/")) {
            handleVersionRequest(req, resp);
            return;
        }
        
        // Handle ontology endpoint
        if (requestURI.endsWith("/ontology") || requestURI.endsWith("/ontology/")) {
            handleOntologyRequest(req, resp);
            return;
        }
        
        // Handle XSD endpoints
        if (requestURI.contains("/xsd")) {
            handleXSDRequest(req, resp, requestURI);
            return;
        }
        
        // Handle metadata endpoint (default)
        try {
            // Try semantic catalog first, fallback to runtime registry
            List<Map<String, Object>> plugins;
            String schemaVersion;
            Map<String, Object> catalogSchema;
            
            try {
                plugins = getPluginMetadataFromCatalog();
                schemaVersion = PluginMetadataCatalog.getSchemaVersion();
                catalogSchema = PluginMetadataCatalog.getCatalogSchema();
                LOG.info("Loaded {} plugins from PluginMetadataCatalog (v{})", plugins.size(), schemaVersion);
            } catch (Exception e) {
                LOG.warn("Failed to load from PluginMetadataCatalog, falling back to CorePluginRegistry: {}", e.getMessage());
                plugins = getPluginMetadataFromRegistry();
                schemaVersion = "2.2.0";
                catalogSchema = null;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("plugins", plugins);
            response.put("count", plugins.size());
            response.put("version", "2.2.0");
            response.put("schemaVersion", schemaVersion);
            if (catalogSchema != null) {
                response.put("schema", catalogSchema);
            }
            response.put("timestamp", System.currentTimeMillis());
            // Add XSD endpoint reference
            response.put("xsdUrl", req.getContextPath() + "/api/plugins/xsd");
            
            // Set response headers
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            
            // Write JSON response
            resp.getWriter().write(GSON.toJson(response));
            resp.setStatus(HttpServletResponse.SC_OK);
            
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to load plugin metadata");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            resp.getWriter().write(GSON.toJson(error));
        }
    }
    
    /**
     * Handle version check requests
     */
    private void handleVersionRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        try {
            String schemaVersion;
            try {
                schemaVersion = PluginMetadataCatalog.getSchemaVersion();
            } catch (Exception e) {
                schemaVersion = "2.2.0";
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("schemaVersion", schemaVersion);
            response.put("version", "2.2.0");
            response.put("timestamp", System.currentTimeMillis());
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            
            resp.getWriter().write(GSON.toJson(response));
            resp.setStatus(HttpServletResponse.SC_OK);
            
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to get schema version");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            resp.getWriter().write(GSON.toJson(error));
        }
    }
    
    /**
     * Handle ontology schema requests
     */
    private void handleOntologyRequest(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        try {
            List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
            Map<String, Object> ontology = new LinkedHashMap<>();
            
            // Build entity schemas
            Map<String, Object> entitySchemas = new LinkedHashMap<>();
            for (PluginDescriptor desc : descriptors) {
                Map<String, Object> entitySchema = new LinkedHashMap<>();
                entitySchema.put("type", "Plugin");
                entitySchema.put("pluginType", desc.getId());
                entitySchema.put("category", desc.getCategory());
                
                // Properties from schema
                Map<String, Object> properties = new LinkedHashMap<>();
                properties.put("id", Map.of("type", "string", "required", true));
                properties.put("type", Map.of("type", "string", "required", true, "defaultValue", desc.getId()));
                properties.put("name", Map.of("type", "string", "required", false));
                properties.put("category", Map.of("type", "string", "required", false));
                
                // Add attribute properties from extensions
                if (desc.getExtensions() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> schemaExt = (Map<String, Object>) desc.getExtensions().get("schema");
                    if (schemaExt != null) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> attrs = (List<Map<String, Object>>) schemaExt.get("attributeDefinitions");
                        if (attrs != null) {
                            for (Map<String, Object> attr : attrs) {
                                Map<String, Object> prop = new LinkedHashMap<>();
                                prop.put("type", attr.getOrDefault("type", "string"));
                                prop.put("required", attr.getOrDefault("required", false));
                                if (attr.containsKey("defaultValue")) {
                                    prop.put("defaultValue", attr.get("defaultValue"));
                                }
                                if (attr.containsKey("allowedValues")) {
                                    prop.put("enumValues", attr.get("allowedValues"));
                                }
                                properties.put((String) attr.get("name"), prop);
                            }
                        }
                    }
                }
                entitySchema.put("properties", properties);
                
                // Relationship constraints from semantics
                Map<String, Object> relationships = new LinkedHashMap<>();
                PluginSemantics semantics = desc.getSemantics();
                if (semantics != null) {
                    if (semantics.allowsChildren() || semantics.acceptsAnyChild()) {
                        List<Map<String, Object>> parentOf = new ArrayList<>();
                        Map<String, Object> parentOfRel = new LinkedHashMap<>();
                        parentOfRel.put("targetType", semantics.acceptsAnyChild() ? List.of("Node") : semantics.getAllowedChildTypes());
                        parentOfRel.put("cardinality", Map.of("min", 0, "max", -1));
                        parentOf.add(parentOfRel);
                        relationships.put("PARENT_OF", parentOf);
                    }
                    if (semantics.getRequiredParentTypes() != null && !semantics.getRequiredParentTypes().isEmpty()) {
                        List<Map<String, Object>> childOf = new ArrayList<>();
                        Map<String, Object> childOfRel = new LinkedHashMap<>();
                        childOfRel.put("targetType", List.of("Node"));
                        childOfRel.put("cardinality", Map.of("min", 1, "max", 1));
                        childOf.add(childOfRel);
                        relationships.put("CHILD_OF", childOf);
                    }
                    if (semantics.canAppearAtRoot()) {
                        List<Map<String, Object>> sequentialTo = new ArrayList<>();
                        Map<String, Object> seqRel = new LinkedHashMap<>();
                        seqRel.put("targetType", List.of("Node"));
                        seqRel.put("cardinality", Map.of("min", 0, "max", -1));
                        sequentialTo.add(seqRel);
                        relationships.put("SEQUENTIAL_TO", sequentialTo);
                    }
                }
                entitySchema.put("allowedRelationships", relationships);
                
                entitySchemas.put(desc.getId(), entitySchema);
            }
            
            ontology.put("entitySchemas", entitySchemas);
            ontology.put("version", PluginMetadataCatalog.getSchemaVersion());
            ontology.put("timestamp", System.currentTimeMillis());
            
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            
            resp.getWriter().write(GSON.toJson(ontology));
            resp.setStatus(HttpServletResponse.SC_OK);
            
        } catch (Exception e) {
            LOG.error("Failed to generate ontology schema", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate ontology schema");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            resp.getWriter().write(GSON.toJson(error));
        }
    }
    
    /**
     * Handle XSD generation requests
     */
    private void handleXSDRequest(HttpServletRequest req, HttpServletResponse resp, 
                                   String requestURI) throws IOException {
        try {
            // Extract version from path if present
            // /api/plugins/xsd or /api/plugins/xsd/2.1 or /api/plugins/xsd/2.1/core
            String namespace = "http://org.webharvest/schema/2.1/core"; // Default
            
            if (requestURI.contains("/xsd/")) {
                String[] parts = requestURI.split("/xsd/");
                if (parts.length > 1) {
                    String versionPart = parts[1];
                    // Map version to namespace
                    if (versionPart.startsWith("2.1")) {
                        namespace = "http://org.webharvest/schema/2.1/core";
                    } else if (versionPart.startsWith("2.0")) {
                        namespace = "http://org.webharvest/schema/2.0/core";
                    } else if (versionPart.startsWith("1.0")) {
                        namespace = "http://web-harvest.sourceforge.net/schema/1.0/config";
                    } else {
                        // Treat as custom namespace (URL-encoded)
                        namespace = java.net.URLDecoder.decode(versionPart, "UTF-8");
                    }
                }
            }
            
            // Generate XSD
            String xsd = XSDGenerator.generateXSD(namespace);
            
            // Set response headers
            resp.setContentType("application/xml");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "public, max-age=3600"); // Cache for 1 hour
            resp.setHeader("Content-Disposition", 
                String.format("attachment; filename=\"webharvest-schema-%s.xsd\"", 
                    namespace.replaceAll("[^a-zA-Z0-9]", "-")));
            
            // Write XSD response
            resp.getWriter().write(xsd);
            resp.setStatus(HttpServletResponse.SC_OK);
            
            LOG.info("Generated XSD schema for namespace: {}", namespace);
            
        } catch (Exception e) {
            LOG.error("Failed to generate XSD schema", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate XSD schema");
            error.put("message", e.getMessage());
            error.put("timestamp", System.currentTimeMillis());
            
            resp.getWriter().write(GSON.toJson(error));
        }
    }
    
    /**
     * Get plugin metadata from PluginMetadataCatalog (new semantic catalog)
     */
    private List<Map<String, Object>> getPluginMetadataFromCatalog() {
        LOG.info("Building plugin metadata from PluginMetadataCatalog...");
        
        List<Map<String, Object>> visualPlugins = new ArrayList<>();
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        
        for (PluginDescriptor desc : descriptors) {
            try {
                Map<String, Object> metadata = buildMetadataFromDescriptor(desc);
                visualPlugins.add(metadata);
            } catch (Exception e) {
                LOG.warn("Failed to build metadata for plugin: {}", desc.getId(), e);
            }
        }
        
        LOG.info("Retrieved {} plugins from PluginMetadataCatalog", visualPlugins.size());
        return visualPlugins;
    }
    
    /**
     * Build metadata map from PluginDescriptor
     */
    private Map<String, Object> buildMetadataFromDescriptor(PluginDescriptor desc) {
        Map<String, Object> plugin = new HashMap<>();
        
        // Basic fields
        plugin.put("id", desc.getId());
        plugin.put("name", desc.getDisplayName());
        plugin.put("elementName", desc.getElementName());
        plugin.put("description", desc.getDescription());
        plugin.put("category", desc.getCategory());
        
        // Documentation
        PluginDocumentation doc = desc.getDocumentation();
        String helpUrl = resolveHelpUrl(desc.getElementName(), doc == null ? null : doc.getUrl());
        plugin.put("helpUrl", helpUrl);
        
        // UI Hints
        String icon;
        List<String> tags;
        String paletteGroup = null;
        PluginUIHints ui = desc.getUiHints();
        if (ui != null) {
            icon = ui.getIcon() != null ? ui.getIcon() : getPluginIcon(desc.getCategory(), desc.getId());
            tags = ui.getTags() != null && !ui.getTags().isEmpty()
                    ? new ArrayList<>(ui.getTags())
                    : Arrays.asList(desc.getId(), safeCategorySlug(desc.getCategory()));
            paletteGroup = ui.getPaletteGroup();
            plugin.put("paletteGroup", paletteGroup);
        } else {
            icon = getPluginIcon(desc.getCategory(), desc.getId());
            tags = Arrays.asList(desc.getId(), safeCategorySlug(desc.getCategory()));
        }
        plugin.put("icon", icon);
        plugin.put("tags", tags);
        
        // Semantics
        PluginSemantics semantics = desc.getSemantics();
        if (semantics != null) {
            Map<String, Object> semanticsMap = new HashMap<>();
            semanticsMap.put("roles", new ArrayList<>(semantics.getRoles()));
            semanticsMap.put("allowsChildren", semantics.allowsChildren());
            semanticsMap.put("acceptsAnyChild", semantics.acceptsAnyChild());
            semanticsMap.put("allowsBody", semantics.allowsBody());
            semanticsMap.put("allowTextContent", semantics.allowsTextContent());
            semanticsMap.put("canAppearAtRoot", semantics.canAppearAtRoot());
            semanticsMap.put("allowedChildTypes", semantics.getAllowedChildTypes());
            semanticsMap.put("requiredParentTypes", semantics.getRequiredParentTypes());
            plugin.put("semantics", semanticsMap);
        }
        
        // Execution semantics from extensions
        if (desc.getExtensions() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> execSemantics = (Map<String, Object>) desc.getExtensions().get("executionSemantics");
            if (execSemantics != null && !execSemantics.isEmpty()) {
                // Add to semantics map for easier access
                if (semantics != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> semanticsMap = (Map<String, Object>) plugin.get("semantics");
                    if (semanticsMap != null) {
                        semanticsMap.put("executionSemantics", execSemantics);
                    }
                }
            }
        }
        
        // Dependencies
        if (desc.getDependencies() != null && !desc.getDependencies().isEmpty()) {
            plugin.put("dependencies", desc.getDependencies());
        }
        
        // Extensions
        Map<String, Object> uiExtensions = null;
        List<Map<String, Object>> masterDetailConstraints = null;
        if (desc.getExtensions() != null && !desc.getExtensions().isEmpty()) {
            plugin.put("extensions", desc.getExtensions());
            
            Object uiBlock = desc.getExtensions().get("ui");
            if (uiBlock instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> uiMap = (Map<String, Object>) uiBlock;
                uiExtensions = uiMap;
            }
            
            // Extract master/detail constraints from extensions for direct access
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> constraints = (List<Map<String, Object>>) desc.getExtensions().get("masterDetailConstraints");
            if (constraints != null && !constraints.isEmpty()) {
                plugin.put("masterDetailConstraints", constraints);
                masterDetailConstraints = constraints;
            }
        }
        
        // Runtime schema (for backward compatibility)
        Map<String, Object> schema = extractSchemaFromDescriptor(desc);
        plugin.put("schema", schema);
        
        // Try to get VisualPluginMetadata from runtime plugin class
        try {
            Class<? extends org.webharvest.plugin.Plugin> pluginClass = 
                Class.forName(desc.getClassName()).asSubclass(org.webharvest.plugin.Plugin.class);
            org.webharvest.plugin.Plugin pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            org.webharvest.plugin.PluginMetadata metadata = pluginInstance.getMetadata();
            
            if (metadata instanceof VisualPluginMetadata) {
                VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
                addVisualMetadataFields(plugin, visualMetadata);
            } else {
                // Always add default port types even if VisualPluginMetadata is not available
                addDefaultPortTypes(plugin);
            }
        } catch (Exception e) {
            LOG.debug("Failed to load VisualPluginMetadata for {}: {}", desc.getId(), e.getMessage());
            // Add default port types as fallback
            addDefaultPortTypes(plugin);
        }
        
        plugin.put("visual", buildVisualEnvelope(
                desc.getId(),
                desc.getElementName(),
                desc.getDisplayName(),
                desc.getCategory(),
                desc.getClassName(),
                icon,
                paletteGroup,
                tags,
                helpUrl,
                uiExtensions,
                masterDetailConstraints));
        
        return plugin;
    }

    private Map<String, Object> buildVisualEnvelope(
            String id,
            String elementName,
            String displayName,
            String category,
            String className,
            String icon,
            String paletteGroup,
            List<String> tags,
            String helpUrl,
            Map<String, Object> uiExtensions,
            List<Map<String, Object>> masterDetailConstraints) {
        Map<String, Object> visual = new LinkedHashMap<>();
        visual.put("apiVersion", VISUAL_API_VERSION);
        visual.put("capabilities", VISUAL_CAPABILITIES);
        visual.put("id", id);
        visual.put("elementName", elementName);
        visual.put("displayName", displayName);
        visual.put("category", category);
        visual.put("className", className);
        visual.put("schemaVersion", PluginMetadataCatalog.getSchemaVersion());
        if (helpUrl != null) {
            visual.put("helpUrl", helpUrl);
        }
        if (icon != null) {
            visual.put("icon", icon);
        }
        if (paletteGroup != null) {
            visual.put("paletteGroup", paletteGroup);
        }
        if (tags != null && !tags.isEmpty()) {
            visual.put("tags", tags);
        }
        if (uiExtensions != null && !uiExtensions.isEmpty()) {
            visual.put("ui", uiExtensions);
        }
        if (masterDetailConstraints != null && !masterDetailConstraints.isEmpty()) {
            visual.put("masterDetailConstraints", masterDetailConstraints);
        }
        return visual;
    }
    
    /**
     * Extract schema from descriptor by loading runtime plugin class
     */
    private Map<String, Object> extractSchemaFromDescriptor(PluginDescriptor desc) {
        try {
            Class<? extends org.webharvest.plugin.Plugin> pluginClass = 
                Class.forName(desc.getClassName()).asSubclass(org.webharvest.plugin.Plugin.class);
            return extractRealPluginSchema(pluginClass);
        } catch (Exception e) {
            LOG.debug("Failed to extract runtime schema for {}: {}", desc.getId(), e.getMessage());
            // Return basic schema from semantics
            Map<String, Object> schema = new HashMap<>();
            if (desc.getSemantics() != null) {
                schema.put("allowsBody", desc.getSemantics().allowsBody());
            } else {
                schema.put("allowsBody", true);
            }
            schema.put("attributes", Collections.emptyList());
            schema.put("allowedSubElements", Collections.emptyList());
            schema.put("requiredSubElements", Collections.emptyList());
            return schema;
        }
    }
    
    /**
     * Get plugin metadata from CorePluginRegistry
     * Creates metadata dynamically from registered plugin classes
     */
    private List<Map<String, Object>> getPluginMetadataFromRegistry() {
        LOG.info("Building plugin metadata from CorePluginRegistry...");
        
        List<Map<String, Object>> visualPlugins = new ArrayList<>();
        
        // Get known core plugins from CorePluginsRegistrar
        String[] corePlugins = {
            "http", "html-to-xml", "xpath", "xquery", "regexp",
            "def", "get", "var", "set-var", "get-var",
            "if", "else", "loop", "while", "try", "catch", "case",
            "file", "template", "text", "script",
            "json-to-xml", "xml-to-json", "xslt", "xml",
            "function", "call", "call-param", "return",
            "sleep", "exit", "empty", "constant", "list", "log",
            "config"
        };
        
        org.webharvest.definition.CorePluginRegistry registry = 
            org.webharvest.definition.CorePluginRegistry.getInstance();
        
        int registeredCount = 0;
        for (String elementName : corePlugins) {
            if (registry.isPluginRegistered(elementName)) {
                try {
                    Class<? extends org.webharvest.plugin.Plugin> pluginClass = 
                        registry.getPluginClass(elementName);
                    
                    if (pluginClass != null) {
                        Map<String, Object> metadata = createMetadataFromPluginClass(elementName, pluginClass);
                        visualPlugins.add(metadata);
                        registeredCount++;
                    }
                } catch (Exception e) {
                    LOG.warn("Failed to create metadata for plugin: {}", elementName, e);
                }
            }
        }
        
        LOG.info("Retrieved {} plugins from CorePluginRegistry (out of {} checked)", 
            registeredCount, corePlugins.length);
        
        return visualPlugins;
    }
    
    /**
     * Create metadata from plugin class with FULL schema extraction
     */
    private Map<String, Object> createMetadataFromPluginClass(
            String elementName, Class<? extends org.webharvest.plugin.Plugin> pluginClass) {
        
        Map<String, Object> plugin = new HashMap<>();
        
        // Get category from plugin metadata (not inferred!)
        String category = "Core";
        String description = getPluginDescription(elementName);
        String version = "2.2.0";
        String author = "WebHarvest Team";
        
        try {
            // 1. Try to get category from @CorePlugin annotation FIRST
            org.webharvest.plugin.annotations.CorePlugin annotation = 
                pluginClass.getAnnotation(org.webharvest.plugin.annotations.CorePlugin.class);
            
            if (annotation != null && annotation.category() != null && !annotation.category().isEmpty()) {
                category = annotation.category();
                description = annotation.description() != null && !annotation.description().isEmpty() 
                    ? annotation.description() : description;
                version = annotation.version() != null && !annotation.version().isEmpty()
                    ? annotation.version() : version;
            }
            
            // 2. Then override with METADATA if available (METADATA takes precedence)
            org.webharvest.plugin.Plugin pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            org.webharvest.plugin.PluginMetadata metadata = pluginInstance.getMetadata();
            
            if (metadata != null) {
                // Override only if METADATA provides non-null/non-empty values
                if (metadata.getCategory() != null && !metadata.getCategory().trim().isEmpty()) {
                    category = metadata.getCategory();
                }
                if (metadata.getDescription() != null && !metadata.getDescription().trim().isEmpty()) {
                    description = metadata.getDescription();
                }
                if (metadata.getVersion() != null && !metadata.getVersion().trim().isEmpty()) {
                    version = metadata.getVersion();
                }
                if (metadata.getAuthor() != null && !metadata.getAuthor().trim().isEmpty()) {
                    author = metadata.getAuthor();
                }
                
                // 3. Add VisualPluginMetadata fields if available
                if (metadata instanceof VisualPluginMetadata) {
                    VisualPluginMetadata visualMetadata = (VisualPluginMetadata) metadata;
                    addVisualMetadataFields(plugin, visualMetadata);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to read plugin metadata for {}: {}", elementName, e.getMessage());
        }
        
        String helpUrl = getHelpUrl(elementName);
        String icon = getPluginIcon("", elementName);
        List<String> tags = Arrays.asList(elementName, safeCategorySlug(category));
        
        plugin.put("id", elementName);
        plugin.put("name", formatPluginName(elementName));
        plugin.put("elementName", elementName);
        plugin.put("description", description);
        plugin.put("category", category); // ← From metadata!
        plugin.put("icon", icon);
        plugin.put("version", version);
        plugin.put("author", author);
        plugin.put("tags", tags);
        plugin.put("helpUrl", helpUrl);
        
        // Extract REAL schema from plugin instance
        Map<String, Object> schema = extractRealPluginSchema(pluginClass);
        plugin.put("schema", schema);
        plugin.put("visual", buildVisualEnvelope(
                elementName,
                elementName,
                formatPluginName(elementName),
                category,
                pluginClass.getName(),
                icon,
                null,
                tags,
                helpUrl,
                null,
                null));
        
        return plugin;
    }
    
    /**
     * Extract real plugin schema by instantiating the plugin
     */
    private Map<String, Object> extractRealPluginSchema(Class<? extends org.webharvest.plugin.Plugin> pluginClass) {
        Map<String, Object> schema = new HashMap<>();
        
        try {
            // Instantiate plugin to get metadata
            org.webharvest.plugin.Plugin pluginInstance = pluginClass.getDeclaredConstructor().newInstance();
            org.webharvest.plugin.PluginMetadata metadata = pluginInstance.getMetadata();
            
            if (metadata != null && metadata.getConfigurationSchema() != null) {
                org.webharvest.plugin.PluginConfigurationSchema pluginSchema = metadata.getConfigurationSchema();
                
                // Extract attributes with FULL details
                List<Map<String, Object>> attributes = new ArrayList<>();
                org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition[] attrDefs = 
                    pluginSchema.getAttributeDefinitions();
                
                if (attrDefs != null) {
                    for (org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition attr : attrDefs) {
                        Map<String, Object> attrMap = new HashMap<>();
                        
                        // Basic info
                        attrMap.put("name", attr.getName());
                        attrMap.put("type", attr.getType() != null ? attr.getType().toString().toLowerCase() : "string");
                        attrMap.put("required", attr.isRequired());
                        attrMap.put("description", attr.getDescription() != null ? attr.getDescription() : "");
                        
                        // Default value
                        if (attr.getDefaultValue() != null) {
                            attrMap.put("defaultValue", attr.getDefaultValue());
                        }
                        
                        // ENUM - allowed values
                        if (attr.getAllowedValues() != null && attr.getAllowedValues().length > 0) {
                            attrMap.put("allowedValues", Arrays.asList(attr.getAllowedValues()));
                            attrMap.put("options", Arrays.asList(attr.getAllowedValues()));
                        }
                        
                        // Numeric - min/max
                        if (attr.getMinValue() != null) {
                            attrMap.put("min", attr.getMinValue());
                        }
                        if (attr.getMaxValue() != null) {
                            attrMap.put("max", attr.getMaxValue());
                        }
                        
                        // String - pattern
                        if (attr.getPattern() != null && !attr.getPattern().isEmpty()) {
                            attrMap.put("pattern", attr.getPattern());
                        }
                        
                        attributes.add(attrMap);
                    }
                }
                
                schema.put("attributes", attributes);
                schema.put("allowsBody", pluginSchema.allowsBody());
                
                // Sub-elements for nested plugins
                String[] allowedSubs = pluginSchema.getAllowedSubElements();
                schema.put("allowedSubElements", 
                    allowedSubs != null ? Arrays.asList(allowedSubs) : Collections.emptyList());
                
                String[] requiredSubs = pluginSchema.getRequiredSubElements();
                schema.put("requiredSubElements",
                    requiredSubs != null ? Arrays.asList(requiredSubs) : Collections.emptyList());
                
                // Required/optional attrs
                String[] requiredAttrs = pluginSchema.getRequiredAttributes();
                schema.put("requiredAttributes",
                    requiredAttrs != null ? Arrays.asList(requiredAttrs) : Collections.emptyList());
                
                String[] optionalAttrs = pluginSchema.getOptionalAttributes();
                schema.put("optionalAttributes",
                    optionalAttrs != null ? Arrays.asList(optionalAttrs) : Collections.emptyList());
                
                LOG.debug("Extracted schema for plugin: {}", pluginClass.getSimpleName());
                
            } else {
                // Fallback to empty schema
                schema.put("attributes", Collections.emptyList());
                schema.put("allowsBody", true);
                schema.put("allowedSubElements", Collections.emptyList());
                schema.put("requiredSubElements", Collections.emptyList());
            }
            
        } catch (Exception e) {
            LOG.warn("Failed to extract schema from plugin class: {}", pluginClass.getSimpleName(), e);
            
            // Fallback schema
            schema.put("attributes", Collections.emptyList());
            schema.put("allowsBody", true);
            schema.put("allowedSubElements", Collections.emptyList());
            schema.put("requiredSubElements", Collections.emptyList());
        }
        
        return schema;
    }
    
    private String safeCategorySlug(String category) {
        if (category == null || category.isBlank()) {
            return "general";
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Format element name to readable name
     */
    private String formatPluginName(String elementName) {
        // Convert "html-to-xml" → "HTML to XML"
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : elementName.toCharArray()) {
            if (c == '-') {
                result.append(' ');
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get plugin description
     */
    private String getPluginDescription(String elementName) {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("http", "Performs HTTP requests to retrieve web pages");
        descriptions.put("xpath", "Extract data using XPath expressions");
        descriptions.put("html-to-xml", "Convert HTML to well-formed XML");
        descriptions.put("xquery", "Query XML data with XQuery");
        descriptions.put("regexp", "Extract data using regular expressions");
        descriptions.put("def", "Define or update a variable");
        descriptions.put("get", "Get variable value");
        descriptions.put("if", "Conditional execution");
        descriptions.put("loop", "Iterate over items");
        descriptions.put("while", "Loop while condition is true");
        descriptions.put("try", "Try block for error handling");
        descriptions.put("catch", "Catch block for error handling");
        descriptions.put("file", "File read/write operations");
        descriptions.put("template", "Generate content from template");
        descriptions.put("script", "Execute JavaScript/Groovy/BeanShell");
        descriptions.put("json-to-xml", "Convert JSON to XML");
        descriptions.put("xml-to-json", "Convert XML to JSON");
        descriptions.put("function", "Define a reusable function");
        descriptions.put("call", "Call a function");
        descriptions.put("log", "Output log message");
        
        return descriptions.getOrDefault(elementName, "WebHarvest plugin");
    }
    
    /**
     * Infer plugin category from element name
     */
    private String inferPluginCategory(String elementName) {
        if (elementName.matches("http|html-to-xml|webbrowser")) {
            return "HTTP & Web";
        } else if (elementName.matches("xpath|xquery|regexp")) {
            return "Data Extraction";
        } else if (elementName.matches("if|else|loop|while|try|catch|case")) {
            return "Control Flow";
        } else if (elementName.matches("def|get|var|set-var|get-var")) {
            return "Variables";
        } else if (elementName.matches("json-to-xml|xml-to-json|xslt|template|text|xml")) {
            return "Data Processing";
        } else if (elementName.matches("file")) {
            return "File Operations";
        } else if (elementName.matches("connection|database")) {
            return "Database";
        } else if (elementName.matches("mail|ftp")) {
            return "Communication";
        } else if (elementName.matches("function|call|call-param|return")) {
            return "Functions";
        } else if (elementName.matches("sleep|exit|empty|constant|list|log|script")) {
            return "Utility";
        } else {
            return "Other";
        }
    }
    
    /**
     * Map plugin to icon (emoji)
     */
    /**
     * Get Font Awesome icon class - EXACT from docs/web/index.html processor-icon
     */
    private String getPluginIcon(String category, String elementName) {
        String name = elementName.toLowerCase();
        
        // HTTP & Web (from docs/web/index.html line 317-332)
        if ("http".equals(name)) return "fa-globe";
        if ("html-to-xml".equals(name)) return "fa-file-code";
        
        // Data Extraction (line 334-396)
        if ("xpath".equals(name)) return "fa-code-branch";
        if ("xquery".equals(name)) return "fa-database";
        if ("regexp".equals(name)) return "fa-search";
        
        // Data Processing (line 342-412)
        if ("json-to-xml".equals(name)) return "fa-brackets-curly";
        if ("xml-to-json".equals(name)) return "fa-code";
        if ("template".equals(name)) return "fa-file-alt";
        if ("text".equals(name)) return "fa-align-left";
        if ("xslt".equals(name)) return "fa-shuffle";
        if ("xml".equals(name)) return "fa-file-code";
        
        // Variables (line 350-364)
        if ("def".equals(name)) return "fa-tag";
        if ("var".equals(name)) return "fa-box";
        if ("get".equals(name)) return "fa-arrow-right";
        if ("get-var".equals(name)) return "fa-arrow-right";
        if ("set-var".equals(name)) return "fa-arrow-left";
        
        // Control Flow (line 374-380)
        if ("loop".equals(name)) return "fa-repeat";
        if ("while".equals(name)) return "fa-rotate";
        if ("if".equals(name)) return "fa-question";
        if ("else".equals(name)) return "fa-exclamation";
        if ("try".equals(name)) return "fa-shield";
        if ("catch".equals(name)) return "fa-triangle-exclamation";
        
        // Functions (line 392-404)
        if ("function".equals(name)) return "fa-function";
        if ("call".equals(name)) return "fa-phone";
        if ("call-param".equals(name)) return "fa-sliders";
        if ("return".equals(name)) return "fa-arrow-turn-down";
        
        // File Operations (line 406-412)
        if ("file".equals(name)) return "fa-file";
        
        // Utility (line 398-404)
        if ("script".equals(name)) return "fa-code";
        if ("constant".equals(name)) return "fa-lock";
        if ("list".equals(name)) return "fa-list";
        if ("sleep".equals(name)) return "fa-clock";
        if ("exit".equals(name)) return "fa-right-from-bracket";
        if ("empty".equals(name)) return "fa-circle-notch";
        if ("log".equals(name)) return "fa-file-lines";
        if ("echo".equals(name)) return "fa-bullhorn";
        
        // Configuration
        if ("config".equals(name)) return "fa-gear";
        
        // Extensions
        if ("database".equals(name)) return "fa-database";
        if ("connection".equals(name)) return "fa-plug";
        if ("db-param".equals(name)) return "fa-sliders";
        if ("ftp".equals(name)) return "fa-server";
        if ("mail".equals(name)) return "fa-envelope";
        if ("webbrowser".equals(name)) return "fa-window-maximize";
        if ("zip".equals(name)) return "fa-file-zipper";
        if ("http-header".equals(name)) return "fa-heading";
        if ("http-param".equals(name)) return "fa-sliders";
        
        return "fa-puzzle-piece";
    }
    
    /**
     * Generate help URL for plugin
     */
    static String getHelpUrl(String elementName) {
        String documentationPage = elementName == null ? null : EXTERNAL_DOCUMENTATION_PAGES.get(elementName);
        if (documentationPage == null && elementName != null && CORE_DOCUMENTATION_PAGES.contains(elementName)) {
            documentationPage = elementName + ".html";
        }
        if (documentationPage == null) {
            documentationPage = "index.html";
        }
        return DOCUMENTATION_BASE_URL + documentationPage;
    }

    static String resolveHelpUrl(String elementName, String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()
                || configuredUrl.startsWith("https://web-harvest.sourceforge.io/documentation/")) {
            return getHelpUrl(elementName);
        }
        return configuredUrl;
    }
    
    /**
     * Get mock plugin metadata for fallback
     */
    private List<Map<String, Object>> getMockPluginMetadata() {
        List<Map<String, Object>> plugins = new ArrayList<>();
        
        // HTTP Plugin
        plugins.add(createPluginMetadata(
            "http",
            "HTTP Request",
            "Performs HTTP requests to retrieve web pages",
            "HTTP & Web",
            "🌐",
            Arrays.asList(
                createAttribute("url", "string", true, null, "Target URL"),
                createAttribute("method", "enum", false, "GET", "HTTP method", 
                    Arrays.asList("GET", "POST", "PUT", "DELETE"), null, null),
                createAttribute("timeout", "number", false, 30000, "Request timeout in milliseconds",
                    null, 0, 300000)
            ),
            true,
            Arrays.asList("http-header", "http-param")
        ));
        
        // XPath Plugin
        plugins.add(createPluginMetadata(
            "xpath",
            "XPath Query",
            "Extract data using XPath expressions",
            "Data Extraction",
            "🔍",
            Arrays.asList(
                createAttribute("expression", "string", true, null, "XPath expression")
            ),
            true,
            Collections.emptyList()
        ));
        
        // Def Plugin
        plugins.add(createPluginMetadata(
            "def",
            "Define Variable",
            "Define or update a variable",
            "Variables",
            "📦",
            Arrays.asList(
                createAttribute("var", "string", true, null, "Variable name")
            ),
            true,
            Collections.emptyList()
        ));
        
        // Loop Plugin
        plugins.add(createPluginMetadata(
            "loop",
            "Loop",
            "Iterate over a list of items",
            "Control Flow",
            "🔁",
            Arrays.asList(
                createAttribute("item", "string", false, null, "Item variable name"),
                createAttribute("index", "string", false, null, "Index variable name")
            ),
            true,
            Arrays.asList("list", "body")
        ));
        
        // If Plugin
        plugins.add(createPluginMetadata(
            "if",
            "Conditional",
            "Conditional execution based on condition",
            "Control Flow",
            "🔀",
            Arrays.asList(
                createAttribute("condition", "string", true, null, "Boolean condition")
            ),
            true,
            Arrays.asList("then", "else")
        ));
        
        return plugins;
    }
    
    private Map<String, Object> createPluginMetadata(
            String elementName, String name, String description, String category,
            String icon, List<Map<String, Object>> attributes, boolean allowsBody,
            List<String> allowedSubElements) {
        
        Map<String, Object> plugin = new HashMap<>();
        plugin.put("id", elementName);
        plugin.put("name", name);
        plugin.put("elementName", elementName);
        plugin.put("description", description);
        plugin.put("category", category);
        plugin.put("icon", icon);
        plugin.put("version", "2.2.0");
        plugin.put("author", "WebHarvest Team");
        plugin.put("tags", Arrays.asList(elementName, category.toLowerCase().replace(" & ", "-")));
        plugin.put("helpUrl", getHelpUrl(elementName));
        
        Map<String, Object> schema = new HashMap<>();
        schema.put("attributes", attributes);
        schema.put("allowsBody", allowsBody);
        schema.put("allowedSubElements", allowedSubElements);
        plugin.put("schema", schema);
        
        return plugin;
    }
    
    private Map<String, Object> createAttribute(
            String name, String type, boolean required, Object defaultValue, String description) {
        return createAttribute(name, type, required, defaultValue, description, null, null, null);
    }
    
    private Map<String, Object> createAttribute(
            String name, String type, boolean required, Object defaultValue, String description,
            List<String> options, Integer min, Integer max) {
        
        Map<String, Object> attr = new HashMap<>();
        attr.put("name", name);
        attr.put("type", type);
        attr.put("required", required);
        attr.put("description", description);
        
        if (defaultValue != null) {
            attr.put("defaultValue", defaultValue);
        }
        
        if (options != null && !options.isEmpty()) {
            attr.put("options", options);
        }
        
        if (min != null) {
            attr.put("min", min);
        }
        
        if (max != null) {
            attr.put("max", max);
        }
        
        return attr;
    }
    
    /**
     * Add VisualPluginMetadata fields to the response map
     */
    private void addVisualMetadataFields(Map<String, Object> visual, VisualPluginMetadata visualMetadata) {
        // Color and group (use group instead of category for visual grouping)
        if (visualMetadata.getColor() != null && !visualMetadata.getColor().isEmpty()) {
            visual.put("color", visualMetadata.getColor());
        }
        if (visualMetadata.getGroup() != null && !visualMetadata.getGroup().isEmpty()) {
            visual.put("group", visualMetadata.getGroup());
        }
        
        // Dimensions
        if (visualMetadata.getDefaultWidth() > 0) {
            visual.put("defaultWidth", visualMetadata.getDefaultWidth());
        }
        if (visualMetadata.getDefaultHeight() > 0) {
            visual.put("defaultHeight", visualMetadata.getDefaultHeight());
        }
        
        // Tooltip
        if (visualMetadata.getTooltip() != null && !visualMetadata.getTooltip().isEmpty()) {
            visual.put("tooltip", visualMetadata.getTooltip());
        }
        
        // Port type
        if (visualMetadata.getPortType() != null) {
            visual.put("portType", visualMetadata.getPortType().name());
        }
        
        // Input ports
        if (visualMetadata.getInputPorts() != null && visualMetadata.getInputPorts().length > 0) {
            List<Map<String, Object>> inputPortsJson = new ArrayList<>();
            for (InputPort port : visualMetadata.getInputPorts()) {
                Map<String, Object> portJson = new HashMap<>();
                portJson.put("name", port.getName());
                if (port.getDescription() != null) {
                    portJson.put("description", port.getDescription());
                }
                if (port.getType() != null) {
                    portJson.put("type", port.getType().name());
                }
                portJson.put("required", port.isRequired());
                inputPortsJson.add(portJson);
            }
            visual.put("inputPorts", inputPortsJson);
        }
        
        // Output ports
        if (visualMetadata.getOutputPorts() != null && visualMetadata.getOutputPorts().length > 0) {
            List<Map<String, Object>> outputPortsJson = new ArrayList<>();
            for (OutputPort port : visualMetadata.getOutputPorts()) {
                Map<String, Object> portJson = new HashMap<>();
                portJson.put("name", port.getName());
                if (port.getDescription() != null) {
                    portJson.put("description", port.getDescription());
                }
                if (port.getType() != null) {
                    portJson.put("type", port.getType().name());
                }
                outputPortsJson.add(portJson);
            }
            visual.put("outputPorts", outputPortsJson);
        }
        
        // Keyboard shortcuts
        if (visualMetadata.getKeyboardShortcuts() != null && visualMetadata.getKeyboardShortcuts().length > 0) {
            List<Map<String, Object>> shortcutsJson = new ArrayList<>();
            for (KeyboardShortcut shortcut : visualMetadata.getKeyboardShortcuts()) {
                Map<String, Object> shortcutJson = new HashMap<>();
                shortcutJson.put("key", shortcut.getKey());
                shortcutJson.put("action", shortcut.getAction());
                shortcutJson.put("isGlobal", shortcut.isGlobal());
                shortcutsJson.add(shortcutJson);
            }
            visual.put("keyboardShortcuts", shortcutsJson);
        }
        
        // Accessibility
        if (visualMetadata.getAccessibility() != null) {
            Map<String, Object> accJson = new HashMap<>();
            AccessibilityMetadata acc = visualMetadata.getAccessibility();
            if (acc.getAriaLabel() != null) {
                accJson.put("ariaLabel", acc.getAriaLabel());
            }
            if (acc.getAriaDescription() != null) {
                accJson.put("ariaDescription", acc.getAriaDescription());
            }
            if (acc.getKeyboardNavigation() != null && acc.getKeyboardNavigation().length > 0) {
                accJson.put("keyboardNavigation", Arrays.asList(acc.getKeyboardNavigation()));
            }
            visual.put("accessibility", accJson);
        }
        
        // Linked services
        if (visualMetadata.getLinkedServices() != null && visualMetadata.getLinkedServices().length > 0) {
            List<Map<String, Object>> servicesJson = new ArrayList<>();
            for (LinkedServiceMetadata service : visualMetadata.getLinkedServices()) {
                Map<String, Object> serviceJson = new HashMap<>();
                serviceJson.put("serviceType", service.getServiceType());
                serviceJson.put("serviceName", service.getServiceName());
                if (service.getDescription() != null) {
                    serviceJson.put("description", service.getDescription());
                }
                serviceJson.put("required", service.isRequired());
                servicesJson.add(serviceJson);
            }
            visual.put("linkedServices", servicesJson);
        }
        
        // Parent-child relationship (for detail plugins)
        if (visualMetadata.getParentRelationship() != null) {
            Map<String, Object> parentJson = new HashMap<>();
            ParentChildRelationship parent = visualMetadata.getParentRelationship();
            parentJson.put("parentElementName", parent.getParentElementName());
            parentJson.put("required", parent.isRequired());
            if (parent.getParentDisplayName() != null) {
                parentJson.put("parentDisplayName", parent.getParentDisplayName());
            }
            visual.put("parentRelationship", parentJson);
        }
        
        // Allowed child plugins (for master plugins)
        if (visualMetadata.getAllowedChildPlugins() != null && visualMetadata.getAllowedChildPlugins().length > 0) {
            visual.put("allowedChildPlugins", Arrays.asList(visualMetadata.getAllowedChildPlugins()));
        }
    }
    
    /**
     * Add default port types when VisualPluginMetadata is not available
     * Ensures port types are always exported for LLM agent
     */
    private void addDefaultPortTypes(Map<String, Object> plugin) {
        @SuppressWarnings("unchecked")
        Map<String, Object> visual = (Map<String, Object>) plugin.get("visual");
        if (visual == null) {
            visual = new HashMap<>();
            plugin.put("visual", visual);
        }
        
        // Add default input port if not present
        if (!visual.containsKey("inputPorts")) {
            List<Map<String, Object>> inputPorts = new ArrayList<>();
            Map<String, Object> defaultInput = new HashMap<>();
            defaultInput.put("name", "input");
            defaultInput.put("type", "STRING");
            defaultInput.put("required", false);
            inputPorts.add(defaultInput);
            visual.put("inputPorts", inputPorts);
        }
        
        // Add default output port if not present
        if (!visual.containsKey("outputPorts")) {
            List<Map<String, Object>> outputPorts = new ArrayList<>();
            Map<String, Object> defaultOutput = new HashMap<>();
            defaultOutput.put("name", "output");
            defaultOutput.put("type", "STRING");
            outputPorts.add(defaultOutput);
            visual.put("outputPorts", outputPorts);
        }
        
        // Add default portType if not present
        if (!visual.containsKey("portType")) {
            visual.put("portType", "STRING");
        }
    }
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // CORS preflight
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}

