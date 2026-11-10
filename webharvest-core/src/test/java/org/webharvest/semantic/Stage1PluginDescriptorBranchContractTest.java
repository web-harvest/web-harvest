package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.testng.annotations.Test;
import org.webharvest.plugin.metadata.PluginDescriptor;
import org.webharvest.semantic.Stage1PluginDescriptor.AttributeDefinition;

/**
 * Public catalog conversion and builder edges on {@link Stage1PluginDescriptor}.
 */
public class Stage1PluginDescriptorBranchContractTest {

    private static PluginDescriptor catalog(Map<String, Object> extensions) {
        return new PluginDescriptor(
                "sample",
                "sample",
                "Sample",
                "test",
                "com.example.Sample",
                "demo",
                "",
                null,
                null,
                null,
                List.of(),
                extensions);
    }

    @Test
    public void fromCatalogWithoutSchemaMarksUnreliableAndEmptyLists() {
        Stage1PluginDescriptor descriptor = Stage1PluginDescriptor.fromCatalog(catalog(Map.of()));
        assertFalse(descriptor.hasReliableSchema());
        assertTrue(descriptor.getRequiredAttributes().isEmpty());
        assertTrue(descriptor.getAllowsBody().isEmpty());
        assertTrue(descriptor.getCanAppearAtRoot().isEmpty());
    }

    @Test
    public void fromCatalogParsesSchemaFlagsAndAttributeDefinitions() {
        Map<String, Object> attr = new HashMap<>();
        attr.put("name", "limit");
        attr.put("type", "INTEGER");
        attr.put("required", "true");
        attr.put("allowedValues", List.of("1", "2"));
        attr.put("minValue", 1);
        attr.put("maxValue", 5);
        attr.put("pattern", "");

        Map<String, Object> schema = new HashMap<>();
        schema.put("requiredAttributes", List.of("url"));
        schema.put("optionalAttributes", List.of("id"));
        schema.put("allowedSubElements", List.of("child"));
        schema.put("requiredSubElements", List.of("body"));
        schema.put("requiredParents", List.of("config"));
        schema.put("allowsBody", true);
        schema.put("canAppearAtRoot", false);
        schema.put("attributeDefinitions", List.of(attr));

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("namespace", "http://example/ns");
        extensions.put("schema", schema);

        Stage1PluginDescriptor descriptor = Stage1PluginDescriptor.fromCatalog(catalog(extensions));
        assertTrue(descriptor.hasReliableSchema());
        assertEquals(descriptor.getNamespaceUri(), "http://example/ns");
        assertEquals(descriptor.getRequiredAttributes(), List.of("url"));
        assertEquals(descriptor.getOptionalAttributes(), List.of("id"));
        assertEquals(descriptor.getAllowedSubElements(), List.of("child"));
        assertEquals(descriptor.getRequiredSubElements(), List.of("body"));
        assertEquals(descriptor.getRequiredParents(), List.of("config"));
        assertEquals(descriptor.getAllowsBody(), Optional.of(Boolean.TRUE));
        assertEquals(descriptor.getCanAppearAtRoot(), Optional.of(Boolean.FALSE));

        AttributeDefinition limit = descriptor.getAttributeDefinitions().get(0);
        assertEquals(limit.getName(), "limit");
        assertTrue(limit.isRequired());
        assertEquals(limit.getAllowedValues(), List.of("1", "2"));
        assertEquals(limit.getMinValue().orElseThrow().intValue(), 1);
        assertEquals(limit.getMaxValue().orElseThrow().intValue(), 5);
        assertFalse(limit.getPattern().isPresent());

        assertTrue(descriptor.getKnownAttributeNames().containsAll(List.of("url", "id", "limit")));
    }

    @Test
    public void equalsHashCodeAndToStringUseIdentityQuadruple() {
        Stage1PluginDescriptor left = Stage1PluginDescriptor.builder()
                .id("http")
                .namespaceUri("http://ns")
                .localName("http")
                .className("com.example.Http")
                .build();
        Stage1PluginDescriptor right = Stage1PluginDescriptor.builder()
                .id("http")
                .namespaceUri("http://ns")
                .localName("http")
                .className("com.example.Http")
                .build();
        Stage1PluginDescriptor differentClass = Stage1PluginDescriptor.builder()
                .id("http")
                .namespaceUri("http://ns")
                .localName("http")
                .className("com.other.Http")
                .build();

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertFalse(left.equals(differentClass));
        assertTrue(left.toString().contains("http"));
    }
}
