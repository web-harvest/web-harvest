# Visual Designer – Next Phase Implementation Guide

This document consolidates the current findings from the metadata/IDE audit and outlines concrete follow-up work needed to make the Visual Designer production-ready. It covers four areas aligned with the approved plan.

---

## 1. Metadata Flow Audit (Back → Front)

| Stage | Responsibility | Current State | Gaps / Follow-ups |
| --- | --- | --- | --- |
| Plugin reflection (`webharvest-core/src/main/java/org/webharvest/plugin/metadata/PluginMetadataCatalog.java`) | Generates `PluginDescriptor` objects with schema + semantic info | Captures attribute definitions, allowed children, namespace, `@MasterDetailConstraint` annotations. `extractSchemaExtensions()` already outputs `schema.attributeDefinitions[].{name,type,required,defaultValue,allowedValues,minValue,maxValue,pattern}` | Missing grouping hints, i18n labels, field control types (e.g. regex, multi-line text), validation metadata beyond scalar ranges. Need new annotations/interfaces (e.g. `@UiField(group="HTTP", kind=TEXTAREA, localizationKey="plugin.http.url")`, `@UiTool(ExpressionBuilder.class)`). Also need plugin-level metadata for palette grouping, default dimensions, recommended child order. |
| API surface (`webharvest-ide/src/main/java/org/webharvest/server/api/PluginMetadataServlet.java`) | Flattens descriptors into `/api/plugins/metadata` response | Passes through semantic roles, schema, master-detail constraints, UI hints (`visual` block) | Should expose schema extensions explicitly (currently nested under `extensions.schema`). Add `visual.apiVersion`, `visual.capabilities` and move sanitized UI hints (icons, palette groups, display labels, VS Code hints). Provide backwards-compatible `extensions` for older IDEs. |
| Frontend ingest (`webharvest-ide/frontend/src/core/PluginManager.ts`, `frontend/src/types/index.ts`) | Normalizes metadata and caches `Plugin` objects | Converts attribute types to `string | number | boolean | url | xpath | enum`; merges semantics into schema; coerces missing booleans | Normalizer strips unknown attribute keys (e.g., planned `ui` or `validation`). Types need `AttributeUiMetadata` (fieldKind, format, placeholder, helperText), `ValidationRule` (pattern, min/max length), `LocalizationToken`, and `DetailConstraint` fields. Introduce version guard: fallback to legacy heuristics when metadata lacks new keys. |
| UI consumers (`frontend/src/visual/VisualDesigner.ts`, `frontend/src/visual/PropertyPanel.ts`, `frontend/src/visual/detail/MasterDetailRegistry.ts`) | Render palette, property editors, master-detail lists | Palette uses `plugin.ui?.group`, `icon`, `category`; PropertyPanel still uses specialized editors (HTTP, XPath) coded inside file; MasterDetailRegistry partially metadata-driven but depends on overrides for headers/params | Need unified metadata-to-component pipeline: no plugin-specific editors unless flagged by metadata. Provide schema-driven renderer with per-field component mapping + validation derived from backend schema extensions, and ensure master-detail overrides replaced by metadata-sourced section templates. |

**Action Items**
1. Extend backend schema annotations (e.g. `@UiField(type=TEXTAREA, group="HTTP Request", order=20)`, `@ExpressionCapable`) and update `PluginMetadataCatalog.extractSchemaExtensions()` plus `buildConstraintMap()`.
2. Version `/api/plugins/metadata` payload (`visual.apiVersion` field already present) and document new keys in `DEVELOPER_GUIDE.md`.
3. Update `frontend/src/types/index.ts` + `PluginManager.normalisePluginMetadata()` to preserve new metadata (avoid silently coercing unknown fields to `string`).
4. Add tests: backend round-trip (new `PluginMetadataCatalogTest`), servlet JSON snapshot (`PluginMetadataServletTest`), frontend unit verifying normalization of `AttributeUiMetadata`.

**Status · 2025‑11‑21**
- `webharvest-core` exposes the new annotations (`@UiField`, `@UiTool`, `@ExpressionCapable`, enriched `@MasterDetailConstraint`), and `PluginMetadataCatalog` now emits ui blocks + validation metadata consumed by the IDE/VS Code pipeline.
- `/api/plugins/metadata` ships a versioned `visual` envelope with capability flags; `PluginManager` and `SchemaDrivenEditor` persist the metadata without lossy coercion.
- Follow-up: backend snapshot tests still to be written before opening up the contract to external consumers.

---

## 2. Property Panel & Detail Editing Enhancements

### Goals
- Schema-driven, componentized editor generation (React-like architecture but still DOM for now).
- Unified validation pipeline (show errors inline, sync with semantic validator).
- Ready for VS Code WebView embedding (no reliance on global browser-only APIs; encapsulate styles).
- Deprecate bespoke editors embedded inside `PropertyPanel.renderNodeForm()` by introducing composable field widgets consumed by both IDE and future VS Code extension.

