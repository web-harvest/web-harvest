# Context-Specific Plugins - Complete List

**Updated:** November 1, 2025  
**Status:** ✅ COMPLETE - All context-specific plugins have full semantic metadata in the unified catalog

---

## 🔒 Context-Specific Plugins (MUST have parent)

### HTTP Context
**Parent:** `<http>`

1. **http-header** ✅ IMPLEMENTED
   - Required Parent: `http`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Add HTTP request headers
   
2. **http-param** ✅ IMPLEMENTED
   - Required Parent: `http`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Add HTTP parameters

### XQuery Context
**Parent:** `<xquery>`

3. **xq-param** ✅ IMPLEMENTED
   - Required Parent: `xquery`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: XQuery external variables

4. **xq-expression** ✅ IMPLEMENTED
   - Required Parent: `xquery`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: XQuery expression code

### Function Call Context
**Parent:** `<call>`

5. **call-param** ✅ IMPLEMENTED
   - Required Parent: `call`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Function call parameters

### Conditional Context
**Parent:** `<if>`

6. **else** ✅ IMPLEMENTED
   - Required Parent: `if`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Else branch

### Exception Context
**Parent:** `<try>`

7. **catch** ✅ IMPLEMENTED
   - Required Parent: `try`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Exception handler

### Function Context
**Parent:** `<function>`

8. **return** ✅ IMPLEMENTED
   - Required Parent: `function`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`
   - Purpose: Function return value

### Regexp Context
**Parent:** `<regexp>`

9. **regexp-pattern** ✅ IMPLEMENTED
   - Required Parent: `regexp`
   - Can Appear at Root: `false`
   - Role: `DEPENDENT`

10. **regexp-source** ✅ IMPLEMENTED
    - Required Parent: `regexp`
    - Can Appear at Root: `false`
    - Role: `DEPENDENT`

11. **regexp-result** ✅ IMPLEMENTED
    - Required Parent: `regexp`
    - Can Appear at Root: `false`
    - Role: `DEPENDENT`

### Stylesheet Context
**Parent:** `<xslt>`

12. **stylesheet** ✅ IMPLEMENTED
    - Required Parent: `xslt`
    - Can Appear at Root: `false`
    - Role: `DEPENDENT`
    - Purpose: XSLT stylesheet definition

---

## 🌐 Universal Plugins (Can appear anywhere)

These have `getRequiredParents() = []` and `canAppearAtRoot() = true`:

- http, html-to-xml
- xpath, xquery, regexp (as containers)
- def, var, set-var, get
- loop, while, if, try
- function, call
- json-to-xml, xml-to-json, template, text, xslt, xml
- file
- script, constant, list, log, sleep, exit, empty

---

## 📊 Implementation Status

| Plugin | Metadata Catalog | API Exposed | Documentation | Tests |
|--------|----------------|-------------|---------------|-------|
| http-header | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| http-param | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| xq-param | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| xq-expression | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| call-param | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| else | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| catch | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| return | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| regexp-pattern | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| regexp-source | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| regexp-result | ✅ YES | ✅ YES | ✅ YES | ✅ YES |
| stylesheet | ✅ YES | ✅ YES | ✅ YES | ✅ YES |

---

## 🎉 Completion Summary

**Status:** ✅ ALL COMPLETE (November 1, 2025)

All context-specific plugins now have:
- Complete semantic metadata in `plugin-semantics.json`
- API exposure via `/api/plugins/metadata`
- Full documentation with summaries and URLs
- Automated validation tests in `PluginMetadataCatalogTest`
- Schema version 1.1.0 with `dependencies` array
- JSON Schema validation

---

## 📚 Related Documentation

- **[Plugin Semantic Metadata](../docs/semantic-metadata/README.md)** - Complete metadata model
- **[Plugin Inventory](../docs/semantic-metadata/PLUGIN_INVENTORY.md)** - Full catalog of 55 plugins
- **[Plugin Metadata Schema](../webharvest-core/src/main/resources/metadata/plugin-metadata-schema.json)** - JSON Schema definition





