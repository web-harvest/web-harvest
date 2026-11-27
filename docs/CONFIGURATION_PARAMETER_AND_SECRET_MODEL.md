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


## 9. Current implementation baseline — source findings (2026-11-27)

This is an initial source characterization, not a complete call-site inventory or a claim that the target model exists.

### 9.1 Plugin configuration is currently string-based and mutable

- `PluginConfiguration` exposes property values as `String` and also exposes mutable child configuration through `setChildren()`.
- `DefaultPluginConfiguration` stores properties in a mutable `HashMap<String, String>`; it supports `setProperty`, `removeProperty` and `clear`.
- Its `getAllProperties()` and `getProperties()` return copies, and its children getter returns an unmodifiable list, but the configuration object itself remains mutable.
- `DefaultPluginConfiguration.toString()` includes the complete properties map. If a sensitive value is ever stored there, routine diagnostic/string conversion can expose it.
- `PluginConfigurationSchema.AttributeDefinition` already describes basic types (STRING, INTEGER, LONG, DOUBLE, BOOLEAN, ENUM, URL, FILE_PATH, REGEX), requiredness, defaults, allowed values and numeric/pattern constraints. However, the current configuration value API remains string-oriented; a schema type does not itself provide a typed, secret-safe runtime value. The current type enum has no first-class parameter-reference, secret-reference or credential-handle type. The convenience getters for integer/long/double values return the supplied default when parsing fails, and boolean parsing treats unrecognized strings as `false`; schema validation must therefore reject malformed values before initialization rather than relying on these getters.

The future design should distinguish immutable validated configuration from mutable XML/parse builders. A secret reference must not be represented as a plaintext string and then passed through the current `toString()` behavior.

### 9.2 Runtime variables are a separate execution mechanism

- `DynamicScopeContext` exposes runtime variable reads/writes, replacement, existence checks and nested-context execution.
- `ScraperContext` maintains a central name-to-stack table and a stack of scope frames. Nested execution pushes a frame, records newly created/overridden variables and restores them in a `finally` block.
- `DefPlugin` and `SetVarPlugin` write execution variables; `set-var` remains a historical XML mapping. These values are dynamic execution data, not a global parameter context.
- The `<config>` plugin documentation calls top-level variables global because they are visible to child processors. That visibility is still within the execution context; it is not a host/application-wide reusable Parameter Context.

The new design must preserve these runtime semantics independently of global parameter binding.

### 9.3 Secret logging risk must be resolved before secret support

The current `BaseTemplater` implementation logs both the input template source and the evaluated result at DEBUG level. Since template evaluation can resolve runtime variables, a credential interpolated into a URL, header or body can appear in the evaluated result log. The current `DefaultPluginConfiguration.toString()` also prints all property values.

These are concrete source-level exposure paths. They do not prove that every deployment enables DEBUG logging, but they make it unsafe to assume that secret values can be passed through the current string/template/configuration path without additional controls.

Before enabling secret references in plugin configuration or runtime expressions:

- redact or remove sensitive template source/result logging;
- provide sensitivity metadata that survives parameter binding and expression evaluation;
- ensure `toString()`, exception messages, debug dumps and diagnostics are redacted by default;
- avoid materializing secret values into general-purpose runtime variables unless an explicit, reviewed capability requires it;
- add tests that place sentinel secret values through configuration, interpolation, exceptions and logging and assert that the sentinel never appears.

### 9.4 Existing XML namespace conventions are not a Parameter Context implementation

Historical `p:*` and `v:*` namespace-family attributes appear in `ElementInfo` metadata and must be characterized for XML compatibility. They must not be assumed to provide the required global parameter-context ownership, revisioning, binding, access control or secret-provider semantics.

The source inventory must trace their actual parser/runtime consumers before any mapping to the new parameter model is proposed.


## 10. XML parameter/variable namespace boundary — current parser does not preserve it (2026-11-27)

Core declares distinct XML namespace URIs for parameter attributes (`WHConstants.XMLNS_PARAM`) and variable attributes (`WHConstants.XMLNS_VAR`). The legacy `ElementInfo` vocabulary recognizes `p:*` and `v:*` as separate allowed namespace families.

However, `StreamingXmlParser.extractAttributes()` currently stores only SAX `getLocalName()` and value in a `Map<String,String>`; it discards the attribute namespace URI and qualified name. Consequently, the current `PluginConfiguration` representation cannot distinguish a parameter attribute from a variable attribute when they share a local name, and a same-local-name collision can overwrite one value.

