# Old Architecture Analysis - Web-Harvest Core

**Date**: October 9, 2025  
**Purpose**: Identify legacy/deprecated code that can be safely removed  
**Status**: Analysis Complete

---

## Summary

Web-Harvest currently uses **HYBRID ARCHITECTURE**:
- **Old**: XmlNode → *Def classes → WebHarvestPlugin (via @Definition)
- **New**: @CorePlugin → CorePluginDef → PluginProcessorAdapter → AbstractPlugin

Both architectures **COEXIST** and are used together.

---

## Components Analysis

### 1. ✅ **KEEP - Still Used by Both Architectures**

#### *Def Classes (org.webharvest.definition/)
**Status**: ✅ **ACTIVE - Used by XML Parser**

All *Def classes are used by `SAXConfigParser` for XML parsing:
- `HttpDef`, `FileDef`, `ScriptDef`, `XPathDef`, etc.
- Usage: 2-3 references each (parser + factory)
- Purpose: XML → Definition tree conversion
- **Decision**: KEEP - integral to XML parsing

**Why they're needed**:
```
XML Config → SAXConfigParser → *Def objects → DefinitionResolver → Plugins/Processors
```

#### IElementDef Interface
**Status**: ✅ **ACTIVE - Core Interface**

- Used: 34+ times across codebase
- Methods: Some deprecated (`getOperationDefs()`, `getNode()`) but still used
- Purpose: Common interface for element definitions
- **Decision**: KEEP - too deeply integrated

#### XmlNode Class
**Status**: ✅ **ACTIVE - XML Representation**

- Used: 72 times in main code
- Purpose: XML node representation during parsing
- Used by: All *Def classes, parsers, validators
- **Decision**: KEEP - fundamental to XML parsing

---

### 2. ⚠️ **DEPRECATED but Still Used**

#### ScraperContext Deprecated Members

**Fields**:
```java
@Deprecated
private final Map<String, FunctionDef> functionDefs;  // Used: 2 places

@Deprecated  
private ScriptingLanguage scriptingLanguage;  // Used: 8 places

@Deprecated
private String charset;  // Used: several places
```

**Methods**:
```java
@Deprecated void addFunctionDef()      // Used: 2 times
@Deprecated FunctionDef getFunctionDef()  // Used: 2 times
@Deprecated void setScriptingLanguage()  // Used: 8 times
@Deprecated ScriptingLanguage getScriptingLanguage()  // Used: 8 times
```

**Decision**: ⚠️ **KEEP for now** - still used, but candidates for future refactoring

**Future action**: Migrate to new function/scripting system, then remove

---

### 3. ✅ **REMOVED - HttpComponents 4.x Migration**

#### Already Removed (r720)
- ❌ `HttpClientManager` - removed
- ❌ `HttpInfo` class - removed
- ❌ `${http}` variable - removed
- ❌ `HttpModule` (Guice module) - removed
- ❌ Test files using old API - removed (5 files)

**Status**: ✅ Complete

---

### 4. 📋 **Candidates for Cleanup**

#### Empty/Minimal Test Files

Found test files with minimal/no tests:
```bash
$ find src/test -name "*Test.java" -exec sh -c 'count=$(grep -c "@Test" "$1"); [ $count -eq 0 ] && echo "$1: $count tests"' _ {} \;
```

**Recommendation**: Add tests or remove placeholder files

#### TODO Comments

Found 2 actionable TODOs:
1. `AbstractProcessor.java`: "can we remove elementDef parameter?"
2. `AbstractElementDef.java`: "Do we really need createBodyDefs parameter?"

**Recommendation**: Review and refactor if possible

---

## Architecture Migration Status

### Old Architecture (@Definition)
**Status**: ⚠️ **DEPRECATED but FUNCTIONAL**

- Still works for backward compatibility
- Used by: Custom user plugins, some legacy tests
- Components: `WebHarvestPluginDef`, `@Definition` annotation

**Decision**: KEEP for backward compatibility

### New Architecture (@CorePlugin)  
**Status**: ✅ **PRIMARY - Actively Used**

- All 47 core plugins use this
- Components: `AbstractPlugin`, `@CorePlugin`, `CorePluginDef`
- Future: All new development uses this

**Decision**: KEEP and enhance

---

## Recommendations

### Short Term (v2.2.1)

1. ✅ **DONE**: Remove HttpComponents 3.1 remnants (completed in r720)
2. ✅ **DONE**: Document deprecated members
3. ⏳ **DO**: Add comments explaining hybrid architecture

### Medium Term (v2.3.0)

1. Migrate deprecated ScraperContext members to new system
2. Create migration guide for custom plugins
3. Add deprecation warnings in logs

### Long Term (v3.0.0)

1. Remove old @Definition architecture entirely
2. Remove WebHarvestPluginDef and related classes
3. Simplify IElementDef interface (remove deprecated methods)
4. Full migration to new plugin architecture

---

## Code to Document (Not Remove)

### Hybrid Architecture Classes

These classes bridge old and new architectures - **KEEP and DOCUMENT**:

1. **`PluginProcessorAdapter`**
   - Adapts AbstractPlugin (@CorePlugin) to Processor interface
   - Critical for new plugins to work in old execution engine
   - Status: ACTIVE, DO NOT REMOVE

2. **`WebHarvestPluginDef`**
   - Definition class for old @Definition plugins
   - Needed for backward compatibility
   - Status: DEPRECATED but KEEP

3. **`CorePluginWebHarvestPluginDef`**
   - Bridges CorePluginDef to WebHarvestPluginDef
   - Allows new plugins to work with old parser
   - Status: ACTIVE, KEEP

---

## What We Learned

### ❌ Cannot Remove:
- *Def classes - used by XML parser
- IElementDef - core interface, deeply integrated
- XmlNode - fundamental to parsing
- Deprecated ScraperContext members - still actively used

### ✅ Can Document Better:
- Add architecture diagrams
- Explain hybrid system
- Mark clear migration path
- Warn about deprecated usage

### ✅ Already Cleaned:
- HttpComponents 3.1 → 4.x migration complete
- Old HTTP classes removed
- Obsolete tests removed

---

## Conclusion

**Current State**: Hybrid architecture working well
- Old and new systems coexist
- All tests pass (2744/2744)
- Coverage improving (XPath 60%, HtmlToXml 72%)

**Recommendation**: 
- ✅ Keep current hybrid architecture
- ✅ Document it better
- ⏳ Plan gradual migration to pure new architecture in v3.0.0
- ❌ Don't remove *Def classes (breaks XML parsing)

**Next Steps**:
1. Add architecture documentation
2. Continue improving test coverage
3. Mark clear deprecation timeline

---

**Analysis By**: Web-Harvest Team  
**Date**: October 9, 2025  
**Safe to Remove**: 0 files (all deprecated code still used)  
**Needs Documentation**: Hybrid architecture approach


