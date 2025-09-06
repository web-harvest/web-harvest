package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.exception.PluginException;

/**
 * Default implementation of PluginRegistrationStrategy.
 * This strategy uses the traditional approach of registering plugins
 * by their class and namespace.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0-SNAPSHOT
 */
public class DefaultPluginRegistrationStrategy implements PluginRegistrationStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginRegistrationStrategy.class);
    
    @Override
    public void registerPlugin(Class<? extends WebHarvestPlugin> plugin, 
                              ConfigurableResolver resolver) throws PluginException {
        final TargetNamespace targetNamespace = plugin.getAnnotation(TargetNamespace.class);
        
        if (targetNamespace == null) {
            throw new PluginException("Plugin " + plugin.getCanonicalName() + 
                                    " does not have @TargetNamespace annotation");
        }
        
        for (String namespace : targetNamespace.value()) {
            LOGGER.info("Registering plugin {} under namespace {}",
                    plugin.getCanonicalName(), namespace);
            resolver.registerPlugin(plugin, namespace);
        }
    }
}
