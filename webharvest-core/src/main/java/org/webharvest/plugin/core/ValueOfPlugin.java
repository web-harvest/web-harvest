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
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ValueOf plugin that evaluates expressions and returns their values.
 * This is a core plugin migrated from the original ValueOfPlugin.
 * 
 * <p>
 * The ValueOf plugin evaluates an expression using BaseTemplater and returns the result.
 * It requires an 'expr' attribute containing the expression to evaluate.
 * </p>
 * 
 * @core This is a core plugin
 * @author Web-Harvest Team
 * @since 2.1.0
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

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "value-of";
            }

            @Override
            public String getName() {
                return "ValueOf Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "ValueOf plugin that evaluates expressions and returns their values";
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
                return new String[]{"core", "value", "expression", "evaluation"};
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
                return ValueOfPlugin.this.getConfigurationSchema();
            }
        };
    }
}