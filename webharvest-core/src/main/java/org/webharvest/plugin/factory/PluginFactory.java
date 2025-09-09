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
import org.webharvest.plugin.exceptions.PluginException;

/**
 * Factory for creating plugin instances.
 * Provides functionality to instantiate plugins with proper configuration and dependency injection.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public interface PluginFactory {

    /**
     * Creates a plugin instance from a plugin class.
     * 
     * @param pluginClass the plugin class
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(Class<? extends Plugin> pluginClass) throws PluginException;

    /**
     * Creates a plugin instance with configuration.
     * 
     * @param pluginClass the plugin class
     * @param configuration the plugin configuration
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(Class<? extends Plugin> pluginClass, PluginConfiguration configuration) throws PluginException;

    /**
     * Creates a plugin instance by plugin ID.
     * 
     * @param pluginId the plugin ID
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(String pluginId) throws PluginException;

    /**
     * Creates a plugin instance by plugin ID with configuration.
     * 
     * @param pluginId the plugin ID
     * @param configuration the plugin configuration
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(String pluginId, PluginConfiguration configuration) throws PluginException;

    /**
     * Creates a plugin instance by namespace and element name.
     * 
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(String namespace, String elementName) throws PluginException;

    /**
     * Creates a plugin instance by namespace and element name with configuration.
     * 
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @param configuration the plugin configuration
     * @return new plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createPlugin(String namespace, String elementName, PluginConfiguration configuration) throws PluginException;

    /**
     * Creates a mock plugin for testing.
     * 
     * @param pluginClass the plugin class
     * @return mock plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createMockPlugin(Class<? extends Plugin> pluginClass) throws PluginException;

    /**
     * Creates a mock plugin for testing with configuration.
     * 
     * @param pluginClass the plugin class
     * @param configuration the plugin configuration
     * @return mock plugin instance
     * @throws PluginException if creation fails
     */
    Plugin createMockPlugin(Class<? extends Plugin> pluginClass, PluginConfiguration configuration) throws PluginException;

    /**
     * Checks if a plugin class can be instantiated.
     * 
     * @param pluginClass the plugin class
     * @return true if can be instantiated, false otherwise
     */
    boolean canCreatePlugin(Class<? extends Plugin> pluginClass);

    /**
     * Gets the plugin class by ID.
     * 
     * @param pluginId the plugin ID
     * @return the plugin class, or null if not found
     */
    Class<? extends Plugin> getPluginClass(String pluginId);

    /**
     * Gets the plugin class by namespace and element name.
     * 
     * @param namespace the plugin namespace
     * @param elementName the element name
     * @return the plugin class, or null if not found
     */
    Class<? extends Plugin> getPluginClass(String namespace, String elementName);
}
