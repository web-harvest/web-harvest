package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.testng.annotations.Test;
import org.webharvest.WHConstants;
import org.webharvest.semantic.Stage1PluginDescriptor.AttributeDefinition;

/**
 * Adversarial / gap-filling coverage for B1/B2 structural validation and plugin
 * resolution. Complements {@link PluginDescriptorIndexTest} and
 * {@link StructuralValidationTest}; does not replace F1–F7 semantic tests.
 */
public class StructuralValidationAdversarialTest {

    private static final String NS = "http://example.org/adv";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    // --- 16. null document ---

    @Test(expectedExceptions = NullPointerException.class)
    public void validateStructural_nullDocument_throwsNpe() {
        new SemanticModel(indexOf(reliableRoot("config"))).validateStructural(null);
    }

    @Test
    public void validateStructural_nullDocument_guardIsDocumentedInJavadoc() throws IOException {
        String source = readMainSource("SemanticModel.java");
        assertTrue(source.contains("@throws NullPointerException if {@code document} is null"),
                "SemanticModel.validateStructural must document NPE for null document");
        assertTrue(source.contains("Objects.requireNonNull(document, \"document\")"),
                "SemanticModel.validateStructural must null-check document");
    }

    // --- 17. publicly constructed SemanticDocument (F4 compatibility) ---

    @Test
    public void publiclyConstructedDocument_validatesViaIndex_withoutXmlProvenance() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("item@" + NS)
                .namespaceUri(NS)
                .localName("item")
                .className("org.example.Item")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .requiredAttributes(List.of("name"))
                .optionalAttributes(List.of())
                .attributeDefinitions(List.of(
                        new AttributeDefinition("name", "STRING", true, List.of(), null, null, null)))
                .allowedSubElements(List.of("x"))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        SemanticAttribute nameAttr = new SemanticAttribute(
                new QualifiedName("", "name", ""), "widget");
        SemanticElement root = new SemanticElement(
                "/{" + NS + "}item[1]",
                new QualifiedName(NS, "item", ""),
                List.of(nameAttr),
                List.of(),
                SourceLocation.unknown());
        SemanticDocument document = new SemanticDocument(
                SourceOrigin.unknown(),
                "<!-- no analyzer provenance; hand-built graph -->",
                List.of(root),
                root);

        StructuralValidationResult ok = model.validateStructural(document);
        assertTrue(ok.isSuccessful(), diagnosticText(ok));
        assertSame(ok.getDocument(), document);
        assertSame(model.inspect(document).getDocument(), document);

