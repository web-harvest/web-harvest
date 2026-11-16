package org.webharvest.semantic;

/** Scope-limited result of the legacy CorePluginRegistry local-name probe. */
public enum RuntimeAvailabilityStatus {
    /** The legacy registry contains the local name; namespace is not part of the probe. */
    LEGACY_LOCAL_NAME_REGISTERED,
    /** The local name is absent from the current CorePluginRegistry snapshot. */
    NOT_REGISTERED_IN_CORE_REGISTRY,
    /** No reliable result is available. */
    UNKNOWN
}
