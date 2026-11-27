# WebHarvest Data Contract Runtime Audit

**Status:** Source audit; findings are limited to the inspected repository tree and commit below.
**Inspected revision:** `main` at `c8830f0fe2f1e283abea95318d2c6fd9a644d0a4`.
**Related target design:** [WebHarvest Data, Stream and Semantic Contract Architecture](WEBHARVEST_DATA_AND_STREAM_CONTRACT_ARCHITECTURE.md).
**Method:** Targeted source inspection of Core data wrappers, plugin configuration, session events and repository paths. This is not a full external-consumer, runtime behavior or serialized-data compatibility audit.

## 1. Executive assessment

The current Core runtime does **not** yet expose a general typed semantic data contract model or a first-class media/stream data model.

The inspected implementation has:
- a legacy `Variable` abstraction optimized for conversion between wrapped Java objects, strings, bytes and lists;
- plugin configuration represented as string-valued properties plus nested configuration nodes;
- plugin/session lifecycle events that expose execution metadata and, for successful plugin completion, the returned `Variable`;
- a SAX-based streaming XML parser, which streams XML parsing but is not a general-purpose data-stream abstraction;
- Stage 1 semantic metadata focused on XML structure and plugin metadata, not format-neutral data contracts.

No MCP source paths or dedicated audio/video, codec, RTSP/WebRTC, media segment or general stream-contract implementation paths were found in the inspected `main` repository tree. This does not rule out external integrations or unpublished branches; it means they are not evidenced as current in-repository Core functionality.

**Architectural consequence:** Do not stretch `Variable`, `PluginConfiguration`, XML metadata or session events into the canonical semantic contract layer. Preserve them as legacy/runtime compatibility surfaces and design new typed contracts alongside them, with explicit adapters and migration evidence.

## 2. Source findings

### 2.1 Variable is a conversion wrapper, not a typed semantic value

**FACT —** `webharvest-core/src/main/java/org/webharvest/runtime/variables/Variable.java`

The abstract API centers on:
- `toBinary()` and `toBinary(charset)`;
- `toString()` and `toString(charset)`;
- `toList()`, `toIterator()` and `isEmpty()`;
- `getWrappedObject()`;
- convenience conversions to boolean, int, long, double and array.

There is no declared semantic contract ID/version, unit, quantity, timestamp, provenance, quality, sensitivity, media type, stream lifecycle or schema-validation result in this abstraction.

The convenience numeric conversions map a null wrapped value to zero; boolean conversion maps null to false. These are legacy conversion semantics, not safe rules for typed domain data where missing, unknown, unavailable and zero/false must remain distinct.

**FACT —** `NodeVariable` stores an arbitrary `Object`. It accepts byte arrays, character data, XML wrappers, iterables and other objects through the wrapped value. String/binary conversion may serialize or stringify the wrapped object; this does not preserve a declared semantic type or contract.

**FACT —** `ListVariable` stores a mutable `ArrayList<Variable>`. Its constructor skips empty values, `addVariable` mutates the list, `toList()` returns the backing list, and `getList()` exposes the backing collection. Its default string representation joins non-empty child string values with newline separators. That representation is lossy for structured values and does not encode a general nested data contract.

**INFERENCE —** The current Variable family is a compatibility-oriented execution value abstraction. It should not be made the canonical data model by adding a few metadata fields: its conversion, mutability, flattening and empty-value semantics are materially different from typed records, observations, events, artifacts and streams.

### 2.2 Plugin configuration is string-valued

**FACT —** `webharvest-core/src/main/java/org/webharvest/plugin/PluginConfiguration.java`

The interface exposes configuration properties as strings and offers all-properties access through `Map<String, String>` and `Properties`, plus nested plugin configuration access.

**FACT —** `DefaultPluginConfiguration.java` stores properties in `Map<String, String>`; constructors copy incoming maps. `getAllProperties()` and `getProperties()` return copies. Its `toString()` includes the properties map.

**INFERENCE —** This is suitable as a legacy XML/plugin initialization representation but cannot alone represent typed input/output schemas, large payloads, artifact references, stream handles, secrets with controlled resolution, or contract identity/version. It must remain distinct from runtime data values and semantic contract descriptors.

### 2.3 Session events are execution telemetry, not a durable semantic event model

**FACT —** `PluginExecutionStartedEvent` carries session identity, plugin name and plugin class.

**FACT —** `PluginExecutionCompletedEvent` carries plugin name/class, execution duration and the raw returned `Variable`.

**FACT —** `PluginExecutionFailedEvent` carries plugin name/class, execution duration and a `Throwable`.

**FACT —** `SessionEvent` assigns `Instant.now()` when the event object is constructed.

**INFERENCE —** These are useful runtime lifecycle/diagnostic events, but they do not establish a general durable event contract with independent event identity, source event time, ingestion time, schema version, replay/ordering guarantees, payload reference, provenance graph or delivery semantics.

The event class documentation uses event-sourcing terminology and describes replay use cases. That documentation alone is not evidence of durable event storage, complete replay guarantees or a persistent event log. Those require separate inspection of the event bus, subscribers, persistence and tests.

**RISK TO CHARACTERIZE —** Successful completion events expose the plugin result Variable directly. Consumers/recorders may retain or inspect potentially large or sensitive values. This needs a consumer and logging audit before any change; do not assume every deployment persists or logs these values.

### 2.4 Streaming XML parsing is not general stream processing

**FACT —** The Core source contains `StreamingXmlParser` and corresponding parser tests.

**FACT —** Repository path inspection found no dedicated Core data-stream API, stream session abstraction, backpressure contract, media segment model, audio/video decoder or media connector implementation on inspected `main`.

