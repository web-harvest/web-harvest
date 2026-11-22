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
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import java.util.ArrayList;
import java.util.List;
import org.webharvest.utils.CommonUtil;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text processor - simple text content wrapper and charset converter.
 * 
 * <p>
 * The {@code <text>} processor wraps text content and optionally converts character encoding.
 * It's mainly used for explicit text declaration or charset conversion scenarios.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Explicit text content</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="message">
 *   <text>This is plain text content</text>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: Charset conversion</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="latinText">
 *   <text charset="ISO-8859-1">
 *     <file path="legacy-file.txt" action="read"/>
 *   </text>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>Note</h3>
 * <p>In most cases, plain text works without {@code <text>} wrapper. This processor
 * is rarely needed in modern configurations.</p>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "text",
    description = "Text plugin that processes text content with charset and delimiter support",
    version = "2.2.0",
    category = "Utilities"
)
public class TextPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.TextPlugin");

    /**
     * Metadata for TextPlugin.
     * 
     * <p>Standalone plugin - wraps text content and optionally converts character encoding.</p>
     */
    public static class TextPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "text";
        }

        @Override
        public String getName() {
            return "Text";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Wraps text content and optionally converts character encoding. Mainly used for explicit text declaration or charset conversion scenarios. In most cases, plain text works without text wrapper.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = TextPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Utilities";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "text", "processing", "charset", "delimiter", "content", "text-processing"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return TextPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "text";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new TextPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-file-alt";
        }
        
        @Override
        public String getColor() {
            return "#F59E0B"; // Amber for text processing
        }
        
        @Override
        public String getGroup() {
            return "Text Processing";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 120;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("content", "Text content to process (from body content)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Processed text content", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.STRING;
        }
        
        @Override
        public String getTooltip() {
            return "Wraps text content and optionally converts character encoding. Mainly used for explicit text declaration or charset conversion scenarios. Supports charset conversion and delimiter joining.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Rarely used directly
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Text Plugin",
                "Wraps text content and optionally converts character encoding. Mainly used for explicit text declaration or charset conversion scenarios.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return null; // Standalone plugin
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // Can contain any child plugins for text content generation
        }
    }
    
    /**
     * Configuration schema for TextPlugin.
     */
    public static class TextPluginConfigurationSchema implements PluginConfigurationSchema {
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
                                return new String[]{"UTF-8", "ISO-8859-1", "Windows-1252", "US-ASCII", "UTF-16", "UTF-32"};
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
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
            for (PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
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
            return new String[0]; // Can contain any child plugins for text content generation
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
            return "2.3.0";
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
    }

    @Override
    public PluginMetadata getMetadata() {
        return new TextPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new TextPluginMetadata().getConfigurationSchema();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        LOGGER.debug("TextPlugin.doInitialize: START");
        // Text plugin doesn't need complex initialization
        LOGGER.debug("TextPlugin.doInitialize: END");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        // TextPlugin should return the child element value directly, not use BodyProcessor
        return doExecute(context);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("TextPlugin.doExecute: START");
        
        try {
            // v2.2.x ULTRA-LEAN: Get text content from PluginConfiguration
            String textContent = getConfiguration().getProperty("_textContent", "");
            LOGGER.debug("TextPlugin: textContent = '{}'", textContent);
            
            // v2.2.x FIX: Apply template interpolation to resolve ${variable} expressions
            if (textContent != null && !textContent.isEmpty() && textContent.contains("${")) {
                if (BaseTemplater.scriptEngineFactory != null) {
                    LOGGER.debug("TextPlugin: Applying template interpolation");
                    Variable interpolated = BaseTemplater.evaluateToVariable(textContent, 
                                                                             context.getScriptingLanguage(), 
                                                                             context);
                    LOGGER.debug("TextPlugin: Interpolated result = '{}'", interpolated);
                    return interpolated;
                } else {
                    LOGGER.warn("TextPlugin: BaseTemplater not available, returning text without interpolation");
                }
            }
            
            // Return as NodeVariable (no interpolation needed or BaseTemplater not available)
            return new NodeVariable(textContent != null ? textContent : "");
            
        } catch (Exception e) {
            LOGGER.error("TextPlugin.doExecute: Exception during execution: {}", e.getMessage(), e);
            throw new RuntimeException("TextPlugin execution failed: " + e.getMessage(), e);
        }
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
