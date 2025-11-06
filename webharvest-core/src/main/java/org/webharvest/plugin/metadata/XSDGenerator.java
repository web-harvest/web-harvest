package org.webharvest.plugin.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates XSD schema files dynamically from plugin metadata.
 * Creates complete XSD schemas with all elements, attributes, and types
 * based on PluginDescriptor objects from PluginMetadataCatalog.
 * 
 * <p>All metadata comes from plugin classes - no static XSD files needed.</p>
 * 
 * @author WebHarvest Team
 * @since 2.3.0
 */
public final class XSDGenerator {
    
    private static final Logger LOG = LoggerFactory.getLogger(XSDGenerator.class);
    
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String DEFAULT_NAMESPACE = "http://org.webharvest/schema/2.1/core";
    
    private XSDGenerator() {
        // Utility class
    }
    
    /**
     * Generates XSD schema for all plugins from PluginMetadataCatalog.
     * 
     * @param namespace target namespace (defaults to 2.1/core if null)
     * @return XSD schema as XML string
     */
    public static String generateXSD(String namespace) {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        return generateXSD(descriptors, namespace != null ? namespace : DEFAULT_NAMESPACE);
    }
    
    /**
     * Generates XSD schema for given plugin descriptors.
     * 
     * @param descriptors list of plugin descriptors
     * @param namespace target namespace
     * @return XSD schema as XML string
     */
    public static String generateXSD(List<PluginDescriptor> descriptors, String namespace) {
        try {
            javax.xml.parsers.DocumentBuilderFactory factory = 
                javax.xml.parsers.DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            // Create root schema element
            Element schema = doc.createElementNS(XSD_NAMESPACE, "xs:schema");
            schema.setAttribute("xmlns", namespace);
            schema.setAttribute("xmlns:xs", XSD_NAMESPACE);
            schema.setAttribute("targetNamespace", namespace);
            schema.setAttribute("elementFormDefault", "unqualified");
            schema.setAttribute("attributeFormDefault", "unqualified");
            doc.appendChild(schema);
            
            // Import XML namespace
            Element xmlImport = doc.createElementNS(XSD_NAMESPACE, "xs:import");
            xmlImport.setAttribute("namespace", "http://www.w3.org/XML/1998/namespace");
            schema.appendChild(xmlImport);
            
            // Filter plugins by namespace (if specified in extensions)
            List<PluginDescriptor> namespacePlugins = descriptors.stream()
                .filter(d -> {
                    String pluginNamespace = getNamespaceFromExtensions(d);
                    return pluginNamespace == null || pluginNamespace.equals(namespace);
                })
                .collect(Collectors.toList());
            
            // Generate bodyElements group (all plugins that can appear in body)
            generateBodyElementsGroup(doc, schema, namespacePlugins);
            
            // Generate bodyType complex type
            generateBodyType(doc, schema);
            
            // Generate config element
            generateConfigElement(doc, schema, namespacePlugins);
            
            // Generate elements and complex types for each plugin
            // Skip config as it's already generated above
            for (PluginDescriptor descriptor : namespacePlugins) {
                if ("config".equals(descriptor.getElementName())) {
                    continue; // Skip config - already generated
                }
                generatePluginElement(doc, schema, descriptor);
            }
            
            // Convert to string
            return documentToString(doc);
            
        } catch (Exception e) {
            LOG.error("Failed to generate XSD schema", e);
            throw new RuntimeException("Failed to generate XSD schema", e);
        }
    }
    
    /**
     * Gets namespace from plugin extensions, or returns null for default namespace.
     */
    private static String getNamespaceFromExtensions(PluginDescriptor descriptor) {
        if (descriptor.getExtensions().containsKey("namespace")) {
            return (String) descriptor.getExtensions().get("namespace");
        }
        return null; // Default namespace
    }
    
