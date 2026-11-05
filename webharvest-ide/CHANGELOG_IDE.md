# Web-Harvest IDE - Change Log

## Version 2.3.3 - December 9, 2025

### 🎯 Semantic Model Alignment - LLM Agent Integration

#### Backend Enhancements
- ✅ **Schema Version Synchronization** - Added `/api/plugins/metadata/version` endpoint
- ✅ **Ontology Endpoint** - Added `/api/plugins/ontology` for unified ontology schema
- ✅ **Execution Semantics** - Added to `PluginDescriptor.extensions`
- ✅ **Port Types Defaults** - Always exported, even when `VisualPluginMetadata` unavailable

#### Frontend Enhancements
- ✅ **Extended Plugin Semantics** - Full `roles` array, `executionSemantics`, `variableSemantics`
- ✅ **Variable Semantics Correlation** - Automatic correlation with `VariableDependency`
- ✅ **LLM-Friendly Format** - `toLLMFormat()` method for JSON serialization
- ✅ **Schema Extensions Unification** - `SchemaMapper` utility always uses `extensions.schema.attributeDefinitions`
- ✅ **Port Compatibility Matrix** - Added for LLM agent routing decisions

#### Technical Details
- **API Endpoints**: `/api/plugins/metadata/version`, `/api/plugins/ontology`
- **New Files**: `SchemaMapper.ts` utility
- **Modified Files**: 9 files (2 backend, 7 frontend)
- **Build Status**: ✅ Compilation successful
- **Documentation**: Complete implementation docs added

See `SEMANTIC_MODEL_ALIGNMENT_IMPLEMENTATION.md` for full details.

---

## Version 2.3.2 - November 24, 2025

### 🎯 Canvas Layout & Connection Routing Optimization

#### Collision Detection & Prevention
- ✅ **Node collision detection** - Prevents nodes from being placed on top of each other
  - `checkNodeCollision()` - Detects full node bounding box collisions
  - `checkLabelCollision()` - Detects label (top 40px) collisions
  - Visual feedback with red outline when collision detected during drag

- ✅ **Automatic node pushing** - Automatically moves conflicting nodes when dragging
  - `pushNodesAway()` - Intelligently pushes nodes away from dragged node
  - Recursive collision checking for pushed nodes
  - Grid snapping for all moved nodes

- ✅ **Nearest free position finder** - Finds closest available position when collision detected
  - `findNearestFreePosition()` - Spiral search algorithm
  - Respects grid snapping
  - Maximum search radius of 500px

#### Connection Routing Optimization
- ✅ **Optimal routing direction selection** - Automatically chooses horizontal vs vertical routing
  - Analyzes existing connections to minimize crossings
  - Calculates path length for both options
  - Selects option with minimum score: (crossings × 100) + (length × 1)
  - `chooseAnchorSides()` now uses intelligent routing algorithm

- ✅ **Crossing detection** - Counts intersections between connection paths
  - `countPathCrossings()` - Analyzes all existing connections
  - `countPathIntersections()` - Detects intersections between orthogonal segments
  - `segmentsIntersect()` - Checks if two orthogonal segments intersect

- ✅ **Obstacle avoidance** - Automatically reroutes connections around nodes
  - `findObstaclesOnPath()` - Detects nodes blocking connection path
  - `rerouteAroundObstacles()` - Creates alternative path avoiding obstacles
  - Enhanced `buildOrthogonalPath()` with automatic obstacle detection

#### Drag & Drop Improvements
- ✅ **Enhanced drag validation** - Real-time collision checking during node dragging
  - Collision detection before position update
  - Automatic node pushing when conflicts detected
  - Visual feedback with CSS class `node-collision-warning`
  - Smooth snapping to grid

#### Technical Implementation
- ✅ **Connection routing algorithm** - Intelligent path selection
  - Horizontal vs vertical routing based on minimal crossings and path length
  - Weighted scoring: crossings (100×) vs length (1×)
  - Simple routing for existing connections (to avoid recursion)
  - Full routing with obstacle detection for new connections

