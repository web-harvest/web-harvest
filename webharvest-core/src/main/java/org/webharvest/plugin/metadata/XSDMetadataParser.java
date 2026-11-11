package org.webharvest.plugin.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for extracting plugin metadata from XSD schema files.
 * Fills gaps in plugin metadata where PluginConfigurationSchema is incomplete.
 * 
 * <p>Extracts:
 * - Element definitions with attributes
 * - Complex types with child element structure
 * - Documentation from xs:documentation tags
 * - Attribute definitions (name, type, required, default values)
 * 
 * @author WebHarvest Team
 * @since 2.3.0
 */
public final class XSDMetadataParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(XSDMetadataParser.class);
    
    private static final String XS_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String XS_PREFIX = "xs";
    
    private XSDMetadataParser() {
        // Utility class
    }
    
    /**
     * Parses XSD file and extracts metadata for all elements.
     * 
     * <p>Note: This is a utility method for parsing XSD files if needed.
     * In normal operation, all metadata is generated dynamically from plugin classes.
     * Static XSD files are no longer used.</p>
     * 
     * @param xsdResourcePath resource path to XSD file (e.g., "/config.xsd")
     * @return map of element names to their metadata
     */
    public static Map<String, XSDElementMetadata> parseXSD(String xsdResourcePath) {
        try (InputStream is = XSDMetadataParser.class.getResourceAsStream(xsdResourcePath)) {
            if (is == null) {
                LOG.warn("XSD resource not found: {}", xsdResourcePath);
                return Collections.emptyMap();
            }
            return parseXSD(is);
        } catch (Exception e) {
            LOG.warn("Failed to parse XSD file {}: {}", xsdResourcePath, e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Parses XSD from InputStream and extracts metadata for all elements.
     * 
     * @param is InputStream containing XSD content
     * @return map of element names to their metadata
     */
    public static Map<String, XSDElementMetadata> parseXSD(InputStream is) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            // XSD metadata is read-only input, not a reason to resolve external resources.
            // Fail closed if the active JAXP provider cannot enforce these restrictions.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            
            return parseSchema(doc);
            
        } catch (Exception e) {
            LOG.warn("Failed to parse XSD from InputStream: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
    
    /**
     * Parses schema document and extracts element metadata.
     */
    private static Map<String, XSDElementMetadata> parseSchema(Document doc) {
        Map<String, XSDElementMetadata> elements = new LinkedHashMap<>();
        
        Element schema = doc.getDocumentElement();
        if (!"schema".equals(schema.getLocalName())) {
            LOG.warn("Root element is not 'schema'");
            return Collections.emptyMap();
        }
        
        // Find all xs:element nodes
        NodeList elementNodes = schema.getElementsByTagNameNS(XS_NAMESPACE, "element");
        
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Element elementNode = (Element) elementNodes.item(i);
            String elementName = elementNode.getAttribute("name");
            
            if (elementName == null || elementName.isEmpty()) {
                continue;
            }
            
            XSDElementMetadata metadata = parseElement(elementNode);
            if (metadata != null) {
                elements.put(elementName, metadata);
            }
        }
        
        LOG.debug("Parsed {} elements from XSD", elements.size());
        return Collections.unmodifiableMap(elements);
    }
    
    /**
     * Parses a single element definition.
     */
    private static XSDElementMetadata parseElement(Element elementNode) {
        String elementName = elementNode.getAttribute("name");
        
        // Extract documentation
        String documentation = extractDocumentation(elementNode);
        
        // Extract type reference
        String typeRef = elementNode.getAttribute("type");
        
        // Find complexType or get attributes from type
        List<XSDAttributeMetadata> attributes = new ArrayList<>();
        List<String> childElements = new ArrayList<>();
        boolean allowsBody = false;
        boolean mixedContent = false;
        
        // Check for complexType directly in element
        NodeList complexTypes = elementNode.getElementsByTagNameNS(XS_NAMESPACE, "complexType");
        if (complexTypes.getLength() > 0) {
            Element complexType = (Element) complexTypes.item(0);
            parseComplexType(complexType, attributes, childElements, allowsBody, mixedContent);
            mixedContent = complexType.hasAttribute("mixed") && 
                "true".equals(complexType.getAttribute("mixed"));
        }
        
        // Check for attributes directly in element
        NodeList attrNodes = elementNode.getElementsByTagNameNS(XS_NAMESPACE, "attribute");
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Element attrNode = (Element) attrNodes.item(i);
            XSDAttributeMetadata attr = parseAttribute(attrNode);
            if (attr != null) {
                attributes.add(attr);
            }
        }
        
        // Check for mixed content (allows text)
        if (mixedContent || hasMixedContent(elementNode)) {
            allowsBody = true;
        }
        
        // Check for child elements (groups, sequences, choices)
        if (hasChildElements(elementNode)) {
            allowsBody = true;
        }
        
        return new XSDElementMetadata(elementName, documentation, attributes, childElements, allowsBody, mixedContent);
    }
    
    /**
     * Parses complexType element.
     */
    private static void parseComplexType(Element complexType, 
                                         List<XSDAttributeMetadata> attributes,
                                         List<String> childElements,
                                         boolean allowsBody,
                                         boolean mixedContent) {
        // Extract attributes
        NodeList attrNodes = complexType.getElementsByTagNameNS(XS_NAMESPACE, "attribute");
        for (int i = 0; i < attrNodes.getLength(); i++) {
            Element attrNode = (Element) attrNodes.item(i);
            XSDAttributeMetadata attr = parseAttribute(attrNode);
            if (attr != null) {
                attributes.add(attr);
            }
        }
        
        // Check for mixed content
        if (complexType.hasAttribute("mixed") && "true".equals(complexType.getAttribute("mixed"))) {
            mixedContent = true;
            allowsBody = true;
        }
        
        // Extract child elements (from sequence, choice, group, etc.)
        extractChildElements(complexType, childElements);
        
        // If has child elements, allows body
        if (!childElements.isEmpty()) {
            allowsBody = true;
        }
    }
    
    /**
     * Extracts child element names from sequence, choice, group, etc.
     */
    private static void extractChildElements(Element parent, List<String> childElements) {
        // Check for sequence
        NodeList sequences = parent.getElementsByTagNameNS(XS_NAMESPACE, "sequence");
        for (int i = 0; i < sequences.getLength(); i++) {
            extractElementsFromContainer((Element) sequences.item(i), childElements);
        }
        
        // Check for choice
        NodeList choices = parent.getElementsByTagNameNS(XS_NAMESPACE, "choice");
        for (int i = 0; i < choices.getLength(); i++) {
            extractElementsFromContainer((Element) choices.item(i), childElements);
        }
        
        // Check for group reference
        NodeList groups = parent.getElementsByTagNameNS(XS_NAMESPACE, "group");
        for (int i = 0; i < groups.getLength(); i++) {
            Element group = (Element) groups.item(i);
            String ref = group.getAttribute("ref");
            if (ref != null && !ref.isEmpty()) {
                // Group reference - child elements determined by group definition
                // This is a simplified extraction
            }
        }
        
        // Direct element references
        extractElementsFromContainer(parent, childElements);
    }
    
    /**
     * Extracts element names from a container (sequence, choice, etc.).
     */
    private static void extractElementsFromContainer(Element container, List<String> childElements) {
        NodeList elementRefs = container.getElementsByTagNameNS(XS_NAMESPACE, "element");
        for (int i = 0; i < elementRefs.getLength(); i++) {
            Element elemRef = (Element) elementRefs.item(i);
            String name = elemRef.getAttribute("name");
            String ref = elemRef.getAttribute("ref");
            
            if (name != null && !name.isEmpty() && !childElements.contains(name)) {
                childElements.add(name);
            } else if (ref != null && !ref.isEmpty() && !childElements.contains(ref)) {
                childElements.add(ref);
            }
        }
    }
    
    /**
     * Parses an attribute definition.
     */
    private static XSDAttributeMetadata parseAttribute(Element attrNode) {
        String name = attrNode.getAttribute("name");
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        String type = attrNode.getAttribute("type");
        String use = attrNode.getAttribute("use"); // "required", "optional", "prohibited"
        String defaultValue = attrNode.getAttribute("default");
        String documentation = extractDocumentation(attrNode);
        
        boolean required = "required".equals(use);
        
        return new XSDAttributeMetadata(name, type, required, defaultValue, documentation);
    }
    
    /**
     * Extracts documentation from xs:annotation/xs:documentation.
     */
    private static String extractDocumentation(Element element) {
        NodeList annotations = element.getElementsByTagNameNS(XS_NAMESPACE, "annotation");
        if (annotations.getLength() == 0) {
            return null;
        }
        
        Element annotation = (Element) annotations.item(0);
        NodeList docs = annotation.getElementsByTagNameNS(XS_NAMESPACE, "documentation");
        
        if (docs.getLength() > 0) {
            Element doc = (Element) docs.item(0);
            return getTextContent(doc).trim();
        }
        
        return null;
    }
    
    /**
     * Gets text content from an element, including all text nodes.
     */
    private static String getTextContent(Element element) {
        StringBuilder sb = new StringBuilder();
        NodeList children = element.getChildNodes();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                sb.append(child.getNodeValue());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Checks if element has mixed content (allows text).
     */
    private static boolean hasMixedContent(Element element) {
        NodeList complexTypes = element.getElementsByTagNameNS(XS_NAMESPACE, "complexType");
        for (int i = 0; i < complexTypes.getLength(); i++) {
            Element ct = (Element) complexTypes.item(i);
            if (ct.hasAttribute("mixed") && "true".equals(ct.getAttribute("mixed"))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if element has child elements (allows body).
     */
    private static boolean hasChildElements(Element element) {
        NodeList sequences = element.getElementsByTagNameNS(XS_NAMESPACE, "sequence");
        NodeList choices = element.getElementsByTagNameNS(XS_NAMESPACE, "choice");
        NodeList groups = element.getElementsByTagNameNS(XS_NAMESPACE, "group");
        NodeList elements = element.getElementsByTagNameNS(XS_NAMESPACE, "element");
        
        return sequences.getLength() > 0 || 
               choices.getLength() > 0 || 
               groups.getLength() > 0 || 
               elements.getLength() > 0;
    }
    
    /**
     * Metadata for an XSD element.
     */
    public static class XSDElementMetadata {
        private final String elementName;
        private final String documentation;
        private final List<XSDAttributeMetadata> attributes;
        private final List<String> childElements;
        private final boolean allowsBody;
        private final boolean mixedContent;
        
        public XSDElementMetadata(String elementName, String documentation,
                                   List<XSDAttributeMetadata> attributes,
                                   List<String> childElements,
                                   boolean allowsBody, boolean mixedContent) {
            this.elementName = elementName;
            this.documentation = documentation;
            this.attributes = attributes != null ? List.copyOf(attributes) : List.of();
            this.childElements = childElements != null ? List.copyOf(childElements) : List.of();
            this.allowsBody = allowsBody;
            this.mixedContent = mixedContent;
        }
        
        public String getElementName() {
            return elementName;
        }
        
        public String getDocumentation() {
            return documentation;
        }
        
        public List<XSDAttributeMetadata> getAttributes() {
            return attributes;
        }
        
        public List<String> getChildElements() {
            return childElements;
        }
        
        public boolean allowsBody() {
            return allowsBody;
        }
        
        public boolean isMixedContent() {
            return mixedContent;
        }
    }
    
    /**
     * Metadata for an XSD attribute.
     */
    public static class XSDAttributeMetadata {
        private final String name;
        private final String type;
        private final boolean required;
        private final String defaultValue;
        private final String documentation;
        
        public XSDAttributeMetadata(String name, String type, boolean required,
                                   String defaultValue, String documentation) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.documentation = documentation;
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public boolean isRequired() {
            return required;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public String getDocumentation() {
            return documentation;
        }
    }
}

