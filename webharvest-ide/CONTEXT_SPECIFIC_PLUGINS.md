# Context-Specific Plugins - Complete List

**Updated:** October 24, 2025  
**Status:** Schema extended in core with canAppearAtRoot() and getRequiredParents()

---

## 🔒 Context-Specific Plugins (MUST have parent)

### HTTP Context
**Parent:** `<http>`

1. **http-header** ✅ IMPLEMENTED
   - Required Parent: `http`
   - Can Appear at Root: `false`
   - Purpose: Add HTTP request headers
   
2. **http-param** ✅ IMPLEMENTED
   - Required Parent: `http`
   - Can Appear at Root: `false`
   - Purpose: Add HTTP parameters

### Database Context  
**Parent:** `<database>` or `<connection>`

3. **db-param** ⏳ TODO
   - Required Parent: `database`, `connection`
   - Can Appear at Root: `false`
   - Purpose: Database query parameters

### XQuery Context
**Parent:** `<xquery>`

4. **xq-param** ⏳ TODO
   - Required Parent: `xquery`
   - Can Appear at Root: `false`
   - Purpose: XQuery external variables

5. **xq-expression** ⏳ TODO
   - Required Parent: `xquery`
   - Can Appear at Root: `false`
   - Purpose: XQuery expression code

### Function Call Context
**Parent:** `<call>`

6. **call-param** ⏳ TODO
   - Required Parent: `call`
   - Can Appear at Root: `false`
   - Purpose: Function call parameters

### Loop Context
**Parent:** `<loop>`

7. **body** ⏳ TODO
   - Required Parent: `loop`
   - Can Appear at Root: `false`
   - Purpose: Loop body content

### Conditional Context
**Parent:** `<if>`

8. **else** ⏳ TODO  
   - Required Parent: `if`
   - Can Appear at Root: `false`
   - Purpose: Else branch

### Exception Context
**Parent:** `<try>`

9. **catch** ⏳ TODO
   - Required Parent: `try`
   - Can Appear at Root: `false`
   - Purpose: Exception handler

### Function Context
**Parent:** `<function>`

10. **return** ⏳ TODO
    - Required Parent: `function`
    - Can Appear at Root: `false`
    - Purpose: Function return value

### Regexp Context
**Parent:** `<regexp>`

11. **regexp-pattern** ⏳ TODO
    - Required Parent: `regexp`
    - Can Appear at Root: `false`

12. **regexp-source** ⏳ TODO
    - Required Parent: `regexp`
    - Can Appear at Root: `false`

13. **regexp-result** ⏳ TODO
    - Required Parent: `regexp`
    - Can Appear at Root: `false`

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

| Plugin | Schema Extended | Tested | Visual Designer Ready |
|--------|----------------|--------|----------------------|
| http-header | ✅ YES | ⏳ TODO | ⏳ TODO |
| http-param | ✅ YES | ⏳ TODO | ⏳ TODO |
| db-param | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| xq-param | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| xq-expression | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| call-param | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| body (loop) | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| else | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| catch | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| return | ⏳ TODO | ⏳ TODO | ⏳ TODO |
| regexp-* | ⏳ TODO | ⏳ TODO | ⏳ TODO |

---

## 🔄 Next Steps

1. ✅ Extend remaining context-specific plugins (10-12 more)
2. ✅ Update PluginMetadataServlet to expose new fields
3. ✅ Update frontend types
4. ✅ Implement validation in ContainerManager
5. ✅ Add visual indicators in palette (lock icons)
6. ✅ Write comprehensive tests
7. ✅ Document in user guide

**Estimated Time:** 6-8 hours for complete implementation + testing

---

**Kontynuować z rozszerzeniem wszystkich context-specific plugins?** 🎯


