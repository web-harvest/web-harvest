# WebHarvest

WebHarvest is an open-source Java workflow and data-extraction platform built around an embeddable Core execution runtime and a format-independent semantic direction. Its product surfaces are evolving toward a human-facing IDE Control Plane, a headless CLI/Data Plane interface, and curated Packs/Kits of official extensions. XML remains a supported compatibility and configuration format.

**Canonical repository:** https://github.com/web-harvest/web-harvest  
**Website source:** https://github.com/web-harvest/website  
**Project roadmap:** [GitHub Issue #1](https://github.com/web-harvest/web-harvest/issues/1)

> **Project status:** The Maven reactor currently declares version `2.3.1`. This is project metadata, not proof of a published GitHub Release or Maven Central artifact. Check [release readiness](docs/RELEASE_READINESS.md) before treating any version as publicly released.

## What WebHarvest provides

- XML-based workflow configuration and execution.
- A Java Core runtime and plugin contracts/registry.
- Optional integration modules for database, FTP, mail, browser automation and ZIP/archive operations.
- A headless CLI for execution and automation (Data Plane surface).
- An IDE/editor evolving toward the primary human-facing Control Plane.
- Curated official extension Packs/Kits as the intended user-facing distribution model; individual extension modules remain separate technical artifacts.

Historical guides and generated plugin counts may describe earlier versions. For current behavior, prefer source, tests, CI and documentation explicitly marked current.

## Quick start for contributors

Requirements: JDK 17+ for the current main branch and next supported release line; previously published v2.2.0 artifacts retain their original Java 11+ runtime requirement. Maven 3.6+ recommended. Node.js/npm are needed only for relevant IDE frontend work.

```bash
git clone https://github.com/web-harvest/web-harvest.git
cd web-harvest
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test
```

For full Core lifecycle verification:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

A focused test run is not equivalent to full reactor verification. See [CONTRIBUTING.md](CONTRIBUTING.md) for the test policy, network-test path and evidence required in a PR.

## Maven modules

| Module | Responsibility |
|---|---|
| `webharvest-core` | Runtime, configuration model, plugin contracts/registry and semantic XML inspection |
| `webharvest-cli` | Headless execution and automation client of Core; Data Plane surface |
| `webharvest-ide` | Human-facing editor/control client of Core; intended Control Plane, not the semantic source of truth |
| `webharvest-database`, `webharvest-ftp`, `webharvest-mail`, `webharvest-webbrowser`, `webharvest-zip` | Optional integration modules |
| `docs` | Documentation resources and Maven-coupled generators |

Core must not depend on IDE or optional integrations. The repository remains a monorepo while the Core boundary and independent plugin contract are stabilized.

## Runtime compatibility

The current production Groovy runtime is Apache Groovy 5.1.3 through the JSR-223 provider. WebHarvest retains Java 17 as its baseline and uses the separate `groovy` and `groovy-jsr223` artifacts rather than the `groovy-all` aggregator. See [Groovy runtime migration notes](docs/GROOVY_RUNTIME_MIGRATION.md) and the [workflow security model](docs/SECURITY.md).

## Plugin ecosystem

Core includes built-in processors and preserves documented legacy compatibility such as the `<set-var>` alias alongside `<def>`. Optional integrations currently exist as Maven modules in this repository.

The project direction is to:
1. keep Core as an embeddable semantic execution runtime, independent of IDE and optional integrations;
2. evolve the format-independent semantic model, polymorphic capability contracts and controlled dynamic loading so agents can construct and validate execution configurations against explicit runtime capabilities;
3. evolve IDE into the human-facing Control Plane for understanding, designing, configuring, supervising and observing Core execution;
4. keep CLI as a separate headless Data Plane surface for scripts, pipelines, automation and integrations;
5. stabilize a versioned public Plugin SPI, descriptor, discovery/registration and compatibility contract;
6. package official extensions for users as curated, compatible Packs/Kits while preserving individual extension artifact boundaries;
7. retain the monorepo until objective extraction gates pass; only then consider a separate company-maintained Extensions repository and Maven release train.

These are roadmap targets, not claims that the independent SPI, separate extensions repository or semantic runtime are already released. See [Project Direction and Architecture Roadmap](docs/PROJECT_DIRECTION.md) and [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md).

## Documentation

- [Project direction and architecture roadmap](docs/PROJECT_DIRECTION.md) — product goals, architecture, phases and decision gates.
- [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) — target third-party plugin contract and current limitations.
- [Monorepo and module boundaries](docs/MONOREPO.md) — current modules, dependency direction and repository extraction gates.
- [Contributing](CONTRIBUTING.md) — workflow, branches, CI and review expectations.
- [Workflow security model](docs/SECURITY.md) — executable XML, scripts, nested includes and safe operation.
- [Groovy runtime migration notes](docs/GROOVY_RUNTIME_MIGRATION.md) — production runtime baseline and downstream compatibility guidance.
- [Release readiness](docs/RELEASE_READINESS.md) — verified distribution status.
- [Semantic metadata](docs/semantic-metadata/) — current catalog/schema notes.
- [XML examples](docs/web/examples/) — illustrative workflow configurations.

The `docs/web/**` tree contains historical and versioned documentation. Do not assume that old version-specific plugin counts or generated pages describe the current Core membership.

## Development workflow

- `main` is the current integration branch.
- `release/<major.minor>` is created only when a verified version enters release stabilization.
- Feature, fix and documentation branches are short-lived PR branches and should be deleted after merge.
- Release hotfixes are made on the affected supported release branch and forward-ported to `main` through a reviewed PR.
- Historical branches are retained until their unique content has been reconciled.

GitHub is the canonical source of truth for issues, code, reviews, CI and project documentation.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md), search existing issues and open a focused pull request. Technical project documentation, source comments, issues and PRs should be written in English.

## License and attribution

See [LICENSE](LICENSE), [NOTICE](NOTICE) and [licences/webharvest_licence.txt](licences/webharvest_licence.txt). Preserve third-party license and attribution notices when redistributing.


## Product architecture

The product roles are intentionally distinct:

- **Core — semantic execution runtime.** Core owns workflow meaning, capability contracts, validation and execution. The target semantic model is format-independent; XML is a supported adapter/compatibility format, not the semantic source of truth. Polymorphic capabilities and controlled dynamic loading are intended to let agents compose and validate execution configurations while a runtime is operating. This is a target architecture, not a claim that arbitrary live reconfiguration is already implemented.
- **IDE — human Control Plane.** The IDE is intended to become the primary human-facing environment to understand, design, inspect, configure, supervise and observe Core execution. The current IDE is an evolving client; it does not own workflow semantics or plugin identity.
- **CLI — Data Plane surface.** CLI is a separate headless interface for invoking Core from scripts, pipelines, automation and integrations. It is not a second human control plane.
- **Official extensions — Packs/Kits.** Users should be able to select curated, compatible sets of official capabilities. Packs/Kits compose extension artifacts; they do not make the underlying extensions one inseparable library or require every extension to be installed.

IDE and CLI are separate product distributions from the Core library. They may initially share a coordinated release version, but their artifact/package scope and release boundaries must remain explicit. See [Project Direction](docs/PROJECT_DIRECTION.md), [Monorepo policy](docs/MONOREPO.md), and [distribution decision #342](https://github.com/web-harvest/web-harvest/issues/342).
