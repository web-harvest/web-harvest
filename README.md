# WebHarvest

WebHarvest is an open-source Java framework for extracting and transforming web data with XML workflow configurations, a Core execution runtime, an extensible plugin architecture, a command-line runner and an optional IDE.

**Canonical repository:** https://github.com/web-harvest/web-harvest  
**Website source:** https://github.com/web-harvest/website  
**Project roadmap:** [GitHub Issue #1](https://github.com/web-harvest/web-harvest/issues/1)

> **Project status:** The Maven reactor currently declares version `2.3.1`. This is project metadata, not proof of a published GitHub Release or Maven Central artifact. Check [release readiness](docs/RELEASE_READINESS.md) before treating any version as publicly released.

## What WebHarvest provides

- XML-based workflow configuration and execution.
- A Java Core runtime and plugin contracts/registry.
- Optional integration modules for database, FTP, mail, browser automation and ZIP/archive operations.
- A CLI for headless execution.
- An optional IDE/editor and visual design surface.

Historical guides and generated plugin counts may describe earlier versions. For current behavior, prefer source, tests, CI and documentation explicitly marked current.

## Quick start for contributors

Requirements: JDK 11 baseline, Maven 3.6+ recommended. Node.js/npm are needed only for relevant IDE frontend work.

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
| `webharvest-cli` | Command-line client of Core |
| `webharvest-ide` | Optional editor/client; not the semantic source of truth |
| `webharvest-database`, `webharvest-ftp`, `webharvest-mail`, `webharvest-webbrowser`, `webharvest-zip` | Optional integration modules |
| `docs` | Documentation resources and Maven-coupled generators |

Core must not depend on IDE or optional integrations. The repository remains a monorepo while the Core boundary and independent plugin contract are stabilized.

## Plugin ecosystem

Core includes built-in processors and preserves documented legacy compatibility such as the `<set-var>` alias alongside `<def>`. Optional integrations currently exist as Maven modules in this repository.

The project direction is to:
1. finish and test a deliberately small, coherent Core plugin set;
2. stabilize a versioned public Plugin SPI, descriptor and discovery/registration contract;
3. prove an independently built plugin outside the WebHarvest source tree;
4. move company-maintained optional plugins to a separate Git repository and Maven release train only after those gates pass;
5. evolve toward semantic-context-driven capability resolution in the runtime.

These are roadmap targets, not claims that the independent SPI, separate extensions repository or semantic runtime are already released. See [Project Direction and Architecture Roadmap](docs/PROJECT_DIRECTION.md) and [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md).

## Documentation

- [Project direction and architecture roadmap](docs/PROJECT_DIRECTION.md) — product goals, architecture, phases and decision gates.
- [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) — target third-party plugin contract and current limitations.
- [Monorepo and module boundaries](docs/MONOREPO.md) — current modules, dependency direction and repository extraction gates.
- [Contributing](CONTRIBUTING.md) — workflow, branches, CI and review expectations.
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
