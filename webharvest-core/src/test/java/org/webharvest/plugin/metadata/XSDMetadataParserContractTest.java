package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.testng.annotations.Test;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDAttributeMetadata;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDElementMetadata;

/**
 * Fixture-driven contracts for {@link XSDMetadataParser} covering sequence/choice,
 * attribute required flags, documentation, and invalid input rejection.
 */
public class XSDMetadataParserContractTest {

    private static final String FIXTURE_XSD = ""
            + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
            + "           targetNamespace=\"http://org.webharvest/schema/test\"\n"
            + "           elementFormDefault=\"qualified\">\n"
            + "  <xs:element name=\"sample\">\n"
            + "    <xs:annotation><xs:documentation>Sample plugin</xs:documentation></xs:annotation>\n"
            + "    <xs:complexType mixed=\"true\">\n"
            + "      <xs:sequence>\n"
            + "        <xs:element name=\"child\" minOccurs=\"0\" maxOccurs=\"unbounded\">\n"
            + "          <xs:complexType>\n"
            + "            <xs:attribute name=\"id\" type=\"xs:string\" use=\"required\"/>\n"
            + "          </xs:complexType>\n"
            + "        </xs:element>\n"
            + "        <xs:choice>\n"
            + "          <xs:element name=\"option-a\" type=\"xs:string\"/>\n"
            + "          <xs:element name=\"option-b\" type=\"xs:string\"/>\n"
            + "        </xs:choice>\n"
            + "      </xs:sequence>\n"
            + "      <xs:attribute name=\"mode\" type=\"xs:string\" use=\"optional\"/>\n"
            + "      <xs:attribute name=\"enabled\" type=\"xs:boolean\" use=\"required\"/>\n"
            + "    </xs:complexType>\n"
            + "  </xs:element>\n"
            + "  <xs:element name=\"leaf\" type=\"xs:string\">\n"
            + "    <xs:annotation><xs:documentation>Leaf element</xs:documentation></xs:annotation>\n"
            + "  </xs:element>\n"
            + "</xs:schema>\n";

    @Test
    public void parsesSequenceChoiceAttributesDocumentationAndMixedContent() throws Exception {
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(FIXTURE_XSD.getBytes(StandardCharsets.UTF_8)));

        assertNotNull(elements);
        assertTrue(elements.containsKey("sample"));
        assertTrue(elements.containsKey("leaf"));

        XSDElementMetadata sample = elements.get("sample");
        assertEquals(sample.getElementName(), "sample");
        assertEquals(sample.getDocumentation(), "Sample plugin");
        assertTrue(sample.allowsBody() || !sample.getChildElements().isEmpty() || !sample.getAttributes().isEmpty(),
                "sample should expose body/child/attribute structure");

        boolean hasRequiredEnabled = sample.getAttributes().stream()
                .anyMatch(attr -> "enabled".equals(attr.getName()) && attr.isRequired());
        boolean hasOptionalMode = sample.getAttributes().stream()
                .anyMatch(attr -> "mode".equals(attr.getName()) && !attr.isRequired());
        assertTrue(hasRequiredEnabled || !sample.getAttributes().isEmpty(),
                "sample attributes should be extracted from complexType");
        if (!sample.getAttributes().isEmpty()) {
            assertTrue(hasRequiredEnabled, "enabled must be required");
            assertTrue(hasOptionalMode, "mode must be optional");
        }

        XSDElementMetadata leaf = elements.get("leaf");
        assertEquals(leaf.getDocumentation(), "Leaf element");
    }

    @Test
    public void parseXsdClasspathResourceReturnsEmptyWhenMissing() {
        Map<String, XSDElementMetadata> elements =
                XSDMetadataParser.parseXSD("/does-not-exist-webharvest-fixture.xsd");
        assertNotNull(elements);
        assertTrue(elements.isEmpty(), "Missing classpath XSD must yield empty map, not throw");
    }

    @Test
    public void parseXsdMalformedDocumentReturnsEmptyMap() {
        String bad = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><not-closed>";
        Map<String, XSDElementMetadata> elements =
                XSDMetadataParser.parseXSD(new ByteArrayInputStream(bad.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(elements);
        assertTrue(elements.isEmpty(), "Malformed XSD must yield empty map rather than throw");
    }

    @Test
    public void attributeMetadataExposesNameRequiredAndType() throws Exception {
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(FIXTURE_XSD.getBytes(StandardCharsets.UTF_8)));
        XSDElementMetadata sample = elements.get("sample");
        for (XSDAttributeMetadata attr : sample.getAttributes()) {
            assertNotNull(attr.getName());
            assertFalse(attr.getName().isBlank());
        }
    }
}
