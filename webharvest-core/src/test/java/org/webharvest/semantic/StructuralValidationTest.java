package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.webharvest.semantic.Stage1PluginDescriptor.AttributeDefinition;

public class StructuralValidationTest {

    private static final String NS = "http://example.org/struct";
    private static final String NS_OTHER = "http://example.org/other";

    @Test
    public void inspectAndValidateStructuralShareSameDocumentInstance() {
        PluginDescriptorIndex index = indexOf(reliableRoot("config"));
        SemanticModel model = new SemanticModel(index);
        SemanticDocument document = load(model, "<config xmlns=\"" + NS + "\"/>");

        InspectionView inspection = model.inspect(document);
        StructuralValidationResult validation = model.validateStructural(document);

        assertSame(inspection.getDocument(), document);
        assertSame(validation.getDocument(), document);
        assertSame(inspection.getDocument(), validation.getDocument());
    }

    @Test
    public void unknownElementEmitsError() {
        PluginDescriptorIndex index = indexOf(reliableRoot("config"));
        SemanticModel model = new SemanticModel(index);
        SemanticDocument document = load(model,
                "<config xmlns=\"" + NS + "\"><nope/></config>");

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(result.isSuccessful());
        assertTrue(codes(result).contains("UNKNOWN_ELEMENT"));
        SemanticDiagnostic diagnostic = firstOf(result, "UNKNOWN_ELEMENT");
        assertEquals(diagnostic.getSeverity(), SemanticDiagnostic.Severity.ERROR);
        assertTrue(diagnostic.getMessage().contains("nope")
                || diagnostic.getMessage().contains("{}nope")
                || diagnostic.getMessage().contains("Unknown"));
    }

    @Test
    public void ambiguousElementViaSyntheticIndex() {
        Stage1PluginDescriptor first = Stage1PluginDescriptor.builder()
                .id("a").namespaceUri(NS).localName("dup").className("org.example.A")
                .reliableSchema(true).canAppearAtRoot(true).allowsBody(true).build();
        Stage1PluginDescriptor second = Stage1PluginDescriptor.builder()
                .id("b").namespaceUri(NS).localName("dup").className("org.example.B")
                .reliableSchema(true).canAppearAtRoot(true).allowsBody(true).build();
        PluginDescriptorIndex index = indexOf(first, second);
        SemanticModel model = new SemanticModel(index);
        SemanticDocument document = load(model, "<dup xmlns=\"" + NS + "\"/>");

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(result.isSuccessful());
        assertEquals(firstOf(result, "AMBIGUOUS_ELEMENT").getSeverity(),
                SemanticDiagnostic.Severity.ERROR);
    }

    @Test
    public void requiredAndUnknownAttributesWithReliableSchema() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("item@" + NS)
                .namespaceUri(NS)
                .localName("item")
                .className("org.example.Item")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .requiredAttributes(List.of("name"))
                .optionalAttributes(List.of("mode"))
                .attributeDefinitions(List.of(
                        new AttributeDefinition("name", "STRING", true, List.of(), null, null, null),
                        new AttributeDefinition("mode", "STRING", false, List.of("a", "b"), null, null, null)))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult missing = model.validateStructural(
                load(model, "<item xmlns=\"" + NS + "\" mode=\"a\"/>"));
        assertTrue(codes(missing).contains("REQUIRED_ATTRIBUTE_MISSING"));

        StructuralValidationResult unknown = model.validateStructural(
                load(model, "<item xmlns=\"" + NS + "\" name=\"x\" weird=\"1\"/>"));
        assertTrue(codes(unknown).contains("UNKNOWN_ATTRIBUTE"));

        StructuralValidationResult invalid = model.validateStructural(
                load(model, "<item xmlns=\"" + NS + "\" name=\"x\" mode=\"z\"/>"));
        assertTrue(codes(invalid).contains("INVALID_ATTRIBUTE_VALUE"));

