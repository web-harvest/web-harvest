# Unified IDE Architecture - Professional Redesign

**Goal:** Merge two applications into ONE unified interface  
**Approach:** Apple HIG compliant, professional UX  
**Priority:** Stabilize existing features, then add new ones

---

## 🎯 Architecture Decision

### ❌ WRONG (Current):
- iframe embedding Visual Designer
- Two separate applications
- Duplicate headers, toolbars, status bars
- Poor UX

### ✅ CORRECT (Target):
- Single unified application
- Shared components (header, tabs, status)
- Mode-specific components (palette, property panel)
- Smooth transitions

---

## 📐 Layout Structure

### Shared Components (Always Visible):
1. **Header** - Logo, project name, main actions
2. **Tab Bar** - Multi-document interface
3. **Toolbar** - Mode switcher + context actions
4. **Status Bar** - Cursor position, stats, errors

### Mode-Specific Components:

#### XML Mode Layout:
```
┌──────────────────────────┬─────────────────┐
│                          │ Logs            │
│  Monaco Editor           ├─────────────────┤
│  (70%)                   │ Results         │
│                          ├─────────────────┤
│                          │ Variables       │
│                          ├─────────────────┤
│                          │ Session         │
│  (30%)                   │                 │
└──────────────────────────┴─────────────────┘
```

#### Visual Mode Layout:
```
┌──────┬────────────────────┬────────────────┐
│Plugin│                    │ Properties     │
│Pale- │  Visual Canvas     │ Panel          │
│tte   │  (Drag & Drop)     │                │
│      │                    │ Attributes     │
│20%   │      60%           │ Body           │
│      │                    │ Children       │
│      │                    │ 20%            │
└──────┴────────────────────┴────────────────┘
```

---

## 🔧 Implementation Strategy

### Phase 1: Cleanup (Remove iframe approach)
- ✅ Remove iframe embedding
- ✅ Keep only direct component integration

### Phase 2: Component Mapping

| Component | XML Mode | Visual Mode |
|-----------|----------|-------------|
| Header | Shared | Shared |
| Tabs | Shared | Shared |
| Toolbar | Run, Validate | Run, Validate, Auto-layout |
| Left Area | - | Plugin Palette |
| Center Area | Monaco Editor | Visual Canvas |
| Right Area | Logs/Variables/Session | Property Panel |
| Status Bar | Shared | Shared |

### Phase 3: CSS Integration
- Merge `visual-designer.css` into main IDE styles
- Use CSS Grid for responsive layout
- Show/hide components based on viewMode

### Phase 4: JavaScript Integration
- Load Visual Designer TypeScript as ES modules
- No iframe communication needed
- Direct function calls

---

## 📋 Implementation Steps

### Step 1: Clean HTML Structure ✅
```html
<div class="ide-main">
  <!-- Left Sidebar (Visual mode only) -->
  <div id="visual-palette-area" style="display: none;">
    <!-- Plugin palette -->
  </div>
  
  <!-- Center: Editor Area -->
  <div class="editor-section">
    <!-- Monaco OR Visual Canvas -->
    <div id="editor"></div>
    <div id="visual-canvas" style="display: none;"></div>
  </div>
  
  <!-- Right Sidebar (mode-dependent) -->
  <div class="sidebar-section">
    <!-- XML mode: Logs/Variables -->
    <div id="xml-sidebar"></div>
    <!-- Visual mode: Property Panel -->
    <div id="visual-properties" style="display: none;"></div>
  </div>
</div>
```

### Step 2: Unified CSS
- Import visual-designer.css styles
- Adapt to fit main IDE layout
- Remove duplicate header/tab styles

### Step 3: Mode Switching Logic
```javascript
switchViewMode(mode) {
  if (mode === 'visual') {
    // Hide Monaco + XML sidebar
    document.getElementById('editor').style.display = 'none';
    document.getElementById('xml-sidebar').style.display = 'none';
    
    // Show Visual components
    document.getElementById('visual-palette-area').style.display = 'block';
    document.getElementById('visual-canvas').style.display = 'block';
    document.getElementById('visual-properties').style.display = 'block';
    
    // Load Visual Designer components (if not loaded)
    if (!this.visualDesignerLoaded) {
      this.initVisualDesigner();
    }
  } else {
    // Reverse: show Monaco, hide Visual
  }
}
```

### Step 4: Visual Designer Integration
- Load VisualDesigner.ts as ES module
- Initialize in existing DOM (no iframe!)
- Reuse existing plugin API, WebSocket, etc.

---

## ✅ Benefits of This Approach

1. **Single Application** - One header, one navigation
2. **Shared State** - Tabs, execution, logs all unified
3. **Better Performance** - No iframe overhead
4. **Consistent UX** - Apple HIG compliant
5. **Maintainable** - One codebase, clear structure

---

## 🚀 Starting Implementation NOW

**Step-by-step, professional approach:**
1. Remove iframe
2. Create proper HTML layout
3. Integrate CSS
4. Load TypeScript modules
5. Test thoroughly

**Estimated time:** 3-4 hours for complete integration

---

**Zaczynam implementację!** 🎯


