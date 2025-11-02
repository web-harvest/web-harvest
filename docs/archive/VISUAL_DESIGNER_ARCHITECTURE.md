# Visual Designer Architecture

## Overview

WebHarvest IDE Visual Designer implements a **hybrid model** combining the best of both worlds:
- **Nested rendering** for hierarchical structures (containers)
- **Connected/Flowchart rendering** for linear sequences

This approach is inspired by Azure Logic Apps and provides intuitive visual representation of WebHarvest configurations.

---

## Design Decision: Hybrid Model

### ✅ Nested Model (for Containers)

**Used for:** Loop, If, While, Try, Function, Case

**Rendering:**
```
┌─────────────────────────────┐
│  LOOP (maxloops="5")        │
├─────────────────────────────┤
│  ┌───────────────────────┐  │
│  │  HTTP (url="...")     │  │
│  └───────────────────────┘  │
│           ↓                  │
│  ┌───────────────────────┐  │
│  │  XPATH (expr="...")   │  │
│  └───────────────────────┘  │
│           ↓                  │
│  ┌───────────────────────┐  │
│  │  DEF (var="result")   │  │
│  └───────────────────────┘  │
└─────────────────────────────┘
```

**Advantages:**
- Natural representation of scope/hierarchy
- Clear visual boundaries
- Easy to see what's inside the container
- Matches XML structure 1:1

---

### ✅ Connected Model (for Linear Sequences)

**Used for:** HTTP, XPath, Def, Regexp, Html-to-XML, Script, etc.

**Rendering:**
```
┌───────────────────────┐
│  HTTP                 │
│  url="api.com"        │
└───────────────────────┘
           ↓
┌───────────────────────┐
│  XPATH                │
│  expression="//title" │
└───────────────────────┘
           ↓
┌───────────────────────┐
│  DEF                  │
│  var="title"          │
└───────────────────────┘
```

**Advantages:**
- Clear data flow
- Intuitive sequence
- Easy to reorder (drag & drop)
- Matches flowchart mental model

---

## Node Classification

### Container Nodes (Nested Rendering)
- `loop` - Iteration container
- `while` - Conditional iteration
- `if` - Conditional execution
- `try` / `catch` - Error handling
- `case` - Switch/case branching
- `function` - Function definition
- `body` - Generic container
- `config` - Root configuration

### Action Nodes (Connected Rendering)
- **Network:** `http`, `ftp`, `mail`
- **Extraction:** `xpath`, `xquery`, `regexp`, `html-to-xml`, `json-to-xml`
- **Variables:** `def`, `get-var`
- **Processing:** `script`, `template`, `xslt`
- **Control:** `return`, `exit`, `sleep`, `log`
- **Storage:** `file`

---

## Architecture Components

### 1. XMLParser
**Purpose:** Convert XML string → VisualNode[]

**Features:**
- DOM-based parsing
- Preserves hierarchy (children)
- Extracts attributes and body content
- Generates unique IDs for visual representation

### 2. XMLGenerator
**Purpose:** Convert VisualNode[] → XML string

**Features:**
- Recursive generation
- Proper indentation
- CDATA wrapping for scripts
- Attribute escaping

### 3. VisualDesigner
**Purpose:** Render and interact with visual nodes

**Features:**
- **Hybrid Rendering:**
  - Container nodes: Nested boxes with children inside
  - Action nodes: Connected flowchart nodes
- **Drag & Drop:**
  - Reorder nodes in linear sequences
  - Add nodes to containers
  - Prevent circular nesting
- **Selection & Editing:**
  - Click to select node
  - Property panel for editing attributes
  - Real-time XML synchronization

### 4. PropertyPanel
**Purpose:** Edit selected node's properties

**Features:**
- Attribute editor (key-value pairs)
- Body content editor (for scripts, templates)
- Validation (required attributes)
- Type-specific editors (URL, XPath expression, etc.)

### 5. PluginPalette
**Purpose:** Drag templates to create new nodes

