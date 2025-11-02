# Reset to Working Visual Designer

## ✅ What WORKS:

### 1. Original IDE: http://localhost:8080/
- Monaco Editor
- Run/Execute
- Logs, Variables, Session
- **Status:** WORKING

### 2. Visual Designer: http://localhost:8080/visual-designer/
- Yellow theme (correct!)
- Font Awesome icons
- Plugin palette
- Canvas layout
- **Status:** WORKING (needs features)

---

## 🎯 Key Requirements (Understood):

### 1. Hierarchy & Containers (Like NiFi Process Groups)
**Container Plugins:**
- `<loop>` - can contain other elements
- `<if>/<else>` - conditional containers
- `<http>` - can contain `<http-header>`, `<http-param>`
- `<function>` - can contain logic
- `<try>/<catch>` - error handling containers

**Visual Representation:**
```
┌─────────────────────┐
│ Loop                │
│ ┌─────────────────┐ │
│ │ If condition    │ │
│ │ ┌─────────────┐ │ │
│ │ │ Http        │ │ │
│ │ │  ├ header   │ │ │
│ │ │  ├ param    │ │ │
│ │ └─────────────┘ │ │
│ └─────────────────┘ │
└─────────────────────┘
```

### 2. Context-Specific Plugins
- `http-header` ONLY inside `<http>`
- `http-param` ONLY inside `<http>`
- `call-param` ONLY inside `<call>`
- `db-param` ONLY inside `<database>`

**Already implemented in backend:**
- ✅ PluginConfigurationSchema.canAppearAtRoot()
- ✅ PluginConfigurationSchema.getRequiredParents()
- ✅ HttpHeaderPlugin returns requiredParents: ["http"]

### 3. Connections (Flow Determinism)
**Sequential Flow:**
```
[def url] → [http] → [html-to-xml] → [xpath] → [file write]
```

**Visual:** Arrow lines showing execution order

### 4. XML ↔ Visual Bidirectional Sync
**Challenge:** Preserve hierarchy when converting

**Example XML:**
```xml
<loop>
    <if>
        <http url="...">
            <http-header name="Auth" value="..."/>
        </http>
    </if>
</loop>
```

**Visual:** Nested boxes with parent-child relationships

---

## 🚀 Implementation Plan (On Working Designer):

### Step 1: Use /visual-designer/ as base
- It already has good layout
- Yellow theme correct
- Plugin palette works

### Step 2: Add Container Support
- Drag plugin → becomes container if schema allows
- Drop zone inside containers
- Visual nesting (indented boxes)

### Step 3: Add Connections
- Click to connect nodes
- Show execution flow
- Respect hierarchy (can't connect child to outside parent)

### Step 4: Property Editing
- Click node → show properties
- Use PropertyPanel.ts (already written!)
- Update node config

### Step 5: XML Sync
- Use XMLParser.ts (already written!)
- Use XMLGenerator.ts (already written!)
- Test roundtrip

---

**Do I continue enhancing /visual-designer/ with these features?**

That's the RIGHT base - it works and looks good! 🎯





