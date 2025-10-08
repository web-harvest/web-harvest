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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.exception.PluginException;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.plugin.Plugin;
import org.webharvest.runtime.processors.Processor;

/**
 * Custom WebHarvestPluginDef for core plugins that can access the plugin class
 * from the CorePluginElementInfo.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public class CorePluginWebHarvestPluginDef extends WebHarvestPluginDef {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorePluginWebHarvestPluginDef.class);

    private final Class<? extends Plugin> pluginClass;

    /**
     * Creates a new CorePluginWebHarvestPluginDef.
     *
     * @param xmlNode the XML node
     * @param processorClass the processor class (should be PluginProcessorAdapter)
     * @param pluginClass the actual plugin class
     */
    public CorePluginWebHarvestPluginDef(final XmlNode xmlNode,
                                        Class<? extends Processor> processorClass,
                                        Class<? extends Plugin> pluginClass) {
        super(xmlNode, processorClass);
        this.pluginClass = pluginClass;
        
        // Create element definitions for child elements
        createChildElementDefinitions();
    }
    
    /**
     * Creates element definitions for all child elements.
     */
    private void createChildElementDefinitions() {
        if (xmlNode == null) {
            return;
        }
        
        try {
            // Get all child elements from elementList
            java.util.List<java.io.Serializable> elementList = xmlNode.getElementList();
            if (elementList != null) {
                for (java.io.Serializable element : elementList) {
                    if (element instanceof XmlNode) {
                        XmlNode childNode = (XmlNode) element;
                        // Create element definition for each child
                        IElementDef childDef = InjectorHelper.getInjector().getInstance(ConfigurableResolver.class)
                            .createElementDefinition(childNode);
                        if (childDef != null) {
                            add(childDef);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error creating child element definitions: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor createPlugin() {
        try {
            // Use the plugin class that was passed in the constructor
            if (pluginClass == null) {
                throw new PluginException("No plugin class provided for CorePluginWebHarvestPluginDef");
            }
            
            // Create the actual plugin instance
            final Plugin pluginInstance = pluginClass.newInstance();
            
            // Create the adapter that wraps the plugin
            final org.webharvest.runtime.processors.PluginProcessorAdapter adapter = 
                new org.webharvest.runtime.processors.PluginProcessorAdapter(pluginInstance);
            
            // Set the element definition on the adapter
            adapter.setElementDef(this);
            
            System.out.println("Created CorePluginWebHarvestPluginDef for plugin: " + pluginClass.getSimpleName());
            
            return adapter;
            
        } catch (InstantiationException e) {
            throw new PluginException("Error instantiating plugin class: " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException("Error instantiating plugin class: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the plugin class.
     *
     * @return the plugin class
     */
    public Class<? extends Plugin> getPluginClass() {
        return pluginClass;
    }

    /**
     * Gets the XML node.
     *
     * @return the XML node
     */
    public XmlNode getXmlNode() {
        return xmlNode;
    }
}
