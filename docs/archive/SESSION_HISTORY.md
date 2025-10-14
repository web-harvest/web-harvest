# Session History - WebHarvest IDE v2.2.0

## 📜 Overview

**Session History** provides a complete audit trail of all scraper executions in the IDE. Every time you run a configuration, WebHarvest creates a session record with full execution details, performance metrics, and results.

> **⚠️ Note on Metrics (v2.2.0)**:  
> HTTP Metrics and Plugin Breakdown are currently **simulated for UI demonstration**.  
> Real-time tracking via EventBus will be available in **v2.3.0**.  
> All other metrics (duration, elements, processing rate) are **accurate and production-ready**.

---

## ✨ Features

### 1. **Persistent History**
- All executions are stored in memory
- Survives tab switches (Apple HIG - per-document state)
- Independent history per IDE instance
- No database required - lightweight in-memory storage

### 2. **Rich Session Data**
Each session contains:
- **Session ID**: Unique identifier (UUID)
- **Timestamp**: When execution started
- **Status**: COMPLETED, FAILED, RUNNING, PENDING, CANCELLED
- **Duration**: Execution time in milliseconds
- **Configuration**: Name of executed XML file
- **Elements Processed**: Number of variables/plugins executed
- **Processing Rate**: Elements per second

### 3. **Interactive UI**
- **List View**: All sessions with key metrics
- **Detail View**: Click any session to see full details
- **Filtering**: By status (COMPLETED, FAILED, etc.)
- **Pagination**: Limit results (default 20, max 100)
- **Refresh**: Real-time updates

### 4. **REST API**
Full programmatic access via `/api/sessions/history`

---

## 🚀 Quick Start

### View History (UI)

1. **Run some configurations** to build history
2. **Open Session panel** (right sidebar)
3. **Click "View History"** button
4. **Browse sessions** - newest first
5. **Click any session** to see details

### Access via API

```bash
# Get last 20 sessions
curl http://localhost:8080/api/sessions/history

# Get only completed sessions
curl http://localhost:8080/api/sessions/history?status=COMPLETED

# Pagination
curl http://localhost:8080/api/sessions/history?limit=50&offset=0
```

---

## 📊 Use Cases

### 1. **Performance Tracking**
Monitor execution times across multiple runs:
```javascript
// Fetch history
const response = await fetch('/api/sessions/history?limit=100');
const data = await response.json();

// Analyze performance
const avgDuration = data.sessions
    .reduce((sum, s) => sum + s.duration, 0) / data.sessions.length;

console.log(`Average execution time: ${avgDuration}ms`);
```

### 2. **Debugging Failed Executions**
Filter only failed sessions to identify patterns:
```bash
curl "http://localhost:8080/api/sessions/history?status=FAILED"
```

### 3. **Audit Trail**
Export complete execution history:
```javascript
// Get all sessions
const history = await fetch('/api/sessions/history?limit=100')
    .then(r => r.json());

// Export to CSV
const csv = history.sessions.map(s => 
    `${s.timestamp},${s.configName},${s.status},${s.duration}ms`
).join('\n');

console.log(csv);
```

### 4. **Capacity Planning**
Track elements processed over time:
```javascript
const sessions = await fetch('/api/sessions/history?limit=1000')
    .then(r => r.json());

const totalElements = sessions.sessions
    .reduce((sum, s) => sum + s.elementsProcessed, 0);

console.log(`Total elements processed: ${totalElements}`);
```

---

## 🔌 API Reference

### GET `/api/sessions/history`

Returns list of executed sessions.

#### **Query Parameters**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `limit` | integer | 20 | Max sessions to return (max: 100) |
| `offset` | integer | 0 | Skip first N sessions (pagination) |
| `status` | string | - | Filter by status (COMPLETED, FAILED, etc.) |

#### **Response Format**

```json
{
  "sessions": [
    {
      "sessionId": "abc123-def456-...",
      "timestamp": "2025-10-13T12:00:00Z",
      "status": "COMPLETED",
      "duration": 150,
      "configName": "scraper.xml",
      "elementsProcessed": 25,
      "processingRate": 166.67
    }
  ],
  "total": 42,
  "limit": 20,
  "offset": 0
}
```

#### **Example Requests**

```bash
# Basic usage
GET /api/sessions/history

# With pagination
GET /api/sessions/history?limit=50&offset=50

# Filter by status
GET /api/sessions/history?status=COMPLETED

# Combine filters
GET /api/sessions/history?status=FAILED&limit=10
```

---

