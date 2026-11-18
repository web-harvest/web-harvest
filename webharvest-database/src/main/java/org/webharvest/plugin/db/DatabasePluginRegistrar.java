/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.db;

import java.util.Objects;

import org.webharvest.definition.CorePluginRegistry;

/**
 * Explicitly registers the optional JDBC XML processors.
 *
 * <p>The Database module is not part of the default Core plugin set. An
 * application that includes this module must call this registrar during
 * runtime setup before executing configurations that use {@code connection}
 * or {@code database}.</p>
 */
public final class DatabasePluginRegistrar {

    private DatabasePluginRegistrar() {
        // Utility class.
    }

    /**
     * Registers the optional JDBC processors in the supplied runtime registry.
     *
     * @param registry runtime registry to update
     */
    public static void register(final CorePluginRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");
        registry.registerMapping(null, ConnectionPlugin.class, "connection");
        registry.registerMapping(null, DatabasePlugin.class, "database");
    }
}
