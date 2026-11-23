# Autonomous Dynamic Plugin Platform

**Status:** Target architecture and release acceptance contract; not a claim about current implementation.
**Priority:** P0 — release-blocking.
**Tracking:** [#94](https://github.com/web-harvest/web-harvest/issues/94), [#346](https://github.com/web-harvest/web-harvest/issues/346), [#347](https://github.com/web-harvest/web-harvest/issues/347), [#348](https://github.com/web-harvest/web-harvest/issues/348), [#349](https://github.com/web-harvest/web-harvest/issues/349), [#350](https://github.com/web-harvest/web-harvest/issues/350), [release gate #88](https://github.com/web-harvest/web-harvest/issues/88).

## 1. Product invariant

WebHarvest is a plugin-capable runtime whose available capabilities are **discovered and reconciled autonomously at runtime**. It must not depend on a manually maintained package-root list, per-plugin registration code, a closed list of known extension classes, or a one-time startup scan.

Built-in and third-party plugins use the same provider, descriptor, validation, registration, semantic projection and execution pipeline. The platform continuously understands what capabilities are currently available from the managed plugin sources and exposes that understanding consistently to runtime consumers, the Semantic Model, metadata/catalog, schema projection, inspection, validation, IDE and agent-facing interfaces.

**Discovery is automatic; trust and execution are policy-controlled.** Finding an artifact does not authorize its code to run.

A one-shot ServiceLoader scan is not this architecture. ServiceLoader or a similar provider mechanism may be used to enumerate providers within an artifact/classloader, but the platform must also own dynamic artifact-source management, change detection, reconciliation, classloader lifecycle and safe publication of changed capabilities.

## 2. JVM boundary and dynamic extension source

A running JVM's ordinary application classpath is not generally mutable. Copying a JAR into a directory does not make its classes visible to an already-running application classloader.

Therefore WebHarvest must provide a supported **managed extension source**, such as a plugin directory or repository, and own the corresponding plugin classloader/module lifecycle. The exact storage and deployment mechanism is an implementation decision; the externally observable behavior is not:

- detect supported artifact additions, replacements and removals while the host is running;
- discover providers without editing Core source or package lists;
- validate candidate changes before they become active;
- publish successful changes without restarting the host in supported deployment modes;
- explicitly document any constrained deployment mode that cannot support live changes. Such a mode must not be represented as fully dynamic.

Built-in providers are supplied through the same generic pipeline. They may be part of the host's trusted default profile, but they must not require a second, manually maintained runtime registration mechanism.

### 2.1 Managed artifact-source contract

The concrete storage product may be a directory, local repository or remote
artifact source, but every supported source must provide equivalent lifecycle
semantics:

- Artifacts are staged under temporary names and become candidates only after a
  complete write, archive-integrity check and atomic promotion. The reconciler
  never loads a JAR that is still being copied or modified in place.
- The host computes a cryptographic content digest and copies/addresses the
  candidate immutably (content-addressed storage or an equivalent immutable
  snapshot). A mutable source path is not the identity of a loaded artifact.
- Trust is evaluated separately from integrity. A digest identifies bytes; it
  does not establish who produced them or whether the host trusts them. Apply
  the host's configured signer/allowlist/provenance policy before activation.
- File watcher notifications are hints only. Coalesce bursts, debounce incomplete
  writes, serialize or compare-and-swap reconciliation, and periodically
  rescan the complete source so missed/overflowed events cannot leave stale
  state indefinitely.
- Symlinks, path traversal, duplicate artifact identity, unsupported archive
  formats, oversized archives and malformed metadata are rejected before
  provider classes are loaded.
- Removal from the source changes desired state; it does not immediately close a
  ClassLoader still referenced by a published generation or active execution.
- Diagnostics retain source identity, content digest, detection time and the
  reconciliation result. Secrets and credential-bearing source URLs are redacted.

The host must define whether a source is local-only or remote and how remote
repository authentication, transport security, caching and offline operation
work. These are host deployment policies, not permissions inferred from a
plugin descriptor.

## 3. Provider descriptor and discovery

Every provider supplies descriptor metadata that can be read without constructing its implementation. The descriptor contract must be versioned and cover at least:

- stable provider ID, provider version and SPI/API compatibility range;
- artifact identity, version, integrity/provenance and source;
- XML QName/namespace, aliases and format bindings;
- configuration schema and semantic capability/role declarations;
- dependencies and compatibility constraints;
- lifecycle/provider-factory identity;
- required permissions and resource-policy declarations.

Discovery and descriptor projection are side-effect-free. Scanning, catalog generation, schema generation and semantic projection must not instantiate implementation classes or execute plugin code. Providers must not require public no-argument constructors. The host constructs an implementation through its DI/provider factory only after compatibility checks and activation/authorization.

The discovery mechanism is generic over managed artifacts. No hard-coded package roots, per-plugin Core registration statements, or manually curated list of extension implementation classes may be required.

## 4. Autonomous reconciliation and registry generations

The platform maintains a current effective registry generation. Artifact-source changes trigger reconciliation:

1. Observe and fingerprint the managed artifact set.
2. Discover provider descriptors from candidate artifacts.
3. Validate descriptor syntax, provider/API compatibility, dependency graph, artifact integrity, IDs, QNames, namespaces, aliases and policy requirements.
4. Resolve host enablement and authorization policy.
5. Construct authorized providers through host DI/factories and prepare them in an isolated candidate generation without irreversible business side effects.
6. Build all runtime and semantic projections from that prepared candidate.
7. Atomically publish the complete new immutable/versioned generation.
8. Retire the previous generation only when no execution/session still references it.

A failed reconciliation must not partially mutate the active registry. The last-known-good generation remains available, and diagnostics identify the failed artifact/provider and reason.

Duplicate IDs, QNames, namespace bindings or aliases are errors, never last-write-wins map updates. Broken or incompatible providers must not silently disappear while bootstrap/reconciliation reports success.

Each generation exposes a stable generation ID or digest and provider/artifact provenance. Registry output must be deterministic regardless of artifact enumeration or JAR ordering.

### 4.1 Reconciliation transaction and lifecycle phases

Reconciliation is serialized per managed source (or uses an equivalent
compare-and-swap protocol) so concurrent filesystem events cannot publish
out-of-order snapshots. A candidate is built from a stable, immutable artifact
set and has a deterministic input digest.

Provider lifecycle must distinguish at least:

1. **Describe:** read and validate artifact metadata without loading provider
   implementation classes.
2. **Authorize:** evaluate host trust, enablement and requested capabilities.
3. **Create/prepare:** load and construct only authorized providers through the
   host factory. Preparation may validate dependencies and allocate reversible,
   host-owned resources, but must not perform irreversible business actions
   such as sending mail, mutating remote data or starting unowned background
   work.
4. **Commit:** atomically publish one immutable generation containing the
   descriptor set, provider handles, dispatch table and semantic/schema
   projections. No consumer may observe a mixture of candidate and active
   projections.
5. **Rollback:** on any preparation/commit failure, close all candidate resources
   and retain the last-known-good generation. Report the failed provider and
   artifact; do not silently omit it and report success.

Actual plugin business effects occur during an admitted execution through
policy-aware host services, not during discovery or descriptor construction.
The lifecycle SPI must define how a provider's prepared handle becomes usable at
commit, how partial activation is rolled back, and how failure after publication
is handled without admitting sessions to a half-ready generation.

## 5. One registry truth for semantics and execution

The effective registry generation is the sole source of truth for the enabled capability set. The following projections must derive from the same generation:

- runtime XML/format dispatch;
- provider and capability metadata/catalog;
- configuration schema and editor/palette projection;
- structural and semantic validation;
- inspection and capability introspection;
- IDE and agent-facing capability views.

The Semantic Model is dynamic: it describes what the current runtime generation can actually do, including capability contracts, inputs/outputs, composition constraints, dependencies and policy requirements. It must not rely on a static list of implementation classes, hard-coded optional-plugin profiles, or independent class-presence probes.

Every inspection/validation result identifies its registry generation. An execution pins a generation. If a caller attempts to execute against a different/stale generation, the host must reject or explicitly revalidate it. Existing sessions may continue on an older generation while new sessions use the latest one; no session may observe a mixture of generations.

## 6. Activation, trust and permissions

Discovery is not activation, and activation is not unrestricted authorization.

The platform distinguishes at least these states:

- discovered: artifact/provider descriptor found;
- compatible: descriptor and dependencies satisfy the supported contract;
- available: provider can be offered by the host;
- enabled: host policy selects it for the effective registry;
- authorized: required permissions and resource policies are granted;
- active/executable: implementation has been constructed and initialized successfully.

The host controls trust, enablement, permissions, resource limits and provider construction. A newly discovered third-party provider must not execute merely because its JAR is present. Disabled or unauthorized providers must not be constructed or exposed as executable capabilities.

Permission declarations must be explicit and inspectable, covering relevant capabilities such as filesystem access, outbound network, database access, browser/script execution, mail delivery, process execution, secrets and resource consumption. Core defines the contract; the embedding host decides policy.

## 6.1 In-process trust boundary

Host enablement, permission declarations and resource policies are necessary
controls, but they do **not** sandbox arbitrary Java code loaded into the same
JVM. In-process providers execute with the effective privileges of the host
process and may be able to access process-visible files, credentials, network,
threads, native interfaces or other ambient capabilities. A Java ClassLoader
provides namespace and lifecycle isolation; it is not a security boundary.

The platform must therefore distinguish trusted in-process extensions from
untrusted code. Only providers admitted by the host's trust policy may execute
in-process. If the product intends to support genuinely untrusted third-party
extensions, their execution requires a separate process and an explicit
IPC/capability boundary, with OS/container restrictions appropriate to the
deployment. Do not describe permission metadata or a custom ClassLoader as a
sandbox. This trust decision is a release architecture requirement, not an
optional implementation detail.

## 7. Update, removal and classloader lifecycle

Dynamic update/removal must be safe while the host is serving work:

- New executions use the newly published generation.
- Existing executions retain their pinned generation and provider references.
- Removal stops admitting new work for the provider.
- The host applies a defined drain, timeout or cancellation policy to sessions using the retiring provider.
- Provider resources are closed on success, failure, cancellation, update, removal and host shutdown.
- A classloader is closed/released only after all sessions, threads, callbacks and resources referencing it are safely retired.
- Failed initialization rolls back candidate resources and leaves the last-known-good generation active.
- Repeated add/update/remove cycles must not leak classloaders, threads, file handles or static state.

The contract must define behavior for dependency updates, incompatible replacement, provider ID reuse and removal of a provider required by another provider.

### 7.1 Classloader and dependency graph rules

A managed artifact is loaded through an owned classloader/module graph, never by
mutating the host application classpath. The SPI/API packages and Java platform
packages must resolve from the host/platform parent; an extension must not
shadow or bundle a second incompatible copy of the WebHarvest SPI. Private
implementation dependencies are resolved from the artifact's immutable,
version-pinned dependency graph. Split packages, ambiguous class ownership and
undeclared dependency access fail closed with diagnostics.

The implementation may choose one classloader per artifact or a deterministic
layered graph, but it must define dependency sharing and conflict resolution.
A dependency update creates a new graph/generation; it must not mutate classes
visible to an existing generation.

Providers must not leave unowned threads, ThreadLocals, context-classloader
references, shutdown hooks, global listeners, JDBC registrations or static
callbacks that pin a retiring loader. The host lifecycle contract must account
for these resources and test that they retire after all generation/session
leases are released. ClassLoader.close() alone is not proof that a loader is
collectible.

## 8. Observability and failure behavior

The host must expose actionable diagnostics for artifact detection, descriptor parsing, compatibility, dependency resolution, policy decisions, activation, reconciliation, generation publication and retirement.

Diagnostics include generation ID, provider ID/version, artifact provenance and stable error category. Sensitive values must not be exposed in logs. A malformed provider or failed update must be isolated; unaffected providers and the last-known-good generation remain usable.

## 9. Mandatory release conformance plan

The release gate is not satisfied by unit tests for a startup scanner. The exact candidate must pass all of the following.

### 9.1 Independent external provider fixture

Build a separate Maven project outside the WebHarvest repository using only the published candidate Core/SPI contract. Its package must not be among any historical Core scan roots. Do not edit Core source, package lists or registration tables to make it discoverable.

### 9.2 Live lifecycle scenario

1. Start a Core-only host with an empty managed extension source; record generation G0.
2. Add the external artifact while the host remains running.
3. Without restart, discover its descriptor and publish G1. Verify metadata, schema, inspection and Semantic Model expose the capability as discovered/available.
4. Apply host enablement and permissions. Verify only then is the provider constructed through host DI/factory.
5. Execute an XML construct using G1.
6. Replace/update the artifact while the host remains running; publish G2. New sessions use G2 while sessions pinned to G1 remain consistent.
7. Remove/disable the artifact; publish G3, stop new admissions, drain/cancel active work, close resources and release the classloader only when safe.
8. Re-add the artifact and verify clean discovery with no stale descriptors or duplicate registrations.

### 9.3 Required negative tests

- Disabled provider constructor/initializer sentinel remains untouched during discovery, catalog generation and semantic projection.
- DI-only provider without a public no-argument constructor is discovered and constructed only after authorization.
- Duplicate IDs, QNames, namespaces and aliases fail deterministically.
- Malformed descriptors, unsupported SPI versions, missing dependencies, invalid permissions and linkage failures produce explicit diagnostics and no partial publication.
- Failed update preserves the last-known-good generation.
- An unauthorized capability cannot be executed even if discovered.
- Stale validation/execution generation mismatch is rejected or revalidated.
- Removed providers cannot receive new work; existing work follows drain/cancel policy.

### 9.4 Concurrency, determinism and leak tests

- Concurrent discovery/reconciliation and execution never expose mixed registry state.
- Each execution pins exactly one generation.
- JAR order and repeated reconciliation produce deterministic registry content/digest.
- Add/update/remove/re-add cycles do not leave duplicate providers or stale semantic entries.
- Classloader, thread, callback and resource leak checks pass after sessions retire.
- Core-only startup and execution work without optional extension modules.
- XML compatibility, including legacy `set-var`, remains intact.
- Runtime dispatch, metadata, schema, semantic index and validation have exact enabled provider/QName/alias parity for each generation.

### 9.5 Evidence required

Attach test reports, exact Core/SPI and fixture artifact hashes, registry generation IDs/digests, and CI run links to #94 and #88. No test is marked passed until executed against the exact candidate.

## 10. Current-state warning

The current implementation is not this target architecture. The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md) records hard-coded package roots, constructor-based metadata catalog creation, independent manual runtime registration, hard-coded optional-plugin availability and lack of a unified generation-aware registry. Existing Stage 1 XML inspection is useful groundwork, not proof of dynamic runtime discovery.

