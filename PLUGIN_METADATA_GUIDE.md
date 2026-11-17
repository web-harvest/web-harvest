# Plugin Metadata Guide

**Verified against:** `main` at `89a472f9f13e99acea92ce07fe11721e2878e99d`  
**Review date:** 2026-11-17

## Overview

This guide explains how to add visual metadata to WebHarvest plugins for integration with the visual editor. The visual metadata system extends `PluginMetadata` with `VisualPluginMetadata`, providing information needed for canvas-based visual editing, Apple HIG compliance, and Azure Data Factory/Apache NiFi patterns.

## Quick Start

To add visual metadata to a plugin:

1. Mark the plugin as palette-eligible with `@CorePlugin(paletteComponent = true)` and provide a `VisualPluginMetadata` implementation from `getMetadata()`
2. Implement all required methods from `VisualPluginMetadata`
3. Run tests to validate metadata completeness
4. Rebuild to generate JSON metadata for frontend

## VisualPluginMetadata Interface

The `VisualPluginMetadata` interface extends `PluginMetadata` with the following methods:

### Visual Appearance

- `String getIcon()` - Icon identifier (FontAwesome class or SVG path)
- `String getColor()` - Category color in hex format (e.g., "#3B82F6")
- `String getGroup()` - Visual group for palette organization (e.g., "HTTP & Network")

### Canvas Dimensions

- `int getDefaultWidth()` - Default node width in pixels (typically 200-300)
- `int getDefaultHeight()` - Default node height in pixels (typically 100-150)

### Data Flow (NiFi-style)

- `InputPort[] getInputPorts()` - Input ports for data flow
- `OutputPort[] getOutputPorts()` - Output ports for data flow
- `PortType getPortType()` - Data type (STRING, XML, BINARY, MIXED)

### User Experience

- `String getTooltip()` - Extended tooltip for UI
- `KeyboardShortcut[] getKeyboardShortcuts()` - Apple HIG keyboard shortcuts
- `AccessibilityMetadata getAccessibility()` - Screen reader support

### Parent-Child Relationships

- `ParentChildRelationship getParentRelationship()` - For detail plugins (e.g., `http-param` requires `http` as parent)
- `String[] getAllowedChildPlugins()` - For master plugins (e.g., `http` allows `http-header`, `http-param`)

### Linked Services (Azure Data Factory style)

- `LinkedServiceMetadata[] getLinkedServices()` - External service connections

## Implementation Pattern

Current Core examples use a static inner metadata class returned by `getMetadata()`. The metadata class implements `VisualPluginMetadata` (which already extends `PluginMetadata`):

```java
@CorePlugin(
    name = "http",
    category = "Network",
    description = "HTTP request plugin",
    paletteComponent = true
)
public class HttpPlugin extends AbstractPlugin {
    
    // ... plugin implementation ...
    
    /**
     * Metadata for HttpPlugin.
     */
    public static class HttpPluginMetadata implements VisualPluginMetadata {
        
        // PluginMetadata implementation
        @Override
        public String getElementName() {
            return "http";
        }
        
        // ... other PluginMetadata methods ...
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-globe";
        }
        
        @Override
        public String getColor() {
            return "#3B82F6"; // Blue for HTTP/Network
        }
        
        @Override
        public String getGroup() {
            return "HTTP & Network";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 120;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("url", "URL to request (from url attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "HTTP response (XML, JSON, or binary)", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Performs HTTP/HTTPS requests with full support for headers, parameters, authentication, redirects, and binary content.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+H", "Add HTTP Request", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "HTTP Request Plugin",
                "Performs HTTP/HTTPS requests with full support for headers, parameters, authentication, redirects, and binary content.",
                new String[]{"Tab", "Enter", "Escape"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return null; // Master plugin - no parent required
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[]{"http-header", "http-param"}; // Master plugin
        }
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return new HttpPluginMetadata();
    }
}
```

## Detail Plugin Example

Detail plugins (child plugins) require a parent relationship:

```java
@CorePlugin(
    name = "http-param",
    category = "Network",
    description = "HTTP parameter plugin"
)
public class HttpParamPlugin extends AbstractPlugin {
    
    // ... plugin implementation ...
    
    public static class HttpParamPluginMetadata implements PluginMetadata, VisualPluginMetadata {
        
        // ... PluginMetadata methods ...
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("http", true, "HTTP Request");
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return null; // Detail plugins don't have children
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugins - no shortcuts
        }
        
        // ... other VisualPluginMetadata methods ...
    }
}
```

## Color Guidelines

Use consistent colors for plugin categories:

