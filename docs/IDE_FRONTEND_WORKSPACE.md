# IDE frontend workspace authority (Issue #27)

## Authoritative workspace

`webharvest-ide/frontend/` is the only product TypeScript frontend.

- Maven `frontend-maven-plugin` builds that directory (`webharvest-ide/pom.xml`).
- The Java server serves `src/main/resources/webapp/dist` produced by the Vite build.
- CI type-check, Jest, and Vite build run against `webharvest-ide/frontend`.

## Retired root workspace

The former root `webharvest-ide/{package.json scripts for vite/tsc, tsconfig.json, vite.config.js}`
targeted `src/main/resources/webapp/visual-designer/`, which was removed in commit
`432d509` ("Remove legacy visual-designer JS files (migrated to TypeScript)").

That root build is obsolete. It is not a second frontend. Do not retarget its
`include` paths at `frontend/src` solely to silence TS18003.

The module-root `webharvest-ide/package.json` is now a maintenance shell for
`sweep:examples` only. Shared compiler defaults for scaffolds such as
`vscode-harness` live in `webharvest-ide/tsconfig.base.json`.

## What CI must validate

1. `webharvest-ide/frontend` type-check, Jest, and build.
2. Shared `@webharvest/*` packages under `packages/visual-shared/`.
3. Maven packaging of `webharvest-ide` (which builds the frontend).

CI must not require a product type-check/build of the retired root Vite workspace.