    /**
     * Generates bodyElements group containing all plugins that can appear in body.
     */
    private static void generateBodyElementsGroup(Document doc, Element schema, 
                                                  List<PluginDescriptor> plugins) {
        Element group = doc.createElementNS(XSD_NAMESPACE, "xs:group");
        group.setAttribute("name", "bodyElements");
        schema.appendChild(group);
        
        Element sequence = doc.createElementNS(XSD_NAMESPACE, "xs:sequence");
        group.appendChild(sequence);
        
        Element choice = doc.createElementNS(XSD_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");
        sequence.appendChild(choice);
        
        // Add all plugins that can appear at root or in body
        for (PluginDescriptor plugin : plugins) {
            // Skip config (root element)
            if ("config".equals(plugin.getElementName())) {
                continue;
            }
            
            // Only include plugins that can appear in body (not context-specific)
            if (plugin.getSemantics().canAppearAtRoot() || 
                plugin.getSemantics().allowsChildren()) {
                Element ref = doc.createElementNS(XSD_NAMESPACE, "xs:element");
                ref.setAttribute("ref", plugin.getElementName());
                choice.appendChild(ref);
            }
        }
        
        // Add xs:any for extensibility
        Element any = doc.createElementNS(XSD_NAMESPACE, "xs:any");
        any.setAttribute("namespace", "##other");
        any.setAttribute("processContents", "strict");
        any.setAttribute("minOccurs", "0");
        any.setAttribute("maxOccurs", "unbounded");
        choice.appendChild(any);
    }
    
    /**
     * Generates bodyType complex type.
     */
    private static void generateBodyType(Document doc, Element schema) {
        Element complexType = doc.createElementNS(XSD_NAMESPACE, "xs:complexType");
        complexType.setAttribute("name", "bodyType");
        complexType.setAttribute("mixed", "true");
        schema.appendChild(complexType);
        
        Element group = doc.createElementNS(XSD_NAMESPACE, "xs:group");
        group.setAttribute("ref", "bodyElements");
        complexType.appendChild(group);
        
        // Add id attribute
        Element idAttr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
        idAttr.setAttribute("name", "id");
        idAttr.setAttribute("use", "optional");
        idAttr.setAttribute("type", "xs:string");
        
        Element idAnnotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
        Element idDoc = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
        idDoc.setTextContent("Special attribute used in debugging. Saves content to _debug/<id>_<num>.debug");
        idAnnotation.appendChild(idDoc);
        idAttr.appendChild(idAnnotation);
        
        complexType.appendChild(idAttr);
    }
    
    /**
     * Generates config root element.
     */
    private static void generateConfigElement(Document doc, Element schema, 
                                             List<PluginDescriptor> plugins) {
        Element configElement = doc.createElementNS(XSD_NAMESPACE, "xs:element");
        configElement.setAttribute("name", "config");
        schema.appendChild(configElement);
        
        Element annotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
        Element documentation = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
        documentation.setTextContent("Root element of every WebHarvest configuration file.");
        annotation.appendChild(documentation);
        configElement.appendChild(annotation);
        
        Element complexType = doc.createElementNS(XSD_NAMESPACE, "xs:complexType");
        configElement.appendChild(complexType);
        
        Element complexContent = doc.createElementNS(XSD_NAMESPACE, "xs:complexContent");
        complexType.appendChild(complexContent);
        
        Element extension = doc.createElementNS(XSD_NAMESPACE, "xs:extension");
        extension.setAttribute("base", "bodyType");
        complexContent.appendChild(extension);
        
        // Add charset attribute
        Element charsetAttr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
        charsetAttr.setAttribute("name", "charset");
        charsetAttr.setAttribute("use", "optional");
        charsetAttr.setAttribute("default", "UTF-8");
        charsetAttr.setAttribute("type", "xs:string");
        
        Element charsetAnnotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
        Element charsetDoc = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
        charsetDoc.setTextContent("Default charset used throughout configuration.");
        charsetAnnotation.appendChild(charsetDoc);
        charsetAttr.appendChild(charsetAnnotation);
        extension.appendChild(charsetAttr);
        
        // Add scriptlang attribute
        Element scriptlangAttr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
        scriptlangAttr.setAttribute("name", "scriptlang");
        scriptlangAttr.setAttribute("use", "optional");
        scriptlangAttr.setAttribute("default", "beanshell");
        scriptlangAttr.setAttribute("type", "xs:string");
        
        Element scriptlangAnnotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
        Element scriptlangDoc = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
        scriptlangDoc.setTextContent("Default scripting engine: beanshell, javascript, or groovy.");
        scriptlangAnnotation.appendChild(scriptlangDoc);
        scriptlangAttr.appendChild(scriptlangAnnotation);
        extension.appendChild(scriptlangAttr);
    }
    
    /**
     * Generates XSD element and complex type for a plugin.
     */
    private static void generatePluginElement(Document doc, Element schema, 
                                              PluginDescriptor descriptor) {
        String elementName = descriptor.getElementName();
        
        // Generate element
        Element element = doc.createElementNS(XSD_NAMESPACE, "xs:element");
        element.setAttribute("name", elementName);
        schema.appendChild(element);
        
        // Add documentation
        if (descriptor.getDescription() != null) {
            Element annotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
            Element documentation = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
            documentation.setTextContent(descriptor.getDescription());
            annotation.appendChild(documentation);
            element.appendChild(annotation);
        }
        
        // Generate complex type
        String typeName = capitalize(elementName) + "Type";
        Element complexType = generateComplexType(doc, schema, descriptor, typeName);
        element.setAttribute("type", typeName);
    }
    
    /**
     * Generates complex type for a plugin.
     */
    private static Element generateComplexType(Document doc, Element schema,
                                              PluginDescriptor descriptor, String typeName) {
        Element complexType = doc.createElementNS(XSD_NAMESPACE, "xs:complexType");
        complexType.setAttribute("name", typeName);
        schema.appendChild(complexType);
        
        PluginSemantics semantics = descriptor.getSemantics();
        @SuppressWarnings("unchecked")
        Map<String, Object> schemaInfo = (Map<String, Object>) descriptor.getExtensions().get("schema");
        
        // Determine if mixed content (allows text)
        boolean mixed = semantics.allowsTextContent();
        if (mixed) {
            complexType.setAttribute("mixed", "true");
        }
        
        // Generate child elements structure
        if (semantics.allowsChildren()) {
            if (schemaInfo != null && schemaInfo.containsKey("allowedSubElements")) {
                // Specific child elements
                @SuppressWarnings("unchecked")
                List<String> allowedChildren = (List<String>) schemaInfo.get("allowedSubElements");
                if (!allowedChildren.isEmpty()) {
                    generateChildElementsSequence(doc, complexType, allowedChildren);
                } else {
                    // Any child from bodyElements
                    generateBodyElementsReference(doc, complexType);
                }
            } else if (semantics.acceptsAnyChild()) {
                // Any child from bodyElements
                generateBodyElementsReference(doc, complexType);
            }
        }
        
        // Generate attributes
        generateAttributes(doc, complexType, descriptor, schemaInfo);
        
        return complexType;
    }
    
    /**
     * Generates sequence of specific child elements.
     */
    private static void generateChildElementsSequence(Document doc, Element complexType,
                                                     List<String> childElementNames) {
        Element sequence = doc.createElementNS(XSD_NAMESPACE, "xs:sequence");
        complexType.appendChild(sequence);
        
        for (String childName : childElementNames) {
            Element childElement = doc.createElementNS(XSD_NAMESPACE, "xs:element");
            childElement.setAttribute("name", childName);
            childElement.setAttribute("minOccurs", "0");
            childElement.setAttribute("maxOccurs", "unbounded");
            
            // Check if it's a specific type (like catch in try)
            // For now, use bodyType
            Element childComplexType = doc.createElementNS(XSD_NAMESPACE, "xs:complexType");
            childComplexType.setAttribute("mixed", "true");
            Element childGroup = doc.createElementNS(XSD_NAMESPACE, "xs:group");
            childGroup.setAttribute("ref", "bodyElements");
            childComplexType.appendChild(childGroup);
            childElement.appendChild(childComplexType);
            
            sequence.appendChild(childElement);
        }
    }
    
    /**
     * Generates reference to bodyElements group.
     */
    private static void generateBodyElementsReference(Document doc, Element complexType) {
        Element group = doc.createElementNS(XSD_NAMESPACE, "xs:group");
        group.setAttribute("ref", "bodyElements");
        complexType.appendChild(group);
    }
    
    /**
     * Generates attributes for a plugin.
     */
    @SuppressWarnings("unchecked")
    private static void generateAttributes(Document doc, Element complexType,
                                          PluginDescriptor descriptor,
                                          Map<String, Object> schemaInfo) {
        if (schemaInfo == null) {
            // Add id attribute as default
            Element idAttr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
            idAttr.setAttribute("name", "id");
            idAttr.setAttribute("type", "xs:string");
            complexType.appendChild(idAttr);
            return;
        }
        
        // Get attribute definitions
        List<Map<String, Object>> attrDefs = new ArrayList<>();
        if (schemaInfo.containsKey("attributeDefinitions")) {
            attrDefs = (List<Map<String, Object>>) schemaInfo.get("attributeDefinitions");
        }
        
        // Generate attributes from definitions
        for (Map<String, Object> attrDef : attrDefs) {
            String name = (String) attrDef.get("name");
            if ("id".equals(name)) {
                continue; // id is handled separately
            }
            
            Element attr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
            attr.setAttribute("name", name);
            
            // Set type
            String type = (String) attrDef.getOrDefault("type", "STRING");
            attr.setAttribute("type", mapAttributeTypeToXSD(type));
            
            // Set required/optional
            Boolean required = (Boolean) attrDef.get("required");
            if (required != null && required) {
                attr.setAttribute("use", "required");
            } else {
                attr.setAttribute("use", "optional");
            }
            
            // Set default value if present
            String defaultValue = (String) attrDef.get("defaultValue");
            if (defaultValue != null && !defaultValue.isEmpty()) {
                attr.setAttribute("default", defaultValue);
            }
            
            // Add documentation if present
            String description = (String) attrDef.get("description");
            if (description != null && !description.isEmpty()) {
                Element annotation = doc.createElementNS(XSD_NAMESPACE, "xs:annotation");
                Element docElement = doc.createElementNS(XSD_NAMESPACE, "xs:documentation");
                docElement.setTextContent(description);
                annotation.appendChild(docElement);
                attr.appendChild(annotation);
            }
            
            complexType.appendChild(attr);
        }
        
        // Always add id attribute
        Element idAttr = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
        idAttr.setAttribute("name", "id");
        idAttr.setAttribute("type", "xs:string");
        idAttr.setAttribute("use", "optional");
        complexType.appendChild(idAttr);
    }
    
    /**
     * Maps attribute type to XSD type.
     */
    private static String mapAttributeTypeToXSD(String type) {
        if (type == null) {
            return "xs:string";
        }
        
        switch (type.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return "xs:int";
            case "LONG":
                return "xs:long";
            case "DOUBLE":
            case "FLOAT":
                return "xs:double";
            case "BOOLEAN":
                return "xs:boolean";
            case "URL":
            case "FILE_PATH":
                return "xs:string";
            case "REGEX":
                return "xs:string";
            case "ENUM":
                return "xs:string";
            default:
                return "xs:string";
        }
    }
    
    /**
     * Capitalizes first letter of string.
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    /**
     * Converts DOM Document to XML string.
     */
    private static String documentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}

