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

/**
 * XQuery parameter definition - used within {@code <xquery>} to pass parameters.
 * 
 * <p>
 * The {@code <xq-param>} processor defines parameters for XQuery expressions.
 * Must be used as a child element of {@code <xquery>}.
 * </p>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * <xquery>
 *   <xq-expression>
 *     declare variable $minPrice external;
 *     //product[price > $minPrice]
 *   </xq-expression>
 *   <xq-param name="minPrice" type="xs:integer">100</xq-param>
 * </xquery>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>name</strong> (required): The parameter name (matches XQuery variable)</li>
 *   <li><strong>type</strong> (optional): XQuery data type (xs:string, xs:integer, etc.)</li>
 *   <li><strong>value</strong> (required): The parameter value (as element content)</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <xquery>} element</li>
 *   <li><strong>Variable Declaration</strong>: XQuery must declare external variables</li>
 *   <li><strong>Type Safety</strong>: Use type attribute for proper data type conversion</li>
 *   <li><strong>Multiple Parameters</strong>: Can define multiple parameters for single query</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xquery>} - Main XQuery processor</li>
 *   <li>{@code <xq-expression>} - XQuery expression definition</li>
 *   <li>{@code <var>} - Variable definition for dynamic values</li>
 *   <li>{@code <get>} - Retrieve variable value for node() parameters</li>
 * </ul>
 * 
 * <h3>Bug Fix (r825 - Oct 2025)</h3>
 * <p>
 * <strong>Critical Fix:</strong> XQParamPlugin.doExecute() now properly executes child elements
 * like {@code <get>} instead of returning EmptyVariable. This enables node() type parameters
 * to receive actual XML data from context variables.
 * </p>
 * 
 * <p>See {@link XQueryPlugin} for complete XQuery examples.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 * @see XQueryPlugin
 */
@CorePlugin(
    elementName = "xq-param",
    description = "XQParam plugin that provides XQuery parameter functionality",
    version = "2.1.0"
)
public class XQParamPlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "xq-param";
        }

        @Override
        public String getName() {
            return "XQParam Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "XQParam plugin that provides XQuery parameter functionality";
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
            return new String[]{"core", "xq-param", "xquery", "parameter", "external"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return XQParamPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xq-param";
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
                                return "Optional identifier for the xq-param element";
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
                                return "Name of the XQuery parameter";
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
                                return "type";
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
                                return "node()";
                            }

                            @Override
                            public String getDescription() {
                                return "Type of the XQuery parameter";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return new String[]{"node()", "node()*", "integer", "integer*", "long", "long*", 
                                                  "float", "float*", "double", "double*", "boolean", "boolean*", 
                                                  "string", "string*"};
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
                    return new String[]{"id", "type"};
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
                    
                    String type = configuration.getProperty("type");
                    if (type != null && !type.trim().isEmpty()) {
                        String[] allowedTypes = {"node()", "node()*", "integer", "integer*", "long", "long*", 
                                               "float", "float*", "double", "double*", "boolean", "boolean*", 
                                               "string", "string*"};
                        boolean validType = false;
                        for (String allowedType : allowedTypes) {
                            if (allowedType.equals(type)) {
                                validType = true;
                                break;
                            }
                        }
                        if (!validType) {
                            throw new PluginValidationException("Invalid type: " + type);
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
                            return "Optional identifier for the xq-param element";
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
                            return "Name of the XQuery parameter";
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
                            return "type";
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
                            return "node()";
                        }

                        @Override
                        public String getDescription() {
                            return "Type of the XQuery parameter";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return new String[]{"node()", "node()*", "integer", "integer*", "long", "long*", 
                                              "float", "float*", "double", "double*", "boolean", "boolean*", 
                                              "string", "string*"};
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
                return new String[]{"id", "type"};
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
                
                String type = configuration.getProperty("type");
                if (type != null && !type.trim().isEmpty()) {
                    String[] allowedTypes = {"node()", "node()*", "integer", "integer*", "long", "long*", 
                                           "float", "float*", "double", "double*", "boolean", "boolean*", 
                                           "string", "string*"};
                    boolean validType = false;
                    for (String allowedType : allowedTypes) {
                        if (allowedType.equals(type)) {
                            validType = true;
                            break;
                        }
                    }
                    if (!validType) {
                        throw new PluginValidationException("Invalid type: " + type);
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
        
        String name = configuration.getProperty("name");
        if (name == null || name.trim().isEmpty()) {
            throw new PluginValidationException("Name attribute is required");
        }
        
        String type = configuration.getProperty("type");
        if (type != null && !type.trim().isEmpty()) {
            String[] allowedTypes = {"node()", "node()*", "integer", "integer*", "long", "long*", 
                                   "float", "float*", "double", "double*", "boolean", "boolean*", 
                                   "string", "string*"};
            boolean validType = false;
            for (String allowedType : allowedTypes) {
                if (allowedType.equals(type)) {
                    validType = true;
                    break;
                }
            }
            if (!validType) {
                throw new PluginValidationException("Invalid type: " + type);
            }
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute child elements (like <get>) to get parameter value
        // This is used by XQueryPlugin when binding node() parameters
        return super.executeBody(context);
    }
}

