No implementation, test pass or release readiness is implied by this design document.

## 11. Implementation sequence and exit gates

1. Complete #93 inventory and approve Core/extension boundaries.
2. Approve #346 versioned descriptor/SPI and compatibility contract.
3. Implement #347 managed artifact source, autonomous discovery, reconciliation and atomic registry generations.
4. Implement #349 host policy, DI activation, execution pinning and safe lifecycle; integrate with #347.
5. Implement #348 dynamic semantic/catalog/schema/inspection/validation projections from the exact registry generation.
6. Complete #350 independent fixture and full live lifecycle/concurrency/security conformance.
7. Update #88 with exact-candidate evidence and explicit maintainer sign-off.

Do not implement a startup-only scanner as an interim substitute. Preserve XML compatibility and keep Core free of compile-time dependencies on optional extensions.


### 7.2 Host-owned service lifetime and the current HTTP singleton gap

The current Core wiring binds `HttpService` to `HttpServiceImpl` in Guice `Singleton` scope inside `ScraperModule` (`ScraperModule.java` blob `d31a5e30b0949c752743123cbcfb4219ba60713e`). This means one service/client is shared by all executions using that injector. The scope is **per injector**, not a process-wide singleton.

The existing `Harvester.execute` path enters and exits a per-execution `ScraperScope`, but `HttpService` is not in that scope. `ScrapingHarvester.execute` exits the scope in `finally` (`ScrapingHarvester.java` blob `2af32ca67c235e075c8cc613118814d54e555338`); `DefaultHarvesterFactory` creates harvesters from the injector-owned providers (`DefaultHarvesterFactory.java` blob `58bae4e22a82d89dac1c6e63e28ce5f3587e687e`). The HTTP client is therefore shared across executions and is not automatically closed when an execution scope exits.

