# Cross-Application Semantic Contract Inventory

**Status:** Evidence-led candidate inventory; no canonical schemas or identifiers are approved here.
**Audience:** WebHarvest, Windy Farm, FinanceFlow/Bill Capture and future data-producing or data-consuming applications.
**Purpose:** Identify real interoperability boundaries before selecting schema formats, namespaces, registries or implementation libraries.
**Non-goal:** Defining a universal ontology, prescribing one database/event format, or claiming that the listed integrations already exist.

## 1. How to read this inventory

Each candidate is classified as:

- **Established product context** — a product or workflow already described. This confirms the use case, not a finished shared schema.
- **Target interoperability context** — a required direction for future systems, explicitly including AI, drones, robots and audio/video streams. This is a design input, not evidence of a deployed producer or consumer.
- **Candidate contract** — a proposed semantic boundary to validate against concrete records, APIs and workflows before standardization.

The inventory deliberately avoids assigning canonical IDs or schema versions. Those require an ownership and namespace decision first.

## 2. Candidate contract inventory

### 2.1 Spatial features and places

**Status:** Established product context (Windy Farm land module); broader robotics use is a target interoperability context.

Potential concepts: managed site/place; spatial feature and geometry; boundary, point, route or area of interest; physical structure/location; spatial observation.

Semantic questions:
- Separate real-world place identity from a particular geometry revision.
- Preserve coordinate reference system, axis order, units, precision and geometry type.
- Define whether a boundary is legally authoritative, surveyed, approximate or user-drawn.
- Represent validity time separately from the time the geometry was recorded.
- Preserve source, method, accuracy and uncertainty; do not imply survey-grade precision for approximate coordinates.
- Define stable references without assuming a fixed farm/property hierarchy.

A map layer, UI marker, GeoJSON feature or database row is not itself the semantic identity of a place or spatial feature.

### 2.2 Energy assets and observations

**Status:** Established product context (Windy Farm Energy, PV and storage monitoring); likely cross-application contract family.

Potential concepts: energy asset (battery, inverter, PV array, charge controller, meter, grid connection, generator); system topology; measured or estimated observation; operating state and transition; energy flow or accounting interval; asset configuration/commissioning record.

An observation must not be reduced to an unqualified number. Where applicable it needs:
- physical quantity and unit (power is not energy);
- measurement point and direction/sign convention;
- instantaneous sample versus interval aggregate;
- observed, calculated, estimated or commanded origin;
- measurement/event time, ingestion time and clock quality;
- quality, validity, stale/unavailable status and uncertainty;
- source device, channel and calibration/configuration context.

A vendor Adapter may translate Victron or another manufacturer's representation. Vendor field names, register numbers and enum values must not become canonical domain identities. The same energy observation may be consumed by a farm application, acquisition flow, analytics service or AI model without shared storage or runtime.

### 2.3 Inventory, stock and storage locations

**Status:** Established product context (Windy Farm pantries, preserves and materials inventory).

Potential concepts: item definition; stock lot/batch; derived stock balance; storage location and containment; stock movement (receipt, consumption, transfer, loss, adjustment); preservation/production batch; expiry or review date.

Important distinctions:
- Item identity is not lot identity.
- Remaining lot quantity is not nominal package size.
- Expiry, best-before, production date and opened-on date are different meanings.
- A stock movement is an event; current balance is a projection that can be rebuilt or reconciled.
- Units must distinguish count, mass, volume, length and package-specific quantities.
- A pantry workflow and building-materials workflow need not share every attribute or business rule.

### 2.4 Financial and source documents

**Status:** Established product context (FinanceFlow/Bill Capture; offline OCR and invoice/receipt archive).

Potential concepts: source document artifact; classification and extraction result; issuer, recipient and other parties; invoice/receipt identity and lifecycle; line item, tax component, total and currency; payment obligation/event; service period or meter reading referenced by a utility invoice; evidence linking extracted values to source pages, regions or text spans.

Critical boundaries:
- Source artifact is not the extracted business document.
- OCR output is not automatically a verified fact.
- An extracted field retains confidence/review state and a link to evidence.
- Currency, tax treatment, gross/net meaning, decimal conventions and locale must be explicit.
- Invoice date, issue date, service period, due date and payment date are distinct.
- KSeF, utility-provider formats and OCR engine output are external representations with explicit versioned mappings, not universal invoice semantics.

