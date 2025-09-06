package org.webharvest.definition;

import org.webharvest.exception.PluginException;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.processors.Processor;

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
            
            return plugin;
        } catch (Exception e) {
            throw new PluginException("Failed to create plugin: " + pluginClass.getName(), e);
        }
    }
}
