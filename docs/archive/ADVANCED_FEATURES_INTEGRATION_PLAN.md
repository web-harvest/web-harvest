# Integrate Advanced Visual Designer Features

## Problem Statement

Użytkownik zauważa, że wiele funkcjonalności opisanych w:
- `CONDUCTOR_ANALYSIS_AND_ROADMAP.md`
- `VISUAL_DESIGNER_ARCHITECTURE.md`

NIE są widoczne w IDE pomimo że pliki TypeScript istnieją.

## Status Analysis

### ✅ Zintegrowane i działające:
- `NodeStateRenderer` - w VisualDesigner.ts (linia 35)
- `NodeMetricsOverlay` - w VisualDesigner.ts (linia 36)  
- `ExpressionBuilder` - w PropertyPanel.ts
- `XPathTester` - w PropertyPanel.ts
- `RegexValidator` - w PropertyPanel.ts
- `ExecutionStateManager` - w App.ts (linia 52)

### ❌ Zaimplementowane ale NIE zintegrowane (tylko w testach):
1. `GridSystem` - brak w VisualDesigner
2. `Minimap` - brak w VisualDesigner
3. `LayoutAlgorithms` - brak w VisualDesigner
4. `IconLibrary` - brak użycia (wizualizacja używa emoji)
5. `PortSystem` - brak w VisualDesigner
6. `ConnectionManager` - brak w VisualDesigner
7. `ContainerRenderer` - brak w VisualDesigner (używa prostego renderowania)
8. `BreadcrumbNav` - brak w VisualDesigner
9. `DrillDownManager` - brak w VisualDesigner
10. `AnimationEngine` - brak użycia
11. `BookmarkManager` - brak w VisualDesigner
12. `CommentSystem` - brak w VisualDesigner
13. `AnnotationLayer` - brak w VisualDesigner
14. `WorkflowDocs` - brak w UI
15. `ZoomController` - brak (VisualDesigner ma własny zoom)
16. `CanvasSearch` - brak w VisualDesigner
17. `SchemaEditor` - brak w PropertyPanel (używa prostych inputów)

### Endpointy:
- ✅ Brak nieużywanych endpointów `/visual-designer`
- ✅ Wszystkie endpointy używane: `/api/*`, `/ws/*`

## Implementation Plan

### Phase 1: Core Visual Enhancements (Priority 1)

#### 1.1 GridSystem Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `GridSystem`
- Add `private gridSystem: GridSystem`
- Call `gridSystem.renderGrid(canvasElement)` w `render()`
- Add toggle button w toolbar (w App.ts)
- Enable snap-to-grid w drag handlers

#### 1.2 IconLibrary Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `IconLibrary`
- Replace emoji icons z Font Awesome icons
- Update `renderNode()` aby używało `IconLibrary.getIcon(node.type)`
- Add category colors dla node borders

####  Many ContainerRenderer Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `ContainerRenderer`
- Replace prostego container rendering z `ContainerRenderer.renderContainer()`
- Add expand/collapse functionality
- Show aggregate state (jeśli wszystkie dzieci SUCCESS → container SUCCESS)

### Phase 2: Navigation & Layout (Priority 2)

#### 2.1 Minimap Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `Minimap`
- Add `private minimap: Minimap`
- Create minimap container w index.html (fixed position, bottom-right)
- Call `minimap.renderMinimap()` w `render()`
- Update minimap na node move/zoom

#### 2.2 ZoomController Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `ZoomController`
- Replace własny zoom logic z `ZoomController`
- Add zoom presets UI (25%, 50%, 100%, 150%, 200%)
- Add "Zoom to Fit" button
- Add "Zoom to Selection" button

#### 2.3 LayoutAlgorithms Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `LayoutAlgorithms`
- Add toolbar buttons: "Vertical Layout", "Horizontal Layout", "Grid Layout", "Hierarchical Layout"
- Call appropriate layout algorithm na click
- Save custom positions after auto-layout (user can still drag)

#### 2.4 BreadcrumbNav & DrillDownManager
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `BreadcrumbNav`, `DrillDownManager`
- Add breadcrumb container nad canvas
- Double-click container → enter drill-down mode
- Show breadcrumbs z path do current container
- Click breadcrumb → navigate up hierarchy

### Phase 3: Connections & Ports (Priority 3)

#### 3.1 PortSystem Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `PortSystem`
- Modify `renderNode()` aby renderowało input/output ports
- Ports renderowane jako małe kółka na bokach nodes
- Visual feedback: hover port → highlight

#### 3.2 ConnectionManager Integration
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `ConnectionManager`
- Replace automatic sequential connections z manual connections
- Drag from output port → input port = tworzy connection
- Visual validation: invalid connections (cycles, type mismatch) = czerwony
- Context menu on connection → add label, condition, delete

