# Resource Ownership and Runtime Integration Audit

**Status:** Source-backed integration audit; no production changes.
**Inspected source revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related design:** [Plugin Registry Generation Design](PLUGIN_REGISTRY_GENERATION_DESIGN.md) and [Variable, Event and Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md).

## 1. Executive findings

WebHarvest already contains a substantial, tested resource ownership primitive: `org.webharvest.runtime.resource.ResourceLedger`.

It is not currently connected to the built-in runtime composition path inspected in this audit:

- `WebHarvestServiceImpl` creates a runtime ID and immutable `ExecutionOwner` for every accepted session, and tracks actual worker exit.
- `ResourceLedger` accepts that runtime identity and supports runtime-, generation-, session- and operation-scoped resources, active-use leases, generation pins, abort, drain, quarantine and close-failure retention.
- The built-in service does not construct, own or call a `ResourceLedger`.
- `ScraperModule` does not bind it.
- `DynamicScopeContext` / `ScraperContext` expose EventBus and ScraperSession but no resource ledger or resource-access contract.
- The inspected `WebHarvestService` interface has no resource-lifecycle API.

**Architectural consequence:** Do not invent a second resource ledger or a parallel generation-retirement mechanism before evaluating this existing implementation. The likely next task is an integration and contract review of ResourceLedger against the new runtime-generation/plugin-provider design, not a rewrite by default.

A concrete ordering hazard also exists: cancellation publishes `SessionCancelledEvent` before the worker is known to have exited, while the Database optional module responds by physically closing pooled JDBC connections. That may be intended to abort blocked I/O, but the current event contract conflates abort with final resource closure.

## 2. Existing ownership primitives

### 2.1 ExecutionOwner and registry generation identity

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/session/ExecutionOwner.java`

An immutable value identity combines:
- runtime ID;
- session ID;
- execution-generation number.

It distinguishes one execution owner from a reused session identifier.

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/registry/RegistryGenerationId.java`

A separate immutable value identity combines runtime ID and an opaque registry-generation value. Its documentation explicitly distinguishes registry-generation identity from execution-generation identity; a content digest alone may not distinguish two simultaneously live provider/classloader lifetimes.

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/service/WebHarvestServiceImpl.java`

- Owns a runtime UUID and monotonically increments an execution-generation counter.
- Creates an `ExecutionOwner(runtimeId, sessionId, executionGeneration)` for each accepted session.
- Supplies that owner to `ScraperSessionImpl`.

These identities are suitable inputs to a runtime-scoped resource ownership model, but execution generation and registry generation must not be conflated.

### 2.2 Actual worker-exit tracking

**FACT —** `ExecutionTrackedSession` separates business completion from worker quiescence. It exposes execution state, termination disposition and waits for observed worker exit.

**FACT —** `ScraperSessionImpl.markExecutionExited()` is documented as the only transition proving quiescence. Interrupt requests and unresponsive status do not set EXITED.

**FACT —** `WebHarvestServiceImpl.SessionTask.run()` calls `markExecutionExited()` after the task has either:
- passed the cancellation gate and completed its execution body and final task bookkeeping; or
- been confirmed not to enter the worker body.

For queued cancellation, `removeQueuedFuture()` marks exit only after the ThreadPoolExecutor confirms the task was removed. With generic executors, the task must run its cancellation gate and report exit itself.

**INFERENCE —** The built-in service already has the correct semantic boundary needed by a resource ledger: observed worker exit, not Future cancellation, interruption, session terminal status or EventBus notification.

## 3. ResourceLedger capabilities

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/resource/ResourceLedger.java`

The implementation defines four scopes:
- `RUNTIME` — owned by the runtime;
- `GENERATION` — owned by an immutable `RegistryGenerationId`;
- `SESSION` — owned by one `ExecutionOwner`;
- `OPERATION` — owned by one `ExecutionOwner`, with narrower intended lifetime.

Its key semantics are:

1. Registration establishes ownership; it is not itself an active-use lease.
2. Consumers acquire explicit leases attributed to an `ExecutionOwner`.
3. Generation resources require a generation pin spanning the entire execution; a lease must match the exact pinned generation.
4. Generation retirement atomically rejects new pins while allowing already-pinned executions to continue using their generation resources.
5. Admission is closed before abort/drain; abort and close are separate actions.
6. Session/operation resources require externally observed owner quiescence before final close.
7. Drain waits for both the ownership boundary and active leases.
8. Timeout leaves the resource `QUARANTINED`; the resource remains retained rather than being treated as safely closed.
9. Close failure is retained as `CLOSE_FAILED`; only successfully closed registrations can be retired.
10. The ledger explicitly refuses to infer worker exit from cancellation, interruption, EventBus delivery or terminal business status.

This is materially more complete than a simple `AutoCloseable` list or a collection of plugin `doCleanup()` callbacks.

### 3.1 Existing tests

**FACT —** `webharvest-core/src/test/java/org/webharvest/runtime/resource/ResourceLedgerTest.java`

The tests cover:
- lease admission racing with close requests;
- runtime resources with multiple attributed consumers;
- rejection of foreign execution owners;
- required owner quiescence even when no lease remains;
- separate/idempotent abort and preserved abort failures;
- close failures and retained registrations;
- concurrent drain/close behavior;
- exact generation pinning and draining across multiple sessions;
- use by already-pinned sessions after retirement starts;
- atomic pin admission versus generation retirement;
- duplicate local resource IDs across owners/generations;
- idempotent lease/pin release and stale registration tokens.

**BOUNDARY —** These tests establish the behavior of ResourceLedger in isolation. They do not prove integration with WebHarvestService, ScraperContext, plugin construction, database pools, HTTP clients, browser drivers or classloader retirement.

### 3.2 Adoption review items

The ledger is a strong foundation, but the following points need explicit design decisions before it becomes the runtime authority:

- **Generation tombstone retention:** `generations` is a `ConcurrentHashMap`; `generationEntry()` uses `computeIfAbsent`, and the inspected implementation never removes entries from this map. The source comment says retired entries remain as tombstones so stale IDs cannot be re-pinned. A long-lived runtime with frequent registry publications therefore retains one GenerationEntry per generation for its lifetime. Decide whether this is an accepted bounded-runtime trade-off or whether generation IDs are guaranteed never to be reused and tombstones can be safely retired.
- **No ledger-level shutdown/drain-all operation:** the public API has registration-level `drainAndClose()`, `size()` and generation-pin operations, but no public snapshot of registrations or runtime-wide `shutdown()/drainAll()`. The integration must define who retains each Registration token and how runtime shutdown drains every resource, including resources whose plugin/provider owner has disappeared.
- **Diagnostics surface:** outstanding leases and close/abort failures are visible through each Registration/DrainResult, but the ledger has no public aggregate diagnostic snapshot. The host needs a way to report all quarantined/failed resources without exposing mutable internal entries.
- **Registration-token discipline:** `Registration.close()` retires only an already-CLOSED entry. Providers must retain the token until drain succeeds; losing the token can leave an entry retained, which is safer than premature close but needs host-level diagnostics/recovery.

These are review questions, not evidence that the tested ledger state machine is incorrect.

## 4. Integration gap in the built-in runtime

The following host/runtime files were inspected for ResourceLedger references and lifecycle wiring:

| Source path | Current integration observed |
|---|---|
| `runtime/service/WebHarvestServiceImpl.java` | Owns runtime ID, execution owner, session tasks and actual worker-exit tracking; no ResourceLedger field, construction or calls. |
| `runtime/service/WebHarvestService.java` | Public service API exposes session execution, lookup, cancellation and shutdown; no resource ledger API. |
| `ioc/ScraperModule.java` | Binds legacy Harvester-scoped EventBus/Registry and context factory; no ResourceLedger binding. |
| `runtime/DynamicScopeContext.java` | Exposes EventBus and ScraperSession; no ResourceLedger or resource capability. |
| `runtime/ScraperContext.java` | Stores EventBus and ScraperSession; no ResourceLedger reference. |
| `plugin/AbstractPlugin.java` | Owns plugin initialize/execute/cleanup callbacks and publishes lifecycle events; no ResourceLedger registration or lease. |

The production ResourceLedger source path and its dedicated test path exist, but no integration was found in the inspected host composition and execution path. GitHub code-search results were incomplete, so this is not a claim that every optional/external module has zero references.

