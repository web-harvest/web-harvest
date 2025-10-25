# WebHarvest Low-Code Visual Designer - Design Document

**Project:** WebHarvest IDE Visual Configuration Designer  
**Version:** 1.0 (Specification)  
**Date:** October 24, 2025  
**Status:** Design Phase  
**Author:** WebHarvest Team

---

## 🎯 Executive Summary

This document specifies the design and implementation of a **low-code visual configuration designer** for WebHarvest IDE, enabling users to build web scraping workflows through a drag-and-drop graphical interface similar to Node-RED, Apache NiFi, and Azure Logic Apps.

### Key Features
- ✅ **Visual Canvas** - Node-RED/NiFi-style flow editor
- ✅ **Drag-and-Drop Plugins** - All 42+ core plugins available in palette
- ✅ **Property Panels** - Auto-generated from plugin metadata/schema
- ✅ **Bidirectional Sync** - XML ↔ Visual (Azure Logic Apps style)
- ✅ **Context Help** - Inline documentation links per plugin
- ✅ **Apple HIG Compliant** - Modern, native-looking design
- ✅ **VSCode Compatible** - Architecture supports future VSCode extension

---

## 📐 Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     WebHarvest IDE                              │
│                                                                 │
│  ┌───────────────┐         ┌───────────────┐                  │
│  │  XML Editor   │◄───────►│ Visual Canvas │                  │
│  │  (Monaco)     │  Sync   │  (Fabric.js)  │                  │
│  └───────────────┘         └───────────────┘                  │
│         │                          │                            │
│         │                          │                            │
│         ▼                          ▼                            │
│  ┌──────────────────────────────────────────┐                 │
│  │     Configuration Manager                │                 │
│  │  - XML Parsing/Generation                │                 │
│  │  - Visual Model ↔ XML Sync               │                 │
│  │  - Validation                             │                 │
│  └──────────────────────────────────────────┘                 │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────────────────────────────────┐                 │
│  │     Plugin Metadata Service              │                 │
│  │  - 42+ Plugin Definitions                │                 │
│  │  - Configuration Schemas                 │                 │
│  │  - Help Documentation URLs               │                 │
│  └──────────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
```

### Component Stack

```
Frontend Layer
├─ Visual Canvas (Fabric.js / Rete.js)
│  ├─ Node rendering
│  ├─ Connection management
│  ├─ Zoom/Pan controls
│  └─ Selection handling
│
├─ Plugin Palette (Searchable sidebar)
│  ├─ Category groups
│  ├─ Drag sources
│  └─ Quick search
│
├─ Property Panel (Dynamic forms)
│  ├─ Auto-generated from schema
│  ├─ Validation feedback
│  ├─ Template editor
│  └─ Help integration
│
├─ XML Editor (Monaco Editor)
│  ├─ Syntax highlighting
│  ├─ Auto-completion
│  └─ Error markers
│
└─ Mode Switcher (View toggle)
   ├─ Visual mode
   ├─ XML mode
   └─ Split mode

Backend Layer
├─ /api/plugins/metadata (GET)
│  └─ Returns all plugin schemas
│
├─ /api/config/parse (POST)
│  └─ XML → Visual model
│
├─ /api/config/generate (POST)
│  └─ Visual model → XML
│
└─ /api/plugins/help/{pluginName} (GET)
   └─ Returns help URL for plugin
