# Missing Features Integration Plan

## Problem Statement

Funkcjonalności opisane w `FINAL_SESSION_SUMMARY_VISUAL_DESIGNER.md` są zaimplementowane w kodzie, ale **NIE SĄ WIDOCZNE w UI** lub **NIE SĄ DOSTĘPNE przez interfejs użytkownika**.

## Status Analysis

### ✅ Funkcje ZAIMPLEMENTOWANE i DZIAŁAJĄCE:
1. **Drag & Move** - `moveNode()`, `handleNodeDragStart/Move` ✅
2. **Zoom & Pan** - `zoomIn()`, `zoomOut()`, `panBy()`, `resetView()` ✅
3. **Auto-Layout** - `autoLayout('grid' | 'vertical' | 'horizontal')` ✅ (kod istnieje)
4. **Manual Connections** - `createManualConnection()`, `wouldCreateCycle()`, `getExecutionOrder()` ✅ (kod istnieje)
5. **Multi-Select** - `selectNodesInRectangle()`, `deleteSelectedNodes()` ✅ (kod istnieje)
6. **Single Config Mode** - `isDirty()`, `markClean()`, `markDirty()`, `getCurrentConfiguration()` ✅
7. **Property Panel** - podstawowa edycja właściwości ✅

### ❌ Funkcje ZAIMPLEMENTOWANE ale NIE DOSTĘPNE w UI:

#### 1. **Keyboard Shortcuts (15+)**
**Kod:** W `VisualDesigner.ts` (linia 900+)
```typescript
✅ ⌘A - Select All (kod istnieje, ale może nie działać)
❌ ⌘D - Duplicate (TODO: clipboard)
❌ ⌫ - Delete Selected (kod istnieje, ale brak handlera)
❌ ⌘Z/⌘⇧Z - Undo/Redo (TODO: skeleton)
❌ ⌘0 - Reset Zoom (kod istnieje, ale brak handlera)
❌ ⌘+/- - Zoom In/Out (kod istnieje, ale brak handlera)
```

**Problem:** Keyboard shortcuts nie są zarejestrowane w `App.ts` lub `index.html`

**Fix:**
- Add keyboard event listeners w `App.ts`
- Register all shortcuts: ⌘A, ⌘D, ⌫, ⌘Z,Tracking ⇧Z, ⌘0, ⌘+/-
- Make sure shortcuts work in Visual mode only

#### 2. **Context Menu**
**Kod:** W `VisualDesigner.ts` (linia 1100+)
```typescript
✅ Delete (⌫) - kod istnieje
❌ Duplicate (⌘D) - TODO: clipboard
❌ Copy/Paste - TODO: skeleton
✅ Edit Properties - kod istnieje
❌ Bring to Front / Send to Back - kod istnieje, ale nie w menu
```

**Problem:** Context menu nie jest wyświetlane na right-click

**Fix:**
- Add right-click handler w `VisualDesigner.ts`
- Show context menu (create DOM element)
- Connect menu actions do existing functions

#### 3. **Auto-Layout UI Controls**
**Kod:** `autoLayout(layoutType)` istnieje ✅

**Problem:** Brak przycisków w toolbar do wywołania auto-layout

**Fix:**
- Add buttons w unified toolbar: "Vertical Layout", "Horizontal Layout", "Grid Layout"
- Connect buttons do `visualDesigner.autoLayout()`
- Show visual feedback (loading spinner podczas layout)

#### 4. **Minimap**
**Kod:** `showMinimap()`, `handleMinimapClick()` istnieją, ale:
```typescript
// TODO: Render minimap (linia 1430)
// TODO: Implement minimap navigation (linia 1445)
```

**Problem:** Minimap nie jest renderowane, tylko placeholder

**Fix:**
- Integrate `Minimap.ts` component do `VisualDesigner.ts`
- Add minimap container w `index.html` (fixed, bottom-right)
- Call `minimap.renderMinimap()` w `render()`
- Add toggle button w toolbar

