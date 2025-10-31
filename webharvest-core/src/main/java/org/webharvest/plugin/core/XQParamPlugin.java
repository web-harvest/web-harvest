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
 * The {@code <xq-param>} processor defines parameters for XQuery expressions, enabling
 * dynamic data binding and type-safe parameter passing. Must be used as a child element of {@code <xquery>}.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic parameter binding</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       declare variable $minPrice as xs:double external;
 *       //product[price > $minPrice]
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="minPrice" type="xs:double">100.0</xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Multiple parameters with different types</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       declare variable $category as xs:string external;
 *       declare variable $maxPrice as xs:double external;
 *       declare variable $includeOutOfStock as xs:boolean external;
 *       
 *       for $p in //product
 *       where $p/category = $category
 *         and $p/price <= $maxPrice
 *         and ($includeOutOfStock or $p/stock > 0)
 *       return $p
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="category" type="xs:string">electronics</xq-param>
 *   <xq-param name="maxPrice" type="xs:double">500.0</xq-param>
 *   <xq-param name="includeOutOfStock" type="xs:boolean">false</xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Dynamic parameters from variables</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="searchTerm">laptop</def>
 * <def var="priceRange">1500</def>
 * 
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       declare variable $search as xs:string external;
 *       declare variable $maxPrice as xs:double external;
 *       
 *       <results>
 *         {
 *           for $p in //product
 *           where contains(lower-case($p/name), lower-case($search))
 *             and $p/price <= $maxPrice
 *           order by $p/price ascending
 *           return $p
 *         }
 *       </results>
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="search" type="xs:string">${searchTerm}</xq-param>
 *   <xq-param name="maxPrice" type="xs:double">${priceRange}</xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: XML node parameters (Bug #61 fix)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="userProfile">
 *   <html-to-xml>
 *     <http url="https://api.example.com/user/123"/>
 *   </html-to-xml>
 * </def>
 * 
 * <xquery>
 *   <xq-expression>
 *     <![CDATA[
 *       declare variable $profile as node() external;
 *       
 *       <userSummary>
 *         <name>{$profile//name/text()}</name>
 *         <email>{$profile//email/text()}</email>
 *         <preferences>
 *           {
 *             for $pref in $profile//preference
 *             return <pref>{$pref/@name}</pref>
 *           }
 *         </preferences>
 *       </userSummary>
 *     ]]>
 *   </xq-expression>
 *   <xq-param name="profile" type="node()">
 *     <get-var name="userProfile"/>
 *   </xq-param>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>name</strong> (required): The parameter name (matches XQuery variable)</li>
 *   <li><strong>type</strong> (optional): XQuery data type (xs:string, xs:integer, xs:double, xs:boolean, node())</li>
 *   <li><strong>Content (body)</strong>: The parameter value</li>
 * </ul>
 * 
 * <h3>Supported Types</h3>
 * <ul>
 *   <li><strong>xs:string</strong> - Text values</li>
 *   <li><strong>xs:integer</strong> - Whole numbers</li>
 *   <li><strong>xs:double</strong> - Decimal numbers</li>
 *   <li><strong>xs:boolean</strong> - true/false values</li>
 *   <li><strong>node()</strong> - XML/HTML content (requires {@code <get-var>})</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <xquery>} element</li>
 *   <li><strong>Variable Declaration</strong>: XQuery must declare external variables</li>
 *   <li><strong>Type Safety</strong>: Use type attribute for proper data type conversion</li>
 *   <li><strong>Multiple Parameters</strong>: Can define multiple parameters for single query</li>
 *   <li><strong>Dynamic Values</strong>: Supports ${var} syntax for dynamic parameter values</li>
 * </ul>
 * 
 * <h3>Bug Fix (r825 - Oct 2025)</h3>
 * <p>
 * <strong>Critical Fix:</strong> XQParamPlugin.doExecute() now properly executes child elements
 * like {@code <get-var>} instead of returning EmptyVariable. This enables node() type parameters
 * to receive actual XML data from context variables.
 * </p>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xquery>} - Main XQuery processor (parent element)</li>
 *   <li>{@code <xq-expression>} - XQuery expression definition</li>
 *   <li>{@code <get-var>} - Retrieve variable value for node() parameters</li>
 *   <li>{@code <def>} - Variable definition for dynamic values</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 * @see XQueryPlugin
 * @see XQExpressionPlugin
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
            CorePlugin annotation = XQParamPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
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
        // Execute child elements (like <get-var>) to get parameter value
        // This is used by XQueryPlugin when binding node() parameters
        return super.executeBody(context);
    }
}

