### Proposed Architecture
1. **Field Registry** (`PropertyFieldFactory`)  
   - Maps attribute metadata → editor component (`text`, `textarea`, `enum select`, `boolean switch`, `expression builder`, `code editor`, `number slider`).  
   - Input metadata from backend: `type`, `ui.fieldKind`, `ui.requiresExpressionBuilder`, `validation.pattern`.  
   - Output: DOM subtree w/ `onChange`, `onValidate` callbacks.
   - Implementation detail: create `frontend/src/visual/editors/fields/index.ts` exporting `createField(fieldConfig, initialValue, callbacks)`; include instrumentation hooks for analytics (field usage metrics).

2. **Schema Renderer** (`SchemaDrivenEditor`)  
   - Iterates over attribute groups (new metadata: `group`, `order`, `description`).  
   - Renders `MasterDetailEditor` for child collections using constraints (already partially implemented).  
   - Switch between **Sidebar** and **Modal** shells via configuration flag (to evaluate VS Code-friendly dialog approach).  
   - Provide slot for plugin-specific advanced panels (HTTP inspector, XPath tester) but mount them lazily and only when metadata marks `ui.tools=['httpInspector']`.

3. **Validation Layer**  
   - Local checks (required, regex, allowedValues).  
   - Live integration with semantic validator (`VisualDesigner.validateNode`) to highlight conflicts between fields.  
   - Display summary banner + per-field hints.  
   - Emit typed change events so undo/redo history stays consistent.

4. **State & Accessibility**  
   - Maintain form state separately from `VisualNode` until user confirms (Apply/Cancel) when in dialog mode.  
   - Keyboard-first navigation, ARIA labels generated from metadata.  
   - Provide `PropertyPanelShell` abstraction with consistent focus management, ESC/Enter shortcuts, and responsive layout breakpoints (3-column desktop vs modal on narrow widths).
   - Mirror VS Code conventions: `Cmd/Ctrl+Enter` to apply, `Esc` to cancel, tab order restricted to panel container, `aria-live` regions for validation errors.

### Deliverables
- Extend `frontend/src/types/index.ts` with `AttributeUiMetadata`, `ValidationRule`, `UiTool`.
- Build `PropertyFieldFactory` + base components in `frontend/src/visual/editors/` (`TextField`, `ToggleField`, `EnumField`, `ExpressionField`, `JsonField`, `NumberSpinner`).
- Introduce `SchemaDrivenEditor` to `frontend/src/visual/PropertyPanel.ts` and migrate `HttpEditor`, `XPathTester`, etc. into opt-in tools invoked when metadata contains `ui.tools`.
- Update tests (`frontend/src/visual/__tests__/PropertyPanel.*`) to assert dynamic rendering, constraint enforcement, disabled states when `maxOccurs` reached.

**Status · 2025‑11‑21**
- `PropertyPanelShell.ts` now wraps every edit session with keyboard traps, ESC/Cmd+Enter handling, and Apply/Cancel buttons, while `SchemaDrivenEditor` + the new field factory render metadata-driven controls (expression/XPath/regex tools, validation summaries, grouped sections).
- Legacy hard-coded editors (HTTP, XPath, Regex) are invoked via metadata decorators only when needed; remaining bespoke panels can be migrated incrementally.
- Next: add focused Jest coverage plus shared export so the VS Code WebView can reuse the same shell/field registry.

---

## 3. Legacy XML Compatibility & Example Strategy

### Current Observations
- Parser/Generator keep `data-width`/`data-height` attributes for IDE layout; now sanitized before validation (`xmlSanitizer`).  
- Legacy pseudo-plugins (`body`, `search_results`, etc.) removed from curated examples, but importer still cannot gracefully ignore unknown elements.  
- Examples live in `webharvest-ide/src/main/resources/webapp/examples/`; migrations done manually.
- `XMLParser` currently trusts DOM children blindly; unknown tags simply become `VisualNode` objects, which later fail validation. Need ability to flag or auto-convert.
- `VisualDesigner.getNodes()` returns flat list without tracking original namespace/prefix usage, so converter must normalize to `http://org.webharvest/schema/2.1/core` before validation.

### Proposed Work
1. **Import Pipeline Enhancements**
   - Add upgrade hooks in `XMLParser` to detect legacy constructs and translate them to supported nodes (e.g., `<body>` → proxy container, `<search_results>` → `<template>`).  
   - Maintain map defined in `frontend/src/visual/legacyMappings.ts` to keep logic centralized (structure: `{ legacyName: { replacement: 'template', transform(node) { ... } } }`).  
   - Warn user via toast/log when coercions happen and persist summary into Run Panel history for traceability.