        SemanticElement missingName = new SemanticElement(
                "/{" + NS + "}item[1]",
                new QualifiedName(NS, "item", ""),
                List.of(),
                List.of(),
                SourceLocation.unknown());
        SemanticDocument bad = new SemanticDocument(
                SourceOrigin.unknown(),
                "synthetic",
                List.of(missingName),
                missingName);
        StructuralValidationResult missing = model.validateStructural(bad);
        assertTrue(codes(missing).contains("REQUIRED_ATTRIBUTE_MISSING"));
    }

    // --- 18. xmlns attributes must not be UNKNOWN_ATTRIBUTE ---

    @Test
    public void xmlnsAttributes_areNotFlaggedAsUnknownAttribute() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("item@" + NS)
                .namespaceUri(NS)
                .localName("item")
                .className("org.example.Item")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .requiredAttributes(List.of("name"))
                .optionalAttributes(List.of())
                .attributeDefinitions(List.of(
                        new AttributeDefinition("name", "STRING", true, List.of(), null, null, null)))
                .allowedSubElements(List.of("x"))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        List<SemanticAttribute> attributes = List.of(
                new SemanticAttribute(new QualifiedName("", "xmlns", ""), NS),
                new SemanticAttribute(new QualifiedName(XMLNS_URI, "p", "xmlns"), "urn:other"),
                new SemanticAttribute(new QualifiedName("", "name", ""), "ok"),
                new SemanticAttribute(new QualifiedName(XMLNS_URI, "xmlns", ""), NS));
        SemanticElement root = new SemanticElement(
                "/{" + NS + "}item[1]",
                new QualifiedName(NS, "item", ""),
                attributes,
                List.of(),
                SourceLocation.unknown());
        SemanticDocument document = new SemanticDocument(
                SourceOrigin.unknown(), "synthetic-xmlns", List.of(root), root);

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"), diagnosticText(result));
        assertTrue(result.isSuccessful(), diagnosticText(result));
    }

    @Test
    public void xmlnsDefaultDeclaration_fromLoadedXml_notUnknownAttribute() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("item@" + NS)
                .namespaceUri(NS)
                .localName("item")
                .className("org.example.Item")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .requiredAttributes(List.of("name"))
                .optionalAttributes(List.of())
                .attributeDefinitions(List.of(
                        new AttributeDefinition("name", "STRING", true, List.of(), null, null, null)))
                .allowedSubElements(List.of("x"))
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));
        SemanticDocument document = load(model,
                "<item xmlns=\"" + NS + "\" name=\"x\"/>");

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"),
                "xmlns must not surface as UNKNOWN_ATTRIBUTE; attrs="
                        + document.getRoot().getAttributes() + " diags=" + diagnosticText(result));
        assertTrue(result.isSuccessful(), diagnosticText(result));
    }

    // --- 19. no message.contains classification in structural validation path ---

    @Test
    public void structuralValidationPath_doesNotClassifyViaMessageContains() throws IOException {
        // Classification must use PluginResolutionStatus / diagnostic codes, never
        // message.contains(...) string sniffing (regression guard for B2).
        Pattern forbidden = Pattern.compile(
                "(getMessage\\(\\)\\s*\\.\\s*contains|\\.message\\s*\\.\\s*contains|"
                        + "diagnostic\\.getMessage\\(\\)\\s*\\.\\s*contains)",
                Pattern.MULTILINE);
        for (String file : List.of("StructuralValidator.java", "SemanticModel.java",
                "PluginDescriptorIndex.java", "PluginResolution.java")) {
            String source = readMainSource(file);
            assertFalse(forbidden.matcher(source).find(),
                    file + " must not classify diagnostics via message.contains");
        }
        // Switch on status enum is the intended resolution classification path.
        String validator = readMainSource("StructuralValidator.java");
        assertTrue(validator.contains("switch (resolution.getStatus())"),
                "StructuralValidator must classify via PluginResolutionStatus switch");
    }

    // --- Gap fills: UNAVAILABLE_OPTIONAL via validateStructural ---

    @Test
    public void unavailableOptional_emitsDiagnosticFromValidateStructural() {
        OptionalPluginProfile profile = new OptionalPluginProfile(List.of(
                new OptionalPluginProfile.Entry(NS, "ghost",
                        "org.webharvest.semantic.DefinitelyMissingPluginXYZ")));
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .descriptors(List.of())
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(profile)
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(false))
                .build();
        SemanticModel model = new SemanticModel(index);

        SemanticElement ghost = new SemanticElement(
                "/{" + NS + "}ghost[1]",
                new QualifiedName(NS, "ghost", ""),
                List.of(),
                List.of(),
                SourceLocation.unknown());
        SemanticDocument document = new SemanticDocument(
                SourceOrigin.unknown(), "synthetic-ghost", List.of(ghost), ghost);

        StructuralValidationResult result = model.validateStructural(document);
        assertFalse(result.isSuccessful());
        assertTrue(codes(result).contains("UNAVAILABLE_OPTIONAL"), diagnosticText(result));
        assertEquals(firstOf(result, "UNAVAILABLE_OPTIONAL").getSeverity(),
                SemanticDiagnostic.Severity.ERROR);

        SemanticElement unknown = new SemanticElement(
                "/{" + NS + "}totally-unknown[1]",
                new QualifiedName(NS, "totally-unknown", ""),
                List.of(),
                List.of(),
                SourceLocation.unknown());
        StructuralValidationResult plain = model.validateStructural(new SemanticDocument(
                SourceOrigin.unknown(), "synthetic-unknown", List.of(unknown), unknown));
        assertTrue(codes(plain).contains("UNKNOWN_ELEMENT"));
        assertFalse(codes(plain).contains("UNAVAILABLE_OPTIONAL"));
    }

    // --- Explicit element alias + rejected non-alias; database vs db ---

    @Test
    public void explicitElementAlias_resolves_andRejectedNonAliasStaysUnknown() {
        Stage1PluginDescriptor db = Stage1PluginDescriptor.builder()
                .id("db@" + WHConstants.XMLNS_CORE)
                .namespaceUri(WHConstants.XMLNS_CORE)
                .localName("db")
                .className("org.webharvest.plugin.db.DatabasePlugin")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(true)
                .allowedSubElements(List.of("x"))
                .build();
        ElementAliasTable aliases = new ElementAliasTable(Map.of("database", "db"));
        PluginDescriptorIndex index = PluginDescriptorIndex.builder()
                .addDescriptor(db)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(aliases)
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();

        assertEquals(index.resolve(WHConstants.XMLNS_CORE, "database").getStatus(),
                PluginResolutionStatus.RESOLVED);
        assertEquals(index.resolve(WHConstants.XMLNS_CORE, "db").getStatus(),
                PluginResolutionStatus.RESOLVED);

        // Without the alias table, "database" must not silently become "db".
        PluginDescriptorIndex noAlias = PluginDescriptorIndex.builder()
                .addDescriptor(db)
                .namespaceAliases(NamespaceAliasTable.empty())
                .elementAliases(ElementAliasTable.empty())
                .optionalProfile(OptionalPluginProfile.empty())
                .runtimeAvailability(PluginDescriptorIndex.RuntimeAvailability.always(true))
                .build();
        assertEquals(noAlias.resolve(WHConstants.XMLNS_CORE, "database").getStatus(),
                PluginResolutionStatus.UNKNOWN_ELEMENT);

        SemanticModel model = new SemanticModel(noAlias);
        SemanticElement databaseEl = new SemanticElement(
                "/{" + WHConstants.XMLNS_CORE + "}database[1]",
                new QualifiedName(WHConstants.XMLNS_CORE, "database", ""),
                List.of(),
                List.of(),
                SourceLocation.unknown());
        StructuralValidationResult result = model.validateStructural(new SemanticDocument(
                SourceOrigin.unknown(), "synthetic-database", List.of(databaseEl), databaseEl));
        assertTrue(codes(result).contains("UNKNOWN_ELEMENT"));
        assertFalse(codes(result).contains("AMBIGUOUS_ELEMENT"));
    }

    // --- Ambiguous + unknown from validateStructural (reinforcement) ---

    @Test
    public void validateStructural_emitsUnknownAndAmbiguousWithStableCodes() {
        Stage1PluginDescriptor first = Stage1PluginDescriptor.builder()
                .id("a").namespaceUri(NS).localName("dup").className("org.example.A")
                .reliableSchema(true).canAppearAtRoot(true).allowsBody(true)
                .allowedSubElements(List.of("x")).build();
        Stage1PluginDescriptor second = Stage1PluginDescriptor.builder()
                .id("b").namespaceUri(NS).localName("dup").className("org.example.B")
                .reliableSchema(true).canAppearAtRoot(true).allowsBody(true)
                .allowedSubElements(List.of("x")).build();
        SemanticModel ambiguousModel = new SemanticModel(indexOf(first, second));
        SemanticDocument ambDoc = load(ambiguousModel, "<dup xmlns=\"" + NS + "\"/>");
        StructuralValidationResult amb = ambiguousModel.validateStructural(ambDoc);
        assertEquals(firstOf(amb, "AMBIGUOUS_ELEMENT").getCode(), "AMBIGUOUS_ELEMENT");

        SemanticModel unknownModel = new SemanticModel(indexOf(reliableRoot("config")));
        StructuralValidationResult unk = unknownModel.validateStructural(
                load(unknownModel, "<config xmlns=\"" + NS + "\"><nope/></config>"));
        assertEquals(firstOf(unk, "UNKNOWN_ELEMENT").getCode(), "UNKNOWN_ELEMENT");
    }

    // --- METADATA_LIMITATION documented for empty attribute closed set ---

    @Test
    public void metadataLimitation_whenReliableButNoClosedAttributeSet() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("bare-attrs@" + NS)
                .namespaceUri(NS)
                .localName("bare-attrs")
                .className("org.example.BareAttrs")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(true)
                .allowedSubElements(List.of("x"))
                .requiredAttributes(List.of())
                .optionalAttributes(List.of())
                .attributeDefinitions(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));
        StructuralValidationResult result = model.validateStructural(
                load(model, "<bare-attrs xmlns=\"" + NS + "\" weird=\"1\"/>"));

        assertTrue(codes(result).contains("METADATA_LIMITATION"), diagnosticText(result));
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"),
                "without closed attribute set, unknown attrs must not be invented");
        assertTrue(firstOf(result, "METADATA_LIMITATION").getMessage().contains("attribute"),
                diagnosticText(result));
    }

    // --- Determinism reinforcement on adversarial path ---

    @Test
    public void adversarialDiagnostics_areDeterministicAcrossRepeatedRuns() {
        SemanticModel model = new SemanticModel(indexOf(reliableRoot("config")));
        SemanticDocument document = load(model,
                "<config xmlns=\"" + NS + "\"><nope/></config>");
        StructuralValidationResult first = model.validateStructural(document);
        StructuralValidationResult second = model.validateStructural(document);
        assertEquals(codes(first), codes(second));
        assertEquals(messages(first), messages(second));
        assertEquals(severities(first), severities(second));
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
        SemanticLoadResult result = model.load(xml, new SourceOrigin("test:adversarial"));
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

    private static String readMainSource(String simpleName) throws IOException {
        Path[] candidates = {
                Paths.get("src/main/java/org/webharvest/semantic", simpleName),
                Paths.get("webharvest-core/src/main/java/org/webharvest/semantic", simpleName),
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate, StandardCharsets.UTF_8);
            }
        }
        throw new IOException("cannot locate " + simpleName + " from " + Paths.get("").toAbsolutePath());
    }
}