**Features:**
- Categorized plugins (Network, Extraction, Control, etc.)
- Search/filter
- Drag template to canvas
- Auto-generate default attributes

---

## Synchronization Flow

### XML → Visual (Load)
```
XML String
    ↓
XMLParser.parse()
    ↓
VisualNode[]
    ↓
VisualDesigner.loadNodes()
    ↓
Render (nested + connected)
```

### Visual → XML (Save)
```
User edits node
    ↓
PropertyPanel updates node.attributes
    ↓
VisualDesigner.getNodes()
    ↓
XMLGenerator.generate()
    ↓
XML String
    ↓
Update active tab content
```

---

## Layout Algorithm

### Linear Layout (Connected Nodes)
```typescript
function calculateLinearLayout(nodes: VisualNode[], options) {
    const positions = {};
    let currentY = options.startY;
    
    nodes.forEach(node => {
        positions[node.id] = { x: options.startX, y: currentY };
        currentY += NODE_HEIGHT + SPACING;
    });
    
    return positions;
}
```

### Nested Layout (Container + Children)
```typescript
function calculateNestedLayout(container: VisualNode, options) {
    const positions = {};
    
    // Container position
    positions[container.id] = { x: options.x, y: options.y };
    
    // Children inside (offset by padding)
    let childY = options.y + HEADER_HEIGHT + PADDING;
    container.children.forEach(child => {
        positions[child.id] = { 
            x: options.x + PADDING, 
            y: childY 
        };
        childY += NODE_HEIGHT + SPACING;
    });
    
    return positions;
}
```

---

## Connection Lines

For connected (flowchart) nodes, draw arrows between sequential nodes:

```typescript
interface Connection {
    from: string; // node ID
    to: string;   // node ID
    type: 'sequential' | 'conditional' | 'error';
}

function generateConnections(nodes: VisualNode[]): Connection[] {
    const connections: Connection[] = [];
    
    for (let i = 0; i < nodes.length - 1; i++) {
        connections.push({
            from: nodes[i].id,
            to: nodes[i + 1].id,
            type: 'sequential'
        });
    }
    
    return connections;
}
```

**Visual representation:**
- Sequential: Solid black arrow →
- Conditional: Dashed green arrow ⇢
- Error handling: Red arrow (try → catch)

---

## Drag & Drop

### Rules:
1. ✅ **Reorder** linear nodes at same level
2. ✅ **Add** node to container (becomes child)
3. ✅ **Remove** node from container (back to root)
4. ❌ **Prevent** adding node to itself
5. ❌ **Prevent** circular nesting (A contains B contains A)

### Implementation:
```typescript
function onDrop(draggedNode: VisualNode, dropTarget: VisualNode | null) {
    // Validate
    if (draggedNode.id === dropTarget?.id) {
        throw new Error('Cannot add node to itself');
    }
    
    if (hasCircularReference(dropTarget, draggedNode)) {
        throw new Error('Circular nesting detected');
    }
    
    // Add to container or reorder
    if (dropTarget && isContainerNode(dropTarget.type)) {
        dropTarget.children.push(draggedNode);
    } else {
        // Reorder at same level
        reorderNodes(nodes, draggedNode.id, dropIndex);
    }
    
    // Re-render and sync XML
    render();
    syncToXML();
}
```

---

## Plugin Palette

### Categories:
1. **Network:** HTTP, FTP, Mail
2. **Extraction:** XPath, XQuery, Regexp, HTML-to-XML, JSON-to-XML
3. **Variables:** Def, Get
4. **Control Flow:** Loop, While, If, Try/Catch, Case
5. **Processing:** Script, Template, XSLT
6. **Storage:** File
7. **Utilities:** Log, Sleep, Exit, Return

### Template Structure:
```typescript
interface PluginTemplate {
    type: string;
    category: string;
    icon: string; // Icon class/path
    label: string;
    defaultAttributes: Record<string, string>;
    description: string;
}
```

