# Validation - Current Status

**Date:** October 25, 2025  
**Status:** 🟡 PARTIALLY WORKING (4/9 tests pass)

## Test Results

```
✅ should reject unknown plugin name (4 ms) 
❌ should reject unknown attribute on def plugin
❌ should reject invalid var name (var2 instead of var)
❌ should reject unknown attribute on xpath plugin  
❌ should reject random text outside tags
❌ should reject http-header outside http plugin
✅ should accept http-header inside http plugin (5 ms)
✅ should accept valid simple configuration (4 ms)
✅ should accept valid http configuration (6 ms)
```

## What Works

- ✅ Unknown plugin detection (`<unknown-plugin/>`)
- ✅ Valid configurations pass
- ✅ SchemaValidator compiles and runs
- ✅ Backend integration completed

## What Doesn't Work

- ❌ Unknown attribute detection (`badattr="invalid"`)
- ❌ Invalid attribute names (`var2` instead of `var`)
- ❌ Context-specific plugin validation (`http-header` outside `http`)
- ❌ Random text detection

## Root Cause

`SchemaValidator.getSchemaFromPluginClass()` returns `NULL`:

```java
private PluginConfigurationSchema getSchemaFromPluginClass(Class<? extends Plugin> pluginClass) {
    try {
        Method getSchemaMethod = pluginClass.getMethod("getConfigurationSchema");
        Object schema = getSchemaMethod.invoke(null); // Static method
        if (schema instanceof PluginConfigurationSchema) {
            return (PluginConfigurationSchema) schema;
        }
    } catch (Exception e) {
        LOG.debug("Could not get schema from plugin class: {}", pluginClass.getSimpleName());
    }
    return null; // ← ALWAYS NULL!
}
```

**Problem:** Plugins don't have `getConfigurationSchema()` method, or it's not static, or it doesn't return `PluginConfigurationSchema`.

## Backend Logs

```
INFO (ConfigServlet.java:86) - 🔍 Validating configuration: 154 bytes
INFO (StreamingXmlParser.java:222) - Element 'def' not found in resolver (old *Def system) - using fallback for POC
INFO (ConfigServlet.java:158) - ✅ Configuration validated successfully
```

Schema validation passes because `schema == null`, so attribute validation is skipped!

```java
if (schema == null) {
    continue; // Skip validation if schema not available ← PROBLEM!
}
```

## Next Steps

1. **Find correct way to get plugin schema**
   - Check `DefPlugin` class structure
   - Look for schema access method
   - Check if schemas are stored in registry

2. **Alternative: Hardcode basic schemas**
   - Create static schema definitions for core plugins
   - Map plugin name → expected attributes
   - Quick fix for v2.3.1

3. **Long-term: Enhance plugin architecture**
   - Add schema metadata to all plugins
   - Ensure schemas are accessible via reflection or registry
   - Document schema API for plugin developers

## Files Modified

- `webharvest-ide/src/main/java/org/webharvest/server/validation/SchemaValidator.java` - NEW
- `webharvest-ide/src/main/java/org/webharvest/server/validation/ValidationResult.java` - NEW
- `webharvest-ide/src/main/java/org/webharvest/server/validation/ValidationError.java` - NEW
- `webharvest-ide/src/main/java/org/webharvest/server/api/ConfigServlet.java` - ENHANCED
- `webharvest-ide/frontend/src/__tests__/integration/Validation.integration.test.ts` - NEW (TDD)

## Conclusion

Validation framework is in place but schema retrieval is broken. Need to find correct API for accessing plugin schemas.





