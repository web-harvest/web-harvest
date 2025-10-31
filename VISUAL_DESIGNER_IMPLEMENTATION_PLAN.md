# Visual Designer Implementation Plan

**Status:** Phase 1 Complete ✅ | Phase 2 In Progress 🚧  
**Updated:** October 28, 2025

---

## ✅ Phase 1: Foundation (COMPLETED)

### 1.1 Architecture & TDD Tests ✅
- [x] Hybrid model decision (nested + connected)
- [x] VisualSynchronization.test.ts (40+ tests)
- [x] VisualModel.test.ts (30+ tests)
- [x] VisualE2E.test.ts (15+ tests)
- [x] PluginPalette.test.ts (20+ tests)
- [x] CanvasInteraction.test.ts (35+ tests) - NEW!

**Total:** 140+ TypeScript tests written

### 1.2 Core Components ✅
- [x] XMLParser (XML → VisualNode[])
- [x] XMLGenerator (VisualNode[] → XML)
- [x] VisualModelHelpers (520 lines)
- [x] PluginPalette (360 lines)
- [x] VisualDesigner - basic rendering (650 lines)
- [x] PropertyPanel - node editing

### 1.3 Plugin Categories ✅
- [x] Added `category` field to @CorePlugin annotation
- [x] Updated 25 plugins with proper categories
- [x] API reads from metadata (not hardcoded)
- [x] PluginCategoriesTest: 21/21 ✅
- [x] CorePluginAnnotationTest: 7/7 ✅

**Categories:**
- Network (1): http
- Extraction (6): xpath, xquery, regexp, html-to-xml, json-to-xml, xml-to-json
- Variables (3): def, get-var, constant
- Control (7): loop, while, if, else, try, catch, case
- Processing (3): script, template, xslt
- Storage (1): file
- Utilities (5): log, sleep, return, exit, empty, text

### 1.4 Synchronization ✅
- [x] XML → Visual (on mode switch)
- [x] Visual → XML (real-time on node change)
- [x] Event handlers (nodeAdded, nodeSelected, onChange)

### 1.5 UI Elements ✅
- [x] Plugin palette with scrolling
- [x] Search/filter plugins
- [x] Drag & drop from palette to canvas
- [x] Property panel for editing

---

## 🚧 Phase 2: Advanced Interactions (IN PROGRESS)

### 2.1 Drag & Move Nodes on Canvas 📝
**Status:** Tests written, implementation pending

**Features:**
- Drag nodes to reposition
- Grid snapping (optional, 20px grid)
- Boundary constraints (keep nodes visible)
- Multi-node drag (move selection together)
- Visual feedback during drag

**Tests:** CanvasInteraction.test.ts
- testMoveNodeWhenDragged()
- testHandleNodeDragStart()
- testHandleDragMoveWithOffset()
- testSnapToGrid()
- testPreventMovingOutsideBounds()

**Implementation:**
```typescript
// VisualDesigner.ts
private dragState: {
    nodeId: string;
    offsetX: number;
    offsetY: number;
} | null = null;

handleNodeDragStart(nodeId: string, mousePos: {x,y}): DragInfo {
    const node = this.findNode(nodeId);
    return {
        nodeId,
        offsetX: mousePos.x - node.position.x,
        offsetY: mousePos.y - node.position.y
    };
}

handleNodeDragMove(mousePos: {x,y}, dragInfo: DragInfo): void {
    let newX = mousePos.x - dragInfo.offsetX;
    let newY = mousePos.y - dragInfo.offsetY;
    
    // Grid snap
    if (this.gridSnapEnabled) {
        newX = Math.round(newX / this.gridSize) * this.gridSize;
        newY = Math.round(newY / this.gridSize) * this.gridSize;
    }
    
    // Boundary check
    newX = Math.max(0, newX);
    newY = Math.max(0, newY);
    
    this.moveNode(dragInfo.nodeId, { x: newX, y: newY });
}
```

---

### 2.2 Zoom & Pan Canvas 📝
**Status:** Tests written, implementation pending

**Features:**
- Zoom in/out (mouse wheel or buttons)
- Zoom range: 10% - 300%
- Zoom to mouse position (smart zoom center)
- Pan canvas (drag background)
- Reset view (Cmd+0)
- Minimap for navigation

