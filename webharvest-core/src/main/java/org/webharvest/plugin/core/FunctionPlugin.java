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
 * Function definition processor - defines reusable functions with parameters.
 * 
 * <p>
 * The {@code <function>} processor defines named, reusable functions that can be
 * called multiple times from different parts of the configuration using {@code <call>}.
 * Functions support parameters, local variables, and return values, enabling
 * modular and maintainable scraper configurations.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Simple function</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="getCurrentDate">
 *   ${_date}
 * </function>
 * 
 * <def var="timestamp">
 *   <call name="getCurrentDate"/>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Function with parameters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="fetchProduct">
 *   <def var="productId"><call-param name="productId"/></def>
 *   
 *   <http url="https://api.example.com/product/${productId}"/>
 * </function>
 * 
 * <!-- Call with different IDs -->
 * <def var="product1"><call name="fetchProduct"><call-param name="productId">123</call-param></call></def>
 * <def var="product2"><call name="fetchProduct"><call-param name="productId">456</call-param></call></def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Reusable extraction logic</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="extractProductInfo">
 *   <def var="html"><call-param name="html"/></def>
 *   
 *   <def var="name">
 *     <xpath expression="//h1[@class='title']/text()">
 *       <html-to-xml>${html}</html-to-xml>
 *     </xpath>
 *   </def>
 *   
 *   <def var="price">
 *     <xpath expression="//span[@class='price']/text()">
 *       <html-to-xml>${html}</html-to-xml>
 *     </xpath>
 *   </def>
 *   
 *   <template>${name}|${price}</template>
 * </function>
 * 
 * <loop item="url">
 *   <tokenize>${urls}</tokenize>
 *   
 *   <def var="page"><http url="${url}"/></def>
 *   <def var="info">
 *     <call name="extractProductInfo">
 *       <call-param name="html">${page}</call-param>
 *     </call>
 *   </def>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Define before use:</strong> Functions must be defined before calling</li>
 *   <li><strong>Parameters:</strong> Retrieved via {@code <call-param>} inside function body</li>
 *   <li><strong>Return value:</strong> Last expression in function body is returned</li>
 *   <li><strong>Variable scope:</strong> Function has local scope for variables</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <call>} - Invoke defined functions</li>
 *   <li>{@code <call-param>} - Define function parameters</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "function",
    description = "Function plugin that defines user-defined functions",
    version = "2.1.0"
)
public class FunctionPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.FunctionPlugin");

    /**
     * Metadata for FunctionPlugin.
     * 
     * <p>Master plugin - allows call-param and return as children. Defines reusable functions with parameters.</p>
     */
    public static class FunctionPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "function";
        }

        @Override
        public String getName() {
            return "Function";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Defines reusable functions with parameters that can be called from other parts of the configuration. Functions encapsulate logic, accept parameters via call-param, and can return values using return. Essential for code reuse and modular scraper design.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = FunctionPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "function", "definition", "custom", "reusable", "master"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return FunctionPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "function";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new FunctionPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-cube";
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
            return 240;
        }
        
        @Override
        public int getDefaultHeight() {
            return 200;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("name", "Function name (from name attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Function return value", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Defines reusable functions with parameters that can be called from other parts of the configuration. Functions encapsulate logic, accept parameters via call-param, and can return values using return. Essential for code reuse and modular scraper design.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+F", "Add Function", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Function Plugin",
                "Defines reusable functions with parameters that can be called from other parts of the configuration.",
                new String[]{"Tab", "Enter", "Escape"}
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
            return new String[]{"call-param", "return"}; // Master plugin - allows call-param and return
        }
    }
    
    /**
     * Configuration schema for FunctionPlugin.
     */
    public static class FunctionPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the function element";
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
                        return "Name of the function to define";
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
                        return "^[a-zA-Z_][a-zA-Z0-9_]*$";
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
            return new String[]{"call-param", "return"}; // Master plugin - allows call-param and return
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
            
            if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                throw new PluginValidationException("Name must be a valid identifier");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new FunctionPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new FunctionPluginMetadata().getConfigurationSchema();
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
        
        if (!name.matches("^[a-zA-Z_][a-zA-Z0-9_-]*$")) {
            throw new PluginValidationException("Name must be a valid identifier");
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        // Get function name - using the same logic as original FunctionProcessor
        String functionName = config.getProperty("name", "");
        if (functionName.isEmpty()) {
            LOGGER.warn("No function name specified for function plugin");
            return EmptyVariable.INSTANCE;
        }
        
        // Log debug information instead of setting properties
        LOGGER.debug("Defining function: {}", functionName);
        
        try {
            // Store function configuration reference for CallPlugin
            // We mark that the function exists by storing a placeholder variable
            // CallPlugin will look up the function configuration from the configuration tree
            context.setLocalVar(functionName, EmptyVariable.INSTANCE);
            
            LOGGER.debug("Function definition completed: {}", functionName);
            LOGGER.debug("Function '{}' is now available for calling", functionName);
            
            return EmptyVariable.INSTANCE;
            
        } catch (Exception e) {
            LOGGER.error("Error during function definition", e);
            return EmptyVariable.INSTANCE;
        }
    }
}