### 2.5 Media assets, streams and segments

**Status:** Target interoperability context explicitly requested for video/audio acquisition and use by AI, drones and robots.

Potential concepts: immutable/versioned media asset; continuous or time-bounded media stream; stream session/source endpoint; media segment/chunk; frame or audio sample window; encoding/codec properties; synchronization relationship to telemetry or other observations.

A stream is not the same thing as a file. Distinguish:
- source capture time, stream presentation time and ingestion/processing time;
- wall-clock timestamps versus monotonic device clocks;
- sequence number, dropped data and discontinuity;
- codec/container/format from semantic content;
- live stream reference from retained media artifact;
- segment identity/order from transport packet identity;
- audio/video synchronization and clock provenance.

Not every consumer must persist raw media. It may retain a reference, selected segments, derived features or event summary if retention and provenance semantics are explicit. Large media payloads should not be forced into ordinary event envelopes or plugin configuration values; describe payload references, access constraints, integrity metadata and lifecycle separately from transport.

### 2.6 AI-derived observations and annotations

**Status:** Target interoperability context explicitly requested; candidate family, not a claim of existing shared AI infrastructure.

Potential concepts: inference/analysis run; model identity/version/configuration; input references; output annotation/classification/detection/transcription/summary/prediction; confidence/score; human review/correction/acceptance; derived observation linked to source evidence.

Provenance questions:
- Which model/version produced the result?
- Which input artifacts/segments and time ranges were used?
- Which preprocessing, prompt, parameters or transformation version materially affected it?
- When did the source event occur and when did inference run?
- Is the output a detection, hypothesis, estimate, transcription or verified fact?
- What does the score mean, and is it calibrated/comparable across models?
- Was a person involved in review or correction?

AI output must not silently overwrite source observations or become verified truth merely because a model emitted it. Preserve raw evidence, machine-derived interpretation and human-validated fact as distinct states.

### 2.7 Devices, robots, missions and commands

**Status:** Target interoperability context explicitly requested for drones and robots; requires separate safety and authority analysis before standardizing actuation.

Potential concepts: device/robot identity and capability profile; telemetry and device health; mission/task definition and execution record; route reference and geospatial constraints; command request, authorization, acknowledgement and execution result; operational mode/interlock state; fault, alert and incident event.

Telemetry, desired state, command request and confirmed physical state are not equivalent.

Any future command contract must distinguish:
- requested, accepted, dispatched and completed action;
- command issuer, authorization scope and policy decision;
- target identity and preconditions;
- expiry, idempotency/replay handling and correlation identity;
- acknowledgement, execution result, failure and cancellation;
- observed physical state after command;
- safety interlocks and loss-of-communications behavior.

This inventory does not authorize remote actuation or define a safe flight/robotics protocol. Safety-critical command semantics need domain-specific review; do not infer them from a generic Capability abstraction.

### 2.8 Acquisition, transformation and lineage

**Status:** Established WebHarvest product direction; relevant to all data families above.

Potential concepts: acquisition source and identity; acquisition attempt/session/result; raw artifact/source record reference; transformation/mapping activity and version; derived artifact/record and parent inputs; validation outcome/quality assessment; delivery attempt and destination receipt.

This is operational lineage, not a replacement for domain semantics. A WebHarvest execution event must not become the canonical meaning of an energy observation, invoice or robot telemetry record. The lineage model should support partial success, retries, duplicate delivery, replay and transformations producing multiple outputs from one input.

## 3. Cross-cutting semantic requirements

These recur across candidate families but should be reusable patterns only when their semantics genuinely match.

### 3.1 Identity and references
- Separate stable real-world/domain identity from source-system IDs, database keys and transport IDs.
- Preserve source identifiers as qualified references with source-system identity.
- Define whether identity is globally unique, owner-scoped or unique only within a source.
- Never use display labels, array positions, file paths or vendor register names as canonical identity.

