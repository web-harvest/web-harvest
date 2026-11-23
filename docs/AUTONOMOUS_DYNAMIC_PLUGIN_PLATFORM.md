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
5. Construct authorized providers through host DI/factories and initialize them in an isolated candidate generation.
6. Build all runtime and semantic projections from that candidate.
7. Atomically publish the complete new immutable/versioned generation.
8. Retire the previous generation only when no execution/session still references it.

A failed reconciliation must not partially mutate the active registry. The last-known-good generation remains available, and diagnostics identify the failed artifact/provider and reason.

Duplicate IDs, QNames, namespace bindings or aliases are errors, never last-write-wins map updates. Broken or incompatible providers must not silently disappear while bootstrap/reconciliation reports success.

Each generation exposes a stable generation ID or digest and provider/artifact provenance. Registry output must be deterministic regardless of artifact enumeration or JAR ordering.

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
