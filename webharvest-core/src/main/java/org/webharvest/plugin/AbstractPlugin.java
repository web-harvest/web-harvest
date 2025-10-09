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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin;

import org.webharvest.definition.IElementDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.ExecutableBodyContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all Web-Harvest plugins.
 * Provides common functionality and template methods for plugin lifecycle management.
 * 
 * <p>
 * This class implements the Template Method pattern, defining the common execution flow
 * while allowing subclasses to implement specific behavior through abstract methods.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public abstract class AbstractPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlugin.class);

    private boolean initialized = false;
    private PluginConfiguration configuration;
    private IElementDef elementDef;
    private Plugin parentPlugin;
    private int runningLevel = 1;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void initialize(PluginConfiguration configuration) throws PluginException {
        if (initialized) {
            throw new PluginException("Plugin is already initialized");
        }
        
        if (configuration == null) {
            throw new PluginException("Configuration cannot be null");
        }
        
        this.configuration = configuration;
        
        try {
            doInitialize(configuration);
            this.initialized = true;
        } catch (Exception e) {
            throw new PluginException("Failed to initialize plugin: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Variable execute(DynamicScopeContext context) throws InterruptedException {
        if (!initialized) {
            throw new IllegalStateException("Plugin must be initialized before execution");
        }
        
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        
        try {
            return doExecute(context);
        } catch (Exception e) {
            throw new RuntimeException("Plugin execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void validateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        doValidateConfiguration(configuration);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        if (initialized) {
            doCleanup();
            this.initialized = false;
            this.configuration = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IElementDef getElementDef() {
        return elementDef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setElementDef(IElementDef elementDef) {
        this.elementDef = elementDef;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Plugin getParentPlugin() {
        return parentPlugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentPlugin(Plugin parentPlugin) {
        this.parentPlugin = parentPlugin;
        if (parentPlugin != null) {
            this.runningLevel = parentPlugin.getRunningLevel() + 1;
        } else {
            this.runningLevel = 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRunningLevel() {
        return runningLevel;
    }

    /**
     * Gets the plugin configuration.
     * 
     * @return the configuration
     */
    protected PluginConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Template method for plugin-specific initialization.
     * Subclasses should override this method to implement their initialization logic.
     * 
     * @param configuration the plugin configuration
     * @throws PluginException if initialization fails
     */
    protected abstract void doInitialize(PluginConfiguration configuration) throws PluginException;

    /**
     * Template method for plugin-specific execution.
     * Subclasses must implement this method to define their execution behavior.
     * 
     * @param context the execution context
     * @return the execution result
     * @throws InterruptedException if execution is interrupted
     */
    protected abstract Variable doExecute(DynamicScopeContext context) throws InterruptedException;

    /**
     * Template method for plugin-specific configuration validation.
     * Subclasses should override this method to implement their validation logic.
     * 
     * @param configuration the configuration to validate
     * @throws PluginValidationException if validation fails
     */
    protected abstract void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException;

    /**
     * Template method for plugin-specific cleanup.
     * Subclasses should override this method to implement their cleanup logic.
     */
    protected void doCleanup() {
        // Default implementation does nothing
    }

    /**
     * Executes the body content of this plugin.
     * NEW ARCHITECTURE (v2.2.x): Uses PluginBodyExecutor instead of ExecutableBodyContentProvider.
     * Falls back to old architecture for backward compatibility.
     * 
     * @param context the execution context
     * @return the result of executing the body content
     * @throws InterruptedException if execution is interrupted
     */
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("AbstractPlugin.executeBody() called for plugin: {}", getClass().getSimpleName());
        
        // Null safety
        if (context == null) {
            LOGGER.warn("Context is null in executeBody()");
            return EmptyVariable.INSTANCE;
        }
        
        // NEW ARCHITECTURE (v2.2.x): Use PluginBodyExecutor with PluginConfiguration.getChildren()
        if (configuration != null && Boolean.TRUE.equals(configuration.hasChildren())) {
            LOGGER.debug("Using NEW architecture: PluginBodyExecutor with {} children", configuration.getChildren().size());
            try {
                PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
                return executor.executeBody(configuration, context);
            } catch (Exception e) {
                LOGGER.warn("PluginBodyExecutor failed: {}", e.getMessage());
                // Fall through to old architecture
            }
        }
        
        // FALLBACK: Old architecture (if elementDef is set but configuration has no children)
        // This supports backward compatibility during migration
        if (elementDef != null && elementDef.hasOperations()) {
            LOGGER.debug("Using OLD architecture fallback: ExecutableBodyContentProvider");
            try {
                ExecutableBodyContentProvider bodyProvider = new ExecutableBodyContentProvider(elementDef);
                Variable result = bodyProvider.executeBody(context);
                LOGGER.debug("Body execution completed with result: {}", result);
                return result;
            } catch (Exception e) {
                LOGGER.warn("Body execution failed: {}", e.getMessage());
                return EmptyVariable.INSTANCE;
            }
        }
        
        LOGGER.debug("No body content to execute");
        return EmptyVariable.INSTANCE;
    }
}