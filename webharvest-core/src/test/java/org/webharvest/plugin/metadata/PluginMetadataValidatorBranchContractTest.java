package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Validation failure and warning paths for {@link PluginMetadataValidator}
 * not exhaustively covered by {@link PluginMetadataValidatorTest}.
 */
public class PluginMetadataValidatorBranchContractTest {

    private static PluginDescriptor minimalDescriptor(
            String id, PluginSemantics semantics, List<String> dependencies, Map<String, Object> extensions) {
        return new PluginDescriptor(
                id,
                id,
                "Display",
                "test",
                "com.example." + id,
                "test",
                "",
                null,
                semantics,
                null,
                dependencies,
                extensions);
    }

    @Test
    public void missingSemanticsIsReportedAsError() {
        PluginDescriptor descriptor = minimalDescriptor("no-semantics", null, List.of(), Map.of());
        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertFalse(result.isValid());
        assertTrue(result.getIssues().stream().anyMatch(issue ->
                issue.getType() == PluginMetadataValidator.ValidationIssue.IssueType.MISSING_SEMANTIC_INFO
                        && issue.isError()));
    }

    @Test
    public void allowsChildrenWithoutBodyOrAnyChildAndNoAllowedTypesEmitsWarning() {
        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.CONTEXT_CONTAINER,
                true,
                true,
                false,
                false,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = minimalDescriptor("container", semantics, List.of(), Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertTrue(result.getWarnings().stream().anyMatch(issue ->
                issue.getType() == PluginMetadataValidator.ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT
                        && issue.getMessage().contains("allowedChildTypes")));
    }

    @Test
    public void schemaExtensionWithoutAttributeDefinitionsEmitsWarning() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("attributeDefinitions", new ArrayList<Map<String, Object>>());
        Map<String, Object> extensions = Map.of("schema", schema);

        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.BODY_EVALUATOR,
                true,
                false,
                false,
                true,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = minimalDescriptor("schema-gap", semantics, List.of(), extensions);

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertTrue(result.getWarnings().stream().anyMatch(issue ->
                issue.getType() == PluginMetadataValidator.ValidationIssue.IssueType.MISSING_ATTRIBUTE_DEFINITION));
    }

    @Test
    public void unknownAllowedChildTypeEmitsDependencyWarning() {
        PluginSemantics parentSemantics = new PluginSemantics(
                PluginSemanticRole.CONTEXT_CONTAINER,
                true,
                true,
                false,
                true,
                false,
                List.of("ghost-child"),
                List.of());
        PluginDescriptor parent = minimalDescriptor("parent", parentSemantics, List.of(), Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(parent));

        assertTrue(result.getWarnings().stream().anyMatch(issue ->
                issue.getType() == PluginMetadataValidator.ValidationIssue.IssueType.INVALID_DEPENDENCY
                        && issue.getMessage().contains("ghost-child")));
    }

    @Test
    public void parentAcceptingAnyChildSuppressesRequiredParentMismatchWarning() {
        PluginSemantics parentSemantics = new PluginSemantics(
                PluginSemanticRole.CONTEXT_CONTAINER,
                true,
                true,
                true,
                true,
                false,
                List.of("other-child"),
                List.of());
        PluginSemantics childSemantics = new PluginSemantics(
                PluginSemanticRole.DEPENDENT,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of("parent"));
        PluginDescriptor parent = minimalDescriptor("parent", parentSemantics, List.of(), Map.of());
        PluginDescriptor child = minimalDescriptor("child", childSemantics, List.of(), Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(parent, child));

        assertFalse(result.getWarnings().stream().anyMatch(issue ->
                issue.getMessage().contains("does not allow 'child' as child")));
    }

    @Test
    public void validationIssueToStringIncludesSeverityAndPluginId() {
        PluginMetadataValidator.ValidationIssue issue = PluginMetadataValidator.ValidationIssue.error(
                "sample",
                PluginMetadataValidator.ValidationIssue.IssueType.MISSING_REQUIRED_FIELD,
                "Missing id");
        assertTrue(issue.toString().startsWith("ERROR [sample]:"));
        assertTrue(issue.isError());

        PluginMetadataValidator.ValidationIssue warning = PluginMetadataValidator.ValidationIssue.warning(
                "sample",
                PluginMetadataValidator.ValidationIssue.IssueType.MISSING_DOCUMENTATION,
                "No docs");
        assertTrue(warning.toString().startsWith("WARNING [sample]:"));
        assertFalse(warning.isError());
    }

    @Test
    public void logValidationResultsAcceptsValidResultWithWarningsOnly() {
        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.BODY_EVALUATOR,
                true,
                true,
                true,
                true,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = minimalDescriptor("warn-only", semantics, List.of(), Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertTrue(result.isValid());
        assertTrue(result.getWarningCount() > 0);
        assertEquals(result.getErrorCount(), 0);
        PluginMetadataValidator.logValidationResults(result);
    }
}
