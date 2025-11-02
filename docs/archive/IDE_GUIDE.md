# Web-Harvest IDE Guide

## Overview

Web-Harvest IDE is a modern, web-based development environment for creating and testing web scraping configurations. It combines the power of Web-Harvest core engine with a professional web interface.

## Quick Start

### Running the IDE

```bash
# From webharvest-ide directory
java -jar target/webharvest-ide-2.2.0.jar

# IDE will automatically:
# 1. Start embedded Jetty server on port 8080 (or next available port)
# 2. Open your default browser
# 3. Load 5 example configurations
```

### Building from Source

```bash
# Build entire project
cd /path/to/web-harvest
mvn clean package -DskipTests

# The IDE JAR will be at:
# webharvest-ide/target/webharvest-ide-2.2.0.jar (27MB fat JAR with all dependencies)
```

## User Interface

### Editor Panel (Left Side)

- **Tabs** - Multiple configurations can be open simultaneously
- **Monaco Editor** - Full-featured code editor with:
  - XML syntax highlighting
  - Auto-completion
  - Error detection
  - Line numbers
  - Minimap

### Results Panel (Right Side)

Three tabs for viewing execution results:

#### 1. Output Tab
- Shows the main execution result (`configResult` variable)
- Displayed in a code window with syntax highlighting
- Useful for seeing the final aggregated output

#### 2. Variables Tab
- Shows ALL variables created during execution
- Each variable displayed as a card with name and value
- Values truncated to 300 chars for readability
- Full values available in browser console

#### 3. Log Tab
- Real-time streaming of execution logs
- Color-coded by level:
  - 🔵 INFO - General information
  - ✅ SUCCESS - Successful operations
  - ⚠️ WARNING - Warnings
  - ❌ ERROR - Errors
- Timestamps for each log entry

### Toolbar

- **▶ Run** - Execute current configuration
- **⏸ Pause** - Pause execution (not yet implemented)
- **⏹ Stop** - Stop execution (not yet implemented)
- **+ New Tab** - Create new empty configuration
- **📁 Open** - Load configuration from file (planned)
- **💾 Save** - Save current configuration (planned)

## Example Configurations

The IDE includes 5 pre-loaded examples:

### 1. HTTP + XPath (`01_http_xpath.xml`)
Simple HTTP request with XPath extraction:
```xml
<config xmlns="http://org.webharvest/schema/2.1/core">
    <def var="url">https://httpbin.org/html</def>
    <def var="html">
        <http url="${url}"/>
    </def>
    <def var="page">
        <html-to-xml>
            <get var="html"/>
        </html-to-xml>
    </def>
    <def var="title">
        <xpath expression="//title/text()">
            <get var="page"/>
        </xpath>
    </def>
</config>
```

**Expected Variables:**
- `url` - The target URL
- `html` - Raw HTML content
- `page` - Parsed XML document
- `title` - Extracted title text
- `configResult` - Combined output

### 2. Quotes Scraping (`02_quotes_scraping.xml`)
Scrapes quotes from quotes.toscrape.com

### 3. JSON API (`03_json_api.xml`)
Fetches and processes JSON data

### 4. Wikipedia Scraping (`04_wikipedia.xml`)
Extracts data from Wikipedia articles

### 5. Data Pipeline (`05_data_pipeline.xml`)
Complex multi-step data processing

## Architecture

### Backend Components

#### WebHarvestServer
Main entry point that:
- Starts Jetty on available port
- Configures servlets and WebSocket endpoints
- Serves static files from `/webapp` in JAR
- Opens browser automatically

#### ConfigServlet (`/api/config/*`)
- POST `/api/config/validate` - Validates XML configuration
- Uses `BufferConfigSource` to parse XML from string
- Returns JSON with validation result

