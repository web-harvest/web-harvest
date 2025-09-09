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

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for Web-Harvest plugins.
 * Provides common functionality and default implementations for plugin lifecycle methods.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public abstract class AbstractPlugin implements Plugin {

    protected PluginMetadata metadata;
    protected PluginConfiguration configuration;
    protected boolean initialized = false;

    /**
     * Constructor that initializes plugin metadata.
     * 
     * @param metadata plugin metadata
     */
    protected AbstractPlugin(PluginMetadata metadata) {
        this.metadata = metadata;
    }

    @Override
    public final Variable execute(DynamicScopeContext context) throws InterruptedException {
        if (!isInitialized()) {
            throw new IllegalStateException("Plugin not initialized: " + getMetadata().getId());
        }
        
        try {
            return doExecute(context);
        } catch (InterruptedException e) {
            throw e; // Re-throw InterruptedException
        } catch (Exception e) {
            throw new RuntimeException("Plugin execution failed: " + getMetadata().getId(), e);
        }
    }

    @Override
    public void initialize(PluginConfiguration configuration) throws PluginException {
        if (isInitialized()) {
            throw new IllegalStateException("Plugin already initialized: " + getMetadata().getId());
        }
        
        this.configuration = configuration;
        validateConfiguration(configuration);
        
        try {
            doInitialize(configuration);
            this.initialized = true;
        } catch (Exception e) {
            throw new PluginException("Plugin initialization failed: " + getMetadata().getId(), e);
        }
    }

    @Override
    public void validateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Plugin configuration cannot be null");
        }
        
        PluginConfigurationSchema schema = getMetadata().getConfigurationSchema();
        if (schema != null) {
            schema.validate(configuration);
        }
        
        doValidateConfiguration(configuration);
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void cleanup() {
        if (isInitialized()) {
            try {
                doCleanup();
            } catch (Exception e) {
                // Log error but don't throw exception during cleanup
                System.err.println("Error during plugin cleanup: " + e.getMessage());
            } finally {
                this.initialized = false;
                this.configuration = null;
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the plugin configuration.
     * 
     * @return plugin configuration
     */
    protected PluginConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Gets a configuration property value.
     * 
     * @param name property name
     * @return property value, or null if not found
     */
    protected String getProperty(String name) {
        return configuration != null ? configuration.getProperty(name) : null;
    }

    /**
     * Gets a configuration property value with default.
     * 
     * @param name property name
     * @param defaultValue default value
     * @return property value or default value
     */
    protected String getProperty(String name, String defaultValue) {
        return configuration != null ? configuration.getProperty(name, defaultValue) : defaultValue;
    }

    /**
     * Gets a boolean configuration property.
     * 
     * @param name property name
     * @param defaultValue default value
     * @return boolean property value
     */
    protected boolean getBooleanProperty(String name, boolean defaultValue) {
        return configuration != null ? configuration.getBooleanProperty(name, defaultValue) : defaultValue;
    }

    /**
     * Gets an integer configuration property.
     * 
     * @param name property name
     * @param defaultValue default value
     * @return integer property value
     */
    protected int getIntProperty(String name, int defaultValue) {
        return configuration != null ? configuration.getIntProperty(name, defaultValue) : defaultValue;
    }

    /**
     * Gets a long configuration property.
     * 
     * @param name property name
     * @param defaultValue default value
     * @return long property value
     */
    protected long getLongProperty(String name, long defaultValue) {
        return configuration != null ? configuration.getLongProperty(name, defaultValue) : defaultValue;
    }

    /**
     * Gets a double configuration property.
     * 
     * @param name property name
     * @param defaultValue default value
     * @return double property value
     */
    protected double getDoubleProperty(String name, double defaultValue) {
        return configuration != null ? configuration.getDoubleProperty(name, defaultValue) : defaultValue;
    }

    /**
     * Template method for plugin execution.
     * Subclasses must implement this method to provide plugin-specific logic.
     * 
     * @param context execution context
     * @return execution result
     * @throws InterruptedException if execution is interrupted
     */
    protected abstract Variable doExecute(DynamicScopeContext context) throws InterruptedException;

    /**
     * Template method for plugin initialization.
     * Subclasses can override this method to perform custom initialization.
     * 
     * @param configuration plugin configuration
     * @throws Exception if initialization fails
     */
    protected void doInitialize(PluginConfiguration configuration) throws Exception {
        // Default implementation does nothing
    }

    /**
     * Template method for custom configuration validation.
     * Subclasses can override this method to perform additional validation.
     * 
     * @param configuration plugin configuration
     * @throws PluginValidationException if validation fails
     */
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Default implementation does nothing
    }

    /**
     * Template method for plugin cleanup.
     * Subclasses can override this method to perform custom cleanup.
     * 
     * @throws Exception if cleanup fails
     */
    protected void doCleanup() throws Exception {
        // Default implementation does nothing
    }

    /**
     * Validates that a required property is present.
     * 
     * @param name property name
     * @throws PluginValidationException if property is missing
     */
    protected void validateRequiredProperty(String name) throws PluginValidationException {
        if (configuration == null || !configuration.hasProperty(name)) {
            throw new PluginValidationException("Required property missing: " + name);
        }
    }

    /**
     * Validates that a property value is not empty.
     * 
     * @param name property name
     * @throws PluginValidationException if property is empty
     */
    protected void validateNonEmptyProperty(String name) throws PluginValidationException {
        String value = getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            throw new PluginValidationException("Property cannot be empty: " + name);
        }
    }

    /**
     * Validates that a property value matches a pattern.
     * 
     * @param name property name
     * @param pattern regex pattern
     * @throws PluginValidationException if property doesn't match pattern
     */
    protected void validatePropertyPattern(String name, String pattern) throws PluginValidationException {
        String value = getProperty(name);
        if (value != null && !value.matches(pattern)) {
            throw new PluginValidationException("Property value does not match pattern: " + name + " = " + value);
        }
    }
}
