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


## 7.5 Host-effect services required by the current Core compatibility surface

The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md), section 15.24, confirms that several Core operations currently reach host effects without a complete policy-aware service contract. The neutral SPI and runtime design must not stop at declarative permission labels.

The host composition API must provide explicit, policy-bearing services (or equivalent capabilities) for at least:

- **Filesystem access:** canonicalized path resolution against host-granted roots; separate read/write/list/create permissions; symlink and race-safe handling; bounded bytes, entries and recursion; deterministic close/cancellation; structured policy-denial errors. A working-directory string alone is not a sandbox.
- **Configuration inclusion:** a resolver that returns a typed source identity and applies policy after relative resolution. File and URL sources need distinct grants; outbound URL policy, redirect revalidation, connect/read timeouts, byte limits, include depth and cycle detection must be enforced at the resolved-source boundary. Nested included configuration must retain the same pinned registry generation and host policy context as its parent execution.
- **Script execution:** an explicitly authorized engine/provider and host-object/capability view, with execution budgets, cancellation and lifecycle ownership. JSR-223 engine discovery must not implicitly authorize a language. Thread-local engine caches and provider classloader references must be retired with their owning runtime/provider generation. A fallback execution path must not silently bypass a denied or unavailable policy-aware engine.

Descriptors may declare requested effects and resource ceilings, but the host grants and enforces them. Built-in Core operations and external providers must use the same service contracts; an extension must not gain a broader filesystem/network/script capability by calling JDK or third-party APIs directly.

The compatibility plan must preserve existing XML behavior where explicitly approved, while restricted profiles can deny effects predictably and expose structured diagnostics. No service implementation or migration is claimed here; this is a required design input for #346, #349 and #350.


### 7.5.1 Resolved-target authorization is mandatory

The follow-up helper trace in PR #351 section 15.25 confirms that current path helpers are not security boundaries: filename adaptation only changes separators; relative filesystem includes are concatenated with the base path; absolute paths are accepted unchanged; and URL includes accept a full URL unchanged. URL dot-segment normalization does not constrain destination hosts, schemes, resolved IP addresses or redirects.

Therefore:
- authorize canonical filesystem targets after path resolution, with symlink/race-safe enforcement at open/list/create time;
- reject or explicitly grant absolute paths and traversal outside configured roots;
- authorize URL scheme, hostname, port and resolved address classes, and revalidate every redirect/connection target;
- apply byte/entry/depth budgets during reads and recursive enumeration, not after materializing the result;
- never treat URL normalization or a base URL as an SSRF control.

The external fixture and built-in compatibility suite must include traversal, absolute-path, symlink, redirect-to-private-address, DNS/address-change and oversized recursive-list cases. This refines the requirements in 7.5; no implementation is present or claimed.


## 7.6 Network, output, time and nested-execution host services

The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md), sections 15.26–15.27, shows that host effects and nested execution need explicit neutral contracts beyond filesystem/include/script services.

- **Network transport:** host-provided request service must accept a policy/deadline/cancellation context; validate initial and every redirect destination; enforce connect/response deadlines and streaming byte limits; expose supported HTTP methods honestly; define status and redirect/header/credential forwarding semantics; redact URL credentials/query values and response bodies from logs by default. A plugin-level timeout attribute that is not carried into the service is not enforcement.
- **Nested execution:** include, loop source evaluation and loop body execution must inherit the exact parent session's immutable registry generation, host policy, cancellation token, deadline and resource budget. No nested `new DefaultPluginBodyExecutor()` may fall back to a process-global mutable registry. The nested operation must retain the parent's generation lease until completion.
- **Output/logging:** standard output/error are host-owned process-global resources. Core logging/output plugins must route through a host output service with redaction and quotas, or be explicitly disabled in restricted profiles; they must not write directly to process streams behind the host's back.
- **Time and control flow:** sleep must consume the same execution deadline/budget and remain interruptible. Loop iterations, materialized source items and accumulated output require bounded budgets. Negative/unbounded loop limits must be explicitly authorized or rejected by policy. Exit/stop outcomes must remain execution-scoped and distinct from host/runtime shutdown.

These are capability contracts, not just descriptor labels. Built-in Core and external providers must receive the same host-controlled services, with lifecycle ownership defined by the neutral runtime API.


### 9.7 Host-effect and nested-generation conformance

The external provider fixture and built-in Core compatibility suite must verify the following against the same neutral service contracts:

- HTTP method support is consistent across descriptor/schema/service; unsupported methods fail during structural/configuration validation rather than after network dispatch.
- Timeout/deadline values reach the transport; connect/read/total deadlines, redirect destination revalidation, response byte ceilings, cancellation and status handling are tested with controlled local fixtures.
- Redirects to loopback, link-local, private and otherwise denied destinations are rejected according to host policy, including multi-hop redirects and address changes. Authorization headers are not forwarded across origins unless the host policy explicitly allows it.
- Oversized/chunked responses are stopped while streaming; response bodies and URL credentials/query tokens do not appear in logs.
- Loop source and body child execution use the same pinned generation as the parent. Reconcile/update during an active loop must not mix generations; a nested include/source/body cannot silently observe the newly published generation.
- Loop source materialization, iteration count, accumulated output and sleep consume bounded per-execution budgets; cancellation interrupts a long loop/sleep and releases generation/resource leases.
- Output/logging is routed through the host-provided output service; restricted profiles can deny or redact it without changing process-global stdout/stderr.
- Execution-scoped exit, cancellation and runtime shutdown remain distinguishable and do not terminate the host JVM or unrelated sessions.

Use deterministic fake/loopback services and failure injection. Tests must assert the effective policy at the transport/output boundary, not only that descriptor metadata declares a permission. No such implementation or passing test is claimed by this design document.


## 7.7 Current Guice coupling is a migration blocker, not an adapter boundary

The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md), section 15.28, confirms that the current runtime composition path is Guice-specific: `DefaultHarvesterFactory` and `ScrapingHarvester` expose Guice `Provider`; `AttributeHolderScope` implements Guice `Scope` and exposes Guice `Key`/`Provider`; `ScraperModule` is an `AbstractModule` and performs static injection; `InjectorHelper` stores a process-static injector; and `DefaultPluginBodyExecutor` resolves providers through that global injector or deprecated no-arg reflection.

An existing `PluginDependencyInjector` interface is not yet the active neutral seam: the default executor does not call it, and its reflection implementation performs field injection only, without constructor injection or lifecycle ownership. It must not be mistaken for the desired host composition API.

Migration constraints:
- Public Core/runtime/plugin contracts must not expose Guice types or require Guice to load and execute a Core-only plain-Java profile.
- The runtime must be created through an explicit host-owned composition/factory contract. No process-static injector, static service locator or hidden second container is permitted.
- Provider construction must be explicit and support constructor injection through a neutral factory/service bridge; adapters may delegate to host Guice/Spring/CDI but may not own an independent parallel object graph.
- Scope semantics must be neutral and explicit (host/application, runtime, registry generation/provider, execution/session). A thread-local scope adapter may be an implementation detail, not the public lifecycle model; asynchronous execution must not accidentally lose or cross-contaminate scope context.
- Runtime shutdown must stop admission, drain/cancel sessions, release provider-generation leases, close runtime-owned services exactly once, and never close host-owned objects.
- Static injection into `BaseTemplater` must be replaced by explicit runtime/session scripting-service access so two runtimes in one JVM cannot overwrite each other's engine factory.

The implementation plan must first inventory public signatures, dependency scopes, static state and construction call sites; then introduce neutral seams behind compatibility adapters. Do not remove Guice from the build or restructure POMs as an isolated cleanup before the plain-Java fixture and Guice adapter tests prove parity.


### 7.6.1 Redirects must not forward credentials across origins

The source audit in PR #351 section 15.29 confirms that the current `HeaderPreservingInterceptor` reapplies configured default headers to every request, including redirects, without checking origin. Its documented use cases include Authorization and API keys. The current redirect test covers only a same-origin relative redirect and asserts that the API key is preserved.

The neutral transport contract must define an origin as scheme + host + effective port and strip Authorization, Proxy-Authorization, Cookie and configured secret headers when a redirect changes origin, by default. Host policy may grant a narrowly scoped exception, but it must be explicit, auditable and bound to the destination. Redirect loops, hop count, scheme downgrade, DNS/address changes and destination policy must be enforced in the transport itself.

The conformance suite must use two independent loopback origins (different ports) and prove secret-header stripping across cross-origin and multi-hop redirects, while separately testing same-origin preservation where allowed. A fake `HttpService` test that only verifies the plugin forwards a verb does not prove the concrete transport supports that verb; real `HttpServiceImpl` transport tests are mandatory.


### 9.8 HTTP transport tests must exercise the real implementation

- Use two local HTTP servers on distinct ports as separate origins. Redirect from origin A to B and assert that Authorization, Cookie, Proxy-Authorization and configured secret headers are removed unless a destination-specific host grant explicitly permits forwarding.
- Test same-origin preservation separately from cross-origin stripping; do not infer one from the other. Include multi-hop redirects, HTTPS-to-HTTP downgrade policy, redirect hop limits and denied private/link-local targets.
- Exercise every advertised HTTP method through the concrete `HttpServiceImpl`; plugin tests with a recording/mock `HttpService` only verify adapter forwarding and cannot establish transport support.
- Verify configured request deadlines actually reach the client, response bodies are bounded while streaming, cancellation aborts in-flight I/O, and logs redact query credentials and response content.
- Keep the test deterministic and offline; no public internet dependency. Test source presence is not a pass—release evidence must include successful execution on the exact candidate commit.


### 7.7.1 Multiple runtimes in one JVM must remain isolated

The source audit in PR #351 section 15.30 confirms `BaseTemplater.scriptEngineFactory` is a public static Guice-injected field read by static evaluation methods. `ScraperModule` installs it through static injection. Consequently, scripting configuration is shared process-wide rather than owned by a runtime/session, so independently embedded WebHarvest runtimes can overwrite or observe one another's engine factory.

The neutral API must make scripting/templating services explicit dependencies of each runtime or execution context. Conformance must create two runtime instances with different engine providers and prove isolation under concurrent execution, reconfiguration and close; closing one runtime must not affect the other. No process-static service locator or static injection is allowed for runtime-specific services.


## 7.8 Query, transformation and regex execution budgets

The source audit in [PLUGIN_INVENTORY.md](PLUGIN_INVENTORY.md), section 15.31, expands the execution-budget boundary beyond loops and transport:

- **Nested dispatch:** function calls, conditional branches and regex result templates also create `DefaultPluginBodyExecutor` instances directly. All nested dispatch must inherit the exact parent registry generation, policy context, cancellation/deadline and resource budget. No plugin may construct a default executor that silently resolves against mutable global state.
- **XPath/XQuery:** provide host-controlled query execution deadlines/cancellation, bounded input/result materialization and an explicit Saxon resource-resolution policy. Confirm the exact Saxon version's defaults and resolver APIs before asserting external-resource exploitability; absence of explicit configuration in current source is a gap to resolve, not proof of a specific vulnerability.
- **XSLT:** preserve the existing secure-processing feature, disabled external URI resolution and disabled external entity/DTD processing. Add output-size and execution-time budgets without re-enabling external resources.
- **HTML/JSON conversion:** bound source bytes, nesting/DOM depth, result bytes and processing time. These plugins currently materialize complete DOM/JSON objects and complete output strings.
- **Regular expressions:** caller-controlled Java regex patterns over complete input strings need a ReDoS-safe execution policy. A session cancellation token cannot safely interrupt arbitrary catastrophic backtracking in `java.util.regex`; use a linear-time engine, restricted pattern language, or isolated bounded execution with explicit compatibility trade-offs.
- **Shared budget:** all nested transformations consume one session budget (time, bytes, nodes/items, output, recursion/depth), not independent per-plugin ceilings that can be multiplied through nesting.

## 9.9 Nested dispatch and transformation conformance

- Reconcile the registry while a function call, conditional branch, regex result template, loop body/source and include are active; each active execution must stay on its pinned generation and new sessions see only the committed generation.
- Assert that nested provider construction uses the runtime's neutral provider factory, never direct no-arg reflection, static injectors or a new default executor detached from the parent context.
- Verify XSLT rejects DOCTYPE, external entities and external stylesheet/document URI resolution; preserve this behavior while adding bounded output and deadline controls.
- Run XPath/XQuery against bounded local fixtures; verify deadline, cancellation, result-size limits and the configured Saxon resource resolver. Include explicit tests for `doc()`/external resource access according to the chosen host policy.
- Exercise HTML/JSON conversion with oversized input, excessive nesting and output expansion; assert bounded failure without exhausting the host process.
- Test regex patterns known to cause catastrophic backtracking against bounded fixtures; prove the selected policy terminates within a deterministic limit and preserves documented legacy pattern behavior.
- Assert the shared budget is consumed across nested calls, rather than reset on each nested executor/plugin.

Tests must run on the exact candidate commit. Source-level test presence or declared metadata is not acceptance evidence.


### 7.8.1 Loop limits are validated and enforced from one effective policy

The source audit in PR #351 section 15.33 found that `WhilePlugin` validation accepts `maxloops` up to 1,000,000 while execution silently clamps to 10,000. Future validation must calculate the effective policy limit once and expose that same value to inspection, diagnostics and execution. A configuration that requests more than the host allows must either be rejected before execution with a structured policy diagnostic or explicitly normalized with a visible effective value; silent runtime clamping is not acceptable. The shared session budget remains authoritative across nested loops.

## 7.9 Plugin construction and dependency injection: the existing neutral-looking SPI is not active

The current source contains `org.webharvest.plugin.di.PluginDependencyInjector` and `DefaultPluginDependencyInjector`, but these types do not currently form the active plugin-construction path and must not be mistaken for an existing container-neutral runtime SPI.

Source evidence on the inspected `main` baseline:

- `DefaultPluginBodyExecutor.createChildPlugin()` resolves the implementation through the process-global `CorePluginRegistry`, then calls `InjectorHelper.getInjector()`. If a Guice injector is present it invokes `injector.getInstance(pluginClass)`; otherwise it falls back to deprecated no-argument reflective construction. It then calls `initialize()`. This active path imports Guice's `Injector` directly.
- `InjectorHelper` stores the Guice `Injector` in a static field. `ScraperModule` requests static injection for that helper. The fallback is therefore not a host composition API; it is a second construction mode with a public no-argument-constructor constraint and no explicit dependency/lifecycle contract.
- `PluginDependencyInjector` is a separate class-keyed registry of `DependencyProvider<T>` and singleton instances. Its default implementation scans only `plugin.getClass().getDeclaredFields()` for the project's own `@InjectDependency` annotation, resolves exact field types and sets fields reflectively. It does not traverse superclasses, support constructor injection, qualifiers/generic keys, explicit scopes beyond singleton/provider, lifecycle ownership/disposal, or participate automatically in `DefaultPluginBodyExecutor.createChildPlugin()`.
- The project's `@InjectDependency` mechanism is distinct from Guice's `@Inject`. Current Core plugins use Guice field injection for runtime services, including `HttpPlugin.httpService`, `ScriptPlugin.scriptEngineFactory`, `FilePlugin.workingDir` (with `@WorkingDir`) and `IncludePlugin.configFactory`. The standalone `PluginDependencyInjector` therefore does not satisfy those dependencies on its own.
- The public `Plugin` interface itself has no Guice types in its method signatures, but this does not make the runtime container-neutral: the default executor and composition path directly depend on Guice, and runtime-specific dependencies are also process-static in `InjectorHelper` and `BaseTemplater`.