        StructuralValidationResult ok = model.validateStructural(
                load(model, "<item xmlns=\"" + NS + "\" name=\"x\" mode=\"a\"/>"));
        assertTrue(ok.isSuccessful());
        assertEquals(ok.getErrorCount(), 0);
    }

    @Test
    public void allowedRequiredChildrenAndParentConstraints() {
        Stage1PluginDescriptor parent = Stage1PluginDescriptor.builder()
                .id("parent@" + NS)
                .namespaceUri(NS)
                .localName("parent")
                .className("org.example.Parent")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .allowedSubElements(List.of("child"))
                .requiredSubElements(List.of("child"))
                .build();
        Stage1PluginDescriptor child = Stage1PluginDescriptor.builder()
                .id("child@" + NS)
                .namespaceUri(NS)
                .localName("child")
                .className("org.example.Child")
                .reliableSchema(true)
                .canAppearAtRoot(false)
                .allowsBody(false)
                .requiredParents(List.of("parent"))
                .allowedSubElements(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(parent, child));

        StructuralValidationResult missingChild = model.validateStructural(
                load(model, "<parent xmlns=\"" + NS + "\"/>"));
        assertTrue(codes(missingChild).contains("REQUIRED_CHILD_MISSING"));

        StructuralValidationResult disallowedChild = model.validateStructural(
                load(model, "<parent xmlns=\"" + NS + "\"><child/><extra/></parent>"));
        assertTrue(codes(disallowedChild).contains("DISALLOWED_CHILD"));
        assertTrue(codes(disallowedChild).contains("UNKNOWN_ELEMENT"));

        StructuralValidationResult badParent = model.validateStructural(
                load(model, "<child xmlns=\"" + NS + "\"/>"));
        assertTrue(codes(badParent).contains("DISALLOWED_ROOT"));
        assertTrue(codes(badParent).contains("INVALID_PARENT"));

        StructuralValidationResult ok = model.validateStructural(
                load(model, "<parent xmlns=\"" + NS + "\"><child/></parent>"));
        assertTrue(ok.isSuccessful(), diagnosticText(ok));
    }

    @Test
    public void disallowedTextAndCdata() {
        Stage1PluginDescriptor leaf = Stage1PluginDescriptor.builder()
                .id("leaf@" + NS)
                .namespaceUri(NS)
                .localName("leaf")
                .className("org.example.Leaf")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .allowedSubElements(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(leaf));

        StructuralValidationResult text = model.validateStructural(
                load(model, "<leaf xmlns=\"" + NS + "\">body</leaf>"));
        assertTrue(codes(text).contains("DISALLOWED_TEXT"));

        StructuralValidationResult cdata = model.validateStructural(
                load(model, "<leaf xmlns=\"" + NS + "\"><![CDATA[x]]></leaf>"));
        assertTrue(codes(cdata).contains("DISALLOWED_TEXT"));

        StructuralValidationResult whitespaceOk = model.validateStructural(
                load(model, "<leaf xmlns=\"" + NS + "\">   </leaf>"));
        assertFalse(codes(whitespaceOk).contains("DISALLOWED_TEXT"));
    }

    @Test
    public void metadataLimitationWhenSchemaMissing() {
        Stage1PluginDescriptor bare = Stage1PluginDescriptor.builder()
                .id("bare@" + NS)
                .namespaceUri(NS)
                .localName("bare")
                .className("org.example.Bare")
                .reliableSchema(false)
                .build();
        SemanticModel model = new SemanticModel(indexOf(bare));
        StructuralValidationResult result = model.validateStructural(
                load(model, "<bare xmlns=\"" + NS + "\" weird=\"1\">text</bare>"));

        assertTrue(result.isSuccessful());
        assertTrue(codes(result).contains("METADATA_LIMITATION"));
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"));
        assertFalse(codes(result).contains("DISALLOWED_TEXT"));
        SemanticDiagnostic limitation = firstOf(result, "METADATA_LIMITATION");
        assertEquals(limitation.getSeverity(), SemanticDiagnostic.Severity.WARNING);
        assertTrue(limitation.getMessage().contains("reliable schema"));
    }

    @Test
    public void diagnosticsAreDeterministicAcrossTwoRuns() {
        Stage1PluginDescriptor config = Stage1PluginDescriptor.builder()
                .id("config@" + NS)
                .namespaceUri(NS)
                .localName("config")
                .className("org.example.Config")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .allowedSubElements(List.of("step"))
                .requiredSubElements(List.of("step"))
                .requiredAttributes(List.of("id"))
                .optionalAttributes(List.of())
                .build();
        Stage1PluginDescriptor step = Stage1PluginDescriptor.builder()
                .id("step@" + NS)
                .namespaceUri(NS)
                .localName("step")
                .className("org.example.Step")
                .reliableSchema(true)
                .canAppearAtRoot(false)
                .allowsBody(false)
                .requiredParents(List.of("config"))
                .allowedSubElements(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(config, step));
        SemanticDocument document = load(model,
                "<config xmlns=\"" + NS + "\"><unknown/></config>");

        StructuralValidationResult first = model.validateStructural(document);
        StructuralValidationResult second = model.validateStructural(document);

        assertEquals(codes(first), codes(second));
        assertEquals(messages(first), messages(second));
        assertEquals(severities(first), severities(second));
        assertEquals(first.getErrorCount(), second.getErrorCount());
        assertEquals(first.getWarningCount(), second.getWarningCount());
    }

    @Test
    public void namespaceMismatchEmitsUnknownPlusWarning() {
        Stage1PluginDescriptor inNs = reliableRoot("loop");
        PluginDescriptorIndex index = indexOf(inNs);
        SemanticModel model = new SemanticModel(index);
        SemanticDocument document = load(model, "<loop xmlns=\"" + NS_OTHER + "\"/>");

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(result.isSuccessful());
        assertTrue(codes(result).contains("UNKNOWN_ELEMENT"));
        assertTrue(codes(result).contains("NAMESPACE_MISMATCH"));
        SemanticDiagnostic unknown = firstOf(result, "UNKNOWN_ELEMENT");
        assertTrue(unknown.getMessage().contains("mismatch")
                || unknown.getMessage().contains(NS));
        assertEquals(firstOf(result, "NAMESPACE_MISMATCH").getSeverity(),
                SemanticDiagnostic.Severity.WARNING);
    }

    @Test
    public void runtimeUnavailableWarningWhenResolved() {
        Stage1PluginDescriptor plugin = reliableRoot("http");
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(plugin)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(false))
                .build();
        SemanticModel model = new SemanticModel(index);
        StructuralValidationResult result = model.validateStructural(
                load(model, "<http xmlns=\"" + NS + "\"/>"));

        assertTrue(result.isSuccessful());
        assertTrue(codes(result).contains("RUNTIME_UNAVAILABLE"));
        assertEquals(firstOf(result, "RUNTIME_UNAVAILABLE").getSeverity(),
                SemanticDiagnostic.Severity.WARNING);
    }

    @Test
    public void emptyAllowedSubElementsEmitsChildPolicyLimitation() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("box@" + NS)
                .namespaceUri(NS)
                .localName("box")
                .className("org.example.Box")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(true)
                .allowedSubElements(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));
        StructuralValidationResult result = model.validateStructural(
                load(model, "<box xmlns=\"" + NS + "\"/>"));

        assertTrue(result.isSuccessful());
        assertTrue(codes(result).stream().anyMatch(c -> "METADATA_LIMITATION".equals(c)));
        assertTrue(result.getDiagnostics().stream()
                .filter(d -> "METADATA_LIMITATION".equals(d.getCode()))
                .anyMatch(d -> d.getMessage().contains("allowedSubElements")
                        || d.getMessage().contains("child-allow")));
    }

    @Test
    public void catalogSmokeValidateStructuralDoesNotThrow() {
        SemanticModel model = new SemanticModel();
        SemanticLoadResult loaded = model.load("<config/>", SourceOrigin.unknown());
        if (!loaded.isSuccessful()) {
            // Catalog may treat bare config differently; still must not throw on empty-ish docs.
            return;
        }
        StructuralValidationResult result = model.validateStructural(loaded.getDocument().get());
        assertNotNull(result.getDiagnostics());
        assertSame(result.getDocument(), loaded.getDocument().get());
    }

    @Test
    public void typedAttributeValuesAcceptValidFormsAndRejectInvalidForms() {
        Stage1PluginDescriptor typed = Stage1PluginDescriptor.builder()
                .id("typed@" + NS)
                .namespaceUri(NS)
                .localName("typed")
                .className("org.example.Typed")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("integerValue", "INTEGER", false, List.of(), null, null, null),
                        new AttributeDefinition("longValue", "LONG", false, List.of(), null, null, null),
                        new AttributeDefinition("doubleValue", "DOUBLE", false, List.of(), null, null, null),
                        new AttributeDefinition("booleanValue", "BOOLEAN", false, List.of(), null, null, null)))
                .build();
        SemanticModel model = new SemanticModel(indexOf(typed));

        StructuralValidationResult valid = model.validateStructural(load(model,
                "<typed xmlns=\"" + NS + "\" integerValue=\" -12 \" longValue=\"9223372036854775807\""
                        + " doubleValue=\"1.25e2\" booleanValue=\"TrUe\"/>"));
        assertTrue(valid.isSuccessful(), diagnosticText(valid));

        assertInvalidTypedValue(model, "integerValue", "1.5", "INTEGER");
        assertInvalidTypedValue(model, "longValue", "9223372036854775808", "LONG");
        assertInvalidTypedValue(model, "doubleValue", "not-a-number", "DOUBLE");
        assertInvalidTypedValue(model, "booleanValue", "yes", "BOOLEAN");
    }

    @Test
    public void numericBoundsAndPatternsReportTheirDistinctInvalidReasons() {
        Stage1PluginDescriptor constrained = Stage1PluginDescriptor.builder()
                .id("constrained@" + NS)
                .namespaceUri(NS)
                .localName("constrained")
                .className("org.example.Constrained")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("quantity", "STRING", false, List.of(),
                                Integer.valueOf(2), Integer.valueOf(8), null),
                        new AttributeDefinition("code", "STRING", false, List.of(),
                                null, null, "^[A-Z]{2}$")))
                .build();
        SemanticModel model = new SemanticModel(indexOf(constrained));

        StructuralValidationResult boundaryValues = model.validateStructural(load(model,
                "<constrained xmlns=\"" + NS + "\" quantity=\"2\" code=\"AZ\"/>"));
        assertTrue(boundaryValues.isSuccessful(), diagnosticText(boundaryValues));

        assertInvalidAttribute(model, "constrained", "quantity", "1", "below minValue");
        assertInvalidAttribute(model, "constrained", "quantity", "9", "above maxValue");
        assertInvalidAttribute(model, "constrained", "quantity", "many", "not numeric for min/max");
        assertInvalidAttribute(model, "constrained", "code", "A1", "does not match pattern");

        Stage1PluginDescriptor malformedPattern = Stage1PluginDescriptor.builder()
                .id("malformed@" + NS)
                .namespaceUri(NS)
                .localName("malformed")
                .className("org.example.Malformed")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .attributeDefinitions(List.of(
                        new AttributeDefinition("code", "STRING", false, List.of(), null, null, "[")))
                .build();
        SemanticModel malformedModel = new SemanticModel(indexOf(malformedPattern));
        assertInvalidAttribute(malformedModel, "malformed", "code", "anything", "declared pattern");
    }

    private static void assertInvalidTypedValue(SemanticModel model, String attribute,
            String value, String expectedType) {
        StructuralValidationResult result = model.validateStructural(load(model,
                "<typed xmlns=\"" + NS + "\" " + attribute + "=\"" + value + "\"/>"));
        assertTrue(codes(result).contains("INVALID_ATTRIBUTE_VALUE"), diagnosticText(result));
        assertTrue(firstOf(result, "INVALID_ATTRIBUTE_VALUE").getMessage().contains("valid " + expectedType),
                diagnosticText(result));
    }

    private static void assertInvalidAttribute(SemanticModel model, String elementName,
            String attribute, String value, String expectedReason) {
        StructuralValidationResult result = model.validateStructural(load(model,
                "<" + elementName + " xmlns=\"" + NS + "\" " + attribute + "=\"" + value + "\"/>"));
        assertTrue(codes(result).contains("INVALID_ATTRIBUTE_VALUE"), diagnosticText(result));
        assertTrue(firstOf(result, "INVALID_ATTRIBUTE_VALUE").getMessage().contains(expectedReason),
                diagnosticText(result));
    }

    private static Stage1PluginDescriptor reliableRoot(String localName) {
        return Stage1PluginDescriptor.builder()
                .id(localName + "@" + NS)
                .namespaceUri(NS)
                .localName(localName)
                .className("org.example." + localName)
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(true)
                .allowedSubElements(List.of("x"))
                .build();
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
        SemanticLoadResult result = model.load(xml, new SourceOrigin("test:structural"));
        assertTrue(result.isSuccessful(), diagnosticText(result));
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

    private static List<SemanticDiagnostic.Severity> severities(StructuralValidationResult result) {
        return result.getDiagnostics().stream()
                .map(SemanticDiagnostic::getSeverity)
                .collect(Collectors.toList());
    }

    private static SemanticDiagnostic firstOf(StructuralValidationResult result, String code) {
        return result.getDiagnostics().stream()
                .filter(d -> code.equals(d.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing code " + code + " in "
                        + diagnosticText(result)));
    }

    private static String diagnosticText(StructuralValidationResult result) {
        List<String> parts = new ArrayList<>();
        for (SemanticDiagnostic diagnostic : result.getDiagnostics()) {
            parts.add(diagnostic.getSeverity() + ":" + diagnostic.getCode() + ":" + diagnostic.getMessage());
        }
        return parts.toString();
    }

    private static String diagnosticText(SemanticLoadResult result) {
        List<String> parts = new ArrayList<>();
        for (SemanticDiagnostic diagnostic : result.getDiagnostics()) {
            parts.add(diagnostic.getSeverity() + ":" + diagnostic.getCode() + ":" + diagnostic.getMessage());
        }
        return parts.toString();
    }
}
