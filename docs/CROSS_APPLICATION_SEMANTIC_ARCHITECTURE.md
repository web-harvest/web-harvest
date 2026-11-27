# Cross-Application Semantic and Functional Architecture

**Status:** Proposed cross-project architecture decision for review.
**Audience:** WebHarvest, Windy Farm and future applications that must exchange or interpret compatible data and functionality.
**Scope:** Shared semantic contracts, functional Packs, scenario Kits, runtime Capabilities, Adapters and technical Modules.
**Non-goal:** Defining a universal domain ontology, a new application framework, or an implementation API before the contracts and use cases are validated.

## 1. Purpose

Applications can share data and cooperate without sharing their implementation languages, frameworks, databases, deployment topology or runtime. They cannot do so reliably if the same terms, values, events or operations have different meanings in each application.

The architecture therefore separates three concerns:

1. **Semantic compatibility:** what a concept, datum, event or operation means.
2. **Functional organization and composition:** which coherent product areas and scenarios provide or combine user-visible functionality.
3. **Technical delivery and execution:** how implementations are packaged, discovered, activated, constructed and retired.

These concerns are related, but they are not interchangeable. In particular:

- A Maven module is not a Pack.
- A Pack is not a Kit.
- A Kit is not a plugin or deployment bundle.
- A plugin class is not a Capability identity.
- An Adapter does not define the meaning of the domain concept it connects.
- An XML element name is not a semantic identity.

This separation is a compatibility requirement, not merely a naming preference.

## 2. Architectural planes

### 2.1 Semantic plane — shared meaning

The semantic plane defines stable, versioned identities and contracts that independent applications can understand.

It includes, where applicable:

- canonical concept, entity, property, event, operation and Capability identifiers;
- semantic schemas, value types, units, constraints and relationships;
- identity and reference rules;
- state and lifecycle meanings;
- compatibility and schema-evolution rules;
- mappings from external/vendor/application representations;
- provenance and transformation information.

The semantic plane must not depend on a particular application, UI, database, XML dialect, Java class, plugin registry, Maven artifact or IoC container.

It must not become one enormous ontology. Shared contracts should be introduced where there is a real interoperability need, with domain vocabularies remaining independently scoped and composable.

### 2.2 Functional plane — Packs and Kits

The functional plane describes what a product offers and how capabilities are organized for people and use cases.

#### Pack

A **Pack** is a stable functional domain or value area: a coherent part of the user's world that an application understands and supports.

Examples include:

- Energy
- Water
- Security
- Resilience
- Inventory

A Pack can organize and curate:

- domain vocabulary, models and rules, while shared cross-application semantic contracts remain independently governed;
- user-facing capabilities and workflows;
- domain-specific UI and interaction;
- integration points and Adapter requirements;
- persistence needs and domain events;
- documentation and user-facing terminology.

A Pack is not required to be one code module, one JAR, one service, one repository or one independently deployable artifact. Its implementation may span multiple technical Modules and may use several Adapters.

A Pack should remain meaningful when a vendor, device, storage engine, UI framework or implementation language changes.

#### Kit

A **Kit** is a scenario-oriented composition or configuration: what a user wants to do, prepare for or achieve.

Examples include:

- Blackout Kit
- Water-continuity Kit
- Remote-site monitoring Kit

A Kit composes existing Capabilities, potentially from multiple Packs. It may add scenario-specific configuration, policies, thresholds, ordering, required resources and user guidance.

A Kit does not redefine or copy the canonical semantic contracts referenced by its constituent Packs. It references stable Capability and semantic contract identities.

A Kit is not automatically a new Pack, a plugin, a Maven module or a bundle of binaries. It may be represented as declarative configuration and resolved against the Capabilities available in a particular application/runtime.

#### Pack versus Kit

| Question | Pack | Kit |
|---|---|---|
| Primary meaning | Which stable functional area does the product support? | Which scenario or outcome is being composed? |
| Stability | Domain-oriented and relatively stable | Scenario/configuration-oriented; may evolve independently |
| Organizes | Domain vocabulary, models, rules and functional capabilities | Composition, scenario policy and configuration |
| Composition | Groups related functionality | References Capabilities from one or more Packs |
| Vendor dependence | Must be vendor-independent | Must not require a particular vendor unless explicitly constrained |
| Technical packaging | Independent of artifact boundaries | Independent of artifact boundaries |