```

---

## 🎨 User Interface Design

### 1. Main Layout (Visual Mode)

```
┌──────────────────────────────────────────────────────────────────┐
│  🌺 WebHarvest IDE         [XML View] [Split View] [Visual View] │
├──────────────────────────────────────────────────────────────────┤
│ ┌──────────────┬────────────────────────────────┬──────────────┐│
│ │              │                                │              ││
│ │  Plugin      │     Visual Canvas             │  Properties  ││
│ │  Palette     │                               │   Panel      ││
│ │              │                                │              ││
│ │ 🔍 Search    │   ┌─────┐                     │  📝 http     ││
│ │              │   │HTTP │─────┐               │              ││
│ │ HTTP & Web   │   └─────┘     │               │  URL:        ││
│ │  • http      │               ▼               │  [________]  ││
│ │  • xpath     │        ┌────────┐             │              ││
│ │  • html-to-  │        │ XPath  │             │  Method:     ││
│ │    xml       │        └────────┘             │  [GET ▼]    ││
│ │              │               │                │              ││
│ │ Control Flow │               ▼               │  Timeout:    ││
│ │  • if        │        ┌────────┐             │  [30000]     ││
│ │  • loop      │        │  File  │             │              ││
│ │  • while     │        └────────┘             │  [Run]       ││
│ │              │                                │  [Validate]  ││
│ │ Data Process │                               │              ││
│ │  • json-to-  │                               │  📄 Help     ││
│ │    xml       │                               │              ││
│ │  • regexp    │  [+] [⊕] [↻] [⚙] 100%        │              ││
│ └──────────────┴────────────────────────────────┴──────────────┘│
│ Status: Ready • 12 nodes • 15 connections • validation OK      │
└──────────────────────────────────────────────────────────────────┘
```

### 2. Split View Mode (XML + Visual)

```
┌──────────────────────────────────────────────────────────────────┐
│  🌺 WebHarvest IDE         [XML View] [Split View] [Visual View] │
├──────────────────────────────────────────────────────────────────┤
│ ┌────────────────────────────┬───────────────────────────────────┐│
│ │ XML Editor                 │ Visual Canvas                     ││
│ │                            │                                   ││
│ │ <?xml version="1.0"?>      │   ┌─────┐                        ││
│ │ <config>                   │   │HTTP │─────┐                  ││
│ │   <def var="url">          │   └─────┘     │                  ││
│ │     https://example.com    │               ▼                  ││
│ │   </def>                   │        ┌────────┐                ││
│ │   <def var="html">         │        │ XPath  │                ││
│ │     <http url="${url}"/>   │        └────────┘                ││
│ │   </def>                   │               │                  ││
│ │   <xpath expr="//title">   │               ▼                  ││
│ │     <get var="html"/>      │        ┌────────┐                ││
│ │   </xpath>                 │        │  Def   │                ││
│ │ </config>                  │        └────────┘                ││
│ │                            │                                   ││
│ │ [↻ Sync from Visual]      │ [↻ Sync from XML]                ││
│ └────────────────────────────┴───────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

### 3. Node Appearance (Apple HIG Style)

```
┌──────────────────────────┐
│  🌐 HTTP Request         │  ← Icon + Name
├──────────────────────────┤
│  URL: example.com        │  ← Primary config preview
│  Method: GET             │  ← Secondary info
├──────────────────────────┤
│  ○ Input                 │  ← Connection ports
│  ● Output                │
└──────────────────────────┘
   Status: ✓ Configured
```

**Node States:**
- ⚪ **Default** - Gray outline
- 🟣 **Selected** - Purple outline (brand color)
- 🔴 **Error** - Red outline with error icon
- 🟡 **Warning** - Yellow outline
- 🟢 **Success** - Green checkmark (after execution)

### 4. Property Panel (Auto-Generated)

Property panel is **auto-generated** from `PluginConfigurationSchema`:

```typescript
interface PropertyPanelConfig {
  pluginName: string;
  metadata: PluginMetadata;
  schema: PluginConfigurationSchema;
  currentValues: Record<string, any>;
}

// Example for HTTP Plugin
{
  pluginName: "http",
  metadata: {
    name: "HTTP Request",
    description: "Performs HTTP requests",
    category: "HTTP & Web",
    helpUrl: "https://web-harvest.sourceforge.io/documentation/plugin-http.html"
  },
  schema: {
    attributes: [
      {
        name: "url",
        type: "string",
        required: true,
        description: "Target URL",
        placeholder: "https://example.com"
      },
      {
        name: "method",
        type: "enum",
        required: false,
        defaultValue: "GET",
        options: ["GET", "POST", "PUT", "DELETE"],
        description: "HTTP method"
      },
      {
        name: "timeout",
        type: "number",
        required: false,
        defaultValue: 30000,
        min: 0,
        max: 300000,
        description: "Request timeout in ms"
      }
    ],
    allowsBody: true,
    bodyType: "template"
  }
}
```

**Property Panel Rendering:**

```
┌───────────────────────────────┐
│ HTTP Request                  │
│ ───────────────────────────  │
│                               │
│ URL * (required)              │
│ ┌───────────────────────────┐│
│ │ https://example.com       ││
│ └───────────────────────────┘│
│ ℹ️ Target URL for request    │
│                               │
│ Method                        │
│ ┌─────────┐                  │
│ │ GET   ▼ │                  │
│ └─────────┘                  │
│                               │
│ Timeout (ms)                  │
│ ┌───────────────────────────┐│
│ │ 30000                     ││
│ └───────────────────────────┘│
│                               │
│ ☐ Follow Redirects            │
│                               │
│ Advanced ▶                    │
│                               │
│ ┌───────────────────────────┐│
│ │ 📄 View Documentation     ││
│ └───────────────────────────┘│
│                               │
│ ┌──────────┐  ┌────────────┐│
│ │  Apply   │  │   Cancel   ││
│ └──────────┘  └────────────┘│
└───────────────────────────────┘
```

