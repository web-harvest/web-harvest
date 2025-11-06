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
// REMOVED in v2.2.x - BodyContentProvider no longer exists
// import org.webharvest.plugin.BodyContentProvider;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.ioc.ScrapingScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root configuration processor - top-level container for WebHarvest scrapers.
 * 
 * <p>
 * The {@code <config>} processor is the root element of every WebHarvest configuration.
 * It defines global settings like charset and scripting language, and contains all
 * other processors. Every WebHarvest scraper must start with this element.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic configuration</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
 *   <def var="baseUrl">https://example.com</def>
 *   <def var="response">
 *     <http url="${baseUrl}/api/data"/>
 *   </def>
 *   <def var="processedData">
 *     <json-to-xml>${response}</json-to-xml>
 *   </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: With custom scripting language</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core" 
 *         charset="UTF-8" 
 *         scriptlang="groovy">
 *   <def var="apiKey">sk_live_abc123</def>
 *   <def var="timestamp">${new Date().getTime()}</def>
 *   
 *   <http url="https://api.example.com/data">
 *     <http-header name="Authorization">Bearer ${apiKey}</http-header>
 *     <http-header name="X-Timestamp">${timestamp}</http-header>
 *   </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Complex scraping workflow</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
 *   <!-- Global variables -->
 *   <def var="baseUrl">https://shop.example.com</def>
 *   <def var="category">electronics</def>
 *   
 *   <!-- Main scraping logic -->
 *   <def var="categoryPage">
 *     <html-to-xml>
 *       <http url="${baseUrl}/category/${category}"/>
 *     </html-to-xml>
 *   </def>
 *   
 *   <def var="productLinks">
 *     <xpath expression="//a[@class='product-link']/@href">
 *       <get var="categoryPage"/>
 *     </xpath>
 *   </def>
 *   
 *   <!-- Process each product -->
 *   <loop item="productUrl" list="${productLinks}">
 *     <def var="productData">
 *       <html-to-xml>
 *         <http url="${baseUrl}${productUrl}"/>
 *       </html-to-xml>
 *     </def>
 *     
 *     <def var="productName">
 *       <xpath expression="//h1[@class='product-title']/text()">
 *         <get var="productData"/>
 *       </xpath>
 *     </def>
 *     
 *     <def var="productPrice">
 *       <xpath expression="//span[@class='price']/text()">
 *         <get var="productData"/>
 *       </xpath>
 *     </def>
 *     
 *     <!-- Save product data -->
 *     <file path="products.xml" action="append">
 *       <product>
 *         <name>${productName}</name>
 *         <price>${productPrice}</price>
 *         <url>${baseUrl}${productUrl}</url>
 *       </product>
 *     </file>
 *   </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Error handling configuration</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core" charset="UTF-8">
 *   <def var="apiEndpoint">https://api.example.com/data</def>
 *   
 *   <try>
 *     <def var="response">
 *       <http url="${apiEndpoint}" timeout="30000"/>
 *     </def>
 *     
 *     <def var="data">
 *       <json-to-xml>${response}</json-to-xml>
 *     </def>
 *     
 *     <file path="success.log" action="append">
 *       Successfully processed API response at ${new Date()}
 *     </file>
 *   </try>
 *   <catch>
 *     <file path="error.log" action="append">
 *       Error processing API: ${exception.message} at ${new Date()}
 *     </file>
 *     
 *     <!-- Fallback to cached data -->
 *     <def var="data">
 *       <file path="cache/last-response.xml" action="read"/>
 *     </def>
 *   </catch>
 * </config>
 * }</pre>
 * 
 * <h3>Attributes</h3>
 * <ul>
 *   <li><strong>xmlns</strong> (required): XML namespace - "http://org.webharvest/schema/2.1/core"</li>
 *   <li><strong>charset</strong> (optional): Default character encoding (default: UTF-8)</li>
 *   <li><strong>scriptlang</strong> (optional): Scripting language: javascript, groovy, beanshell (default: javascript)</li>
 * </ul>
 * 
 * <h3>Global Settings</h3>
 * <ul>
 *   <li><strong>Character Encoding:</strong> Affects all text processing and file operations</li>
 *   <li><strong>Scripting Language:</strong> Used for ${} expressions and script execution</li>
 *   <li><strong>Variable Scope:</strong> Global variables defined here are available everywhere</li>
 *   <li><strong>Error Handling:</strong> Global try-catch blocks can wrap entire scraper</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Root Element:</strong> Must be the outermost element in every WebHarvest configuration</li>
 *   <li><strong>Namespace Required:</strong> Must include xmlns attribute for proper validation</li>
 *   <li><strong>Global Scope:</strong> Variables defined here are available to all child processors</li>
 *   <li><strong>Execution Order:</strong> Processors execute in document order</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li><strong>All processors</strong> - Must be children of config</li>
 *   <li>{@code <def>} - Variable definition</li>
 *   <li>{@code <http>} - HTTP requests</li>
 *   <li>{@code <loop>} - Iteration</li>
 *   <li>{@code <try>} / {@code <catch>} - Error handling</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "config",
    description = "Config plugin that provides configuration functionality",
    version = "2.2.0",
    systemComponent = true
)
@ScrapingScope
public class ConfigPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.ConfigPlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "config";
        }

        @Override
        public String getName() {
            return "Config Plugin";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Config plugin that provides configuration functionality";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = ConfigPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "config", "configuration", "charset", "scripting", "setup"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return ConfigPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "config";
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
                                return "charset";
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
                                return "UTF-8";
                            }

                            @Override
                            public String getDescription() {
                                return "Character encoding for the scraper (e.g., UTF-8, ISO-8859-1)";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return new String[]{"UTF-8", "ISO-8859-1", "Windows-1252", "US-ASCII"};
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
                                return "scriptlang";
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
                                return "beanshell";
                            }

                            @Override
                            public String getDescription() {
                                return "Scripting language for script execution";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return new String[]{"beanshell", "javascript", "groovy"};
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
                    return "2.3.0";
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
                    return new String[]{"charset", "scriptlang"};
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
                    
                    // Validate charset
                    String charset = configuration.getProperty("charset");
                    if (charset != null) {
                        String[] allowedCharsets = {"UTF-8", "ISO-8859-1", "Windows-1252", "US-ASCII"};
                        boolean validCharset = false;
                        for (String allowed : allowedCharsets) {
                            if (allowed.equalsIgnoreCase(charset)) {
                                validCharset = true;
                                break;
                            }
                        }
                        if (!validCharset) {
                            throw new PluginValidationException("Invalid charset: " + charset + ". Allowed values: " + 
                                String.join(", ", allowedCharsets));
                        }
                    }
                    
                    // Validate scriptlang
                    String scriptlang = configuration.getProperty("scriptlang");
                    if (scriptlang != null) {
                        String[] allowedLanguages = {"beanshell", "javascript", "groovy"};
                        boolean validLanguage = false;
                        for (String allowed : allowedLanguages) {
                            if (allowed.equalsIgnoreCase(scriptlang)) {
                                validLanguage = true;
                                break;
                            }
                        }
                        if (!validLanguage) {
                            throw new PluginValidationException("Invalid scriptlang: " + scriptlang + ". Allowed values: " + 
                                String.join(", ", allowedLanguages));
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
        return METADATA.getConfigurationSchema();
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
        // Validate using schema
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        // IMPORTANT: ConfigPlugin executes children WITHOUT nested scope!
        // Variables created by child <def> elements should persist in the root scope,
        // not be cleaned up when the config body finishes.
        // This is critical for IDE and other tools that need to extract results.
        
        LOGGER.debug("ConfigPlugin.executeBody: Executing children in CURRENT scope (no nesting)");
        
        // v2.2.x ULTRA-LEAN: Use AbstractPlugin.executeBody() which now uses PluginBodyExecutor
        // PluginBodyExecutor executes children in current context (no nested scope) - same behavior!
        return super.executeBody(context);
    }

    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        // Set charset
        String charset = config.getProperty("charset", "UTF-8");
        context.setCharset(charset);
        LOGGER.debug("Set charset to: {}", charset);
        
        // Set scripting language
        String scriptlang = config.getProperty("scriptlang", "beanshell");
        ScriptingLanguage scriptingLanguage = ScriptingLanguage.recognize(scriptlang);
        if (scriptingLanguage == null) {
            scriptingLanguage = ScriptingLanguage.BEANSHELL; // Default fallback
        }
        context.setScriptingLanguage(scriptingLanguage);
        LOGGER.debug("Set scripting language to: {}", scriptingLanguage);
        
        // Execute body content (child elements)
        Variable result = executeBody(context);
        
        // Set the result as a variable in the context
        context.setLocalVar("configResult", result);
        LOGGER.debug("Config plugin - set variable 'configResult' in context");
        
        LOGGER.debug("Config plugin executed successfully");
        
        return result;
    }
}
