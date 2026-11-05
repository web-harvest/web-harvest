# Semantic Model Alignment - API Reference

**Version**: 2.3.0  
**Last Updated**: 2025-12-09

## Overview

This document contains detailed API documentation for the semantic model correlation system between backend and frontend. All endpoints are available under the base URL `/api/plugins`.

---

## Backend API Endpoints

### 1. GET /api/plugins/metadata

Returns full metadata for all registered plugins.

#### Request

```
GET /api/plugins/metadata
```

**Query Parameters**:
- `format` (optional): `"llm"` - returns simplified format for LLM

**Headers**:
- `Accept: application/json`

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/json; charset=UTF-8`

**Response Body**:
```json
{
  "plugins": [
    {
      "id": "http",
      "name": "HTTP Request",
      "description": "Performs HTTP request",
      "category": "Network",
      "version": "1.0.0",
      "schemaVersion": "2.3.0",
      "semantics": {
        "roles": ["LEAF", "NETWORK"],
        "executionSemantics": {
          "isContainer": false,
          "isLeaf": true,
          "executionOrder": "sequential",
          "canPause": false,
          "canResume": false
        }
      },
      "extensions": {
        "schema": {
          "attributeDefinitions": [
            {
              "name": "url",
              "type": "string",
              "required": true,
              "description": "URL to request"
            }
          ],
          "allowsBody": false,
          "canAppearAtRoot": true,
          "requiredParents": [],
          "allowedSubElements": []
        },
        "executionSemantics": {
          "isContainer": false,
          "isLeaf": true,
          "executionOrder": "sequential"
        }
      },
      "visual": {
        "icon": "fas fa-globe",
        "color": "#3B82F6",
        "group": "HTTP & Network",
        "inputPorts": [
          {
            "id": "input",
            "label": "Input",
            "type": "MIXED"
          }
        ],
        "outputPorts": [
          {
            "id": "output",
            "label": "Output",
            "type": "XML"
          }
        ]
      },
      "schema": {
        "attributes": [...],
        "attributeDefinitions": [...],
        "canHaveBody": false,
        "canHaveChildren": false
      }
    }
  ],
  "count": 150,
  "version": "2.2.0",
  "schemaVersion": "2.3.0",
  "schema": {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "type": "object",
    "properties": {
      "plugins": {
        "type": "array",
        "items": { "$ref": "#/definitions/Plugin" }
      }
    }
  },
  "timestamp": 1702147200000,
  "xsdUrl": "/api/plugins/xsd"
}
```

#### Response Fields

- **plugins** (array): List of all plugins with full metadata
- **count** (number): Number of plugins
- **version** (string): Application version
- **schemaVersion** (string): Metadata schema version (used for synchronization)
- **schema** (object): JSON Schema for response validation
- **timestamp** (number): Unix timestamp in milliseconds
- **xsdUrl** (string): URL to XSD endpoint

#### Error Responses

**500 Internal Server Error**:
```json
{
  "error": "Failed to load plugin metadata",
  "message": "Detailed error message",
  "timestamp": 1702147200000
}
```

---

### 2. GET /api/plugins/metadata/version

Returns the metadata schema version for synchronization verification.

#### Request

```
GET /api/plugins/metadata/version
```

**Headers**:
- `Accept: application/json`

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/json; charset=UTF-8`

**Response Body**:
```json
{
  "schemaVersion": "2.3.0",
  "version": "2.2.0",
  "timestamp": 1702147200000
}
```

#### Response Fields

- **schemaVersion** (string): Metadata schema version
- **version** (string): Application version
- **timestamp** (number): Unix timestamp in milliseconds

#### Error Responses

**500 Internal Server Error**:
```json
{
  "error": "Failed to get schema version",
  "message": "Detailed error message",
  "timestamp": 1702147200000
}
```

#### Użycie w Frontend

```typescript
const response = await fetch('/api/plugins/metadata/version');
const data = await response.json();
console.log(`Current schema version: ${data.schemaVersion}`);
```

---

### 3. GET /api/plugins/ontology

