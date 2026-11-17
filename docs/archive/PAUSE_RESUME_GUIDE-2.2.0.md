> **Historical reference — feature currently unverified.** The REST endpoints and pause-state fields exist, but the execution loop does not call the pause checkpoint. See [Issue #167](https://github.com/web-harvest/web-harvest/issues/167). Do not use this document as evidence that execution can currently be paused or resumed.

# Pause/Resume Execution - WebHarvest IDE v2.2.0

## 📋 Overview

**Pause/Resume** allows you to temporarily halt scraper execution and continue later from the same point. This is useful for:
- Long-running scrapers you want to check mid-execution
- Debugging specific steps without re-running from start
- Resource management (pause to free CPU/memory)
- Interactive development workflows

---

## ✨ Features

### 1. **Pause** ⏸
- Pause running execution at any time
- Execution state preserved (variables, context)
- WebSocket connection maintained
- Can resume later or stop completely

### 2. **Resume** ▶
- Continue from exact point where paused
- All variables and context restored
- Execution picks up seamlessly
- No data loss

### 3. **Stop** ⏹
- Permanently stop execution
- Cannot resume after stop
- Session marked as CANCELLED
- Resources cleaned up

---

## 🚀 Quick Start

### Using UI Buttons

1. **Run configuration** (click ▶ Run button)
2. **During execution**, you'll see control buttons appear:
   - `⏸ Pause` - Pause execution
   - `⏹ Stop` - Stop completely
3. **When paused**, buttons change to:
   - `▶ Resume` - Continue execution
   - `⏹ Stop` - Stop permanently
4. **Click Resume** to continue or **Stop** to cancel

### Using API

```bash
# Pause execution
curl -X POST http://localhost:8080/api/execution/{executionId}/pause

# Resume execution
curl -X POST http://localhost:8080/api/execution/{executionId}/resume

# Stop execution
curl -X POST http://localhost:8080/api/execution/{executionId}/stop
```

---

## 🔌 API Reference

### POST /api/execution/{id}/pause

Pauses a running execution.

**Request:**
```bash
POST /api/execution/abc-123-def-456/pause
```

**Response (Success):**
```json
{
  "success": true,
  "executionId": "abc-123-def-456",
  "action": "pause",
  "status": "PAUSED"
}
```

**Response (Error):**
```json
{
  "success": false,
  "error": "Cannot pause - execution not running"
}
```

**Status Codes:**
- `200 OK` - Pause successful
- `400 Bad Request` - Execution not in valid state
- `404 Not Found` - Execution ID not found

---

### POST /api/execution/{id}/resume

Resumes a paused execution.

**Request:**
```bash
POST /api/execution/abc-123-def-456/resume
```

**Response (Success):**
```json
{
  "success": true,
  "executionId": "abc-123-def-456",
  "action": "resume",
  "status": "RUNNING"
}
```

---

### POST /api/execution/{id}/stop

Stops execution permanently (cannot resume).

**Request:**
```bash
POST /api/execution/abc-123-def-456/stop
```

**Response (Success):**
```json
{
  "success": true,
  "executionId": "abc-123-def-456",
  "action": "stop",
  "status": "STOPPED"
}
```

---

## 💡 Use Cases

### 1. **Debug Long Scrapers**
```javascript
// Pause after specific step
async function pauseAfterHttpRequest() {
    // Run scraper
    const execution = await startExecution();
    
    // Wait for HTTP to complete
    await sleep(2000);
    
    // Pause to inspect variables
    await fetch(`/api/execution/${execution.id}/pause`, {method: 'POST'});
    
    // Check variables in UI
    // Then resume when ready
}
```

### 2. **Resource Management**
```javascript
// Pause expensive scraper during peak hours
async function pauseDuringPeakHours() {
    const hour = new Date().getHours();
    
    if (hour >= 9 && hour <= 17) {  // Business hours
        await fetch(`/api/execution/${executionId}/pause`, {method: 'POST'});
        console.log('Paused during peak hours');
    }
}
```

### 3. **Interactive Debugging**
```
User workflow:
1. Run scraper
2. Pause after HTTP request
3. Inspect HTML in Variables panel
4. Check if XPath is correct
5. Resume to continue
6. Pause again after next step
7. Iterate until working
```

---

## 🔧 Technical Details

### How Pause Works

```java
// Backend (ExecutionManager)
public boolean pause(String executionId) {
    ExecutionJob job = activeExecutions.get(executionId);
    job.pause();  // Sets paused flag
    sendUpdate(executionId, "PAUSED", "Execution paused", progress, null);
    return true;
}

// Plugins check pause status
if (ExecutionManager.checkPauseOrStop()) {
    // Paused - wait until resumed
    // or Stopped - exit immediately
}
```

### State Preservation

When paused, the following is preserved:
- ✅ **Variables** - All created variables in context
- ✅ **Execution Position** - Exact plugin being executed
- ✅ **Configuration** - Original XML config
- ✅ **Session ID** - Same session continues
- ✅ **Logs** - All previous log entries
- ✅ **WebSocket** - Connection remains open

### Resume Behavior

```java
// Backend
public boolean resume(String executionId) {
    ExecutionJob job = activeExecutions.get(executionId);
    job.resume();  // Clears paused flag, notifies pauseLock
    sendUpdate(executionId, "RUNNING", "Execution resumed", progress, null);
    return true;
}

// Execution thread wakes up and continues
synchronized (pauseLock) {
    while (paused && !stopped) {
        pauseLock.wait();  // Blocks here during pause
    }
}
// Continues execution from next plugin
```

---

## ⚠️ Limitations (v2.2.0)

### 1. **Basic Implementation**
- **Status**: Thread-based pause/resume
- **Limitation**: Pause only works between plugins, not during plugin execution
- **Example**: If HTTP request is in progress, pause waits until request completes

### 2. **No Checkpoint Persistence**
- **Limitation**: Paused state lost on server restart
- **Workaround**: Don't restart server while execution is paused
- **Future**: v2.3.0 will save checkpoints to disk

### 3. **Sleep Plugin**
- **Current**: `<sleep>` doesn't respect pause immediately
- **Behavior**: Sleep completes before pause takes effect
- **Future**: v2.3.0 will add interruptible sleep

### 4. **HTTP Requests**
- **Current**: Ongoing HTTP request completes before pause
- **Behavior**: Pause happens after response received
- **Acceptable**: Most HTTP requests complete quickly (<1s)

---

## 🎯 Best Practices

### 1. **Pause Points**
Insert pause-friendly operations:
```xml
<config>
  <http url="..."/>
  <def var="checkpoint1">${http}</def>
  <!-- Natural pause point - between plugins -->
  
  <xpath expression="...">
    <html-to-xml>${checkpoint1}</html-to-xml>
  </xpath>
  <def var="checkpoint2">${xpath}</def>
  <!-- Another pause point -->
</config>
```

### 2. **Don't Pause During Long Operations**
Avoid pausing during:
- Large file downloads
- Database bulk inserts
- Complex XPath/regexp processing

**Better**: Pause between operations

### 3. **Use Stop for Quick Exit**
If you don't need to resume:
- Use **Stop** instead of Pause
- Faster (no state preservation)
- Frees resources immediately

---

## 🚀 Future Enhancements (v2.3.0)

### Planned Features

1. **Checkpoint Persistence**
   - Save paused state to disk
   - Resume after server restart
   - Export/import checkpoints

2. **Interruptible Operations**
   - Pause during HTTP requests
   - Pause during sleep
   - Pause during loops

3. **Step-by-Step Execution**
   - Execute one plugin at a time
   - Step into/over/out
   - Breakpoints in configuration

4. **Visual Debugger**
   - Highlight current plugin in editor
   - Show execution stack
   - Inspect variables at any point

---

## 📚 Related Documentation

- [Session Management](webharvest-core/SESSION_API_REFERENCE.md)
- [Session History](SESSION_HISTORY.md)
- [Enhanced Metrics](ENHANCED_METRICS_GUIDE.md)
- [IDE Guide](docs/web/ide.html)

---

## 🐛 Troubleshooting

### "Cannot pause - execution not running"
**Cause**: Execution already finished or not started  
**Solution**: Can only pause RUNNING executions

### "Cannot resume - execution not paused"
**Cause**: Execution not in PAUSED state  
**Solution**: First pause, then resume

### Pause doesn't take effect immediately
**Cause**: Waiting for current plugin to complete  
**Normal**: Pause happens at plugin boundaries  
**Wait**: Current operation will finish first

### Paused state lost after server restart
**Cause**: v2.2.0 doesn't persist paused state  
**Workaround**: Don't restart server while paused  
**Future**: v2.3.0 will add checkpoint persistence

---

## 🎓 Examples

### Example 1: Pause for Manual Inspection

```xml
<config>
  <!-- Fetch data -->
  <http url="https://api.example.com/data"/>
  <def var="apiData">${http}</def>
  
  <!-- User can PAUSE here to check apiData -->
  
  <!-- Process data -->
  <xpath expression="//item">
    <json-to-xml>${apiData}</json-to-xml>
  </xpath>
  
  <!-- User can PAUSE here to check xpath results -->
</config>
```

**Workflow:**
1. Run config
2. Wait 1 second
3. Click **Pause**
4. Check `apiData` in Variables panel
5. Click **Resume** to continue

### Example 2: Controlled Rate Limiting

```javascript
// Pause between batches to avoid rate limits
async function rateLimitedScraping(executionId) {
    let batchCount = 0;
    
    setInterval(async () => {
        batchCount++;
        
        if (batchCount % 10 === 0) {
            // Pause every 10 items
            await fetch(`/api/execution/${executionId}/pause`, {method: 'POST'});
            console.log('Paused for rate limiting');
            
            // Resume after 5 seconds
            setTimeout(async () => {
                await fetch(`/api/execution/${executionId}/resume`, {method: 'POST'});
                console.log('Resumed scraping');
            }, 5000);
        }
    }, 1000);
}
```

---

## 📊 Status Summary

### v2.2.0 Implementation

| Feature | Status | Notes |
|---------|--------|-------|
| **Pause Button** | ✅ Working | UI button in execution controls |
| **Resume Button** | ✅ Working | Appears when paused |
| **Stop Button** | ✅ Working | Confirms before stopping |
| **API Endpoints** | ✅ Working | POST /pause, /resume, /stop |
| **WebSocket Updates** | ✅ Working | PAUSED/RUNNING status sent |
| **State Preservation** | ✅ Working | Variables and context maintained |
| **Thread Control** | ✅ Working | Thread-based pause/resume |
| **Checkpoint Persistence** | ❌ Not yet | Coming in v2.3.0 |
| **Interruptible Sleep** | ❌ Not yet | Coming in v2.3.0 |
| **Step Debugger** | ❌ Not yet | Coming in v2.4.0 |

---

**Author**: Robert Bala  
**Version**: 2.2.0  
**Date**: October 13, 2025  
**Status**: ✅ Basic Pause/Resume Working  
**Advanced Features**: Coming in v2.3.0-v2.4.0

