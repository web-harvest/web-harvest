# Incremental Enhancement Plan

## ✅ Step-by-Step Approach (Safe & Tested)

### Phase 1: Verify Base (5 min)
- ✅ Current IDE works at http://localhost:8080/
- ✅ Run button works
- ✅ Logs work
- ✅ Variables work

### Phase 2: Add View Switcher UI (15 min)
- Add [XML] [Visual] buttons to toolbar
- NO logic yet, just UI
- Test: buttons visible and styled

### Phase 3: Add Conditional Sidebar (20 min)
- Left sidebar for plugin palette (hidden by default)
- Show/hide on button click
- Test: sidebar appears/disappears

### Phase 4: Load Plugins (20 min)
- Fetch from /api/plugins
- Render in sidebar
- Test: 29 plugins visible with icons

### Phase 5: Add Visual Canvas (20 min)
- Canvas div in center
- Parse XML on switch
- Render as boxes
- Test: XML nodes visible as boxes

### Phase 6: Property Editing (30 min)
- Right sidebar in Visual mode
- Click node → show properties
- Edit → update node
- Test: can edit node properties

### Phase 7: Full Sync (30 min)
- Visual → XML generation
- XML → Visual parsing
- Test: roundtrip preserves data

---

**Total: ~2.5 hours**  
**Each step tested before moving to next!**

Starting with Phase 1...

