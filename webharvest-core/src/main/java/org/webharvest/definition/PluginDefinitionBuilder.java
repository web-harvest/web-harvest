package org.webharvest.definition;

import java.util.Map;

import org.webharvest.runtime.processors.Processor;

/**
 * Builder for creating WebHarvestPluginDef instances.
 * Provides fluent API for plugin definition creation.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public final class PluginDefinitionBuilder {
    
    private XmlNode xmlNode;
    private Class<? extends Processor> pluginClass;
    
    private PluginDefinitionBuilder() {
        // Private constructor
    }
    
    /**
     * Creates a new builder instance.
     */
    public static PluginDefinitionBuilder create() {
        return new PluginDefinitionBuilder();
    }
    
    /**
     * Sets the XML node for the plugin definition.
     */
    public PluginDefinitionBuilder withXmlNode(XmlNode xmlNode) {
        this.xmlNode = xmlNode;
        return this;
    }
    
    /**
     * Sets the plugin class for the definition.
     */
    public PluginDefinitionBuilder withPluginClass(Class<? extends Processor> pluginClass) {
        this.pluginClass = pluginClass;
        return this;
    }
    
    /**
     * Builds the WebHarvestPluginDef instance.
     */
    public WebHarvestPluginDef build() {
        if (xmlNode == null) {
            throw new IllegalStateException("XML node is required");
        }
        if (pluginClass == null) {
            throw new IllegalStateException("Plugin class is required");
        }
        
        return new WebHarvestPluginDef(xmlNode, pluginClass);
    }
}