A Pack may be useful without any Kit. A Kit may combine Capabilities from several Packs. Neither relationship requires a one-to-one mapping.

#### Semantic contracts versus Capabilities

A **semantic contract** defines what a piece of information, entity, state, event, relationship or operation means. A **Capability** defines an observable functional ability or operation that a runtime/application can provide or require. They are related but not interchangeable.

For example, a shared semantic contract may define an energy-storage observation (state, measured quantity, unit, timestamp and quality). A Capability may promise to retrieve or publish observations conforming to that contract. Different applications and providers can implement that Capability using different technologies while preserving the same semantic meaning.

A Capability may reference multiple semantic contracts for its inputs, outputs and events. A semantic contract may be used by multiple Capabilities, Packs and applications. Neither identity should be derived from the other.

A Pack organizes a functional area and curates the semantic contracts and Capabilities relevant to it. It is not automatically the sole authority for a shared contract used by other Packs or applications. Contract ownership, namespace governance and versioning remain explicit in the semantic plane.

### 2.3 Runtime and delivery plane — implementation

This plane defines how software is implemented and made executable.

#### Core

Core provides reusable, domain-neutral infrastructure and contracts: runtime lifecycle, execution, semantic validation mechanisms, Capability resolution, resource ownership, security boundaries and extension points.

Core must not absorb application-specific domain models merely because multiple applications use the same runtime technology.

#### Capability

A **Capability** is a stable, named functional contract representing something a runtime/application can provide or require.

It is the unit of functional activation and resolution at runtime. A Capability identity is independent of the Java class, plugin ID, Adapter, vendor, Module, XML name or UI label that happens to implement or expose it.

A Capability contract may define:

- stable identifier and semantic version;
- purpose and observable behavior;
- typed inputs, outputs and events;
- preconditions, postconditions and constraints;
- permissions, resource requirements and security properties;
- compatibility and failure semantics.

A Capability can be implemented by different providers. More than one provider may satisfy the same contract, subject to explicit selection and compatibility policy.

Not every internal helper or plugin needs to be exposed as a user-facing Capability. Do not inflate the public Capability vocabulary with implementation details.

#### Adapter

An **Adapter** connects an external system, protocol, device, vendor model or legacy application to a stable contract.

Examples include integrations for Victron, Ropam, Synology, HTTP APIs or databases.

An Adapter translates representations and interaction patterns. It must not redefine the meaning of domain concepts or shared semantic contracts referenced by a Pack, nor the Capability contract it implements.

Replacing an Adapter should not require changing a Kit or the consuming application's domain vocabulary, except where the replacement has materially different supported behavior or constraints. Those differences must be declared rather than hidden.

#### Plugin

A **Plugin** is a technical executable extension/provider unit recognized by a runtime. It supplies one or more implementation bindings and may implement one or more Capabilities, provide Adapters, or contribute other runtime behavior.

Plugin identity is not Capability identity. Plugin metadata, XML bindings and UI presentation are projections or integration details, not the canonical source of domain meaning.

#### Module

A **Module** is a technical unit of construction, dependency management, distribution, activation and lifecycle ownership.

Depending on the host/runtime, a Module may correspond to a Maven artifact, JAR, package, classloader scope, service bundle or another deployment unit. The architecture must not hard-code Maven as the universal meaning of Module.

A Module can contain or contribute multiple Plugins, Adapters and Capability providers. A Pack can span multiple Modules; one Module can contribute to more than one Pack. A Kit may require Capabilities whose providers are delivered by several Modules.

Module membership answers questions such as:

- What is installed or made available?
- Which dependencies and compatibility ranges apply?
- Which classloader/provider resources are owned together?
- What can be activated, stopped or retired as a technical unit?

It does not answer what the functionality means or which user-facing domain it belongs to.

#### XML dialect and other representations