`HttpServiceImpl` implements `AutoCloseable` and closes its underlying `CloseableHttpClient` (`HttpServiceImpl.java` blob `9e3dab1a40250f4d059ea812c74012d994873f0d`). However, Guice's injector does not provide an application shutdown contract here, and the inspected production CLI creates an injector and executes a harvester without a corresponding `HttpService.close()` or host-runtime close path (`WebHarvestCLI.java` blob `c094100d160ae02618604923a32a79a85de3bbb0`). The Core API does not currently expose a runtime/container owner with deterministic shutdown semantics.

The test suite shows inconsistent ownership:
- `HttpPluginWithDependencyInjectionTest` explicitly obtains and closes `HttpServiceImpl` in `@AfterMethod` (`fa82e5b92b00fb43cf1577bf40bd2b7cb3d77b9f`).
- `HttpServiceImplBasicTest` creates a new injector in `@BeforeMethod`, retains only the service, and has no teardown/close (`3e4d6930be8c51c8b2ac4e7043227e8c827b9fcd`).

**Conclusion:** singleton scope itself is not automatically the defect. A shared, thread-safe HTTP connection pool can be the right lifetime across concurrent sessions. The demonstrated defect is that the owning runtime/injector has no explicit, reliable resource-disposal contract; the current CLI and at least one test path do not close the client. Moving the binding to per-execution scope would avoid neither the ownership question nor necessarily be desirable: it could create one client/pool per execution and lose connection reuse.

