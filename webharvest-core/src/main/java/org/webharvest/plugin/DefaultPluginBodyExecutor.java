/*  Copyright (c) 2006-2025, Robert Bala
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
*/
package org.webharvest.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.CorePluginRegistry;
import org.webharvest.ioc.InjectorHelper;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import com.google.inject.Injector;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of PluginBodyExecutor for new architecture (v2.2.x).
 * 
 * This implementation uses PluginConfiguration.getChildren() to execute child plugins
 * without relying on the old IElementDef/Processor architecture.
 * 
 * <p><b>v2.2.0 CRITICAL FIX (October 2025):</b> Plugin instances are now created via Guice
 * {@link InjectorHelper#getInjector()}.getInstance() instead of reflection ({@code newInstance()}).
 * This ensures that {@code @Inject} dependencies (like {@code HttpService}) are properly injected.
 * Previously, plugins with dependencies would fail with {@code NullPointerException} or 
 * {@code IllegalStateException}.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version 2.2.0
 */
public class DefaultPluginBodyExecutor implements PluginBodyExecutor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginBodyExecutor.class);
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Variable executeBody(PluginConfiguration config, DynamicScopeContext context) 
            throws InterruptedException {
        
        if (config == null || !config.hasChildren()) {
            LOGGER.debug("No children to execute in configuration");
            return EmptyVariable.INSTANCE;
        }
        
        LOGGER.debug("Executing body with {} children", config.getChildren().size());
        
        final List<Object> results = new ArrayList<>();
        
        for (PluginConfiguration childConfig : config.getChildren()) {
            try {
                Variable childResult = executeChildPlugin(childConfig, context);
                if (childResult != null && !(childResult instanceof EmptyVariable)) {
                    results.add(childResult);
                }
            } catch (IllegalArgumentException e) {
                if (e.getCause() instanceof org.webharvest.plugin.exceptions.PluginValidationException) {
                    // Invalid configuration is a hard failure.
                    throw e;
                }
                LOGGER.error("Failed to execute child plugin: {}", childConfig.getElementName(), e);
            } catch (Exception e) {
                LOGGER.error("Failed to execute child plugin: {}", childConfig.getElementName(), e);
                // Preserve historical body behavior for non-validation runtime failures:
                // continue with remaining siblings.
            }
        }
        
        // Always return ListVariable (even if empty) for consistency with old architecture
        return new ListVariable(results);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Variable getBodyTextContent(PluginConfiguration config, DynamicScopeContext context) 
            throws InterruptedException {
        
        Variable bodyResult = executeBody(config, context);
        
        if (bodyResult == null || bodyResult instanceof EmptyVariable) {
            return new NodeVariable("");
        }
        
        // Convert result to text
        String textContent = bodyResult.toString();
        return new NodeVariable(textContent);
    }
    
    /**
     * Executes a single child plugin.
     * 
     * @param childConfig child plugin configuration
     * @param context execution context
     * @return execution result
     * @throws InterruptedException if interrupted
     */
    private Variable executeChildPlugin(PluginConfiguration childConfig, DynamicScopeContext context) 
            throws InterruptedException {
        
        LOGGER.debug("Executing child plugin: {}", childConfig.getElementName());
        
        // Get plugin class from registry
        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        Class<? extends Plugin> pluginClass = registry.getPluginClass(childConfig.getElementName());
        
        if (pluginClass == null) {
            LOGGER.warn("Plugin class not found for element: {}", childConfig.getElementName());
            return EmptyVariable.INSTANCE;
        }
        
        try {
            // v2.2.0 CRITICAL FIX: Use Guice Injector to create plugin instances
            // This ensures @Inject dependencies (like HttpService) are properly injected!
            // OLD: Plugin childPlugin = pluginClass.newInstance(); // ❌ No dependency injection!
            // NEW: Use InjectorHelper to get Guice-managed instance
            Plugin childPlugin;
            Injector injector = InjectorHelper.getInjector();
            if (injector != null) {
                // Use Guice to create instance (with dependency injection)
                childPlugin = injector.getInstance(pluginClass);
                LOGGER.debug("Created plugin {} via Guice injector", childConfig.getElementName());
            } else {
                // Fallback to reflection (for tests without Guice)
                childPlugin = pluginClass.newInstance();
                LOGGER.warn("Created plugin {} via reflection (no Guice injector available)", 
                           childConfig.getElementName());
            }
            
            // Initialize plugin
            childPlugin.initialize(childConfig);
            
            // Execute plugin
            Variable result = childPlugin.execute(context);
            
            LOGGER.debug("Child plugin {} executed successfully", childConfig.getElementName());
            
            return result;
            
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Failed to create plugin instance: {}", pluginClass.getName(), e);
            return EmptyVariable.INSTANCE;
        } catch (org.webharvest.plugin.exceptions.PluginValidationException e) {
            LOGGER.error("Invalid configuration for plugin {}: {}", childConfig.getElementName(), e.getMessage());
            throw new IllegalArgumentException(
                    "Invalid configuration for plugin '" + childConfig.getElementName() + "': " + e.getMessage(),
                    e);
        } catch (org.webharvest.plugin.exceptions.PluginException e) {
            LOGGER.error("Failed to initialize plugin: {}", childConfig.getElementName(), e);
            return EmptyVariable.INSTANCE;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Failed to execute plugin: {}", childConfig.getElementName(), e);
            throw e;
        }
    }
}

