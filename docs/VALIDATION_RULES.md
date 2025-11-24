# Configuration Validation Rules

## Overview

This document describes the validation rules applied to WebHarvest XML configurations, including context-specific plugin requirements, attribute validation, and special cases.

## Table of Contents

1. [Context-Specific Plugins](#context-specific-plugins)
2. [Parent-Child Relationships](#parent-child-relationships)
3. [Special Cases](#special-cases)
4. [Attribute Validation](#attribute-validation)
5. [Implementation Details](#implementation-details)

---

## Context-Specific Plugins

Context-specific plugins **must** appear as children of specific parent elements. The validator checks these relationships to ensure correct XML structure.

### HTTP Context
- **`http-header`**: Must be child of `<http>`
- **`http-param`**: Must be child of `<http>`

### XQuery Context
- **`xq-param`**: Must be child of `<xquery>`
- **`xq-expression`**: Must be child of `<xquery>`

### Function Call Context
- **`call-param`**: Can appear in two contexts:
  1. **Directly in `<call>`**: Defines parameter value to pass to function
  2. **In `<def>` within `<function>`**: Retrieves parameter value from caller

### Conditional Context
- **`else`**: Must be child of `<if>`

### Exception Context
- **`catch`**: Must be child of `<try>`

### Function Context
- **`return`**: Must be child of `<function>`

### Regexp Context
- **`regexp-pattern`**: Must be child of `<regexp>`
- **`regexp-source`**: Must be child of `<regexp>`

---

## Parent-Child Relationships

### Standard Validation

For most context-specific plugins, the validator checks if the plugin's direct parent matches one of the required parents from `PluginConfigurationSchema.getRequiredParents()`.

**Example**:
```java
// http-header requires parent "http"
if (parentPluginName != null && schema.isContextSpecific()) {
    String[] requiredParents = schema.getRequiredParents();
    boolean foundParent = false;
    for (String reqParent : requiredParents) {
        if (reqParent.equals(parentPluginName)) {
            foundParent = true;
            break;
        }
    }
    if (!foundParent) {
        errors.add(new ValidationError(
            "Plugin 'http-header' cannot appear in '" + parentPluginName + 
            "'. Required parent: " + Arrays.toString(requiredParents),
            ...
        ));
    }
}
```

### Special Case: `call-param` in `<def>`

The `<call-param>` plugin has a special validation rule: it can appear in `<def>` **only if** that `<def>` is within a `<function>` context.

**Implementation**:
```java
// Special case: call-param in def is allowed only if def is within function
if (!foundParent && "call-param".equals(pluginName) && "def".equals(parentPluginName)) {
    if (isWithinFunction(element)) {
        foundParent = true;
    }
}

private boolean isWithinFunction(Element element) {
    Node parent = element.getParentNode();
    while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
        Element parentElement = (Element) parent;
        String parentName = parentElement.getLocalName();
        if ("function".equals(parentName)) {
            return true;
        }
        if ("config".equals(parentName)) {
            return false; // Reached root, no function found
        }
        parent = parent.getParentNode();
    }
    return false;
}
```

**Why This Is Needed**:
- `<call-param>` inside `<call>` defines parameter values
- `<call-param>` inside `<def>` within `<function>` retrieves parameter values
- The validator must check the DOM hierarchy to allow this pattern

**Example**:
```xml
<function name="greet">
  <def var="name"><call-param name="name"/></def>  <!-- ✅ Valid -->
  <template>Hello ${name}</template>
</function>

<def var="name"><call-param name="name"/></def>  <!-- ❌ Invalid: not in function -->
```

---

## Special Cases

### Function Definitions

Function definitions (created by `<function>` elements) are stored in the execution context as `EmptyVariable` placeholders. However, they are **automatically filtered out** from the Variables panel in the IDE.

**Implementation**:
```java
// In ExecutionManager.extractVariables()
if (value != null && value instanceof EmptyVariable) {
    if (isFunctionDefinition(configTree, key)) {
        LOG.debug("    → Skipping function definition: {}", key);
        continue; // Don't add to result map
    }
}

private boolean isFunctionDefinition(PluginConfigurationTree configTree, String name) {
    if (configTree == null || name == null || name.isEmpty()) {
        return false;
    }
    return findFunctionInTree(configTree, name) != null;
}
```

**Result**: Function names like `greet`, `myFunction` do not appear in the Variables panel, making it easier to see actual execution results.

---

## Attribute Validation

The validator checks:

1. **Required Attributes**: All attributes marked as `required` in the schema must be present
2. **Unknown Attributes**: Attributes not defined in the schema are flagged as errors
3. **Attribute Types**: Type validation is performed (if type information is available)

**Example**:
```java
// Check for required attributes
String[] requiredAttrs = schema.getRequiredAttributes();
if (requiredAttrs != null) {
    for (String requiredAttr : requiredAttrs) {
        if (!elementAttrs.contains(requiredAttr)) {
            errors.add(new ValidationError(
                "Required attribute '" + requiredAttr + "' missing on plugin '" + pluginName + "'",
                ...
            ));
        }
    }
}
```

---

## Implementation Details

### Schema Retrieval

The validator retrieves plugin schemas by:
1. Getting the plugin class from `CorePluginRegistry`
2. Instantiating the plugin
3. Calling `getMetadata().getConfigurationSchema()`

**Code**:
```java
private PluginConfigurationSchema getSchemaFromPlugin(Class<? extends Plugin> pluginClass) {
    try {
        Plugin plugin = pluginClass.getDeclaredConstructor().newInstance();
        Method getMetadataMethod = pluginClass.getMethod("getMetadata");
        Object metadata = getMetadataMethod.invoke(plugin);
        
        if (metadata instanceof PluginMetadata) {
            PluginMetadata pluginMeta = (PluginMetadata) metadata;
            return pluginMeta.getConfigurationSchema();
        }
    } catch (Exception e) {
        LOG.warn("Could not get schema from plugin: {}", pluginClass.getSimpleName(), e);
    }
    return null;
}
```

### Validation Flow

1. **Parse XML**: Convert XML string to DOM
2. **Validate Root**: Ensure root element is `<config>`
3. **Recursive Validation**: For each element:
   - Check if plugin exists in registry
   - Get plugin schema
   - Validate context (parent requirements)
   - Validate attributes
   - Recursively validate children

### Error Reporting

Validation errors include:
- **Error Message**: Human-readable description
- **Line Number**: XML line where error occurred (if available)
- **Plugin Name**: Name of the plugin with the error
- **Error Type**: Category of error (`INVALID_PARENT`, `MISSING_ATTRIBUTE`, etc.)

---

## Best Practices

1. **Define functions before calling them** (lexical order in XML)
2. **Use correct parent elements** for context-specific plugins
3. **Provide all required attributes** for plugins
4. **Check validation errors** before executing configurations
5. **Use IDE validation** to catch errors early

---

## Related Documentation

- [Function Execution Guide](FUNCTION_EXECUTION.md) - Function definition, calling, and parameters
- [Plugin Hierarchy Specification](../webharvest-ide/PLUGIN_HIERARCHY_SPECIFICATION.md) - Complete plugin hierarchy rules
- [Context-Specific Plugins](../webharvest-ide/CONTEXT_SPECIFIC_PLUGINS.md) - List of all context-specific plugins

---

**Last Updated**: 2025-01-15  
**Version**: 2.3.1