- ✅ **Performance optimizations**
  - Caching of existing connection paths
  - Efficient collision detection using bounding boxes
  - Grid-based position snapping

## Version 2.3.1 - November 23, 2025

### 🎨 UI Improvements

#### Visual Designer
- ✅ **Removed unnecessary 'Grid snap locked' indicator** - Cleaner toolbar without redundant status indicator
  - Grid snap functionality remains active (always enabled)
  - Removed static span element from toolbar for better UI clarity

#### Connection Rendering Fixes
- ✅ **Fixed connection rendering to STOP marker** - Connections now properly render to workflow-end marker when nodes are moved
  - Manual marker positions are always respected
  - Automatic marker positions recalculated from model during drag operations

- ✅ **Fixed SVG overlay sizing** - SVG now covers full canvas, not just viewport
  - Connections visible even when canvas is scrolled beyond viewport
  - Proper viewBox attribute for correct scaling

#### Property Editor Enhancements
- ✅ **Improved ergonomics** - Added proper padding and margins to right sidebar sections
- ✅ **Enhanced value formatting** - Better validation and formatting for various data types
- ✅ **Syntax highlighting** - Added Prism.js for JSON, CODE, and EXPRESSION fields
- ✅ **Enhanced tooltips** - Better visual feedback and information display

#### Session Information
- ✅ **Restored session info display** - Session information and token usage now displayed in Execution section
- ✅ **Per-tab session data** - Session data stored per-tab and displayed in run history

## Version 2.2.1 - October 11, 2025

### 📚 Documentation Update - XPath Examples Fixed

**All built-in IDE examples now use correct XPath syntax for text extraction.**

#### Changes
- ✅ **app.js** - Updated 5 example functions:
  - Example 1: HTTP + XPath (`//title/text()`, `//h1/text()`)
  - Example 2: Quotes Scraping (`//span[@class='text']/text()`, `//small[@class='author']/text()`)
  - Example 3: JSON API (`//name/text()`, `//email/text()`, `//address/city/text()`)
  - Example 4: Wikipedia (`//h1[@id='firstHeading']/text()`, `string(//p[1])`)
  - Example 5: Data Pipeline (`//name/text()`, `//email/text()`, `//item[1]/title/text()`)

