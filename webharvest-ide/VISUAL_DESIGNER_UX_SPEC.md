# WebHarvest Visual Designer - UX Specification & Mockups

**Version:** 1.0  
**Date:** October 24, 2025  
**Status:** Design Specification  
**Compliant with:** Apple Human Interface Guidelines

---

## 📋 Table of Contents

1. [Design Philosophy](#design-philosophy)
2. [User Workflows](#user-workflows)
3. [Interface Mockups](#interface-mockups)
4. [Interaction Patterns](#interaction-patterns)
5. [Responsive Design](#responsive-design)
6. [Accessibility](#accessibility)
7. [Keyboard Shortcuts](#keyboard-shortcuts)
8. [Animation & Motion](#animation--motion)

---

## 🎨 Design Philosophy

### Core Principles

**1. Progressive Disclosure**
- Show essential controls immediately
- Advanced features hidden behind "Advanced" sections
- Context-sensitive help appears when needed

**2. Immediate Feedback**
- Real-time validation as user types
- Visual feedback for all actions (drag, drop, connect)
- Status indicators always visible

**3. Consistency**
- Unified design language across all views
- Predictable interaction patterns
- Consistent terminology

**4. Error Prevention**
- Validation before errors occur
- Clear constraints (e.g., connection rules)
- Confirmation for destructive actions

**5. Efficiency**
- Keyboard shortcuts for power users
- Quick access palette (⌘K / Ctrl+K)
- Templates for common patterns

---

## 🎯 User Workflows

### Workflow 1: First-Time User (Creating Simple Scraper)

**Goal:** Extract title from a webpage  
**Time:** < 5 minutes  
**Steps:**

```
1. User opens Visual Designer
   → Sees clean canvas with plugin palette

2. User drags "HTTP" plugin to canvas
   → Node appears with default configuration
   → Property panel opens on right

3. User enters URL in property panel
   → Real-time validation (green checkmark)

4. User drags "HTML to XML" plugin
   → Node appears below HTTP node
   → Auto-suggests connection

5. User connects HTTP output → HTML-to-XML input
   → Connection line appears with animation
   → Flow direction indicated by arrow

6. User drags "XPath" plugin
   → Node appears below HTML-to-XML
   → Auto-connects (smart suggestion)

7. User enters XPath expression: //title/text()
   → Syntax highlighting in input
   → Validation indicator

8. User clicks "Run" in toolbar
   → Execution visualization (nodes light up in sequence)
   → Results appear in right panel
   → Success! Title extracted

9. User switches to "XML View"
   → Sees generated XML configuration
   → Formatted and syntax highlighted
```

**Key UX Features:**
- ✅ Minimal steps (8 actions)
- ✅ Guided by visual cues
- ✅ Auto-suggestions reduce friction
- ✅ Immediate validation feedback
- ✅ Instant execution & results

### Workflow 2: Power User (Complex Pipeline)

**Goal:** Multi-step data extraction with error handling  
**Time:** < 10 minutes  
**Features Used:**

```
1. Quick Add (⌘K)
   → Type "http" → Enter
   → Node added at cursor position

2. Duplicate Node (⌘D)
   → Selects node → ⌘D
   → Creates copy with same config

3. Connection Validation
   → Attempts invalid connection
   → Visual feedback (red line + tooltip)
   → "Cannot connect: type mismatch"

4. Template Body Editor
   → Double-click node
   → Opens inline editor for body content
   → Syntax highlighting for templates

5. Error Handling Pattern
   → Wraps nodes in "Try/Catch"
   → Visual container appears
   → Nested nodes shown inside

6. Execution with Breakpoints
   → Clicks node to add breakpoint
   → Runs configuration
   → Pauses at breakpoint
   → Inspects variables

7. Export to XML
   → ⌘E to export
   → XML copied to clipboard
   → Notification: "Configuration copied"
```

**Key UX Features:**
- ✅ Keyboard-driven workflow
- ✅ Smart defaults
- ✅ Visual grouping (try/catch containers)
- ✅ Inline editing
- ✅ Debugging support

### Workflow 3: Switching Between Views

**Goal:** Edit in XML, view visually, iterate  
**Scenario:** User prefers XML editing but wants visual overview

```
1. User starts in XML mode
   → Writes XML configuration
   → Uses Monaco autocomplete

2. User clicks "Visual View"
   → System parses XML
   → Generates visual layout
   → Shows loading indicator (< 500ms)
   → Canvas appears with nodes

3. User identifies issue in visual
   → Sees missing connection visually
   → Adds connection by dragging

4. User clicks "Sync to XML"
   → XML updates instantly
   → Changed lines highlighted (diff)
   → Cursor jumps to changed section

5. User makes XML edit
   → Changes attribute value
   → Clicks "Sync from XML"
   → Node property updates visually
   → Node briefly highlights (flash)

6. User uses Split View
   → Both views visible side-by-side
   → Edits in XML → sees visual update
   → Clicks node → XML scrolls to definition
```

**Key UX Features:**
- ✅ Bidirectional sync
- ✅ Diff highlighting
- ✅ Synchronized scrolling
- ✅ Flash feedback for changes

---

## 🖼️ Interface Mockups

### Mockup 1: Main Visual Designer (Default View)

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 🌺 WebHarvest Visual Designer                                            │
│                                                                           │
│  [XML View] [Split View] [▣ Visual View]        🔍 Search  ⚙ Settings   │
├────────────┬────────────────────────────────────────────┬────────────────┤
│            │                                            │                │
│ PLUGINS    │                                            │  PROPERTIES    │
│            │                                            │                │
│ 🔍 Search  │                  Canvas                    │  🌐 HTTP       │
│ [_______]  │                                            │  Request       │
│            │                                            │                │
│ ▼ HTTP &   │            ┌────────────────┐             │  URL *         │
│   Web      │            │  🌐 HTTP       │             │  ┌──────────┐  │
│  • http    │            │  GET example..│◄─────────┐  │  │https://  │  │
│  • xpath   │            └────────────────┘          │  │  │example.  │  │
│  • html..  │                    │                   │  │  │com       │  │
│            │                    ▼                   │  │  └──────────┘  │
│ ▼ Control  │            ┌────────────────┐          │  │  ✓ Valid URL   │
│   Flow     │            │  🔄 HTML to    │          │  │                │
│  • if      │            │  XML           │          │  │  Method        │
│  • loop    │            └────────────────┘          │  │  [GET    ▼]   │
│  • while   │                    │                   │  │                │
│  • try     │                    ▼                   │  │  Timeout       │
│            │            ┌────────────────┐          │  │  [30000  ]ms  │
│ ▼ Data     │            │  🔍 XPath      │          │  │                │
│   Extract  │            │  //title       │          │  │  [Apply]      │
│  • xpath   │            └────────────────┘          │  │  [Cancel]     │
│  • regexp  │                    │                   │  │                │
│  • xquery  │                    ▼                   │  │  📄 View      │
│            │            ┌────────────────┐          │  │  Documentation│
│ ▼ Variabl..│            │  📦 Define     │          │  │                │
│            │            │  var="title"   │          │  │                │
│            │            └────────────────┘          │  │                │
│            │                                         │  │                │
│            │                                         │  │                │
│            │  [+] [⊕] [↻] [⚙]      🔍 100%         │  │                │
├────────────┴────────────────────────────────────────┴────────────────────┤
│  ● Ready   │  5 nodes   │  4 connections   │  ✓ Valid                  │
└──────────────────────────────────────────────────────────────────────────┘

Legend:
  [▣ Visual View] - Active button (purple gradient background)
  ● Ready - Status indicator (green dot)
  ✓ Valid - Validation indicator
  🔍 100% - Zoom level
  * - Required field
```

### Mockup 2: Split View (XML + Visual)

```
┌──────────────────────────────────────────────────────────────────────────┐
│ 🌺 WebHarvest Visual Designer                                            │
│                                                                           │
│  [XML View] [▣ Split View] [Visual View]         🔍 Search  ⚙ Settings  │
├────────────────────────────────┬─────────────────────────────────────────┤
│  XML EDITOR                    │  VISUAL CANVAS                          │
│                                │                                         │
│  1  <?xml version="1.0"?>      │         ┌────────────────┐             │
│  2  <config xmlns="http://...">│         │  🌐 HTTP       │             │
│  3    <def var="url">          │         │  GET example.. │◄────┐       │
│  4      https://example.com    │         └────────────────┘     │       │
│  5    </def>                   │                 │               │       │
│  6    <def var="html">         │                 ▼               │       │
│  7      <http url="${url}"/>   │         ┌────────────────┐     │       │
│  8    </def>                   │         │  🔄 HTML to    │     │       │
│  9    <xpath expr="//title">   │         │  XML           │     │       │
│ 10      <html-to-xml>          │         └────────────────┘     │       │
│ 11        <get var="html"/>    │                 │               │       │
│ 12      </html-to-xml>         │                 ▼               │       │
│ 13    </xpath>                 │         ┌────────────────┐     │       │
│ 14  </config>                  │         │  🔍 XPath      │     │       │
│ 15                              │         │  //title       │     │       │
│                                │         └────────────────┘     │       │
│                                │                                         │
│  [↻ Sync from Visual]         │         [↻ Sync to XML]                │
│                                │                                         │
│                                │                                         │
│                                │  [+] [⊕] [↻] [⚙]      🔍 100%         │
├────────────────────────────────┴─────────────────────────────────────────┤
│  ● Ready   │  4 nodes   │  3 connections   │  ✓ Valid                  │
└──────────────────────────────────────────────────────────────────────────┘

Interaction:
  • Click line in XML → corresponding node highlights in visual
  • Click node in visual → XML scrolls to definition
  • Edit in XML → "Sync from Visual" button activates
  • Edit in visual → "Sync to XML" button activates
```

### Mockup 3: Node Detail View (Inline Editing)

```
┌─────────────────────────────────────────┐
│  🌐 HTTP Request                        │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                         │
│  GET https://example.com                │
│  Timeout: 30000ms                       │
│                                         │
│  ╭───────────────────────────────────╮ │
│  │ ○ Input (trigger)                 │ │
│  ╰───────────────────────────────────╯ │
│                                         │
│  ╭───────────────────────────────────╮ │
│  │ ● Output (html)                   │ │
│  ╰───────────────────────────────────╯ │
│                                         │
│  Status: ✓ Configured                  │
└─────────────────────────────────────────┘
      ↓ (Double-click to expand)
┌─────────────────────────────────────────────────────┐
│  🌐 HTTP Request                    [×] [−] [□]    │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│                                                     │
│  URL * (required)                                   │
│  ┌───────────────────────────────────────────────┐ │
│  │ https://example.com                           │ │
│  └───────────────────────────────────────────────┘ │
│  ✓ Valid URL                                        │
│                                                     │
│  Method                                             │
│  ┌─────────┐                                       │
│  │ GET   ▼ │ [POST] [PUT] [DELETE]                │
│  └─────────┘                                       │
│                                                     │
│  Timeout (ms)                                       │
│  ┌───────────────────────────────────────────────┐ │
│  │ 30000                                         │ │
│  └───────────────────────────────────────────────┘ │
│  ℹ️ Request timeout in milliseconds               │
│                                                     │
│  ☐ Follow Redirects                                │
│  ☐ Use Proxy                                        │
│                                                     │
│  ▶ Advanced Options                                │
│                                                     │
│  ╭─────────────────────────────────────────────╮  │
│  │ ○ Input (trigger)                           │  │
│  ╰─────────────────────────────────────────────╯  │
│                                                     │
│  ╭─────────────────────────────────────────────╮  │
│  │ ● Output (html)                             │  │
│  ╰─────────────────────────────────────────────╯  │
│                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  Apply   │  │  Cancel  │  │  📄 Help     │   │
│  └──────────┘  └──────────┘  └──────────────┘   │
└─────────────────────────────────────────────────────┘

States:
  • Collapsed - Shows summary only
  • Expanded - Shows full configuration
  • Inline - Editable within canvas
  • Panel - Editable in property panel (right sidebar)
```

### Mockup 4: Plugin Palette (Searchable)

```
┌────────────────────────┐
│  PLUGINS               │
│                        │
│  🔍 Search plugins     │
│  ┌──────────────────┐ │
│  │ http             │ │
│  └──────────────────┘ │
│                        │
│  ▼ HTTP & Web         │
│    ┌──────────────┐   │
│    │ 🌐 http      │   │  ← Drag handle
│    │ HTTP Request │   │
│    └──────────────┘   │
│    ┌──────────────┐   │
│    │ 🔄 html-to-  │   │
│    │    xml       │   │
│    └──────────────┘   │
│    ┌──────────────┐   │
│    │ 🌍 webbrowse │   │
│    │    r         │   │
│    └──────────────┘   │
│                        │
│  ▼ Data Extraction    │
│    ┌──────────────┐   │
│    │ 🔍 xpath     │   │
│    │ XPath Query  │   │
│    └──────────────┘   │
│    ┌──────────────┐   │
│    │ 📝 regexp    │   │
│    │ Regular Expr │   │
│    └──────────────┘   │
│    ┌──────────────┐   │
│    │ 📊 xquery    │   │
│    │ XQuery       │   │
│    └──────────────┘   │
│                        │
│  ▼ Control Flow       │
│    ┌──────────────┐   │
│    │ 🔀 if        │   │
│    │ Conditional  │   │
│    └──────────────┘   │
│    ┌──────────────┐   │
│    │ 🔁 loop      │   │
│    │ For Loop     │   │
│    └──────────────┘   │
│                        │
│  ▼ Variables (3)      │
│  ▼ Database (2)       │
│  ▼ File Ops (1)       │
│                        │
│  [+ Custom Plugin]    │
└────────────────────────┘

Features:
  • Search filters plugins in real-time
  • Categories collapsible
  • Drag-and-drop to canvas
  • Recently used (at top)
  • Badge count for collapsed categories
```

### Mockup 5: Connection Validation Feedback

```
Valid Connection:
┌────────────┐              ┌────────────┐
│    HTTP    │              │   XPath    │
│            │──────────────▶│            │
│         ●──┘              └──○         │
└────────────┘              └────────────┘
  (Green arrow, smooth animation)

Invalid Connection:
┌────────────┐    ╳╳╳╳╳╳╳╳    ┌────────────┐
│   XPath    │    ╳╳╳╳╳╳╳╳    │    File    │
│            │────╳╳╳╳╳╳╳╳────│            │
│         ●──┘    ╳╳╳╳╳╳╳╳    └──○         │
└────────────┘              └────────────┘
  (Red dashed line + error tooltip)
  
  ┌───────────────────────────────────┐
  │ ⚠️ Cannot connect:                │
  │ XPath output is XML,              │
  │ File expects text input           │
  └───────────────────────────────────┘
```

### Mockup 6: Execution Visualization

```
Before Execution:
┌────────────┐         ┌────────────┐         ┌────────────┐
│    HTTP    │────────▶│ HTML-to-XML│────────▶│   XPath    │
│            │         │            │         │            │
└────────────┘         └────────────┘         └────────────┘
  (All nodes: gray border)

During Execution (HTTP running):
┌────────────┐         ┌────────────┐         ┌────────────┐
│    HTTP    │────────▶│ HTML-to-XML│────────▶│   XPath    │
│ ⏳ Loading │         │            │         │            │
└────────────┘         └────────────┘         └────────────┘
  (HTTP: purple pulsing border)

During Execution (XPath running):
┌────────────┐         ┌────────────┐         ┌────────────┐
│  ✓ HTTP    │────────▶│ ✓ HTML-XML │────────▶│   XPath    │
│            │         │            │         │ ⏳ Running │
└────────────┘         └────────────┘         └────────────┘
  (Completed: green checkmark, Current: purple pulsing)

Completed:
┌────────────┐         ┌────────────┐         ┌────────────┐
│  ✓ HTTP    │────────▶│ ✓ HTML-XML │────────▶│  ✓ XPath   │
│  1.2s      │         │  0.3s      │         │  0.1s      │
└────────────┘         └────────────┘         └────────────┘
  (All: green border + execution time)
```

---

## 🎭 Interaction Patterns

### Drag and Drop

**From Palette to Canvas:**
```
1. User hovers over plugin in palette
   → Cursor: pointer
   → Plugin card: subtle lift (transform: translateY(-2px))

2. User presses mouse down
   → Cursor: grabbing
   → Plugin card: shadow increases

3. User drags
   → Ghost image follows cursor (semi-transparent)
   → Canvas shows drop zones (dotted outline)

4. User drops on valid zone
   → Ghost fades out
   → Node animates in (scale 0→1, 200ms ease-out)
   → Property panel opens automatically

5. User drops on invalid zone
   → Ghost snaps back to palette (elastic animation)
   → Shake animation on ghost
   → Brief red flash on canvas
```

**Node to Node Connection:**
```
1. User hovers over output port
   → Port grows (scale: 1.2)
   → Tooltip: "Drag to connect"

2. User starts drag from output
   → Connection line follows cursor (bezier curve)
   → Compatible input ports highlight (green glow)
   → Incompatible ports gray out

3. User hovers over compatible input
   → Input port pulses (scale animation)
   → Connection line snaps to port (magnetic effect)

4. User releases on compatible input
   → Connection line animates into place (bezier→straight→bezier)
   → Success feedback (green flash on both ports)
   → Connection appears with arrow

5. User releases on incompatible input
   → Connection line retracts (reverse animation)
   → Error tooltip appears
   → Ports return to normal
```

### Selection and Multi-Select

**Single Selection:**
```
Click node → Purple outline (2px) + shadow
Click canvas → Clear selection
```

**Multi-Selection:**
```
1. Shift+Click nodes
   → Each clicked node adds to selection
   → Purple outline on all selected

2. Drag selection box
   → Semi-transparent purple rectangle
   → Nodes inside: highlight as cursor passes
   → Release: all nodes in box selected

3. Bulk operations
   → Delete: Delete key (confirmation if >5 nodes)
   → Move: Drag any selected node, all move together
   → Copy: Cmd+C
   → Paste: Cmd+V (pastes at cursor position)
```

### Context Menu

**Right-click on Node:**
```
┌────────────────────────┐
│  Copy           ⌘C    │
│  Cut            ⌘X    │
│  Paste          ⌘V    │
│  ──────────────────    │
│  Duplicate      ⌘D    │
│  Delete         Del   │
│  ──────────────────    │
│  Properties     ⏎     │
│  View Help      ?     │
│  ──────────────────    │
│  Add Breakpoint       │
│  Disable Node         │
└────────────────────────┘
```

**Right-click on Canvas:**
```
┌────────────────────────┐
│  Add Plugin...  ⌘K    │
│  ──────────────────    │
│  Paste          ⌘V    │
│  ──────────────────    │
│  Zoom In        +     │
│  Zoom Out       -     │
│  Fit to View    0     │
│  ──────────────────    │
│  Export to XML  ⌘E    │
│  Import from XML      │
└────────────────────────┘
```

### Quick Actions Toolbar

```
┌─────────────────────────────────────────────┐
│  [+]  [⊕]  [↻]  [⚙]  [▶]  [⏸]  [⏹]  100% │
│   │    │    │    │    │    │    │     └─ Zoom
│   │    │    │    │    │    │    └─ Stop execution
│   │    │    │    │    │    └─ Pause execution
│   │    │    │    │    └─ Run configuration
│   │    │    │    └─ Settings
│   │    │    └─ Auto-layout
│   │    └─ Add connection
│   └─ Add node
└─────────────────────────────────────────────┘

Tooltips on hover:
  [+] → "Add node (⌘K)"
  [⊕] → "Add connection (⌘L)"
  [↻] → "Auto-layout (⌘R)"
  [⚙] → "Canvas settings"
  [▶] → "Run (⌘↵)"
```

---

## 📱 Responsive Design

### Breakpoints

```css
/* Desktop (default) - 1440px+ */
.visual-designer {
  --palette-width: 280px;
  --canvas-min-width: 800px;
  --property-width: 320px;
}

/* Laptop - 1024px to 1439px */
@media (max-width: 1439px) {
  .visual-designer {
    --palette-width: 240px;
    --property-width: 280px;
  }
}

/* Tablet - 768px to 1023px */
@media (max-width: 1023px) {
  .visual-designer {
    /* Stack vertically */
    --palette-width: 100%;
    --canvas-min-width: 100%;
    --property-width: 100%;
  }
  
  /* Collapsible panels */
  .designer-palette {
    position: absolute;
    left: -280px;
    transition: left 0.3s;
  }
  
  .designer-palette.open {
    left: 0;
  }
}

/* Mobile - < 768px */
@media (max-width: 767px) {
  /* Show message: "For best experience, use desktop" */
  /* Or: Simplified view with XML editor only */
}
```

### Layout Adaptations

**Desktop (1440px+):**
```
┌──────────┬───────────────────┬──────────┐
│ Palette  │      Canvas       │  Props   │
│  (280)   │       (840)       │  (320)   │
└──────────┴───────────────────┴──────────┘
```

**Laptop (1024px):**
```
┌────────┬─────────────────┬────────┐
│Palette │     Canvas      │ Props  │
│ (240)  │      (544)      │ (240)  │
└────────┴─────────────────┴────────┘
```

**Tablet (768px):**
```
┌─────────────────────────────────┐
│ [☰] Canvas              [⚙]    │  ← Hamburger + Settings
├─────────────────────────────────┤
│                                 │
│          Canvas                 │
│         (768px)                 │
│                                 │
└─────────────────────────────────┘

Tap [☰] → Palette slides in from left
Tap node → Properties slide in from right
```

---

## ♿ Accessibility

### WCAG 2.1 Level AA Compliance

**1. Keyboard Navigation**

```
Tab Order:
  1. View switcher buttons
  2. Toolbar buttons
  3. Plugin palette search
  4. Plugin items (focusable)
  5. Canvas nodes (focusable)
  6. Property panel inputs
  7. Action buttons

Arrow Keys:
  - Canvas selected → Arrow keys move node
  - Palette focused → Arrow keys navigate plugins
  - Property inputs → Arrow keys navigate fields

Enter/Space:
  - Activate focused element
  - Add focused plugin to canvas
  - Toggle checkboxes

Escape:
  - Close property panel
  - Cancel drag operation
  - Clear selection

Delete/Backspace:
  - Delete selected nodes (with confirmation)
```

**2. Screen Reader Support**

```html
<!-- Semantic HTML -->
<nav aria-label="Plugin Palette" role="navigation">
  <button aria-label="HTTP Request plugin, drag to add to canvas">
    🌐 HTTP Request
  </button>
</nav>

<main aria-label="Configuration Canvas" role="main">
  <svg role="img" aria-label="Visual workflow with 5 nodes and 4 connections">
    <!-- Canvas SVG -->
  </svg>
</main>

<aside aria-label="Property Panel" role="complementary">
  <form aria-label="HTTP Request configuration">
    <label for="url">URL (required)</label>
    <input id="url" aria-required="true" aria-invalid="false" />
  </form>
</aside>

<!-- Live regions for dynamic updates -->
<div role="status" aria-live="polite" aria-atomic="true">
  Node added to canvas
</div>

<div role="alert" aria-live="assertive">
  Error: Invalid connection
</div>
```

**3. Visual Accessibility**

```css
/* Color Contrast Ratios (WCAG AA) */
--text-on-light: #212121;    /* 16:1 on white */
--text-secondary: #757575;   /* 4.6:1 on white */
--link-color: #5E35B1;       /* 4.5:1 on white */
--error-color: #C62828;      /* 4.6:1 on white */

/* Focus Indicators */
*:focus {
  outline: 3px solid var(--color-accent); /* Yellow */
  outline-offset: 2px;
}

/* Reduced Motion */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

/* High Contrast Mode */
@media (prefers-contrast: high) {
  .visual-node {
    border-width: 3px;
  }
  
  .connection-line {
    stroke-width: 3px;
  }
}
```

**4. Text Alternatives**

```html
<!-- Icons -->
<button aria-label="Add node">
  <span aria-hidden="true">+</span>
</button>

<!-- Status indicators -->
<span class="status-icon" aria-label="Valid configuration">
  <span aria-hidden="true">✓</span>
</span>

<!-- Complex visualizations -->
<svg role="img" aria-labelledby="flow-title flow-desc">
  <title id="flow-title">Scraping Workflow</title>
  <desc id="flow-desc">
    A workflow with HTTP request, HTML parser, and XPath extractor
  </desc>
</svg>
```

---

## ⌨️ Keyboard Shortcuts

### Global Shortcuts

| Shortcut | Action | Context |
|----------|--------|---------|
| `⌘K` / `Ctrl+K` | Quick add plugin | Any |
| `⌘S` / `Ctrl+S` | Save configuration | Any |
| `⌘Z` / `Ctrl+Z` | Undo | Any |
| `⌘⇧Z` / `Ctrl+Shift+Z` | Redo | Any |
| `⌘/` / `Ctrl+/` | Toggle help panel | Any |
| `⌘1` | Switch to XML view | Any |
| `⌘2` | Switch to Split view | Any |
| `⌘3` | Switch to Visual view | Any |

### Canvas Shortcuts

| Shortcut | Action | Context |
|----------|--------|---------|
| `Space + Drag` | Pan canvas | Canvas |
| `⌘+` / `Ctrl++` | Zoom in | Canvas |
| `⌘-` / `Ctrl+-` | Zoom out | Canvas |
| `⌘0` / `Ctrl+0` | Fit to view | Canvas |
| `⌘A` / `Ctrl+A` | Select all nodes | Canvas |
| `⌘D` / `Ctrl+D` | Duplicate selected | Canvas |
| `Delete` / `Backspace` | Delete selected | Canvas |
| `⌘C` / `Ctrl+C` | Copy selected | Canvas |
| `⌘V` / `Ctrl+V` | Paste | Canvas |
| `⌘L` / `Ctrl+L` | Add connection | Canvas |
| `⌘R` / `Ctrl+R` | Auto-layout | Canvas |
| `⌘↵` / `Ctrl+Enter` | Run configuration | Canvas |
| `Escape` | Clear selection | Canvas |

### Node Shortcuts

| Shortcut | Action | Context |
|----------|--------|---------|
| `Enter` | Open properties | Node selected |
| `E` | Edit inline | Node selected |
| `?` | View help | Node selected |
| `B` | Toggle breakpoint | Node selected |
| `D` | Disable node | Node selected |
| `↑↓←→` | Move node | Node selected |
| `⇧+↑↓←→` | Move node (fast) | Node selected |

### Property Panel Shortcuts

| Shortcut | Action | Context |
|----------|--------|---------|
| `⌘↵` / `Ctrl+Enter` | Apply changes | Property panel |
| `Escape` | Cancel changes | Property panel |
| `Tab` | Next field | Property panel |
| `⇧+Tab` | Previous field | Property panel |

---

## 🎬 Animation & Motion

### Design Principles

**1. Purpose-Driven**
- Animations guide attention
- Feedback confirms actions
- Transitions show relationships

**2. Performance**
- 60 FPS (16ms frame budget)
- GPU-accelerated (transform, opacity)
- Avoid layout thrashing

**3. Duration Standards**
- **Micro (100-200ms):** Hover, focus states
- **Normal (200-400ms):** Transitions, reveals
- **Macro (400-600ms):** Complex sequences

### Animation Specifications

**Node Addition:**
```css
@keyframes nodeAppear {
  0% {
    opacity: 0;
    transform: scale(0.8) translateY(-20px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.visual-node.appearing {
  animation: nodeAppear 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
```

**Connection Animation:**
```css
@keyframes connectionDraw {
  0% {
    stroke-dashoffset: 1000;
  }
  100% {
    stroke-dashoffset: 0;
  }
}

.connection-line.animating {
  stroke-dasharray: 1000;
  animation: connectionDraw 400ms ease-out;
}
```

**Execution Pulse:**
```css
@keyframes executionPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(126, 87, 194, 0.7);
    border-color: var(--color-primary);
  }
  50% {
    box-shadow: 0 0 0 10px rgba(126, 87, 194, 0);
    border-color: var(--color-secondary);
  }
}

.visual-node.executing {
  animation: executionPulse 1.5s infinite;
}
```

**Property Panel Slide:**
```css
@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.property-panel.entering {
  animation: slideIn 250ms cubic-bezier(0.4, 0, 0.2, 1);
}
```

**Error Shake:**
```css
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}

.visual-node.error {
  animation: shake 400ms;
}
```

### Transition Timing

```css
/* Easing functions */
:root {
  --ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-out: cubic-bezier(0, 0, 0.2, 1);
  --ease-in: cubic-bezier(0.4, 0, 1, 1);
  --ease-bounce: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-elastic: cubic-bezier(0.68, -0.55, 0.265, 1.55);
}

/* Standard transitions */
.btn {
  transition: all 150ms var(--ease-in-out);
}

.visual-node {
  transition: 
    transform 200ms var(--ease-out),
    box-shadow 200ms var(--ease-out),
    border-color 150ms var(--ease-in-out);
}

.connection-line {
  transition: stroke 200ms var(--ease-in-out);
}
```

---

## 🎯 Success Metrics

### Usability Metrics

**Task Completion Time:**
- First scraper creation: < 5 minutes (target)
- Complex pipeline: < 10 minutes (target)
- XML ↔ Visual switch: < 2 seconds (technical)

**Error Rate:**
- Configuration errors: < 10% (target)
- Connection errors: < 5% (target)
- Sync conflicts: < 1% (target)

**User Satisfaction:**
- SUS Score: > 80 (target)
- NPS Score: > 50 (target)
- Task success rate: > 95% (target)

### Accessibility Metrics

- Keyboard-only completion: 100% of tasks
- Screen reader compatibility: WCAG 2.1 AA
- Color contrast: All text 4.5:1 minimum
- Focus indicators: Visible on all interactive elements

---

## 📚 Summary

This UX specification provides:

✅ **Complete interaction patterns** for all user workflows  
✅ **Detailed mockups** for every view and component  
✅ **Accessibility guidelines** (WCAG 2.1 AA compliant)  
✅ **Animation specifications** for smooth, purposeful motion  
✅ **Keyboard shortcuts** for power users  
✅ **Responsive design** for multiple screen sizes  
✅ **Apple HIG compliance** for modern, native feel

**Next Steps:**
1. ✅ Review mockups with stakeholders
2. ⏩ Create interactive prototype (Figma/Adobe XD)
3. ⏩ User testing with 5-10 participants
4. ⏩ Iterate based on feedback
5. ⏩ Proceed to implementation

---

**Document Version:** 1.0  
**Last Updated:** October 24, 2025  
**Maintainers:** WebHarvest UX Team