**Provisional target:** introduce an explicit host/runtime owner (for example, an `AutoCloseable` runtime/container facade) that owns the injector and all application-scoped services. It must stop new executions, drain or cancel active sessions according to policy, then close managed services exactly once. Keep shared HTTP transport at runtime scope if concurrency tests validate it; pass per-session/request policy and cancellation context separately rather than turning the connection pool into a session singleton. Do not close the shared service at the end of an individual scrape.

Required tests: two concurrent sessions safely share the runtime-owned service; closing one session does not close the client; host shutdown prevents new work, drains/cancels active work and closes the client once; close is idempotent; failures during startup/shutdown clean up already-created resources; post-close use fails deterministically. Update test fixtures to close their runtime/injector owner rather than reaching into Guice for an implementation-specific service.

This is a separate host lifecycle issue under #349, not a reason to change the plugin inventory or replace `Singleton` with `ScrapingScope` immediately. No runtime code or tests were changed/run in this audit.


## 7.3 Container-neutral embedding and host IoC integration

WebHarvest is an embeddable Java library/runtime. It must be usable inside an
arbitrary Java application without requiring that application to adopt Guice,
Spring, CDI, or any other specific IoC container. The embedding application
owns its composition root and remains free to use its existing container—or
plain Java construction where appropriate.

