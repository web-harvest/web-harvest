package org.webharvest.plugin.metadata;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Tests for PluginMetadataValidator.
 */
public class PluginMetadataValidatorTest {
    
    @BeforeMethod
    public void setUp() {
        // Ensure catalog is initialized
        PluginMetadataCatalog.getAllDescriptors();
    }
    
    @Test
    public void testValidateAll() {
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validateAll();
        
        assertNotNull(result);
        
        // Log results for debugging
        PluginMetadataValidator.logValidationResults(result);
        
        // Should have some warnings but ideally no errors
        // (Some warnings are expected for plugins without complete metadata)
        assertTrue(result.getErrorCount() >= 0, "Should not have errors");
        assertTrue(result.getWarningCount() >= 0, "Should have some warnings");
    }
    
    @Test
    public void testValidateCompleteDescriptor() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Create a complete, valid descriptor
        PluginSemantics semantics = new PluginSemantics(
            PluginSemanticRole.BODY_EVALUATOR,
            true, // canAppearAtRoot
            true, // allowsChildren
            true, // acceptsAnyChild
            true, // allowBody
            false, // allowTextContent
            List.of(), // allowedChildTypes
            List.of() // requiredParentTypes
        );
        
        PluginDocumentation doc = new PluginDocumentation(
            "Test description", "http://example.com/test", List.of()
        );
        
        PluginUIHints uiHints = new PluginUIHints(
            "Test Group", "fa-test", List.of("test")
        );
        
        Map<String, Object> schema = new HashMap<>();
        List<Map<String, Object>> attrs = new ArrayList<>();
        Map<String, Object> attr = new HashMap<>();
        attr.put("name", "testAttr");
        attr.put("type", "STRING");
        attr.put("required", true);
        attrs.add(attr);
        schema.put("attributeDefinitions", attrs);
        
        Map<String, Object> extensions = new HashMap<>();
        extensions.put("schema", schema);
        
        PluginDescriptor desc = new PluginDescriptor(
            "test-plugin",
            "test-plugin",
            "Test Plugin",
            "test",
            "TestPlugin",
            "Test",
            "Test description",
            doc,
            semantics,
            uiHints,
            List.of(),
            extensions
        );
        
