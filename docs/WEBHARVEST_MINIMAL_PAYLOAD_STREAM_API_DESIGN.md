# WebHarvest Minimal Payload and Stream API Design

**Status:** Architecture proposal for review; no Java API is approved or implemented.
**Evidence baseline:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related audits:** [Variable, Event and Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md), [Resource Ownership and Runtime Integration Audit](WEBHARVEST_RESOURCE_OWNERSHIP_INTEGRATION_AUDIT.md), [WebHarvest Data Contract Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md).

## 1. Purpose and non-goals

This proposal defines the smallest useful boundary between plugin execution and data/payload/stream handling in WebHarvest.

It must support acquisition and integration of:
- ordinary structured records and documents;
- observations and domain events;
- files and binary artifacts;
- finite HTTP/FTP/database payloads;
- long-lived or real-time streams, including audio/video and sensor/robot/drone telemetry;
- derived results produced by parsers, transformations and optional AI/ML providers.

It does **not** make WebHarvest a data lake, storage engine, message broker, media server, codec framework, AI runtime, MCP server or robot-control safety system.

The Core boundary must remain independent of XML, Guice, a specific IoC container, a storage vendor, a transport protocol, a media framework, an AI provider and MCP. Those are adapters/providers.

## 2. Three concepts that must not be collapsed

### 2.1 Semantic data contract

A semantic contract states what a value, record, observation, event, artifact or operation means.

Minimum identity:
- stable contract ID;
- explicit contract version;
- optional schema/validation descriptor;
- semantic metadata such as units, quantity, source identity and quality where applicable.

The identity must not be derived from a Java class, Variable subclass, plugin ID, XML QName, MIME type, file extension, transport protocol or AI model name.

The Core runtime may carry a `DataContractRef`; it need not own a central semantic schema registry or domain vocabulary in the first milestone.

### 2.2 Finite payload

A finite payload is a bounded byte/text/document body whose production eventually completes. It may be small and inline or large and externally managed.

The contract must distinguish:
- descriptor/metadata from payload bytes;
- an inline value from an opaque externally managed payload reference;
- a payload reference from an active read/write lease;
- a payload's logical identity from one physical storage location;
- content type/encoding from semantic contract identity.

Minimum descriptor fields to evaluate:
- media type (optional when unknown);
- character encoding (only where meaningful);
- known length (optional);
- checksum/integrity metadata (optional);
- immutable payload identity/reference;
- source and derivation metadata held outside the raw byte stream.

Do not require every payload to be copied into a `byte[]`. Byte arrays remain a valid adapter for small payloads and legacy APIs.

### 2.3 Live stream

A live stream is a sequence of values/frames/segments with an explicit lifecycle. It is not a finite payload that happens to be read slowly, and it is not automatically a durable event log.

A stream descriptor identifies:
- stream identity and version;
- content/track descriptors where applicable;
- source identity;
- time semantics;
- ordering/sequence semantics;
- supported delivery and cancellation behavior.

A stream item may carry:
- sequence number;
- source/capture timestamp;
- media presentation timestamp (PTS), if applicable;
- monotonic acquisition time;
- synchronized wall-clock time, if available;
- ingestion and processing timestamps;
- payload or artifact reference;
- source/track identity and quality metadata.

Missing timestamps must remain missing; the runtime must not silently substitute wall-clock time for source time.

## 3. Candidate minimal Java boundary

The following names are illustrative only. Do not copy them into public API without a separate API review.

### 3.1 Payload descriptor and read lease

A finite payload needs two distinct objects:

- **Descriptor/reference:** immutable description and identity; safe to pass through plugin outputs, metadata, lineage and event notifications.
- **Read lease:** temporary permission to consume bytes; has explicit ownership and close semantics and is backed by the runtime resource lifecycle.

Candidate shape:

```java
interface PayloadRef {
    PayloadDescriptor descriptor();
}

interface PayloadAccess {
    PayloadReader open(PayloadRef payload, PayloadReadOptions options);
}

interface PayloadReader extends AutoCloseable {
    InputStream input();
    long bytesRead();
    void close();
}
```

