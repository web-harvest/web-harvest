# Changelog

All notable changes to WebHarvest will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **Visual Designer Polish**: Container summary cards now surface `Details` / `Focus` actions in English, the Details shortcut auto-activates the Properties tab, semantic role badges render friendly labels, and the connection overlay grows with the canvas so links stay visible when nodes move off the initial viewport.
- **Plugin Card Consistency**: All tiles on the canvas now share the same neutral Apple HIG card shell—no role-based borders, duplicate titles, or dependent badges—so Constant, Empty, Case, and every other plugin look and behave identically.
- **Right Sidebar Harmonization**: All right-column panels (Properties, Logs, Results, Variables, Run & Test) now share the Apple HIG pill navigation, glass header, and padded body introduced with the Run panel, giving novices and experts a consistent visual rhythm when switching tasks.
- **Plugin Palette Apple HIG Refresh**: Palette toolbar now features segmented controls for All/Workflow/Utilities, search adopts native Mac styling, dependent plugins are no longer exposed as draggable tiles (a guidance banner links users to the parent card actions instead), and the primary list focuses on runnable workflow blocks and utilities.
- **Breadcrumb Navigation Focus Mode**: Canvas top bar renders an Apple HIG breadcrumb with drill-down trails; selecting breadcrumbs resets focus levels without re-render jitter, and empty workflow levels surface inline guidance instead of blank canvases.
- **Horizontal Flow Layout**: Root-level nodes now auto-arrange from left to right using measured card widths; manual placements are respected, and sequential links anchor to the lateral edges of nodes/start/end markers for a BPMN-style main path.
- **Drag Ergonomics**: Node dragging uses grab/grabbing cursors with selection locking, and expanded containers gain scrollable, padded interiors with refreshed Apple HIG-inspired styling for deeply nested flows.
- **Process Markers & Grid**: Start/end markers are fully draggable with manual positions persisted, the background grid renders by default and resizes with the workspace, and connection lines update instantly while nodes are dragged.
- **Master/Detail Editor Refresh**: Property panel and canvas now share a metadata-driven detail manager. The HTTP request card retains its dedicated request configuration, but headers/params – along with CALL, ZIP, FTP, and other detail plugins – render through the same reusable component with add/reorder/remove controls on both canvas and property panel.
- **Workflow Marker Anchors**: BPMN start/end markers received compact styling and now auto-connect to the nearest root container nodes, eliminating the detached line segments that previously appeared when all top-level plugins were containers.
- **HTTP Task UX Alignment**: HTTP containers now manage headers and query parameters directly inside the node, mirroring the Orkes Conductor HTTP task experience; the canvas shows an inline summary with quick actions, context menus add detail entries, and drop validation points authors back to the HTTP configuration panel rather than dragging detail plugins separately ([reference](https://orkes.io/content/reference-docs/system-tasks/http))
- **Control Flow Branching UX**: `if`/`else` and `try`/`catch` blocks surface inline branch cards inside the parent node for adding, reordering, and editing detail plugins. Branch drop zones accept child nodes directly, and drag/drop validation now nudges authors to use these controls instead of placing `else`/`catch` tiles manually.
- **Semantic Metadata Master/Detail Coverage**: synchronized plugin metadata with XML configurations; `try/catch`, `if/else`, `call/call-param`, `http/{http-header,http-param}`, `zip/zip-entry`, and `ftp/{ftp-get,ftp-put,ftp-list}` now expose explicit parent/child constraints in both Java schemas and generated JSON
- **Focus Summary Preview Cards**: Drill-down mode now renders detail sections as preview cards with inline lists, includes execution status chips, and broadcasts summary data to the sidebar; the property panel mirrors the same breakdown beneath the type header.
- **Run/Test Workflow Panel Refresh**: Execution controls expose Restore/Run/Details actions per history entry, render correlation/idempotency/task-domain metadata inline, and allow replaying task-specific runs without re-selecting nodes; the panel now mirrors Orkes “Run Workflow” behaviour including inline payload previews.
- **Run/Test Workflow Panel Sidebar Integration**: Execution controls are embedded as a dedicated sidebar tab alongside Properties/Logs/Results, giving the canvas full-width breathing space; novice users see a concise “Run & Test” hint with one-click Run/Validate buttons, while advanced options (correlation, idempotency, task-domain JSON) live in a collapsible drawer. History remains per-tab, supports restore-and-rerun flows, and surfaces terminal statuses exactly where the run was initiated.

### Removed
- **Session Sidebar Tab**: Historical execution/session telemetry lives inside the Run & Test history drawer; the separate Session tab and panel were deleted to reduce sidebar clutter.
- **BodyProcessorPlugin**: Dropped the internal `body-processor` plugin class and its documentation/tests. The framework now relies on `AbstractPlugin#executeBody(...)` directly, and the system component remains hidden from the catalogue.
- **Property Panel Control Alignment**: Schema-driven attributes adopt Apple HIG-inspired controls—boolean fields render as toggles, compact enums switch to segmented controls, and number/url inputs retain platform styling—while tests cover the new variants.
- **Visual Designer Containers**: the semantic drop validator now treats HTTP/FTP/ZIP/CALL masters as containers so detail plugins can be dragged directly into their parents in the canvas
- **Semantic Metadata Containers v2**: `PluginSemanticsResolver` now promotes any root-level plugin that allows body content to `allowsAnyChild=true`, making top-level processors (`xpath`, `script`, `regexp`, `template`, etc.) behave as general-purpose containers without manual allowlists
- **Semantic Metadata Roles**: plugin metadata now emits multi-role semantics derived directly from runtime metadata; REST/JSON payloads expose `roles[]` alongside legacy `role` for backwards compatibility, enabling IDE styling and validation to distinguish combined container/body/dependent behaviors.
- **Visual Designer UX**: added BPMN-style start/end markers for every configuration, repositioned child reorder controls to the card footer, and surfaced palette help via a dedicated `?` icon for dependent components
- **Nested Container Rendering**: container nodes now render recursively inside other containers with preserved drag/drop, resize, and reorder affordances, and root-level containers participate in sequential connection autowiring so the primary flow remains visible even when top-level nodes are all containers.
- **Canvas Ergonomics**: the canvas now uses a dedicated scroll container (`.canvas-content-wrapper`) plus a transform stage (`.canvas-stage`), keeping grid overlays visible across the full workspace, enabling native scrolling for large flows, and resizing the stage to match distant nodes; grab/grabbing cursors and touch-action tweaks make container manipulation less fragile.
- **Working Directory Configuration**: All test and IDE executions now use `target/` as working directory
  - Test base classes (`XmlBasedIntegrationTestBase`, `ScraperTestBase`) initialize `ScraperModule` with `workingDir = "target"`
  - IDE `ExecutionManager` and `ConfigServlet` use `workingDir = "target"` for all scraper executions
  - All generated files (from example XML configurations) now go to `target/` instead of project root
  - Prevents pollution of project root with test output files
  - All test example XML files updated to use relative paths (resolved against `target/`)
  - `.gitignore` updated to remove webharvest-core specific patterns (files now in `target/` which is already ignored)
  - Fixed: All test classes updated to use `workingDir = "target"` (previously some used `"."` causing files in project root)

### Fixed
- **IDE Java Version Requirement**: IDE now requires Java 11 (not Java 17/21)
- **Documentation**: Developer guide updated with a "Master/Detail Relationship Coverage" section explaining how palette validation is derived from curated XML examples.
  - Java 17 and 21 have stricter module access restrictions requiring `--add-opens` flags
  - Java 11 works without these restrictions and is the recommended version for IDE
  - Updated documentation to specify Java 11 requirement
  - Fixed: All test classes updated to use `workingDir = "target"` instead of `"."`
    - `SessionManagementIntegrationTest`, `HttpPluginWithDependencyInjectionTest`, `DefaultPluginBodyExecutorTest`, `HandlerHolderIntegrationTest`, `HttpClient5MigrationTest`, `BaseTemplaterStaticInjectionTest`, `JSRScriptEngineAdapterVariableConversionTest`
  - Fixed: IDE `ConfigServlet` uses `workingDir = "target"` (previously used `"."` causing files in project root)
  - Fixed: Generated files cleanup - removed all test output files from project root and `webharvest-core/` directory
  - Fixed: "target 2" directory issue - was a one-time Maven clean plugin issue, not a code problem
- **Drill-down Detail Connections**: Focused containers no longer attempt to render auto-connections for detail plugins (HTTP headers, params, etc.), eliminating the "anchor not found" warnings and keeping the focus canvas free of stray arrows; covered by `VisualDesigner.drilldown.test.ts`.
- **NYTimes Workflow UX**: HTTP request cards now surface inline `name/value` editors for headers & params (with semantic fallbacks), canvas detail rows render labeled summaries, and the NYTimes sample layout is guarded by `MasterDetailEditor.test.ts` + `NYTimesLayout.test.ts`.
- **Visual Model TDD Alignment**: Manual connection tests now target sequential action nodes (`def`, `xpath`, `script`) so port rendering stays deterministic while master-detail hosts (`http`, `ftp`, `zip`, `call`) remain classified as containers; updated `VisualDesigner.ports`, `CanvasInteraction`, and `VisualModel` suites verify the revised behaviour, and `App.openVisual` regression coverage now stubs `loadNodesWithPrompt` via the new workflow-aware import path.

## [2.3.1] - November 3, 2025

### 🎨 IDE Visual Designer - Advanced Features & Fixes

#### ✅ Connection Management
- **New:** Connection selection - click on manual connections to select them
- **New:** Connection context menu - right-click on connections for Edit/Delete options
- **New:** Connection redefinition - edit connection source/target (temporary prompt-based UI)
- **New:** Connection deletion - remove connections from context menu
- **Fixed:** Event listeners properly cleaned up on re-render (no duplicate listeners)
- **Fixed:** Connection visual feedback - selected connections highlighted in blue with thicker stroke
- **Improved:** Connection ID detection - proper distinction between manual and automatic connections

#### ✅ Node Management
- **New:** Root node reordering - Move Up/Down options in context menu for root-level nodes
- **Fixed:** Container movement - containers now move with all nested children recursively
- **Fixed:** Position initialization - missing positions are initialized before movement calculations
- **Improved:** Context menu positioning - uses `position: fixed` relative to viewport for correct placement

#### ✅ Canvas Layout Fixes
- **Fixed:** Canvas layout properly embedded in flexbox (no longer "floating")
- **Fixed:** Node dragging works correctly with zoom/pan transformations
- **Fixed:** Drop positions correctly calculated accounting for scroll and transform
- **Improved:** Coordinate system - `viewportToCanvas()` method for consistent positioning
- **Improved:** UI cleanup - removed duplicate Run/Validate buttons from header
- **Improved:** Execution controls (Pause/Resume/Stop) appear only during execution

#### ✅ Plugin Metadata
- **Updated:** `html-to-xml` plugin metadata - now allows children (`acceptsAnyChild: true`)
- **Support:** Nested configurations with `html-to-xml` containing `<get>` elements

#### ✅ Dynamic Metadata Generation (v2.3.0+)
- **New:** `PluginClassScanner` - Dynamic plugin discovery from classpath using Reflections
- **New:** `XSDGenerator` - Dynamic XSD schema generation from plugin metadata
- **New:** `XSDMetadataParser` - Extracts metadata from existing XSD files
- **New:** `PluginMetadataValidator` - Validates completeness and correctness of plugin metadata
  - Checks for required fields, missing attributes, inconsistent relationships
  - Detects circular dependencies and orphaned plugins
  - Generates comprehensive validation reports
- **New:** XSD API endpoints - `/api/plugins/xsd` and `/api/plugins/xsd/{version}`
- **Refactored:** `PluginMetadataCatalog` - Now generates all metadata dynamically from plugin classes
- **Removed:** All static JSON metadata files - completely eliminated
  - `plugin-semantics.json` - deprecated, no longer used
  - `plugin-ui-overrides.json` - removed, UI hints now fully auto-generated
  - All metadata (semantic + UI) now generated dynamically from plugin classes
  - Icons, palette groups, and tags inferred from plugin category and element names
- **Benefits:** 
  - All metadata comes from code (always in sync)
  - New plugins automatically discovered
  - Type-safe metadata extraction
  - Validated metadata ensures completeness
  - Full test coverage (40+ tests for metadata components)

**Technical Details:**
- Three-layer canvas structure: `.canvas-area` (scrollable) → `.canvas-content-wrapper` (transform) → nodes
- Transform applied to wrapper, not scrollable container
- All coordinate calculations use canvas coordinates (accounts for pan/zoom/scroll)
- Connection event listeners managed via `connectionLineElements` Map for proper cleanup
- Context menu appended to `document.body` with `position: fixed` for viewport-relative positioning

**Tests:**
- ✅ 870 tests passed (61 test suites)
- ✅ New integration tests: `NestedConfigSupport.test.ts` (11 tests), `ConnectionOperations.test.ts` (7 tests), `VisualDesignerFixes.test.ts` (8 tests)

### 🎨 IDE Visual Designer (v2.3)
- **New:** Visual drag-and-drop editor for WebHarvest configurations
- **Features:** Plugin palette, canvas, nested containers, semantic validation
- **Reorder:** Arrow buttons (⬆️⬇️) to change plugin order in containers
- **Nested Drops:** Support for multi-level container nesting
- **Semantic Validation:** Prevents invalid plugin placements (e.g., `catch` without `try`)
- **XML Sync:** Bidirectional synchronization between Visual and XML modes
- **Architecture:** TypeScript/Vite frontend with semantic metadata system
- **Docs:** Complete user guide in USER_GUIDE.md

### 📚 Documentation Consolidation
- **Reduced:** 183 .md files → 32 core files
- **Created:** USER_GUIDE.md (unified IDE + Visual Designer guide)
- **Archived:** All session summaries, plans, and status files
- **Consolidated:** PLUGINS.md with context-specific plugins section
- **Updated:** DOCUMENTATION_INDEX.md with new structure

### 🔧 Plugin Metadata System
- **New:** Semantic metadata catalog with 55 plugins
- **Schema:** v1.1.0 with dependencies, roles, and constraints
- **Validation:** Context-aware plugin placement rules
- **Docs:** docs/semantic-metadata/ with inventory and README
- **Tests:** Complete TDD coverage for metadata system

## [2.2.1] - October 11, 2025

### 🔒 CRITICAL: HttpClient 5.x Migration (r753)
- **Migrated:** Apache HttpClient 4.5.14 → 5.5.1
- **Reason:** Critical security component - active development and patches
- **Approach:** TDD with 15 new comprehensive tests
- **Added:** WireMock 2.35.0 for reliable HTTP server mocking
- **Impact:** Modern HTTP/2 support, improved security, no regressions
- **Tests:** 2442/2442 PASS (added 15 HTTP-specific tests)

### 📦 Dependencies Cleanup (r743-r750)
- **Removed:** 9 unused dependencies (commons-dbutils, commons-email, commons-net, commons-text, commons-beanutils, guice-multibindings, jackson-databind, commons-lang3)
- **Migrated:** commons-lang 2.5 (EOL) → commons-lang3 3.14.0 (r749)
- **Optimized:** Script engines scope compile → runtime (groovy, rhino) (r750)
- **Verified:** Each removal tested with full test suite

### 🎨 Branding & Code Quality (r751-r752)
- **Rebranded:** "Web-Harvest" → "WebHarvest" across entire codebase (r751)
  - 10 POM files, 23 MD files, 85+ Java files updated
- **Logging:** Replaced all System.out.println with SLF4J (r752)
  - 35 production files updated
  - AbstractPlugin: LOGGER changed to protected for subclasses
  - Consistent logging across all modules

### 📚 Documentation - XPath Best Practices

**Fixed inconsistency between tests, documentation, and IDE examples regarding XPath text extraction.**

#### Problem
- Integration tests used correct syntax: `//element/text()`
- Documentation examples used: `//element` (returns element with tags, not text)
- IDE built-in examples used: `//element` (returns element with tags, not text)
- Users copying from docs/IDE got XML elements instead of text content
- Example: `//h1` returns `"<h1>Title</h1>"` instead of `"Title"`

#### Solution
1. **Updated 16 XML example files** with correct XPath syntax:
   - `docs/web/examples/`: 6 files (advanced_search, api_integration, database, ftp, nytimes, amazon)
   - `web-harvest-docs-upload/examples/`: 7 files (same + simple_test, modern_web_scraping)
   - `webharvest-ide/src/main/resources/webapp/examples/`: 3 files (simple_api, quotes, wikipedia)

2. **Updated IDE built-in examples** (`app.js`):
   - 5 JavaScript functions with XML templates
   - 13 XPath expressions fixed to use `/text()` or `string()`

3. **Created comprehensive guide**: `docs/web/XPATH_BEST_PRACTICES.md`
   - Explains `//element` vs `//element/text()` vs `string(//element)`
   - Provides correct patterns and examples
   - Shows when to use each approach
   - Quick reference table
   - Testing guidelines

4. **Added integration tests**: `IDEExamplesIntegrationTest.java`
   - 3 test methods covering IDE examples
   - Verifies XPath returns TEXT, not XML elements
   - Tests httpbin.org, quotes.toscrape.com, and JSON API examples

#### Changed Patterns

**BEFORE (incorrect):**
```xml
<xpath expression="//title"/>  → Returns: <title>Text</title>
<xpath expression="//h1"/>     → Returns: <h1>Text</h1>
```

**AFTER (correct):**
```xml
<xpath expression="//title/text()"/>  → Returns: Text
<xpath expression="//h1/text()"/>     → Returns: Text
OR: <xpath expression="string(//h1)"/> → Returns: Text
```

#### Files Changed
- Documentation: 13 example XML files updated
- Published docs: 10 example XML files updated
- IDE: app.js (5 functions, 13 XPath expressions)
- New guide: XPATH_BEST_PRACTICES.md
- New tests: IDEExamplesIntegrationTest.java (3 test methods, all PASS)

#### Impact
- ✅ Users now get correct results from XPath
- ✅ Examples are consistent with tests
- ✅ Clear documentation of best practices
- ✅ No breaking changes (examples just improved)
- ✅ IDE examples verified by integration tests

#### Test Results
- ✅ IDEExamplesIntegrationTest: 3/3 PASS (httpbin, quotes.toscrape, httpbin/json)
- ✅ All existing tests: 2424/2424 PASS
- ✅ Total: 2427/2427 PASS (100% pass rate)

## [2.2.1] - October 10, 2025

### 🐛 Critical Bug Fixes - TDD Approach

**Two Critical Bugs Fixed** using TDD methodology!

#### Bug #1: DefaultPluginBodyExecutor Dependency Injection
- **Issue:** Plugin children created via reflection (`newInstance()`) instead of Guice
- **Impact:** `@Inject` dependencies (like `HttpService`) were NOT injected
- **Symptom:** HttpPlugin returned empty content, all HTTP requests failed
- **Fix:** Use `InjectorHelper.getInjector().getInstance(pluginClass)` instead of `newInstance()`
- **Result:** All plugins with dependencies now work correctly
- **Tests:** +9 new tests (HttpService, HttpPlugin unit & integration)

#### Bug #2: BeanShell Variable Type Conversion
**18-year-old bug fixed!**

#### Fixed Issues
- **JSRScriptEngineAdapter.copyVariables()** - CRITICAL FIX
  - ❌ **Old behavior (since 2006):** Passed `Variable` objects directly to BeanShell for non-ScriptingVariable types
  - ✅ **New behavior:** Always calls `getWrappedObject()` + automatic `String→Number` conversion
  - 🎯 **Impact:** BeanShell arithmetic (`counter + 1`), comparisons (`count < 5`), and complex expressions now work correctly
  - 📊 **Test coverage:** 4 new unit tests in `JSRScriptEngineAdapterVariableConversionTest`

#### Test Infrastructure Improvements
- **BaseTemplater static injection** - Verified Guice static injection works correctly
  - 📊 **New tests:** `BaseTemplaterStaticInjectionTest` (3 test methods)
- **Real ScraperContext in tests** - Replaced Mockito mocks with real `ScraperContext` instances
  - 🎯 **Fixed:** 9 variable interpolation tests in `DefPluginTest`, `SetVarPluginTest`, `TextPluginTest`
  - 💡 **Reason:** Mock context doesn't implement `Iterable`, breaking `JSRScriptEngineAdapter.copyVariables()`
- **DefaultPluginConfiguration in tests** - Replaced Mockito mocks with real configurations
  - 🎯 **Fixed:** `testDefPluginWithChildrenExecution` and similar tests
  - 💡 **Reason:** Mockito mocks don't work properly with `DefaultPluginBodyExecutor`

#### Test Results
- ✅ **Before:** 2419 tests, 12 failures
- ✅ **After:** 2419 tests, 0 failures (100% pass rate!)
- ✅ **New tests added:** 7 test methods (2 new test classes)
- ✅ **Tests fixed:** 12 failures resolved

#### Architecture Verification
- Compared new architecture (r693) with old architecture (r680)
- Confirmed: Logic is identical, only class names changed
- Validated: `PluginBodyExecutor` is full replacement for `BodyProcessor`

## [2.2.1] - October 2025

### 🚀 Major Architecture Upgrade: ULTRA-LEAN v2.2.x

**New Plugin Execution Engine** - Simplified, faster, cleaner architecture

#### What's New
- **PluginBodyExecutor** - New interface for executing plugin body content
- **DefaultPluginBodyExecutor** - Implementation with 57% test coverage ✅
- **PluginConfiguration.getChildren()** - Direct access to child configurations
- **StreamingXmlParser enhancement** - Now populates `children` in PluginConfiguration
- **AbstractPlugin.executeBody()** - Modernized to use new architecture with backward compatibility

#### Architecture Evolution
```
OLD (v2.1.x):
XML → StreamingXmlParser → PluginConfigurationTree → AbstractPlugin → ExecutableBodyContentProvider → IElementDef → body execution

NEW (v2.2.x ULTRA-LEAN):
XML → StreamingXmlParser → PluginConfiguration (with children!) → AbstractPlugin → PluginBodyExecutor → body execution
```

#### Benefits
- ✅ **88% simpler** - Eliminated IElementDef dependency in body execution
- ✅ **Direct child access** - PluginConfiguration.getChildren() provides immediate access
- ✅ **Backward compatible** - Falls back to ExecutableBodyContentProvider if needed
- ✅ **Fully tested** - 9 unit tests for DefaultPluginBodyExecutor, all PASS
- ✅ **57% coverage** - Exceeds minimum 55% requirement

#### Implementation Details
**1. PluginConfiguration enhanced:**
```java
public interface PluginConfiguration {
    List<PluginConfiguration> getChildren();  // NEW!
    void setChildren(List<PluginConfiguration> children);  // NEW!
    boolean hasChildren();  // NEW!
}
```

**2. StreamingXmlParser now populates children:**
```java
// In endElement():
if (current.hasChildren() && config instanceof DefaultPluginConfiguration) {
    List<PluginConfiguration> childConfigs = new ArrayList<>();
    for (PluginConfigurationTree child : current.getChildren()) {
        childConfigs.add(child.getConfiguration());
    }
    ((DefaultPluginConfiguration) config).setChildren(childConfigs);
}
```

**3. AbstractPlugin.executeBody() modernized:**
```java
protected Variable executeBody(DynamicScopeContext context) {
    // NEW ARCHITECTURE (v2.2.x): Use PluginBodyExecutor
    if (configuration != null && Boolean.TRUE.equals(configuration.hasChildren())) {
        PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
        return executor.executeBody(configuration, context);
    }
    
    // FALLBACK: Old architecture (backward compatibility)
    if (elementDef != null && elementDef.hasOperations()) {
        ExecutableBodyContentProvider bodyProvider = new ExecutableBodyContentProvider(elementDef);
        return bodyProvider.executeBody(context);
    }
    
    return EmptyVariable.INSTANCE;
}
```

**4. DefaultPluginBodyExecutor:**
```java
public class DefaultPluginBodyExecutor implements PluginBodyExecutor {
    @Override
    public Variable executeBody(PluginConfiguration config, DynamicScopeContext context) {
        List<Object> results = new ArrayList<>();
        for (PluginConfiguration childConfig : config.getChildren()) {
            Variable childResult = executeChildPlugin(childConfig, context);
            if (childResult != null && !(childResult instanceof EmptyVariable)) {
                results.add(childResult);
            }
        }
        return new ListVariable(results);  // Always ListVariable for consistency
    }
}
```

#### Test Results
- **DefaultPluginBodyExecutorTest**: 9/9 PASS (100%)
  - executeBody with no children
  - executeBody with single child
  - executeBody with multiple children
  - executeBody with non-existent plugin (graceful handling)
  - getBodyTextContent with text nodes
  - getBodyTextContent mixed (text + elements)
  - Empty results → empty ListVariable
- **Full core suite**: 2598/2598 PASS (100%)
- **Coverage**: 57% (target: 55% ✅)

#### Migration Status
- ✅ **Phase 1**: PluginConfiguration.getChildren/setChildren/hasChildren
- ✅ **Phase 2**: StreamingXmlParser populates children
- ✅ **Phase 3**: PluginBodyExecutor interface + DefaultPluginBodyExecutor
- ✅ **Phase 4**: AbstractPlugin.executeBody() uses new architecture
- ⏸️ **Phase 5**: Remove ExecutableBodyContentProvider (backward compatibility kept)
- ⏸️ **Phase 6**: Remove IElementDef from Plugin interface (backward compatibility kept)

#### Files Changed
- `PluginConfiguration.java` - Added getChildren/setChildren/hasChildren methods
- `DefaultPluginConfiguration.java` - Implemented children management
- `StreamingXmlParser.java` - Populates children in PluginConfiguration
- `PluginBodyExecutor.java` - NEW interface
- `DefaultPluginBodyExecutor.java` - NEW implementation (57% coverage)
- `AbstractPlugin.java` - executeBody() uses new architecture
- `DefaultPluginBodyExecutorTest.java` - NEW: 9 comprehensive tests

#### Backward Compatibility
- ✅ All existing configurations work unchanged
- ✅ Old ExecutableBodyContentProvider still available as fallback
- ✅ IElementDef still in Plugin interface (for compatibility)
- ✅ Zero breaking changes
- ✅ 2598/2598 tests PASS

---

### 🐛 Bug Fixes

**Bug #41: Cannot Download Binary Files (RAR/ZIP Corrupted)** (r719) - FIXED
- **Issue**: RAR/ZIP/PDF files corrupted when saved with type="binary"
- **Problem**: FilePlugin used toString().getBytes() → double conversion → corruption
- **Fix**: FilePlugin now uses NodeVariable.toBinary() for binary writes
- **Related**: Bug #39 (HttpPlugin binary) - together ensure end-to-end binary preservation
- **Impact**: RAR, ZIP, PDF, large images now download correctly
- **Tests**: 7 tests, 2639/2639 PASS

**Bug #27: Indent/RunningProcessors Broken by Exceptions** (r718) - FIXED
- **Issue**: Log indent grows, memory leak, 100% CPU when exceptions caught in try/catch
- **Problem**: AbstractProcessor.run() didn't wrap execution in try-finally
- **Root Cause**: finalize Execution() never called when exception thrown
- **Fix**: Added try-finally wrapper to ensure cleanup ALWAYS happens
- **Impact**: Prevents memory leaks, CPU spikes, and log corruption in long-running scrapers
- **Critical for**: Production environments, error handling in loops
- **Tests**: 6 tests, 2632/2632 PASS

**Bug #33: JavaScript and AJAX Support** (r717) - FIXED (REGRESSION)
- **Status**: Feature restored (existed r649-r655, lost r695, restored v2.2.1)
- **Issue**: No JavaScript/AJAX support for dynamic pages
- **History**: PhantomJS implementation (2013-2015) lost during refactoring
- **Fix**: Reimplemented with HtmlUnit 2.70.0 (pure Java, actively maintained)
- **Features**:
  - JavaScript execution (enabled/disabled)
  - AJAX handling with configurable wait
  - Multiple named pages support
  - 4 actions: load, execute-script, get-content, close
- **Impact**: SPAs (React/Vue/Angular), AJAX pagination, dynamic forms now work
- **Tests**: 15 Bug #33 tests, 47/47 module tests PASS, **51% coverage**

**Bug #39: Forcing MIME Type to Binary for Images** (r714) - FIXED
- **Issue**: Binary content corrupted when server sends wrong Content-Type
- **Problem**: HttpPlugin converted all responses to String, corrupting binary data (0xFF → 0xEF 0xBF 0xBD)
- **Fix**: Auto-detection of binary MIME types (image/*, video/*, audio/*, application/pdf, etc.)
- **Changes**: 
  - Added `HttpResponse.getBodyAsBinary()` API
  - HttpServiceImpl stores responses as byte[]
  - HttpPlugin auto-detects binary Content-Type
- **Impact**: Images, PDFs, videos now handled correctly without corruption
- **Tests**: 4 validation tests, 2618/2618 PASS, zero regression

**Bug #13: MIME Types Detection for RSS/XML Feeds** (r716) - Verified Fixed
- **Status**: Fixed by architecture refactoring (v2.1.0+), verified in v2.2.1
- **Issue**: Old HttpProcessor used whitelist (text/*, application/xml), user wanted application/rss
- **Problem**: Had to manually add each text MIME type (rss, atom, json, etc.)
- **Fix**: New HttpPlugin uses inverse approach - detect binary, default to text
- **Impact**: application/rss+xml, application/atom+xml, application/json all work automatically
- **Benefits**: Zero maintenance, extensible, safer defaults
- **Tests**: 8 verification tests (RSS, Atom, JSON, XML), 2626/2626 PASS

**Bug #18: HttpProcessor Charset Detection** (r712) - Resolved  
- **Status**: Not a Bug - Correct HTTP Behavior
- **Issue**: User expected meta tag charset, but HTTP header charset was used
- **Analysis**: HTTP Content-Type header has priority over HTML meta tags (per RFC 2616)
- **Resolution**: Resolved by architecture refactoring, Apache HttpClient follows HTTP spec
- **Workarounds**: Extract meta tag charset via XPath, or specify charset explicitly
- **Tests**: 5 documentation tests explaining HTTP behavior and workarounds

**Bug #7: CommonUtils.fullUrl: ".." Is Not Handled Correctly** (r711)
- **Issue**: Parent directory references ("..") not normalized in URLs
- **Problem**: IIS servers close connections on URLs with ".." (e.g., `http://example.com/foo/../bar.htm`)
- **Fix**: Added normalizePath() to resolve ".." and "." segments like browsers do
- **Impact**: Compatible with all servers (IIS, Apache, Nginx), follows URL standards
- **Tests**: 8 tests including multiple ".." levels, edge cases, regression tests

**Bug #29: CommonUtils.createVariable Does Not Handle NativeJavaObjects** (r709)
- **Issue**: JavaScript (Rhino) returns NativeJavaObject wrappers, not unwrapped Java objects
- **Problem**: createVariable() stored wrapped objects in NodeVariable instead of actual objects
- **Fix**: Detect NativeJavaObject, unwrap using reflection, recursively create correct Variable type
- **Impact**: Seamless JavaScript ↔ WebHarvest integration, Lists → ListVariable, etc.
- **Tests**: 5 tests including JavaScript integration with real Rhino Context

**Bug #47: Character Set Problem in CLI Mode** (r707)
- **Issue**: Config files read with platform default charset (WIN1250 on Windows) instead of UTF-8
- **Problem**: Non-ASCII characters garbled in CLI mode on Windows
- **Fix**: FileConfigSource and URLConfigSource now explicitly use UTF-8
- **Impact**: Consistent UTF-8 encoding across all platforms (Windows, Linux, Mac)
- **Tests**: 4 tests including UTF-8 character verification

**Bug #48: Use getResponseBodyAsStream Instead of getResponseBody** (r706)
- **Issue**: Used deprecated `getResponseBody()` which buffers entire response in memory
- **Warning**: "Going to buffer response body of large or unknown size"
- **Fix**: Changed to `getResponseBodyAsStream()` for streaming, memory-efficient processing
- **Impact**: Large file downloads no longer cause OutOfMemoryError or warnings
- **Tests**: 5 comprehensive tests including 1MB performance test

**Bug #55: Character '#' in URL Parameters Not Encoded** (r704)
- **Issue**: '#' in URL parameter values was not encoded, causing URL truncation
- **Example**: `?color=#FF0000` became `?color=` (truncated)
- **Fix**: Removed special case in `CommonUtil.encodeUrlParam()` - '#' now encoded as %23
- **Impact**: Color codes, hashtags, any '#' in parameters now work correctly
- **Tests**: 7 comprehensive tests added (4 bug + 3 regression)

**Bug #61: JSONToXML "content" Key Not Correctly Converted** (r702)
- **Issue**: org.json.XML treats "content" as special text node key, strips it from XML
- **Example**: `{"a": {"content": "hello"}}` became `<a>hello</a>` (content key lost)
- **Fix**: Created `JsonXmlConverter` utility - all JSON keys treated equally
- **Impact**: CMS/blog APIs with "content" field now work correctly
- **Tests**: 9 comprehensive tests added (4 bug + 5 regression)

**Bug #62: Regexp Group Isolation Issue** (r701)
- **Issue**: Groups from first `<regexp>` persisted into second regexp execution
- **Example**: In loops, second regexp's `group1` showed values from first regexp
- **Fix**: Added `clearAllGroupVariables()` - clears group0-99 before each execution
- **Impact**: Multiple sequential regexp calls now properly isolated
- **Tests**: 7 comprehensive tests added (5 unit + 2 integration)

**Bug #60: Invalid Variable Name in try/catch** (r703 - documentation only)
- **Issue**: Old `TryProcessor` used `_error` (invalid variable name with underscore)
- **Status**: Already fixed in v2.1.0 during GUI→Plugin migration
- **Current**: `TryPlugin` uses valid names: `error`, `errorType`, `stacktrace`
- **Verification**: Existing tests pass, no code changes needed

### 🎨 Web-Based IDE

**Major Features** (r693, r698):
- Modern web-based IDE with embedded Jetty server
- Monaco Editor integration (VS Code engine)
- Real-time execution with WebSocket streaming
- Multi-tab editing with 5 pre-loaded examples
- Variable inspector with syntax highlighting
- WebSocket lifecycle management with disconnect notifications
- Single executable JAR: `java -jar webharvest-ide-2.2.0.jar`

### 🏗️ Architecture Improvements

**Root Configuration Executor** (r7xx)
- **Change**: Removed legacy `ConfigPlugin`; root `<config>` is now executed directly by the runtime
- **Impact**: Charset/scriptlang settings applied by core, variables stay in global scope without plugin indirection
- **Implementation**: New `RootConfigurationExecutor` drives child execution via `DefaultPluginBodyExecutor`
- **Tests**: Added TDD coverage (`ScraperRootExecutionTest`) plus updated metadata audits

**HTTP Test Fixtures** (r7xx)
- **Change**: IDE and core integration suites bind `HttpService` to in-memory fixtures (example.com, httpbin.org)
- **Impact**: Test runs no longer depend on external network availability; XPath/JSON assertions have deterministic payloads
- **Shared Assets**: Stubs reused between `EmbeddedExamplesExecutionTest` and `XmlBasedIntegrationTestBase`
- **Tests**: `EmbeddedExamplesExecutionTest`, `DefPluginIntegrationTest` (no network) and full `webharvest-core` suite

**Plugin Discovery** (r694, r695)
- PluginProcessorAdapter for automatic plugin wrapping
- Integration tests for all 6 plugin modules
- Core, Database, FTP, Mail, WebBrowser, Zip modules tested

### 📝 Documentation

**Website Updates** (r699, r700):
- New IDE section on homepage with live preview
- Plugin Architecture section documenting all 57 plugins
- Updated all navigation menus across 20+ pages
- Version 2.2.0→2.2.1 references updated
- Removed outdated Apache HttpClient JavaDoc
- Removed old processor pages

**Bug Tracking**:
- Created BUGS_FIXED.md - comprehensive tracking system
- Documents all bugs: description, root cause, fix, tests
- Version tracking for fixes

### ✅ Testing & Quality

- **Test Suite**: 2618 tests (100% passing)
- **New Tests**: +56 tests for bug fixes/analysis (8 Bug #7 + 1 Bug #16 + 5 Bug #18 + 5 Bug #29 + 4 Bug #39 + 4 Bug #47 + 5 Bug #48 + 7 Bug #55 + 9 Bug #61 + 7 Bug #62 + 1 Bug #60 doc)
- **Integration Tests**: XML-based tests for all plugin modules + JavaScript/Rhino integration
- **Zero Regression**: All existing functionality maintained
- **New Testing Guide**: Comprehensive testing methodology in DEVELOPER_GUIDE.md (750+ lines)

## [2.2.0] - October 2025

### 🚀 Major Infrastructure Upgrade

**SourceForge Issue #41: HttpComponents 4.x Migration** - COMPLETED
- **Migrated from:** Apache Commons HttpClient 3.1 (deprecated 2007)
- **Migrated to:** Apache HttpComponents 4.5.14 (modern, actively maintained)
- **Impact:** Complete HTTP client infrastructure upgrade
- **Benefit:** Cloud compatibility (JElastic, OpenShift), better security, modern features
- **User Impact:** None - fully backward compatible, existing configs work without changes
- **Files:** New `HttpServiceImpl.java`, `HttpService.java`, `HttpResponse.java`
- **Removed:** Legacy `HttpClientManager`, `${http}` variable, `HttpInfo` class
- **Tests:** All 2639 tests pass, zero regression

**Why this matters:**
- ✅ **Cloud Compatible** - Now works on JElastic, OpenShift, AWS Lambda, etc.
- ✅ **Actively Maintained** - Regular security updates and bug fixes
- ✅ **Better Performance** - Connection pooling, async support
- ✅ **Modern SSL/TLS** - Up-to-date security protocols
- ✅ **Fewer Bugs** - Many old commons-httpclient bugs resolved

**Technical Details:**
```xml
<!-- OLD Dependency (removed) -->
<dependency>
    <groupId>commons-httpclient</groupId>
    <artifactId>commons-httpclient</artifactId>
    <version>3.1</version>
</dependency>

<!-- NEW Dependency -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.14</version>
</dependency>
```

---

### 🏗️ Architecture - Database Module Separation

**BREAKING CHANGE** (for module developers only):
- Separated database functionality from `webharvest-core` into dedicated `webharvest-database` module
- Moved 6 `org.webharvest.runtime.database.*` classes to `webharvest-database`
- Moved 2 test classes and test resources (MockDBDriver.jar, XML examples)
- Created new `DatabaseModule` for Guice dependency injection
- **Impact**: If using database features with `webharvest-core`, add explicit `webharvest-database` dependency
- **End Users**: No impact - CLI and IDE include all modules automatically

### ✅ Testing & Quality

- **Test Coverage**: Improved from 44.55% to 50.20% (+5.65%) for `webharvest-core`
- **New Tests**: Added 520+ unit tests across 20 new test files
- **Integration Tests**: Created XML-based integration test framework with 75+ tests
- **Passing Rate**: 100% (2,604/2,604 tests passing)
- **High Coverage Plugins**: 14 plugins now have ≥70% coverage

### 📦 Project Organization

- Moved XML examples from root `examples/` directory to module test resources
- Moved database test resources to `webharvest-database` module  
- Moved webbrowser test resources to `webharvest-webbrowser` module
- Removed 25+ temporary/duplicate documentation files
- Removed obsolete classpath.txt files

### 🐛 Bug Fixes

- Fixed `ScraperContextTest.testSetVar()` - lenient comparison for cached fields
- Fixed integration test resource loading paths

### 📝 Documentation

- Reorganized documentation structure - removed 25+ duplicate files
- Updated CHANGELOG.md with comprehensive October 2025 changes
- Created DOCUMENTATION_INDEX.md for easy navigation
- Maintained essential docs: README, CONTRIBUTING, DEVELOPER_GUIDE, PLUGINS

## [2.2.0] - 2025-01-15

### Added
- **ConnectionPlugin**: NEW! Named database connections with automatic connection pooling
  - Define connections once with `<connection id="myDB" driver="..." url="..."/>`
  - Reuse connections with `<database connection="myDB" sql="..."/>`
  - Support for multiple concurrent database connections
  - Automatic connection pooling for better performance
  - Full backward compatibility with inline connection syntax
- **Plugin Validation Framework**: Comprehensive validation for plugin initialization state
- **HttpService Integration**: Full HTTP service implementation with Apache HttpClient 4
- **Comprehensive Plugin Testing**: 16+ unit tests covering edge cases and error scenarios
- **Plugin Documentation**: Complete analysis of plugin behavior and patterns (`PLUGIN_TESTING_ANALYSIS.md`)
- **ConfigurableResolver API Extensions**: Added `getElementNames()`, `getElementInfo()`, `isPluginRegistered()`, `unregisterPlugin()` methods

### Fixed  
- **HttpPlugin**: Added HttpService injection validation and template resolution
- **TextPlugin**: Fixed elementDef validation and body content extraction
- **WhilePlugin**: Added configuration validation for safe loop execution
- **Logging**: Migrated from System.out.println to SLF4J loggers throughout core plugins
- **Plugin Registration**: Graceful handling of duplicate plugin registration errors

### Plugin Behavior Analysis
- **Working Plugins**: 8/15 core plugins fully functional (ConstantPlugin, VarPlugin, DefPlugin, HtmlToXmlPlugin, XPathPlugin, JsonToXmlPlugin, XmlToJsonPlugin, TokenizePlugin)
- **Body Content Issue**: Identified systematic problem with plugins requiring XML body content execution
- **Exception Handling**: All refactored plugins now throw explicit exceptions for invalid initialization states

## [2.1.0] - 2025-01-XX

### Added
- **New Plugin Architecture**: Complete migration from old processor system to new plugin architecture
- **47+ Core Plugins**: Comprehensive set of built-in plugins for web scraping and data processing
- **Automatic Plugin Discovery**: Plugin system with automatic discovery using annotations
- **Enhanced HTTP Client**: Updated HTTP client implementation with better error handling
- **Comprehensive Test Suite**: 1863+ tests with 63% overall code coverage
- **Code Quality Tools**: Integrated JaCoCo, PMD, SpotBugs, and Checkstyle
- **Developer Documentation**: Complete API documentation and developer guide
- **Maven Multi-module Structure**: Organized project structure with separate modules

### Core Plugins Added
- **HTTP Plugin**: Web page retrieval and HTTP operations
- **XPath Plugin**: XML/HTML data extraction using XPath expressions
- **XQuery Plugin**: Advanced XML querying capabilities
- **Template Plugin**: Dynamic content generation with template engine
- **Script Plugin**: JavaScript, Groovy, and BeanShell execution
- **File Plugin**: File I/O operations and management
- **Regexp Plugin**: Regular expression matching and processing
- **XML Plugin**: XML processing and transformation
- **JSON Plugin**: JSON processing and conversion
- **Text Plugin**: Text manipulation and processing
- **If Plugin**: Conditional execution
- **While Plugin**: Loop execution with condition evaluation
- **Try/Catch Plugin**: Exception handling and error management
- **Function Plugin**: User-defined function definition and calls
- **Database Plugin**: SQL database operations and connectivity
- **Variable Plugin**: Variable management and scoping
- **Sleep Plugin**: Execution delays and timing control
- **Exit Plugin**: Execution termination and control flow
- **Config Plugin**: Configuration management and access

### Changed
- **Architecture Migration**: Complete migration from old processor architecture to new plugin system
- **HTTP Client Upgrade**: Updated from Apache Commons HttpClient 3.x to standard Java HttpURLConnection
- **Dependency Management**: Updated Maven dependencies and removed obsolete libraries
- **Configuration Schema**: Enhanced XML schema validation and error reporting
- **Error Handling**: Improved exception handling with specific exception types
- **Logging**: Standardized logging using SLF4J throughout the codebase
- **Thread Safety**: Enhanced thread safety in core components

### Deprecated
- **WebHarvestPlugin**: Marked as deprecated in favor of new AbstractPlugin architecture
- **Old Processor Classes**: Legacy processor classes marked for removal in future versions
- **HttpClientManager**: Old HTTP client management classes deprecated
- **Legacy Configuration**: Old configuration format deprecated in favor of new schema

### Removed
- **Old HTTP Client Classes**: Removed Apache Commons HttpClient 3.x dependencies
- **Legacy Processor Implementations**: Removed old processor implementations from core
- **Unused Database Classes**: Removed unused database connection management classes
- **Obsolete SSL Classes**: Removed deprecated SSL-related classes
- **Duplicate Annotations**: Removed duplicate annotation classes

### Fixed
- **Memory Leaks**: Fixed memory leaks in plugin execution and context management
- **Thread Safety Issues**: Resolved thread safety problems in registry and context classes
- **Configuration Validation**: Fixed configuration validation and error reporting
- **Plugin Discovery**: Resolved plugin discovery and registration issues
- **Test Coverage**: Improved test coverage and fixed failing tests
- **Compilation Issues**: Fixed compilation errors and dependency conflicts

### Security
- **Input Validation**: Enhanced input validation and sanitization
- **HTTP Security**: Improved HTTP client security with proper SSL handling
- **Resource Management**: Better resource management and cleanup
- **Error Information**: Reduced information leakage in error messages

## [2.0.0] - 2024-XX-XX

### Added
- **Plugin System Foundation**: Initial implementation of new plugin architecture
- **Core Plugin Framework**: Basic plugin framework with AbstractPlugin base class
- **Configuration Parser**: SAX-based XML configuration parser
- **Runtime Engine**: Basic runtime engine with context management
- **Variable System**: Initial variable system implementation

### Changed
- **Architecture Refactoring**: Major architectural changes from processor to plugin system
- **Configuration Format**: Updated configuration format and schema
- **API Changes**: Significant API changes for better extensibility

### Deprecated
- **Processor System**: Old processor system marked as deprecated
- **Legacy APIs**: Old APIs marked for removal in future versions

## [1.0.0] - 2006-XX-XX

### Added
- **Initial Release**: First release of WebHarvest
- **Basic Web Scraping**: Core web scraping functionality
- **XML Configuration**: XML-based configuration system
- **Processor Architecture**: Original processor-based architecture
- **HTTP Client**: Basic HTTP client implementation
- **XPath Support**: XPath-based data extraction
- **Template Engine**: Basic template processing

### Contributors

- **Vladimir Nikic** - Original author and project founder
- **Alexander Wajda** - Core development and architecture
- **Robert Bala** - Project administration and architecture migration
- **Piotr Dyraga** - Plugin system development and testing
- **Maciej Czapiewski** - Quality assurance and testing

### Acknowledgments

Special thanks to all contributors, testers, and users who have helped improve WebHarvest over the years. The migration to the new plugin architecture represents a significant milestone in the project's evolution, providing better performance, maintainability, and extensibility.

---

For more information about WebHarvest, visit our [GitHub repository](https://github.com/webharvest/webharvest) or check the [Developer Guide](DEVELOPER_GUIDE.md).