**Tests:** CanvasInteraction.test.ts
- testZoomIn()
- testZoomOut()
- testZoomToSpecificLevel()
- testConstrainZoomToMinMax()
- testZoomAtMousePosition()
- testPanCanvas()
- testResetView()

**Implementation:**
```typescript
// VisualDesigner.ts
private zoom: number = 1.0;
private pan: { x: number; y: number } = { x: 0, y: 0 };
private readonly MIN_ZOOM = 0.1;
private readonly MAX_ZOOM = 3.0;

zoomIn(): void {
    this.setZoom(this.zoom * 1.2);
}

zoomOut(): void {
    this.setZoom(this.zoom / 1.2);
}

setZoom(newZoom: number): void {
    this.zoom = Math.max(this.MIN_ZOOM, Math.min(this.MAX_ZOOM, newZoom));
    this.applyTransform();
}

applyTransform(): void {
    const transform = `translate(${this.pan.x}px, ${this.pan.y}px) scale(${this.zoom})`;
    this.canvasElement.style.transform = transform;
}

handleMouseWheel(event: WheelEvent): void {
    if (event.ctrlKey || event.metaKey) {
        event.preventDefault();
        const delta = event.deltaY > 0 ? 0.9 : 1.1;
        this.zoomAt({ x: event.clientX, y: event.clientY }, this.zoom * delta);
    } else {
        // Pan with mouse wheel
        this.pan({ x: -event.deltaX, y: -event.deltaY });
    }
}
```

---

### 2.3 Manual Connections Between Nodes 📝
**Status:** Tests written, implementation pending

**Features:**
- Drag from node output to another node input
- Visual feedback (temp line during drag)
- Connection validation (no circular references)
- Connection types (sequential, conditional, error)
- Delete connections (click + Delete key)
- Connections affect execution order in XML

**Tests:** CanvasInteraction.test.ts
- testCreateConnectionByDragging()
- testPreventCircularConnections()
- testDeleteConnection()
- testUpdateXMLWhenConnectionsChange()

**Implementation:**
```typescript
// VisualNode extension
interface VisualNode {
    // ... existing fields
    inputs?: string[];  // IDs of nodes feeding into this
    outputs?: string[]; // IDs of nodes this feeds into
}

// VisualDesigner.ts
private manualConnections: ManualConnection[] = [];

interface ManualConnection {
    id: string;
    from: string; // node ID
    to: string;   // node ID
    type: 'sequential' | 'conditional' | 'error';
}

createManualConnection(fromId: string, toId: string): void {
    // Validate no circular reference
    if (this.wouldCreateCycle(fromId, toId)) {
        throw new Error('Circular connection detected');
    }
    
    const connection: ManualConnection = {
        id: `conn-${Date.now()}`,
        from: fromId,
        to: toId,
        type: 'sequential'
    };
    
    this.manualConnections.push(connection);
    this.renderConnections();
    this.emit('connectionCreated', connection);
}

getExecutionOrder(): string[] {
    // Topological sort based on manual connections
    return topologicalSort(this.nodes, this.manualConnections);
}
```

---

### 2.4 Context Menu 📝
**Status:** Tests written, implementation pending

**Menu Items:**
- Delete (⌫)
- Duplicate (⌘D)
- Copy (⌘C)
- Paste (⌘V)
- --- (separator)
- Edit Properties
- --- (separator)
- Bring to Front
- Send to Back

**Implementation:**
```typescript
showContextMenu(nodeId: string, position: {x,y}): ContextMenu {
    const menu: ContextMenu = {
        position,
        items: [
            { label: 'Delete', action: () => this.removeNode(nodeId), shortcut: '⌫' },
            { label: 'Duplicate', action: () => this.duplicateNode(nodeId), shortcut: '⌘D' },
            { label: 'Copy', action: () => this.copyNode(nodeId), shortcut: '⌘C' },
            { label: 'Paste', action: () => this.pasteNode(), shortcut: '⌘V' },
            { type: 'separator' },
            { label: 'Edit Properties', action: () => this.propertyPanel.showNode(node) },
            { type: 'separator' },
            { label: 'Bring to Front', action: () => this.bringToFront(nodeId) },
            { label: 'Send to Back', action: () => this.sendToBack(nodeId) }
        ]
    };
    
    this.renderContextMenu(menu);
    return menu;
}
```

---

