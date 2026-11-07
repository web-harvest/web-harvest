# IDE browser E2E (Playwright)

## Purpose

Playwright is a separate browser-level test layer for the IDE frontend. Jest remains the
unit/integration test runner. Browser tests cover the real browser DOM, layout, SVG rendering
and user interactions.

## Current harness

- Runner: `@playwright/test` (pinned direct dependency).
- Browser gate: Chromium only.
- Local command: `npm run test:e2e`.
- Configuration: `webharvest-ide/frontend/playwright.config.ts`.
- Tests: `webharvest-ide/frontend/e2e/**/*.spec.ts`.
- Vite is started by Playwright on `127.0.0.1:4173`.
- Backend plugin metadata and runtime-version endpoints are stubbed by the browser fixture.
- External HTTPS assets are blocked; Monaco's public editor API is stubbed for deterministic
  startup without downloading the third-party editor distribution.
- The optional backend WebSocket is stubbed as an offline connection so the browser fixture
  does not depend on a local backend or produce Vite proxy retries.
- On failure, Playwright retains trace, screenshot and video; CI uploads the report and
  test-results directory.

## HTML and bootstrap contract

The canonical HTML shell is
`webharvest-ide/src/main/resources/webapp/index.html`. It is the production document paired
with the Maven/runtime application and owns the required IDE DOM structure.

Vite development serves that same shell through the `webharvest-runtime-html` plugin. The
plugin changes only the production bundle references:
- `dist/assets/main.js` becomes the Vite source entry `/src/main.ts`;
- the production `dist/assets/main.css` link is removed because `main.ts` imports the
  frontend stylesheet directly.

The production HTML file is not copied or separately maintained for tests. Production bundle
paths, Vite build output and Maven/runtime HTML remain unchanged.

The Vite development server also serves the existing `webapp/static/**` tree at its original
`/static/**` URL prefix. The middleware confines resolved paths to that static directory and
does not expose arbitrary filesystem paths.

## Browser coverage

The current Chromium test verifies:
1. The canonical runtime shell is served, including the real canvas, plugin palette and right
   sidebar containers (there is intentionally no `#app` mount in the runtime shell).
2. The existing `/static/css/enterprise.css` URL resolves through Vite.
3. `App.initialize()` completes and the visual canvas becomes visible.
4. The canvas has non-zero browser layout dimensions.
5. A real toolbar click toggles the plugin sidebar and changes its visible state.
6. No uncaught browser page errors occur during this startup path.

Backend metadata is stubbed as an empty plugin catalog. The test does not fake the canvas DOM
or instantiate VisualDesigner through a test-only path.

The suite also loads a deterministic two-node root workflow through the existing
`window.app.visualDesigner.loadNodes()` API and verifies that the live D3 renderer emits an
SVG `path.connection-path` and a matching `polygon.connection-arrow` with non-empty path data.
The test waits for the semantic-model/anchor-registration lifecycle through observable DOM
output rather than fixed sleeps.

The suite also exercises node selection through the rendered canvas UI. Clicking the node
header label applies the selected-node styling, opens the real right sidebar, activates the
Properties section and renders the selected node's type in the PropertyPanel.

The suite also verifies that a real pointer drag beyond the movement threshold still repositions
a node after the click/drag lifecycle fix.

The PropertyPanel edit test changes a node attribute through the rendered editor, verifies
Cancel restores the original value without mutating the VisualDesigner model, then applies a
second value and verifies the App callback persists it through `VisualDesigner.updateNode()`.
It also verifies the generated XML is synchronized to both the active tab content and the
Monaco editor value through App's existing `syncVisualToXML()` path. Dirty-state transitions
are asserted through the actual Apply button.

Connection editing and broader plugin-schema validation remain separate coverage units.

## Reproducibility

The frontend now has a committed npm lockfile (`webharvest-ide/frontend/package-lock.json`,
lockfileVersion 3). CI generated it, inspected the local shared-package links and pinned
Playwright version, and verified it with a clean `npm ci`.

The root `.gitignore` retains the general package-lock ignore rule with a narrow exception
for this frontend lockfile only. The frontend validation and Playwright jobs use `npm ci`,
with dependency caching keyed by the lockfile.

## CI

The `IDE browser E2E (Playwright, Chromium)` job is separate from the existing frontend Jest,
type-check and build job. It installs Chromium and uploads Playwright diagnostics even when
tests fail. It is not a substitute for the existing Jest ratchet or XML semantic sweep.

Firefox/WebKit and required-merge-check status remain deferred until Chromium coverage is
stable and independently reviewed.
