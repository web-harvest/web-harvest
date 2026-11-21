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
import org.webharvest.runtime.variables.Variable;

/**
 * Else branch processor - fallback execution when {@code <if>} condition is false.
 * 
 * <p>
 * The {@code <else>} processor executes when the preceding {@code <if>} condition
 * evaluates to false. Used with {@code <if>} for conditional branching logic.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Simple if-else logic</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <if condition="${stock > 0}">
 *   <def var="status" value="In Stock"/>
 * </if>
 * <else>
 *   <def var="status" value="Out of Stock"/>
 * </else>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: Handle empty results</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="title">
 *   <xpath expression="//h1/text()">
 *     <html-to-xml>${response}</html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <if condition="${!empty(title)}">
 *   <file path="title.txt" action="write">${title}</file>
 * </if>
 * <else>
 *   <file path="errors.log" action="append">
 *     Title not found on page ${url}
 *   </file>
 * </else>
 * </config>
 * }</pre>
 * 
 * <h3>Example 3: Error handling with HTTP</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="response">
 *   <try>
 *     <http url="https://api.example.com/data"/>
 *   </try>
 *   <catch>
 *     <empty/>
 *   </catch>
 * </def>
 * 
 * <if condition="${!empty(response)}">
 *   <!-- Process successful response -->
 *   <def var="data">
 *     <json-to-xml>${response}</json-to-xml>
 *   </def>
 * </if>
 * <else>
 *   <!-- Fallback to cached data -->
 *   <def var="data">
 *     <file path="cache/last-response.xml" action="read"/>
 *   </def>
 * </else>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>No parameters</strong>: Else block executes when condition is false</li>
 *   <li><strong>Content</strong>: Contains processors to execute in else case</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Returns the result of executing the else block body, or EmptyVariable if no body.</p>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used immediately after {@code <if>} or {@code <case>} block</li>
 *   <li><strong>Conditional Logic</strong>: Executes only when preceding condition is false</li>
 *   <li><strong>Fallback Processing</strong>: Provides alternative execution path</li>
 *   <li><strong>Variable Scope</strong>: Variables defined in else block are available after execution</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <if>} - Conditional execution with condition</li>
 *   <li>{@code <case>} - Case-based conditional execution</li>
 *   <li>{@code <try>} / {@code <catch>} - Exception handling</li>
 *   <li>{@code <def>} - Variable definition</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see IfPlugin
 * @see CasePlugin
 */
@CorePlugin(
    elementName = "else",
    description = "Else plugin that provides the else branch for conditional execution",
    version = "2.1.0",
    category = "Control"
)
public class ElsePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ElsePlugin");

    /**
     * Metadata for ElsePlugin.
     * 
     * <p>Detail plugin - requires if as parent. Executes when if condition is false.</p>
     */
    public static class ElsePluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "else";
        }

        @Override
        public String getName() {
            return "Else";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes body content when the preceding if condition is false. Provides fallback execution for conditional logic. Must be used within if element.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ElsePlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "else", "conditional", "fallback", "detail"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return ElsePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "else";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new ElsePluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-arrow-right";
        }
        
        @Override
        public String getColor() {
            return "#818CF8"; // Light indigo for detail plugins
        }
        
        @Override
        public String getGroup() {
            return "Control Flow";
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
            return new InputPort[0]; // No input ports - executes when if condition is false
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Result from body execution (when if condition is false)", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes body content when the preceding if condition is false. Provides fallback execution for conditional logic. Must be used within if element.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Else Plugin",
                "Executes body content when the preceding if condition is false. Provides fallback execution for conditional logic.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("if", true, "If"); // Required parent
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for ElsePlugin.
     */
    public static class ElsePluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the else element";
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
            return new String[0];
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
    }

    @Override
    public PluginMetadata getMetadata() {
        return new ElsePluginMetadata();
    }
    
    public PluginConfigurationSchema getConfigurationSchema() {
        return new ElsePluginMetadata().getConfigurationSchema();
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
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // The parent If / Case executor decides whether this branch is selected.
        // Else itself is only responsible for executing its body.
        return executeBody(context);
    }
}