### Example Templates:
```typescript
{
    type: 'http',
    category: 'network',
    icon: 'network-icon',
    label: 'HTTP Request',
    defaultAttributes: { 
        url: 'https://api.example.com',
        method: 'GET'
    },
    description: 'Fetch data from HTTP endpoint'
}

{
    type: 'loop',
    category: 'control',
    icon: 'loop-icon',
    label: 'Loop',
    defaultAttributes: { maxloops: '10' },
    description: 'Iterate multiple times'
}
```

---

## Real-Time Synchronization

### Trigger Points:
1. **Node attribute changed** → Update XML
2. **Node added/removed** → Update XML
3. **Node reordered** → Update XML
4. **Switch to XML mode** → Generate full XML from visual
5. **Switch to Visual mode** → Parse XML to visual

### Debouncing:
- Attribute changes: Debounce 500ms (while typing)
- Structural changes: Immediate (add/remove/reorder)

---

## Error Handling

### Graceful Degradation:
1. **Invalid XML** → Show error, stay in XML mode
2. **Missing plugins** → Show placeholder node with warning
3. **Circular nesting attempt** → Prevent, show error message
4. **Empty nodes** → Show empty state, allow drag from palette

---

## Future Enhancements

### Phase 2:
- [ ] Auto-layout (automatic positioning)
- [ ] Zoom in/out
- [ ] Minimap for large configurations
- [ ] Undo/Redo for visual edits
- [ ] Copy/Paste nodes
- [ ] Keyboard shortcuts (Delete, Duplicate, etc.)
- [ ] Export as image (PNG/SVG)

### Phase 3:
- [ ] Collaborative editing (real-time multi-user)
- [ ] Version control integration
- [ ] AI-assisted configuration (suggest next plugin)
- [ ] Performance profiling overlay (show execution time per node)

---

## Testing Strategy

### TDD Approach:
1. ✅ **Unit Tests:** XMLParser, XMLGenerator, Layout algorithms
2. ✅ **Integration Tests:** XML ↔ Visual round-trip
3. ✅ **Visual Tests:** Rendering, Drag & Drop, Property editing
4. ✅ **E2E Tests:** Full workflow (load example → edit → save → run)

### Test Coverage Goals:
- XMLParser: 90%+
- XMLGenerator: 90%+
- VisualDesigner: 80%+
- PropertyPanel: 80%+
- Overall: 85%+

---

## Implementation Phases

### ✅ Phase 1 (Current): Foundation
- [x] XMLParser (XML → VisualNode[])
- [x] XMLGenerator (VisualNode[] → XML)
- [x] VisualDesigner (basic rendering)
- [x] PropertyPanel (basic editing)
- [x] Mode switching (XML ↔ Visual)

### 🚧 Phase 2 (In Progress): Hybrid Model
- [ ] Implement node classification (container vs action)
- [ ] Nested rendering for containers
- [ ] Connected rendering for linear sequences
- [ ] Connection lines/arrows
- [ ] Layout algorithms (linear + nested)
- [ ] TDD tests for all features

### 📋 Phase 3 (Next): Drag & Drop
- [ ] Drag from palette to canvas
- [ ] Reorder nodes
- [ ] Add to containers
- [ ] Validation (circular nesting, etc.)

### 📋 Phase 4 (Future): Polish
- [ ] Auto-layout
- [ ] Zoom/pan
- [ ] Keyboard shortcuts
- [ ] Export as image

---

## Technology Stack

- **Language:** TypeScript
- **Rendering:** Canvas API / SVG
- **Styling:** CSS (Apple HIG inspired)
- **Build:** Vite
- **Testing:** Jest
- **XML Parsing:** DOMParser (browser native)
- **Drag & Drop:** HTML5 Drag & Drop API

---

## Conclusion

The hybrid model (nested + connected) provides the best user experience by:
1. **Matching mental models** - Containers look like containers, sequences look like flows
2. **Flexibility** - Best of both worlds
3. **Scalability** - Works for small and large configurations
4. **Familiarity** - Similar to Azure Logic Apps, AWS Step Functions

This architecture enables WebHarvest IDE to provide a world-class visual design experience while maintaining full XML compatibility.




