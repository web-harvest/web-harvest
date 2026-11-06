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
