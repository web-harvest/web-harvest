package org.webharvest.plugin.metadata;

/**
 * High level semantic classification for a plugin node.
 */
public enum PluginSemanticRole {

    /** Root configuration container (e.g. {@code <config>}). */
    ROOT_CONTAINER,

    /**
     * Containers that provide execution or scoping context (loops, control
     * flow, connection/session scopes).
     */
    CONTEXT_CONTAINER,

    /**
     * Plugins that evaluate their body and return a value (extractors,
     * transformers, body processors).
     */
    BODY_EVALUATOR,

    /**
     * Plugins that require a specific parent context (e.g. {@code <catch>},
     * {@code <http-header>}).
     */
    DEPENDENT,

    /** Leaf operations that neither host nor require child plugins. */
    LEAF
}

