/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.exception.PluginException;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;

/**
 * Strategy for registering core plugins annotated with {@link CorePlugin}.
 * This strategy handles the registration of new plugin architecture plugins
 * that extend {@link Plugin} and are annotated with {@link CorePlugin}.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public class CorePluginRegistrationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(
            CorePluginRegistrationStrategy.class);

    /**
     * Registers a core plugin with the given resolver.
     * 
     * @param pluginClass the plugin class to register
     * @param resolver the resolver to register the plugin with
     * @throws PluginException if registration fails
     */
    public void registerPlugin(Class<? extends Plugin> pluginClass, 
                              ConfigurableResolver resolver) throws PluginException {
        System.out.println("CorePluginRegistrationStrategy.registerPlugin called for: " + pluginClass.getName());
        LOGGER.info("CorePluginRegistrationStrategy.registerPlugin called for: {}", pluginClass.getName());
        
        final CorePlugin annotation = pluginClass.getAnnotation(CorePlugin.class);
        if (annotation == null) {
            throw new PluginException("Plugin class " + pluginClass.getName() + 
                                    " is not annotated with @CorePlugin");
        }

        try {
            // Create an instance to validate the plugin
            final Plugin pluginInstance = pluginClass.newInstance();
            
            // Get plugin metadata
            final String elementName = annotation.elementName();
            final String namespace = annotation.namespace();
            
            System.out.println("CorePluginRegistrationStrategy: Registering core plugin: " + elementName + " in namespace: " + namespace);
            LOGGER.info("Registering core plugin: {} in namespace: {}", 
                       elementName, namespace);
            
            // Create ElementInfo using the CorePluginElementInfoFactory
            // This will use the PluginProcessorAdapter to bridge new Plugin with old Processor system
            final ElementInfo elementInfo = CorePluginElementInfoFactory.getElementInfo(pluginClass);
            
            // Register the mapping between processor class and plugin class
            // Use a unique key based on element name to avoid conflicts
            CorePluginRegistry.getInstance().registerMapping(
                org.webharvest.runtime.processors.PluginProcessorAdapter.class, 
                pluginClass,
                elementName
            );
            
            // Register the plugin with the resolver
            try {
                resolver.registerPlugin(elementInfo, namespace);
                LOGGER.debug("Core plugin {} registered successfully", elementName);
            } catch (PluginException e) {
                if (e.getMessage().contains("already registered")) {
                    LOGGER.info("Core plugin {} was already registered, skipping", elementName);
                    return; // Skip registration if already registered
                } else {
                    throw e; // Re-throw if it's a different error
                }
            }
            
        } catch (InstantiationException e) {
            throw new PluginException("Error instantiating plugin class \"" + 
                                    pluginClass.getName() + "\": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException("Error instantiating plugin class \"" + 
                                    pluginClass.getName() + "\": " + e.getMessage(), e);
        }
    }
}
