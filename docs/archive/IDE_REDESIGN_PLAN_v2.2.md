# WebHarvest IDE Redesign - Apple HIG + Enterprise Design System

## 🎯 Goals

1. **Modern, Attractive UI** - enterprise.css design system
2. **Apple HIG Compliance** - Document-based model (per-tab state)
3. **Smart Layout** - Editor + sidebar views (variables, logs, results) in single tab
4. **Context Help** - Plugin documentation integration
5. **Example Loader** - Import from docs/web/examples/
6. **Version-aware** - Links to versioned documentation

---

## 🎨 Design System Integration

### **Color Palette (from enterprise.css):**
```css
--purple: #7B1FA2     /* Primary brand */
--magenta: #D81B60    /* Accents */
--yellow: #FFC107     /* Highlights */
--lime: #A4C639       /* Success */
```

### **Components to Reuse:**
- `.site-header` - Unified navigation
- `.code-window` - Editor container styling
- Gradient buttons (`.btn-primary`, `.btn-outline`)
- Tab system styling
- Cards for panels

---

## 📐 New Layout Design (Apple HIG)

### **Structure:**
```
┌─────────────────────────────────────────────────────────┐
│  WebHarvest IDE v2.2              [File] [Examples] [?] │ Header
├─────────────────────────────────────────────────────────┤
│ Tab1.xml × │ Tab2.xml × │ Tab3.xml × │ +               │ Tabs
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────────────────────┬──────────────────────┐ │
│  │                             │  [Editor] [Variables]│ │
│  │                             │  [Logs] [Results]    │ │
│  │                             │                      │ │
│  │     Monaco Editor           │  ┌────────────────┐ │ │
│  │     (XML Config)            │  │                │ │ │
│  │                             │  │  Active Panel  │ │ │
│  │                             │  │  (Variables,   │ │ │
│  │                             │  │   Logs, etc.)  │ │ │
│  │                             │  │                │ │ │
│  │                             │  └────────────────┘ │ │
│  └─────────────────────────────┴──────────────────────┘ │
│                                                          │
│  [▶ Execute] [⏹ Stop] [💾 Save] [📥 Import]            │ Actions
└─────────────────────────────────────────────────────────┘
```

### **Key Innovation:**
- **Split View:** Editor (70%) + Sidebar (30%)
- **Tab Buttons in Sidebar:** Switch between Variables/Logs/Results/Session
- **Per-Tab State:** Each document tab maintains its own execution state
- **Resizable Panels:** Drag to adjust editor/sidebar ratio

---

## 🔧 Technical Implementation

### **1. Per-Tab State (Already Implemented ✅)**
```javascript
tab = {
    id, name, content, modified,
    executionId,
    logs: [],
    results: {},
    sessionInfo: {},
    tokenUsage: {},
    variables: {},  // NEW: Track variables
    activePanel: 'editor'  // NEW: Which sidebar panel is visible
}
```

### **2. Sidebar Panel Switching:**
```javascript
switchSidebarPanel(panelName) {
    // panelName: 'editor', 'variables', 'logs', 'results', 'session'
    tab.activePanel = panelName;
    renderSidebar();
}
```

### **3. Context-Aware Help:**
```javascript
// Detect cursor position in Monaco Editor
editor.onDidChangeCursorPosition((e) => {
    let word = getWordAtPosition(e.position);
    if (isPluginElement(word)) {
        showPluginHelp(word);  // Show tooltip with link to docs
    }
});
```

---

## 🎨 Visual Design Updates

### **Header (Unified with Website):**
```html
<header class="site-header">
    <div class="container">
        <img src="/static/img/logo.png" class="logo" />
        <span class="project-name">WebHarvest IDE</span>
        
        <nav class="ide-nav">
            <button class="nav-btn"><i class="fas fa-folder-open"></i> Open</button>
            <button class="nav-btn"><i class="fas fa-save"></i> Save</button>
            <button class="nav-btn"><i class="fas fa-download"></i> Examples</button>
            <button class="nav-btn"><i class="fas fa-question-circle"></i> Help</button>
        </nav>
    </div>
</header>
```

