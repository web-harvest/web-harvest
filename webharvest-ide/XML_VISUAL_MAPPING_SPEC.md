# XML ↔ Visual Mapping Specification

**CRITICAL:** This document defines the EXACT mapping between XML configuration and Visual canvas.  
**Rule:** Every visual element MUST have a 1:1 correspondence with XML structure.

---

## 🎯 Core Principle

```
XML Element = Visual Node
XML Attributes = Node Properties
XML Children = Child Nodes
XML Body Text = Node Body Content
```

**Bidirectional Conversion MUST be lossless:**
- Visual → XML → Visual = identical
- XML → Visual → XML = identical

---

## 📋 XML Structure Example

### Example 1: Simple HTTP Request
```xml
<config>
  <http url="https://example.com" method="GET">
    <http-header name="User-Agent" value="WebHarvest" />
  </http>
  <xpath expression="//title/text()" />
  <var name="title" />
</config>
```

### Visual Representation:
```
ROOT (config)
├─ http (url="https://example.com", method="GET")
│  └─ http-header (name="User-Agent", value="WebHarvest")
├─ xpath (expression="//title/text()")
└─ var (name="title")
```

### Canvas Layout:
```
┌─ http ─────────────────────────────┐
│ url: https://example.com           │
│ method: GET                         │
│ ────────────────────────────────── │
│   ┌─ http-header ────────────┐    │
│   │ name: User-Agent          │    │
│   │ value: WebHarvest         │    │
│   └───────────────────────────┘    │
└─────────────────────────────────────┘
      ↓
┌─ xpath ────────────────────────────┐
│ expression: //title/text()         │
└─────────────────────────────────────┘
      ↓
┌─ var ──────────────────────────────┐
│ name: title                         │
└─────────────────────────────────────┘
```

---

## 🔄 Mapping Rules

### Rule 1: Element → Node
```typescript
// XML Element
<loop index="i" max="10">
  <http url="${url}" />
</loop>

// Visual Node
{
  id: "node_1",
  type: "loop",
  plugin: LoopPluginMetadata,
  config: {
    index: "i",
    max: "10"
  },
  children: ["node_2"],
  parentId: null
}
```

### Rule 2: Attributes → Node Config
```typescript
// XML
<http url="https://api.example.com" method="POST" timeout="5000" />

// Visual Node
{
  type: "http",
  config: {
    url: "https://api.example.com",
    method: "POST",
    timeout: "5000"
  }
}
```

### Rule 3: Body Content → Node Body
```typescript
// XML with body
<template>
  <html>
    <h1>${title}</h1>
    <p>${description}</p>
  </html>
</template>

// Visual Node
{
  type: "template",
  config: {},
  body: "<html>\n  <h1>${title}</h1>\n  <p>${description}</p>\n</html>"
}
```

### Rule 4: Nesting → Parent-Child
```typescript
// XML nested structure
<loop index="item" list="${items}">
  <http url="${item.url}" />
  <xpath expression="//title" />
  <var name="title" />
</loop>

// Visual Structure
loop (parent=null)
├─ http (parent=loop)
├─ xpath (parent=loop)
└─ var (parent=loop)
```

---

## 🔨 Data Model Requirements

### VisualNode Must Include:
```typescript
interface VisualNode {
  // Identity
  id: string;                    // Generated: "node_1", "node_2"
  type: string;                  // XML element name: "http", "loop"
  
  // Configuration
  config: Record<string, any>;   // XML attributes as key-value
  body: string | null;           // XML body content (for template, etc.)
  
  // Hierarchy (matches XML nesting)
  parentId: string | null;       // Parent node ID (null = root)
  children: string[];            // Child node IDs (in order!)
  
  // Visual (NOT in XML)
  x: number;
  y: number;
  plugin: PluginMetadata;
  executionState: ...
}
```

---

## 🔄 Conversion Algorithms

### XML → Visual Converter

