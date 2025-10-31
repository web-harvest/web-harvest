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
 * Regexp source text definition - used within {@code <regexp>} to specify input text.
 * 
 * <p>
 * The {@code <regexp-source>} processor defines the text content to match against using regular expressions.
 * Must be used as a child element of {@code <regexp>}. Supports variable substitution and complex text sources.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Simple text matching</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[\\d{3}-\\d{3}-\\d{4}]]></regexp-pattern>
 *   <regexp-source>Call us at 555-123-4567 or 800-555-0199</regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Variable substitution</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="htmlContent">
 *   <http url="https://example.com"/>
 * </def>
 * 
 * <regexp>
 *   <regexp-pattern><![CDATA[<a href="([^"]+)">]]></regexp-pattern>
 *   <regexp-source>${htmlContent}</regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Complex text processing</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="rawText">
 *   <file path="data.txt" action="read"/>
 * </def>
 * 
 * <regexp>
 *   <regexp-pattern><![CDATA[Email: ([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})]]></regexp-pattern>
 *   <regexp-source>${rawText}</regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Multi-line text with HTML</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <regexp>
 *   <regexp-pattern><![CDATA[<div class="price">\\$([\\d,]+\\.[\\d]{2})</div>]]></regexp-pattern>
 *   <regexp-source>
 *     <html-to-xml>
 *       <http url="https://shop.example.com/product/123"/>
 *     </html-to-xml>
 *   </regexp-source>
 * </regexp>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>Content (body)</strong>: The text content to match against</li>
 * </ul>
 * 
 * <h3>Source Types</h3>
 * <p>Can contain various types of content:</p>
 * <ul>
 *   <li><strong>Plain text:</strong> Direct text content</li>
 *   <li><strong>Variables:</strong> ${varName} for dynamic content</li>
 *   <li><strong>Processors:</strong> Results from other processors</li>
 *   <li><strong>File content:</strong> Text from file operations</li>
 *   <li><strong>HTTP responses:</strong> Web page content</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <regexp>} element</li>
 *   <li><strong>Text Input</strong>: Provides the text content for pattern matching</li>
 *   <li><strong>Variable Substitution</strong>: Supports ${var} syntax for dynamic content</li>
 *   <li><strong>Multiple Matches</strong>: Can match multiple occurrences in source text</li>
 *   <li><strong>Encoding</strong>: Text encoding handled automatically</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <regexp>} - Main regexp processor (parent element)</li>
 *   <li>{@code <regexp-pattern>} - Regular expression pattern</li>
 *   <li>{@code <regexp-result>} - Access to matched groups</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see RegexpPlugin
 * @see RegexpPatternPlugin
 * @see RegexpResultPlugin
 */
@CorePlugin(
    elementName = "regexp-source",
    description = "Regexp source plugin that provides regexp source functionality",
    version = "2.1.0"
)
public class RegexpSourcePlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "regexp-source";
        }

        @Override
        public String getName() {
            return "Regexp Source Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Regexp source plugin that provides regexp source functionality";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = RegexpSourcePlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "regexp", "source", "regex", "text"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return RegexpSourcePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "regexp-source";
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
                                return "Optional identifier for the regexp-source element";
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
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Do nothing - this is a placeholder plugin
        return EmptyVariable.INSTANCE;
    }
}

















