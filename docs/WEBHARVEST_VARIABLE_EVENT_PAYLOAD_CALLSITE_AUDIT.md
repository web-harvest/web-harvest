# Variable, Event and Payload Call-Site Audit

**Status:** Bounded source/call-site audit; not a claim of exhaustive external-consumer coverage.
**Inspected source revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related design:** [WebHarvest Data, Stream and Semantic Contract Architecture](WEBHARVEST_DATA_AND_STREAM_CONTRACT_ARCHITECTURE.md).
**Related baseline:** [WebHarvest Data Contract Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md).

## 1. Executive findings

This pass traced representative producer/consumer paths from plugin execution through HTTP, files, database results, Variable conversion and session events.

The most consequential findings are:

1. **There are two distinct in-process event paths.** The older `org.webharvest.events` path routes `HarvesterEvent` through a Harvester-scoped Guava EventBus. The newer `org.webharvest.runtime.session.events` path publishes session/plugin lifecycle events through the EventBus carried by `ScraperSession` and `DynamicScopeContext`. They are not one unified event contract.
2. **The newer event path is not a durable event store.** `AbstractPlugin` calls `EventBus.post()`; `ScraperSessionImpl` posts lifecycle events; the full-stream integration test creates an in-memory EventBus and an in-memory collector. The inspected path contains no durable append, event serialization, checkpoint, replay reader or retention policy. The “event sourcing” wording in class/test documentation overstates what this implementation demonstrates.
3. **Successful plugin events expose the raw result Variable.** A subscriber can retain a mutable/large wrapped value, including a byte array. There is no payload reference, redaction contract or retention boundary in this event type.
4. **HTTP response handling is fully materialized.** `HttpServiceImpl` calls `EntityUtils.toByteArray(entity)`; `HttpPlugin` returns the complete response as a `NodeVariable` wrapping a byte array for binary content.
5. **File binary support is incomplete and asymmetric.** Binary writes are supported, but `FilePlugin.handleFileRead()` explicitly returns `EmptyVariable` for `type="binary"`.
6. **List/binary aggregation is memory-based.** `ListVariable.toBinary()` accumulates all bytes in a `ByteArrayOutputStream`; `FilePlugin.appendBinary()` repeatedly allocates and copies a growing byte array.
7. **Database query results are materialized as XML text.** `DatabasePlugin.executeSql()` reads every row into a `StringBuilder` and returns one `NodeVariable`; the inspected implementation does not expose a cursor/stream result contract.

These are current implementation facts on the inspected revision. They justify a separate payload/stream contract boundary; they do not select a transport, storage engine, media framework or third-party library.

## 2. Event path inventory

### 2.1 Legacy Harvester-scoped events

**FACT —** `webharvest-core/src/main/java/org/webharvest/events/DefaultHandlerHolder.java`

- Stores registered handlers in a `LinkedList<EventHandler<?>>`.
- On `onBeforeScraping(Harvester)`, obtains an EventBus from a Guice Provider, registers handlers and associates the bus with the Harvester in a Registry.
- On `onAfterScraping(Harvester)`, unregisters handlers and removes the association.

**FACT —** `webharvest-core/src/main/java/org/webharvest/events/HarvesterEventSink.java`

- Resolves the EventBus by `event.getHarvester()`.
- Calls `eventBus.post(event)`.
- Throws if the event or scope bus is missing.

**FACT —** `EventSink.java` describes listener dispatch, not a durable store. Its API exposes only `publish(event)`.

**INFERENCE —** This is scoped in-process notification infrastructure. It must not be conflated with the newer session/plugin lifecycle event family or used as evidence of persistent event sourcing.

### 2.2 Session/plugin lifecycle events

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/session/ScraperSessionImpl.java`

- The default constructor creates a new Guava `EventBus`.
- The constructor posts `SessionCreatedEvent`.
- `markRunning`, `markCompleted`, `markFailed` and cancellation post corresponding session lifecycle events.
- The EventBus is an in-memory object supplied to the session; no durable event repository is part of this class.

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/DynamicScopeContext.java` exposes `getEventBus()` and `setEventBus()`. The context documentation says the reference is set once and supports calls from multiple threads.

