package org.webharvest.semantic;

/**
 * Outcome of Stage-1 plugin descriptor resolution against the catalog index.
 *
 * <p>{@link #UNAVAILABLE_OPTIONAL} is emitted only when an
 * {@link OptionalPluginProfile} entry matches and its class is absent from the
 * classpath. Unknown elements outside that profile remain {@link #UNKNOWN_ELEMENT}.
 */
public enum PluginResolutionStatus {
    /** Unique catalog descriptor for the canonical (namespace, localName) key. */
    RESOLVED,
    /** No catalog descriptor (and not an unavailable optional-profile hit). */
    UNKNOWN_ELEMENT,
    /** Multiple catalog descriptors collide on the same canonical key. */
    AMBIGUOUS,
    /**
     * Optional extension profile entry matched, but {@code Class.forName} failed.
     * Not used for arbitrary unknown elements.
     */
    UNAVAILABLE_OPTIONAL
}
