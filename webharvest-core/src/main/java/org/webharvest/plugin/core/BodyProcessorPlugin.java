/*
 Copyright (c) 2006-2025 the original author or authors.

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

package org.webharvest.plugin.core;

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
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Body execution processor - internal utility for executing child processors.
 * 
 * <p>
 * The {@code <body-processor>} is an internal processor used by the framework
 * to execute child processors and collect their results. It provides a standardized
 * way to execute processor body content and return the results as a variable list.
 * </p>
 * 
 * <h3>Internal Usage</h3>
 * <p>This processor is automatically used by the framework when processors need to execute
 * their body content. It's rarely used directly in user XML configurations.</p>
 * 
 * <h3>Framework Integration</h3>
 * <ul>
 *   <li><strong>Child Execution:</strong> Executes all child processors in sequence</li>
 *   <li><strong>Result Collection:</strong> Collects results from child processors</li>
 *   <li><strong>Variable Management:</strong> Manages variable scope during execution</li>
 *   <li><strong>Error Handling:</strong> Provides consistent error handling for body execution</li>
 * </ul>
 * 
 * <h3>When Framework Uses It</h3>
 * <ul>
 *   <li><strong>Container Processors:</strong> When processors contain child elements</li>
 *   <li><strong>Loop Processors:</strong> For executing loop body content</li>
 *   <li><strong>Conditional Processors:</strong> For executing conditional branches</li>
 *   <li><strong>Function Processors:</strong> For executing function body</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Internal Use:</strong> Primarily used by framework, not user configurations</li>
 *   <li><strong>Body Execution:</strong> Executes child processors and returns results</li>
 *   <li><strong>Variable Scope:</strong> Maintains proper variable scope during execution</li>
 *   <li><strong>Result Format:</strong> Returns results as appropriate variable type</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li><strong>All container processors</strong> - Use this internally for body execution</li>
 *   <li>{@code <loop>} - Uses body-processor for loop iterations</li>
 *   <li>{@code <if>} - Uses body-processor for conditional execution</li>
 *   <li>{@code <function>} - Uses body-processor for function body</li>
 * </ul>
 * 
 * <h3>Note</h3>
 * <p><strong>Framework internal processor.</strong> Users typically don't need to use this
 * directly. It's automatically used by the framework when processors need to execute
 * their body content.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(
    elementName = "body-processor",
    description = "Plugin which executes only body and returns variables list",
    version = "1.0.0",
    systemComponent = true
)
public class BodyProcessorPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.BodyProcessorPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute body content using new architecture
        LOGGER.debug("BodyProcessorPlugin executing body content");
        return super.executeBody(context);
    }

    /**
     * Metadata for BodyProcessorPlugin.
     * 
     * <p>Internal framework plugin - executes body content and returns results. Used by framework for child processor execution.</p>
     */
    public static class BodyProcessorPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "body-processor";
        }

        @Override
        public String getName() {
            return "Body Processor";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Internal framework processor that executes body content and returns results as a variable list. Used automatically by the framework when processors need to execute their child elements. Provides standardized body execution, result collection, and variable scope management. Rarely used directly in user configurations.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = BodyProcessorPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "body", "processor", "execution", "internal", "framework"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return BodyProcessorPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "body-processor";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new BodyProcessorPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-cog";
        }
        
        @Override
        public String getColor() {
            return "#6B7280"; // Gray for internal/framework plugins
        }
        
        @Override
        public String getGroup() {
            return "Framework";
        }
        
        @Override
        public int getDefaultWidth() {
            return 180;
        }
        
        @Override
        public int getDefaultHeight() {
            return 100;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[0]; // No explicit inputs
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Results from body execution", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Internal framework processor that executes body content and returns results. Used automatically by the framework for child processor execution.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Internal plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Body Processor Plugin",
                "Internal framework processor that executes body content and returns results.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return null; // Standalone plugin
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // Can contain any child plugins
        }
    }
    
    /**
     * Configuration schema for BodyProcessorPlugin.
     */
    public static class BodyProcessorPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[0];
        }
        
        @Override
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
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
            return new String[0]; // Can contain any child plugins
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[0];
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            // No validation needed - no attributes
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new BodyProcessorPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new BodyProcessorPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        // No specific validation needed for body processor
    }
}
