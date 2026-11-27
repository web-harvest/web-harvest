# WebHarvest

WebHarvest is an open-source, Java-first **Adaptive Data Acquisition & Integration Platform**. It is designed to acquire, understand, validate, transform, enrich and integrate data from diverse sources through specialized, extensible plugins. Web scraping is one acquisition capability, not the product boundary; workflow composition is one execution mechanism, not the product category. XML remains a supported compatibility and configuration format while the platform evolves toward a format-independent semantic model.

**Canonical repository:** https://github.com/web-harvest/web-harvest  
**Website source:** https://github.com/web-harvest/website  
**Project roadmap:** [GitHub Issue #1](https://github.com/web-harvest/web-harvest/issues/1)

> **Project status:** The Maven reactor currently declares version `2.3.1`. This is project metadata, not proof of a published GitHub Release or Maven Central artifact. Check [release readiness](docs/RELEASE_READINESS.md) before treating any version as publicly released.

## What WebHarvest provides

- A Java Core runtime and an existing plugin-based execution model, with XML compatibility.
- Existing integration modules for databases, FTP, mail, browser automation and ZIP/archive operations.
- A CLI and an IDE/editor as distinct clients of Core; neither is the semantic source of truth.
- A target plugin ecosystem spanning acquisition, parsing, extraction, validation, transformation, enrichment and destination capabilities.
- A target adaptive composition model in which deterministic logic and policy-constrained agents can propose reusable data operations.

The broader source-neutral platform, complete semantic model, independently versioned plugin ecosystem and safe adaptive runtime are architectural targets—not claims that every capability is already implemented.

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
1. establish a format- and source-neutral Core semantic model and stable capability contracts;
2. make specialized plugins independently identifiable and composable across acquisition, parsing, extraction, validation, transformation, enrichment and destination operations;
3. define safe, bounded adaptive composition: deterministic execution remains first-class, while agents may propose changes that Core validates and applies only at safe lifecycle boundaries;
4. persist validated, portable definitions (eventually through a DSL) so they can be inspected, reused, reloaded and adapted in later runs;
5. keep MCP, CLI and IDE as optional adapters/clients—not Core dependencies or sources of semantic authority;
6. define provenance, lineage, data-quality and policy boundaries as cross-cutting platform contracts;
7. retain the monorepo until objective API, compatibility, ownership and release gates justify extraction.

These are roadmap targets, not claims that the independent SPI, separate extensions repository or semantic runtime are already released. See [Project Direction and Architecture Roadmap](docs/PROJECT_DIRECTION.md), [Monorepo policy](docs/MONOREPO.md) and [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md).

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

The product category is **Adaptive Data Acquisition & Integration Platform**. The product roles are intentionally distinct:

- **Core — portable execution foundation and domain-neutral runtime contracts.** Core owns execution, validation mechanisms, policy enforcement and generic data-operation/Capability contracts. Cross-application domain semantics remain in independently governed, versioned semantic contracts; Core must not become the owner of every application's domain vocabulary. XML is a compatibility/authoring adapter, not the semantic source of truth. Current Core still contains historical XML/workflow and Guice coupling; container-neutral, format-neutral Core is a target reached through explicit migration gates.
- **IDE — optional human-facing Control Plane.** The IDE may provide design, inspection, configuration, supervision and observation. It is a client; it does not own workflow semantics, plugin identity or runtime lifecycle.
- **CLI — optional headless adapter.** CLI is one way to invoke Core from scripts, pipelines and automation. The broader data plane is not limited to CLI and must also support direct Java/API and other host integrations.
- **Plugins, functional Packs and scenario Kits.** Plugins are executable providers that implement one or more stable Capability contracts. A Pack is a stable functional domain/value area; a Kit composes Capabilities into a scenario or outcome. Neither term denotes a Maven artifact or binary distribution. Technical groupings of compatible extension artifacts are distribution profiles or extension bundles. See [Cross-Application Semantic and Functional Architecture](docs/CROSS_APPLICATION_SEMANTIC_ARCHITECTURE.md).

Agents are planners/proposers, not trusted runtime authorities. MCP is an optional interoperability adapter for capability discovery/invocation; it is not the semantic model, scheduler, authorization layer or a Core dependency. The runtime and policy boundary must validate all proposed changes, enforce resource/security budgets and preserve execution safety. See [Project Direction](docs/PROJECT_DIRECTION.md), [Monorepo policy](docs/MONOREPO.md), and [architecture decision #342](https://github.com/web-harvest/web-harvest/issues/342).
