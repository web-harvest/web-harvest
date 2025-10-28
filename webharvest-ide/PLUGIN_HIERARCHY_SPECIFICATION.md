# Plugin Hierarchy Specification

**CRITICAL:** Define exact rules for plugin nesting and context requirements

---

## 🎯 Plugin Classification

### Category 1: Top-Level Plugins (TLD - Can appear at root)
These plugins can appear directly under `<config>`:

**Data Extraction:**
- `http` - HTTP requests
- `html-to-xml` - HTML parsing
- `xpath` - XPath extraction
- `xquery` - XQuery processing
- `regexp` - Regular expressions

**Variables:**
- `def` - Define variable
- `var` - Declare variable
- `set-var` - Set variable value

**Control Flow:**
- `loop` - Iteration
- `while` - Conditional iteration
- `if` - Conditional execution
- `try` - Error handling

**Functions:**
- `function` - Function definition
- `call` - Function call

**File Operations:**
- `file` - File I/O

**Data Processing:**
- `template` - Template processing
- `text` - Text output
- `json-to-xml` - JSON conversion
- `xml-to-json` - XML conversion
- `xslt` - XSLT transformation

**Utility:**
- `script` - JavaScript/Groovy
- `log` - Logging
- `sleep` - Delay
- `exit` - Terminate

**Configuration:**
- `config` - ROOT element (special)

---

### Category 2: Context-Specific Plugins (Must be children of specific parents)

**HTTP Context (ONLY in `<http>`):**
- `http-header` - HTTP request header
  - **MUST be child of:** `http`
  - **CANNOT appear:** anywhere else
  
- `http-param` - HTTP request parameter
  - **MUST be child of:** `http`
  - **CANNOT appear:** anywhere else

**Database Context (ONLY in `<database>`):**
- `db-param` - Database parameter
  - **MUST be child of:** `database`
  - **CANNOT appear:** anywhere else

**XQuery Context (ONLY in `<xquery>`):**
- `xq-param` - XQuery parameter
  - **MUST be child of:** `xquery`
  - **CANNOT appear:** anywhere else
  
- `xq-expression` - XQuery expression
  - **MUST be child of:** `xquery`
  - **CANNOT appear:** anywhere else

**Function Call Context (ONLY in `<call>`):**
- `call-param` - Function call parameter
  - **MUST be child of:** `call`
  - **CANNOT appear:** anywhere else

**Loop Context:**
- `body` - Loop body (special element)
  - **MUST be child of:** `loop`
  - **CANNOT appear:** anywhere else

**Conditional Context:**
- `else` - Else branch
  - **MUST be child of:** `if`
  - **CANNOT appear:** anywhere else

- `catch` - Exception handler
  - **MUST be child of:** `try`
  - **CANNOT appear:** anywhere else

**Function Context:**
- `return` - Return value
  - **MUST be child of:** `function`
  - **CANNOT appear:** anywhere else

---

### Category 3: Universal Plugins (Can appear anywhere)
These can be children of ANY container:

- `get` - Get variable value
- `constant` - Constant value
- `empty` - Empty value
- `list` - List literal
- All data extraction plugins (when nested)
- All data processing plugins (when nested)

---

## 🏗️ Extended PluginMetadata Schema

### Current Schema (Insufficient):
```java
interface PluginConfigurationSchema {
  String[] getAllowedSubElements();
  String[] getRequiredSubElements();
  AttributeDefinition[] getAttributeDefinitions();
  boolean allowsBody();
}
```

### REQUIRED Extension:
```java
interface PluginConfigurationSchema {
  // Existing
  String[] getAllowedSubElements();      // What children this can have
  String[] getRequiredSubElements();     // Required children
  AttributeDefinition[] getAttributeDefinitions();
  boolean allowsBody();
  
  // NEW: Context requirements
  boolean canAppearAtRoot();             // Can be top-level?
  String[] getRequiredParents();         // MUST be child of these (empty = universal)
  boolean isUniversal();                 // Can appear anywhere?
  
  // NEW: Nesting constraints
  int getMaxNestingDepth();              // Max levels (0 = leaf, -1 = unlimited)
  boolean canContainSelf();              // Can contain same type? (e.g., if inside if)
  
  // NEW: Ordering constraints
  boolean preserveChildOrder();          // Does child order matter? (default: true)
  String[] getPreferredChildOrder();     // Suggested order for children
}
```

---

## 📋 Implementation Plan

### Phase 1: Core Enhancement (Java)

**File:** `webharvest-core/src/main/java/org/webharvest/plugin/PluginConfigurationSchema.java`

Add methods:
```java
/**
 * Can this plugin appear at root level (directly under <config>)?
 * @return true if can be root element, false if must be nested
 */
default boolean canAppearAtRoot() {
    return true;  // Most plugins can be at root
}

/**
 * Get list of required parent plugins.
 * Empty array means can appear anywhere.
 * Non-empty means MUST be child of one of these parents.
 */
default String[] getRequiredParents() {
    return new String[0];  // Default: universal
}

/**
 * Is this a universal plugin that can appear in any context?
 */
default boolean isUniversal() {
    return getRequiredParents().length == 0 && canAppearAtRoot();
}
```

### Phase 2: Plugin Annotations

**Example:** HttpHeaderPlugin
```java
@CorePlugin(
    name = "http-header",
    requiredParents = {"http"},  // NEW annotation
    canAppearAtRoot = false       // NEW annotation
)
public class HttpHeaderPlugin extends AbstractPlugin {
    @Override
    public PluginConfigurationSchema getSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public String[] getRequiredParents() {
                return new String[]{"http"};  // CRITICAL!
            }
            
            @Override
            public boolean canAppearAtRoot() {
                return false;  // CRITICAL!
            }
        };
    }
}
```

