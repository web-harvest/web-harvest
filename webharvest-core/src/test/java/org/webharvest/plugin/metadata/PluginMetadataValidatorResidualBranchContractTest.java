package org.webharvest.plugin.metadata;

import static org.testng.Assert.*;

import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

/**
 * Residual validator contracts for missing identity fields, empty roles,
 * dependent-without-parents, and null schema extension maps.
 */
public class PluginMetadataValidatorResidualBranchContractTest {

    @Test
    public void missingIdentityFieldsAreReportedAsErrors() {
        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.LEAF,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = new PluginDescriptor(
                "",
                "",
                "Display",
                "test",
                "",
                "test",
                "has description",
                null,
                semantics,
                null,
                List.of(),
                Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertFalse(result.isValid());
        long missingRequired = result.getIssues().stream()
                .filter(issue -> issue.getType()
                        == PluginMetadataValidator.ValidationIssue.IssueType.MISSING_REQUIRED_FIELD)
                .count();
        assertTrue(missingRequired >= 3, "id, elementName, and className must be required: " + result.getIssues());
    }

    @Test
    public void dependentWithoutRequiredParentsIsError() {
        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.DEPENDENT,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = new PluginDescriptor(
                "orphan-dep",
                "orphan-dep",
                "Display",
                "test",
                "com.example.OrphanDep",
                "test",
                "desc",
                null,
                semantics,
                null,
                List.of(),
                Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertTrue(result.getIssues().stream().anyMatch(issue ->
                issue.getType() == PluginMetadataValidator.ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT
                        && issue.isError()
                        && issue.getMessage().contains("requiredParentTypes")));
    }

    @Test
    public void emptyDescriptionEmitsDocumentationWarning() {
        PluginSemantics semantics = new PluginSemantics(
                PluginSemanticRole.LEAF,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of());
        PluginDescriptor descriptor = new PluginDescriptor(
                "no-desc",
                "no-desc",
                "Display",
                "test",
                "com.example.NoDesc",
                "test",
                "",
                null,
                semantics,
                null,
                List.of(),
                Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(descriptor));

        assertTrue(result.getWarnings().stream().anyMatch(issue ->
                issue.getType()
                        == PluginMetadataValidator.ValidationIssue.IssueType.MISSING_DOCUMENTATION));
    }

    @Test
    public void parentChildMismatchWhenRequiredParentsDifferEmitsWarning() {
        PluginSemantics parentSemantics = new PluginSemantics(
                PluginSemanticRole.CONTEXT_CONTAINER,
                true,
                true,
                false,
                true,
                false,
                List.of("child"),
                List.of());
        PluginSemantics childSemantics = new PluginSemantics(
                PluginSemanticRole.DEPENDENT,
                false,
                false,
                false,
                false,
                false,
                List.of(),
                List.of("other-parent"));
        PluginDescriptor parent = new PluginDescriptor(
                "parent", "parent", "P", "test", "com.example.Parent", "test", "d",
                null, parentSemantics, null, List.of(), Map.of());
        PluginDescriptor child = new PluginDescriptor(
                "child", "child", "C", "test", "com.example.Child", "test", "d",
                null, childSemantics, null, List.of(), Map.of());

        PluginMetadataValidator.ValidationResult result =
                PluginMetadataValidator.validate(List.of(parent, child));

        assertTrue(result.getWarnings().stream().anyMatch(issue ->
                issue.getType()
                        == PluginMetadataValidator.ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT
                        && issue.getMessage().contains("requires different parents")));
    }
}
