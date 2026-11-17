/*
 * Copyright (c) 2006-2025 the original author or authors.
 *
 * Redistribution and use of this software in source and binary forms,
 * with or without modification, are permitted provided that the following
 * conditions are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following disclaimer
 *   in the documentation and/or other materials provided with the distribution.
 *
 * * The name of Web-Harvest may not be used to endorse or promote
 *   products derived from this software without specific prior written permission.
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

package org.webharvest.definition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.webharvest.plugin.Plugin;

/**
 * Registry that maps processor classes to their corresponding plugin classes.
 * This allows the system to know which plugin to instantiate for a given processor.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public final class CorePluginRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorePluginRegistry.class);

    private static final CorePluginRegistry INSTANCE = new CorePluginRegistry();

    private final Map<String, Class<? extends Plugin>> elementNameToPluginMap =
        new ConcurrentHashMap<>();

    /**
     * Private constructor for singleton.
     */
    private CorePluginRegistry() {
        // Private constructor
    }

    /**
     * Gets the singleton instance.
     *
     * @return the registry instance
     */
    public static CorePluginRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a mapping between a processor class and a plugin class.
     *
     * @param processorClass the processor class
     * @param pluginClass the plugin class
     * @param elementName the element name
     */
    public void registerMapping(Class processorClass,
                               Class<? extends Plugin> pluginClass,
                               String elementName) {
        LOGGER.debug("CorePluginRegistry.registerMapping: " + elementName + " -> " + pluginClass.getName());
        elementNameToPluginMap.put(elementName, pluginClass);
    }

    /**
     * Checks if a plugin is registered for the given element name.
     *
     * @param elementName the element name
     * @return true if registered, false otherwise
     */
    public boolean isPluginRegistered(String elementName) {
        return elementNameToPluginMap.containsKey(elementName);
    }

    /**
     * Gets the plugin class for the given element name.
     *
     * @param elementName the element name
     * @return the plugin class, or null if not found
     */
    public Class<? extends Plugin> getPluginClass(String elementName) {
        return elementNameToPluginMap.get(elementName);
    }

    /**
     * Returns an immutable snapshot of the registered XML element names.
     *
     * <p>Package-private to keep registry enumeration out of the public API while
     * allowing the registrar contract test to verify the complete mapping set.</p>
     *
     * @return an immutable snapshot of registered element names
     */
    java.util.Set<String> getRegisteredElementNames() {
        return java.util.Collections.unmodifiableSet(
                new java.util.HashSet<>(elementNameToPluginMap.keySet()));
    }

    /**
     * Clears all mappings.
     */
    public void clear() {
        elementNameToPluginMap.clear();
    }
}