#### 5. **Manual Connections - Visual UI**
**Kod:** `createManualConnection()`, ports logic istnieje ✅

**Problem:** 
- Brak visual ports na nodes
- Brak drag-from-port functionality
- Brak visual connections (linie między nodes)

**Fix:**
- Integrate `PortSystem.ts` - render ports on nodes
- Integrate `ConnectionManager.ts` - handle dragging connections
- Draw SVG connections between nodes
- Add connection context menu (delete, label)

#### 6. **Multi-Select - Visual Feedback**
**Kod:** `selectNodesInRectangle()` istnieje ✅

**Problem:** 
- Brak marquee rectangle (selection box) podczas drag
- Brak visual feedback dla multi-selected nodes
- Brak toolbar actions dla multi-select (delete all, align, etc.)

**Fix:**
- Draw selection rectangle podczas drag (mousedown + mousemove)
- Highlight selected nodes z border/fill
- Add multi-select toolbar (delete all, duplicate all, align)

#### 7. **Zoom Controls UI**
**Kod:** `zoomIn()`, `zoomOut()`, `resetView()` istnieją ✅

**Problem:** Brak zoom controls w UI (buttons/slider)

**Fix:**
- Add zoom controls w unified toolbar
- Buttons: Zoom In, Zoom Out, Reset (100%), Zoom to Fit
- Zoom slider (25% - 300%)
- Display current zoom % w toolbar

#### 8. **Grid Toggle**
**Kod:** `setGridSnap()`, grid logic istnieje ✅

**Problem:** Brak toggle button dla grid overlay

**Fix:**
- Add "Grid" toggle button w toolbar
- Integrate `GridSystem.ts` - render grid overlay
- Visual feedback: grid visible/not visible

#### 9. **Undo/Redo**
**Kod:** Skeleton istnieje, ale:
```typescript
// TODO: Implement undo stack (linia 1012)
// TODO: Implement redo stack (linia 1020)
```

**Problem:** Undo/Redo nie działa (tylko console.log)

**Fix:**
- Implement undo stack (save state before each operation)
- Implement redo stack
- Connect ⌘Z/⌘⇧Z shortcuts
- Add undo/redo buttons w toolbar (disabled when no history)

#### 10. **Copy/Paste**
**Kod:** Skeleton istnieje, ale:
```typescript
// TODO: Implement clipboard (linia 1166, 1174)
```

**Problem:** Copy/Paste nie działa

**Fix:**
- Implement clipboard using `navigator.clipboard` API
- Serialize selected nodes to JSON
- Paste creates new nodes with unique IDs
- Support ⌘C/⌘V shortcuts

#### 11. **Drag & Drop from Palette - Visual Feedback**
**Kod:** `onAddNode()` działa ✅

**Problem:** Brak visual feedback podczas drag from palette

**Fix:**
- Add drag preview (ghost node)
- Show drop zone highlight na canvas
- Animate node appearance po drop

#### 12. **Node Icons (Font Awesome)**
**Kod:** Używa emoji ❌

**Problem:** Dokument mówi o Font Awesome icons, ale kod używa emoji

**Fix:**
- Integrate `IconLibrary.ts` do `VisualDesigner.ts`
- Replace emoji z Font Awesome icons
- Use category colors dla node borders

#### 13. **Container Rendering**
**Kod:** Podstawowy container rendering ✅, ale brak advanced features

**Problem:** 
- Brak expand/collapse
- Brak aggregate state visualization
- Brak nested rendering (dzieci wewnątrz parenta)

**Fix:**
- Integrate `ContainerRenderer.ts`
- Add expand/collapse button na container headers
- Show aggregate state (all children SUCCESS → container SUCCESS)
- Render children wewnątrz container box

## Implementation Priority

