# WebHarvest Developer Guide

**Version:** 2.3.1  
**Last Updated:** November 3, 2025

## 🎉 **Recent Achievements**

### ✅ **Visual Designer + Semantic Metadata (v2.3.1)**

**Visual Designer:** Complete drag-and-drop editor for WebHarvest configurations
- TypeScript/Vite frontend with professional UI
- Plugin palette with 55 plugins from semantic metadata
- Nested containers with reordering support
- Semantic validation for plugin configurations
- Bidirectional XML ↔ Visual synchronization

**Semantic Metadata System:**
- Unified plugin catalog (55 plugins)
- Schema v1.1.0 with dependencies and roles
- Context-aware validation
- Complete TDD coverage

### ✅ **IDE Modernization (v2.2)**

**Web-Based IDE** replacing legacy Swing application:
- Embedded Jetty 11 - standalone executable JAR
- Monaco Editor - professional code editing
- WebSocket streaming - real-time logs and results
- Variable extraction - all variables accessible in UI
- Zero regression - all tests pass

#### 🔧 Critical Core Fix:

**ConfigPlugin Scope Enhancement:**
Variables created by `<def>` now execute in ROOT scope instead of nested scopes, ensuring they persist after configuration execution.

**Result:** 
- ✅ **Variables accessible to IDE and tools**
- ✅ **No cleanup of user variables**
- ✅ **Full backward compatibility**

---

## 🎯 **Plugin Architecture - 2025**

### Two Coexisting Architectures:

```
Legacy: @Definition → WebHarvestPluginDef → WebHarvestPlugin (Processor)
Modern: @CorePlugin → CorePluginDef → PluginProcessorAdapter → AbstractProcessor<IElementDef>
```

