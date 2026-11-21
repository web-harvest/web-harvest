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