Required target:

1. Make plugin creation an explicit runtime-owned neutral contract, e.g. a provider/factory receiving a plugin descriptor and a typed service-resolution context. Constructor injection must be first-class; no public no-argument constructor requirement.
2. Keep the Core SPI free of Guice/Spring/CDI classes and annotations. The host may supply instances/factories directly; optional adapters may bridge Guice first and Spring next. WebHarvest must not create a second IoC container inside an embedding host.
3. Resolve services by stable typed keys/requirements with explicit qualifiers and lifetime semantics; do not reduce the contract to raw `Class<?>` keys if that prevents qualified services or classloader-safe identity.
4. Make provider construction, initialization, cleanup and failure rollback part of the same explicit lifecycle contract. Define ownership for host-, runtime-, generation-, session- and plugin-scoped dependencies; close only resources owned by the corresponding runtime.
5. Remove process-global injector lookup from active dispatch. Every nested executor and plugin factory must receive the same runtime composition context and pinned registry generation.
6. Preserve a simple plain-Java embedding path with no Guice dependency required to load, inspect, validate or execute a Core-only profile. Prove it in a clean consumer fixture; separately prove host-supplied Guice and Spring adapters without starting nested containers.

Conformance gaps to close:

- Plain Java host constructs and closes a Core-only runtime with constructor-injected plugins and no Guice classes on its runtime classpath.
- A host-managed Guice injector and a host-managed Spring application context can each supply services to WebHarvest without WebHarvest creating or owning another container.
- Two independent runtimes in one JVM use different providers/configuration concurrently; closing one does not affect the other.
- Plugin constructor, initialization, execution, cleanup and failed-startup paths have deterministic ordering and exactly-once disposal.
- A plugin requiring a qualified service receives the intended binding; missing/ambiguous bindings fail before execution with structured diagnostics.
- Reconciliation and nested dispatch use the same runtime provider factory and pinned generation; a provider failure leaves the last-known-good generation active.
- No active runtime path reaches `InjectorHelper`, Guice `Injector`, static injection, or reflective no-argument construction.

This is a source-backed migration blocker, not an implementation proposal to retrofit `DefaultPluginDependencyInjector` as-is. Its existing tests establish basic singleton/provider lookup and field injection for a simple test class; they do not establish constructor injection, superclass traversal, active-executor integration, container neutrality, isolation or lifecycle ownership. Tests were inspected, not executed.
## 7.10 Public runtime implementation signatures still expose Guice

The neutral Plugin interface does not by itself establish a container-neutral public embedding API. A source inspection of the public runtime implementation classes finds direct Guice types in public signatures and inheritance:

- DefaultHarvesterFactory has a public @Inject constructor accepting Guice Provider<WebScraper>, Provider<EventBus> and ScraperScope, and its implementation stores those Guice providers.
- ScrapingHarvester has a public @Inject constructor accepting Guice Provider<WebScraper> and Provider<EventBus>, plus ScraperScope. Its public class is part of the runtime implementation surface.
- AttributeHolderScope is public and implements Guice Scope and Provider<AHT>; its public scope method accepts Guice Key<T> and Provider<T>.
- ScraperModule is a Guice AbstractModule, requests static injection for InjectorHelper and BaseTemplater, and binds a static ScraperScope. It is a container adapter today, but the runtime factory/harvester constructors and public scope class also expose Guice directly.

This refines the earlier interface-level observation: Harvest/Harvester and Plugin facade contracts may be neutral, but the current public implementation/composition seam is not. Embedders cannot use the current DefaultHarvesterFactory/ScrapingHarvester constructors or AttributeHolderScope without taking a Guice dependency. Treat these as migration seams, not as the future public neutral API.

Required target: publish a small neutral runtime construction API whose public types use WebHarvest-owned provider/factory/lifecycle abstractions or host-supplied instances, and keep Guice Provider/Scope/Key/AbstractModule confined to an optional Guice adapter artifact. The plain-Java consumer fixture must compile and run without Guice on its compile or runtime classpath; a separate host-managed Guice fixture must demonstrate adapter interoperability. Do not merely relocate Guice types behind another public wrapper while leaving Guice mandatory in Core's transitive dependency graph.

Source blobs on current main:
- DefaultHarvesterFactory.java: 58bae4e22a82d89dac1c6e63e28ce5f3587e687e.
- ScrapingHarvester.java: 2af32ca67c235e075c8cc613118814d54e555338.
- AttributeHolderScope.java: 11a39c99e517ae9ae1ba09cf69a885b636465b64.
- ScraperModule.java: d31a5e30b0949c752743123cbcfb4219ba60713e.
## 7.11 Core's Maven dependency graph currently makes Guice a compile dependency

The POM check confirms that Guice is not merely an implementation detail accidentally imported by one adapter class. `webharvest-core/pom.xml` declares `com.google.inject:guice` as a normal dependency with no scope, so Maven treats it as compile/runtime transitive dependency for Core consumers. The root POM's dependencyManagement entry pins Guice 5.1.0; dependencyManagement alone does not add it, but the Core module's explicit dependency does. Core also declares Guava as a normal dependency.

Combined with the source findings in 7.9–7.10, this means a Java application consuming the current Core artifact receives Guice transitively even if it wants only the plain Java facade. Moving only `ScraperModule` to a new adapter artifact would not remove the dependency while `AttributeHolderScope`, `DefaultHarvesterFactory`, `ScrapingHarvester`, active executor construction and other Core source still compile against Guice.

Required migration boundary:
- Remove Guice types from the public and internal Core runtime contracts that are needed for Core-only load/inspect/validate/execute.
- Move Guice composition, scopes, module bindings and Guice-specific provider bridges into an optional adapter artifact, with an explicit dependency on Core.
- Ensure Core's POM has no compile/runtime Guice dependency; a test-scoped Guice dependency is acceptable only where legacy tests genuinely require it and does not leak to consumers.
- Verify the resolved consumer dependency tree, not only source imports: a minimal plain-Java fixture must compile and run with Guice absent from its classpath. A separate fixture should use a host-owned Guice injector through the adapter.
- Do not create a new parent POM or flatten the current reactor as a shortcut. The artifact split must be designed and verified against the current reactor/publication model after the SPI decision.

Evidence on current `main`:
- `webharvest-core/pom.xml` blob `d6dd45ba5ee481b30fce3f07d4ce08a1b1ea45ed`: Guice dependency has no `<scope>` element.
- Root `pom.xml` blob `dc944dc1a15a2c4eedecd270af2016a9ec03c0d4`: dependencyManagement pins Guice 5.1.0.
- This is a POM/source inspection only; no dependency tree was resolved and no consumer fixture was built in this audit.
## 7.12 Complete direct nested-executor construction call-site set

A follow-up source search included fully-qualified construction expressions and found a further direct path missed by the abbreviated list in 7.8. The mapped Core source contains these direct constructions:

- `CallPlugin`: function-body dispatch.
- `IfPlugin`: selected conditional branch dispatch (fully-qualified `DefaultPluginBodyExecutor`).
- `CasePlugin`: selected case/fallback branch dispatch.
- `IncludePlugin`: included configuration children.
- `LoopPlugin`: loop-body dispatch.
- `RegexpPlugin`: two result/template dispatch paths.

`TryPlugin` uses `AbstractPlugin.executeBody()` at the inspected call site and does not construct a separate executor there. The seven direct constructions across six plugin classes must all receive the same runtime composition context, provider factory, pinned registry generation, policy, cancellation/deadline and shared budget. This is a source-text call-site inventory, not bytecode/dataflow proof against indirect factories.

Correction: earlier sections that listed only Call/If/Regexp or Call/Include/Loop/Regexp were incomplete. Use this six-class/seven-construction list as the current direct call-site inventory.
## 7.13 ScrapingScope is a process-wide shared instance, not runtime-owned

A focused source check of `ScraperScope` and `AttributeHolderScope` makes the multi-runtime limitation more precise:

- `ScraperModule` declares `private static final ScraperScope SCRAPER_SCOPE = new ScraperScope()`.
- Every `ScraperModule` binds its `ScrapingScope` to that same static `SCRAPER_SCOPE` instance and binds `ScraperScope.class` to the same instance.
- `ScraperScope` extends `AttributeHolderScope<AttributeHolder>`; the latter stores its scope stack in an instance `ThreadLocal<Stack<AHT>>`.
- `ScrapingHarvester.execute()` enters this shared scope before creating/using the session and exits it in `finally`.

Thus separate Guice injectors do not receive separate scope objects: the scope instance and its ThreadLocal are process-wide. The ThreadLocal stack provides same-thread nesting, but it does not encode runtime identity or establish independently owned per-runtime scope state. It must not be presented as evidence of multi-runtime isolation. In particular, two independently configured WebHarvest runtimes on one thread share the same scope stack object, and the static injector/script-engine seams remain separate sources of cross-runtime coupling.

Required target: the runtime composition root owns its own execution/session scope implementation. A host/runtime identity must be explicit in the provider context; nested executions must inherit the correct runtime context without relying on a process-wide ThreadLocal stack. Conformance must exercise two runtime instances on the same thread with nested and sequential executions, plus concurrent execution on separate threads, and prove that closing or failing one runtime cannot expose or dispose another runtime's scoped services.

Source blobs on current `main`: `ScraperModule.java` `d31a5e30b0949c752743123cbcfb4219ba60713e`; `ScraperScope.java` `877b943c38994566cc0128629d26de6a3618bcd9`; `AttributeHolderScope.java` `11a39c99e517ae9ae1ba09cf69a885b636465b64`; `ScrapingHarvester.java` `2af32ca67c235e075c8cc613118814d54e555338`.
## 7.14 Current Plugin cleanup contract conflates per-execution cleanup and unload

A direct lifecycle trace of `Plugin`, `AbstractPlugin` and `DefaultPluginBodyExecutor` found a mismatch that the dynamic provider transaction must resolve:

- `Plugin.cleanup()` is documented as cleaning resources when a plugin is unloaded.
- `AbstractPlugin.execute()` invokes protected `doCleanup()` from its `finally` block after every execution, including execution failure/interruption paths. It does not clear `initialized` or the stored configuration afterward.
- `AbstractPlugin.cleanup()` separately invokes `doCleanup()` again when `initialized` is true, then clears `initialized` and configuration. Thus calling the public cleanup after a normal execution can invoke the same cleanup hook a second time; idempotence is not enforced by the base class.
- `AbstractPlugin.initialize()` stores configuration and calls `doInitialize()`. If initialization throws, it wraps/rethrows the failure but does not invoke `doCleanup()` as rollback and does not clear the stored configuration. `initialized` remains false.
- `DefaultPluginBodyExecutor.createChildPlugin()` creates and initializes the instance. `executeChildPlugin()` then calls `childPlugin.execute(context)` and returns the result; it does not call public `childPlugin.cleanup()` in a `finally` block. The conditional selected-branch paths likewise create and execute child plugins.

Consequences:

1. Per-execution resource release (`doCleanup()` in `execute`) is not the same lifecycle event as provider/plugin unload (`cleanup()`). A dynamic plugin may own classloader/provider/generation resources that must outlive one execution and be closed only when its owning generation/runtime is retired.
2. Initialization that allocates a resource and then fails has no guaranteed rollback hook in the current base class or executor construction path.
3. The public cleanup hook can run after per-execution cleanup without base-class exactly-once protection. Plugin implementations must currently self-manage idempotence if both paths are used.
4. The executor does not demonstrate a deterministic plugin-instance disposal path after create/execute. This must be defined before dynamic activation/reconciliation can claim safe rollback or unload.

Required target: distinguish construction, initialize/prepare, per-execution completion, rollback-after-failed-prepare, and final close/unload. Make cleanup ownership explicit (provider instance vs execution instance vs generation/runtime), guarantee exactly-once disposal, and preserve the ability for generation-scoped providers to serve multiple sessions without closing on each execution. A failed candidate preparation must close all resources it allocated and preserve the last-known-good generation.

Required tests: initialization fails after allocating a test resource → rollback closes once; execute succeeds/fails/is interrupted → per-execution cleanup runs once; provider/generation remains usable across multiple executions; generation retirement closes plugin/provider resources once after leases drain; repeated close is harmless; one runtime's close does not dispose another runtime's services.

Source blobs on current `main`: `Plugin.java` `29772a9b37c8fead735f4a5e0bd47e5b90b4367e`; `AbstractPlugin.java` `be3298304840e4a585b4fb148c2e62cf471ee8f7`; `DefaultPluginBodyExecutor.java` `0116211ed31986b757f6f9e4bb5689f199b54f69`.
## 7.15 Existing lifecycle tests do not close the unload/rollback gap

Test-source review confirms that existing lifecycle tests intentionally protect per-execution cleanup and interruption ordering: `AbstractPluginExceptionBug5Test` checks cleanup after success, failure and interruption. `AbstractPluginLifecycleBranchContractTest` checks initialized-state reset and repeated public cleanup without a preceding execute. `DefaultPluginBodyExecutorTest` exercises child execution/interruption but the inspected fixture does not assert public `Plugin.cleanup()` after child creation/execution.

The inspected tests do not cover partial initialization failure after resource allocation, execute-then-public-cleanup double invocation on the same initialized instance, provider reuse across sessions, generation lease draining or exactly-once disposal on generation retirement. Test sources were inspected, not run.

New conformance tests must retain the existing per-execution cleanup and interrupt-order guarantees while separately proving failed-prepare rollback and final provider/generation close.


## 7.16 Shared AbstractPlugin body delegation also creates a detached executor

A complete read of `AbstractPlugin.executeBody()` identifies an additional runtime construction path beyond the seven direct constructions in six mapped plugin classes listed in 7.12:

- When a plugin configuration has children, `AbstractPlugin.executeBody()` constructs `new DefaultPluginBodyExecutor()` and delegates the children to it.
- `TryPlugin` uses this shared base-class body path. It does not contain its own constructor expression, but its actual body execution still reaches a detached default executor.
- Consequently, the seven construction expressions in 7.12 are not the complete runtime inventory. The shared base-class path is an additional construction site with potentially broad reach across plugins that delegate through `executeBody()`.
- The migration must replace both explicit nested-dispatch construction and this base-class delegation with a runtime-owned executor/provider context. All paths must inherit the same pinned generation, host policy, cancellation/deadline and shared budget; no default constructor may silently drop that context.
- The out-of-tree conformance fixture must cover explicit nested dispatch (Call/If/Case/Include/Loop/Regexp) and base-class body delegation (including Try), asserting that provider identity, generation and policy context remain unchanged through every nested layer.

This is source inspection only; tests were not run. Source blob: `AbstractPlugin.java` `be3298304840e4a585b4fb148c2e62cf471ee8f7`.


## 7.17 Resource ownership must be explicit across invocation, session, generation, runtime and host scopes

A targeted source audit of resource-bearing built-ins shows distinct owners that must not be collapsed into a generic plugin close() call:

- **Invocation scope:** JDBC ResultSet/Statement and FTP client connections are created and closed inside the operation's doExecute() / finally paths. These must be bounded by each invocation and must not wait for registry-generation retirement.
- **Session scope:** named JDBC connection pools are registered with the session EventBus and stored in the session execution context. The no-EventBus compatibility path closes the named pool directly. Session completion/failure/cancellation owns disposal; ending one session must not dispose another session's resources.
- **Browser state:** WebBrowserPlugin holds static process-level maps, with entries keyed by session ID and a legacy weak-keyed context map for sessionless execution. Per-session state registers with the session EventBus and closes pages/unregisters on completed, failed or cancelled events. closeAllPages() is a separate global shutdown operation. The storage is process-global even though the intended entry lifetime is session-scoped; runtime identity and two-runtime isolation remain unproven.
- **Runtime scope:** the Guice-bound HttpServiceImpl is shared per injector and implements AutoCloseable. Its underlying HTTP client must survive individual plugin/session completion when shared; an explicit runtime owner must stop admissions/drain sessions before closing it exactly once.
- **Generation scope:** only provider instances and resources explicitly owned by that generation may be retired after all generation leases drain. A generation must not close a host-owned service or a runtime-shared transport pool merely because a provider used it.
- **Host scope:** services supplied by an embedding application remain host-owned unless the host explicitly transfers ownership. Runtime shutdown must not close them by default.

A targeted scan did not find doCleanup() overrides in the resource-bearing built-in implementations inspected; this is not a repository-wide absence claim. Several built-ins release their operation resources in doExecute() finally blocks, independently of AbstractPlugin.doCleanup(). Therefore, lifecycle contracts must separately define per-invocation completion, session termination, provider prepare/rollback/final close, generation retirement and runtime shutdown. The conformance suite must assert that closing one scope cannot prematurely close resources owned by a wider scope, and that all resources are eventually released by their actual owner. Source inspection only; no tests run.


## 7.18 The legacy PluginFactory/PluginRegistry path must not become a second lifecycle authority

The repository contains a parallel legacy factory/registry API distinct from the active CorePluginRegistry + DefaultPluginBodyExecutor path:

- DefaultPluginRegistry.registerPlugin(Class) reflectively constructs and retains a Plugin instance in its registry maps; lookups and metadata are based on that instance.
- unregisterPlugin removes registry entries but does not call Plugin.cleanup() on the retained object.
- DefaultPluginFactory uses the retained object to obtain its class, then reflectively constructs a separate instance (no-arg preferred, PluginMetadata constructor fallback) and optionally initializes it. It does not return an explicit disposal handle or define cleanup on failed initialization or caller completion.
- The parallel DefaultPluginDependencyInjector injects annotated fields reflectively and maintains singleton/provider maps, but prior source audit established that this injector is not wired into the active body executor. It is not a substitute for the runtime-neutral host provider contract.

This is not evidence that this factory is the active CLI execution path. The dynamic platform must make an explicit compatibility decision: retire this API, or adapt it as a facade over the same descriptor-only authoritative registry and provider/lifecycle model. It must not retain a second source of plugin identity, construction semantics or disposal ownership. Conformance should cover registry registration/unregistration, retained prototype disposal, factory-created instance ownership, failed preparation rollback and API parity. Source inspection only; tests not run.


## 7.19 Browser state needs runtime-scoped ownership, not a process-wide shutdown hook

The current `WebBrowserPlugin` implementation has useful session cleanup but does not yet provide a safe embedding/runtime ownership boundary:

- `SESSION_PAGES` and `CONTEXT_PAGES` are static process-wide maps. The session map is keyed by session ID alone, without runtime identity.
- Session-backed `BrowserSessionState` registers with the session EventBus and closes/unregisters on completed, failed and cancelled events. A terminal-state check after lookup also closes a state if terminal transition races with registration. Preserve these protections.
- `closeAllPages()` closes every browser state in the process. It cannot be used as an individual WebHarvest runtime's close operation when multiple runtimes share one JVM.
- The legacy sessionless-context map has weak keys but no explicit context-disposal event in the inspected class. Weak-key collection does not itself guarantee `WebClient.close()` is invoked.

Required target: browser resources are owned by a runtime-scoped service and keyed by runtime + session identity; session completion closes only that session; runtime close closes only that runtime's remaining sessions after admission/drain; host-wide shutdown is a separate explicit operation. Keep the terminal-race/idempotent-close guarantees. Conformance must prove runtime A close leaves runtime B's browser pages usable, and that sessionless compatibility either has deterministic disposal or is explicitly deprecated/restricted. Source inspection only; no tests run. Source blob: `WebBrowserPlugin.java` `be7675390275b16ed63e5515a11969585acd2de6`.


## 7.20 Session lifecycle must not silently vanish when an IoC provider is unavailable

Current `ScrapingHarvester.execute()` obtains `EventBus` through an injected Guice `Provider`. If the provider is null or `.get()` throws, the implementation logs a warning and continues without an EventBus. A `ScraperSession` is created and attached to the context only when EventBus exists. This means no session completed/failed/cancelled lifecycle events are available in the fallback path; browser code then uses its legacy sessionless static weak-key map.

The existing `finally` correctly exits the scraping scope, and session-backed execution emits terminal state before scope exit. Preserve that ordering, but do not silently downgrade lifecycle guarantees because an optional container binding is absent.

Target contract: the neutral runtime always owns a lifecycle/session coordinator. A host IoC adapter may supply it, but if a required binding is unavailable the runtime must fail clearly rather than proceed with weaker cleanup semantics. If a deliberately reduced no-session mode is retained for compatibility, it must be explicit, bounded and tested with deterministic resource disposal. Conformance cases: no EventBus binding, provider lookup failure, normal completion, plugin failure, interruption/cancellation, session terminal cleanup and no leaked browser/JDBC/session resources.

Source inspection only; no tests run. Source blob: `ScrapingHarvester.java` `2af32ca67c235e075c8cc613118814d54e555338`.

## 7.21 Guice type-listener registrations and AOP interception are not runtime-owned

The multi-runtime hazard extends beyond the shared scope and static service fields. The current Guice extension points also resolve their owning services through process-global state and do not define reverse lifecycle operations:

- `ScrapingInterceptor.invoke()` calls `InjectorHelper.getInjector()`, then resolves `ScraperScope` and `ScrapingAwareHelper` from that injector. An AOP-intercepted Harvester belonging to runtime A can therefore enter the scope and notify listeners obtained from runtime B after B's injector has most recently performed static injection.
- `EventBusTypeListener.afterInjection()` also calls `InjectorHelper.getInjector()` and registers the injected object with that injector's `EventBus`. A subscriber created by A can consequently be registered on B's EventBus. The listener does not capture the injector/EventBus that owns its Guice binding and has no corresponding unregister/disposal callback.
- `ScrapingAwareTypeListener` obtains a provider for `ScrapingAwareHelper` from its `TypeEncounter`, but the helper stores listeners in a mutable `ArrayList`, documents that registration is not thread-safe, and has no remove operation. The source explicitly records the missing reverse process. Runtime disposal cannot currently detach these listener references through a supported lifecycle API.

This is a separate lifecycle defect from merely sharing a static scope instance: even if scope objects and script engines become runtime-owned, the current interceptor and type-listener callbacks can still route events or retain objects across runtime boundaries.

Required target:

1. Interceptors and injection/type-listener callbacks must capture or receive the owning runtime's explicit service context; no callback may resolve an injector, EventBus, scope or listener collection through a process-static service locator.
2. Subscriber and lifecycle-listener registration must return an ownership/disposal handle (or equivalent deterministic unregister operation). The handle belongs to the runtime/container that created the subscriber.
3. Runtime close must stop new registrations, detach only that runtime's subscribers/listeners, and leave other runtimes' EventBus instances and callbacks usable. Registration/removal must be safe under concurrent injection and shutdown.
4. A host-owned EventBus must remain host-owned; WebHarvest may register/unregister its subscribers but must not close the host's bus.

Required conformance tests (source inspection only; not run):

- Construct runtimes A and B, then create an A subscriber after B is initialized; publish an event on each bus and prove the subscriber receives only A's event.
- Invoke a valid AOP-intercepted Harvester for A after B has been initialized; prove its scope and before/after listeners are resolved from A, not B.
- Close A while B remains active; prove A subscribers/listeners are detached and B continues receiving events and executing harvesters.
- Race subscriber/listener creation against runtime close; prove no post-close registration leak, cross-runtime registration or concurrent collection failure.
- Repeat on reused executor threads to ensure teardown does not depend on thread-local cleanup alone.

Source evidence on the audited `main` baseline:

- `ScrapingInterceptor.java`: blob `8de38b972d5c6eb41cc00e9c38ebe0f584e8c9e2` — static injector lookup for AOP scope/helper resolution; helper's mutable listener collection and missing removal path.
- `ScrapingAwareTypeListener.java`: blob `4ed97073d64878be10cd4798c47b56ecd169f1cf` — post-injection listener registration with no reverse callback.
- `EventBusTypeListener.java`: blob `3b368eb75ce842f3db6a67b1e9424f644b341824` — static injector lookup and EventBus registration without disposal.
- Focused test-source blobs: `ScrapingInterceptorTest.java` `3eb90ecee27a679f956f22fcde4c8b0cc99ab4ff`; `ScrapingAwareTypeListenerTest.java` `6d854e1107fc16d42b31f1207021e3d6e59faaae`; `EventBusTypeListenerTest.java` `9eb438cdd6cc8cd44b2636f777f661d0a3ea4bde`. Their current assertions do not prove the valid intercepted path, which EventBus receives a subscriber, or listener removal at runtime close.

The companion `MULTI_RUNTIME_ISOLATION_AUDIT.md` records the source-level call paths and the focused test-coverage observations in greater detail. This section adds the normative ownership and acceptance requirements to the target architecture; it does not claim that the current runtime satisfies them.


## 7.22 Session cancellation is not worker termination (GRACE/KILL acceptance)

The current asynchronous service/session implementation does not provide a reliable termination protocol. The following findings are based on source inspection of the current `main` baseline; they are not runtime-test results.

### Current source behavior

- `WebHarvestServiceImpl.executeAsync()` submits a lambda with `executorService.submit(...)` but discards the returned `Future`. The service therefore has no per-session execution handle to cancel, interrupt, await or inspect.
- `ScraperSessionImpl.cancel()` changes the public status to `CANCELLED`, posts `SessionCancelledEvent` and signals waiters. It does not interrupt the executing thread, cancel a task or wait for plugin/descendant work to quiesce. The public terminal state can therefore precede actual execution termination.
- The worker continues after cancellation unless some independent code stops it. Its later `markCompleted()` requires status `RUNNING`; after the session has been marked `CANCELLED`, that transition fails. The status machine therefore conflates a cancellation request with execution completion and can report a terminal state while work is still active.
- `executeWithTimeout()` creates a new single-thread executor for each timed session, submits the actual scraper work to it, calls `future.cancel(true)` only on timeout, and never shuts that executor down. The future is local to this method and is not connected to `cancelSession()` or service shutdown. The outer session worker can finish while the inner scraper task remains unresponsive.
- `shutdown()` calls `executorService.shutdown()` and waits for 60 seconds, but ignores the boolean returned by `awaitTermination`. It can log shutdown complete without proving worker termination. It also does not escalate or report still-running sessions.
- `shutdownNow()` first calls `session.cancel()` on registered sessions and then calls `executorService.shutdownNow()`. Because the submitted task handles are not retained, the service cannot correlate interruption/queued-task removal with each session or await per-session quiescence. Work delegated to the separate timeout executor is outside this executor's shutdown.
- Existing `ScraperSessionTest` cancellation coverage asserts only status transition. `WebHarvestServiceTest` uses timing/sleep assertions and does not prove that the worker thread, nested executor, plugin operation or resource leases have exited. These tests do not establish GRACE or KILL semantics.

### Required two-mode termination contract

The platform must separate **termination request**, **session outcome**, and **execution quiescence**. A session must not be reported as safely terminated merely because a cancellation flag or public status changed.

**GRACE (cooperative termination):**

1. Atomically stop admission of new work for the target session and its descendants.
2. Propagate one cancellation signal and the session's monotonic deadline through nested dispatch, plugin calls and host services.
3. Retain and signal the actual task/worker handles; interrupt only as a cooperative wake-up mechanism where the operation contract permits it.
4. Allow bounded cleanup and resource release, while preventing new external side effects from starting after cancellation admission closes.
5. Await worker, child-operation and resource-lease quiescence within the configured grace period.
6. Report `TERMINATED_GRACEFULLY` only after quiescence is proven. If the deadline expires, report an explicit unresponsive/escalation state rather than a false terminal success.

**KILL (forced escalation):**

1. Escalate only after GRACE expires or an explicit host policy requests immediate escalation.
2. For in-process Java code, use cooperative abort hooks and thread interruption as best-effort escalation; never use `Thread.stop()` and never claim that interruption forcibly terminates arbitrary plugin code.
3. If in-process work does not quiesce, mark it `UNRESPONSIVE`/`QUARANTINED`, deny it new work, retain ownership of resources that may still be in use, and surface that the JVM worker is still alive.
4. A hard-kill guarantee for arbitrary/non-cooperative providers requires process isolation. The host may report `KILLED` only after the worker process has exited and its owned resources have been reconciled.
5. Preserve the distinction between execution termination and external-effect disposition. Do not infer rollback, exactly-once behavior or retry safety from a worker exit or interrupt.

### Required implementation and conformance gates

- One authoritative task handle per session, covering the actual execution (including timeout/delegated execution); no detached per-session executor.
- Atomic admission/termination state machine that handles cancellation-before-start, cancellation-during-start, concurrent completion, repeated cancel, timeout-vs-cancel and shutdown races without illegal terminal transitions.
- Separate observable fields/states for requested termination mode, session/business outcome, worker liveness/quiescence, cleanup outcome and unresolved external effects.
- Bounded GRACE duration and separately bounded cleanup budget, both based on monotonic time; no unbounded joins or hidden executor threads.
- Service shutdown must stop admission, request GRACE for all active sessions, await actual quiescence, escalate according to policy, and return/report incomplete shutdown if workers remain alive. It must not log success solely because a wait method returned.
- Tests must use latches/barriers and observable worker/resource handles rather than sleeps: cancellation before task start; cancellation while blocked in an interruptible operation; non-cooperative plugin; nested child operation; timeout and explicit cancel racing; normal completion racing cancel; service close with active sessions; interrupted caller; cleanup failure; worker still alive after KILL request; process-isolated worker exit.
- Compatibility tests must prove that legacy plugins are not silently advertised as hard-killable or safely retryable. Legacy in-process execution defaults to cooperative/best-effort cancellation with unknown quiescence until verified.

### Source evidence (main baseline)

