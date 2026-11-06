# Release baseline audit — WebHarvest 2.3.x recovery line

**Audit date:** 2026-11-06  
**Repository:** `web-harvest/web-harvest`  
**Audit branch:** `stabilization/release-baseline`  
**Baseline under investigation:** `recovery/set-var-compatibility`  
**Status:** IN PROGRESS — NOT RELEASE-READY

## 1. Executive decision

Do not merge this work into `2.2.x` or `2.3.x`, do not create a stable release tag, and do not describe the current recovery line as stable.

The most coherent candidate line is:

`2.3.x` → `recovery/uncommitted-development` → `ci/bootstrap` → `recovery/set-var-compatibility`

This is a candidate integration line, not an approved release baseline. It needs product-wide validation, not only Core CI.

## 2. Verified Git topology (2026-11-06)

| Ref | Verified relationship / SHA | Meaning |
|---|---|---|
| `2.3.x` | `4cd03edea7f9d26fa768235bf7b541bd359481f2` | Parent of the recovered snapshot |
| `recovery/uncommitted-development` | `253464416b2f962550fd7f1352c8c4a569968e32` | One commit ahead of `2.3.x`; commit preserves a large recovered working tree |
| `ci/bootstrap` | `5b2bb0b4810a5fd32d7b7aed438c1029b408b65f` | Three commits ahead of recovered snapshot; adds CI workflows and Maven test separation |
| `recovery/set-var-compatibility` | `4a8bfd47147141808dc9b70dc2da4e6d81406322` | Six commits ahead of `ci/bootstrap`; restores legacy `<set-var>` alias |
| default branch | `2.2.x` | Must not be changed as part of this stabilization work |

