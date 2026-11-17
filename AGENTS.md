# Agent Operating Instructions

## Execution cadence: fire-and-forget CI

Work independently of GitHub Actions completion. Do not block the next task on a running CI workflow.

1. After pushing a commit or opening/updating a PR, record its exact commit SHA and Actions run URL when available.
2. Immediately continue with the next independent inspection, analysis, documentation task, or implementation task. Do not poll or wait for CI as the default next step.
3. Check Actions at natural decision points: before merging a PR, before declaring a quality gate complete, and during final closeout.
4. Classify every observed run accurately as pending, in progress, success, failure, or cancelled. Never describe a non-successful or unfinished run as passing.
5. If a task depends on a running gate's result, work on independent tasks first and return to the dependency when the result is available.
6. Do not merge or declare a required gate satisfied without successful required evidence, unless the repository owner explicitly authorizes a documented exception.
7. Avoid unnecessary polling and avoid cancelling useful runs. A superseded run cancelled by configured concurrency is not, by itself, evidence of a CI loop or a reason to stop other work.

## Engineering discipline

- Inspect before deciding; decide before implementing; verify after implementation.
- Prefer one concrete problem at a time, with source-backed evidence and observable tests.
- Do not introduce workarounds that bypass the current architecture.
- Keep GitHub as the sole source of truth for project state and decisions.
