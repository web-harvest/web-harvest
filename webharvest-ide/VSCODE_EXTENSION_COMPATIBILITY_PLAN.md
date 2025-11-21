# VS Code Extension Compatibility Plan

This plan organizes the work required to make the Visual Designer embeddable inside a future Visual Studio Code extension. It consolidates requirements from `VISUAL_DESIGNER_NEXT_PHASE.md`, `XML_VISUAL_MAPPING_SPEC.md`, and `TYPESCRIPT_MIGRATION_PROGRESS.md`, and enumerates concrete repository changes.

---

## 1. Objectives
- Reuse the existing visual-designer experience inside a VS Code WebView without behavioural differences.
- Guarantee lossless XML ⇄ visual conversions so desktop IDE and VS Code stay in sync.
- Expose a stable metadata+schema contract that third parties (including the VS Code extension) can consume.
- Maintain accessibility, keyboard shortcuts, and validation behaviours mandated by VS Code guidelines.

---

## 2. Backend / API Deliverables
1. **Schema Annotations**
   - Extend `webharvest-core` annotations (`@UiField`, `@UiTool`, `@ExpressionCapable`, etc.) to describe:
     - Field grouping/order, control type, placeholder/description, validation patterns, localization keys.
     - Plugin-level palette grouping, default node sizing, recommended child ordering.
   - Update `PluginMetadataCatalog.extractSchemaExtensions()` and `buildConstraintMap()` to emit new metadata blocks.

2. **Versioned Metadata Endpoint**
   - Modify `PluginMetadataServlet` (`webharvest-ide/src/main/java/org/webharvest/server/api/PluginMetadataServlet.java`):
     - Add `visual.apiVersion` (e.g., `2.0`) and `visual.capabilities` array.
     - Surface schema extensions under first-class keys (`schema.attributeDefinitions`, `ui`, `validation`, `tools`).
     - Keep legacy consumers working via `extensions.*` shim.
   - Document the JSON schema snippet in `DEVELOPER_GUIDE.md` and create a snapshot test (`PluginMetadataServletTest`).

3. **Backend Tests**
   - Add `PluginMetadataCatalogTest` (round-trip annotations ➝ catalog JSON).
   - Add servlet-level snapshot test verifying payload equality (sample descriptor fixture).

---

## 3. Frontend / Shared Packages
1. **Type Definitions**
   - Extend `frontend/src/types/index.ts`:
     - `AttributeUiMetadata`, `ValidationRule`, `LocalizationToken`, `UiTool`, `DetailConstraint`.
   - Update `PluginManager.normalisePluginMetadata()` to preserve unknown keys, gate new logic on `visual.apiVersion`.

2. **Schema-Driven Property Panel**
   - Create `frontend/src/visual/editors/fields/` with `TextField`, `TextareaField`, `EnumField`, `ToggleField`, `ExpressionField`, `NumberField`, `JsonField`.
   - Implement `PropertyFieldFactory` and `SchemaDrivenEditor` inside `frontend/src/visual/PropertyPanel.ts`.
   - Provide `PropertyPanelShell` abstraction with keyboard/focus rules matching VS Code (Ctrl/Cmd+Enter apply, Esc cancel).
   - Expose these components via a new shared package (`frontend/src/shared/visual-editor/`) so a VS Code WebView bundle can import them.

3. **XML Round-Trip Module**
   - Finalize `XMLGenerator` (pending per `TYPESCRIPT_MIGRATION_PROGRESS.md`) so parser and generator live in a shared TS module (`frontend/src/shared/xml-roundtrip`).
   - Add Jest round-trip tests using fixtures from `webharvest-ide/src/main/resources/webapp/examples/`.

4. **Legacy Import Toolkit**
   - Implement `frontend/src/visual/legacyMappings.ts` and parser upgrade hooks to coerce deprecated elements on load.
   - Surface compatibility warnings via a reusable notification API (IDE and VS Code extension share identical messaging).

