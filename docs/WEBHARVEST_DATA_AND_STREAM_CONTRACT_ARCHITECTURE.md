# WebHarvest Data, Stream and Semantic Contract Architecture

**Status:** Architecture proposal for review; no new public SPI or runtime implementation is authorized by this document.
**Scope:** WebHarvest Core/runtime, plugin contracts, data acquisition and integration, portable definitions, agent composition and optional MCP adapters.
**Out of scope:** Designing internal data models or product architecture of consuming applications.
**Source of direction:** [Project Direction](PROJECT_DIRECTION.md).
**Implementation evidence and next-step proposal:** [Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md), [Variable/Event/Payload Call-Site Audit](WEBHARVEST_VARIABLE_EVENT_PAYLOAD_CALLSITE_AUDIT.md), [Resource Ownership Integration Audit](WEBHARVEST_RESOURCE_OWNERSHIP_INTEGRATION_AUDIT.md), [Minimal Payload and Stream API Design](WEBHARVEST_MINIMAL_PAYLOAD_STREAM_API_DESIGN.md). The API design is a draft and is not approved for implementation.

## 1. Purpose and product boundary

WebHarvest is an adaptive data acquisition and integration platform. It must acquire, understand, validate, process, enrich, transform and deliver heterogeneous data through specialized capabilities.

Its semantic Core must describe the meaning and lifecycle of data operations without becoming:
- a universal business ontology;
- a mandatory storage engine or data lake;
- a transport protocol or message broker;
- a video/audio codec framework;
- an AI model runtime;
- a drone/robot control system;
- a workflow notation or MCP server.

The platform must work with these through explicit contracts and optional providers without requiring them in every deployment.

The central question is not "what is the universal WebHarvest record?" It is:

> What minimum semantic contracts must WebHarvest understand to compose, validate, execute, observe and safely connect data operations across heterogeneous representations?

## 2. Five distinct contract layers

Do not collapse these layers into one descriptor or schema.

### 2.1 Semantic data contract

Defines the meaning and constraints of data crossing an operation boundary. It may describe a structured record/document, observation, event/state transition, binary/media artifact, stream/segment, external content reference or derived result/annotation.

A semantic contract is independent of Java classes, Plugin IDs, XML QNames, transport envelopes, database tables and serialization formats.

### 2.2 Capability contract

Defines an observable operation a provider can perform: acquire, read, parse, decode, extract, validate, transform, enrich, correlate, publish or deliver.

It declares typed inputs, outputs, side effects, preconditions, constraints, failure behavior and relevant resource/security requirements. It references semantic data contracts; it does not redefine their meaning.

### 2.3 Plugin/provider contract

Defines the executable implementation that provides one or more Capabilities. It covers stable provider identity, compatibility, configuration, dependency requirements, construction, lifecycle, diagnostics and resource ownership.

A Plugin is not a data type and a Plugin ID is not a semantic contract ID.

### 2.4 Representation/transport contract

Defines how information is encoded or carried: XML, JSON, CSV, database rows, HTTP, files, object references, message frames, media containers, stream protocols or MCP tool arguments.

Representation contracts map to semantic contracts. They are not semantic authorities.

### 2.5 Execution/lineage contract

Defines operational facts needed to explain what WebHarvest did: source, run, attempt, operation, provider version, input/output references, transformation, validation, timing, partial outcome and delivery receipt.

Execution lineage is not a substitute for domain data. A workflow step called http or xpath does not define the meaning of the business data it processes.

## 3. Core data model: distinct semantic shapes

WebHarvest should support a small number of distinct data shapes. These are conceptual categories, not a proposal to force all payloads into one Java class.

### 3.1 Record/document

A finite structured or semi-structured value with a declared contract or an explicitly unknown/opaque representation.

Requirements:
- contract identity/version when known;
- content/media type and representation metadata;
- schema/validation outcome distinct from the payload;
- source and provenance references where applicable;
- support for unknown fields/extensions under an explicit compatibility policy.

### 3.2 Observation

A statement that a value or condition was observed, measured, estimated or inferred at a point or over an interval.

Requirements depend on domain but may include subject/target identity; property or quantity identity; value and unit/type; observation time and separately ingestion time; source/method; quality, uncertainty and status; measurement location/context.