- ✅ **webapp/examples/** - Updated 3 XML example files:
  - `01_simple_api.xml` - `//slideshow/author/text()`
  - `02_quotes_scraping.xml` - `/text()` for quotes and authors
  - `03_wikipedia.xml` - `/text()` for title, `string()` for intro

#### Impact
- XPath now returns **text only**, not XML elements with tags
- Example: `//h1/text()` returns `"Herman Melville - Moby-Dick"` instead of `"<h1>Herman Melville - Moby-Dick</h1>"`
- Users can now safely copy and run examples from IDE
- All examples verified by `IDEExamplesIntegrationTest` (3/3 PASS)

#### Related Documentation
- See `docs/web/XPATH_BEST_PRACTICES.md` for complete XPath text extraction guide
- Integration tests: `webharvest-core/.../IDEExamplesIntegrationTest.java`

## Version 2.2.0 - October 2025

### 🎉 Initial Release - Web-Based IDE

Complete redesign of Web-Harvest IDE from Swing desktop application to modern web-based interface.

### Features

#### Core Architecture
- ✅ **Embedded Jetty 11** - HTTP server embedded in Java application
- ✅ **Single JAR Distribution** - `java -jar webharvest-ide-2.2.0.jar`
- ✅ **Auto-Port Selection** - Finds available port (8080-8179)
- ✅ **Auto-Browser Launch** - Opens default browser automatically

#### User Interface
- ✅ **Monaco Editor** - Professional XML code editor (VS Code engine)
- ✅ **Multi-Tab Interface** - Work with multiple configurations
- ✅ **Syntax Highlighting** - XML syntax highlighting and validation
- ✅ **Auto-Completion** - Code suggestions (planned)
- ✅ **Line Numbers & Minimap** - Professional editor features

#### Execution & Results
- ✅ **Real-Time Execution** - Live progress and status updates
- ✅ **WebSocket Streaming** - Real-time logs and results
- ✅ **Three-Panel Results**:
  - Output tab - Main execution result
  - Variables tab - All extracted variables
  - Log tab - Color-coded execution logs
- ✅ **Progress Indicator** - Visual execution progress
- ✅ **Execution Controls** - Run, Pause, Stop (pause/stop planned)

#### Pre-loaded Examples
1. **HTTP + XPath** - Simple web scraping with XPath
2. **Quotes Scraping** - Scraping quotes.toscrape.com
3. **JSON API** - Fetching and processing JSON data
4. **Wikipedia** - Extracting Wikipedia article data
5. **Data Pipeline** - Complex multi-step processing

### Technical Improvements

#### Backend (webharvest-ide)

**WebHarvestServer.java**
- Jetty embedded server initialization
- Single `ServletContextHandler` for all endpoints
- Static file serving from JAR resources
- Proper resource loading from `/webapp` in JAR

**API Servlets**
- `ConfigServlet` - Configuration validation endpoint
- `ExecutionServlet` - Execution control (start, pause, resume, stop)
- `ExecutionWebSocketServlet` - Real-time updates endpoint

**Execution Management**
- `ExecutionManager` - Singleton execution orchestrator
- Thread pool (5 concurrent executions)
- WebSocket session management
- Variable extraction with scope fix integration

**Integration Tests**
- `ExecutionManagerWebSocketTest` - 4 professional tests
- Real Jetty WebSocket (not mocks!)
- Tests: variable extraction, logs, WebSocket, pipeline
- **100% pass rate** (4/4 tests)

#### Frontend (webapp/)

**app.js**
- Tab management (add, close, switch)
- Monaco Editor initialization with XML language
- Configuration validation before execution
- WebSocket connection management
- Synchronous WebSocket connection (`connectWebSocketSync`)
- Results display (output, variables, logs)
- Error handling and UI feedback

**Styling**
- Based on `docs/web/css/style.css`
- IDE-specific styles in `ide.css`
- Code highlighting with `prism.css`
- Web-Harvest brand colors and logo
- Professional code windows

### Critical Bug Fixes

#### ConfigPlugin Scope Fix (webharvest-core)

**Issue:** Variables created by `<def>` were executed in nested scopes created by `BodyProcessor.executeWithinNewContext()`. When the config completed, the `finally` block in `ScraperContext.executeWithinNewContext()` cleaned up all variables from nested scopes, making them inaccessible.

**Root Cause:**
```java
// ScraperContext.java lines 177-179
finally {
    for (String varName : variablesNamesStack.pop()) {
        removeVarFromCRT(varName);  // Variables DELETED here!
    }
}
```

**Solution:**
Modified `ConfigPlugin.executeBody()` to execute children directly in current scope instead of using `super.executeBody()` which creates nested scope:

```java
// Before:
protected Variable executeBody(DynamicScopeContext context) {
    return super.executeBody(context); // Creates nested scope!
}

// After:
protected Variable executeBody(DynamicScopeContext context) {
    // Execute children DIRECTLY in current scope
    for (IElementDef def : getElementDef().getOperationDefs()) {
        Processor processor = ProcessorFactory.INSTANCE.createProcessor(context, def);
        processor.run(context); // Variables stay in ROOT scope!
    }
}
```

**Impact:**
- ✅ All `<def>` variables now persist in root scope
- ✅ IDE can extract and display all variables
- ✅ No regression - all 2556 core tests pass
- ✅ Discovered by professional integration tests

#### WebSocket Timing Fix

**Issue:** Frontend submitted execution request before WebSocket was connected, causing backend to have no session to send updates to.

**Solution:**
1. Frontend requests execution ID first
2. Frontend connects WebSocket synchronously (`connectWebSocketSync` returns Promise)
3. Frontend submits execution only after WebSocket is connected
4. Backend has small delay (100ms) to ensure WebSocket is ready

#### ScraperExecutionEndEvent Enhancement (webharvest-core)

**Issue:** Event didn't include context, so listeners couldn't extract variables.

**Solution:** Added `DynamicScopeContext` to event constructor and posted event INSIDE try block (before any cleanup).

```java
// Before:
eventBus.post(new ScraperExecutionEndEvent(this, executionTime));

// After:
eventBus.post(new ScraperExecutionEndEvent(this, executionTime, context));
```

### Known Issues & Limitations

#### Maven Shade Plugin Performance
- Shade plugin can be slow (30-90 seconds) with many dependencies
- Caused by duplicate handling and service file merging
- **Workaround:** Optimized configuration to skip transformers

#### Pause/Resume Not Implemented
- UI buttons present but functionality not yet implemented
- Requires integration with Web-Harvest execution threading model

#### Example Configurations Load Timing
- Fixed by reordering `init()` - `loadExampleTabs()` before `initMonaco()`
- Ensures tabs are populated before editor tries to load content

### Dependencies

#### New Dependencies
- `org.eclipse.jetty:jetty-server:11.0.18`
- `org.eclipse.jetty:jetty-servlet:11.0.18`
- `org.eclipse.jetty.websocket:websocket-jetty-server:11.0.18`
- `org.eclipse.jetty.websocket:websocket-jetty-api:11.0.18`
- `org.eclipse.jetty.websocket:websocket-jetty-client:11.0.18` (test scope)
- `com.google.code.gson:gson:2.10.1`
- `org.testng:testng:7.9.0` (test scope)

#### Removed Dependencies
- All Swing-related libraries (old GUI removed)
- JGoodies, SwingX, and other desktop UI libraries

### Migration from Old IDE

The old Swing-based IDE has been completely removed:
- ✅ All Java GUI code removed from `src/main/java/org/webharvest/gui/`
- ✅ Bounce library removed
- ✅ Old test files removed
- ✅ CommandLine.java removed (replaced by WebHarvestServer.java)

### File Organization

```
webharvest-ide/
├── IDE_GUIDE.md                        # User guide
├── CHANGELOG_IDE.md                    # This file
├── src/main/java/
│   └── org/webharvest/server/          # NEW - All backend code
└── src/main/resources/webapp/          # NEW - All frontend code
```

### Testing Strategy

Professional integration tests using real Jetty WebSocket client:
- No mocks - tests use actual WebSocket connections
- Tests cover entire flow: config → execution → variables → UI
- Each test verifies: completion status, logs, variable extraction
- Tests helped discover the ConfigPlugin scope bug!

### Performance

- **Startup Time:** ~600ms
- **Small Config Execution:** <100ms
- **HTTP Request Config:** ~500-2000ms (network dependent)
- **WebSocket Latency:** <10ms
- **Variable Extraction:** <5ms for typical configs

### Browser Compatibility

Tested and working on:
- ✅ Chrome/Chromium 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

Requires JavaScript enabled and WebSocket support (all modern browsers).

---

## Future Enhancements

### Planned Features
- [ ] Pause/Resume execution
- [ ] File upload/download
- [ ] Configuration persistence
- [ ] Execution history
- [ ] Variable search/filter
- [ ] Export results (JSON, CSV, XML)
- [ ] Syntax validation while typing
- [ ] Auto-completion for Web-Harvest elements
- [ ] Configuration templates library
- [ ] Dark mode toggle

### Under Consideration
- [ ] Multi-user support (session management)
- [ ] Execution scheduling
- [ ] Result comparison (diff view)
- [ ] Performance profiling
- [ ] Plugin marketplace

---

**Maintained by:** Web-Harvest Team  
**Project:** https://sourceforge.net/projects/web-harvest/  
**License:** BSD 3-Clause

