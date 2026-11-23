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


## Current integration status — 2026-11-21 (supersedes earlier checkpoints)

**Canonical source:** public repository `web-harvest/web-harvest`, branch `main`, commit `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1` (PR #285 merge).

### Security remediation integration

All 12 approved, bounded remediation PRs have been merged sequentially, with exact-head validation and post-merge checks before proceeding:

| PR | Scope | Result |
|---|---|---|
| #271 | Production Groovy 5.1.3 | Merged |
| #272 | jsoup 1.23.2 | Merged |
| #273 | Apache HttpComponents Client 5.6.5 / Core 5.4.4 | Merged |
| #276 | Jetty 12.1.15 EE9 | Merged |
| #278 | Commons Net 3.13.0 / PASV hardening | Merged |
| #279 | Commons Email 1.6.0 / Jakarta Mail 1.6.8 | Merged |
| #280 | org.json 20260719 | Merged |
| #281 | Guava 33.8.0-jre | Merged |
| #282 | Commons Collections 3.2.2 | Merged |
| #283 | Remove unused Commons Lang 2.5 / 3.12.0 direct declarations | Merged |
| #284 | Docs Jackson 2.23.0 | Merged |
| #285 | Test-only WireMock 2.35.1 | Merged |

Post-merge checks on the final main commit completed: OSV, full Java 17 reactor/static analysis, Core verify + JaCoCo, FTP + JaCoCo, logging/shaded audit and Maven verify all **SUCCESS**. The Groovy runtime Core/JSR-223 compatibility job was conditionally **SKIPPED**, not passed. Conditional Core network tests were also not counted as passes. See [Issue #194](https://github.com/web-harvest/web-harvest/issues/194) and the PR records for exact evidence.

**Qualification:** green post-merge checks establish the configured checks on this source commit; they do not, by themselves, establish a clean final release SBOM, complete packaged CLI/IDE inventory, reproducible distribution, Maven Central publication or release readiness.

### Updated work order

1. **P0 — Final integrated security evidence:** run/retain a trusted aggregate production + test SBOM/OSV scan on exact `main`; inspect the full findings, package set and scan artifacts. Distinguish inherited findings, new findings, test-only findings, false positives and reachability/mitigation; record accepted residual risks explicitly.
2. **P0 — Release artifact proof:** build the exact CLI and IDE distribution from the same main SHA; inventory shaded/runtime contents, hashes, licenses/notices and embedded dependency versions; run packaged startup/smoke and compatibility checks. Do not reuse old artifact hashes as evidence for this commit.
3. **P1 — Release engineering:** modernize legacy OSSRH deployment wiring to the supported Sonatype Central Publisher Portal path in a separate non-publishing PR; modernize/verify source, Javadoc and GPG plugin lifecycle; validate a bundle with upload disabled. No credentials or keys are changed.
4. **P1 — Release documentation:** refresh `docs/RELEASE_READINESS.md`, reconcile CHANGELOG [Unreleased]/v2.3.2 wording with the 2.3.1 reactor metadata, update Java 17 baseline and confirm ambiguous docs-module/license metadata with maintainer evidence.
5. **P1 — Product stabilization:** independently reconcile IDE defects/tests (including #31 and remaining IDE issues) and Core/runtime/plugin compatibility against current main. Do not claim the IDE is closed or complete without current evidence.
6. **P1 — Semantic Model Stage 1:** reconcile #8/#9/#11/#45 acceptance matrix and current-main compatibility evidence. Stage 1 implementation exists; formal acceptance is not closed. Keep Stage 2 canonical format-independent model deferred until Stage 1 is accepted.
7. **P2 — Plugin platform:** complete inventory and maintainer decision in #93 before SPI implementation, module moves or repository extraction.
8. **P2 — Documentation and website:** finalize user-facing docs after the release candidate facts are stable; website work follows verified docs and requires separate deployment authorization.

### Release decision

**NO-GO** for tag, GitHub Release or Maven publication. The security remediation queue is integrated, but the combined final SBOM/artifact evidence and release engineering/documentation gates are not yet complete. No release, tag, deployment, signing-key or credential action is authorized by this checkpoint.

### Work-package owners / canonical tracking

- Security evidence and residual-risk classification: #194 (closed integration parent; follow-up findings should be tracked in bounded child issues if any remain).
- Release/distribution: #88 and `docs/RELEASE_READINESS.md`.
- IDE stabilization: #31 and existing IDE issues.
- Semantic Stage 1 acceptance: #8, #9, #11, #45.
- Built-in plugin inventory / boundary: #93 (then existing plugin-contract issues #4/#60/#68).
- Documentation governance: #85/#86; website: `web-harvest/website#1`.

## Current integration status — 2026-11-22 (PR #317 merged)

**Canonical source:** public repository `web-harvest/web-harvest`, branch `main`, merge commit `5adfb247559b747fa35288966a5ef0add730e7fc` (PR #317).

### Javadoc enforcement and generated API artifacts

- Default Javadoc doclint is enforced across all shipped Java modules: fatal doclint errors fail the build; non-fatal warnings remain visible. `failOnWarnings` is intentionally not enabled while the warning baseline is being triaged.
- PR #317 restored `failOnError=true`, removed the blanket doclint suppression, and corrected fatal source markup/heading errors in Core, Database, FTP, Mail, WebBrowser, ZIP, CLI and IDE.
- Exact PR-head Java 17 full reactor/static analysis, Javadoc generation, packaged CLI and IDE smoke tests, and shaded package inventory all passed.
- Actual Javadoc JAR inspection covered all eight modules, 1,101 HTML pages and 44,334 local href/src references; no missing local files or unresolved local fragments were found.
- The visible warning baseline is: Core at least 100 (Javadoc output capped at 100), Database 14, FTP 7, Mail 5, WebBrowser 4, ZIP 2, CLI 3 and IDE 74. These warnings primarily concern missing comments and incomplete parameter/return/throws descriptions. Track remediation in [Issue #291](https://github.com/web-harvest/web-harvest/issues/291); do not suppress doclint or enable warning-fatal mode merely to make the build green.

### Exact-head quality and security evidence

On PR #317 head `7659929e361f394a61a23216d31313e83f936786`:
- Java 17 full reactor/static analysis and all eight Javadoc JAR checks: PASS ([run 45006145026](https://github.com/web-harvest/web-harvest/actions/runs/45006145026)).
- Core verify + JaCoCo and production Groovy 5.1.3 / JSR-223: PASS ([run 45006145127](https://github.com/web-harvest/web-harvest/actions/runs/45006145127)).
- FTP tests + JaCoCo 80% gate: PASS ([run 45006144994](https://github.com/web-harvest/web-harvest/actions/runs/45006144994)).
- Optional Database regression: PASS ([run 45006144985](https://github.com/web-harvest/web-harvest/actions/runs/45006144985)).
- Logging/shaded audit: PASS ([run 45006144954](https://github.com/web-harvest/web-harvest/actions/runs/45006144954)).
- Dependency inventory, OSV scan, Actions lockfile and Semantic Core Contract: PASS ([inventory](https://github.com/web-harvest/web-harvest/actions/runs/45006144957), [OSV](https://github.com/web-harvest/web-harvest/actions/runs/45006144948), [lockfile](https://github.com/web-harvest/web-harvest/actions/runs/45006144969), [semantic](https://github.com/web-harvest/web-harvest/actions/runs/45006145127)).
- The experimental Groovy runtime compatibility job was conditionally SKIPPED and is not counted as PASS.

The above is exact PR-head evidence. Post-merge verification on merge commit `5adfb247559b747fa35288966a5ef0add730e7fc` remains a separate required check; do not infer it from PR-head success.

### Release decision and next gates

**Release remains NO-GO.** PR #317 closes the fatal doclint enforcement/remediation batch; it does not establish release readiness.

Remaining release gates are tracked in [Issue #88](https://github.com/web-harvest/web-harvest/issues/88):
1. Complete post-merge verification on the new `main` commit.
2. Reconcile third-party license and NOTICE attribution for shaded CLI/IDE packages. Presence of LICENSE/NOTICE resources is not proof of completeness.
3. Review aggregate production and test SBOM findings, classify inherited/new/test-only/false-positive/reachable findings, and record accepted residual risk.
4. Verify source, Javadoc and signature bundles and modernize/validate Maven Central Publisher Portal release engineering in non-upload mode.
5. Reconcile release metadata, changelog, Java 17 compatibility/install guidance and exact release matrix.
6. Continue the root Markdown, maintained `docs/**`, historical-content and documentation navigation audit ([Issue #152](https://github.com/web-harvest/web-harvest/issues/152)).

No release tag, GitHub Release, Maven publication, signing/credential change or website deployment has been performed or is authorized by this status update.


---

## 10. Product topology clarification — 2026-11-23

This section records the maintainer's clarified product direction and refines the earlier descriptions of IDE as merely optional and CLI as a generic client. It distinguishes current implementation from the intended architecture.

### 10.1 Core — semantic execution runtime

Core is an embeddable Java runtime, not a standalone Control Plane service. It owns workflow execution, the format-independent semantic model, capability contracts, validation and controlled resolution of implementations.

The target architecture combines:

- a format-independent semantic representation, with XML retained as a supported compatibility/configuration adapter;
- polymorphic capability contracts so workflows and agents can express required behavior without binding to a display label or concrete implementation class;
- explicit, compatibility-aware capability resolution and controlled dynamic loading;
- structured diagnostics and observable lifecycle/registration outcomes.

The intended agent capability is to compose, validate and supply execution configurations against the semantic/runtime contracts, including configurations introduced while a runtime is operating. This is a **target**, not evidence that safe live reconfiguration, a stable public SPI, or a complete canonical semantic runtime is already implemented. Dynamic loading must never mean arbitrary classpath scanning, implicit trust, silent fallback or uncontrolled code execution.

### 10.2 IDE — human-facing Control Plane

The IDE is intended to become one of WebHarvest's primary product surfaces and the principal human-facing Control Plane. Its responsibilities should evolve toward:

- understanding and inspecting semantic workflows and available capabilities;
- designing and configuring workflows without owning their canonical meaning;
- supervising execution and presenting lifecycle, validation and diagnostic state;
- observing runtime activity and helping a human reason about changes and failures.

The current IDE is not yet certified against this full target. It remains a client/editor whose semantic behavior must progressively move behind Core contracts. Visual layout/editor state must remain distinct from workflow semantics and plugin identity.

The IDE is a separate product distribution from the Core library. This does not imply a separate Git repository now.

### 10.3 CLI — one headless Data Plane interface

CLI is one non-interactive interface to the broader Data Plane. It should expose Core execution to scripts, pipelines, scheduled jobs and external integrations. The Data Plane may also be accessed through programmatic APIs or direct agent/Core integration; it is not synonymous with the CLI. CLI is not intended to become a parallel human Control Plane or own semantic authority.

CLI is a separate product distribution from both Core and IDE. Its current POM directly depends on Core and all five extension modules (Database, FTP, Mail, WebBrowser and ZIP), so today's shaded CLI distribution includes the integrations as a practical packaging fact. That current coupling must not define the long-term Data Plane contract or extension pack taxonomy.

### 10.4 Official extension Packs/Kits

Official extensions should be presented and managed as curated, compatible Packs/Kits: coherent capability sets with explicit versions and Core/SPI compatibility. A pack is a user-facing selection, installation and compatibility unit; it is not necessarily a single combined JAR.

Keep individual extension modules/artifacts independently identifiable and buildable where appropriate. A future pack manifest should describe included extension IDs/artifacts, versions, required/optional capabilities, Core/SPI compatibility ranges and dependency constraints. This manifest/installer experience is a target, not a current feature.

Initial taxonomy candidates for evaluation (not yet approved release products):

- **Data Integration Kit:** Database, FTP and Mail.
- **Web Automation Kit:** WebBrowser.
- **Archive capability / Kit:** ZIP, potentially included in broader kits rather than distributed as a standalone pack.

Validate these groupings against real workflows, runtime dependency footprint, security/trust profile, plugin discovery and user value before creating pack artifacts.

### 10.5 Repository and distribution boundaries

- Keep the current monorepo. Repository extraction is governed by the objective gates in [MONOREPO.md](MONOREPO.md).
- Keep Core as the independently consumable library contract.
- Distribute IDE and CLI separately from the Core library and from each other. A coordinated version number may be used initially, but artifact scope and release decisions must be explicit.
- Keep extension-level artifact boundaries; compose official extensions into Packs/Kits above those artifacts.
- Do not publish every Maven reactor module merely because it exists in the reactor.
- Do not infer Central bundle membership from reactor membership or from the current CLI's shaded dependency graph.

The exact 2.3.1 Maven publication set remains a decision tracked in [#342](https://github.com/web-harvest/web-harvest/issues/342). Central Publisher Portal bundle engineering in [#341](https://github.com/web-harvest/web-harvest/issues/341) must consume that agreed scope and remains blocked until the maintainer is notified before implementation.
