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
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.processors.Processor;

/**
 * Definition for core plugins that use the new Plugin architecture.
 * This class creates PluginProcessorAdapter instances that wrap the actual Plugin instances.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public class CorePluginDef extends AbstractElementDef {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorePluginDef.class);

    private final Class<? extends Plugin> pluginClass;
    private final CorePlugin annotation;

    /**
     * Creates a new CorePluginDef for the given plugin class.
     * This constructor is used internally.
     *
     * @param xmlNode the XML node
     * @param pluginClass the plugin class
     */
    public CorePluginDef(final XmlNode xmlNode, Class<? extends Plugin> pluginClass, boolean internal) {
        super(xmlNode);
        this.pluginClass = pluginClass;
        this.annotation = pluginClass.getAnnotation(CorePlugin.class);
        
        if (annotation == null) {
            throw new IllegalArgumentException("Plugin class " + pluginClass.getName() + 
                                             " is not annotated with @CorePlugin");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Processor createPlugin() {
        try {
            System.out.println("CorePluginDef.createPlugin(): START for " + pluginClass.getSimpleName());
            
            // Create the actual plugin instance using Guice
            final Plugin pluginInstance = org.webharvest.ioc.InjectorHelper.getInjector().getInstance(pluginClass);
            System.out.println("CorePluginDef.createPlugin(): pluginInstance=" + pluginInstance);
            
            // Create the adapter that wraps the plugin
            System.out.println("CorePluginDef.createPlugin(): Creating PluginProcessorAdapter with pluginInstance");
            final org.webharvest.runtime.processors.PluginProcessorAdapter adapter = 
                new org.webharvest.runtime.processors.PluginProcessorAdapter(pluginInstance);
            System.out.println("CorePluginDef.createPlugin(): adapter=" + adapter);
            
            // Set the element definition on the adapter
            System.out.println("CorePluginDef.createPlugin(): Setting elementDef on adapter");
            adapter.setElementDef(this);
            
            // Update plugin configuration and initialize the plugin
            System.out.println("CorePluginDef.createPlugin(): Updating plugin configuration and initializing");
            adapter.updatePluginConfiguration();
            adapter.initializePlugin();
            
            System.out.println("Created CorePluginDef for plugin: " + pluginClass.getSimpleName());
            
            return adapter;
            
        } catch (Exception e) {
            throw new PluginException("Error instantiating plugin class \"" + 
                                    pluginClass.getName() + "\": " + e.getMessage(), e);
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
     * Gets the CorePlugin annotation.
     *
     * @return the annotation
     */
    public CorePlugin getAnnotation() {
        return annotation;
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
