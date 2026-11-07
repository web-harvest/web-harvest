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
- 1341 tests execute after graph-cycle, XML whitespace/CDATA, and mixed-content regression coverage
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
chain. The current 1,342-test total includes one graph-layout regression test
covering those cases, two XML text/CDATA tests, one mixed-content round-trip
test, and one mixed-content clone-reference test.
The initial follow-up run lowered the failed-test ceiling from 262 to 255.
A later verified run on the current branch tip reduced the remaining failures
further; see the Issue #31 verification checkpoint below.


## Issue #32 — semantic XML example round-trip

The sweep now compares parsed XML structure rather than serialized formatting.
It preserves the source `config` wrapper attributes (including namespaces and
charset), ignores comments/XML declarations/attribute order/indentation-only
text and Visual Designer layout metadata, and retains meaningful text/CDATA.

The shared `VisualNode` contract now has optional `contentSequence` metadata
for mixed text/child ordering. The generator uses it only while its text and
child-ID snapshots still match the node; edited or reordered nodes fall back to
the established body-plus-children behavior. This avoids stale source metadata
overriding edits.

The Node 20 CI sweep verified all 26 examples: 26 passed, 0 mismatches.


## Issue #31 — next contract-migration pass

The verified run `42299758618` completed with 53 failed suites and 244 failed
tests (1,342 total tests). The failure ceiling was lowered from 255 to 244.

### Verification checkpoint — run 42302143349

Commit: `7412b0fa65efe1863e70f8af91e0f69584b53a08`.

- 132 suites executed: 51 failed, 81 passed.
- 1,342 tests executed: 213 failed, 1,129 passed.
- Jest gate passed against the then-current ceilings of 53 suites / 244 tests.
- Frontend type-check and Vite build passed.
- Shared package builds and consumer smoke tests passed.
- Node 20 semantic XML sweep passed: 26 examples, 0 mismatches.
- IDE frontend authority guard passed.

Compared with run `42299758618`, this run reduced failures by 2 suites and
31 tests. No suites were skipped. The Jest process used `--detectOpenHandles
--forceExit` to make open-handle diagnostics explicit.

The checked-in ratchet is now reduced to the observed 51 failed suites / 213
failed tests. This is still a non-zero baseline, not a green Jest suite.

### Remaining failure clusters from run 42302143349

- Stale VisualDesigner test APIs: `connectNodes`, `getConnections`, and
  `exportModelForLLM` are not current public methods; current equivalents
  include `createManualConnection`, `getManualConnections`, and the
  `VisualSemanticModelManager` model/LLM-format methods.
- Incomplete test doubles: `ConnectionRenderer` / model tests still supply
  plugin-manager doubles without `findPlugin` or pass a null manager.
- Drag-handler fixtures assign to the read-only `svgElement` getter.
- Layout/UI expectations still encode old geometry or display contracts
  (manual-node positions, swim lanes, sidebar visibility, SVG identity).
- Some App and PropertyPanel tests still need fixture/API migration.

Next: migrate one failure family at a time against current source contracts,
run the focused suites, then rerun the full workflow. Do not lower the ratchet
again unless a new full Jest run proves the lower counts.


## Issue #31 — migration unit A: UIManager display-mode tests

### Evidence from CI run 42318210798

The `UIManager` suite had three failures with distinct causes:

1. `showEditor(true)` and `switchToXMLMode()` expected `display: block`, while
   the current production contract deliberately configures the editor as a flex
   layout (`display: flex`) with flex sizing.
2. `switchToVisualMode()` schedules canvas activation after 10ms so the editor
   can finish hiding before canvas measurement. The test asserted synchronously
   before that transition had run.

### Change in this unit

- Align the two editor visibility expectations with the current flex-layout
  contract; production code is unchanged.
- Use Jest fake timers and advance exactly 10ms in the visual-mode test, making
  the asynchronous transition deterministic rather than relying on wall-clock
  sleeps.
- Preserve assertions for editor visibility, canvas visibility and plugin
  sidebar visibility.

### Verification status

The change is pushed on `stabilization/issue31-uimanager-contract`.
Focused Jest and the full TypeScript Validation workflow are pending. Do not
count these failures as resolved until the CI output confirms the suite passes.