**INFERENCE —** The word "streaming" in `StreamingXmlParser` refers to incremental XML parsing. It must not be used as evidence that WebHarvest currently supports continuous acquisition/processing streams, bounded subscriber flow control or media stream lifecycle.

### 2.5 Stage 1 semantic model is not a data contract registry

**FACT —** The current Stage 1 semantic package contains XML document/element/content structures, qualified names, plugin descriptor/index structures and structural validation types.

**FACT —** The existing Stage 1 contract explicitly limits its purpose to semantic XML inspection and plugin/XML metadata. It is not the final canonical format-neutral semantic runtime.

**INFERENCE —** Do not expand Stage 1 by treating XML element descriptors as data schemas or Capabilities. The future data contract model must be independently identified and then projected into XML/IDE/agent surfaces where relevant.

### 2.6 MCP and media implementation inventory

**FACT —** A recursive path inventory of the inspected `main` tree returned no paths matching MCP implementation names and no dedicated media/audio/video/camera/codec/RTSP/WebRTC source paths.

**FACT —** The same inventory found only `StreamingXmlParser` and its tests for stream-related names in Core.

**BOUNDARY —** This is repository-path evidence, not proof that no external MCP server, private integration, experimental branch or third-party media provider exists. Current Core support must be described as unverified/absent from inspected `main`, not universally impossible.

## 3. What the current runtime can and cannot claim

| Concern | Evidence-backed current state | Not established by this audit |
|---|---|---|
| Runtime values | Variable wrappers for object/string/bytes/list conversion | Typed, versioned semantic data contracts |
| Binary values | Byte-array support through wrapped objects and conversion methods | Managed immutable artifact references, integrity, retention and access policy |
| Structured values | XML-related wrappers and arbitrary wrapped Java objects | Portable record/document schema contract |
| Missing values | Empty/null conversion behavior in legacy wrappers | Universal distinction among unknown, missing, unavailable, stale and invalid |
| Plugin configuration | String property map and nested plugin config | Typed operation I/O schema or large payload transport |
| Runtime events | Plugin start/completion/failure and session lifecycle event classes | Durable event log, replay guarantee, domain event schema or delivery semantics |
| XML parser | Incremental XML parsing | General continuous stream processing/backpressure |
| Media | No dedicated media paths found in inspected tree | Audio/video capture, decode, segmentation and correlation |
| AI/MCP | No MCP source path found in inspected tree | Core agent API, inference provider SPI or MCP adapter |
| Provenance | Some execution/session/plugin metadata exists | End-to-end data lineage across source, transformation and destination |

## 4. Architecture implications

1. **Keep Variable as a legacy execution abstraction.** Add a separate typed contract/data boundary rather than turning Variable into a universal envelope.
2. **Separate payload from metadata.** Large binary/media data should be represented by bounded streams or managed artifact references, not forced through string conversion or configuration properties.
3. **Separate domain events from runtime events.** Plugin lifecycle notifications may reference data contracts, but they are not themselves the domain event model.
4. **Separate XML grammar from semantic data schemas.** XML remains a compatibility representation and must map to the same semantic operation/contract model as future APIs and DSLs.
5. **Treat streaming as a lifecycle contract.** It needs bounded demand/buffering, cancellation acknowledgement, terminal outcomes and resource leases; an unbounded queue or iterator is not sufficient.
6. **Keep media/AI/MCP providers optional.** Core should provide only proven neutral contracts and lifecycle support.
7. **Preserve source evidence.** Parsing, extraction and inference should create derived outputs with references to original inputs and transformation/provider versions, not silently replace raw source material.

## 5. Required follow-up audits

**Follow-up evidence added:** [Variable, Event and Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md) traces representative Core/Database paths for Variable conversion, HTTP/file/database payload materialization and both event-bus families. It establishes that the inspected session/plugin event path is in-process Guava EventBus dispatch, not a durable event store, and records the concrete DatabasePlugin subscriber that releases connections on session terminal events.

This advances (but does not close) items 1, 3 and 4 below. The repository-wide Variable consumer inventory, complete subscriber inventory, external consumer scope and full payload lifecycle/resource-budget audit remain open. GitHub code-search connector results were empty for known symbols and are not accepted as evidence of absence.

Before API design or implementation, complete these bounded inspections:

1. **Variable consumers:** map every Core plugin and public API use of Variable conversions, list flattening, wrapped objects and byte arrays; identify compatibility constraints and data-loss points.
2. **Plugin configuration consumers:** map config schema validation, property conversion, child-node semantics and configuration serialization/export.
3. **Event consumers:** inspect EventBus registration, event sinks/recorders, persistence, logging and tests; establish actual replay and retention behavior.
4. **Payload lifecycle:** trace file/binary/HTTP/database outputs and identify where data is copied, materialized, retained or streamed.
5. **Execution ownership:** map any proposed stream/session/artifact lease to the existing execution owner/resource ledger and generation retirement model.
6. **Semantic metadata:** verify the boundary between Stage 1 XML descriptors and future Capability/data contract declarations.
7. **External compatibility:** identify public consumers and serialized artifacts before changing Variable or PluginConfiguration contracts.

Each audit should produce exact call-site lists and tests. Do not claim exhaustiveness until the repository search and external consumer scope are documented.

## 6. Review conclusion

The proposed WebHarvest architecture is directionally compatible with the current Core only if it is introduced as a new, explicit contract layer with legacy adapters.

The current Variable, PluginConfiguration, session-event and XML metadata abstractions are not adequate as canonical contracts for heterogeneous records, real-time streams, media artifacts or AI-derived data. This is a source-backed design constraint, not a criticism of their original legacy purposes.

No production code should change until the follow-up audits establish the compatibility surface and a minimal API can be tested independently of XML, Guice, a specific transport, media framework or AI provider.
