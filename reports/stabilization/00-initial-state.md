# Phase 0 — Initial working state

**Captured (session continuation):** 2026-11-13T21:01Z (local)  
**Prior campaign reports:** already present on `stabilization/comprehensive-cleanup-20261113` @ `c9f67ff`  
**Coordinator working copy:** `/Users/robertbala/src/web-harvest-git` (unchanged checkout)  
**Implementation worktree:** `/Users/robertbala/src/web-harvest-stabilization-wt`  
**Backup (this session):** `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-220111/`  
**Prior backup (untouched):** `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-213218/`

## Verified identity

| Item | Value |
| --- | --- |
| Path | `/Users/robertbala/src/web-harvest-git` |
| `origin` | `https://github.com/web-harvest/web-harvest.git` |
| Repository | `web-harvest/web-harvest` (private) |
| GitHub default branch | `2.2.x` |
| Starting HEAD (`2.2.x`) | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| Also configured (unused) | `sourceforge-legacy` remote — **not fetched / not used** |
| Open PR at session start | [#82](https://github.com/web-harvest/web-harvest/pull/82) on `stabilization/comprehensive-cleanup-20261113` @ `c9f67ff` |

## Working tree at session start

- Main checkout branch: `2.2.x` tracking `origin/2.2.x`
- Staged / unstaged modifications: **none**
- Untracked (preserved, not deleted):
  - `.iteration-evidence/`
  - Duplicate Maven output dirs named `webharvest-*/target 2/`
- Extra worktrees:
  - `/Users/robertbala/src/web-harvest-stabilization-wt` (campaign)
  - `/Users/robertbala/src/web-harvest-stack-repair-wt` on `implementation/issue-48-visual-palette-contract` @ `9814a74`

## Remote heads after `git fetch origin`

| Branch | SHA |
| --- | --- |
| `origin/2.2.x` | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| `origin/master` | `645baf8c61c6f3686907cfa487104ded340d90fe` |
| `origin/stabilization/comprehensive-cleanup-20261113` | `c9f67ff7463d09f868f632f22b17f8d36a7409ed` |

## Checkpoint policy

- New timestamped backup created **before** edits in this continuation
- Main checkout left on `2.2.x` @ `1ad20f8`
- Implementation continued only in the dedicated worktree / branch