**INFERENCE —** ResourceLedger currently behaves as a tested runtime utility without a built-in runtime owner or plugin/provider integration contract. It cannot yet protect actual plugin resources merely by existing in Core.

## 5. Cancellation ordering hazard: database pool

**FACT —** `ScraperSessionImpl.cancel()`

1. Changes the business status to CANCELLED under the session monitor.
2. Invokes the service cancellation callback, which requests interruption/cancellation of the worker.
3. Signals business-completion waiters.
4. Posts `SessionCancelledEvent`.
5. Returns without waiting for `markExecutionExited()`.

Therefore, `SessionCancelledEvent` is a cancellation notification, not proof that the worker has exited.

**FACT —** `webharvest-database/src/main/java/org/webharvest/runtime/database/StandaloneConnectionPool.java`

- Registers itself with the session EventBus.
- Handles `SessionCancelledEvent` by calling `closeAllConnections()`.
- `closeAllConnections()` calls `releaseAllConnections()`.
- `releaseAllConnections()` iterates the registered proxies and closes their target physical JDBC connections.

**FACT —** `DatabasePlugin.java` registers inline connection pools with the context EventBus when available and relies on session terminal events for physical connection cleanup.

**FACT —** `webharvest-database/src/test/java/org/webharvest/runtime/database/StandaloneConnectionPoolTest.java`, `releasesConnectionsWhenSessionIsCancelled()`, posts a `SessionCancelledEvent` directly and asserts that the physical JDBC connection is already closed. The test does not model an in-flight JDBC operation, a still-running worker or delayed worker exit.

**RISK / INFERENCE —** On cancellation, physical JDBC connection closure can occur while the worker is still unwinding or while a JDBC call is responding to interruption. This may be intentional best-effort I/O abort, but the same action is also the final close. The current API does not explicitly distinguish:
- stop admitting new operations;
- abort/cancel active I/O;
- wait until worker exit and active leases drain;
- close the physical resource.

This needs a deterministic cancellation race test with a blocked/in-flight JDBC operation before changing behavior. Do not silently delay cancellation abort if that would break responsiveness; separate abort from final close.

## 6. Recommended integration direction (proposal, not implementation)

1. **One ledger per runtime.** The runtime/service that owns `runtimeId` should own the corresponding ResourceLedger; avoid global/static ownership.
2. **Pin one immutable registry generation at execution admission.** The execution task and its `ExecutionOwner` must carry the exact `RegistryGenerationId` selected before parsing/dispatch.
3. **Release the generation pin only after actual worker exit.** The existing `SessionTask` exit path is the authoritative integration seam; business completion/cancellation events are not.
4. **Give providers a narrow resource-registration/access contract.** Providers should register resources with explicit scope and receive only the registration/lease capabilities needed to use them. Do not expose the whole ledger as an unrestricted plugin service by default.
5. **Use operation/session scopes for transient resources.** Examples include a request body stream, file handle, JDBC statement/result set or short-lived device session. Use generation scope for provider/classloader-bound resources; runtime scope for shared HTTP clients or shared transport pools.
6. **Keep abort distinct from close.** Cancellation may invoke a provider-specific abort action; final close must wait for actual owner quiescence and lease drain.
7. **Surface quarantine and close failures.** A timed-out or failed close is an unresolved resource, not a successful cleanup. Keep diagnostics and prevent premature generation retirement.
8. **Test integration, not just ledger mechanics.** Add service-level tests for a blocked worker, cancellation, timeout, generation replacement, outstanding lease, provider cleanup and runtime shutdown before using ResourceLedger as the actual lifecycle authority.

Before implementation, review ResourceLedger's API against the target runtime SPI and classloader lifecycle. Reuse or revise it based on that review; do not assume the current API is final simply because its unit tests are comprehensive.

## 7. Review conclusion

The repository already has two important foundations:
- a built-in execution coordinator that distinguishes business status from actual worker exit;
- a resource ledger with explicit ownership, generation pins, leases, abort/drain, quarantine and failure retention.

The missing piece is their integration. This is a more precise next architecture task than designing another resource-management abstraction from scratch.

No production code was changed by this audit.
