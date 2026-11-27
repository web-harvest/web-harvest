# WebHarvest Plugin Author Guide

**Status:** Developer-facing design guide for the target external plugin contract. The independent public SPI is not yet declared stable; check the release notes and compatibility matrix before depending on any API.
**Audience:** Plugin developers, integrators and maintainers.
**Related:** [Project direction](PROJECT_DIRECTION.md), [module policy](MONOREPO.md), [contribution guide](../CONTRIBUTING.md).

## 1. The goal

WebHarvest is moving toward an ecosystem in which plugins can be implemented, built, tested and distributed independently of the WebHarvest monorepo and IDE.

The intended developer experience is:

- a published, versioned Core/plugin API dependency;
- a small and documented implementation contract;
- explicit plugin identity and metadata;
- configuration schema and validation;
- deterministic discovery and registration;
- lifecycle and resource cleanup;
- compatibility checks and actionable diagnostics;
- an independent Maven build and contract-test harness.

This guide describes the required shape of that contract. It does not promise that all of it is already available in a published artifact.

## 2. What a plugin is responsible for

A Plugin is an executable provider. It may implement one or more independently identified Capabilities; the Plugin's identity is not the Capability's semantic identity. A workflow declares the Capability contract it requires, and the runtime resolves a compatible provider. The provider contract must make the following explicit:

| Concern | Required declaration/behavior |
|---|---|
| Identity | Stable, globally unique plugin ID; display name is not identity |
| Compatibility | Supported Plugin SPI/Core API range |
| Metadata | Version, description, vendor/maintainer and capability identifiers |
| Configuration | Accepted properties, types, defaults, required values and validation |
| Execution | Inputs, outputs, side effects, error behavior and interruption behavior |
| Capability implementation | Stable Capability IDs/versions implemented by this Plugin, with typed inputs/outputs and valid execution context |
| Dependencies | Required libraries, external services and optional capabilities |
| Lifecycle | Initialization, readiness, cleanup and resource ownership |
| Diagnostics | Clear errors for invalid configuration, incompatibility and initialization/execution failures |
| Tests | Contract tests, deterministic behavior tests and separately identified network tests |

A plugin must not assume that IDE classes, editor state or internal implementation classes are present in the runtime.

## 3. SPI and implementation model

The current source tree contains the `org.webharvest.plugin.Plugin` interface and `AbstractPlugin` base class. These are useful evidence of the existing design, but are not by themselves a declaration of a stable third-party SPI.

The target SPI must define:
- the supported plugin lifecycle and thread/concurrency expectations;
- configuration and execution context guarantees;
- stable identity and descriptor representation;
- compatibility negotiation;
- error and diagnostic contracts;
- cleanup behavior, including partial initialization failure;
- supported extension points and explicitly unsupported internals.

Prefer depending on the smallest published API artifact rather than the full runtime or IDE. The exact Maven coordinates and package compatibility policy will be published only after the SPI stabilization gate.

## 4. Descriptor and discovery contract

The target descriptor is the authoritative declaration used by tooling and runtime. At minimum it must identify:

- plugin ID and plugin version;
- SPI/API compatibility range;
- implementation/provider entry point;
- capability IDs and semantic roles;
- configuration schema/version;
- typed input and output contracts;
- required and optional dependencies;
- execution-context constraints;
- lifecycle and security/trust metadata where applicable.

Discovery should be deterministic and inspectable. Java service-provider metadata is one possible mechanism; controlled classpath scanning may supplement it. The final mechanism must be selected and documented by the maintainers.

Discovery is not permission to instantiate every class found on the classpath. Before registration, the runtime must reject or report:
- missing or malformed descriptors;
- duplicate plugin IDs or ambiguous providers;
- unsupported API compatibility;
- unsatisfied dependencies;
- invalid capability or schema declarations;
- disallowed or untrusted providers under the application's policy.

Registration and initialization outcomes must be available through diagnostics. Failure must not silently leave a partially registered plugin.

