# Monorepo modules and boundary policy

**Status:** Current module layout and dependency rules are facts about `main`; future Core membership and repository extraction are gated decisions.
**Related:** [Project roadmap #1](https://github.com/web-harvest/web-harvest/issues/1), [Project direction](PROJECT_DIRECTION.md), [public launch readiness #84](https://github.com/web-harvest/web-harvest/issues/84).

## 1. Repository decision

**Retain the current monorepo.** Do not split Core, plugins, CLI, IDE or documentation into separate repositories while the Core plugin set and external Plugin SPI are still being stabilized.

The current Maven reactor uses a shared project version and shared CI. The plugin modules and clients still depend on Core APIs that are evolving. Premature extraction would turn internal boundaries into public compatibility promises before they are ready.

The intended future is a separate company-maintained extensions repository and Maven release train, but only after the objective gates in section 6 pass.

## 2. Current Maven modules

| Module | Current responsibility | Boundary |
|---|---|---|
| `webharvest-core` | Workflow runtime, configuration model, plugin contracts/registry, XML execution and Stage 1 semantic inspection | Must not depend on IDE or optional integrations |
| `webharvest-cli` | Headless CLI consuming Core | No semantic authority of its own |
| `webharvest-ide` | Optional editor/client UX and embedded webapp | No canonical workflow semantics or plugin identity source |
| `webharvest-database` | Database extensions | Depends on Core; candidate for future company extensions |
| `webharvest-ftp` | FTP extensions | Depends on Core; candidate for future company extensions |
| `webharvest-mail` | Mail extensions | Depends on Core; candidate for future company extensions |
| `webharvest-webbrowser` | Browser automation extensions | Depends on Core; candidate for future company extensions |
| `webharvest-zip` | ZIP/archive extensions | Depends on Core; candidate for future company extensions |
| `docs` | Documentation resources and Maven-coupled generators | Not a production website deployment peer |

These are module responsibilities, not a final plugin-by-plugin Core classification. A class is not Core merely because it currently lives in `webharvest-core` or has a Core registration annotation.

## 3. Dependency direction

```text
webharvest-cli ───────────────▶ webharvest-core
webharvest-ide ───────────────▶ webharvest-core
optional plugin modules ──────▶ webharvest-core
webharvest-core ──────────────X optional modules / IDE
```

Core owns the stable execution and plugin contracts. Clients and extensions consume those contracts. There must be no reverse dependency from Core to an optional integration.

## 4. Plugin classification policy

Every shipped plugin implementation must be inventoried and assigned one of these roles:

1. **Core execution primitive** — essential workflow execution operations.
2. **Core control flow** — branching, iteration and composition semantics.
3. **Core data/extraction** — foundational data and extraction operations.
4. **Internal/system** — runtime infrastructure not necessarily exposed as a workflow processor.
5. **Optional integration** — functionality coupled to an external protocol, service, platform or substantial optional dependency.

The inventory must include XML element names and aliases, implementation class, owning module, dependencies, schema/configuration, validation, execution/variable semantics, descriptor metadata, discovery/registration, lifecycle, tests, compatibility obligations and proposed destination.

FTP, database, mail, browser automation and ZIP/archive operations are candidates for optional integrations. The final decision must be based on evidence and workflow requirements, not assumed from the module name.

Preserve legacy XML compatibility—including the `<set-var>` alias—unless an explicit deprecation/removal decision and migration path are approved.

## 5. Intended future repository layout

- **Core:** runtime, essential built-in plugins, versioned Plugin SPI, descriptors and semantic contracts.
- **Company Extensions:** optional company-maintained plugins distributed as independent Maven artifacts from a separate Git repository.
- **Third-party plugins:** independently maintained artifacts built against published Core APIs.
- **CLI/IDE:** clients consuming Core, with separate release boundaries only when independent builds and compatibility are demonstrated.

The public Plugin Author Guide describes the target experience. Existing `Plugin` / `AbstractPlugin` classes and registration mechanisms are evidence to inspect, not proof that a stable independent SPI is already published.

## 6. Extraction gates

### Company Extensions repository

All gates are required:

- Core plugin membership has been decided and Core has no dependency on optional integration modules.
- Plugin SPI, identity and descriptor contracts are stable, documented and versioned.
- Core ↔ plugin compatibility/versioning policy is published.
- At least one plugin is built and tested out-of-tree against published Core artifacts without compiling the monorepo.
- The target application automatically discovers, validates, registers and reports that plugin.
- Independent CI, Maven publication, examples and compatibility tests exist.
- Installation, upgrade, failure and security/trust behavior are documented.

### IDE repository (separate, later decision)

- IDE builds and tests against published Core artifacts without sharing Core source trees.
- Visual/editor state is separated from workflow domain semantics.
- Ordinary IDE changes do not require Core internals to change.
- Required browser-level validation is independently available.

Until the relevant gates pass, keep the modules in this monorepo.

## 7. Website boundary

| Path / repository | Classification | Policy |
|---|---|---|
| `web-harvest/website` | Canonical public website source | Own website HTML/CSS/JS/assets and website-only tooling |
| `docs/web/**` | Historical/versioned application documentation still in this repository | Retain until website build, preview and cutover criteria are met |
| `docs/scripts/**` | Application-coupled documentation generators | Keep with the Maven project |
| `webharvest-ide/.../webapp/**` | IDE runtime UI | Keep with IDE |

The website repository must not become a Maven reactor peer or a second source of truth for application behavior.

## 8. Branch and release policy

- `main` is the current integration/development branch.
- Create `release/<major.minor>` from a verified `main` commit when that version enters release stabilization/RC; do not maintain speculative release lines.
- Keep a release branch only while its version is supported.
- Use short-lived `feature/*`, `fix/*` and `docs/*` PR branches; delete merged heads automatically where possible.
- Fix release-specific bugs on the affected release branch, then forward-port to `main` through a reviewed PR.
- Backport a `main` fix to a supported release only when applicable and after compatibility review.
- Preserve historical `master` and `2.2.x` lines. Reconcile unique work before deleting any other branch.
- Do not create a permanent `develop` branch without a demonstrated need.

## 9. Related documentation

- [Project Direction and Architecture Roadmap](PROJECT_DIRECTION.md)
- [Plugin Author Guide](PLUGIN_AUTHOR_GUIDE.md)
- [Contribution workflow](../CONTRIBUTING.md)
- [Canonical roadmap issue](https://github.com/web-harvest/web-harvest/issues/1)
