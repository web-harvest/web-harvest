# IDE frontend Jest — D3 ESM triage (Issue #26)

## Strategy (evidence-backed)

Jest runs in CommonJS mode with ts-jest. D3 v7 and its ESM-only closure
(`d3`, `d3-*`, `internmap`, `delaunator`, `robust-predicates`) are transformed
via an explicit `transformIgnorePatterns` allow-list. No global D3 mock.
No `--experimental-vm-modules` requirement.

Native ESM Jest (ts-jest `default-esm` + Node VM modules) also clears the D3
parse error but requires Jest global shims, writable `ResizeObserver` stubs,
and changes to `jest.mock` / `require` semantics. The CJS transform preserves
existing test mechanics with a config-only change.

## Baseline before fix (CI run 42238688660 / local repro)

- 132 suites: 62 failed, 70 passed
- 932 tests: 75 failed, 857 passed
- Root error: `SyntaxError: Unexpected token 'export'` from `d3` ESM entry

## After D3 transform (local + CI evidence)

- 132 suites load; D3 parse errors: 0
- 1341 tests execute after adding four graph-layout regression cases (previously masked suites now run)
- Remaining failures: **53 suites / 262 tests** (CI run on `stabilization/ts-validation-25-26-27`)
- CI ratchet: `webharvest-ide/frontend/jest-known-failures.json` fails the job if
  failed counts rise or suite/test totals drift

## Failure classes (product / test contract — not D3)

| Class | Evidence | Action |
|---|---|---|
| Legacy VisualDesigner / ConnectionManager APIs | Calls to removed helpers (`getConnectionAnchor`, `addConnection`, …) | Migrate tests to current APIs |
| Incomplete PluginManager doubles | Missing `findPlugin` / `getSchemaVersion` | Complete fixtures or narrow adapters |
| jsdom platform gaps | `fetch`, `SVGPathElement`, `ResizeObserver is not defined`, layout geometry | Explicit shims / deterministic fixtures |
| UI/layout contract drift | Old sidebar / panel expectations; `Cannot set property svgElement` on mocks | Align fixtures to current UI |
| Product defect fixed in Issue #30: cyclic layout graph caused unbounded recursion in `computeLongestPath` | Iterative SCC condensation + longest-path layering; DAG layering preserved and cycles share a layer |

These classes are tracked as follow-up work. This unit does not skip, exclude,
or weaken suites to force a green count. The CI job is an infrastructure +
ratchet gate, not a claim that all assertions pass.


## Issue #30 — cycle-safe graph layering

The auto-layout layer calculation now condenses strongly connected components
before computing longest-path layers. Nodes in one cycle share a layer; edges
between components are layered on the resulting DAG. Both graph traversals are
iterative, so large workflows do not depend on the JavaScript call-stack limit.

Regression coverage includes branching/converging DAGs, a two-node cycle with
incoming and outgoing edges, self-loops, disconnected nodes, and a 2,500-node
chain. The Jest test-count expectation is updated from 1,337 to 1,341; the
failure ceilings remain unchanged at 53 suites / 262 tests.
