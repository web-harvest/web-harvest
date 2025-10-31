# Conductor Analysis & WebHarvest Visual Designer Roadmap

**Inspired by:** [Conductor OSS](https://github.com/conductor-oss/conductor/) - Netflix's workflow orchestration platform  
**Date:** October 28, 2025  
**Status:** Analysis + Implementation Plan

---

## 🎯 **Co musimy zaimplementować (inspiracja Conductor):**

### **1. Property Panel - Edycja właściwości node'ów**
### **2. Relationships - Relacje między komponentami**
### **3. Component Editing - Zaawansowana edycja komponentów**

---

## 📊 **Conductor Architecture Analysis**

### **Key Features z Conductor:**

#### **1. Workflow as JSON (Conductor approach)**
```json
{
  "name": "sample_workflow",
  "version": 1,
  "tasks": [
    {
      "name": "http_task",
      "taskReferenceName": "http_1",
      "type": "HTTP",
      "inputParameters": {
        "http_request": {
          "uri": "https://api.example.com",
          "method": "GET"
        }
      }
    },
    {
      "name": "process_response",
      "taskReferenceName": "process_1",
      "type": "SIMPLE",
      "inputParameters": {
        "data": "${http_1.output}"
      }
    }
  ]
}
```

**WebHarvest equivalent (XML):**
```xml
<config>
  <http url="https://api.example.com" var="http_1"/>
  <xpath expression="//data">
    <var name="http_1"/>
  </xpath>
</config>
```

**Key Insight:** Conductor używa JSON + task references. My używamy XML + zmienne.

---

#### **2. Property Panel (Conductor style)**

**W Conductor:**
- Right sidebar panel
- Input/Output parameters
- Task-specific configuration
- Validation rules
- Variable binding (${task.output})

**Dla WebHarvest potrzebujemy:**

```typescript
interface PropertyPanel {
    // Current (mamy już):
    showNode(node: VisualNode): void;
    
    // TODO: Dodać:
    validateProperty(key: string, value: string): ValidationResult;
    showAdvancedProperties(): void;
    showVariableBindings(): void;  // ${var_name} picker
    showSchemaHelp(): void;        // Help tooltip z dokumentacją
}
```

**UI Layout:**
```
┌─────────────────────────────────┐
│ Property Panel: <node.type>     │
├─────────────────────────────────┤
│ Basic Properties:               │
│   url: [https://...        ]    │
│   method: [GET ▼]               │
│   timeout: [30000          ]    │
│                                 │
│ Advanced Properties: [Show]     │
│                                 │
│ Variable Bindings:              │
│   Output var: [result      ]    │
│   Input vars: [Select...   ]    │
│                                 │
│ Documentation: [?]              │
└─────────────────────────────────┘
```

---

#### **3. Relationships (Conductor uses):**

**Sequential (default):**
```
task1 → task2 → task3
```

**Conditional (DECISION node):**
```
            ┌→ task_yes
decision ───┤
            └→ task_no
```

**Parallel (FORK/JOIN):**
```
       ┌→ task_a ─┐
fork ──┼→ task_b ─┼→ join → task_next
       └→ task_c ─┘
```

**Loop (DO_WHILE):**
```
task_1 → task_2 ⤴ (repeat if condition)
```

**WebHarvest equivalent:**

| Conductor | WebHarvest | Implementation |
|-----------|------------|----------------|
| Sequential | XML order | ✅ Works (default) |
| DECISION | `<if>` / `<case>` | ✅ Works |
| FORK/JOIN | Manual connections | ❌ TODO |
| DO_WHILE | `<while>` / `<loop>` | ✅ Works |

---

#### **4. Component Editing (Conductor features):**

**Task Configuration:**
- Inline editing (double-click on canvas)
- Property panel (sidebar)
- JSON editor (advanced mode)
- Template library (pre-configured tasks)

**Validation:**
- Real-time validation
- Required fields marked with *
- Type checking (URL, number, boolean)
- Custom validators per plugin

**Variable Binding:**
- Auto-complete for variables (${var_name})
- Click to select from list
- Inline variable preview

---

## 🚀 **Implementation Roadmap (TDD)**

### **Phase 1: Enhanced Property Panel** (2-3 days)

#### **Features:**
1. ✅ Basic property editing (mamy już)
2. ❌ Real-time validation
3. ❌ Variable picker (${var_name} autocomplete)
4. ❌ Schema-based help tooltips
5. ❌ Advanced properties toggle
6. ❌ Type-specific editors (URL picker, color picker, etc.)

#### **TDD Tests:**
```typescript
describe('PropertyPanel - Enhanced', () => {
    it('should validate URL format in real-time');
    it('should show autocomplete for variable names');
    it('should toggle advanced properties section');
    it('should show schema help tooltip on hover');
    it('should use type-specific editors (URL, number, boolean)');
    it('should mark required fields with asterisk');
    it('should prevent saving invalid values');
});
```

#### **Implementation:**
```typescript
class EnhancedPropertyPanel {
    private validators: Map<string, Validator> = new Map();
    private variableAutocomplete: VariableAutocomplete;
    
    validateProperty(key: string, value: string): ValidationResult {
        const validator = this.validators.get(key);
        return validator ? validator.validate(value) : { valid: true };
    }
    
    showVariablePicker(inputElement: HTMLInputElement): void {
        const availableVars = this.context.getAllVariables();
        this.variableAutocomplete.show(inputElement, availableVars);
    }
    
    renderPropertyEditor(attribute: PluginAttribute): HTMLElement {
        switch (attribute.type) {
            case 'url':
                return this.createURLEditor(attribute);
            case 'boolean':
                return this.createToggleSwitch(attribute);
            case 'enum':
                return this.createDropdown(attribute);
            case 'variable':
                return this.createVariablePicker(attribute);
            default:
                return this.createTextInput(attribute);
        }
    }
}
```

---

### **Phase 2: Manual Relationships (FORK/JOIN)** (3-4 days)

#### **Features:**
1. ✅ Basic connections (mamy już - topological sort)
2. ❌ Connection types (sequential, conditional, parallel)
3. ❌ Visual feedback (different line styles)
4. ❌ FORK/JOIN nodes (parallel execution)
5. ❌ Connection labels (condition expressions)

#### **TDD Tests:**
```typescript
describe('Manual Relationships - Advanced', () => {
    it('should create FORK node with multiple outputs');
    it('should create JOIN node with multiple inputs');
    it('should execute tasks in parallel between FORK and JOIN');
    it('should render different line styles for connection types');
    it('should add conditional labels to connections');
    it('should validate FORK/JOIN pairing');
});
```

#### **Implementation:**
```typescript
interface ManualConnection {
    id: string;
    from: string;
    to: string;
    type: 'sequential' | 'conditional' | 'parallel' | 'error';
    condition?: string;  // For conditional connections
    label?: string;      // Display label
}

class RelationshipManager {
    createForkNode(): VisualNode {
        return {
            id: generateId(),
            type: 'fork',
            category: 'Control',
            attributes: { parallelTasks: [] },
            outputs: []  // Multiple outputs
        };
    }
    
    createJoinNode(): VisualNode {
        return {
            id: generateId(),
            type: 'join',
            category: 'Control',
            attributes: {},
            inputs: []  // Multiple inputs
        };
    }
    
    getExecutionPlan(): ExecutionPlan {
        // Analyze connections and create execution plan
        // Sequential: task1 → task2 → task3
        // Parallel: fork → [task_a, task_b, task_c] → join
        return this.buildExecutionTree(this.connections);
    }
}
```

**Visual Rendering:**
```typescript
// Different line styles for connection types
function renderConnection(conn: ManualConnection): SVGLineElement {
    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    
    switch (conn.type) {
        case 'sequential':
            line.setAttribute('stroke', '#4CAF50');
            line.setAttribute('stroke-width', '2');
            break;
        case 'conditional':
            line.setAttribute('stroke', '#FF9800');
            line.setAttribute('stroke-dasharray', '5,5');
            break;
        case 'parallel':
            line.setAttribute('stroke', '#2196F3');
            line.setAttribute('stroke-width', '3');
            break;
        case 'error':
            line.setAttribute('stroke', '#F44336');
            line.setAttribute('stroke-dasharray', '10,5');
            break;
    }
    
    return line;
}
```

---

### **Phase 3: Component Editing (Conductor-inspired)** (2-3 days)

#### **Features:**
1. ❌ Inline editing (double-click on canvas node)
2. ❌ Template library (pre-configured plugins)
3. ❌ JSON/XML view switcher
4. ❌ Undo/Redo stack (proper implementation)
5. ❌ Copy/Paste with references

#### **TDD Tests:**
```typescript
describe('Component Editing - Conductor-style', () => {
    it('should enable inline editing on double-click');
    it('should show template library modal');
    it('should apply template to selected node');
    it('should switch between XML and JSON view');
    it('should undo last N changes');
    it('should redo undone changes');
    it('should copy node with all properties');
    it('should paste node and update variable references');
});
```

#### **Implementation:**
```typescript
class ComponentEditor {
    private undoStack: NodeSnapshot[] = [];
    private redoStack: NodeSnapshot[] = [];
    private maxUndoSteps = 50;
    
    enableInlineEdit(node: VisualNode, position: {x, y}): void {
        const editor = document.createElement('div');
        editor.className = 'inline-editor';
        editor.style.position = 'absolute';
        editor.style.left = `${position.x}px`;
        editor.style.top = `${position.y}px`;
        
        // Render compact property form
        Object.keys(node.attributes).forEach(key => {
            const input = this.createInput(key, node.attributes[key]);
            editor.appendChild(input);
        });
        
        // Save on blur or Enter
        editor.addEventListener('blur', () => this.saveInlineChanges(node, editor));
        editor.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') this.saveInlineChanges(node, editor);
            if (e.key === 'Escape') editor.remove();
        });
        
        document.body.appendChild(editor);
        editor.querySelector('input')?.focus();
    }
    
    showTemplateLibrary(): void {
        const templates = [
            { name: 'HTTP GET', plugin: 'http', attributes: { method: 'GET' } },
            { name: 'HTTP POST JSON', plugin: 'http', attributes: { method: 'POST', contentType: 'application/json' } },
            { name: 'XPath Extract', plugin: 'xpath', attributes: { expression: '//div' } },
            // ... więcej templates
        ];
        
        // Render modal with templates
        this.renderTemplateModal(templates);
    }
    
    undo(): void {
        if (this.undoStack.length === 0) return;
        
        const current = this.captureSnapshot();
        this.redoStack.push(current);
        
        const previous = this.undoStack.pop()!;
        this.restoreSnapshot(previous);
    }
    
    redo(): void {
        if (this.redoStack.length === 0) return;
        
        const current = this.captureSnapshot();
        this.undoStack.push(current);
        
        const next = this.redoStack.pop()!;
        this.restoreSnapshot(next);
    }
    
    private captureSnapshot(): NodeSnapshot {
        return {
            nodes: JSON.parse(JSON.stringify(this.nodes)),
            connections: JSON.parse(JSON.stringify(this.connections)),
            timestamp: Date.now()
        };
    }
}
```

---

## 🎨 **UI/UX Improvements (Apple HIG + Conductor style)**

### **1. Canvas Controls (bottom-right corner):**
```
┌───────────────────┐
│ [🔍+] [🔍-] [↻]  │  Zoom In, Out, Reset
│ [⊞] [⊟] [═]      │  Grid, Vertical, Horizontal layout
│ [🗺️] Minimap     │  Toggle minimap
└───────────────────┘
```

### **2. Context Menu (right-click):**
```
┌─────────────────────────┐
│ ✏️  Edit Properties      │
│ 📋 Copy            ⌘C   │
│ 📄 Duplicate       ⌘D   │
│ 🗑️  Delete         ⌫    │
│ ─────────────────────── │
│ 🔗 Add Connection       │
│ 🔌 Disconnect           │
│ ─────────────────────── │
│ 📚 View Documentation   │
│ 🔍 Find Usages          │
└─────────────────────────┘
```

### **3. Toolbar (top):**
```
┌─────────────────────────────────────────────────────────┐
│ [▶️ Run] [⏸️ Pause] [⏹️ Stop] [✓ Validate]             │
│ [💾 Save] [📂 Open] [🆕 New] │ [↶ Undo] [↷ Redo]      │
│                                │ Zoom: [100% ▼]         │
└─────────────────────────────────────────────────────────┘
```

---

## 📝 **Implementation Priority**

### **Sprint 1 (3-4 days):**
1. ✅ Fix Plugin Palette loading (CURRENT - in progress)
2. ❌ Enhanced Property Panel (validation, variable picker)
3. ❌ Inline editing (double-click on node)

### **Sprint 2 (3-4 days):**
1. ❌ Connection types (sequential, conditional, parallel)
2. ❌ FORK/JOIN nodes
3. ❌ Visual line styles for connections

### **Sprint 3 (2-3 days):**
1. ❌ Template library
2. ❌ Proper Undo/Redo stack
3. ❌ Copy/Paste with variable updates

### **Sprint 4 (2-3 days):**
1. ❌ Minimap implementation
2. ❌ Canvas controls (zoom buttons, layout buttons)
3. ❌ Context menu enhancements

---

## 🔥 **CURRENT ISSUE: Plugin Palette nie ładuje**

**Status:** Debugging with TDD tests + debug logs

**Debug Plan:**
1. ✅ Dodane logi w `PluginPalette.ts`
2. ✅ Dodane logi w `App.ts`
3. ✅ Napisane testy TDD (PluginPalette.integration.test.ts)
4. ✅ Full rebuild (mvn clean install)
5. ⏳ Czekam na logi z konsoli przeglądarki

**Oczekiwane logi:**
```
🚀 [App] Step 3: Initializing Plugin Palette...
🔌 [PluginPalette] init() started - fetching from API...
🔌 [PluginPalette] Response status: 200
🔌 [PluginPalette] Received data: 27 plugins
✅ [PluginPalette] Loaded 27 plugins from API!
🎨 [PluginPalette] render() started
🎨 [PluginPalette] Templates count: 27
✅ [PluginPalette] Render complete! 8 categories, 27 plugins
```

---

## 📚 **References**

- **Conductor GitHub:** https://github.com/conductor-oss/conductor/
- **Conductor UI:** https://github.com/conductor-oss/conductor/tree/main/ui
- **Conductor Docs:** https://conductor-oss.org/
- **Apple HIG:** https://developer.apple.com/design/human-interface-guidelines/
- **React Flow (similar library):** https://reactflow.dev/

---

## 🎯 **Success Metrics**

### **Must Have (MVP):**
- ✅ Visual mode działa
- ✅ Drag & drop z palety
- ⏳ Plugin Palette ładuje się z API
- ❌ Property Panel z validacją
- ❌ Manual connections działają
- ❌ XML ↔ Visual sync 100%

### **Nice to Have:**
- ❌ Inline editing
- ❌ Template library
- ❌ Undo/Redo
- ❌ Minimap
- ❌ FORK/JOIN nodes

### **Future:**
- ❌ Collaborative editing (WebSocket)
- ❌ AI suggestions
- ❌ Workflow templates
- ❌ Export to PNG/SVG

---

**Created:** October 28, 2025  
**Status:** Analysis Complete + Sprint 1 in progress  
**Next:** Fix Plugin Palette, then Enhanced Property Panel  

**TDD Approach:** ✅ 100%  
**Apple HIG Compliance:** ✅ Yes  
**Conductor-Inspired:** ✅ Yes




