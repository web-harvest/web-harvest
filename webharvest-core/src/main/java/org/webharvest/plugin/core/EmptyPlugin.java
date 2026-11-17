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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Empty check processor - tests if a value is empty or executes body without output.
 * 
 * <p>
 * The {@code <empty>} processor serves two purposes: (1) Check if a value is empty/null,
 * and (2) Execute processors for their side effects without producing output.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Check if variable is empty</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <if condition="${empty(apiKey)}">
 *   <file path="error.log" action="append">API key not configured!</file>
 *   <exit/>
 * </if>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Execute for side effects only</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <empty>
 *   <!-- Log activity without storing result -->
 *   <file path="debug.log" action="append">
 *     Processing started at ${_date}
 *   </file>
 * </empty>
 * </config>
 * }</pre>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <if>} - Conditional execution</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "empty",
    description = "Empty plugin that executes its body and returns an empty variable",
    version = "2.1.0",
    category = "Utilities"
)
public class EmptyPlugin extends AbstractPlugin {

    /**
     * Metadata for EmptyPlugin.
     * 
     * <p>Standalone plugin - tests if a value is empty or executes body without output.</p>
     */
    public static class EmptyPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "empty";
        }

        @Override
        public String getName() {
            return "Empty";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Serves two purposes: (1) Check if a value is empty/null, and (2) Execute processors for their side effects without producing output. Useful for grouping operations and conditional checks.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = EmptyPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Utilities";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "empty", "grouping", "utility", "side-effects"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return EmptyPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "empty";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new EmptyPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-square";
        }
        
        @Override
        public String getColor() {
            return "#6B7280"; // Gray for utilities
        }
        
        @Override
        public String getGroup() {
            return "Utility";
        }
        
        @Override
        public int getDefaultWidth() {
            return 180;
        }
        
        @Override
        public int getDefaultHeight() {
            return 140;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("content", "Content to check (from body content)", PortType.MIXED, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Empty variable (always)", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.STRING;
        }
        
        @Override
        public String getTooltip() {
            return "Serves two purposes: (1) Check if a value is empty/null, and (2) Execute processors for their side effects without producing output. Can contain any child plugins.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Rarely used directly
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Empty Plugin",
                "Tests if a value is empty/null or executes processors for their side effects without producing output.",
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
     * Configuration schema for EmptyPlugin.
     */
    public static class EmptyPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the empty element";
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
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[0]; // Can contain any child plugins
        }

        @Override
        public String[] getRequiredSubElements() {
            return new String[0];
        }

        @Override
        public boolean allowsBody() {
            return true;
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new EmptyPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new EmptyPluginMetadata().getConfigurationSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Empty plugin doesn't need initialization
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute nested plugins for their side effects, but intentionally
        // discard the aggregated body result: <empty> always returns empty.
        executeBody(context);
        return EmptyVariable.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Empty plugin doesn't have complex configuration validation
        // The schema validation is handled by the framework
    }
}