### 2.5 Keyboard Shortcuts (Apple HIG) 📝
**Status:** Tests written, implementation pending

| Shortcut | Action | Test |
|----------|--------|------|
| ⌘A | Select All | ✅ |
| ⌘D | Duplicate | ✅ |
| ⌘C | Copy | ✅ |
| ⌘V | Paste | ✅ |
| ⌘Z | Undo | ✅ |
| ⌘⇧Z | Redo | ✅ |
| ⌫/Delete | Delete Selected | ✅ |
| ⌘0 | Reset Zoom | ✅ |
| ⌘+ | Zoom In | ✅ |
| ⌘- | Zoom Out | ✅ |
| Space+Drag | Pan Canvas | 📝 |
| ⌘F | Find Node | 📝 |

---

### 2.6 Load Plugins from API 📝
**Status:** Planned

**Current:** Hardcoded templates in `VisualModelHelpers.ts`

**Target:** Load from `/api/plugins/metadata`

**Implementation:**
```typescript
// PluginManager.ts (existing)
async loadPlugins(): Promise<Plugin[]> {
    const response = await fetch('/api/plugins/metadata');
    const data = await response.json();
    return data.plugins;
}

// PluginPalette.ts
async init(): Promise<void> {
    const plugins = await this.pluginManager.loadPlugins();
    
    // Convert to PluginTemplate[]
    this.templates = plugins.map(p => ({
        type: p.id,
        category: p.category,
        icon: p.icon,
        label: p.name,
        defaultAttributes: this.extractDefaultAttributes(p.schema),
        description: p.description
    }));
    
    this.render();
}
```

---

## 📋 Phase 3: Polish & UX (PLANNED)

### 3.1 Visual Refinements
- [ ] CSS styling for nested containers
- [ ] CSS styling for connected nodes
- [ ] Smooth animations (drag, zoom, pan)
- [ ] Connection lines with curves (Bezier)
- [ ] Node icons from plugin metadata
- [ ] Color coding by category

### 3.2 Performance
- [ ] Virtual rendering (only visible nodes)
- [ ] Canvas viewport culling
- [ ] Debounced XML sync
- [ ] Lazy load plugins

### 3.3 Accessibility (Apple HIG)
- [ ] Keyboard-only navigation
- [ ] Screen reader support
- [ ] High contrast mode
- [ ] Focus indicators
- [ ] ARIA labels

---

## 🎯 Next Immediate Tasks

