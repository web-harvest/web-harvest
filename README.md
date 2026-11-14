# WebHarvest

WebHarvest is an open-source Java framework for extracting and transforming web data with XML workflow configurations, an extensible plugin system, a command-line runner, and an optional web IDE.

**Canonical repository:** https://github.com/web-harvest/web-harvest  
**Intended public site identity:** `webharvest.org` (domain purchased; hosting/DNS cutover is not part of this documentation update)  
**Website source repository:** https://github.com/web-harvest/website

The Maven project version declared in the root POM is **2.3.1**. That is project metadata only. It is **not** evidence that a 2.3.1 GitHub Release or Maven Central artifact has been published. As of the inspection recorded in [Issue #88](https://github.com/web-harvest/web-harvest/issues/88), this repository has no GitHub Releases, and Maven Central search did not list `org.webharvest:webharvest` artifacts for 2.3.1.

## What it does

- Runs XML scraper/workflow configurations against HTTP and other data sources
- Provides a Core execution runtime and plugin registry
- Ships optional integration modules (database, FTP, mail, ZIP, web browser)
- Offers a CLI for headless configuration execution
- Includes an optional IDE with XML editing and a visual designer surface

Treat historical docs, marketing pages, and older guides as illustrative until verified against the current `main` sources and CI.

## Requirements

- **JDK 11** (root reactor compiler `source`/`target` are 11)
- **Maven 3.x** (3.6+ recommended)
- **Node.js / npm** only when changing IDE frontend packages

Some nested modules historically declare older compiler settings (for example CLI `1.8`, docs module `8`). Prefer the root reactor Java 11 baseline unless a module-specific build path is intentionally used and documented.

## Quick start

1. Clone and enter the repository:

   ```bash
   git clone https://github.com/web-harvest/web-harvest.git
   cd web-harvest
   ```

2. Build Core and its upstream reactor modules:

   ```bash
   mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test
   ```

3. Inspect example configurations under `docs/web/examples/` (for example `simple_test.xml`). Examples are illustrative; verify behavior against the modules you actually build and run.

4. CLI entry point (after a successful CLI module build): shaded/main class `org.webharvest.cli.WebHarvestCLI` in `webharvest-cli`.

## Build and verification

Run commands from the repository root on the commit under review.

Full reactor verification:

```bash
mvn --batch-mode --no-transfer-progress verify
```

Core module with required upstream modules:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

Core unit tests only (Surefire):

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test
```

Core integration tests are executed through Failsafe in the Semantic Core CI workflow. A green focused unit-test run is **not** equivalent to full reactor verification.

Canonical CI workflow: [`.github/workflows/semantic-core-ci.yml`](.github/workflows/semantic-core-ci.yml)  
Successful published-main verification previously recorded: Actions run [`43578560301`](https://github.com/web-harvest/web-harvest/actions/runs/43578560301) on `436cd50b159a5a22fc9e040663ebd323c5cdfa52`.

When reporting validation, record the exact command, commit SHA, test counts, failures, errors, skipped tests, and CI run URL.

## Module overview

| Module | Responsibility |
|---|---|
| `webharvest-core` | Configuration model, plugin contracts/registry, execution runtime, Stage 1 semantic XML inspection |
| `webharvest-cli` | Command-line execution of configurations |
| `webharvest-ide` | Optional IDE / Visual Designer client (not the semantic source of truth) |
| `webharvest-database` | Database plugin extensions |
| `webharvest-ftp` | FTP plugin extensions |
| `webharvest-mail` | Mail plugin extensions |
| `webharvest-webbrowser` | Browser automation plugin extensions |
| `webharvest-zip` | ZIP archive plugin extensions |
| `docs` | Documentation resources and Maven-coupled generators |

Dependency direction: extension modules and CLI/IDE depend on Core; Core must not depend on IDE. See [docs/MONOREPO.md](docs/MONOREPO.md).

## Plugin ecosystem

Core registers built-in processors (including the legacy `<set-var>` compatibility alias alongside `<def>`). Optional capabilities live in the extension modules above.

Authoritative plugin identity and metadata live in Core sources and descriptors, not in outdated root markdown summaries. Prefer:

- Core plugin registration and metadata under `webharvest-core`
- [docs/semantic-metadata/](docs/semantic-metadata/) for catalog/schema notes
- Example XML under `docs/web/examples/` and module `examples/` trees

Do not treat historical plugin counts in older documents as current guarantees without verifying against source or tests.

## IDE

`webharvest-ide` is an optional client/editor. Workflow meaning belongs to Core. The embedded IDE web application under `webharvest-ide/src/main/resources/webapp/` is product UI, not the public project website.

## Documentation and examples

| Resource | Notes |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | Contribution workflow and evidence expectations |
| [docs/MONOREPO.md](docs/MONOREPO.md) | Module boundaries and extraction criteria |
| [docs/RELEASE_READINESS.md](docs/RELEASE_READINESS.md) | Honest release/distribution status |
| [docs/GITHUB_TAXONOMY.md](docs/GITHUB_TAXONOMY.md) | Issue label/topic taxonomy |
| [docs/web/examples/](docs/web/examples/) | Illustrative XML configurations |
| [docs/semantic-metadata/](docs/semantic-metadata/) | Plugin metadata catalog notes |
| [web-harvest/website](https://github.com/web-harvest/website) | Canonical public website source (migration in progress) |

Root files such as `PLUGINS.md`, `DEVELOPER_GUIDE.md`, and `DOCUMENTATION_INDEX.md` may mix current and historical material. Prefer code, tests, and CI over narrative claims when they disagree.

The application tree still contains `docs/web/**` until the website repository cutover is complete. Do not assume that tree has already been removed.

## Current development status

- Default branch: **`main`**
- Public repository publication completed under [Issue #79](https://github.com/web-harvest/web-harvest/issues/79)
- Active public-launch readiness work: [Issue #84](https://github.com/web-harvest/web-harvest/issues/84)
- Product/architecture roadmap: [Issue #1](https://github.com/web-harvest/web-harvest/issues/1)
- Stage 1 Core semantic XML inspection exists in `org.webharvest.semantic` and is covered by focused CI contract tests; it is **not** the finished format-independent canonical model
- MCP, a new DSL syntax, and broad IDE semantic ownership are **not** current product deliverables

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.

- [Issues](https://github.com/web-harvest/web-harvest/issues)
- [Pull requests](https://github.com/web-harvest/web-harvest/pulls)
- [Commits on `main`](https://github.com/web-harvest/web-harvest/commits/main)

GitHub is the sole source of truth for development. Do not use historical external trackers as active contribution channels.

## License and attribution

BSD-style project license text: [licences/webharvest_licence.txt](licences/webharvest_licence.txt). See [LICENSE](LICENSE) and [NOTICE](NOTICE) for license and historical attribution. Preserve license and third-party notices when redistributing.
