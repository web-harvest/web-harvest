# 11 — JaCoCo coverage repair (clean 3758/4697)

**Verdict:** **GO (PASS)**

Clean Core `verify` reproducibly meets the 80% branch gate at **3758 / 4697** after three focused regression tests. No production code, JaCoCo threshold, or Maven plugin changes.

- Date: 2026-11-13
- Repair worktree: `/Users/robertbala/src/web-harvest-jacoco-repair-wt`
- Branch: `fix/jacoco-branch-coverage-repair` (based on Option B tip `3859aac`)
- Coordinator checkout preserved: `/Users/robertbala/src/web-harvest-git` on `2.2.x` @ `1ad20f8`
- Option B diagnostic worktree preserved: `/Users/robertbala/src/web-harvest-option-b-integration-wt` on `integration/main-cutover-20261113` @ `3859aac`
- Local commit authorized after final evidence audit. No push. No PR.

---

## 1. Starting commit and worktree details

| Item | Value |
|------|-------|
| Base tip | `3859aacd4392c02795451b6fd09077a363cd9010` — `docs: add PR #82 STOP comment link to Option B report` |
| New worktree | `/Users/robertbala/src/web-harvest-jacoco-repair-wt` |
| New branch | `fix/jacoco-branch-coverage-repair` |
| Pre-edit status | clean for Core sources; untracked copy of report `10` + evidence dir only |
| Prod Java tree SHA | `1892fd038bc81e874e97b463d3b96b799205d229` (unchanged) |
| Test Java tree SHA (pre) | `fea2557b19608b4a0ef1b14ca789b71f0785e2b2` |
| `XPathPlugin.java` sha256 | `b7044a7949b9b44168856ca92847202fc7df0dace830f22abf440a9f03541714` |
| `ScraperSessionImpl.java` sha256 | `c7a19b5cb4c46d17a8fc8ebe5ee5a72394e5a716def34c9365b391b07d893cc4` |
| JDK / Maven / JaCoCo | OpenJDK **11.0.28**, Maven **3.9.11**, jacoco-maven-plugin **0.8.13** |
| Diagnostic input | `reports/stabilization/10-jacoco-branch-discrepancy.md` (copied from Option B wt; not in `3859aac` tree) |

Pre-edit record: `evidence/jacoco-repair/pre-edit-state.txt`

---

## 2. Baseline and clean-build reproduction

Command (targets wiped first):