Core must not invent universal meanings for domain quantities. It should provide typed contract and validation mechanisms so domain-owned observation schemas can be consumed safely.

### 3.3 Event

A statement that something happened or that a state transition occurred.

An event contract must distinguish:
- event identity from delivery/message identity;
- event time from processing/ingestion time;
- producer/source identity;
- subject and relevant correlation/causation references;
- event type and version;
- payload contract;
- duplicate/replay and ordering semantics where required.

Do not assume exactly-once delivery. A transport's delivery guarantee is not a semantic event guarantee.

### 3.4 Artifact

A finite retained object or externally addressable content item: file, document, image, audio/video recording, archive, model output or binary payload.

A semantic artifact reference should be separable from its physical storage location. Where applicable it needs stable artifact identity; media/content type and size; integrity digest; source and capture/creation metadata; retention/access classification; version or immutability semantics; and one or more resolvable locations/retrieval methods.

Large binary content must not be copied into generic workflow variables, plugin configuration maps or ordinary event envelopes merely for convenience.

### 3.5 Stream

A potentially unbounded or long-lived sequence of typed data units, with explicit temporal and flow-control semantics.

A stream contract must define:
- element/data contract;
- source and stream/session identity;
- ordering guarantees and sequence semantics;
- event-time and ingestion-time behavior;
- bounded buffering and backpressure;
- cancellation and terminal conditions;
- reconnect/resume behavior, if supported;
- gap, duplicate, late and out-of-order handling;
- checkpoint/offset semantics only where the source supports them.

A stream is not just a collection that happens to arrive slowly. Nor is it necessarily a durable event log.

### 3.6 Derived result/annotation

A value produced by parsing, extraction, transformation, inference, classification, correlation or human review.

It should retain, where relevant: output contract identity/version; references to source artifacts/records/stream segments; operation/provider/model version; transformation parameters or definition version needed for reproducibility; confidence/quality semantics; validation and review state; links to supporting evidence.

Derived results must not silently overwrite source evidence.

## 4. Media and real-time acquisition

Audio/video and sensor streams should be first-class acquisition targets without making media processing mandatory in Core.

Separate these concerns:

1. **Capture/source connector** — connects to a camera, microphone, RTSP/WebRTC/API/file source or device gateway.
2. **Transport/session** — maintains a connection/session and exposes availability, timestamps, sequence and flow control.
3. **Demultiplexing/decoding** — extracts elementary audio/video/data tracks and frames/samples.
4. **Segmentation** — creates bounded time windows or independently addressable chunks.
5. **Interpretation** — detection, transcription, OCR, classification, embedding, summarization or other analysis.
6. **Correlation** — aligns media with device telemetry, location, events or other streams.
7. **Persistence/delivery** — stores or forwards raw media, segments, derived results or references.

These may be separate Capabilities and Providers. Do not require one giant video plugin to own every stage.

### 4.1 Time model

Media and device streams frequently have more than one clock. Preserve distinctions among capture/source clock; media presentation timestamp; device monotonic clock; synchronized wall-clock time; ingestion/receipt time; processing/inference time.

Record clock source and synchronization quality when material. A timestamp without clock semantics may be insufficient for joining video with telemetry.

### 4.2 Payload and resource model

A live stream may produce high-volume data continuously. The runtime needs explicit resource budgets and lifecycle ownership for network sockets/sessions; decoder processes/native buffers; frame/sample queues; temporary segment storage; GPU/accelerator resources when used; downstream consumers and slow subscribers.

Backpressure must be explicit. A slow AI consumer must not cause unbounded memory growth. The contract must define whether the source is paused, data is dropped, sampled, spooled or the operation fails.

### 4.3 Core versus optional media modules

Core should own only format-neutral stream/session/data-flow abstractions and lifecycle contracts after those are proven by use cases.

Codec libraries, RTSP/WebRTC clients, camera vendors, GPU runtimes, speech recognition and computer-vision engines belong in optional modules/providers. Their classpath presence must not activate them.

## 5. AI and agent integration

AI is a consumer/provider of Capabilities, not a separate semantic universe inside Core.

An inference provider may implement Capabilities for classification/extraction; transcription/OCR; object/event detection; entity resolution; summarization/enrichment; embeddings/similarity search; planning/proposal generation.

