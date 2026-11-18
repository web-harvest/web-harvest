package org.webharvest.plugin.metadata;

import org.testng.annotations.Test;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDAttributeMetadata;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDElementMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Behavior tests for XSDMetadataParser using both generated and inline schemas.
 */
public class XSDMetadataParserTest {

    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String CORE_NAMESPACE = "http://org.webharvest/schema/2.1/core";

    private Map<String, XSDElementMetadata> parseGeneratedXSD(String namespace) {
        String xsdContent = XSDGenerator.generateXSD(namespace);
        assertNotNull(xsdContent, "XSDGenerator should generate schema");
        assertFalse(xsdContent.trim().isEmpty(), "Generated schema should not be empty");
        return parseXSD(xsdContent);
    }

    private Map<String, XSDElementMetadata> parseInlineSchema(String elementName, String elementContent) {
        String xsd = "<xs:schema xmlns:xs=\"" + XSD_NAMESPACE + "\">"
                + "<xs:element name=\"" + elementName + "\">"
                + elementContent
                + "</xs:element></xs:schema>";
        return parseXSD(xsd);
    }

    private Map<String, XSDElementMetadata> parseXSD(String xsd) {
        try (InputStream input = new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8))) {
            return XSDMetadataParser.parseXSD(input);
        } catch (IOException e) {
            throw new AssertionError("Could not close in-memory XSD input", e);
        }
    }

    @Test
    public void testParseGeneratedXSD() {
        Map<String, XSDElementMetadata> elements = parseGeneratedXSD(CORE_NAMESPACE);

        assertTrue(elements.containsKey("config"), "Generated schema must contain its config root");
        assertTrue(elements.containsKey("http"), "Generated Core schema must contain the HTTP element");
        assertTrue(elements.containsKey("loop"), "Generated Core schema must contain the Loop element");
        assertEquals(elements.get("http").getElementName(), "http");
    }

    @Test
    public void testParseHttpElement() {
        Map<String, XSDElementMetadata> elements = parseInlineSchema("http",
                "<xs:annotation><xs:documentation>HTTP request</xs:documentation></xs:annotation>"
                + "<xs:complexType><xs:attribute name=\"url\" type=\"xs:anyURI\" use=\"required\"/>"
                + "</xs:complexType>");

        XSDElementMetadata http = elements.get("http");
        assertNotNull(http, "Should parse the HTTP element");
        assertEquals(http.getDocumentation(), "HTTP request");
        assertEquals(http.getAttributes().size(), 1);
        assertEquals(http.getAttributes().get(0).getName(), "url");
        assertTrue(http.getAttributes().get(0).isRequired());
    }

    @Test
    public void testParseFileElement() {
        Map<String, XSDElementMetadata> elements = parseInlineSchema("file",
                "<xs:complexType>"
                + "<xs:attribute name=\"path\" type=\"xs:string\" use=\"required\"/>"
                + "<xs:attribute name=\"encoding\" type=\"xs:string\" default=\"UTF-8\"/>"
                + "</xs:complexType>");

        XSDElementMetadata file = elements.get("file");
        assertNotNull(file, "Should parse the File element");
        assertEquals(file.getAttributes().size(), 2);

        XSDAttributeMetadata path = file.getAttributes().stream()
                .filter(attribute -> "path".equals(attribute.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(path, "File schema must expose the path attribute");
        assertTrue(path.isRequired(), "File path must be required");

        XSDAttributeMetadata encoding = file.getAttributes().stream()
                .filter(attribute -> "encoding".equals(attribute.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(encoding, "File schema must expose the encoding attribute");
        assertFalse(encoding.isRequired(), "Encoding is optional");
        assertEquals(encoding.getDefaultValue(), "UTF-8");
    }

    @Test
    public void testParseTryElement() {
        Map<String, XSDElementMetadata> elements = parseInlineSchema("try",
                "<xs:complexType><xs:sequence>"
                + "<xs:element name=\"catch\" type=\"xs:string\" minOccurs=\"0\"/>"
                + "</xs:sequence></xs:complexType>");

        XSDElementMetadata tryElement = elements.get("try");
        assertNotNull(tryElement, "Should parse the Try element");
        assertTrue(tryElement.allowsBody(), "A sequence of child elements allows a body");
        assertTrue(tryElement.getChildElements().contains("catch"),
                "The parser must expose the declared Catch child");
    }

    @Test
    public void testParseLoopElement() {
        Map<String, XSDElementMetadata> elements = parseInlineSchema("loop",
                "<xs:complexType mixed=\"true\"><xs:sequence>"
                + "<xs:element name=\"item\" type=\"xs:string\" minOccurs=\"0\" maxOccurs=\"unbounded\"/>"
                + "</xs:sequence></xs:complexType>");

        XSDElementMetadata loop = elements.get("loop");
        assertNotNull(loop, "Should parse the Loop element");
        assertTrue(loop.allowsBody(), "A mixed complex type with child elements allows a body");
        assertTrue(loop.isMixedContent(), "The mixed-content declaration must be preserved");
        assertTrue(loop.getChildElements().contains("item"),
                "The parser must expose the declared item child");
    }

    @Test
    public void testAttributeMetadata() {
        Map<String, XSDElementMetadata> elements = parseInlineSchema("http",
                "<xs:complexType>"
                + "<xs:attribute name=\"url\" type=\"xs:anyURI\" use=\"required\">"
                + "<xs:annotation><xs:documentation>Target URL</xs:documentation></xs:annotation>"
                + "</xs:attribute>"
                + "<xs:attribute name=\"method\" type=\"xs:string\" default=\"GET\"/>"
                + "</xs:complexType>");

        XSDElementMetadata http = elements.get("http");
        assertNotNull(http);
        assertEquals(http.getAttributes().size(), 2);

        XSDAttributeMetadata url = http.getAttributes().stream()
                .filter(attribute -> "url".equals(attribute.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(url, "The URL attribute must be parsed");
        assertEquals(url.getType(), "xs:anyURI");
        assertTrue(url.isRequired());
        assertEquals(url.getDocumentation(), "Target URL");

        XSDAttributeMetadata method = http.getAttributes().stream()
                .filter(attribute -> "method".equals(attribute.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(method, "The method attribute must be parsed");
        assertFalse(method.isRequired());
        assertEquals(method.getDefaultValue(), "GET");
    }

    @Test
    public void testParseNonExistentXSD() {
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD("/non-existent.xsd");
        assertNotNull(elements);
        assertTrue(elements.isEmpty(), "A missing resource should produce an empty result");
    }

    @Test
    public void testParseLegacyNamespaceXSD() {
        Map<String, XSDElementMetadata> elements =
                parseGeneratedXSD("http://org.webharvest/schema/1.0/config");

        assertTrue(elements.containsKey("config"),
                "The generator must retain the configuration root for the legacy namespace");
    }
}