### 3.2 Time
Do not collapse all timestamps into one generic `timestamp`. Distinguish, where relevant: event/observation time; source capture time; ingestion/receipt time; processing/inference time; valid-from/valid-to; device monotonic time and wall-clock synchronization quality. Intervals must define endpoint inclusivity, timezone handling and aggregation semantics.

### 3.3 Units, quantity and uncertainty
A numeric measurement needs semantic quantity and unit. Conversion must be explicit and dimensionally valid. Preserve precision, resolution, uncertainty and method when material. Missing, unknown, not applicable, unavailable, stale and invalid are not interchangeable.

### 3.4 Provenance and derivation
A value may be observed, imported, calculated, estimated, inferred, manually entered or corrected. Preserve the distinction and material transformation chain. Provenance should be proportional: avoid heavyweight lineage for trivial fields, but do not discard evidence needed to interpret or audit derived records.

### 3.5 Quality and confidence
A generic numeric `confidence` field is insufficient. Define producer, scale, meaning and calibration context. Keep measurement quality, OCR confidence, model confidence and human review status distinct.

### 3.6 Unknown extensions and forward compatibility
Preserve unknown optional fields/extensions when safe, but reject unknown values when they affect safety, authorization, units, state transitions or required behavior. Forward compatibility must never mean silently guessing semantics.

## 4. Proposed ownership and governance model

The evidence does not justify one central authority owning every domain vocabulary. Adopt a federated model:

1. **Domain steward:** accountable for meaning, examples, constraints and evolution of a domain contract family.
2. **Cross-domain contract review:** required when a contract is intentionally shared across domains/applications or changes a common semantic primitive.
3. **Application owner:** maps its internal model to the shared contract and declares supported subsets/versions; it does not unilaterally redefine shared meaning.
4. **Adapter/provider owner:** maintains explicit, tested mappings from source/vendor representations to the contract version it implements.
5. **Platform/Core owner:** supplies neutral mechanisms for identity resolution, schema validation, compatibility checks, provenance transport and contract discovery; it does not own every domain vocabulary.

A shared contract should have one accountable steward even when several applications consume it. Co-ownership without a decision process is not governance.

## 5. Proposed identity and versioning policy

These are recommendations for review, not adopted identifiers or release rules.

- Use globally unambiguous, domain-owned identifiers. Keep identifiers stable across language, implementation, vendor and storage changes.
- Keep contract identity separate from contract version, Capability identity, Pack membership, Plugin ID and Module version.
- Prefer immutable published contract versions. A consumer declares exact versions or a compatibility range it understands.
- Treat changes to meaning, units, cardinality, state transitions, required fields or operational guarantees as potentially breaking even if serialized shape is unchanged.
- Treat additive optional fields as compatible only when old consumers can safely ignore them and interpretation of existing data does not change.
- Never reuse an identifier for a different meaning. Deprecate and supersede instead.
- Publish explicit mapping/conversion rules; do not rely on name similarity or best-effort coercion.
- Do not select JSON Schema, Avro, Protobuf, AsyncAPI, RDF/OWL or another format as semantic authority. A schema technology expresses a contract; it does not establish its meaning or governance.

## 6. Capability relationship

A Capability is a functional promise; a semantic contract defines the meaning of its inputs, outputs and events.

Illustrative examples only:
- An energy-observation read Capability could require one or more Energy Observation contracts.
- A media-analysis Capability could consume Media Segment references and produce AI Annotation records.
- A document-acquisition Capability could produce Source Document artifacts, while extraction produces a versioned Extraction Result.
- A robot-telemetry Capability could publish telemetry observations; a command Capability requires separate authorization and safety contracts.

These are not approved IDs. A Capability may consume/produce multiple semantic contracts, and one semantic contract may be used by multiple Capabilities.

## 7. Application coverage matrix

