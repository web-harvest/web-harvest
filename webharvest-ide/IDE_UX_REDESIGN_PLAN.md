# IDE UX Redesign - Apple HIG Principles

**Goal**: Each tab = independent document with own logs, results, variables, status

---

## 🎯 Apple HIG Principles Applied

### 1. Document-Centric Model
```
Each tab is a self-contained document workspace:
- Own content (XML editor)
- Own results (execution output)
- Own state (logs, variables, status)
- Own lifecycle (run, pause, stop)
```

### 2. Clear Visual Hierarchy
```
Tab Header:
  [Name] [▶ Run] [⏸ Pause] [⏹ Stop] [Status Badge] [×]

Tab Content (Split View):
  ┌─────────────────┬─────────────────┐
  │   Editor        │   Results       │
  │   (Monaco)      │   Tabs:         │
  │                 │   • Output      │
  │                 │   • Variables   │
  │                 │   • Logs        │
  │                 │   • Session     │
  └─────────────────┴─────────────────┘
```

### 3. Immediate Feedback
```
Status indicators:
🟢 RUNNING   - Animation
🔵 PENDING   - Waiting
✅ COMPLETED - Success color
❌ FAILED    - Error color
⏸️ PAUSED    - Gray/orange
```

### 4. Consistency
```
All tabs use same layout
Same controls in same positions
Predictable behavior
```

---

## 🏗️ Architecture Changes

### Backend (ExecutionManager)

**Current Problem**:
```java
Map<String, ExecutionJob> activeExecutions;  // OK
Map<String, WebSocketSession> webSocketSessions;  // OK

sendLog(executionId, "INFO", "message");  // ✗ Logs lost when execution ends
```

**Solution**:
```java
class ExecutionJob {
    List<LogEntry> logs;  // Store ALL logs
    Map<String, String> results;  // Store results
    ScraperSession session;  // Session metadata
    TokenTracker tokenTracker;  // Token usage
    
    void addLog(String level, String message);
    List<LogEntry> getAllLogs();
}

// Keep jobs in memory even after completion (with TTL cleanup)
```

### Frontend (app.js)

**Current Problem**:
```javascript
app.currentTab  // ✗ Global state
app.tabs[]      // Tab metadata only
// Results/Logs/Variables are GLOBAL panels
```

**Solution**:
```javascript
app.tabs[] = {
    id: "tab-1",
    name: "Example 1",
    content: "<?xml...>",
    executionId: null,
    
    // PER-TAB STATE:
    logs: [],           // Own logs
    results: {},        // Own results
    variables: {},      // Own variables  
    sessionInfo: {},    // Session metadata
    tokenUsage: {},     // Token data
    status: "IDLE"      // Own status
}

// Each tab renders its OWN results panel
```

---

## 📝 Implementation Plan

### Phase 1: Backend Changes

**ExecutionJob.java** - Store logs/results:
```java
class ExecutionJob {
    private final List<LogEntry> logs = new CopyOnWriteArrayList<>();
    private final Map<String, String> results = new ConcurrentHashMap<>();
    
    public void addLog(String level, String message) {
        logs.add(new LogEntry(System.currentTimeMillis(), level, message));
    }
    
    public List<LogEntry> getAllLogs() {
        return new ArrayList<>(logs);
    }
}

class LogEntry {
    long timestamp;
    String level;
    String message;
}
```

**ExecutionManager.java** - Keep jobs longer:
```java
private final Map<String, ExecutionJob> completedJobs = new ConcurrentHashMap<>();

// After execution completes
completedJobs.put(job.getId(), job);

// New endpoint: GET /api/execution/{id}/logs
// Returns: job.getAllLogs()
```

### Phase 2: Frontend Changes

**app.js** - Per-tab state:
```javascript
// Tab data structure
{
    id: "tab-1",
    name: "Example 1",
    content: "<config>...",
    executionId: null,
    status: "IDLE",
    
    // Results (per tab!)
    output: "",
    variables: {},
    logs: [],
    sessionInfo: null,
    tokenUsage: null
}

// When switching tabs
function switchToTab(tabId) {
    app.currentTab = tabId;
    renderTabContent(tabId);
    renderTabResults(tabId);  // Show THIS tab's results!
}

// When receiving WebSocket message
function onExecutionUpdate(data) {
    let tab = findTabByExecutionId(data.executionId);
    if (tab) {
        tab.logs.push(data.log);
        tab.variables = data.variables;
        // Only update UI if this tab is active
        if (tab.id === app.currentTab) {
            renderResults(tab);
        }
    }
}
```