```typescript
function parseXMLToVisual(xmlString: string): VisualNode[] {
  const parser = new DOMParser();
  const doc = parser.parseFromString(xmlString, 'text/xml');
  const root = doc.documentElement; // <config>
  
  const nodes: VisualNode[] = [];
  let nodeCounter = 0;
  
  function processElement(
    element: Element, 
    parentId: string | null,
    x: number,
    y: number
  ): string {
    const nodeId = `node_${++nodeCounter}`;
    const tagName = element.tagName.toLowerCase();
    
    // Extract attributes
    const config: Record<string, any> = {};
    for (let i = 0; i < element.attributes.length; i++) {
      const attr = element.attributes[i];
      config[attr.name] = attr.value;
    }
    
    // Extract body (text content, excluding child elements)
    let body: string | null = null;
    const textContent = element.textContent?.trim();
    if (textContent && element.children.length === 0) {
      body = textContent;
    }
    
    // Create node
    const node: VisualNode = {
      id: nodeId,
      type: tagName,
      config: config,
      body: body,
      parentId: parentId,
      children: [],
      x: x,
      y: y,
      plugin: getPlugin(tagName),
      executionState: 'pending'
    };
    
    nodes.push(node);
    
    // Process children
    let childY = y + 80;
    Array.from(element.children).forEach(child => {
      const childId = processElement(child, nodeId, x + 40, childY);
      node.children.push(childId);
      childY += 80;
    });
    
    return nodeId;
  }
  
  // Process all root elements (children of <config>)
  let rootY = 50;
  Array.from(root.children).forEach(child => {
    processElement(child, null, 50, rootY);
    rootY += 200;
  });
  
  return nodes;
}
```

### Visual → XML Generator

```typescript
function generateXMLFromVisual(nodes: VisualNode[]): string {
  const doc = document.implementation.createDocument('', '', null);
  const root = doc.createElement('config');
  root.setAttribute('xmlns', 'http://org.webharvest/schema/2.1/core');
  doc.appendChild(root);
  
  function buildElement(node: VisualNode): Element {
    const element = doc.createElement(node.type);
    
    // Add attributes
    Object.entries(node.config).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        element.setAttribute(key, String(value));
      }
    });
    
    // Add children (in order!)
    node.children.forEach(childId => {
      const childNode = nodes.find(n => n.id === childId);
      if (childNode) {
        const childElement = buildElement(childNode);
        element.appendChild(childElement);
      }
    });
    
    // Add body content
    if (node.body) {
      element.textContent = node.body;
    }
    
    return element;
  }
  
  // Build all root nodes
  const rootNodes = nodes.filter(n => !n.parentId);
  rootNodes.forEach(node => {
    const element = buildElement(node);
    root.appendChild(element);
  });
  
  // Serialize to string
  const serializer = new XMLSerializer();
  let xml = serializer.serializeToString(doc);
  
  // Format with indentation
  xml = formatXML(xml);
  
  return xml;
}
```

---

## ✅ Validation Rules

### Rule 1: Order Matters
```xml
<!-- XML order is preserved -->
<loop>
  <http />    <!-- First child -->
  <xpath />   <!-- Second child -->
  <var />     <!-- Third child -->
</loop>
```

**Visual:** Children array order = XML order
```typescript
node.children = ["node_http", "node_xpath", "node_var"]  // MUST maintain order!
```

### Rule 2: Required Attributes
```typescript
// Plugin schema says "url" is required
<http url="..." />  ✅ Valid

// Missing required attribute
<http />  ❌ Invalid - cannot save to XML!
```

**Visual Validation:**
- Before generating XML, check all required attributes
- Show error if required attribute is missing
- Prevent save until fixed

### Rule 3: Allowed Children
```xml
<!-- http can only contain http-header and http-param -->
<http url="...">
  <http-header />  ✅ Valid
  <xpath />        ❌ Invalid - xpath not allowed in http!
</http>
```

**Visual Validation:**
- Use `allowedSubElements` from schema
- Prevent dropping invalid children into containers
- Show error message

### Rule 4: Body Content
```xml
<!-- template allows body -->
<template>
  <html><h1>${title}</h1></html>
</template>

<!-- http does NOT allow body -->
<http url="...">
  Some text here  ❌ Invalid!
</http>
```