### 3.1 Shared Package Extraction (Scope · 2025‑11‑21)
| Package | Entry Point | Responsibilities | Notes |
| --- | --- | --- | --- |
| `packages/visual-shared/metadata-client` | `metadataClient.ts` | Fetch `/api/plugins/metadata`, normalise payload (version guard, `visual` envelope), expose typed hooks for VS Code + IDE | Re-use existing `PluginManager` logic; emit ESM + CJS builds via Vite library mode |
| `packages/visual-shared/xml-roundtrip` | `index.ts` | `XMLParser`, `XMLGenerator`, sanitizer helpers, legacy notices, sweep harness utilities | Parser currently depends on DOM APIs; bundle `linkedom` fallback for Node/VSC |
| `packages/visual-shared/property-panel` | `index.tsx` | `PropertyPanelShell`, `SchemaDrivenEditor`, field registry, CSS tokens | Export CSS variables + design tokens; keep advanced tool adapters optional |

Implementation steps:
1. Create `packages/visual-shared/*` workspaces, hoist build tooling (tsconfig paths, Vite lib config).
2. Move non-DOM helpers (schemas, metadata normalizers, field factory) into packages; keep IDE-only shims thin.
3. Document consumption contract in this plan + `DEVELOPER_GUIDE.md` (how extensions import, bundle CSS, register tooling).
4. Add sample usage to future VS Code harness before publishing packages to npm feed.

---

## 4. VS Code Extension Contract
1. **WebView Message Protocol**
   - Define `visualDesigner.postMessage` schema: `LOAD_METADATA`, `LOAD_XML`, `SAVE_XML`, `APPLY_CHANGE`, `VALIDATION_RESULT`, `STATUS`.
   - Document protocol in this file and later in `docs/visual-designer/vscode.md`.

2. **Resource Packaging**
   - Build a lightweight bundle (Vite library mode) containing:
     - Metadata client (`/api/plugins/metadata` adapter).
     - XML parser/generator utilities.
     - Schema-driven property panel components and styling tokens (CSS variables compatible with VS Code themes).

3. **Accessibility & Theming**
   - Align CSS tokens with VS Code theming variables (`--vscode-editor-foreground`, etc.).
   - Ensure ARIA labels/roles come from metadata (`AttributeUiMetadata.ariaLabelKey`).

4. **Prototype Harness**
   - Create an integration harness using `@vscode/test-web` that spins up a mock WebView, loads the shared bundle, and runs smoke tests (open config, edit property, validate, export XML).

---

## 5. Testing Strategy
- **Unit:** Metadata normalization, field factory, XML round-trip, legacy mapping conversions.
- **Integration:** WebView harness tests, Jest DOM tests for keyboard/focus behaviour, backend servlet snapshot tests.
- **E2E:** Headless browser test verifying property edits propagate to XML and survive undo/redo; later reuse Playwright inside VS Code.
- **Tooling:** New scripts (`scripts/sweep_examples.ts`) and CI jobs to validate example XML files with the shared round-trip module.

---

## 6. Rollout Sequence
1. Backend annotation + servlet versioning (Sprint 1).
2. Type definitions + metadata normalization (Sprint 1 overlap).
3. Schema-driven property panel + field registry (Sprint 2–3).
4. XML generator + shared round-trip tests (Sprint 2).
5. Legacy compatibility toolkit + sweeper script (Sprint 3).
6. Shared packages + VS Code WebView prototype (Sprint 4).
7. Integration/documentation updates (`DEVELOPER_GUIDE.md`, `USER_GUIDE.md`, new VS Code guide).

---

## 7. Open Questions
- Minimum VS Code version and WebView API baseline?
- Do we ship the extension with an embedded backend, or rely on users running the IDE server locally?
- How do we authenticate `/api/plugins/metadata` when served remotely inside VS Code?
- Should the metadata contract include localization bundles, or will the extension provide its own?

Collect answers before Sprint 3 to avoid rework.