This is a concrete compatibility and configuration-model boundary. The future model must preserve an expanded attribute identity and represent parameter/variable references as typed values before flattening or interpolation. Do not assume the existing parser already provides a working global parameter context merely because the old namespace constants and `ElementInfo` markers exist.


## 11. Runtime-variable scope and mutation semantics — current source findings (2026-11-27)

### 11.1 Runtime variables belong to one execution context

- `ScraperContext` stores variables in a name-to-stack table and tracks nested `ScopeFrame` objects.
- `setLocalVar()` creates a variable in the current frame. If the name already exists in an outer frame, the new value is pushed as a shadow and the previous stack depth is recorded. Re-defining a name created in the same frame replaces that frame's current value.
- `executeWithinNewContext()` pushes a frame and restores/removes its variables in `finally`, including on failure or interruption.
- `RootConfigurationExecutor` applies run settings (charset and scripting language) and executes root children against the supplied context. It does not create a persistent/global parameter store or an additional nested scope. The old `ConfigPlugin` documentation's use of the word "global" means visible across children of that scraper execution, not shared across runs, applications or runtimes.
- `DynamicScopeContext` is the current variable access contract; it also exposes framework-specific EventBus/session/configuration concerns that are separate from the variable model and are tracked in the Core decoupling work.

### 11.2 Definition and assignment have different shadowing behavior

- `DefPlugin` uses `context.setLocalVar()`: defining a name already visible in an outer frame shadows it for the current nested scope and restores the previous value when that scope exits.
- Legacy `SetVarPlugin` uses `replaceExistingVar()` when a visible name exists. This replaces the top value in place, so an update to an outer variable survives nested-scope cleanup. If the name does not exist, it creates a local variable.
- This difference is intentional in current code and is covered by variable-scope tests/compatibility behavior. A future configuration model must not conflate assignment/shadowing with parameter-context override semantics.

### 11.3 There is no global parameter context today

The existing runtime has execution variables, plugin configuration, root run settings and XML namespace constants. It does not currently provide a host/application-level, named, versioned parameter context with immutable snapshots, runtime binding, revision pinning or secret-aware access control. The legacy `p:` namespace is not proof of such a feature; the active parser currently discards attribute namespace identity (see section 10).

## 12. Secret exposure inventory — broader than configuration and templating

The source audit found value-bearing logs in several ordinary execution paths:

| Path | Current behavior | Required treatment before secret support |
|---|---|---|
| `DefaultPluginConfiguration.toString()` | Includes the complete properties map | Redacted representation; no raw values by default |
| `BaseTemplater` | DEBUG logs template source and evaluated result | Never log raw source/result when sensitive data may be referenced |
| `DefPlugin` | INFO logs configuration, body results, XML text and assigned/returned variable values | Log names/types/lengths or opaque IDs only |
| `SetVarPlugin` | DEBUG logs value attributes, body text, evaluated values and read-back values | Redact by sensitivity label; no value interpolation in diagnostics |
| `GetVarPlugin` | DEBUG logs configuration and retrieved variable values | Do not stringify sensitive variables |
| `TemplatePlugin` | INFO logs body/template content and evaluated results | Treat template input/output as potentially sensitive |
| Execution events | `PluginExecutionCompletedEvent` carries a `Variable` result; `PluginExecutionFailedEvent` carries a `Throwable` | Event consumers/recorders need redaction and sensitivity metadata; exception messages/causes must not leak secrets |

These are direct source-level exposure paths, not claims that every deployment enables DEBUG or has an event subscriber that persists values. They demonstrate that simply adding a Vault client or a `SecretReference` field would not make secret handling safe.

### 12.1 Required secret-flow boundary

Before any secret can enter a plugin execution path:

- sensitivity/provenance metadata must survive parameter binding, configuration validation and expression evaluation;
- raw secret material must not be implicitly converted to `String`, `Variable.toString()`, template output, exception text or event payloads;
- secret resolution must be explicit, capability-scoped and as late as practical (ideally at the operation boundary);
- plugin contracts must declare which inputs accept secret references and whether the operation requires text, bytes, a certificate/key object or another typed credential;
- logs, events, tracing, diagnostics and error reporting must redact by default and have sentinel-value tests proving that secrets cannot leak;
- a plugin that does not declare/receive the required secret capability must not be able to resolve the reference itself or enumerate unrelated secrets.

A global parameter context may hold ordinary values and typed references to secrets. It must not become a back door for making secret material globally visible as ordinary runtime variables.
