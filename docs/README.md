# WebHarvest documentation

This directory contains maintained project documentation, historical/versioned HTML and XML references, examples, scripts and site assets. It is not the generated Java API Javadoc output: the Maven reactor packages the rendered Javadoc HTML in each Java module's attached `*-javadoc.jar`. Those artifacts are built from source and are not maintained in this directory.

For current project direction and contribution guidance, start here:

- [Project Direction and Architecture Roadmap](PROJECT_DIRECTION.md) — product goals, architecture principles, phases and exit gates.
- [Cross-Application Semantic and Functional Architecture](CROSS_APPLICATION_SEMANTIC_ARCHITECTURE.md) — shared semantic contracts and the distinct meanings of Pack, Kit, Capability, Adapter, Plugin and Module.
- [Cross-Application Semantic Contract Inventory](CROSS_APPLICATION_SEMANTIC_CONTRACT_INVENTORY.md) — evidence-led candidate domains, governance, versioning and validation gates; no schemas are approved yet.
- [WebHarvest Data, Stream and Semantic Contract Architecture](WEBHARVEST_DATA_AND_STREAM_CONTRACT_ARCHITECTURE.md) — WebHarvest-specific boundaries for records, observations, events, artifacts, streams, AI providers, agents, MCP and Core.
- [WebHarvest Data Contract Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md) — source-backed assessment of current Variable, PluginConfiguration, runtime events, XML parsing and the absence of in-tree media/MCP paths on the inspected main revision.\n- [Variable, Event and Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md) — bounded source trace of both event-bus families and HTTP, file, database, binary and collection payload paths.\n- [Resource Ownership and Runtime Integration Audit](WEBHARVEST_RESOURCE_OWNERSHIP_INTEGRATION_AUDIT.md) — evaluates the existing ResourceLedger, actual worker-exit tracking, missing runtime integration and cancellation-time JDBC cleanup ordering.
- [Plugin Author Guide](PLUGIN_AUTHOR_GUIDE.md) — target external plugin contract, discovery and compatibility expectations; explicitly marks what is not yet stable.
- [Monorepo and Module Boundary Policy](MONOREPO.md) — current Maven modules, dependency direction and future repository extraction gates.
- [Plugin Inventory](PLUGIN_INVENTORY.md) — preliminary source-backed registration/discovery baseline; not yet an approved Core membership list.
- [Contributor Guide](../CONTRIBUTING.md) — branches, bug fixes, CI, review and evidence expectations.
- [Workflow Security Model](SECURITY.md) — trust boundaries for executable XML, scripts, nested includes and the embedded IDE.
- [Groovy Runtime Migration Notes](GROOVY_RUNTIME_MIGRATION.md) — production Groovy baseline and compatibility guidance.
- [Project roadmap on GitHub](https://github.com/web-harvest/web-harvest/issues/1).

## Architecture proposals under review

The following documents describe proposed target architecture and are not claims of implemented behavior:

- [Plugin Registry Generation Design](PLUGIN_REGISTRY_GENERATION_DESIGN.md) — one runtime-owned immutable generation, plugin descriptor and XML grammar projections, historical XML metadata migration, and retirement of legacy registries.
- [Configuration Parameters and Secret Resolution](CONFIGURATION_PARAMETER_AND_SECRET_MODEL.md) — runtime variables, plugin-instance configuration, versioned global parameter contexts and a future host-supplied secret-provider boundary.

Current review: [PR #423](https://github.com/web-harvest/web-harvest/pull/423). These proposals remain subject to the source inventory, compatibility characterization and architecture gates tracked in GitHub Issues #346, #347, #422 and #424.

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
