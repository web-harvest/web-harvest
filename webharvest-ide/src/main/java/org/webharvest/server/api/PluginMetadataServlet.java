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

/**
 * Servlet providing plugin metadata for the Visual Designer.
 * 
 * Returns JSON with all registered plugins, their schemas, and metadata.
 * This endpoint is used by the visual designer frontend to populate
 * the plugin palette and generate property panels.
 * 
 * @author WebHarvest Team
 * @since 2.2.0
 */
@WebServlet(name = "PluginMetadataServlet", urlPatterns = {"/api/plugins/metadata"})
public class PluginMetadataServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataServlet.class);
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        try {
            // Get all plugins from CorePluginRegistry
            List<Map<String, Object>> plugins = getPluginMetadataFromRegistry();
            
            Map<String, Object> response = new HashMap<>();
            response.put("plugins", plugins);
            response.put("count", plugins.size());
            response.put("version", "2.2.0");
            response.put("timestamp", System.currentTimeMillis());
            
            // Set response headers
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.setHeader("Access-Control-Allow-Origin", "*"); // For development with Vite
            
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
        
        Map<String, Object> visual = new HashMap<>();
        
        visual.put("id", elementName);
        visual.put("name", formatPluginName(elementName));
        visual.put("elementName", elementName);
        visual.put("description", getPluginDescription(elementName));
        visual.put("category", inferPluginCategory(elementName));
        visual.put("icon", getPluginIcon("", elementName));
        visual.put("version", "2.2.0");
        visual.put("author", "WebHarvest Team");
        visual.put("tags", Arrays.asList(elementName, inferPluginCategory(elementName).toLowerCase()));
        visual.put("helpUrl", getHelpUrl(elementName));
        
        // Extract REAL schema from plugin instance
        Map<String, Object> schema = extractRealPluginSchema(pluginClass);
        visual.put("schema", schema);
        
        return visual;
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
    private String getHelpUrl(String elementName) {
        return String.format(
            "https://web-harvest.sourceforge.io/documentation/2.2.0/%s.html",
            elementName
        );
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
        plugin.put("helpUrl", "https://web-harvest.sourceforge.io/documentation/plugin-" + elementName + ".html");
        
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
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // CORS preflight
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}

