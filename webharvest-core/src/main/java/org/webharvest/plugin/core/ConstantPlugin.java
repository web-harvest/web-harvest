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
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;

/**
 * Constant value processor - defines static, immutable text values.
 * 
 * <p>
 * The {@code <constant>} processor creates a static, immutable text value. It's primarily used
 * internally by the framework for static configuration values and literal text content.
 * In user configurations, {@code <def>} or direct text content is typically preferred.
 * </p>
 * 
 * <h3>When to Use</h3>
 * <ul>
 *   <li>When you need an explicit constant value (rare in user configs)</li>
 *   <li>Framework uses it internally for literal text nodes</li>
 *   <li>For configs that need to explicitly mark static values</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic constant value</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="apiKey">
 *   <constant value="sk_live_abc123xyz"/>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Static configuration value</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <constant value="https://api.example.com/v1"/>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Preferred alternative - use &lt;def&gt;</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- This is clearer and more common -->
 * <def var="baseUrl" value="https://api.example.com/v1"/>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>value</strong> (required): The static text value to return</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Returns a NodeVariable containing the constant text value.</p>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <def>} - Define variables (preferred for most cases)</li>
 *   <li>{@code <text>} - Text content wrapper</li>
 *   <li>{@code <var>} - Variable reference</li>
 * </ul>
 * 
 * <h3>Note</h3>
 * <p><strong>Rarely used directly in user configurations.</strong> The framework uses it internally
 * for static text nodes. For your own configs, prefer {@code <def>} with a value attribute or
 * direct text content for better readability.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "constant",
    description = "Constant plugin that provides static text content",
    version = "2.2.0",
    category = "Variables"
)
public class ConstantPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ConstantPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "constant";
        }

        @Override
        public String getName() {
            return "Constant Plugin";
        }

        @Override
        public String getVersion() {
            return "2.2.0";
        }

        @Override
        public String getDescription() {
            return "Constant plugin that provides static text content";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ConstantPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Variables";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "constant", "text", "static"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return ConstantPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "constant";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                    return new PluginConfigurationSchema.AttributeDefinition[]{
                        new PluginConfigurationSchema.AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "value";
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
                                return "The constant text value";
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
                    return "2.2.0";
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
                    return new String[]{"value"};
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    if (configuration == null) {
                        throw new PluginValidationException("Configuration cannot be null");
                    }
                    // Additional validation can be added here if needed
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[]{
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return "value";
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
                            return "The constant text value";
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
                return "2.2.0";
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
                return new String[]{"value"};
            }

            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                if (configuration == null) {
                    throw new PluginValidationException("Configuration cannot be null");
                }
                // Additional validation can be added here if needed
            }
        };
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
        LOGGER.info("ConstantPlugin.doExecute: START");
        
        // Get constant value from configuration or raw text
        String value = super.getConfiguration().getProperty("value");
        LOGGER.info("ConstantPlugin.doExecute: Value from config: '{}'", value);
        
        // If no value attribute, try raw text content
        if (value == null || value.isEmpty()) {
            LOGGER.info("ConstantPlugin.doExecute: No value attribute, trying body text content");
            try {
                // Use AbstractPlugin.getBodyTextContent() - new architecture (v2.2.x)
                Variable bodyContent = super.getBodyTextContent(context);
                LOGGER.info("ConstantPlugin.doExecute: Body content result: {}", bodyContent);
                if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                    value = bodyContent.toString();
                    LOGGER.info("ConstantPlugin.doExecute: Using body content: '{}'", value);
                }
            } catch (Exception e) {
                LOGGER.warn("ConstantPlugin.doExecute: Error executing body content: {}", e.getMessage());
            }
        }
        
        // Default to empty string if still no value
        if (value == null) {
            LOGGER.warn("ConstantPlugin.doExecute: No value found, using empty string");
            value = "";
        }
        
        // v2.2.x FIX: Apply template interpolation to constant value
        Variable result;
        if (BaseTemplater.scriptEngineFactory != null && value.contains("${")) {
            LOGGER.debug("ConstantPlugin.doExecute: Applying template interpolation to value");
            result = BaseTemplater.evaluateToVariable(value, context.getScriptingLanguage(), context);
            LOGGER.debug("ConstantPlugin.doExecute: Interpolated result: {}", result);
        } else {
            result = new NodeVariable(value);
        }
        LOGGER.info("ConstantPlugin.doExecute: Created result: {}", result);
        
        // Set the result as a variable in the context
        context.setLocalVar("constantResult", result);
        LOGGER.info("ConstantPlugin.doExecute: Set variable 'constantResult' in context");
        
        LOGGER.info("ConstantPlugin.doExecute: Returning result: {}", result);
        return result;
    }
    // getBodyTextContent() removed - using AbstractPlugin.getBodyTextContent() (v2.2.x new architecture)
}





