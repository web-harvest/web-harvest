# 09 — Option B integration candidate (`integration/main-cutover-20261113`)

**Verdict: STOP** — local merge commit created and reviewed, but Core `verify` failed the JaCoCo branch gate. The merge was **not** pushed.

Date: 2026-11-13  
Worktree: `/Users/robertbala/src/web-harvest-option-b-integration-wt`  
Coordinator checkout preserved at `/Users/robertbala/src/web-harvest-git` on `2.2.x`.

## Starting refs

| Ref | SHA |
|-----|-----|
| Expected remote integration tip | `a3b2977b26e42cf69b792898211aab77c82a6284` |
| Observed `origin/integration/main-cutover-20261113` (pre-work) | `a3b2977b26e42cf69b792898211aab77c82a6284` (match) |
| `origin/2.2.x` | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| `origin/master` | `645baf8c61c6f3686907cfa487104ded340d90fe` |
| Pre-merge ahead/behind vs `2.2.x` | 158 ahead / 6 behind |
| Merge base vs `2.2.x` | `645baf8c61c6f3686907cfa487104ded340d90fe` |

GitHub was the only remote used for fetch/push operations. SourceForge was not accessed.

## Merge result (local only)

| Item | Value |
|------|-------|
| Merge commit | `66343080f381eb6dc90c5054740e69bf7f3f6139` |
| Parent 1 (campaign) | `a3b2977b26e42cf69b792898211aab77c82a6284` |
| Parent 2 (`2.2.x`) | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| Resulting tree | `bd1de0f855635b9a5ad8072c36513026b0093d3e` |
| Tree vs pre-merge campaign tip | **identical** |

Command: `git merge --no-commit --no-ff origin/2.2.x`

Campaign tip remains an ancestor. `2.2.x` tip is recorded as the second parent.

## Conflicts and resolutions

Exactly the four expected conflicts; no additional conflicts.

| Path | Conflict type | Resolution |
|------|---------------|------------|
| `docs/archive/FINAL_COVERAGE_SESSION_SUMMARY_r792-r802.md` | rename/delete (`2.2.x` deleted root copy; campaign kept archive) | **Keep campaign archive** (`git add` ours) |
| `docs/archive/SESSION_COVERAGE_SUMMARY_r792-r796.md` | rename/delete | **Keep campaign archive** |
| `docs/archive/SESSION_FINAL_COMPLETE_r792-r808.md` | rename/delete | **Keep campaign archive** |
| `pom.xml` | content | **Keep campaign POM** (`git checkout --ours`) |

### Documentation rationale

`2.2.x` commit `1ad20f8` removed obsolete **root** session summaries. The campaign had already relocated the historical copies under `docs/archive/`. Owner policy: retain archived historical documentation; do not discard it merely because `2.2.x` deleted the root paths.

### POM rationale

Campaign `pom.xml` already includes the GitHub project/SCM URLs introduced on `2.2.x` (`9c4a645`). Retained campaign-only build configuration:

- Javadoc plugin configuration
- Surefire `--add-opens` argLine for Java 11 reflective tests
- Absence of `guice-assistedinject`

### `guice-assistedinject` investigation

- Present on `origin/2.2.x` POM; absent on campaign POM.
- Workspace search for `AssistedInject`, `@Assisted`, and `assistedinject` outside `docs/archive/` and `target/`: **no consumers**.
- Module POMs reference `guice` / `guice-multibindings` only.
- Conclusion: do **not** restore `guice-assistedinject` without a concrete consumer.

### Auto-merged `2.2.x` content

The six commits unique to `2.2.x` did not introduce a tree delta after conflict resolution:

| Commit | Subject | Outcome in merge tree |
|--------|---------|------------------------|
| `1ad20f8` | remove root session summaries | Conflict; archives retained |
| `9c4a645` | GitHub contribution/SCM docs | `CONTRIBUTING.md` / `README.md` / `LICENSE` already identical to `2.2.x`; POM kept campaign |
| `87ec535` | Bug #36 credential-shaped fixture (#81) | File content already identical on campaign tip |
| `e252967` / `d96420f` / `05ed9b3` | historical `2.2.x` / master merges | Ancestry only; no unique tree delta vs campaign |

## Verification (exact merge commit)

