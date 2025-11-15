# Visual Designer - Hierarchy & Nesting Design

**Critical Requirement:** Support for nested/container plugins with visual hierarchy and execution flow tracking.

---

## 🎯 Core Concept

### Plugin Types by Nesting Capability

1. **Container Plugins** (can contain children)
   - `loop`, `while` - iteration containers
   - `if`, `else`, `try`, `catch` - conditional containers
   - `function` - function definition container
   - `http` - can contain `http-header`, `http-param`
   - `database` - can contain `db-param`
   - `template` - can contain dynamic content

2. **Leaf Plugins** (cannot contain children)
   - `xpath`, `regexp`, `xquery` - extractors
   - `var`, `set-var`, `get` - variable operations
   - `file`, `log`, `sleep` - utilities

3. **Special Plugins** (must be children of specific parents)
   - `http-header` - only inside `http`
   - `http-param` - only inside `http`
   - `db-param` - only inside `database`
   - `call-param` - only inside `call`

---

## 🏗️ Visual Hierarchy Models

### Model 1: Container Nodes (Preferred - like NiFi Process Groups)

```
┌─────────────────────────────────────────┐
│ 🔁 loop (id="items")                    │
│ ─────────────────────────────────────── │
│ ┌────────────────┐  ┌────────────────┐ │
│ │ 🌐 http        │→ │ 📊 xpath       │ │
│ └────────────────┘  └────────────────┘ │
│          ↓                              │
│ ┌────────────────────────────────────┐ │
│ │ 💾 var                             │ │
│ └────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Features:**
- Container is a special node with expanded area
- Children rendered inside container boundary
- Visual nesting with indentation/padding
- Collapsible/expandable containers
- Drag children within container only

### Model 2: Tree View (Alternative - like Azure Logic Apps)

```
loop (id="items")
├─ http (url="${baseUrl}/item")
│  └─ http-header (name="Authorization")
├─ xpath (expression="//title")
└─ var (name="title")
```

**Features:**
- Hierarchical tree on left side
- Canvas shows flat view with connection indicators
- Parent-child relationships shown via indentation
- Click to focus/highlight on canvas

### Model 3: Hybrid (Recommended - Best of Both)

**Canvas View:**
- Container nodes render as expanded boxes
- Children inside containers
- Visual hierarchy clear

**Outline Panel (new):**
- Tree view of complete structure
- Sync with canvas selection
- Drag to reorder/re-parent

---

## 🎨 Visual Design (Lime-based)

### Color Palette
```css
--lime: #CDDC39;           /* Primary accent */
--lime-light: #E6EE9C;     /* Hover states */
--lime-dark: #9E9D24;      /* Active states */
--lime-glow: rgba(205, 220, 57, 0.3); /* Selection glow */

--container-bg: #F5F5F5;   /* Light gray for containers */
--container-border: #9E9D24; /* Lime-dark for borders */
--child-indent: 24px;      /* Indentation for children */
```

### Container Node Style
```
┌─ 🔁 loop ─────────────────────────────┐
│ id="items", max="10"                  │
├───────────────────────────────────────┤
│                                       │
│   ┌─ 🌐 http ──────────────────┐     │
│   │ url="${item}/details"      │     │
│   └────────────────────────────┘     │
│                                       │
│   ┌─ 📊 xpath ─────────────────┐     │
│   │ expression="//title/text()" │     │
│   └────────────────────────────┘     │
│                                       │
└───────────────────────────────────────┘
```

**Visual Hierarchy:**
- Container: Thicker border (3px), lime accent on top
- Children: Regular border (2px), indented 24px
- Nesting level: Visual depth via shadows
- Max nesting: 5 levels (prevent chaos)

---

## 🔗 Connection Model

### Sequential Connections (within container)
```
http → xpath → var
```
- Simple arrow connections
- Show execution order
- Light gray lines

### Container I/O Ports
```
    INPUT
      ↓
┌─────────────┐
│   loop      │
│  (children) │
└─────────────┘
      ↓
    OUTPUT
```

**Rules:**
- Containers have input/output ports
- Data flows into container (input)
- Results flow out (output)
- Internal execution hidden until expanded

---

## 🎬 Execution Flow Tracking

### Visual States

1. **PENDING** (default)
   - Gray node
   - No indicator

2. **RUNNING** (active execution)
   - Lime glow around node
   - Pulsing animation
   - Progress indicator

3. **COMPLETED** (success)
   - Green checkmark badge
   - Lime border
   - Result count badge

4. **ERROR** (failed)
   - Red border
   - Error icon badge
   - Click to see error details

5. **PAUSED** (suspended)
   - Yellow/amber indicator
   - Pause icon

### Execution Animation
```
loop [RUNNING] 🟢
├─ http [COMPLETED] ✅ (230ms, 5.2KB)
├─ xpath [RUNNING] 🟢
└─ var [PENDING] ⏸️
```

**Features:**
- Highlight current executing node with lime glow
- Show execution time on node
- Display data size/count
- Animate data flow through connections
- History panel showing execution log

---

## 📊 Data Flow Visualization

### Node Data Badge
```
┌─────────────────────────┐
│ 🌐 http                 │ ✅ 5.2KB ⏱️ 230ms
│ url="..."               │
└─────────────────────────┘
```

**Information:**
- ✅ Success indicator
- 📦 Data size (5.2KB)
- ⏱️ Execution time (230ms)
- 🔢 Item count (for lists)
- 🔗 Connection count (for nested)

### Connection Data Flow
```
http ────[5.2KB]────> xpath ────[3 items]────> var
      🟢 flowing              🟢 flowing
