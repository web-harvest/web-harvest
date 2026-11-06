# Full product validation workflow

The workflow `.github/workflows/full-validation.yml` adds broader validation beyond Core CI:

- full Maven reactor unit-test phase and package lifecycle (including the active IDE frontend Maven build);
- full Maven reactor Failsafe integration-test phase;
- the active IDE frontend under `webharvest-ide/frontend`: TypeScript type-check, Jest tests and Vite build;
- the two shared TypeScript wrapper packages (`metadata-client` and `xml-roundtrip`), compiled against the active frontend source tree.

## Deliberate scope boundaries

- The root-level `webharvest-ide/package.json`, `tsconfig.json` and `vite.config.js` still describe an older Visual Designer path at `src/main/resources/webapp/visual-designer`, which is absent from the current tree. They are not the active IDE frontend and are not treated as a product build gate. The current frontend is validated directly and through the Maven IDE package lifecycle.
- The shared packages are wrappers that re-export implementation from `webharvest-ide/frontend/src`; they are compiled with the active frontend dependencies installed. Their package output mirrors the monorepo source layout.
- The VS Code harness remains a prototype. Its scripts are TODO placeholders and it is not represented as a validated product surface.

## Reproducibility

The frontend packages currently have no committed lockfiles. The workflow therefore uses `npm install` and records a known reproducibility limitation. Adding and reviewing lockfiles, then switching to `npm ci`, remains a separate baseline task.