Returns ontology schema for all plugins, enabling dynamic building of entity schemas and relationships.

#### Request

```
GET /api/plugins/ontology
```

**Headers**:
- `Accept: application/json`

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/json; charset=UTF-8`

**Response Body**:
```json
{
  "entitySchemas": {
    "http": {
      "type": "Plugin",
      "pluginType": "http",
      "properties": {
        "url": {
          "name": "url",
          "type": "string",
          "required": true,
          "description": "URL to request"
        },
        "method": {
          "name": "method",
          "type": "string",
          "required": false,
          "defaultValue": "GET",
          "allowedValues": ["GET", "POST", "PUT", "DELETE"]
        }
      },
      "allowedRelationships": {
        "parent": {
          "types": ["workflow", "container"],
          "required": false
        },
        "children": {
          "types": ["http-header", "http-param"],
          "required": false
        },
        "siblings": {
          "types": ["http", "file-download"],
          "constraints": []
        }
      }
    }
  },
  "version": "2.3.0",
  "timestamp": 1702147200000
}
```

#### Response Fields

- **entitySchemas** (object): Mapping `pluginId` → entity schema
  - **type** (string): Entity type (always "Plugin")
  - **pluginType** (string): Plugin ID
  - **properties** (object): Plugin properties (from schema extensions)
  - **allowedRelationships** (object): Allowed relationships
    - **parent** (object): Parent relationships
    - **children** (object): Children relationships
    - **siblings** (object): Sibling relationships
- **version** (string): Schema version
- **timestamp** (number): Unix timestamp in milliseconds

#### Użycie w Frontend

```typescript
const response = await fetch('/api/plugins/ontology');
const ontology = await response.json();

// Use as source of truth for ontology
const httpSchema = ontology.entitySchemas['http'];
console.log(`HTTP plugin properties:`, httpSchema.properties);
console.log(`Allowed children:`, httpSchema.allowedRelationships.children.types);
```

---

### 4. GET /api/plugins/xsd

Returns generated XSD schema for default namespace.

#### Request

```
GET /api/plugins/xsd
```

**Headers**:
- `Accept: application/xml` lub `text/xml`

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/xml; charset=UTF-8`

**Response Body**: XML Schema Definition (XSD)

---

### 5. GET /api/plugins/xsd/{version}

Returns generated XSD schema for specified version/namespace.

#### Request

```
GET /api/plugins/xsd/2.3.0
```

**Path Parameters**:
- `version` (string): Version/namespace

**Headers**:
- `Accept: application/xml` lub `text/xml`

#### Response

**Status Code**: `200 OK`

**Content-Type**: `application/xml; charset=UTF-8`

**Response Body**: XML Schema Definition (XSD)

---

## Frontend API

### PluginManager

Class managing plugin metadata in frontend.

#### Metody

##### `loadPlugins(): Promise<Plugin[]>`

Loads plugin metadata from backend.

```typescript
const pluginManager = new PluginManager();
const plugins = await pluginManager.loadPlugins();
console.log(`Loaded ${plugins.length} plugins`);
```

**Returns**: `Promise<Plugin[]>` - List of normalized plugins

**Throws**: `Error` if loading fails

---

##### `getPlugins(): Plugin[]`

Returns loaded plugins.

```typescript
const plugins = pluginManager.getPlugins();
```

**Returns**: `Plugin[]` - Copy of plugin list

---

##### `getSchemaVersion(): string | null`

Returns current schema version.

```typescript
const version = pluginManager.getSchemaVersion();
console.log(`Schema version: ${version}`);
```

**Returns**: `string | null` - Schema version or `null` if not loaded

---

##### `checkSchemaVersion(): Promise<{ changed: boolean; newVersion: string | null }>`

Checks if schema version has changed (throttled to 60 seconds).

```typescript
const { changed, newVersion } = await pluginManager.checkSchemaVersion();
if (changed) {
    console.log(`Schema version changed to ${newVersion}`);
    await pluginManager.refresh();
}
```

