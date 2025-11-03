# WebHarvest Visual Designer - Technical Implementation Guide

**Version:** 1.0  
**Date:** October 24, 2025  
**Status:** Implementation Reference

---

## 📋 Table of Contents

1. [Technology Setup](#technology-setup)
2. [Frontend Architecture](#frontend-architecture)
3. [Plugin Metadata System](#plugin-metadata-system)
4. [Visual Canvas Implementation](#visual-canvas-implementation)
5. [Property Panel Generation](#property-panel-generation)
6. [XML-Visual Synchronization](#xml-visual-synchronization)
7. [Backend Integration](#backend-integration)
8. [Testing Strategy](#testing-strategy)

---

## 🚀 Technology Setup

### Project Structure

```
webharvest-ide/
├── src/main/
│   ├── java/org/webharvest/server/
│   │   ├── api/
│   │   │   ├── PluginMetadataServlet.java
│   │   │   ├── ConfigParserServlet.java
│   │   │   └── ConfigGeneratorServlet.java
│   │   └── visual/
│   │       ├── VisualModelConverter.java
│   │       └── LayoutEngine.java
│   └── resources/webapp/
│       ├── visual-designer/
│       │   ├── index.html
│       │   ├── core/
│       │   │   ├── VisualDesigner.js
│       │   │   ├── PluginRegistry.js
│       │   │   ├── ConfigSynchronizer.js
│       │   │   └── ValidationEngine.js
│       │   ├── components/
│       │   │   ├── Canvas.js
│       │   │   ├── PluginPalette.js
│       │   │   ├── PropertyPanel.js
│       │   │   └── VisualNode.js
│       │   ├── converters/
│       │   │   ├── XmlToVisual.js
│       │   │   ├── VisualToXml.js
│       │   │   └── LayoutAlgorithms.js
│       │   └── styles/
│       │       ├── visual-designer.css
│       │       └── nodes.css
│       └── static/
│           └── vendor/
│               ├── rete.min.js
│               └── fabric.min.js
└── package.json (for frontend build)
```

### Dependencies Installation

```bash
# Frontend dependencies (package.json)
npm init -y
npm install --save-dev vite @types/node

# Canvas library (choose one)
npm install rete rete-area-plugin rete-connection-plugin rete-context-menu-plugin
# OR
npm install fabric

# XML parsing
npm install fast-xml-parser

# Validation
npm install zod

# UI utilities
npm install classnames lodash
```

### Build Configuration (vite.config.js)

```javascript
import { defineConfig } from 'vite';
import path from 'path';

export default defineConfig({
  root: 'src/main/resources/webapp/visual-designer',
  base: '/visual-designer/',
  build: {
    outDir: '../../../../../target/classes/webapp/visual-designer',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        main: path.resolve(__dirname, 'src/main/resources/webapp/visual-designer/index.html')
      }
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
```

---

## 🏗️ Frontend Architecture

### Core Classes Overview

```typescript
// Type definitions (types.ts)

interface PluginMetadata {
  id: string;
  name: string;
  elementName: string;
  description: string;
  category: string;
  icon: string;
  version: string;
  author: string;
  tags: string[];
  helpUrl: string;
  schema: PluginConfigurationSchema;
}

interface PluginConfigurationSchema {
  attributes: AttributeDefinition[];
  allowsBody: boolean;
  bodyType?: 'text' | 'template' | 'xml';
  allowedSubElements: string[];
  requiredSubElements: string[];
}

interface AttributeDefinition {
  name: string;
  type: 'string' | 'number' | 'boolean' | 'enum';
  required: boolean;
  defaultValue?: any;
  description: string;
  options?: string[]; // For enum type
  min?: number;       // For number type
  max?: number;
  pattern?: string;   // For string validation
}

interface VisualNode {
  id: string;
  type: string;  // Plugin element name
  position: { x: number; y: number };
  config: Record<string, any>;
  connections: {
    inputs: string[];
    outputs: string[];
  };
  metadata?: {
    label?: string;
    collapsed?: boolean;
  };
}

interface VisualConnection {
  id: string;
  source: string;
  target: string;
  sourcePort?: string;
  targetPort?: string;
}

interface VisualModel {
  version: string;
  nodes: VisualNode[];
  connections: VisualConnection[];
  metadata: {
    name?: string;
    description?: string;
    created: string;
    modified: string;
  };
}
```

### Main Application Class

```typescript
// VisualDesigner.js

class VisualDesigner {
  private canvas: Canvas;
  private palette: PluginPalette;
  private propertyPanel: PropertyPanel;
  private xmlEditor: Monaco.Editor;
  private synchronizer: ConfigSynchronizer;
  private pluginRegistry: PluginRegistry;
  
  constructor(container: HTMLElement) {
    this.initializeComponents(container);
    this.setupEventHandlers();
    this.loadPluginMetadata();
  }
  
  private async initializeComponents(container: HTMLElement) {
    // Create layout
    const layout = this.createLayout(container);
    
    // Initialize canvas
    this.canvas = new Canvas(layout.canvasContainer, {
      width: layout.canvasContainer.clientWidth,
      height: layout.canvasContainer.clientHeight,
      background: '#FAFAFA',
      grid: true
    });
    
    // Initialize plugin palette
    this.palette = new PluginPalette(layout.paletteContainer);
    
    // Initialize property panel
    this.propertyPanel = new PropertyPanel(layout.propertyContainer);
    
    // Initialize XML editor
    this.xmlEditor = await this.initializeMonacoEditor(layout.xmlContainer);
    
    // Initialize synchronizer
    this.synchronizer = new ConfigSynchronizer(
      this.canvas,
      this.xmlEditor
    );
  }
  
  private createLayout(container: HTMLElement): Layout {
    container.innerHTML = `
      <div class="visual-designer">
        <div class="designer-header">
          <h1>WebHarvest Visual Designer</h1>
          <div class="view-switcher">
            <button class="btn-view active" data-view="visual">Visual</button>
            <button class="btn-view" data-view="split">Split</button>
            <button class="btn-view" data-view="xml">XML</button>
          </div>
          <div class="toolbar">
            <button class="btn-icon" title="Undo"><i class="icon-undo"></i></button>
            <button class="btn-icon" title="Redo"><i class="icon-redo"></i></button>
            <button class="btn-icon" title="Zoom In"><i class="icon-zoom-in"></i></button>
            <button class="btn-icon" title="Zoom Out"><i class="icon-zoom-out"></i></button>
            <button class="btn-icon" title="Fit"><i class="icon-fit"></i></button>
          </div>
        </div>
        
        <div class="designer-body">
          <div class="designer-palette" id="palette-container"></div>
          <div class="designer-canvas" id="canvas-container"></div>
          <div class="designer-properties" id="property-container"></div>
        </div>
        
        <div class="designer-footer">
          <span class="status-text">Ready</span>
          <span class="node-count">0 nodes</span>
          <span class="connection-count">0 connections</span>
        </div>
      </div>
    `;
    
    return {
      paletteContainer: container.querySelector('#palette-container'),
      canvasContainer: container.querySelector('#canvas-container'),
      propertyContainer: container.querySelector('#property-container'),
      xmlContainer: container.querySelector('#xml-container')
    };
  }
  
  private setupEventHandlers() {
    // Canvas selection
    this.canvas.on('node:selected', (node: VisualNode) => {
      this.propertyPanel.showProperties(node);
    });
    
    // Palette drag
    this.palette.on('plugin:drag', (pluginType: string, event: DragEvent) => {
      this.handlePluginDrag(pluginType, event);
    });
    
    // Canvas drop
    this.canvas.on('drop', (event: DragEvent) => {
      this.handlePluginDrop(event);
    });
    
    // Property change
    this.propertyPanel.on('property:changed', (nodeId: string, property: string, value: any) => {
      this.canvas.updateNodeProperty(nodeId, property, value);
      this.synchronizer.syncFromVisual();
    });
    
    // Connection created
    this.canvas.on('connection:created', (connection: VisualConnection) => {
      this.synchronizer.syncFromVisual();
    });
  }
  
  private async loadPluginMetadata() {
    try {
      const response = await fetch('/api/plugins/metadata');
      const data = await response.json();
      
      this.pluginRegistry = new PluginRegistry(data.plugins);
      this.palette.populate(this.pluginRegistry);
      
      console.log(`Loaded ${data.plugins.length} plugins`);
    } catch (error) {
      console.error('Failed to load plugin metadata:', error);
      this.showError('Failed to load plugins. Please refresh.');
    }
  }
  
  // Public API
  
  public loadConfiguration(xml: string) {
    this.synchronizer.loadFromXml(xml);
  }
  
  public getConfiguration(): string {
    return this.synchronizer.getXml();
  }
  
  public getVisualModel(): VisualModel {
    return this.canvas.getModel();
  }
  
  public validate(): ValidationResult {
    return this.canvas.validate();
  }
}
```

---

## 🔌 Plugin Metadata System

### Backend: PluginMetadataServlet

```java
package org.webharvest.server.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.registry.CorePluginRegistry;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/api/plugins/metadata")
public class PluginMetadataServlet extends HttpServlet {
    
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        
        try {
            // Get all registered plugins
            CorePluginRegistry registry = CorePluginRegistry.getInstance();
            Collection<PluginMetadata> plugins = registry.getAllPluginMetadata();
            
            // Convert to visual designer format
            List<Map<String, Object>> visualPlugins = plugins.stream()
                .map(this::convertToVisualFormat)
                .collect(Collectors.toList());
            
            // Sort by category and name
            visualPlugins.sort(Comparator
                .comparing((Map<String, Object> p) -> (String) p.get("category"))
                .thenComparing(p -> (String) p.get("name")));
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("plugins", visualPlugins);
            response.put("count", visualPlugins.size());
            response.put("version", "2.2.0");
            
            // Send JSON response
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(GSON.toJson(response));
            
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write(GSON.toJson(Map.of(
                "error", "Failed to load plugin metadata",
                "message", e.getMessage()
            )));
        }
    }
    
    private Map<String, Object> convertToVisualFormat(PluginMetadata metadata) {
        Map<String, Object> visual = new HashMap<>();
        
        visual.put("id", metadata.getId());
        visual.put("name", metadata.getName());
        visual.put("elementName", metadata.getElementName());
        visual.put("description", metadata.getDescription());
        visual.put("category", metadata.getCategory());
        visual.put("icon", getPluginIcon(metadata.getCategory(), metadata.getElementName()));
        visual.put("version", metadata.getVersion());
        visual.put("author", metadata.getAuthor());
        visual.put("tags", metadata.getTags());
        visual.put("helpUrl", getHelpUrl(metadata.getElementName()));
        visual.put("schema", convertSchema(metadata.getConfigurationSchema()));
        
        return visual;
    }
    
    private String getPluginIcon(String category, String elementName) {
        // Map plugins to emoji icons
        Map<String, String> iconMap = new HashMap<>();
        iconMap.put("http", "🌐");
        iconMap.put("xpath", "🔍");
        iconMap.put("regexp", "📝");
        iconMap.put("xquery", "📊");
        iconMap.put("if", "🔀");
        iconMap.put("loop", "🔁");
        iconMap.put("while", "↻");
        iconMap.put("try", "🛡️");
        iconMap.put("def", "📦");
        iconMap.put("file", "📁");
        iconMap.put("database", "🗄️");
        iconMap.put("mail", "📧");
        // ... add more mappings
        
        return iconMap.getOrDefault(elementName, "⚙️");
    }
    
    private String getHelpUrl(String elementName) {
        return String.format(
            "https://web-harvest.sourceforge.io/documentation/plugin-%s.html",
            elementName
        );
    }
    
    private Map<String, Object> convertSchema(PluginConfigurationSchema schema) {
        if (schema == null) return null;
        
        Map<String, Object> schemaMap = new HashMap<>();
        
        // Convert attributes
        List<Map<String, Object>> attributes = new ArrayList<>();
        for (AttributeDefinition attr : schema.getAttributeDefinitions()) {
            Map<String, Object> attrMap = new HashMap<>();
            attrMap.put("name", attr.getName());
            attrMap.put("type", attr.getType());
            attrMap.put("required", attr.isRequired());
            attrMap.put("defaultValue", attr.getDefaultValue());
            attrMap.put("description", attr.getDescription());
            
            // Type-specific properties
            if ("enum".equals(attr.getType()) && attr.getOptions() != null) {
                attrMap.put("options", attr.getOptions());
            }
            if ("number".equals(attr.getType())) {
                attrMap.put("min", attr.getMin());
                attrMap.put("max", attr.getMax());
            }
            if ("string".equals(attr.getType()) && attr.getPattern() != null) {
                attrMap.put("pattern", attr.getPattern());
            }
            
            attributes.add(attrMap);
        }
        schemaMap.put("attributes", attributes);
        
        // Body configuration
        schemaMap.put("allowsBody", schema.allowsBody());
        schemaMap.put("bodyType", getBodyType(schema));
        
        // Sub-elements
        schemaMap.put("allowedSubElements", schema.getAllowedSubElements());
        schemaMap.put("requiredSubElements", schema.getRequiredSubElements());
        
        return schemaMap;
    }
    
    private String getBodyType(PluginConfigurationSchema schema) {
        // Determine body type from schema
        if (!schema.allowsBody()) return null;
        
        String[] subElements = schema.getAllowedSubElements();
        if (subElements == null || subElements.length == 0) {
            return "text"; // Text-only body
        } else {
            return "xml"; // Complex XML body
        }
    }
}
```

### Frontend: PluginRegistry

```typescript
// PluginRegistry.js

class PluginRegistry {
  private plugins: Map<string, PluginMetadata>;
  private categories: Map<string, PluginMetadata[]>;
  
  constructor(pluginsData: PluginMetadata[]) {
    this.plugins = new Map();
    this.categories = new Map();
    
    this.registerPlugins(pluginsData);
  }
  
  private registerPlugins(pluginsData: PluginMetadata[]) {
    for (const plugin of pluginsData) {
      // Index by element name
      this.plugins.set(plugin.elementName, plugin);
      
      // Group by category
      if (!this.categories.has(plugin.category)) {
        this.categories.set(plugin.category, []);
      }
      this.categories.get(plugin.category)!.push(plugin);
    }
    
    console.log(`Registered ${this.plugins.size} plugins in ${this.categories.size} categories`);
  }
  
  public getPlugin(elementName: string): PluginMetadata | undefined {
    return this.plugins.get(elementName);
  }
  
  public getAllPlugins(): PluginMetadata[] {
    return Array.from(this.plugins.values());
  }
  
  public getCategories(): string[] {
    return Array.from(this.categories.keys());
  }
  
  public getPluginsByCategory(category: string): PluginMetadata[] {
    return this.categories.get(category) || [];
  }
  
  public search(query: string): PluginMetadata[] {
    const lowerQuery = query.toLowerCase();
    
    return this.getAllPlugins().filter(plugin =>
      plugin.name.toLowerCase().includes(lowerQuery) ||
      plugin.description.toLowerCase().includes(lowerQuery) ||
      plugin.tags.some(tag => tag.toLowerCase().includes(lowerQuery)) ||
      plugin.category.toLowerCase().includes(lowerQuery)
    );
  }
}
```

---

## 🎨 Visual Canvas Implementation

### Using Rete.js

```typescript
// Canvas.js (Rete.js implementation)

import Rete from 'rete';
import ConnectionPlugin from 'rete-connection-plugin';
import VueRenderPlugin from 'rete-vue-render-plugin';
import ContextMenuPlugin from 'rete-context-menu-plugin';
import AreaPlugin from 'rete-area-plugin';

class Canvas extends EventEmitter {
  private editor: Rete.NodeEditor;
  private components: Map<string, Rete.Component>;
  
  constructor(container: HTMLElement, options: CanvasOptions) {
    super();
    this.components = new Map();
    this.initializeEditor(container, options);
  }
  
  private async initializeEditor(container: HTMLElement, options: CanvasOptions) {
    // Create Rete editor
    this.editor = new Rete.NodeEditor('webharvest@0.1.0', container);
    
    // Install plugins
    this.editor.use(ConnectionPlugin);
    this.editor.use(VueRenderPlugin);
    this.editor.use(ContextMenuPlugin);
    
    const areaPlugin = new AreaPlugin(this.editor);
    this.editor.use(areaPlugin);
    
    // Setup events
    this.setupEditorEvents();
    
    // Set background
    this.setBackground(options.background, options.grid);
  }
  
  private setupEditorEvents() {
    // Node selection
    this.editor.on('nodeselected', (node: Rete.Node) => {
      this.emit('node:selected', this.nodeToVisual(node));
    });
    
    // Connection created
    this.editor.on('connectioncreated', (connection: Rete.Connection) => {
      this.emit('connection:created', {
        id: `${connection.output.node.id}_${connection.input.node.id}`,
        source: String(connection.output.node.id),
        target: String(connection.input.node.id)
      });
    });
    
    // Node removed
    this.editor.on('noderemoved', (node: Rete.Node) => {
      this.emit('node:removed', String(node.id));
    });
  }
  
  public registerPlugin(metadata: PluginMetadata) {
    const component = new PluginComponent(metadata);
    this.components.set(metadata.elementName, component);
    this.editor.register(component);
  }
  
  public async addNode(type: string, position: { x: number, y: number }, config: Record<string, any> = {}) {
    const component = this.components.get(type);
    if (!component) {
      throw new Error(`Plugin ${type} not registered`);
    }
    
    const node = await component.createNode(config);
    node.position = [position.x, position.y];
    
    this.editor.addNode(node);
    await this.editor.view.updateConnections({ node });
    
    return node;
  }
  
  public updateNodeProperty(nodeId: string, property: string, value: any) {
    const node = this.editor.nodes.find(n => n.id === Number(nodeId));
    if (node) {
      node.data[property] = value;
      this.editor.view.updateConnections({ node });
    }
  }
  
  public getModel(): VisualModel {
    const nodes: VisualNode[] = this.editor.nodes.map(node => this.nodeToVisual(node));
    const connections: VisualConnection[] = [];
    
    // Extract connections
    this.editor.nodes.forEach(node => {
      node.outputs.forEach((output: any) => {
        output.connections.forEach((conn: any) => {
          connections.push({
            id: `${node.id}_${conn.node.id}`,
            source: String(node.id),
            target: String(conn.node.id)
          });
        });
      });
    });
    
    return {
      version: '1.0',
      nodes: nodes,
      connections: connections,
      metadata: {
        created: new Date().toISOString(),
        modified: new Date().toISOString()
      }
    };
  }
  
  public loadModel(model: VisualModel) {
    // Clear existing
    this.editor.clear();
    
    // Add nodes
    for (const visualNode of model.nodes) {
      this.addNode(visualNode.type, visualNode.position, visualNode.config);
    }
    
    // Add connections (after all nodes are created)
    setTimeout(() => {
      for (const conn of model.connections) {
        const sourceNode = this.editor.nodes.find(n => n.id === Number(conn.source));
        const targetNode = this.editor.nodes.find(n => n.id === Number(conn.target));
        
        if (sourceNode && targetNode) {
          const sourceOutput = sourceNode.outputs.get('output');
          const targetInput = targetNode.inputs.get('input');
          
          if (sourceOutput && targetInput) {
            this.editor.connect(sourceOutput, targetInput);
          }
        }
      }
    }, 100);
  }
  
  private nodeToVisual(node: Rete.Node): VisualNode {
    return {
      id: String(node.id),
      type: node.name,
      position: { x: node.position[0], y: node.position[1] },
      config: node.data,
      connections: {
        inputs: Array.from(node.inputs.values()).flatMap(input => 
          input.connections.map(conn => String(conn.output.node.id))
        ),
        outputs: Array.from(node.outputs.values()).flatMap(output =>
          output.connections.map(conn => String(conn.input.node.id))
        )
      }
    };
  }
}

// Plugin Component for Rete
class PluginComponent extends Rete.Component {
  private metadata: PluginMetadata;
  
  constructor(metadata: PluginMetadata) {
    super(metadata.elementName);
    this.metadata = metadata;
  }
  
  async builder(node: Rete.Node) {
    // Create input socket
    const input = new Rete.Input('input', 'Input', socketAny);
    
    // Create output socket
    const output = new Rete.Output('output', 'Output', socketAny);
    
    // Create controls for each attribute
    for (const attr of this.metadata.schema.attributes) {
      const control = this.createControl(attr);
      node.addControl(control);
    }
    
    return node
      .addInput(input)
      .addOutput(output);
  }
  
  private createControl(attr: AttributeDefinition): Rete.Control {
    switch (attr.type) {
      case 'string':
        return new TextControl(this.editor, attr.name, attr.description);
      case 'number':
        return new NumberControl(this.editor, attr.name, attr.min, attr.max);
      case 'boolean':
        return new CheckboxControl(this.editor, attr.name);
      case 'enum':
        return new SelectControl(this.editor, attr.name, attr.options || []);
      default:
        return new TextControl(this.editor, attr.name, attr.description);
    }
  }
  
  worker(node: NodeData, inputs: WorkerInputs, outputs: WorkerOutputs) {
    // Execution logic (if needed for preview)
    outputs['output'] = node.data;
  }
}
```

---

## 📝 Property Panel Generation

```typescript
// PropertyPanel.js

class PropertyPanel extends EventEmitter {
  private container: HTMLElement;
  private currentNode: VisualNode | null = null;
  private currentPlugin: PluginMetadata | null = null;
  
  constructor(container: HTMLElement) {
    super();
    this.container = container;
    this.showEmpty();
  }
  
  public showProperties(node: VisualNode) {
    this.currentNode = node;
    this.currentPlugin = pluginRegistry.getPlugin(node.type);
    
    if (!this.currentPlugin) {
      this.showError(`Unknown plugin: ${node.type}`);
      return;
    }
    
    this.render();
  }
  
  private render() {
    if (!this.currentNode || !this.currentPlugin) return;
    
    const html = `
      <div class="property-panel-content">
        ${this.renderHeader()}
        ${this.renderAttributes()}
        ${this.renderBody()}
        ${this.renderHelp()}
        ${this.renderActions()}
      </div>
    `;
    
    this.container.innerHTML = html;
    this.attachEventListeners();
  }
  
  private renderHeader(): string {
    return `
      <div class="property-header">
        <div class="property-icon">${this.currentPlugin!.icon}</div>
        <div class="property-title">
          <h3>${this.currentPlugin!.name}</h3>
          <p class="property-description">${this.currentPlugin!.description}</p>
        </div>
      </div>
    `;
  }
  
  private renderAttributes(): string {
    const schema = this.currentPlugin!.schema;
    if (!schema || !schema.attributes || schema.attributes.length === 0) {
      return '';
    }
    
    const attributesHtml = schema.attributes.map(attr => 
      this.renderAttribute(attr)
    ).join('');
    
    return `
      <div class="property-section">
        <h4 class="section-title">Configuration</h4>
        <div class="property-fields">
          ${attributesHtml}
        </div>
      </div>
    `;
  }
  
  private renderAttribute(attr: AttributeDefinition): string {
    const value = this.currentNode!.config[attr.name] ?? attr.defaultValue ?? '';
    const required = attr.required ? '<span class="required">*</span>' : '';
    
    return `
      <div class="property-field">
        <label class="field-label">
          ${attr.name} ${required}
          <span class="field-description">${attr.description || ''}</span>
        </label>
        ${this.renderInput(attr, value)}
      </div>
    `;
  }
  
  private renderInput(attr: AttributeDefinition, value: any): string {
    switch (attr.type) {
      case 'string':
        return `
          <input 
            type="text" 
            class="field-input" 
            name="${attr.name}"
            value="${this.escapeHtml(value)}"
            placeholder="${attr.description || ''}"
            ${attr.pattern ? `pattern="${attr.pattern}"` : ''}
            ${attr.required ? 'required' : ''}
          />
        `;
        
      case 'number':
        return `
          <input 
            type="number" 
            class="field-input" 
            name="${attr.name}"
            value="${value}"
            ${attr.min !== undefined ? `min="${attr.min}"` : ''}
            ${attr.max !== undefined ? `max="${attr.max}"` : ''}
            ${attr.required ? 'required' : ''}
          />
        `;
        
      case 'boolean':
        return `
          <label class="field-checkbox">
            <input 
              type="checkbox" 
              name="${attr.name}"
              ${value ? 'checked' : ''}
            />
            <span class="checkbox-label">${attr.description || 'Enabled'}</span>
          </label>
        `;
        
      case 'enum':
        const options = attr.options || [];
        const optionsHtml = options.map(opt => 
          `<option value="${opt}" ${opt === value ? 'selected' : ''}>${opt}</option>`
        ).join('');
        
        return `
          <select class="field-select" name="${attr.name}" ${attr.required ? 'required' : ''}>
            ${!attr.required ? '<option value="">-- Select --</option>' : ''}
            ${optionsHtml}
          </select>
        `;
        
      default:
        return `<input type="text" class="field-input" name="${attr.name}" value="${value}" />`;
    }
  }
  
  private renderBody(): string {
    const schema = this.currentPlugin!.schema;
    if (!schema.allowsBody) return '';
    
    const bodyContent = this.currentNode!.config._body || '';
    
    return `
      <div class="property-section">
        <h4 class="section-title">Body Content</h4>
        <textarea 
          class="field-textarea" 
          name="_body"
          rows="8"
          placeholder="Enter ${schema.bodyType || 'text'} content..."
        >${this.escapeHtml(bodyContent)}</textarea>
      </div>
    `;
  }
  
  private renderHelp(): string {
    return `
      <div class="property-section">
        <a 
          href="${this.currentPlugin!.helpUrl}" 
          target="_blank"
          class="btn-help"
        >
          📄 View Documentation
        </a>
      </div>
    `;
  }
  
  private renderActions(): string {
    return `
      <div class="property-actions">
        <button class="btn-primary" data-action="apply">Apply</button>
        <button class="btn-secondary" data-action="cancel">Cancel</button>
      </div>
    `;
  }
  
  private attachEventListeners() {
    // Input change listeners
    this.container.querySelectorAll('.field-input, .field-select, .field-textarea').forEach(input => {
      input.addEventListener('input', (e) => {
        const target = e.target as HTMLInputElement;
        this.handlePropertyChange(target.name, target.value);
      });
    });
    
    // Checkbox listeners
    this.container.querySelectorAll('input[type="checkbox"]').forEach(checkbox => {
      checkbox.addEventListener('change', (e) => {
        const target = e.target as HTMLInputElement;
        this.handlePropertyChange(target.name, target.checked);
      });
    });
    
    // Action buttons
    this.container.querySelectorAll('[data-action]').forEach(button => {
      button.addEventListener('click', (e) => {
        const action = (e.target as HTMLElement).dataset.action;
        if (action === 'apply') {
          this.applyChanges();
        } else if (action === 'cancel') {
          this.cancelChanges();
        }
      });
    });
  }
  
  private handlePropertyChange(name: string, value: any) {
    if (!this.currentNode) return;
    
    // Update node config
    this.currentNode.config[name] = value;
    
    // Emit event for canvas update
    this.emit('property:changed', this.currentNode.id, name, value);
  }
  
  private applyChanges() {
    // Validate
    const validation = this.validate();
    if (!validation.valid) {
      this.showValidationErrors(validation.errors);
      return;
    }
    
    // Apply and close
    this.emit('properties:applied', this.currentNode);
  }
  
  private cancelChanges() {
    // Reload from original node
    if (this.currentNode) {
      this.showProperties(this.currentNode);
    }
  }
  
  private validate(): ValidationResult {
    const errors: ValidationError[] = [];
    const schema = this.currentPlugin!.schema;
    
    // Validate required attributes
    for (const attr of schema.attributes) {
      if (attr.required) {
        const value = this.currentNode!.config[attr.name];
        if (value === undefined || value === null || value === '') {
          errors.push({
            field: attr.name,
            message: `${attr.name} is required`
          });
        }
      }
    }
    
    return {
      valid: errors.length === 0,
      errors: errors
    };
  }
  
  private showValidationErrors(errors: ValidationError[]) {
    // Highlight error fields
    errors.forEach(error => {
      const field = this.container.querySelector(`[name="${error.field}"]`);
      if (field) {
        field.classList.add('field-error');
        
        // Show error message
        const errorMsg = document.createElement('div');
        errorMsg.className = 'field-error-message';
        errorMsg.textContent = error.message;
        field.parentElement?.appendChild(errorMsg);
      }
    });
  }
  
  private showEmpty() {
    this.container.innerHTML = `
      <div class="property-empty">
        <p>Select a node to view its properties</p>
      </div>
    `;
  }
  
  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }
}
```

---

## 🔄 XML-Visual Synchronization

```typescript
// ConfigSynchronizer.js

class ConfigSynchronizer {
  private canvas: Canvas;
  private xmlEditor: Monaco.Editor;
  private xmlToVisual: XmlToVisualConverter;
  private visualToXml: VisualToXmlConverter;
  
  constructor(canvas: Canvas, xmlEditor: Monaco.Editor) {
    this.canvas = canvas;
    this.xmlEditor = xmlEditor;
    this.xmlToVisual = new XmlToVisualConverter();
    this.visualToXml = new VisualToXmlConverter();
  }
  
  public async loadFromXml(xml: string) {
    try {
      const model = await this.xmlToVisual.convert(xml);
      this.canvas.loadModel(model);
      this.showSuccess('Configuration loaded from XML');
    } catch (error) {
      this.showError(`Failed to parse XML: ${error.message}`);
      throw error;
    }
  }
  
  public async syncFromXml() {
    const xml = this.xmlEditor.getValue();
    await this.loadFromXml(xml);
  }
  
  public async syncFromVisual() {
    try {
      const model = this.canvas.getModel();
      const xml = await this.visualToXml.convert(model);
      this.xmlEditor.setValue(xml);
      this.showSuccess('Configuration synced to XML');
    } catch (error) {
      this.showError(`Failed to generate XML: ${error.message}`);
      throw error;
    }
  }
  
  public getXml(): string {
    return this.xmlEditor.getValue();
  }
  
  private showSuccess(message: string) {
    // Show notification
    console.log('✓', message);
  }
  
  private showError(message: string) {
    // Show error notification
    console.error('✗', message);
  }
}
```

---

## 🧪 Testing Strategy

### Unit Tests

```typescript
// __tests__/XmlToVisualConverter.test.ts

describe('XmlToVisualConverter', () => {
  let converter: XmlToVisualConverter;
  
  beforeEach(() => {
    converter = new XmlToVisualConverter();
  });
  
  test('converts simple HTTP request', async () => {
    const xml = `
      <?xml version="1.0" encoding="UTF-8"?>
      <config xmlns="http://org.webharvest/schema/2.1/core">
        <http url="https://example.com" method="GET"/>
      </config>
    `;
    
    const model = await converter.convert(xml);
    
    expect(model.nodes).toHaveLength(1);
    expect(model.nodes[0].type).toBe('http');
    expect(model.nodes[0].config.url).toBe('https://example.com');
    expect(model.nodes[0].config.method).toBe('GET');
  });
  
  test('converts nested pipeline', async () => {
    const xml = `
      <?xml version="1.0" encoding="UTF-8"?>
      <config>
        <def var="html">
          <http url="https://example.com"/>
        </def>
        <def var="title">
          <xpath expression="//title">
            <get var="html"/>
          </xpath>
        </def>
      </config>
    `;
    
    const model = await converter.convert(xml);
    
    expect(model.nodes).toHaveLength(4); // def, http, def, xpath
    expect(model.connections).toHaveLength(3); // http→def, get→xpath, xpath→def
  });
});
```

### Integration Tests

```typescript
// __tests__/VisualDesigner.integration.test.ts

describe('Visual Designer Integration', () => {
  let designer: VisualDesigner;
  let container: HTMLElement;
  
  beforeEach(() => {
    container = document.createElement('div');
    document.body.appendChild(container);
    designer = new VisualDesigner(container);
  });
  
  afterEach(() => {
    document.body.removeChild(container);
  });
  
  test('loads plugins from API', async () => {
    // Mock API response
    global.fetch = jest.fn().mockResolvedValue({
      json: async () => ({
        plugins: [
          { id: 'http', name: 'HTTP Request', ... }
        ]
      })
    });
    
    await designer.loadPluginMetadata();
    
    const plugins = designer.pluginRegistry.getAllPlugins();
    expect(plugins.length).toBeGreaterThan(0);
  });
  
  test('creates visual model and syncs to XML', async () => {
    // Add nodes
    await designer.canvas.addNode('http', { x: 100, y: 100 }, {
      url: 'https://example.com',
      method: 'GET'
    });
    
    // Sync to XML
    await designer.synchronizer.syncFromVisual();
    
    const xml = designer.getConfiguration();
    expect(xml).toContain('<http');
    expect(xml).toContain('url="https://example.com"');
  });
});
```

---

## 📚 Summary

This technical guide provides:

✅ **Complete implementation reference** for all core components  
✅ **Code examples** in TypeScript and Java  
✅ **Plugin metadata system** with auto-generation  
✅ **Rete.js canvas integration** for node-based editing  
✅ **Dynamic property panels** from schemas  
✅ **XML ↔ Visual synchronization** logic  
✅ **Testing strategies** for quality assurance

**Next Steps:**
1. Review code architecture
2. Setup development environment
3. Implement Phase 1 (Foundation)
4. Iterate based on user feedback

---

**Document Version:** 1.0  
**Last Updated:** October 24, 2025