---

## 🔌 Plugin Palette Organization

### Category Structure

Plugins are organized by **category** from metadata:

```typescript
interface PluginPalette {
  categories: {
    "HTTP & Web": [
      { name: "http", icon: "🌐", description: "HTTP Request" },
      { name: "html-to-xml", icon: "🔄", description: "HTML Parser" },
      { name: "webbrowser", icon: "🌍", description: "Browser Automation" }
    ],
    "Data Extraction": [
      { name: "xpath", icon: "🔍", description: "XPath Query" },
      { name: "regexp", icon: "📝", description: "Regular Expression" },
      { name: "xquery", icon: "📊", description: "XQuery" }
    ],
    "Control Flow": [
      { name: "if", icon: "🔀", description: "Conditional" },
      { name: "loop", icon: "🔁", description: "Loop" },
      { name: "while", icon: "↻", description: "While Loop" },
      { name: "try", icon: "🛡️", description: "Try/Catch" }
    ],
    "Variables": [
      { name: "def", icon: "📦", description: "Define Variable" },
      { name: "get", icon: "📤", description: "Get Variable" }
    ],
    "Data Processing": [
      { name: "json-to-xml", icon: "🔄", description: "JSON to XML" },
      { name: "xml-to-json", icon: "🔄", description: "XML to JSON" },
      { name: "xslt", icon: "⚙️", description: "XSLT Transform" }
    ],
    "File Operations": [
      { name: "file", icon: "📁", description: "File I/O" }
    ],
    "Database": [
      { name: "connection", icon: "🔌", description: "DB Connection" },
      { name: "database", icon: "🗄️", description: "SQL Query" }
    ],
    "Communication": [
      { name: "mail", icon: "📧", description: "Send Email" },
      { name: "ftp", icon: "📡", description: "FTP Operations" }
    ]
  }
}
```

### Search & Filter

```typescript
class PluginSearch {
  // Search by: name, category, tags, description
  search(query: string): Plugin[] {
    return plugins.filter(p => 
      p.name.includes(query) ||
      p.description.includes(query) ||
      p.tags.some(t => t.includes(query)) ||
      p.category.includes(query)
    );
  }
}
```

**Example:**
- Search: `"json"` → Returns: json-to-xml, xml-to-json
- Search: `"extract"` → Returns: xpath, regexp, xquery (category: "Data Extraction")

---

## 🔄 Bidirectional Synchronization (XML ↔ Visual)

### Architecture: Azure Logic Apps Pattern

Like Azure Logic Apps, we maintain **three representations**:

1. **Visual Model** (in-memory JavaScript objects)
2. **XML Configuration** (Monaco Editor text)
3. **Execution Model** (backend WebHarvest format)

```typescript
interface VisualNode {
  id: string;                    // Unique node ID (generated)
  type: string;                  // Plugin name (e.g., "http")
  position: { x: number, y: number };
  config: Record<string, any>;   // Plugin configuration
  connections: {
    inputs: string[];            // IDs of source nodes
    outputs: string[];           // IDs of target nodes
  };
}

interface VisualModel {
  nodes: VisualNode[];
  connections: Connection[];
  metadata: {
    version: string;
    created: string;
    modified: string;
  };
}
```

### Synchronization Flow

#### XML → Visual (Parse & Render)

```typescript
class XmlToVisualConverter {
  convert(xmlString: string): VisualModel {
    // 1. Parse XML using DOMParser
    const doc = new DOMParser().parseFromString(xmlString, 'text/xml');
    
    // 2. Traverse DOM tree
    const nodes: VisualNode[] = [];
    const connections: Connection[] = [];
    let x = 50, y = 50;
    
    const traverse = (element: Element, parentId?: string) => {
      const nodeId = generateId();
      
      // Extract plugin type and config
      const type = element.tagName;
      const config = extractAttributes(element);
      
      // Create visual node
      nodes.push({
        id: nodeId,
        type: type,
        position: { x: x, y: y },
        config: config,
        connections: { inputs: [], outputs: [] }
      });
      
      // Create connection from parent
      if (parentId) {
        connections.push({
          id: generateId(),
          source: parentId,
          target: nodeId
        });
      }
      
      // Auto-layout: move to next position
      y += 120;
      if (y > 600) { x += 200; y = 50; }
      
      // Traverse children
      for (const child of element.children) {
        traverse(child, nodeId);
      }
    };
    
    traverse(doc.documentElement);
    
    return { nodes, connections, metadata: {} };
  }
}
```

