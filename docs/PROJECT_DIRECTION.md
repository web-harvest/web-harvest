# WebHarvest project direction and architecture roadmap

**Status:** Maintainer-approved direction, 2026-11-15. Current implementation facts are explicitly distinguished from target architecture.
**Audience:** Users evaluating WebHarvest, prospective contributors, plugin authors, integrators and maintainers.
**Canonical planning record:** [GitHub roadmap issue #1](https://github.com/web-harvest/web-harvest/issues/1).
**Architecture boundary:** [Monorepo and module policy](MONOREPO.md).

## 1. Project direction

WebHarvest is an open-source Java framework for describing and executing data-extraction workflows. XML remains the supported workflow interchange/configuration format while the Core runtime and semantic contracts are being stabilized.

The project is being developed around four complementary goals:

1. A dependable, independently usable Core runtime.
2. A coherent built-in plugin set that provides the essential workflow execution capabilities.
3. An extensible plugin platform that allows optional integrations and third-party plugins to be built and distributed independently.
4. A longer-term semantic runtime that can resolve workflow capabilities and plugin implementations from explicit semantic context rather than relying on hard-coded assumptions.

The project is intentionally evolutionary. Compatibility with existing XML configurations and the current Java ecosystem matters. New architecture must be introduced behind tested contracts and migration gates, not by replacing working behavior in one large rewrite.

## 2. Current state versus target

| Area | Current state | Target |
|---|---|---|
| Workflow format | XML configuration and execution are established | XML remains a supported adapter/interchange format; semantic model is not tied to IDE presentation |
| Core runtime | Maven module `webharvest-core` owns runtime and plugin contracts | Small, self-sufficient execution engine with stable public API |
| Plugin metadata | Existing plugin metadata, registration and Stage 1 semantic inspection are present | One versioned descriptor/semantic contract used consistently by validation, discovery, tooling and runtime |
| Plugin discovery | Existing scanning/registration mechanisms are present | Deterministic discovery, compatibility validation, registration diagnostics and controlled lifecycle |
| Optional integrations | Separate Maven modules exist in this monorepo | Company-maintained optional extensions in a separate Git repository after extraction gates pass |
| External plugins | The current API is not yet declared a stable independent public SPI | Third parties can build and test plugins outside the WebHarvest source tree against published Core artifacts |
| Semantic runtime | Stage 1 inspection is not a complete format-independent semantic runtime | Runtime resolves compatible capabilities and typed bindings from workflow semantic context |
| Distribution | Root Maven version is project metadata; do not infer a published release from it | Reproducible, versioned Core and extension artifacts with documented compatibility |

A feature described in the target column must not be advertised as already available. In particular, classpath scanning alone is not semantic capability resolution, and Stage 1 XML inspection is not the finished canonical semantic model.

## 3. Architecture principles

### 3.1 Core is the platform boundary

Core owns the stable concepts required to interpret and execute a workflow:

- workflow/runtime execution contracts;
- configuration and variable semantics;
- essential built-in processors and control-flow primitives;
- plugin SPI, identity and lifecycle contracts;
- descriptor and semantic metadata contracts;
- validation and deterministic plugin resolution;
- runtime diagnostics and test support.

Core must not depend on optional integration modules, the IDE, website code or vendor-specific services. CLI, IDE and optional plugins consume Core; they do not become alternate semantic authorities.

### 3.2 Core membership is a deliberate product decision

A class does not belong to Core merely because it currently resides in the Core module or has a Core registration annotation. Each plugin must be classified by its role in the minimal execution engine, dependencies, semantics, compatibility obligations and tests.

The classification categories are:

- **Core execution primitive** — essential operations required to execute workflows.
- **Core control flow** — branching, iteration, composition and other execution-structure behavior.
- **Core data/extraction** — foundational data access, transformation and extraction operations.
- **Internal/system** — runtime infrastructure that is not necessarily a user-facing workflow processor.
- **Optional integration** — capabilities coupled to external systems, protocols, services or substantial optional libraries.

FTP, database, mail, browser automation and ZIP/archive capabilities are candidates for optional integration. The final classification must be based on source and runtime evidence, not this illustrative list alone.

Legacy XML compatibility, including the historical `<set-var>` alias alongside `<def>`, must be explicitly recorded and tested before any migration or removal.

### 3.3 Plugin discovery is not plugin trust

Automatic discovery should make installed, compatible plugins available without hand-editing the application registry. It must not mean arbitrary classes are silently executed.

The target registration pipeline must validate plugin identity, descriptor/API compatibility, dependencies, duplicate IDs, configuration contracts and trust policy; then initialize plugins with observable diagnostics and reliable cleanup. The exact discovery mechanism (for example Java service-provider metadata plus controlled scanning) is an implementation decision to be made against the current code.

### 3.4 Semantic context is the long-term runtime contract

The intended semantic context describes workflow meaning independently of a specific editor. It should represent required capabilities, typed inputs and outputs, variables, constraints, plugin identities and compatibility requirements.

The runtime should resolve these requirements against descriptors of installed plugins, validate that the chosen implementation satisfies the contract, and bind typed values explicitly. Resolution must not depend on display names, accidental classpath order or silent local-name guessing.

## 4. Module and repository strategy

The current repository remains a monorepo while Core APIs and plugin boundaries are being stabilized. Current modules and dependency rules are documented in [MONOREPO.md](MONOREPO.md).

The intended future shape is:

- **WebHarvest Core repository/artifacts:** runtime, essential plugins, stable SPI and semantic contracts.
- **WebHarvest company extensions repository/artifacts:** optional company-maintained plugins such as FTP, database, mail, browser and archive integrations, subject to final classification.
- **External third-party plugin repositories:** independently maintained plugins built against published Core APIs.
- **CLI and IDE:** clients of Core, with their own release boundaries only when independent builds and compatibility are demonstrated.

The extensions repository is not created merely to make the tree look cleaner. Extraction requires a stable SPI, published Core artifacts, a compatibility policy, an independently built out-of-tree plugin, automated discovery in a target application and independent CI/release evidence. Until then, the current monorepo is the source of truth.

## 5. Plugin author experience — target

A plugin author should eventually be able to:

1. Create an independent Maven project.
2. Depend on a published, versioned WebHarvest plugin API/Core artifact.
3. Implement the documented SPI and declare stable plugin identity and metadata.
4. Describe configuration inputs, validation rules, semantic capabilities and typed outputs.
5. Package the plugin using the documented provider/discovery contract.
6. Run contract tests without checking out or compiling the WebHarvest monorepo.
7. Add the artifact to a compatible application distribution and have it discovered, validated and registered automatically.
8. Inspect clear diagnostics when the plugin is incompatible, duplicated, misconfigured or fails initialization.

The public Plugin Author Guide describes the target contract and current limitations. It is not a claim that every target capability is already released.

## 6. Development and release model

### Branches

- `main` is the current integration/development line and the default branch.
- `release/<major.minor>` is created from a verified `main` commit only after the release scope is frozen and the version enters stabilization/RC. Do not create it merely because development has started.
- Immediately after the release cut, `main` continues as the next development line (for example, cutting `release/2.3.x` means `main` moves to the next development version, such as `2.4.0-SNAPSHOT`).
- Keep one active maintenance branch per supported release line. A previous release line remains only while it has an explicit support commitment; otherwise it is preserved as a read-only historical ref.
- The existing `2.2.x` branch is 164 commits behind current `main`; it is historical, not an active maintenance line unless support is explicitly reinstated.
- A release branch is maintained only while that release line is supported.
- `feature/*`, `fix/*` and `docs/*` branches are short-lived PR branches and should be deleted after merge.
- Historical `master` and `2.2.x` refs are preserved as historical lines unless a separate maintenance decision is made.
- Do not introduce a permanent `develop` branch or speculative release branches without a demonstrated need.

### Bug fixes and backports

- A bug affecting only unreleased work is fixed on a short-lived branch from `main`, then merged by PR.
- A bug affecting a supported release is fixed on a short-lived branch based on the corresponding `release/x.y` branch, then merged there by PR.
- A release hotfix is merged into that release line and then forward-ported to `main` through a separate reviewed PR (normally a cherry-pick) when applicable.
- A fix made on `main` is backported to a supported release only when the defect affects that release and the change is compatible; use a separate short-lived branch and PR based on that release branch.
- Do not merge all of `main` into a release branch to carry one fix. Each target line gets its own CI evidence.
- Every backport must identify the source fix, target release, compatibility considerations and independent CI evidence.

### Merge and branch hygiene

- PRs must identify their base branch and related issue.
- Required CI and review must complete before merge.
- Use GitHub automatic head-branch deletion for merged short-lived branches where possible.
- Do not delete historical, active, or diverged branches until unique commits and reports are reconciled.
- No branch cleanup should rewrite history or force-move protected lines.

## 7. Roadmap and decision gates

### Phase A — Stabilize the existing Core contracts

- Complete the Stage 1 metadata contract review and associated issue/PR gates.
- Keep deterministic Core CI authoritative and network-dependent tests explicitly opt-in.
- Preserve honest separation of unit, integration and network test results.
- Establish exact source-of-truth metadata and compatibility behavior.

**Exit gate:** independently reviewed metadata contract; green required CI on the merged commit; known test limitations recorded.

### Phase B — Inventory and close the Core plugin set

Tracking: [Issue #93 — inventory and approve the built-in plugin set](https://github.com/web-harvest/web-harvest/issues/93). The [preliminary source inventory](PLUGIN_INVENTORY.md) records the current registration/discovery baseline and known discrepancies; it is not the final classification.

- Inventory every plugin implementation across current modules.
- Record element names and aliases, implementation class, owner module, dependencies, configuration/schema, validation, execution/variable semantics, metadata, discovery, lifecycle, tests and compatibility.
- Classify each implementation as Core execution, Core control flow, Core data/extraction, internal/system or optional integration.
- Approve a Core membership list and a migration/disposition table.
- Implement the agreed Core set in bounded PRs with XML compatibility and runtime tests.

**Exit gate:** every shipped plugin has an evidence-backed classification; no unresolved Core dependency on optional integrations; essential built-in plugin contracts and tests are complete.

### Phase C — Stabilize the external Plugin SPI

- Define versioned plugin identity, descriptors, configuration schema, lifecycle and capability metadata.
- Define deterministic discovery, compatibility checks, registration order/duplicate handling, trust and failure diagnostics.
- Define a compatibility matrix between Core API and plugin artifacts.
- Provide a plugin test harness and a minimal independent sample.

**Exit gate:** an out-of-tree plugin builds and passes contract tests against published Core artifacts without compiling WebHarvest source.

### Phase D — Company extensions distribution

- Move only approved optional company-maintained plugins into a separate Git repository.
- Publish independently versioned Maven artifacts and CI.
- Verify automatic discovery and registration in the target application.
- Document installation, compatibility, upgrades and security/trust expectations.

**Exit gate:** independent repository, CI and release process; no reverse dependency from Core; end-to-end application integration verified.

### Phase E — Semantic-context-driven runtime

- Define a format-independent semantic workflow model, with XML as a supported adapter.
- Unify plugin descriptors, capability requirements, typed input/output contracts and resolution rules.
- Add deterministic semantic capability matching and validation.
- Integrate resolved implementations with runtime dispatch and diagnostics.
- Prove behavior with Core plugins and independently distributed extensions.

**Exit gate:** end-to-end tests demonstrate semantic resolution, typed binding, compatibility rejection and deterministic execution. Do not claim this phase complete based only on scanning or descriptor parsing.

## 8. Quality and release policy

- CI results must identify the exact commit SHA and workflow run.
- Unit, deterministic integration and network tests must be reported separately.
- A green focused test is not equivalent to full reactor verification.
- Do not weaken assertions, delete tests, add broad exclusions or lower thresholds to manufacture a green build.
- Public release claims require published artifacts and reproducible installation instructions; a POM version alone is not a release.
- Breaking API, XML or descriptor changes require an explicit compatibility and migration plan.

## 9. How to participate

- Users: report reproducible defects and describe the XML workflow, expected behavior and environment.
- Contributors: read [CONTRIBUTING.md](../CONTRIBUTING.md), search existing issues and open a focused PR.
- Plugin authors: see [Plugin Author Guide](PLUGIN_AUTHOR_GUIDE.md); until the independent SPI gate is complete, treat examples as design guidance rather than a stable published API guarantee.
- Maintainers: use [GitHub roadmap issue #1](https://github.com/web-harvest/web-harvest/issues/1) as the canonical planning record.

All project-facing documentation and technical coordination in GitHub should be maintained in English. The roadmap is directional; implementation status is determined by merged code, tests, published artifacts and CI evidence.
