# Visual Designer - Current Status & Action Plan

**Date:** October 24, 2025  
**Current Phase:** 1 (Partial - needs rework)  
**Status:** 🔄 **NEEDS SIGNIFICANT IMPROVEMENTS**

---

## ❌ Current Issues (User Feedback)

### 1. Plugin Properties Not Working
**Problem:** No ability to edit or read plugin properties  
**Root Cause:** Properties should come from plugin metadata schemas  
**Required:**
- Extract `getConfigurationSchema()` from each plugin
- Get `AttributeDefinition[]` with types, required fields, allowed values
- Build dynamic forms from schema
- Validate inputs against schema
- Save configuration back to node

### 2. Look & Feel Not Matching
**Problem:** UI doesn't match website or existing IDE  
**Issues:**
- Logo not used (should use `/docs/web/images/logo.png`)
- Colors don't match (should use enterprise.css exactly)
- Layout feels "student project", not professional
- Missing Font Awesome icons
- No grid/shadows/depth like docs/web

### 3. Plugin Nesting Not Supported
**Problem:** Some plugins have sub-elements (http-header, http-param, list/body, etc.)  
**Examples:**
```xml
<http url="...">
  <http-header name="...">value</http-header>  ← Child plugin
  <http-param name="...">value</http-param>    ← Child plugin
</http>

<loop item="x">
  <list>...</list>    ← Child section
  <body>...</body>    ← Child section
</loop>
```
**Required:** Visual representation of parent-child relationships

### 4. XML ↔ Visual Sync Not Working
**Problem:** No conversion between modes  
**Required:**
- Parse XML to visual model
- Generate XML from visual model
- Validate before switching
- Show errors if invalid
- Bidirectional sync

### 5. No Validation & Error Prevention
**Problem:** Can save/apply invalid configuration  
**Required:**
- Real-time validation
- Prevent invalid connections
- Show error messages
- Block invalid operations

### 6. Canvas Too Basic
**Problem:** Looks unprofessional, lacks features  
**Missing:**
- Professional node styling (like Node-RED/NiFi)
- Connection lines with arrows
- Auto-layout algorithm
- Zoom/pan controls that work
- Mini-map for navigation
- Snap-to-grid
- Selection box
- Context menus

---

## ✅ What Actually Works Now

1. ✅ Build system (Maven + npm integrated)
2. ✅ API endpoint (`/api/plugins/metadata`)
3. ✅ 29 plugins from CorePluginRegistry
4. ✅ Basic drag from palette
5. ✅ Basic node rendering
6. ✅ Node selection

---

## 🎯 Required Actions

### PRIORITY 1: Get Real Plugin Schemas ✅

**Task:** Extract full metadata from plugins

```java
// In PluginMetadataServlet
Plugin plugin = pluginInstance; // Get actual instance
PluginMetadata metadata = plugin.getMetadata();
PluginConfigurationSchema schema = metadata.getConfigurationSchema();
AttributeDefinition[] attrs = schema.getAttributeDefinitions();

for (AttributeDefinition attr : attrs) {
    String name = attr.getName();
    AttributeType type = attr.getType();  // STRING, ENUM, BOOLEAN, etc.
    boolean required = attr.isRequired();
    String defaultValue = attr.getDefaultValue();
    String[] allowedValues = attr.getAllowedValues();  // For ENUM
    Number min = attr.getMinValue();  // For numbers
    Number max = attr.getMaxValue();
    String pattern = attr.getPattern();  // For validation
    String description = attr.getDescription();
}
```

### PRIORITY 2: Match Existing IDE Styling

**Files to analyze:**
- `/docs/web/css/enterprise.css` - Design tokens
- `/webharvest-ide/src/main/resources/webapp/static/css/ide-redesign.css` - IDE styles
- `/webharvest-ide/src/main/resources/webapp/index.html` - Current IDE structure

