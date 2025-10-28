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
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import org.webharvest.utils.CommonUtil;

/**
 * Multi-branch conditional processor - switch/case statement with multiple conditions.
 * 
 * <p>
 * The {@code <case>} processor evaluates multiple {@code <if>} conditions in sequence and
 * executes the first matching branch. It's equivalent to if-else-if chains but with cleaner
 * syntax for multiple conditions. Provides structured conditional logic.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Route by HTTP status</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <case>
 *   <if condition="${httpStatus == '200'}">
 *     <def var="result" value="success"/>
 *   </if>
 *   <if condition="${httpStatus == '404'}">
 *     <def var="result" value="not_found"/>
 *   </if>
 *   <if condition="${httpStatus >= '500'}">
 *     <def var="result" value="server_error"/>
 *   </if>
 *   <else>
 *     <def var="result" value="unknown"/>
 *   </else>
 * </case>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: User role-based processing</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <case>
 *   <if condition="${userRole == 'admin'}">
 *     <def var="permissions" value="full_access"/>
 *     <def var="dashboard" value="admin_dashboard"/>
 *   </if>
 *   <if condition="${userRole == 'moderator'}">
 *     <def var="permissions" value="moderate_content"/>
 *     <def var="dashboard" value="moderator_dashboard"/>
 *   </if>
 *   <if condition="${userRole == 'user'}">
 *     <def var="permissions" value="basic_access"/>
 *     <def var="dashboard" value="user_dashboard"/>
 *   </if>
 *   <else>
 *     <def var="permissions" value="guest_access"/>
 *     <def var="dashboard" value="login_page"/>
 *   </else>
 * </case>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: File type processing</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="fileExtension">
 *   <xpath expression="substring-after(//file/@name, '.')">
 *     <get var="fileInfo"/>
 *   </xpath>
 * </def>
 * 
 * <case>
 *   <if condition="${fileExtension == 'xml'}">
 *     <def var="processor" value="xml_parser"/>
 *     <def var="outputFormat" value="structured_data"/>
 *   </if>
 *   <if condition="${fileExtension == 'json'}">
 *     <def var="processor" value="json_parser"/>
 *     <def var="outputFormat" value="structured_data"/>
 *   </if>
 *   <if condition="${fileExtension == 'csv'}">
 *     <def var="processor" value="csv_parser"/>
 *     <def var="outputFormat" value="tabular_data"/>
 *   </if>
 *   <else>
 *     <def var="processor" value="text_parser"/>
 *     <def var="outputFormat" value="plain_text"/>
 *   </else>
 * </case>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Error handling with case</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="errorCode">
 *   <try>
 *     <http url="https://api.example.com/data"/>
 *   </try>
 *   <catch>
 *     <def var="errorType">${exception.class.simpleName}</def>
 *   </catch>
 * </def>
 * 
 * <case>
 *   <if condition="${errorType == 'ConnectException'}">
 *     <def var="action" value="retry_later"/>
 *     <def var="message" value="Network connection failed"/>
 *   </if>
 *   <if condition="${errorType == 'SocketTimeoutException'}">
 *     <def var="action" value="increase_timeout"/>
 *     <def var="message" value="Request timed out"/>
 *   </if>
 *   <if condition="${errorType == 'IOException'}">
 *     <def var="action" value="check_permissions"/>
 *     <def var="message" value="IO error occurred"/>
 *   </if>
 *   <else>
 *     <def var="action" value="log_and_continue"/>
 *     <def var="message" value="Unknown error: ${errorType}"/>
 *   </else>
 * </case>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: Contains {@code <if>} and optional {@code <else>} elements</li>
 * </ul>
 * 
 * <h3>Behavior</h3>
 * <ul>
 *   <li><strong>Sequential Evaluation:</strong> Conditions evaluated in order</li>
 *   <li><strong>First Match Wins:</strong> Only first matching {@code <if>} executes</li>
 *   <li><strong>Else Fallback:</strong> Optional {@code <else>} executes if no conditions match</li>
 *   <li><strong>Single Execution:</strong> Only one branch executes per case statement</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>First match wins:</strong> Only first matching {@code <if>} executes</li>
 *   <li><strong>Else is optional:</strong> Executed if no conditions match</li>
 *   <li><strong>Condition Order:</strong> Order of {@code <if>} elements matters</li>
 *   <li><strong>Variable Scope:</strong> Variables defined in executed branch are available after</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <if>} - Single conditional execution</li>
 *   <li>{@code <else>} - Fallback execution</li>
 *   <li>{@code <def>} - Variable definition</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see IfPlugin
 * @see ElsePlugin
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "case",
    description = "Case plugin that provides conditional execution with multiple if-else branches",
    version = "2.1.0",
    category = "Control"
)
public class CasePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.CasePlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "case";
        }

        @Override
        public String getName() {
            return "Case Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Case plugin that provides conditional execution with multiple if-else branches";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            return "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "case", "conditional", "if", "else"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return CasePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "case";
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
                                return "Optional identifier for the case element";
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
                    return new String[]{"if"};
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[]{"if", "else"};
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
                            return "Optional identifier for the case element";
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
                return new String[]{"if"};
            }

            @Override
            public String[] getAllowedSubElements() {
                return new String[]{"if", "else"};
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
        LOGGER.info("CasePlugin.doExecute: START");
        
        // Execute child elements (if/else branches) and collect results
        Variable result = super.executeBody(context);
        
        LOGGER.info("CasePlugin.doExecute: Result: {}", result);
        return result;
    }
}





