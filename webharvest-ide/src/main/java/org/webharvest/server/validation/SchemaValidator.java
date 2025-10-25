package org.webharvest.server.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Schema-based validator for WebHarvest configurations.
 * Validates against plugin schemas (attributes, context, hierarchy).
 */
public class SchemaValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(SchemaValidator.class);
    
    private final CorePluginRegistry pluginRegistry;
    
    public SchemaValidator() {
        this.pluginRegistry = CorePluginRegistry.getInstance();
    }
    
    /**
     * Validates XML configuration against plugin schemas
     * @param xmlContent XML configuration string
     * @return Validation result with list of errors
     */
    public ValidationResult validate(String xmlContent) {
        List<ValidationError> errors = new ArrayList<>();
        
        try {
            // Parse XML DOM
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
            
            Element root = doc.getDocumentElement();
            if (!"config".equals(root.getLocalName())) {
                errors.add(new ValidationError(
                    "Root element must be 'config'",
                    1,
                    "config",
                    "STRUCTURE"
                ));
                return new ValidationResult(false, errors);
            }
            
            // Validate all child elements recursively
            validateElement(root, null, errors);
            
        } catch (Exception e) {
            LOG.error("Failed to parse XML for validation", e);
            errors.add(new ValidationError(
                "XML parse error: " + e.getMessage(),
                0,
                "unknown",
                "XML_PARSE"
            ));
        }
        
        return errors.isEmpty() 
            ? new ValidationResult(true, errors)
            : new ValidationResult(false, errors);
    }
    
    private void validateElement(Element element, String parentPluginName, List<ValidationError> errors) {
        NodeList children = element.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element child = (Element) node;
                String pluginName = child.getLocalName();
                
                // Skip 'config' root
                if ("config".equals(pluginName)) {
                    validateElement(child, null, errors);
                    continue;
                }
                
                // Check if plugin exists
                if (!pluginRegistry.isPluginRegistered(pluginName)) {
                    errors.add(new ValidationError(
                        "Unknown plugin: " + pluginName,
                        getLineNumber(child),
                        pluginName,
                        "UNKNOWN_PLUGIN"
                    ));
                    continue;
                }
                
                // Get plugin class
                Class<? extends Plugin> pluginClass = pluginRegistry.getPluginClass(pluginName);
                if (pluginClass == null) {
                    continue; // Skip validation if class not found
                }
                
                // Get schema from plugin instance
                PluginConfigurationSchema schema = getSchemaFromPlugin(pluginClass);
                if (schema == null) {
                    LOG.debug("No schema available for plugin: {}", pluginName);
                    continue; // Skip validation if schema not available
                }
                
                // Validate context (parent requirements)
                if (!schema.canAppearAtRoot() && parentPluginName == null) {
                    String[] requiredParentsArray = schema.getRequiredParents();
                    if (requiredParentsArray != null && requiredParentsArray.length > 0) {
                        errors.add(new ValidationError(
                            "Plugin '" + pluginName + "' requires parent: " + Arrays.toString(requiredParentsArray),
                            getLineNumber(child),
                            pluginName,
                            "INVALID_CONTEXT"
                        ));
                    }
                }
                
                if (parentPluginName != null && schema.isContextSpecific()) {
                    String[] requiredParentsArray = schema.getRequiredParents();
                    if (requiredParentsArray != null) {
                        boolean foundParent = false;
                        for (String reqParent : requiredParentsArray) {
                            if (reqParent.equals(parentPluginName)) {
                                foundParent = true;
                                break;
                            }
                        }
                        if (!foundParent) {
                            errors.add(new ValidationError(
                                "Plugin '" + pluginName + "' cannot appear in '" + parentPluginName + "'. Required parent: " + Arrays.toString(requiredParentsArray),
                                getLineNumber(child),
                                pluginName,
                                "INVALID_PARENT"
                            ));
                        }
                    }
                }
                
                // Validate attributes
                validateAttributes(child, schema, errors);
                
                // Recursively validate children
                validateElement(child, pluginName, errors);
                
            } else if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent().trim();
                if (!text.isEmpty() && !"config".equals(element.getLocalName())) {
                    // Text content is allowed in most plugins (as body content)
                    // Only flag if it's truly random text at config level
                    if (element.getLocalName().equals("config")) {
                        errors.add(new ValidationError(
                            "Invalid text content outside plugin tags: " + text.substring(0, Math.min(50, text.length())),
                            getLineNumber(element),
                            element.getLocalName(),
                            "INVALID_CONTENT"
                        ));
                    }
                }
            }
        }
    }
    
    private void validateAttributes(Element element, PluginConfigurationSchema schema, List<ValidationError> errors) {
        String pluginName = element.getLocalName();
        
        // Get all attributes from element
        org.w3c.dom.NamedNodeMap attributes = element.getAttributes();
        Set<String> elementAttrs = new HashSet<>();
        
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            String attrName = attr.getLocalName();
            if (attrName == null) {
                attrName = attr.getNodeName();
            }
            
            // Skip xmlns attributes
            if (attrName.startsWith("xmlns")) {
                continue;
            }
            
            elementAttrs.add(attrName);
        }
        
        // Get allowed attributes from schema
        Set<String> allowedAttrs = new HashSet<>();
        
        // Add required attributes
        String[] requiredAttrs = schema.getRequiredAttributes();
        if (requiredAttrs != null) {
            allowedAttrs.addAll(Arrays.asList(requiredAttrs));
        }
        
        // Add optional attributes
        String[] optionalAttrs = schema.getOptionalAttributes();
        if (optionalAttrs != null) {
            allowedAttrs.addAll(Arrays.asList(optionalAttrs));
        }
        
        // Check for unknown attributes
        for (String attrName : elementAttrs) {
            if (!allowedAttrs.contains(attrName)) {
                errors.add(new ValidationError(
                    "Unknown attribute '" + attrName + "' on plugin '" + pluginName + "'. Allowed: " + allowedAttrs,
                    getLineNumber(element),
                    pluginName,
                    "UNKNOWN_ATTRIBUTE"
                ));
            }
        }
        
        // Check for required attributes
        if (requiredAttrs != null) {
            for (String requiredAttr : requiredAttrs) {
                if (!elementAttrs.contains(requiredAttr)) {
                    errors.add(new ValidationError(
                        "Required attribute '" + requiredAttr + "' missing on plugin '" + pluginName + "'",
                        getLineNumber(element),
                        pluginName,
                        "MISSING_ATTRIBUTE"
                    ));
                }
            }
        }
    }
    
    /**
     * Get schema from plugin instance via getMetadata()
     */
    private PluginConfigurationSchema getSchemaFromPlugin(Class<? extends Plugin> pluginClass) {
        try {
            // Try to create plugin instance with default constructor
            Plugin plugin = pluginClass.getDeclaredConstructor().newInstance();
            
            // Get metadata from plugin
            Method getMetadataMethod = pluginClass.getMethod("getMetadata");
            Object metadata = getMetadataMethod.invoke(plugin);
            
            if (metadata instanceof org.webharvest.plugin.PluginMetadata) {
                org.webharvest.plugin.PluginMetadata pluginMeta = 
                    (org.webharvest.plugin.PluginMetadata) metadata;
                PluginConfigurationSchema schema = pluginMeta.getConfigurationSchema();
                if (schema != null) {
                    LOG.info("✓ Got schema for plugin: {} - canAppearAtRoot: {}", 
                        pluginClass.getSimpleName(), schema.canAppearAtRoot());
                } else {
                    LOG.warn("⚠ Schema is NULL for plugin: {}", pluginClass.getSimpleName());
                }
                return schema;
            } else {
                LOG.warn("⚠ getMetadata() did not return PluginMetadata for: {}", pluginClass.getSimpleName());
            }
        } catch (Exception e) {
            LOG.warn("❌ Could not get schema from plugin: {} - {}", 
                pluginClass.getSimpleName(), e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private int getLineNumber(Node node) {
        // Note: Line numbers are not available in standard DOM parser
        // Would need to use SAX parser with LineNumberReader for accurate line numbers
        // For now, return 0 (can be enhanced later)
        return 0;
    }
}

