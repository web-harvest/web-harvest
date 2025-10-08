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
import org.webharvest.definition.IElementDef;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Try plugin that provides exception handling with try-catch functionality.
 * This is a core plugin migrated from the original TryProcessor.
 * 
 * <p>
 * The try plugin allows execution of code blocks with exception handling.
 * It executes the try body and catches any exceptions, then executes
 * the catch block with error information available in the context.
 * </p>
 * 
 * @core This is a core plugin
 * @author Web-Harvest Team
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "try",
    description = "Try plugin that provides exception handling with try-catch functionality",
    version = "2.1.0"
)
public class TryPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.TryPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "try";
        }

        @Override
        public String getName() {
            return "Try Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Try plugin that provides exception handling with try-catch functionality";
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
            return new String[]{"core", "try", "catch", "exception", "error-handling"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return TryPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "try";
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
                                return "Optional identifier for the try element";
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
                    return true;
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[]{"body", "catch"};
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[]{"body", "catch"};
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[]{"id"};
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[0];
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
                            return "Optional identifier for the try element";
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
                return true;
            }

            @Override
            public String[] getRequiredSubElements() {
                return new String[]{"body", "catch"};
            }

            @Override
            public String[] getAllowedSubElements() {
                return new String[]{"body", "catch"};
            }

            @Override
            public String[] getOptionalAttributes() {
                return new String[]{"id"};
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[0];
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
        System.out.println("TryPlugin.doExecute: START");
        LOGGER.debug("TryPlugin.doExecute: START");
        
        try {
            // Execute child elements directly in the same context (not using BodyProcessor)
            System.out.println("TryPlugin.doExecute: About to execute child elements directly");
            Variable result = executeChildElementsDirectly(context);
            System.out.println("TryPlugin.doExecute: Child elements executed successfully, result=" + result);
            
            // Set the result as a variable in the context
            context.setLocalVar("tryResult", result);
            System.out.println("TryPlugin.doExecute: Set variable 'tryResult' in context");
            
            return result;
            
        } catch (Exception e) {
            System.out.println("TryPlugin.doExecute: Exception caught: " + e.getMessage());
            LOGGER.warn("Exception caught with try plugin: {}", e.getMessage());
            
            // Set error information in context
            context.setLocalVar("error", new NodeVariable(e.getMessage()));
            context.setLocalVar("errorType", new NodeVariable(e.getClass().getSimpleName()));
            System.out.println("TryPlugin.doExecute: Set error variables in context");
            
            // Return empty result - the catch block should handle the error
            return EmptyVariable.INSTANCE;
        }
    }

    /**
     * Execute child elements directly in the same context (not using BodyProcessor)
     * to avoid context isolation issues.
     */
    private Variable executeChildElementsDirectly(DynamicScopeContext context) throws InterruptedException {
        final List<Object> resultList = new ArrayList<Object>();
        final org.webharvest.definition.IElementDef[] defs = getElementDef().getOperationDefs();
        
        for (org.webharvest.definition.IElementDef def : defs) {
            final org.webharvest.runtime.processors.Processor processor = org.webharvest.runtime.processors.ProcessorFactory.INSTANCE.createProcessor(context, def);
            processor.setParentProcessor(null); // Set parent to null to avoid circular dependency
            final Variable variable = processor.run(context);
            if (!variable.isEmpty()) {
                resultList.addAll(variable.toList());
            }
        }
        
        return resultList.isEmpty() ? EmptyVariable.INSTANCE : new ListVariable(resultList);
    }
}





