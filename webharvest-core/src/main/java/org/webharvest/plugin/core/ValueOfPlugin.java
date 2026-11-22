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
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expression evaluation processor - evaluates ${} expressions explicitly.
 * 
 * <p>
 * The {@code <value-of>} processor explicitly evaluates template expressions and returns their values.
 * Mostly used for XPath-style value extraction and when you need explicit expression evaluation.
 * In most cases, direct ${} syntax is simpler and preferred.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Explicit expression evaluation</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="productName">WebHarvest Pro</def>
 * <def var="price">99.99</def>
 * 
 * <!-- Explicit evaluation -->
 * <def var="description">
 *   <value-of expr="${productName} - $${price}"/>
 * </def>
 * 
 * <!-- Simpler alternative -->
 * <def var="description2">${productName} - $${price}</def>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: XPath-style value extraction</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="xmlData">
 *   <html-to-xml>
 *     <http url="https://example.com/product"/>
 *   </html-to-xml>
 * </def>
 * 
 * <def var="productTitle">
 *   <value-of expr="//h1[@class='product-title']/text()"/>
 * </def>
 * 
 * <def var="productPrice">
 *   <value-of expr="//span[@class='price']/text()"/>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>Example 3: Complex expression evaluation</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="baseUrl">https://api.example.com</def>
 * <def var="endpoint">/v1/products</def>
 * <def var="apiKey">abc123</def>
 * 
 * <def var="fullUrl">
 *   <value-of expr="${baseUrl}${endpoint}?key=${apiKey}"/>
 * </def>
 * 
 * <!-- Alternative syntax -->
 * <def var="fullUrl2">${baseUrl}${endpoint}?key=${apiKey}</def>
 * </config>
 * }</pre>
 * 
 * <h3>Example 4: When explicit evaluation is useful</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- When you need to ensure expression is evaluated in specific context -->
 * <def var="dynamicContent">
 *   <value-of expr="${userInput}"/>
 * </def>
 * 
 * <!-- For debugging - explicit evaluation shows what's being processed -->
 * <log message="Processing: ${dynamicContent}"/>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>expr</strong> (required): The expression to evaluate</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Returns the evaluated result of the expression as a Variable.</p>
 * 
 * <h3>When to Use</h3>
 * <ul>
 *   <li><strong>XPath expressions:</strong> When extracting values from XML/HTML</li>
 *   <li><strong>Explicit evaluation:</strong> When you need to ensure expression is evaluated</li>
 *   <li><strong>Debugging:</strong> To see exactly what expression is being processed</li>
 *   <li><strong>Complex expressions:</strong> For multi-part expressions that need explicit handling</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Expression Required:</strong> The expr attribute is mandatory</li>
 *   <li><strong>Template Syntax:</strong> Uses ${} template syntax</li>
 *   <li><strong>Variable Access:</strong> Can access all variables in current scope</li>
 *   <li><strong>XPath Support:</strong> Supports XPath expressions for XML/HTML data</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li><strong>Direct ${} syntax</strong> - Preferred for simple expressions</li>
 *   <li>{@code <xpath>} - XPath extraction from XML/HTML</li>
 *   <li>{@code <def>} - Variable definition</li>
 * </ul>
 * 
 * <h3>Note</h3>
 * <p><strong>Rarely needed in modern configurations.</strong> Direct ${} syntax is simpler and preferred
 * for most use cases. Use {@code <value-of>} only when you need explicit expression evaluation
 * or XPath-style extraction.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "value-of",
    description = "ValueOf plugin that evaluates expressions and returns their values",
    version = "2.1.0"
)
public class ValueOfPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ValueOfPlugin");
    private static final String ATTR_EXPRESSION = "expr";

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No initialization needed
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String expression = configuration.getProperty(ATTR_EXPRESSION);
        if (expression == null || expression.trim().isEmpty()) {
            throw new PluginValidationException("Expression attribute is required for value-of plugin");
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        final String expression = getConfiguration().getProperty(ATTR_EXPRESSION);
        
        if (expression == null) {
            throw new RuntimeException("Expression attribute is required for value-of plugin");
        }
        
        try {
            // Log debug information instead of setting properties
            LOGGER.debug("ValueOfPlugin: expression = {}", expression);
            return BaseTemplater.evaluateToVariable(expression, null, context);
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating expression: " + e.getMessage(), e);
        }
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[]{
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return ATTR_EXPRESSION;
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
                            return "Expression to evaluate";
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
                return "2.1.0";
            }

            @Override
            public boolean allowsBody() {
                return false;
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
                return new String[0];
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[]{ATTR_EXPRESSION};
            }

            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                if (configuration == null) {
                    throw new PluginValidationException("Configuration cannot be null");
                }
                
                String expression = configuration.getProperty(ATTR_EXPRESSION);
                if (expression == null || expression.trim().isEmpty()) {
                    throw new PluginValidationException("Expression attribute is required for value-of plugin");
                }
            }
        };
    }

    /**
     * Metadata for ValueOfPlugin.
     * 
     * <p>Standalone plugin - explicitly evaluates ${} expressions and returns their values.</p>
     */
    public static class ValueOfPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "value-of";
        }

        @Override
        public String getName() {
            return "Value Of";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Explicitly evaluates template expressions and returns their values. Mostly used for XPath-style value extraction and when you need explicit expression evaluation. In most cases, direct ${} syntax is simpler and preferred.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ValueOfPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "value", "expression", "evaluation", "template", "xpath"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return ValueOfPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "value-of";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new ValueOfPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-code";
        }
        
        @Override
        public String getColor() {
            return "#F59E0B"; // Amber for text processing
        }
        
        @Override
        public String getGroup() {
            return "Text Processing";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 100;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("expr", "Expression to evaluate (from expr attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Evaluated expression result", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.STRING;
        }
        
        @Override
        public String getTooltip() {
            return "Explicitly evaluates template expressions and returns their values. Mostly used for XPath-style value extraction and when you need explicit expression evaluation. Supports ${} template syntax and XPath expressions.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Rarely used directly
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Value Of Plugin",
                "Explicitly evaluates template expressions and returns their values. Mostly used for XPath-style value extraction.",
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
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for ValueOfPlugin.
     */
    public static class ValueOfPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return ATTR_EXPRESSION;
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
                        return "Expression to evaluate";
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
            return false;
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
            return new String[0];
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[]{ATTR_EXPRESSION};
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            
            String expression = configuration.getProperty(ATTR_EXPRESSION);
            if (expression == null || expression.trim().isEmpty()) {
                throw new PluginValidationException("Expression attribute is required for value-of plugin");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new ValueOfPluginMetadata();
    }
}