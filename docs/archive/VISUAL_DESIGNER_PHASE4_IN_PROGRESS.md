# Visual Designer - Phase 4 Implementation In Progress

**Date:** October 29, 2025  
**Phase:** Advanced Configuration Panel  
**Status:** 🚧 IN PROGRESS  
**Build:** ✅ SUCCESS

---

## Overview

Phase 4 focuses on creating advanced, schema-driven property editors that dramatically improve the UX for configuring workflow plugins. This phase builds on the plugin metadata system to provide intelligent, context-aware editing capabilities.

---

## Components Implemented So Far

### 1. SchemaEditor (`SchemaEditor.ts`) ✅ COMPLETE

**Purpose:** Generate smart property editors dynamically based on plugin schema

**Supported Field Types:**
- **String** → Text input with validation
- **URL** → URL input with format validation
- **Number** → Number input with min/max
- **Boolean** → Toggle checkbox
- **Enum** → Dropdown select with predefined values
- **XPath** → Textarea for multi-line expressions

**Key Features:**
- Automatic field generation from plugin schema
- Type-specific validation (URL format, number range, required fields)
- Real-time validation with visual error indicators
- onChange callbacks for reactive updates
- Smart placeholder values from schema defaults
- Consistent styling matching WebHarvest brand

**API:**
```typescript
generateEditor(container: HTMLElement, plugin: Plugin, attributes: Record<string, string>): void
validate(): ValidationError[]
getValue(): Record<string, string>
onChange(callback: (attributes: Record<string, string>) => void): void
clear(): void
```

**Validation Features:**
- Required field checking
- URL format validation (using URL constructor)
- Number type validation
- Visual error indicators (red border, pink background)
- Error message array with field names

**Example Usage:**
```typescript
const editor = new SchemaEditor();
editor.generateEditor(container, httpPlugin, { url: 'http://example.com' });

editor.onChange((attributes) => {
    console.log('Attributes changed:', attributes);
    // Update node in VisualDesigner
});

const errors = editor.validate();
if (errors.length === 0) {
    const values = editor.getValue();
    // Save values
}
```

**Test Coverage:**
- 12+ test cases in `SchemaEditor.test.ts`
- Field generation for all types
- Validation scenarios
- onChange callback behavior
- getValue functionality

---

### 2. ExpressionBuilder (`ExpressionBuilder.ts`) ✅ COMPLETE

**Purpose:** Visual builder for `${variable}` expressions with autocomplete and live preview

**Key Features:**

**Autocomplete System:**
- Triggers on `${` input
- Filters available variables by prefix
- Click to insert variable at cursor
- Keyboard navigation (upcoming)

**Variable Management:**
- Display list of available variables
- Click variable to insert
- Variable type indicators
- Real-time variable list updates

**Function Library:**
- Built-in functions: `concat`, `replace`, `substring`, `uppercase`, `lowercase`, `trim`, `length`
- Function signatures with parameter names
- Click to insert function template
- Parameter placeholders (`${param}`)

**Live Preview:**
- Real-time expression evaluation
- Variable substitution with actual values
- Error indicators for undefined variables
- Preview updates on every keystroke

**Syntax Highlighting:**
- Variables highlighted in purple
- Visual distinction from plain text
- Span-based highlighting for HTML display

**Validation:**
- Balanced brace checking
- Undefined variable warnings
- Error messages array

**UI Design:**
```
┌─────────────────────────────────────────┐
│ Expression Builder                      │
├─────────────────────────────────────────┤
│ [Textarea with ${variable} syntax]      │
│                                         │
│ ┌─────────────┐  ┌─────────────────┐  │
│ │ Variables:  │  │ Functions:       │  │
│ │ • httpResp  │  │ • concat(a, b)   │  │
│ │ • pageTitle │  │ • replace(s,f,r) │  │
│ │ • loopIndex │  │ • substring(...)  │  │
│ └─────────────┘  └─────────────────┘  │
│                                         │
│ Preview: "Hello World Page 1"           │
└─────────────────────────────────────────┘
```

**API:**
```typescript
render(container: HTMLElement, initialExpression: string): void
setAvailableVariables(variables: Record<string, string>): void
insertVariable(varName: string): void
insertFunction(funcName: string, parameters: string[]): void
evaluatePreview(): string
getValue(): string
validate(): string[]
getWarnings(): string[]
onChange(callback: (expression: string) => void): void
```

**Function Definitions:**
```typescript
interface FunctionDefinition {
    name: string;
    parameters: string[];
    description: string;
    example: string;
}
```

**Test Coverage:**
- 18+ test cases in `ExpressionBuilder.test.ts`
- Rendering and initialization
- Variable management and autocomplete
- Function insertion
- Preview evaluation (simple and multiple variables)
- Syntax highlighting
- Validation (braces, undefined variables)

---

## Files Created

### New TypeScript Files:
```
webharvest-ide/frontend/src/visual/SchemaEditor.ts
webharvest-ide/frontend/src/visual/ExpressionBuilder.ts
```

### New Test Files:
```
webharvest-ide/frontend/src/visual/__tests__/SchemaEditor.test.ts (170 lines, 12+ tests)
webharvest-ide/frontend/src/visual/__tests__/ExpressionBuilder.test.ts (285 lines, 18+ tests)
```

---

