package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.processors.WebHarvestPlugin;
import org.webharvest.runtime.processors.plugins.TargetNamespace;
import org.webharvest.annotation.Definition;
import org.webharvest.exception.PluginException;

/**
 * ElementInfo-based implementation of PluginRegistrationStrategy.
 * This strategy uses the newer ElementInfo approach for registering plugins.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0-SNAPSHOT
 */
public class ElementInfoPluginRegistrationStrategy implements PluginRegistrationStrategy {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ElementInfoPluginRegistrationStrategy.class);
    
    @Override
    public void registerPlugin(Class<? extends WebHarvestPlugin> plugin, 
                              ConfigurableResolver resolver) throws PluginException {
        final TargetNamespace targetNamespace = plugin.getAnnotation(TargetNamespace.class);
        final Definition definition = plugin.getAnnotation(Definition.class);
        
        if (targetNamespace == null) {
            throw new PluginException("Plugin " + plugin.getCanonicalName() + 
                                    " does not have @TargetNamespace annotation");
        }
        
        if (definition != null) {
            final ElementInfo elementInfo = new ElementInfo(
                    definition.value(), 
                    WebHarvestPluginDef.class, 
                    plugin, 
                    null, 
                    null, 
                    false, 
                    null);

            for (String namespace : targetNamespace.value()) {
                LOGGER.info("Registering plugin {} under namespace {}",
                        plugin.getCanonicalName(), namespace);
                resolver.registerPlugin(elementInfo, namespace);
            }
        } else {
            // Fallback to default strategy if no @Definition annotation
            LOGGER.warn("Plugin {} does not have @Definition annotation, using default registration", 
                       plugin.getCanonicalName());
            new DefaultPluginRegistrationStrategy().registerPlugin(plugin, resolver);
        }
    }
}
