/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.plugin.registry;

import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Registry for managing WebHarvest plugins.
 * Provides functionality to register, discover, and manage plugins.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public interface PluginRegistry {

    /**
     * Registers a plugin in the registry.
     * 
     * @param plugin the plugin to register
     * @throws PluginException if registration fails
     */
    void registerPlugin(Plugin plugin) throws PluginException;

    /**
     * Registers a plugin class in the registry.
     * The plugin will be instantiated when needed.
     * 
     * @param pluginClass the plugin class to register
     * @throws PluginException if registration fails
     */
    void registerPlugin(Class<? extends Plugin> pluginClass) throws PluginException;

    /**
     * Unregisters a plugin from the registry.
     * 
     * @param pluginId the ID of the plugin to unregister
     * @throws PluginException if unregistration fails
     */
    void unregisterPlugin(String pluginId) throws PluginException;

    /**
     * Gets a plugin by its ID.
     * 
     * @param pluginId the plugin ID
     * @return the plugin, or null if not found
     */
    Plugin getPlugin(String pluginId);

    /**
     * Gets a plugin by namespace and element name.
     * 
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @return the plugin, or null if not found
     */
    Plugin getPlugin(String namespace, String elementName);

    /**
     * Gets all registered plugins.
     * 
     * @return collection of all registered plugins
     */
    Collection<Plugin> getAllPlugins();

    /**
     * Gets plugins by category.
     * 
     * @param category the plugin category
     * @return list of plugins in the category
     */
    List<Plugin> getPluginsByCategory(String category);

    /**
     * Gets plugins by namespace.
     * 
     * @param namespace the plugin namespace
     * @return list of plugins in the namespace
     */
    List<Plugin> getPluginsByNamespace(String namespace);

    /**
     * Gets plugin metadata by ID.
     * 
     * @param pluginId the plugin ID
     * @return plugin metadata, or null if not found
     */
    PluginMetadata getPluginMetadata(String pluginId);

    /**
     * Gets all plugin metadata.
     * 
     * @return collection of all plugin metadata
     */
    Collection<PluginMetadata> getAllPluginMetadata();

    /**
     * Searches for plugins by query.
     * 
     * @param query the search query
     * @return list of matching plugins
     */
    List<Plugin> searchPlugins(String query);

    /**
     * Checks if a plugin is registered.
     * 
     * @param pluginId the plugin ID
     * @return true if registered, false otherwise
     */
    boolean isPluginRegistered(String pluginId);

    /**
     * Checks if a plugin is registered by namespace and element name.
     * 
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @return true if registered, false otherwise
     */
    boolean isPluginRegistered(String namespace, String elementName);

    /**
     * Gets the number of registered plugins.
     * 
     * @return number of registered plugins
     */
    int getPluginCount();

    /**
     * Gets plugin statistics.
     * 
     * @return map of plugin statistics
     */
    Map<String, Object> getPluginStatistics();

    /**
     * Clears all registered plugins.
     */
    void clear();

    /**
     * Refreshes the plugin registry.
     * This may reload plugins or update the registry state.
     * 
     * @throws PluginException if refresh fails
     */
    void refresh() throws PluginException;
}


