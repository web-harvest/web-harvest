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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;

/**
 * Text plugin that processes text content with charset and delimiter support.
 * This is a core plugin migrated from the original TextProcessor.
 * 
 * <p>
 * The text plugin allows processing of text content with configurable charset
 * and delimiter settings. It can handle both single text values and lists of text.
 * </p>
 * 
 * @core This is a core plugin
 * @author Robert Bala
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "text",
    description = "Text plugin that processes text content with charset and delimiter support",
    version = "2.1.0"
)
public class TextPlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "text";
        }

        @Override
        public String getName() {
            return "Text Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Text plugin that processes text content with charset and delimiter support";
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
            return new String[]{"core", "text", "charset", "delimiter"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[]{
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "id";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.STRING;
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
                                return "Optional identifier for the text element";
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
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "charset";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.STRING;
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
                                return "Character encoding for text processing";
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
                        new AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "delimiter";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.STRING;
                            }

                            @Override
                            public boolean isRequired() {
                                return false;
                            }

                            @Override
                            public String getDefaultValue() {
                                return "\\n";
                            }

                            @Override
                            public String getDescription() {
                                return "Delimiter for joining multiple text values";
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
                public AttributeDefinition getAttribute(String name) {
                    for (AttributeDefinition attr : getAttributeDefinitions()) {
                        if (attr.getName().equals(name)) {
                            return attr;
                        }
                    }
                    return null;
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[0];
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[]{"id", "charset", "delimiter"};
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
                public boolean allowsBody() {
                    return true;
                }

                @Override
                public String getVersion() {
                    return "2.1.0";
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    // Validate charset if provided
                    String charset = configuration.getProperty("charset");
                    if (charset != null && !charset.isEmpty()) {
                        try {
                            java.nio.charset.Charset.forName(charset);
                        } catch (Exception e) {
                            throw new PluginValidationException("Invalid charset: " + charset);
                        }
                    }
                }
            };
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String getElementName() {
            return "text";
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getClassName() {
            return TextPlugin.class.getName();
        }
    };

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Text plugin doesn't need complex initialization
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Get charset from configuration or context
        String charset = null;
        if (getConfiguration() != null) {
            charset = getConfiguration().getProperty("charset");
        }
        if (CommonUtil.isEmptyString(charset)) {
            charset = context.getCharset();
        }

        // Get delimiter from configuration
        String delimiter = null;
        if (getConfiguration() != null) {
            delimiter = getConfiguration().getProperty("delimiter");
        }
        if (delimiter == null) {
            delimiter = "\n";
        }

        // TODO: Implement body execution
        // This would need to be implemented to execute child plugins
        // For now, we return an empty variable
        
        return new NodeVariable("");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Validate charset if provided
        String charset = configuration.getProperty("charset");
        if (charset != null && !charset.isEmpty()) {
            try {
                java.nio.charset.Charset.forName(charset);
            } catch (Exception e) {
                throw new PluginValidationException("Invalid charset: " + charset);
            }
        }
    }
}
