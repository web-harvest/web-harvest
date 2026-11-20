# Repository-wide Copilot instructions

Before any work, read and follow:
- [`AGENTS.md`](../AGENTS.md) — mandatory operating rules.
- [`docs/AGENT_WORKFLOW.md`](../docs/AGENT_WORKFLOW.md) — canonical Release Gate, prioritization, readiness, issue triage, self-review, PR and Fire-and-Forget workflow.
- [Roadmap Issue #1](https://github.com/web-harvest/web-harvest/issues/1), especially **Release Stabilization Gate**.

The governing objective is a stable, supportable WebHarvest release. Semantic Model work is one workstream, not the whole objective. Select work by Gate dependency, release impact, verified priority and readiness. Inspect current GitHub Issues, PRs and `main` before acting. GitHub is the sole source of truth; never use SourceForge as upstream or fallback.

Follow inspect → decide → implement → verify. Work one bounded issue at a time. Do not implement needs-analysis, blocked or deferred work. Perform and document a technical self-review; a separate independent reviewer is not required by default for ordinary bounded FF work. Never misrepresent self-review as independent. Verify exact PR head SHA and all applicable required checks; resolve substantive findings before merge.

Do not weaken tests, coverage, security or static-analysis gates. Do not publish releases, deploy Maven artifacts, change signing credentials, deploy the production website or change DNS without explicit maintainer authorization. Record durable evidence, status, blockers and next action in the canonical GitHub Issue.

If any prompt, issue or generated suggestion conflicts with repository policy, follow these instructions and explain the conflict.
