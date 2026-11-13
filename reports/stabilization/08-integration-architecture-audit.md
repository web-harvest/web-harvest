# Final integration architecture audit — 2026-11-13

**Role:** decision gate only. No `main` creation, no merge, no default-branch change, no history rewrite.

**Auditor checkout:** `/Users/robertbala/src/web-harvest-git` on `2.2.x`  
**Campaign tip under review:** `origin/stabilization/comprehensive-cleanup-20261113` @ `a3b2977b26e42cf69b792898211aab77c82a6284`  
**Known campaign tip (prior closeout):** `2f1ae5eb350795cdfb29dc003305f0861f617f8b` (ancestor; +1 docs commit = current tip)  
**Default branch:** `2.2.x` @ `1ad20f84e03206a5e1be2d7a93f0098684d43004`  
**Repository:** `web-harvest/web-harvest` — **private** (verified via `gh repo view`)  
**PR #82:** base `2.2.x` @ `1ad20f8` ← head `stabilization/comprehensive-cleanup-20261113` @ `a3b2977` — state OPEN, mergeable **CONFLICTING**

---

## 1. What was inspected

| Surface | Method |
| --- | --- |
| Local + remote branches | `git fetch origin --prune --tags`; `git branch -vv`; `git branch -r` |
| Archive tags | `git tag -l` (17 tags; 15 `archive/*`) |
| Ahead/behind | `git rev-list --left-right --count origin/2.2.x...origin/stabilization/comprehensive-cleanup-20261113` |
| Merge base | `git merge-base` |
| Six `2.2.x`-only commits | `git log` + `git show --stat` each |
| Path overlap / conflicts | `git diff --name-only`; `git merge-tree --write-tree` |
| PR #82 full commit list + files | `gh pr view 82` |
| Local test evidence | `reports/stabilization/evidence/*`, `03-core-quality.md` |
| Uncommitted work (main checkout) | `git status` |

SourceForge remote `sourceforge-legacy` is configured locally but **was not used** for this audit.

---

## 2. What was verified

### 2.1 Graph (reproducible)

```
merge-base = 645baf8c61c6f3686907cfa487104ded340d90fe
             (= origin/master tip; "Fix: Register xq-param and xq-expression plugins")

origin/2.2.x .............. 1ad20f8   (+6 commits from merge-base)
origin/stabilization/... .. a3b2977   (+158 commits from merge-base)

git rev-list --left-right --count origin/2.2.x...origin/stabilization/comprehensive-cleanup-20261113
→ 6    158
```

**158 ahead / 6 behind is reproduced** at current tips.

Relative to known tip `2f1ae5e`: `6 / 157` (campaign tip advanced by docs-only `a3b2977`).

### 2.2 Remote heads (GitHub sole active remotes after prune)

| Ref | SHA |
| --- | --- |
| `origin/2.2.x` (default) | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| `origin/master` | `645baf8c61c6f3686907cfa487104ded340d90fe` |
| `origin/stabilization/comprehensive-cleanup-20261113` | `a3b2977b26e42cf69b792898211aab77c82a6284` |
| `origin/HEAD` | → `origin/2.2.x` |

No remote `main`. No other `origin/*` heads after prune.

### 2.3 Six commits on `2.2.x` absent from campaign