**Returns**: `Promise<{ changed: boolean; newVersion: string | null }>`
- **changed**: `true` if version has changed
- **newVersion**: New schema version or `null`

**Throttling**: Maximum one check per 60 seconds

---

##### `refresh(): Promise<void>`

Refreshes plugin metadata if schema version has changed.

```typescript
await pluginManager.refresh();
```

**Returns**: `Promise<void>`

**Events**: Calls all registered listeners via `onRefresh()`

---

##### `onRefresh(listener: () => void): () => void`

Registers listener called on metadata refresh.

```typescript
const unsubscribe = pluginManager.onRefresh(() => {
    console.log('Plugins refreshed!');
    // Update UI, rebuild model, etc.
});

// Later, to cancel subscription:
unsubscribe();
```

**Parameters**:
- **listener** (`() => void`): Function called on refresh

**Returns**: `() => void` - Function to cancel subscription

---

##### `findPlugin(id: string): Plugin | null`

Finds plugin by ID.

```typescript
const httpPlugin = pluginManager.findPlugin('http');
if (httpPlugin) {
    console.log(`Found plugin: ${httpPlugin.name}`);
}
```

**Parameters**:
- **id** (`string`): Plugin ID

**Returns**: `Plugin | null` - Plugin or `null` if not found

---

##### `searchPlugins(query: string): Plugin[]`

Searches plugins by name, description, ID, or tags.

```typescript
const results = pluginManager.searchPlugins('http');
console.log(`Found ${results.length} plugins matching "http"`);
```

**Parameters**:
- **query** (`string`): Search query

**Returns**: `Plugin[]` - List of matching plugins

---

### VisualSemanticModelManager

Class managing the visual layer semantic model.

#### Metody

##### `buildModel(): VisualSemanticModel`

Builds semantic model in real-time based on VisualDesigner state.

```typescript
const modelManager = new VisualSemanticModelManager(designer, pluginManager);
const model = modelManager.buildModel();
console.log(`Model contains ${model.workflow.allNodes.size} nodes`);
```

**Returns**: `VisualSemanticModel` - Full semantic model

---

##### `getModel(): VisualSemanticModel | null`

Returns current semantic model.

```typescript
const model = modelManager.getModel();
if (model) {
    console.log(`Workflow ID: ${model.workflowId}`);
    console.log(`Schema version: ${model.schemaVersion}`);
}
```

**Returns**: `VisualSemanticModel | null` - Model or `null` if not built

---

##### `toLLMFormat(): VisualSemanticModelForLLM`

Converts model to LLM-friendly format (JSON-serializable).

```typescript
const llmModel = modelManager.toLLMFormat();
const json = JSON.stringify(llmModel);
// Wyślij do LLM agenta
await fetch('/api/llm/analyze', {
    method: 'POST',
    body: json,
    headers: { 'Content-Type': 'application/json' }
});
```

**Returns**: `VisualSemanticModelForLLM` - Model in LLM format (all Map/Set converted to Object/Array)

---

##### `subscribe(listener: (model: VisualSemanticModel) => void): () => void`

Subscribes to model updates.

```typescript
const unsubscribe = modelManager.subscribe((model) => {
    console.log('Model updated!');
    // Update UI, refresh visualization, etc.
});

// Later, to cancel subscription:
unsubscribe();
```

**Parameters**:
- **listener** (`(model: VisualSemanticModel) => void`): Function called on update

**Returns**: `() => void` - Function to cancel subscription

---

## Data Structures

### Plugin

```typescript
interface Plugin {
    id: string;
    name: string;
    description: string;
    category: string;
    version: string;
    schemaVersion?: string;
    semantics?: {
        roles: string[];
        executionSemantics?: ExecutionSemantics;
    };
    extensions?: {
        schema?: {
            attributeDefinitions: AttributeDefinition[];
            allowsBody?: boolean;
            canAppearAtRoot?: boolean;
            requiredParents?: string[];
            allowedSubElements?: string[];
        };
        executionSemantics?: ExecutionSemantics;
        masterDetailConstraints?: MasterDetailConstraint[];
    };
    visual?: {
        icon: string;
        color: string;
        group: string;
        inputPorts: Port[];
        outputPorts: Port[];
    };
    schema: PluginSchema;
}
```

