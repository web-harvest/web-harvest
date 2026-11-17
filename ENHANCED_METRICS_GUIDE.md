# Enhanced Metrics — Current Implementation Status

## 📊 Overview

The IDE exposes a session-metrics payload, but most resource fields are not currently backed by runtime instrumentation. This guide distinguishes measured session timing from synthetic HTTP/plugin estimates and unrecorded token fields.

---

## Verified implementation status

**Source baseline:** `main` at `a63a614daedfb86ae173c524a96c511ac577af89` (2026-11-17).

| Metric | Current implementation | Important limitation |
|---|---|---|
| **Duration** | Measured from session start/end timestamps | Actual session duration |
| **Elements processed** | Counts variables extracted into the execution result map | Not a count of source records, loop items or documents processed |
| **Processing rate** | Derived from the result-variable count and duration | Inherits the same limitation |
| **HTTP metrics** | TokenTracker is initialized but no runtime code records HTTP events; when XML contains an HTTP element, ExecutionManager inserts synthetic request/byte/latency values | Not observed HTTP telemetry |
| **Plugin breakdown** | Synthetic estimates added by inspecting a small set of XML tag names | Not measured plugin execution time; only selected plugin types are represented |

### Why Simulated?

ExecutionManager initializes an empty TokenTrackerImpl, but HttpPlugin and ScraperContext do not record HTTP events into it. ExecutionManager logs that TokenTracker is not collecting data and inserts synthetic HTTP values when the XML contains an HTTP element. The plugin breakdown is also synthetic: selected XML tags are inspected and fixed estimated durations are recorded. No runtime plugin lifecycle timing is wired into SessionMetrics.

Do not treat the displayed plugin times, synthetic HTTP fallback values, or result-variable count as production telemetry.

### What This Means

- The IDE can render session information, synthetic HTTP fields and synthetic plugin-breakdown fields when present in the WebSocket payload.
- `GET /api/execution/{id}/info` returns execution status/progress only; `GET /api/sessions/history` returns basic session fields. The completed-execution WebSocket message carries the optional synthetic HTTP/plugin fields.
- Synthetic metric values do not alter scraper execution, but they can mislead operational decisions.
- The token payload has CPU and memory fields, but no recording path was found; these values are zero/default rather than measured.
- No release or delivery date is committed for real telemetry.

---

---

## ✨ Available Metrics

### 1. Basic Session Metrics
- **Duration**: Session start-to-end time in milliseconds.
- **Elements Processed**: Number of variables copied into the final result map by ExecutionManager; this is not the number of input records or loop iterations.
- **Processing Rate**: Result variables per second, not source records per second.
- **Status**: Session lifecycle status such as RUNNING, COMPLETED or FAILED.

### 2. HTTP Metrics
- **Requests / bytes**: No real HTTP request/byte recording is currently wired to TokenTracker.
- **Fallback**: If the XML contains `<http`, the IDE inserts a synthetic request (5 KB, 150 ms).
- **Average response time**: No per-request latency is measured; the displayed 150 ms is synthetic.

### 3. Plugin Breakdown
- The current implementation detects selected tags in the XML text and records fixed estimates for http, xpath, def, html-to-xml, regexp and script.
- These are not measured per-plugin execution times. Visual bars and sorting operate on estimates, not observed runtime.

### 4. Token usage model (not instrumented)
- `TokenUsage` defines HTTP request/byte, CPU-time and memory-peak fields.
- `TokenTrackerImpl` stores values only when a caller explicitly records them.
- The current execution path initializes the tracker but does not record these resources; do not interpret zero/default values as measured consumption.

---

## 🚀 Quick Start

### View Metrics in IDE

1. **Open IDE**: http://localhost:8080
2. **Run a configuration**.
3. **Inspect the session panel** (where enabled by the current IDE build).
4. **Treat the displayed HTTP/plugin values as synthetic** until Issue #168 is resolved:
   - Current Session (duration and result-variable proxy count)
   - HTTP Metrics (synthetic fallback values)
   - Plugin Breakdown (synthetic estimates)

### Access via API

```text
GET /api/execution/{id}/info       -> execution status and progress
GET /api/sessions/history          -> timestamp, duration, result-variable count, rate, status
/ws/execution/* completed message  -> sessionInfo with synthetic HTTP/plugin fields
```

