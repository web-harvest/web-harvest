# Stage-1 Semantic Metadata Contract

This document defines how the Stage-1 semantic model interprets plugin metadata.
It deliberately distinguishes legacy catalog/IDE semantics from evidence strong
enough to produce structural validation errors.

## Child policy

A Stage-1 descriptor carries a child policy and its provenance.

| Policy | Meaning | Structural behavior |
| --- | --- | --- |
| `ALLOW_LIST` | An authoritative closed child allow-list is declared. Its entries may still be legacy local-name-only metadata. | Reject a child only when a qualified allow-list identity is available and the child's qualified identity is absent; otherwise emit a metadata limitation and skip the hard check. |
| `ANY_CHILD` | An authoritative plugin declaration explicitly accepts arbitrary plugin children. | Do not apply an allow-list rejection. |
| `FORBID_CHILDREN` | An authoritative producer explicitly declares a closed policy with no plugin children. | Reject every plugin child. No current metadata producer supplies this state. |
| `UNKNOWN` | Available metadata cannot establish a safe child policy. | Skip allow/deny conclusions and emit a metadata limitation. |

The policy is independent of body text support and independent of the presence of
the legacy schema extension.

### Explicit declarations versus legacy inference

Only `VisualPluginMetadata.acceptsArbitraryChildren() == true` is authoritative
for Stage-1 `ANY_CHILD`. The legacy resolver also exposes an effective
`allowsAnyChild` value inferred from BODY_EVALUATOR plus an empty allow-list.
That legacy value remains available to existing consumers, but is not structural
authority. This prevents body evaluators such as `def`, `http-header`, or
`regexp-pattern` from being treated as unrestricted containers merely because
of body semantics.

A non-empty catalog allow-list is retained in Stage 1, including children supplied
by visual metadata rather than only the schema extension. Since current producers
store those names without namespace URIs, Stage 1 records the list but does not use
it to reject a child by local name alone.

Empty or missing legacy lists mean `UNKNOWN`, never `FORBID_CHILDREN`.
`FORBID_CHILDREN` must be introduced only by an explicit closed-policy
metadata producer. The public Stage-1 descriptor builder enforces this invariant:
a `FORBID_CHILDREN` policy paired with any provenance other than
`EXPLICIT_CLOSED_POLICY` is rejected at construction time.

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
absent. Attribute, text, and root-placement checks still require their respective
schema metadata.

## Runtime availability

The active runtime probe is `CorePluginRegistry.isPluginRegistered(localName)`.
It is namespace-unaware and represents only membership in the current legacy
registry snapshot. It does not prove namespace-qualified dispatch, successful
plugin construction, or successful execution.

Stage 1 exposes this as `RuntimeAvailabilityStatus`:

- `LEGACY_LOCAL_NAME_REGISTERED`
- `NOT_REGISTERED_IN_CORE_REGISTRY`
- `UNKNOWN`

The former `PluginResolution.isRuntimeExecutable()` boolean is retired because
its name implied a stronger guarantee than the probe could establish. No parser,
registry lifecycle, namespace-aware runtime dispatch, or execution behavior is
changed by this contract.

## Compatibility and migration

- Legacy resolver `allowsAnyChild` behavior is preserved for existing catalog
  consumers; Stage 1 uses the separate explicit declaration.
- The public `PluginResolution` API change from
  `isRuntimeExecutable()` / the boolean `resolved(..., boolean)` factory to
  `RuntimeAvailabilityStatus` is source- and binary-incompatible. Consumers
  must migrate to `getRuntimeAvailabilityStatus()` and the enum-based factory.
  This API must not be shipped as a compatible minor/patch update: before
  artifact publication, the project must either make a major-version decision
  or deliberately restore a compatibility bridge. This PR does not itself
  declare a release version or authorize artifact publication.
- Legacy local-name relationship getters remain available but are not used for
  hard namespace-aware validation.
- New authoritative metadata producers should populate qualified identities.
- No current producer should infer a namespace or emit `FORBID_CHILDREN` from
  an empty list.
- A future runtime migration must replace the scoped legacy probe with a
  namespace-aware capability model before claiming qualified executability.
