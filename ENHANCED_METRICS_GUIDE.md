# Enhanced Metrics — Current Implementation Status

## 📊 Overview

Enhanced Metrics provide deep insight into scraper execution performance. Beyond basic timing, you get detailed breakdowns of HTTP activity, plugin execution times, and resource usage.

---

## Verified implementation status

**Source baseline:** `main` at `89a472f9f13e99acea92ce07fe11721e2878e99d` (2026-11-17).

| Metric | Current implementation | Important limitation |
|---|---|---|
| **Duration** | Measured from session start/end timestamps | Actual session duration |
| **Elements processed** | Counts variables extracted into the execution result map | Not a count of source records, loop items or documents processed |
| **Processing rate** | Derived from the result-variable count and duration | Inherits the same limitation |
| **HTTP metrics** | Uses aggregated TokenTracker request/byte totals when available; otherwise inserts synthetic values when the XML contains an HTTP element | TokenTracker does not provide per-request response times here; its average response time is reported as zero |
| **Plugin breakdown** | Synthetic estimates added by inspecting a small set of XML tag names | Not measured plugin execution time; only selected plugin types are represented |

### Why Simulated?

The current code provides partial aggregate HTTP counters through TokenTracker, but it does not provide reliable per-request latency. The plugin breakdown is still synthetic: ExecutionManager inspects XML text and records fixed estimated durations for selected tags. No runtime plugin lifecycle event tracking is wired into SessionMetrics.

Do not treat the displayed plugin times, synthetic HTTP fallback values, or result-variable count as production telemetry.

### What This Means

- The IDE can render session information, HTTP metrics and plugin-breakdown fields when present in the WebSocket payload.
- Session information is exposed through the IDE execution/session API servlets.
- Synthetic metric values do not alter scraper execution, but they can mislead operational decisions.
- No release or delivery date is committed for real per-plugin timing.

---

---

## ✨ Available Metrics

### 1. Basic Session Metrics
- **Duration**: Session start-to-end time in milliseconds.
- **Elements Processed**: Number of variables copied into the final result map by ExecutionManager; this is not the number of input records or loop iterations.
- **Processing Rate**: Result variables per second, not source records per second.
- **Status**: Session lifecycle status such as RUNNING, COMPLETED or FAILED.

### 2. HTTP Metrics
- **Requests / bytes**: Aggregated from TokenTracker when request and byte totals are available.
- **Fallback**: If TokenTracker has no usable HTTP totals and the XML contains <http, the IDE currently inserts a synthetic request (5 KB, 150 ms).
- **Average response time**: TokenTracker does not measure individual response times in this path; the aggregate path reports zero. The 150 ms fallback is synthetic.

### 3. Plugin Breakdown
- The current implementation detects selected tags in the XML text and records fixed estimates for http, xpath, def, html-to-xml, regexp and script.
- These are not measured per-plugin execution times. Visual bars and sorting operate on estimates, not observed runtime.

### 4. **Token Usage** ✅ (Already in v2.1)
- **CPU Time**: Total CPU milliseconds
- **Memory Peak**: Maximum heap usage
- **HTTP Stats**: Request/byte counts

---

## 🚀 Quick Start

### View Metrics in IDE

1. **Open IDE**: http://localhost:8080
2. **Run configuration** (e.g., "01_http_xpath.xml")
3. **Click Session tab** (right sidebar)
4. **See 3 cards**:
   - Current Session (basic metrics)
   - HTTP Metrics (network activity)
   - Plugin Breakdown (execution times)

### Access via API

```bash
# Get session with metrics
curl http://localhost:8080/api/execution/{executionId}/info

# WebSocket delivers metrics in COMPLETED message
{
  "status": "COMPLETED",
  "sessionInfo": {
    "duration": 150,
    "elementsProcessed": 25,
    "processingRate": 166.67,
    "httpMetrics": {
      "requests": 3,
      "bytesDownloaded": 15360,
      "avgResponseTime": 250
    },
    "pluginBreakdown": {
      "http": 150,
      "xpath": 25,
      "def": 5
    }
  }
}
```

---

## 📈 HTTP Metrics

### Internal metrics model

The following methods exist on `SessionMetrics`; this is an internal model API, not proof that runtime instrumentation calls them for each request. In the current IDE path, `ExecutionManager` copies aggregate TokenTracker totals at session completion and may add synthetic fallback values.

```java
SessionMetrics metrics = session.getMetrics();

// Record HTTP request
metrics.recordHttpRequest(
    5120,   // bytes downloaded
    256,    // bytes uploaded
    150     // response time (ms)
);

// Query metrics
int requests = metrics.getHttpRequests();
long downloaded = metrics.getBytesDownloaded();
long avgTime = metrics.getAvgResponseTime();
```

