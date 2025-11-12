## Run & Test Workflow Panel — Final UX Specification

### Placement & Navigation
- Appears as the **`Run & Test`** tab in the right-hand sidebar, adjacent to `Properties`, `Logs`, `Results`, `Variables`, and `Session`.
- The sidebar nav shares a consistent Apple HIG segmented style; the Run tab can be activated through mouse or keyboard (Arrow keys + Enter).
- Canvas now spans the full width between palette and sidebar, removing the previous floating overlay.

### Panel Anatomy

```
┌ Run & Test ────────────────────────────────────────────────┐
│ hint: “Run immediately…”                                   │
│                                                            │
│ Workflow input (JSON)        [ Format ] [ Clear ]          │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ { }                                                   │ │
│ └───────────────────────────────────────────────────────┘ │
│                                                            │
│ ▶ Advanced options ▸                                        │
│   (correlation id / idempotency key / task→domain JSON)     │
│                                                            │
│ [▶ Run Workflow]  [✓ Validate]        [Pause][Resume][Stop] │
│                                                            │
│ History                          [Clear]                   │
│ • 12:41 Completed (workflow)    [Restore]   [Run]          │
│ • 11:08 Failed (task http)      [Restore]   [Run]          │
└────────────────────────────────────────────────────────────┘
```

### Behaviour Details
- **Quick Actions**
  - `Run Workflow` and `Validate` live next to each other in the header, adopting the Run panel styling used elsewhere in the IDE.
  - Execution scope toggles (`Workflow` / `Selected Task`) sit beside the title; task scope is auto-disabled until a node is selected.
  - Pause/Resume/Stop buttons appear only when an execution is in progress (mirrors existing execution state transitions).

- **Form Inputs**
  - JSON textarea always visible with `Format` (pretty-print) and `Clear` shortcuts.
  - Advanced metadata is housed in a `<details>` element. When closed, novices see only the essentials; power users can expand to edit correlation/idempotency keys and the task→domain mapping.
  - JSON validation errors display inline, directly beneath the header in a red banner.

- **History**
  - Per-tab history with newest-first order. Each entry shows timestamp, scope badge, terminal status, and two affordances:
    - `Restore` rehydrates form fields without running.
    - `Run` executes the stored payload immediately (reuses scope + selected node snapshot).
  - History persists through IDE reloads via `localStorage` (keyed by `RUN_PANEL_STORAGE_PREFIX + tabId`); it is purged when a tab is closed or the user clicks `Clear`.

- **Novice vs Expert Flow**
  - *Novice*: sees hint text, pastes JSON, hits Run/Validate, reads history status chips — no exposure to correlation/idempotency.
  - *Expert*: expands Advanced options, tunes metadata, replays targeted task runs, and inspects stored payloads before re-running.

- **Accessibility**
  - Summary arrow rotates to indicate drawer state; `<details>` element is focusable and keyboard togglable.
  - History entries are buttons inside list items, enabling Arrow Up/Down navigation plus Enter/Space activation.
  - All actionable controls include ARIA labels and tooltips (e.g., “Run Workflow (⌘R)”).

### Data Model & Persistence
- `RunPanelState` persists per tab:  
  ```ts
  {
    inputJson: string;
    correlationId: string;
    idempotencyKey: string;
    taskDomainJson: string;
    history: RunHistoryEntry[]; // capped (default 10) for perf
  }
  ```
- History entries track `scope`, `payload`, `selectionContext`, `createdAt`, and the terminal `status` fed from WebSocket updates.
- Local storage is flushed when a tab is manually closed or the history is cleared.

### API & Component Interactions
- `RunWorkflowPanel` invokes `App.handleRunFromPanel()` for Run, `App.validateConfig()` for Validate, and delegates execution state updates to `UIManager`.
- Terminal statuses flow from `WebSocketManager.onUpdate` to `App.updateRunHistoryStatus`, updating the latest entry’s badge/metadata.
- Node selection changes call `runPanel.updateSelection […]` so the task scope toggle reflects current context.

### Testing Notes
- `App.executionControls.test.ts` covers:
  - Sidebar mounting.
  - Execution state toggles.
  - History restore + rerun.
- `UIManager.test.ts` verifies visibility toggles between XML and Visual mode (Run tab hidden in XML).
- Additional coverage lives in `RunWorkflowPanel` unit tests (input sync, advanced drawer, history rendering).

### Future Considerations
- Add JSON minify/copy actions to complement existing Format/Clear.
- Persist drawer open/closed state per user.
- Explore remote history syncing once backend exposes execution archives.
