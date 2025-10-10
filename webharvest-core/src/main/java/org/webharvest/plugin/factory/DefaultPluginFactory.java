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
package org.webharvest.plugin.factory;

import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.registry.PluginRegistry;
import org.webharvest.plugin.exceptions.PluginException;
// REMOVED in v2.2.x - MockPlugin no longer exists
// import org.webharvest.plugin.mock.MockPlugin;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Default implementation of PluginFactory.
 * Provides functionality to create plugin instances with proper configuration and dependency injection.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class DefaultPluginFactory implements PluginFactory {

    private final PluginRegistry pluginRegistry;
    private final Map<Class<? extends Plugin>, Constructor<? extends Plugin>> constructorCache = new ConcurrentHashMap<>();

    /**
     * Constructor that requires a plugin registry.
     * 
     * @param pluginRegistry the plugin registry to use
     */
    public DefaultPluginFactory(PluginRegistry pluginRegistry) {
        if (pluginRegistry == null) {
            throw new IllegalArgumentException("Plugin registry cannot be null");
        }
        this.pluginRegistry = pluginRegistry;
    }

    @Override
    public Plugin createPlugin(Class<? extends Plugin> pluginClass) throws PluginException {
        return createPlugin(pluginClass, null);
    }

    @Override
    public Plugin createPlugin(Class<? extends Plugin> pluginClass, PluginConfiguration configuration) throws PluginException {
        if (pluginClass == null) {
            throw new PluginException("Plugin class cannot be null");
        }

        try {
            Plugin plugin = instantiatePlugin(pluginClass);
            
            if (configuration != null) {
                plugin.initialize(configuration);
            }
            
            return plugin;
        } catch (Exception e) {
            throw new PluginException("Failed to create plugin: " + pluginClass.getName(), e);
        }
    }

    @Override
    public Plugin createPlugin(String pluginId) throws PluginException {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("Plugin ID cannot be null or empty");
        }

        Plugin existingPlugin = pluginRegistry.getPlugin(pluginId);
        if (existingPlugin != null) {
            // Return a copy of the existing plugin
            return this.createPlugin(existingPlugin.getClass(), (PluginConfiguration) null);
        }

        throw new PluginException("Plugin not found: " + pluginId);
    }

    @Override
    public Plugin createPlugin(String pluginId, PluginConfiguration configuration) throws PluginException {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("Plugin ID cannot be null or empty");
        }

        Plugin existingPlugin = pluginRegistry.getPlugin(pluginId);
        if (existingPlugin != null) {
            // Return a copy of the existing plugin
            return this.createPlugin(existingPlugin.getClass(), configuration);
        }

        throw new PluginException("Plugin not found: " + pluginId);
    }

    @Override
    public Plugin createPlugin(String namespace, String elementName) throws PluginException {
        return createPlugin(namespace, elementName, null);
    }

    @Override
    public Plugin createPlugin(String namespace, String elementName, PluginConfiguration configuration) throws PluginException {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new PluginException("Namespace cannot be null or empty");
        }
        if (elementName == null || elementName.trim().isEmpty()) {
            throw new PluginException("Element name cannot be null or empty");
        }

        Plugin existingPlugin = pluginRegistry.getPlugin(namespace, elementName);
        if (existingPlugin != null) {
            // Return a copy of the existing plugin
            return this.createPlugin(existingPlugin.getClass(), configuration);
        }

        throw new PluginException("Plugin not found: " + namespace + ":" + elementName);
    }

    @Override
    public Plugin createMockPlugin(Class<? extends Plugin> pluginClass) throws PluginException {
        return createMockPlugin(pluginClass, null);
    }

    @Override
    public Plugin createMockPlugin(Class<? extends Plugin> pluginClass, PluginConfiguration configuration) throws PluginException {
        if (pluginClass == null) {
            throw new PluginException("Plugin class cannot be null");
        }

        try {
            // Create a mock plugin with basic information
            String className = pluginClass.getSimpleName();
            String pluginId = className.toLowerCase();
            String pluginName = className;
            
            // v2.2.x: MockPlugin removed, throw exception instead
            throw new PluginException("Cannot create mock plugin: MockPlugin class removed in v2.2.x. Plugin class: " + pluginClass.getName());
        } catch (Exception e) {
            throw new PluginException("Failed to create mock plugin: " + pluginClass.getName(), e);
        }
    }

    @Override
    public boolean canCreatePlugin(Class<? extends Plugin> pluginClass) {
        if (pluginClass == null) {
            return false;
        }

        try {
            // Check if we can get a constructor
            getConstructor(pluginClass);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Class<? extends Plugin> getPluginClass(String pluginId) {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            return null;
        }

        Plugin plugin = pluginRegistry.getPlugin(pluginId);
        return plugin != null ? plugin.getClass() : null;
    }

    @Override
    public Class<? extends Plugin> getPluginClass(String namespace, String elementName) {
        if (namespace == null || elementName == null) {
            return null;
        }

        Plugin plugin = pluginRegistry.getPlugin(namespace, elementName);
        return plugin != null ? plugin.getClass() : null;
    }

    /**
     * Instantiates a plugin using reflection.
     * 
     * @param pluginClass the plugin class
     * @return new plugin instance
     * @throws Exception if instantiation fails
     */
    private Plugin instantiatePlugin(Class<? extends Plugin> pluginClass) throws Exception {
        Constructor<? extends Plugin> constructor = getConstructor(pluginClass);
        
        if (constructor.getParameterCount() == 0) {
            // No-args constructor
            return constructor.newInstance();
        } else {
            // Constructor with parameters - try to find a suitable constructor
            Constructor<?>[] constructors = pluginClass.getConstructors();
            for (Constructor<?> ctor : constructors) {
                if (ctor.getParameterCount() == 1 && 
                    PluginMetadata.class.isAssignableFrom(ctor.getParameterTypes()[0])) {
                    // Constructor with PluginMetadata parameter
                    PluginMetadata metadata = createDefaultMetadata(pluginClass);
                    return (Plugin) ctor.newInstance(metadata);
                }
            }
            
            // Fallback to no-args constructor if available
            if (constructor.getParameterCount() == 0) {
                return constructor.newInstance();
            }
            
            throw new PluginException("No suitable constructor found for plugin: " + pluginClass.getName());
        }
    }

    /**
     * Gets or creates a constructor for the plugin class.
     * 
     * @param pluginClass the plugin class
     * @return constructor for the plugin class
     * @throws Exception if constructor cannot be found
     */
    private Constructor<? extends Plugin> getConstructor(Class<? extends Plugin> pluginClass) throws Exception {
        return constructorCache.computeIfAbsent(pluginClass, clazz -> {
            try {
                // Try to find a no-args constructor first
                Constructor<? extends Plugin> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } catch (NoSuchMethodException e) {
                // Try to find a constructor with PluginMetadata parameter
                try {
                    Constructor<? extends Plugin> constructor = clazz.getDeclaredConstructor(PluginMetadata.class);
                    constructor.setAccessible(true);
                    return constructor;
                } catch (NoSuchMethodException e2) {
                    throw new RuntimeException("No suitable constructor found for plugin: " + clazz.getName(), e2);
                }
            }
        });
    }

    /**
     * Creates default metadata for a plugin class.
     * 
     * @param pluginClass the plugin class
     * @return default metadata
     */
    private PluginMetadata createDefaultMetadata(Class<? extends Plugin> pluginClass) {
        String className = pluginClass.getSimpleName();
        String pluginId = className.toLowerCase();
        String pluginName = className;
        String description = "Plugin: " + className;
        String category = "default";
        String namespace = "http://webharvest.org/plugins";
        String elementName = className.toLowerCase();
        
        return new DefaultPluginMetadata(pluginId, pluginName, "1.0.0", description, 
                                       "Web-Harvest Team", category, 
                                       new String[]{"plugin"}, 
                                       new String[0], 
                                       null, namespace, elementName, false, className);
    }

    /**
     * Default implementation of PluginMetadata.
     */
    public static class DefaultPluginMetadata implements PluginMetadata {
        private final String id;
        private final String name;
        private final String version;
        private final String description;
        private final String author;
        private final String category;
        private final String[] tags;
        private final String[] dependencies;
        private final PluginConfigurationSchema configurationSchema;
        private final String namespace;
        private final String elementName;
        private final boolean corePlugin;
        private final String className;

        public DefaultPluginMetadata(String id, String name, String version, String description,
                                   String author, String category, String[] tags,
                                   String[] dependencies,
                                   PluginConfigurationSchema configurationSchema,
                                   String namespace, String elementName, boolean corePlugin,
                                   String className) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.description = description;
            this.author = author;
            this.category = category;
            this.tags = tags;
            this.dependencies = dependencies;
            this.configurationSchema = configurationSchema;
            this.namespace = namespace;
            this.elementName = elementName;
            this.corePlugin = corePlugin;
            this.className = className;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public String getAuthor() { return author; }
        public String getCategory() { return category; }
        public String[] getTags() { return tags; }
        public String[] getDependencies() { return dependencies; }
        public PluginConfigurationSchema getConfigurationSchema() { return configurationSchema; }
        public String getNamespace() { return namespace; }
        public String getElementName() { return elementName; }
        public boolean isCorePlugin() { return corePlugin; }
        public String getClassName() { return className; }
    }
}