### Phase 3: Metadata Servlet Enhancement

**File:** `PluginMetadataServlet.java`

Update JSON response:
```json
{
  "elementName": "http-header",
  "name": "HTTP Header",
  "schema": {
    "attributes": [...],
    "allowedSubElements": [],
    "requiredSubElements": [],
    
    "canAppearAtRoot": false,
    "requiredParents": ["http"],
    "isUniversal": false,
    "maxNestingDepth": 0
  }
}
```

### Phase 4: Frontend Validation

**File:** `ContainerManager.ts`

```typescript
/**
 * Validate if plugin can be dropped at location
 */
public canDropPlugin(
  pluginType: string, 
  targetContainerId: string | null
): ValidationResult {
  const plugin = getPlugin(pluginType);
  
  // Check if requires parent
  if (plugin.schema.requiredParents.length > 0) {
    if (!targetContainerId) {
      return {
        valid: false,
        error: `${plugin.name} must be inside: ${plugin.schema.requiredParents.join(', ')}`
      };
    }
    
    const container = getNode(targetContainerId);
    if (!plugin.schema.requiredParents.includes(container.type)) {
      return {
        valid: false,
        error: `${plugin.name} can only be inside: ${plugin.schema.requiredParents.join(', ')}`
      };
    }
  }
  
  // Check if can appear at root
  if (!targetContainerId && !plugin.schema.canAppearAtRoot) {
    return {
      valid: false,
      error: `${plugin.name} cannot appear at root level`
    };
  }
  
  return { valid: true };
}
```

---

## 🧪 Test Matrix

### Test Case Matrix:

| Plugin | Root OK? | Required Parent | Test Scenario |
|--------|----------|-----------------|---------------|
| http | ✅ YES | none | Drop on canvas → ✅ |
| http-header | ❌ NO | http | Drop on canvas → ❌ "Must be inside http" |
| http-header | ❌ NO | http | Drop on canvas → ❌ |
| http-header | ✅ YES | http | Drop into http → ✅ |
| http-header | ❌ NO | http | Drop into loop → ❌ "Must be inside http" |
| xpath | ✅ YES | none | Drop on canvas → ✅ |
| xpath | ✅ YES | none | Drop into loop → ✅ |
| xq-param | ❌ NO | xquery | Drop on canvas → ❌ |
| xq-param | ✅ YES | xquery | Drop into xquery → ✅ |

### Validation Messages:

```typescript
// Good error messages
"http-header must be inside: http"
"xq-param can only appear inside xquery"
"http-header cannot appear at root level"
"loop cannot contain http-header (only allowed in http)"
```

---

## 📚 Documentation Requirements

### 1. Plugin Reference (for each plugin)

**Example: http-header**
```markdown
## http-header

**Context:** HTTP Request Configuration  
**Required Parent:** `<http>`  
**Can Appear at Root:** No  
**Max Instances:** Unlimited

### Description
Adds a custom HTTP header to the HTTP request.

### Parent Context
This plugin MUST appear inside `<http>` element.

### Example
```xml
<http url="https://api.example.com">
  <http-header name="Authorization" value="Bearer token"/>
  <http-header name="Content-Type" value="application/json"/>
</http>
```

### Invalid Usage
```xml
<!-- ❌ ERROR: http-header cannot appear at root -->
<http-header name="Test" value="value"/>

<!-- ❌ ERROR: http-header can only be in http, not loop -->
<loop>
  <http-header name="Test" value="value"/>
</loop>
```
```

### 2. Visual Designer Documentation

**Section: "Plugin Contexts and Nesting Rules"**

```markdown
## Nesting Rules

### Top-Level Plugins
These can be dragged directly onto the canvas:
- http, xpath, def, loop, if, function, etc.

### Context-Specific Plugins
These appear in the palette but CANNOT be dragged to canvas.
They can ONLY be dropped into their required parent:

**HTTP Context:**
- http-header → Can only be dropped into http node
- http-param → Can only be dropped into http node

**XQuery Context:**
- xq-param → Can only be dropped into xquery node
- xq-expression → Can only be dropped into xquery node

### Visual Indicators
- 🔒 Locked icon = Context-specific (cannot drop on canvas)
- 🌐 Universal icon = Can be dropped anywhere
- Container icon = Can contain children
```

---

## 🚀 Implementation Action Items

### Priority 1: Core Schema Extension (2-3 hours)
1. Update `PluginConfigurationSchema` interface
2. Implement in all plugins
3. Document each plugin's context requirements
4. Unit tests for schema

### Priority 2: Backend API Update (1-2 hours)
1. Update `PluginMetadataServlet` to include new fields
2. Test API response
3. Verify all 29 plugins have correct metadata

### Priority 3: Frontend Validation (2-3 hours)
1. Update TypeScript types
2. Implement comprehensive validation
3. Show clear error messages
4. Visual indicators in palette (lock icons)

### Priority 4: Testing (3-4 hours)
1. Unit tests for each plugin type
2. Integration tests for nesting
3. UI tests for drag & drop validation
4. Error message tests

### Priority 5: Documentation (2-3 hours)
1. Document each plugin's context
2. Create nesting rules guide
3. Add examples for each scenario
4. Visual designer user guide

---

## 🎯 Immediate Next Steps

**I will now:**
1. ✅ Read existing PluginConfigurationSchema in core
2. ✅ Design extended schema
3. ✅ Implement in backend (PluginMetadataServlet)
4. ✅ Update frontend types
5. ✅ Create comprehensive test suite
6. ✅ Document thoroughly

**This is CRITICAL foundation - nothing else matters until this is solid!** 🎯

Czy mam kontynuować z tą szczegółową implementacją? To zajmie 10-15 godzin ale będzie fundamentem całego systemu.