Example shape of the completed WebSocket message (illustrative values only):
```json
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

The following methods exist on `SessionMetrics`; this is an internal model API, not proof that runtime instrumentation calls them for each request. No current HTTP request call site was found. `ExecutionManager` instead adds synthetic fallback values when the XML contains an HTTP element.

```java
SessionMetrics metrics = session.getMetrics();

// Record HTTP request
metrics.recordHttpRequest(
    5120,   // bytes downloaded
    256,    // bytes uploaded
    150     // response time (ms)
);

// Query metrics
long requests = metrics.getHttpRequestCount();
long downloaded = metrics.getHttpBytesDownloaded();
long avgTime = metrics.getAverageHttpResponseTime();
```

### Caveats for analysis

The mapped `GET /api/execution/{id}/info` route returns execution status/progress, not metrics. The mapped `/api/sessions/history` route returns timestamp, duration, elementsProcessed, processingRate and status; it does not include `httpMetrics` or `pluginBreakdown`. Those optional fields are attached to the completed-execution WebSocket `sessionInfo` payload. The older `ExecutionInfoServlet` source file is not registered by `WebHarvestServer`.

Do not use synthetic fallback values or zero response-time values for operational bandwidth/performance conclusions.

### Operational use

The current payload is not suitable for automated bandwidth budgets or latency regression alerts. HTTP fields are synthetic because no runtime recorder populates TokenTracker. Use the completed-execution WebSocket `sessionInfo` payload only to inspect the current UI behavior, not as operational telemetry.

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

### Operational use

Do not use this breakdown to identify bottlenecks, compare configurations or create performance alerts. The current values are fixed estimates added after execution by XML tag inspection, not measured plugin durations.

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
│ - TokenTracker remains unpopulated │
│ - Adds synthetic HTTP/plugin estimates │
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

## Guidance

- Treat duration as the measured session duration.
- Treat `elementsProcessed` and `processingRate` as result-variable proxies, not source-record metrics.
- Treat HTTP request/byte totals as unverified until the TokenTracker source is confirmed for the session.
- Treat plugin breakdown values as synthetic estimates only.
- Do not use the current plugin or HTTP metrics for production alerting, capacity planning or performance regression analysis.

---

## Current limitations and follow-up work

- **Result count:** the IDE currently counts final result variables as processed elements. It does not count source records or loop iterations.
- **HTTP:** no runtime recorder populates TokenTracker; request/byte/latency values are synthetic when an HTTP tag is detected.
- **Plugin timing:** current plugin breakdown values are fixed estimates based on XML tag presence. They are not suitable for performance regression analysis or bottleneck diagnosis.
- **CPU/memory:** fields exist in `TokenUsage`, but no runtime recording call site was found; they are not measured.
- **Future work:** real HTTP request/response instrumentation, plugin lifecycle timing and memory measurements require separate implementation and tests. No release version or delivery date is committed.

---
## 📚 Related Documentation

- [Session Management](webharvest-core/SESSION_API_REFERENCE.md)
- [Session History](SESSION_HISTORY.md)
- [Token Tracking](webharvest-core/TOKEN_TRACKING.md)
- [IDE Guide](docs/web/ide.html)

---

## 🎯 Summary

**The current IDE session payload may include:**
- Aggregate HTTP request/byte fields from TokenTracker, or synthetic fallback values
- Synthetic plugin estimates based on selected XML tags
- Basic session duration and result-variable count
- Session details in the completed-execution WebSocket payload; the session-history endpoint omits HTTP/plugin fields

**UI Status:**
- The IDE renders the session fields supplied by the execution payload.
- Displaying a metric does not establish that its value is measured or operationally reliable.

**Data Status:**
- HTTP request/byte/latency values are synthetic because no runtime call site records TokenTracker HTTP usage.
- Plugin breakdown values are synthetic estimates.
- Result counts represent final result variables, not source records.

---

**Author**: Robert Bala  
**Verified against**: `main` at `89a472f9f13e99acea92ce07fe11721e2878e99d`  
**Review date**: 2026-11-17  
**Status**: UI payload exists; several metric values remain synthetic or proxy measurements.