- `WebHarvestServiceImpl.java`: blob `10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec` — discarded submitted Future, detached timeout executor, timeout-only inner Future cancellation, ignored `awaitTermination` result, and service shutdown paths.
- `ScraperSessionImpl.java`: blob `78d07d1c485ea215d68d6d415f36b2be0c180a4f` — `cancel()` changes status/posts event/signals completion without owning or interrupting a worker; terminal transition is not worker-quiescence proof.
- `ScraperSession.java`: blob `c8d0086ea160e2d8f02657c4cc9aa9d11d65b237` — public contract describes cancellation as best-effort/cooperative but does not expose termination mode, task handle, worker liveness or quiescence.
- `SessionOptions.java`: blob `6afb9740dbab866cbf2ce2487735bc5d78bb1ea8` — one execution timeout option; no grace period, escalation policy or cleanup budget.
- `ScraperSessionTest.java`: blob `31988741de660cb7455851e98ff75a706ea7d3dc` — cancellation tests assert state only; no worker/child/resource termination proof.
- `WebHarvestServiceTest.java`: blob `024d1aeb30dc1ec6a97cfd0d1afa847c0eccf07c` — service tests do not assert actual worker quiescence or detached timeout-executor cleanup.

Source inspection only. No runtime implementation, POM change or test execution is included in this documentation checkpoint.


## 7.23 Interruption propagation and blocking-operation audit

This section records a source-level audit of representative Core and optional providers on `main`. It distinguishes (a) interruption being swallowed or converted into ordinary failure, (b) blocking I/O having no operation-scoped abort handle, and (c) operations whose cancellation behavior has not been demonstrated. These are source findings, not measured runtime behavior.

### Confirmed interruption-swallowing paths

- **FilePlugin** (`8d78e797975ab236a47420d18f28600beaae04bf`): `doExecute()` declares `InterruptedException` but its broad `catch (Exception)` converts it to `EmptyVariable`. `handleFileWrite()` also catches `Exception` around `super.executeBody(context)`, suppressing interruption from child plugins and continuing into file-write logic. Template processing has another broad catch that treats failure as a warning and continues. The read/write/list helpers also use synchronous file APIs without a cancellation/abort abstraction.
- **XQueryPlugin** (`b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`): XQuery evaluation catches `Exception` and wraps it as `ScraperXQueryException`; child-element execution catches `Exception` and returns `EmptyVariable`. Because `InterruptedException` is an `Exception`, both paths can convert interruption into ordinary plugin failure/fallback instead of preserving cancellation.
- **RegexpPlugin** (`a860ab08554122680c4d5d5fde1be3d683139c6a`): the main execution catch converts any `Exception` to an empty `ListVariable`; result-template processing catches any `Exception` and returns the original template. The main method declares `InterruptedException`, but these broad catches can suppress it in nested execution.
- **WebBrowserPlugin** (`be7675390275b16ed63e5515a11969585acd2de6`): the top-level `doExecute()` catches `Exception` from `executeAction()` and wraps it in `RuntimeException`, including an `InterruptedException` from the execute-script path. `handleExecuteScript()` itself catches `Exception` around browser execution and JavaScript waiting, also wrapping interruption without restoring the interrupt flag. `handleLoad()` does restore the flag when its explicit `Thread.sleep()` is interrupted, but wraps it as `PluginException`. This is inconsistent propagation across actions.

### Blocking operations without demonstrated cancellation handles

- **HTTP service** (`9e3dab1a40250f4d059ea812c74012d994873f0d`): `executeRequest()` calls the shared Apache client synchronously and consumes the response entity before returning. The inspected method exposes no request-scoped cancellation handle or link to a session deadline. IOException is wrapped as `HttpServiceException`; source inspection alone does not prove whether every underlying socket wait is interruptible.
- **FTP Get** (`fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`): connect/login/retrieveFile are synchronous Commons Net calls. The client is disconnected in `finally`, but the executing thread has no externally retained operation handle to abort a blocked transfer. The broad catch wraps ordinary exceptions; interruption behavior of the library call must be established with a deterministic test and explicit socket/data-connection timeouts.
- **FTP Put** (`34061eb4fbc3a70f95d8d90a3a2faef611659f82`): same structural limitation for upload. Disconnect in `finally` is cleanup after control returns; it is not a demonstrated external abort mechanism while `storeFile` is blocked.
- **Mail** (`083297481e7e218916a77f0dea2308c63bbac7e7`): `email.send()` is synchronous and has no session-scoped abort handle in the inspected plugin. A broad `catch (Exception)` wraps failure. SMTP timeout/connection behavior and commit ambiguity need explicit tests; after message acceptance by the server, cancellation cannot imply message rollback.
- **WebBrowser**: HtmlUnit page loading and JavaScript background waits are synchronous from the plugin's perspective. The inspected code has a configured JavaScript wait and `Thread.sleep`, but no session deadline/cancellation token passed to the WebClient or a registered close/abort callback for the active page operation.

### Positive propagation examples, with limits

- **IncludePlugin** (`ab06127f7f5402978937160a42455b4c0107dfab`): explicitly catches `InterruptedException`, restores the interrupt flag and rethrows it before its general exception wrapper.
- **XPathPlugin** (`e519eb21a2e6faeef43a9d23c219502ac7f7f2a6`): body execution explicitly rethrows `InterruptedException` before its broad exception handling.
- **TryPlugin** (`f1363d5e880f424276deeabac1082750fe61a012`): delegates child execution through `executeBody()` and declares `InterruptedException`; this is a propagation path, not proof that all descendants and blocking libraries are cancellable.
- **CasePlugin** (`1b12791f6539d84209a72044bfabee23f096d6bd`): selected branch execution is delegated and cleanup failures are attached as suppressed exceptions. Cancellation behavior still depends on branch/plugin execution and cleanup contracts.

### Required remediation and conformance rules

1. At every broad catch boundary, catch/rethrow `InterruptedException` before `Exception`; restore the interrupt flag when translating interruption into an API that cannot declare it. Never return a successful/empty fallback after cancellation.
2. Define one shared cancellation/deadline context for descendants and host services. Thread interruption is a wake-up hint, not the authoritative lifecycle state.
3. HTTP, FTP, SMTP, browser and filesystem adapters need explicit operation ownership, finite connect/response/read/write deadlines, and host-visible abort/close handles where supported.
4. Preserve side-effect uncertainty. FTP upload, file writes, HTTP mutations and SMTP send may have committed before cancellation is observed; report `COMMIT_UNKNOWN` where the protocol cannot establish the outcome and prohibit blind retry.
5. Keep GRACE cleanup bounded and separate from operation deadlines. Cleanup must close owned resources without allowing a cancelled plugin to start new child work.
6. Add deterministic tests using latches and controlled fake servers: interrupt during each broad catch; cancel during child-body execution; cancel during HTTP response-body read; FTP data transfer; SMTP send/acceptance; HtmlUnit load and JavaScript wait; file write before/after open; assert interrupt propagation, abort invocation, worker quiescence and effect disposition.
7. Add static regression checks or focused unit tests that prevent new `catch (Exception)` blocks from swallowing `InterruptedException` in methods whose call graph can execute nested plugins or blocking operations.

### Exact inspected source blobs

