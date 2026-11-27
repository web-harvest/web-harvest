# Configuration Parameters and Secret Resolution

**Status:** Proposed architecture; documentation only.
**Scope:** Runtime variables, plugin-instance configuration, global parameter contexts, and future secret-provider integration.
**Related work:** [Core runtime and XML compatibility boundary (#422)](https://github.com/web-harvest/web-harvest/issues/422), [Plugin SPI and descriptor contract (#346)](https://github.com/web-harvest/web-harvest/issues/346), [Plugin registry generation design](PLUGIN_REGISTRY_GENERATION_DESIGN.md).

## 1. Purpose

WebHarvest has configuration values with different owners, visibility rules and lifetimes. They must not be collapsed into a single global map or confused with the plugin registry.

The architecture distinguishes four concerns:

1. runtime variables, which are execution data;
2. plugin-instance configuration, which initializes one plugin instance;
3. global parameter contexts, which provide reusable host/application/environment configuration;
4. secret providers, which resolve sensitive references under host-controlled authorization.

These concerns cooperate through explicit resolution contracts. A plugin must not need to know whether an ordinary value came from an XML literal, a parameter context, or a host configuration source. It must also not gain unrestricted access to the global parameter store or secret backend.

## 2. Scope model

| Scope | Owner | Lifetime | Typical examples |
|---|---|---|---|
| Runtime variables | Execution context | Execution / nested scope | Extracted values, loop variables, intermediate results |
| Plugin-instance configuration | Plugin factory + instance | Initialization and instance lifetime | Endpoint, timeout, mode, pool policy |
| Global parameter context | Host/runtime configuration | Versioned context revision | Shared endpoints, environment-specific limits, feature settings |
| Secret provider | Host/security integration | Provider-defined | API keys, passwords, tokens, certificates, private keys |

### 2.1 Runtime variables

Runtime variables remain part of the execution data model. Their scope and mutation rules follow execution/nested-scope semantics. They are not persistent global parameters and must not be silently promoted into configuration.

Existing expression and variable semantics are compatibility-sensitive and must be characterized before any redesign.

### 2.2 Plugin-instance configuration

A plugin descriptor declares its configuration schema: names, types, requiredness, defaults, validation, sensitivity classification and supported value sources.

The factory resolves and validates configuration before initialization, then supplies a typed configuration view to the plugin. Configuration should be immutable for the initialized instance unless a future explicit reconfiguration lifecycle is designed.

Multiple instances of one plugin type may have different configuration values. Plugin code must not read configuration from a process-global singleton.

### 2.3 Global parameter contexts

A global parameter context is a named, versioned collection of reusable values. It is inspired by the operational role of Apache NiFi Parameter Contexts, not intended as a copy of NiFi's API.

Design requirements:

- explicit context identity and version/revision;
- immutable snapshots once captured by a configuration revision or execution;
- typed values and declared references;
- explicit visibility and binding to a host/runtime/flow;
- deterministic override and precedence rules;
- validation against consumer/plugin schemas;
- clear distinction between ordinary values and secret references;
- export/import behavior that never exports secret plaintext.

No ambient environment variable, system property or process-global map should silently override an explicitly bound parameter context. Such sources may be supported as explicit parameter providers with documented precedence.

## 3. Resolution and consistency

The host selects and binds the applicable parameter context. At configuration load or execution admission, WebHarvest captures a context revision/snapshot. Nested plugin execution inherits the same snapshot unless an explicit child context boundary is declared.

The resolution pipeline should be conceptually:

1. read the plugin descriptor's declared configuration schema;
2. bind instance configuration inputs;
3. resolve ordinary parameter references from the captured parameter-context snapshot;
4. validate types, required values and constraints;
5. preserve secret references as typed references until the authorized secret boundary resolves them;
6. create the plugin instance with a least-privilege configuration/capability view.

The exact snapshot boundary (configuration load versus execution admission) must be selected and documented so that a running execution cannot observe arbitrary configuration edits midway through its lifetime.

## 4. Secret references and providers

Sensitive material is not an ordinary global parameter value. A parameter may point to a secret reference, but the secret bytes/value remain owned by a provider.

Illustrative reference:

```yaml
parameters:
  services:
    weather:
      endpoint: https://example.invalid/api
      apiKey:
        $secret: vault://services/weather/api-key
```

The scheme above is illustrative only; it does not establish a public URI grammar.

A neutral Core/host contract should represent:

- secret reference (provider + logical identifier + optional version);
- access context (runtime, execution, plugin identity and requested capability);
- provider resolution result (preferably a protected/typed value or credential handle);
- provider errors without leaking secret content.

Provider implementations may later integrate environment variables, protected local files, operating-system credential stores, HashiCorp Vault or other systems. Core must not depend on a particular Vault product.

### 4.1 Security requirements

- Never write plaintext secrets into DSL, XML, JSON exports, metadata catalogs, error messages or logs.
- Redact secret-bearing values by default; do not rely on every plugin author remembering to redact.
- Authorize resolution using explicit host/runtime context and least privilege.
- Do not expose all global parameters or all secret providers to every plugin.
- Minimize secret copies and lifetime; avoid immutable `String` for raw secret material when a safer representation is practical.
- Treat certificates/private keys as typed credential material, not merely arbitrary strings.
- Define behavior for missing providers, denied access, expired credentials and malformed references.
- Make rotation/revocation semantics explicit. A running operation must not silently switch credentials halfway through a network operation.
- Keep provider implementations replaceable and testable without a live Vault service.

### 4.2 Rotation consistency

The parameter snapshot normally pins the secret reference and policy, not necessarily plaintext secret bytes. The architecture must explicitly choose whether a resolution obtains:

- the latest provider value at resolution time;
- a provider-version-pinned value;
- or an execution/operation-scoped resolved credential handle.

The choice may differ by credential type and operation boundary, but must be deterministic and observable without exposing the secret. No implicit mid-operation refresh is allowed.

## 5. Registry and plugin descriptor relationship

The plugin registry generation owns immutable descriptor metadata and configuration schemas. It does not own mutable global parameter values, secret material or a Vault client.

The descriptor may declare:

- required configuration keys and types;
- whether a value may be supplied by a parameter reference;
- whether a key is sensitive or must resolve to a credential type;
- required secret/credential capabilities;
- validation and lifecycle constraints.

The runtime/host supplies parameter-context snapshots and secret-resolution capabilities to the generation-bound plugin factory. This keeps registry publication, configuration management and secret storage as separate responsibilities.

## 6. Migration inventory required before implementation

Inspect and document:

- current variable interpolation and runtime variable scopes;
- XML namespace/attribute conventions for parameters and variables;
- `PluginConfiguration` and `DefaultPluginConfiguration`;
- `PluginConfigurationSchema` and metadata generation;
- plugin initialization and failure/cleanup behavior;
- all configuration source types and include semantics;
- logging, diagnostics and serialization paths that may expose sensitive values.

Do not infer that existing `p:` and `v:` XML namespaces already provide the full semantics of a global parameter context or a secret provider. Characterize their actual behavior first.

## 7. Acceptance matrix

| Area | Required evidence |
|---|---|
| Runtime variables | Existing nested-scope and expression behavior is characterized and regression-tested |
| Instance configuration | Typed validation, defaults, required values and initialization failure behavior are deterministic |
| Multiple instances | Same plugin descriptor can create independent instances with different configuration |
| Global parameters | Explicit binding, revision identity, immutable snapshot and override rules are tested |
| Runtime isolation | Two runtimes use different parameter contexts without leakage |
| Execution consistency | A running execution does not observe an unannounced parameter-context mutation |
| Secret reference | DSL/config exports contain only references, never secret plaintext |
| Authorization | A plugin can resolve only the secrets/capabilities explicitly granted by the host |
| Rotation | Defined version/latest semantics are deterministic at the documented operation boundary |
| Redaction | Logs, exceptions, diagnostics and metadata do not reveal secret values |
| Provider independence | Core tests run with fake/local providers; no Vault dependency is mandatory |
| Offline operation | Host can supply a local/offline provider without changing plugin contracts |

## 8. Non-goals for the initial architecture slice

- Implementing a Vault server or proprietary secret store.
- Making a network Vault service mandatory for Core startup.
- Moving runtime variables into a global parameter store.
- Storing plaintext secrets in a registry generation.
- Letting plugin implementations access a process-global configuration singleton.
- Defining a public configuration file syntax before existing XML/configuration semantics are inventoried.

This document defines design requirements only. Production implementation requires a completed source inventory, reviewed contracts and focused migration tests.
