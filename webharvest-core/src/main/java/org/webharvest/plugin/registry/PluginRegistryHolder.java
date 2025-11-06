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
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.utils.ClassPathScanner;
import org.webharvest.utils.ClassPathScannerImpl;

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
     * <p>Automatically discovers and registers all plugins annotated with {@code @CorePlugin}
     * from the {@code org.webharvest.plugin.core} package.</p>
     *
     * @return the initialized plugin registry
     */
    private static PluginRegistry createAndInitializeRegistry() {
        try {
            DefaultPluginRegistry registry = new DefaultPluginRegistry();
            
            // Automatically discover all plugins with @CorePlugin annotation
            ClassPathScanner scanner = new ClassPathScannerImpl("org.webharvest.plugin.core");
            java.util.Set<Class<?>> pluginClasses = scanner.getTypesAnnotatedWith(CorePlugin.class);
            
            int registeredCount = 0;
            int skippedCount = 0;
            
            for (Class<?> pluginClass : pluginClasses) {
                try {
                    // Verify it's a Plugin class
                    if (!Plugin.class.isAssignableFrom(pluginClass)) {
                        LOG.debug("Skipping {} - not a Plugin class", pluginClass.getName());
                        skippedCount++;
                        continue;
                    }
                    
                    // Check if it's a concrete class (not abstract or interface)
                    if (java.lang.reflect.Modifier.isAbstract(pluginClass.getModifiers())) {
                        LOG.debug("Skipping {} - abstract class", pluginClass.getName());
                        skippedCount++;
                        continue;
                    }
                    
                    @SuppressWarnings("unchecked")
                    Class<? extends Plugin> pluginType = (Class<? extends Plugin>) pluginClass;
                    
                    // Register plugin
                    registry.registerPlugin(pluginType);
                    registeredCount++;
                    
                } catch (PluginException e) {
                    LOG.warn("Failed to register plugin {}: {}", pluginClass.getName(), e.getMessage());
                    skippedCount++;
                } catch (Exception e) {
                    LOG.warn("Error registering plugin {}: {}", pluginClass.getName(), e.getMessage());
                    skippedCount++;
                }
            }
            
            LOG.info("Initialized plugin registry with {} core plugins ({} skipped)", 
                registeredCount, skippedCount);
            return registry;
        } catch (Exception e) {
            LOG.error("Failed to initialize plugin registry", e);
            // Return empty registry to prevent null pointer exceptions
            return new DefaultPluginRegistry();
        }
    }
}


