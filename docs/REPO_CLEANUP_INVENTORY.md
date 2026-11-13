# Repository cleanup inventory (2026-11-13)

Evidence-driven triage for the Core stabilization stack tip
`quality/core-meaningful-branch-coverage` (`60dbbde`) and the dedicated cleanup
branch `maintenance/repo-cleanup-stabilization`.

## Baseline

| Item | Value |
| --- | --- |
| Repository | `web-harvest/web-harvest` |
| Default branch | `2.2.x` @ `e252967` |
| Cleanup base | `origin/quality/core-meaningful-branch-coverage` @ `60dbbde` |
| Worktree | clean at branch creation |
| GitHub Actions | blocked by account spending limit (do not treat as verification) |

## Open PR ownership (do not duplicate)

| PR | Head | Classification | Notes |
| --- | --- | --- | --- |
| #70 | `docs/github-only-contribution-flow` | active / required | Owns SourceForge publishing retirement, README rewrite, LICENSE, SCM metadata against `2.2.x` |
| #76 | `quality/core-meaningful-branch-coverage` | active / required | Meaningful Core branch-coverage Gate C |
| #78 | `maintenance/semantic-core-ci-cost-control` | active / required | CI event deduplication + stale-run cancellation on stabilization base |
| #74…#50 | Core implementation stack | active / required | Stacked Core contracts; tip is stabilization/quality |
| #47 | `implementation/issue-46-metadata-contract` | uncertain — preserve | WIP metadata contract |
| IDE stack #33–#43 | visual designer | active / separate | Not in scope of Core cleanup |

## Recovery / docs branches

| Branch | Classification | Evidence |
| --- | --- | --- |
| `docs/readme-current-project-state` | superseded by PR #70 intent | Overlaps README/SourceForge guide cleanup; not an ancestor of PR #70 |
| `recovery/full-validation-probe` | unique work — preserve | Full-validation workflows + IDE/HTTP fixture experiments; **not** ancestor of quality tip |
| `recovery/ide-http-test-fixtures` | partially represented | HTTP fixture ideas re-landed via `implementation/issue-48-http-test-fixtures` on the Core stack; remaining IDE/workflow pieces stay unique |
| `recovery/*` experiment branches | obsolete candidates — owner approval | CI frontend experiments; do not delete without owner decision |

## Cleanup actions on this branch

1. Point Maven `<url>` / `<scm>` at GitHub (same intent as PR #70, applied on the Core stack tip).
2. Replace SourceForge-centric `CONTRIBUTING.md` with GitHub-canonical contribution flow.
3. Expose root `LICENSE` (BSD text already present on PR #70).
4. Remove ephemeral AI prompts, NiFi research dumps, and abandoned session planning notes from the repository root.
5. Restore offline-safe `Http*` unit tests to Surefire by removing the blanket `**/*Http*.java` exclusion; keep `excludedGroups=network` and Failsafe ownership of `*IntegrationTest`.
6. Add meaningful Handler / CallPlugin / JsonGenerator / CommonUtil / SetVar registration contract tests for residual gaps.
7. Leave SourceForge operational scripts (`upload-docs.sh`, `publish-release.sh`, `SFTP-*`, `SOURCEFORGE-SETUP.md`) for PR #70 to retire against `2.2.x`.

## Local verification (cleanup branch tip)

Command: `mvn -B -ntp -pl webharvest-core -am verify`

| Check | Result |
| --- | --- |
| Surefire | 3517 tests, 0 failures, 0 errors, 0 skipped |
| Failsafe | 255 tests, 0 failures, 0 errors, 0 skipped |
| Checkstyle | 0 violations |
| SpotBugs | analysis completed (`failOnError` remains false in POM; not strengthened here) |
| JaCoCo instruction | ~90.7% (threshold 80% — met) |
| JaCoCo branch | **78.84%** (3703/4697) — threshold 80% **not met** |
| Overall verify | **FAIL** solely on JaCoCo branch gate |

Frontend: `npm run type-check` passed; `npm run lint` reports many pre-existing errors (not introduced by this cleanup).

GitHub Actions: account spending limit — workflows not used as verification.

## Coverage residual after cleanup

- `CallPlugin` ~73% branch
- `SemanticXmlAnalyzer.Handler` ~71% branch
- `PluginMetadataJsonGenerator` ~67% branch
- `PluginMetadataCatalog` / `HttpPlugin` / `CommonUtil` ≥80% branch
- Further Gate C lift remains owned by PR #76 / issue #75
