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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Default implementation of PluginRegistry.
 * Provides thread-safe plugin registration and discovery functionality.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class DefaultPluginRegistry implements PluginRegistry {

    private final Map<String, Plugin> pluginsById = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends Plugin>> pluginClassesById = new ConcurrentHashMap<>();
    private final Map<String, Plugin> pluginsByNamespaceAndElement = new ConcurrentHashMap<>();
    private final Map<String, List<Plugin>> pluginsByCategory = new ConcurrentHashMap<>();
    private final Map<String, List<Plugin>> pluginsByNamespace = new ConcurrentHashMap<>();
    private final Map<String, PluginMetadata> metadataById = new ConcurrentHashMap<>();

    @Override
    public void registerPlugin(Plugin plugin) throws PluginException {
        if (plugin == null) {
            throw new PluginException("Plugin cannot be null");
        }

        PluginMetadata metadata = plugin.getMetadata();
        if (metadata == null) {
            throw new PluginException("Plugin metadata cannot be null");
        }

        String pluginId = metadata.getId();
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("Plugin ID cannot be null or empty");
        }

        if (pluginsById.containsKey(pluginId)) {
            throw new PluginException("Plugin already registered: " + pluginId);
        }

        // Register plugin
        pluginsById.put(pluginId, plugin);
        metadataById.put(pluginId, metadata);

        // Register by namespace and element name
        String namespace = metadata.getNamespace();
        String elementName = metadata.getElementName();
        if (namespace != null && elementName != null) {
            String key = namespace + ":" + elementName;
            pluginsByNamespaceAndElement.put(key, plugin);
        }

        // Register by category
        String category = metadata.getCategory();
        if (category != null) {
            pluginsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(plugin);
        }

        // Register by namespace
        if (namespace != null) {
            pluginsByNamespace.computeIfAbsent(namespace, k -> new ArrayList<>()).add(plugin);
        }
    }

    @Override
    public void registerPlugin(Class<? extends Plugin> pluginClass) throws PluginException {
        if (pluginClass == null) {
            throw new PluginException("Plugin class cannot be null");
        }

        try {
            Plugin plugin = pluginClass.newInstance();
            registerPlugin(plugin);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new PluginException("Failed to instantiate plugin class: " + pluginClass.getName(), e);
        }
    }

    @Override
    public void unregisterPlugin(String pluginId) throws PluginException {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("Plugin ID cannot be null or empty");
        }

        Plugin plugin = pluginsById.remove(pluginId);
        if (plugin == null) {
            throw new PluginException("Plugin not found: " + pluginId);
        }

        PluginMetadata metadata = plugin.getMetadata();
        if (metadata != null) {
            // Remove from namespace and element mapping
            String namespace = metadata.getNamespace();
            String elementName = metadata.getElementName();
            if (namespace != null && elementName != null) {
                String key = namespace + ":" + elementName;
                pluginsByNamespaceAndElement.remove(key);
            }

            // Remove from category mapping
            String category = metadata.getCategory();
            if (category != null) {
                List<Plugin> categoryPlugins = pluginsByCategory.get(category);
                if (categoryPlugins != null) {
                    categoryPlugins.remove(plugin);
                    if (categoryPlugins.isEmpty()) {
                        pluginsByCategory.remove(category);
                    }
                }
            }

            // Remove from namespace mapping
            if (namespace != null) {
                List<Plugin> namespacePlugins = pluginsByNamespace.get(namespace);
                if (namespacePlugins != null) {
                    namespacePlugins.remove(plugin);
                    if (namespacePlugins.isEmpty()) {
                        pluginsByNamespace.remove(namespace);
                    }
                }
            }
        }

        metadataById.remove(pluginId);
        pluginClassesById.remove(pluginId);
    }

    @Override
    public Plugin getPlugin(String pluginId) {
        return pluginsById.get(pluginId);
    }

    @Override
    public Plugin getPlugin(String namespace, String elementName) {
        if (namespace == null || elementName == null) {
            return null;
        }
        String key = namespace + ":" + elementName;
        return pluginsByNamespaceAndElement.get(key);
    }

    @Override
    public Collection<Plugin> getAllPlugins() {
        return new ArrayList<>(pluginsById.values());
    }

    @Override
    public List<Plugin> getPluginsByCategory(String category) {
        if (category == null) {
            return new ArrayList<>();
        }
        List<Plugin> plugins = pluginsByCategory.get(category);
        return plugins != null ? new ArrayList<>(plugins) : new ArrayList<>();
    }

    @Override
    public List<Plugin> getPluginsByNamespace(String namespace) {
        if (namespace == null) {
            return new ArrayList<>();
        }
        List<Plugin> plugins = pluginsByNamespace.get(namespace);
        return plugins != null ? new ArrayList<>(plugins) : new ArrayList<>();
    }

    @Override
    public PluginMetadata getPluginMetadata(String pluginId) {
        return metadataById.get(pluginId);
    }

    @Override
    public Collection<PluginMetadata> getAllPluginMetadata() {
        return new ArrayList<>(metadataById.values());
    }

    @Override
    public List<Plugin> searchPlugins(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerQuery = query.toLowerCase();
        return pluginsById.values().stream()
                .filter(plugin -> {
                    PluginMetadata metadata = plugin.getMetadata();
                    if (metadata == null) return false;
                    
                    return (metadata.getName() != null && metadata.getName().toLowerCase().contains(lowerQuery)) ||
                           (metadata.getDescription() != null && metadata.getDescription().toLowerCase().contains(lowerQuery)) ||
                           (metadata.getId() != null && metadata.getId().toLowerCase().contains(lowerQuery)) ||
                           (metadata.getCategory() != null && metadata.getCategory().toLowerCase().contains(lowerQuery));
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isPluginRegistered(String pluginId) {
        return pluginId != null && pluginsById.containsKey(pluginId);
    }

    @Override
    public boolean isPluginRegistered(String namespace, String elementName) {
        if (namespace == null || elementName == null) {
            return false;
        }
        String key = namespace + ":" + elementName;
        return pluginsByNamespaceAndElement.containsKey(key);
    }

    @Override
    public int getPluginCount() {
        return pluginsById.size();
    }

    @Override
    public Map<String, Object> getPluginStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlugins", pluginsById.size());
        stats.put("categories", pluginsByCategory.size());
        stats.put("namespaces", pluginsByNamespace.size());
        
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (Map.Entry<String, List<Plugin>> entry : pluginsByCategory.entrySet()) {
            categoryCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("pluginsByCategory", categoryCounts);
        
        Map<String, Integer> namespaceCounts = new HashMap<>();
        for (Map.Entry<String, List<Plugin>> entry : pluginsByNamespace.entrySet()) {
            namespaceCounts.put(entry.getKey(), entry.getValue().size());
        }
        stats.put("pluginsByNamespace", namespaceCounts);
        
        return stats;
    }

    @Override
    public void clear() {
        pluginsById.clear();
        pluginClassesById.clear();
        pluginsByNamespaceAndElement.clear();
        pluginsByCategory.clear();
        pluginsByNamespace.clear();
        metadataById.clear();
    }

    @Override
    public void refresh() throws PluginException {
        // Default implementation does nothing
        // Subclasses can override to implement refresh logic
    }
}