### ExecutionSemantics

```typescript
interface ExecutionSemantics {
    isContainer: boolean;
    isLeaf: boolean;
    executionOrder: 'sequential' | 'parallel' | 'conditional';
    canPause: boolean;
    canResume: boolean;
}
```

### VariableSemantics

```typescript
interface VariableSemantics {
    definesVariables: string[];
    usesVariables: string[];
    variableScope: 'root' | 'parent';
}
```

### PluginSemanticInfo

```typescript
interface PluginSemanticInfo {
    id: string;
    name: string;
    description: string;
    semantics: {
        roles: string[];
        executionSemantics?: ExecutionSemantics;
        variableSemantics?: VariableSemantics;
    };
    visual: {
        icon?: string;
        color?: string;
        group?: string;
        inputPorts: ConnectionPoint[];
        outputPorts: ConnectionPoint[];
        portCompatibility: Record<string, string[]>;
        masterDetailConstraints?: MasterDetailConstraint[];
    };
    schema: PluginSchema;
}
```

### VisualSemanticModel

```typescript
interface VisualSemanticModel {
    version: string;
    timestamp: number;
    workflowId: string;
    schemaVersion: string | null;
    availablePlugins: Map<string, PluginSemanticInfo>;
    workflow: {
        rootNodes: VisualNodeState[];
        allNodes: Map<string, VisualNodeState>;
        connections: Map<string, VisualConnectionState>;
        variableDependencies: Map<string, VariableDependency>;
    };
    layout: {
        viewport: ViewportState;
        nodePositions: Map<string, NodePositionState>;
        connectionPaths: Map<string, ConnectionPathState>;
    };
    ui: UIState;
    availableActions: Map<string, ActionDescription>;
}
```

### VisualSemanticModelForLLM

```typescript
interface VisualSemanticModelForLLM {
    version: string;
    timestamp: number;
    workflowId: string;
    schemaVersion: string | null;
    availablePlugins: Record<string, PluginSemanticInfo>;
    workflow: {
        rootNodes: VisualNodeState[];
        allNodes: Record<string, VisualNodeState>;
        connections: VisualConnectionState[];
        variableDependencies: VariableDependency[];
    };
    layout: {
        viewport: ViewportState;
        nodePositions: Record<string, NodePositionState>;
        connectionPaths: Record<string, ConnectionPathState>;
    };
    ui: UIState;
    availableActions: Record<string, ActionDescription>;
}
```

---

## Usage Examples

### Example 1: Checking and Refreshing Metadata

```typescript
import { PluginManager } from './core/PluginManager';

const pluginManager = new PluginManager();

// Load plugins
await pluginManager.loadPlugins();

// Register listener for refresh
pluginManager.onRefresh(() => {
    console.log('Plugins refreshed! Rebuilding UI...');
    // Update UI, rebuild palette, etc.
});

// Check schema version (automatically throttled)
setInterval(async () => {
    const { changed, newVersion } = await pluginManager.checkSchemaVersion();
    if (changed) {
        console.log(`Schema version changed to ${newVersion}`);
        await pluginManager.refresh(); // Automatically calls listeners
    }
}, 60000); // Check every 60 seconds
```

### Example 2: Using Semantic Model for LLM

```typescript
import { VisualSemanticModelManager } from './visual/VisualSemanticModelManager';
import { PluginManager } from './core/PluginManager';
import { VisualDesigner } from './visual/VisualDesigner';

const pluginManager = new PluginManager();
await pluginManager.loadPlugins();

const designer = new VisualDesigner(/* ... */);
const modelManager = new VisualSemanticModelManager(designer, pluginManager);

// Build model
const model = modelManager.buildModel();

// Convert to LLM format
const llmModel = modelManager.toLLMFormat();

// Send to LLM agent
const response = await fetch('/api/llm/analyze', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(llmModel)
});

const analysis = await response.json();
console.log('LLM Analysis:', analysis);
```

