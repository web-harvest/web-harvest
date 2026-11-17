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

package org.webharvest.plugin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception catch block processor - handles exceptions from {@code <try>} block.
 * 
 * <p>
 * The {@code <catch>} processor must be a child of {@code <try>} and executes only when an
 * ordinary runtime exception occurs in that Try body. It provides the message through
 * {@code ${error}} and the backward-compatible {@code ${_error}} alias
 * and enables graceful error handling, logging, and recovery.
 * </p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <try>
 *   <http url="https://unreliable-api.com/data"/>
 * </try>
 * <catch>
 *   <log message="API request failed: ${error}"/>
 *   <var name="fallback_data">Default data</var>
 * </catch>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>No parameters</strong>: Catch block executes when exception occurs</li>
 *   <li><strong>Error Variable</strong>: ${error} contains the exception message; ${_error} is a compatibility alias</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be a child of {@code <try>}</li>
 *   <li><strong>Error Variable</strong>: ${error} and ${_error} are available within the catch body</li>
 *   <li><strong>Exception Handling</strong>: Handles ordinary runtime failures; Include failures, validation failures and interruption bypass Catch</li>
 *   <li><strong>Recovery</strong>: Can define fallback values or alternative processing</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <try>} - Try block for exception handling</li>
 *   <li>{@code <log>} - Logging error messages</li>
 *   <li>{@code <var>} - Define fallback variables</li>
 * </ul>
 * 
 * <p>See {@link TryPlugin} for complete try-catch examples.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see TryPlugin
 */
@CorePlugin(
    elementName = "catch",
    description = "Catch plugin that provides the catch block for exception handling",
    version = "2.1.0",
    category = "Control"
)
public class CatchPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.CatchPlugin");

    /**
     * Metadata for CatchPlugin.
     * 
     * <p>Detail plugin - requires try as parent. Handles exceptions from try block.</p>
     */
    public static class CatchPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "catch";
        }

        @Override
        public String getName() {
            return "Catch";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes the Catch child when an ordinary runtime failure occurs in Try. Provides the message through ${error} and the compatibility alias ${_error}. Include failures, validation failures and interruption bypass Catch.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = CatchPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "catch", "exception", "error-handling", "try-catch", "detail"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return CatchPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "catch";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new CatchPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-exclamation-triangle";
        }
        
        @Override
        public String getColor() {
            return "#F59E0B"; // Amber for error handling detail
        }
        
        @Override
        public String getGroup() {
            return "Control Flow";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 160;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("error", "Error details from Try (${error}; compatibility alias ${_error})", PortType.STRING, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Result from body execution (error recovery)", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes body content when an exception occurs in the preceding try block. Provides access to error details via ${_error} variable. Essential for graceful error handling, logging, and fallback processing. Must be used within try element.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Catch Plugin",
                "Executes body content when an exception occurs in the preceding try block. Provides access to error details via ${_error} variable.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("try", true, "Try"); // Required parent
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for CatchPlugin.
     */
    public static class CatchPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
                new PluginConfigurationSchema.AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "id";
                            }

                            @Override
                            public PluginConfigurationSchema.AttributeType getType() {
                                return PluginConfigurationSchema.AttributeType.STRING;
                            }

                            @Override
                            public boolean isRequired() {
                                return false;
                            }

                            @Override
                            public String getDefaultValue() {
                                return null;
                            }

                            @Override
                            public String getDescription() {
                                return "Optional identifier for the catch element";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return null;
                            }

                            @Override
                            public Number getMinValue() {
                                return null;
                            }

                            @Override
                            public Number getMaxValue() {
                                return null;
                            }

                            @Override
                            public String getPattern() {
                                return null;
                            }
                        }
            };
        }
        
        @Override
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
            for (PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
                if (attr.getName().equals(name)) {
                    return attr;
                }
            }
            return null;
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public boolean allowsBody() {
            return true;
        }

        @Override
        public String[] getRequiredSubElements() {
            return new String[0];
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[0]; // Can contain any child plugins for error recovery
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            // Additional validation can be added here if needed
        }

        @Override
        public boolean canAppearAtRoot() {
            return false;
        }

        @Override
        public String[] getRequiredParents() {
            return new String[]{"try"};
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new CatchPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new CatchPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        // Additional validation can be added here if needed
    }

    @Override
    protected boolean shouldPropagateBodyExecutionFailures() {
        // A failed recovery handler must not be silently swallowed by its own body executor.
        return true;
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("doExecute: START");
        
        // Check if there was an error in the try block
        Variable errorVar = context.getVar("error");
        if (errorVar != null && !(errorVar instanceof EmptyVariable)) {
            LOGGER.info("Error detected, executing catch block: {}", errorVar);
            
            // Execute child elements directly in the same context (not using BodyProcessor)
            LOGGER.debug("About to execute child elements directly");
            Variable result = executeChildElementsDirectly(context);
            LOGGER.debug("Child elements executed, result={}", result);
            
            // Set the result as a variable in the context
            context.setLocalVar("catchResult", result);
            LOGGER.debug("Set variable 'catchResult' in context");
            
            return result;
        } else {
            LOGGER.debug("No error detected, skipping catch block");
            return EmptyVariable.INSTANCE;
        }
    }

    /**
     * Execute child elements directly in the same context (v2.2.x ULTRA-LEAN)
     * Uses new architecture: configuration.getChildren() + Plugin
     */
    private Variable executeChildElementsDirectly(DynamicScopeContext context) throws InterruptedException {
        // v2.2.x: Use AbstractPlugin.executeBody() which uses PluginBodyExecutor
        // PluginBodyExecutor handles children execution properly
        return super.executeBody(context);
    }
}





