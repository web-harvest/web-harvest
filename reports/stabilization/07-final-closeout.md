# Phase 8 — Final closeout

## Starting state

| Item | Value |
| --- | --- |
| Main checkout path | `/Users/robertbala/src/web-harvest-git` |
| Starting branch / HEAD | `2.2.x` @ `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| Backup (prior campaign) | `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-213218/` |
| Backup (this continuation) | `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-220111/` |
| Implementation worktree | `/Users/robertbala/src/web-harvest-stabilization-wt` |
| Prior tip at session start | `c9f67ff7463d09f868f632f22b17f8d36a7409ed` |
| Integration baseline parent | `maintenance/repo-cleanup-stabilization` @ `6eac8c5` |

## Implemented and verified

1. Preserved main checkout and untracked local evidence; worked only in dedicated worktree/branch.
2. Gate C residual batch 2: meaningful If/JsonToXml/XPath/While/Template/Call/SetVar/factory/registry/Stage1 contracts.
3. Full local Core `verify` **PASS** (Surefire 3552, Failsafe 255, Checkstyle 0, SpotBugs 0).
4. JaCoCo branch coverage **80.01%** (3758/4697); instruction **91.29%**; threshold unchanged.
5. Working-tree Gitleaks re-scan: 0 findings.
6. Reports under `reports/stabilization/` updated with exact commands/exits.

## Not done / owner approval required

- Change GitHub default branch or rename/delete remotes.
- Merge any PR (including #82).
- History rewrite for historical Stripe-shaped fixture.
- Delete SourceForge website/publish scripts (Issue #79).
- Depend on GitHub Actions runners (billing block separate).
- Full multi-module reactor verification beyond Core.

## Final Maven snapshot

See `03-core-quality.md`. Verify exit **0**; JaCoCo branch gate met.

## Reports index

- `00-initial-state.md`
- `01-branch-pr-inventory.md`
- `02-build-test-baseline.md`
- `03-core-quality.md`
- `04-repository-hygiene.md`
- `05-security-publication.md`
- `06-ci-assessment.md`
- `07-final-closeout.md`

## Final campaign HEAD

| Item | SHA |
| --- | --- |
| Campaign tip (final pushed) | `2f1ae5eb350795cdfb29dc003305f0861f617f8b` |
| Reports evidence commit | `19fc3571994557f0cc1cb5b3f450ba299764418f` |
| Tests commit | `66caefdd7bcabd4092154c9500a104b76bd83cf9` |
| Prior tip at session start | `c9f67ff7463d09f868f632f22b17f8d36a7409ed` |
| Parent tip | `6eac8c5fd3ff27b79715500005abb736b114ed27` |
| Default `2.2.x` | `1ad20f84e03206a5e1be2d7a93f0098684d43004` |

## Published artifacts

- PR: https://github.com/web-harvest/web-harvest/pull/82
- Issues: #75 (Gate C), #77 (hygiene), #79 (publication)
