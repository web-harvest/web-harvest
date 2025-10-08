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
import org.webharvest.plugin.ExecutableBodyContentProvider;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Constant plugin that provides static text content.
 * This is a core plugin migrated from the original ConstantProcessor.
 * 
 * <p>
 * The constant plugin allows definition of static text content that can be used
 * internally in the Web-Harvest configuration. It is not explicitly used in
 * configuration XML but is used internally to define static content.
 * </p>
 * 
 * @core This is a core plugin
 * @author Web-Harvest Team
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "constant",
    description = "Constant plugin that provides static text content",
    version = "2.2.0"
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
            return "core";
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
                // Try to get text content directly from XML node
                Variable bodyContent = getBodyTextContent(context);
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
        
        Variable result = new NodeVariable(value);
        LOGGER.info("ConstantPlugin.doExecute: Created result: {}", result);
        
        // Set the result as a variable in the context
        context.setLocalVar("constantResult", result);
        LOGGER.info("ConstantPlugin.doExecute: Set variable 'constantResult' in context");
        
        LOGGER.info("ConstantPlugin.doExecute: Returning result: {}", result);
        return result;
    }
    /**
     * Gets text content from XML node for constant value.
     * 
     * @param context the execution context
     * @return the text content variable
     * @throws InterruptedException if execution is interrupted
     */
    private Variable getBodyTextContent(DynamicScopeContext context) throws InterruptedException {
        if (getElementDef() == null) {
            LOGGER.debug("No element definition available for text content extraction");
            return EmptyVariable.INSTANCE;
        }
        
        try {
            ExecutableBodyContentProvider bodyProvider = new ExecutableBodyContentProvider(getElementDef());
            return bodyProvider.getBodyTextContent(context);
        } catch (Exception e) {
            LOGGER.warn("Error extracting text content: {}", e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
}





