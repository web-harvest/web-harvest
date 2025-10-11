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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
// import org.webharvest.utils.CommonUtil;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * If plugin that provides conditional processing.
 * This is a core plugin migrated from the original IfProcessor.
 * 
 * <p>
 * The if plugin provides conditional processing based on a condition expression.
 * It currently returns an empty variable as the original processor does.
 * </p>
 * 
 * @core This is a core plugin
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "if",
    description = "If plugin that provides conditional processing",
    version = "2.1.0"
)
public class IfPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.IfPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "if";
        }

        @Override
        public String getName() {
            return "If Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "If plugin that provides conditional processing";
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
            return new String[]{"core", "if", "conditional", "control"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getElementName() {
            return "if";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String getClassName() {
            return IfPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }


        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[]{
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "condition";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.STRING;
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
                                return "Condition expression to evaluate";
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
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "id";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.STRING;
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
                                return "Optional identifier for the if element";
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
                public AttributeDefinition getAttribute(String name) {
                    for (AttributeDefinition attr : getAttributeDefinitions()) {
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
                    return new String[]{"condition"};
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
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[]{
                    new AttributeDefinition() {
                        @Override
                        public String getName() {
                            return "condition";
                        }

                        @Override
                        public AttributeType getType() {
                            return AttributeType.STRING;
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
                            return "Condition expression to evaluate";
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
                    new AttributeDefinition() {
                        @Override
                        public String getName() {
                            return "id";
                        }

                        @Override
                        public AttributeType getType() {
                            return AttributeType.STRING;
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
                            return "Optional identifier for the if element";
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
            public AttributeDefinition getAttribute(String name) {
                for (AttributeDefinition attr : getAttributeDefinitions()) {
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
                return new String[]{"condition"};
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
        System.out.println("IfPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();

        // Get condition from configuration - XML schema defines 'condition' attribute
        String condition = config.getProperty("condition", "");
        
        System.out.println("If plugin - Condition: '" + condition + "'");
        System.out.println("If plugin - Config properties: " + config.getProperties());
        System.out.println("If plugin - Config toString: " + config);

        try {
            // Evaluate condition (simplified - in real implementation this would be more complex)
            boolean conditionResult = evaluateCondition(condition, context);
            System.out.println("IfPlugin.doExecute: conditionResult=" + conditionResult);
            
            if (conditionResult) {
                // Execute body content when condition is true
                Variable bodyResult = executeBody(context);
                System.out.println("IfPlugin.doExecute: Condition true, executing body content");
                return bodyResult;
            } else {
                // Return EmptyVariable for false conditions
                System.out.println("IfPlugin.doExecute: Condition false, returning EmptyVariable");
                return EmptyVariable.INSTANCE;
            }

        } catch (Exception e) {
            LOGGER.error("Error during if condition evaluation", e);
            return EmptyVariable.INSTANCE;
        }
    }
    
                private boolean evaluateCondition(String condition, DynamicScopeContext context) {
                    if (condition == null || condition.isEmpty()) {
                        return false;
                    }
                    
                    try {
                        System.out.println("IfPlugin.evaluateCondition: Original condition=" + condition);
                        
                        // Handle template expressions like ${condition == 'true'}
                        if (condition.contains("${") && condition.contains("}")) {
                            // Extract the expression inside ${...}
                            String expression = condition.substring(condition.indexOf("${") + 2, condition.lastIndexOf("}"));
                            System.out.println("IfPlugin.evaluateCondition: Extracted expression=" + expression);
                            
                            // Handle comparison expressions like "condition == 'true'"
                            if (expression.contains("==")) {
                                String[] parts = expression.split("==");
                                if (parts.length == 2) {
                                    String leftVar = parts[0].trim();
                                    String rightValue = parts[1].trim().replaceAll("['\"]", "");
                                    
                                    System.out.println("IfPlugin.evaluateCondition: leftVar='" + leftVar + "', rightValue='" + rightValue + "'");
                                    
                                    // Get the variable value from context
                                    Variable leftVarValue = context.getVar(leftVar);
                                    String leftValue = leftVarValue != null ? leftVarValue.toString() : "";
                                    
                                    System.out.println("IfPlugin.evaluateCondition: leftValue='" + leftValue + "'");
                                    
                                    boolean result = leftValue.equals(rightValue);
                                    System.out.println("IfPlugin.evaluateCondition: comparison result=" + result);
                                    return result;
                                }
                            }
                            
                            // Handle other comparison operators
                            if (expression.contains("!=")) {
                                String[] parts = expression.split("!=");
                                if (parts.length == 2) {
                                    String leftVar = parts[0].trim();
                                    String rightValue = parts[1].trim().replaceAll("['\"]", "");
                                    
                                    Variable leftVarValue = context.getVar(leftVar);
                                    String leftValue = leftVarValue != null ? leftVarValue.toString() : "";
                                    
                                    boolean result = !leftValue.equals(rightValue);
                                    System.out.println("IfPlugin.evaluateCondition: != comparison result=" + result);
                                    return result;
                                }
                            }
                            
                            // If it's a simple variable reference, get its value
                            Variable varValue = context.getVar(expression.trim());
                            if (varValue != null) {
                                String value = varValue.toString();
                                boolean result = !value.equals("false") && !value.equals("0") && !value.isEmpty();
                                System.out.println("IfPlugin.evaluateCondition: variable value='" + value + "', result=" + result);
                                return result;
                            }
                        }
                        
                        // For simple boolean values
                        boolean result = !condition.equals("false") && !condition.equals("0") && !condition.isEmpty();
                        System.out.println("IfPlugin.evaluateCondition: simple boolean result=" + result);
                        return result;
                    } catch (Exception e) {
                        LOGGER.error("Error evaluating condition: " + condition, e);
                        return false;
                    }
                }

}
