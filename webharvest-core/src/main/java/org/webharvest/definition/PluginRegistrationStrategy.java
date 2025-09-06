package org.webharvest.definition;

import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.exception.PluginException;

/**
 * Strategy interface for plugin registration.
 * This interface defines different strategies for registering plugins
 * in the resolver, allowing for flexible plugin registration approaches.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0-SNAPSHOT
 */
public interface PluginRegistrationStrategy {
    
    /**
     * Registers a plugin in the resolver using the specified strategy.
     * 
     * @param plugin the plugin class to register
     * @param resolver the resolver to register the plugin in
     * @throws PluginException if registration fails
     */
    void registerPlugin(Class<? extends WebHarvestPlugin> plugin, 
                       ConfigurableResolver resolver) throws PluginException;
}