```bash
rm -rf webharvest-core/target target
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

| Run | BRANCH | Sessions | `jacoco.exec` | Gate | Lines 514 / 253 / 271 |
|-----|--------|----------|---------------|------|------------------------|
| Pre-edit baseline | **3756 / 4697** (79.9659%) | 2 | 637 562 B | **FAIL** (`0.79 < 0.80`) | 514: mb=1,cb=1; 253: mb=0,cb=2; 271: mb=1,cb=1 |

Matches report `10`: clean baseline is 3756; XPath `startsWith("v:")` false side systematically missed; `ScraperSessionImpl` illegal-state branches flip (this baseline already had 253 covered and 271 half-covered).

Log: `evidence/jacoco-repair/baseline-verify.log`

---

## 3. Source-level explanation of both branches

### 3.1 `XPathPlugin.java:514` — `key.startsWith("v:")` false side

`XPathPluginConfigurationSchema.validate`:

1. Reject null config.
2. If `expression` is non-blank → accept (return).
3. Else iterate property keys; if any key starts with `v:` → accept.
4. Else throw `PluginValidationException`.

Committed Gate C tests covered empty keySet (loop never entered) and `v:`-only / `v:`-first maps (true side only). HashMap iteration on this JDK stably visits `v:doc` before `expression`, so the false side of `startsWith("v:")` was never taken.

**Intended behavior for non-`v:` keys:** keep scanning; if no `v:` key appears and expression is absent/blank, reject. The false branch is therefore the legitimate “ordinary attribute” path (e.g. `id`, `type`).

### 3.2 `ScraperSessionImpl.java:253` — `status != PENDING` in `markRunning`

Documented contract (`@throws IllegalStateException if session is not in PENDING state`):

- PENDING + non-null context → RUNNING, publish started event.
- Any other status → throw; status/context unchanged.

No committed unit test called `markRunning` from a non-PENDING state. Coverage was incidental and flapped with `markCompleted` line 271 (`status != RUNNING`), keeping the bundle at 3756.

### 3.3 Secondary pin — `markCompleted` line 271

Report `10` §3 noted line 271 as a compensating flip. Pre-edit baseline already covered 253 but left 271 half-covered. Covering only the two named gaps would have yielded **3757 / 4697** on that run profile (still below the gate). A third test pins the documented `@throws IllegalStateException if session is not in RUNNING state` path so both illegal-state branches are deterministic.

---

## 4. Tests added (names and assertions)

| Test | File | Assertions / contract |
|------|------|------------------------|
| `xpathSchemaRejectsNonVariableAttributesWithoutExpression` | `GateCStabilizationResidualBatch2ContractTest` | Config with `id` + `type`, no expression / no `v:*` → `PluginValidationException` (`expectedExceptions`) |
| `testMarkRunning_WhenNotPending_ThrowsIllegalStateException` | `ScraperSessionTest` | After PENDING→RUNNING, second `markRunning` → `IllegalStateException` message contains `Cannot mark as running from state: RUNNING`; status remains RUNNING; context unchanged |
| `testMarkCompleted_WhenNotRunning_ThrowsIllegalStateException` | `ScraperSessionTest` | From PENDING, `markCompleted` → `IllegalStateException` message contains `Cannot mark as completed from state: PENDING`; status remains PENDING |

Surefire (both post-fix runs): all three **PASS** (`TEST-TestSuite.xml` / `testng-results.xml`).

---

## 5. Files changed and diff summary

**Committed (this repair):**

| Path | Change |
|------|--------|
| `webharvest-core/src/test/java/org/webharvest/plugin/core/GateCStabilizationResidualBatch2ContractTest.java` | +9 lines (one test) |
| `webharvest-core/src/test/java/org/webharvest/runtime/session/ScraperSessionTest.java` | +32 lines (two tests) |
| `reports/stabilization/11-jacoco-coverage-repair.md` | this report |

**Worktree-local evidence (not committed):** `reports/stabilization/evidence/jacoco-repair/*` — baseline/run logs, `.exec`/XML copies, pre-edit state. `*.exec` and `*.log` are gitignored; XML/summary copies remain untracked reference material. Report `10` remains an untracked working copy (Option B diagnostic).

**Not changed:** any `src/main/**`, JaCoCo limits (`minimum` 0.80), plugin versions, CI workflows, unrelated tests.

`git diff --stat` (test sources only): `2 files changed, 41 insertions(+)`.

Production file hashes unchanged from pre-edit.

---

## 6. Two independent clean verification runs

Identical procedure each time (full wipe of `webharvest-core/target` and root `target` before Maven):

```bash
rm -rf webharvest-core/target target
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

| | Run 1 | Run 2 |
|--|-------|-------|
| Log | `evidence/jacoco-repair/verify-run1.log` | `evidence/jacoco-repair/verify-run2.log` |
| Maven | **BUILD SUCCESS** | **BUILD SUCCESS** |
| Surefire | 3555 / Failures 0 / Errors 0 / Skipped 0 | same |
| Failsafe | 255 / Failures 0 / Errors 0 / Skipped 0 | same |
| Checkstyle | 0 violations | 0 violations |
| SpotBugs | Done (pass) | Done (pass) |
| JaCoCo check | All coverage checks have been met | All coverage checks have been met |
| BRANCH | **3758 / 4697** (80.0085%) | **3758 / 4697** (80.0085%) |
| Sessions | **2** (Surefire + Failsafe) | **2** |
| Session IDs | `…-b0cc50b7`, `…-40dbad88` | `…-13c2dccf`, `…-ff8a5bd5` |
| `jacoco.exec` size | 637 566 B | 637 566 B |
| Line 514 | mb=0, cb=2 | mb=0, cb=2 |
| Line 253 | mb=0, cb=2 | mb=0, cb=2 |
| Line 271 | mb=0, cb=2 | mb=0, cb=2 |

Session IDs differ across runs → execution data is newly generated, not appended from the prior verify. Session count remains 2 (not the historical 18-session polluted campaign profile).

Preserved artifacts: `run1-jacoco.exec`, `run1-jacoco.xml`, `run2-jacoco.exec`, `run2-jacoco.xml`.

---

## 7. JaCoCo branch counts and percentages

| Phase | Covered / Total | Ratio | Gate |
|-------|-----------------|-------|------|
| Pre-edit clean baseline | **3756 / 4697** | 79.9659% | FAIL |
| Post-fix clean run 1 | **3758 / 4697** | 80.0085% | PASS |
| Post-fix clean run 2 | **3758 / 4697** | 80.0085% | PASS |
| Historical polluted campaign (reference only) | 3758 / 4697 | 80.01% | n/a (18 sessions) |

Integer branch count is authoritative; both clean post-fix runs match **3758**.

---

## 8. Remaining limitations / uncertainty

1. **Third test beyond the two named gaps:** `testMarkCompleted_WhenNotRunning_ThrowsIllegalStateException` was required for reproducible ≥3758 given report `10`’s secondary flip on line 271 and the observed pre-edit baseline (253 already covered, 271 missed). It asserts the existing documented `@throws IllegalStateException if session is not in RUNNING state` contract only; creates a fresh PENDING session and calls `markCompleted()` — deterministic and order-independent; no production behavior change.
2. **Report `10` is not in commit `3859aac`:** it remains untracked in the Option B worktree; this repair wt holds a working copy for reference (not part of the repair commit).
3. **Push / PR** not performed; coordinator `2.2.x` and Option B integration tip unchanged.
4. Other `ScraperSessionImpl` illegal transitions (e.g. `markFailed` from wrong state) were not expanded; only the gate-relevant branches were pinned.
5. **Final audit** re-verified preserved Run 1/Run 2 JaCoCo XML (BRANCH **3758/4697**; lines 514/253/271 all `mb=0,cb=2`), both verify logs (Surefire 3555 / Failsafe 255 / Checkstyle 0 / SpotBugs `BugInstance size is 0` / coverage gate met / BUILD SUCCESS), and two distinct session IDs per `.exec` without a third Maven run.

---

## 9. Final GO / NO-GO

| Criterion | Result |
|-----------|--------|
| Tests exercise previously uncovered branches (514 false; 253 non-PENDING; 271 non-RUNNING pinned) | **Yes** |
| No unrelated production changes | **Yes** |
| No new failures / errors / skipped | **Yes** (3555 + 255, 0/0/0) |
| Checkstyle + SpotBugs pass | **Yes** |
| JaCoCo ≥ **3758 / 4697** | **Yes** (both clean runs) |
| Two independent cleaned verifies, no accumulated exec | **Yes** (2 sessions each, distinct session IDs) |
| Thresholds unchanged | **Yes** |
| Minimal explainable test deltas | **Yes** (+41 test lines) |

**Decision: GO (PASS).** Local commit created after final audit; do not merge/push without further instruction.
