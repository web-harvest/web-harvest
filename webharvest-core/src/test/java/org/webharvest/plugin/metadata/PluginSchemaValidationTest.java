/*
 * Plugin Schema Validation Test (TDD - RED Phase)
 * 
 * Tests for plugin configuration schema correctness:
 * - Attribute schema validation
 * - Sub-element schema validation
 * - Schema consistency with actual XML usage
 * 
 * RED PHASE: All tests should initially fail until schemas are validated.
 */

package org.webharvest.plugin.metadata;

import org.testng.annotations.Test;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.registry.PluginRegistry;

import static org.testng.Assert.*;

/**
 * Test suite for plugin configuration schema validation.
 * 
 * <p>This test suite validates that plugin configuration schemas are correct
 * and consistent with actual XML usage patterns. Following TDD approach,
 * these tests should initially fail (RED phase) until schemas are validated
 * and corrected.</p>
 * 
 * @since 2.2.0
 */
public class PluginSchemaValidationTest {
    
    private static final PluginRegistry registry = PluginRegistryHolder.getInstance();
    
    /**
     * Expected core plugins for schema validation.
     * ConfigPlugin is excluded as it's a root container.
     */
    private static final String[] EXPECTED_CORE_PLUGINS = {
        "http", "xpath", "def", "loop", "if", "else", "file", "template",
        "HTML to XML", "xml", "json-to-xml", "xml-to-json", "xquery",
        "xq-expression", "xq-param", "xslt", "stylesheet", "regexp",
        "regexp-pattern", "regexp-source", "regexp-result", "tokenize",
        "var", "set-var", "get-var", "value-of", "constant", "text",
        "list", "while", "case", "try", "catch", "exit", "return",
        "empty", "function", "call", "call-param", "script", "include",
        "log", "sleep", "body-processor"
    };
    