- `FilePlugin.java`: `8d78e797975ab236a47420d18f28600beaae04bf`
- `XQueryPlugin.java`: `b1f0ee00c16c9a37144e7e27cb46bfd57de6e9f1`
- `RegexpPlugin.java`: `a860ab08554122680c4d5d5fde1be3d683139c6a`
- `IncludePlugin.java`: `ab06127f7f5402978937160a42455b4c0107dfab`
- `XPathPlugin.java`: `e519eb21a2e6faeef43a9d23c219502ac7f7f2a6`
- `CasePlugin.java`: `1b12791f6539d84209a72044bfabee23f096d6bd`
- `TryPlugin.java`: `f1363d5e880f424276deeabac1082750fe61a012`
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`
- `FtpGetPlugin.java`: `fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`
- `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`
- `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`

Source inspection only. No code or test changes are included in this checkpoint.

## 7.24 Session resource ownership, cleanup scope and cross-session isolation

This section audits the ownership and cleanup of representative session resources. The key distinction is between a resource being associated with a session by convention and the runtime actually owning a resource lease that can be drained safely. Source inspection found a concrete cross-session cleanup defect in the current database path.

### Confirmed cross-session JDBC cleanup defect

- One service-wide event bus is shared by all sessions (WebHarvestServiceImpl.java, blob 10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec). The service constructs each ScraperSessionImpl with the same eventBus, then injects that same instance into every DynamicScopeContext.
- Both inline database operations and named connection declarations register independent pools on that shared bus (DatabasePlugin.java, blob b3934d753f17f133bd582b50206db701af843728; ConnectionPlugin.java, blob edb6ed93584cb03d967c830d120848c7e891f481; StandaloneConnectionPool.java, blob ab4a92fb63599e88a55821fe141bf5aa50327008). Each creates a StandaloneConnectionPool and registers it with context.getEventBus(), then expects terminal session events to release its physical connections.
- The pool's terminal-event handlers do not check event session identity. onExecutionEnd(SessionCompletedEvent), onExecutionError(SessionFailedEvent) and onExecutionCancelled(SessionCancelledEvent) all call closeAllConnections() unconditionally. Since the bus is shared, a completion, failure or cancellation event from session A can close the JDBC connections currently owned by an active operation in session B. The pool then unregisters itself from the shared bus. This is a source-proven cross-session isolation defect; no runtime reproduction was performed in this documentation audit.
- The same event-bus topology means event subscription alone is not a sufficient ownership boundary. Every listener must validate the event's session/generation identity before changing session-owned resources. The bus remains shared.

### Resource ownership patterns and limits

- Browser state (WebBrowserPlugin.java, blob be7675390275b16ed63e5515a11969585acd2de6) uses a static map keyed by session ID and its BrowserSessionState filters completion/failure/cancellation events by that ID. It closes its WebClients and removes the map entry on terminal events. This is a better ownership key than the JDBC listener, but the close happens on the terminal event itself: there is no operation lease/drain protocol proving that an in-flight page load or script has quiesced before the client is closed. Session state is therefore associated and cleaned up, but concurrent-operation safety is not established.
- Session registry (SessionRegistryImpl.java, blob 8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124) tracks session objects only. It has no resource registration, lease count, close ordering, or quiescence contract. Unregistering/clearing a session is not resource cleanup.
- Plugin lifecycle cleanup (AbstractPlugin.java, blob be3298304840e4a585b4fb148c2e62cf471ee8f7) invokes per-plugin doCleanup() in a finally path, but suppresses cleanup exceptions to preserve the execution error. That is useful local hygiene, not an authoritative session resource ledger; it cannot prove that resources created by nested/async work have drained.
- HTTP service (HttpServiceImpl.java, blob 9e3dab1a40250f4d059ea812c74012d994873f0d) owns an AutoCloseable client, but the inspected request path does not register an operation-scoped abort/lease with the session. Its close method is a service/client lifecycle operation, not proof that an individual session can safely abort only its own in-flight request.

### Shared EventBus is intentional — subscriber state must be thread-safe

The shared EventBus is an architectural invariant, not the defect to remove. The runtime should retain one service/runtime-scoped EventBus so sessions, plugins and infrastructure can exchange typed events without creating isolated event topologies. Do not solve resource isolation by creating one EventBus per session.

The defect is in subscriber ownership and mutable subscriber state:

- DynamicScopeContext explicitly documents that EventBus.post() is thread-safe and that the bus reference is initialized before the context is shared. WebHarvestServiceImpl deliberately injects the same EventBus into every session and Scraper.
- A thread-safe event publication mechanism does not make each subscriber's fields, registries, JDBC connections or cleanup protocol thread-safe. Concurrent sessions can post events concurrently; subscriber callbacks must therefore tolerate concurrent invocation and must not mutate shared state without a defined synchronization/ownership strategy.
- StandaloneConnectionPool explicitly documents that it is not thread-safe. Its current event handlers also close the pool on every session terminal event without checking the event's session identity. This is both an ownership/isolation bug and a concurrency-safety gap; the fix is not a private EventBus.
- Keep the shared bus as the transport. Bind each session-owned resource registration to an immutable session ID and execution generation. A subscriber may receive all events, but it must act only on matching ownership identity. Alternatively, a runtime-level resource coordinator can subscribe once and route events to the matching session resource owner.
- Subscriber registration/unregistration must itself be lifecycle-safe. Avoid registering mutable resource objects in ways that permit a callback to race with close/unregister and operate on partially retired state. Define callback serialization or make callbacks idempotent and concurrency-safe.
- Event ordering must not be assumed across independent posting threads. If a resource needs ordered transitions, serialize them per session/resource or enforce them through an atomic state machine; a globally serialized event bus is not required and could unnecessarily serialize unrelated sessions.

Thus the target is: **one shared, thread-safe event transport; concurrent-safe subscribers; immutable event/session identity; session-scoped resource ownership; per-resource synchronization/leases.** A session-A terminal event must never close session-B resources, while both sessions continue to publish and consume events on the same bus.

### Required ownership contract

1. Introduce an authoritative session-scoped resource registry/lease owner. Registration must bind a resource to an immutable session identity and execution generation; ownership cannot be inferred from a shared event bus.
2. Make terminal cleanup idempotent and identity-checked. A resource registered for session A must never be closed by session B's completion, failure, cancellation or timeout.
3. Define resource states such as registered, active/in-use, close-requested, draining, closed and close-failed. A close request must prevent new leases, signal supported abort handles, and await in-flight leases within the GRACE cleanup budget.
4. Separate resource close initiation from proof of operation quiescence. Closing a JDBC connection or HtmlUnit client while a statement/page operation is still active is not, by itself, proof that the worker has stopped using it.
5. Make resource registration race-safe with session termination: if termination wins, registration must fail or immediately close the candidate; if registration wins, termination must see and drain it. No resource may be published after the final cleanup snapshot. Keep the EventBus shared; enforce this race safety in the resource owner/registry, not by creating per-session buses.
6. Scope application-wide clients/pools separately from session-owned handles. A session must close only its lease/borrowed handle; it must not close a shared application client used by other sessions.
7. Preserve close failures and unresolved use-after-close states in the session termination report. Do not silently treat a failed resource close as graceful quiescence.
8. Add deterministic multi-session tests on the same shared EventBus: two concurrent sessions with separate inline/named JDBC pools; complete/fail/cancel session A while B is blocked in a statement and assert B remains usable; cancel B and assert only B's resource closes; concurrently post terminal events and verify subscriber thread safety; race registration against cancellation; close browser/client while an operation lease is held; verify cleanup idempotency, lease drain and close-failure reporting.

### Exact inspected source blobs

- WebHarvestServiceImpl.java: 10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec
- DynamicScopeContext.java: f5ca038f25442f597986bf64ebc65e958b92a010
- ScraperSessionImpl.java: 78d07d1c485ea215d68d6d415f36b2be0c180a4f
- SessionRegistryImpl.java: 8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124
- DatabasePlugin.java: b3934d753f17f133bd582b50206db701af843728
- ConnectionPlugin.java: edb6ed93584cb03d967c830d120848c7e891f481
- StandaloneConnectionPool.java: ab4a92fb63599e88a55821fe141bf5aa50327008
- WebBrowserPlugin.java: be7675390275b16ed63e5515a11969585acd2de6
- AbstractPlugin.java: be3298304840e4a585b4fb148c2e62cf471ee8f7
- HttpServiceImpl.java: 9e3dab1a40250f4d059ea812c74012d994873f0d

Source inspection only. No runtime implementation, POM change, test execution or reproduction is included in this checkpoint.

## 7.25 Shared registry concurrency, mutation atomicity and classloader cache isolation

This section extends the shared-EventBus rule to mutable plugin-discovery and registry structures. A thread-safe transport does not make registries or their lifecycle operations thread-safe. The inspected implementation contains several concrete concurrency and isolation gaps.

### DefaultPluginRegistry: concurrent maps do not make compound registry operations atomic

DefaultPluginRegistry describes itself as providing thread-safe registration and discovery, and uses ConcurrentHashMap for each top-level index. That is insufficient for its compound operations:

- registerPlugin() performs containsKey(pluginId) followed by separate writes to the ID, metadata, QName, category and namespace indexes. Two concurrent registrations of the same ID can both pass the check; the last ID/metadata write wins while category/namespace lists can retain both plugin instances. Readers can observe partially published index state between those writes.
- Category and namespace values are mutable ArrayList instances created inside computeIfAbsent(). The map operation is atomic, but the subsequent add(plugin) is not. Concurrent registration, unregister, clear() or snapshot reads can race on those lists; copying a list in getPluginsByCategory() / getPluginsByNamespace() does not make the source list safe to traverse while it is mutated.
- Namespace+element registration uses unconditional put(), so two different plugin IDs with the same QName silently overwrite the QName index while both remain in the ID/category/namespace indexes. unregisterPlugin() later removes the QName key unconditionally; unregistering the older plugin can therefore erase the newer plugin's mapping.
- unregisterPlugin() removes the primary ID entry before updating the other indexes. Concurrent readers can observe an index set that no longer represents one coherent registry generation. clear() likewise clears indexes sequentially, not as one atomic publication.
- registerPlugin(Class) instantiates the plugin immediately before registration. That is executable provider construction during registration, not descriptor-only discovery or a side-effect-free staging phase.

The current DefaultPluginRegistryTest source exercises sequential registration, duplicate IDs, QName lookup and unregister behavior. The inspected test file contains no concurrent registration/unregistration, duplicate-QName collision, partial-publication, list-race or clear-vs-read test. This is a test-source observation; tests were not run.

### CorePluginRegistry: individually concurrent operations, globally shared mutable state

CorePluginRegistry is a process-wide static singleton backed by a ConcurrentHashMap. Individual map operations are concurrent-safe, but the registry is not runtime-scoped:

- every runtime/scanner using getInstance() mutates the same element-name map;
- registerMapping() silently overwrites an existing element-name mapping;
- clear() removes every runtime's mappings, not just the caller's;
- there is no immutable snapshot/generation publication, collision report or ownership token.

This is a multi-runtime isolation defect even though the backing map is concurrent. The registry's map-level thread safety must not be described as runtime isolation or atomic registry lifecycle.

### Discovery caches and scanner mutation

- PluginClassScanner.cachedClassNames is one process-static volatile cache. Its synchronized initialization prevents simultaneous cache construction, but the cache key contains no classloader, runtime, artifact set or discovery generation. A successful scan in the first classloader context is reused by later runtimes/classloaders. clearCache() is global and invalidates all callers at once; it does not create a runtime-specific cache.
- PluginPackageScanner preserves the exposing ClassLoader while enumerating candidate classes and uses Class.forName(name, false, loader), which is a positive property for class identity. However, registration is then written to the injected CorePluginRegistry; in the current default composition that is the process-global singleton. Concurrent scans can therefore race through a shared mutable target and overwrite element mappings. The scanner itself does not stage a complete immutable result or publish it atomically.
- The scanner's local enumeration map is per invocation; this does not make the target registry or the process-static discovery cache isolated. Do not infer that every scanner method is intrinsically unsafe merely because the target registry is global—the concrete defect is the shared mutation target and unkeyed static cache.

### Required target and conformance

1. Replace mutable multi-index updates with a single-writer transaction or immutable registry-generation build followed by one atomic snapshot swap. Readers must see either the complete old generation or the complete new generation.
2. Enforce plugin-ID and QName uniqueness before publication. A collision must fail with a deterministic diagnostic; unregister must remove only the exact mapping/owner it registered.
3. Do not expose mutable ArrayList values behind concurrent maps. Prefer immutable index values inside an immutable generation; do not rely on ConcurrentHashMap to protect nested mutable objects.
4. Scope Core registries and discovery caches to an explicit runtime/artifact/classloader generation. Remove process-global CorePluginRegistry ownership from runtime composition; clear() must affect only its owner.
5. Keep descriptor discovery side-effect-free. Provider construction must occur only after compatibility, enablement, trust and permission checks, not as an incidental registry registration step.
6. Add deterministic concurrency tests: same-ID simultaneous registration; different IDs with the same QName; register/unregister of colliding QName; concurrent category/namespace reads and writes; clear vs readers; two runtime registries with same element names; two classloaders with different plugin sets; cache refresh while an older generation remains in use.
7. Add an out-of-tree fixture proving that runtime A can update/retire its plugin generation while runtime B continues to resolve and execute its own generation, with no global cache clear or registry mutation crossing the boundary.

### Exact inspected source blobs

- DefaultPluginRegistry.java: da5c6f6e835cc8b551394e0853a70ca4bcd60d2f
- CorePluginRegistry.java: 20f478addbf2bacc481e4e11e0328f06942a2a38
- PluginPackageScanner.java: c0e6cbff6cb8c472f66c4a897263cb3c3c7f8232
- PluginClassScanner.java: dadd28a434000816253ac66bff736e2c5fff0494
- DefaultPluginRegistryTest.java: ee2dd3c932637c39bd25282505d06c940a286f64

Source inspection only. No runtime implementation, POM changes, test execution or runtime reproduction is included in this checkpoint.

## 7.26 Subscriber lifecycle, listener retention and lock-escaped registry views

This section audits the lifecycle of Guice-managed EventBus subscribers and ScrapingAware callbacks, plus the generic registry locking decorator. These are distinct mechanisms from the service-level shared EventBus in sections 7.24–7.25; the legacy Guice scope must not be assumed to be the same EventBus instance as WebHarvestServiceImpl's injected service bus.

### EventBusTypeListener: automatic registration has no matching retirement path

- EventBusTypeListener detects any Guice-created type with a declared @Subscribe method and, in afterInjection(), resolves EventBus through the static InjectorHelper and calls register(instance). The listener stores no registration handle and defines no corresponding unregister path.
- ScraperModule binds EventBus in ScrapingScope, while EventBusTypeListener resolves it from the current static Injector. This is the legacy Guice-scoped bus path. Its behavior depends on the active scope and injector; do not conflate it with WebHarvestServiceImpl's explicit service-owned EventBus injection.
- A subscriber instance that outlives its intended scope can remain strongly reachable from the bus after its owner is retired. Conversely, an object injected when no ScrapingScope is active may fail to resolve the scoped EventBus. The inspected code has no explicit owner token, scope-close hook or deterministic registration cleanup.
- EventBusTypeListener's class scan uses getDeclaredMethods(); inherited subscriber methods are not considered by that scan. This is a discovery correctness detail, separate from thread safety, and should be covered when defining the new subscriber registration contract.

### ScrapingAwareHelper: confirmed mutable-list race and unbounded retention

- ScraperModule binds one ScrapingAwareHelper instance into the injector. The helper stores listeners in a plain ArrayList.
- ScrapingAwareTypeListener adds every injected ScrapingAware object to that helper. The helper's own source explicitly marks addListener as not thread-safe and has a TODO asking how to unregister listeners.
- addListener() can race with onBeforeScraping()/onAfterScraping() iteration. ArrayList mutation during iteration can produce inconsistent callback delivery or ConcurrentModificationException. Concurrent invocations can also call the same listener instance simultaneously; no listener-level serialization contract is declared.
- No removal method exists. The helper's strong references can retain transient or scope-bound instances beyond their intended lifetime, including objects whose dependencies/classloaders should otherwise be retired.
- ScrapingInterceptor invokes onBeforeScraping() before entering its try/finally. If a listener throws during the before notification, callback execution is skipped and scope.exit() is not reached by that finally block. In the finally path, an exception from onAfterScraping() can also prevent scope.exit(). Lifecycle callbacks therefore need failure aggregation and unconditional scope cleanup, not just a concurrent collection.

### LockedRegistry: lock does not protect an escaped live view

- AbstractRegistry uses a plain HashMap and returns storage.keySet() directly from listBound().
- LockedRegistry acquires its read lock while calling delegate.listBound(), but returns the delegate's live keySet after releasing the lock. A caller can iterate or mutate that view outside the decorator's lock, bypassing the lock entirely. Concurrent bind/unbind can therefore race with external iteration; callers can also remove bindings through the returned set without acquiring the write lock.
- The decorator serializes access only while calls pass through its methods. It cannot make a mutable object safe after that object escapes. listBound() must return an immutable snapshot (or a documented snapshot copy) rather than a live backing view.

### Required subscriber and callback lifecycle contract

1. Every subscriber registration must be owned by an explicit runtime/scope/generation and return a registration token or close handle. Retirement must unregister exactly that instance from exactly that bus.
2. Registration and retirement must be race-safe: no late subscriber publication after the owner enters closing; a registration racing with close must either be included in retirement or fail/undo itself.
3. Keep the service-level EventBus shared as already decided. Do not introduce per-session buses as a workaround. Distinguish legacy Guice ScrapingScope buses from the service-level bus explicitly in composition and migration.
4. Replace ScrapingAwareHelper's global mutable listener list with lifecycle-owned registrations and immutable callback snapshots. Define whether callbacks may execute concurrently; if not, serialize per listener/owner without globally serializing unrelated sessions.
5. Callback failures must not skip mandatory scope/resource cleanup. Run teardown in an unconditional finally path and preserve all callback/cleanup failures in a structured result or suppressed-exception chain.
6. Make generic registry enumeration return a detached immutable snapshot. No live HashMap keySet, values, entrySet or mutable nested collection may escape a lock boundary.
7. Add deterministic tests for concurrent add/notify/remove; scope retirement and listener reachability; duplicate registration; callback exception before and after invocation; concurrent callbacks to one listener; subscriber unregister on scope/runtime close; registration racing with close; and listBound snapshot iteration during concurrent bind/unbind.

### Exact inspected source blobs

- EventBusTypeListener.java: 3b368eb75ce842f3db6a67b1e9424f644b341824
- ScrapingAwareTypeListener.java: 4ed97073d64878be10cd4798c47b56ecd169f1cf
- ScrapingInterceptor.java: 8de38b972d5c6eb41cc00e9c38ebe0f584e8c9e2
- ScraperModule.java: d31a5e30b0949c752743123cbcfb4219ba60713e
- InjectorHelper.java: 24c114b24a3abc079a9d447b59155e3598f5abe7
- AbstractRegistry.java: 9622bee0c049297ec0004ecb89673373f65d5fc8
- LockedRegistry.java: d9beeadd2fdc63a3a1526899bd798973d0f37a16
- EventBusTypeListenerTest.java: 9eb438cdd6cc8cd44b2636f777f661d0a3ea4bde
- ScrapingAwareTypeListenerTest.java: 6d854e1107fc16d42b31f1207021e3d6e59faaae

Source inspection only. No runtime implementation, test execution or leak/concurrency reproduction is included in this checkpoint.

## 7.27 Execution-context confinement, Guice scope boundaries and session registry identity

This section distinguishes genuinely thread-confined legacy execution objects from process-wide state, and records where the current code does not enforce that confinement. The intended target remains concurrent sessions on a shared service EventBus; per-session mutable execution context must not be concurrently accessed unless its API explicitly supports it.

### ScraperContext: mutable state is safe only under an undocumented single-execution-thread assumption

- ScraperContext stores variable state in plain HashMap<String, Stack<Variable>>, with a separate mutable Stack<ScopeFrame>, HashSet and HashMap per nested frame. setLocalVar(), getVar(), replaceExistingVar(), iteration and executeWithinNewContext() do not synchronize these structures or publish immutable snapshots.
- executeWithinNewContext() mutates the shared scopeStack and centralReferenceTable. Two threads entering nested contexts on the same ScraperContext can interleave pushes/pops and variable stack changes, causing one execution to observe or remove another execution's variables. This is not a claim that ordinary single-threaded nested plugin execution is broken; it means concurrent use of one context is not supported by the implementation and must be prohibited or redesigned.
- ScraperContext exposes mutable setters for config, EventBus and ScraperSession. The source comment says EventBus/session are set once and then immutable, but setters do not enforce one-time assignment, immutability, visibility or session/context identity. A caller can replace these references while another thread is using the context.
- ScraperModule's ContextFactory obtains a fresh ScraperContext from an unscoped Provider and sets its Config, which supports a per-execution-context construction model. However, the type/API does not encode thread confinement, ownership, or generation identity, and the inspected factory does not itself bind the created context to exactly one worker/session.

### Guice ScrapingScope: ThreadLocal isolates threads, not asynchronous execution propagation

- AttributeHolderScope stores a Stack of AttributeHolder instances in a ThreadLocal. Nested scope entry/exit is therefore isolated by Java thread, and scoped object creation synchronizes on the current holder's attribute map.
- This is not asynchronous context propagation. If execution moves to another executor thread, that thread has no current ScrapingScope unless it explicitly enters one with the correct holder. Conversely, reusing a worker thread without balanced exit can expose stale scope state to later tasks.
- exit() pops the holder but does not remove the ThreadLocal when its stack becomes empty. Long-lived executor threads can retain an empty ThreadLocal/Stack allocation; more importantly, any unbalanced enter/exit retains the holder and its scoped objects on that worker thread.
- The scope is static/shared at ScraperModule class level, but the per-thread stack is the isolation boundary. Do not treat a shared ScraperScope instance as a shared scope context, nor assume it follows a session across thread hops.

### Scope-bound status and Monitor

- ScraperModule binds Monitor, EventBasedStatusHolder and RunningStatusGuard in ScrapingScope, so their intended lifetime is one thread's active scraping scope rather than one process-wide singleton.
- EventBasedStatusHolder keeps a plain mutable ScraperState and changes it in EventBus subscriber methods. This is acceptable only if event delivery and reads follow the intended scope/thread discipline; it is not independently thread-safe, and the handlers do not inspect an execution/session identity in the event payload.
- The monitor coordinates pause/resume transitions within the scope, but it does not establish worker cancellation, session ownership, or safe cross-thread context propagation. Status pause/resume and session terminal state remain separate state machines.

### SessionRegistry: concurrent map, but identity and lifecycle semantics are incomplete

- SessionRegistryImpl uses ConcurrentHashMap and returns detached collection snapshots; these are positive map-level properties.
- register() uses put(sessionId, session), silently replacing any existing session with the same ID. A late registration can replace a live session; a later unregister(sessionId) can then remove whichever instance currently occupies that key, not necessarily the instance that originally registered it.
- unregister() and clear() only remove references from the registry. They do not request cancellation, await quiescence, retire resources, unregister subscribers or preserve a termination record. Registry membership is not execution ownership or proof of lifecycle completion.
- The returned snapshots contain mutable ScraperSession objects. Snapshot collection structure is detached, but session status, metrics and context continue to change concurrently; callers must not mistake a list snapshot for an immutable point-in-time session-state snapshot.

### Required execution-context and scope contract

1. Give each execution context an immutable owner identity: runtime ID, session ID and execution generation. Bind the context once before publication; reject reassignment to another session/generation.
2. Explicitly choose one of two models for context variables: strict single-worker thread confinement with assertions that reject cross-thread use, or a redesigned execution-local context stack that supports documented concurrency. Do not merely replace HashMap with ConcurrentHashMap: nested variable-stack semantics and scope frames require atomic ownership.
3. For asynchronous work, propagate an explicit scope/context token and enter/exit it around each task. Never rely on ThreadLocal state following an executor task. Ensure exit is unconditional and remove the ThreadLocal when the stack becomes empty.
4. Keep ScrapingScope's pause/status objects scoped to a single execution owner. Define event identity and reject stale or cross-session pause/resume/stop events; keep pause state separate from session cancellation and worker termination.
5. Change SessionRegistry registration to reject duplicate IDs or use putIfAbsent with an explicit collision result. Make unregister identity-aware (session ID plus exact instance/generation) and separate registry removal from session termination/resource retirement.
6. Add deterministic tests: same ScraperContext accessed from two threads; nested variable scopes under interleaving; context reassignment after binding; scope propagation across executor threads; unbalanced scope cleanup and ThreadLocal removal; stale pause/resume event delivery; duplicate session-ID registration and stale unregister; registry snapshot versus live mutable session state.

### Exact inspected source blobs

- ScraperContext.java: 6273525f5566ef59575acb593dfec8f3ad3b5e21
- ScraperModule.java: d31a5e30b0949c752743123cbcfb4219ba60713e
- AttributeHolderScope.java: 11a39c99e517ae9ae1ba09cf69a885b636465b64
- ScopeAttributeHolder.java: 9267c320c96ee4b956eb0d24619b60aeaa258352
- EventBasedStatusHolder.java: 34cc3dadef3f2d651a463bf28192b64765eea70e
- SessionRegistryImpl.java: 8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124
- ScraperSessionImpl.java: 78d07d1c485ea215d68d6d415f36b2be0c180a4f
- SessionMetrics.java: 1204e7566ab56760323703b031e7985bf635feed

Source inspection only. No runtime implementation, test execution, stress test or ThreadLocal leak reproduction is included in this checkpoint.

## 7.28 Shared service and scripting-engine state across concurrent sessions

This audit checks singleton-like services used by concurrent execution. A shared service may be safe to share only when its mutable state, request state and close lifecycle are separately controlled.

### Script engines: thread isolation is not session isolation

- JSRScriptEngineFactory uses ThreadLocalCache keyed only by ScriptingLanguage. The cache is per factory instance and per Java worker thread, not per ScraperSession or execution generation.
- WebHarvestServiceImpl uses a reusable executor (the default is a cached thread pool). A worker thread can execute session A and later session B; if both request the same scripting language, the same underlying javax.script.ScriptEngine can be reused across those sessions.
- JSRScriptEngineAdapter explicitly documents that the adapted engine is not thread-safe. ThreadLocalCache prevents two threads from sharing one cached engine concurrently, but does not prevent sequential cross-session reuse on a pooled worker, nor does it define a reset/dispose boundary when a session ends.
- The adapter clears engine-scope bindings before copying variables and includes pollution detection, which is a useful mitigation. However, clearing bindings is not proof that provider-specific interpreter state, compiled artifacts, global objects, classloader references or host objects are reset. The provider-specific behavior must be tested; do not claim a confirmed cross-session data leak without such a test.
- ScriptEngineManager is held as one factory field. Its provider discovery/manager thread-safety and provider classloader isolation are not established by the inspected code.

### BaseTemplater: mutable global injection point

- BaseTemplater is a utility with static mutable `@Inject public static ScriptEngineFactory scriptEngineFactory`. This is process-global state, not runtime/session-scoped dependency injection.
- Concurrent evaluation reads the same static reference. Concurrent injector/bootstrap activity can replace it while work is running; separate runtimes cannot safely select independent factories or classloader generations. There is no explicit lifecycle or visibility contract for the field.
- The per-call local result list and indexes are local, but that does not neutralize the static dependency.

### HttpServiceImpl: shareable client, unclear owner/close coordination

- HttpServiceImpl owns one final Apache CloseableHttpClient and executes request-local HttpGet/HttpPost/response objects. This is a plausible shared-client design; the inspected source does not establish that the client itself is unsafe for concurrent requests.
- The service is AutoCloseable, but the inspected API does not bind its close lifecycle to runtime shutdown, session leases or in-flight request draining. Closing the shared client while requests are active needs an explicit owner/lease contract; per-session close would be incorrect for a runtime-shared client.
- Request/response maps are local to each call. No shared mutable request-header map is retained by executeRequest. Constructor-supplied default headers are copied into a private HashMap by HeaderPreservingInterceptor; getDefaultHeaders() returns a detached copy. This is a positive defensive-copy property, although the returned copy is not itself immutable.

### DefaultPluginFactory and metadata catalog

- DefaultPluginFactory's constructor cache is a ConcurrentHashMap keyed by Class. This safely publishes constructor lookup results at map level, but the key strongly retains Class objects and therefore their defining classloaders for the lifetime of the factory. A runtime/plugin-generation retirement must release the factory/cache as part of classloader retirement.
- PluginMetadataCatalog is static/eager and builds descriptors once from PluginClassScanner's class-name list. It loads classes with one-argument Class.forName(), which uses the catalog's defining loader and initializes the class. The resulting static descriptor map is not runtime-, artifact- or classloader-generation scoped. This compounds the scanner cache isolation issue in 7.25.
- The catalog map is unmodifiable, but descriptor internals and the returned catalog schema are not proven deeply immutable by the inspected excerpts. Treat deep immutability as unverified until all nested collections/objects are inspected.

### Required service ownership rules

1. Treat ScriptEngine as mutable session state unless a provider-specific conformance test proves safe reset/reuse. The conservative default is a session/generation-owned engine with deterministic disposal; an optimized worker-local cache requires an explicit reset contract and cross-session isolation tests.
2. Replace BaseTemplater static injection with an instance dependency or an explicitly runtime-scoped immutable service. No mutable process-global injection point may select a runtime's provider.
3. Make the HTTP client runtime-owned and shared across sessions only through a lease-aware service. Runtime shutdown closes admission, drains in-flight request leases, then closes the client once. Sessions must never close a runtime-shared client.
4. Tie plugin factories, metadata catalogs and constructor caches to an immutable runtime/plugin generation. Retirement must release all strong Class/Method/Constructor references before closing the defining classloader.
5. Make all runtime configuration immutable after publication; defensively copy default headers and nested metadata/schema values.
6. Add cross-session tests on the same reused worker thread: session A writes interpreter globals/host objects, session B must not observe them; session A ends and its generation/classloader must become collectible after retirement. Add concurrent HTTP calls with shutdown/close racing and assert lease drain; two independent runtimes must use different factories/catalogs without static cross-talk.

### Exact inspected source blobs

- DefaultPluginFactory.java: 91c8084f671027954375ff3cab784691f8ae26e5
- PluginMetadataCatalog.java: 5ac82d7fb6436b458b29647e4c929878557b1c6e
- JSRScriptEngineFactory.java: f99047b30d2ca455898771ae8c207fafe4f1efd7
- HeaderPreservingInterceptor.java: 182181cc2652b68fbe6c769149d1bcdb622d57d1
- ThreadLocalCache.java: 3d69e3c9066f0ed0201e13da0427429df227e429
- JSRScriptEngineAdapter.java: 962f414ca17497461974faab2a9c26344332ff11
- BaseTemplater.java: dacefb8faec186176148ab2a0cbc5a538941d4c5
- HttpServiceImpl.java: 9e3dab1a40250f4d059ea812c74012d994873f0d
- WebHarvestServiceImpl.java: 10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec

Source inspection only. Provider-specific interpreter state, Apache client concurrency under close, classloader collectability and provider-specific interpreter reset semantics were not reproduced or tested. HeaderPreservingInterceptor defensive-copies its input map; its returned map is a detached but mutable copy.

## 8. Prioritized execution backlog and decision gates

This is the active implementation queue derived from the source audits above. Priority means dependency/blocking order, not permission to bypass maintainer approval. Until the architecture decisions in issues #93/#346 and the release/compatibility boundaries are approved, work remains audit/specification and test design; runtime implementation is STOP.

### P0 — must be resolved before claiming safe concurrent multi-session execution

1. **P0-01 — Session execution ownership and termination proof** (sections 7.22–7.23; #354): retain worker/child task handles; implement GRACE/KILL state machine; separate cancellation request, business outcome and proven quiescence; cover timeout executors; report unresponsive/quarantined workers. Acceptance: deterministic latch-based tests prove GRACE drain, KILL escalation, no false terminal/quiescent state and no orphan executor.
2. **P0-02 — Session resource ledger and leases** (7.24, 8.3–8.4; #355): immutable runtime/session/generation owner; race-safe registration; lease acquisition/release; close-request/drain/closed/failed states; idempotent cleanup and failure report. Acceptance: session A terminal events never close B JDBC/browser/HTTP resources on the same shared EventBus.
3. **P0-03 — Shared EventBus subscriber safety and lifecycle** (7.24, 7.26; #356): retain one shared service bus; make subscribers concurrent-safe, owner-aware and unregisterable; prevent late registration during retirement. Acceptance: concurrent A/B terminal events, registration-vs-close race, idempotent delivery and subscriber retirement tests.
4. **P0-04 — Context confinement and asynchronous propagation** (7.27 and MULTI_RUNTIME_ISOLATION_AUDIT.md; #357): bind context to runtime/session/generation and one worker; explicitly propagate scope tokens across executor hops; clear ThreadLocal in finally; reject context reassignment and cross-thread mutable access. Acceptance: interleaved nested-context tests and executor reuse tests show no variable/scope bleed.
5. **P0-05 — Interruption/deadline/effect disposition** (7.22–7.23, 7.28; #358): preserve InterruptedException; propagate shared deadline and abort handles; record COMMIT_UNKNOWN for external side effects. Acceptance: controlled HTTP/FTP/SMTP/JDBC/file tests prove bounded cancellation and no unsafe automatic retry.
6. **P0-06 — Runtime-owned immutable plugin generation** (7.25–7.26, 7.28; #347, dependent on #346): atomically publish complete registry snapshots; scope caches/factories/catalogs to runtime and classloader generation; enforce ID/QName collisions; retire exact generation. Acceptance: two-runtime out-of-tree fixture with overlapping QNames and distinct plugin classloaders.

### P1 — required before production-grade dynamic plugin lifecycle

7. **P1-01 — Subscriber/scope teardown correctness** (7.26–7.27): registration tokens, exact unregister, callback failure aggregation, unconditional scope exit, detached registry snapshots. Acceptance: no listener retention after generation retirement and no cleanup skipped after callback failure.
8. **P1-02 — Script-engine session isolation** (7.28): prove provider reset semantics or use session-owned engines; remove static BaseTemplater injection. Acceptance: same-worker A→B interpreter contamination tests and two-runtime independent factory tests.
9. **P1-03 — Shared HTTP client lifecycle** (7.28): runtime ownership, in-flight leases, immutable default headers, close-after-drain. Acceptance: concurrent requests during shutdown and close-race tests.
10. **P1-04 — Classloader/cache retirement** (7.25, 7.28): remove static discovery/catalog caches and strong factory references across generations. Acceptance: generation retirement plus weak-reference/classloader reclamation test.
11. **P1-05 — Concurrency conformance suite**: add stress/repeatable tests for registry mutation, event delivery, session context, subscriber lifecycle and resource closure; no reliance on sleeps or timing-only assertions.

### P2 — after P0/P1 architecture and conformance gates

12. **P2-01 — Performance and cache optimization**: only after isolation correctness, measure per-session engine cost and consider safe worker-local reuse with explicit reset contracts.
13. **P2-02 — Observability**: structured termination/resource reports, owner/generation correlation, subscriber and lease diagnostics, unresponsive worker visibility.
14. **P2-03 — Compatibility and migration**: document legacy provider capability levels, defaults, opt-in concurrency/isolation, migration fixtures and deprecation path.

### Immediate next work order

- Completed source-audit checkpoints: P0-01 through P0-04 are now mapped to dedicated issues #354–#357; P0-03/P0-04 source and existing-test gaps are documented in sections 7.26–7.27.
- Completed source audit/specification for P0-05 interruption/deadline propagation and external-effect disposition; dedicated work package #358 now depends on #354/#355 and coordinates with #356/#357.
- Next: complete P0-06 immutable plugin-generation audit, focusing on registry publication, runtime/classloader-scoped caches and generation retirement; avoid duplicating sections 7.25/7.28 and identify the remaining unverified call paths.
- Then: inspect reusable tests and map every P0 item to exact test classes and deterministic missing cases; publish one traceability matrix linking source evidence, issue, implementation boundary and acceptance test.
- STOP: do not change runtime code, merge PR #352, close issues or publish a release until the maintainer decision gates are explicitly satisfied.

## 8.1 Existing test coverage mapped to the prioritized backlog

This is a source-level test inventory at baseline `1729bdf4620a66686d02605083e27e27d1ab3944`. It records what the inspected tests actually assert, not whether the suite currently passes. Test presence is not acceptance evidence for concurrency unless the test controls the relevant race and asserts the ownership/quiescence invariant.

| Backlog | Existing test source | Current evidence | Missing acceptance coverage |
|---|---|---|---|
| P0-01 termination / GRACE-KILL | `WebHarvestServiceTest`, `ScraperSessionTest` | Basic session creation, status transitions, cancel status and shutdown API. Some waits use `Thread.sleep`. | Worker Future retention, child task exit, GRACE deadline, KILL escalation, non-cooperative worker quarantine, timeout executor shutdown, shutdown await result and proof of quiescence. |
| P0-02 resource ledger / leases | `StandaloneConnectionPoolTest`, `WebBrowserPluginLifecycleTest` | Component-level pool/browser lifecycle behavior exists. | Same shared EventBus with sessions A/B; A terminal event must not close B; registration-vs-close race; in-flight lease drain; close-failure report. |
| P0-03 subscriber lifecycle | `EventBusTypeListenerTest`, `ScrapingAwareTypeListenerTest`, `ScrapingInterceptorTest` | Helper callback invocation and listener-add smoke tests. | Actual EventBus registration/unregister, scope/runtime retirement, duplicate registration, concurrent callbacks, callback failure with guaranteed teardown and retention checks. |
| P0-04 context/scope confinement | `ScraperContextTest`, `ScraperContextVariableScopeTest`, `ScraperContextExtendedTest` | Variable and nested-scope behavior under ordinary sequential tests. | Cross-thread access rejection or supported concurrency, interleaved nested contexts, executor context propagation, ThreadLocal removal, owner/generation mismatch. |
| P0-05 interruption/deadline/effect disposition | HTTP/FTP/mail/JDBC/file plugin tests (individual module suites) | Functional plugin behavior; not sufficient evidence of bounded cancellation or commit disposition. | Controlled blocking operations, abort handles, propagated deadline, interruption preservation, `COMMIT_UNKNOWN` and no unsafe retry. |
| P0-06 registry/generation isolation | `DefaultPluginRegistryTest`, `PluginClassScannerTest`, `PluginMetadataCatalogDynamicTest` | Sequential registry operations, scan/cache behavior and dynamic metadata catalog construction. | Atomic multi-index publication, concurrent ID/QName collision, two runtime registries, distinct classloaders, generation pinning/retirement and cache isolation. |
| P1-02 script-engine isolation | `JSRScriptEngineFactoryTest`, `JSRScriptEngineAdapterTest`, `BaseTemplaterTest`, `BaseTemplaterStaticInjectionTest` | Provider availability, adapter delegation and variable copying/static injection behavior. | Same pooled worker executing A then B with interpreter globals/host objects; provider reset/dispose contract; independent runtime factories; concurrent use and generation retirement. |
| P1-03 HTTP client lifecycle | `HttpServiceImplBasicTest`, `HttpServiceRedirectContractTest` | Injection, network request basics and redirect-header behavior. | Concurrent in-flight calls, close-vs-request race, runtime lease drain, request deadline/abort and close exactly once. |
| P1-04 classloader/cache retirement | `PluginClassScannerTest`, `PluginMetadataCatalogDynamicTest`, `DefaultPluginRegistryTest` | Discovery/catalog behavior on the current classpath. | Two isolated classloaders, same plugin IDs/QNames, no cross-runtime static cache, factory/catalog retirement and classloader reclamation. |

### Test-source observations that change priority

- `SessionRegistryTest.testRegister_ReplaceExisting` explicitly asserts that duplicate session IDs replace the prior session. This is current tested behavior, not an incidental untested defect. The P0-04/P0-01 redesign must deliberately change the contract and update this test to assert rejection or identity-safe replacement semantics.
- `WebHarvestServiceTest.testCancelSession_ExistingSession` uses a fixed sleep and accepts either cancellation or a terminal state. It does not establish that a worker stopped. Replace with latches and an observable worker-exit signal.
- `ScraperSessionTest` has a test class comment mentioning thread safety, but its inspected tests primarily exercise state transitions and waiter behavior; they do not prove cancellation interrupts the executing worker.
- `JSRScriptEngineFactoryTest` explicitly notes that little more can be tested for engine providers. Current tests prove provider availability, not same-thread cross-session interpreter isolation.
- `DefaultPluginRegistryTest` covers duplicate ID sequentially and ordinary register/unregister; this does not cover simultaneous check-then-act or QName collisions across plugin IDs.

### Immediate test implementation sequence after approval

1. Build one deterministic test harness around `CountDownLatch`, barriers and explicit worker-exit futures; remove timing-only sleeps from cancellation acceptance tests.
2. Add P0-01 and P0-02 end-to-end shared-bus tests first, because cancellation and resource closure can corrupt other active sessions.
3. Add P0-03/P0-04 concurrency and scope-propagation tests, including executor thread reuse.
4. Add P0-06 two-runtime/two-classloader fixture before changing discovery/cache implementation.
5. Add P1 script-engine A→B contamination tests and HTTP close/drain tests.
6. Keep existing sequential compatibility tests, but update tests whose current expectations intentionally conflict with the new ownership contract (notably duplicate session-ID replacement).

### Exact inspected test blobs

- WebHarvestServiceTest.java: 024d1aeb30dc1ec6a97cfd0d1afa847c0eccf07c
- ScraperSessionTest.java: 31988741de660cb7455851e98ff75a706ea7d3dc
- SessionRegistryTest.java: aea82998ecf4ac81a8d02765af3f36465c3f8083
- JSRScriptEngineFactoryTest.java: a679c5b83865e146388721488431c9a93c2f05ee
- JSRScriptEngineAdapterTest.java: d6b448f225d466ae86cfb54385b61719257b2afe
- HttpServiceImplBasicTest.java: 3e4d6930be8c51c8b2ac4e7043227e8c827b9fcd
- DefaultPluginRegistryTest.java: ee2dd3c932637c39bd25282505d06c940a286f64
- ScrapingInterceptorTest.java: 3eb90ecee27a679f956f22fcde4c8b0cc99ab4ff

Source inspection only. No tests were executed as part of this inventory.

## 8.2 P0-01 execution-control state machine: concrete races and missing ownership

Baseline source: `1729bdf4620a66686d02605083e27e27d1ab3944`. This pass traces the actual service/session control flow and distinguishes session status from worker execution state.

### Confirmed control-flow defects

1. **The submitted task handle is discarded.** `WebHarvestServiceImpl.executeAsync()` calls `executorService.submit(...)` without retaining the returned `Future`. `ScraperSessionImpl` therefore cannot cancel/interupt or await its actual worker.
2. **Cancellation publishes terminal state before worker exit.** `ScraperSessionImpl.cancel()` sets `CANCELLED`, records end time, posts `SessionCancelledEvent`, and signals completion. It has no worker handle and does not interrupt or await execution. A caller of `awaitCompletion()` can therefore return while plugin code is still running.
3. **A cancelled session can continue through the normal execution path.** `executeSession()` does not check cancellation before context creation, `markRunning`, scraper invocation or successful completion. If cancellation wins before `markRunning`, `markRunning` throws because status is no longer PENDING; if cancellation wins while scraper code is running, that code has no service-owned interruption path. The later `markCompleted()` then rejects the CANCELLED state; `markFailed()` is a no-op for an already-terminal session. Neither path proves worker exit.
4. **Timeout execution creates an unowned executor.** `executeWithTimeout()` creates a new single-thread executor for each timeout session, submits work, and retains only its Future locally. On normal completion it never shuts the executor down. On timeout it cancels the Future but still does not shut down/await the executor or prove the scraper worker stopped.
5. **Shutdown is not a termination proof.** `shutdown()` ignores the boolean returned by `awaitTermination(60, SECONDS)`, then logs completion regardless. `shutdownNow()` cancels session status and invokes executorService.shutdownNow(), but cannot cancel timeout executors and does not await worker exit.
6. **Admission races with shutdown.** `executeAsync()` checks a volatile boolean and later submits without an atomic admission/submit boundary. Shutdown can begin between the check and submission. The lifecycle needs a synchronized/atomic admission gate that closes before draining and has defined rollback if submission is rejected.
7. **Session identity replacement is dangerous for control ownership.** `SessionRegistryImpl.register()` uses `put`, replacing a prior object with the same ID; `unregister(String)` removes by key only. Existing tests explicitly expect replacement (see 8.1), so changing this requires a deliberate API/test contract, not a silent implementation tweak.

### Correct state model

Do not overload `SessionStatus` with execution truth. Keep at least these orthogonal dimensions:

- **Request/outcome:** PENDING, RUNNING, COMPLETED, FAILED, CANCEL_REQUESTED, CANCELLED (business/API compatibility may retain current names with documented semantics).
- **Execution:** NOT_SUBMITTED, QUEUED, RUNNING, INTERRUPT_REQUESTED, EXITED, UNRESPONSIVE, QUARANTINED.
- **Termination disposition:** NOT_REQUESTED, GRACE_IN_PROGRESS, GRACEFUL, KILL_IN_PROGRESS, KILL_UNRESPONSIVE, PROCESS_EXIT_CONFIRMED.
- **Resource disposition:** OPEN, CLOSE_REQUESTED, DRAINING, CLOSED, CLOSE_FAILED, retained while owner remains unresponsive.

Every submitted task must have an immutable owner tuple `(runtimeId, sessionId, executionGeneration)` and a retained task handle. Timeout, explicit cancellation, runtime shutdown and parent/child cancellation must converge on one idempotent termination coordinator; they must not independently mutate status and race cleanup.

### Required transition/ordering contract

1. Atomically close admission before shutdown begins draining. A submission either becomes registered and owned before closure, or is rejected; there is no accepted-but-untracked task.
2. Register the session, execution generation, task handle and resource owner before execution can publish RUNNING. If submission fails, roll back registration and report rejection without leaving a phantom session.
3. Cancellation first records a request and closes admission to new child operations. It then propagates one deadline/token and requests interruption/abort on owned tasks and operation handles.
4. GRACE waits within a bounded cleanup budget for worker/child exit and resource lease drain. Only observed completion signals `GRACEFUL`/`EXITED`.
5. KILL for in-process Java is best-effort interruption plus quarantine. A non-cooperative thread is `UNRESPONSIVE`/`QUARANTINED`; do not close resources it may still be using and do not report it as killed. Hard kill requires a process boundary and confirmed process exit.
6. Timeout execution must use a service/runtime-owned executor or an explicitly owned per-session executor whose shutdown and termination are part of the same report. No anonymous executor may escape the session ledger.
7. `awaitCompletion()` must document whether it waits for business outcome or actual execution quiescence. Provide distinct waits if both are needed; never let a terminal business status imply worker exit.
8. Shutdown reports timeout/unresponsive tasks and honors the actual result of `awaitTermination`; it must not log successful termination on a false result.

### P0-01 acceptance tests — exact scenarios

- **Cancel-before-start:** hold executor admission with a deterministic gate, cancel after registration but before worker entry; assert scraper body never begins and worker reaches EXITED.
- **Cancel-during-block:** plugin signals `entered`, blocks on a latch; cancel; assert interrupt/abort observed, body exits, then resources drain. Verify `awaitBusinessOutcome` can complete earlier only if explicitly documented, while `awaitExecutionExit` remains pending.
- **GRACE cooperative:** worker and child operation respond to cancellation; assert all task handles exit and all leases close before the grace deadline.
- **KILL non-cooperative:** worker deliberately ignores interruption; assert result is UNRESPONSIVE/QUARANTINED, no false EXITED/KILLED status, and resources remain retained until safe retirement.
- **Timeout normal completion:** assert timeout executor/task is terminated and no worker thread remains after completion.
- **Timeout expiration:** controlled blocking plugin; assert cancellation request, bounded cleanup, worker disposition and resource report are consistent.
- **Shutdown/admission race:** coordinate submit and shutdown with barriers; every submission is either accepted+tracked or rejected, never orphaned.
- **Shutdown timeout:** inject a worker that outlives the shutdown budget; assert shutdown result exposes failure to terminate rather than logging success.
- **Duplicate ID/replacement:** update the existing replacement test to the chosen identity-safe contract; prove stale unregister/cancel cannot affect the replacement session.

### Existing tests and source references

- `WebHarvestServiceImpl.java`: `10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec`
- `ScraperSessionImpl.java`: `78d07d1c485ea215d68d6d415f36b2be0c180a4f`
- `SessionRegistryImpl.java`: `8813c995b6f2c7e4b4adb3a4a12d57cbb3c6c124`
- `SessionOptions.java`: `6afb9740dbab866cbf2ce2487735bc5d78bb1ea8`
- `WebHarvestServiceTest.java`: `024d1aeb30dc1ec6a97cfd0d1afa847c0eccf07c`
- `ScraperSessionTest.java`: `31988741de660cb7455851e98ff75a706ea7d3dc`
- `SessionRegistryTest.java`: `aea82998ecf4ac81a8d02765af3f36465c3f8083`
- `SessionManagementIntegrationTest.java`: `63f3ef6581ffec66a19200c0fd0aefda826cd999`

### Priority decision

Keep this as **P0-01, first implementation slice**, ahead of resource-ledger implementation: without retained task handles and truthful execution disposition, resource leases cannot know when it is safe to close. P0-02 must be designed alongside it, but resource closure implementation depends on P0-01's quiescence signal. No runtime implementation was performed in this audit.

## 8.3 P0-02 resource ownership: JDBC and browser paths are not equivalent

Baseline `1729bdf4620a66686d02605083e27e27d1ab3944`. This pass compared the actual resource subscribers and their existing lifecycle tests. It refines the earlier broad P0-02 assessment: browser ownership has meaningful per-session isolation coverage, while JDBC pool cleanup still reacts to every terminal event on the shared bus.

### JDBC pool: cross-session close defect remains directly visible

- `StandaloneConnectionPool.registerForSession(EventBus)` records only the EventBus reference; it does not record the owning session ID or execution generation.
- Its `@Subscribe` handlers for `SessionCompletedEvent`, `SessionFailedEvent` and `SessionCancelledEvent` all call `closeAllConnections()` unconditionally. They do not inspect the event's session ID.
- The service creates one shared EventBus for all sessions. Consequently, every registered JDBC pool subscriber sees every session terminal event. A terminal event from A can close a pool still in use by B.
- `StandaloneConnectionPoolTest` currently tests each terminal event by posting it to a pool registered on a standalone EventBus. It asserts the physical connection closes, but does not use two sessions or prove owner matching. The test's event objects use mocked sessions; there is no cross-session identity assertion.
- The pool Javadoc describes the EventBus as the current session's bus, but that assumption conflicts with `WebHarvestServiceImpl` injecting one shared EventBus into all session contexts. The documentation and composition root are inconsistent.

### Browser state: a useful positive control, but not full lease safety

- `WebBrowserPlugin` uses a static concurrent map keyed by session ID, creates a `BrowserSessionState` per session, and its three terminal event handlers compare the event session ID before closing. This avoids the JDBC subscriber's cross-session event bug.
- `WebBrowserPluginLifecycleTest.browserPagesAreIsolatedAndCleanedUpPerSession` creates sessions A and B on the same EventBus, verifies page isolation, terminates A, and verifies B's named/default pages remain usable. This is a real positive multi-session regression test.
- Browser close currently closes WebClients immediately on the matching terminal event. The inspected path has no operation lease or in-flight request drain proof; session-ID filtering protects ownership but does not prove it is safe to close a client while an operation is still using it.

### Refined P0-02 work items

1. Preserve the one shared EventBus; do not regress browser ownership by introducing per-session buses.
2. Give every JDBC pool an immutable `(runtimeId, sessionId, executionGeneration)` owner and filter terminal events by the complete owner identity.
3. Prefer registration through a session resource coordinator/ledger over independent ad-hoc EventBus registration; the coordinator owns registration tokens and cleanup.
4. Add a two-session JDBC regression test mirroring the browser test: A and B both acquire live connections on the same shared EventBus; completing/failing/cancelling A closes only A; B's connection remains usable; then B's own terminal event closes B.
5. Add race tests for pool registration concurrent with session termination and for terminal event concurrent with connection acquisition/use.
6. For JDBC and browser resources, add operation leases so close request prevents new operations and drains active operations within GRACE budget. Identity filtering alone is insufficient.
7. Preserve close failures and unresolved in-use resources in the termination report; do not silently treat close request as resource quiescence.

### Exact test/source blobs

- `StandaloneConnectionPool.java`: `ab4a92fb63599e88a55821fe141bf5aa50327008`
- `StandaloneConnectionPoolTest.java`: `1498206a92d2c2006bb399b71720e633590b0efa`
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`
- `WebBrowserPluginLifecycleTest.java`: `572f475dacabb7f706ce150d945a6dd762a83114`
- `WebHarvestServiceImpl.java`: `10e60a93a70b417cdb55dd674ad2e5bc8f9bbdec`

