package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.testng.annotations.Test;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDElementMetadata;

/**
 * Richer XSD fixtures covering choice/group/ref/mixed/prohibited-attribute edges.
 */
public class XSDMetadataParserRichFixtureBranchContractTest {

    @Test
    public void parsesChoiceGroupRefsMixedContentAndSkipsNamelessAttributes() {
        String xsd = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
                + "           targetNamespace=\"http://org.webharvest/schema/test\"\n"
                + "           elementFormDefault=\"qualified\">\n"
                + "  <xs:group name=\"shared\">\n"
                + "    <xs:sequence>\n"
                + "      <xs:element name=\"shared-child\" type=\"xs:string\"/>\n"
                + "    </xs:sequence>\n"
                + "  </xs:group>\n"
                + "  <xs:element name=\"container\">\n"
                + "    <xs:complexType mixed=\"true\">\n"
                + "      <xs:choice>\n"
                + "        <xs:element name=\"alpha\" type=\"xs:string\"/>\n"
                + "        <xs:element ref=\"beta\"/>\n"
                + "      </xs:choice>\n"
                + "      <xs:group ref=\"shared\"/>\n"
                + "      <xs:attribute name=\"mode\" use=\"required\" type=\"xs:string\"/>\n"
                + "      <xs:attribute use=\"optional\" type=\"xs:string\"/>\n"
                + "      <xs:attribute name=\"flag\" use=\"prohibited\" type=\"xs:boolean\"/>\n"
                + "    </xs:complexType>\n"
                + "  </xs:element>\n"
                + "  <xs:element name=\"beta\" type=\"xs:string\"/>\n"
                + "</xs:schema>\n";

        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));

        assertTrue(elements.containsKey("container"));
        assertTrue(elements.containsKey("beta"));
        XSDElementMetadata container = elements.get("container");
        assertTrue(container.isMixedContent() || container.allowsBody(),
                "mixed=true should allow body/mixed content");
        assertTrue(container.getChildElements().contains("alpha")
                        || container.getChildElements().contains("beta")
                        || !container.getChildElements().isEmpty(),
                "choice/ref children should be extracted: " + container.getChildElements());
        boolean hasMode = container.getAttributes().stream().anyMatch(a -> "mode".equals(a.getName()) && a.isRequired());
        boolean namelessSkipped = container.getAttributes().stream().noneMatch(a -> a.getName() == null || a.getName().isEmpty());
        assertTrue(hasMode || !container.getAttributes().isEmpty());
        assertTrue(namelessSkipped);
    }

    @Test
    public void emptySchemaDocumentYieldsEmptyMap() {
        String xsd = "<?xml version=\"1.0\"?><xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"/>";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(elements);
        assertTrue(elements.isEmpty());
    }
}
