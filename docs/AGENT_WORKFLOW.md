# Agent Work Selection, Issue Triage and PR Workflow

**Status:** Maintainer operating policy  
**Scope:** All agents and contributors working in `web-harvest/web-harvest`  
**Source of truth:** This repository, its GitHub Issues, PRs, commits and CI. The roadmap is Issue #1.  
**Mandatory entry point:** Root `AGENTS.md`.

## 1. Non-negotiable engineering sequence

> Inspect first. Decide second. Implement third. Verify last.

No agent may begin implementation merely because an issue is open or a user said "continue". First establish current repository state, issue validity, dependencies, and acceptance criteria.

GitHub is the only upstream and source of truth. SourceForge is legacy only: do not fetch, sync, cite as upstream, or use it as a fallback.

## 2. Priority model

Every actionable issue should have exactly one priority label:

| Label | Meaning | Scheduling rule |
|---|---|---|
| `priority:P0` | Critical security exposure, data loss/corruption, or severe production/release blocker with evidence | Triage immediately. Do not assume exploitability; establish reachability and impact before remediation. |
| `priority:P1` | Core/runtime correctness, compatibility, security remediation after triage, or a verified blocker to the active roadmap stage | First implementation queue after P0 triage. |
| `priority:P2` | Normal maintenance, CI, dependency hygiene, docs required for current work, and non-blocking quality improvements | Work after P0/P1. |
| `priority:P3` | IDE/product capabilities, plugin ecosystem, or later roadmap functionality not blocking the active stage | Schedule when the relevant stage is active. |
| `priority:P4` | Proposal, exploration, speculative direction, or work lacking an accepted implementation contract | Backlog only; never auto-implement. |

Priority describes impact/importance, not readiness, effort, age, or urgency inferred from wording. Security issues must be re-triaged against the current dependency graph and reachable code paths; historical scan findings are not proof of current exposure.

## 3. Readiness model

Every actionable issue should have exactly one readiness label:

- `status:ready` — bounded implementation scope, current evidence, acceptance criteria, dependencies, and verification plan are present.
- `status:needs-analysis` — a code audit, reproduction, design comparison, or maintainer decision is required before implementation.
- `status:blocked` — a concrete external prerequisite or dependency prevents progress; explain what and who/what can unblock it.
- `status:deferred` — intentionally outside the active delivery horizon; state the trigger for reconsideration.

An issue can be high priority and not ready. Never create an implementation PR from `needs-analysis`, `blocked`, or `deferred` work.

## 4. Type and area metadata

Use one or more type labels where applicable:
- `type:bug`, `type:security`, `type:feature`, `type:maintenance`, `type:audit`, `type:docs`, `type:architecture`.

Preserve and use the existing `area:*` labels (for example Core, IDE, CI, docs) where relevant. Do not replace useful existing taxonomy. Do not create labels ad hoc; if a required label does not exist, report it and use the issue body until repository labels are provisioned.

## 5. Issue quality contract

Before an issue becomes `status:ready`, it must contain:

1. **Objective / problem:** what user or engineering outcome is required.
2. **Evidence:** reproducible behavior, source paths/call sites, CI run or concrete observation; distinguish FACT, INFERENCE, UNKNOWN and PROPOSAL.
3. **Scope:** included work and explicit non-goals.
4. **Acceptance criteria:** observable, testable outcomes.
5. **Dependencies:** prerequisite issues, decisions, modules, or external services.
6. **Verification plan:** exact relevant tests/workflows and any manual checks.
7. **Compatibility and risk:** runtime/API/XML/plugin/security effects, or explicitly "none identified".
8. **Relationship links:** related, duplicate, superseding, or fixing PRs/issues.

If any required element is unknown, keep the issue at `status:needs-analysis` or `status:blocked`. Do not fabricate certainty to make it ready.

## 6. Issue lifecycle and stale-work reconciliation

For each open issue, inspect the body, comments, linked PRs, current `main`, and test evidence.

- **Completed:** close as completed only when current code and evidence satisfy all acceptance criteria; link the fixing PR/commit and record what was verified.
- **Duplicate:** close as duplicate and link the canonical issue.
- **Obsolete / no longer planned:** close as not planned only with a concise reason and, where applicable, a replacement issue or roadmap decision.
- **Superseded:** update the old issue with a link to the replacement and close only when no unique work remains.
- **Historical audit / decision record:** retain open only if it still has an explicit unresolved deliverable; otherwise preserve the record and close with an evidence-backed explanation.
- **Partially completed:** split or rewrite the remaining work; do not close merely because some related PR merged.

Never mass-close or bulk-relabel issues without individual verification. Preserve history and avoid duplicate tracking.

