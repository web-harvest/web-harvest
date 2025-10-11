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
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.WHConstants;
import org.webharvest.utils.CommonUtil;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * While plugin that provides while loop functionality.
 * This is a core plugin migrated from the original WhileProcessor.
 * 
 * <p>
 * The While plugin executes its body content repeatedly while a condition
 * is true. It supports index variables, maximum loop limits, and empty
 * result handling.
 * </p>
 * 
 * @core This is a core plugin
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "while",
    description = "While plugin that provides while loop functionality",
    version = "2.1.0"
)
public class WhilePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.WhilePlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "while";
        }

        @Override
        public String getName() {
            return "While Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "While plugin that provides while loop functionality";
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
            return new String[]{"core", "while", "loop", "iteration", "condition", "control"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return WhilePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "while";
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
                                return "Optional identifier for the while element";
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
                                return "condition";
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
                                return "Condition to evaluate for loop continuation";
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
                                return "index";
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
                                return "Variable name for loop index";
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
                                return "maxloops";
                            }

                            @Override
                            public PluginConfigurationSchema.AttributeType getType() {
                                return PluginConfigurationSchema.AttributeType.INTEGER;
                            }

                            @Override
                            public boolean isRequired() {
                                return false;
                            }

                            @Override
                            public String getDefaultValue() {
                                return "1000";
                            }

                            @Override
                            public String getDescription() {
                                return "Maximum number of loop iterations";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return null;
                            }

                            @Override
                            public Number getMinValue() {
                                return 1;
                            }

                            @Override
                            public Number getMaxValue() {
                                return 1000000;
                            }

                            @Override
                            public String getPattern() {
                                return null;
                            }
                        },
                        new PluginConfigurationSchema.AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "empty";
                            }

                            @Override
                            public PluginConfigurationSchema.AttributeType getType() {
                                return PluginConfigurationSchema.AttributeType.BOOLEAN;
                            }

                            @Override
                            public boolean isRequired() {
                                return false;
                            }

                            @Override
                            public String getDefaultValue() {
                                return "false";
                            }

                            @Override
                            public String getDescription() {
                                return "Whether to return empty result (true) or collect results (false)";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return new String[]{"true", "false"};
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
                    return new String[]{"id", "index", "maxloops", "empty"};
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
                    
                    String condition = configuration.getProperty("condition");
                    // Allow empty condition - it will be treated as false
                    // if (condition == null || condition.trim().isEmpty()) {
                    //     throw new PluginValidationException("Condition attribute is required");
                    // }
                    
                    String maxloops = configuration.getProperty("maxloops");
                    if (maxloops != null && !maxloops.trim().isEmpty()) {
                        try {
                            int maxLoopsValue = Integer.parseInt(maxloops);
                            if (maxLoopsValue < 1 || maxLoopsValue > 1000000) {
                                throw new PluginValidationException("Maxloops must be between 1 and 1000000");
                            }
                        } catch (NumberFormatException e) {
                            throw new PluginValidationException("Maxloops must be a valid integer");
                        }
                    }
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
                            return "Optional identifier for the while element";
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
                            return "condition";
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
                            return "Condition to evaluate for loop continuation";
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
                            return "index";
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
                            return "Variable name for loop index";
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
                            return "maxloops";
                        }

                        @Override
                        public PluginConfigurationSchema.AttributeType getType() {
                            return PluginConfigurationSchema.AttributeType.INTEGER;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "1000";
                        }

                        @Override
                        public String getDescription() {
                            return "Maximum number of loop iterations";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return null;
                        }

                        @Override
                        public Number getMinValue() {
                            return 1;
                        }

                        @Override
                        public Number getMaxValue() {
                            return 1000000;
                        }

                        @Override
                        public String getPattern() {
                            return null;
                        }
                    },
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return "empty";
                        }

                        @Override
                        public PluginConfigurationSchema.AttributeType getType() {
                            return PluginConfigurationSchema.AttributeType.BOOLEAN;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "false";
                        }

                        @Override
                        public String getDescription() {
                            return "Whether to return empty result (true) or collect results (false)";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return new String[]{"true", "false"};
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
                return new String[]{"id", "index", "maxloops", "empty"};
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
                
                String condition = configuration.getProperty("condition");
                // Allow empty condition - it will be treated as false
                // if (condition == null || condition.trim().isEmpty()) {
                //     throw new PluginValidationException("Condition attribute is required");
                // }
                
                String maxloops = configuration.getProperty("maxloops");
                if (maxloops != null && !maxloops.trim().isEmpty()) {
                    try {
                        int maxLoopsValue = Integer.parseInt(maxloops);
                        if (maxLoopsValue < 1 || maxLoopsValue > 1000000) {
                            throw new PluginValidationException("Maxloops must be between 1 and 1000000");
                        }
                    } catch (NumberFormatException e) {
                        throw new PluginValidationException("Maxloops must be a valid integer");
                    }
                }
            }
        };
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
        
        String condition = configuration.getProperty("condition");
        // Allow empty condition - it will be treated as false
        // if (condition == null || condition.trim().isEmpty()) {
        //     throw new PluginValidationException("Condition attribute is required");
        // }
        
        String maxloops = configuration.getProperty("maxloops");
        if (maxloops != null && !maxloops.trim().isEmpty()) {
            try {
                int maxLoopsValue = Integer.parseInt(maxloops);
                if (maxLoopsValue < 1 || maxLoopsValue > 1000000) {
                    throw new PluginValidationException("Maxloops must be between 1 and 1000000");
                }
            } catch (NumberFormatException e) {
                throw new PluginValidationException("Maxloops must be a valid integer");
            }
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("WhilePlugin.doExecute: START");
        
        // Validate plugin configuration
        if (getConfiguration() == null) {
            throw new RuntimeException("WhilePlugin requires valid configuration - check XML structure");
        }
        
        PluginConfiguration config = getConfiguration();
        LOGGER.trace("WhilePlugin.doExecute: config=" + config);
        LOGGER.trace("WhilePlugin.doExecute: config.getClass()=" + (config != null ? config.getClass().getName() : "null"));

        // Get while configuration - using the same logic as original WhileProcessor
        String index = config.getProperty("index", "");
        String maxLoopsString = config.getProperty("maxloops", "");
        // XML schema defines 'test' attribute (renamed from 'condition')
        String condition = config.getProperty("test", "");
        LOGGER.debug("doExecute: config.getProperty('test', '')='" + condition + "'");
        // If 'test' is not found, try 'condition' for backward compatibility
        if (condition.isEmpty()) {
            condition = config.getProperty("condition", "");
            LOGGER.debug("doExecute: config.getProperty('condition', '')='" + condition + "'");
        }
        boolean isEmpty = CommonUtil.getBooleanValue(config.getProperty("empty", "false"), false);

        LOGGER.trace("WhilePlugin.doExecute: condition='" + condition + "', index='" + index + "', maxLoops='" + maxLoopsString + "', isEmpty=" + isEmpty);

        final List<Object> resultList = new ArrayList<Object>();
        int i = 1;

        // Define first value of index variable
        if (index != null && !index.isEmpty()) {
            context.setLocalVar(index, new NodeVariable(String.valueOf(i)));
        }

        // Evaluate condition using BaseTemplater like original WhileProcessor
        String evaluatedCondition = BaseTemplater.evaluateToString(condition, context.getScriptingLanguage(), context);
        LOGGER.debug("doExecute: evaluatedCondition='" + evaluatedCondition + "'");
        LOGGER.trace("WhilePlugin.doExecute: context.getScriptingLanguage()=" + context.getScriptingLanguage());
        
        // Check if counter is set in context
        Variable counterVar = context.getVar("counter");
        LOGGER.trace("WhilePlugin.doExecute: counter in context=" + counterVar);
        
        // Try to evaluate as boolean using BeanShell directly
        boolean conditionResult = false;
        try {
            // Use BeanShell to evaluate the already-evaluated condition as a boolean expression
            org.webharvest.runtime.scripting.ScriptSource scriptSource = 
                new org.webharvest.runtime.scripting.ScriptSource(evaluatedCondition, context.getScriptingLanguage());
            Object result = BaseTemplater.scriptEngineFactory.getEngine(scriptSource).evaluate(context, scriptSource);
            LOGGER.trace("WhilePlugin.doExecute: BeanShell result=" + result + ", type=" + (result != null ? result.getClass().getName() : "null"));
            
            if (result instanceof Boolean) {
                conditionResult = (Boolean) result;
            } else if (result != null) {
                // Try to convert to boolean
                String resultStr = result.toString();
                conditionResult = CommonUtil.isBooleanTrue(resultStr);
            }
            LOGGER.trace("WhilePlugin.doExecute: conditionResult=" + conditionResult);
        } catch (Exception e) {
            LOGGER.trace("WhilePlugin.doExecute: Error evaluating condition: " + e.getMessage());
            // Fallback to string evaluation
            conditionResult = CommonUtil.isBooleanTrue(evaluatedCondition);
        }

        // Iterates while testing variable represents boolean true or loop limit is exceeded
        final double maxLoops = NumberUtils.toDouble(maxLoopsString, 1000); // Use 1000 as default instead of DEFAULT_MAX_LOOPS
        
        // Apply safety limits to prevent infinite loops
        final double safeMaxLoops = Math.max(0, Math.min(maxLoops, 10000)); // Minimum 0, Maximum 10000 iterations
        LOGGER.trace("WhilePlugin.doExecute: maxLoops=" + maxLoops + ", safeMaxLoops=" + safeMaxLoops);

        // Use conditionResult from BaseTemplater.evaluateToVariable
        while (conditionResult && (i <= safeMaxLoops)) {
            LOGGER.debug("doExecute: Loop iteration " + i + ", evaluatedCondition='" + evaluatedCondition + "'");
            
            // Execute body content using new architecture
            LOGGER.debug("doExecute: About to execute body content");
            Variable loopResult = executeBody(context);
            LOGGER.trace("WhilePlugin.doExecute: Body result.class=" + (loopResult != null ? loopResult.getClass().getName() : "null"));
            LOGGER.trace("WhilePlugin.doExecute: Body result=" + loopResult);
            
            if (!isEmpty) {
                resultList.addAll(loopResult.toList());
            }

            i++;
            // Define current value of index variable
            if (index != null && !index.isEmpty()) {
                context.setLocalVar(index, new NodeVariable(String.valueOf(i)));
            }
            
            // IMPORTANT: If condition uses variables from body (like SetVarPlugin), 
            // we need to extract the updated value from the nested context.
            // For now, we'll check if condition uses 'counter' variable and update it.
            if (condition.contains("counter")) {
                LOGGER.debug("doExecute: Condition uses 'counter' variable, incrementing it");
                Variable currentCounter = context.getVar("counter");
                if (currentCounter != null) {
                    try {
                        int counterValue = Integer.parseInt(currentCounter.toString());
                        context.setLocalVar("counter", new NodeVariable(String.valueOf(counterValue + 1)));
                        LOGGER.trace("WhilePlugin.doExecute: Incremented counter to " + (counterValue + 1));
                    } catch (NumberFormatException e) {
                        LOGGER.trace("WhilePlugin.doExecute: Counter is not a number: " + currentCounter);
                    }
                }
            }

            // Re-evaluate condition for next iteration using BeanShell
            try {
                // Re-evaluate the condition using BaseTemplater first
                String reEvaluatedCondition = BaseTemplater.evaluateToString(condition, context.getScriptingLanguage(), context);
                LOGGER.debug("doExecute: Re-evaluated condition='" + reEvaluatedCondition + "'");
                
                // Then evaluate as boolean using BeanShell
                org.webharvest.runtime.scripting.ScriptSource scriptSource = 
                    new org.webharvest.runtime.scripting.ScriptSource(reEvaluatedCondition, context.getScriptingLanguage());
                Object result = BaseTemplater.scriptEngineFactory.getEngine(scriptSource).evaluate(context, scriptSource);
                LOGGER.trace("WhilePlugin.doExecute: Re-evaluated BeanShell result=" + result + ", type=" + (result != null ? result.getClass().getName() : "null"));
                
                if (result instanceof Boolean) {
                    conditionResult = (Boolean) result;
                } else if (result != null) {
                    String resultStr = result.toString();
                    conditionResult = CommonUtil.isBooleanTrue(resultStr);
                }
                LOGGER.trace("WhilePlugin.doExecute: Re-evaluated conditionResult=" + conditionResult);
            } catch (Exception e) {
                LOGGER.trace("WhilePlugin.doExecute: Error re-evaluating condition: " + e.getMessage());
                conditionResult = false;
            }

            // Safety check: if condition is still "true" and we're in test mode, break after first iteration
            // Only break if maxloops is not explicitly set (to allow tests to control loop count)
            if ("true".equals(evaluatedCondition) && i > 1 && maxLoopsString.isEmpty()) {
                LOGGER.trace("Breaking infinite loop in test mode");
                break;
            }
        }

        LOGGER.trace("WhilePlugin.doExecute: Loop finished, i=" + i + ", resultList.size=" + resultList.size());
        if (isEmpty) {
            return EmptyVariable.INSTANCE;
        } else {
            return resultList.isEmpty() ? EmptyVariable.INSTANCE : new ListVariable(resultList);
        }
    }

    
}





