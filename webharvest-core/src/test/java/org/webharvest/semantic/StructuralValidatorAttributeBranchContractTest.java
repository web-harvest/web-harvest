package org.webharvest.semantic;

import static org.testng.Assert.*;

import java.util.List;
import java.util.stream.Collectors;
import org.testng.annotations.Test;

/**
 * Structural validation contracts for attribute name closure without definitions,
 * xmlns handling, and parent local-name mismatches.
 */
public class StructuralValidatorAttributeBranchContractTest {

    private static final String NS = "http://example.org/struct-branch";

    @Test
    public void unknownAttributeDetectedWhenOnlyRequiredOptionalListsExist() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("item@" + NS)
                .namespaceUri(NS)
                .localName("item")
                .className("org.example.Item")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .requiredAttributes(List.of("id"))
                .optionalAttributes(List.of("mode"))
                .attributeDefinitions(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult unknown = model.validateStructural(load(model,
                "<item xmlns=\"" + NS + "\" id=\"1\" extra=\"x\"/>"));
        assertTrue(codes(unknown).contains("UNKNOWN_ATTRIBUTE"));
        assertFalse(messages(unknown).stream().anyMatch(m -> m.contains("unknown-attribute check skipped")));

        StructuralValidationResult ok = model.validateStructural(load(model,
                "<item xmlns=\"" + NS + "\" id=\"1\" mode=\"on\"/>"));
        assertTrue(ok.isSuccessful(), diagnosticText(ok));
    }

    @Test
    public void xmlnsDeclarationsAreIgnoredForUnknownAttributeChecks() {
        Stage1PluginDescriptor plugin = Stage1PluginDescriptor.builder()
                .id("root@" + NS)
                .namespaceUri(NS)
                .localName("root")
                .className("org.example.Root")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(true)
                .attributeDefinitions(List.of())
                .build();
        SemanticModel model = new SemanticModel(indexOf(plugin));

        StructuralValidationResult result = model.validateStructural(load(model,
                "<root xmlns=\"" + NS + "\" xmlns:other=\"http://other.ns\"/>"));
        assertFalse(codes(result).contains("UNKNOWN_ATTRIBUTE"), diagnosticText(result));
    }

    @Test
    public void legacyRequiredParentLocalNameDoesNotProduceQualifiedParentError() {
        Stage1PluginDescriptor container = Stage1PluginDescriptor.builder()
                .id("container@" + NS)
                .namespaceUri(NS)
                .localName("container")
                .className("org.example.Container")
                .reliableSchema(true)
                .canAppearAtRoot(true)
                .allowsBody(false)
                .allowedSubElements(List.of("leaf"))
                .build();
        Stage1PluginDescriptor leaf = Stage1PluginDescriptor.builder()
                .id("leaf@" + NS)
                .namespaceUri(NS)
                .localName("leaf")
                .className("org.example.Leaf")
                .reliableSchema(true)
                .canAppearAtRoot(false)
                .allowsBody(false)
                .requiredParents(List.of("parent"))
                .build();
        SemanticModel model = new SemanticModel(indexOf(container, leaf));

        StructuralValidationResult result = model.validateStructural(load(model,
                "<container xmlns=\"" + NS + "\"><leaf/></container>"));
        assertTrue(codes(result).contains("METADATA_LIMITATION"), diagnosticText(result));
        assertFalse(codes(result).contains("INVALID_PARENT"), diagnosticText(result));
        assertTrue(messages(result).stream()
                .anyMatch(m -> m.contains("namespace-unqualified legacy names")));
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
        SemanticLoadResult result = model.load(xml, new SourceOrigin("test:struct-branch"));
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
