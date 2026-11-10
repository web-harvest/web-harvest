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

import com.google.common.eventbus.EventBus;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.events.PluginExecutionStartedEvent;
import org.webharvest.runtime.session.events.PluginExecutionCompletedEvent;
import org.webharvest.runtime.session.events.PluginExecutionFailedEvent;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for all WebHarvest plugins.
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

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractPlugin.class);

    private boolean initialized = false;
    private PluginConfiguration configuration;
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
        } catch (org.webharvest.plugin.exceptions.PluginValidationException e) {
            // Configuration errors must remain distinguishable so callers can fail hard.
            throw e;
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
        
        // Bug #24: Event Sourcing - Get EventBus and Session from context
        final EventBus eventBus = context.getEventBus();
        final ScraperSession session = context.getSession();
        final String pluginName = configuration != null ? configuration.getElementName() : getClass().getSimpleName();
        final long startTimeMs = System.currentTimeMillis();
        
        // Bug #24: Publish PluginExecutionStartedEvent
        if (eventBus != null && session != null) {
            try {
                eventBus.post(new PluginExecutionStartedEvent(session, pluginName, getClass()));
            } catch (Exception eventEx) {
                LOGGER.warn("Failed to publish PluginExecutionStartedEvent: {}", eventEx.getMessage());
                // Don't fail execution due to event publishing error
            }
        }
        
        // Bug #5 Fix: Wrap execution in try-finally to ensure cleanup always runs
        Variable result = null;
        Throwable executionError = null;
        try {
            result = doExecute(context);
            return result;
        } catch (Exception e) {
            executionError = e;
            
            // Bug #24: Publish PluginExecutionFailedEvent
            if (eventBus != null && session != null) {
                try {
                    long executionTimeMs = System.currentTimeMillis() - startTimeMs;
                    eventBus.post(new PluginExecutionFailedEvent(
                        session, pluginName, getClass(), executionTimeMs, e));
                    
                    // Record metrics
                    session.getMetrics().recordPluginExecution(pluginName, executionTimeMs);
                } catch (Exception eventEx) {
                    LOGGER.warn("Failed to publish PluginExecutionFailedEvent: {}", eventEx.getMessage());
                }
            }
            
            throw new RuntimeException("Plugin execution failed: " + e.getMessage(), e);
        } finally {
            // Bug #24: Publish PluginExecutionCompletedEvent on success
            if (executionError == null && eventBus != null && session != null) {
                try {
                    long executionTimeMs = System.currentTimeMillis() - startTimeMs;
                    eventBus.post(new PluginExecutionCompletedEvent(
                        session, pluginName, getClass(), executionTimeMs, result));
                    
                    // Record metrics
                    session.getMetrics().recordPluginExecution(pluginName, executionTimeMs);
                } catch (Exception eventEx) {
                    LOGGER.warn("Failed to publish PluginExecutionCompletedEvent: {}", eventEx.getMessage());
                }
            }
            
            // Bug #5: Cleanup must be called even when doExecute() throws exception
            // This ensures resources are released and metrics are recorded
            try {
                doCleanup();
            } catch (Exception cleanupEx) {
                LOGGER.warn("Cleanup failed for plugin {}: {}", 
                    getClass().getSimpleName(), cleanupEx.getMessage());
                // Don't re-throw cleanup exceptions - original exception takes precedence
            }
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

    // v2.2.x ULTRA-LEAN: Removed getElementDef/setElementDef
    // Old architecture methods eliminated

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
     * v2.2.x ULTRA-LEAN: Uses PluginBodyExecutor with PluginConfiguration.getChildren().
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
        
        // v2.2.x ULTRA-LEAN: Use PluginBodyExecutor with PluginConfiguration.getChildren()
        if (configuration != null && Boolean.TRUE.equals(configuration.hasChildren())) {
            LOGGER.debug("Using ULTRA-LEAN architecture: PluginBodyExecutor with {} children", configuration.getChildren().size());
            try {
                PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
                return executor.executeBody(configuration, context);
            } catch (Exception e) {
                LOGGER.error("PluginBodyExecutor failed: {}", e.getMessage(), e);
                return EmptyVariable.INSTANCE;
            }
        }
        
        LOGGER.debug("No body content to execute (no children)");
        return EmptyVariable.INSTANCE;
    }

    /**
     * Executes plugin body and extracts text content only (v2.2.x).
     * This method uses the new architecture (PluginBodyExecutor) with fallback to old architecture.
     * 
     * @param context the execution context
     * @return the text content from body execution
     * @throws InterruptedException if execution is interrupted
     * @since 2.2.0
     */
    protected Variable getBodyTextContent(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("AbstractPlugin.getBodyTextContent() called for plugin: {}", getClass().getSimpleName());
        
        // Null safety
        if (context == null) {
            LOGGER.warn("Context is null in getBodyTextContent()");
            return EmptyVariable.INSTANCE;
        }
        
        // v2.2.x CRITICAL FIX: Check _textContent FIRST before checking children!
        // StreamingXmlParser sets _textContent for text-only elements
        if (configuration != null) {
            String textContent = configuration.getProperty("_textContent");
            if (textContent != null && !textContent.trim().isEmpty()) {
                LOGGER.debug("AbstractPlugin.getBodyTextContent: Found _textContent: '{}'", textContent);
                // Apply template interpolation if BaseTemplater is available
                if (org.webharvest.runtime.templaters.BaseTemplater.scriptEngineFactory != null) {
                    return org.webharvest.runtime.templaters.BaseTemplater.evaluateToVariable(
                        textContent, context.getScriptingLanguage(), context);
                } else {
                    return new NodeVariable(textContent);
                }
            }
        }
        
        // v2.2.x ULTRA-LEAN: Use PluginBodyExecutor with PluginConfiguration.getChildren()
        if (configuration != null && Boolean.TRUE.equals(configuration.hasChildren())) {
            LOGGER.debug("Using ULTRA-LEAN architecture: PluginBodyExecutor.getBodyTextContent() with {} children", 
                        configuration.getChildren().size());
            try {
                PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
                return executor.getBodyTextContent(configuration, context);
            } catch (Exception e) {
                LOGGER.error("PluginBodyExecutor.getBodyTextContent() failed: {}", e.getMessage(), e);
                return EmptyVariable.INSTANCE;
            }
        }
        
        LOGGER.debug("No body text content to extract (no _textContent and no children)");
        return EmptyVariable.INSTANCE;
    }
}