This is not yet a decision that Java `InputStream` is the final abstraction. The important requirements are that access is explicit, readers are closeable, payload metadata is separate, and the runtime can attribute and drain active reads. The reader owns the underlying stream and its resource lease; `input()` is a borrowed view, and closing the reader closes the stream and releases the lease. A reader must not expose an unbounded in-memory copy as the default behavior.

A corresponding write API may be supplied by an optional artifact/storage provider. It should define commit versus abort and return a stable PayloadRef after successful completion. Core must not mandate a particular storage backend.

### 3.2 Stream session and bounded delivery

For a live stream, the first API candidate should be based on JDK `java.util.concurrent.Flow` (the runtime baseline is Java 17), with an explicit stream session around it:

```java
interface StreamSession<T> extends AutoCloseable {
    StreamDescriptor descriptor();
    Flow.Publisher<T> publisher();
    CompletionStage<StreamTermination> termination();
    void cancel(StreamCancellation reason);
}
```

This is a candidate shape, not an approved signature. It still needs decisions about multi-subscriber fan-out, terminal semantics, cancellation acknowledgement and resource ownership. `cancel()`/`close()` request termination; the `termination()` stage should complete only after the provider has observed source termination and released its source-side resources. A per-subscriber `Flow.Subscription.cancel()` cancels only that subscriber unless the provider's explicit fan-out policy says no consumers remain and source shutdown is appropriate.

Non-negotiable behavior:
- bounded per-subscriber buffering;
- explicit demand/backpressure;
- explicit overflow policy (block, drop, fail, or hand off to an optional durable sink);
- no implicit unbounded queue;
- cancellation propagation to the source/provider;
- observable terminal outcome (completed, failed, cancelled, timed out);
- deterministic release of source, decoder, network, device and buffer resources;
- slow AI/analytics consumers cannot cause unbounded memory growth or silently block unrelated consumers.

A subscriber cancelling its subscription must not be confused with the source stream being globally terminated. Multi-consumer fan-out policy must be explicit.

### 3.3 Typed data envelope

Semantic identity and payload access may be joined by an immutable envelope:

```java
interface DataValue {
    DataContractRef contract();
    ValueMetadata metadata();
    PayloadRef payload();
}
```

This is only appropriate when a value has an external/large payload. Small structured values may use a typed representation supplied by a semantic contract provider. Do not force every scalar or record through a byte stream.

The envelope must not become a generic bag of arbitrary mutable Java objects. Validation, version compatibility and provenance should be explicit.

## 4. Resource lifecycle is part of the API, not an implementation detail

The existing `ResourceLedger` already models:
- runtime, generation, session and operation ownership;
- active-use leases;
- exact registry-generation pins;
- separate abort and close actions;
- owner quiescence;
- bounded drain;
- quarantine and close-failure retention.

The missing work is runtime/provider integration and review of its adoption gaps, not another parallel ledger.

Required lifecycle sequence:

1. Runtime admits execution and pins the exact immutable registry generation.
2. Provider opens a resource and registers it with the correct scope.
3. Consumer acquires a lease attributed to the actual execution owner.
4. Cancellation may request abort to interrupt active I/O; it does not prove worker exit.
5. The execution coordinator observes actual worker exit.
6. Owner quiescence is recorded; new leases are rejected.
7. Active leases drain within a bounded budget.
8. The resource is closed; timeout/failure remains visible and quarantined.
9. Registration and generation may be retired only after successful close and zero active pins.

A session terminal event is not proof of worker quiescence. Existing Database and WebBrowser subscribers close resources on `SessionCancelledEvent`; those paths require cancellation-race characterization before replacement.

**Budget is a separate policy contract.** ResourceLedger protects ownership and lifetime; TokenTracker/SessionMetrics hold explicitly recorded counters; neither enforces quotas. Payload size, stream duration, concurrent sessions, queue capacity, memory and provider-specific I/O limits must come from an explicit host-supplied execution/resource budget. The neutral payload/stream types must report relevant usage and failures without embedding billing or tenant quota policy.

## 5. Legacy Variable adapter boundary

Keep the existing `Variable`, `NodeVariable` and `ListVariable` APIs as compatibility surfaces during migration.

Candidate adapters:
- `NodeVariable(byte[])` → finite inline binary payload;
- `NodeVariable(String)` → finite text payload with explicit/default encoding;
- managed external bytes/file → PayloadRef plus read lease;
- `ListVariable` → ordered collection only when the producer's semantics say it is a collection; never infer that concatenation means a byte stream;
- legacy XML/JSON values → structured/document adapters with explicit media and semantic contract references where known.

