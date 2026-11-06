# TypeScript validation architecture (Issues #25–#27)

## Decisions

### A. `@webharvest/metadata-client` and `@webharvest/xml-roundtrip` — real extraction

Packages own their implementations and type contracts. They no longer re-export
from `webharvest-ide/frontend/src`.

| Package | Owns | Depends on |
|---|---|---|
| `@webharvest/metadata-client` | `PluginNormalizer`, `SchemaMapper`, plugin metadata types, `PluginLookup` | none |
| `@webharvest/xml-roundtrip` | `XMLParser`, `XMLGenerator`, `legacyMappings`, `xmlSanitizer`, `VisualNode` | `@webharvest/metadata-client` (`PluginLookup` only) |

Public exports are the package `exports` map pointing at `dist/`. Each package
has an explicit local tsconfig (`target`/`lib` ES2020, `module`/`moduleResolution`
Node16, `rootDir` `src`). Frontend consumes the packages via `file:` dependencies
and path/Vite aliases; legacy relative import sites remain as thin re-export
shims.

Rejected: widening `rootDir`, inheriting the frontend tsconfig, or keeping
facade-only packages that reach into the IDE tree.

### B. Jest / D3 — CJS transform of the ESM-only D3 closure

Keep Jest + ts-jest in CommonJS mode. Transform `d3`, `d3-*`, `internmap`,
`delaunator`, and `robust-predicates` via `transformIgnorePatterns`. Do not
globally mock D3. Native ESM Jest remains a viable alternative but costs more
test-runtime changes; see `docs/FRONTEND_JEST_TRIAGE.md`.

### C. IDE workspace — single authoritative frontend

`webharvest-ide/frontend` is the product frontend. The root Vite/`tsc` workspace
targeting deleted `visual-designer/` sources is retired. CI validates the
frontend and shared packages, not a second product type-check at the module root.
See `docs/IDE_FRONTEND_WORKSPACE.md`.

## Documented limitations (accepted for this unit)

- **Resolution split:** Vite/`tsc` resolve `@webharvest/*` to package `src/` via
  path aliases; Jest resolves to compiled `dist/` (Node16 `.js` import
  specifiers). `pretest`/`prebuild` rebuild packages first.
- **Jest ratchet is count-based:** CI fails on D3 parse errors, suite/test total
  drift, or failed-count increases above `jest-known-failures.json`. It does
  not name individual failing tests; Issue #26 classification for remaining
  failures is class-level in `docs/FRONTEND_JEST_TRIAGE.md`, not per-test.
- **Lockfiles:** root `.gitignore` still ignores `package-lock.json`; installs
  remain unpinned (pre-existing).
- **Public API surface:** package `export *` re-exports helpers needed by
  frontend shims; narrowing to an explicit export list is follow-up work.
- **`sweep:examples`:** now runs as a Node 20 CI job. It compares semantic
  XML structure across all 26 examples, preserving wrapper attributes,
  namespaces, meaningful text/CDATA, and mixed-content order while ignoring
  comments, declarations, attribute order, layout metadata, and indentation.
  All 26 examples pass the semantic round-trip gate.
- **Node skew:** shared-package and XML sweep jobs use Node 20; frontend Jest
  job uses Node 18.19 (product Maven frontend version).
