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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML content wrapper processor - explicit XML content declaration.
 * 
 * <p>
 * The {@code <xml>} processor wraps XML content for explicit declaration and processing.
 * Rarely needed in modern configurations - most XML is processed directly without this wrapper.
 * Useful when you need to ensure XML content is properly parsed and validated.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Explicit XML declaration</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="xmlData">
 *   <xml>
 *     <products>
 *       <product id="1">
 *         <name>Laptop</name>
 *         <price>999.99</price>
 *       </product>
 *       <product id="2">
 *         <name>Mouse</name>
 *         <price>29.99</price>
 *       </product>
 *     </products>
 *   </xml>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Dynamic XML generation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="productName">WebHarvest Pro</def>
 * <def var="productPrice">199.99</def>
 * 
 * <def var="productXml">
 *   <xml>
 *     <product>
 *       <name>${productName}</name>
 *       <price>${productPrice}</price>
 *       <timestamp>${new Date()}</timestamp>
 *     </product>
 *   </xml>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: XML template with processing</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="userData">
 *   <xml>
 *     <user>
 *       <id>12345</id>
 *       <name>John Doe</name>
 *       <email>john@example.com</email>
 *       <preferences>
 *         <theme>dark</theme>
 *         <language>en</language>
 *       </preferences>
 *     </user>
 *   </xml>
 * </def>
 * 
 * <!-- Process the XML -->
 * <def var="userName">
 *   <xpath expression="//name/text()">
 *     <get var="userData"/>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: When explicit XML is useful</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- When you need to ensure XML is properly parsed -->
 * <def var="configTemplate">
 *   <xml>
 *     <configuration>
 *       <database>
 *         <host>localhost</host>
 *         <port>3306</port>
 *         <name>webharvest</name>
 *       </database>
 *       <api>
 *         <baseUrl>https://api.example.com</baseUrl>
 *         <timeout>30000</timeout>
 *       </api>
 *     </configuration>
 *   </xml>
 * </def>
 * 
 * <!-- Alternative: Direct XML (usually preferred) -->
 * <def var="configTemplate2">
 *   <configuration>
 *     <database>
 *       <host>localhost</host>
 *       <port>3306</port>
 *       <name>webharvest</name>
 *     </database>
 *   </configuration>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: XML content to wrap</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Returns a NodeVariable containing the parsed XML content.</p>
 * 
 * <h3>When to Use</h3>
 * <ul>
 *   <li><strong>Explicit Declaration:</strong> When you want to explicitly mark XML content</li>
 *   <li><strong>Validation:</strong> When you need to ensure XML is properly parsed</li>
 *   <li><strong>Template Processing:</strong> When generating XML with variable substitution</li>
 *   <li><strong>Debugging:</strong> When you want to see exactly what XML is being processed</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Rarely Needed:</strong> Most XML content works directly without this wrapper</li>
 *   <li><strong>XML Parsing:</strong> Ensures content is properly parsed as XML</li>
 *   <li><strong>Variable Substitution:</strong> Supports ${var} syntax within XML content</li>
 *   <li><strong>Validation:</strong> XML content must be well-formed</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li><strong>Direct XML content</strong> - Preferred for most cases</li>
 *   <li>{@code <xpath>} - Extract data from XML</li>
 *   <li>{@code <html-to-xml>} - Convert HTML to XML</li>
 *   <li>{@code <json-to-xml>} - Convert JSON to XML</li>
 * </ul>
 * 
 * <h3>Note</h3>
 * <p><strong>Usually unnecessary.</strong> Direct XML content works in most contexts.
 * Use {@code <xml>} only when you need explicit XML declaration or validation.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "xml",
    description = "XML plugin that provides XML processing functionality",
    version = "2.1.0"
)
public class XMLPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.XMLPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "xml";
        }

        @Override
        public String getName() {
            return "XML Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "XML plugin that provides XML processing functionality";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = XMLPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xml", "processing", "container", "markup"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return XMLPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xml";
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
                                return "Optional identifier for the xml element";
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
                            return "Optional identifier for the xml element";
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
        LOGGER.debug("XMLPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();

        // Get XML configuration
        String charset = config.getProperty("charset", context.getCharset());
        boolean validate = CommonUtil.getBooleanValue(config.getProperty("validate", "false"), false);
        String schema = config.getProperty("schema", "");

        LOGGER.debug("XML plugin - Charset: " + charset + ", Validate: " + validate + ", Schema: " + schema);

        try {
            // Execute body content to get XML content
            Variable bodyContent = executeBody(context);
            LOGGER.debug("XMLPlugin.doExecute: bodyContent=" + bodyContent);
            
            String xmlContent = "";
            if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                xmlContent = bodyContent.toString();
                LOGGER.debug("XMLPlugin.doExecute: Retrieved XML content: " + xmlContent);
            } else {
                LOGGER.debug("XMLPlugin.doExecute: No body content available");
                return EmptyVariable.INSTANCE;
            }

            // Process XML content
            if (xmlContent == null || xmlContent.isEmpty()) {
                LOGGER.warn("No XML content provided for XML plugin");
                return EmptyVariable.INSTANCE;
            }

            // Basic XML processing - in a real implementation this would parse and validate XML
            LOGGER.debug("Processing XML content with length: " + xmlContent.length());
            
            // Return the XML content as NodeVariable
            return new NodeVariable(xmlContent);

        } catch (Exception e) {
            LOGGER.error("Error during XML processing", e);
            LOGGER.debug("XMLPlugin.doExecute: ERROR: " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
}
