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

    /** Internal marker identifying an Else paired with an adjacent sibling If. */
    public static final String SYNTHETIC_SIBLING_ELSE_PROPERTY = "__webharvest.runtime.siblingElse";
    
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

        validateSiblingElsePairs(config.getChildren());
        return executeChildren(config.getChildren(), context);
    }

    /**
     * Ensures each sibling Else belongs to exactly one immediately preceding If.
     * This validation runs before any child executes, so malformed control-flow
     * structures cannot partially mutate the execution context.
     *
     * @param children ordered child configurations
     */
    private void validateSiblingElsePairs(List<PluginConfiguration> children) {
        for (int i = 0; i < children.size(); i++) {
            if ("else".equals(children.get(i).getElementName())
                    && (i == 0 || !"if".equals(children.get(i - 1).getElementName()))) {
                throw new IllegalArgumentException(
                        "Else must immediately follow exactly one sibling If (child index " + i + ")");
            }
        }
    }

    /**
     * Executes an ordered list of child configurations. An adjacent sibling
     * {@code <else>} is paired with its preceding {@code <if>} so both XML
     * representations share the same conditional execution path.
     *
     * @param children ordered child configurations
     * @param context execution context
     * @return aggregated child results
     * @throws InterruptedException if child execution is interrupted
     */
    public Variable executeChildren(List<PluginConfiguration> children, DynamicScopeContext context)
            throws InterruptedException {

        if (children == null || children.isEmpty()) {
            return EmptyVariable.INSTANCE;
        }

        LOGGER.debug("Executing body with {} children", children.size());
        final List<Object> results = new ArrayList<>();

        for (int i = 0; i < children.size(); i++) {
            PluginConfiguration childConfig = children.get(i);

            // XML permits Else either as an If child or as the immediately
            // following sibling. Normalize the sibling form at execution time
            // without mutating the parsed configuration tree.
            if ("if".equals(childConfig.getElementName())
                    && i + 1 < children.size()
                    && "else".equals(children.get(i + 1).getElementName())) {
                childConfig = withSiblingElse(childConfig, children.get(i + 1));
                i++;
            }

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
            } catch (InterruptedException e) {
                // Cancellation aborts the current body; later siblings must not run.
                // The enclosing AbstractPlugin lifecycle restores the flag after cleanup.
                throw e;
            } catch (Exception e) {
                LOGGER.error("Failed to execute child plugin: {}", childConfig.getElementName(), e);
                // Preserve historical body behavior for non-validation runtime failures:
                // continue with remaining siblings.
            }
        }

        return new ListVariable(results);
    }

    private PluginConfiguration withSiblingElse(
            PluginConfiguration ifConfig, PluginConfiguration elseConfig) {
        List<PluginConfiguration> children = new ArrayList<>(ifConfig.getChildren());
        // Preserve explicit ownership when the flattened child list contains
        // both a nested If and this outer If's following sibling Else.
        DefaultPluginConfiguration ownedElse = new DefaultPluginConfiguration(
                elseConfig.getPluginId(),
                elseConfig.getNamespace(),
                elseConfig.getElementName(),
                elseConfig.getAllProperties());
        ownedElse.setChildren(elseConfig.getChildren());
        ownedElse.setProperty(SYNTHETIC_SIBLING_ELSE_PROPERTY, true);
        children.add(ownedElse);

        DefaultPluginConfiguration paired = new DefaultPluginConfiguration(
                ifConfig.getPluginId(),
                ifConfig.getNamespace(),
                ifConfig.getElementName(),
                ifConfig.getAllProperties());
        paired.setChildren(children);
        return paired;
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
        Plugin childPlugin = createChildPlugin(childConfig);

        Variable result = childPlugin.execute(context);
        LOGGER.debug("Child plugin {} executed successfully", childConfig.getElementName());
        return result;
    }

    /**
     * Creates and initializes a child plugin using the same registry and
     * dependency-injection path as ordinary body execution. Conditional
     * containers can use this when they must inspect a branch before execution.
     *
     * @param childConfig child plugin configuration
     * @return initialized plugin
     * @throws IllegalArgumentException if no plugin is registered for the element
     * @throws IllegalStateException if plugin construction or initialization fails
     */
    public Plugin createChildPlugin(PluginConfiguration childConfig) {
        if (childConfig == null) {
            throw new IllegalArgumentException("Child plugin configuration cannot be null");
        }

        CorePluginRegistry registry = CorePluginRegistry.getInstance();
        Class<? extends Plugin> pluginClass = registry.getPluginClass(childConfig.getElementName());
        if (pluginClass == null) {
            throw new IllegalArgumentException(
                    "No plugin registered for element '" + childConfig.getElementName() + "'");
        }

        try {
            Plugin childPlugin;
            Injector injector = InjectorHelper.getInjector();
            if (injector != null) {
                childPlugin = injector.getInstance(pluginClass);
                LOGGER.debug("Created plugin {} via Guice injector", childConfig.getElementName());
            } else {
                childPlugin = pluginClass.newInstance();
                LOGGER.warn("Created plugin {} via reflection (no Guice injector available)",
                        childConfig.getElementName());
            }

            childPlugin.initialize(childConfig);
            return childPlugin;
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Failed to create plugin instance: {}", pluginClass.getName(), e);
            throw new IllegalStateException(
                    "Failed to create plugin '" + childConfig.getElementName() + "'", e);
        } catch (org.webharvest.plugin.exceptions.PluginValidationException e) {
            LOGGER.error("Invalid configuration for plugin {}: {}", childConfig.getElementName(), e.getMessage());
            throw new IllegalArgumentException(
                    "Invalid configuration for plugin '" + childConfig.getElementName() + "': " + e.getMessage(),
                    e);
        } catch (org.webharvest.plugin.exceptions.PluginException e) {
            LOGGER.error("Failed to initialize plugin: {}", childConfig.getElementName(), e);
            throw new IllegalStateException(
                    "Failed to initialize plugin '" + childConfig.getElementName() + "'", e);
        }
    }

}

