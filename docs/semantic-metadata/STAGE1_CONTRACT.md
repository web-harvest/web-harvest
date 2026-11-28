# Stage 1 Semantic Metadata Contract

This document defines how the Stage 1 semantic model interprets plugin metadata.
It distinguishes legacy catalog/IDE semantics from evidence strong enough to
produce structural validation errors.

## Scope boundary

The term *semantic* here describes the limited interpretation of plugin metadata for XML/editor structure. It is not a canonical domain semantic contract, a cross-application ontology, or a stable runtime Capability contract. Qualified XML relationships in this document are document-grammar identities, not domain-entity relationships.

For the cross-application meaning of semantic contracts and their distinction from Capabilities, see [Cross-Application Semantic and Functional Architecture](../CROSS_APPLICATION_SEMANTIC_ARCHITECTURE.md). The Stage 1 rules below must not be used as evidence that those broader contracts or runtime resolution guarantees already exist.

## Child policy

A Stage 1 descriptor carries a child policy and its provenance.

| Policy | Meaning | Structural behavior |
| --- | --- | --- |
| `ALLOW_LIST` | A closed child allow-list is declared. Its entries may still be legacy local-name-only metadata. | Reject a child only when a qualified allow-list identity is available and the child's qualified identity is absent; otherwise emit a metadata limitation and skip the hard check. |
| `ANY_CHILD` | An authoritative plugin declaration explicitly accepts arbitrary plugin children. | Do not apply an allow-list rejection. |
| `FORBID_CHILDREN` | An authoritative producer explicitly declares a closed policy with no plugin children. | Reject every plugin child. No current catalog producer supplies this state. |
| `UNKNOWN` | Available metadata cannot establish a safe child policy. | Skip allow/deny conclusions and emit a metadata limitation. |

Only `VisualPluginMetadata.acceptsArbitraryChildren() == true` is authoritative
for Stage 1 `ANY_CHILD`. The legacy resolver also exposes an effective
`allowsAnyChild` value inferred from BODY_EVALUATOR plus an empty allow-list.
That legacy value remains available to existing consumers, but is not structural
authority. This prevents body evaluators from being treated as unrestricted
containers merely because of body semantics.

A non-empty catalog allow-list is retained in Stage 1, including children supplied
by visual metadata rather than only the schema extension. Since current producers
store those names without namespace URIs, Stage 1 records the list but does not use
it to reject a child by local name alone.

Empty or missing legacy lists mean `UNKNOWN`, never `FORBID_CHILDREN`.
`FORBID_CHILDREN` requires the explicit `EXPLICIT_CLOSED_POLICY` provenance;
the public descriptor builder rejects any other provenance for that state.

## Qualified relationships

Structural relationship identity is the pair:

`(namespaceUri, localName)`

The existing immutable `QualifiedName` type is used for allowed-child,
required-child, and required-parent identities. Prefixes are source presentation
data and are not identity. Namespace and element aliases are canonicalized only
through the configured alias tables before comparison.

Legacy fields (`allowedSubElements`, `requiredSubElements`, and
`requiredParents`) remain local-name-only for compatibility. They are not
silently assigned a namespace from the parent, child, or catalog descriptor.
Their presence produces a metadata limitation and does not create a hard
qualified relationship conclusion.

Qualified relationship validation runs even when the legacy schema extension is
absent. Attribute and text checks still require their respective schema metadata.
The current catalog projection does not yet populate qualified relationship
identities; catalog-backed allow-lists therefore remain warning-and-skip until an
authoritative producer supplies qualified names. This is intentional and is not
a claim that those elements accept arbitrary children.

## Runtime availability

The active Stage 1 probe is `CorePluginRegistry.isPluginRegistered(localName)`.
It is namespace-unaware and represents only membership in the current legacy
registry snapshot. It does not prove namespace-qualified dispatch, successful
plugin construction, or successful execution.

Stage 1 exposes this as `RuntimeAvailabilityStatus`:

- `LEGACY_LOCAL_NAME_REGISTERED`
- `NOT_REGISTERED_IN_CORE_REGISTRY`
- `UNKNOWN`

The former `PluginResolution.isRuntimeExecutable()` boolean and
`resolved(..., boolean)` factory remain as deprecated compatibility bridges.
The boolean returns true only when the legacy local-name probe positively reports
registration; false also covers UNKNOWN. New consumers should use
`getRuntimeAvailabilityStatus()` and the enum factory to preserve that
distinction. Probe exceptions yield `UNKNOWN`. No parser, registry lifecycle,
namespace-aware runtime dispatch, or execution behavior is changed.

## Compatibility and migration

- Existing `allowsAnyChild()` semantics remain available to legacy consumers.
  The new explicit flag records only `VisualPluginMetadata` declarations.
- Legacy local-name relationship getters remain available but are not used for
  hard namespace-aware validation.
- New authoritative metadata producers should populate qualified identities.
- No current producer should infer a namespace or emit `FORBID_CHILDREN` from
  an empty list.
- A future runtime migration must use the authoritative namespace-aware runtime
  capability model before claiming qualified executability.