#### Visual → XML (Generate)

```typescript
class VisualToXmlConverter {
  convert(model: VisualModel): string {
    // 1. Build execution tree from visual connections
    const tree = buildExecutionTree(model);
    
    // 2. Generate XML recursively
    const generate = (node: TreeNode, indent: number = 0): string => {
      const spaces = ' '.repeat(indent * 2);
      const tagName = node.type;
      const attrs = formatAttributes(node.config);
      
      let xml = `${spaces}<${tagName}${attrs}`;
      
      if (node.children.length === 0 && !node.textContent) {
        xml += '/>\n';
      } else {
        xml += '>\n';
        
        // Text content
        if (node.textContent) {
          xml += `${spaces}  ${node.textContent}\n`;
        }
        
        // Children
        for (const child of node.children) {
          xml += generate(child, indent + 1);
        }
        
        xml += `${spaces}</${tagName}>\n`;
      }
      
      return xml;
    };
    
    let xml = '<?xml version="1.0" encoding="UTF-8"?>\n';
    xml += '<config xmlns="http://org.webharvest/schema/2.1/core">\n';
    xml += generate(tree.root, 1);
    xml += '</config>';
    
    return xml;
  }
}
```

### Auto-Sync Strategy

```typescript
class ConfigurationSynchronizer {
  private xmlEditor: Monaco.Editor;
  private visualCanvas: Canvas;
  private syncMode: 'auto' | 'manual' = 'manual';
  
  setupSyncListeners() {
    // XML → Visual sync (on demand)
    xmlEditor.onDidChangeContent(debounce(() => {
      if (this.syncMode === 'auto') {
        this.syncFromXml();
      }
    }, 500));
    
    // Visual → XML sync (on demand)
    visualCanvas.on('change', debounce(() => {
      if (this.syncMode === 'auto') {
        this.syncFromVisual();
      }
    }, 500));
  }
  
  syncFromXml() {
    try {
      const xml = xmlEditor.getValue();
      const model = xmlToVisualConverter.convert(xml);
      visualCanvas.loadModel(model);
      this.showNotification('✓ Synced from XML', 'success');
    } catch (error) {
      this.showNotification('⚠️ XML parse error: ' + error.message, 'error');
    }
  }
  
  syncFromVisual() {
    try {
      const model = visualCanvas.getModel();
      const xml = visualToXmlConverter.convert(model);
      xmlEditor.setValue(xml);
      this.showNotification('✓ Synced to XML', 'success');
    } catch (error) {
      this.showNotification('⚠️ Model conversion error: ' + error.message, 'error');
    }
  }
}
```

**User Experience:**
- **Manual Mode** (default): Explicit sync buttons
  - `[↻ Sync from XML]` - Updates visual from XML
  - `[↻ Sync to XML]` - Updates XML from visual
- **Auto Mode** (optional): Real-time sync with 500ms debounce

---

## 🎨 Visual Design System (Apple HIG Compliance)

### Design Principles

Following Apple Human Interface Guidelines:

1. **Clarity** - Clean, minimal UI with clear affordances
2. **Deference** - Content-focused, subtle chrome
3. **Depth** - Layered interface with shadows and transitions
4. **Consistency** - Uniform spacing, colors, typography

### Color Palette (WebHarvest Brand)

From `enterprise.css`:

```css
/* Primary Colors */
--color-primary: #7E57C2;      /* Purple */
--color-secondary: #D81B60;     /* Magenta */
--color-accent: #FFC107;        /* Yellow */

/* Node States */
--node-default: #9E9E9E;        /* Gray */
--node-selected: #7E57C2;       /* Purple */
--node-error: #E53935;          /* Red */
--node-success: #43A047;        /* Green */
--node-warning: #FB8C00;        /* Orange */

/* Canvas */
--canvas-background: #FAFAFA;   /* Light gray */
--canvas-grid: #E0E0E0;         /* Grid lines */

/* UI Elements */
--panel-background: #FFFFFF;
--panel-border: #E0E0E0;
--text-primary: #212121;
--text-secondary: #757575;
```

### Typography

```css
/* Following Apple SF Pro / San Francisco */
--font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 
               'Helvetica Neue', Arial, sans-serif;

/* Sizes */
--font-size-small: 12px;
--font-size-body: 14px;
--font-size-heading: 16px;
--font-size-title: 20px;

/* Weights */
--font-weight-regular: 400;
--font-weight-medium: 500;
--font-weight-semibold: 600;
```

### Spacing (8pt Grid)

