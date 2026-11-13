# Phase 1–2 — Branch and pull-request inventory

**Evidence date:** 2026-11-13  
**Default branch (GitHub):** `2.2.x` @ `1ad20f84e03206a5e1be2d7a93f0098684d43004`  
**Highest local Core stack tip:** `maintenance/repo-cleanup-stabilization` @ `6eac8c5fd3ff27b79715500005abb736b114ed27`

## Recommended long-term structure (owner approval required)

Do **not** mechanically rename `master` → `main`. GitHub default is already `2.2.x`, not `master`.

| Role | Proposed branch | Rationale |
| --- | --- | --- |
| Long-term integration / future default | New `main` created from the **verified Core stack tip after reintegration with `2.2.x`** (today: cleanup tip + cherry-picks of `9c4a645`, `87ec535`/`1ad20f8` intent) | Carries Issue #48 stack, Gate C coverage work, hygiene, CI workflow |
| Maintained release / bugfix line | Keep `2.2.x` until first public tag from the new integration line | Preserves PR ancestry and the only remote head with recent publication docs (#70 subset, #81) |
| Next-gen / visual designer line | Archive or park local `2.3.x` @ `4cd03ed` (already tagged `archive/2.3.x`) | Diverged; not the Core CI stack; unique IDE/layout commits need explicit port or retention |
| Historical | Keep `master` as frozen ancestor (`645baf8`); reachable from `2.2.x` | No unique commits vs `2.2.x` (ahead 0 / behind 6) |

### Migration sequence (commands for owner; **not executed**)

1. Push the verified stabilization branch and open a PR targeting `2.2.x` **or** a newly created `main` that fast-forwards/`merge`s from that tip.
2. After CI green and review: either merge into `2.2.x` then `gh api` rename default → `main` from that tip, **or** create `main` from the tip and switch default once.
3. Retarget any reopened stacked PRs only after parent SHAs are restored on GitHub.
4. Delete remote heads only after archive tags exist and Issue #77 owner checklist is checked.

## Branch matrix (local heads after origin prune)

Classification key: **1** Active implementation · **2** Stacked-PR dependency · **3** Release/integration baseline · **4** Recovery unique · **5** Duplicate reference · **6** Superseded · **7** Archival candidate (owner approval)

| Branch | HEAD | Last commit | Ahead/Behind `2.2.x` | Reachable from tip? | Disposition | Class |
| --- | --- | --- | --- | --- | --- | --- |
| `2.2.x` | `1ad20f8` | 2026-11-13 | 0/0 | baseline | **Release baseline / GitHub default** | 3 |
| `master` | `645baf8` | 2025-10-25 | 0/6 | yes (ancestor) | Keep as historical remote; no unique work | 5 |
| `maintenance/repo-cleanup-stabilization` | `6eac8c5` | 2026-11-13 | 147/6 | tip | **Primary local integration tip**; reopen as PR after re-push | 1 |
| `quality/core-meaningful-branch-coverage` | `60dbbde` | 2026-11-13 | 144/6 | yes (parent of tip) | Required stack parent for Gate C; archived as tip −3 | 2 |
| `stabilization/release-readiness-core` | `ba72428` | 2026-11-11 | 130/6 | yes | Stack parent (PR #74 head) | 2 |
| `implementation/issue-48-*` / `#60`/`#65`/`#68` | various | 2026-11-11 | 66–126 / 6 | yes (except noted) | Stacked ancestors of tip | 2 |
| `implementation/issue-63-ci-event-deduplication` | `35f6094` | 2026-11-11 | 120/6 | **no** (not in quality/maint tip) | Unique CI dedupe commits; preserve; relates to closed #67 / #78 intent | 4 |
| `ci/core-regression-remediation` | `5f7d69d` | 2026-11-09 | 60/6 | yes | Stack ancestor | 2 |
| `recovery/uncommitted-development` | `ad27b50` | 2026-11-08 | 50/6 | yes | Semantic Stage 1 recovery base; keep until tip lands | 4 |
| `ci/bootstrap` | `5b2bb0b` | 2026-11-06 | 44/6 | **no** | Early CI bootstrap; largely superseded by tip workflow | 6 |
| `stabilization/ts-validation-25-26-27` | `7b6e700` | 2026-11-06 | 48/6 | **no** | TS validation line; separate from Core tip | 4 |
| `2.3.x` | `4cd03ed` | 2025-11-24 | 40/6 | yes (ancestor of tip via recovery path) | Local only; tagged `archive/2.3.x`; park | 7 |
| `maintenance/issue-77-gitleaks-remediation` | `8a7f552` | 2026-11-13 | 1/3 | n/a | Content merged via #81 to `2.2.x`; local tip is stale rebase of same fix | 6 |
| `stabilization/comprehensive-cleanup-20261113` | tip advances this session | 2026-11-13 | based on `6eac8c5` + campaign | **Open PR #82** → `2.2.x` | Active implementation; Gate C 80% now locally green | 1 |

**Exact SHA comparison notes**

- Tip is **not** an ancestor of `2.2.x`. Diverged at `645baf8` (merge-base).
- `2.2.x` unique commits not in tip: `05ed9b3`, `d96420f`, `e252967`, `87ec535` (#81), `9c4a645` (PR #70 subset), `1ad20f8` (session artifact removal).
- Archive tags on GitHub preserve many deleted remote tips (`archive/maintenance-repo-cleanup-stabilization` points at older `9f2119d`, **not** current `6eac8c5` — tip advanced after tagging).

## Pull-request dispositions (recommendations only; none closed/rebased by this campaign)

| PR | State | Base ← Head | Disposition |
| --- | --- | --- | --- |
| **#82** | **Open** | `2.2.x` ← stabilization tip | **Primary consolidation PR**; review/merge after owner branch-strategy approval |
| #81 | **Merged** | `2.2.x` ← gitleaks | Done; re-apply equivalent fix onto Core tip (this campaign) |
| #80 | Closed unmerged | quality ← maint cleanup | Reopen equivalent after tip re-push; contains hygiene + Http Surefire restore |
| #78 | Closed unmerged | stab ← CI cost | Do **not** resurrect blindly; verify overlap with #63/#67 first |
| #76 | Closed unmerged | stab ← quality Gate C | Reopen as coverage continuation under Issue #75 |
| #74…#50 | Closed unmerged | stacked Core | Preserve ancestry; land as one consolidated PR onto `2.2.x`/`main` rather than 15 microscopic PRs |
| #70 | Closed unmerged | `2.2.x` ← docs | Docs subset already on `2.2.x` (`9c4a645`); leftover SourceForge script removals **deferred** (Issue #79) |
| #71 | Closed unmerged | README alternate | Superseded by #70 / `9c4a645` |
| IDE #33–#43 | Closed unmerged | designer stacks | Separate workstream; archive tags exist for several |

## Issues

| Issue | Role |
| --- | --- |
| #75 | JaCoCo branch 80% objective — tip historically ~78.84% |
| #77 | Branch/PR hygiene — remote heads already pruned; local+tags retain unique work |
| #79 | Public publication readiness — website/SF scripts deferred |
| #48 | Parent Core CI honesty / stack |
| #61 | Related hygiene inventory (older) |

## Consolidation opportunity

Replace the closed microscopic stack (#50–#74 + #76 + #80) with **one** PR-ready branch (`stabilization/comprehensive-cleanup-20261113` or re-pushed `maintenance/repo-cleanup-stabilization`) targeting `2.2.x`, after integrating the six `2.2.x`-only commits' **behavioral** content (docs README, LICENSE already on tip, gitleaks fixture).
