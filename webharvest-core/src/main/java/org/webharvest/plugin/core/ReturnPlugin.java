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
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Function return processor - explicitly sets function return value and exits function.
 * 
 * <p>
 * The {@code <return>} processor is used inside {@code <function>} definitions to explicitly
 * return a value and immediately exit the function. If no {@code <return>} is used, the
 * function returns the result of its last expression.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Early return from function</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="findProduct">
 *   <def var="query"><call-param name="query"/></def>
 *   
 *   <loop item="product" maxloops="100">
 *     <xpath expression="//product">...</xpath>
 *     
 *     <def var="name"><xpath expression=".//name/text()">${product}</xpath></def>
 *     
 *     <if condition="${name == query}">
 *       <return>${product}</return>  <!-- Found! Exit function immediately -->
 *     </if>
 *   </loop>
 *   
 *   <!-- Not found -->
 *   <return>NOT_FOUND</return>
 * </function>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: Conditional return</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="validateEmail">
 *   <def var="email"><call-param name="email"/></def>
 *   
 *   <if condition="${empty(email)}">
 *     <return>false</return>
 *   </if>
 *   
 *   <regexp>
 *     <regexp-pattern>^[\\w.-]+@[\\w.-]+\\.\\w+$</regexp-pattern>
 *     <regexp-source>${email}</regexp-source>
 *   </regexp>
 *   
 *   <if condition="${!empty(group0)}">
 *     <return>true</return>
 *   </if>
 *   
 *   <return>false</return>
 * </function>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Function only:</strong> Used only inside {@code <function>} definitions</li>
 *   <li><strong>Immediate exit:</strong> Stops function execution, returns to caller</li>
 *   <li><strong>Optional:</strong> Functions can return last expression without {@code <return>}</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <function>} - Function definition</li>
 *   <li>{@code <call>} - Function invocation</li>
 *   <li>{@code <exit>} - Exit entire scraper (not just function)</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "return",
    description = "Return plugin that sets the return value for function calls",
    version = "2.2.0",
    category = "Utilities"
)
public class ReturnPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ReturnPlugin.class);

    /**
     * Metadata for ReturnPlugin.
     * 
     * <p>Detail plugin - used inside function definitions to explicitly return a value and exit the function.</p>
     */
    public static class ReturnPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "return";
        }

        @Override
        public String getName() {
            return "Return";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Explicitly sets function return value and immediately exits the function. Used inside function definitions to return computed results. If no return is used, the function returns the result of its last expression.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ReturnPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Utilities";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "return", "function", "control-flow", "detail"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return ReturnPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "return";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new ReturnPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-arrow-left";
        }
        
        @Override
        public String getColor() {
            return "#8B5CF6"; // Purple for functions
        }
        
        @Override
        public String getGroup() {
            return "Functions & Scripting";
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
            return new InputPort[]{
                new InputPort("value", "Return value (from body content)", PortType.MIXED, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[0]; // No output - terminates function execution
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Explicitly sets function return value and immediately exits the function. Used inside function definitions to return computed results. Must be used within function element.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Return Plugin",
                "Explicitly sets function return value and immediately exits the function. Used inside function definitions.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("function", true, "Function"); // Required parent
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for ReturnPlugin.
     */
    public static class ReturnPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the return element";
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
            return new String[0];
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
            // Return plugin doesn't have complex validation
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new ReturnPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new ReturnPluginMetadata().getConfigurationSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Return plugin doesn't need complex initialization
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        // ReturnPlugin should return the child element value directly, not use BodyProcessor
        return doExecute(context);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute child elements to get return value
        Variable returnValue = executeChildElementsDirectly(context);
        
        LOGGER.info("ReturnPlugin.doExecute: returnValue type: {}, value: '{}'", 
            returnValue != null ? returnValue.getClass().getSimpleName() : "null",
            returnValue != null ? returnValue.toString() : "null");
        
        // Set the return value in context for CallPlugin to pick up
        context.setLocalVar("functionResult", returnValue);
        
        // Set flag to indicate that ReturnPlugin was called
        // This allows CallPlugin to distinguish between "no return" and "return with empty value"
        // Variable name must start with letter (VariableName validation)
        context.setLocalVar("returnPluginCalled", new org.webharvest.runtime.variables.NodeVariable("true"));
        LOGGER.info("ReturnPlugin: Set generic flag 'returnPluginCalled'");
        
        // Also try to find and set function-specific flag if it exists
        // This is a best-effort approach - if we can't find it, the generic flag will work
        try {
            for (org.webharvest.utils.KeyValuePair<org.webharvest.runtime.variables.Variable> pair : context) {
                String key = pair.getKey();
                if (key != null && key.startsWith("__returnCalled_")) {
                    context.setLocalVar(key, new org.webharvest.runtime.variables.NodeVariable("true"));
                    LOGGER.info("ReturnPlugin: Set function-specific flag: {}", key);
                    break;
                }
            }
        } catch (Exception e) {
            // If iteration fails, generic flag is still set
            LOGGER.warn("ReturnPlugin: Failed to find function-specific flag: {}", e.getMessage());
        }
        
        LOGGER.info("ReturnPlugin: Set functionResult and flags");
        return EmptyVariable.INSTANCE;
    }

    /**
     * Execute child elements directly in the same context (v2.2.x ULTRA-LEAN)
     * Uses new architecture: PluginBodyExecutor
     */
    private Variable executeChildElementsDirectly(DynamicScopeContext context) throws InterruptedException {
        // v2.2.x: Use AbstractPlugin.executeBody() which uses PluginBodyExecutor
        return super.executeBody(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Return plugin doesn't have complex configuration validation
    }
}