## Build Status

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
Exit code: 0
```

**TypeScript Compilation:** ✅ SUCCESS  
**Frontend Build:** ✅ SUCCESS  
**No Type Errors:** ✅ VERIFIED

---

## Remaining Phase 4 Components

### 4.3 XPath Tester (Next Priority)

**File:** `webharvest-ide/frontend/src/visual/XPathTester.ts`

**Features Planned:**
- Interactive XPath editor with Monaco integration
- Live XPath validation against XML schema
- Test against sample XML (from previous HTTP response)
- Result preview with matched elements highlighted
- Syntax error highlighting with line numbers
- XPath function library reference

**TDD Tests:** `XPathTester.test.ts`, `XPathMatcher.test.ts`

### 4.4 Regex Validator

**File:** `webharvest-ide/frontend/src/visual/RegexValidator.ts`

**Features Planned:**
- Regex syntax highlighting
- Test against sample text input
- Match highlighting with capture group indicators
- Common regex patterns library (email, URL, phone, etc.)
- Explanation of regex components

**TDD Tests:** `RegexValidator.test.ts`, `RegexPatterns.test.ts`

### 4.5 Live Data Preview

**File:** `webharvest-ide/frontend/src/visual/DataPreviewPanel.ts`

**Features Planned:**
- XML preview with syntax highlighting and collapse/expand
- JSON tree view
- HTML rendered preview
- Plain text with line numbers
- Large data handling (pagination, virtualization)

**TDD Tests:** `DataPreviewPanel.test.ts`, `SyntaxHighlighter.test.ts`

---

## Integration Points

### With PropertyPanel

The SchemaEditor and ExpressionBuilder should be integrated into the existing PropertyPanel:

```typescript
// In PropertyPanel.ts
import { SchemaEditor } from './SchemaEditor';
import { ExpressionBuilder } from './ExpressionBuilder';

export class PropertyPanel {
    private schemaEditor: SchemaEditor = new SchemaEditor();
    private expressionBuilder: ExpressionBuilder | null = null;

    showNode(node: VisualNode, plugin: Plugin): void {
        // Use SchemaEditor for standard fields
        this.schemaEditor.generateEditor(
            this.container,
            plugin,
            node.attributes
        );

        // For XPath/expression fields, show ExpressionBuilder
        const xpathFields = plugin.schema.attributes
            .filter(attr => attr.type === 'xpath');

        xpathFields.forEach(field => {
            this.expressionBuilder = new ExpressionBuilder();
            // Render expression builder for this field
        });
    }
}
```

### With VisualDesigner

Node updates from the PropertyPanel should flow back to the VisualDesigner:

```typescript
// In App.ts
propertyPanel.onChange((nodeId, updatedAttributes) => {
    visualDesigner.updateNode(nodeId, {
        ...node,
        attributes: updatedAttributes
    });
    visualDesigner.markDirty(); // Track unsaved changes
});
```

---

## Success Criteria Progress

**Phase 4 Success Criteria:**
- ✅ Schema-driven property editor with smart input types and validation
- ✅ Visual expression builder with variable autocomplete and live preview
- ⏳ Interactive XPath editor with live validation and result preview
- ⏳ Regex validator with pattern library and match highlighting
- ⏳ Live data preview with syntax highlighting

**Current Status:** 2 / 5 components complete (40%)

---

## Next Steps

### Immediate (XPath Tester):

1. **Create XPathTester.test.ts** with TDD tests
2. **Implement XPathTester.ts** with:
   - Monaco editor integration for syntax highlighting
   - XPath validation using browser's XPathEvaluator
   - Sample XML input for testing
   - Result highlighting
3. **Integration with PropertyPanel** for XPath attribute fields

### Then (Regex Validator):

1. **Create RegexValidator.test.ts**
2. **Implement RegexValidator.ts** with pattern library
3. **Integration with PropertyPanel** for regex fields

### Finally (Data Preview):

1. **Create DataPreviewPanel.test.ts**
2. **Implement DataPreviewPanel.ts** with multi-format support
3. **Integration with execution state** to show live data

---

## Demo Scenarios (Once Complete)

### Scenario 1: HTTP Plugin Configuration

1. User clicks HTTP node in visual canvas
2. PropertyPanel opens with SchemaEditor
3. Smart URL input with validation
4. Dropdown for HTTP method (GET, POST, PUT, DELETE)
5. Boolean toggle for "Follow Redirects"
6. Real-time validation shows errors
7. onChange updates node attributes immediately

### Scenario 2: XPath Plugin with Expression Builder

1. User clicks XPath node
2. PropertyPanel shows XPath field
3. Click "Expression Builder" button
4. Builder shows available variables (httpResponse, etc.)
5. User types `${` → autocomplete appears
6. Click variable to insert
7. Live preview shows extracted value
8. Insert function: `substring(${httpResponse}, 0, 100)`
9. Preview updates in real-time

### Scenario 3: Regex Testing

1. User adds Regexp plugin
2. PropertyPanel shows regex field
3. Click "Test Regex" button
4. RegexValidator opens
5. Enter sample text
6. Regex highlights matches in real-time
7. Shows capture groups
8. Pick pattern from library (email, URL, etc.)

---

## Performance Considerations

### SchemaEditor:
- Lazy field generation (only visible fields)
- Debounced validation (300ms delay)
- Minimal DOM manipulation

### ExpressionBuilder:
- Preview evaluation debounced (150ms)
- Autocomplete triggered only on `${`
- Variable list virtualization (for 100+ variables)

---

## Technical Debt

None identified yet. Code follows TDD best practices with comprehensive test coverage.

---

## Conclusion

Phase 4 is progressing well with 2 out of 5 components complete. The SchemaEditor and ExpressionBuilder provide a solid foundation for advanced property editing. The remaining components (XPathTester, RegexValidator, DataPreviewPanel) will complete the advanced configuration panel experience.

**Next Session Goal:** Complete XPathTester and integrate Phase 4 components with PropertyPanel.

---

## Documentation References

- Plan: `/visual-designer-advanced-features.plan.md`
- Phase 1 Summary: `/VISUAL_DESIGNER_PHASE1_COMPLETE.md`
- Tests: `/webharvest-ide/frontend/src/visual/__tests__/`


