# Contributing to WebHarvest

Thank you for your interest in WebHarvest. Contributions from users, developers, integrators and plugin authors are welcome.

## 1. Project source of truth

The GitHub repository is the canonical source for source code, issues, pull requests, reviews, CI and project documentation:

- Repository: https://github.com/web-harvest/web-harvest
- Default branch: `main`
- Issues: https://github.com/web-harvest/web-harvest/issues
- Pull requests: https://github.com/web-harvest/web-harvest/pulls
- Roadmap: https://github.com/web-harvest/web-harvest/issues/1

The public website source is maintained separately at https://github.com/web-harvest/website. The application repository may still contain historical/versioned docs under `docs/web/**`.

Read the [Project Direction and Architecture Roadmap](docs/PROJECT_DIRECTION.md) and [Monorepo Boundary Policy](docs/MONOREPO.md) before proposing architectural or module-boundary changes.

## 2. Development prerequisites

- JDK 11 baseline (use the version required by the module being changed).
- Maven 3.6 or newer.
- Node.js and npm only for relevant IDE frontend packages.

Import the repository as a Maven project in IntelliJ IDEA, Eclipse or another Maven-compatible IDE.

## 3. Build and test

From the repository root:

```bash
# Core focused unit suite and required upstream modules
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test

# Core full lifecycle (Surefire + Failsafe and configured quality gates)
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify

# Full Maven reactor
mvn --batch-mode --no-transfer-progress verify
```

A focused passing test is not equivalent to full module or reactor verification. Preserve the Surefire/Failsafe separation and configured quality checks.

Core network-tagged TestNG suites are intentionally excluded from the default deterministic gate. Run them explicitly from the repository root:

```bash
# Network-tagged Core unit tests
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am -Pnetwork-tests test-compile surefire:test

# Network-tagged Core integration tests, in a separate invocation
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am -Pnetwork-tests test-compile failsafe:integration-test failsafe:verify
```

These commands require network access and are opt-in diagnostics; they do not replace or weaken deterministic CI. Run unit and integration paths separately so one failure does not suppress diagnostics from the other.

Do not obtain a green build by deleting tests, weakening assertions, adding broad exclusions, setting `continue-on-error` or lowering coverage thresholds. If a test is environment-dependent, document the dependency and retain an explicit test path.

## 4. Branch and release workflow

### Branches

- `main` is the current integration/development line and the default branch.
- `release/<major.minor>` is created from a verified `main` commit only when a version enters release stabilization/RC.
- Historical `master` and `2.2.x` branches are preserved as historical lines unless a separate maintenance decision is made.
- Use short-lived `feature/*`, `fix/*` and `docs/*` branches for pull requests. Delete the head branch after merge (prefer repository automatic deletion).
- Do not create a permanent `develop` branch or speculative release lines without an explicit reason.

### Bug fixes

- Fix unreleased-work bugs on a short-lived branch from `main).
- Fix a bug affecting a supported release on the corresponding `release/x.y` branch.
- Merge the hotfix to that release line and forward-port it to `main` through a reviewed PR when applicable.
- Backport a `main` fix only when the defect affects a supported release and the change is compatible.
- Identify the source fix, target release, compatibility considerations and CI evidence in every backport PR.

### Pull request expectations

1. Search existing issues and PRs before starting. For substantial or ambiguous work, agree on the behavior/contract in an issue first.
2. Create a focused branch from the intended base. Verify the base and current HEAD before changing files.
3. Keep the PR bounded; avoid mixing unrelated refactors, generated files or formatting churn.
4. Add/update tests that demonstrate intended behavior. Prefer public API and real XML/runtime integration tests where appropriate.
5. Record exact commands and results, including failures and known limitations.
6. Explain problem, design, compatibility implications, test evidence and remaining risks in the PR.
7. Identify parent PRs for stacked changes; do not merge a child before its required parent.
8. Wait for required CI and review. Do not merge with failing required checks or unresolved blocking review feedback.

Use English for issue titles/bodies, PRs, commit messages, source comments, Javadoc and project documentation. Reference related issues and use closing keywords only when the PR fully resolves them.

## 5. Plugin and architecture contributions

Core is intended to remain the smallest self-sufficient workflow runtime and stable plugin platform. A plugin is not Core merely because its class currently resides in `webharvest-core`.

Before moving plugins or changing module boundaries:
- inspect runtime call sites, dependencies, metadata and tests;
- classify the plugin as Core execution, Core control flow, Core data/extraction, internal/system or optional integration;
- record XML element names, aliases and compatibility behavior;
- identify configuration/schema, execution and variable semantics, discovery and lifecycle;
- propose the classification and migration in an issue before implementation.

FTP, database, mail, browser automation and ZIP/archive capabilities are candidates for optional extensions; final membership is evidence-based. Preserve documented legacy XML compatibility, including `<set-var>`, unless an explicit migration decision is approved.

The [Plugin Author Guide](docs/PLUGIN_AUTHOR_GUIDE.md) describes the intended independent plugin developer experience. The public Plugin SPI is not considered stable until the project explicitly publishes its version and compatibility policy. Do not build third-party compatibility promises on internal classes or IDE implementation details.

## 6. Evidence required in a PR

A PR should state:

- exact commit SHA tested;
- exact commands executed and exit status;
- unit and integration test counts, failures, errors and skipped tests;
- relevant GitHub Actions workflow/run links and artifacts;
- whether checks were focused, module-wide or full-reactor;
- known limitations and what remains untested.

A green focused test is not evidence that the complete module or repository is green. Distinguish targeted validation from full regression validation.

## 7. Code and documentation standards

- Use English for code, identifiers, comments, Javadoc, commit messages, issues, PRs and project documentation.
- Follow conventions in the affected module.
- Document public API behavior and compatibility constraints.
- Avoid dependencies unless justified and reviewed.
- Preserve backward compatibility unless a breaking change is explicitly agreed and documented.
- Update user/developer documentation when behavior, configuration or supported workflows change.
- Keep current facts separate from proposals and roadmap targets. Do not claim a planned feature or unpublished artifact is already available.

## 8. Security and licensing

Never commit credentials, access tokens, private keys, production data or other secrets. Use GitHub Actions secrets or an approved local secret-management mechanism.

Report suspected vulnerabilities privately through the repository owner's GitHub security contact or GitHub's private vulnerability reporting facility when enabled. Do not publish exploit details in a public issue.

Preserve existing copyright, license and third-party attribution notices. See [LICENSE](LICENSE), [NOTICE](NOTICE) and the license files before redistributing.
