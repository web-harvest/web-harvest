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

package org.webharvest.runtime.processors;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.IElementDef;
import org.webharvest.definition.WebHarvestPluginDef;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.definition.XmlNode;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;

import java.util.Map;

/**
 * Adapter that wraps Plugin instances to work as Processor instances.
 * This allows the new @CorePlugin architecture to work with the existing Processor framework.
 *
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class PluginProcessorAdapter extends AbstractProcessor<IElementDef> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginProcessorAdapter.class);

    private Plugin plugin;
    private PluginConfiguration pluginConfiguration;

    /**
     * Default constructor for creating adapter instances.
     */
    public PluginProcessorAdapter() {
        super();
    }

    /**
     * Constructor that creates adapter for a specific plugin instance.
     *
     * @param plugin the plugin instance
     */
    public PluginProcessorAdapter(Plugin plugin) {
        super();
        System.out.println("PluginProcessorAdapter(Plugin) constructor: plugin=" + plugin);
        this.plugin = plugin;
        System.out.println("PluginProcessorAdapter(Plugin) constructor: this.plugin=" + this.plugin);
    }

    /**
     * Constructor that creates adapter for a specific plugin class and element definition.
     *
     * @param pluginClass the plugin class
     * @param elementDef the element definition
     */
    public PluginProcessorAdapter(Class<? extends Plugin> pluginClass, WebHarvestPluginDef elementDef) {
        super();
        this.elementDef = elementDef;
        
        try {
            // Create plugin instance using Guice
            Injector injector = InjectorHelper.getInjector();
            this.plugin = injector.getInstance(pluginClass);
            
            // Update configuration based on element definition
            updatePluginConfiguration();
            
            // Initialize plugin with configuration
            if (plugin != null && pluginConfiguration != null) {
                plugin.initialize(pluginConfiguration);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to create PluginProcessorAdapter for {}", pluginClass.getName(), e);
            throw new RuntimeException("Failed to create PluginProcessorAdapter", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable execute(DynamicScopeContext context) throws InterruptedException {
        if (plugin == null) {
            LOGGER.error("Plugin is null - adapter was not properly initialized");
            throw new RuntimeException("Plugin is null - adapter was not properly initialized");
        }

        try {
            // For new @CorePlugin architecture, plugins don't need setElementDef()
            // They get configuration through PluginConfiguration instead
            
            // Execute the plugin
            return plugin.execute(context);
        } catch (Exception e) {
            LOGGER.error("Error executing plugin: {}", plugin.getClass().getName(), e);
            throw new RuntimeException("Plugin execution failed", e);
        }
    }

    /**
     * Initializes the plugin with the current configuration.
     */
    public void initializePlugin() throws PluginException {
        if (plugin != null && pluginConfiguration != null) {
            // Only initialize if not already initialized
            if (!plugin.isInitialized()) {
                plugin.initialize(pluginConfiguration);
            } else {
                System.out.println("Plugin " + plugin.getClass().getSimpleName() + " is already initialized, skipping initialization");
            }
        }
    }

    /**
     * Updates the plugin configuration based on the current element definition.
     */
    public void updatePluginConfiguration() {
        if (elementDef != null) {
            // Create a new configuration based on the element definition
            String pluginId = "unknown";
            if (plugin != null) {
                try {
                    pluginId = plugin.getClass().getSimpleName();
                } catch (Exception e) {
                    LOGGER.error("Error getting plugin class name: {}", e.getMessage());
                    pluginId = "unknown";
                }
            }
            
            String elementName = elementDef.getShortElementName();
            if (elementName == null) {
                elementName = "unknown";
            }
            
            this.pluginConfiguration = new DefaultPluginConfiguration(
                pluginId,  // pluginId
                "http://org.webharvest/schema/2.1/core",  // namespace
                elementName  // elementName
            );
            
            // Add XML attributes to plugin configuration
            if (elementDef instanceof WebHarvestPluginDef) {
                WebHarvestPluginDef webHarvestDef = (WebHarvestPluginDef) elementDef;
                Map<String, String> attributes = webHarvestDef.getAttributes();
                LOGGER.debug("Found {} XML attributes", attributes.size());
                        for (Map.Entry<String, String> entry : attributes.entrySet()) {
                            String attrName = entry.getKey();
                            String attrValue = entry.getValue();
                    LOGGER.debug("Setting attribute '{}' = '{}'", attrName, attrValue);
                                if (this.pluginConfiguration instanceof DefaultPluginConfiguration) {
                                    ((DefaultPluginConfiguration) this.pluginConfiguration).setProperty(attrName, attrValue);
                    }
                }
            } else if (elementDef instanceof CorePluginDef) {
                // For CorePluginDef, get attributes from the XML node
                CorePluginDef coreDef = (CorePluginDef) elementDef;
                XmlNode xmlNode = coreDef.getNode();
                System.out.println("PluginProcessorAdapter.updatePluginConfiguration: CorePluginDef xmlNode=" + xmlNode);
                if (xmlNode != null) {
                    Map<String, String> attributes = xmlNode.getAttributes(xmlNode.getUri());
                    System.out.println("PluginProcessorAdapter.updatePluginConfiguration: Found " + attributes.size() + " XML attributes for CorePluginDef");
                    LOGGER.debug("Found {} XML attributes for CorePluginDef", attributes.size());
                    for (Map.Entry<String, String> entry : attributes.entrySet()) {
                        String attrName = entry.getKey();
                        String attrValue = entry.getValue();
                        System.out.println("PluginProcessorAdapter.updatePluginConfiguration: Setting attribute '" + attrName + "' = '" + attrValue + "'");
                        LOGGER.debug("Setting attribute '{}' = '{}'", attrName, attrValue);
                        if (this.pluginConfiguration instanceof DefaultPluginConfiguration) {
                            ((DefaultPluginConfiguration) this.pluginConfiguration).setProperty(attrName, attrValue);
                        }
                    }
                } else {
                    System.out.println("PluginProcessorAdapter.updatePluginConfiguration: xmlNode is null for CorePluginDef");
                }
            }
            
            LOGGER.debug("Created configuration successfully");
        } else {
            LOGGER.warn("elementDef is null, cannot update configuration");
            this.pluginConfiguration = new DefaultPluginConfiguration(
                "unknown",  // pluginId
                "http://org.webharvest/schema/2.1/core",  // namespace
                "unknown"  // elementName
            );
        }
    }
    
    /**
     * Gets the plugin configuration.
     * @return the plugin configuration
     */
    public PluginConfiguration getPluginConfiguration() {
        return pluginConfiguration;
    }
    
    /**
     * Checks if the plugin is initialized.
     * @return true if the plugin is initialized, false otherwise
     */
    public boolean isInitialized() {
        return plugin != null && plugin.isInitialized();
    }
    
    /**
     * Initializes the plugin with the given configuration.
     * @param configuration the plugin configuration
     * @throws PluginException if initialization fails
     */
    public void initialize(PluginConfiguration configuration) throws PluginException {
        if (plugin != null) {
            plugin.initialize(configuration);
        }
    }

    /**
     * Gets the adapted plugin.
     *
     * @return the plugin instance
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Sets the plugin instance for this adapter.
     * This method is used when the adapter is created via the default constructor.
     *
     * @param plugin the plugin instance
     */
    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
        if (plugin != null) {
            // Update configuration with plugin information
            this.pluginConfiguration = new DefaultPluginConfiguration(
                plugin.getClass().getSimpleName(),  // pluginId
                "http://org.webharvest/schema/2.1/core",  // namespace
                "unknown"  // elementName - will be updated when setElementDef is called
            );
        }
    }

    /**
     * Override setElementDef to prevent creating new plugin instances.
     * Instead, update the existing plugin's configuration.
     */
    @Override
    public void setElementDef(IElementDef elementDef) {
        System.out.println("PluginProcessorAdapter.setElementDef() OVERRIDE called with elementDef=" + elementDef);
        
        // Store elementDef - accept any IElementDef for new plugins
        this.elementDef = elementDef;
        
        // If plugin is already created, update its configuration
        if (plugin != null) {
            System.out.println("PluginProcessorAdapter.setElementDef() OVERRIDE: plugin is not null, updating configuration");
            // Set element definition on the plugin for compatibility with old architecture
            plugin.setElementDef(elementDef);
            // Update configuration for new @CorePlugin architecture
            updatePluginConfiguration();
            try {
                if (pluginConfiguration != null) {
                    plugin.cleanup();
                    plugin.initialize(pluginConfiguration);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to re-initialize plugin with updated configuration", e);
            }
        } else {
            System.out.println("PluginProcessorAdapter.setElementDef() OVERRIDE: plugin is null, cannot update configuration");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (plugin != null) {
            return "PluginProcessorAdapter[" + plugin.getClass().getSimpleName() + "]";
        } else {
            return "PluginProcessorAdapter[not-initialized]";
        }
    }
}