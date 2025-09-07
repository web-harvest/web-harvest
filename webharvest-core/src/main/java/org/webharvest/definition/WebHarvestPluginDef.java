package org.webharvest.definition;

import java.util.Map;

import org.webharvest.exception.PluginException;
import org.webharvest.runtime.processors.Processor;

/**
 * Definition of all plugin processors.
 * Uses composition and delegation to separate concerns.
 */
public class WebHarvestPluginDef extends AbstractElementDef {

    private final Class<? extends Processor> pluginClass;
    private final PluginFactory pluginFactory;

    public WebHarvestPluginDef(final XmlNode xmlNode,
            Class<? extends Processor> pluginClass) {
        this(xmlNode, pluginClass, new PluginFactory());
    }
    
    /**
     * Constructor with dependency injection for testing.
     */
    WebHarvestPluginDef(final XmlNode xmlNode,
                       Class<? extends Processor> pluginClass,
                       PluginFactory pluginFactory) {
        super(xmlNode);
        this.pluginClass = pluginClass;
        this.pluginFactory = pluginFactory;
    }

    public String getUri() {
        return xmlNode.getUri();
    }

    public Map<String, String> getAttributes() {
        return getAttributes(xmlNode.getUri());
    }

    public Map<String, String> getAttributes(String uri) {
        return xmlNode.getAttributes(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor createPlugin() {
        return pluginFactory.createPlugin(pluginClass, this);
    }

    @Override
    public String getNamespaceURI() {
        return xmlNode.getUri();
    }

    @Deprecated
    @Override
    public XmlNode getNode() {
        return xmlNode;
    }

}