**Status:**
- ✅ **42 core plugins** detected and registered
- ✅ **All plugins** working 100%
- ✅ **Both architectures** coexist without conflicts

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [IDE Development](#ide-development)
3. [Visual Designer Architecture](#visual-designer-architecture)
4. [Semantic Metadata System](#semantic-metadata-system)
5. [Plugin Development](#plugin-development)
6. [Core APIs](#core-apis)
7. [Configuration System](#configuration-system)
8. [Runtime System](#runtime-system)
9. [Testing Guidelines](#testing-guidelines)
10. [Code Quality Standards](#code-quality-standards)

## IDE Development

### Overview

The WebHarvest IDE is built as a web application with embedded Jetty server. See [USER_GUIDE.md](USER_GUIDE.md) for user documentation.

### Architecture

```
Frontend (Browser)           Backend (Java)               Core Engine
─────────────────           ──────────────               ───────────
Monaco Editor    ←HTTP→     ConfigServlet    ───┐
  ↓                           ↓                  │
Tabs & UI        ←HTTP→     ExecutionServlet   ├→ Harvest API
  ↓                           ↓                  │
Results Display  ←WS→       ExecutionManager  ─┘
                              ↓
                            Thread Pool
                              ↓
                            Harvester instances
```

### Key Components

#### Backend (`src/main/java/org/webharvest/server/`)

**WebHarvestServer.java**
- Main class with `public static void main()`
- Initializes Jetty server
- Configures ServletContextHandler
- Serves static files from `/webapp` in JAR resources
- Finds available port (8080-8179)

**API Layer**
- `ConfigServlet` - Validates configurations using `BufferConfigSource`
- `ExecutionServlet` - Manages execution lifecycle
- Returns execution ID immediately, execution runs asynchronously

**WebSocket Layer**
- `ExecutionWebSocketServlet` - Jetty WebSocket servlet
- `ExecutionWebSocketHandler` - Handles connections, messages, errors
- Protocol: JSON messages for logs and status updates

**Execution Management**
- `ExecutionManager` - Singleton orchestrator
- Thread pool: `Executors.newFixedThreadPool(5)`
- Maps execution IDs to WebSocket sessions
- **Variable Extraction:** Iterates over `DynamicScopeContext` after execution

#### Frontend (`src/main/resources/webapp/`)

**index.html**
- Single-page application structure
- Three-panel layout: Editor | Results (Output/Variables/Log)
- Uses WebHarvest branding (logo, colors)

**static/js/app.js**
- Application state management
- Tab CRUD operations
- Monaco Editor integration
- WebSocket connection management
- Results rendering

**static/css/**
- `style.css` - Copied from `docs/web/css/style.css`
- `ide.css` - IDE-specific styles
- `prism.css` - Code highlighting

### Development Workflow

#### Adding New Backend Feature

1. **Create Servlet or extend ExecutionManager**
   ```java
   @WebServlet("/api/my-feature")
   public class MyFeatureServlet extends HttpServlet {
       @Override
       protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
           // Implementation
       }
   }
   ```

2. **Register in WebHarvestServer**
   ```java
   context.addServlet(new ServletHolder(new MyFeatureServlet()), "/api/my-feature/*");
   ```

3. **Add Integration Test**
   ```java
   @Test
   public void testMyFeature() throws Exception {
       // Test with real WebSocket
   }
   ```

#### Adding New Frontend Feature (TypeScript/Vite - v2.3+)

**CRITICAL**: The IDE now uses TypeScript/Vite, NOT plain JavaScript!

1. **Edit TypeScript files in `webharvest-ide/frontend/src/`**
   ```typescript
   // Example: webharvest-ide/frontend/src/core/App.ts
   public myNewFeature(): void {
       // Implementation
   }
   ```

2. **Update HTML** (if UI changes needed)
   ```html
   <!-- webharvest-ide/src/main/resources/webapp/index.html -->
   <button onclick="app.myNewFeature()">My Feature</button>
   ```

3. **Update CSS** (if styling needed)
   ```css
   /* webharvest-ide/src/main/resources/webapp/static/css/ide-redesign.css */
   .my-feature {
       /* Styles */
   }
   ```

4. **REBUILD FRONTEND AND JAR**
   ```bash
   # Step 1: Build frontend (TypeScript → JavaScript)
   cd webharvest-ide/frontend
   npm run build
   # Outputs to: webharvest-ide/src/main/resources/webapp/dist/
   
   # Step 2: Build JAR (includes frontend)
   cd ..
   mvn install -DskipTests
   # Creates: webharvest-ide/target/webharvest-ide-2.3.1.jar
   
   # Step 3: Run IDE
   java -jar target/webharvest-ide-2.3.1.jar
   ```

**IMPORTANT**: After ANY frontend changes:
- Must run `npm run build` in `frontend/` directory
- Must rebuild JAR to include new frontend assets
- Hard refresh browser (Cmd+Shift+R / Ctrl+Shift+R) to clear cache

### ConfigPlugin Scope Fix (CRITICAL)

**Background:**  
Originally, `ConfigPlugin` used `super.executeBody()` which delegates to `BodyProcessor`. `BodyProcessor` executes children in nested scopes via `context.executeWithinNewContext()`. When execution completed, the `finally` block cleaned up all variables from nested scopes.

**Problem:**
```java
// ScraperContext.executeWithinNewContext() - lines 164-181
public <R> R executeWithinNewContext(Callable<R> callable) {
    try {
        variablesNamesStack.push(new HashSet<String>());
        return callable.call();
    } finally {
        for (String varName : variablesNamesStack.pop()) {
            removeVarFromCRT(varName); // CLEANUP - variables deleted!
        }
    }
}
```

**Solution in ConfigPlugin.executeBody():**
```java
@Override
protected Variable executeBody(DynamicScopeContext context) {
    // Execute children DIRECTLY in current scope (no nesting!)
    final IElementDef[] defs = getElementDef().getOperationDefs();
    
    for (IElementDef def : defs) {
        final Processor processor = ProcessorFactory.INSTANCE.createProcessor(context, def);
        processor.run(context); // Variables stay in ROOT context!
    }
    // Return combined results
}
```

**Testing:**
- Unit test: `ScraperContextTest.testExecuteWithinNewContext()` demonstrates the scope behavior
- Integration test: `ExecutionManagerWebSocketTest.testMultiVariablePipeline_CRITICAL()` verifies variables are extracted

### Testing IDE

#### Unit Tests
Not applicable - IDE components are integration-focused.

#### Integration Tests
Located in `src/test/java/org/webharvest/server/manager/ExecutionManagerWebSocketTest.java`

**Setup:**
- Starts real Jetty server on port 8888
- Creates WebSocket client connections
- Submits real configurations
- Verifies WebSocket messages

**Example Test:**
```java
@Test
public void testSimpleDefVariables() throws Exception {
    String configXml = "...";
    ExecutionCapture capture = executeAndCapture("test-def", configXml);
    
    assertTrue(capture.completed);
    capture.assertHasVariable("greeting");
    capture.assertVariableContains("greeting", "Hello World");
}
```

**Running Tests:**
```bash
cd webharvest-ide
mvn test

# Output:
# ✓ testSimpleDefVariables
# ✓ testXPathPluginVariables
# ✓ testMultiVariablePipeline_CRITICAL  
# ✓ testLogStreaming
# Tests run: 4, Failures: 0, Errors: 0
```

### Building IDE (v2.3+ TypeScript/Vite)

#### Complete Build Process (Frontend + Backend)

**When changing ANY frontend TypeScript files:**

```bash
# Step 1: Build frontend (TypeScript → JavaScript)
cd webharvest-ide/frontend
npm run build
# ~2-5 seconds, outputs to src/main/resources/webapp/dist/

# Step 2: Build JAR (includes frontend assets)
cd ..
mvn install -DskipTests
# ~30-60 seconds, creates target/webharvest-ide-2.3.1.jar

# Step 3: Run IDE
java -jar target/webharvest-ide-2.3.1.jar
```

**When only changing Java backend:**

```bash
# Skip frontend build, just compile Java
cd webharvest-ide
mvn compile jar:jar shade:shade -DskipTests
java -jar target/webharvest-ide-2.3.1.jar
```

#### Quick Development Workflow

**For rapid iteration on frontend changes:**

```bash
# Terminal 1: Start frontend watcher (auto-rebuild on save)
cd webharvest-ide/frontend
npm run watch  # Or: npm run dev

# Terminal 2: After each change, rebuild JAR
cd webharvest-ide
mvn jar:jar shade:shade -DskipTests
pkill -f webharvest-ide
java -jar target/webharvest-ide-2.3.1.jar &

# Browser: Hard refresh (Cmd+Shift+R) to see changes
```

**After ANY frontend change:**
1. ✅ Run `npm run build` in `frontend/` directory
2. ✅ Rebuild JAR with `mvn install -DskipTests`
3. ✅ Restart IDE
4. ✅ Hard refresh browser

**Common mistake:**
- ❌ Editing TypeScript files but not rebuilding
- ❌ Running `mvn install` without `npm run build` first
- ❌ Forgetting to hard refresh browser
- ❌ This leads to: "I don't see my changes!" 💔

---

## Visual Designer Architecture

### Advanced Features (v2.3.1)

**Connection Management:**
- Manual connections between nodes via port system
- Connection selection and context menu (Edit/Delete)
- Connection redefinition with validation
- Visual feedback for selected connections

**Node Management:**
- Root node reordering (Move Up/Down in context menu)
- Container movement with nested children
- Position initialization for nodes without positions
- Recursive child movement maintaining relative positions

**Context Menu:**
- Fixed positioning relative to viewport
- Root node detection for reordering options
- Connection-specific menu items
- Proper cleanup on close

**Event Listener Management:**
- `connectionLineElements` Map tracks connection line references
- Cleanup before each render prevents duplicate listeners
- Clone/replace pattern for removing event listeners

### Overview

The Visual Designer is a complete drag-and-drop editor for creating WebHarvest configurations visually, introduced in v2.3.

### Technology Stack

**Frontend:**
- **TypeScript** - Type-safe development
- **Vite** - Fast build tool and dev server
- **Vanilla DOM** - No framework overhead
- **SVG** - Canvas rendering for nodes and connections

**Backend:**
- **Jetty 11** - Embedded server
- **PluginMetadataCatalog** - 55 plugins with semantic metadata
- **PluginMetadataServlet** - API endpoint for plugin metadata

### Key Components

**Frontend (`webharvest-ide/frontend/src/`):**

```
core/
├── App.ts                    # Main application, mode switching
├── PluginManager.ts          # Loads 55 plugins from API
└── types/index.ts            # TypeScript type definitions

visual/
├── VisualDesigner.ts         # Core canvas, nodes, events
├── PluginPalette.ts          # Plugin list with search
├── ContainerRenderer.ts      # Expand/collapse containers
├── SemanticValidator.ts      # Validation rules
├── XMLParser.ts             # XML → Visual nodes
├── XMLGenerator.ts          # Visual nodes → XML
└── ConnectionManager.ts      # Manual connections
```

**Backend (`webharvest-core/src/main/java/org/webharvest/plugin/metadata/`):**

```
PluginMetadataCatalog.java    # Unified metadata catalog (55 plugins)
PluginDescriptor.java         # Plugin metadata structure
PluginSemantics.java          # Semantic roles and constraints
PluginSemanticsResolver.java  # Runtime semantic resolution
PluginClassScanner.java       # Dynamic plugin discovery
XSDGenerator.java             # Dynamic XSD schema generation
XSDMetadataParser.java        # XSD metadata extraction
```

### Semantic Metadata System

**Schema Version:** 1.1.0

**Key Features:**
- **Plugin Roles:** CONTAINER, PROCESSOR, TRANSFORMER, DATA_SOURCE, etc.
- **Dependencies:** Explicit parent-child relationships
- **Constraints:** `acceptsAnyChild`, `allowedChildTypes`, `requiredParent`
- **UI Hints:** Icons, categories, grouping

**Example:**
```json
{
  "elementName": "catch",
  "displayName": "Catch",
  "role": "ERROR_HANDLER",
  "semantics": {
    "role": "DEPENDENT",
    "requiresParent": ["try"],
    "canAppearAtRoot": false
  },
  "dependencies": ["try"]
}
```

**Files:**
- `plugin-metadata-schema.json` - JSON Schema validation (for API responses)
- `PluginMetadataCatalog` - Runtime catalog builder
- `PluginClassScanner` - Dynamic plugin discovery from classpath
- `XSDGenerator` - Dynamic XSD schema generation from metadata

**Deprecated/Removed:**
- `plugin-semantics.json` - ❌ Removed (all metadata generated dynamically)
- `plugin-ui-overrides.json` - ❌ Removed (UI hints auto-generated from category/element name)

### Dynamic Metadata Generation (v2.3.0+)

**All metadata is now generated dynamically from plugin classes - no static JSON files required.**

**Key Components:**

1. **PluginClassScanner** - Discovers all `@CorePlugin` classes at runtime
   - Uses Reflections library for classpath scanning
   - Caches results for performance
   - Provides `allPluginClassNames()` method

2. **PluginMetadataCatalog** - Builds unified metadata catalog
   - Extracts metadata from plugin classes and `PluginConfigurationSchema`
   - Applies optional UI overrides from `plugin-ui-overrides.json`
   - Generates complete semantic information (roles, constraints, dependencies)
   - Schema version: 2.3.0

3. **XSDGenerator** - Generates XSD schemas dynamically
   - Creates complete XSD from `PluginDescriptor` objects
   - Generates `config` element, `bodyElements` group, `bodyType`, and all plugin elements
   - Supports custom namespaces
   - Generates attributes, complex types, and child element structures
   - **Usage:**
     ```java
     // Generate XSD for default namespace
     String xsd = XSDGenerator.generateXSD(null);
     
     // Generate XSD for custom namespace
     String xsd = XSDGenerator.generateXSD("http://custom.namespace/schema/1.0");
     
     // Generate XSD for specific descriptors
     List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
     String xsd = XSDGenerator.generateXSD(descriptors, namespace);
     ```

4. **XSDMetadataParser** - Extracts metadata from existing XSD files
   - Can be used to fill gaps in plugin metadata
   - Parses XSD elements, attributes, and types
   - Useful for legacy plugin support

5. **PluginMetadataValidator** - Validates completeness and correctness of plugin metadata
   - Checks for required metadata fields (id, elementName, description, etc.)
   - Validates missing attribute definitions
   - Checks for inconsistent child/parent relationships
   - Detects circular dependencies
   - Identifies orphaned plugins (dependencies that don't exist)
   - Generates warnings for incomplete metadata
   - **Usage:**
     ```java
     // Validate all plugins
     ValidationResult result = PluginMetadataValidator.validateAll();
     
     // Validate specific descriptors
     List<PluginDescriptor> descriptors = PluginMetadataCatalog.getAllDescriptors();
     ValidationResult result = PluginMetadataValidator.validate(descriptors);
     
     // Check results
     if (!result.isValid()) {
         List<ValidationIssue> errors = result.getErrors();
         List<ValidationIssue> warnings = result.getWarnings();
         PluginMetadataValidator.logValidationResults(result);
     }
     ```

**Benefits:**
- ✅ **No static files** - All metadata comes from code
- ✅ **Always in sync** - Metadata matches actual plugin classes
- ✅ **Extensible** - New plugins automatically discovered
- ✅ **Type-safe** - Metadata extracted from Java annotations and schemas
- ✅ **Validated** - PluginMetadataValidator ensures completeness
- ✅ **Testable** - Full unit test coverage for all components

### Canvas Layout & Coordinate System

**Fixed Canvas Integration (v2.3.1):**
- Canvas properly embedded in flexbox layout (no longer "floating")
- Three-layer structure:
  - `.canvas-area` - Scrollable container (flex: 1 1 0%)
  - `.canvas-content-wrapper` - Transform wrapper (receives pan/zoom)
  - Nodes & SVG overlay - Absolute positioned content

**Coordinate System:**
- **Viewport coordinates** → **Canvas coordinates** conversion
- Method: `viewportToCanvas(x, y)` accounts for:
  - Container scroll position (`scrollLeft`, `scrollTop`)
  - Transform pan offset (`panOffset.x`, `panOffset.y`)
  - Zoom level (`zoom`)
- All drop/drag calculations use canvas coordinates for consistency

**Transform Handling:**
- Transform applied to `.canvas-content-wrapper` (not scrollable container)
- `transform-origin: 0 0` for top-left origin
- SVG overlay matches container size (before zoom)
- All node positions stored in canvas coordinates

**UI Improvements:**
- Run/Validate buttons moved to canvas area (top-right)
- Removed duplicate buttons from header toolbar
- Execution controls (Pause/Resume/Stop) appear during execution only

### Visual Designer Features

**Implemented:**
- ✅ Drag & drop from plugin palette
- ✅ Plugin nesting (containers)
- ✅ Reordering plugins with ⬆️⬇️ buttons
- ✅ Nested container drops
- ✅ Semantic validation
- ✅ XML ↔ Visual synchronization
- ✅ Undo/Redo/Copy/Paste
- ✅ History tracking
- ✅ Node dragging (works correctly with zoom/pan)
- ✅ Fixed canvas layout (properly embedded in layout)

**Planned:**
- Manual connections (ports)
- Execution state visualization
- Property panels with advanced editors
- Minimap and zoom controls
- Multi-select with marquee

### Data Flow

```
1. App.ts → Switch to Visual mode
2. Load XML → XMLParser.parseXML()
3. Render nodes → VisualDesigner.render()
4. User edits → Drag/drop/reorder
5. Validate → SemanticValidator.validateDrop()
6. Save → XMLGenerator.generateXML()
7. Sync → Update App state
```

### Testing

**TDD Test Files:**
- `VisualDesigner.test.ts` - Core canvas logic
- `SemanticValidator.test.ts` - Validation rules (20 tests)
- `NestedReordering.test.ts` - Reordering functionality
- `XMLParser.test.ts` - XML parsing
- `XMLGenerator.test.ts` - XML generation

**Test Coverage:** 599 passing tests

### Build & Deploy

See [IDE Development - Build Process](#ide-development) section above.

---

## Semantic Metadata System

### Overview

The semantic metadata system provides unified, curated information about all WebHarvest plugins, enabling the IDE to offer intelligent features like validation, autocomplete, and visual editing.

### Architecture

```
plugin-semantics.json (curated)
    ↓
PluginMetadataCatalog (Java)
    ↓
PluginMetadataServlet (API)
    ↓
PluginManager (TypeScript)
    ↓
SemanticValidator (Validation)
```

### Plugin Metadata Structure

Each plugin has:

```typescript
interface PluginDescriptor {
  elementName: string;        // XML element name
  displayName: string;        // Human-readable name
  documentation: {
    summary: string;          // Brief description
    url: string;              // Full docs URL
  };
  semantics: {
    role: string;             // CONTAINER, PROCESSOR, etc.
    acceptsAnyChild: boolean;
    allowedChildTypes: string[];
    requiresParent: string[];
    canAppearAtRoot: boolean;
  };
  dependencies: string[];     // Parent types
  ui: {
    icon: string;             // Font Awesome icon
    category: string;         // HTTP & Web, Data Extraction, etc.
    paletteGroup: string;     // Grouping in palette
  };
}
```

### Usage in IDE

**Validation:**
```typescript
const result = validator.validateDrop('catch', 'try');
if (!result.valid) {
  showError(result.message); // "catch requires try parent"
}
```

**Palette Filtering:**
```typescript
const canAppearAtRoot = plugin.semantics?.canAppearAtRoot;
if (!canAppearAtRoot) {
  // Gray out, disable drag
}
```

**Documentation Links:**
```typescript
<a href={plugin.documentation.url}>View Docs</a>
```

### Adding New Plugins

**For Core Plugins:**
1. Add `@CorePlugin` annotation to Java class
2. PluginMetadataCatalog auto-discovers it
3. Add curated metadata to `plugin-semantics.json` if needed

**For External Plugins:**
1. Add to `PluginClassIndex`
2. Add curated metadata to `plugin-semantics.json`
3. Rebuild core module

### Documentation

- **README:** `docs/semantic-metadata/README.md`
- **Inventory:** `docs/semantic-metadata/PLUGIN_INVENTORY.md`
- **Schema:** `plugin-metadata-schema.json`

---

## Architecture Overview

WebHarvest follows a modular, plugin-based architecture designed for extensibility and maintainability.

### Core Components

- **Plugin System**: Extensible plugin architecture with automatic discovery
- **Configuration Engine**: XML-based configuration parsing and validation
- **Runtime Engine**: Execution engine with context management
- **Variable System**: Dynamic variable handling and scoping
- **Template Engine**: Dynamic content generation
- **Scripting Engine**: JavaScript, Groovy, BeanShell support

### Design Patterns

- **Template Method Pattern**: Used in `AbstractPlugin` for common execution flow
- **Strategy Pattern**: Used for different plugin implementations
- **Factory Pattern**: Used for plugin instantiation
- **Decorator Pattern**: Used in `LockedRegistry` for thread safety
- **Dependency Injection**: Used throughout with Google Guice

## Plugin Development

### Creating a New Plugin

#### Bug #40 Feature (v2.2.1): Automatic Plugin Discovery

WebHarvest now supports automatic discovery of plugins from external packages! You can create plugins in your own packages and have them automatically registered without modifying core code.

**System Property Configuration:**
```bash
# Scan custom packages for plugins
java -Dwebharvest.plugin.packages=com.example.plugins,com.mycompany.webharvest \
     -jar webharvest.jar config.xml
```

**Programmatic Configuration:**
```java
import org.webharvest.plugin.scanner.PluginPackageScanner;
import org.webharvest.definition.CorePluginRegistry;

CorePluginRegistry registry = CorePluginRegistry.getInstance();
PluginPackageScanner scanner = new PluginPackageScanner(registry);

// Scan your custom packages
scanner.scanPackages("com.example.plugins", "com.mycompany.webharvest");
```

### Step-by-Step Plugin Creation

1. **Extend AbstractPlugin**:
```java
package com.example.plugins;  // Your custom package!

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.NodeVariable;

@CorePlugin(elementName = "myplugin")
public class MyPlugin extends AbstractPlugin {
    
    @Override
    protected Variable doExecute(DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        // Plugin implementation
        return new NodeVariable("result");
    }
    
    @Override
    protected void doValidateConfiguration(PluginConfiguration config) 
            throws PluginValidationException {
        // Configuration validation
    }
}
```

2. **Define Plugin Metadata**:
```java
private static final PluginMetadata METADATA = new PluginMetadata() {
    @Override
    public String getName() { return "myplugin"; }
    
    @Override
    public String getDescription() { return "My custom plugin"; }
    
    @Override
    public String getVersion() { return "1.0.0"; }
    
    @Override
    public PluginConfigurationSchema getConfigurationSchema() {
        return PluginConfigurationSchema.builder()
            .addRequiredProperty("requiredParam")
            .addOptionalProperty("optionalParam", "defaultValue")
            .build();
    }
};
```

3. **Implement Configuration Validation**:
```java
@Override
protected void doValidateConfiguration(PluginConfiguration config) 
        throws PluginValidationException {
    String requiredParam = config.getProperty("requiredParam");
    if (requiredParam == null || requiredParam.isEmpty()) {
        throw new PluginValidationException("requiredParam is required");
    }
}
```

### Plugin Lifecycle

1. **Initialization**: Configuration is validated and set
2. **Execution**: Plugin performs its main functionality
3. **Cleanup**: Resources are released and results are returned

### Plugin Discovery (Bug #40 - v2.2.1)

**Automatic Scanning:**
- Place your plugin in any package
- Add `@CorePlugin(elementName = "yourplugin")` annotation
- Configure scanning via system property
- Plugin automatically discovered and registered

**Disabling XML Schema Validation (Bug #40 - v2.2.1):**

If your custom plugin doesn't have an XSD schema, you can disable validation:

```bash
# Disable schema validation for development
java -Dwebharvest.schema.validation=false \
     -Dwebharvest.plugin.packages=com.example.plugins \
     -jar webharvest.jar config.xml
```

This is useful for:
- Custom plugins without XSD schemas
- Development and testing environments
- Faster parsing (no schema validation overhead)

**Production Use:**
```bash
# Production: Enable validation (default), scan custom packages
java -Dwebharvest.plugin.packages=com.example.plugins \
     -jar webharvest.jar config.xml
```

### Best Practices

#### DOM Structure Validation (Bug #14 - Already Implemented)

Always validate DOM structure before data extraction to handle page changes gracefully:

**Method 1: XPath count() validation**
```xml
<def var="hasRequiredElements">
  <xpath expression="count(//div[@class='product']) > 0 and count(//span[@class='price']) > 0">
    <get var="page"/>
  </xpath>
</def>

<if condition="${hasRequiredElements}">
  <!-- Safe to extract -->
</if>
```

**Method 2: Multiple checks with If/Else**
```xml
<def var="titleCount"><xpath expression="count(//h1)">${page}</xpath></def>
<def var="priceCount"><xpath expression="count(//span[@class='price'])">${page}</xpath></def>

<if condition="${titleCount > 0 && priceCount > 0}">
  <!-- Structure valid -->
</if>
```

**Method 3: Try/Catch for flexible validation**
```xml
<try>
  <def var="price">
    <xpath expression="//span[@class='price']/text()">${page}</xpath>
  </def>
</try>
<catch>
  <def var="price" value="N/A"/>  <!-- Fallback if structure missing -->
</catch>
```

#### General Best Practices

- **Thread Safety**: Plugins should be stateless and thread-safe
- **Error Handling**: Use appropriate exception types (`PluginException`, `PluginValidationException`)
- **Logging**: Use SLF4J for consistent logging
- **Resource Management**: Properly close resources in try-with-resources blocks
- **Configuration**: Validate all required parameters
- **Package Naming**: Use reverse domain notation (com.company.project.plugins)
- **Testing**: Write comprehensive tests for your custom plugins

## Core APIs

### AbstractPlugin

Base class for all plugins providing common functionality:

```java
public abstract class AbstractPlugin implements Plugin {
    // Template method for execution
    public final Variable execute(DynamicScopeContext context) 
            throws InterruptedException, PluginException;
    
    // Abstract methods to implement
    protected abstract Variable doExecute(DynamicScopeContext context) 
            throws PluginException, InterruptedException;
    
    protected abstract void doValidateConfiguration(PluginConfiguration config) 
            throws PluginValidationException;
}
```

### DynamicScopeContext

Runtime context for variable management and execution state:

```java
public class DynamicScopeContext {
    // Variable management
    public void setVar(String name, Variable value);
    public Variable getVar(String name);
    public boolean hasVar(String name);
    
    // Context information
    public String getCharset();
    public void setCharset(String charset);
    
    // Execution control
    public boolean isInterrupted();
    public void checkInterrupted() throws InterruptedException;
}
```

### PluginConfiguration

Configuration management for plugins:

```java
public interface PluginConfiguration {
    // Property access
    String getProperty(String name);
    String getProperty(String name, String defaultValue);
    void setProperty(String name, String value);
    
    // Configuration metadata
    String getElementName();
    String getNamespace();
}
```

### Variable System

WebHarvest uses a flexible variable system:

```java
// Variable types
NodeVariable nodeVar = new NodeVariable("text content");
ListVariable listVar = new ListVariable(Arrays.asList("item1", "item2"));
EmptyVariable emptyVar = EmptyVariable.INSTANCE;

// Variable operations
String text = nodeVar.toString();
List<Object> items = listVar.toList();
boolean isEmpty = variable.isEmpty();
```

## Configuration System

WebHarvest has two configuration systems:
1. **Application Settings** - Application-wide configuration (CLI args, proxy, logging, etc.)
2. **Workflow Configuration** - XML-based scraping workflow definitions

### Application Settings (NEW in v2.2.1 - Bug #39)

#### Overview
WebHarvest v2.2.1 introduces a unified settings system that eliminates configuration duplication between GUI and CLI modes.

**Key Features:**
- Single configuration source for GUI and CLI
- Modern CLI syntax using Apache Commons CLI
- Auto-discovery from standard locations
- CLI arguments can override file settings
- Backward compatible with legacy format

**Key Classes:**
- `org.webharvest.settings.WebHarvestSettings` - Settings container
- `org.webharvest.cli.CommandLineParser` - CLI argument parser

#### Usage Example

```java
// Load settings from default locations
WebHarvestSettings settings = WebHarvestSettings.loadDefault();
// Auto-discovers from:
// 1. -Dwebharvest.settings=path
// 2. ~/.webharvest/webharvest.properties
// 3. ./webharvest.properties

// Override with Builder
WebHarvestSettings custom = new WebHarvestSettings.Builder(settings)
    .workDir("/custom/path")
    .debug(true)
    .proxyHost("proxy.company.com")
    .build();

// Parse CLI arguments
CommandLineParser parser = new CommandLineParser(settings);
WebHarvestSettings finalSettings = parser.parse(args);
```

**CLI Formats:**

Modern (recommended):
```bash
java -jar webharvest-cli.jar -c config.xml -w /tmp --loglevel DEBUG
```

Legacy (still supported):
```bash
java -jar webharvest-cli.jar config=config.xml workdir=/tmp debug=true
```

**For complete documentation, see:**
- `BUG_39_IMPLEMENTATION_SUMMARY.md`
- `docs/web/documentation/settings-cli.html`

### Workflow Configuration (XML)

### XML Schema

WebHarvest uses XML Schema for configuration validation:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- Plugin configurations -->
</config>
```

### Configuration Parsing

```java
// Parse configuration
ConfigParser parser = new SAXConfigParser();
ConfigSource source = new FileConfigSource("config.xml");
IElementDef rootElement = parser.parse(source);

// Create config object
Config config = new LazyConfig(source);
config.reload();
IElementDef elementDef = config.getElementDef();
```

### Plugin Configuration Schema

```java
PluginConfigurationSchema schema = PluginConfigurationSchema.builder()
    .addRequiredProperty("url")
    .addOptionalProperty("method", "GET")
    .addOptionalProperty("timeout", "30000")
    .addOptionalProperty("followRedirects", "true")
    .build();
```

## Runtime System

### Scraper Execution

```java
// Create scraper
Config config = Config.fromFile("scraper.xml");
Scraper scraper = new Scraper(config);

// Execute with context
DynamicScopeContext context = new ScraperContext();
scraper.execute(context);

// Get results
Variable result = context.getVar("result");
```

### Error Handling

```java
try {
    scraper.execute(context);
} catch (PluginException e) {
    logger.error("Plugin execution failed", e);
} catch (InterruptedException e) {
    logger.warn("Execution was interrupted", e);
    Thread.currentThread().interrupt();
}
```

### Context Management

```java
// Set variables
context.setVar("baseUrl", new NodeVariable("https://example.com"));
context.setVar("userAgent", new NodeVariable("WebHarvest/2.1.0"));

// Get variables
Variable baseUrl = context.getVar("baseUrl");
String url = baseUrl.toString();
```

## Testing Guidelines

This comprehensive guide documents the testing methodology proven effective during development of WebHarvest 2.2.1, where we achieved **2600 tests with 100% pass rate and zero regressions** while fixing 8 bugs.

### Testing Philosophy

**Core Principle**: *"Fixing bugs correctly prevents new bugs"*

Every bug fix follows this pattern:
1. **Analyze** - Understand root cause
2. **Fix** - Implement solution
3. **Test** - Bug-specific + regression tests
4. **Document** - JavaDoc + BUGS_FIXED.md

**Result**: Zero regression rate across 2600 tests.

---

### Test Types and When to Use Them

#### 1. **Bug-Specific Tests** (Hard Tests)

**Purpose**: Reproduce the exact issue reported by users.

**When to use**:
- Fixing a reported bug
- Need to verify the issue exists
- Want to prevent regression

**Structure**:
```java
@Test(groups = {"unit", "bugs", "hard"})
public void testBug47_ConfigFileReadAsUtf8_NotPlatformDefault() throws Exception {
    System.out.println("\n### Bug #47: UTF-8 test ###");
    
    // Create test file with UTF-8 characters (reproduce issue)
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>UTF-8: ąćęłńóśźż 中文 Русский</config>";
    File testFile = new File("target/test-output/bug47_utf8_config.xml");
    Files.write(testFile.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
    
    // Read using FileConfigSource (should use UTF-8, not platform default)
    FileConfigSource source = new FileConfigSource(testFile);
    Reader reader = source.getReader();
    
    // Read content
    String readContent = readAll(reader);
    
    // Verify UTF-8 characters are preserved (not garbled)
    assertTrue(readContent.contains("ąćęłńóśźż"), "Polish chars should be preserved");
    assertTrue(readContent.contains("中文"), "Chinese chars should be preserved");
    assertTrue(readContent.contains("Русский"), "Russian chars should be preserved");
    
    System.out.println("✅ Bug #47: Config file read as UTF-8");
}
```

**Key Elements**:
- Descriptive name with bug number
- Console output for visibility
- Reproduces exact user scenario
- Clear assertion messages
- Cleanup (delete test files)

#### 2. **Regression Tests**

**Purpose**: Ensure fix doesn't break existing functionality.

**When to use**:
- After every bug fix
- Alongside bug-specific tests
- Testing edge cases

**Structure**:
```java
@Test(groups = {"unit", "bugs", "regression"})
public void testAsciiConfigFile_NoRegression() throws Exception {
    // Test that ASCII-only files still work perfectly after UTF-8 fix
    File testFile = new File("target/test-output/ascii_config.xml");
    String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<config>ASCII only content 123 ABC</config>";
    Files.write(testFile.toPath(), xmlContent.getBytes(StandardCharsets.UTF_8));
    
    FileConfigSource source = new FileConfigSource(testFile);
    String content = readAll(source.getReader());
    
    // ASCII should be identical
    assertEquals(content, xmlContent);
    System.out.println("✅ ASCII config files work perfectly (no regression)");
}
```

**Best Practices**:
- Test the "happy path" still works
- Test common use cases
- Test simpler scenarios
- Quick to run

#### 3. **Integration Tests** (XML-Based)

**Purpose**: Test complete scraping scenarios with real XML configurations.

**When to use**:
- Testing plugin interactions
- End-to-end scenarios
- Real-world use cases

**Base Class Pattern**:
```java
public abstract class XmlBasedIntegrationTestBase {
    
    protected void executeXmlConfig(String xmlFilePath) throws Exception {
        Injector injector = Guice.createInjector(
            new ScraperModule("."),
            new HttpModule(ProxySettings.NO_PROXY_SET)
        );
        
        Harvest harvest = injector.getInstance(Harvest.class);
        ConfigSourceFactory factory = injector.getInstance(ConfigSourceFactory.class);
        
        // Load XML from test resources
        URL resourceUrl = getClass().getClassLoader().getResource(xmlFilePath);
        ConfigSource configSource = factory.create(new File(resourceUrl.toURI()));
        
        // Execute configuration
        Harvester harvester = harvest.getHarvester(configSource, callback);
        harvester.execute(contextCallback);
    }
    
    protected String executeAndReadOutputFile(String xmlPath, String outputPath) {
        String fullOutputPath = "target/test-output/" + outputPath;
        outputFilesToCleanup.add(fullOutputPath);
        
        executeXmlConfig(xmlPath);
        
        Path outputPath = Paths.get(fullOutputPath);
        assertTrue(Files.exists(outputPath), "Output file should exist: " + fullOutputPath);
        
        return new String(Files.readAllBytes(outputPath));
    }
}
```

**Test Implementation**:
```java
public class Bug62RegexpGroupIsolationIntegrationTest extends XmlBasedIntegrationTestBase {
    
    @Test(groups = {"integration", "bugs"})
    public void testBug62_RegexpGroupIsolation() throws Exception {
        System.out.println("\n### Bug #62: Regexp group isolation ###");
        
        // Execute XML that uses two sequential regexps
        String xmlPath = "integration-tests/bug62_regexp_group_isolation.xml";
        String output = executeAndReadOutputFile(xmlPath, "bug62_output.txt");
        
        // Verify: second regexp should have its own group values
        assertTrue(output.contains("Group from second: correct_value"));
        assertFalse(output.contains("Group from first: old_value"));
        
        System.out.println("✅ Bug #62: Regexp groups isolated correctly");
    }
}
```

**XML Configuration** (in `src/test/resources/integration-tests/`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<config xmlns="http://org.webharvest/schema/2.1/core">
    <!-- First regexp sets group0-group2 -->
    <regexp pattern="(\w+):(\d+)" source="test:123">
        <def var="first_group0">${group0}</def>
        <def var="first_group1">${group1}</def>
    </regexp>
    
    <!-- Second regexp should have its OWN groups, not first's -->
    <regexp pattern="(\w+)" source="hello">
        <def var="second_group0">${group0}</def>
        <def var="second_group1">${group1}</def>
    </regexp>
    
    <file action="write" path="target/test-output/bug62_output.txt">
        First: ${first_group0}, ${first_group1}
        Second: ${second_group0}, ${second_group1}
    </file>
</config>
```

#### 4. **Edge Case Tests**

**Purpose**: Test unusual or boundary conditions.

**Examples**:
```java
@Test(groups = {"unit", "bugs", "edge-case"})
public void testEmptyConfigFile_NoException() throws Exception {
    // Empty file should not cause errors
    File testFile = new File("target/test-output/empty.xml");
    testFile.createNewFile();
    
    FileConfigSource source = new FileConfigSource(testFile);
    Reader reader = source.getReader();
    
    int charsRead = reader.read(new char[1024]);
    assertEquals(charsRead, -1, "Empty file should return -1");
    
    System.out.println("✅ Empty file handled gracefully");
}

@Test(groups = {"unit", "bugs", "edge-case"})
public void testNativeJavaObject_WrappingNull() throws Exception {
    // NativeJavaObject wrapping null should not crash
    Context cx = Context.enter();
    try {
        Scriptable scope = cx.initStandardObjects();
        ScriptableObject.putProperty(scope, "nullObj", null);
        Object nativeJavaObject = cx.evaluateString(scope, "nullObj", "test", 1, null);
        
        Variable result = CommonUtil.createVariable(nativeJavaObject);
        
        assertTrue(result instanceof NodeVariable);
        System.out.println("✅ NativeJavaObject wrapping null handled");
    } finally {
        Context.exit();
    }
}
```

---

### Test Organization and Configuration

#### TestNG Groups

Tests are organized into groups for selective execution:

```java
// Bug-specific test
@Test(groups = {"unit", "bugs", "hard"})

// Regression test
@Test(groups = {"unit", "bugs", "regression"})

// Integration test
@Test(groups = {"integration", "bugs"})

// Edge case
@Test(groups = {"unit", "bugs", "edge-case"})
```

**Run specific groups**:
```bash
# Run only bug tests
mvn test -Dgroups="bugs"

# Run only unit tests
mvn test -Dgroups="unit"

# Run only integration tests
mvn test -Dgroups="integration"
```

#### Test File Naming Conventions

```
Unit Tests:
- ClassNameTest.java (basic unit tests)
- ClassNameBug##Test.java (bug-specific unit tests)
- ClassNameEnhancedTest.java (enhanced coverage tests)

Integration Tests:
- Bug##DescriptionIntegrationTest.java
- PluginNameIntegrationTest.java

XML Configurations:
- integration-tests/bug##_description.xml
- integration-tests/plugin_name_example.xml
```

#### Test Output Management

**CRITICAL RULE**: All test outputs go to `target/` directory

**Working Directory Configuration:**
- **Tests**: `ScraperModule` is initialized with `workingDir = "target"` in test base classes
- **IDE**: `ExecutionManager` uses `workingDir = "target"` for all executions
- **Result**: All `<file>` operations write to `target/` by default

```java
// GOOD ✅ - Relative path resolves to target/output.txt
<file action="write" path="output.txt">...</file>

// GOOD ✅ - Absolute paths still work
<file action="write" path="/tmp/output.txt">...</file>

// BAD ❌ - Old pattern (no longer needed)
<file action="write" path="target/test-output/output.txt">...</file>  // Creates target/target/test-output/...
```

**Cleanup**:
```java
@AfterMethod
public void cleanup() {
    for (String filePath : outputFilesToCleanup) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // Log but don't fail
        }
    }
    outputFilesToCleanup.clear();
}
```

---

### Test Coverage with JaCoCo

#### Maven Configuration (`pom.xml`):

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.13</version>
    <executions>
        <execution>
            <id>default-prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>default-report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### Generate Coverage Report:

```bash
# Run tests and generate coverage
mvn clean test

# View report
open target/site/jacoco/index.html
```

#### Coverage Targets:

- **Core plugins**: 50%+ coverage
- **Critical components**: 60%+ coverage
- **Bug fixes**: 100% coverage of fixed code paths

**Why 50% is good**:
- Covers main execution paths
- Includes edge cases
- Catches most bugs (proven in v2.2.1)
- Balances effort vs. value

---

### Documentation Standards for Tests

#### 1. Test Class JavaDoc

```java
/**
 * Test for Bug #47: Character Set Problem in CLI Mode
 * 
 * Issue: FileConfigSource used FileReader which relies on platform default charset
 * (e.g., WIN1250 on Windows instead of UTF-8), causing encoding problems.
 * 
 * Fix: Changed to InputStreamReader with explicit UTF-8 charset.
 * 
 * This test suite ensures the fix works correctly and prevents regression.
 */
public class FileConfigSourceBug47Test {
    // ...
}
```

#### 2. Test Method Documentation

```java
/**
 * Test Bug #47: Config files should be read as UTF-8, not platform default
 * 
 * This test creates a file with UTF-8 characters (Polish, Chinese, Russian)
 * and verifies they are preserved when read through FileConfigSource.
 * 
 * Before fix: Characters would be garbled on Windows (WIN1250)
 * After fix: Characters preserved correctly on all platforms
 */
@Test(groups = {"unit", "bugs", "hard"})
public void testConfigFileReadAsUtf8_NotPlatformDefault() throws Exception {
    // ...
}
```

#### 3. Console Output for Debugging

```java
System.out.println("\n### Bug #47: UTF-8 test ###");
System.out.println("  Original: " + originalContent.substring(0, 100) + "...");
System.out.println("  Read:     " + readContent.substring(0, 100) + "...");
System.out.println("✅ Bug #47: Config file read as UTF-8");
```

**Benefits**:
- Easy to see which test is running
- Quick debugging when test fails
- Clear success indicators
- Helps with long test suites

#### 4. BUGS_FIXED.md Integration

Every bug fix must be documented in `BUGS_FIXED.md`:

```markdown
### Bug #47: Character Set Problem in CLI Mode

**Status**: ✅ FIXED  
**Fixed in**: v2.2.1  
**Severity**: High  

#### Description
Configuration files loaded in CLI mode were read using platform default
charset instead of UTF-8, causing encoding problems.

#### Root Cause
FileConfigSource.java line 137:
```java
return new FileReader(file);  // Platform default charset!
```

#### Fix
```java
return new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
```

#### Tests Added
1. ✅ testConfigFileReadAsUtf8_NotPlatformDefault()
2. ✅ testWin1250FileWithUtf8Reader_CharactersHandled()
3. ✅ testAsciiConfigFile_NoRegression()
4. ✅ testEmptyConfigFile_NoException()

**Test Results**: 4/4 PASS, Full suite: 2594/2594 PASS, Zero regression
```

---

### Complete Example: Bug Fix Workflow

Let's walk through fixing Bug #55 as a complete example:

#### Step 1: Analyze the Bug

**User Report**:
> "The '#' character in URL parameter (e.g., `?param=#value`) causes URL truncation."

**Root Cause Analysis**:
```bash
# Find where URL encoding happens
grep -r "encodeUrl" webharvest-core/src/main/java/

# Identify: CommonUtil.encodeUrlParam() line 263
# Problem: '#' not encoded, treated as URL fragment
```

#### Step 2: Write Bug-Specific Test

```java
@Test(groups = {"unit", "bugs", "hard"})
public void testBug55_HashInParameterValue_EncodedCorrectly() {
    String param = "value#123";  // Hash in middle
    String encoded = CommonUtil.encodeUrlParam(param, "UTF-8");
    
    assertTrue(encoded.contains("%23"), 
        "Hash should be encoded as %23");
    assertFalse(encoded.contains("#"), 
        "Hash should not appear unencoded");
}
```

#### Step 3: Implement Fix

```java
// Bug #55 Fix: Encode '#' as %23
private static String encodeUrlParam(String value, String charset) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < decoded.length(); i++) {
        char ch = decoded.charAt(i);
        // NEW: Encode '#' as %23 (was previously left unencoded)
        result.append(URLEncoder.encode(String.valueOf(ch), charset));
    }
    return result.toString();
}
```

#### Step 4: Add Regression Tests

```java
@Test(groups = {"unit", "bugs", "regression"})
public void testUrlEncoding_NormalParameters_NoRegression() {
    String param = "normal_value";
    String encoded = CommonUtil.encodeUrlParam(param, "UTF-8");
    assertEquals(encoded, "normal_value");
}

@Test(groups = {"unit", "bugs", "regression"})
public void testUrlEncoding_OtherSpecialChars_StillWork() {
    String param = "value&123";
    String encoded = CommonUtil.encodeUrlParam(param, "UTF-8");
    assertTrue(encoded.contains("%26"));  // & encoded
}
```

#### Step 5: Run Tests

```bash
# Run bug-specific tests
mvn test -Dtest=CommonUtilBug55Test

# Run full suite (check for regressions)
mvn test

# Check coverage
mvn jacoco:report
open target/site/jacoco/index.html
```

#### Step 6: Update Documentation

**JavaDoc**:
```java
/**
 * Encodes URL parameter value.
 * 
 * <h3>Bug #55 Fix (v2.2.1): Hash Encoding</h3>
 * <p>
 * The '#' character is now properly encoded as %23 to prevent URL truncation.
 * </p>
 * 
 * @version 2.2.1
 */
private static String encodeUrlParam(String value, String charset) {
    // ...
}
```

**BUGS_FIXED.md**:
- Add complete bug description
- Document root cause
- Show before/after code
- List all tests added
- Report test results

#### Step 7: Commit

```bash
svn commit -m "Fix Bug #55: Hash in URL parameters (v2.2.1)

ISSUE: '#' in URL parameter values causes URL truncation
FIX: Encode '#' as %23 in CommonUtil.encodeUrlParam()
TESTS: 7 tests (4 bug + 3 regression) - all PASS
COVERAGE: Full suite 2594/2594 PASS, zero regression
DOCS: Updated JavaDoc, BUGS_FIXED.md, CHANGELOG.md"
```

---

### Running Tests Efficiently

#### During Development:

```bash
# Run single test class
mvn test -Dtest=Bug55Test

# Run single test method
mvn test -Dtest=Bug55Test#testHashEncoding

# Run all bug tests
mvn test -Dgroups="bugs"

# Quick compile + test
mvn clean compile test-compile
```

#### Before Commit:

```bash
# Full test suite (MUST pass 100%)
mvn clean test

# Check coverage
mvn jacoco:report

# Verify no regressions
grep "Tests run:" target/surefire-reports/*.txt
```

#### CI/CD:

```bash
# Full build with all checks
mvn clean install

# Coverage threshold enforcement
mvn jacoco:check -Dcoverage.minimum=0.50
```

---

### Common Pitfalls and Solutions

#### Pitfall 1: Platform-Dependent Tests

**Bad**:
```java
// Assumes Unix file paths
File f = new File("/tmp/test.txt");
```

**Good**:
```java
// Use platform-independent paths
File f = new File("target/test-output/test.txt");
```

#### Pitfall 2: Polluting Project Root

**Bad**:
```xml
<file action="write" path="test_output.txt">...</file>
```

**Good**:
```xml
<file action="write" path="target/test-output/test_output.txt">...</file>
```

#### Pitfall 3: Test Order Dependencies

**Bad**:
```java
// Test B depends on Test A running first
static String sharedState;
```

**Good**:
```java
// Each test is independent
@BeforeMethod
public void setup() {
    // Fresh state for each test
}
```

#### Pitfall 4: Ignoring Cleanup

**Bad**:
```java
// Files left behind after test
Files.write(path, content);
```

**Good**:
```java
@AfterMethod
public void cleanup() {
    Files.deleteIfExists(path);
}
```

---

### Metrics and Success Criteria

Track these metrics for quality assurance:

```
Test Suite Size: 2600 tests (baseline: 2562)
Pass Rate: 100% (2600/2600)
Regression Rate: 0% (zero regressions introduced)
Coverage: ~50% (core modules)
Bugs Fixed: 8 (with comprehensive tests)
Duplicates Found: 1 (Bug #36 = #47)
```

**Success Criteria for Release**:
- ✅ All tests pass (100%)
- ✅ No regressions introduced
- ✅ Coverage maintained or improved
- ✅ All bugs documented
- ✅ JavaDoc updated
- ✅ BUGS_FIXED.md current

---

### Real-World Results (v2.2.1)

This methodology was used throughout v2.2.1 development with excellent results:

**Bugs Fixed/Verified**: 8
- Bug #62: Regexp group isolation
- Bug #61: JSONToXML content key
- Bug #55: Hash in URL parameters
- Bug #48: HTTP response streaming
- Bug #47/36: Charset in CLI (duplicate detected)
- Bug #29: NativeJavaObject handling
- Bug #16: Try/Catch verified

**Tests Added**: +38 comprehensive tests
**Final Suite**: 2600 tests, 100% passing
**Regressions**: Zero
**Coverage**: ~50% (all modules)

**Key Achievement**: *"Fixing bugs prevents new bugs"* - PROVEN with zero regression rate.

---

### Quick Reference Card

```
Test Type          | When to Use              | Group Tag
-------------------|--------------------------|----------------
Bug-Specific       | Reproduce user issue     | bugs, hard
Regression         | After any fix            | bugs, regression
Integration (XML)  | End-to-end scenarios     | integration, bugs
Edge Case          | Boundary conditions      | bugs, edge-case

Output Location: target/ (workingDir is set to "target" in test base classes)
Coverage Target: 50%+ (core), 100% (bug fixes)
Pass Requirement: 100% (zero failures)
Regression Policy: Zero tolerance

Documentation Required:
 ✅ Test class JavaDoc
 ✅ Test method docs
 ✅ Console output
 ✅ BUGS_FIXED.md entry
 ✅ Code JavaDoc update
 ✅ CHANGELOG.md entry
```

---

### Additional Resources

- **JaCoCo Documentation**: https://www.jacoco.org/jacoco/
- **TestNG Groups**: https://testng.org/doc/documentation-main.html#test-groups
- **Maven Surefire**: https://maven.apache.org/surefire/maven-surefire-plugin/
- **Integration Testing**: `webharvest-core/src/test/java/org/webharvest/plugin/integration/`
- **Bug Examples**: `BUGS_FIXED.md` - 8 complete bug fix examples

## Code Quality Standards

### Coding Standards

- Follow Java coding conventions
- Use meaningful variable and method names
- Add comprehensive Javadoc comments
- Use appropriate design patterns
- Keep methods focused and small

### Documentation Standards

- Class-level Javadoc with purpose and usage examples
- Method-level Javadoc with parameters, return values, and exceptions
- Inline comments for complex logic
- README files for major components

### Performance Considerations

- Use efficient data structures
- Minimize object creation in loops
- Use try-with-resources for resource management
- Consider memory usage for large datasets
- Profile performance-critical code

### Security Considerations

- Validate all input parameters
- Sanitize user-provided data
- Use secure HTTP connections when possible
- Avoid exposing sensitive information in logs
- Follow secure coding practices

## Examples

### Simple HTTP Plugin

```java
@CorePlugin(elementName = "http")
public class HttpPlugin extends AbstractPlugin {
    
    @Override
    protected Variable doExecute(DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        PluginConfiguration config = getConfiguration();
        String url = config.getProperty("url");
        String method = config.getProperty("method", "GET");
        
        try {
            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod(method);
            
            int statusCode = connection.getResponseCode();
            if (statusCode >= 400) {
                throw new PluginException("HTTP error: " + statusCode);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return new NodeVariable(response.toString());
            }
        } catch (Exception e) {
            throw new PluginException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    protected void doValidateConfiguration(PluginConfiguration config) 
            throws PluginValidationException {
        String url = config.getProperty("url");
        if (url == null || url.isEmpty()) {
            throw new PluginValidationException("URL is required");
        }
    }
}
```

### Custom Data Processing Plugin

```java
@CorePlugin(elementName = "process")
public class ProcessPlugin extends AbstractPlugin {
    
    @Override
    protected Variable doExecute(DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        PluginConfiguration config = getConfiguration();
        String input = config.getProperty("input");
        String operation = config.getProperty("operation", "uppercase");
        
        Variable bodyResult = executeBody(context);
        String data = bodyResult.toString();
        
        String result;
        switch (operation.toLowerCase()) {
            case "uppercase":
                result = data.toUpperCase();
                break;
            case "lowercase":
                result = data.toLowerCase();
                break;
            case "reverse":
                result = new StringBuilder(data).reverse().toString();
                break;
            default:
                throw new PluginValidationException("Unknown operation: " + operation);
        }
        
        return new NodeVariable(result);
    }
    
    @Override
    protected void doValidateConfiguration(PluginConfiguration config) 
            throws PluginValidationException {
        String operation = config.getProperty("operation", "uppercase");
        if (!isValidOperation(operation)) {
            throw new PluginValidationException("Invalid operation: " + operation);
        }
    }
    
    private boolean isValidOperation(String operation) {
        return "uppercase".equals(operation) || 
               "lowercase".equals(operation) || 
               "reverse".equals(operation);
    }
}
```

This developer guide provides comprehensive information for developing with WebHarvest, including architecture overview, plugin development, core APIs, and best practices.