## 5. Configuration and validation

A plugin's configuration contract should be machine-readable and consistent with the runtime's validation model.

Document:
- element name and namespace;
- accepted attributes/children;
- required/optional values and defaults;
- data types and conversion rules;
- cross-field constraints;
- valid semantic contexts;
- validation timing and error reporting;
- compatibility aliases and deprecation policy.

Do not infer plugin identity from an XML local name alone. Preserve existing XML compatibility only where it is explicitly supported and covered by tests.

## 6. Execution and lifecycle

The plugin contract must state:
- how execution context and variables are accessed;
- how inputs are bound and outputs are returned;
- whether execution is stateful or safe for concurrent use;
- how cancellation/interruption is handled;
- how external resources are acquired and released;
- what happens when initialization partially succeeds and then fails;
- how exceptions are surfaced to callers and diagnostics.

Plugins must not leak threads, sockets, database connections, browser processes or other resources. Cleanup must be idempotent or otherwise precisely documented.

## 7. Build and test expectations

The target independent build will be a normal Maven project depending on a published compatible Core API. Exact coordinates and sample code will be added when the SPI is released.

Each plugin should provide:
1. unit tests for its own behavior;
2. descriptor/schema validation tests;
3. SPI contract tests;
4. deterministic runtime integration tests;
5. network/service tests separated from offline deterministic tests;
6. compatibility tests against each supported Core line.

A plugin must be buildable without adding its source directory to the WebHarvest Maven reactor. It must not rely on unpublished classes or IDE implementation details.

## 8. Installation and automatic registration

The target application flow is:

1. The operator installs the plugin artifact using the documented packaging mechanism.
2. The runtime discovers the provider/descriptor.
3. The runtime validates identity, API compatibility, dependencies, schema and policy.
4. The runtime registers the Plugin's provider bindings for the declared Capabilities and exposes their descriptors to semantic tooling.
5. The runtime initializes the plugin and reports readiness or a structured failure.
6. Workflow validation and execution resolve only compatible registered capabilities.
7. Shutdown invokes plugin cleanup and releases owned resources.

The exact packaging/discovery mechanism is still subject to implementation and testing. Do not assume that dropping an arbitrary JAR into a directory is currently supported.

## 9. Semantic capability resolution — target

The long-term runtime will resolve a workflow's semantic requirements against compatible Capability providers contributed by installed Plugins.

A workflow should declare what capability it needs and the typed contract it expects, rather than binding itself to a display label or implementation class. The resolver must check:
- capability identity and version constraints;
- input/output type compatibility;
- configuration schema and constraints;
- execution-context requirements;
- dependency and policy constraints;
- ambiguity and deterministic selection.

The runtime should explain why a candidate was selected or rejected. Silent fallback to a similarly named plugin is not acceptable.

This is a target architecture. Existing plugin scanning and Stage 1 semantic inspection do not yet establish this end-to-end behavior.

## 10. Compatibility and security

- Treat plugin IDs as stable API.
- Version descriptors and schemas independently where appropriate.
- Publish a Core ↔ plugin compatibility matrix.
- Define behavior for missing, incompatible, duplicate and deprecated plugins.
- Do not embed credentials in descriptors or plugin artifacts.
- Make external network access and other side effects explicit.
- Apply an explicit trust/allow-list policy where the host application requires it.
- Do not load untrusted plugin code merely because it is discoverable.

## 11. Current limitations and how to contribute

Until the SPI is declared stable:
- use the current repository source and tests as implementation evidence;
- do not publish third-party compatibility promises based on internal classes;
- propose SPI changes through GitHub Issues before implementing a dependent plugin;
- include a minimal independent plugin proof as part of the stabilization work.

Start with [CONTRIBUTING.md](../CONTRIBUTING.md) and the [project roadmap](https://github.com/web-harvest/web-harvest/issues/1). The first external sample plugin will be accepted as proof only when it builds against published Core artifacts and is discovered and registered in a target application without compiling the WebHarvest monorepo.
