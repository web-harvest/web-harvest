# Phase 4 — Repository hygiene

## Verified changes in this campaign

| Change | Evidence |
| --- | --- |
| Replace tip README (session-changelog style) with canonical overview from `origin/2.2.x` (`9c4a645` intent) | Diff vs tip `README.md` |
| Remediate Stripe-shaped API key fixture on Core tip (same intent as merged #81) | `HttpRedirectIntegrationBug36Test` |
| Remove accidental `CallPluginTest.java.backup` | `git rm` |
| CI: concurrency cancellation; push/PR triggers for `quality/**` and `maintenance/**` | `.github/workflows/semantic-core-ci.yml` |
| Add residual meaningful Core contract tests | `StabilizationResidualBranchContractTest` |
| Gate C residual batch 2 (session continuation) | `GateCStabilizationResidualBatch2ContractTest`, factory/registry/Stage1 equals contracts |
| Stabilization reports under `reports/stabilization/` | this tree |

## Explicitly preserved (do not delete without Issue #79)

- `SOURCEFORGE-SETUP.md`
- `SFTP-UPLOAD-INSTRUCTIONS.md`
- `upload-docs.sh`, `publish-release.sh`, `update_footers.sh`
- Website content under `docs/web/` that still mentions historical hosting

## Observed but not broadly refactored

- Large historical docs (`BUGS_FIXED.md` ~8106 lines) remain; useful as project history, not release-facing
- Session archives under `docs/archive/` and some module-local `SESSION_*` / `V2.2_*` notes
- Duplicate local untracked `target 2/` dirs in the main checkout (macOS copies) — not committed; recommend deleting locally
- `docs/REPO_CLEANUP_INVENTORY.md` is stale relative to closed PRs; superseded by these reports

## Maven / SCM

- Root `<url>` / `<scm>` already point at GitHub on the tip (`6eac8c5`)
- `LICENSE` + `NOTICE` + `licences/` present
- Modules: core, cli, ide, database, ftp, mail, webbrowser, zip, docs

## Unsafe / deferred cleanup

- Do not mass-delete SourceForge operational scripts (Issue #79)
- Do not delete local-only stack branches until archive tags cover current tips (`archive/maintenance-repo-cleanup-stabilization` is behind `6eac8c5`)
