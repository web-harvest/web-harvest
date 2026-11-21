# Security remediation integration — current status

**Repository:** `web-harvest/web-harvest`  
**Canonical branch:** `main`  
**Verified source SHA:** `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1`  
**Date:** 2026-11-21  
**Tracking:** [Issue #194](https://github.com/web-harvest/web-harvest/issues/194), [Release gate #88](https://github.com/web-harvest/web-harvest/issues/88)

## Executive status

<badge>12/12 remediation PRs merged</badge>

All 12 approved, bounded remediation PRs were merged sequentially. Each merge was guarded by exact-head validation and followed by required post-merge checks before the next merge. The final post-merge check set on `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1` completed successfully for OSV, full Java 17 reactor/static analysis, Core verify + JaCoCo, FTP + JaCoCo, logging/shaded audit and Maven verify.

The Groovy runtime Core/JSR-223 compatibility job was conditionally skipped. It is **not** counted as a pass. Conditional network-dependent tests likewise remain explicitly skipped where workflow filters excluded them.

## Integrated changes

| PR | Integrated change | Merge commit |
|---|---|---|
| [#271](https://github.com/web-harvest/web-harvest/pull/271) | Production Groovy 5.1.3 | `80ac154a86b2e618dcad926eea7647fae3d8e7d5` |
| [#272](https://github.com/web-harvest/web-harvest/pull/272) | jsoup 1.23.2 | `580bc983a0716c4bd9be45abcaa4f0cec8b6800c` |
| [#273](https://github.com/web-harvest/web-harvest/pull/273) | HttpComponents Client 5.6.5 / Core 5.4.4 | `749b95aadbace2569e16fdc587d359bcbdc623fe` |
| [#276](https://github.com/web-harvest/web-harvest/pull/276) | Jetty 12.1.15 EE9 | `ad2a116aef0597769362aef84095bb409bbfd4fe` |
| [#278](https://github.com/web-harvest/web-harvest/pull/278) | Commons Net 3.13.0 / PASV hardening | `d5072f82d1dd960fe2891c6e231be889094f226a` |
| [#279](https://github.com/web-harvest/web-harvest/pull/279) | Commons Email 1.6.0 / Jakarta Mail 1.6.8 | `2db11228389e98f76fa7a6ef8772a186dfe42614` |
| [#280](https://github.com/web-harvest/web-harvest/pull/280) | org.json 20260719 | `4c1280c00254991394559e1764a2bb49d751b235` |
| [#281](https://github.com/web-harvest/web-harvest/pull/281) | Guava 33.8.0-jre | `6707725a7f27e7bea30ebfb5b72e505b483be94e` |
| [#282](https://github.com/web-harvest/web-harvest/pull/282) | Commons Collections 3.2.2 | `2ddb8e52a804053548bd0731de57fcd3e478a809` |
| [#283](https://github.com/web-harvest/web-harvest/pull/283) | Remove unused Commons Lang direct declarations | `9d22cd8c8bcdd937645f969f9571c9b8fbce36bf` |
| [#284](https://github.com/web-harvest/web-harvest/pull/284) | Docs Jackson 2.23.0 | `5b2c3a09ed20c66be5d6e34254134545f8394ba0` |
| [#285](https://github.com/web-harvest/web-harvest/pull/285) | Test-only WireMock 2.35.1 | `c84761a3396bcabae656d3a3ef75c5f2ed7a71d1` |

## Exact-main post-merge check summary

| Check | Result |
|---|---|
| OSV scheduled scan | SUCCESS |
| Full reactor tests + static analysis (Temurin 17) | SUCCESS |
| Core verify + JaCoCo | SUCCESS |
| FTP tests + JaCoCo | SUCCESS |
| Logging dependency / shaded package audit | SUCCESS |
| Maven verify | SUCCESS |
| Groovy runtime Core + JSR-223 compatibility | SKIPPED by condition; not a pass |

Check-run conclusions are recorded on commit [`c84761a3396bcabae656d3a3ef75c5f2ed7a71d1`](https://github.com/web-harvest/web-harvest/commit/c84761a3396bcabae656d3a3ef75c5f2ed7a71d1).

## What this does and does not establish

Established: the bounded dependency/security changes are integrated on main and the configured post-merge checks above succeeded.

Not yet established: a complete final release-candidate SBOM and residual-risk sign-off; a freshly built and fully inventoried CLI/IDE distributable from this SHA; reproducible release artifacts; Central Portal publication readiness; or permission to release. Earlier artifact hashes are historical and must not be reused as evidence for this commit.

## Next actions

1. [ ] Generate/inspect the trusted aggregate production + test SBOM/OSV outputs for exact main; verify exact package sets and scanner exit status.
2. [ ] Classify all residual inherited/new/test-only findings, reachability, mitigations and accepted risks.
3. [ ] Build CLI and IDE packages from exact main; record artifact SHA-256, shaded dependency inventories, licenses/notices and packaged smoke results.
4. [ ] Update [#88](https://github.com/web-harvest/web-harvest/issues/88) and `docs/RELEASE_READINESS.md` with the verified results.
5. [ ] Keep release status NO-GO until all release gates and maintainer authorization are satisfied.

## Authorization boundary

No version bump, tag, GitHub Release, Maven/Central upload, signing-key or credential change, or production website deployment is authorized by this status document.