Source inspection only; the JDBC cross-session failure was not executed as a runtime reproduction. Browser test source demonstrates the positive per-session behavior but was not run in this audit.


## 8.4 P0-02 resource ownership: HTTP, FTP and SMTP operation boundaries

Baseline: `1729bdf4620a66686d02605083e27e27d1ab3944`. This pass extends 8.3 beyond JDBC/browser. It distinguishes long-lived shared resources from operation-local resources and external effects. The source review is static; concurrency failures below are risks implied by the observed lifecycle, not claimed runtime reproductions.

### HTTP: runtime singleton with no visible drain contract

- `ScraperModule` binds `HttpService` to `HttpServiceImpl` in Guice `Singleton` scope. This is injector/runtime-scoped, not session-scoped.
- `HttpServiceImpl` owns one final `CloseableHttpClient`; each request creates a request object and `executeRequest` closes its `CloseableHttpResponse` with try-with-resources.
- `HttpServiceImpl.close()` directly closes the shared client. The inspected `HttpService` contract exposes request methods but no operation lease, cancellation/abort handle, admission gate or drain operation.
- The client may be safely shared for concurrent requests according to its library contract, but thread-safe request execution does not prove safe shutdown while requests are active. Runtime shutdown needs to stop new requests, account for in-flight calls, and close the client only after drain or explicitly report unresolved work.
- Do not make this client session-owned merely to simplify cleanup. Its observed ownership is injector/runtime; the new runtime resource coordinator should register it at that level.

