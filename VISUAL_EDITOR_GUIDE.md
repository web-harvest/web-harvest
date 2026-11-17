# Visual Editor User Guide

**Verified against:** `main` at `89a472f9f13e99acea92ce07fe11721e2878e99d`  
**Review date:** 2026-11-17

## Overview

> **Architecture boundary:** The Visual Editor is an optional IDE authoring adapter. It is not the canonical semantic runtime or a format-independent execution model; the exported XML and Core plugin contracts remain authoritative. The palette includes only plugins explicitly eligible for visual presentation through `@CorePlugin(paletteComponent = true)` and `VisualPluginMetadata`. Verify generated XML before relying on a visual arrangement.

The WebHarvest Visual Editor provides a drag-and-drop canvas interface for creating WebHarvest configurations visually. It follows Apple Human Interface Guidelines (HIG) and patterns inspired by Azure Data Factory and Apache NiFi.

## Getting Started

### Accessing Visual Editor

1. Open WebHarvest IDE in your browser
2. Click "Visual Mode" button in the toolbar
3. The visual editor canvas appears with plugin palette on the left

### First Steps

1. **Browse plugins:** Scroll through the plugin palette, organized by groups (HTTP & Network, XML Processing, Control Flow, etc.)
2. **Add a plugin:** Drag a plugin from the palette to the canvas
3. **Configure:** Click on a plugin node to open the property panel
4. **Connect:** Drag from output port to input port to create connections
5. **Switch modes:** Toggle between Visual and XML modes anytime

## Interface Overview

### Plugin Palette

The left sidebar contains palette-eligible plugins with visual metadata, organized by visual groups:

- **HTTP & Network** - HTTP requests, FTP, web browser
- **XML Processing** - XPath, XQuery, XSLT
- **Control Flow** - Loop, While, If, Try-Catch
- **Data Transformation** - JSON to XML, XML to JSON, Regexp
- **File Operations** - File read/write, ZIP operations
- **Scripting** - Script execution, template processing

Each plugin shows:
- **Icon** - Visual identifier
- **Name** - Plugin name
- **Color accent** - Left border color matching category

### Canvas

The main canvas area where you build your workflow:

- **Drag & Drop:** Drag plugins from palette to canvas
- **Move nodes:** Click and drag nodes to reposition
- **Select:** Click on a node to select it
- **Multi-select:** Drag a rectangle to select multiple nodes
- **Zoom:** Use `Cmd/Ctrl + =` to zoom in, `Cmd/Ctrl + -` to zoom out
- **Pan:** Click and drag empty canvas area to pan

### Property Panel

Right sidebar shows configuration for selected plugin:

- **Attributes:** Plugin attributes (URL, method, etc.)
- **Validation:** Real-time validation with error messages
- **Help:** Plugin description and documentation links

## Basic Operations

### Adding Plugins

1. **From palette:**
   - Drag plugin from palette to canvas
   - Drop on canvas or inside container nodes

2. **Keyboard shortcut:**
   - Some plugins have keyboard shortcuts (see plugin tooltip)
   - Press shortcut to add plugin (e.g., `Cmd+Shift+H` for HTTP)

3. **Context menu:**
   - Right-click on canvas to see context menu
   - Select "Add Plugin" for quick access

### Configuring Plugins

1. **Select plugin:** Click on a plugin node
2. **Property panel opens:** Right sidebar shows configuration
3. **Edit attributes:** Change values in property panel
4. **Validation:** Errors appear in real-time if invalid
5. **Tooltips:** Hover over plugin to see detailed description

### Creating Connections

Connections show visual relationships between plugins (NiFi-style). They are editor relationships, not an independent runtime dataflow contract:

1. **Output port:** Bottom of source plugin (output port)
2. **Input port:** Top of target plugin (input port)
3. **Drag connection:** Click and drag from output to input port
4. **Visual feedback:** Port highlights when hovering during drag

**Connection types:**
- **Sequential (solid line):** Visual ordering/relationship
- **Conditional (dashed line):** Conditional visual relationship (inspect exported XML for actual If/Else semantics)
- **Error (red line):** Error-handling visual relationship (inspect exported XML and Try/Catch structure)

### Nested Containers

Some plugins are containers (e.g., `loop`, `while`, `if`):

1. **Add child:** Drag plugin into container node
2. **Visual indicator:** Container shows children area
3. **Auto-layout:** Children are automatically positioned
4. **Validation:** Only valid child plugins can be added

**Container plugins:**
- `loop` - Iterates over collection
- `while` - Conditional loop
- `if` - Conditional execution
- `try` - Error handling
- `http` - Allows `http-header`, `http-param` children
- `xquery` - Allows `xq-expression`, `xq-param` children

## Keyboard Shortcuts

### Global Shortcuts

- `Cmd/Ctrl + A` - Select all nodes
- `Cmd/Ctrl + Delete` - Delete selected nodes
- `Cmd/Ctrl + =` - Zoom in
- `Cmd/Ctrl + -` - Zoom out
- `Cmd/Ctrl + 0` - Reset zoom
- `Delete` / `Backspace` - Delete selected nodes

### Plugin-Specific Shortcuts

Some plugins have keyboard shortcuts (displayed in tooltip):
- `Cmd+Shift+H` - Add HTTP Request plugin
- `Cmd+Shift+X` - Add XPath plugin
- (See plugin tooltips for full list)

### Navigation

- `Tab` - Navigate between plugin nodes
- `Enter` - Open property panel for selected plugin
- `Escape` - Deselect / Close property panel
- `Arrow keys` - Navigate plugin palette

## Visual Features

### Color Coding

Plugins are color-coded by category:

- 🔵 **Blue** - HTTP & Network
- 🟢 **Green** - XML Processing
- 🟠 **Orange** - Control Flow
- 🟣 **Purple** - Data Transformation
- 🔷 **Indigo** - File Operations
- 🩷 **Pink** - Scripting

### Node Dimensions

Each plugin has default dimensions optimized for its complexity:

- **Small** (180x100) - Simple operations
- **Medium** (220x120) - Standard plugins
- **Large** (260x150) - Complex transformers

Dimensions are automatically set from plugin metadata.

### Ports

NiFi-style input/output ports:

- **Input ports** (top) - Receive data from other plugins
- **Output ports** (bottom) - Send data to other plugins
- **Port types:** STRING, XML, BINARY, MIXED (shown as icons)

### Tooltips

Hover over any plugin to see:
- **Name** - Plugin name
- **Description** - Detailed description
- **Keyboard shortcuts** - Available shortcuts
- **Category** - Visual group

## XML ↔ Visual Synchronization

### Bidirectional Conversion

The visual editor automatically synchronizes with XML:

1. **Visual → XML:** Changes in visual editor update XML in real-time
2. **XML → Visual:** Load XML file to see visual representation
3. **Auto-positioning:** XML nodes are automatically positioned using plugin metadata

### Switching Modes

1. **Visual → XML:**
   - Click "XML Mode" button
   - XML is generated from visual nodes
   - Edit XML directly if needed

2. **XML → Visual:**
   - Click "Visual Mode" button
   - XML is parsed and displayed visually
   - Nodes are auto-positioned intelligently

**Note:** Unsaved changes are preserved when switching modes.

## Validation

### Real-Time Validation

The visual editor validates your configuration in real-time:

- **Semantic validation:** Checks if plugins can appear in current context
- **Parent-child validation:** Ensures detail plugins have correct parent
- **Attribute validation:** Validates required attributes and formats
- **Connection validation:** Checks port type compatibility

### Error Messages

Validation errors appear:
- **In property panel:** Red error messages below invalid fields
- **On canvas:** Invalid nodes show error indicator
- **In tooltip:** Hover over invalid node to see error

## Accessibility

### Screen Reader Support

The visual editor exposes accessibility metadata and keyboard-oriented controls; end-to-end assistive-technology coverage must be verified separately:

- **ARIA labels:** All interactive elements have labels
- **Keyboard navigation:** Full keyboard support
- **Color:** Category colors are supplemented by text labels and port metadata; this guide does not claim a completed WCAG or screen-reader audit

### Keyboard Navigation

- `Tab` - Navigate between elements
- `Enter` - Activate selected element
- `Escape` - Cancel / Close
- `Arrow keys` - Navigate in lists and palettes

## Tips & Best Practices

### Workflow Design

1. **Start with HTTP:** Most workflows start with an HTTP request
2. **Use XPath/XQuery:** Extract data from XML/HTML responses
3. **Transform data:** Use JSON/XML converters as needed
4. **Save results:** End with file operations or database writes

### Performance

1. **Limit node count:** Keep workflows under 50 nodes for best performance
2. **Use containers:** Group related operations in containers
3. **Reuse:** Reuse patterns by copying or exporting their XML; a separate template catalog is not guaranteed

### Organization

1. **Group related plugins:** Keep related plugins close together
2. **Use alignment:** Snap to grid for cleaner layouts
3. **Label connections:** Use descriptive connection names (right-click)

## Troubleshooting

### Plugin not appearing in palette

1. Check if plugin has `VisualPluginMetadata` implemented
2. Verify the plugin is explicitly palette-eligible and provides `VisualPluginMetadata`
3. Refresh page or rebuild project

### Connection not working

1. Check port types match (STRING → STRING, XML → XML)
2. Verify plugins are in valid positions
3. Check validation errors in property panel

### XML sync issues

1. Switch to XML mode to see actual XML
2. Check for syntax errors in XML
3. Rebuild visual from XML if needed

### Performance issues

1. Reduce node count (split into multiple workflows)
2. Disable minimap if not needed
3. Clear browser cache

## Examples

### Example 1: Simple HTTP Request

1. Drag `http` plugin to canvas
2. Configure URL in property panel
3. Add a separate `def`/XPath operation after the HTTP result
4. Pass the response variable to the XPath expression
5. Configure the XPath expression; do not nest XPath as an HTTP detail child

### Example 2: Loop with HTTP

1. Drag `loop` container to canvas
2. Configure loop collection
3. Drag `http` inside `loop` container
4. Configure HTTP URL (can use loop variables)
5. Add `xpath` to extract data from each HTTP response

### Example 3: Error Handling

1. Drag `try` container to canvas
2. Add `http` plugin inside `try`
3. Add `catch` plugin for error handling
4. Configure error handling logic

## Advanced Features

### Auto-Layout

The visual editor automatically positions nodes:

- **Group-based:** Nodes from same group are positioned horizontally
- **Hierarchical:** Container children are nested and auto-positioned
- **Spacing:** Optimal spacing based on node dimensions

### Minimap

Enable minimap for large workflows:

1. Click "Minimap" button in toolbar
2. See overview of entire canvas
3. Click on minimap to jump to that area

### Grid Snap

Enable grid snap for alignment:

1. Click "Grid" button in toolbar
2. Nodes snap to grid when moving
3. Cleaner, more organized layouts

## Related Documentation

- `PLUGIN_METADATA_GUIDE.md` - Developer guide for adding visual metadata
- `DEVELOPER_GUIDE.md` - General developer documentation
- `USER_GUIDE.md` - General user guide

## Support

For issues or questions:

1. Check validation errors in property panel
2. Review console logs (F12 in browser)
3. Check `DEVELOPER_GUIDE.md` for technical details
4. Report issues with workflow XML and error messages