#### ExecutionServlet (`/api/execute/*`)
- POST `/api/execute` - Start new execution, returns execution ID
- POST `/api/execute/{id}/pause` - Pause execution
- POST `/api/execute/{id}/resume` - Resume execution  
- POST `/api/execute/{id}/stop` - Stop execution
- GET `/api/execute/{id}/status` - Get execution status

#### ExecutionWebSocketServlet (`/ws/execution/*`)
- WebSocket endpoint for real-time updates
- Query param: `?id=<executionId>`
- Sends two types of messages:
  1. **Log messages**: `{type: "log", level: "INFO", message: "...", timestamp: ...}`
  2. **Status updates**: `{status: "COMPLETED", message: "...", progress: 100, results: {...}}`

#### ExecutionManager
- Singleton managing concurrent executions
- Thread pool: 5 concurrent executions max
- Handles WebSocket registration/unregistration
- Extracts variables from execution context
- **Key Feature:** Uses ConfigPlugin scope fix to extract ALL variables

### Frontend Components

#### app.js
Main application logic:
- Tab management
- Monaco Editor initialization
- Configuration validation
- Execution control
- WebSocket connection and message handling
- Results display

#### Styling
Based on `docs/web/` styling:
- CSS variables for consistent theming
- Web-Harvest brand colors
- Professional code windows
- Responsive layout

## Technical Details

### Variable Extraction (ConfigPlugin Scope Fix)

**Critical Improvement (October 2025):**

Previously, variables created by `<def>` were executed in nested scopes and cleaned up after config execution, making them inaccessible.

**Solution:** `ConfigPlugin.executeBody()` now executes children in the ROOT scope:

```java
// OLD (with nested scope - variables cleaned up):
return context.executeWithinNewContext(() -> {
    // Variables created here are LOST after return
});

// NEW (direct execution in current scope - variables persist):
for (IElementDef def : defs) {
    Processor processor = ProcessorFactory.INSTANCE.createProcessor(context, def);
    processor.run(context); // Variables stay in ROOT context!
}
```

**Impact:**
- ✅ All variables accessible after execution
- ✅ IDE can display results
- ✅ No regression (all 2556 core tests pass)

### WebSocket Protocol

#### Client → Server
```json
{
  "configXml": "<?xml version='1.0'?>...",
  "configName": "my_config.xml"
}
```

#### Server → Client (Log)
```json
{
  "type": "log",
  "level": "INFO|SUCCESS|WARNING|ERROR",
  "message": "...",
  "timestamp": 1234567890
}
```

#### Server → Client (Status Update)
```json
{
  "status": "STARTED|RUNNING|COMPLETED|FAILED|STOPPED",
  "message": "Execution completed successfully",
  "progress": 100,
  "results": {
    "configResult": "...",
    "url": "...",
    "html": "...",
    ...
  }
}
```

## Testing

### Integration Tests

Located in `src/test/java/org/webharvest/server/manager/ExecutionManagerWebSocketTest.java`

**Test 1: Simple DEF Variables**
- Verifies basic variable extraction
- Tests: `greeting`, `user` variables

**Test 2: XPath Plugin**
- Tests XPath extraction on static XML
- Variables: `xmlContent`, `items`, `count`

**Test 3: Multi-Variable Pipeline (CRITICAL)**
- Full HTML parsing and XPath extraction
- Variables: `rawHtml`, `page`, `title`, `heading`
- This test caught the scope cleanup bug!

**Test 4: Log Streaming**
- Verifies logs are delivered via WebSocket
- Checks for INFO, SUCCESS messages

### Running Tests

```bash
cd webharvest-ide
mvn test

# Expected output:
# Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
# ✓ testSimpleDefVariables
# ✓ testXPathPluginVariables  
# ✓ testMultiVariablePipeline_CRITICAL
# ✓ testLogStreaming
```

## Troubleshooting

### IDE doesn't start
- Check if port 8080 is available
- Check logs in `ide.log`
- Verify Java 11+ is installed