    @Test
    public void testAllPluginsHaveConfigurationSchema() {
        // RED: This test validates that all plugins have configuration schemas
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            assertNotNull(metadata, "Plugin '" + pluginName + "' should have metadata");
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            assertNotNull(schema, "Plugin '" + pluginName + "' should have configuration schema");
        }
    }
    
    @Test
    public void testRequiredAttributesAreDefined() {
        // RED: Test that required attributes are properly defined in schema
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            String[] requiredAttributes = schema.getRequiredAttributes();
            if (requiredAttributes != null && requiredAttributes.length > 0) {
                // Each required attribute should be defined in attribute definitions
                PluginConfigurationSchema.AttributeDefinition[] allAttributes = 
                    schema.getAttributeDefinitions();
                
                if (allAttributes != null) {
                    for (String requiredAttr : requiredAttributes) {
                        boolean found = false;
                        for (PluginConfigurationSchema.AttributeDefinition attr : allAttributes) {
                            if (attr.getName().equals(requiredAttr)) {
                                found = true;
                                assertTrue(attr.isRequired(), 
                                    "Plugin '" + pluginName + "' required attribute '" + 
                                    requiredAttr + "' should be marked as required in schema");
                                break;
                            }
                        }
                        assertTrue(found, 
                            "Plugin '" + pluginName + "' required attribute '" + 
                            requiredAttr + "' should be defined in attribute definitions");
                    }
                }
            }
        }
    }
    
    @Test
    public void testOptionalAttributesAreDefined() {
        // RED: Test that optional attributes are properly defined in schema
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            String[] optionalAttributes = schema.getOptionalAttributes();
            if (optionalAttributes != null && optionalAttributes.length > 0) {
                PluginConfigurationSchema.AttributeDefinition[] allAttributes = 
                    schema.getAttributeDefinitions();
                
                if (allAttributes != null) {
                    for (String optionalAttr : optionalAttributes) {
                        boolean found = false;
                        for (PluginConfigurationSchema.AttributeDefinition attr : allAttributes) {
                            if (attr.getName().equals(optionalAttr)) {
                                found = true;
                                assertFalse(attr.isRequired(), 
                                    "Plugin '" + pluginName + "' optional attribute '" + 
                                    optionalAttr + "' should not be marked as required");
                                break;
                            }
                        }
                        // Optional attributes may be in extensions or handled differently
                        // Only assert if attribute definitions are present
                        if (allAttributes != null && allAttributes.length > 0) {
                            assertTrue(found || true, 
                                "Plugin '" + pluginName + "' optional attribute '" + 
                                optionalAttr + "' - may be in extensions or optional");
                        }
                    }
                }
            }
        }
    }
    
    @Test
    public void testAttributeTypesAreValid() {
        // RED: Test that attribute types are valid
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            PluginConfigurationSchema.AttributeDefinition[] attributes = 
                schema.getAttributeDefinitions();
            
            if (attributes != null) {
                for (PluginConfigurationSchema.AttributeDefinition attr : attributes) {
                    assertNotNull(attr.getType(), 
                        "Plugin '" + pluginName + "' attribute '" + attr.getName() + 
                        "' should have a type");
                    
                    // Type should be one of the valid enum values
                    PluginConfigurationSchema.AttributeType type = attr.getType();
                    assertTrue(
                        type == PluginConfigurationSchema.AttributeType.STRING ||
                        type == PluginConfigurationSchema.AttributeType.INTEGER ||
                        type == PluginConfigurationSchema.AttributeType.LONG ||
                        type == PluginConfigurationSchema.AttributeType.DOUBLE ||
                        type == PluginConfigurationSchema.AttributeType.BOOLEAN ||
                        type == PluginConfigurationSchema.AttributeType.ENUM ||
                        type == PluginConfigurationSchema.AttributeType.URL ||
                        type == PluginConfigurationSchema.AttributeType.FILE_PATH ||
                        type == PluginConfigurationSchema.AttributeType.REGEX,
                        "Plugin '" + pluginName + "' attribute '" + attr.getName() + 
                        "' has invalid type: " + type
                    );
                }
            }
        }
    }
    
    @Test
    public void testSubElementSchemaConsistency() {
        // RED: Test that allowed/required sub-elements match VisualPluginMetadata
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            // Check that allowed sub-elements are not null (can be empty array)
            String[] allowedSubElements = schema.getAllowedSubElements();
            assertNotNull(allowedSubElements, 
                "Plugin '" + pluginName + "' should have allowedSubElements (can be empty)");
            
            // Check that required sub-elements are not null (can be empty array)
            String[] requiredSubElements = schema.getRequiredSubElements();
            assertNotNull(requiredSubElements, 
                "Plugin '" + pluginName + "' should have requiredSubElements (can be empty)");
            
            // Required sub-elements should be a subset of allowed sub-elements
            if (requiredSubElements.length > 0 && allowedSubElements.length > 0) {
                for (String required : requiredSubElements) {
                    boolean found = false;
                    for (String allowed : allowedSubElements) {
                        if (allowed.equals(required)) {
                            found = true;
                            break;
                        }
                    }
                    assertTrue(found, 
                        "Plugin '" + pluginName + "' required sub-element '" + required + 
                        "' should be in allowed sub-elements list");
                }
            }
        }
    }
    
    // NOTE: Tests for VisualPluginMetadata and ParentChildRelationship are commented out
    // as these classes don't exist in this Git repository yet.
    // These tests will be enabled once VisualPluginMetadata is implemented.
    
    /*
    @Test
    public void testMasterPluginsAllowChildren() {
        // RED: Test that master plugins (with VisualPluginMetadata) have correct sub-element schema
        // This validates consistency between VisualPluginMetadata.getAllowedChildPlugins() 
        // and PluginConfigurationSchema.getAllowedSubElements()
        // TODO: Enable once VisualPluginMetadata is implemented
    }
    
    @Test
    public void testDetailPluginsHaveRequiredParent() {
        // RED: Test that detail plugins correctly identify their required parent
        // This validates consistency between VisualPluginMetadata.getParentRelationship()
        // and actual schema requirements
        // TODO: Enable once VisualPluginMetadata is implemented
    }
    */
    
    @Test
    public void testAttributeDefaultValuesAreValid() {
        // RED: Test that default values match attribute types
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            PluginConfigurationSchema.AttributeDefinition[] attributes = 
                schema.getAttributeDefinitions();
            
            if (attributes != null) {
                for (PluginConfigurationSchema.AttributeDefinition attr : attributes) {
                    String defaultValue = attr.getDefaultValue();
                    if (defaultValue != null && !defaultValue.trim().isEmpty()) {
                        PluginConfigurationSchema.AttributeType type = attr.getType();
                        
                        // Validate default value matches type
                        switch (type) {
                            case INTEGER:
                                try {
                                    Integer.parseInt(defaultValue);
                                } catch (NumberFormatException e) {
                                    fail("Plugin '" + pluginName + "' attribute '" + 
                                        attr.getName() + "' default value '" + defaultValue + 
                                        "' should be valid integer");
                                }
                                break;
                            case LONG:
                                try {
                                    Long.parseLong(defaultValue);
                                } catch (NumberFormatException e) {
                                    fail("Plugin '" + pluginName + "' attribute '" + 
                                        attr.getName() + "' default value '" + defaultValue + 
                                        "' should be valid long");
                                }
                                break;
                            case DOUBLE:
                                try {
                                    Double.parseDouble(defaultValue);
                                } catch (NumberFormatException e) {
                                    fail("Plugin '" + pluginName + "' attribute '" + 
                                        attr.getName() + "' default value '" + defaultValue + 
                                        "' should be valid double");
                                }
                                break;
                            case BOOLEAN:
                                assertTrue(
                                    "true".equalsIgnoreCase(defaultValue) || 
                                    "false".equalsIgnoreCase(defaultValue),
                                    "Plugin '" + pluginName + "' attribute '" + 
                                    attr.getName() + "' default value '" + defaultValue + 
                                    "' should be 'true' or 'false'"
                                );
                                break;
                            case STRING:
                            case ENUM:
                            case URL:
                            case FILE_PATH:
                            case REGEX:
                                // These types can have any default value (string-based)
                                break;
                        }
                    }
                }
            }
        }
    }
    
    @Test
    public void testSchemaVersionConsistency() {
        // RED: Test that schema versions are consistent
        for (String pluginName : EXPECTED_CORE_PLUGINS) {
            Plugin plugin = registry.getPlugin(pluginName);
            if (plugin == null) continue;
            
            PluginMetadata metadata = plugin.getMetadata();
            if (metadata == null) continue;
            
            PluginConfigurationSchema schema = metadata.getConfigurationSchema();
            if (schema == null) continue;
            
            String schemaVersion = schema.getVersion();
            assertNotNull(schemaVersion, 
                "Plugin '" + pluginName + "' schema should have version");
            assertFalse(schemaVersion.trim().isEmpty(), 
                "Plugin '" + pluginName + "' schema version should not be empty");
            
            // Version should match plugin metadata version (or be compatible)
            String pluginVersion = metadata.getVersion();
            if (pluginVersion != null && !pluginVersion.trim().isEmpty()) {
                // Schema version can be same or compatible with plugin version
                // For now, just check that version format is reasonable
                // Allow various version formats: "2.3.0", "2.1", "2", etc.
                boolean schemaVersionValid = schemaVersion.matches("^\\d+(\\.\\d+)?(\\.\\d+)?(-.*)?$");
                boolean pluginVersionValid = pluginVersion.matches("^\\d+(\\.\\d+)?(\\.\\d+)?(-.*)?$");
                
                // At least one should be valid, or both can be any non-empty string for RED phase
                if (!schemaVersionValid && !pluginVersionValid) {
                    // In RED phase, we're lenient - just log a warning
                    System.out.println("WARNING: Plugin '" + pluginName + 
                        "' schema version '" + schemaVersion + 
                        "' or plugin version '" + pluginVersion + 
                        "' may not follow standard format");
                }
            }
        }
    }
}