```

**Animation:**
- Particles flowing through connections
- Lime-colored dots moving along line
- Speed based on data size
- Pause on hover to inspect

---

## 🏛️ Data Model

### Node Structure
```typescript
interface VisualNode {
  id: string;
  type: string; // 'http', 'loop', etc.
  position: { x: number; y: number };
  size: { width: number; height: number };
  
  // Hierarchy
  parentId: string | null;
  children: string[]; // Child node IDs
  canHaveChildren: boolean;
  allowedChildren: string[]; // e.g., ['http-header'] for http
  
  // Configuration
  attributes: Record<string, any>;
  body: string | null;
  
  // Execution tracking
  executionState: 'pending' | 'running' | 'completed' | 'error' | 'paused';
  executionTime?: number; // milliseconds
  dataSize?: number; // bytes
  itemCount?: number;
  errorMessage?: string;
  
  // Visual
  isCollapsed: boolean; // for containers
  isSelected: boolean;
  isHighlighted: boolean;
}
```

### Connection Structure
```typescript
interface VisualConnection {
  id: string;
  sourceId: string;
  targetId: string;
  type: 'sequential' | 'parent-child' | 'data-flow';
  
  // Execution tracking
  dataFlowing: boolean;
  dataSize?: number;
  
  // Visual
  isHighlighted: boolean;
}
```

---

## 🎛️ UI Components

### 1. Canvas Area (main)
- Drag & drop from palette
- Container nodes with children inside
- Zoom/pan controls
- Mini-map showing hierarchy

### 2. Outline Panel (new - left side)
```
📄 Configuration
├─ 🔁 loop (items)
│  ├─ 🌐 http
│  │  └─ 🏷️ http-header
│  ├─ 📊 xpath
│  └─ 💾 var
└─ 📝 log
```
- Tree view of complete structure
- Drag to reorder
- Click to select/focus
- Show execution state icons

### 3. Property Panel (right)
- Selected node properties
- Dynamic form based on schema
- Add child button (for containers)
- Validation errors

### 4. Execution Panel (bottom)
```
🎬 Execution Log
[10:15:32] ▶ Started execution
[10:15:33] 🔁 loop: Iteration 1/10
[10:15:33] 🌐 http: GET https://example.com (230ms, 5.2KB)
[10:15:34] 📊 xpath: Extracted 3 items
[10:15:34] 💾 var: Stored value "title"
[10:15:35] 🔁 loop: Iteration 2/10
...
```

### 5. Data Inspector (right panel, tab 2)
```
Selected: http (node-123)
─────────────────────────
Response:
  Status: 200 OK
  Size: 5.2KB
  Time: 230ms
  
Body: (first 200 chars)
<!DOCTYPE html>
<html>...
  
Variables Created:
  - ${response}: [HTML Document]
  - ${statusCode}: 200
```

---

## 🔨 Implementation Plan

### Phase 1: Container Nodes (NOW)
1. ✅ Update color scheme to lime-based
2. ✅ Identify container vs leaf plugins from schema
3. ✅ Implement container node rendering
4. ✅ Child node positioning within container
5. ✅ Drag & drop children into containers
6. ✅ Visual hierarchy with borders/shadows

### Phase 2: Connections
1. Sequential connections within container
2. Container input/output ports
3. Connection validation (type checking)
4. Visual connection styling

### Phase 3: Execution Tracking
1. Execution state in data model
2. Visual state indicators
3. Real-time updates from backend
4. Animation for running nodes
5. Data flow animation

### Phase 4: Outline Panel
1. Tree view component
2. Sync with canvas
3. Drag to reorder
4. Focus/highlight

### Phase 5: Data Inspector
1. Show execution results
2. Variable inspection
3. Response/request details
4. Error details

---

## 🎯 Success Criteria

1. ✅ User can create nested structures visually
2. ✅ Hierarchy is clear and intuitive
3. ✅ Execution flow is trackable in real-time
4. ✅ Data at each step is visible
5. ✅ Parent-child relationships are enforced
6. ✅ No orphaned nodes
7. ✅ Professional Apple HIG-compliant UI

---

**Next:** Implement container nodes with lime styling!