### **Tab Bar (macOS-style):**
```css
.tab-bar {
    background: linear-gradient(180deg, #f5f5f5, #e8e8e8);
    border-bottom: 1px solid #d0d0d0;
    display: flex;
    padding: 0 var(--spacing-2);
}

.tab {
    padding: var(--spacing-2) var(--spacing-4);
    background: rgba(255,255,255,0.5);
    border-radius: var(--radius-md) var(--radius-md) 0 0;
    margin-right: var(--spacing-1);
    position: relative;
}

.tab.active {
    background: white;
    box-shadow: 0 -2px 8px rgba(0,0,0,0.1);
}

.tab.modified::after {
    content: '●';
    color: var(--magenta);
    position: absolute;
    right: 8px;
    top: 50%;
    transform: translateY(-50%);
}
```

### **Sidebar Panels (Cards):**
```css
.sidebar-panel {
    background: white;
    border-radius: var(--radius-xl);
    padding: var(--spacing-4);
    box-shadow: var(--shadow-lg);
}

.panel-tabs {
    display: flex;
    gap: var(--spacing-2);
    margin-bottom: var(--spacing-4);
}

.panel-tab {
    padding: var(--spacing-2) var(--spacing-3);
    background: var(--gray-100);
    border-radius: var(--radius-md);
    border: none;
    cursor: pointer;
    transition: all 0.2s;
}

.panel-tab.active {
    background: var(--gradient-primary);
    color: white;
}
```

---

## 💡 New Features

### **1. Example Loader Modal:**
```html
<div class="modal" id="examplesModal">
    <div class="modal-content">
        <h2>Load Example Configuration</h2>
        
        <div class="examples-grid">
            <!-- Categories from docs/web/examples/ -->
            <div class="example-card" data-file="simple_test.xml">
                <i class="fas fa-vial"></i>
                <h4>Simple HTTP Test</h4>
                <p>Basic HTTP request</p>
            </div>
            
            <!-- ... more examples ... -->
        </div>
        
        <button class="btn btn-primary" onclick="loadExample()">
            Load Selected
        </button>
    </div>
</div>
```

### **2. Context Help Tooltip:**
```javascript
// When cursor on <http>:
showPluginTooltip({
    element: 'http',
    title: 'HTTP Plugin',
    description: 'HTTP/HTTPS request plugin',
    link: 'https://webharvest.sourceforge.io/plugins/http.html',
    version: '2.2.0'
});
```

### **3. Variable Inspector (Sidebar):**
```html
<div id="variablesPanel" class="sidebar-panel">
    <div class="panel-header">
        <h3>Variables</h3>
        <span class="badge">12</span>
    </div>
    
    <div class="variables-list">
        <div class="variable-item">
            <span class="var-name">productName</span>
            <span class="var-value">MacBook Pro</span>
            <span class="var-type">String</span>
        </div>
        <!-- ... -->
    </div>
</div>
```

---

## 🎯 Implementation Steps

1. **Update IDE CSS** - Import enterprise.css, add IDE-specific overrides
2. **Redesign Layout** - Split view (editor + sidebar)
3. **Sidebar Tabs** - Variables, Logs, Results, Session
4. **Example Loader** - Modal with examples from docs/web/examples/
5. **Context Help** - Monaco tooltip integration
6. **Responsive** - Works on tablets/large phones

---

## 📝 Apple HIG Compliance Checklist

- [x] **Document-based:** Each tab = independent document
- [x] **Per-document state:** Logs/results/variables per tab
- [ ] **Clear visual hierarchy:** Primary (editor) vs secondary (sidebar)
- [ ] **Consistent navigation:** Unified header with website
- [ ] **Progressive disclosure:** Sidebar panels show/hide as needed
- [ ] **Feedback:** Visual indicators for execution state
- [ ] **Undo/Redo:** Monaco editor built-in
- [ ] **Keyboard shortcuts:** Cmd+S (save), Cmd+R (run), etc.

---

Ready to implement?