### Caveats for analysis

The `/api/sessions/history` response currently contains only timestamp, duration, elementsProcessed, processingRate and status. It does not include `httpMetrics` or `pluginBreakdown`. Those optional fields are attached to the completed-execution WebSocket `sessionInfo` payload.

Do not use synthetic fallback values or zero response-time values for operational bandwidth/performance conclusions.

#### 1. **Bandwidth Monitoring**
Track total data transfer:
```javascript
// Use completed WebSocket sessionInfo payloads only when the HTTP totals
// came from TokenTracker. The current fallback values are synthetic.
```

#### 2. Performance regression
Not supported by current telemetry: TokenTracker does not provide per-request response times in this path, and the synthetic fallback is not measured latency. Do not build regression alerts from `avgResponseTime` until real request timing is implemented.

#### 3. Request optimization
Request/byte totals can be inspected in the completed WebSocket `sessionInfo` payload when populated from TokenTracker. `elementsProcessed` is the count of result variables, not source records, so ratios based on it are not record-level efficiency metrics.

---

## 🔌 Plugin Breakdown

### What's Tracked

The current IDE implementation does not measure each plugin type's total execution time. The code below demonstrates the `SessionMetrics` accumulator API only; the current `ExecutionManager` writes fixed estimates after execution based on XML tag presence.
```java
SessionMetrics metrics = session.getMetrics();

// Record plugin execution
metrics.recordPluginExecution("http", 150);
metrics.recordPluginExecution("xpath", 25);
metrics.recordPluginExecution("def", 5);

// Get breakdown
Map<String, Long> breakdown = metrics.getPluginBreakdown();
breakdown.forEach((plugin, timeMs) -> {
    System.out.println(plugin + ": " + timeMs + "ms");
});
```

### Do not use for performance analysis

Because current values are estimates rather than measured execution time, sorting, percentage bars and comparisons are illustrative only. They cannot identify real bottlenecks or support performance regression decisions.

#### 1. **Identify Bottlenecks**
Find slowest plugins:
```javascript
const session = /* completed session */;
if (session.pluginBreakdown) {
    const sorted = Object.entries(session.pluginBreakdown)
        .sort((a, b) => b[1] - a[1]);
    
    console.log('Slowest plugin:', sorted[0][0], sorted[0][1] + 'ms');
}
```

#### 2. **Optimization Targets**
Calculate percentage of time per plugin:
```javascript
const breakdown = session.pluginBreakdown;
const total = session.duration;

Object.entries(breakdown).forEach(([plugin, time]) => {
    const percent = (time / total * 100).toFixed(1);
    console.log(`${plugin}: ${percent}% of execution time`);
});
```

#### 3. **Compare Configurations**
See which scraper uses which plugins most:
```javascript
const sessions = await fetch('/api/sessions/history?limit=100')
    .then(r => r.json());

const httpHeavy = sessions.sessions
    .filter(s => s.pluginBreakdown && s.pluginBreakdown.http > 100)
    .map(s => s.configName);

console.log('HTTP-heavy scrapers:', httpHeavy);
```

---

## 🎨 UI Features

### Session Panel Layout

The following is an illustrative layout; the numbers are examples, not observed performance data.

```
┌─────────────────────────────┐
│ ⚡ Current Session          │
│ ID: abc123...               │
│ Status: ✓ COMPLETED         │
│ Duration: 150ms             │
│ Elements: 25                │
│ Rate: 166.67 elem/s         │
├─────────────────────────────┤
│ 🌐 HTTP Metrics            │
│ Requests: 3                 │
│ Downloaded: 15.0 KB         │
│ Uploaded: 0.25 KB           │
│ Avg Response: 250 ms        │
├─────────────────────────────┤
│ 📊 Plugin Breakdown        │
│ 🧩 http      150ms █████████│
│ 🧩 xpath      25ms ██       │
│ 🧩 def         5ms ▌        │
└─────────────────────────────┘
│ [📜 View All Sessions]      │
└─────────────────────────────┘
```

### Visual Features
- **Color-coded bars**: Purple → Magenta gradient
- **Sorted**: Longest-running plugins first
- **Percentage bars**: Visual proportion of execution time
- **Icons**: Font Awesome icons for each metric type

---

## 🔧 Technical Implementation

### Architecture

