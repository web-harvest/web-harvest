# Release and distribution readiness assessment

**Issue:** [#88](https://github.com/web-harvest/web-harvest/issues/88) (parent [#84](https://github.com/web-harvest/web-harvest/issues/84))  
**Inspected against:** `main` @ `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1`  
**Assessment date:** 2026-11-21  
**Recommendation:** **NO-GO** for GitHub Release or Maven publication.

This assessment supersedes the earlier baseline references and Java 11 statements in this document. It is a source/CI checkpoint, not a release approval.

## 1. Current source and CI facts

| Area | Current evidence | Classification |
|---|---|---|
| Canonical source | Public repository, default branch `main`, exact SHA `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1` | FACT |
| Java baseline | Root POM declares `maven.compiler.release=17` | FACT |
| Security remediation | All 12 approved PRs #271, #272, #273, #276, #278–#285 merged sequentially | FACT |
| Final-main checks | OSV, full Java 17 reactor/static analysis, Core + JaCoCo, FTP + JaCoCo, logging/shaded audit and verify succeeded | FACT |
| Conditional tests | Groovy Core/JSR-223 compatibility job skipped; not a pass. Network-dependent groups may be conditionally skipped | FACT / qualification |
| Maven version | Root reactor declares 2.3.1 | FACT; not publication evidence |
| GitHub Release | Earlier API inspection returned zero releases; recheck at candidate freeze | Historical FACT; current freeze recheck pending |
| Maven Central | Current-coordinate consumer resolution has not been reverified for this checkpoint | UNKNOWN |
| Exact release artifacts | Final CLI/IDE distribution build, inventory and checksums for this SHA not yet recorded | OPEN |
| SBOM/OSV | Configured OSV check passed on final main; consolidated artifact-level report and residual finding classification still required | PARTIAL |

## 2. Security integration record

| PR | Change | Status |
|---|---|---|
| #271 | Production Groovy 5.1.3 | Merged |
| #272 | jsoup 1.23.2 | Merged |
| #273 | HttpComponents Client 5.6.5 / Core 5.4.4 | Merged |
| #276 | Jetty 12.1.15 EE9 | Merged |
| #278 | Commons Net 3.13.0 and PASV hardening | Merged |
| #279 | Commons Email 1.6.0 / Jakarta Mail 1.6.8 | Merged |
| #280 | org.json 20260719 | Merged |
| #281 | Guava 33.8.0-jre | Merged |
| #282 | Commons Collections 3.2.2 | Merged |
| #283 | Remove unused Commons Lang direct declarations | Merged |
| #284 | Docs Jackson 2.23.0 | Merged |
| #285 | Test-only WireMock 2.35.1 | Merged |

The Groovy runtime compatibility job was conditionally skipped and must not be represented as a pass. A green OSV job on this commit does not replace inspection of the generated production and test SBOMs, package-set matching, final shaded artifacts and inherited residual findings.

## 3. Remaining release blockers

### P0 — final security and artifact evidence

- [ ] Produce and retain the trusted aggregate production + test SBOM/OSV result for exact main SHA `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1`.
- [ ] Inspect scanner logs, exit status, package-set equality and generated artifacts; classify inherited/new/test-only findings and document residual risk.
- [ ] Build CLI and IDE distributables from this exact SHA; record artifact names, SHA-256, embedded dependency inventory and license/notice inventory.
- [ ] Run packaged CLI/IDE startup and compatibility smoke tests against the final dependency set.

### P1 — publishing and build lifecycle

- [ ] Replace retired OSSRH deployment wiring with the supported Sonatype Central Publisher Portal workflow in a separate PR.
- [ ] Preserve manual publication by default; validate bundle generation with upload disabled.
- [ ] Modernize and verify Maven source/Javadoc/GPG plugin lifecycle; inspect that sources, Javadoc and signatures are actually attached and usable. A successful `verify` alone is insufficient.
- [ ] Verify Central Portal metadata, consumer resolution and exact intended artifact coordinates without publishing.

### P1 — release narrative and metadata

- [ ] Refresh this assessment whenever the candidate SHA changes.
- [ ] Reconcile CHANGELOG `[Unreleased]` and the `v2.3.2` subsection against reactor version 2.3.1; do not silently relabel or delete scope.
- [ ] Confirm docs-module description referring to 2.2.0 and license/SPDX metadata with source/maintainer evidence before editing.
- [ ] Recheck GitHub tags/releases, release assets/checksums and Maven Central availability at candidate freeze.

## 4. GO criteria

GO requires all of the following on one exact candidate SHA:

1. Required CI and meaningful compatibility tests pass; all skipped/manual checks are explicitly resolved or accepted.
2. Trusted production + test SBOM/OSV evidence is complete and residual findings are classified and accepted.
3. CLI/IDE artifacts are reproducible, inventoried, hashed and pass packaged smoke tests.
4. Source, Javadoc and signature artifacts are produced and verified; non-publishing Central bundle validation succeeds.
5. Version, changelog, compatibility notes, installation docs and artifact coordinates agree.
6. Maintainer explicitly approves the exact tag/release/publication action.

## 5. Explicit non-actions / authorization boundary

- No version bump, tag, GitHub Release, Maven deploy or Central Portal upload.
- No credential, token, GPG key or signing-secret changes.
- No production website deployment.
- POM version 2.3.1 is not represented as proof of a published release.

**Current decision: NO-GO.** Security remediation integration is complete, but final SBOM/artifact proof and independent release-engineering/documentation gates remain open.

---

## 2026-11-22 checkpoint — supersedes the earlier inspected SHA

**Canonical source:** `main` includes PR #317 and PR #319; the latest merged change is PR #320 at `fb37955c2fb16cfe9600e615921bfab812f48071`.  
**Recommendation:** **NO-GO** for GitHub Release, Maven Central publication or website deployment.

This section is a dated evidence update. It does not erase the historical checkpoint above.

### Changes merged since the previous assessment

- [PR #317](https://github.com/web-harvest/web-harvest/pull/317) restored fatal Javadoc doclint enforcement and fixed fatal source markup errors across all eight Java modules. The warning baseline remains non-fatal and visible.
- [PR #319](https://github.com/web-harvest/web-harvest/pull/319) updated the project direction/architecture status with the exact Javadoc and CI evidence.
- [PR #320](https://github.com/web-harvest/web-harvest/pull/320) added the existing BSD license declaration, canonical project URL and SCM coordinates to the standalone `docs/pom.xml`. This closes the one missing-license-metadata item in the production and test CycloneDX SBOMs.

### Exact evidence for PR #320

PR head: `55ce5efac4298b73682663d15fd325c2f72d46f0`; merge commit: `fb37955c2fb16cfe9600e615921bfab812f48071`.

All six PR workflows completed successfully:
- Java 17 full reactor/static analysis, eight Javadoc JARs, CLI/IDE smoke tests and shaded package inventory — [run 45006761900](https://github.com/web-harvest/web-harvest/actions/runs/45006761900).
- OSV and production/test SBOM comparison — [run 45006761820](https://github.com/web-harvest/web-harvest/actions/runs/45006761820).
- Dependency inventory — [run 45006761816](https://github.com/web-harvest/web-harvest/actions/runs/45006761816).
- Documentation Module CI/site generation — [run 45006761834](https://github.com/web-harvest/web-harvest/actions/runs/45006761834).
- Logging Dependency Audit — [run 45006761805](https://github.com/web-harvest/web-harvest/actions/runs/45006761805).
- GitHub Actions dependency lockfile — [run 45006761846](https://github.com/web-harvest/web-harvest/actions/runs/45006761846).

The SBOM comparison reports 85 production and 26 test components on both base and head; the sole missing license metadata on the base (the standalone docs POM) is present on head; both OSV comparison reports contain no new findings. Package counts did not change.

**Qualification:** these are PR-head workflow results. The commit workflow lookup for merge commit `fb37955c2fb16cfe9600e615921bfab812f48071` returned no runs at the time of this checkpoint; do not describe PR-head checks as post-merge CI.

### Shaded package attribution — still unresolved

The exact-head CLI/IDE package inventory lists 12 and 11 license/notice-like resource paths respectively. Both contain multiple LICENSE/NOTICE resources, Maven POM metadata for embedded components and individual ANTLR/ASM/JSR-223 license files; CLI additionally contains the HtmlUnit fetch polyfill license.

The downloaded package-audit artifact records resource paths, not the full contents of every embedded license/notice resource. Therefore this evidence establishes resource presence only. It does **not** prove complete component-to-license attribution, required NOTICE text, correct treatment of multi-license alternatives, or legal sufficiency.

The root `NOTICE` names a limited set of third-party components and then says “other components as documented in the project dependencies”. That catch-all has not yet been reconciled to every dependency actually shipped in the CLI and IDE. A separate component-to-obligation matrix and content-level review remain mandatory.

### Remaining release blockers

1. **Third-party notices and licenses:** enumerate every component shipped in CLI and IDE; inspect actual embedded resource contents; map each component to exact upstream license and NOTICE obligations; record multi-license selections and any exclusions. Do not change the project's BSD license as part of this work.
2. **SBOM residual risk:** review all production and test components and OSV results, distinguishing runtime-reachable, test-only, inherited, disputed and false-positive findings; document disposition and ownership.
3. **Release engineering:** verify the complete source/Javadoc/binary/signature bundle and Maven Central Publisher Portal workflow in non-upload mode. No credentials, signing keys or publication actions are authorized by this assessment.
4. **Coordinates and publication history:** verify every current `org.webharvest` GAV and version against authoritative Maven Central records at candidate freeze. Historical `net.sourceforge.web-harvest` coordinates are not evidence for current coordinates.
5. **Documentation:** continue root Markdown and maintained `docs/**` audit; classify historical/versioned content; resolve the generated API landing-page dead end by choosing a truthful per-module link or aggregate-site strategy. Do not claim aggregation until implemented.
6. **Post-merge verification:** obtain and record an explicit main-branch verification result where workflow triggers permit; PR-head success is not a substitute.
7. **Javadoc warning debt:** Core output is capped at 100 warnings; other module counts observed on PR #317 head were Database 14, FTP 7, Mail 5, WebBrowser 4, ZIP 2, CLI 3 and IDE 74. Keep warnings visible and non-fatal until the separate remediation/baseline plan is approved.

### Decision

**RELEASE = NO-GO.** No release tag, GitHub Release, Maven publication, signing/credential change or website deployment has been performed or authorized.


## 2026-11-22 follow-up — shaded distribution evidence

**Canonical source:** PR #322 merged to `main` as `84a7e7d3b747e0af3929ec76cbad0bb372a50cd6`.  
**Decision remains: NO-GO.** This follow-up supplements the prior checkpoint; it does not authorize a release or publication.

### CI and retained artifact

The exact PR head `cd1c773ed3732c9fadd017b3a0e938d8797a31b8` passed:
- Java 17 full reactor, static analysis, Javadoc JAR audit, packaged CLI and IDE smoke tests, shaded inventory and diagnostics upload — run [45010539592](https://github.com/web-harvest/web-harvest/actions/runs/45010539592).
- Workflow Configuration Validation — [45010539576](https://github.com/web-harvest/web-harvest/actions/runs/45010539576).
- Actions dependency lockfile — [45010539661](https://github.com/web-harvest/web-harvest/actions/runs/45010539661).
- OSV Scanner PR Scan — [45010539595](https://github.com/web-harvest/web-harvest/actions/runs/45010539595).

Retained artifact: `java17-reactor-diagnostics`, artifact ID `14749662873`, SHA-256 `88c91fd9d480bd99b95114dd0da62ff8eb34e5678e9ba55c71bb85cf1b24a4f0`. It includes actual extracted legal-resource and embedded Maven POM/property payloads, with archive path, extracted path, size and SHA-256 in each distribution manifest.

| Distribution | Extracted resources | Maven metadata | License/notice resources |
|---|---:|---:|---:|
| CLI | 123 | 110 | 13 |
| IDE | 96 | 84 | 12 |

### Relocated Jetty and legal-resource finding

The CLI contains 1,280 relocated classes under `org/htmlunit/jetty/**`, no classes under `org/eclipse/jetty/**`, and 12 embedded Jetty 12.1.15 origin metadata records. The IDE contains 1,931 original-namespace `org/eclipse/jetty/**` classes. These are package-level facts; coordinate-to-class-family mapping and exact component-level SBOM generation remain open in [#323](https://github.com/web-harvest/web-harvest/issues/323).

The CLI's extracted `META-INF/NOTICE.md` is Jakarta Mail's notice; the IDE's is Eclipse Servlet's notice. The extracted `META-INF/NOTICE.txt` in both packages is Apache Commons Lang's notice. None of these is Jetty's notice. The exact Jetty 12.1.15 upstream source was checked at tag `jetty-12.1.15`, commit `770f5e4d1c39673d6356163118a1e6bf1117ced3`: its root `LICENSE` is EPL-2.0 and its `NOTICE.txt` declares EPL-2.0 or Apache-2.0 and contains Jetty project/trademark/copyright and third-party notices. Upstream file blob IDs: LICENSE `22840e2944ffc53b1953091fc31954e69126348d`; NOTICE `ea13d34f3c2e9a22eb2b5c9fd1919f7f6b9d61db`.

**Unresolved:** the shaded CLI/IDE archives do not currently expose a Jetty-specific NOTICE payload in the extracted manifest. The packaging process must preserve/aggregate Jetty's applicable legal notices without overwriting notices from Jakarta Mail, Eclipse Servlet, Commons Lang or other components. This is a release blocker, not a legal conclusion.

### Follow-up ownership and gates

- [#323](https://github.com/web-harvest/web-harvest/issues/323) owns distribution-specific CLI/IDE component manifests or CycloneDX SBOMs, class-to-origin correlation, aggregate SBOM delta classification, license/NOTICE reconciliation and deterministic CI assertions.
- [#88](https://github.com/web-harvest/web-harvest/issues/88) remains the release gate.
- No release tag, GitHub Release, Maven publication, signing-key/credential change or website deployment has been performed or authorized.


## 2026-11-23 checkpoint — exact shaded distribution verification and mandatory plugin-platform gate

**Source baseline:** `main` at `1729bdf4620a66686d02605083e27e27d1ab3944` (the base of the current release-evidence and documentation PRs).  
**Combined distribution candidate:** PR [#340](https://github.com/web-harvest/web-harvest/pull/340), head `788309118af09a4198f35044b0dc3ccc4a60b7e0`.  
**Recommendation remains: NO-GO.** This is an exact-head technical evidence update, not a merge, legal approval or release authorization.

### Exact-head technical verification — PR #340

The combined shaded-distribution candidate passed the following checks on its exact head:

- Java 17 full reactor, static analysis, Javadoc, packaged CLI/IDE smoke, shaded inventory and class-origin/provenance, CycloneDX validation and distribution-specific OSV — [run 45121496312](https://github.com/web-harvest/web-harvest/actions/runs/45121496312), SUCCESS.
- Workflow configuration validation — [run 45121496291](https://github.com/web-harvest/web-harvest/actions/runs/45121496291), SUCCESS.
- Aggregate base-vs-head production/test SBOM and OSV comparison — [run 45121496426](https://github.com/web-harvest/web-harvest/actions/runs/45121496426), SUCCESS.

Retained artifacts inspected on this candidate:

| Artifact | ID | SHA-256 |
|---|---:|---|
| `java17-reactor-diagnostics` | `14794145976` | `2dc996b40c7566979da67c8cb1e6dcd40bb759758f0a1696461ccb1acb0c63f8` |
| `osv-cyclonedx-evidence` | `14794275559` | `d6803f899ee8ddf1fc8fb70dec870fa4a12b1f927cb2429177e90a584bfd9522` |

### Distribution-specific results

| Check | CLI | IDE |
|---|---:|---:|
| CycloneDX components / unique PURLs | 72 / 72 | 54 / 54 |
| OSV packages scanned | 72 | 54 |
| Exact BOM-to-scan PURL set equality | PASS | PASS |
| Vulnerability findings | 0 | 0 |
| Missing license entries in generated BOM | 0 | 0 |
| Class-origin unmatched / ambiguous / differing-byte matches | 0 | 0 |

Additional exact-artifact evidence:

- CLI archive SHA-256: `04cdae0fe3a21e1279ad908757ff10b7f85c8d472f613acac2fa46f086502aa3`.
- IDE archive SHA-256: `6b0275c508ed64f2f38257da0138a34fb1a08e1731270794a886d8c879a8904c`.
- CLI class attribution: 21,591 classes = 20,307 exact-byte dependency matches + 1,280 separately mapped relocated Jetty classes + 4 first-party classes.
- IDE class attribution: 17,535 classes = 17,512 exact-byte dependency matches + 23 first-party classes.
- Jetty NOTICE payload is byte-identical in both archives; SHA-256 `ce642d8c8861ef05cf94c010a2d707425ed086d56641c127d0636fd0ac031e77`.
- All eight generated Javadoc JARs contain `index.html`.
- Aggregate production/test comparison: 85 production and 26 test packages on both base and head; all four scanner exit codes were zero; no new production or test-only findings were reported.

**Evidence limits:** PURL membership and class-origin correlation do not prove package-content equivalence. A zero-finding OSV scan is not a legal opinion. The generated BOM's license fields and retained NOTICE bytes are technical evidence, not a maintainer/legal disposition of every shipped license obligation.

### Remaining release blockers and decisions

1. **Legal disposition — OPEN.** The Jetty Servlet API multi-license expression (Apache-2.0 / EPL-1.0 in the inspected dependency metadata), shipped license texts, NOTICE obligations and any required license selection still require explicit legal review/disposition. Do not infer approval from complete metadata or the presence of NOTICE files.
2. **Publication scope — OPEN under #342.** Exact 2.3.1 GAVs and the root-parent publication strategy remain undecided. The root parent still needs a release-safe treatment of inherited retired OSSRH metadata and a clean-consumer resolution proof. CLI and IDE remain separate product distributions; extension publication is not implied by reactor membership.
3. **Central bundle — BLOCKED under #341.** Do not implement until #342's exact scope is approved and the maintainer has been notified. Any future work remains non-upload only; no Portal credentials, signing keys or upload action.
4. **Plugin platform — P0 release blocker.** #93 inventory/boundary approval and #346–#350 (versioned SPI, unified live registry, semantic parity, activation/policy/lifecycle isolation and independent out-of-tree conformance) are mandatory before release GO. Draft design/audit PRs #351 and #352 are documentation evidence only; they do not implement the platform.
5. **Documentation integration — OPEN.** PR #343 clarifies Core / Control Plane / Data Plane / Packs-Kits boundaries. Its exact head `d424bfdcb5024977d569c07cfb54e0f844ad3959` passed Java 17 Reactor Static Analysis (run `45134034583`), Documentation Module CI (`45134034363`), OSV (`45134034353`) and Actions lockfile (`45134034681`). PR remains Draft/unmerged.

### Decision

**RELEASE = NO-GO.** The exact PR #340 candidate has strong technical distribution/SBOM/Javadoc evidence, but legal disposition, exact 2.3.1 publication scope and the mandatory plugin-platform implementation/conformance gate remain unresolved. PR #340 is Draft/unmerged. PR #343 is Draft/unmerged. No tag, GitHub Release, Maven publication, signing/credential change or website deployment has been performed or authorized.


### 2.3.1 publication matrix — proposal awaiting explicit approval (#342)

The latest #342 decision record contains a concrete proposed scope. It is useful for release planning but is **not yet an approved publication list**:

| Coordinate / product | Proposed 2.3.1 treatment | Required gate |
|---|---|---|
| `org.webharvest:webharvest:2.3.1:pom` | Publish current root parent, conditionally | Remove retired OSSRH routing; isolate GPG signing from ordinary `verify`; prove published child POMs and a clean consumer resolve through this parent |
| `org.webharvest:webharvest-core:2.3.1:jar` | Primary Central library | Sources/Javadoc/signatures, exact artifact inventory and clean-consumer proof |
| `org.webharvest:webharvest-cli:2.3.1:jar` | Exclude from Core Central bundle; separate application distribution | Decide its independent download/release channel; do not imply that its current shaded five-extension payload is the supported Packs/Kits mechanism |
| `org.webharvest:webharvest-ide:2.3.1:jar` | Exclude from Core Central bundle; separate application distribution | Separate application packaging and legal/provenance obligations |
| `org.webharvest:webharvest-{database,ftp,mail,webbrowser,zip}:2.3.1:jar` | Defer public publication | #93 approved classification and #346–#350 stable SPI, activation, compatibility, policy and conformance gates |
| `org.webharvest:webharvest-docs:2.3.1:pom` | Exclude | No demonstrated downstream consumer use case |

The parent option is the least disruptive proposal because the existing child POMs inherit it and use its managed configuration. It is not release-safe as currently inspected: the parent carries retired OSSRH `distributionManagement` and a GPG profile bound to `verify`. A dedicated parent or flattening strategy would require its own compatibility proof and is not an acceptable untested release-time shortcut.

The proposal also defers all five extension artifacts rather than publishing a nominally public SPI that does not yet exist. The current CLI's direct dependency on all five extensions is a packaging fact, not an approval to publish them or a future Pack/Kit design.

**Maintainer decision still required:** explicitly approve/revise the exact parent + Core GAV scope, confirm CLI/IDE distribution channels, and approve deferral of all five extension GAVs. Until that decision is recorded in #342, #341 remains blocked. This checkpoint records the proposal without converting it into authorization.