XML, Java APIs, JSON, DSLs, IDE metadata and agent-facing descriptions are representations or adapters.

An XML QName, JSON property name, Java class name or UI label must map explicitly to a semantic or runtime identity. Prefix spelling, local element names and display labels are not cross-application semantic identity.

## 3. Relationship model

The relationships are intentionally many-to-many where the domain requires it:

- A Pack groups related domain concepts and Capabilities.
- A Capability may be used by more than one Pack or application when its meaning genuinely applies in those contexts.
- A Kit composes Capabilities from one or more Packs.
- An Adapter implements or translates a Capability/domain contract for an external system.
- A Plugin provides executable implementation bindings; it may implement several Capabilities or expose multiple representation bindings.
- A Module delivers one or more Plugins/Adapters/providers and owns their technical dependencies and lifecycle.
- An application selects the Packs, Kits, Capabilities and Modules appropriate to its product and deployment.

Do not infer these relationships from package names, source folders, Maven dependencies, annotations, XML names or classpath presence. They must be declared and validated.

### Illustrative topology

```text
                         Shared semantic contracts
                   identities · schemas · units · events
                                  |
              +-------------------+-------------------+
              |                                       |
       Functional plane                        Runtime/delivery plane
              |                                       |
       +------+-------+                         +-----+------+
       |              |                         |            |
     Pack A         Pack B                    Module 1     Module 2
       |              |                         |            |
   Capabilities   Capabilities               Plugins      Adapters
       |              |                         \            /
       +-------+------+                          Capability providers
               |
          Scenario Kit
       (composes Capabilities)
               |
          Application
```

This diagram is conceptual. It does not impose a deployment topology, dependency direction between every box, or a one-to-one mapping.

## 4. Cross-application semantic compatibility

### 4.1 Compatibility is shared meaning, not shared implementation

Two applications are semantically compatible when they interpret the same contract consistently enough for the intended exchange or composition.

They do not need to share:

- programming language or runtime;
- database or persistence model;
- UI or user workflow;
- plugin implementation;
- Module boundaries or artifact layout;
- serialization format.

They do need agreement on the meaning of exchanged identities, values, units, states, events, relationships and operations.

A field called `power` is not compatible merely because both applications use that name. Its physical quantity, unit, sign convention, measurement point, timestamp semantics, precision and quality interpretation may differ.

### 4.2 Canonical identities

Cross-application contracts require stable, globally unambiguous identifiers with explicit ownership and versioning policy.

Identifiers should be independent of:

- localized display names;
- Java package/class names;
- plugin and Module IDs;
- XML prefixes or local names;
- vendor-specific identifiers.

Domain vocabularies should be namespaced and governed by their owning domain or shared contract authority. Avoid a central registry that silently claims ownership of every domain concept.

Renames of display labels need not change identity. Semantic changes must follow explicit compatibility/versioning rules.

### 4.3 Typed schemas and units

Schemas should define more than field names and primitive storage types. Where relevant, they must express:

- semantic type and quantity;
- unit and conversion rules;
- allowed ranges and precision;
- nullability and unknown/unavailable distinctions;
- timestamps, time zones and interval semantics;
- identifiers and reference integrity;
- enumerations and state-transition meaning;
- quality, confidence and measurement context.

Do not silently coerce incompatible units, states or meanings. Lossy conversions and assumptions must be explicit and observable.

### 4.4 Versioning and evolution

Semantic contracts need their own versioning policy, separate from application, Plugin, Module, API and artifact versions.

The compatibility model should distinguish at least:

- additive compatible evolution;
- deprecated but still understood fields or values;
- breaking changes to meaning, constraints or required behavior;
- unknown extensions that can be safely preserved;
- unsupported versions that must be rejected or handled through an explicit adapter.

A version number alone does not establish compatibility. Compatibility must be evaluated against the actual contract changes and consumer requirements.

### 4.5 Mappings and anti-corruption boundaries

External systems and existing applications will use their own vocabularies and structures. Adapters/mappings translate those representations into or out of canonical contracts.

Mappings should be explicit, testable and versioned. They should record:

- source and target contract identities/versions;
- transformations and unit conversions;
- fields that are dropped, synthesized or approximated;
- unsupported or ambiguous source values;
- provenance needed to trace a result to its origin.

Do not let a vendor schema leak into the canonical domain model simply because it is the first available implementation.

### 4.6 Provenance and data quality

When data crosses application or Adapter boundaries, preserve relevant provenance:

- source system/device and source record identity;
- observation/event time and ingestion time;
- transformation/mapping identity and version;
- measurement method and units;
- quality, confidence and validation outcome;
- missing, stale, estimated or substituted value status.

Applications may store and present this information differently, but must not erase distinctions that change interpretation.

## 5. Boundaries and dependency rules

1. Shared semantic contracts must not depend on WebHarvest, Windy Farm or another consuming application.
2. Core runtime infrastructure must remain domain-neutral. Domain Packs may depend on Core contracts; Core must not depend on application Packs.
3. Packs own domain meaning and functional organization, not technical classloader or artifact lifecycle.
4. Kits compose stable Capabilities and semantic contracts; they must not bind to implementation classes or vendor-specific plugin names as their primary identity.
5. Capabilities define observable functional contracts; Plugins and Adapters provide implementations.
6. Modules own technical delivery and lifecycle. They do not define semantic or functional taxonomy.
7. XML/JSON/DSL/UI metadata are projections or adapters. They must not become competing semantic authorities.
8. Discovery, installation, activation, authorization and semantic compatibility are separate decisions.
9. A Module being present on a classpath does not mean its Plugins or Capabilities are trusted or active.
10. A Pack or Kit must not imply that every implementation is installed, available, authorized or compatible in every deployment.

## 6. Application interoperability example

Consider two independent applications that need to work with energy-storage observations.

- The Energy Pack owns the domain concepts and user-facing workflows for energy storage.
- A Capability defines the contract for obtaining storage state and measurements.
- A Victron Adapter maps a device/vendor representation to that contract.
- WebHarvest may acquire and transform the source data using a Plugin delivered by an optional Module.
- Windy Farm may consume the normalized contract and present it through its own Energy Pack UI and persistence.
- A Blackout Kit may compose storage-state, energy-production, inventory and communication Capabilities across several Packs.

The applications do not need to share a Victron Java library, WebHarvest plugin, Maven Module, database schema or UI. They need a compatible semantic contract and explicit mappings.

This example is illustrative; it does not assert that these integrations or cross-application contracts are already implemented.

## 7. Terminology and anti-collision rules

Use the terms consistently:

| Term | Use it for | Do not use it for |
|---|---|---|
| Core | Shared runtime/platform infrastructure and neutral contracts | A user-facing domain Pack |
| Pack | Stable functional domain/value area | JAR, Maven module or installer bundle |
| Kit | Scenario/configuration composing Capabilities | Pack synonym, plugin collection or binary package |
| Capability | Stable functional contract and runtime resolution unit | Concrete implementation class |
| Adapter | Translation/integration to an external system or representation | Domain definition or vendor-specific Pack |
| Plugin | Executable runtime extension/provider | Semantic concept or Pack |
| Module | Technical delivery, dependency and lifecycle unit | Functional domain or scenario |
| Semantic contract | Versioned shared meaning and constraints | Serialization format or metadata display |
| XML dialect | XML grammar and representation mapping | Canonical plugin/capability identity |

**Packaging terminology:** when a distribution combines multiple Modules, call it a *distribution*, *distribution profile*, *extension bundle* or another explicitly technical term. Do not call it a Pack or Kit unless it independently satisfies the functional/scenario definitions above. This avoids overloading the Pack/Kit vocabulary across products.

## 8. Application to WebHarvest

For WebHarvest, this model means:

