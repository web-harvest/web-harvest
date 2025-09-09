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
package org.webharvest.plugin;

import java.util.List;
import java.util.Map;

/**
 * Metadata information about a plugin.
 * Contains all necessary information to identify, load, and configure a plugin.
 * 
 * @author Web-Harvest Team
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
     * @return list of plugin tags
     */
    List<String> getTags();

    /**
     * Gets the plugin dependencies.
     * 
     * @return map of dependency ID to version requirement
     */
    Map<String, String> getDependencies();

    /**
     * Gets the plugin configuration schema.
     * 
     * @return configuration schema
     */
    PluginConfigurationSchema getConfigurationSchema();

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