### Phase 4: Advanced Features (Priority 4)

#### 4.1 CanvasSearch
**File:** `webharvest-ide/frontend/src/core/App.ts`
- Import `CanvasSearch`
- Add keyboard shortcut Cmd+F (Mac) / Ctrl+F (Windows)
- Show search overlay
- Search by node type, attribute value, body content
- Highlight matching nodes
- Navigate through results (Next/Previous)

#### 4.2 BookmarkManager
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `BookmarkManager`
- Add "Bookmark View" button w toolbar
- Save: current zoom + pan + selected nodes
- Bookmark list w sidebar
- Click bookmark → restore view

#### 4.3 AnimationEngine
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `AnimationEngine`
- Apply animations na node add/remove/move
- Apply animations na state transitions (IDLE → RUNNING → SUCCESS)
- Smooth transitions dla zoom/pan

#### 4.4 CommentSystem & AnnotationLayer
**File:** `webharvest-ide/frontend/src/visual/VisualDesigner.ts`
- Import `CommentSystem`, `AnnotationLayer`
- Right-click node → "Add Comment"
- Comment tooltip on hover
- Annotation tools: text, boxes, arrows, sticky notes
- Toggle annotations visibility

#### 4.5 WorkflowDocs Generator
**File:** `webharvest-ide/frontend/src/core/App.ts`
- Import `WorkflowDocs`
- Add "Generate Documentation" button w toolbar
- Generate markdown/HTML z workflow structure
- Export diagram jako SVG/PNG
- Include node descriptions, connections, variables

### Phase 5: Property Panel Enhancements

#### 5.1 SchemaEditor Integration
**File:** `webharvest-ide/frontend/src/visual/PropertyPanel.ts`
- Import `SchemaEditor`
- Replace prostych inputów z `SchemaEditor.generateEditor()`
- Use plugin schemas z API (`/api/plugins/metadata`)
- Auto-generate field types (URL, number, boolean, enum)
- Show validation errors in real-time
- Show required fields z asterisk (*)

## Files to Modify

### Core Files:
1. `webharvest-ide/frontend/src/visual/VisualDesigner.ts` - główna integracja
2. `webharvest-ide/frontend/src/core/App.ts` - event handlers, shortcuts
3. `webharvest-ide/frontend/src/visual/PropertyPanel.ts` - SchemaEditor
4. `webharvest-ide/src/main/resources/webapp/index.html` - UI containers (minimap, breadcrumbs, zoom controls)

### New UI Elements Needed:
- Minimap container (fixed, bottom-right)
- Breadcrumb container (fixed, top canvas)
- Zoom controls toolbar (presets, zoom-to-fit, zoom-to-selection)
- Layout buttons toolbar (Vertical, Horizontal, Grid, Hierarchical)
- Search overlay modal (Cmd+F)

## Implementation Order (TDD)

1. **Phase 1.1-1.3** (Grid, Icons, Containers) - 2-3 dni
2. **Phase 2.1-2.4** (Navigation) - 2-3 dni
3. **Phase 3.1-3.2** (Connections) - 2-3 dni
4. **Phase 4** (Advanced Features) - 3-4 dni
5. **Phase 5** (Property Panel) - 1-2 dni

**Total: ~12-15 dni robocze**

## Expected Outcome

Po integracji użytkownik zobaczy:
- ✅ Grid overlay z snap-to-grid
- ✅ Font Awesome icons zamiast emoji
- ✅ Minimap w prawym dolnym rogu
- ✅ Zoom controls (presets, zoom-to-fit)
- ✅ Auto-layout buttons
- ✅ Breadcrumbs dla nested containers
- ✅ Ports na nodes dla manual connections
- ✅ Search (Cmd+F) z highlighting
- ✅ Bookmarks dla zapisanych widoków
- ✅ Smooth animations
- ✅ Comments i annotations
- ✅ Workflow docs generator
- ✅ Zaawansowany Property Panel z schema-driven editors

## Testing Strategy

Każda faza:
1. ✅ TDD tests już istnieją w `__tests__/`
2. Integrate component do VisualDesigner
3. Add UI controls w App.ts
4. Test manualnie w przeglądarce
5. Verify nie psuje existing functionality

## Next Steps

1. **Zacznij od Phase 1** - najważniejsze visual enhancements
2. **Integruj stopniowo** - jedna faza na raz, testuj dokładnie
3. **Utrzymaj backward compatibility** - nie psuj existing functionality
4. **User feedback** - testuj z użytkownikiem po każdej fazie