### No variables in Results
- Check browser console (F12) for errors
- Verify WebSocket connected (console shows "✓ WebSocket connected")
- Check backend logs for "Extracted X variables"

### UI hangs or freezes
- Reload page (Cmd+R / Ctrl+R)
- Check if execution completed (backend logs)
- Clear browser cache

### Build hangs on `maven-shade-plugin`
This is a known issue with many dependencies. Solution:
```bash
# Kill hanging process
pkill -9 java

# Build with minimal config
mvn clean compile jar:jar shade:shade -DskipTests
```

## Development

### Project Structure

```
webharvest-ide/
├── src/main/java/
│   └── org/webharvest/server/
│       ├── WebHarvestServer.java      # Main entry point
│       ├── api/
│       │   ├── ConfigServlet.java      # Config validation
│       │   └── ExecutionServlet.java   # Execution control
│       ├── manager/
│       │   ├── ExecutionManager.java   # Execution orchestration
│       │   ├── ExecutionJob.java       # Job state
│       │   └── ExecutionRequest.java   # Job request
│       └── websocket/
│           ├── ExecutionWebSocketServlet.java
│           └── ExecutionWebSocketHandler.java
├── src/main/resources/webapp/
│   ├── index.html                     # Main UI
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css             # Main styles (from docs/web)
│   │   │   ├── ide.css               # IDE-specific styles
│   │   │   └── prism.css             # Code highlighting
│   │   └── js/
│   │       ├── app.js                 # Main application logic
│   │       ├── monaco-editor/         # Monaco Editor files
│   │       ├── prism.js               # Code highlighting
│   │       └── script.js              # Utilities
│   └── examples/
│       ├── 01_simple_api.xml
│       ├── 02_quotes_scraping.xml
│       ├── 03_json_api.xml
│       ├── 04_wikipedia.xml
│       └── 05_data_pipeline.xml
└── src/test/java/
    └── org/webharvest/server/manager/
        └── ExecutionManagerWebSocketTest.java  # Integration tests
```

### Adding New Features

#### Adding a New Example Configuration

1. Create XML file in `src/main/resources/webapp/examples/`
2. Add tab loading in `app.js`:
```javascript
loadExampleTabs() {
    this.addTab('my_example.xml', this.getMyExample());
}

getMyExample() {
    return `<?xml version="1.0" encoding="UTF-8"?>
    <config xmlns="http://org.webharvest/schema/2.1/core">
        <!-- Your configuration -->
    </config>`;
}
```

#### Adding a New API Endpoint

1. Create servlet in `src/main/java/org/webharvest/server/api/`
2. Register in `WebHarvestServer.java`:
```java
context.addServlet(new ServletHolder(new MyServlet()), "/api/my-endpoint/*");
```

## Known Issues

### Module-info Warnings
You may see warnings like:
```
WARNING: We have a duplicate module-info.class in ...
```
These are harmless - they occur because Jetty JARs include Java 9+ module descriptors.

### Shade Plugin Performance
The `maven-shade-plugin` can be slow (30-60 seconds) due to:
- Large number of dependencies (~50 JARs)
- Duplicate handling
- Service file merging

**Workaround:** Configuration optimized to skip transformers and reduce processing.

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

## Version History

### 2.2.0 (October 2025)
- ✅ Web-based IDE with Embedded Jetty
- ✅ ConfigPlugin scope fix for variable extraction
- ✅ Professional integration tests (4 tests, 100% pass)
- ✅ Real-time WebSocket streaming
- ✅ Monaco Editor integration
- ✅ No regression (all 2556 core tests pass)

### 2.1.0
- Core plugin architecture with @CorePlugin annotation
- Database module separation
- Enhanced HTTP client

---

**Maintained by:** Web-Harvest Team  
**Project Site:** https://sourceforge.net/projects/web-harvest/  
**Documentation:** https://web-harvest.sourceforge.io/  
**License:** BSD 3-Clause

