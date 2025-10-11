/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.webharvest.exception.PluginException;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * Tokenize plugin that splits text into tokens based on delimiters.
 * Migrated from org.webharvest.runtime.processors.plugins.TokenizePlugin
 */
@CorePlugin(elementName = "tokenize")
public class TokenizePlugin extends AbstractPlugin {

    private static final String DELIMITERS_ATT_NAME = "delimiters";
    private static final String TRIM_TOKENS_ATT_NAME = "trimtokens";
    private static final String ALLOW_EMPTY_TOKENS_ATT_NAME = "allowemptytokens";

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "tokenize"; }
            @Override
            public String getName() { return "Tokenize Plugin"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Tokenize plugin that splits text into tokens based on delimiters"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
            public String getCategory() { return "core"; }
            @Override
            public String[] getTags() { return new String[]{"core", "tokenize", "split", "text"}; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public String getClassName() { return TokenizePlugin.class.getName(); }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getElementName() { return "tokenize"; }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return TokenizePlugin.this.getConfigurationSchema(); }
        };
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - all attributes are optional
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[]{
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() {
                            return DELIMITERS_ATT_NAME;
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
                            return "\\n\\r";
                        }

                        @Override
                        public String getDescription() {
                            return "Delimiters to use for tokenization";
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
                            return TRIM_TOKENS_ATT_NAME;
                        }

                        @Override
                        public PluginConfigurationSchema.AttributeType getType() {
                            return PluginConfigurationSchema.AttributeType.BOOLEAN;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "true";
                        }

                        @Override
                        public String getDescription() {
                            return "Whether to trim tokens";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return new String[]{"true", "false"};
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
                            return ALLOW_EMPTY_TOKENS_ATT_NAME;
                        }

                        @Override
                        public PluginConfigurationSchema.AttributeType getType() {
                            return PluginConfigurationSchema.AttributeType.BOOLEAN;
                        }

                        @Override
                        public boolean isRequired() {
                            return false;
                        }

                        @Override
                        public String getDefaultValue() {
                            return "false";
                        }

                        @Override
                        public String getDescription() {
                            return "Whether to allow empty tokens";
                        }

                        @Override
                        public String[] getAllowedValues() {
                            return new String[]{"true", "false"};
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
                return new String[]{DELIMITERS_ATT_NAME, TRIM_TOKENS_ATT_NAME, ALLOW_EMPTY_TOKENS_ATT_NAME};
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
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        String delimiters = getConfiguration().getProperty("delimiters");
        if (delimiters == null || "".equals(delimiters)) {
            delimiters = "\n\r";
        }
        boolean trimTokens = getConfiguration().getBooleanProperty("trimtokens", true);
        boolean allowEmptyTokens = getConfiguration().getBooleanProperty("allowemptytokens", false);
        
        // Execute body content to get text to tokenize
        Variable bodyContent = executeBody(context);
        String text = "";
        
        if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
            text = bodyContent.toString();
            LOGGER.debug("TokenizePlugin: Retrieved text content: " + text);
        } else {
            LOGGER.debug("TokenizePlugin: No body content available");
            return new ListVariable();
        }

        String[] tokens = CommonUtil.tokenize(text, delimiters, trimTokens, allowEmptyTokens);

        ListVariable listVariable = new ListVariable();
        for (String token : tokens) {
            listVariable.addVariable(new NodeVariable(token));
        }

        return listVariable;
    }
}
