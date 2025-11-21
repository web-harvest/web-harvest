# VS Code WebView Harness (Prototype)

This workspace hosts the proof-of-concept harness that runs the schema-driven property panel inside a VS Code WebView (via `@vscode/test-web`). It will eventually be wired into CI so metadata/visual changes are tested in a simulated extension environment.

## Layout
```
vscode-harness/
├── package.json            # scripts + devDependencies (@vscode/test-web, vite, ts)
├── tsconfig.json           # references parent config
├── fixtures/               # recorded metadata + XML payloads
├── src/
│   ├── host/main.ts        # TODO: bootstraps vscode-test-web scenario
│   └── webview/main.ts     # simple WebView script that logs messages
└── README.md               # this file
```

## Next Steps
1. Wire `src/host/main.ts` to `@vscode/test-web` once the shared packages are published.
2. Replace the placeholder WebView script with imports from `packages/visual-shared/property-panel`.
3. Add Vite config + build script so `npm run dev:webview` spins up the bundle locally.
4. Capture snapshots (Playwright) to guard against regressions.

See `docs/VSCODE_WEBVIEW_HARNESS.md` for the full architectural plan and message contract.