```css
--spacing-xs: 4px;
--spacing-sm: 8px;
--spacing-md: 16px;
--spacing-lg: 24px;
--spacing-xl: 32px;
```

### Shadows (Depth)

```css
/* Elevation levels */
--shadow-1: 0 1px 3px rgba(0,0,0,0.12);
--shadow-2: 0 2px 6px rgba(0,0,0,0.16);
--shadow-3: 0 4px 12px rgba(0,0,0,0.20);
--shadow-4: 0 8px 24px rgba(0,0,0,0.24);
```

### Animations

```css
/* Smooth, natural motion */
--transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-normal: 250ms cubic-bezier(0.4, 0, 0.2, 1);
--transition-slow: 400ms cubic-bezier(0.4, 0, 0.2, 1);
```

### Component Examples

**Button:**
```css
.btn-primary {
  background: linear-gradient(135deg, #7E57C2, #D81B60);
  color: white;
  padding: 8px 16px;
  border-radius: 8px;
  font-weight: 500;
  box-shadow: var(--shadow-2);
  transition: all var(--transition-normal);
}

.btn-primary:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-3);
}

.btn-primary:active {
  transform: translateY(0);
  box-shadow: var(--shadow-1);
}
```

**Node:**
```css
.visual-node {
  background: white;
  border: 2px solid var(--node-default);
  border-radius: 12px;
  padding: 12px;
  box-shadow: var(--shadow-2);
  transition: all var(--transition-normal);
}

.visual-node:hover {
  box-shadow: var(--shadow-3);
}

.visual-node.selected {
  border-color: var(--node-selected);
  box-shadow: 0 0 0 4px rgba(126, 87, 194, 0.2);
}

.visual-node.error {
  border-color: var(--node-error);
  animation: shake 0.4s;
}
```

**Property Panel:**
```css
.property-panel {
  background: var(--panel-background);
  border-left: 1px solid var(--panel-border);
  box-shadow: var(--shadow-2);
  overflow-y: auto;
}

.property-group {
  padding: var(--spacing-md);
  border-bottom: 1px solid var(--panel-border);
}

.property-label {
  font-size: var(--font-size-small);
  font-weight: var(--font-weight-medium);
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--spacing-xs);
}

.property-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--panel-border);
  border-radius: 6px;
  font-size: var(--font-size-body);
  transition: border-color var(--transition-fast);
}

.property-input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(126, 87, 194, 0.1);
}
```

---

## 🛠️ Technology Stack

### Frontend Technologies

```typescript
// Core Libraries
const techStack = {
  // Canvas/Flow Editor
  canvas: 'Fabric.js' | 'Rete.js' | 'React Flow',
  
  // UI Framework
  ui: 'Vanilla JS' | 'React 18',
  
  // XML Editor
  editor: 'Monaco Editor',
  
  // State Management
  state: 'Zustand' | 'Redux Toolkit',
  
  // HTTP Client
  http: 'Fetch API',
  
  // Validation
  validation: 'Zod' | 'Yup',
  
  // Build Tool
  build: 'Vite'
};
```

### Recommended: Rete.js

**Why Rete.js?**
- ✅ Built specifically for node-based editors
- ✅ Plugin architecture (aligns with WebHarvest)
- ✅ TypeScript support
- ✅ Custom node rendering
- ✅ Connection validation
- ✅ Undo/redo built-in
- ✅ Active development

**Example Rete.js Node:**
```typescript
import Rete from 'rete';

class HttpNode extends Rete.Component {
  constructor() {
    super('HTTP Request');
  }
  
  builder(node: Rete.Node) {
    // Inputs
    const inp = new Rete.Input('trigger', 'Trigger', socketAny);
    
    // Outputs
    const out = new Rete.Output('result', 'Result', socketAny);
    
    // Controls (property inputs)
    const urlControl = new TextControl(this.editor, 'url', 'URL');
    const methodControl = new SelectControl(this.editor, 'method', 
      ['GET', 'POST', 'PUT', 'DELETE']);
    
    return node
      .addInput(inp)
      .addControl(urlControl)
      .addControl(methodControl)
      .addOutput(out);
  }
  
  worker(node: NodeData, inputs: WorkerInputs, outputs: WorkerOutputs) {
    // Execution logic (if running in editor)
    const url = node.data.url;
    const method = node.data.method;
    // ... HTTP request logic
  }
}
```

### Alternative: React Flow

**Pros:**
- ✅ Excellent React integration
- ✅ Great performance
- ✅ Beautiful default styling
- ✅ Good documentation

**Cons:**
- ⚠️ Requires React (current IDE is Vanilla JS)
- ⚠️ More opinionated

