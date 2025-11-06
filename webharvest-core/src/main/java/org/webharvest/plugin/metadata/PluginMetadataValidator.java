package org.webharvest.plugin.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates completeness and correctness of plugin metadata.
 * 
 * Checks for:
 * - Required metadata fields (id, elementName, description, etc.)
 * - Missing attribute definitions
 * - Inconsistent child/parent relationships
 * - Missing or invalid semantic information
 * - Circular dependencies
 * - Orphaned plugins (dependencies that don't exist)
 * 
 * @author WebHarvest Team
 * @since 2.3.0
 */
public final class PluginMetadataValidator {
    
    private static final Logger LOG = LoggerFactory.getLogger(PluginMetadataValidator.class);
    
    private PluginMetadataValidator() {
        // Utility class
    }
    
    /**
     * Validation result containing issues found.
     */
    public static final class ValidationResult {
        private final List<ValidationIssue> issues;
        private final boolean isValid;
        
        private ValidationResult(List<ValidationIssue> issues) {
            this.issues = issues == null ? List.of() : List.copyOf(issues);
            this.isValid = this.issues.stream().noneMatch(ValidationIssue::isError);
        }
        
        public List<ValidationIssue> getIssues() {
            return issues;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public List<ValidationIssue> getErrors() {
            return issues.stream()
                .filter(ValidationIssue::isError)
                .collect(Collectors.toList());
        }
        
        public List<ValidationIssue> getWarnings() {
            return issues.stream()
                .filter(i -> !i.isError())
                .collect(Collectors.toList());
        }
        
        public int getErrorCount() {
            return (int) issues.stream().filter(ValidationIssue::isError).count();
        }
        
        public int getWarningCount() {
            return (int) issues.stream().filter(i -> !i.isError()).count();
        }
    }
    
    /**
     * A validation issue (error or warning).
     */
    public static final class ValidationIssue {
        private final String pluginId;
        private final IssueType type;
        private final String message;
        private final boolean isError;
        
        public enum IssueType {
            MISSING_REQUIRED_FIELD,
            MISSING_ATTRIBUTE_DEFINITION,
            MISSING_SEMANTIC_INFO,
            INVALID_DEPENDENCY,
            CIRCULAR_DEPENDENCY,
            ORPHANED_PLUGIN,
            INCONSISTENT_CHILD_PARENT,
            MISSING_DOCUMENTATION,
            MISSING_UI_HINTS,
            INVALID_NAMESPACE
        }
        
        private ValidationIssue(String pluginId, IssueType type, String message, boolean isError) {
            this.pluginId = pluginId;
            this.type = type;
            this.message = message;
            this.isError = isError;
        }
        
        public static ValidationIssue error(String pluginId, IssueType type, String message) {
            return new ValidationIssue(pluginId, type, message, true);
        }
        
        public static ValidationIssue warning(String pluginId, IssueType type, String message) {
            return new ValidationIssue(pluginId, type, message, false);
        }
        
        public String getPluginId() {
            return pluginId;
        }
        
        public IssueType getType() {
            return type;
        }
        
        public String getMessage() {
            return message;
        }
        
        public boolean isError() {
            return isError;
        }
        
        @Override
        public String toString() {
            return String.format("%s [%s]: %s", 
                isError ? "ERROR" : "WARNING", pluginId, message);
        }
    }
    
    /**
     * Validates all plugins in the catalog.
     * 
     * @return ValidationResult with all issues found
     */
    public static ValidationResult validateAll() {
        List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
        return validate(descriptors);
    }
    
    /**
     * Validates a list of plugin descriptors.
     * 
     * @param descriptors list of plugin descriptors to validate
     * @return ValidationResult with all issues found
     */
    public static ValidationResult validate(List<PluginDescriptor> descriptors) {
        List<ValidationIssue> issues = new ArrayList<>();
        Map<String, PluginDescriptor> descriptorMap = new HashMap<>();
        
        // Build map for quick lookup
        for (PluginDescriptor desc : descriptors) {
            descriptorMap.put(desc.getId(), desc);
        }
        
        // Validate each descriptor
        for (PluginDescriptor desc : descriptors) {
            issues.addAll(validateDescriptor(desc, descriptorMap));
        }
        
        // Cross-validation checks
        issues.addAll(validateDependencies(descriptors, descriptorMap));
        issues.addAll(validateCircularDependencies(descriptors, descriptorMap));
        issues.addAll(validateChildParentConsistency(descriptors, descriptorMap));
        
        return new ValidationResult(issues);
    }
    
    /**
     * Validates a single plugin descriptor.
     */
    private static List<ValidationIssue> validateDescriptor(
            PluginDescriptor desc, Map<String, PluginDescriptor> allDescriptors) {
        
        List<ValidationIssue> issues = new ArrayList<>();
        String pluginId = desc.getId();
        
        // Required fields
        if (desc.getId() == null || desc.getId().isEmpty()) {
            issues.add(ValidationIssue.error(pluginId, 
                ValidationIssue.IssueType.MISSING_REQUIRED_FIELD, 
                "Missing required field: id"));
        }
        
        if (desc.getElementName() == null || desc.getElementName().isEmpty()) {
            issues.add(ValidationIssue.error(pluginId, 
                ValidationIssue.IssueType.MISSING_REQUIRED_FIELD, 
                "Missing required field: elementName"));
        }
        
        if (desc.getDescription() == null || desc.getDescription().isEmpty()) {
            issues.add(ValidationIssue.warning(pluginId, 
                ValidationIssue.IssueType.MISSING_DOCUMENTATION, 
                "Missing description - recommended for better IDE support"));
        }
        
        if (desc.getClassName() == null || desc.getClassName().isEmpty()) {
            issues.add(ValidationIssue.error(pluginId, 
                ValidationIssue.IssueType.MISSING_REQUIRED_FIELD, 
                "Missing required field: className"));
        }
        
        // Semantic information
        PluginSemantics semantics = desc.getSemantics();
        if (semantics == null) {
            issues.add(ValidationIssue.error(pluginId, 
                ValidationIssue.IssueType.MISSING_SEMANTIC_INFO, 
                "Missing semantic information - required for validation"));
        } else {
            // Validate semantic consistency
            if (semantics.getRole() == null) {
                issues.add(ValidationIssue.error(pluginId, 
                    ValidationIssue.IssueType.MISSING_SEMANTIC_INFO, 
                    "Missing semantic role"));
            }
            
            // Check for inconsistencies
            if (semantics.allowsChildren() && !semantics.allowsBody() && !semantics.acceptsAnyChild()) {
                if (semantics.getAllowedChildTypes().isEmpty()) {
                    issues.add(ValidationIssue.warning(pluginId, 
                        ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT, 
                        "Plugin allows children but has no allowedChildTypes specified"));
                }
            }
            
            // Dependent plugins should have requiredParentTypes
            if (semantics.getRole() == PluginSemanticRole.DEPENDENT) {
                if (semantics.getRequiredParentTypes().isEmpty()) {
                    issues.add(ValidationIssue.error(pluginId, 
                        ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT, 
                        "Dependent plugin must specify requiredParentTypes"));
                }
            }
        }
        
        // Check extensions for schema information
        Map<String, Object> extensions = desc.getExtensions();
        if (extensions != null && extensions.containsKey("schema")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> schema = (Map<String, Object>) extensions.get("schema");
            if (schema != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> attrDefs = 
                    (List<Map<String, Object>>) schema.get("attributeDefinitions");
                
                if (attrDefs == null || attrDefs.isEmpty()) {
                    // This is only a warning if the plugin might have attributes
                    // (we can't always know this without loading the class)
                    issues.add(ValidationIssue.warning(pluginId, 
                        ValidationIssue.IssueType.MISSING_ATTRIBUTE_DEFINITION, 
                        "No attribute definitions found - may be missing attribute metadata"));
                }
            }
        }
        
        // UI hints - recommended but not required
        if (desc.getUiHints() == null) {
            issues.add(ValidationIssue.warning(pluginId, 
                ValidationIssue.IssueType.MISSING_UI_HINTS, 
                "Missing UI hints - recommended for better IDE experience"));
        }
        
        // Documentation - recommended
        if (desc.getDocumentation() == null) {
            issues.add(ValidationIssue.warning(pluginId, 
                ValidationIssue.IssueType.MISSING_DOCUMENTATION, 
                "Missing documentation - recommended for user guidance"));
        }
        
        return issues;
    }
    
    /**
     * Validates dependencies (checks if referenced plugins exist).
     */
    private static List<ValidationIssue> validateDependencies(
            List<PluginDescriptor> descriptors, Map<String, PluginDescriptor> descriptorMap) {
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        for (PluginDescriptor desc : descriptors) {
            String pluginId = desc.getId();
            
            // Check dependencies
            for (String dep : desc.getDependencies()) {
                if (!descriptorMap.containsKey(dep)) {
                    issues.add(ValidationIssue.error(pluginId, 
                        ValidationIssue.IssueType.INVALID_DEPENDENCY, 
                        String.format("Dependency '%s' does not exist", dep)));
                }
            }
            
            // Check required parent types
            PluginSemantics semantics = desc.getSemantics();
            if (semantics != null) {
                for (String parentType : semantics.getRequiredParentTypes()) {
                    if (!descriptorMap.containsKey(parentType)) {
                        issues.add(ValidationIssue.error(pluginId, 
                            ValidationIssue.IssueType.INVALID_DEPENDENCY, 
                            String.format("Required parent type '%s' does not exist", parentType)));
                    }
                }
                
                // Check allowed child types
                for (String childType : semantics.getAllowedChildTypes()) {
                    if (!descriptorMap.containsKey(childType)) {
                        issues.add(ValidationIssue.warning(pluginId, 
                            ValidationIssue.IssueType.INVALID_DEPENDENCY, 
                            String.format("Allowed child type '%s' does not exist", childType)));
                    }
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Validates for circular dependencies (A depends on B, B depends on A).
     */
    private static List<ValidationIssue> validateCircularDependencies(
            List<PluginDescriptor> descriptors, Map<String, PluginDescriptor> descriptorMap) {
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        for (PluginDescriptor desc : descriptors) {
            String pluginId = desc.getId();
            Set<String> visited = new java.util.HashSet<>();
            
            if (hasCircularDependency(desc, descriptorMap, visited, new java.util.HashSet<>())) {
                issues.add(ValidationIssue.error(pluginId, 
                    ValidationIssue.IssueType.CIRCULAR_DEPENDENCY, 
                    "Circular dependency detected in plugin dependencies"));
            }
        }
        
        return issues;
    }
    
    /**
     * Recursive check for circular dependencies using DFS.
     */
    private static boolean hasCircularDependency(
            PluginDescriptor desc, 
            Map<String, PluginDescriptor> descriptorMap,
            Set<String> visited, 
            Set<String> recursionStack) {
        
        String pluginId = desc.getId();
        
        if (recursionStack.contains(pluginId)) {
            return true; // Circular dependency found
        }
        
        if (visited.contains(pluginId)) {
            return false; // Already checked
        }
        
        visited.add(pluginId);
        recursionStack.add(pluginId);
        
        // Check all dependencies
        for (String dep : desc.getDependencies()) {
            PluginDescriptor depDesc = descriptorMap.get(dep);
            if (depDesc != null) {
                if (hasCircularDependency(depDesc, descriptorMap, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(pluginId);
        return false;
    }
    
    /**
     * Validates consistency between child and parent relationships.
     */
    private static List<ValidationIssue> validateChildParentConsistency(
            List<PluginDescriptor> descriptors, Map<String, PluginDescriptor> descriptorMap) {
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        for (PluginDescriptor desc : descriptors) {
            String pluginId = desc.getId();
            PluginSemantics semantics = desc.getSemantics();
            
            if (semantics == null) {
                continue;
            }
            
            // Check: if A allows B as child, B should allow A as parent (or be generic)
            for (String allowedChild : semantics.getAllowedChildTypes()) {
                PluginDescriptor childDesc = descriptorMap.get(allowedChild);
                if (childDesc != null) {
                    PluginSemantics childSemantics = childDesc.getSemantics();
                    if (childSemantics != null) {
                        List<String> requiredParents = childSemantics.getRequiredParentTypes();
                        if (!requiredParents.isEmpty() && !requiredParents.contains(pluginId)) {
                            // This is a warning, not an error - child might accept any parent
                            issues.add(ValidationIssue.warning(allowedChild, 
                                ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT, 
                                String.format("Plugin '%s' allows '%s' as child, but '%s' requires different parents: %s", 
                                    pluginId, allowedChild, allowedChild, requiredParents)));
                        }
                    }
                }
            }
            
            // Check: if A requires B as parent, B should allow A as child
            for (String requiredParent : semantics.getRequiredParentTypes()) {
                PluginDescriptor parentDesc = descriptorMap.get(requiredParent);
                if (parentDesc != null) {
                    PluginSemantics parentSemantics = parentDesc.getSemantics();
                    if (parentSemantics != null) {
                        if (parentSemantics.acceptsAnyChild()) {
                            // OK - parent accepts any child
                            continue;
                        }
                        
                        List<String> allowedChildren = parentSemantics.getAllowedChildTypes();
                        if (!allowedChildren.isEmpty() && !allowedChildren.contains(pluginId)) {
                            issues.add(ValidationIssue.warning(pluginId, 
                                ValidationIssue.IssueType.INCONSISTENT_CHILD_PARENT, 
                                String.format("Plugin '%s' requires '%s' as parent, but '%s' does not allow '%s' as child", 
                                    pluginId, requiredParent, requiredParent, pluginId)));
                        }
                    }
                }
            }
        }
        
        return issues;
    }
    
    /**
     * Logs validation results to the logger.
     */
    public static void logValidationResults(ValidationResult result) {
        if (result.isValid()) {
            LOG.info("Plugin metadata validation passed: {} warnings", result.getWarningCount());
        } else {
            LOG.error("Plugin metadata validation failed: {} errors, {} warnings", 
                result.getErrorCount(), result.getWarningCount());
        }
        
        for (ValidationIssue issue : result.getErrors()) {
            LOG.error("  {}", issue);
        }
        
        for (ValidationIssue issue : result.getWarnings()) {
            LOG.warn("  {}", issue);
        }
    }
}