```
┌─────────────────────────────────┐
│ SessionMetrics.java             │
│ ┌─────────────────────────────┐ │
│ │ httpRequests: AtomicInteger │ │
│ │ bytesDownloaded: AtomicLong │ │
│ │ pluginTimes: ConcurrentMap  │ │
│ │                             │ │
│ │ recordHttpRequest()         │ │
│ │ recordPluginExecution()     │ │
│ │ getPluginBreakdown()        │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
         ▲
         │ Updated at session finalization
         │
┌─────────────────────────────────┐
│ ExecutionManager.java           │
│ - Copies aggregate HTTP totals │
│ - Adds synthetic plugin estimates │
│ - Sends sessionInfo at completion │
└─────────────────────────────────┘
         │
         │ JSON over WebSocket
         ▼
┌─────────────────────────────────┐
│ Frontend (app.js)               │
│ - Receives sessionInfo          │
│ - Renders metric cards          │
│ - Shows plugin breakdown bars   │
└─────────────────────────────────┘
```

### Data Structure

```typescript
interface SessionInfo {
  sessionId: string;
  duration: number;
  elementsProcessed: number;
  processingRate: number;
  
  // Session metrics payload (current IDE implementation)
  httpMetrics?: {
    requests: number;
    bytesDownloaded: number;
    bytesUploaded: number;
    avgResponseTime: number;
  };
  
  pluginBreakdown?: {
    [pluginName: string]: number;  // milliseconds
  };
}
```

---

## 💡 Best Practices

### 1. **Monitor Expensive Operations**
Track scrapers that make many HTTP requests:
```javascript
async function findHttpHeavy() {
    const history = await fetch('/api/sessions/history?limit=100')
        .then(r => r.json());
    
    return history.sessions
        .filter(s => s.httpMetrics && s.httpMetrics.requests > 10)
        .map(s => ({
            config: s.configName,
            requests: s.httpMetrics.requests,
            mb: (s.httpMetrics.bytesDownloaded / 1024 / 1024).toFixed(2)
        }));
}
```

### 2. **Optimize Plugin Usage**
Identify plugins that consume most time:
```javascript
function analyzePluginUsage(session) {
    if (!session.pluginBreakdown) return;
    
    const total = Object.values(session.pluginBreakdown)
        .reduce((sum, time) => sum + time, 0);
    
    Object.entries(session.pluginBreakdown).forEach(([plugin, time]) => {
        const percent = (time / total * 100).toFixed(1);
        if (percent > 50) {
            console.warn(`${plugin} dominates: ${percent}% of execution`);
        }
    });
}
```

### 3. **Bandwidth Budgets**
Alert when download exceeds limit:
```javascript
const MAX_MB = 10;

function checkBandwidth(session) {
    if (!session.httpMetrics) return;
    
    const mb = session.httpMetrics.bytesDownloaded / 1024 / 1024;
    if (mb > MAX_MB) {
        console.warn(`Bandwidth exceeded: ${mb.toFixed(2)}MB > ${MAX_MB}MB`);
    }
}
```

---

## Current limitations and follow-up work

- **Result count:** the IDE currently counts final result variables as processed elements. It does not count source records or loop iterations.
- **HTTP:** aggregate request/byte totals depend on TokenTracker data. Per-request response times are not measured in this path; the synthetic fallback must be clearly distinguished from observed data.
- **Plugin timing:** current plugin breakdown values are fixed estimates based on XML tag presence. They are not suitable for performance regression analysis or bottleneck diagnosis.
- **Memory:** the session metrics model does not expose a verified memory-snapshot tracking pipeline.
- **Future work:** real HTTP request/response instrumentation, plugin lifecycle timing and memory measurements require separate implementation and tests. No release version or delivery date is committed.

---
## 📚 Related Documentation

- [Session Management](webharvest-core/SESSION_API_REFERENCE.md)
- [Session History](SESSION_HISTORY.md)
- [Token Tracking](webharvest-core/TOKEN_TRACKING.md)
- [IDE Guide](docs/web/ide.html)

---

## 🎯 Summary

**The current IDE session payload provides:**
- ✅ HTTP activity monitoring
- ✅ Plugin execution breakdown
- ✅ Visual performance analysis
- ✅ Complete API access

**UI Status:**
- ✅ All metric cards implemented
- ✅ Visual bars for plugin breakdown
- ✅ Professional design
- ✅ Responsive layout

**Data Status:**
- HTTP request/byte totals may be aggregated from TokenTracker or may be synthetic fallback values.
- Plugin breakdown values are synthetic estimates.
- Result counts represent final result variables, not source records.

---

**Author**: Robert Bala  
**Verified against**: `main` at `89a472f9f13e99acea92ce07fe11721e2878e99d`  
**Review date**: 2026-11-17  
**Status**: UI payload exists; several metric values remain synthetic or proxy measurements.

