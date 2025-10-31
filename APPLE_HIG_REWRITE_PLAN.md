# Apple HIG Rewrite Plan - WebHarvest IDE v2.3.1

## Problem Analysis

Current TypeScript App:
- ❌ Monaco Editor nie renderuje się
- ❌ Brak pluginów w palecie
- ❌ Examples modal pusty
- ❌ Visual Mode pusty canvas
- ❌ Przyciski nie działają
- ❌ Brak integracji z istniejącym DOM

**Root Cause:** App.ts tylko tworzy komponenty ale nie renderuje UI ani nie ładuje danych.

---

## Solution: Clean Slate Rewrite

### Apple HIG Principles to Follow:

1. **Clarity** - Clear visual hierarchy, readable text
2. **Deference** - Content over chrome, minimize UI clutter
3. **Depth** - Layered interface, clear navigation
4. **Consistency** - Familiar patterns, predictable behavior
5. **Direct Manipulation** - Visual feedback, drag & drop
6. **Feedback** - Immediate response to actions
7. **Metaphors** - Real-world analogies (canvas, palette)
8. **User Control** - Non-destructive actions, undo support

### Architecture (Clean)

```
App (main orchestrator)
├── UIManager (renders & manages DOM)
│   ├── Header
│   ├── TabBar
│   ├── Toolbar
│   ├── EditorArea (Monaco)
│   ├── VisualCanvas
│   ├── PluginPalette
│   ├── PropertyPanel
│   └── SidebarPanels
├── StateManager (app state)
├── TabManager (tab management)
├── EditorManager (Monaco wrapper)
├── VisualDesigner (canvas logic)
├── PluginManager (plugin data)
├── ExecutionManager (run workflows)
└── WebSocketManager (real-time updates)
```

### Implementation Steps (TDD)

#### Phase 1: UI Foundation (30min)
1. Create UIManager (tests + impl)
   - Render header, tabs, toolbar
   - Show/hide panels
   - Mode switching UI

2. Create StateManager (tests + impl)
   - App state
   - View mode
   - Active tab

#### Phase 2: Core Features (40min)
3. Create EditorManager (tests + impl)
   - Monaco initialization
   - Get/set content
   - Change handlers

4. Create PluginManager (tests + impl)
   - Fetch plugins from API
   - Render palette
   - Category grouping
   - Search/filter

5. Create ExamplesManager (tests + impl)
   - Fetch examples
   - Render modal
   - Load example

#### Phase 3: Visual Designer (30min)
6. Enhance VisualDesigner (tests + impl)
   - Render nodes on canvas
   - Drag & drop from palette
   - Node selection
   - Hierarchical rendering

7. Enhance PropertyPanel (tests + impl)
   - Dynamic form generation
   - Attribute editing
   - Body editing
   - Live updates

#### Phase 4: Integration (20min)
8. Wire all components in App.ts
   - Initialize all managers
   - Setup event flow
   - Mode switching logic
   - Execution flow

9. Test full workflows
   - Load example
   - Edit in Monaco
   - Switch to Visual
   - Edit properties
   - Switch to XML
   - Run execution

---

## Key Improvements

### 1. Separation of Concerns
- **UIManager** = Pure DOM manipulation
- **StateManager** = App state
- **BusinessLogic** = Separate managers

### 2. Apple HIG Compliance
- **Native macOS feel** - toolbars, panels, modals
- **Consistent spacing** - 4px/8px/12px/16px/24px
- **System fonts** - Inter/San Francisco
- **Colors** - Yellow (#FFC107) accent, neutral grays
- **Animations** - Smooth 150ms transitions
- **Feedback** - Immediate visual response

### 3. Better Testing
- Each manager fully tested
- UI components testable
- Integration tests
- E2E workflows

---

## File Structure (New)

```
frontend/src/
├── core/
│   ├── App.ts                 # Main orchestrator
│   ├── StateManager.ts        # App state
│   ├── UIManager.ts           # DOM rendering
│   ├── EditorManager.ts       # Monaco wrapper
│   ├── PluginManager.ts       # Plugin data & palette
│   ├── ExamplesManager.ts     # Examples modal
│   ├── ExecutionManager.ts    # Run workflows
│   ├── TabManager.ts          # ✅ Already done
│   └── WebSocketManager.ts    # ✅ Already done
├── visual/
│   ├── VisualDesigner.ts      # ✅ Already done (enhance)
│   ├── PropertyPanel.ts       # ✅ Already done (enhance)
│   ├── XMLParser.ts           # ✅ Already done
│   └── XMLGenerator.ts        # ✅ Already done
├── panels/
│   ├── LogsPanel.ts           # ✅ Already done
│   ├── ResultsPanel.ts        # ✅ Already done
│   ├── VariablesPanel.ts      # ✅ Already done
│   └── SessionPanel.ts        # ✅ Already done
└── styles/
    └── apple-hig.css          # Apple HIG compliant styles
```

---

## Success Criteria

✅ Monaco Editor loads and works
✅ Examples modal shows list
✅ Can load example
✅ Visual Mode shows plugins in palette
✅ Can drag plugin to canvas
✅ Canvas shows parsed XML nodes
✅ Can edit node properties
✅ Can switch XML ↔ Visual
✅ Can run execution
✅ Logs/Results/Variables update
✅ All buttons work
✅ Follows Apple HIG

---

## Estimated Time

- Phase 1: 30 min
- Phase 2: 40 min
- Phase 3: 30 min
- Phase 4: 20 min

**Total: ~2 hours**

Already done: TabManager, WebSocketManager, Panels (40% of work)

Remaining: ~1.2 hours

---

## Start Now?

Ready to begin clean implementation with Apple HIG guidelines and full TDD.





