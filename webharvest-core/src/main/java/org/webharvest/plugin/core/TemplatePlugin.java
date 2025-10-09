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
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template plugin that evaluates template content using scripting languages.
 * Supports ${expression} syntax with beanshell, javascript, and groovy.
 * This plugin uses BaseTemplater for template processing.
 */
@CorePlugin(
    elementName = "template",
    description = "Template plugin that provides template processing functionality",
    version = "2.2.0"
)
public class TemplatePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.TemplatePlugin");

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "template";
        }

        @Override
        public String getName() {
            return "Template Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "Template plugin that provides template processing functionality";
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
            return new String[]{"core", "template", "scripting", "beanshell", "javascript", "groovy"};
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
                                return "Optional identifier for the template element";
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
                                return "language";
                            }

                            @Override
                            public AttributeType getType() {
                                return AttributeType.ENUM;
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
                                return "Scripting language to use for template evaluation";
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
                    return new String[]{"id", "language"};
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
                    String language = configuration.getProperty("language");
                    if (language != null && !language.isEmpty()) {
                        if (!"beanshell".equals(language) && !"javascript".equals(language) && !"groovy".equals(language)) {
                            throw new PluginValidationException("Invalid language value: " + language + ". Must be one of: beanshell, javascript, groovy");
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
            return "template";
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getClassName() {
            return TemplatePlugin.class.getName();
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
    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Template plugin doesn't need complex initialization
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.info("TemplatePlugin.doExecute: START");
        
        PluginConfiguration config = getConfiguration();
        
        // Get template configuration
        String language = config.getProperty("language", "beanshell");
        String templateId = config.getProperty("id", "");
        
        LOGGER.info("TemplatePlugin.doExecute: Language: {}, ID: {}", language, templateId);
        LOGGER.info("TemplatePlugin.doExecute: Context charset: {}", context.getCharset());
        
            try {
                // Execute body content to get template text
                Variable bodyContent = super.executeBody(context);
                LOGGER.info("TemplatePlugin.doExecute: Body content result: {}", bodyContent);
                
                String templateContent = "";
                if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                    templateContent = bodyContent.toString();
                    LOGGER.info("TemplatePlugin.doExecute: Using body content: [{}]", templateContent);
                } else {
                    // Try to get raw text from child elements as XML
                    LOGGER.info("TemplatePlugin.doExecute: Body content empty, trying XML conversion");
                    templateContent = getChildElementsAsRawText();
                    LOGGER.info("TemplatePlugin.doExecute: XML conversion result: [{}]", templateContent);
                }
            
            if (templateContent == null || templateContent.isEmpty()) {
                LOGGER.warn("TemplatePlugin.doExecute: No template content - returning EmptyVariable");
                return EmptyVariable.INSTANCE;
            }
            
            LOGGER.info("TemplatePlugin.doExecute: Final template content: [{}]", templateContent);
            
            // Convert language string to ScriptingLanguage enum
            ScriptingLanguage scriptingLanguage = convertToScriptingLanguage(language);
            LOGGER.info("TemplatePlugin.doExecute: Using scripting language: {}", scriptingLanguage);
            
            // Evaluate the template using BaseTemplater
            LOGGER.info("TemplatePlugin.doExecute: Calling BaseTemplater.evaluateToVariable");
            Variable result;
            try {
                result = BaseTemplater.evaluateToVariable(templateContent, scriptingLanguage, context);
                LOGGER.info("TemplatePlugin.doExecute: BaseTemplater result: {}", result);
                LOGGER.info("TemplatePlugin.doExecute: BaseTemplater result class: {}", result != null ? result.getClass().getName() : "null");
            } catch (Exception e) {
                LOGGER.warn("TemplatePlugin.doExecute: BaseTemplater failed, returning template as-is: {}", e.getMessage());
                // If BaseTemplater fails (e.g., in tests), return the template content as-is
                result = new NodeVariable(templateContent);
            }
            
            // Set the result as a variable in the context for compatibility
            if (templateId != null && !templateId.isEmpty()) {
                context.setLocalVar(templateId, result);
                LOGGER.info("TemplatePlugin.doExecute: Set variable '{}' in context", templateId);
            }
            
            LOGGER.info("TemplatePlugin.doExecute: Returning result: {}", result);
            return result;
            
        } catch (Exception e) {
            LOGGER.error("Template plugin - error during execution", e);
            return EmptyVariable.INSTANCE;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        String language = configuration.getProperty("language");
        if (language != null && !language.isEmpty()) {
            if (!"beanshell".equals(language) && !"javascript".equals(language) && !"groovy".equals(language)) {
                throw new PluginValidationException("Invalid language value: " + language + ". Must be one of: beanshell, javascript, groovy");
            }
        }
    }
    
    /**
     * Gets child elements as raw text (v2.2.x ULTRA-LEAN).
     * Uses new architecture: _textContent property from configuration
     */
    private String getChildElementsAsRawText() {
        try {
            // v2.2.x: Use _textContent property from PluginConfiguration
            // StreamingXmlParser populates this with CDATA and text content
            String textContent = getConfiguration().getProperty("_textContent");
            if (textContent != null && !textContent.trim().isEmpty()) {
                LOGGER.debug("Returning _textContent: {}", textContent);
                return textContent;
            }
            LOGGER.debug("No _textContent available in configuration");
            return "";
        } catch (Exception e) {
            LOGGER.warn("Could not get child elements as raw text: " + e.getMessage());
            return "";
        }
    }
    
    /**
     * Convert language string to ScriptingLanguage enum
     */
    private ScriptingLanguage convertToScriptingLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return null; // Use default from context
        }
        
        switch (language.toLowerCase()) {
            case "beanshell":
                return ScriptingLanguage.BEANSHELL;
            case "javascript":
                return ScriptingLanguage.JAVASCRIPT;
            case "groovy":
                return ScriptingLanguage.GROOVY;
            default:
                LOGGER.warn("Template plugin - unknown language '{}', using default", language);
                return null; // Use default from context
        }
    }
}