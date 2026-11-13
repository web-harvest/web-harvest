# Phase 0 — Initial working state

**Captured:** 2026-11-13T20:32Z (local)  
**Coordinator working copy:** `/Users/robertbala/src/web-harvest-git` (unchanged checkout)  
**Implementation worktree:** `/Users/robertbala/src/web-harvest-stabilization-wt`  
**Backup:** `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-213218/web-harvest-all-refs.bundle`

## Verified identity

| Item | Value |
| --- | --- |
| Path | `/Users/robertbala/src/web-harvest-git` |
| `origin` | `https://github.com/web-harvest/web-harvest.git` |
| Repository | `web-harvest/web-harvest` (private) |
| GitHub default branch | `2.2.x` |
| Starting HEAD (`2.2.x`) | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| Also configured (unused) | `sourceforge-legacy` remote — **not fetched / not used** |

## Working tree at campaign start

- Branch: `2.2.x` tracking `origin/2.2.x`
- Staged / unstaged modifications: **none**
- Untracked (preserved, not deleted):
  - `.iteration-evidence/` (local evidence; already gitignored)
  - Duplicate Maven output dirs named `webharvest-*/target 2/` (macOS copy artifacts; not committed)
- Extra worktree: `/Users/robertbala/src/web-harvest-stack-repair-wt` on `implementation/issue-48-visual-palette-contract` @ `9814a74`

## Remote vs local after `git fetch origin --prune`

**GitHub heads present after prune:**

| Branch | SHA |
| --- | --- |
| `origin/2.2.x` | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| `origin/master` | `645baf8c61c6f3686907cfa487104ded340d90fe` |

All previously pushed Core / recovery / quality / maintenance heads were **absent from GitHub** after prune. Tips remain as **local branches** and many as `archive/*` tags.

## Open / closed PRs relevant to current line

- Open PRs: **none** (`gh pr list --state open` → `[]`)
- Merged into `2.2.x` recently: **#81** (Gitleaks Stripe-shaped fixture remediation)
- Closed without merge (selected): #70 (docs; subset later landed on `2.2.x`), #74–#80 Core stack / quality / cleanup, #76 Gate C coverage, #78 CI cost control

## Local work not present on GitHub

Entire Core stabilization stack tip through `maintenance/repo-cleanup-stabilization` @ `6eac8c5` (**147 commits ahead** of `origin/2.2.x`, **6 behind**). Also local `2.3.x`, recovery, CI bootstrap, and Issue #48 implementation branches (see `01-branch-pr-inventory.md`).

## Checkpoint policy

- Timestamped `git bundle --all` created **before** edits
- Main checkout left on `2.2.x` @ `1ad20f8`
- Implementation performed only in dedicated worktree / branch `stabilization/comprehensive-cleanup-20261113` based on `maintenance/repo-cleanup-stabilization`
