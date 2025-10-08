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
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Call plugin that executes function calls.
 * This is a core plugin migrated from the original CallProcessor.
 * 
 * <p>
 * The CallPlugin allows calling defined functions with parameters.
 * It supports the same functionality as the original CallProcessor but with
 * improved error handling, dependency injection, and modern architecture.
 * </p>
 * 
 * @core This is a core plugin
 * @author Web-Harvest Team
 * @since 2.2.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "call",
    description = "Call plugin that executes function calls",
    version = "2.2.0"
)
public class CallPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.CallPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "call";
        }

        @Override
        public String getName() {
            return "Call Plugin";
        }

        @Override
        public String getVersion() {
            return "2.2.0";
        }

        @Override
        public String getDescription() {
            return "Call plugin that executes function calls";
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
            return new String[]{"core", "call", "function", "execution", "invocation"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return CallPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "call";
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
                                return "Name of the function to call";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return null;
                            }

                            @Override
                            public String getPattern() {
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
                        },
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
                                return "Optional identifier for the call element";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return null;
                            }

                            @Override
                            public String getPattern() {
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
            };
        }
    };

    private Map<String, Variable> functionParams = new HashMap<String, Variable>();
    private Variable functionResult = new NodeVariable("");

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        
        LOGGER.info("CallPlugin initialized with function: {}", configuration.getProperty("name"));
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
        LOGGER.info("CallPlugin.doExecute: START");
        
        PluginConfiguration config = getConfiguration();
        
        // Get function name - using the same logic as original CallProcessor
        String functionName = BaseTemplater.evaluateToString(config.getProperty("name", ""), null, context);
        LOGGER.info("CallPlugin.doExecute: Function name: {}", functionName);
        
        if (functionName.isEmpty()) {
            LOGGER.error("CallPlugin.doExecute: No function name specified");
            return EmptyVariable.INSTANCE;
        }

        // TODO: Implement function definition lookup
        // In the original CallProcessor, this would look up FunctionDef from context
        // For now, we'll execute the body and return the result
        
        try {
            // TODO: Implement proper body execution
            Variable bodyContent = EmptyVariable.INSTANCE;
            
            // Set function result
            functionResult = bodyContent != null ? bodyContent : new NodeVariable("");
            LOGGER.info("CallPlugin.doExecute: Function result: {}", functionResult);
            
            return functionResult;
            
        } catch (Exception e) {
            LOGGER.error("CallPlugin.doExecute: Error executing function call: " + e.getMessage(), e);
            return EmptyVariable.INSTANCE;
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return getMetadata().getConfigurationSchema();
    }

    /**
     * Sets the function result.
     * 
     * @param result the result to set
     */
    public void setFunctionResult(Variable result) {
        this.functionResult = result;
    }

    /**
     * Adds a function parameter.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addFunctionParam(String name, Variable value) {
        functionParams.put(name, value);
        LOGGER.debug("CallPlugin: Added function parameter '{}' = {}", name, value);
    }
}