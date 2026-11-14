# Monorepo modules and boundary policy

**Status:** FACT for current module layout on `main`; PROPOSAL for future extraction criteria.  
**Related:** [#84](https://github.com/web-harvest/web-harvest/issues/84), [#86](https://github.com/web-harvest/web-harvest/issues/86), roadmap [#1](https://github.com/web-harvest/web-harvest/issues/1).

## Decision

**Retain the current monorepo for now.** Do not split Core, plugins, CLI, IDE, or docs into separate repositories during the public-launch readiness iteration.

Rationale:

- Shared version (`2.3.1` in the reactor), CI, and compatibility contracts still move together.
- Plugin modules and IDE still depend on Core APIs that are stabilizing.
- Premature repository splits would freeze unstable boundaries and complicate honest release assessment.

## Current Maven modules

| Module | Owns | Must not own |
|---|---|---|
| `webharvest-core` | Workflow runtime, plugin SPI/registry, XML configuration execution, Stage 1 semantic inspection | IDE UI, VisualDesigner layout state, website publishing |
| `webharvest-cli` | Headless CLI entry points that consume Core | Semantic authority or IDE-only models |
| `webharvest-ide` | Optional editor/client UX, embedded webapp | Canonical workflow semantics |
| `webharvest-database` / `ftp` / `mail` / `webbrowser` / `zip` | Optional plugin implementations and their dependencies | Core semantic model ownership |
| `docs` | Maven-coupled documentation generators and packaged docs resources | Production website deployment as a second source of truth |

## Dependency direction

```text
webharvest-ide ──▶ webharvest-core
webharvest-cli ──▶ webharvest-core (+ optional extension modules)
extension modules ──▶ webharvest-core
docs generators may read application sources/classes
website repository is separate and must not become a Maven reactor peer
```

Invariant: **Core does not depend on IDE.** IDE and other clients consume Core contracts.

## Website boundary

| Path / repository | Classification | Location policy |
|---|---|---|
| `web-harvest/website` | Canonical public website source | Own website HTML/CSS/JS/assets, website-only scripts, SEO tooling |
| `docs/web/**` (this repo) | Historical / still-present website tree | Keep until website builds, previews, and cutover criteria are met |
| `docs/scripts/**` app-coupled generators | Maven/product documentation tooling | Keep here |
| `webharvest-ide/.../webapp/**` | IDE runtime UI | Keep here |

## Future extraction criteria

Extraction is allowed only when objective evidence exists. These are gates, not a schedule.

### Plugins repository gate

- Stable public plugin SPI and descriptor contract versioned independently of IDE
- Documented compatibility/versioning matrix for Core ↔ plugin releases
- At least one external/out-of-tree plugin built and tested against published Core artifacts
- Independent release cadence that does not require synchronized IDE commits

### IDE repository gate

- IDE builds, tests, and releases against a published Core API without sharing Core source trees
- Visual/editor state is separated from workflow domain semantics
- No requirement to modify Core internals to ship ordinary IDE UX changes
- Browser-level validation remains available where product policy requires it

Until those gates are met, keep one repository and document boundaries here.