## 💡 Tips & Best Practices

### 1. **Monitor Long-Running Jobs**
Set up periodic checks:
```javascript
setInterval(async () => {
    const response = await fetch('/api/sessions/history?limit=1');
    const latest = response.json().sessions[0];
    
    if (latest.status === 'RUNNING' && latest.duration > 60000) {
        console.warn('Job running >60s:', latest.sessionId);
    }
}, 5000);
```

### 2. **Track Success Rate**
Calculate success/failure ratio:
```javascript
const sessions = await fetch('/api/sessions/history?limit=100')
    .then(r => r.json());

const completed = sessions.sessions.filter(s => s.status === 'COMPLETED').length;
const failed = sessions.sessions.filter(s => s.status === 'FAILED').length;

console.log(`Success rate: ${(completed / sessions.total * 100).toFixed(1)}%`);
```

### 3. **Performance Baseline**
Compare current runs to historical average:
```javascript
async function isSlowerThanUsual(currentDuration, configName) {
    const history = await fetch(`/api/sessions/history?limit=50`)
        .then(r => r.json());
    
    const similar = history.sessions
        .filter(s => s.configName === configName && s.status === 'COMPLETED');
    
    const avgDuration = similar.reduce((sum, s) => sum + s.duration, 0) / similar.length;
    
    return currentDuration > avgDuration * 1.5; // 50% slower
}
```

---

## 🔧 Technical Details

### Architecture

```
┌─────────────────────────────────────────┐
│  Frontend (app.js)                      │
│  ┌─────────────────────────────────┐   │
│  │ loadSessionHistory()             │   │
│  │ renderSessionHistory()           │   │
│  │ showSessionDetails()             │   │
│  └─────────────────────────────────┘   │
└────────────────┬────────────────────────┘
                 │ HTTP GET
                 ▼
┌─────────────────────────────────────────┐
│  Backend (SessionHistoryServlet.java)   │
│  ┌─────────────────────────────────┐   │
│  │ doGet() → GET /api/sessions/*   │   │
│  └─────────────────────────────────┘   │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│  ExecutionManager.java                  │
│  ┌─────────────────────────────────┐   │
│  │ completedExecutions: Map        │   │
│  │ getCompletedExecutions()        │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

### Storage

- **In-Memory**: `ConcurrentHashMap<String, ExecutionJob>`
- **Thread-Safe**: Concurrent reads/writes supported
- **Lifecycle**: Persists for IDE instance lifetime
- **Limit**: No hard limit (grows with executions)

### Performance

- **Query Time**: O(n) where n = total sessions
- **Filtering**: O(n) - linear scan
- **Sorting**: O(n log n) - by timestamp
- **Memory**: ~1KB per session

---

## 🚀 Future Enhancements

Planned features for upcoming releases:

### v2.3.0
- [ ] **Persistent Storage**: SQLite database for history
- [ ] **Export to CSV/JSON**: Download full history
- [ ] **Search**: Full-text search in session data
- [ ] **Charts**: Visual performance graphs

### v2.4.0
- [ ] **Comparison**: Compare two sessions side-by-side
- [ ] **Alerts**: Notify on failed executions
- [ ] **Retention Policy**: Auto-delete old sessions
- [ ] **Cloud Sync**: Share history across devices

---

## 📚 Related Documentation

- [Session Management](./SESSION_API_REFERENCE.md) - Core session API
- [Token Tracking](./TOKEN_TRACKING.md) - Resource usage tracking
- [IDE Guide](./docs/web/ide.html) - Complete IDE documentation
- [REST API](./API_REFERENCE.md) - All API endpoints

---

## 🐛 Troubleshooting

### History is Empty
**Cause**: No executions run yet  
**Solution**: Execute some configurations to build history

### Old Sessions Missing
**Cause**: Server restart clears in-memory history  
**Solution**: In v2.3.0, history will persist to database

### Slow Loading (>1000 sessions)
**Cause**: Large history set  
**Solution**: Use pagination (`?limit=50&offset=0`)

### Wrong Timestamps
**Cause**: Server timezone mismatch  
**Solution**: All timestamps are UTC (ISO 8601 format)

---

## 📝 Changelog

### v2.2.0 (October 2025)
- ✅ Initial session history implementation
- ✅ REST API endpoint `/api/sessions/history`
- ✅ UI with list and detail views
- ✅ Status filtering and pagination
- ✅ Real-time session tracking

---

**Author**: Robert Bala  
**Version**: 2.2.0  
**Date**: October 13, 2025  
**License**: BSD License

