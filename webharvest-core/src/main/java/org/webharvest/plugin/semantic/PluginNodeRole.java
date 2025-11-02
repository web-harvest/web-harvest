package org.webharvest.plugin.semantic;

/**
 * High level semantic classification for plugin nodes used by the IDE, API and
 * future LLM integrations.
 */
public enum PluginNodeRole {

    /** Root container like {@code <config>} representing the configuration entry point. */
    ROOT_CONTAINER,

    /**
     * Containers that provide execution context for nested processors (loops,
     * control flow, connection scopes, etc.).
     */
    CONTEXT_CONTAINER,

    /**
     * Plugins that evaluate their body to produce a value (e.g. {@code <def>},
     * {@code <xpath>}).
     */
    BODY_EVALUATOR,

    /**
     * Plugins that require a specific parent context and cannot exist on their
     * own (e.g. {@code <catch>}, {@code <http-header>}).
     */
    DEPENDENT,

    /** Leaf operations with no child processors. */
    LEAF
}

