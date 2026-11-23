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


---

## 2026-11-23 — product distribution scope clarification (not a release approval)

The maintainer has clarified the intended product topology:

- **Core** is the primary embeddable semantic execution library.
- **IDE** is the intended human-facing Control Plane and a separate product distribution.
- **CLI** is the headless Data Plane surface and a separate product distribution.
- **Official extensions** should be offered as curated, compatible Packs/Kits while preserving individual extension artifact identities.
- The current monorepo remains in place; no repository split is authorized or required to achieve separate product distributions.

This changes how the 2.3.1 artifact set must be evaluated. Reactor membership, current CLI shaded dependencies and the fact that modules share version 2.3.1 are not publication-scope decisions.

### Required scope decision before bundle implementation

Issue [#342](https://github.com/web-harvest/web-harvest/issues/342) owns the exact 2.3.1 GAV/publication and distribution matrix. It must distinguish:

1. Core library and the parent POM needed by downstream Maven consumers (or an explicitly chosen alternative parent/flattening strategy).
2. CLI application distribution, outside the Core library bundle.
3. IDE application distribution, outside the Core library bundle.
4. Individually identifiable extension artifacts and any optional BOM/pack metadata.
5. Documentation-only POM/module exclusions unless a specific consumer-facing artifact is justified.

Issue [#341](https://github.com/web-harvest/web-harvest/issues/341) owns the technical Central Publisher Portal bundle workflow, but must consume the agreed scope from #342. No bundle implementation has begun. Per maintainer instruction, notify the maintainer before beginning that implementation.

**Release remains NO-GO.** This clarification does not authorize a version change, tag, GitHub Release, Maven publication, signing/credential change or website deployment.
