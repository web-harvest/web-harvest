# VS Code WebView Harness – Prototype Plan

## Goals
- Spin up a lightweight VS Code WebView (via `@vscode/test-web`) that loads the schema-driven property editor + visual shared packages without the full IDE shell.
- Exercise the message bridge (`postMessage`) that will be shared between the eventual extension host and the WebView bundle (load metadata, sync XML, apply node edits, stream validation events).
- Automate smoke tests in CI (Chromium) so regressions are caught without manually building an extension VSIX.

## Architecture Overview
| Layer | Responsibilities | Notes |
| --- | --- | --- |
| Harness Host (`vscode-harness/src/host`) | Registers mock commands, wires `@vscode/test-web`, emulates extension-side APIs (`acquireVsCodeApi`, telemetry stubs). | Runs inside Node; publishes fixtures (metadata, XML) to the WebView. |
| WebView Bundle (`vscode-harness/src/webview`) | Imports shared packages (`metadata-client`, `xml-roundtrip`, `property-panel`), renders a stripped property panel and handles host messages. | Built via Vite library mode → served to `@vscode/test-web`. |
| Message Bus (`webharvest-ide/docs/VSCODE_WEBVIEW_HARNESS.md#message-contract`) | Formal contract for `LOAD_METADATA`, `LOAD_XML`, `APPLY_NODE_CHANGE`, `VALIDATION_RESULT`, `STATUS`. | Spec aligns with `/api/plugins/metadata` v2 payload + future VS Code extension. |

## Message Contract
| Direction | Command | Payload | Description |
| --- | --- | --- | --- |
| Host → WebView | `LOAD_METADATA` | `{ apiVersion, plugins }` | Send normalized metadata snapshot (mirrors `/api/plugins/metadata`). |
| Host → WebView | `LOAD_XML` | `{ xml, selection? }` | Load a configuration + highlight selection. |
| WebView → Host | `APPLY_NODE_CHANGE` | `{ nodeId, attributes, body }` | Send node edits back to the host (host persists / re-validates). |
| WebView → Host | `REQUEST_METADATA` | `{}` | WebView asks host for latest metadata (host responds with `LOAD_METADATA`). |
| Both Ways | `STATUS` | `{ level: 'info' | 'error', message }` | Simple heartbeat/log channel displayed in harness UI. |
| WebView → Host | `VALIDATION_RESULT` | `{ nodeId?, errors: ValidationError[] }` | Surface schema/semantic issues to the host pane. |

## Project Layout (proposed)
```
webharvest-ide/
└── vscode-harness/
    ├── package.json
    ├── tsconfig.json
    ├── README.md
    ├── src/
    │   ├── host/main.ts          # bootstraps @vscode/test-web
    │   └── webview/
    │       ├── index.html
    │       ├── main.ts           # acquires VS Code API, renders PropertyPanelShell
    │       └── styles.css        # reuses shared CSS tokens
    └── fixtures/
        ├── metadata.json         # recorded payload from /api/plugins/metadata
        └── sample.xml
```

## Build & Test Steps (future)
1. `npm install` inside `vscode-harness/` (installs `@vscode/test-web`, `vite`, `typescript`).
2. `npm run webview:build` – builds the WebView bundle via Vite library mode.
3. `npm run harness:test` – runs the Chromium-based `@vscode/test-web` scenario that loads the bundle, sends metadata/XML fixtures, and asserts on DOM snapshots + posted messages.

## Open Questions
- How much of the IDE layout needs to be replicated inside the harness vs. focusing on the property editor-only surface?
- Should the harness talk to a running IDE backend for live metadata, or rely on recorded fixtures + contract tests?
- How do we mock VS Code theme tokens so that PropertyPanelShell adapts to light/dark automatically?

## Next Steps
1. Scaffold `webharvest-ide/vscode-harness/` (package.json, tsconfig, README, fixtures, initial host/webview stubs).
2. Wire the WebView bundle to the shared packages (once extracted) and stand up a simple DOM smoke test.
3. Document the message contract in `DEVELOPER_GUIDE.md` after the prototype stabilizes.


