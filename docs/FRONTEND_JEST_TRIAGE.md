# IDE frontend Jest recovery — triage

## Baseline evidence

The first successful ESM-enabled Jest execution on the recovery branch loaded all 132 suites:
- 76 suites passed, 56 failed.
- 1,059 tests passed, 276 failed.
- D3 v7 was loaded as ESM; the earlier CommonJS parse failure was gone.
- The frontend TypeScript type-check completed successfully.
- The two shared TypeScript wrapper packages compiled successfully.

These figures are the baseline run, not a claim about the latest in-progress run. Re-run the full workflow after each bounded fix and record the new result.

## Failure classes established from source and test inspection

| Class | Evidence | Correct next action |
|---|---|---|
| Legacy VisualDesigner API in tests | Several tests call `connectNodes`, `getConnectionAnchor`, `getConnectionPointFromModel`, `getNodeById`, or `calculateArrowAngle`. The current `VisualDesigner` exposes `findNode`, `ConnectionManager.createConnection`, and the current port/connection model; the source explicitly records the old anchor helper among removed helpers. | Migrate each affected test to the current model/manager APIs. Do not reintroduce retired methods solely to satisfy old tests. |
| DOM anchor lifecycle not represented in semantic-model tests | `VisualSemanticModelManager.buildConnectionStates` registers connections as pending when SVG/anchor elements are unavailable and skips creating their connection state until anchors are registered. Some tests construct a partial designer without an SVG or registered anchors, then immediately dereference `connectionState.anchors`. | Build realistic anchor fixtures and assert pending/recalculation behavior separately. |
| Obsolete ConnectionManager calls | `ConnectionManager` exposes `createConnection`; an integration test called nonexistent `addConnection`. That test was updated to use `createConnection`. | Search remaining tests for retired manager methods and migrate only where the current API contract is clear. |
| Incomplete PluginManager test doubles | Some fixtures implement `getPlugins`/legacy `getPlugin` but omit newer `findPlugin` or `getSchemaVersion`. | A narrow compatibility resolver was added to `VisualSemanticModelManager`; validate it against real and legacy fixtures. |
| jsdom environment assumptions | Tests assign `global.ResizeObserver`; the ESM setup now provides a writable stub. Some SVG suites also assume browser globals such as `SVGPathElement`. | Add only missing platform shims with explicit behavior and tests; do not fake layout dimensions. |
| UI/layout expectations from an older sidebar or browser geometry | Several tests expect old panel display modes or non-zero measured dimensions that jsdom does not provide. | Update fixtures to use the current `RightSidebar` structure and inject deterministic geometry where the behavior under test requires it. Do not loosen assertions without evidence. |

## Current scope

Already repaired in the recovery branch:
- Jest native ESM execution for D3.
- Explicit Jest ESM globals and writable ResizeObserver test setup.
- Shared TypeScript package builds against the active frontend tsconfig.
- Surefire/Failsafe lifecycle placement for the packaged-JAR tab-management test.
- Maven integration workflow now packages before invoking Failsafe.
- Initial test fixtures aligned with the current sidebar and SVG property contracts.
- Legacy PluginManager adapter compatibility.
- One obsolete `ConnectionManager.addConnection` call replaced with `createConnection`.

## Acceptance criteria for the next frontend unit

1. All 132 Jest suites execute under the real ESM configuration.
2. No suite is excluded, skipped, or weakened to obtain a green result.
3. Every migrated test exercises a current public API or a deliberately documented compatibility alias.
4. DOM/SVG tests provide explicit, deterministic fixtures for geometry and anchor registration.
5. Full Jest, type-check, and Vite build pass together in GitHub Actions.
6. Any intentionally obsolete test is rewritten around the current contract, not silently removed.

## Guardrails

- Keep this work on `recovery/frontend-ci-baseline` and draft PR #28.
- Do not merge, tag, release, or publish.
- Do not change `2.2.x`, `2.3.x`, or `master`.
- Preserve the distinction between a test-environment failure, an obsolete test contract, and a product defect.
