# Full product validation workflow

The workflow `.github/workflows/full-validation.yml` adds broader validation beyond Core CI:

- full Maven reactor unit-test phase and package lifecycle (including the IDE Maven frontend build);
- full Maven reactor Failsafe integration-test phase;
- standalone IDE frontend type-check, Jest tests and Vite build;
- IDE workspace TypeScript checks/build;
- shared `metadata-client` and `xml-roundtrip` TypeScript package builds.

The frontend packages currently have no committed lockfiles. The workflow therefore uses `npm install` and records a known reproducibility limitation; adding and reviewing lockfiles remains a separate baseline task.

The VS Code harness remains an explicit scaffold: its current scripts are TODO placeholders and are not represented as a validated product surface.
