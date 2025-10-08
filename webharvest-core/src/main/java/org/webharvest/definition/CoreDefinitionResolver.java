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
 * IMPLIECT WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
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

import org.webharvest.exception.ConfigurationException;
import org.webharvest.plugin.Plugin;
import org.webharvest.runtime.processors.PluginProcessorAdapter;

/**
 * New definition resolver that uses CorePluginRegistry for the new architecture.
 * This replaces the old DefinitionResolver for CorePlugin-based plugins.
 *
 * @author Robert Bala
 * @since 2.1.0
 */
public class CoreDefinitionResolver {

    /**
     * Creates element definition for CorePlugin-based plugins.
     *
     * @param node the XML node
     * @return element definition
     */
    public IElementDef createElementDefinition(XmlNode node) {
        final String nodeName = node.getName();
        final String nodeUri = node.getUri();

        System.out.println("CoreDefinitionResolver.createElementDefinition: nodeName=" + nodeName + ", nodeUri=" + nodeUri);

        // Check if plugin is registered in CorePluginRegistry
        if (CorePluginRegistry.getInstance().isPluginRegistered(nodeName)) {
            System.out.println("CoreDefinitionResolver.createElementDefinition: Plugin " + nodeName + " is registered in CorePluginRegistry");
            Class<? extends Plugin> pluginClass = CorePluginRegistry.getInstance().getPluginClass(nodeName);
            if (pluginClass != null) {
                System.out.println("CoreDefinitionResolver.createElementDefinition: Creating CorePluginDef for " + nodeName + " -> " + pluginClass.getName());
                
                // Use CorePluginDef which will create PluginProcessorAdapter internally
                CorePluginDef elementDef = new CorePluginDef(node, pluginClass, true);
                
                // Add child elements
                for (final Object element : node.getElementList()) {
                    elementDef.add(toElementDef(element));
                }
                
                return elementDef;
            }
        }

        System.out.println("CoreDefinitionResolver.createElementDefinition: Plugin " + nodeName + " is NOT registered in CorePluginRegistry");
        throw new ConfigurationException("Unexpected configuration element (URI, name): (" + nodeUri + "," + nodeName + ")!");
    }
    
    /**
     * Converts an object to an element definition.
     * This method delegates to the main DefinitionResolver for consistency.
     * 
     * @param subject the object to convert
     * @return element definition
     */
    private IElementDef toElementDef(final Object subject) {
        if (subject instanceof XmlNode) {
            return createElementDefinition((XmlNode) subject);
        } else {
            // For non-XmlNode objects, return null for now
            // TODO: Implement proper handling for other object types
            return null;
        }
    }
}
