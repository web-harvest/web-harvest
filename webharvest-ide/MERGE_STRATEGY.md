# Merge Strategy - Copy Working Code

## 🎯 Goal: 
Don't reinvent - REUSE existing working code!

## 📋 What to Copy from app.js to unified-app.js:

### 1. WebSocket Execution (PRIORITY 1)
**Lines ~907-1200 from app.js:**
- `runConfig()` - complete function
- `connectWebSocketSync()` - WebSocket connection
- `onmessage` handlers - LOG, PROGRESS, VARIABLE, RESULT, COMPLETE, ERROR
- `addLog()`, `showProgress()`, `renderLogs()`, `renderVariables()`

**Estimated:** ~300 lines of code

### 2. Tab Management 
**Already have basic version, enhance with:**
- Save/Load functionality
- Modified indicator
- Close confirmation

### 3. Visual Designer Integration
**Copy from /visual-designer/core/:**
- Drag & drop logic
- Node rendering
- Property panel

---

## ⏱️ Implementation Time:

- Copy WebSocket code: 30 min
- Test execution works: 15 min
- Copy visual designer: 1 hour
- Test visual mode: 30 min
- Polish & bug fixes: 1 hour

**Total: 3-4 hours for WORKING unified IDE**

---

Starting with WebSocket NOW...

