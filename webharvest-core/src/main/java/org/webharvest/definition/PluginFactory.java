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

package org.webharvest.definition;

import org.webharvest.exception.PluginException;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.processors.Processor;
import org.webharvest.runtime.processors.PluginProcessorAdapter;

/**
 * Factory for creating plugin instances.
 * Separates plugin creation logic from definition class.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public final class PluginFactory {
    
    public PluginFactory() {
        // Public constructor for dependency injection
    }
    
    /**
     * Creates a new plugin instance with proper dependency injection.
     * 
     * @param pluginClass the plugin class
     * @param elementDef the element definition
     * @return configured plugin instance
     * @throws PluginException if plugin creation fails
     */
    public static Processor createPlugin(Class<? extends Processor> pluginClass, 
                                       IElementDef elementDef) throws PluginException {
        if (pluginClass == null) {
            throw new PluginException("Plugin class cannot be null");
        }
        
        try {
            Processor plugin = pluginClass.getDeclaredConstructor().newInstance();
            plugin.setElementDef(elementDef);
            
            // Inject dependencies using Guice
            InjectorHelper.getInjector().injectMembers(plugin);
            
            // Initialize plugin with configuration if it's a PluginProcessorAdapter
            if (plugin instanceof PluginProcessorAdapter) {
                PluginProcessorAdapter adapter = (PluginProcessorAdapter) plugin;
                // Call the public method to update configuration
                adapter.updatePluginConfiguration();
                if (!adapter.isInitialized()) {
                    adapter.initialize(adapter.getPluginConfiguration());
                }
            }
            
            return plugin;
        } catch (Exception e) {
            throw new PluginException("Failed to create plugin: " + pluginClass.getName(), e);
        }
    }
}