---

## 📦 Backend API Enhancements

### New API Endpoints

#### 1. GET /api/plugins/metadata

Returns metadata for all plugins.

**Response:**
```json
{
  "plugins": [
    {
      "id": "http",
      "name": "HTTP Request",
      "elementName": "http",
      "description": "Performs HTTP requests",
      "category": "HTTP & Web",
      "icon": "🌐",
      "version": "2.2.0",
      "author": "WebHarvest Team",
      "tags": ["http", "web", "request"],
      "helpUrl": "https://web-harvest.sourceforge.io/documentation/plugin-http.html",
      "schema": {
        "attributes": [
          {
            "name": "url",
            "type": "string",
            "required": true,
            "description": "Target URL",
            "defaultValue": null
          },
          {
            "name": "method",
            "type": "enum",
            "required": false,
            "options": ["GET", "POST", "PUT", "DELETE"],
            "defaultValue": "GET"
          },
          {
            "name": "timeout",
            "type": "number",
            "required": false,
            "min": 0,
            "max": 300000,
            "defaultValue": 30000
          }
        ],
        "allowsBody": true,
        "allowedSubElements": ["http-header", "http-param"],
        "requiredSubElements": []
      }
    }
    // ... more plugins
  ]
}
```

**Implementation:**
```java
@WebServlet("/api/plugins/metadata")
public class PluginMetadataServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        // Get all registered plugins
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        Collection<PluginMetadata> plugins = registry.getAllPluginMetadata();
        
        // Convert to JSON
        Gson gson = new Gson();
        String json = gson.toJson(Map.of("plugins", plugins));
        
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }
}
```

#### 2. POST /api/config/parse

Parses XML configuration to visual model.

**Request:**
```json
{
  "xml": "<?xml version=\"1.0\"?>..."
}
```

**Response:**
```json
{
  "model": {
    "nodes": [
      {
        "id": "node_1",
        "type": "http",
        "position": { "x": 50, "y": 50 },
        "config": {
          "url": "https://example.com",
          "method": "GET"
        }
      }
    ],
    "connections": [
      {
        "id": "conn_1",
        "source": "node_1",
        "target": "node_2"
      }
    ]
  },
  "errors": []
}
```

#### 3. POST /api/config/generate

Generates XML from visual model.

**Request:**
```json
{
  "model": {
    "nodes": [...],
    "connections": [...]
  }
}
```

**Response:**
```json
{
  "xml": "<?xml version=\"1.0\"?>...",
  "valid": true,
  "errors": []
}
```

---

## 🔗 Context-Sensitive Help Integration

### Help URL Strategy

Each plugin metadata includes `helpUrl`:

```typescript
interface PluginMetadata {
  helpUrl: string;  // e.g., "https://web-harvest.sourceforge.io/documentation/plugin-http.html"
}
```

### Help Panel Implementation

```typescript
class HelpPanel {
  show(pluginName: string) {
    // Get plugin metadata
    const plugin = pluginRegistry.getPlugin(pluginName);
    
    // Show help in sidebar or modal
    const helpUrl = plugin.metadata.helpUrl;
    
    // Option 1: Inline iframe
    this.showInline(helpUrl);
    
    // Option 2: External link
    this.openExternal(helpUrl);
    
    // Option 3: Embedded docs
    this.showEmbedded(plugin.metadata.documentation);
  }
  
  showInline(url: string) {
    // Display iframe in property panel
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.className = 'help-frame';
    propertyPanel.appendChild(iframe);
  }
}
```

### Help Button in Property Panel

```html
<div class="property-panel">
  <h3>HTTP Request</h3>
  
  <!-- Property fields -->
  <input name="url" />
  <select name="method"></select>
  
  <!-- Help button -->
  <button class="btn-help" onclick="helpPanel.show('http')">
    📄 View Documentation
  </button>
</div>
```

---

## 📱 VSCode Extension Compatibility

### Architecture Alignment

The visual designer is architected to support **future VSCode extension**:

```
WebHarvest Visual Designer
├─ Core Logic (Shared)
│  ├─ Plugin metadata parsing
│  ├─ XML ↔ Visual conversion
│  ├─ Validation logic
│  └─ Configuration management
│
├─ Web IDE Frontend
│  ├─ Rete.js canvas
│  ├─ Monaco Editor
│  └─ Web-specific UI
│
└─ VSCode Extension (Future)
   ├─ VSCode Webview (Rete.js canvas)
   ├─ VSCode Editor API
   └─ VSCode-specific UI
```

### Shared Core Module

