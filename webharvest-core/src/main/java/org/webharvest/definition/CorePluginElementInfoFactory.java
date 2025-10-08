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

import org.webharvest.exception.PluginException;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.processors.PluginProcessorAdapter;
import org.webharvest.utils.CommonUtil;

/**
 * Factory class responsible for creating {@link ElementInfo} instances
 * from core plugins annotated with {@link CorePlugin}.
 * This factory handles the new plugin architecture plugins.
 *
 * @author Robert Bala
 * @since 2.1.0
 * @version %I%, %G%
 */
public final class CorePluginElementInfoFactory {

    /**
     * Class constructor preventing instantiation.
     */
    private CorePluginElementInfoFactory() {
        // Do nothing constructor
    }

    /**
     * Creates new {@link ElementInfo} object from core plugin class that is about
     * to be registered.
     *
     * @param pluginClass
     *            core plugin class to register.
     * @return ElementInfo for the core plugin
     * @throws PluginException if the plugin class is invalid
     */
    @SuppressWarnings("unchecked")
    public static ElementInfo getElementInfo(
            final Class<? extends Plugin> pluginClass) {
        if (pluginClass == null) {
            throw new IllegalArgumentException("Plugin class is required");
        }

        final CorePlugin annotation = pluginClass.getAnnotation(CorePlugin.class);
        if (annotation == null) {
            throw new PluginException("Plugin class \"" + pluginClass.getName() + 
                                    "\" is not annotated with @CorePlugin");
        }

        final String elementName = annotation.elementName();
        
        if (!CommonUtil.isValidXmlIdentifier(elementName)) {
            throw new PluginException("Plugin class \"" + pluginClass.getName() + 
                                    "\" does not define valid element name: " + elementName);
        }

        // Create ElementInfo using the WebHarvestPluginDef
        // This allows new Plugin instances to work with the old Processor system
        return new ElementInfo(
            elementName,                              // name
            WebHarvestPluginDef.class,               // definitionClass
            getProcessorClassForPlugin(pluginClass), // processorClass (specific adapter)
            null,                                     // validTags (allow all)
            null,                                     // validAtts (allow all)
            true,                                     // isInternal (core plugins are internal)
            new Class[0]                              // dependantProcessors (none for now)
        );
    }

    /**
     * Gets the appropriate processor class for the given plugin class.
     * This creates a mapping between new plugins and their processor adapters.
     *
     * @param pluginClass the plugin class
     * @return the processor class
     */
    @SuppressWarnings("unchecked")
    private static Class<? extends org.webharvest.runtime.processors.Processor> 
            getProcessorClassForPlugin(final Class<? extends Plugin> pluginClass) {
        
        // Use the generic PluginProcessorAdapter for all core plugins
        // This allows new plugins to work with the old Processor system
        return (Class<? extends org.webharvest.runtime.processors.Processor>) 
            org.webharvest.runtime.processors.PluginProcessorAdapter.class;
    }
}