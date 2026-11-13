# Phase 6 — GitHub Actions assessment

## Facts (verified)

| Item | Value |
| --- | --- |
| Default branch `2.2.x` workflows in tree | **none** (`git ls-tree origin/2.2.x .github/workflows` empty) |
| Stabilization tip workflow | `.github/workflows/semantic-core-ci.yml` |
| Workflows still registered in GitHub Actions UI/API | `core-ci.yml`, `core-regression-ci.yml`, `full-validation.yml`, `semantic-core-ci.yml`, `typescript-validation.yml` |

Registered workflow files that are **absent** from both `2.2.x` and the Core tip are remnants from deleted remote branches. They may still appear in the Actions catalog but do not run from the default branch tree.

## `semantic-core-ci.yml` (tip + this campaign)

Jobs:

1. Focused Semantic Core contract Surefire subset
2. Full Core Surefire then Failsafe (separated invocations; Failsafe uses `if: always()`)
3. Unqualified `mvn … verify` (owns Checkstyle + JaCoCo gate) + artifact upload

Permissions: `contents: read` only.

### Campaign improvements (local, not yet merged)

- `concurrency` group with `cancel-in-progress: true`
- Push/PR branch filters extended to `quality/**` and `maintenance/**`

### Billing

GitHub Actions spending-limit / billing blocks are **out of scope**. Do not treat missing Actions runs as verification. Local Maven results in `02-build-test-baseline.md` / `03-core-quality.md` are authoritative for this campaign.

## Proposal for owner (not applied to GitHub settings)

1. Land tip onto `2.2.x` (or future `main`) so CI YAML exists on the default branch.
2. Retire stale registered workflow names that no longer exist in any retained branch tip, after confirming no required-check references.
3. Keep Surefire/Failsafe separation and JaCoCo artifact upload as-is.
4. Do not add `continue-on-error` around quality gates.