| Application/context | Established or target responsibility | Candidate shared contract families | Important boundary |
|---|---|---|---|
| WebHarvest | Acquire, transform, validate and deliver data | Acquisition lineage; source artifacts; generic observations/events only when domain semantics are explicit | Workflow/plugin execution metadata is not domain meaning |
| Windy Farm | Land/spatial data, energy assets/observations, pantry/material stock | Spatial features; energy observations; inventory/stock; media/telemetry if adopted | UI, local persistence and vendor device models remain application/Adapter concerns |
| FinanceFlow / Bill Capture | Offline document capture, OCR, invoice/receipt archive and extracted values | Source document; extraction result; financial document and line-item concepts | OCR hypothesis is not verified financial fact |
| AI processing systems | Analyze media, documents and observations | AI inference run; annotation; derived observation; provenance | Model output must retain evidence and review status |
| Drone/robot systems | Capture media and telemetry; execute tasks; possibly accept commands | Media stream/segment; device telemetry; mission/task; command lifecycle | Actuation requires separate safety/authorization contract |
| Data lake / analytics | Retain, index, correlate and serve heterogeneous records | References, provenance, time, quality, schema identity and domain contracts above | Storage layout/query engine is not a semantic contract |

This is a coverage hypothesis. It does not claim that every application currently implements every listed contract.

## 8. What should not become a universal shared contract

Keep these concerns local or representation-specific unless a concrete interoperability case proves otherwise: UI labels/screens/navigation/preferences; database layouts/ORM entities/partition keys; Maven modules/packages/plugin classes/registry descriptors; XML element names/JSON property spelling/transport envelopes; vendor register maps/protocol packets/API wrappers; model-specific tensors/embeddings/private intermediate representations; application workflow steps/internal state machines; generic key/value bags that erase units, types, provenance or lifecycle.

A shared contract is justified by shared meaning and a real producer/consumer relationship, not by a desire to make every internal model look alike.

## 9. Evidence gaps and next validation work

Before publishing canonical schemas, collect representative examples and answer:

1. **Spatial:** current Windy Farm point/boundary representation, coordinate systems, source accuracy and geometry revision behavior.
2. **Energy:** actual Victron and meter payloads, channel/register meanings, sign conventions, sample/interval semantics and existing persistence fields.
3. **Inventory:** representative pantry/material records, batch/expiry rules, units, location hierarchy and stock movement behavior.
4. **Finance:** representative invoice/receipt fields, OCR evidence representation, currency/tax/date semantics and KSeF/provider mapping boundaries.
5. **Media:** target source types, stream protocols, timestamp/clock behavior, chunking, retention and references to stored segments.
6. **AI:** representative inference outputs, model/version provenance, confidence semantics, human review and links to source evidence.
7. **Robotics:** actual telemetry/mission/command examples and safety/authorization ownership; do not design command schemas from hypothetical fields.
8. **WebHarvest:** execution lineage available today versus what must be added, including retries, duplicate delivery and partial outcomes.

For each example, record producer, consumer, source format, current internal representation, semantic ambiguity, transformation and evidence of an actual exchange. Mark hypothetical relationships explicitly.

## 10. Decision gates

Do not move to schema-format selection or implementation until:
- at least one real producer/consumer exchange is documented for each family proposed as shared;
- each contract has a named steward and clear scope;
- domain identity is separated from vendor/source identity;
- units, time, missingness, quality and provenance semantics are explicit where applicable;
- consumer compatibility and unknown-field behavior are defined;
- mappings are testable with representative fixtures;
- safety-critical command semantics are separately reviewed;
- the proposed contract does not merely duplicate a local model without interoperability benefit.

## 11. Current conclusions

1. Strongest initial shared candidates: **Energy Observation**, **Spatial Feature**, **Inventory/Stock Movement**, **Source Document/Extraction Evidence**, and **Acquisition Lineage**, because they map to established product contexts.
2. **Media Stream/Segment**, **AI Annotation/Inference**, and **Device Telemetry/Mission/Command** are important target families for the AI, drone and robotics direction, but need concrete source/consumer evidence before standardization.
3. Provenance, time, identity, units and quality are cross-cutting requirements, not a reason to collapse all data into one generic record.
4. Semantic contracts should be domain-owned and independently versioned. Core should provide neutral mechanics, not own all domain semantics.
5. Capability contracts should reference semantic contracts; Packs curate domain functionality; Kits compose Capabilities for scenarios. None should be inferred from module packaging.

This inventory is a working basis for the next evidence-gathering pass. It does not approve any schema, namespace, identifier, API or implementation.
