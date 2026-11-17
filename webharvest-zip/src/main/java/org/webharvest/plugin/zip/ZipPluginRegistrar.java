/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.zip;

import java.util.Objects;

import org.webharvest.definition.CorePluginRegistry;

/**
 * Explicitly registers the optional ZIP XML processors.
 *
 * <p>ZIP remains outside the default Core registry. Applications that include
 * this optional module must call this registrar during runtime setup.</p>
 *
 * @author WebHarvest Team
 * @since 2.2.0
 */
public final class ZipPluginRegistrar {

    private ZipPluginRegistrar() {
        // Utility class.
    }

    /**
     * Registers {@code zip} and {@code zip-entry} in the supplied registry.
     *
     * @param registry runtime registry to update
     */
    public static void register(CorePluginRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");
        registry.registerMapping(null, ZipPlugin.class, "zip");
        registry.registerMapping(null, ZipEntryPlugin.class, "zip-entry");
    }
}