Do not:
- stringify arbitrary values to make them fit the new API;
- treat all ListVariable instances as homogeneous records;
- change LoopPlugin/WhilePlugin aggregation or CallPlugin normalization as part of the adapter;
- return mutable byte-array references as if they were immutable payload handles;
- silently map EmptyVariable/null/zero/false to the same semantic state.

The current File, HTTP, FTP, ZIP, Mail, Database and browser call sites must migrate incrementally behind compatibility tests.

## 6. AI, media and derived data

AI is a consumer/provider of capabilities, not a separate semantic data model.

An AI/ML provider may consume:
- a finite PayloadRef through a read lease;
- a StreamSession subscription;
- typed structured data under a declared Semantic Contract.

An AI/ML provider may produce a derived result with:
- source payload/stream item references;
- provider/model identity and version;
- relevant inference configuration/version;
- source and processing timestamps;
- confidence/score semantics (not an untyped number);
- validation/review state where required.

Derived outputs are untrusted data. They do not overwrite raw source evidence. Correlation and interpretation are separate operations and may have different latency/resource budgets.

Audio/video codecs, RTSP/WebRTC, camera capture, speech recognition, computer vision, GPU execution and vendor-specific models remain optional provider modules. Core should not define codec frame types or vendor-specific device APIs.

## 7. First migration slices (proposal)

1. **Characterization:** lock current Variable conversion, loop/call/list behavior and binary compatibility in tests.
2. **Resource integration:** wire ResourceLedger to the built-in runtime's actual worker-exit boundary; test cancellation, lease drain and generation pinning. Resolve tombstone retention and runtime-wide drain diagnostics.
3. **Finite payload adapter:** add one small in-memory payload implementation and one managed/streaming test provider; no storage vendor dependency.
4. **HTTP and file proof:** add streaming HTTP response consumption and binary file read behind new interfaces; preserve legacy plugin output adapters.
5. **Optional-module proof:** migrate FTP/ZIP or Mail as a second independent provider to validate that the contract is not shaped around one implementation.
6. **Live stream proof:** add a synthetic source with deterministic demand, bounded queues, cancellation and terminal tests before integrating real media.
7. **Semantic envelope:** introduce versioned DataContractRef only after the finite payload and stream boundaries are stable enough to identify their actual needs.

Do not begin with a broker, object store, lakehouse, media server or AI framework selection. Those are implementation/provider decisions after neutral contracts and workload limits are measured.

## 8. Acceptance gates before implementation

- No dependency on XML, Guice, a specific IoC container, storage vendor, broker, media framework, AI framework or MCP in the neutral API.
- Small inline payloads and large streaming payloads both work without forcing all content into byte arrays.
- Payload descriptor is immutable and separate from active read/write leases.
- Resource ownership and worker-exit ordering are testable with the existing ResourceLedger.
- Backpressure and buffering are bounded and observable.
- Slow consumers, cancellation, timeout, provider failure and runtime shutdown have deterministic tests.
- Multi-subscriber fan-out and overflow policy are explicit.
- Source/capture/PTS/monotonic/ingestion/processing time are not conflated.
- Legacy Variable adapters preserve current behavior until explicit migration decisions are approved.
- A second independent provider validates that the API is not HTTP-specific.
- AI-derived data retains provenance and is distinguishable from original source data.

## 9. Open decisions

1. Whether `PayloadRef` is always immutable and externally addressable, or whether short-lived inline references may be session-local.
2. Whether the first reader API should expose InputStream, ReadableByteChannel or both.
3. Whether a generic Flow-based stream API is sufficient for all source types, or whether pull-based bounded readers are also required.
4. How per-subscriber fan-out buffers are budgeted and what default overflow policy is safe.
5. Whether artifact write/commit belongs in a separate optional provider SPI rather than the neutral Core contract.
6. Whether semantic schema resolution/validation is a host service, a provider capability or an external catalog integration.
7. How ResourceLedger generation tombstones and runtime-wide drain/diagnostics are handled before long-lived production use.

No public API names, signatures, dependencies or implementation decisions are approved by this proposal.
