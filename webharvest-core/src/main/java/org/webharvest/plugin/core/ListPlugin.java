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
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * List plugin that creates a list variable from child elements.
 */
@CorePlugin(
    elementName = "list",
    description = "List plugin that creates a list variable",
    version = "2.1.0"
)
public class ListPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ListPlugin");

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "list";
            }

            @Override
            public String getName() {
                return "List Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "List plugin that provides list functionality";
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
                return new String[]{"core", "list", "collection", "variable"};
            }

            @Override
            public String[] getDependencies() {
                return new String[0];
            }

            @Override
            public String getClassName() {
                return ListPlugin.class.getName();
            }

            @Override
            public boolean isCorePlugin() {
                return true;
            }

            @Override
            public String getElementName() {
                return "list";
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
                                public String getDescription() {
                                    return "Optional identifier for the list element";
                                }

                                @Override
                                public boolean isRequired() {
                                    return false;
                                }

                                @Override
                                public PluginConfigurationSchema.AttributeType getType() {
                                    return PluginConfigurationSchema.AttributeType.STRING;
                                }

                                @Override
                                public String getDefaultValue() {
                                    return null;
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
                        return new String[0];
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws PluginValidationException {
                        if (configuration == null) {
                            throw new PluginValidationException("Configuration cannot be null");
                        }
                    }
                };
            }
        };
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return getMetadata().getConfigurationSchema();
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
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        System.out.println("List plugin executing - creating list variable");
        
        try {
            // List plugin executes its body content and returns it as a list
            Variable result = super.executeBody(context);
            
            // Set the result as a variable in the context
            context.setLocalVar("listResult", result);
            System.out.println("List plugin - set variable 'listResult' in context");
            
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Error in list plugin", e);
            return EmptyVariable.INSTANCE;
        }
    }
}