### FTP: operation-local client, synchronous network work

- `FtpPlugin` creates a new `FTPClient` for each invocation, executes connect/login and the selected operation synchronously, then logs out/disconnects in `finally`.
- The separate `FtpGetPlugin` and `FtpPutPlugin` paths likewise create a client per invocation and disconnect in `finally`.
- This is a useful operation-local ownership pattern: it does not share one FTP client across sessions. However, no explicit operation handle is registered with the session, and the inspected code does not expose a cancellation hook that can abort a blocked connect/transfer. A worker interrupt alone is not proof that the socket operation has stopped.
- Model the client/socket as an operation-owned resource whose lifetime is nested under the session. Register its abort/close action before blocking network I/O; GRACE requests abort and awaits operation exit; KILL may quarantine it if exit cannot be confirmed. Do not close it twice or report successful drain solely because the plugin method returned an exception.

### SMTP: operation-local message object, externally ambiguous outcome

- `MailPlugin.doExecute()` constructs a new Commons Email object for each invocation and calls synchronous `email.send()`. No reusable SMTP client or retained send/abort handle is visible in this path.
- The message object is invocation-local, but SMTP delivery is an external side effect. If cancellation/interruption races with the send, local worker exit cannot establish whether the remote server accepted the message. The result model must preserve an explicit effect disposition such as NOT_STARTED, IN_PROGRESS, CONFIRMED, FAILED or UNKNOWN; it must not retry an UNKNOWN send automatically.
- This is not a shared-resource lease in the same sense as JDBC/browser/HTTP. It is an operation/effect record that must be linked to the execution owner and cancellation report.

