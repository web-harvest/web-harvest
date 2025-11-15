# WebHarvest Visual Designer - Quick Start Guide

**Version:** 2.3.1  
**Status:** ✅ Working  
**URL:** `http://localhost:8080/visual-designer/`

---

## 🚀 How to Run

### Production Mode (Single Command)

```bash
cd /path/to/web-harvest-git
mvn clean package -DskipTests
java -jar webharvest-ide/target/webharvest-ide-2.2.0.jar
```

Browser opens automatically to: `http://localhost:8080`

Navigate to Visual Designer: `http://localhost:8080/visual-designer/`

---

## 🎨 How to Use

### 1. Drag & Drop Workflow

**Step 1: Drag Plugin from Palette**
```
Left Sidebar (Plugin Palette)
  ↓
Click and hold on "Http" plugin
  ↓
Drag to canvas (center area)
  ↓
Release mouse
  ↓
✨ Node appears!
```

**Step 2: Add More Nodes**
```
Drag "Xpath" → Drop below Http node
Drag "Def" → Drop to create variable
Drag "Loop" → Drop for iteration
```

**Step 3: Move Nodes**
```
Click and drag any node to reposition it
Nodes can be placed anywhere on canvas
```

**Step 4: Select Node**
```
Click any node once
  ↓
Card gets a blue glow + thicker border (selected)
  ↓
Property panel (right) shows node info + action buttons
```

### 2. Tile Anatomy & Resizing (2025 refresh)

| Element | Purpose | Notes |
| --- | --- | --- |
| Header row | Icon + plugin type | Shows only the canonical name (no duplicate `name`/`id` attributes). |
| Metadata pills | `Children / Containers / Leaves` | Always rendered in a single row thanks to the default **320 × 160 px** footprint. |
| Ports & anchors | Connection points | Ports hug the edge of the white tile; arrows always enter/exit from the card perimeter. |
| Selection frame | Blue glow | The highlight frame and the card share the exact same width/height, so pressing **Details** or clicking elsewhere never leaves ghost borders. |

**Resizing workflow**

1. Select a node so that resize handles (8 dots) appear.
2. Drag any handle—both the grey shell *and* the white interior resize together, keeping ports aligned.
3. Release to snap to a minimum of **320 × 160 px**; larger sizes persist and are serialized into the configuration.
4. Click empty canvas to clear focus; the blue glow disappears immediately.

Because the highlight, ports, and resize maths all share the same bounds, you no longer see the “floating label” effect that previously happened on the first node between Start/Stop markers.

---

## 🔍 Available Plugins (29 Total)

### HTTP & Web (2)
- 🌐 **Http** - HTTP requests
- 🔄 **Html To Xml** - HTML parser

### Data Extraction (3)
- 🔍 **Xpath** - XPath queries
- 📊 **Xquery** - XQuery processing
- 📝 **Regexp** - Regular expressions

### Variables (4)
- 📦 **Def** - Define variable
- 📤 **Get** - Get variable
- 💾 **Var** - Variable operations
- 💾 **Set Var** - Set variable value

### Control Flow (6)
- 🔀 **If** - Conditional
- ⤵️ **Else** - Else clause
- 🔁 **Loop** - Iteration
- ↻ **While** - While loop
- 🛡️ **Try** - Error handling
- 🎯 **Catch** - Catch errors

### Data Processing (6)
- 🔄 **Json To Xml** - JSON converter
- 🔄 **Xml To Json** - XML converter
- 📄 **Template** - Template engine
- 📝 **Text** - Text processing
- ⚙️ **Xslt** - XSLT transform
- 📄 **Xml** - XML processing

### Functions (4)
- ƒ **Function** - Define function
- 📞 **Call** - Call function
- 📞 **Call Param** - Function parameter
- ↩️ **Return** - Return value

### Utility (6)
- ⏱️ **Sleep** - Delay execution
- 🚪 **Exit** - Stop execution
- ∅ **Empty** - Empty operation
- 🔢 **Constant** - Constant value
- 📋 **List** - List operations
- 📋 **Log** - Console logging
- 💻 **Script** - Execute scripts

### File Operations (1)
- 📁 **File** - File I/O

### Other (1)
- ⚙️ **Config** - Configuration

---

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `⌘Z` / `Ctrl+Z` | Undo (coming in Phase 2) |
| `⌘+` / `Ctrl++` | Zoom in |
| `⌘-` / `Ctrl+-` | Zoom out |
| `⌘0` / `Ctrl+0` | Fit to view |
| `⌘K` / `Ctrl+K` | Quick add plugin (Phase 2) |
| `⌘S` / `Ctrl+S` | Save configuration (Phase 3) |

---

## 🎯 Current Features

### ✅ Working NOW
- Drag plugins from palette to canvas
- Drop to create nodes
- Move nodes by dragging
- Click to select nodes
- View node info in property panel
- Collapse/expand categories
- Search plugins (input ready, search in Phase 2)
- Status bar with node count

### ⏭️ Coming in Phase 2
- Edit node properties (forms)
- Validate configuration
- Apply/cancel changes
- Template editor for body content
- Help documentation viewer
- Real-time validation feedback

### ⏭️ Coming in Phase 3
- Connect nodes (visual connections)
- Generate XML from visual
- Load XML to visual
- Bidirectional sync
- Split view (XML + Visual)

---

## 🐛 Troubleshooting

### Visual Designer doesn't load

**Check:**
1. Server is running: `http://localhost:8080`
2. No console errors (F12)
3. JAR was built with `mvn package` (not just `mvn compile`)

**Solution:**
```bash
mvn clean package -DskipTests
java -jar webharvest-ide/target/webharvest-ide-2.2.0.jar
```

### Drag & drop doesn't work

**Check:**
1. Browser console for JavaScript errors
2. Plugins loaded (console should show "✅ Loaded X plugins")
3. Try different browser (Chrome/Firefox recommended)

**Solution:** Hard refresh (⌘⇧R / Ctrl+Shift+F5)

### No plugins in palette

**Check:**
1. API works: `curl http://localhost:8080/api/plugins/metadata`
2. Should return JSON with "count": 29
3. Console shows "✅ Loaded 29 plugins"

**Solution:** Restart server, clear browser cache

---

## 🎉 Success Indicators

You know it's working when you see:

✅ **Palette:** 9 categories with 29 plugins  
✅ **Drag:** Plugin follows mouse, canvas highlights  
✅ **Drop:** Node appears at drop position  
✅ **Move:** Nodes can be dragged around  
✅ **Select:** Node gets purple border on click  
✅ **Properties:** Right panel shows selected node  
✅ **Console:** "✅ Visual Designer initialization complete"

---

## 📚 Next Steps

### Explore

Try building a simple workflow:
1. Drag **Http** to canvas
2. Drag **Html To Xml** below it
3. Drag **Xpath** to extract data
4. Drag **Def** to save result
5. Click each node to see its properties

### Learn

Read documentation:
- **Design:** `LOW_CODE_VISUAL_DESIGNER.md`
- **Technical:** `VISUAL_DESIGNER_TECHNICAL_GUIDE.md`
- **UX:** `VISUAL_DESIGNER_UX_SPEC.md`

### Contribute

Ready for Phase 2? Check:
- `VISUAL_DESIGNER_ROADMAP.md` - Implementation plan
- `VISUAL_DESIGNER_INTEGRATION.md` - Architecture explained

---

**Happy Visual Configuration Building!** 🌺🎨

**Version:** 1.0  
**Last Updated:** October 24, 2025