```typescript
// @webharvest/visual-designer-core
export class VisualDesignerCore {
  // Portable, framework-agnostic logic
  
  parseXml(xml: string): VisualModel { ... }
  generateXml(model: VisualModel): string { ... }
  validateModel(model: VisualModel): ValidationResult { ... }
  
  // Can be used in:
  // - Web IDE (browser)
  // - VSCode extension (Electron/Node.js)
  // - CLI tool (Node.js)
}
```

### VSCode Extension Structure

```
webharvest-vscode/
├── package.json
├── src/
│   ├── extension.ts          # Main entry point
│   ├── webview/
│   │   ├── VisualEditor.svelte   # Webview UI (Rete.js)
│   │   └── style.css
│   ├── providers/
│   │   ├── ConfigEditorProvider.ts
│   │   └── CompletionProvider.ts
│   └── core/
│       └── index.ts          # Import @webharvest/visual-designer-core
└── resources/
    └── icons/
```

### VSCode Extension Features

```typescript
// extension.ts
export function activate(context: vscode.ExtensionContext) {
  // Register custom editor for .xml files
  const provider = new WebHarvestEditorProvider(context);
  const registration = vscode.window.registerCustomEditorProvider(
    'webharvest.visualEditor',
    provider,
    { webviewOptions: { retainContextWhenHidden: true } }
  );
  
  context.subscriptions.push(registration);
  
  // Register commands
  context.subscriptions.push(
    vscode.commands.registerCommand('webharvest.switchToVisual', () => {
      // Switch to visual mode
    }),
    vscode.commands.registerCommand('webharvest.switchToXml', () => {
      // Switch to XML mode
    })
  );
}
```

---

## 🚀 Implementation Phases

### Phase 1: Foundation (4 weeks)

**Goals:**
- ✅ Canvas infrastructure
- ✅ Basic node rendering
- ✅ Plugin metadata API
- ✅ Simple drag-and-drop

**Tasks:**
1. Setup Rete.js in IDE
2. Create base node component
3. Implement plugin palette
4. Create metadata servlet
5. Basic node-to-node connections

**Deliverables:**
- Working canvas with 5 sample plugins
- Drag-and-drop from palette
- Node connections
- Property panel (static)

### Phase 2: Configuration & Properties (3 weeks)

**Goals:**
- ✅ Dynamic property panels
- ✅ Schema-based form generation
- ✅ Validation

**Tasks:**
1. Auto-generate property forms from schema
2. Implement property validation
3. Add template editor for body content
4. Connection port validation
5. Node state management

**Deliverables:**
- Fully functional property panels
- All 42+ plugins supported
- Validation feedback

### Phase 3: XML Synchronization (4 weeks)

**Goals:**
- ✅ XML ↔ Visual conversion
- ✅ Bidirectional sync
- ✅ Error handling

**Tasks:**
1. XML parser (DOM-based)
2. Visual-to-XML generator
3. Tree layout algorithm
4. Sync API endpoints
5. Conflict resolution

**Deliverables:**
- XML → Visual import
- Visual → XML export
- Manual sync buttons
- Split view mode

### Phase 4: Polish & UX (3 weeks)

**Goals:**
- ✅ Apple HIG compliance
- ✅ Help integration
- ✅ Advanced features

**Tasks:**
1. Apply design system
2. Add animations/transitions
3. Help panel implementation
4. Keyboard shortcuts
5. Accessibility (WCAG 2.1)
6. Undo/redo
7. Auto-layout improvements

**Deliverables:**
- Polished, production-ready UI
- Context help system
- Keyboard navigation
- Screen reader support

### Phase 5: Advanced Features (4 weeks)

**Goals:**
- ✅ Auto-sync mode
- ✅ Templates & snippets
- ✅ Testing integration

**Tasks:**
1. Auto-sync with debouncing
2. Configuration templates library
3. Inline execution preview
4. Error highlighting
5. Performance optimization

**Deliverables:**
- Auto-sync mode
- Template library
- Enhanced testing workflow

### Phase 6: VSCode Extension (6 weeks)

**Goals:**
- ✅ Port to VSCode
- ✅ Marketplace publication

**Tasks:**
1. Extract core to shared module
2. Create VSCode extension scaffold
3. Implement custom editor provider
4. Adapt UI for VSCode webview
5. Testing & packaging
6. Marketplace submission

**Deliverables:**
- VSCode extension
- Published to Marketplace
- Documentation

**Total Timeline:** ~24 weeks (6 months)

---

## 📊 Success Metrics

### User Experience Metrics

