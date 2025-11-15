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

### Visual Tile Layout Contract (v2.3.1)

To keep palette, canvas, and property panel perfectly aligned we enforce a shared sizing contract:

1. **Default footprint:** Each `.visual-node` initializes at **320 × 160 px**. That guarantees the semantic metadata row `Children / Containers / Leaves` stays on a single line regardless of locale.
2. **Shared bounds:** The blue selection glow, the `.visual-node` wrapper, and the `.visual-node__surface` inner tile all share the same width/height. That means the highlight you see when clicking **Details** or multi-selecting is *exactly* the card outline—no floating borders.
3. **Resize parity:** Dragging any resize handle updates:
   - the outer shell (hit target, focus halo),
   - the internal white tile (where headers + metadata live),
   - and the port anchor calculations (connection lines exit from the resized perimeter).
4. **Minimum enforcement:** Even if plugins ship smaller metadata defaults, runtime rendering clamps to 320 × 160 px so the metadata pills never wrap mid-word.
5. **Anchor math:** `computeAnchorPoint()` treats the card edges as the source of truth, so start/end markers and manual connections always terminate on real geometry (no more arrows ending inside highlight paddings).

Documenting this contract keeps the CSS, TypeScript renderer, and property-panel selection logic in sync whenever we revisit layout decisions.

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