### 🔴 **HIGH PRIORITY** (Core UX):
1. **Keyboard Shortcuts** - podstawowe shortcuts muszą działać
2. **Context Menu** - right-click powinien pokazać menu
3. **Zoom Controls** - zoom buttons/slider w toolbar
4. **Grid Toggle** - pokaż/ukryj grid
5. **Auto-Layout Buttons** - buttons w toolbar

### 🟡 **MEDIUM PRIORITY** (Better UX):
6. **Minimap** - overview navigation
7. **Multi-Select Visual Feedback** - marquee rectangle, highlights
8. **Node Icons (Font Awesome)** - lepszy wygląd
9. **Container Rendering** - expand/collapse, nested view

### 🟢 **LOW PRIORITY** (Nice to have):
10. **Manual Connections UI** - ports, drag connections
11. **Undo/Redo** - history management
12. **Copy/Paste** - clipboard operations
13. **Drag Preview** - visual feedback

## Files to Modify

### 1. `webharvest-ide/frontend/src/core/App.ts`
- Add keyboard event listener registration
- Add toolbar button handlers (auto-layout, zoom, grid)
- Connect shortcuts do VisualDesigner methods

### 2. `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Integrate `Minimap.ts`, `GridSystem.ts`, `IconLibrary.ts`, `ContainerRenderer.ts`
- Add right-click handler dla context menu
- Implement undo/redo stacks
- Implement clipboard (copy/paste)
- Add marquee selection rectangle rendering

### 3. `webharvest-ide/src/main/resources/webapp/index.html`
- Add minimap container (fixed, bottom-right)
- Add zoom controls w unified toolbar
- Add auto-layout buttons w toolbar
- Add grid toggle button

### 4. `webharvest-ide/frontend/src/visual/PropertyPanel.ts`
- Integrate `SchemaEditor.ts` (jeśli nie jest jeszcze zintegrowany)

 converge

## Quick Wins (Can be done in 1-2 hours):

1. **Add Zoom Controls to Toolbar**
   - 5 buttons: Zoom In, Zoom Out, Reset, Zoom to Fit, Zoom Slider
   - Connect do existing `zoomIn()`, `zoomOut()`, `resetView()`

2. **Add Auto-Layout Buttons**
   - 3 buttons: Vertical, Horizontal, Grid
   - Connect do existing `autoLayout()`

3. **Add Grid Toggle**
   - 1 button: Grid On/Off
   - Connect do existing `setGridSnap()`

4. **Register Keyboard Shortcuts**
   - ⌘0 (Reset Zoom)
   - ⌘+/- (Zoom In/Out)
   - ⌘A (Select All)
   - ⌫ (Delete Selected)

5. **Add Context Menu (Basic)**
   - Right-click na node → show menu
   - Actions: Delete, Edit Properties

## Testing Checklist

Po każdym fix, testuj:
- [ ] Keyboard shortcuts działają (sprawdź w Console czy nie ma błędów)
- [ ] Buttons w toolbar działają (sprawdź visual feedback)
- [ ] Context menu pokazuje się na right-click
- [ ] Zoom controls zmieniają zoom (sprawdź czy canvas się zmienia)
- [ ] Grid toggle pokazuje/ukrywa grid overlay
- [ ] Auto-layout reorganizuje nodes (sprawdź pozycje)
- [ ] No errors w Console

## Expected Outcome

Po integracji, użytkownik zobaczy:
- ✅ Zoom controls w toolbar (In, Out, Reset, Fit, Slider)
- ✅ Auto-layout buttons (Vertical, Horizontal, Grid)
- ✅ Grid toggle button
- ✅ Keyboard shortcuts działające (⌘0, ⌘+/-, ⌘A, ⌫)
- ✅ Context menu na right-click
- ✅ Minimap w prawym dolnym rogu
- ✅ Font Awesome icons zamiast emoji
- ✅ Multi-select z visual feedback (marquee rectangle)
- ✅ Container expand/collapse
- ✅ Visual connections między nodes (jeśli manual connections)