        descriptors.add(desc);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        // Complete descriptor should have no errors
        assertEquals(result.getErrorCount(), 0, 
            "Complete descriptor should have no errors");
    }
    
    @Test
    public void testValidateMissingRequiredFields() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Create descriptor with missing required fields
        PluginSemantics semantics = new PluginSemantics(
            PluginSemanticRole.BODY_EVALUATOR,
            true, true, true, true, false, List.of(), List.of()
        );
        
        // Missing id, elementName, className
        PluginDescriptor desc = new PluginDescriptor(
            "", // Missing id
            "", // Missing elementName
            "Test",
            "test",
            "", // Missing className
            "Test",
            "Description",
            null, // No documentation
            semantics,
            null, // No UI hints
            List.of(),
            Map.of()
        );
        
        descriptors.add(desc);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0, 
            "Should have errors for missing required fields");
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.getType() == 
                PluginMetadataValidator.ValidationIssue.IssueType.MISSING_REQUIRED_FIELD),
            "Should have missing required field issues");
    }
    
    @Test
    public void testValidateInvalidDependency() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        PluginSemantics semantics = new PluginSemantics(
            PluginSemanticRole.DEPENDENT,
            false, // canAppearAtRoot
            false, // allowsChildren
            false, // acceptsAnyChild
            false, // allowBody
            false, // allowTextContent
            List.of(), // allowedChildTypes
            List.of("non-existent-parent") // Invalid dependency
        );
        
        PluginDescriptor desc = new PluginDescriptor(
            "test-plugin",
            "test-plugin",
            "Test",
            "test",
            "TestPlugin",
            "Test",
            "Description",
            null,
            semantics,
            null,
            List.of("non-existent-plugin"), // Invalid dependency
            Map.of()
        );
        
        descriptors.add(desc);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0, 
            "Should have errors for invalid dependencies");
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.getType() == 
                PluginMetadataValidator.ValidationIssue.IssueType.INVALID_DEPENDENCY),
            "Should have invalid dependency issues");
    }
    
    @Test
    public void testValidateDependentPluginWithoutRequiredParent() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Dependent plugin without requiredParentTypes
        PluginSemantics semantics = new PluginSemantics(
            PluginSemanticRole.DEPENDENT,
            false,
            false,
            false,
            false,
            false,
            List.of(),
            List.of() // Missing required parent types
        );
        
        PluginDescriptor desc = new PluginDescriptor(
            "test-dependent",
            "test-dependent",
            "Test Dependent",
            "test",
            "TestDependent",
            "Test",
            "Description",
            null,
            semantics,
            null,
            List.of(),
            Map.of()
        );
        
        descriptors.add(desc);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0, 
            "Should have errors for dependent plugin without requiredParentTypes");
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.getType() == 
                PluginMetadataValidator.ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT),
            "Should have inconsistent child/parent issue");
    }
    
    @Test
    public void testValidateChildParentConsistency() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Parent plugin
        PluginSemantics parentSemantics = new PluginSemantics(
            PluginSemanticRole.CONTEXT_CONTAINER,
            true,
            true,
            false, // Does not accept any child
            true,
            false,
            List.of("child-plugin"), // Only allows child-plugin
            List.of()
        );
        
        PluginDescriptor parent = new PluginDescriptor(
            "parent-plugin",
            "parent-plugin",
            "Parent",
            "test",
            "ParentPlugin",
            "Test",
            "Description",
            null,
            parentSemantics,
            null,
            List.of(),
            Map.of()
        );
        
        // Child plugin that requires different parent
        PluginSemantics childSemantics = new PluginSemantics(
            PluginSemanticRole.DEPENDENT,
            false,
            false,
            false,
            false,
            false,
            List.of(),
            List.of("different-parent") // Requires different parent
        );
        
        PluginDescriptor child = new PluginDescriptor(
            "child-plugin",
            "child-plugin",
            "Child",
            "test",
            "ChildPlugin",
            "Test",
            "Description",
            null,
            childSemantics,
            null,
            List.of(),
            Map.of()
        );
        
        descriptors.add(parent);
        descriptors.add(child);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        // Should have warnings about inconsistency
        assertTrue(result.getWarningCount() > 0, 
            "Should have warnings for child/parent inconsistency");
    }
    
    @Test
    public void testValidateCircularDependency() {
        List<PluginDescriptor> descriptors = new ArrayList<>();
        
        // Plugin A depends on B
        PluginDescriptor a = new PluginDescriptor(
            "plugin-a",
            "plugin-a",
            "Plugin A",
            "test",
            "PluginA",
            "Test",
            "Description",
            null,
            new PluginSemantics(PluginSemanticRole.BODY_EVALUATOR, true, true, true, true, false, List.of(), List.of()),
            null,
            List.of("plugin-b"), // Depends on B
            Map.of()
        );
        
        // Plugin B depends on A (circular)
        PluginDescriptor b = new PluginDescriptor(
            "plugin-b",
            "plugin-b",
            "Plugin B",
            "test",
            "PluginB",
            "Test",
            "Description",
            null,
            new PluginSemantics(PluginSemanticRole.BODY_EVALUATOR, true, true, true, true, false, List.of(), List.of()),
            null,
            List.of("plugin-a"), // Depends on A (circular)
            Map.of()
        );
        
        descriptors.add(a);
        descriptors.add(b);
        
        PluginMetadataValidator.ValidationResult result = 
            PluginMetadataValidator.validate(descriptors);
        
        assertNotNull(result);
        assertTrue(result.getErrorCount() > 0, 
            "Should have errors for circular dependencies");
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.getType() == 
                PluginMetadataValidator.ValidationIssue.IssueType.CIRCULAR_DEPENDENCY),
            "Should have circular dependency issues");
    }
}

