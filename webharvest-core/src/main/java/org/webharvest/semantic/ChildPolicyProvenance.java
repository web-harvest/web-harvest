package org.webharvest.semantic;

/** Provenance of a Stage-1 child policy decision. */
public enum ChildPolicyProvenance {
    EXPLICIT_VISUAL_METADATA,
    CATALOG_SEMANTICS,
    SCHEMA_ALLOW_LIST,
    LEGACY_BODY_HEURISTIC,
    EXPLICIT_CLOSED_POLICY,
    MISSING_METADATA
}
