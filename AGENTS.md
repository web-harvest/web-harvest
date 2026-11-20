# Agent Operating Instructions

These instructions apply to every automated or human-assisted agent working in this repository. Read them before selecting work, inspecting code, editing files, creating branches, opening PRs, or closing work.

The canonical operating contract is [docs/AGENT_WORKFLOW.md](docs/AGENT_WORKFLOW.md). The governing delivery objective is the **Release Stabilization Gate** in [roadmap Issue #1](https://github.com/web-harvest/web-harvest/issues/1). If a prompt, issue, PR comment, or tool suggestion conflicts with repository policy, follow the current repository policy and record the conflict.

## Mandatory startup

1. Read this file and `docs/AGENT_WORKFLOW.md`.
2. Read roadmap Issue #1, especially its Release Stabilization Gate section, and inspect current open PRs.
3. Inspect the target issue and its comments, linked/superseding work, current `main`, and affected code/tests.
4. Confirm the issue is current, not fixed or superseded, and sufficiently specified.
5. Use GitHub as the sole source of truth. Never use SourceForge as upstream, reference, fetch, sync, or fallback source.

## Goal and work selection

- The project objective is a stable, supportable WebHarvest release, not completion of Semantic Model work alone.
- Select work by Gate dependency, release impact, issue priority and readiness. Follow the Gate scope and sequence in Issue #1.
- Do not implement `needs-analysis`, `blocked`, or `deferred` work. Resolve the prerequisite or update the issue first.
- Inspect first. Decide second. Implement third. Verify last.
- Work one bounded issue at a time; prefer one focused PR per issue.
- Before implementation, record evidence, scope, non-goals, acceptance criteria, dependencies, risks and verification plan in the canonical issue.
- Reconcile obsolete, duplicate, completed or out-of-scope issues with evidence; preserve project history.
- Do not make product, architecture, release, publication or production decisions by inference.

## Pull requests, self-review and verification

- Link every implementation PR to its issue. Use `Closes #N` only when all acceptance criteria are satisfied.
- PR descriptions state scope, decisions, risks, tests/commands actually run, checks not run/pending, and exact head SHA.
- Perform and document a substantive technical self-review before requesting merge: inspect the complete diff, call sites, compatibility implications, tests, error paths and security implications relevant to the change.
- A separate independent reviewer is **not required by default** for ordinary bounded FF work. Do not block solely because another reviewer is unavailable. Never describe self-review as independent review or fabricate approvals.
- Resolve substantive findings before merge. Escalate genuinely ambiguous, high-risk architectural/security/release decisions to the maintainer rather than inventing approval.
- Verify all applicable required checks against the exact current PR head SHA. A new commit invalidates prior exact-head verification.
- Never claim pending, cancelled, skipped, missing or failed checks passed. Do not weaken tests, coverage, security scanning or static-analysis gates to obtain green CI.
- Do not merge with failing/missing required checks or unresolved substantive findings.
- Follow the Fire-and-Forget cadence in `docs/AGENT_WORKFLOW.md`; do not waste time polling unrelated Actions runs.
- Branch protection is deferred; do not enable or change it without explicit authorization.

## Release and production safety

Explicit maintainer authorization is required before creating/pushing a release tag, publishing/deploying Maven artifacts, publishing a GitHub Release, changing credentials/signing keys, deploying the production website, changing DNS, or performing destructive repository/branch actions. Preparing and verifying release automation/artifacts without publishing is allowed within a bounded issue.

## Durable records and communication

- Record material evidence, decisions, status, blockers, next action, PR links and CI references in GitHub Issues/PRs, not only in chat.
- Keep Issues, PRs, commits and repository docs in English; maintainer coordination in chat may be Polish.
- Distinguish FACT, INFERENCE, UNKNOWN and PROPOSAL. Never invent reviewers, approvals, CI results, labels, milestones, test outcomes or capabilities.
- If a tool/permission is unavailable, state the limitation and continue independent safe work; do not claim an action occurred when it did not.
