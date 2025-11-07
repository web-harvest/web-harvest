/*  Copyright (c) 2006-2025 the original author or authors.
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
package org.webharvest.plugin;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.webharvest.plugin.semantic.PluginNodeRole;

/**
 * Metadata information about a plugin.
 * Contains all necessary information to identify, load, and configure a plugin.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public interface PluginMetadata {

    /**
     * Gets the unique plugin identifier.
     * 
     * @return plugin ID
     */
    String getId();

    /**
     * Gets the plugin name.
     * 
     * @return plugin name
     */
    String getName();

    /**
     * Gets the plugin version.
     * 
     * @return plugin version
     */
    String getVersion();

    /**
     * Gets the plugin description.
     * 
     * @return plugin description
     */
    String getDescription();

    /**
     * Gets the plugin author.
     * 
     * @return plugin author
     */
    String getAuthor();

    /**
     * Gets the plugin category.
     * 
     * @return plugin category
     */
    String getCategory();

    /**
     * Gets the plugin tags.
     * 
     * @return array of plugin tags
     */
    String[] getTags();

    /**
     * Gets the plugin dependencies.
     * 
     * @return array of dependency IDs
     */
    String[] getDependencies();

    /**
     * Gets the plugin configuration schema.
     * 
     * @return configuration schema
     */
    PluginConfigurationSchema getConfigurationSchema();

    /**
     * Declares the semantic roles associated with this plugin. Implementations can
     * override to provide richer metadata; by default no explicit roles are declared
     * and the semantic resolver will infer them heuristically.
     *
     * @return immutable set of semantic roles declared for this plugin
     */
    default Set<PluginNodeRole> getDeclaredRoles() {
        EnumSet<PluginNodeRole> roles = EnumSet.noneOf(PluginNodeRole.class);

        PluginConfigurationSchema schema = getConfigurationSchema();
        if (schema != null) {
            String[] requiredParents = schema.getRequiredParents();
            if (requiredParents != null && requiredParents.length > 0) {
                roles.add(PluginNodeRole.DEPENDENT);
            }

            if (schema.allowsBody()) {
                roles.add(PluginNodeRole.BODY_EVALUATOR);
            }

            String[] allowedSubElements = schema.getAllowedSubElements();
            if (allowedSubElements != null && allowedSubElements.length > 0) {
                roles.add(PluginNodeRole.CONTEXT_CONTAINER);
            }
        }

        if (this instanceof VisualPluginMetadata) {
            VisualPluginMetadata visualMetadata = (VisualPluginMetadata) this;
            if (visualMetadata.isDetailPlugin()) {
                roles.add(PluginNodeRole.DEPENDENT);
            }
            if (visualMetadata.isMasterPlugin()) {
                roles.add(PluginNodeRole.CONTEXT_CONTAINER);
            }
        }

        if (roles.isEmpty()) {
            roles.add(PluginNodeRole.LEAF);
        }

        return Collections.unmodifiableSet(roles);
    }

    /**
     * Gets the plugin namespace.
     * 
     * @return plugin namespace
     */
    String getNamespace();

    /**
     * Gets the plugin element name.
     * 
     * @return element name used in XML configuration
     */
    String getElementName();

    /**
     * Checks if the plugin is a core plugin.
     * 
     * @return true if core plugin, false otherwise
     */
    boolean isCorePlugin();

    /**
     * Gets the plugin class name.
     * 
     * @return fully qualified class name
     */
    String getClassName();
}
