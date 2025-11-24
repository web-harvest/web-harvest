# Property Editor - User Guide

## Overview

The Property Editor in WebHarvest IDE allows you to configure plugin properties for nodes in your visual workflow. It uses a **deferred changes pattern** where all edits are stored locally until you explicitly apply them, preventing accidental modifications.

## Table of Contents

1. [Basic Usage](#basic-usage)
2. [Apply/Cancel Pattern](#applycancel-pattern)
3. [Visual Indicators](#visual-indicators)
4. [Keyboard Shortcuts](#keyboard-shortcuts)
5. [Confirmation Dialogs](#confirmation-dialogs)
6. [Validation](#validation)
7. [Advanced Editors](#advanced-editors)

---

## Basic Usage

### Opening the Property Editor

1. **Click a node** on the canvas to select it
2. The Properties panel automatically opens in the right sidebar
3. Alternatively, **double-click a node** to open properties

### Editing Properties

1. Make changes to any property field
2. Changes are stored **locally** (not yet saved)
3. Click **Apply** to save changes, or **Cancel** to discard

---

## Apply/Cancel Pattern

### How It Works

The Property Editor uses a **deferred changes** approach:

- **All edits are local** - Changes are stored in memory, not persisted immediately
- **Apply button** - Saves changes to the visual model and XML
- **Cancel button** - Discards all changes and restores original values
- **Visual feedback** - Clear indicators show when you have unsaved changes

### Workflow

```
1. Select node → Properties panel opens
2. Edit properties → Changes stored locally (dirty state)
3. Visual indicators appear (red dot, highlighted Apply button)
4. Click Apply → Changes persisted to model
   OR
   Click Cancel → Changes discarded, original values restored
```

### Example

```typescript
// User edits URL field
input.value = "https://new-url.com"
// → isDirty = true
// → Red dot appears
// → Apply button highlighted

// User clicks Apply
handleApply()
// → onChangeCallback called
// → Visual model updated
// → XML synchronized
// → isDirty = false
// → Indicators cleared
```

---

## Visual Indicators

### Dirty Indicator

A **red pulsing dot** appears next to the panel title when you have unsaved changes:

```
Properties ●
```

- **Visible**: When `isDirty === true`
- **Hidden**: When no changes or after Apply
- **Animation**: Subtle pulse to draw attention

### Apply Button

The Apply button changes appearance based on state:

- **Default**: Disabled, gray
- **Has Changes**: Enabled, highlighted with blue glow
- **Validation Errors**: Disabled, shows error count

**CSS Classes:**
- `.property-panel-shell__button--has-changes` - Blue highlight with glow animation

### Button States

| State | Apply Button | Cancel Button |
|-------|--------------|---------------|
| No changes | Disabled | Enabled |
| Has changes (valid) | Enabled, highlighted | Enabled |
| Has changes (errors) | Disabled | Enabled |

---

## Keyboard Shortcuts

### ESC Key
- **Action**: Cancel changes
- **Behavior**: Restores original node values and re-renders form
- **Works**: Anywhere in the property panel

### Cmd/Ctrl + Enter
- **Action**: Apply changes
- **Behavior**: Validates and persists changes (if valid)
- **Works**: Anywhere in the property panel

### Tab Navigation
- **Focus trap**: Tab cycles through fields within the panel
- **Shift+Tab**: Reverse navigation
- **Escape**: Exits focus trap and cancels changes

---

## Confirmation Dialogs

When you have unsaved changes and try to:

### Switch to Different Node

**Dialog appears with options:**
- **Save changes** - Applies changes, then switches to new node
- **Discard changes** - Cancels changes, then switches to new node
- **Cancel** - Stays on current node, keeps unsaved changes

### Close Properties Panel

**Dialog appears when:**
- Switching to another sidebar section (Execution, Data Flow, Monitoring)
- Closing the right sidebar

**Options:**
- **Save changes** - Applies changes, then closes panel
- **Discard changes** - Cancels changes, then closes panel
- **Cancel** - Keeps panel open with unsaved changes

### Switch Mode (Visual ↔ XML)

**Dialog appears when:**
- Switching from Visual to XML mode with unsaved property changes

**Options:**
- **Save changes** - Applies changes, then switches mode
- **Discard changes** - Cancels changes, then switches mode
- **Cancel** - Stays in Visual mode

---

## Validation

### Real-time Validation

- **As you type**: Fields are validated in real-time
- **Visual feedback**: Error borders, error messages
- **Apply button**: Disabled if validation errors exist

### Validation Errors

Errors are displayed:
- **Inline**: Red border on invalid fields
- **Summary**: List of all errors at top of panel
- **Tooltips**: Hover for detailed error messages

### Apply with Errors

- **Apply button is disabled** when validation errors exist
- **Fix errors first** before applying changes
- **Cancel always works** (discards invalid changes)

---

## Advanced Editors

### Expression Builder

For fields that support expressions (e.g., `${variable}`):

1. Click **Expression Builder** button next to field
2. Modal opens with:
   - Variable browser
   - Expression editor
   - Live preview
3. Changes mark panel as dirty
4. Click **Apply & Close** in modal to save

### XPath Tester

For XPath expression fields:

1. Click **XPath Tester** button
2. Modal opens with:
   - XPath editor
   - Test against sample XML
   - Results preview
3. Changes mark panel as dirty

### Regex Validator

For regex pattern fields:

1. Click **Regex Validator** button
2. Modal opens with:
   - Pattern editor
   - Test string input
   - Match results
3. Changes mark panel as dirty

---

## Best Practices

1. **Review before applying** - Check all changes before clicking Apply
2. **Use Cancel liberally** - Don't hesitate to cancel if unsure
3. **Watch for indicators** - Red dot means unsaved changes
4. **Fix validation errors** - Apply button won't work with errors
5. **Use keyboard shortcuts** - Faster workflow with ESC and Cmd+Enter

---

## Technical Details

### Implementation

- **File**: `webharvest-ide/frontend/src/visual/PropertyPanel.ts`
- **Shell**: `webharvest-ide/frontend/src/visual/PropertyPanelShell.ts`
- **State Management**: `isDirty` flag tracks unsaved changes
- **Snapshots**: `originalNode` stores initial state for Cancel

### Change Flow

```
User edits field
  ↓
onChange handler
  ↓
Update currentNode (local)
  ↓
Set isDirty = true
  ↓
Update visual indicators
  ↓
[User clicks Apply]
  ↓
Validate
  ↓
Call onChangeCallback (persist)
  ↓
Update originalNode snapshot
  ↓
Set isDirty = false
  ↓
Clear indicators
```

### API Methods

```typescript
// Check for unsaved changes
hasUnsavedChanges(): boolean

// Show confirmation dialog
checkUnsavedChanges(): Promise<boolean>

// Apply changes
handleApply(): void

// Cancel changes
handleCancel(): void
```

---

**Last Updated**: 2025-01-15  
**Version**: 2.3.1