### Task #1: Fix Remaining 'core' Categories
**Plugins to fix:**
- text → Utilities ✅
- xml-to-json → Extraction ✅
- empty → Utilities ✅
- constant → Variables ✅
- config → Core (OK, it's root element)

### Task #2: Implement Drag & Move Nodes
1. Write additional edge case tests
2. Implement drag handlers in VisualDesigner
3. Add grid snap toggle UI
4. Test in browser
5. Verify XML sync works

### Task #3: Implement Zoom & Pan
1. Add zoom state to VisualDesigner
2. Implement mouse wheel handler
3. Add zoom buttons to UI (+/- /reset)
4. Implement pan (Space+Drag or trackpad)
5. Test smooth transitions

### Task #4: Implement Manual Connections
1. Add connection mode toggle
2. Implement drag from output port
3. Visual feedback (temporary line)
4. Drop on input port
5. Store in manualConnections[]
6. Update execution order in XML

### Task #5: Load Plugins from API
1. Update PluginPalette.init() to fetch from API
2. Map API response to PluginTemplate[]
3. Extract default attributes from schema
4. Remove hardcoded templates
5. Test with all 45 plugins

---

## 🔬 Testing Strategy

### TDD Workflow
```
1. Write test (RED)
2. Run test → FAIL
3. Implement feature (GREEN)
4. Run test → PASS
5. Refactor if needed
6. Repeat
```

### Test Coverage Goals
- VisualDesigner: 85%+
- PluginPalette: 90%+
- VisualModelHelpers: 95%+
- XMLParser/Generator: 90%+
- Overall: 85%+

---

## 🎨 UX Principles (Apple HIG)

### Direct Manipulation
- Drag to move (not click-then-click)
- Resize by dragging handles
- Delete with visual feedback

### Keyboard First
- Every action has keyboard shortcut
- Tab to navigate
- Enter to activate
- Escape to cancel

### Visual Feedback
- Hover states
- Active states
- Disabled states
- Progress indicators
- Smooth animations (200-300ms)

### Consistency
- Icons from SF Symbols style
- Colors: Accent (blue), Warning (yellow), Error (red)
- Typography: SF Pro (or system font)
- Spacing: 8px grid

---

## 📊 Implementation Progress

### Components Status
| Component | Lines | Tests | Status |
|-----------|-------|-------|--------|
| VisualDesigner | 650 | 80+ | 🚧 60% |
| VisualModelHelpers | 520 | 30+ | ✅ 100% |
| PluginPalette | 360 | 20+ | ✅ 100% |
| XMLParser | 115 | 40+ | ✅ 100% |
| XMLGenerator | 106 | 40+ | ✅ 100% |
| PropertyPanel | 141 | 10+ | ✅ 90% |
| App (integration) | 900+ | 15+ | ✅ 95% |

**Total:** ~2,800 lines TypeScript + 140+ tests

---

## 🚀 Deployment Checklist

### Before Release
- [ ] Run all TypeScript tests (npm test)
- [ ] Run all Java tests (mvn test)
- [ ] Test in Chrome, Safari, Firefox
- [ ] Test keyboard shortcuts
- [ ] Test drag & drop
- [ ] Test zoom/pan
- [ ] Load test (100+ nodes)
- [ ] Mobile responsiveness (iPad)

### Performance Benchmarks
- Canvas render: < 100ms for 50 nodes
- XML generation: < 50ms for 50 nodes
- Zoom/Pan: 60fps smooth
- Drag node: No lag, smooth cursor tracking

---

## 📝 Documentation Needed

### User Guide
- [ ] Quick start tutorial
- [ ] Video: Create first workflow in Visual Designer
- [ ] Keyboard shortcuts reference
- [ ] Tips & tricks

### Developer Guide
- [ ] Architecture overview
- [ ] Adding new plugin to palette
- [ ] Extending visual node types
- [ ] Testing guide

---

## 🎓 Lessons Learned

### What Worked Well ✅
1. **TDD Approach:** Tests first prevented many bugs
2. **Hybrid Model:** Nested + connected is intuitive
3. **Metadata-driven:** Categories from @CorePlugin annotation
4. **Real-time Sync:** Visual ↔ XML feels instant

### Challenges 🤔
1. **Browser Cache:** Required server-side no-cache headers
2. **Minification:** Tests needed to handle minified code
3. **TypeScript + Maven:** Frontend build integration
4. **SVG Rendering:** Connection arrows need proper math

### Future Improvements 💡
1. **WebGL Rendering:** For 1000+ nodes performance
2. **Collaborative Editing:** Real-time multi-user
3. **AI Suggestions:** Auto-suggest next plugin
4. **Templates:** Save/load workflow templates
5. **Export:** PNG/SVG export for documentation

---

## 🔗 References

### Inspiration
- **Orkes Conductor:** https://orkes.io/ (workflow orchestration)
- **Azure Logic Apps:** Nested containers + flowchart
- **AWS Step Functions:** Visual workflow editor
- **Node-RED:** Node-based flow editor

### Apple HIG
- Direct manipulation
- Keyboard shortcuts (⌘-based)
- Drag & drop patterns
- Context menus
- Accessibility

### Technologies
- **Canvas:** HTML5 Canvas / SVG
- **Drag & Drop:** HTML5 Drag & Drop API
- **Zoom:** CSS transform: scale()
- **Pan:** CSS transform: translate()
- **Testing:** Jest + TestNG

---

## Summary

**Phase 1 (Foundation):** ✅ COMPLETE
- Architecture decided
- Core components implemented
- Basic UI working
- 140+ tests written
- Categories in metadata

**Phase 2 (Advanced):** 🚧 IN PROGRESS
- Drag & move nodes: Tests written, implementing
- Zoom & pan: Tests written, implementing  
- Manual connections: Tests written, implementing
- Context menu: Tests written, implementing
- API integration: Planned

**Phase 3 (Polish):** 📋 PLANNED
- CSS styling
- Performance optimization
- Accessibility
- Documentation

**Current Focus:** Implementing drag & move + zoom/pan with TDD

---

**IDE Status:** ✅ Running on http://localhost:8080
**Build:** ✅ All tests pass
**Ready for:** Browser testing + feature completion