2. **Export Guards**
   - `XMLGenerator` should omit empty optional attributes, enforce canonical ordering, and optionally inject comments stating the IDE version.  
   - Provide flag to keep layout metadata when exporting for round-trip editing, but default to clean schema output for validation/execution (`XMLGenerator.generate({ includeLayout: false })`).
   - Add regression tests in `frontend/src/visual/__tests__/XMLGenerator.test.ts` to ensure sanitized output matches schema expectations (no `data-*` attributes, deduped whitespace).

3. **Example Hygiene Tooling**
   - Script (`scripts/sweep_examples.ts`) to load each XML, run through parser/generator, diff results, and confirm schema validation.  
   - Document allowed patterns in `USER_GUIDE.md` / `DEVELOPER_GUIDE.md` to discourage reintroduction of ancient constructs.  
   - Add CI job that runs the sweeper + `/api/config/validate` for all examples, surfacing diffs in PR comments.
   - Provide per-example metadata (JSON) describing scenario + expected outputs to assist automated smoke tests. Useful for new VS Code extension as well.

4. **Backward Compatibility Mode**
   - When loading third-party configs, offer “Compatibility Report” summarizing unsupported plugins + suggested replacements.  
   - Could be served by backend endpoint using `SchemaValidator` to enumerate missing plugins/attributes along with recommended modern equivalents.  
   - In IDE, show modal summarizing automatic fixes + required manual actions before rendering on canvas.

**Status · 2025‑11‑21**
- Parser-level conversions (`legacyMappings.ts` + `XMLParser.getLegacyNotices()`) are live; `App.ts` logs each auto-fix and the new `scripts/sweep_examples.ts` CLI round-trips all bundled examples to surface mismatches.
- Upcoming work: surface the compatibility report UI and wire backend validation so VS Code/Web IDE share the same warnings.

---

## 4. Roadmap, Testing & Documentation

### Milestones
1. **Metadata Enrichment (Sprint 1)**
   - Backend annotations for UI hints + validation (core).  
   - `/api/plugins/metadata` v2 format + compatibility shim.  
   - Tests: `PluginMetadataCatalogTest`, `PluginMetadataServletTest`, snapshot of metadata schema, contract test verifying frontend `PluginManager` can parse sample payload.

2. **Schema-Driven Property Panel (Sprint 2–3)**
   - Field registry + renderer, conversion of top 10 plugins to new metadata.  
   - UX study: sidebar vs modal (A/B flag).  
    - Tests: Jest DOM tests for new components, Cypress (or Playwright) flow for editing nodes, regression test ensuring undo/redo works with schema-driven editor.

3. **Legacy Compatibility Toolkit (Sprint 3)**
   - Legacy mapping table, import warnings, example sweeper script.  
   - Tests: Parser round-trip cases (`frontend/src/visual/__tests__/XMLParser.legacy.test.ts`), backend validation for migrated XML, CLI sweeper snapshot to detect unexpected transformations.

4. **VS Code Alignment (Sprint 4)**
   - Extract shared UI package (CSS tokens, component primitives).  
   - Provide documentation for extension authors on consuming `/api/plugins/metadata`.  
   - E2E tests inside VS Code webview sandbox (prototype) plus contract tests ensuring metadata parity between IDE + extension.

### Documentation & Communication
- Update `DEVELOPER_GUIDE.md` with metadata annotation reference + property panel architecture, including sample annotation usage and JSON excerpt from `/api/plugins/metadata`.  
- Expand `USER_GUIDE.md` “Visual Designer” chapter describing new editing workflow, compatibility warnings, and how layout metadata behaves (with screenshots once available).  
- Maintain change log entries to help plugin authors adopt new annotations; add FAQ entry for “Why can’t I add legacy `<body>` nodes anymore?”.
- Create `docs/visual-designer/` micro-site with migration cookbook (legacy XML → modern nodes), metadata reference, and VS Code integration notes.

### Testing Strategy
- **Unit:** Backend metadata extraction, frontend field components, XML sanitizer utilities.  
- **Integration:** Example validation test suite (already exists) extended to cover legacy conversion scenarios.  
- **E2E:** Run IDE in headless browser to ensure property edits persist, master/detail constraints enforced, and compatibility banners display correctly.  
- **Tooling:** CI job to run `scripts/validate_examples.py` + new sweeper script on every PR touching examples or metadata.

---

## Next Steps Checklist
1. Design metadata annotations / schema extensions needed for field rendering (backend + doc).  
2. Prototype schema-driven property panel with one plugin (e.g., `http`) and measure UX vs modal.  
3. Build legacy mapping utilities + importer warnings with automated sweeper.  
4. Formalize roadmap in Jira (or equivalent) and align docs/tests accordingly; reference this document from `DEVELOPER_GUIDE.md`.


