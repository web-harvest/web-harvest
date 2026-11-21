# WebHarvest documentation

This directory contains maintained project documentation, historical/versioned HTML and XML references, examples, scripts and site assets. It is not the generated Java API Javadoc output: the Maven reactor generates Javadocs under each Java module's `target/apidocs` and packages them as `*-javadoc.jar`. Those artifacts are built from source and are not maintained in this directory.

For current project direction and contribution guidance, start here:

- [Project Direction and Architecture Roadmap](PROJECT_DIRECTION.md) — product goals, architecture principles, phases and exit gates.
- [Plugin Author Guide](PLUGIN_AUTHOR_GUIDE.md) — target external plugin contract, discovery and compatibility expectations; explicitly marks what is not yet stable.
- [Monorepo and Module Boundary Policy](MONOREPO.md) — current Maven modules, dependency direction and future repository extraction gates.
- [Plugin Inventory](PLUGIN_INVENTORY.md) — preliminary source-backed registration/discovery baseline; not yet an approved Core membership list.
- [Contributor Guide](../CONTRIBUTING.md) — branches, bug fixes, CI, review and evidence expectations.
- [Workflow Security Model](SECURITY.md) — trust boundaries for executable XML, scripts, nested includes and the embedded IDE.
- [Groovy Runtime Migration Notes](GROOVY_RUNTIME_MIGRATION.md) — production Groovy baseline and compatibility guidance.
- [Project roadmap on GitHub](https://github.com/web-harvest/web-harvest/issues/1).

## Documentation status

Some content under `web/` and versioned subdirectories was written for earlier Web-Harvest releases. It remains useful as historical reference and for compatibility research, but must not be treated as an authoritative statement of current Core plugin membership, current API stability, current release availability or current runtime behavior without verification against source, tests and CI.

In particular:
- historical plugin counts and category labels are not the approved current Core inventory;
- a plugin's location in a Java package or Maven module does not alone determine whether it belongs to Core;
- the existence of scanning/registration code does not prove a stable third-party SPI or arbitrary JAR auto-installation;
- Stage 1 semantic XML inspection is not the completed format-independent semantic runtime;
- Maven POM version metadata is not proof that an artifact or GitHub Release has been published.

## Existing resources

- `web/` — historical/versioned HTML documentation and examples.
- `web/examples/` — illustrative XML configurations; verify behavior against current runtime.
- `semantic-metadata/` — current semantic metadata/catalog notes.
- `scripts/` and `src/main/` — documentation generation tools and Maven-coupled resources.
- `FUNCTION_EXECUTION.md`, `VALIDATION_RULES.md` and other topic guides — consult with the version and source caveats above.

## Contributing to documentation

Use English for project-facing documentation. State whether each claim is a verified current fact, an accepted decision or a future proposal. Test commands and XML examples against the relevant module when possible, and record the exact commit and result. Do not silently rewrite historical versioned documentation as though it described the current release.