The provider contract must declare model identity/version, input/output contracts, execution requirements, resource profile, deterministic/non-deterministic behavior and relevant data-handling constraints.

AI results are untrusted derived data. Preserve source evidence, model provenance, output confidence semantics and review status. Do not treat model output as authorization, policy or proof of a physical action.

### 5.1 Agent composition

An agent may inspect available Capability descriptors and propose a composition or modify a portable definition. It must not bypass contract/type compatibility; provider trust and activation policy; authorization and secret boundaries; resource/concurrency budgets; execution lifecycle and generation pinning; validation and audit requirements.

An accepted definition is a versioned artifact. Agent proposals are not active runtime mutations. Apply accepted changes only at a defined safe execution boundary.

### 5.2 MCP boundary

MCP is an optional protocol adapter that exposes or consumes selected Capabilities. MCP tool names and JSON argument schemas are projections of the same authoritative Capability contracts.

MCP must not become the canonical workflow definition, a second plugin registry, the authorization authority, a mandatory Core dependency or the semantic definition of data exchanged by tools.

Direct Java/API, CLI, IDE and non-MCP integrations must remain possible.

## 6. Capability families for WebHarvest

These are functional categories from the product direction, not a finalized public catalog.

| Family | Typical responsibility | Example contract boundaries |
|---|---|---|
| Acquire | Connect to and read from a source | Source identity, acquisition request, record/artifact/stream output |
| Understand | Parse, decode, classify and extract | Input representation, output semantic contract, evidence references |
| Process | Validate, normalize, transform, enrich, correlate and deduplicate | Input/output contracts, transformation identity, loss/quality report |
| Integrate | Write, publish, synchronize and deliver | Destination contract, delivery attempt, receipt/partial outcome |
| Observe | Expose provenance, quality, diagnostics and execution evidence | Lineage, validation report, quality assessment, execution outcome |
| Coordinate | Compose and execute bounded operations | Definition, dependency/capability requirements, execution policy and lifecycle |

A single Plugin may implement multiple Capabilities, but do not merge unrelated responsibilities solely to reduce plugin count. A Capability may have multiple provider implementations.

## 7. Data flow and execution boundaries

A target data path should be expressible without binding it to XML or a particular storage engine:

    Source
      -> Acquire Capability
      -> Record / Observation / Event / Artifact / Stream
      -> Understand Capability (optional)
      -> Process Capability (zero or more)
      -> Validate / Quality assessment
      -> Integrate Capability
      -> Destination

At each boundary:
- declare input and output contracts;
- distinguish streaming from materialized/bounded input;
- preserve relevant provenance and source references;
- report partial success and loss explicitly;
- enforce data-handling policy and resource budgets;
- make retries and duplicate behavior explicit;
- avoid implicit conversions that change meaning.

This is a conceptual data path, not a mandate that every flow use all stages or a fixed linear pipeline.

## 8. Semantic compatibility and negotiation

Compatibility must be checked at the contract boundary, not inferred from matching field names or provider metadata.

A consumer/provider match should consider semantic contract identity and supported versions; required versus optional fields/values; units, cardinality, nullability and temporal semantics; stream ordering, delivery and backpressure guarantees; representation adapters and conversion loss; security/data classification and permissions; runtime resource and lifecycle requirements.

A provider may be technically loadable but semantically incompatible. A schema may parse successfully while its meaning is wrong. These are distinct failure classes and should produce distinct diagnostics.

Unknown extensions may be preserved when safe. Unknown values that affect security, authorization, state transitions, units or required execution behavior must fail closed.

## 9. What belongs in Core

### Candidate Core responsibilities

- stable, format-neutral contract identity and version references;
- typed input/output and contract compatibility abstractions;
- data-shape distinctions needed by execution (record, event, artifact, stream/reference);
- bounded streaming/backpressure and cancellation contracts, if justified by runtime use cases;
- provenance/lineage primitives for execution and transformations;
- validation outcomes and structured diagnostics;
- capability resolution, provider trust/activation and generation pinning;
- resource ownership, budgets and lifecycle integration;
- portable semantic definitions independent of XML, MCP and IDE.

### Explicitly not Core responsibilities

