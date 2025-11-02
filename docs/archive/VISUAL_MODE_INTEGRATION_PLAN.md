# Visual Mode Integration Plan

**Goal:** Add Visual Designer as alternate view mode in EXISTING IDE  
**Approach:** Extend existing IDE, don't replace it!

---

## 🎯 Architecture

### Current IDE (app.js):
```
TabSystem
├─ Tab 1: config.xml
│  ├─ Monaco Editor (XML view)
│  ├─ WebSocket connection
│  ├─ Logs panel
│  ├─ Variables panel
│  └─ Session panel
└─ Tab 2: scraper.xml
   └─ (same structure)
```

### Extended IDE (with Visual mode):
```
TabSystem
├─ Tab 1: config.xml
│  ├─ View Mode Switcher: [XML] [Visual]
│  ├─ IF XML mode:
│  │  └─ Monaco Editor
│  ├─ IF Visual mode:
│  │  └─ Visual Designer Canvas
│  ├─ WebSocket connection (shared!)
│  ├─ Logs panel (shared!)
│  ├─ Variables panel (shared!)
│  └─ Session panel (shared!)
```

---

## 📋 Implementation Steps

### Step 1: Add View Mode to Tab State
**File:** `app.js`

```javascript
// Extend tab object
{
  id: 'tab-1',
  name: 'config.xml',
  content: '<config>...</config>',  // XML content
  viewMode: 'xml',  // NEW: 'xml' or 'visual'
  visualNodes: [],   // NEW: Visual Designer nodes
  modified: false,
  // ... existing fields
}
```

### Step 2: Add View Switcher UI
**File:** `index.html` - Add to editor-toolbar

```html
<div class="view-switcher">
  <button class="btn-view active" data-view="xml">
    <i class="fas fa-code"></i> XML
  </button>
  <button class="btn-view" data-view="visual">
    <i class="fas fa-project-diagram"></i> Visual
  </button>
</div>
```

### Step 3: Conditional Rendering
**File:** `app.js`

```javascript
switchTabView(tabId, viewMode) {
  const tab = this.tabs.find(t => t.id === tabId);
  tab.viewMode = viewMode;
  
  if (viewMode === 'xml') {
    // Show Monaco Editor
    document.getElementById('editor').style.display = 'block';
    document.getElementById('visual-canvas').style.display = 'none';
    
    // If switching from visual, convert nodes to XML
    if (tab.visualNodes.length > 0) {
      const xml = visualDesigner.generateXML(tab.visualNodes);
      tab.content = xml;
      this.editor.setValue(xml);
    }
  } else {
    // Show Visual Designer
    document.getElementById('editor').style.display = 'none';
    document.getElementById('visual-canvas').style.display = 'block';
    
    // Parse XML to visual nodes
    const xml = this.editor.getValue();
    tab.visualNodes = visualDesigner.parseXML(xml);
    visualDesigner.render(tab.visualNodes);
  }
}
```

### Step 4: Share WebSocket Execution
**File:** `app.js`

```javascript
// When Run button clicked
runConfig() {
  const tab = this.getCurrentTab();
  
  // Get XML (from Monaco or generate from Visual)
  let xml;
  if (tab.viewMode === 'xml') {
    xml = this.editor.getValue();
  } else {
    xml = visualDesigner.generateXML(tab.visualNodes);
  }
  
  // Execute via WebSocket (existing code)
  this.executeViaWebSocket(xml);
  
  // If in visual mode, highlight executing nodes
  if (tab.viewMode === 'visual') {
    visualDesigner.startExecutionTracking(this.currentExecutionId);
  }
}
```

### Step 5: Bidirectional Sync
**File:** `VisualDesigner.ts`

```typescript
// When properties change in Visual mode
onNodePropertyChange(nodeId, config) {
  // Update visual node
  updateNode(nodeId, config);
  
  // Generate XML
  const xml = xmlGenerator.generate(getAllNodes());
  
  // Update tab content (so Monaco sees it when switching back)
  app.updateTabContent(app.activeTabId, xml);
}
```

---

## 🎨 UI Layout

### XML Mode (existing):
```
┌─────────────────────────────────────────┐
│ [XML] Visual                            │
├─────────────────────────────────────────┤
│                                         │
│   Monaco Editor                         │
│   (XML code)                            │
│                                         │
└─────────────────────────────────────────┘
```

### Visual Mode (new):
```
┌─────────────────────────────────────────┐
│ XML [Visual]                            │
├──────────┬──────────────────────────────┤
│ Palette  │  Canvas                      │
│          │                              │
│ plugins  │  (nodes)                     │
│          │                              │
└──────────┴──────────────────────────────┘
```

### Right Panel (shared in both modes):
```
┌──────────────┐
│ Logs         │
├──────────────┤
│ Variables    │
├──────────────┤
│ Session      │
└──────────────┘
```

---

## ✅ What Stays the Same

- ✅ Tab system
- ✅ WebSocket execution
- ✅ Logs panel
- ✅ Results panel
- ✅ Variables panel
- ✅ Session panel
- ✅ Save/Load buttons
- ✅ Run/Stop/Pause buttons
- ✅ Header
- ✅ Status bar

---

## 🆕 What's New

- NEW: View mode switcher (XML / Visual)
- NEW: Visual canvas (when Visual mode)
- NEW: Plugin palette (when Visual mode)
- NEW: Property panel (when Visual mode, replaces Results tabs)

---

## 🚀 Implementation Priority

### Phase 1: Basic Integration (2-3 hours)
1. Add viewMode to tab state
2. Add view switcher UI
3. Show/hide Monaco vs Canvas based on mode
4. Basic XML → Visual conversion on switch
5. Test switching back and forth

### Phase 2: Sync & Execution (2-3 hours)
1. Visual → XML generation on mode switch
2. Run button works in both modes
3. Visual mode shows executing node highlight
4. Logs/Variables work same in both modes

### Phase 3: Property Editing (1-2 hours)
1. In Visual mode, right panel shows node properties
2. In XML mode, right panel shows Results/Logs
3. Both modes share state

### Phase 4: Polish (2-3 hours)
1. Smooth transitions
2. State preservation
3. Validation in both modes
4. Error handling

---

**Total:** 7-11 hours for full integration with existing IDE

**Zacznę teraz od Phase 1?** 🎯





