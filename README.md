# WebHarvest

**WebHarvest** is a Java framework for configuring and running web data extraction workflows using XML and an extensible plugin architecture.

The project is being actively modernized. The GitHub repository is the canonical home for source code, issues, pull requests, CI, and documentation:

- **Repository:** https://github.com/web-harvest/web-harvest
- **Issues:** https://github.com/web-harvest/web-harvest/issues
- **Pull requests:** https://github.com/web-harvest/web-harvest/pulls
- **Contribution guide:** [CONTRIBUTING.md](CONTRIBUTING.md)

> **Project status:** Development and stabilization are in progress. Do not interpret historical test totals, badges, or release references from older project material as a statement that the current default branch is fully green or production-ready. Follow current CI results and the open stabilization issues before adopting a build.

## What it provides

- XML-based configuration of extraction and data-processing workflows.
- A modular Java codebase with a plugin-based execution model.
- HTTP, HTML/XML processing, XPath/XQuery, scripting, templating, and control-flow capabilities.
- Optional modules for CLI, IDE, database, FTP, mail, web-browser, and ZIP integrations.
- Extensibility for custom plugins and application integrations.

The exact capabilities and compatibility guarantees depend on the module and version being used. Consult the source and module documentation for behavior not covered here.

## Repository layout

The Maven reactor currently contains these modules:

| Module | Purpose |
|---|---|
| `webharvest-core` | Core configuration, plugin, and execution functionality |
| `webharvest-cli` | Command-line application |
| `webharvest-ide` | Web-based visual development environment |
| `webharvest-database` | Database integration |
| `webharvest-ftp` | FTP integration |
| `webharvest-mail` | Mail integration |
| `webharvest-webbrowser` | Browser integration |
| `webharvest-zip` | ZIP/archive integration |
| `docs` | Project documentation |

## Requirements

- Java 11 or newer for the current development line.
- Maven 3.6 or newer.

Some optional modules and integration tests may require additional services or environment configuration. Check their module documentation and test setup before running them.

## Build and test

Clone the canonical repository:

```bash
git clone https://github.com/web-harvest/web-harvest.git
cd web-harvest
```

Build the Maven reactor:

```bash
mvn --batch-mode --no-transfer-progress clean install
```

Run the Core module with its required upstream modules:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

These commands describe how to request the build and test lifecycle; they are not a claim that every current branch or test suite passes. For a change, record the exact commit, command, exit status, test counts, skipped tests, and relevant CI run in its pull request. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Development status and known work

The project is undergoing a staged modernization and regression-remediation effort. Current work includes Core contracts and compatibility, plugin/catalog behavior, XML fidelity and security, IDE integration, and CI reliability.

Before depending on a particular capability:

1. Check the current default-branch CI status.
2. Review open Issues for known regressions and compatibility constraints.
3. Pin the exact commit or released artifact used by your application.
4. Run the tests relevant to your integration.

Historical notes and investigation results are retained in GitHub Issues and commit history. Older documentation may describe behavior or test results from a different code baseline.

## Contributing

Contributions are welcome, but the project uses a review-first workflow:

1. Search or open a GitHub Issue to agree on expected behavior.
2. Create a focused branch from the intended base.
3. Add tests for the behavior being changed.
4. Run the relevant test lifecycle and report exact evidence.
5. Open a pull request and wait for CI and review.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before starting. GitHub is the sole source of truth; do not use historical external trackers or SVN repositories.

## License and third-party notices

The repository contains the project's license text at [licences/webharvest_licence.txt](licences/webharvest_licence.txt), Apache License 2.0 text at [licences/apache_licence.txt](licences/apache_licence.txt), additional third-party license texts under `licences/`, and attribution information in [NOTICE](NOTICE).

Preserve all applicable copyright, license, and third-party notices when redistributing the project. The project-level license and the terms for bundled third-party components must be reviewed separately. See the files above for the actual terms; this README is not a substitute for them.

## Security

Do not publish credentials, tokens, private keys, personal data, or vulnerability details in public Issues or pull requests. Use GitHub's private vulnerability reporting facility if it is enabled for this repository. If it is not available, contact the repository maintainers through the GitHub organization before disclosing sensitive details publicly.

