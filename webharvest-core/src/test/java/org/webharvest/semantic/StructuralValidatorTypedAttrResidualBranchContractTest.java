package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.webharvest.semantic.Stage1PluginDescriptor.AttributeDefinition;

/**
 * Residual StructuralValidator contracts for non-numeric attribute types,
 * open attribute sets with namespaced attributes, and one-sided numeric bounds.
 */
public class StructuralValidatorTypedAttrResidualBranchContractTest {

    private static final String NS = "http://example.org/struct-typed-residual";
    private static final String NS_OTHER = "http://example.org/other";

    @Test
    public void enumUrlFilePathAndRegexTypesAcceptOpaqueValuesWithoutNumericChecks() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("opaque@" + NS)
                .namespaceUri(NS)
                .localName("opaque")
                .className("org.example.Opaque")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("mode", "ENUM", false, List.of(), null, null, null),
                        new AttributeDefinition("href", " url ", false, List.of(), null, null, null),
                        new AttributeDefinition("path", "FILE_PATH", false, List.of(), null, null, null),
                        new AttributeDefinition("expr", "REGEX", false, List.of(), null, null, null),
                        new AttributeDefinition("custom", "CUSTOM_TYPE", false, List.of(), null, null, null)))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult result = model.validateStructural(load(model,
                "<opaque xmlns=\"" + NS + "\" mode=\"maybe\" href=\"not-a-url\" path=\"***\""
                        + " expr=\"[unterminated\" custom=\"anything\"/>"));
        assertTrue(result.isSuccessful(), diagnosticText(result));
    }

    @Test
    public void enumAllowedValuesStillRejectUnknownTokens() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("choice@" + NS)
                .namespaceUri(NS)
                .localName("choice")
                .className("org.example.Choice")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("mode", "ENUM", false, List.of("a", "b"), null, null, null)))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult ok = model.validateStructural(load(model,
                "<choice xmlns=\"" + NS + "\" mode=\"a\"/>"));
        assertTrue(ok.isSuccessful(), diagnosticText(ok));

        StructuralValidationResult bad = model.validateStructural(load(model,
                "<choice xmlns=\"" + NS + "\" mode=\"c\"/>"));
        assertTrue(codes(bad).contains("INVALID_ATTRIBUTE_VALUE"), diagnosticText(bad));
        assertTrue(messages(bad).stream().anyMatch(m -> m.contains("allowedValues")));
    }

    @Test
    public void namespacedAttributeIgnoredWhenNoClosedAttributeSetExists() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("open@" + NS)
                .namespaceUri(NS)
                .localName("open")
                .className("org.example.Open")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of())
                .requiredAttributes(List.of())
                .optionalAttributes(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult result = model.validateStructural(load(model,
                "<open xmlns=\"" + NS + "\" xmlns:x=\"" + NS_OTHER + "\" x:extra=\"1\"/>"));
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"), diagnosticText(result));
        assertTrue(codes(result).contains("METADATA_LIMITATION")
                        || result.getDiagnostics().stream().anyMatch(d ->
                        d.getMessage().contains("unknown-attribute check skipped")),
                diagnosticText(result));
    }

    @Test
    public void minOnlyAndMaxOnlyBoundsRejectOutsideRangeIndependently() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("bounds@" + NS)
                .namespaceUri(NS)
                .localName("bounds")
                .className("org.example.Bounds")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("floor", "STRING", false, List.of(),
                                Integer.valueOf(5), null, null),
                        new AttributeDefinition("ceiling", "STRING", false, List.of(),
                                null, Integer.valueOf(10), null)))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult ok = model.validateStructural(load(model,
                "<bounds xmlns=\"" + NS + "\" floor=\"5\" ceiling=\"10\"/>"));
        assertTrue(ok.isSuccessful(), diagnosticText(ok));

        StructuralValidationResult below = model.validateStructural(load(model,
                "<bounds xmlns=\"" + NS + "\" floor=\"4\"/>"));
        assertTrue(codes(below).contains("INVALID_ATTRIBUTE_VALUE"), diagnosticText(below));
        assertTrue(messages(below).stream().anyMatch(m -> m.contains("below minValue")));

        StructuralValidationResult above = model.validateStructural(load(model,
                "<bounds xmlns=\"" + NS + "\" ceiling=\"11\"/>"));
        assertTrue(codes(above).contains("INVALID_ATTRIBUTE_VALUE"), diagnosticText(above));
        assertTrue(messages(above).stream().anyMatch(m -> m.contains("above maxValue")));
    }

    private static PluginDescriptorIndex indexOf(Stage1PluginDescriptor... descriptors) {
        return PluginDescriptorIndex.builder()
                .descriptors(List.of(descriptors))
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();
    }

    private static SemanticDocument load(SemanticModel model, String xml) {
        SemanticLoadResult result = model.load(xml, new SourceOrigin("test:struct-typed-residual"));
        assertTrue(result.isSuccessful(), result.getDiagnostics().toString());
        return result.getDocument().get();
    }

    private static List<String> codes(StructuralValidationResult result) {
        return result.getDiagnostics().stream()
                .map(SemanticDiagnostic::getCode)
                .collect(Collectors.toList());
    }

    private static List<String> messages(StructuralValidationResult result) {
        return result.getDiagnostics().stream()
                .map(SemanticDiagnostic::getMessage)
                .collect(Collectors.toList());
    }

    private static String diagnosticText(StructuralValidationResult result) {
        return result.getDiagnostics().toString();
    }
}