- domain vocabularies for energy, finance, agriculture, geospatial, robotics or healthcare;
- vendor-specific source/destination models;
- every serialization format or protocol client;
- codec, computer-vision, speech, LLM or embedding implementations;
- broker, lakehouse, database or object-store implementation;
- UI/IDE presentation and localized labels;
- MCP server/client implementation;
- flight-control or robot actuation safety logic.

Core may define extension points that optional modules implement. An extension point does not imply the corresponding technology belongs in Core.

## 10. XML, portable definitions and registry implications

XML remains a compatibility representation. Its elements and attributes must map to semantic operations, Capability requirements and typed configuration contracts. XML local names are not Capability IDs.

The same semantic definition must be usable from existing XML configurations through a compatibility adapter; Java/API composition; a future portable DSL; IDE authoring and inspection; agent-proposed definitions; optional MCP-facing operations.

The runtime registry generation must publish enough information to validate and resolve Plugin/provider identity and version; provided Capabilities and versions; input/output semantic contract references; representation bindings (including XML QNames and aliases); configuration contracts; dependencies, trust and resource requirements; construction and lifecycle ownership.

Metadata/IDE/MCP projections must derive from the same authoritative generation and contract definitions. They must not scan and invent a second semantic universe.

## 11. Current implementation evidence

The source-backed baseline is recorded separately in [WebHarvest Data Contract Runtime Audit](WEBHARVEST_DATA_CONTRACT_RUNTIME_AUDIT.md). In brief, current Variable wrappers are conversion-oriented rather than typed semantic contracts; PluginConfiguration uses string-valued properties; session lifecycle events are not evidence of a durable domain event log; StreamingXmlParser is an XML parser, not a general stream-processing API; and no dedicated media/MCP implementation paths were found in the inspected main tree. These findings constrain the target design but do not authorize a compatibility-breaking rewrite.

## 12. Evidence gaps before implementation

The following are open architecture questions, not assumed current capabilities:

1. **Streaming execution:** identify current synchronous/materializing paths and determine where a stream contract can be introduced without changing established plugin semantics.
2. **Payload representation:** inspect Variable, plugin inputs/outputs and PluginConfiguration to identify where large/binary/typed payloads are currently forced into string/tree representations.
3. **Lineage:** inventory current execution events, source references, transformation identity and partial-result reporting; distinguish existing evidence from proposed additions.
4. **Resource lifecycle:** map stream sessions, buffers, decoders, external processes and provider resources onto runtime ownership and generation retirement.
5. **Capability descriptors:** determine the minimum machine-readable declaration required for type checking, provider resolution and agent composition.
6. **XML compatibility:** map existing XML elements and nested declarative nodes to semantic operations/configuration, retaining namespace-aware identity.
7. **MCP:** inspect the current MCP module/tool surface (if present) and classify it as adapter, experimental integration or absent; do not infer a Core API from tool names.
8. **AI providers:** define only provider-neutral inference input/output and provenance boundaries; keep model frameworks optional.
9. **Backpressure and failure:** specify bounded queue behavior, drop/spool/retry policies, partial outcomes and cancellation acknowledgement.
10. **Security:** classify untrusted payloads, prompt-injection-bearing content, executable scripts, external references and data exfiltration boundaries.

Each finding must cite source paths, call sites and tests, and be classified FACT, INFERENCE, UNKNOWN or PROPOSAL. No implementation should begin merely because a conceptual contract is described here.

## 13. Proposed architecture decisions

1. WebHarvest Core models data operations and the minimum neutral semantics needed to compose them; it does not own every domain ontology.
2. Semantic data contracts, Capability contracts, Plugin/provider descriptors, representation contracts and execution lineage remain separate.
3. Record/document, observation, event, artifact, stream and derived result are distinct semantic shapes; no universal untyped envelope.
4. Media, AI, broker, storage, codec and device integrations are optional providers/modules unless a demonstrated minimal neutral contract belongs in Core.
5. Streaming must be bounded and lifecycle-aware; it is not implemented by wrapping an unbounded queue around existing synchronous plugins.
6. Agent and MCP interfaces are adapters over the same contracts; neither is an authority for semantics, trust or authorization.
7. XML compatibility, Java/API, IDE, DSL and MCP must converge on one semantic validation and runtime resolution model.
8. Implementation follows source inventory, contract characterization, API review, migration gates and tests.

These decisions are target architecture proposals until accepted through the repository's architecture and release gates.