**The Core/runtime/plugin contracts are container-neutral.** No public Core,
runtime or plugin SPI contract may require Guice/Spring/CDI types, annotations,
modules, scopes, injectors, bean factories or container-specific lifecycle
callbacks. A container is an integration mechanism supplied by the host, not
part of WebHarvest's product identity or runtime semantics.

The architecture must separate four responsibilities:

1. **WebHarvest runtime contract:** defines provider creation/activation,
   required host services, execution/session contexts, registry generations,
   lifecycle and disposal semantics without naming an IoC implementation.
2. **Host composition contract:** lets the embedding application provide
   services, factories and provider instances using its own container and
   configuration. The host controls object construction and dependency
   resolution.
3. **Container adapters:** bridge the neutral contract to a specific container.
   The existing Guice integration is the first compatibility adapter; a
   Spring adapter is a sensible next integration target. CDI or other adapters
   should be added when justified by real embedding demand, not by embedding
   several containers into Core.
4. **Standalone composition:** Core/runtime must also be constructible without
   any external IoC framework, so small Java applications and tests are not
   forced to install a container.

### 7.3.1 Dependency and module boundaries

- The Core artifact must not require Guice (or another IoC framework) merely to
  load, inspect, validate or execute the supported Core-only profile.
- Container-specific integration code belongs in separate adapter artifacts
  or optional integration modules, not in the neutral Core API/SPI.