- **Learning Curve:** New users can create simple scraper in < 5 minutes
- **Productivity:** 50% faster configuration vs. XML-only
- **Error Rate:** 80% reduction in XML syntax errors
- **Satisfaction:** 4.5+ / 5.0 user rating

### Technical Metrics

- **Performance:** Canvas with 100 nodes @ 60 FPS
- **Sync Speed:** XML ↔ Visual sync < 500ms
- **Validation:** Real-time validation < 100ms
- **Load Time:** Initial canvas load < 2s

### Adoption Metrics

- **IDE Usage:** 60%+ of users prefer visual mode
- **VSCode Extension:** 1000+ installs in first month
- **Help Usage:** 40%+ users access inline help
- **Templates:** 50+ community templates

---

## 🔒 Security & Validation

### Configuration Validation

```typescript
class ConfigurationValidator {
  validate(model: VisualModel): ValidationResult {
    const errors: ValidationError[] = [];
    
    // 1. Schema validation
    for (const node of model.nodes) {
      const plugin = pluginRegistry.getPlugin(node.type);
      const schema = plugin.metadata.schema;
      
      // Validate required attributes
      for (const attr of schema.requiredAttributes) {
        if (!node.config[attr]) {
          errors.push({
            nodeId: node.id,
            field: attr,
            message: `Required attribute '${attr}' is missing`
          });
        }
      }
      
      // Validate attribute types
      for (const [key, value] of Object.entries(node.config)) {
        const attrDef = schema.getAttribute(key);
        if (attrDef && !this.validateType(value, attrDef.type)) {
          errors.push({
            nodeId: node.id,
            field: key,
            message: `Invalid type for '${key}', expected ${attrDef.type}`
          });
        }
      }
    }
    
    // 2. Connection validation
    for (const conn of model.connections) {
      const source = model.nodes.find(n => n.id === conn.source);
      const target = model.nodes.find(n => n.id === conn.target);
      
      if (!source || !target) {
        errors.push({
          connectionId: conn.id,
          message: 'Invalid connection: node not found'
        });
      }
    }
    
    // 3. Circular dependency check
    if (this.hasCircularDependency(model)) {
      errors.push({
        message: 'Configuration contains circular dependencies'
      });
    }
    
    return {
      valid: errors.length === 0,
      errors: errors
    };
  }
}
```

### XSS Prevention

```typescript
// Sanitize all user inputs
import DOMPurify from 'dompurify';

class PropertyInput {
  setValue(value: string) {
    // Sanitize HTML/scripts
    const clean = DOMPurify.sanitize(value);
    this.input.value = clean;
  }
}
```

---

## 📚 Documentation Plan

### User Documentation

1. **Quick Start Guide** (5 min read)
   - First visual scraper
   - Basic concepts
   - Common patterns

2. **User Manual** (30 min read)
   - All features explained
   - Property panel reference
   - Keyboard shortcuts
   - Tips & tricks

3. **Video Tutorials** (15 videos, 2-5 min each)
   - Introduction
   - Creating your first scraper
   - Using control flow
   - Data extraction patterns
   - Database integration
   - Testing & debugging

4. **Plugin Reference** (per-plugin pages)
   - Description
   - Configuration options
   - Examples
   - Best practices

### Developer Documentation

1. **Architecture Guide**
   - System overview
   - Component diagram
   - Data flow
   - Extension points

2. **API Reference**
   - REST API endpoints
   - Request/response formats
   - Error codes

3. **Plugin Development**
   - Creating custom plugins
   - Registering metadata
   - Visual representation
   - Testing plugins

4. **Contributing Guide**
   - Code style
   - Pull request process
   - Testing requirements

---

## 🎯 Summary

This design document specifies a comprehensive **low-code visual configuration designer** for WebHarvest IDE that:

✅ **Provides Node-RED/Nifi-style interface** with drag-and-drop plugins  
✅ **Auto-generates property panels** from plugin metadata/schemas  
✅ **Supports bidirectional XML ↔ Visual sync** (Azure Logic Apps pattern)  
✅ **Integrates context-sensitive help** for each plugin  
✅ **Follows Apple Human Interface Guidelines** for modern, native UX  
✅ **Designed for VSCode extension** compatibility (shared core architecture)  
✅ **Uses WebHarvest brand design** (enterprise.css theme)

**Next Steps:**
1. ✅ Review & approve this design document
2. ⏩ Create technical prototype (Phase 1)
3. ⏩ User testing & iteration
4. ⏩ Full implementation (Phases 2-6)

---

**Document Version:** 1.0  
**Last Updated:** October 24, 2025  
**Maintainers:** WebHarvest IDE Team