**Use:**
- Logo from `/docs/web/images/logo.png`
- Same header (`.site-header`)
- Same colors (Purple #7B1FA2, Magenta #D81B60, Yellow #FFC107, Lime #A4C639)
- Font Awesome icons
- Same spacing, shadows, radii

### PRIORITY 3: Property Panel with Editing

**Requirements:**
- Dynamic form generation from `AttributeDefinition[]`
- Input types based on `AttributeType`:
  - STRING → `<input type="text">`
  - ENUM → `<select>` with `getAllowedValues()`
  - BOOLEAN → `<input type="checkbox">`
  - INTEGER/LONG/DOUBLE → `<input type="number">` with min/max
  - URL → `<input type="url">`
  - FILE_PATH → file picker
  - REGEX → text with pattern validation
- Real-time validation
- Apply/Cancel buttons
- Save to node config

### PRIORITY 4: Nested Plugins Support

**Approach:**
- Container nodes (http, loop, try, etc.)
- Child nodes rendered inside parent
- Visual hierarchy (indentation/nesting)
- Or: accordion/sections within node

### PRIORITY 5: XML Converters

**XML → Visual:**
```typescript
parseXML(xml: string): VisualModel {
  1. Parse XML with DOMParser
  2. Traverse tree recursively
  3. Create VisualNode for each element
  4. Extract attributes → node.config
  5. Handle parent-child relationships
  6. Auto-layout nodes
}
```

**Visual → XML:**
```typescript
generateXML(model: VisualModel): string {
  1. Build execution tree from connections
  2. Generate XML recursively
  3. Include attributes from node.config
  4. Include child elements
  5. Format and indent
}
```

### PRIORITY 6: Professional Canvas

**Options:**
1. **Rete.js** - Use properly with all features
2. **React Flow** - If willing to use React
3. **Custom SVG** - Full control but more work

**Features needed:**
- Bezier connection lines
- Arrow indicators
- Port compatibility checking
- Auto-routing (avoid overlaps)
- Grid snapping
- Multi-select
- Undo/redo
- Mini-map

---

## 📋 Revised Implementation Plan

### Phase 1 REWORK (2-3 weeks)

#### Week 1: Foundation Done Right
- [ ] **Proper logo integration** (use real logo.png)
- [ ] **Match IDE styling exactly** (same header, colors, fonts)
- [ ] **Full plugin metadata** (with schemas, attributes, child elements)
- [ ] **Professional palette** (with categories, search, icons)

#### Week 2: Canvas & Property Panels
- [ ] **Rete.js canvas** (proper integration)
- [ ] **Node rendering** (professional styling)
- [ ] **Connection lines** (bezier curves with arrows)
- [ ] **Property panel** (dynamic forms from schema)
- [ ] **Validation** (real-time feedback)

#### Week 3: XML Integration
- [ ] **XML parser** (XML → Visual)
- [ ] **XML generator** (Visual → XML)
- [ ] **View modes** (XML / Split / Visual)
- [ ] **Sync validation** (prevent errors)
- [ ] **Error reporting** (show issues)

---

## 💡 Recommendations

Given the complexity and your feedback, I recommend:

**Option A: Full Professional Implementation** (3-4 weeks)
- Rework everything properly
- Match IDE design exactly
- Full feature set
- Production-ready

**Option B: Incremental Improvements** (1-2 weeks per feature)
- Fix one priority at a time
- Test with users between fixes
- Iterate based on feedback

**Option C: External Library** (1 week)
- Use existing flow editor library (react-flow, butterfly, etc.)
- Customize to WebHarvest needs
- Faster but less control

---

## 🤔 Questions for You

1. **Timeline:** How urgent is this? Should I do quick fixes or proper rewrite?
2. **Scope:** Should I focus on Phase 1 perfection or continue to Phase 2-3?
3. **Technology:** Stick with Rete.js or try React Flow / other?
4. **Testing:** Should I create interactive prototype first for review?

---

## 🚀 Next Steps (Your Choice)

**Option 1: Continue with Fixes**
I'll systematically fix all 6 priorities, testing each one

**Option 2: Create Detailed Spec First**
I'll create mockups/prototypes in HTML first, get approval, then implement

**Option 3: Focus on Core Features**
Skip fancy UI, focus on XML ↔ Visual sync and property editing

---

**Your call - what's the priority?**

---

**Document Version:** 1.0  
**Last Updated:** October 24, 2025  
**Status:** Awaiting Direction






