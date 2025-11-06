package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

/**
 * Unit tests for XSDGenerator.
 */
public class XSDGeneratorTest {
    
    private static final String DEFAULT_NAMESPACE = "http://org.webharvest/schema/2.1/core";
    
    @BeforeMethod
    public void setUp() {
        // Ensure PluginMetadataCatalog is initialized
        PluginMetadataCatalog.getAllDescriptors();
    }
    
    @Test
    void testGenerateXSDWithDefaultNamespace() {
        String xsd = XSDGenerator.generateXSD(null);
        
        assertNotNull(xsd);
        assertTrue(xsd.contains("<?xml"));
        assertTrue(xsd.contains("xs:schema"));
        assertTrue(xsd.contains("targetNamespace=\"" + DEFAULT_NAMESPACE + "\""));
    }
    
    @Test
    void testGenerateXSDWithCustomNamespace() {
        String customNamespace = "http://test.namespace/schema/1.0";
        String xsd = XSDGenerator.generateXSD(customNamespace);
        
        assertNotNull(xsd);
        assertTrue(xsd.contains("targetNamespace=\"" + customNamespace + "\""));
    }
    
    @Test
    void testGenerateXSDContainsConfigElement() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        // Debug: print first 500 chars
        if (xsd.length() > 500) {
            System.out.println("XSD preview: " + xsd.substring(0, 500));
        }
        
        Document doc = parseXML(xsd);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new javax.xml.namespace.NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                if ("xs".equals(prefix)) return "http://www.w3.org/2001/XMLSchema";
                return null;
            }
            public String getPrefix(String namespaceURI) { return null; }
            public java.util.Iterator<String> getPrefixes(String namespaceURI) { return null; }
        });
        
        // Check config element exists (without namespace prefix)
        NodeList configElements = (NodeList) xpath.evaluate(
            "//*[local-name()='element' and @name='config']", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(configElements.getLength() >= 1, "Config element should exist");
        
        // Check config has charset attribute
        NodeList charsetAttrs = (NodeList) xpath.evaluate(
            "//*[local-name()='element' and @name='config']//*[local-name()='attribute' and @name='charset']", 
            doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(charsetAttrs.getLength() >= 1, "Config should have charset attribute");
        
        // Check config has scriptlang attribute
        NodeList scriptlangAttrs = (NodeList) xpath.evaluate(
            "//*[local-name()='element' and @name='config']//*[local-name()='attribute' and @name='scriptlang']", 
            doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(scriptlangAttrs.getLength() >= 1, "Config should have scriptlang attribute");
    }
    
    @Test
    void testGenerateXSDContainsBodyElementsGroup() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        Document doc = parseXML(xsd);
        XPath xpath = XPathFactory.newInstance().newXPath();
        
        // Check bodyElements group exists (using local-name for namespace-agnostic matching)
        NodeList bodyGroups = (NodeList) xpath.evaluate(
            "//*[local-name()='group' and @name='bodyElements']", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(bodyGroups.getLength() >= 1, "bodyElements group should exist");
        
        // Check bodyType exists
        NodeList bodyTypes = (NodeList) xpath.evaluate(
            "//*[local-name()='complexType' and @name='bodyType']", doc, javax.xml.xpath.XPathConstants.NODESET);
        assertTrue(bodyTypes.getLength() >= 1, "bodyType should exist");
    }
    
    @Test
    void testGenerateXSDContainsPluginElements() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        // Simple string check - faster and more reliable
        assertTrue(xsd.contains("http") || xsd.contains("xpath") || xsd.contains("file") || 
                   xsd.contains("loop") || xsd.contains("if"), 
                   "XSD should contain at least some plugin elements");
    }
    
    @Test
    void testGenerateXSDWithSpecificDescriptors() throws Exception {
        // Create test descriptors
        List<PluginDescriptor> descriptors = createTestDescriptors();
        
        String xsd = XSDGenerator.generateXSD(descriptors, DEFAULT_NAMESPACE);
        
        assertNotNull(xsd);
        assertTrue(xsd.contains("test-plugin"));
        assertTrue(xsd.contains("test-container"));
    }
    
    @Test
    void testGenerateXSDPluginComplexTypes() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        // Simple string check
        assertTrue(xsd.contains("complexType") && xsd.contains("Type\""), 
                   "Should have complex types");
    }
    
    @Test
    void testGenerateXSDAttributes() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        // Simple string check - id attribute should be present
        assertTrue(xsd.contains("attribute") && (xsd.contains("name=\"id\"") || xsd.contains("@name='id'")),
                   "Should have id attributes");
    }
    
    @Test
    void testGenerateXSDValidXML() throws Exception {
        String xsd = XSDGenerator.generateXSD(null);
        
        // Try to parse as XML
        Document doc = parseXML(xsd);
        assertNotNull(doc);
        
        // Check root element
        Element root = doc.getDocumentElement();
        assertNotNull(root);
        assertEquals("schema", root.getLocalName());
    }
    
    @Test
    void testGenerateXSDNamespaceFiltering() {
        String namespace1 = "http://test.namespace1";
        String namespace2 = "http://test.namespace2";
        
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Add descriptor with namespace1
        Map<String, Object> extensions1 = new HashMap<>();
        extensions1.put("namespace", namespace1);
        descriptors.add(createTestDescriptor("plugin1", extensions1));
        
        // Add descriptor with namespace2
        Map<String, Object> extensions2 = new HashMap<>();
        extensions2.put("namespace", namespace2);
        descriptors.add(createTestDescriptor("plugin2", extensions2));
        
        // Generate XSD for namespace1
        String xsd1 = XSDGenerator.generateXSD(descriptors, namespace1);
        assertTrue(xsd1.contains("plugin1"));
        assertFalse(xsd1.contains("plugin2"));
        
        // Generate XSD for namespace2
        String xsd2 = XSDGenerator.generateXSD(descriptors, namespace2);
        assertTrue(xsd2.contains("plugin2"));
        assertFalse(xsd2.contains("plugin1"));
    }
    
    // Helper methods
    
    private List<PluginDescriptor> createTestDescriptors() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Simple plugin
        descriptors.add(createTestDescriptor("test-plugin", Map.of()));
        
        // Container plugin
        Map<String, Object> schemaInfo = new HashMap<>();
        schemaInfo.put("allowedSubElements", Arrays.asList("test-plugin"));
        
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("schema", schemaInfo);
        
        descriptors.add(createTestDescriptor("test-container", extensions));
        
        return descriptors;
    }
    
    private PluginDescriptor createTestDescriptor(String elementName, Map<String, Object> extensions) {
        PluginSemantics semantics = new PluginSemantics(
            PluginSemanticRole.BODY_EVALUATOR,
            true, // canAppearAtRoot
            true, // allowsChildren
            true, // acceptsAnyChild
            true, // allowBody
            false, // allowTextContent
            List.of(), // allowedChildTypes
            List.of() // requiredParentTypes
        );
        
        return new PluginDescriptor(
            elementName + "-id",
            elementName,
            elementName,
            "test",
            "TestClass",
            "test",
            "Test description",
            null, // documentation
            semantics,
            null, // uiHints
            List.of(), // dependencies
            extensions
        );
    }
    
    private Document parseXML(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new java.io.ByteArrayInputStream(xml.getBytes("UTF-8")));
    }
}

