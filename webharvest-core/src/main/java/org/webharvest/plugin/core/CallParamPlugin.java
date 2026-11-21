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
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Function call parameter - used within {@code <call>} to pass arguments to functions.
 * 
 * <p>
 * The {@code <call-param>} processor defines a parameter passed to a function when calling it with {@code <call>}.
 * Parameters can be simple values or complex expressions. Must be used as a child element of {@code <call>}.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h4>Example 1: Simple parameters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="greet">
 *   <def var="name"><call-param name="userName"/></def>
 *   <def var="greeting">Hello, ${name}!</def>
 *   <return>${greeting}</return>
 * </function>
 * 
 * <def var="message">
 *   <call name="greet">
 *     <call-param name="userName">John</call-param>
 *   </call>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Complex parameter values</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="fetchProduct">
 *   <def var="id"><call-param name="productId"/></def>
 *   <def var="apiUrl">https://api.example.com/products/${id}</def>
 *   <def var="response">
 *     <http url="${apiUrl}"/>
 *   </def>
 *   <return>${response}</return>
 * </function>
 * 
 * <call name="fetchProduct">
 *   <call-param name="productId">
 *     <xpath expression="//product/@id">
 *       <html-to-xml>${catalog}</html-to-xml>
 *     </xpath>
 *   </call-param>
 * </call>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Multiple parameters with types</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="formatPrice">
 *   <def var="amount"><call-param name="price"/></def>
 *   <def var="curr"><call-param name="currency"/></def>
 *   <def var="decimals"><call-param name="decimalPlaces"/></def>
 *   
 *   <return>${curr} ${amount}</return>
 * </function>
 * 
 * <call name="formatPrice">
 *   <call-param name="price">19.99</call-param>
 *   <call-param name="currency">USD</call-param>
 *   <call-param name="decimalPlaces">2</call-param>
 * </call>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Passing XML/HTML content</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="extractLinks">
 *   <def var="htmlContent"><call-param name="html"/></def>
 *   <def var="links">
 *     <xpath expression="//a/@href">
 *       <html-to-xml>${htmlContent}</html-to-xml>
 *     </xpath>
 *   </def>
 *   <return>${links}</return>
 * </function>
 * 
 * <call name="extractLinks">
 *   <call-param name="html">
 *     <http url="https://example.com"/>
 *   </call-param>
 * </call>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>name</strong> (required): The parameter name used to access value in function</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Returns the evaluated body content as the parameter value.</p>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used within {@code <call>} element</li>
 *   <li><strong>Parameter Access</strong>: Inside function, use {@code <call-param name="paramName"/>} to retrieve value</li>
 *   <li><strong>Type Handling</strong>: Parameters can be strings, XML, lists, or any variable type</li>
 *   <li><strong>Evaluation</strong>: Body content is evaluated before passing to function</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <call>} - Function invocation (parent element)</li>
 *   <li>{@code <function>} - Function definition</li>
 *   <li>{@code <return>} - Return value from function</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see CallPlugin
 * @see FunctionPlugin
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "call-param",
    description = "CallParam plugin that provides function call parameter functionality",
    version = "2.1.0"
)
public class CallParamPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallParamPlugin.class);

    /**
     * Metadata for CallParamPlugin.
     * 
     * <p>Detail plugin - requires call as parent. Defines function call parameters.</p>
     */
    public static class CallParamPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "call-param";
        }

        @Override
        public String getName() {
            return "Call Parameter";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Defines parameters passed to functions when calling them. Must be used within call element. Parameters can be simple values, expressions, or complex data structures. The parameter value is evaluated from the body content and passed to the function.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = CallParamPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "call-param", "parameter", "function", "call", "detail"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return CallParamPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "call-param";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new CallParamPluginConfigurationSchema();
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
                new InputPort("name", "Parameter name (from name attribute)", PortType.STRING, true),
                new InputPort("value", "Parameter value (from body content)", PortType.MIXED, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Parameter definition passed to parent call", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Defines parameters passed to functions when calling them. Must be used within call element. Parameters can be simple values, expressions, or complex data structures.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Call Parameter Plugin",
                "Defines parameters passed to functions when calling them. Must be used within call element.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("call", true, "Call Function"); // Required parent
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for CallParamPlugin.
     */
    public static class CallParamPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the call-param element";
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
                },
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "name";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.STRING;
                    }

                    @Override
                    public boolean isRequired() {
                        return true;
                    }

                    @Override
                    public String getDefaultValue() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Name of the parameter";
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
        public String[] getRequiredParents() {
            // call-param can appear in two contexts:
            // 1. Directly in <call> - defining parameter value
            // 2. In <function> (inside <def>) - retrieving parameter value
            // We allow both "call" and "def" as parents, but validation will check
            // if "def" is within "function" context
            return new String[]{"call", "def"};
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[]{"name"};
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            
            String name = configuration.getProperty("name");
            if (name == null || name.trim().isEmpty()) {
                throw new PluginValidationException("Name attribute is required");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new CallParamPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new CallParamPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String name = configuration.getProperty("name");
        if (name == null || name.trim().isEmpty()) {
            throw new PluginValidationException("Name attribute is required");
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String paramName = config.getProperty("name", "");
        
        if (paramName.isEmpty()) {
            LOGGER.warn("CallParamPlugin: No parameter name specified");
            return EmptyVariable.INSTANCE;
        }
        
        // Check if we're inside a function (retrieving parameter value)
        // Look for parameter in context with special prefix
        // Variable name must start with letter, so we use "callParam" + capitalized name
        String capitalizedName = paramName.substring(0, 1).toUpperCase() + 
            (paramName.length() > 1 ? paramName.substring(1) : "");
        String paramVarName = "callParam" + capitalizedName;
        Variable paramValue = context.getVar(paramVarName);
        
        if (paramValue != null) {
            // We're inside a function, retrieving a parameter value
            int paramLength = paramValue.toString().length();
            LOGGER.info("CallParamPlugin: Retrieved parameter '{}' from function call ({} chars, var: '{}')", 
                new Object[]{paramName, paramLength, paramVarName});
            return paramValue;
        }
        
        LOGGER.info("CallParamPlugin: Parameter '{}' not found in context (looking for '{}'), executing body", 
            new Object[]{paramName, paramVarName});
        
        // We're in a <call> element, executing body to get parameter value
        // Use getBodyTextContent() to handle both text content and child plugins
        paramValue = super.getBodyTextContent(context);
        
        if (paramValue == null || paramValue instanceof EmptyVariable) {
            LOGGER.debug("CallParamPlugin: Parameter '{}' has empty value", paramName);
            return EmptyVariable.INSTANCE;
        }
        
        LOGGER.info("CallParamPlugin: Parameter '{}' = {} chars (from body)", paramName, paramValue.toString().length());
        return paramValue;
    }
}

