- Plugin descriptors declare stable service/capability requirements and
  provider lifecycle metadata; they do not declare Guice keys, Spring bean
  names, CDI qualifiers or container-specific scopes.
- The host adapter resolves those requirements against the host's own
  composition root and supplies a neutral provider factory/service-access
  contract to WebHarvest.
- Do not start a second independent IoC container inside WebHarvest when the
  host already has one. One host-owned composition graph should own the
  WebHarvest runtime and its integrations.
- A plugin may use constructor injection and need not expose a public
  no-argument constructor. Its construction must be delegated to the host
  factory/adapter after descriptor validation and host authorization.
- A plain-Java factory is a first-class integration path, not a fallback with
  reduced lifecycle guarantees.

### 7.3.2 Explicit ownership and scopes

The neutral lifecycle contract must distinguish and define at least:

- **Host/application scope:** services shared with the embedding application;
  their ultimate owner remains the host unless ownership is explicitly
  transferred.
- **WebHarvest runtime scope:** runtime-owned shared services and registries;
  closed when the runtime owner is closed, not when one execution ends.
- **Registry-generation scope:** provider handles and dependency/classloader
  graph retained while any session pins that generation.
- **Execution/session scope:** per-execution context, policy view, cancellation
  and temporary resources; closing one session must not dispose host- or
  runtime-scoped services.
- **Provider scope:** provider-specific prepared resources, concurrency model
  and close semantics, explicitly declared by the SPI.

The host adapter maps these neutral lifetimes to its container's scopes where
possible. If a container cannot express a required lifetime directly, the
adapter must implement it explicitly; it must not silently approximate a
runtime singleton with a per-session bean or let container shutdown race active
sessions.

The runtime owner must stop admission, apply the configured drain/cancel
policy, retire generation leases, close runtime-owned resources exactly once,
and leave host-owned services to the host. Shutdown and close operations must
be idempotent and failure-reporting.

### 7.3.3 Current migration boundary (not yet implemented)

The current source is Guice-oriented: `ScraperModule` is the composition
root, and the inspected runtime also includes Guice-specific factory/scope and
plugin-injection paths (including `DefaultHarvesterFactory`,
`ScrapingHarvester`, `AttributeHolderScope`, `DatabaseModule` and
`PluginDependencyInjector`). The existing `HttpServiceImpl` singleton is
per Guice injector, while its deterministic disposal is not owned by an
explicit public runtime handle in the inspected CLI path.

These are migration seams, not a reason to move the singleton blindly into
session scope. First define the neutral host/runtime/session/provider lifecycle
and service-resolution contracts; then adapt the existing Guice composition
to those contracts. Preserve shared thread-safe services at runtime scope and
keep execution-specific policy/cancellation in session scope.

### 7.3.4 Mandatory embedding conformance

The release conformance suite must prove the same Core/runtime and dynamic
plugin behavior in these independent host arrangements:

- a plain-Java host using explicit factories, with no IoC framework;
- a Guice host adapter;
- a Spring host adapter (once declared as a supported adapter).

For each arrangement, verify Core-only startup, host service injection,
constructor-injected external plugin activation, dynamic add/update/remove,
generation pinning, concurrent sessions, cancellation, deterministic shutdown
and resource ownership. The external plugin fixture must not compile against
container-specific WebHarvest internals. The same neutral SPI and behavior
must be used in every arrangement.

