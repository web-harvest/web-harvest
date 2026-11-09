# WebHarvest

WebHarvest is an open-source, Java-based framework for extracting data from web pages using XML scraper configurations and an extensible plugin system.

**Canonical repository:** https://github.com/web-harvest/web-harvest

The repository's Maven project version is currently **2.3.1**. This is project metadata, not a claim that a 2.3.1 release is published or production-ready. The project is under active development; consult the current issues, pull requests, and CI results before relying on a particular capability or build.

## Project modules

The Maven reactor currently contains these modules:

| Module | Purpose |
|---|---|
| `webharvest-core` | Core configuration, plugin, and execution functionality |
| `webharvest-cli` | Command-line interface |
| `webharvest-ide` | IDE application and related integration |
| `webharvest-database` | Database integration |
| `webharvest-ftp` | FTP integration |
| `webharvest-mail` | Mail integration |
| `webharvest-webbrowser` | Browser-related integration |
| `webharvest-zip` | ZIP archive integration |
| `docs` | Documentation resources |

The root `pom.xml` is the Maven reactor entry point. The configured Java compiler source and target levels are 11.

## Requirements

- JDK 11
- Maven (use a current Maven 3.x installation)

Check the active CI workflow and module POMs for any additional environment-specific requirements.

## Build and test

Run commands from the repository root.

Build and run the complete Maven verification lifecycle:

```bash
mvn --batch-mode --no-transfer-progress verify
```

Run the Core module and its required upstream reactor modules:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am verify
```

Run the Core unit-test phase only:

```bash
mvn --batch-mode --no-transfer-progress -pl webharvest-core -am test
```

A successful focused test run is not equivalent to a successful full reactor verification. When reporting validation, state the exact command, commit SHA, test counts, failures, errors, and skipped tests. Check the GitHub Actions result for the commit under review; do not infer current project health from historical test counts in old documentation.

## Getting started

1. Clone the canonical repository:

   ```bash
   git clone https://github.com/web-harvest/web-harvest.git
   cd web-harvest
   ```

2. Inspect the current issues and pull requests to understand active work and known limitations.

3. Build and test using the commands above before making changes.

4. Explore the module-specific documentation and examples in the repository. Treat examples as illustrative and verify them against the current implementation.

## Contributing

Contributions are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or pull request.

All project development activity belongs in this GitHub repository:

- [Issues and bug reports](https://github.com/web-harvest/web-harvest/issues)
- [Pull requests](https://github.com/web-harvest/web-harvest/pulls)
- [Repository activity and commits](https://github.com/web-harvest/web-harvest/commits/2.2.x)

Use a focused branch, add or update tests for behavior changes, run the relevant verification, and include reproducible evidence in the pull request. Do not describe targeted tests as full-suite validation.

## Project status and compatibility

WebHarvest has a long project history and is undergoing ongoing maintenance and modernization. Some modules, integrations, examples, and historical compatibility paths may have different levels of validation.

Before adopting the project or upgrading an existing integration:

- inspect the current issue tracker for known defects and limitations;
- verify the required Java and dependency versions;
- run representative configurations against the exact version and modules you intend to use;
- review compatibility notes and release information rather than assuming all historical behavior is unchanged.

No claim of complete test-suite success, release readiness, or support for every historical configuration is made by this README.

## License and attribution

The repository identifies the project license as a BSD-style license. The full project license text is in [licences/webharvest_licence.txt](licences/webharvest_licence.txt). See [NOTICE](NOTICE) for attribution and historical contributor information.

Preserve the license and third-party notices when redistributing the project or derivative works.
