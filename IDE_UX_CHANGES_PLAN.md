# IDE UX Redesign - Per-Tab State (Apple HIG)

## 🎯 Objective

Transform IDE from **global state** to **per-tab state** model, following Apple Human Interface Guidelines for document-based applications.

## 📊 Current Architecture (BROKEN UX)

```
app = {
  tabs: [{id, name, content, modified}],  // ✅ Already per-tab
  editor: monaco,                          // ✅ Shared (OK)
  
  // ❌ GLOBAL STATE - BREAKS MULTI-TAB UX:
  logContent: <div>,                       // All tabs share ONE log panel
  outputContent: <div>,                    // All tabs share ONE output panel
  variablesContent: <div>,                 // All tabs share ONE variables panel
  currentExecutionId: string               // Only tracks LAST execution
}
```

**Problem**: When switching tabs, user **loses logs and results** from previous executions! ❌

## 🎯 Target Architecture (Apple HIG)

```
app = {
  tabs: [
    {
      id, name, content, modified,           // ✅ Existing
      
      // NEW PER-TAB STATE:
      executionId: null,                     // UUID of execution
      logs: [{time, level, message}],        // Execution logs
      results: {var1: 'value', ...},         // Scraped variables
      sessionInfo: {                         // Session metadata
        sessionId, clientId, status,
        duration, elementsProcessed, ...
      },
      tokenUsage: {                          // Token tracking
        httpRequests, httpBytes, cpuMs, ...
      },
      status: 'IDLE' | 'RUNNING' | 'COMPLETED' | 'FAILED'
    }
  ],
  activeTabId: string,
  editor: monaco                             // Shared (editor content)
}
```

## 🔧 Changes Required

### 1. Tab Object Structure

```javascript
// OLD:
const tab = {
    id: tabId,
    name: tabName,
    content: tabContent,
    modified: false
};

// NEW:
const tab = {
    id: tabId,
    name: tabName,
    content: tabContent,
    modified: false,
    
    // v2.2: Per-tab execution state
    executionId: null,
    logs: [],
    results: {},
    sessionInfo: null,
    tokenUsage: null,
    status: 'IDLE',
    lastUpdate: null
};
```

### 2. Modified Functions

#### `addLog(type, message)`
```javascript
// OLD: Appends to global logContent div
// NEW: Appends to activeTab.logs[] AND updates UI if active

addLog(type, message) {
    const tab = this.getActiveTab();
    if (!tab) return;
    
    const logEntry = {
        time: new Date().toLocaleTimeString(),
        type: type,
        message: message
    };
    
    tab.logs.push(logEntry);
    
    // Update UI for active tab
    this.renderLogs();
}
```

#### `displayResults(results)`
```javascript
// OLD: Updates global outputContent/variablesContent divs
// NEW: Saves to activeTab.results AND renders if active

displayResults(results) {
    const tab = this.getActiveTab();
    if (!tab) return;
    
    tab.results = results;
    tab.status = 'COMPLETED';
    
    // Update UI for active tab
    this.renderResults();
}
```

#### `switchTab(tabId)`
```javascript
// OLD: Only switches editor content
// NEW: Switches editor + restores logs/results/session info

switchTab(tabId) {
    // Save current editor content
    if (this.activeTabId && this.editor) {
        const currentTab = this.tabs.find(t => t.id === this.activeTabId);
        if (currentTab) {
            currentTab.content = this.editor.getValue();
        }
    }
    
    // Switch to new tab
    this.activeTabId = tabId;
    const tab = this.tabs.find(t => t.id === tabId);
    
    if (tab) {
        // Restore editor
        if (this.editor) {
            this.editor.setValue(tab.content);
        }
        
        // Restore logs, results, session info
        this.renderLogs();
        this.renderResults();
        this.renderSessionInfo();
    }
    
    this.renderTabs();
}
```

#### `runConfig()`
```javascript
// OLD: Stores executionId globally
// NEW: Stores executionId in active tab

async runConfig() {
    const tab = this.getActiveTab();
    if (!tab) return;
    
    // Clear previous execution data
    tab.logs = [];
    tab.results = {};
    tab.sessionInfo = null;
    tab.tokenUsage = null;
    tab.status = 'RUNNING';
    
    // ... execute ...
    
    const result = await fetch('/api/execute', {...});
    tab.executionId = result.executionId;  // Store in tab!
    
    // Connect WebSocket for THIS tab
    await this.connectWebSocketSync(result.executionId);
}
```

### 3. New Functions