**Visual Validation:**
- Check `allowsBody` from schema
- Only show body editor for plugins that allow it
- Prevent setting body on plugins that don't support it

---

## 🎨 Visual Canvas Constraints

### Positioning Strategy
```typescript
// Sequential nodes (siblings)
node1.y = 100;
node2.y = 200;  // 100px spacing
node3.y = 300;

// Children (indented)
parent.x = 100;
child1.x = 140;  // 40px indent
child2.x = 140;
```

**Why:** Preserve visual hierarchy but allow free positioning.

### Auto-Layout Algorithm
```typescript
function autoLayout(nodes: VisualNode[]): void {
  let y = 50;
  const rootNodes = nodes.filter(n => !n.parentId);
  
  function layoutNode(node: VisualNode, x: number, depth: number): number {
    node.x = x;
    node.y = y;
    y += 100;
    
    // Layout children
    node.children.forEach(childId => {
      const child = findNode(childId);
      if (child) {
        layoutNode(child, x + 40, depth + 1);
      }
    });
    
    return y;
  }
  
  rootNodes.forEach(node => layoutNode(node, 50, 0));
}
```

---

## 🧪 Test Cases

### Test 1: Simple Linear Flow
```xml
<config>
  <http url="https://example.com" />
  <xpath expression="//title" />
  <var name="title" />
</config>
```

**Expected Visual:**
- 3 nodes on canvas
- NO parent-child relationships (all siblings)
- Sequential layout (vertical)

### Test 2: Container with Children
```xml
<config>
  <loop index="i" max="10">
    <http url="${url[i]}" />
    <xpath expression="//data" />
  </loop>
</config>
```

**Expected Visual:**
- 1 container node (loop)
- 2 child nodes (http, xpath) INSIDE loop
- Outline shows tree structure

### Test 3: Nested Containers
```xml
<config>
  <loop index="i" max="10">
    <if condition="${i > 5}">
      <http url="${url}" />
    </if>
  </loop>
</config>
```

**Expected Visual:**
- loop (container) at root
- if (container) inside loop
- http (leaf) inside if
- Outline shows 3 levels

### Test 4: Special Children
```xml
<config>
  <http url="https://api.example.com" method="POST">
    <http-header name="Authorization" value="Bearer ${token}" />
    <http-header name="Content-Type" value="application/json" />
    <http-param name="query" value="${searchTerm}" />
  </http>
</config>
```

**Expected Visual:**
- http (container)
- 3 children (2x http-header, 1x http-param)
- Validation: these children ONLY allowed in http

---

## 🚨 Implementation Checkpoint

**BEFORE continuing with UI features, I MUST:**

1. ✅ **Implement XML Parser** (XML → Visual)
2. ✅ **Implement XML Generator** (Visual → XML)
3. ✅ **Test roundtrip conversion** (XML → Visual → XML)
4. ✅ **Validate against real examples**
5. ✅ **Ensure order preservation**
6. ✅ **Handle all attribute types**
7. ✅ **Support body content**

**ONLY THEN** can I safely continue with:
- Connections (must match XML order)
- Property editing (must map to XML attributes)
- Execution tracking (annotate existing XML structure)

---

## 📝 Action Plan

### Phase 1: XML Converters (NOW - 2-3 hours)
1. Create `XMLParser.ts` - parse XML string to VisualNode[]
2. Create `XMLGenerator.ts` - generate XML from VisualNode[]
3. Test with real XML examples from docs/web/examples/
4. Validate roundtrip conversion

### Phase 2: Monaco Integration (1-2 hours)
1. Add Monaco Editor for XML view
2. Implement XML/Visual/Split view switching
3. Sync changes bidirectionally
4. Show validation errors

### Phase 3: Then Continue with UI (4-6 hours)
1. Connections (now safe - follow XML order)
2. Property forms (map to XML attributes)
3. Execution tracking (overlay on XML structure)

---

**STOPPING all UI development until XML conversion is proven working!**

**Next:** Implement XMLParser and XMLGenerator with full testing. 🛑➡️🔄

