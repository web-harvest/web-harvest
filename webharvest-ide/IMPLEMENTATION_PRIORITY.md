# Implementation Priority - Fix What Matters

## ❌ Current Problems:

1. **Run button nie działa** - brak WebSocket execution
2. **Visual mode "farsa"** - tylko puste miejsce, nie da się nic zrobić
3. **Brak drag & drop** - nie można umieszczać komponentów
4. **Brak property editing** - nie można ustawiać właściwości
5. **Nowy kod NIE wykorzystuje istniejącego** - reinventing the wheel

---

## ✅ Solution: MERGE istniejącego kodu z nowym layoutem!

### Priority 1: Run Button (CRITICAL!)
**Copy from app.js:**
- WebSocket initialization
- runConfig() function
- Execution handling
- Logs display

**Status:** 0% - TODO NOW

### Priority 2: Visual Mode Basics
**Copy from visual-designer:**
- Drag & drop logic
- Node rendering on canvas
- Property panel integration

**Status:** 0% - TODO NEXT

### Priority 3: Full Integration
- Save/Load
- Variables tracking
- Session info

---

## 🎯 New Approach:

**STOP creating from scratch!**
**START copying working code!**

1. Take app.js WebSocket code → add to unified-app.js
2. Take visual-designer drag&drop → add to unified-app.js
3. Test each feature as we add it

**Estimated time:** 2-3 hours to have WORKING IDE

Starting NOW with WebSocket execution...