### Phase 3: UI Layout

**HTML Structure**:
```html
<div class="tab-workspace" data-tab-id="tab-1">
  <!-- Left: Editor -->
  <div class="editor-panel">
    <div class="editor-toolbar">
      <button onclick="runTab('tab-1')">▶ Run</button>
      <button onclick="pauseTab('tab-1')">⏸ Pause</button>
      <button onclick="stopTab('tab-1')">⏹ Stop</button>
      <span class="status-badge" data-status="IDLE">IDLE</span>
    </div>
    <div id="editor-tab-1"></div>
  </div>
  
  <!-- Right: Results (per tab!) -->
  <div class="results-panel">
    <div class="result-tabs">
      <button onclick="showTabResult('tab-1', 'output')">Output</button>
      <button onclick="showTabResult('tab-1', 'variables')">Variables</button>
      <button onclick="showTabResult('tab-1', 'logs')">Logs</button>
      <button onclick="showTabResult('tab-1', 'session')">Session Info</button>
    </div>
    
    <div class="result-content">
      <div class="output-view" data-tab="tab-1"></div>
      <div class="variables-view" data-tab="tab-1"></div>
      <div class="logs-view" data-tab="tab-1"></div>
      <div class="session-view" data-tab="tab-1"></div>
    </div>
  </div>
</div>
```

---

## 🎨 Apple HIG Visual Design

### Status Badges
```css
.status-IDLE      { background: #gray; }
.status-PENDING   { background: #blue; animation: pulse; }
.status-RUNNING   { background: #green; animation: spin; }
.status-COMPLETED { background: #success-green; }
.status-FAILED    { background: #red; }
.status-PAUSED    { background: #orange; }
```

### Session Info Panel (New!)
```
┌─────────────────────────────────────┐
│ Session Information                 │
├─────────────────────────────────────┤
│ Session ID:  abc123...              │
│ Client ID:   IDE-xyz789             │
│ Status:      🟢 COMPLETED           │
│ Duration:    2.5s                   │
│ Elements:    42 processed           │
│                                     │
│ Token Usage:                        │
│   HTTP Requests:  5                 │
│   Bytes Downloaded: 52.4 KB         │
│   CPU Time:  1,250ms                │
│   Memory Peak: 45 MB                │
└─────────────────────────────────────┘
```

---

## 📋 Implementation Checklist

### Backend (30 min)
- [ ] ExecutionJob: add logs storage
- [ ] ExecutionJob: add LogEntry class
- [ ] ExecutionManager: persist completed jobs
- [ ] ExecutionManager: sendLog() stores in job
- [ ] New endpoint: GET /api/execution/{id}/logs
- [ ] New endpoint: GET /api/execution/{id}/session

### Frontend (45 min)
- [ ] app.js: per-tab state (logs, results, variables)
- [ ] app.js: findTabByExecutionId()
- [ ] app.js: renderTabResults() - per-tab rendering
- [ ] app.js: onWebSocketMessage() - route to correct tab
- [ ] index.html: duplicate results panels per tab
- [ ] ide.css: status badges, animations

### Testing (15 min)
- [ ] Open 3 tabs
- [ ] Run all 3 simultaneously
- [ ] Verify each shows own results
- [ ] Switch between tabs - results persist
- [ ] Close tab - results cleared

---

## 🚀 Quick Win Alternative (10 min)

Jeśli chcesz szybsze rozwiązanie:

**Simple Tab History**:
```javascript
// Store last execution per tab
app.tabs[].lastExecution = {
    logs: [...],
    results: {...},
    variables: {...}
}

// When switching tabs
function switchToTab(tabId) {
    let tab = app.tabs.find(t => t.id === tabId);
    if (tab.lastExecution) {
        renderResults(tab.lastExecution);
    }
}
```

---

## 🎯 Twoja Decyzja

**Opcja A**: Pełna przebudowa (Apple HIG compliant)
- ~90 minut pracy
- Professional UX
- Per-tab wszystko

**Opcja B**: Quick win (simple history)
- ~10 minut
- Podstawowa funkcjonalność
- Wyniki per-tab

**Opcja C**: Commit Core, IDE później
- Core jest gotowy (2569 tests pass)
- IDE jako v2.2.1 lub osobny branch

Co wolisz?