Command (run on `66343080f381eb6dc90c5054740e69bf7f3f6139`):

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

Evidence:

- `reports/stabilization/evidence/mvn-verify-summary.json`
- `reports/stabilization/evidence/14-jacoco-merge.summary.txt`
- `reports/stabilization/evidence/mvn-verify-failure-excerpt.txt`
- `reports/stabilization/evidence/mvn-verify.exitcode` → `1`
- Full local log (gitignored): `reports/stabilization/evidence/mvn-verify-webharvest-core.log`

### Results

| Gate | Result |
|------|--------|
| Maven exit code | **1** (FAILURE) |
| Surefire | **3552** tests, **0** failures, **0** errors, **0** skipped |
| Failsafe | **255** tests, **0** failures, **0** errors, **0** skipped |
| Checkstyle | **0** violations |
| SpotBugs | **0** findings (`BugInstance size is 0`) |
| JaCoCo instructions | **91.2770%** (47318 / 51840) — threshold met |
| JaCoCo branches | **79.9659%** (3756 / 4697) — **FAIL** (gate expects ≥ 80%; plugin reported `0.79`) |

Failure line:

```text
Rule violated for bundle webharvest-core: branches covered ratio is 0.79, but expected minimum is 0.80
```

### Coverage note (diagnosis only)

Prior campaign evidence (historical, not reused as proof for this merge): branches **80.01%** (3758 / 4697).

This merge tree is **byte-identical** to `a3b2977^{tree}`, yet this run measured **3756 / 4697** (−2 covered branches). That is consistent with borderline / non-deterministic branch coverage on an otherwise unchanged tree—not with merge conflict mis-resolution. No production-code “fix” was attempted under the authorization boundaries.

## Push status

**Not pushed.** Remote `origin/integration/main-cutover-20261113` remains:

`a3b2977b26e42cf69b792898211aab77c82a6284`

Authorization required successful verification before a normal push. Pushing an unverified candidate was refused.

## GitHub Actions

Workflow `.github/workflows/semantic-core-ci.yml` triggers on push/PR for listed branch patterns (`implementation/**`, `stabilization/**`, etc.) and `workflow_dispatch`. It does **not** automatically cover `integration/**` pushes.

**No GitHub Actions run was claimed or observed for merge commit `6634308`.** Local Maven failure is the authoritative verification result for this commit.

## Unchanged protections (confirmed)

| Item | Status |
|------|--------|
| `main` branch | **not created** (`git ls-remote` showed no `refs/heads/main`) |
| Default branch | remains **`2.2.x`** |
| PR #82 | still **OPEN**, base **`2.2.x`**, head **`stabilization/comprehensive-cleanup-20261113`**, metadata not modified by this work |
| `origin/2.2.x` | unchanged at `1ad20f8…` |
| `origin/master` | unchanged at `645baf8…` |
| Coordinator checkout | still on `2.2.x` with pre-existing untracked local artifacts only |

## Remaining limitations / owner-only next steps

1. Decide how to clear the JaCoCo branch gate on the merge tree (e.g. stabilize the −2 branch delta with an authorized test-only residual, or re-run verify until a reproducible ≥80% result is recorded—owner call).
2. After a green verify on the exact candidate commit, authorize a **normal** push of `integration/main-cutover-20261113`.
3. Owner-only later: create/retarget `main`, change default branch, or merge/close/retarget PR #82 — **not** done here.
4. Optionally extend CI triggers to `integration/**` if automated GHA evidence is required for future cutover candidates — **not** changed here.

## Publication

- PR #82 STOP comment: https://github.com/web-harvest/web-harvest/pull/82#issuecomment-6865873972
- Report path (local / unpushed): `reports/stabilization/09-option-b-integration.md`
- Because verify failed, this report was committed locally only and was **not** published to the remote integration tip.

## Local recovery pointers

```text
Worktree: /Users/robertbala/src/web-harvest-option-b-integration-wt
Branch:   integration/main-cutover-20261113
Merge:    66343080f381eb6dc90c5054740e69bf7f3f6139
Report:   efd86d43e015dfd66cdf982b7e3eb737e27950d5 (plus any follow-up docs commits)
Remote tip: a3b2977b26e42cf69b792898211aab77c82a6284
```
