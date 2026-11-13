# Contributing to WebHarvest

Thank you for your interest in contributing to WebHarvest.

## Source of truth

The GitHub repository is the sole source of truth for the project:

- Repository: https://github.com/web-harvest/web-harvest
- Issues and bug reports: https://github.com/web-harvest/web-harvest/issues
- Pull requests: https://github.com/web-harvest/web-harvest/pulls

Use this repository for source code, branches, issues, reviews, CI results, and project documentation. Do not submit patches through external or historical project trackers.

## Development prerequisites

- Java 11 or newer (use the version required by the module being changed).
- Maven 3.6 or newer.
- Node.js and npm only when working on the IDE frontend or other JavaScript packages.

Import the repository as a Maven project in IntelliJ IDEA, Eclipse, or another Maven-compatible IDE.

## Build and test

From the repository root:

```bash
# Build the project
mvn --batch-mode --no-transfer-progress clean install

# Run the Core test lifecycle and its required upstream modules
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

Run focused tests while developing, but run the relevant full module test lifecycle before requesting review. For Core changes, preserve the Surefire/Failsafe separation and the configured quality checks.

Do not obtain a green build by deleting tests, weakening assertions, adding broad exclusions, setting `continue-on-error`, or lowering coverage thresholds. If a test is environment-dependent, document the dependency and keep it available through an explicit test path.

For frontend changes, use the scripts declared by the package being changed and follow the corresponding repository CI workflow. Record the exact commands and results in the pull request.

## Contribution workflow

1. Search existing GitHub Issues before starting work. If the change is substantial or its expected behavior is unclear, open an issue to agree on the contract first.
2. Create a focused branch from the intended base branch. Check the base and current HEAD before making changes.
3. Keep changes bounded and avoid mixing unrelated refactors, generated files, or formatting churn.
4. Add or update tests that demonstrate the intended behavior. Prefer public API and real XML/runtime integration tests where appropriate.
5. Run the relevant checks and record exact results, including failures and known limitations.
6. Open a pull request against the intended base branch. Explain the problem, design, compatibility implications, tests run, and any remaining risks.
7. Keep stacked pull requests explicit: identify parent PRs and do not merge a child independently of its required parent.
8. Wait for CI and review. Do not merge or release while required checks are failing or the change has unresolved blocking review feedback.

Use clear commit and pull-request titles in English. Reference related Issues and use GitHub's closing keywords only when the PR fully resolves the referenced issue.

## Code standards

- Use English for source code, identifiers, comments, Javadoc, commit messages, and Markdown documentation.
- Follow the existing formatting and conventions in the affected module.
- Document public API behavior and compatibility constraints.
- Avoid new dependencies unless they are justified and reviewed.
- Preserve backward compatibility unless a breaking change has been explicitly agreed and documented.
- Update relevant user and developer documentation when behavior, configuration, or supported workflows change.

## Tests and evidence

A pull request should state:

- The exact commit or branch head tested.
- Commands executed and their exit status.
- Unit and integration test counts, including failures, errors, and skipped tests.
- Relevant CI workflow/run links and diagnostic artifacts.
- What the tests do not cover and any remaining unknowns.

A focused passing test is not evidence that the complete module or repository is green. Distinguish targeted validation from full regression validation.

## Security

Never commit credentials, access tokens, private keys, production data, or other secrets. Use GitHub Actions secrets or an approved local secret-management mechanism. Report suspected vulnerabilities privately through the repository owner's GitHub security contact or GitHub's private vulnerability reporting facility when enabled; do not publish exploit details in a public issue.

## License and attribution

Contributions are subject to the project's applicable license and contribution terms. Preserve existing copyright, license, and third-party attribution notices. Do not remove or rewrite historical attribution as part of unrelated changes.