## 7. Work selection algorithm

At the start of each work cycle:

1. Inspect current `main` SHA, open PRs, required checks and relevant issue activity.
2. Identify issues with `status:ready` and a priority label. Until labels are fully provisioned, derive a provisional queue from verified issue content and explicitly mark it provisional.
3. Exclude duplicates, completed, superseded, blocked, deferred, and needs-analysis issues.
4. Select the highest priority (P0 before P1, then P2, P3; P4 is not auto-selected).
5. Within a priority, honor dependency order first, then active roadmap stage, then oldest ready issue. A newer urgent security finding may supersede this order.
6. Choose one bounded issue. Reconfirm it against current `main` before editing.
7. If no issue is genuinely ready, perform a bounded analysis/reconciliation task and update GitHub; do not invent implementation work.

An issue's age, label, title prefix, or placement in the roadmap does not alone establish priority.

## 8. Branch and PR workflow

- Use a short-lived branch from the current `main` for each independent task.
- Prefer one issue per PR. Combine issues only when the changes are inseparable and explain why.
- Keep PR scope narrow; no drive-by refactors.
- PR title should state the change. PR body must include:
  - issue link and `Closes #N` only when fully complete;
  - problem/evidence and implementation summary;
  - scope and non-goals;
  - compatibility/security risks;
  - tests and commands actually run, with results;
  - checks not run, skipped, pending, or environment-limited;
  - exact head SHA at the time of verification.
- Do not mark draft work ready until implementation and self-review are complete.
- Update the issue with PR link and current state.

## 9. Verification and merge policy

- Test at the narrowest relevant level first, then required module/reactor/security/static-analysis gates as specified by the issue and CI.
- Verify checks against the exact current PR head SHA. A new commit invalidates the prior exact-head verification claim.
- Preserve test coverage thresholds, integration tests, static analysis and security scanning. Never hide failures through broad exclusions, weakened assertions, or `continue-on-error`.
- Passing CI does not equal independent review. Obtain an actual eligible reviewer; never claim self-review is independent.
- Do not merge when required checks are failing/pending/missing, or review findings remain unresolved.
- Branch protection is currently deferred by the repository owner. Agents must not enable or modify it without explicit authorization.

## 10. Fire-and-forget CI cadence

Do not make GitHub Actions polling the default activity.

1. After a commit/PR, record the exact SHA and run links when available.
2. Continue immediately with an independent inspection, analysis, documentation, or implementation task.
3. Return to the run at natural decision points: before merge, before declaring a gate complete, and during closeout.
4. Accurately classify runs as queued, in progress, success, failure, cancelled, or skipped.
5. If the next action depends on a running gate, do independent work first and revisit it later.
6. Never merge or declare a required gate satisfied without successful evidence.

## 11. Dependency and security work

- Do not perform blanket dependency upgrades.
- Establish current direct/transitive/test-scope dependency trees and actual runtime packaging.
- Map vulnerable dependency use sites and exploit preconditions; classify reachable, conditionally reachable, not reachable, or unknown.
- Check maintained fixed versions and Java/runtime compatibility before proposing migration.
- Separate library families into focused PRs; compare effective POMs and resolved trees before/after.
- Keep findings visible until verified remediation. Tighten blocking gates only after an evidence-backed baseline and policy exist.

## 12. Communication and durable records

- Issues, PRs, commit messages and repository docs are in English. Maintainer conversation may be Polish.
- Record decisions, evidence, blockers, commands and results in GitHub, not only in chat.
- Use FACT / INFERENCE / UNKNOWN / PROPOSAL labels for material conclusions.
- Be explicit about tool/permission limits. Never invent reviewers, approvals, checks, labels, milestones, test results or repository settings.
- Do not publish releases, change repository settings, delete important branches, or perform other destructive actions without appropriate authorization.

## 13. Required preflight checklist

Before implementation:
- [ ] Read `AGENTS.md` and this document.
- [ ] Read Issue #1 and the target issue including comments.
- [ ] Inspect open PRs and linked/superseding work.
- [ ] Confirm current `main` SHA and reproduce/locate source evidence.
- [ ] Confirm issue is not already fixed or obsolete.
- [ ] Confirm priority, readiness, dependencies, scope and acceptance criteria.
- [ ] Record a verification plan.

Before closing a PR/issue:
- [ ] Verify exact PR head SHA.
- [ ] Required checks succeeded on that SHA.
- [ ] Independent review is complete and findings resolved.
- [ ] Acceptance criteria are demonstrably satisfied.
- [ ] Issue/PR contains durable evidence and correct closure reason.