### Example 3: Loading Ontology from Backend

```typescript
async function loadOntology() {
    try {
        const response = await fetch('/api/plugins/ontology');
        const ontology = await response.json();
        
        // Use as source of truth
        const httpSchema = ontology.entitySchemas['http'];
        
        console.log('HTTP Plugin Properties:', httpSchema.properties);
        console.log('Allowed Children:', httpSchema.allowedRelationships.children.types);
        console.log('Allowed Parents:', httpSchema.allowedRelationships.parent.types);
        
        return ontology;
    } catch (error) {
        console.error('Failed to load ontology:', error);
        // Fallback to local OntologyBuilder
        return null;
    }
}
```

### Example 4: Mapping Schema Extensions

```typescript
import { mapSchemaFromExtensions, getAttributeDefinitions } from './shared/metadata/SchemaMapper';

// Get plugin
const plugin = pluginManager.findPlugin('http');
if (!plugin) return;

// Always prefers extensions.schema.attributeDefinitions
const schema = mapSchemaFromExtensions(plugin.extensions, plugin.schema);

// Get attribute definitions
const attributes = getAttributeDefinitions(plugin);

console.log('Schema:', schema);
console.log('Attributes:', attributes);
```

### Example 5: Subscribing to Model Updates

```typescript
const modelManager = new VisualSemanticModelManager(designer, pluginManager);

// Subscribe to updates
const unsubscribe = modelManager.subscribe((model) => {
    console.log('Model updated!');
    console.log(`Nodes: ${model.workflow.allNodes.size}`);
    console.log(`Connections: ${model.workflow.connections.size}`);
    console.log(`Schema version: ${model.schemaVersion}`);
    
    // Update UI
    updateVisualization(model);
    
    // Send to LLM if needed
    if (shouldSendToLLM()) {
        const llmModel = modelManager.toLLMFormat();
        sendToLLM(llmModel);
    }
});

// Later, to cancel subscription:
// unsubscribe();
```

---

## Error Handling

### Backend Errors

All endpoints return errors in the following format:

```json
{
  "error": "Error type",
  "message": "Detailed error message",
  "timestamp": 1702147200000
}
```

**Status Codes**:
- `200 OK`: Success
- `400 Bad Request`: Invalid request
- `404 Not Found`: Endpoint does not exist
- `500 Internal Server Error`: Server error

### Frontend Errors

All asynchronous methods may throw `Error`:

```typescript
try {
    await pluginManager.loadPlugins();
} catch (error) {
    console.error('Failed to load plugins:', error);
    // Fallback logic
}
```

---

## Best Practices

1. **Always check schema version**: Use `checkSchemaVersion()` before critical operations
2. **Use throttling**: Don't call `checkSchemaVersion()` too often (automatically throttled to 60s)
3. **Subscribe to updates**: Use `subscribe()` and `onRefresh()` instead of polling
4. **Handle errors**: Always use try-catch for asynchronous operations
5. **Use LLM format only when needed**: `toLLMFormat()` is expensive, use only when sending to LLM
6. **Cache metadata**: Plugin metadata is cached, don't load it multiple times
7. **Fallback to local**: If backend is unavailable, use local `OntologyBuilder`

---

## Changelog

### Version 2.3.0 (2025-12-09)

- ✅ Added endpoint `/api/plugins/metadata/version`
- ✅ Added endpoint `/api/plugins/ontology`
- ✅ Added `schemaVersion` to all responses
- ✅ Added `executionSemantics` to plugin metadata
- ✅ Added `portCompatibility` matrix
- ✅ Added `toLLMFormat()` to `VisualSemanticModelManager`
- ✅ Added `checkSchemaVersion()` and `refresh()` to `PluginManager`
- ✅ Unified schema extensions via `SchemaMapper`

---

## Support

For issues or questions:
1. Check implementation documentation: `SEMANTIC_MODEL_ALIGNMENT_IMPLEMENTATION.md`
2. Check troubleshooting guide in implementation documentation
3. Check logs in browser console (frontend) and server (backend)

