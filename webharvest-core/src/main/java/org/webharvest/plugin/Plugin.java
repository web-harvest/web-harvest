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

import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.definition.IElementDef;

/**
 * Core interface for all Web-Harvest plugins.
 * This interface defines the contract that all plugins must implement
 * to be compatible with the Web-Harvest plugin system.
 * 
 * <p>
 * Plugins are independent, modular components that can be dynamically
 * loaded and configured. Each plugin represents a specific functionality
 * that can be used in Web-Harvest configurations.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public interface Plugin {

    /**
     * Executes the plugin with the given context.
     * 
     * @param context the execution context containing variables and configuration
     * @return the result of plugin execution
     * @throws InterruptedException if the execution is interrupted
     */
    Variable execute(DynamicScopeContext context) throws InterruptedException;

    /**
     * Gets the plugin metadata.
     * 
     * @return plugin metadata containing name, version, description, etc.
     */
    PluginMetadata getMetadata();

    /**
     * Initializes the plugin with the given configuration.
     * This method is called once when the plugin is loaded.
     * 
     * @param configuration plugin configuration
     * @throws PluginException if initialization fails
     */
    void initialize(PluginConfiguration configuration) throws PluginException;

    /**
     * Validates the plugin configuration.
     * 
     * @param configuration plugin configuration to validate
     * @throws PluginValidationException if configuration is invalid
     */
    void validateConfiguration(PluginConfiguration configuration) throws PluginValidationException;

    /**
     * Checks if the plugin is properly initialized.
     * 
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();

    /**
     * Cleans up resources used by the plugin.
     * This method is called when the plugin is unloaded.
     */
    void cleanup();

    /**
     * Gets the element definition for this plugin.
     * This method provides compatibility with the existing processor system.
     * 
     * @return the element definition
     */
    IElementDef getElementDef();

    /**
     * Sets the element definition for this plugin.
     * This method provides compatibility with the existing processor system.
     * 
     * @param elementDef the element definition to set
     */
    void setElementDef(IElementDef elementDef);

    /**
     * Gets the parent plugin in the execution hierarchy.
     * 
     * @return the parent plugin, or null if this is a root plugin
     */
    Plugin getParentPlugin();

    /**
     * Sets the parent plugin in the execution hierarchy.
     * 
     * @param parentPlugin the parent plugin
     */
    void setParentPlugin(Plugin parentPlugin);

    /**
     * Gets the running level in the plugin hierarchy.
     * Value of the lowest level is 1.
     * 
     * @return the running level
     */
    int getRunningLevel();
}
