/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.core.HttpPlugin;
import org.webharvest.plugin.core.ReturnPlugin;
import org.webharvest.plugin.core.TextPlugin;
import org.webharvest.plugin.exceptions.PluginException;

/**
 * Singleton holder for the global plugin registry instance.
 * This provides a centralized way to access the plugin registry throughout the application.
 *
 * @author Robert Bala
 * @since 2.1.0
 */
public final class PluginRegistryHolder {

    private static final Logger LOG = LoggerFactory.getLogger(PluginRegistryHolder.class);

    private static volatile PluginRegistry INSTANCE;
    private static final Object LOCK = new Object();

    /**
     * Private constructor to prevent instantiation.
     */
    private PluginRegistryHolder() {
    }

    /**
     * Gets the global plugin registry instance.
     * If the instance doesn't exist, it will be created and initialized with core plugins.
     *
     * @return the plugin registry instance
     */
    public static PluginRegistry getInstance() {
        if (INSTANCE == null) {
            synchronized (LOCK) {
                if (INSTANCE == null) {
                    INSTANCE = createAndInitializeRegistry();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Sets the global plugin registry instance.
     * This method is primarily intended for testing and dependency injection.
     *
     * @param registry the plugin registry instance to set
     */
    public static void setInstance(PluginRegistry registry) {
        synchronized (LOCK) {
            INSTANCE = registry;
        }
    }

    /**
     * Resets the global plugin registry instance.
     * This method is primarily intended for testing.
     */
    public static void reset() {
        synchronized (LOCK) {
            INSTANCE = null;
        }
    }

    /**
     * Creates and initializes a new plugin registry with core plugins.
     *
     * @return the initialized plugin registry
     */
    private static PluginRegistry createAndInitializeRegistry() {
        try {
            DefaultPluginRegistry registry = new DefaultPluginRegistry();
            
            // Register core plugins
            registry.registerPlugin(TextPlugin.class);
            registry.registerPlugin(ReturnPlugin.class);
            registry.registerPlugin(HttpPlugin.class);
            
            LOG.info("Initialized plugin registry with {} core plugins", registry.getPluginCount());
            return registry;
        } catch (PluginException e) {
            LOG.error("Failed to initialize plugin registry", e);
            // Return empty registry to prevent null pointer exceptions
            return new DefaultPluginRegistry();
        }
    }
}