### Unified ownership and lease contract for P0-02

Use one resource ledger/coordinator with immutable owner identity `(runtimeId, sessionId, executionGeneration)`, plus a resource identity and declared scope: RUNTIME, SESSION or OPERATION. Every registration returns an idempotent registration token; cleanup is performed by that token/owner, never by an unqualified global close.

A lease is acquired before a resource is used and released in `finally`. The coordinator must atomically reject new leases after close is requested, track active leases, and expose these distinct states:

- registration: REGISTERED / REJECTED;
- use: IDLE / IN_USE;
- close: OPEN / CLOSE_REQUESTED / DRAINING / CLOSED / CLOSE_FAILED / QUARANTINED;
- effect disposition (where applicable): NOT_STARTED / IN_PROGRESS / CONFIRMED / FAILED / UNKNOWN.

Close request and lease acquisition must share one atomic boundary. The coordinator waits only within the GRACE cleanup budget. A timeout with active leases is unresolved, not CLOSED. KILL must retain a resource that an unresponsive worker may still access. Runtime-scoped resources drain only during runtime shutdown; session and operation resources drain at their owning boundary. A session terminal event on the shared EventBus is a signal to the coordinator, not permission for every subscriber to close every resource.

### Deterministic acceptance tests to add

1. **HTTP runtime drain:** block a request at a controlled transport barrier; request runtime close; assert new requests are rejected, client close waits for the active request lease, and close failures/timeouts are surfaced.
2. **HTTP multi-session:** A and B issue requests through the same runtime client; terminating A does not close the shared client or interrupt B.
3. **Browser close/use race:** pause after obtaining a WebClient lease but before/during page operation; request session close; assert no new lease is admitted and close waits for the active operation.
4. **JDBC two-session ownership:** on one shared EventBus, A's completed/failed/cancelled event closes only A's pool; B remains usable until B terminates.
5. **JDBC registration/use race:** coordinate pool registration, connection acquisition and termination; assert no post-close registration/use escapes the ledger and no connection is closed while a lease is active.
6. **FTP abort:** block connect/transfer with a controllable local server; request GRACE and verify socket abort plus worker exit; if not confirmed within budget, report unresolved/quarantined.
7. **SMTP ambiguous send:** simulate disconnect/cancellation after server acceptance but before client acknowledgement; assert effect disposition UNKNOWN and no automatic retry.
8. **Close failure:** inject close failures for each resource class; assert the termination report retains each failure and never marks the resource CLOSED.

### Exact source blobs

- `ScraperModule.java`: `d31a5e30b0949c752743123cbcfb4219ba60713e`
- `HttpServiceImpl.java`: `9e3dab1a40250f4d059ea812c74012d994873f0d`
- `WebBrowserPlugin.java`: `be7675390275b16ed63e5515a11969585acd2de6`
- `StandaloneConnectionPool.java`: `ab4a92fb63599e88a55821fe141bf5aa50327008`
- `DatabasePlugin.java`: `b3934d753f17f133bd582b50206db701af843728`
- `ConnectionPlugin.java`: `edb6ed93584cb03d967c830d120848c7e891f481`
- `FtpPlugin.java`: `d7098611ea7e0569b7cf9af01346be7b951da799`
- `FtpGetPlugin.java`: `fd4ee5da4556bf91bdae7940072cf7a5e106f6d5`
- `FtpPutPlugin.java`: `34061eb4fbc3a70f95d8d90a3a2faef611659f82`
- `MailPlugin.java`: `083297481e7e218916a77f0dea2308c63bbac7e7`
- `WebBrowserPluginLifecycleTest.java`: `572f475dacabb7f706ce150d945a6dd762a83114`

This section is an architecture/source audit only. No runtime code or tests were changed or executed.


## 8.5 P0 test traceability matrix — concrete source paths and release evidence

This matrix resolves the test class names in 8.1 to concrete repository paths from the recursive Git tree at baseline `1729bdf4620a66686d02605083e27e27d1ab3944`. Blob IDs are included where available so follow-up implementation can pin review to exact test sources. The matrix distinguishes existing functional coverage from missing acceptance evidence. No test execution is implied.

| Priority | Existing test path(s) | What the current test inventory establishes | Required additions / release evidence |
|---|---|---|---|
| P0-01 | `webharvest-core/src/test/java/org/webharvest/runtime/session/ScraperSessionTest.java` (blob `31988741de660cb7455851e98ff75a706ea7d3dc`); `.../SessionRegistryTest.java` (blob `aea82998ecf4ac81a8d02765af3f36465c3f8083`); `.../integration/SessionManagementIntegrationTest.java` (blob `63f3ef6581ffec66a19200c0fd0aefda826cd999`) | Session state, registry behavior and management integration; existing duplicate-ID replacement contract is explicit. | Add service-level task-handle tests for cancel-before-start, cancel-during-block, normal-completion-vs-cancel, timeout-vs-cancel, submit-vs-shutdown, child operation exit, GRACE deadline and non-cooperative KILL quarantine. Assert worker exit independently of session outcome. |
| P0-01 | `webharvest-core/src/test/java/org/webharvest/runtime/session/SessionStatusTest.java` (blob `58b4027cc54aa8d11c445348e24d526f0b62d59f`); `.../events/SessionEventsTest.java` (blob `072dc60e757e04d580b6156381e036b9029c8289`) | Status and event shape/behavior. | Assert event identity includes runtime/session/generation as required; stale-generation events cannot mutate current execution state. |
| P0-02 | `webharvest-database/src/test/java/org/webharvest/runtime/database/StandaloneConnectionPoolTest.java` (blob `1498206a92d2c2006bb399b71720e633590b0efa`) | Pool closes on terminal events in a component-level isolated bus. | Add same-shared-bus A/B test for completed/failed/cancelled events; acquire connection leases; race register/acquire/close; verify B remains usable after A termination; report close failure and unresolved lease. |
| P0-02 | `webharvest-webbrowser/src/test/java/org/webharvest/plugin/webbrowser/WebBrowserPluginLifecycleTest.java` (blob `572f475dacabb7f706ce150d945a6dd762a83114`) | Positive per-session page isolation and owner-filtered cleanup on a shared EventBus. | Retain this as regression control; add close-vs-in-flight-page-operation lease/drain tests and stale generation handling. |
| P0-02 / P1-03 | `webharvest-core/src/test/java/org/webharvest/plugin/services/HttpServiceImplBasicTest.java` (blob `3e4d6930be8c51c8b2ac4e7043227e8c827b9fcd`) | Basic HTTP service behavior. | Controlled transport test for in-flight request vs runtime close, new-request admission rejection, request deadline/abort and close failure. Multi-session A/B proves session close does not close runtime-owned client. |
| P0-03 | `webharvest-core/src/test/java/org/webharvest/ioc/EventBusTypeListenerTest.java` (blob `9eb438cdd6cc8cd44b2636f777f661d0a3ea4bde`); `.../ScrapingAwareTypeListenerTest.java` (blob `6d854e1107fc16d42b31f1207021e3d6e59faaae`); `.../ScrapingInterceptorTest.java` (blob `3eb90ecee27a679f956f22fcde4c8b0cc99ab4ff`) | Subscriber/listener discovery and callback smoke coverage. | Add registration token/unregister tests, owner retirement and registration-vs-close race, duplicate/stale token behavior, concurrent callback contract, failure aggregation and proof scope exit always runs after before/after callback exceptions. |
| P0-03 | `webharvest-core/src/test/java/org/webharvest/LockedRegistryTest.java` (blob `7121a6e4d3a415e56c2713159eb67cc67323baa0`) | Registry decorator behavior. | Assert `listBound()` is detached/immutable; mutate registry concurrently after snapshot; caller cannot mutate delegate storage through returned collection. |
| P0-04 | `webharvest-core/src/test/java/org/webharvest/runtime/ScraperContextTest.java` (blob `de2a6e824c5bd256174ddb2b8440021ceceee05e`); `.../ScraperContextVariableScopeTest.java` (blob `3fc268dd01715aa0e404b629f7fd96f2ed183c7a`); `.../ScraperContextExtendedTest.java` (blob `1405700c10abd6a91661fd6518c4340651d2ba56`); `.../DynamicScopeContextExtendedTest.java` (blob `fd5a23f259d24abbdc6bec663ba32373340fc1a9`) | Sequential variable/scope semantics. | Add explicit confinement rejection or deterministic concurrent semantics; interleaved nested scopes; immutable owner binding; context propagation across executor hops; ThreadLocal cleanup on success/failure/interruption/cancel and reused-worker test. |
| P0-05 | `webharvest-ftp/src/test/java/org/webharvest/plugin/ftp/FtpPluginBehaviorTest.java` (blob `32a86a9291cf7dda40bace1a089c25ec2bdaa446`); `.../FtpPluginExceptionTest.java` (blob `7a26ddf4fc5d8edeb7d6cc883c7527faa6619133`); `.../FtpPluginTest.java` (blob `dfeec2757e16f71015bc87537223b4e54e21f608`); `webharvest-mail/src/test/java/org/webharvest/plugin/mail/MailPluginTest.java` (blob `fcd0b6c2a895a0a31fddd8443696cdb7e8de0462`) | Functional and exception behavior for FTP/mail; no evidence of deterministic in-flight cancellation or commit disposition. | Controlled local FTP/SMTP servers; block at transfer/send; cancel at protocol boundaries; assert interrupt preservation, abort/timeout, worker exit and UNKNOWN after remote acceptance but lost acknowledgement. No blind retry. |
| P0-05 | `webharvest-core/src/test/java/org/webharvest/plugin/services/HttpServiceImplBasicTest.java`; `webharvest-webbrowser/src/test/java/org/webharvest/plugin/webbrowser/WebBrowserPluginTest.java` (blob `9128f01def43d53d62b780c49ba7ea7d38c8ceba`); `.../WebBrowserPluginLifecycleTest.java` | Functional HTTP/browser behavior and browser lifecycle. | Add blocked response-body/browser-load cancellation, deadline propagation, operation abort and effect disposition tests; verify broad catches do not turn interruption into fallback/success. |
| P0-06 | `webharvest-core/src/test/java/org/webharvest/plugin/registry/DefaultPluginRegistryTest.java` (blob `ee2dd3c932637c39bd25282505d06c940a286f64`); `.../DefaultPluginRegistryBranchContractTest.java` (blob `2fb498088b4f32563c185259e301df6069317be8`); `.../DefaultPluginRegistryUnregisterResidualBranchContractTest.java` (blob `07de046e581e135ee41224bf3f15dec51f797ae3`) | Sequential registry contract and branch regression coverage. | Add simultaneous same-ID registration, QName collision, atomic snapshot publication, stale unregister and immutable generation read consistency. |
| P0-06 / P1-04 | `webharvest-core/src/test/java/org/webharvest/plugin/metadata/PluginClassScannerTest.java` (blob `acb1d90457c8912f23f0078a9e0f73092178c221`); `.../PluginMetadataCatalogDynamicTest.java` (blob `c3582c89aad930e15c714b434b0a5c4aba4d6425`); `.../PluginMetadataCatalogTest.java` (blob `40efed43975105fc214357ba526c78d4615d9693`) | Current classpath scanning and metadata catalog behavior. | Add two-runtime/two-classloader fixture, independent cache invalidation, pinned old generation during new publication, failed candidate rollback, provider non-instantiation during descriptor discovery and classloader retirement evidence. |
| P1-02 | `webharvest-core/src/test/java/org/webharvest/script/JSRScriptEngineFactoryTest.java` and `JSRScriptEngineAdapterTest.java`; `.../BaseTemplaterTest.java`; `.../BaseTemplaterStaticInjectionTest.java` | Provider availability, adapter behavior and static injection regression coverage. | Same pooled worker executes A then B with sentinel globals/host objects; prove engine reset/disposal and independent runtime factories. |

### Shared deterministic harness requirements

The P0 suite should use a small reusable test harness, not duplicated timing-based tests:

- **Gates:** `CountDownLatch` / `CyclicBarrier` for worker entry, operation entry, close request, and release.
- **Observable handles:** explicit worker-exit Future/latch, operation lease count, registration token state, resource close result and generation identity.
- **Controlled endpoints:** local HTTP/FTP/SMTP fixtures with protocol-level barriers and explicit acknowledgement/connection-drop points.
- **Assertions:** distinguish business result, execution exit, resource closure and external-effect disposition. A terminal session status alone is never sufficient.
- **Failure discipline:** every test has bounded waits and releases its gates in `finally`; no test leaves a non-daemon worker or server thread behind.
- **Test ordering:** P0-01 harness first; P0-02 shared-bus ownership and lease drain second; P0-03/P0-04 lifecycle and context propagation third; P0-05 side-effect semantics fourth; P0-06 two-runtime/two-classloader generation fixture fifth. P1 isolation tests follow on the same harness.

### Evidence and execution boundary

The test paths and blob IDs above were obtained from the Git tree at the stated baseline. The descriptions of existing assertions are from the prior source-level review recorded in 8.1–8.4. No tests were executed in this checkpoint. CI green status must be recorded per exact commit SHA, not inferred from an earlier commit.