- Core remains a domain-neutral data acquisition/integration runtime.
- Capability contracts express data operations and required behavior independently of concrete Plugin implementations.
- Plugins provide executable behavior and may implement multiple Capabilities.
- Adapters integrate concrete sources, protocols, destinations and vendor systems.
- Modules package and own plugin/provider dependencies and lifecycle.
- XML dialect descriptors describe XML syntax and mapping, not semantic ownership.
- Metadata, IDE palette and agent/tool descriptions are projections of the same authoritative contracts.
- Domain-specific Packs and scenario Kits may be introduced where WebHarvest has a real product-level functional model; they must not be invented merely to classify Maven modules.
- WebHarvest distribution profiles may include selected Modules, but are not thereby Packs or Kits.

The runtime registry/generation design must therefore keep Module identity, Plugin identity, Capability identity, semantic contract identity and XML QName identity distinct.

## 9. Relationship to existing project documentation

This document defines the cross-cutting vocabulary and conceptual boundaries. It does not replace implementation-specific designs.

- [Project Direction](PROJECT_DIRECTION.md) describes WebHarvest's product and distribution topology.
- [Plugin Registry Generation Design](PLUGIN_REGISTRY_GENERATION_DESIGN.md) describes runtime registration, descriptors, XML bindings and generation lifecycle.
- [Configuration Parameters and Secret Resolution](CONFIGURATION_PARAMETER_AND_SECRET_MODEL.md) describes configuration scopes and secret handling.
- [Semantic Metadata Stage 1 Contract](semantic-metadata/STAGE1_CONTRACT.md) describes the current limited semantic XML-inspection behavior.
- [Cross-Application Semantic Contract Inventory](CROSS_APPLICATION_SEMANTIC_CONTRACT_INVENTORY.md) identifies candidate shared domains and the evidence required before schemas or identifiers are standardized.

Where older documentation uses *Pack* or *Kit* to mean a binary/artifact grouping, treat that usage as historical or provisional and migrate it to technical distribution terminology. The functional definitions in this document are the intended cross-application meaning.

## 10. Decisions and open questions

### Proposed decisions

1. Adopt the distinct semantic, functional and runtime/delivery planes.
2. Reserve Pack for a stable functional domain/value area.
3. Reserve Kit for a scenario-oriented composition/configuration of Capabilities.
4. Treat Capability as a stable functional contract and runtime activation/resolution unit.
5. Treat Adapter as the integration/translation boundary to external systems and representations.
6. Treat Module as a technical delivery/dependency/lifecycle unit.
7. Keep semantic identity and versioning independent from implementation, packaging and serialization.
8. Use explicit technical distribution terminology rather than overloading Pack/Kit.

### Deliberately open

- Whether shared semantic contracts should live in a dedicated repository/artifact, a small set of versioned libraries, or independently versioned domain vocabularies.
- Governance and namespace ownership for cross-application identifiers.
- The exact compatibility/versioning policy and machine-readable schema format.
- How much semantic contract metadata belongs in Core versus domain-owned contract packages.
- Whether WebHarvest itself needs product-level Packs/Kits, and which concrete use cases justify them.
- How Kits are represented, validated and resolved across applications with different installed Modules and available Capabilities.

These questions must be answered from concrete interoperability use cases. They do not block adopting the conceptual boundaries above.

## 11. Adoption and acceptance criteria

The architecture is adopted only when future designs and implementations demonstrate that:

1. The same semantic contract can be consumed by two applications with different implementation stacks.
2. Contract identity does not change when a Plugin class, Adapter, vendor, Module or XML element is renamed or replaced.
3. A Pack can span multiple Modules without changing its functional identity.
4. A Module can contribute to multiple Packs without merging their domain semantics.
5. A Kit can compose Capabilities from multiple Packs without copying their domain models.
6. Multiple Adapters/providers can implement one Capability contract, with explicit compatibility and selection.
7. Incompatible units, meanings, states or schema versions are detected rather than silently coerced.
8. Mappings are explicit and testable, with provenance for lossy or approximate transformations.
9. Optional Modules being installed does not automatically activate or authorize their Capabilities.
10. XML, IDE, API and agent-facing representations resolve to the same semantic/runtime identities.
11. Technical distribution bundles are not presented as functional Packs/Kits unless they independently satisfy those definitions.

Until these criteria are demonstrated, this document is a target architecture proposal, not a claim that cross-application semantic interoperability is already implemented.
