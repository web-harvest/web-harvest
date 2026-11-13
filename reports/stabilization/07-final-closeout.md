# Phase 8 — Final closeout

## Starting state

| Item | Value |
| --- | --- |
| Main checkout path | `/Users/robertbala/src/web-harvest-git` |
| Starting branch / HEAD | `2.2.x` @ `1ad20f84e03206a5e1be2d7a93f0098684d43004` |
| Backup bundle | `/Users/robertbala/src/web-harvest-backups/stabilization-checkpoint-20261113-213218/` |
| Implementation worktree | `/Users/robertbala/src/web-harvest-stabilization-wt` |
| Integration baseline | `maintenance/repo-cleanup-stabilization` @ `6eac8c5` |

## Implemented (verified)

1. Re-applied #81 Gitleaks fixture remediation onto Core tip.
2. Replaced tip README with canonical GitHub overview from `2.2.x`.
3. Removed accidental `CallPluginTest.java.backup`.
4. Added meaningful residual Core/CommonUtil contract tests (+34 Surefire tests vs tip).
5. CI workflow: concurrency + `quality/**` / `maintenance/**` triggers.
6. Full local Core validate / Surefire / Failsafe / verify evidence recorded.
7. Gitleaks worktree + history audit recorded.
8. Branch/PR/security/CI reports under `reports/stabilization/`.

## Not done / owner approval required

- Change GitHub default branch or rename/delete remotes.
- Merge any PR.
- History rewrite for historical Stripe-shaped fixture.
- Delete SourceForge website/publish scripts (Issue #79).
- Reach JaCoCo **80%** branch (measured **79.14%**, ~41 branches short).
- Depend on GitHub Actions runners (billing block separate).

## Final Maven snapshot

See `03-core-quality.md`. Verify exit **1** solely due to JaCoCo branch gate.

## Reports index

- `00-initial-state.md`
- `01-branch-pr-inventory.md`
- `02-build-test-baseline.md`
- `03-core-quality.md`
- `04-repository-hygiene.md`
- `05-security-publication.md`
- `06-ci-assessment.md`
- `07-final-closeout.md`