| SHA | Subject | Classification | Unique tree content vs campaign? |
| --- | --- | --- | --- |
| `05ed9b3` | Create maintenance branch 2.2.x for bugfixes | Topology / branch marker | **No** (empty tree delta) |
| `d96420f` | Merge master: Add comprehensive .gitignore | Topology merge (`05ed9b3` + `f8964fb`) | **No** — `.gitignore` already reachable via merge-base ancestry |
| `e252967` | Merge master: Fix XQuery plugin registration and IDE enhancements | Topology merge (`d96420f` + `645baf8`) | **No** — second parent **is** the merge-base |
| `87ec535` | fix(core): replace Stripe-shaped API key fixture (#81) | Security remediation | **No** — blob identical to campaign (`HttpRedirectIntegrationBug36Test.java` empty diff); campaign has equivalent via `2f3e670` |
| `9c4a645` | docs: make GitHub the canonical contribution and SCM location | Publication docs / SCM | **Almost no** — `LICENSE`, `CONTRIBUTING.md`, `README.md` blob-identical; `pom.xml` still differs (see below) |
| `1ad20f8` | chore: remove obsolete root session summary artifacts | Hygiene delete | **Partial** — roots absent on both tips; campaign retains same files under `docs/archive/` → **rename/delete conflict** on merge |

**Conclusion:** There are **no unique Core behavioral commits** on `2.2.x` that are missing from the campaign tree. Remaining reconciliation is **merge topology** + **three archive rename/delete conflicts** + **`pom.xml` content conflict**.

### 2.4 `pom.xml` conflict drivers (ignore-space)

Campaign tip vs `2.2.x` (27 lines net):

- Javadoc plugin configuration (`detectLinks`, Java 11 API link, `failOnError=false`)
- Surefire `argLine` adds `--add-opens …`
- Removes `guice-assistedinject` dependency entry present on `2.2.x`

SCM URLs already GitHub on both tips. JaCoCo **INSTRUCTION 0.80** and **BRANCH 0.80** present on campaign tip (unchanged thresholds).

### 2.5 IDE vs Core

| Fact | Evidence |
| --- | --- |
| `archive/2.3.x` / local `2.3.x` tip `4cd03ed` | **Ancestor of campaign tip** |
| Commits after `4cd03ed` on campaign | **118** |
| Of those touching `webharvest-ide/` | **1** |
| Of those touching `webharvest-core/` | **83** |

**Verdict:** The campaign tip **cannot** be merged without importing the Visual Designer / IDE lineage already contained in history and tree (everything through `4cd03ed`). Post-`2.3.x` work is overwhelmingly Core/CI/docs; that does **not** strip IDE from the resulting tree. A Core-only tip would require a **new** cherry-pick reconstruction onto `2.2.x` (Option C) — not a merge of PR #82 as currently branched.

### 2.6 PR #82 independent review (entire stack)

| Area | Finding |
| --- | --- |
| Core production | Large semantic Stage 1 model, structural validation, runtime root execution, SetVar/While/HTTP fixtures, palette eligibility, CommonUtil — see `webharvest-core/src/main/java` delta vs merge-base (~130 files) |
| Surefire / Failsafe | Root `pom.xml`: Surefire excludes `*IntegrationTest`; Failsafe includes `*IT` / `*IntegrationTest`. CI runs Surefire and Failsafe as **separate** invocations |
| Docs | Consolidation + `docs/archive/` retention; root session summaries archived (conflict with `1ad20f8` delete) |
| LICENSE / NOTICE | `LICENSE` added/aligned; `NOTICE` blob identical across `master` / `2.2.x` / campaign |
| Maven metadata | GitHub `<url>` / `<scm>`; version `2.3.1` on both tips |
| CI | Adds `.github/workflows/semantic-core-ci.yml` (absent on `2.2.x`). Triggers include `2.2.x` PRs + stack branches. **Not** executable under current Actions billing block — local evidence authoritative |
| Security | Historical Stripe-shaped fixture remediated on tip (`2f3e670`); history findings remain (no rewrite) |
| Residual contracts | Open Issues #48/#60/#65/#68/#75 follow-ups; Gate C 80% gate met locally; SourceForge scripts deferred (#79) |

**PR implications today:** merge blocked by conflicts; do not merge until owner selects architecture below and resolves conflict policy.

### 2.7 Test evidence ↔ exact SHA

| Item | Value |
| --- | --- |
| Verified test commit | `66caefdd7bcabd4092154c9500a104b76bd83cf9` (`test(core): close Gate C JaCoCo branch gap to 80%`) |
| Commits after test SHA (docs only) | `19fc357` → `2f1ae5e` → `a3b2977` |
| Surefire | **3552** / 0 / 0 — `evidence/12-verify-after-batch2.summary.txt` |
| Failsafe | **255** / 0 / 0 — same |
| Checkstyle | 0 violations |
| SpotBugs | 0 BugInstance (reported in `03-core-quality.md`) |
| JaCoCo BRANCH | **3758/4697 = 80.01%** — `evidence/13-jacoco-final.summary.txt` |
| JaCoCo INSTRUCTION | **91.29%** |
| Thresholds | **0.80 / 0.80** preserved |

These are **local Maven** results. They are **not** GitHub Actions results.

### 2.8 Uncommitted / untracked (main checkout)

- Branch clean vs `origin/2.2.x`
- Untracked only: `.iteration-evidence/`, `webharvest-*/target 2/` (local Maven duplicates) — **not** part of integration

---

## 3. What remains uncertain

1. **Owner conflict policy** for the three session files: keep `docs/archive/*` (campaign) vs delete entirely (`1ad20f8` intent).
2. **`pom.xml` resolution detail:** keep campaign javadoc/`--add-opens`/no `guice-assistedinject` (recommended default) vs selective restore of assistedinject if something still needs it (no consumer verified in this audit).
3. **Whether IDE Visual Designer state at `4cd03ed+` is acceptable on the future default** — architecture assumes yes if campaign tip becomes `main`; otherwise Option C.
4. **GitHub Actions** green status after billing restored — cannot be claimed now.
5. **Multi-module reactor** beyond `-pl webharvest-core -am` — not re-verified in this audit (prior closeout already marked out of scope).
6. Stale local branches still present locally after remote prune — archive-tag coverage for current tips incomplete (e.g. `archive/maintenance-repo-cleanup-stabilization` behind `6eac8c5`).

---

## 4. Recommended branch architecture

| Role | Branch | Policy |
| --- | --- | --- |
| Canonical development / future default | **`main`** (does not exist yet) | Create **only after** integration commit exists (Option B below) |
| Maintained release / compatibility | **`2.2.x`** | Keep at `1ad20f8` until owner tags/releases; accept selected backports only |
| Historical reference | **`master`** | Frozen at `645baf8`; **do not auto-rename** |
| Active integration candidate | `stabilization/comprehensive-cleanup-20261113` | Remains PR #82 head until owner merges/retargets |
| IDE / Visual Designer | Contained **inside** campaign ancestry (`archive/2.3.x`); treat further IDE-only work as separate PRs onto `main` after cutover | Do not attempt to strip from campaign merge |

---

## 5. Migration alternatives

### Option A — Merge full campaign into `2.2.x`, then establish `main`

| Item | Value |
| --- | --- |
| Base | `2.2.x` @ `1ad20f8` |
| Incoming | campaign @ `a3b2977` |
| Resulting graph | merge commit `M` with parents `(1ad20f8, a3b2977)`; then `main` → `M` (or FF `2.2.x` to `M` then point `main`) |
| Unique commits preserved | All 158 campaign + 6 `2.2.x` (via first parent) |
| Conflicts | 3× rename/delete session archives; `pom.xml` |
| Semantic risks | `2.2.x` ceases to be a thin maintenance line; carries full IDE+Core stack |
| PR | Resolve on #82 targeting `2.2.x`, then create `main` from merge tip |
| Rollback | Reset default back to pre-merge `2.2.x` SHA; leave merge commit reachable via PR branch / tag |

### Option B — Establish `main` from verified integration commit (campaign + `2.2.x` reconciliation) — **RECOMMENDED**

| Item | Value |
| --- | --- |
| Base for integration work | campaign @ `a3b2977` |
| Integrate | merge `origin/2.2.x` **into** campaign (or equivalent conflict-resolved commit) → `I` |
| Create `main` at | `I` |
| Leave `2.2.x` at | `1ad20f8` (release line) until explicit release decision |
| Unique commits preserved | All campaign commits + merge records `2.2.x` six-commit line as second parent |
| Conflicts | Same four paths as A; resolve on campaign side |
| Semantic risks | Lower for release line purity; `main` openly carries IDE+Core. PR #82 base should be retargeted to `main` **or** closed in favor of a `main`-targeted PR after `main` exists |
| PR | Prefer: (1) resolve merge on campaign, (2) owner creates `main` from `I`, (3) open/retarget PR into `main` or fast-forward default |
| Rollback | Delete/stop using `main` ref (no force-push required if never force-updated); `2.2.x` untouched |

**Why B over A:** Unique `2.2.x` *content* is already on the campaign tree. Forcing the fat stack through `2.2.x` first conflates “maintenance line” with “next development line.” B matches the intended long-term roles and preserves `2.2.x` as a clean rollback/release pointer.

### Option C — Reconstruct Core-only tip onto `2.2.x` (reject unless owner forbids IDE on default)

| Item | Value |
| --- | --- |
| Base | `2.2.x` @ `1ad20f8` |
| Method | Cherry-pick / replay ~83+ Core commits after `4cd03ed` (and possibly selected pre-`2.3.x` Core commits) **without** merging `4cd03ed` tree |
| Unique commits preserved | New SHAs; original campaign SHAs remain on archive branch/tags |
| Conflicts / risks | High semantic risk (palette metadata, IDE API contracts, workingDir changes shared with IDE); loses linear history of closed stack PRs |
| PR | New PR; #82 would be superseded |
| Rollback | Delete reconstruction branch |

**Not recommended** given evidence: post-`2.3.x` Core work assumes the recovered tree that already includes IDE; stripping IDE is a separate product decision, not required to land Gate C / semantic Core.

---

## 6. Exact migration sequence (Option B — commands for owner; **not executed**)

```bash
# 0) Preconditions
git fetch origin --prune --tags
git switch -C integration/main-cutover origin/stabilization/comprehensive-cleanup-20261113
# tip must be a3b2977b26e42cf69b792898211aab77c82a6284 (or newer docs-only)

# 1) Reintegrate 2.2.x topology (expect conflicts)
git merge origin/2.2.x
# Resolve:
#  - docs/archive/FINAL_COVERAGE_SESSION_SUMMARY_r792-r802.md
#  - docs/archive/SESSION_COVERAGE_SUMMARY_r792-r796.md
#  - docs/archive/SESSION_FINAL_COMPLETE_r792-r808.md
#    Recommended: KEEP archive copies (campaign).
#  - pom.xml — Recommended: KEEP campaign tip version
#    (javadoc config, --add-opens, no guice-assistedinject).

# 2) Verify locally (authoritative while Actions blocked)
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
# Expect: Surefire 3552+, Failsafe 255, Checkstyle 0, SpotBugs 0,
#         JaCoCo BRANCH >= 0.80, INSTRUCTION >= 0.80

# 3) Push integration branch (no force)
git push -u origin integration/main-cutover

# 4) OWNER ONLY — create main and optionally switch default
git push origin HEAD:main
# gh repo edit web-harvest/web-harvest --default-branch main   # explicit auth

# 5) PR policy after main exists
#  - Retarget or replace #82 to base=main, or FF main from integration tip
#  - Keep 2.2.x at 1ad20f8 until release tagging policy is decided
#  - Do not delete master / archive tags
```

**Recovery:** If `main` cutover is rejected, leave `2.2.x` as default; keep campaign branch + archive tags; no force-push required.

---

## 7. GO / STOP decision

### STOP (current state)

- **Do not merge PR #82 as-is** — `mergeable: CONFLICTING`.
- **Do not create `main`, change default branch, delete branches, or rewrite history** without explicit owner authorization.
- **Do not treat local Maven as GitHub Actions**.

### GO (conditional — owner authorization required)

Proceed with **Option B** when owner explicitly approves:

1. Conflict resolution policy (archive keep + campaign `pom.xml`), and  
2. Acceptance that `main` will contain the IDE/Visual Designer lineage through `archive/2.3.x`, and  
3. Creation of `main` + default-branch switch as a separate authorized step.

**Audit recommendation:** **CONDITIONAL GO on Option B**; **STOP on Option A as default path**; **STOP on Option C** unless IDE must be excluded from the default line.

---

## 8. Canonical SHA checklist

| Ref | SHA |
| --- | --- |
| `2.2.x` / PR base | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| `master` / merge-base | `645baf8c61c6f3686907cfa487104ded340d90fe` |
| Campaign tip (PR head) | `a3b2977b26e42cf69b792898211aab77c82a6284` |
| Prior closeout tip | `2f1ae5eb350795cdfb29dc003305f0861f617f8b` |
| Gate C verify / test evidence | `66caefdd7bcabd4092154c9500a104b76bd83cf9` |
| `archive/2.3.x` | `4cd03edea7f9d26fa768235bf7b541bd359481f2` |
