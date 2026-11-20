# Agent Work Selection, Release Gate and PR Workflow

**Status:** Maintainer operating policy  
**Scope:** All agents and contributors in `web-harvest/web-harvest`  
**Source of truth:** GitHub repository, Issues, PRs, commits and CI  
**Governing delivery objective:** [Roadmap Issue #1 — Release Stabilization Gate](https://github.com/web-harvest/web-harvest/issues/1)  
**Mandatory entry point:** Root `AGENTS.md`

## 1. Governing objective: Release Stabilization Gate

The objective is to prepare one stable, supportable WebHarvest release. Semantic Model work is one workstream, not the entire project objective. Agents must use the Gate scope and sequence in Issue #1 to select and order work.

The Gate covers:
1. **Implementation and compatibility:** reconcile unfinished in-scope functionality; close serious correctness defects; verify Core/runtime, plugins, XML compatibility, CLI and IDE; defer or retire out-of-scope work explicitly.
2. **Tests and engineering quality:** meaningful unit/integration/compatibility/packaging tests; required CI, coverage and static analysis; report narrow versus broad diagnostics honestly.
3. **Dependencies and security:** current direct/transitive dependency graph and shipped artifacts; reachability-based triage; compatibility-safe upgrades; document residual risks.
4. **Release engineering:** reproducible build/versioning; GitHub tags/releases/assets/checksums; Maven publication and consumer resolution; inspect actual artifacts and POM metadata.
5. **Documentation:** align installation, quick start, module/plugin/CLI/IDE, compatibility, changelog and migration docs with the exact candidate.
6. **Website:** update only after release facts and docs are verified; align version, features and links with the actual release.

Sequence: inventory/scope → implementation/compatibility stabilization → quality/security blockers → release build and GitHub/Maven distribution verification → documentation → website → final evidence-based GO/NO-GO.

A green CI run or empty PR queue is not release readiness. GO requires evidence for the exact candidate: successful build and required tests/security checks, intended artifact contents, correct publication/resolution, and matching docs/site. Any release-critical unknown is NO-GO or requires an explicit, recorded maintainer exception.

No agent may publish a release, push a release tag, deploy Maven artifacts, alter signing credentials/keys, deploy production website changes or change DNS without explicit maintainer authorization. Preparing and testing the process/artifacts is permitted.

## 2. Non-negotiable engineering sequence

> Inspect first. Decide second. Implement third. Verify last.

GitHub is the only upstream and source of truth. SourceForge is legacy only: never fetch, sync, cite as upstream or use as fallback.

At session start:
1. Read `AGENTS.md` and this workflow.
2. Read Issue #1 and its Release Stabilization Gate.
3. Inspect current `main` SHA, open PRs and relevant CI.
4. Read target issue and all material comments, linked PRs and dependencies.
5. Revalidate issue scope and evidence against current `main`.

## 3. Priority model

Every actionable issue should have exactly one priority label when repository labels support it:

| Label | Meaning | Scheduling |
|---|---|---|
| `priority:P0` | Critical security exposure, data loss/corruption, severe release blocker with evidence | Immediate triage |
| `priority:P1` | Core/runtime correctness, compatibility, serious security remediation, verified active Gate blocker | First implementation queue |
| `priority:P2` | Normal maintenance, CI, dependency hygiene, release/docs work not currently blocking | After P0/P1 |
| `priority:P3` | Later product, IDE or plugin capabilities not blocking the release | Later |
| `priority:P4` | Proposal/exploration without accepted contract | Backlog; never auto-implement |

Priority describes impact, not effort, age, readiness or wording. Gate dependency can order work within a priority and can elevate a verified release blocker after evidence-based triage. Never invent priority.

## 4. Readiness model

Use exactly one readiness classification:
- `status:ready`: bounded scope, current evidence, acceptance criteria, dependencies and verification plan exist.
- `status:needs-analysis`: audit, reproduction, design comparison or maintainer decision is required.
- `status:blocked`: concrete prerequisite prevents progress; name the unblock condition.
- `status:deferred`: intentionally outside current delivery horizon; state reconsideration trigger.

High priority does not imply ready. Never create implementation PRs for needs-analysis, blocked or deferred issues.

## 5. Type and area metadata

Use applicable `type:bug`, `type:security`, `type:feature`, `type:maintenance`, `type:audit`, `type:docs`, `type:architecture` and existing `area:*` labels. Do not create labels ad hoc. If labels are unavailable, record a **provisional** priority/readiness classification in the issue body and report the missing label; body fields do not mean GitHub labels were applied. Issue forms collect triage metadata but do not automatically apply labels.

## 6. Issue quality and lifecycle

Before marking ready, an issue must contain:
1. Objective/problem and intended outcome.
2. Evidence (paths, call sites, reproduction, CI); distinguish FACT/INFERENCE/UNKNOWN/PROPOSAL.
3. Scope and explicit non-goals.
4. Observable acceptance criteria.
5. Dependencies and related issues/PRs.
6. Verification plan, including manual checks.
7. Compatibility/security risks or "none identified".
8. Gate workstream and release impact/dependency.

For every open issue, inspect body, comments, linked PRs, current `main` and evidence:
- Close completed only when all acceptance criteria are satisfied; cite fixing PR/commit and verification.
- Close duplicates/obsolete/superseded only with reason and canonical links.
- Retain historical decision records only while they have a real unresolved deliverable.
- Rewrite/split partially completed work; do not close because a related PR merged.
- Never mass-close or bulk-relabel without individual verification.

## 7. Work selection algorithm

1. Inspect current `main`, open PRs, applicable required checks and relevant issue activity.
2. Build the queue from verified priority/readiness metadata. Until labels are fully provisioned, use explicitly provisional body-based triage.
3. Exclude completed, duplicate, superseded, blocked, deferred and needs-analysis work from implementation selection.
4. Select highest priority, then Gate dependency/release impact, then active roadmap stage, then oldest genuinely ready issue. Evidence-backed urgent security work may supersede this order.
5. Choose one bounded issue and reconfirm it against current `main`.
6. If no issue is ready, perform a bounded analysis/reconciliation task and update GitHub; do not invent implementation.

Do not select solely by age, convenience, title prefix, or Semantic Model stage.

## 8. Branch and PR workflow

- Use a short-lived branch from current `main`; one bounded issue per PR by default.
- PR body states issue link, problem/evidence, scope/non-goals, implementation, compatibility/security risks, tests/commands actually run, skipped/pending/environment-limited checks and exact head SHA.
- Use `Closes #N` only when the PR fully satisfies the issue.
- Update the canonical issue with PR link, status, dependencies, evidence and next action.

## 9. Technical self-review, verification and merge

For ordinary bounded FF work, a separate independent reviewer is **not required by default**. The implementing agent performs a documented technical self-review. This is not independent review and must never be represented as such.

Self-review checklist, proportionate to risk:
- Read the complete diff and inspect affected call sites/contracts.
- Check error paths, resource lifecycle, concurrency and security implications where relevant.
- Check API/XML/plugin/runtime compatibility and packaging effects where relevant.
- Confirm tests exercise behavior rather than merely implementation details.
- Inspect generated artifacts/configuration changes and ensure no secrets or unrelated files.
- Record findings and their resolution in the PR or issue.

Escalate to the maintainer when the change entails unresolved product/architecture choices, high-risk security judgment, release publication, production deployment, destructive actions or missing authorization. Do not require a second reviewer merely because one is unavailable.

Before merge:
- Verify all applicable required checks on the exact current PR head SHA. A new commit invalidates earlier verification.
- Resolve substantive findings; no unresolved correctness/security blocker.
- Do not merge with failing, pending or missing required checks.
- Preserve coverage, tests, security scans and static-analysis gates. Never weaken them to get green CI.
- Branch protection is deferred; do not enable/change it without explicit authorization.

## 10. Fire-and-forget CI cadence

1. After a commit/PR, record exact SHA and run links when available.
2. Continue independent work rather than polling/waiting on unrelated Actions.
3. Revisit runs at merge, Gate-completion and closeout decision points.
4. Classify queued/in-progress/success/failure/cancelled/skipped accurately.
5. Never merge or declare a gate satisfied without successful evidence.

## 11. Dependency and security work

- No blanket dependency upgrades.
- Establish current direct/transitive/test dependency trees and actual shipped runtime packaging.
- Map vulnerable dependency use sites and exploit preconditions; classify reachable, conditionally reachable, not reachable or unknown.
- Check fixed versions and Java/runtime compatibility before migration.
- Separate library families into focused PRs; compare effective POMs and resolved trees.
- Keep residual findings visible until verified remediation.

## 12. Durable records and communication

- Issues, PRs, commits and repository docs are English; maintainer coordination in chat may be Polish.
- Record decisions, evidence, blockers, tests, status and next action in GitHub, not only in chat.
- Never invent reviewers, approvals, CI results, labels, milestones, tests or capabilities.
- If a tool/permission is unavailable, state the limitation and continue safe independent work.

## 13. Required preflight and closeout

Before implementation:
- [ ] Read `AGENTS.md`, this workflow and roadmap #1 Gate.
- [ ] Inspect current `main`, open PRs, target issue/comments and linked work.
- [ ] Confirm issue validity, priority, readiness, Gate workstream/dependency and release impact.
- [ ] Confirm evidence, scope, non-goals, acceptance and verification plan.

Before merge/closeout:
- [ ] Exact current PR head SHA recorded.
- [ ] Applicable required checks succeeded on that SHA.
- [ ] Technical self-review performed and substantive findings resolved.
- [ ] Acceptance criteria demonstrated with evidence.
- [ ] Canonical issue updated with status, results, dependencies and next action.
- [ ] Any release/production action has explicit maintainer authorization.
