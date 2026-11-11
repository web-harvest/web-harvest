package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDAttributeMetadata;
import org.webharvest.plugin.metadata.XSDMetadataParser.XSDElementMetadata;

/**
 * Residual XSD parser contracts: rejected roots, nameless skips, group-only body,
 * annotation gaps, classpath success, and null-safe metadata constructors.
 */
public class XSDMetadataParserResidualBranchContractTest {

    @Test
    public void nonSchemaRootYieldsEmptyMap() {
        String xml = "<?xml version=\"1.0\"?><xs:element xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" name=\"orphan\"/>";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(elements);
        assertTrue(elements.isEmpty());
    }

    @Test
    public void namelessTopLevelElementsAreSkippedWhileNamedOnesRemain() {
        String xsd = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:element ref=\"ghost\"/>\n"
                + "  <xs:element name=\"visible\" type=\"xs:string\">\n"
                + "    <xs:annotation/>\n"
                + "  </xs:element>\n"
                + "</xs:schema>\n";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        assertEquals(elements.size(), 1);
        assertTrue(elements.containsKey("visible"));
        assertNull(elements.get("visible").getDocumentation());
    }

    @Test
    public void groupOnlyComplexTypeMarksBodyAllowedWithoutResolvingGroupChildren() {
        String xsd = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:group name=\"shared\">\n"
                + "    <xs:sequence>\n"
                + "      <xs:element name=\"shared-child\" type=\"xs:string\"/>\n"
                + "    </xs:sequence>\n"
                + "  </xs:group>\n"
                + "  <xs:element name=\"holder\">\n"
                + "    <xs:complexType mixed=\"false\">\n"
                + "      <xs:group ref=\"shared\"/>\n"
                + "      <xs:attribute name=\"id\" type=\"xs:string\" use=\"optional\"/>\n"
                + "    </xs:complexType>\n"
                + "  </xs:element>\n"
                + "</xs:schema>\n";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        XSDElementMetadata holder = elements.get("holder");
        assertNotNull(holder);
        assertTrue(holder.allowsBody(),
                "group reference is treated as body/children capability even without inline element names");
        assertFalse(holder.isMixedContent());
        assertTrue(holder.getAttributes().stream().anyMatch(a -> "id".equals(a.getName()) && !a.isRequired()));
    }

    @Test
    public void sequenceDedupesDuplicateChildNamesAndIgnoresNamelessElementSlots() {
        String xsd = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:element name=\"holder\">\n"
                + "    <xs:complexType>\n"
                + "      <xs:sequence>\n"
                + "        <xs:element name=\"child\" type=\"xs:string\"/>\n"
                + "        <xs:element name=\"child\" type=\"xs:string\"/>\n"
                + "        <xs:element ref=\"alias\"/>\n"
                + "        <xs:element/>\n"
                + "      </xs:sequence>\n"
                + "    </xs:complexType>\n"
                + "  </xs:element>\n"
                + "  <xs:element name=\"alias\" type=\"xs:string\"/>\n"
                + "</xs:schema>\n";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        XSDElementMetadata holder = elements.get("holder");
        assertNotNull(holder);
        long childCount = holder.getChildElements().stream().filter("child"::equals).count();
        assertEquals(childCount, 1, "duplicate child names must be deduped: " + holder.getChildElements());
        assertTrue(holder.getChildElements().contains("alias"));
        assertFalse(holder.getChildElements().stream().anyMatch(name -> name == null || name.isEmpty()));
    }

    @Test
    public void documentationWithOnlyElementChildrenYieldsEmptyTrimmedText() {
        String xsd = ""
                + "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "  <xs:element name=\"docish\">\n"
                + "    <xs:annotation>\n"
                + "      <xs:documentation><span xmlns=\"http://www.w3.org/1999/xhtml\">ignored</span></xs:documentation>\n"
                + "    </xs:annotation>\n"
                + "    <xs:complexType/>\n"
                + "  </xs:element>\n"
                + "</xs:schema>\n";
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));
        assertEquals(elements.get("docish").getDocumentation(), "");
    }

    @Test
    public void classpathMockSchemaResourceParsesToEmptyMapSuccessfully() {
        Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD("/mockSchema.xsd");
        assertNotNull(elements);
        assertTrue(elements.isEmpty(), "empty schema fixture must succeed with empty element map");
    }

    @Test
    public void elementMetadataConstructorAcceptsNullAttributeAndChildLists() {
        XSDElementMetadata metadata = new XSDElementMetadata(
                "anon", "docs", null, null, true, false);
        assertEquals(metadata.getElementName(), "anon");
        assertEquals(metadata.getDocumentation(), "docs");
        assertEquals(metadata.getAttributes(), List.of());
        assertEquals(metadata.getChildElements(), List.of());
        assertTrue(metadata.allowsBody());
        assertFalse(metadata.isMixedContent());
    }


    @Test
    public void externalEntitiesAreNeverExpandedFromLocalFiles() throws Exception {
        Path secret = Files.createTempFile("webharvest-xsd-xxe-", ".txt");
        String marker = "WEBHARVEST_XXE_MUST_NOT_BE_EXPOSED";
        try {
            Files.write(secret, marker.getBytes(StandardCharsets.UTF_8));
            String xsd = ""
                    + "<!DOCTYPE xs:schema [<!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">]>"
                    + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                    + "  <xs:element name=\"unsafe\">"
                    + "    <xs:annotation><xs:documentation>&xxe;</xs:documentation></xs:annotation>"
                    + "  </xs:element>"
                    + "</xs:schema>";

            Map<String, XSDElementMetadata> elements = XSDMetadataParser.parseXSD(
                    new ByteArrayInputStream(xsd.getBytes(StandardCharsets.UTF_8)));

            String documentation = elements.containsKey("unsafe")
                    ? elements.get("unsafe").getDocumentation() : null;
            assertFalse(marker.equals(documentation),
                    "XSD parsing must not disclose content from an external entity");
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    public void attributeMetadataExposesDefaultsAndDocumentation() {
        XSDAttributeMetadata attr = new XSDAttributeMetadata(
                "mode", "xs:string", false, "auto", "Mode docs");
        assertEquals(attr.getName(), "mode");
        assertEquals(attr.getType(), "xs:string");
        assertFalse(attr.isRequired());
        assertEquals(attr.getDefaultValue(), "auto");
        assertEquals(attr.getDocumentation(), "Mode docs");
    }
}