```javascript
// Get active tab
getActiveTab() {
    return this.tabs.find(t => t.id === this.activeTabId);
}

// Render logs for active tab
renderLogs() {
    const tab = this.getActiveTab();
    const logContent = document.getElementById('logContent');
    
    logContent.innerHTML = '';
    
    if (tab && tab.logs) {
        tab.logs.forEach(log => {
            const entry = document.createElement('div');
            entry.className = 'log-entry ' + log.type;
            entry.textContent = `[${log.time}] ${log.message}`;
            logContent.appendChild(entry);
        });
    }
    
    logContent.scrollTop = logContent.scrollHeight;
}

// Render results for active tab
renderResults() {
    const tab = this.getActiveTab();
    
    if (!tab || !tab.results || Object.keys(tab.results).length === 0) {
        // Show "no results" state
        document.getElementById('outputContent').textContent = 'No results yet';
        document.getElementById('variablesContent').innerHTML = '';
        return;
    }
    
    // Display results (similar to current displayResults)
    const outputContent = document.getElementById('outputContent');
    const variablesContent = document.getElementById('variablesContent');
    
    outputContent.textContent = tab.results.configResult || 
                                 tab.results.html || 
                                 Object.values(tab.results)[0] || 
                                 'No output';
    
    variablesContent.innerHTML = '';
    Object.entries(tab.results).forEach(([name, value]) => {
        const card = document.createElement('div');
        card.className = 'variable-card';
        // ... create variable card ...
        variablesContent.appendChild(card);
    });
}

// Render session info for active tab
renderSessionInfo() {
    const tab = this.getActiveTab();
    const sessionPanel = document.getElementById('sessionPanel');
    
    if (!tab || !tab.sessionInfo) {
        sessionPanel.innerHTML = '<p>No session info available</p>';
        return;
    }
    
    sessionPanel.innerHTML = `
        <div class="session-info-card">
            <h3>Session Information</h3>
            <p><strong>Session ID:</strong> ${tab.sessionInfo.sessionId.substring(0, 8)}...</p>
            <p><strong>Client ID:</strong> ${tab.sessionInfo.clientId}</p>
            <p><strong>Status:</strong> ${tab.sessionInfo.status}</p>
            <p><strong>Duration:</strong> ${tab.sessionInfo.duration}ms</p>
            <p><strong>Elements:</strong> ${tab.sessionInfo.elementsProcessed}</p>
        </div>
        
        <div class="token-usage-card">
            <h3>Token Usage</h3>
            <p><strong>HTTP Requests:</strong> ${tab.tokenUsage.httpRequests}</p>
            <p><strong>HTTP Bytes:</strong> ${tab.tokenUsage.httpBytes}</p>
            <p><strong>CPU Time:</strong> ${tab.tokenUsage.cpuMs}ms</p>
            <p><strong>Memory Peak:</strong> ${tab.tokenUsage.memoryBytes} bytes</p>
        </div>
    `;
}

// Load execution history when switching to completed tab
async loadExecutionHistory(tabId) {
    const tab = this.tabs.find(t => t.id === tabId);
    if (!tab || !tab.executionId) return;
    
    // Fetch from /api/execution/{id}/info
    try {
        const response = await fetch(`/api/execution/${tab.executionId}/info`);
        const data = await response.json();
        
        // Restore session info if available
        if (data.sessionInfo) {
            tab.sessionInfo = data.sessionInfo;
        }
        
        if (data.tokenUsage) {
            tab.tokenUsage = data.tokenUsage;
        }
        
        // Re-render
        this.renderSessionInfo();
    } catch (err) {
        console.error('Failed to load execution history:', err);
    }
}
```

### 4. HTML Changes

Add **Session** tab to results panel:

```html
<div class="results-tabs">
    <button class="result-tab active" data-tab="output">Output</button>
    <button class="result-tab" data-tab="variables">Variables</button>
    <button class="result-tab" data-tab="log">Log</button>
    <button class="result-tab" data-tab="session">Session</button>  <!-- NEW -->
</div>

<div class="results-content">
    <div id="outputPanel" class="result-panel active">...</div>
    <div id="variablesPanel" class="result-panel">...</div>
    <div id="logPanel" class="result-panel">...</div>
    <div id="sessionPanel" class="result-panel">          <!-- NEW -->
        <p>No session info available</p>
    </div>
</div>
```

## 🎨 User Experience (Apple HIG)

### Before (BROKEN):
1. User opens Tab 1, runs config → sees logs
2. User switches to Tab 2, runs config → Tab 1 logs **DISAPPEAR** ❌
3. User switches back to Tab 1 → **EMPTY LOGS** ❌

### After (CORRECT):
1. User opens Tab 1, runs config → sees logs in Tab 1
2. User switches to Tab 2, runs config → Tab 1 logs **PRESERVED** ✅
3. User switches back to Tab 1 → **SEES ORIGINAL LOGS** ✅

Each tab is an independent **document** with its own execution history!

## 🚀 Implementation Steps

1. ✅ Backend: ExecutionJob stores logs/results
2. ✅ Backend: /api/execution/{id}/logs endpoint
3. ⏳ Frontend: Extend tab object structure
4. ⏳ Frontend: Modify addLog/displayResults
5. ⏳ Frontend: Modify switchTab to restore state
6. ⏳ Frontend: Add Session info tab
7. ⏳ Frontend: Add renderSessionInfo()
8. ⏳ Testing: Multiple tabs with concurrent execution

## 📝 Migration Notes

- **Backward Compatible**: Old code still works (uses activeTab.logs instead of global)
- **No Breaking Changes**: Just adds new fields to tab object
- **Progressive Enhancement**: Session info only shown if available

## 🎯 Success Criteria

- [ ] Each tab keeps own logs after switching
- [ ] Each tab keeps own results after switching
- [ ] Session info displayed in dedicated panel
- [ ] Token usage visible
- [ ] Multiple tabs can run concurrently
- [ ] Switching tabs is instant (no flicker)
- [ ] No memory leaks (old executions cleaned)

---

**Status**: Backend COMPLETE ✅, Frontend IN PROGRESS ⏳