**FACT —** `webharvest-core/src/main/java/org/webharvest/plugin/AbstractPlugin.java`

- At entry to `execute()`, reads the EventBus and ScraperSession from the context.
- Posts `PluginExecutionStartedEvent`.
- On exception, posts `PluginExecutionFailedEvent` with the Throwable and elapsed time.
- On success, posts `PluginExecutionCompletedEvent` with the returned Variable and elapsed time.
- Event publication failures are caught and logged; plugin execution is not failed solely because event publication failed.
- Records plugin timing into session metrics in the success/failure paths.

**FACT —** `PluginExecutionCompletedEvent.java` retains the exact `Variable` reference supplied to its constructor and returns it from `getResult()`. Its `toString()` only reports whether a result exists, not its contents.

**FACT —** `SessionEvent.java` records `Instant.now()` at event-object construction. This is event creation time, not source/capture time or ingestion time.

**FACT —** `webharvest-core/src/test/java/org/webharvest/runtime/session/events/EventSourcingFullStreamBug24IntegrationTest.java` creates `new EventBus()`, registers a test collector backed by `CopyOnWriteArrayList`, executes test plugins and asserts that the collector received start/completion/failure notifications and ordering. The test does not write an event log or reconstruct execution from persisted events.

**FACT —** `ScraperSession.java` explicitly documents that built-in processed-element, HTTP and plugin counters are not automatically connected to runtime events (issue #168); only timing is recorded by the built-in service.

**INFERENCE —** The current contract is best named *in-process execution lifecycle notifications*. Durable event sourcing, replay, event delivery guarantees and cross-process event transport remain unimplemented/unproven by this inspected path.

### 2.3 A concrete lifecycle subscriber

**FACT —** `webharvest-database/src/main/java/org/webharvest/runtime/database/StandaloneConnectionPool.java`

- `registerForSession(EventBus)` registers the pool as a subscriber.
- `SessionCompletedEvent`, `SessionFailedEvent` and `SessionCancelledEvent` handlers call `closeAllConnections()`.
- `closeAllConnections()` releases physical connections and unregisters the pool from that EventBus.

**FACT —** `DatabasePlugin.java` registers an inline connection pool with `context.getEventBus()` when available. It then relies on session lifecycle events for physical connection cleanup; without a session EventBus it closes the pool directly in its own finally path.

**INFERENCE —** Session events currently participate in resource lifecycle management, not only monitoring. Any future event abstraction or replacement must preserve deterministic cleanup on success, failure and cancellation, and must define behavior when event dispatch fails or the session terminates abnormally.

## 3. Payload producer/consumer call-site map

| Path | Current representation and behavior | Consequence / limitation |
|---|---|---|
| `HttpServiceImpl.executeRequest()` | Reads the complete HTTP entity with `EntityUtils.toByteArray()`; returns a response backed by `byte[]`. | Whole response is buffered before plugin-level processing; no streaming response handle or backpressure. |
| `HttpPlugin.doExecute()` | Detects binary content and returns `new NodeVariable(binaryData)`; text content becomes `NodeVariable(String)`. | Payload type is inferred from content type and represented as arbitrary wrapped object; no explicit media type/length/checksum/ownership contract on Variable. |
| `FilePlugin.handleFileRead()` | Text mode calls `Files.readAllBytes()` and constructs a String; binary mode returns `EmptyVariable`. | Text read materializes full file; binary read is explicitly not implemented. |
| `FilePlugin.handleFileWrite()` | Binary mode converts body to byte array and writes via `FileOutputStream`; text mode converts body to String then bytes. | Binary write is supported but still consumes a fully materialized Variable/byte array; text path is lossy for non-text data. |
| `FilePlugin.appendBinary()` | Iterates `body.toList()`, obtains each `toBinary()`, repeatedly allocates a larger array and copies old+new bytes. | Full materialization and repeated copying; potentially quadratic copying cost as fragments accumulate. |
| `ListVariable.toBinary()` | Appends every child byte array into `ByteArrayOutputStream`, then returns a new byte array. | Full aggregate must fit in memory; no demand, cancellation or bounded-buffer contract. |
| `ListVariable.toString()` | Joins non-empty child values with newline delimiters. | Flattening is not a reversible serialization of arbitrary structured values. |
| `NodeVariable` | Stores arbitrary `Object`; `toBinary()` returns the same wrapped byte[] reference when data is a byte array; `getWrappedObject()` returns the raw object. | No defensive ownership boundary; callers may share/mutate the same array. |
| `DatabasePlugin.executeSql()` | Reads a SELECT ResultSet row-by-row but appends all values into one XML `StringBuilder`; returns one `NodeVariable`. | Row iteration is incremental internally, but output is fully materialized; column values are coerced to String; no typed row schema/cursor/stream result. |
| `JsonToXmlPlugin.doExecute()` / `XmlToJsonPlugin.doExecute()` | Convert whole body strings into in-memory JSON/XML structures and return serialized text in `NodeVariable`. | Transformation boundary is whole-document materialization, not incremental parsing or streaming transformation. |

### 3.1 Additional Variable semantics relevant to adapters

**FACT —** `NodeVariable.java`

- `toString()` decodes byte arrays as UTF-8 by default.
- `toBinary()` returns the wrapped byte array directly when the wrapped object is `byte[]`; otherwise it stringifies then encodes.
- `getWrappedObject()` returns the original `data` reference.
- `toList()` derives a list from iterator semantics; it is not a typed record collection.

**FACT —** `ListVariable.java`

- Owns a mutable `ArrayList<Variable>`.
- `toList()` and `getList()` expose that backing collection.
- `addVariable()` mutates the collection and flattens nested `ListVariable` children by adding their elements.
- `toBinary()` concatenates child byte arrays without framing or element boundaries.

**INFERENCE —** A migration adapter cannot safely infer semantic type, framing, ownership, immutability or missing-value meaning from these conversions. Compatibility adapters should preserve existing behavior while new APIs make these properties explicit.

## 4. Core plugin source sweep: Variable boundaries

A bounded source sweep inspected all **47 Java source files** directly under `webharvest-core/src/main/java/org/webharvest/plugin/core/` at the inspected revision for Variable conversion, collection and wrapped-object operations. This is a package-level scan, not an exhaustive repository-wide or downstream-consumer inventory.

The sweep confirms that Variable is the common execution return/input type across control-flow and transformation plugins, but meaningful conversion/shape changes cluster at several boundaries:

| Source path / method | Observed behavior | Why it matters |
|---|---|---|
| `plugin/core/LoopInputSourceResolver.java` — `unwrapSingleChildResult()`, `flatten()` | Removes one body-executor aggregation layer and preserves direct ListVariable members and their Variable types. | This is an example of a careful compatibility boundary; preserve this behavior in migration characterization. |
| `plugin/core/LoopPlugin.java` — `doExecute()` | Runs each source item in a nested scope, but appends each non-empty body result via `bodyResult.toString()` to one StringBuilder and returns a NodeVariable string. | A loop can accept typed list members but its aggregate output becomes flattened text. It is not a typed map/collect operation. |
| `plugin/core/CallPlugin.java` — `normalizeReturnValue()` | For ListVariable, selects the last non-empty item; for NodeVariable wrapping a Java List, returns one item or joins multiple items with spaces; also normalizes bracketed string forms. | Function-call return normalization is intentionally legacy-specific and can discard list structure. |
| `plugin/core/XPathPlugin.java` — CSS evaluation path | Collects matching element text values into ListVariable of NodeVariable strings. | XPath/CSS result is a list of extracted text values, not a typed XML-node/result-set contract. |
| `plugin/core/TokenizePlugin.java` and `RegexpPlugin.java` | Build ListVariable values from token/match strings. | These are list-producing text transformations; element boundaries exist only in the Variable collection. |
| `plugin/core/WhilePlugin.java` | Aggregates iteration body results into a ListVariable on its result path. | Differs from LoopPlugin's string-concatenating output; superficially similar iteration plugins do not share one output shape. |
| `plugin/core/XQueryPlugin.java` | Converts a Variable to String XML, reparses it with Saxon to bind node() parameters; falls back to string on conversion failure. | Structured values cross a stringify/reparse boundary and can be copied/materialized. |
| `plugin/core/JsonToXmlPlugin.java` and `XmlToJsonPlugin.java` | Convert body values to String before parsing and serialize the transformed whole document into NodeVariable. | Current JSON/XML conversion is whole-document materialization. |
| `plugin/core/IfPlugin.java` | Inspects `getWrappedObject()` to implement truthiness, in addition to string/numeric handling. | Consumers can depend on the concrete wrapped Java type, not merely Variable's public conversion methods. |

Other Core plugins use Variable primarily as an execution/return carrier, scope value or body aggregation type (for example Def, GetVar, SetVar, Var, Config, Function, Return, Catch, Try, Script, Template, Text and List). The 47-file sweep does not prove that no additional conversion paths exist in other packages, scripting adapters, optional modules, IDE code or external applications.

**Architectural implication:** Preserve legacy behavior through explicit adapters and characterization tests. Do not silently normalize Loop and While to one behavior, change CallPlugin's return normalization, or assume every ListVariable is a homogeneous typed collection.

## 5. Design constraints derived from evidence

The next data/payload API design should be evaluated against these constraints:

1. **Separate value metadata from payload access.** Contract identity/version, media type, schema, units, provenance and quality cannot be inferred from `Variable`.
2. **Represent large payloads without mandatory materialization.** Support bounded streaming or managed artifact references; byte arrays remain a valid small-payload adapter, not the universal transport.
3. **Make ownership explicit.** Define who opens, consumes, closes, retains, copies and deletes a payload. A raw mutable byte[] reference is not an ownership model.
4. **Specify bounded flow control.** Demand/backpressure, maximum buffering, cancellation, timeout, failure and terminal completion must be explicit. A generic Iterator is not sufficient.
5. **Keep source and derived artifacts distinct.** Transformations should be able to emit a derived result with source reference, transformation identity/version and relevant timestamps without replacing the original payload.
6. **Keep runtime notifications distinct from domain events.** Session/plugin lifecycle events may carry references and execution metadata, but domain event schemas and durable event persistence are separate concerns.
7. **Preserve resource cleanup semantics.** Database pool cleanup currently depends on session terminal notifications; stream/artifact resources need deterministic cleanup on completion, failure, cancellation and runtime shutdown.
8. **Do not force media frameworks into Core.** Core should own neutral payload/stream lifecycle contracts only where proven necessary; codecs, capture protocols, AI inference and vendor integrations remain provider modules.

## 5. Remaining unknowns and next inspections

- Full Variable consumer inventory across all Core plugins, optional modules, scripting engines, IDE, public APIs and downstream embedding applications remains incomplete. GitHub code-search connector queries returned no results for known symbols; those empty results are not evidence of no call sites.
- The full subscriber inventory for both EventBus families, including all `@Subscribe` methods and runtime registration sites, remains to be completed.
- The exact configured Guava EventBus exception handler, subscriber failure policy and behavior during shutdown require a focused audit.
- HTTP response size limits, timeout behavior, maximum in-memory response size and service lifetime require a separate resource-budget audit.
- File plugin binary read behavior has no confirmed compatibility contract from this pass; existing tests and documented XML examples must be checked before changing it.
- Database result materialization limits, BLOB handling and transaction/cursor ownership need separate tests/audit.
- Public/external consumers and persisted artifacts are not covered by repository source inspection.

## 6. Review conclusion

This evidence is sufficient to reject two unsafe assumptions: that the existing session event stream is already a durable event-sourcing subsystem, and that current HTTP/file/list payload handling is already suitable for large or continuous data streams.

It is not sufficient to implement a replacement API. The next step is a bounded contract proposal with explicit ownership, streaming, cancellation, bounded buffering, artifact references and legacy Variable adapters, followed by characterization tests. No production code change is authorized by this audit.