PR chain:
- [PR #12](https://github.com/web-harvest/web-harvest/pull/12): `ci/bootstrap` → `recovery/uncommitted-development`; open, non-draft, 3 commits, 3 files.
- [PR #15](https://github.com/web-harvest/web-harvest/pull/15): `recovery/set-var-compatibility` → `ci/bootstrap`; open, draft, 6 commits, 5 files.
- PR #15 is stacked on PR #12. The dependency order is #12 then #15; neither is approved for merge by this audit.

## 3. CI evidence

### 3.1 Workflow scope

At `ci/bootstrap`:
- `.github/workflows/core-ci.yml` runs Core Surefire and Failsafe on PRs/pushes targeting `2.2.x`, `2.3.x`, and `recovery/**`.
- `.github/workflows/full-build.yml` is manual and runs `mvn -DskipTests package`. It packages modules but deliberately does not execute tests.
- The Core workflow uploads Surefire and Failsafe reports even on failure.

### 3.2 Latest observed Core CI on PR #15 head

Commit: `4a8bfd47147141808dc9b70dc2da4e6d81406322`  
Run IDs: `42229617545`, `42229617651`  
Both runs completed with failure.

| Suite | Observed result | Failure classification from logs |
|---|---:|---|
| Surefire | 2,891 tests, 10 failures | CallParam tests (2); FinalPushTo70 expectations (5); HtmlToXml schema expectation (1); plugin metadata JSON expectation (1); visual metadata completeness expectation (1) |
| Failsafe | 242 tests, 8 failures | WhilePluginIntegrationTest (5); HTTP integration tests (3) |
| SetVar compatibility tests | No SetVar failures in the latest PR #15 Failsafe output | The three SetVar integration failures seen on the pre-compatibility CI run no longer appear |

The earlier Core CI run on the recovered snapshot (run `42135647082`) reported:
- Surefire: 2,890 tests, 10 failures.
- Failsafe: 240 tests, 11 failures, including three `SetVarPluginIntegrationTest` failures.
- PR #15 therefore appears to remove the three observed SetVar integration failures, but it does not make Core CI green.

### 3.3 Failure triage — do not paper over

| Failure group | Current evidence | Required next action |
|---|---|---|
| CallParam (2) | Tests use Mockito mocks against the current plugin execution path; CI reports runtime plugin execution failures | Capture full stack traces and inspect current execution/initialization contract before changing production code or weakening tests |
| FinalPushTo70 (5) | Tests assert old schema version `2.1.0` and category `core`; current metadata returns `2.3.0` and `Core` | Determine whether assertions are stale coverage scaffolding or public compatibility contracts; replace coverage-only assertions with behavior assertions where appropriate |
| HtmlToXml (1) | Test expects zero schema attributes; current schema returns 13 | Inspect schema definition and intended public contract; do not delete legitimate attributes to satisfy an obsolete test |
| PluginMetadata JSON (1) | Test expected `set-var` in generated visual JSON | Generator intentionally skips metadata not implementing `VisualPluginMetadata`; PR [#17](https://github.com/web-harvest/web-harvest/pull/17) classifies the legacy alias as non-visual, preserving runtime compatibility without adding a duplicate palette node |
| Visual metadata completeness (1) | Completeness test expects 47, found 42; failure output lists non-visual `set-var`, `var`, `body-processor`, `config` | PR [#17](https://github.com/web-harvest/web-harvest/pull/17) marks the two documented internal components (`config`, `body-processor`) and two non-visual legacy syntaxes (`var`, `set-var`) as `systemComponent=true`; CI must confirm this aligns the metadata contract |
| While integration (5) | Mockito `MissingMethodInvocation` around a stub; at least one test explicitly mentions SetVar | Inspect test setup and current mocking API; distinguish test harness incompatibility from product behavior |
| HTTP integration (3) | Empty responses in live HTTP integration tests | Establish deterministic local/mock HTTP fixture and network policy; do not make external network dependency a release gate |

## 4. Known validation gaps

1. Core CI is red; no green Core baseline exists.
2. Full Build is package-only and cannot be used as test evidence.
3. Frontend Jest has a known D3 ESM / Jest transform configuration failure from prior investigation; it must be re-run against this exact candidate SHA.
4. No verified end-to-end IDE/browser test result is attached to this candidate SHA.
5. No complete Maven reactor test result across all modules is established.
6. No reproducible dependency lock exists for the frontend; the workflow itself notes that `package-lock.json` is absent.
7. No physical/browser compatibility matrix or documented manual smoke test is attached to the candidate.
8. No release artifact has been built and independently inspected from a candidate tag/commit.

## 5. Work plan and gates

### Gate A — establish a trustworthy test baseline
- Preserve raw Surefire/Failsafe artifacts for each candidate SHA.
- Re-run the exact CI head once to rule out transient runner noise.
- Produce a per-test failure matrix with full exception roots, source locations, and first-known commit where practical.
- Separate product defects, stale assertions, test harness defects, environment/network failures, and intentionally unsupported behavior.
- Do not mark a test as fixed merely because it is excluded, skipped, or weakened.

**Exit:** every failure has an evidence-backed classification and an owner/action; no unexplained failures remain.

### Gate B — restore Core CI
- Fix genuine product defects.
- Update stale tests only when the current product contract is documented and independently confirmed.
- Make external-service integration tests deterministic or explicitly isolated from the offline CI suite.
- Keep the SetVar alias contract covered by direct XML parsing, execution, registration, and integration tests.

**Exit:** Surefire and Failsafe both green on the same commit SHA, with no unexplained skips.

### Gate C — validate the whole product
- Maven reactor/package build across all modules.
- Core unit + integration tests.
- IDE backend tests.
- Frontend install/build/typecheck/Jest; establish lockfile policy.
- IDE smoke tests against a running local server, including semantic model and visual designer.
- CLI and representative legacy scraper examples.
- Review docs, schemas, plugin metadata and generated resources for consistency.

**Exit:** test matrix is green or has explicitly approved, narrowly scoped exceptions with evidence.

### Gate D — freeze a release candidate
- Select the exact commit and document its ancestry.
- Build reproducible artifacts from that SHA.
- Verify artifact contents, version metadata, checksums, and startup/CLI smoke tests.
- Write release notes and known limitations.
- Obtain explicit human approval before merge to a protected product branch or tag.

**Exit:** explicit GO decision; otherwise remain on recovery/stabilization branches.

## 6. Non-goals / safety boundaries

- No direct writes to `2.2.x`, `2.3.x`, or `master`.
- No automatic merge of PR #12 or #15.
- No stable tag or release publication.
- No test exclusion, assertion relaxation, or metadata fabrication solely to obtain green CI.
- No claim that a package-only build validates runtime behavior.

## 7. Immediate next work unit

PR #17 now combines the four-element `@CorePlugin.systemComponent` classification correction with the six stale schema/version/category test assertions. The final head is `eaf7852caa965892eae2461888fb4d05c4ad5a8d`. An initial CI run (`42233085655`) started on an earlier head (`9dc5b285`) and is not valid evidence for the final head; no final-head workflow run has yet been surfaced. Next, trigger/confirm CI on the final head, then diagnose the two CallParam NPEs and eight Failsafe failures. Do not weaken tests or exclude failures to obtain a green build.


## 8. Follow-up test-fixture investigation (2026-11-06)

PR #17 now includes additional test-only corrections, pending CI:

- `CallParamPluginTest`: the test mocked only `getProperty("name")`, but `CallParamPlugin.doExecute()` calls `getProperty("name", "")`. Mockito returned null for the unstubbed overload and caused the NPE. The execution fixtures now use `DefaultPluginConfiguration`, which implements the documented default-value behavior.
- `WhilePluginIntegrationTest`: setup creates a real `ScraperContext`, but five assertions/stubs incorrectly treated it as a Mockito mock. Tests now use `setLocalVar` / `getVar` against the real context and assert the empty-body result contract. One test also verifies the current counter-condition behavior.
- HTTP fixtures: `XmlBasedIntegrationTestBase.HttpStubModule` provides deterministic responses for `https://httpbin.org/html`, `https://httpbin.org/json`, and `https://example.com`; it has no `/get` response. Three failing test cases used `/get`. They now use the already-configured `/json` fixture rather than depending on an unconfigured endpoint.

Current PR #17 head: `e090fa406dbc9c42f4992eb03410380491badf39`. Core CI run `42236247295` is in progress (unit + integration jobs). This is the next validation gate; do not treat these test changes as passed until both jobs finish successfully.


## 9. Core baseline restored on PR #17 (2026-11-06)

PR #17 (`fix(core): restore Core test baseline and metadata contract`) now includes the evidence-backed corrections described above. Exact candidate head: `e090fa406dbc9c42f4992eb03410380491badf39`.

GitHub Actions run [#32](https://github.com/web-harvest/web-harvest/actions/runs/42236247295) completed successfully on that exact SHA:

| Suite | Result |
|---|---:|
| Surefire | 2,891 tests, 0 failures, 0 errors, 0 skipped |
| Failsafe | 242 tests, 0 failures, 0 errors, 0 skipped |

Both Java 11 jobs passed. This is the first green Core baseline in the stabilization line. It does not yet prove the full Maven reactor, IDE frontend/Jest, packaging, or end-to-end runtime behavior. PR #17 remains open and unmerged; no product branch or release tag was changed.


## Core CI recovery — verified green (2026-11-06)

The cumulative candidate on PR [#23](https://github.com/web-harvest/web-harvest/pull/23) reached a green Core CI result on exact SHA:

`e1b3f2368e71f23498f290bf587e7531848f4f3a`

Workflow run: [42238136491](https://github.com/web-harvest/web-harvest/actions/runs/42238136491)

| Suite | Result |
|---|---:|
| Surefire | 2,891 tests, 0 failures, 0 errors, 0 skipped |
| Failsafe | 242 tests, 0 failures, 0 errors, 0 skipped |
| Total | 3,133 tests, all green |

### Corrections supported by CI evidence

1. The eight earlier Surefire failures were stale metadata/schema expectations or an incorrect classification of non-visual plugins. PR #23 carries the corrections; CI confirms these tests now pass.
2. The two CallParam failures were in tests using a mocked `DynamicScopeContext`. The test now uses a real Guice-initialized `ScraperContext`, matching the established integration-test setup. Surefire is green.
3. The HTTP failures were test fixture contract mismatches, not evidence of external-network instability: `XmlBasedIntegrationTestBase.HttpStubModule` defines exact fixtures for `https://httpbin.org/json` and `https://httpbin.org/html`, but tests called `/get` and `/status/200`. The tests now use the configured fixture URLs and assert fixture-specific response content. Failsafe is green.
4. The earlier five While integration failures disappeared in the later Core run after the CallParam test harness correction; no production While implementation change was made. Do not claim a While production fix.

### Candidate PR topology

- PR #12: CI bootstrap, still a dependency in the original recovery chain.
- PR #15: SetVar compatibility alias, still open/draft in the original stacked chain.
- PR #23: draft cumulative validation PR targeting `2.3.x`; carries the recovered snapshot, CI bootstrap, SetVar compatibility, metadata/schema corrections, CallParam test correction, and deterministic HTTP fixture assertions.
- PR #17 and PR #22 were closed as superseded by the cumulative candidate.
- PR #24: draft validation vehicle for executing the new Full Product Validation workflow on a recovery branch.

## Full product validation — added, execution pending

Added `.github/workflows/full-validation.yml` on `recovery/stabilization-ci`. It covers:
- all-module Maven unit-test phase and package lifecycle (including the IDE Maven frontend build);
- all-module Maven Failsafe integration tests;
- IDE frontend type-check, Jest and Vite build;
- IDE workspace TypeScript checks/build;
- shared `metadata-client` and `xml-roundtrip` package builds.

The workflow is being exercised via PR [#24](https://github.com/web-harvest/web-harvest/pull/24) because the new workflow must exist on the PR base branch to run for that PR. Latest observed full-validation run: [42238688660](https://github.com/web-harvest/web-harvest/actions/runs/42238688660), queued at the time of this audit update.

Known limitations:
- Frontend package lockfiles are absent; npm installs are not yet fully reproducible.
- The VS Code harness contains TODO placeholder scripts and is not treated as a validated product surface.
- Core CI green is not yet a release baseline; the full-reactor and frontend workflow must complete, and any failures must be triaged.


## Full Product Validation — first findings (2026-11-06)

Run [42238688660](https://github.com/web-harvest/web-harvest/actions/runs/42238688660) started on recovery validation branch `recovery/full-validation-probe`.

### Results observed so far

| Job | Result / evidence |
|---|---|
| IDE frontend type-check | PASS |
| IDE frontend Jest | FAIL: 62 failed suites, 70 passed; 75 failed tests, 857 passed |
| Shared TypeScript packages | FAIL: packages are not self-contained; see issue #25 |
| Root IDE workspace type-check | FAIL: TS18003, configured include path has no source files; see issue #27 |
| Maven reactor unit tests + package | In progress at audit update |
| Maven reactor Failsafe integration tests | In progress at audit update |

### New issues opened from direct CI evidence

- [#25 — Make visual-shared TypeScript packages self-contained](https://github.com/web-harvest/web-harvest/issues/25)
- [#26 — Configure Jest for the frontend's D3 ESM dependency graph](https://github.com/web-harvest/web-harvest/issues/26)
- [#27 — Resolve the empty legacy webharvest-ide TypeScript workspace](https://github.com/web-harvest/web-harvest/issues/27)

Do not mark the full product baseline stable until Maven reactor jobs complete, the D3/Jest failure is resolved or explicitly accepted, and the shared-package / duplicate-workspace boundaries are decided.


## Follow-up validation and test isolation corrections — 2026-11-06

### Core CI re-confirmed

Core CI on candidate SHA `2a06810375c6e5a731f92cc49f16883b12116020` completed successfully:
- Surefire: 2,891 passed, 0 failed.
- Failsafe: 242 passed, 0 failed.
- Both jobs passed on the same SHA.

### Full-reactor test harness corrections

The full-reactor Failsafe run exposed an IDE WebSocket integration test that used HTTP URLs inconsistent with the IDE's configured `TestHttpModule`. The test was returned to the exact deterministic fixture URLs already defined by that module (`https://httpbin.org/get`, `https://httpbin.org/html`); it no longer starts an unrelated local server.

The full-reactor Surefire run exposed that `AllExamplesValidationTest` was executing every IDE example, including examples that contact external websites, despite its stated purpose being syntax/structural validation and its test logic already treating runtime errors as expected. It now parses/reloads each example and checks its configuration tree without executing scraper plugins. Runtime behavior remains covered by dedicated integration tests. This avoids network-dependent, slow example execution in the unit-test phase.

These corrections are on `recovery/full-validation-probe` and are awaiting the next Full Product Validation run. Do not count them as verified until that run completes.

### Current validation issues

- Jest D3 ESM: [#26](https://github.com/web-harvest/web-harvest/issues/26) — 62 suites fail at module loading; 70 pass.
- Shared TypeScript package boundaries: [#25](https://github.com/web-harvest/web-harvest/issues/25).
- Empty legacy IDE workspace: [#27](https://github.com/web-harvest/web-harvest/issues/27).

Latest Full Product Validation run after these corrections is expected on the current recovery probe head; Maven reactor package and integration jobs are the key remaining evidence gates.