- **HTTP & Network:** `#3B82F6` (Blue)
- **XML Processing:** `#10B981` (Green)
- **Control Flow:** `#F59E0B` (Orange)
- **Data Transformation:** `#8B5CF6` (Purple)
- **File Operations:** `#6366F1` (Indigo)
- **Scripting:** `#EC4899` (Pink)

## Icon Guidelines

Icons should be:
- FontAwesome classes (e.g., `"fas fa-globe"`)
- Or SVG paths starting with `"M "` or `"<svg"`

Preferred FontAwesome icons:
- HTTP/Network: `fas fa-globe`, `fas fa-network-wired`
- XML: `fas fa-code`, `fas fa-file-code`
- Control Flow: `fas fa-project-diagram`, `fas fa-sitemap`
- File: `fas fa-file`, `fas fa-folder`

## Default Dimensions

Recommended dimensions:
- **Small plugins:** 180x100 (e.g., simple operations)
- **Medium plugins:** 220x120 (e.g., HTTP, XML)
- **Large plugins:** 260x150 (e.g., complex transformers)

## Port Types

Use appropriate `PortType` for data flow:

- `PortType.STRING` - String data
- `PortType.XML` - XML documents
- `PortType.BINARY` - Binary data (files, images)
- `PortType.MIXED` - Mixed or unknown type

## Keyboard Shortcuts

Follow Apple HIG guidelines:
- Use `Cmd` on Mac, `Ctrl` on Windows/Linux
- Global shortcuts: `isGlobal = true`
- Context-specific: `isGlobal = false`
- Format: `"Cmd+Shift+H"`, `"Cmd+N"`, etc.

## Accessibility

Every palette-eligible Core plugin must provide accessibility metadata. The completeness test does not assert that every registered or system plugin belongs in the visual palette:

```java
@Override
public AccessibilityMetadata getAccessibility() {
    return new AccessibilityMetadata(
        "Plugin Name",                    // ariaLabel
        "Detailed description...",        // ariaDescription
        new String[]{"Tab", "Enter"}     // keyboardNavigation hints
    );
}
```

## Validation

After implementing `VisualPluginMetadata`, run:

```bash
mvn -pl webharvest-core -Dtest=VisualPluginMetadataCompletenessTest test
```

This test enumerates Core plugins annotated with `@CorePlugin`, `systemComponent = false` and `paletteComponent = true`. It validates:
- All required fields are populated
- Field formats are correct (colors, icons, dimensions)
- Input/output ports are valid
- Parent-child relationships are consistent
- Accessibility metadata is complete

## JSON Metadata Generation

Visual metadata is converted to JSON during the `webharvest-ide` Maven module's `process-classes` phase (see `webharvest-ide/pom.xml`):

1. `PluginMetadataJsonGenerator` discovers Core plugins explicitly eligible for the visual palette and requires their metadata to implement `VisualPluginMetadata`
2. Generates `plugins.json` in `webharvest-ide/src/main/resources/webapp/static/metadata/`
3. Frontend TypeScript code consumes this JSON

## Best Practices

1. **Consistency:** Use consistent colors, icons, and dimensions for similar plugins
2. **Completeness:** Always provide all required fields (don't return null unless appropriate)
3. **Documentation:** Add clear descriptions in tooltips and accessibility metadata
4. **Testing:** Run `VisualPluginMetadataCompletenessTest` before committing
5. **Parent-Child:** Correctly define master/detail relationships for nested plugins

## Troubleshooting

### Metadata not appearing in visual editor

1. Check that plugin implements `VisualPluginMetadata`
2. Verify `@CorePlugin` annotation is present
3. Ensure `systemComponent = false` (system components are excluded)
4. Rebuild the IDE module: `mvn -pl webharvest-ide -am process-classes`
5. Check JSON generation: `target/classes/webapp/static/metadata/plugins.json`

### Validation test failures

1. Run specific test: `mvn test -Dtest=VisualPluginMetadataCompletenessTest#testAllPluginsHaveVisualMetadata`
2. Check test output for missing fields
3. Fix metadata gaps identified by test
4. Re-run tests until all pass

### Ports not displaying

1. Verify `getInputPorts()` and `getOutputPorts()` return non-null arrays
2. Check port names are non-empty
3. Ensure port types are valid `PortType` enum values
4. Check frontend console for errors

## Related Documentation

- `VISUAL_EDITOR_GUIDE.md` - User guide for visual editor
- `DEVELOPER_GUIDE.md` - General developer documentation
- `VisualPluginMetadata.java` - Interface source code
- `VisualPluginMetadataCompletenessTest.java` - Validation tests

## Examples

See these plugin implementations for reference:

- `HttpPlugin.java` - Master plugin with children
- `HttpParamPlugin.java` - Detail plugin with parent relationship
- `XQueryPlugin.java` - Master plugin with complex children
- `XQParamPlugin.java` - Detail plugin example