This is a product-level embedding requirement, not a request to support every
IoC framework with a bespoke adapter on day one. The neutral contract must
permit arbitrary host containers; supported first-party adapters are delivered
and tested as separate integrations.


## 7.4 Descriptor contract: provider identity versus XML child syntax

The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md), sections 15.22–15.23, demonstrates that an XML element name is not necessarily an independently activatable provider. The SPI descriptor must model these separately.

A descriptor contract must express at least:

- **Provider identity and provenance:** stable provider ID, artifact identity/digest, qualified element identity (namespace URI + local name), aliases and compatibility/version rules.
- **XML role:** executable capability, declarative detail/configuration node, internal/root construct, or compatibility alias. Role is not inferred from package, annotation, visual palette category or class name.
- **Parent/child grammar:** allowed and required child roles, required parent contexts, cardinality, ordering where relevant, and whether constraints apply to the direct parent or an ancestor context. A flat `requiredParents: string[]` is insufficient for constructs such as `call-param`, whose documented contexts are direct child of `call` for arguments and child of `def` within a `function` for parameter lookup.
- **Value/body semantics:** whether the node consumes raw text/configuration, evaluates its body, allows nested executable processors, contributes a value to its parent, or is interpreted entirely by the parent. For example, RegExp and XSLT parents read child configuration directly; XQuery consumes expression/parameter nodes, and XQuery parameter bodies can evaluate nested value-producing processors.
- **Construction and lifecycle:** whether a provider instance is required at all for a declarative child, and if so, how it is created, scoped and closed through the neutral host/runtime contract.

The current implementation contains meaningful drift between visual `ParentChildRelationship` metadata and schema `getRequiredParents()`; some detail nodes advertise a required parent visually while their schemas omit the corresponding constraint. Other child implementation classes are no-op placeholders while the parent directly consumes their raw configuration. A discovery/catalog implementation must therefore not instantiate every annotated class to build its semantic model, nor assume every registered XML name represents an executable extension.

**Generation invariant:** parsing, structural validation, semantic projection, inspection and execution must consume one immutable descriptor/grammar generation. The parsed tree must be validated against the exact generation pinned by execution; stale validation cannot silently authorize execution under a changed provider or grammar generation. Descriptor discovery remains side-effect-free and does not instantiate providers. Provider preparation/activation happens only after host policy authorizes the candidate generation.

These fields are design requirements to settle in the SPI contract (#346) and external fixture (#350), not an implementation claim. No runtime changes or tests are implied by this document update.


### 9.6 Descriptor and concrete XML-tree conformance

The external fixture and Core compatibility suite must additionally prove:

- discovery/catalog/semantic projection reads provider descriptors without invoking provider constructors or static initialization with observable side effects;
- a declarative child node can be represented, validated and interpreted by its parent without being instantiated as an independent provider;
- required and allowed child rules are enforced against the concrete XML tree, including missing required children, illegal children, duplicate singleton children and ordering where the descriptor declares order significant;
- ancestor-sensitive constraints are enforced (including the two documented `call-param` contexts), rather than approximated by a flat immediate-parent string list;
- body semantics are preserved: raw text, evaluated expression, nested value-producing processor and parent-consumed configuration are distinguishable;
- schema and visual metadata disagreements are surfaced as descriptor-build diagnostics or rejected, never silently resolved by choosing one source;
- namespace URI + local name is the dispatch identity; duplicate qualified identities and conflicting aliases are rejected during candidate-generation validation, while the same local name in distinct namespaces remains unambiguous;
- an XML tree validated under generation N cannot execute under generation N+1 without explicit revalidation; a running session remains pinned to its original generation.

Tests must include both positive legacy XML fixtures and negative malformed-tree/collision fixtures. They must assert not only validation results but also which provider instances were and were not constructed. These tests complement, rather than replace, the live add/update/remove/re-add lifecycle scenario in section 9.2.
