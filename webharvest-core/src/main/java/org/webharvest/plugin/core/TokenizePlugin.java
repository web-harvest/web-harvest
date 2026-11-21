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
import org.webharvest.plugin.VisualPluginMetadata;
import org.webharvest.plugin.InputPort;
import org.webharvest.plugin.OutputPort;
import org.webharvest.plugin.PortType;
import org.webharvest.plugin.KeyboardShortcut;
import org.webharvest.plugin.AccessibilityMetadata;
import org.webharvest.plugin.LinkedServiceMetadata;
import org.webharvest.plugin.ParentChildRelationship;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Text tokenizer processor - splits text into tokens using delimiters for iteration.
 * 
 * <p>
 * The {@code <tokenize>} processor splits text content into individual tokens (words, lines,
 * or custom-delimited parts) and returns them as a ListVariable for iteration with {@code <loop>}.
 * This is useful for processing CSV data, splitting lines, parsing structured text, or any
 * scenario requiring text segmentation.
 * </p>
 * 
 * <h2>Parameters</h2>
 * <ul>
 *   <li><strong>delimiters:</strong> Characters to use as delimiters (default: space, tab, newline)</li>
 *   <li><strong>trimtokens:</strong> Trim whitespace from each token (default: true)</li>
 *   <li><strong>allowemptytokens:</strong> Include empty tokens in result (default: false)</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Split by default delimiters (whitespace)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="text" value="apple banana cherry date elderberry"/>
 * 
 * <loop item="fruit">
 *   <tokenize>${text}</tokenize>
 *   
 *   <file path="fruits.txt" action="append">
 *     ${fruit}
 *   </file>
 * </loop>
 * <!-- Processes: apple, banana, cherry, date, elderberry -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Split CSV line</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="csvLine" value="John,Doe,john@example.com,30"/>
 * 
 * <loop item="field" index="i">
 *   <tokenize delimiters=",">${csvLine}</tokenize>
 *   
 *   <def var="field${i}" value="${field}"/>
 * </loop>
 * <!-- field0=John, field1=Doe, field2=john@example.com, field3=30 -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Process multiline text</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="urls">
 *   https://example.com/page1
 *   https://example.com/page2
 *   https://example.com/page3
 * </def>
 * 
 * <loop item="url">
 *   <tokenize delimiters="&#10;">${urls}</tokenize>
 *   
 *   <http url="${url}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Parse semicolon-separated values</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="data" value="id:123;name:Product A;price:99.99"/>
 * 
 * <loop item="pair">
 *   <tokenize delimiters=";">${data}</tokenize>
 *   
 *   <!-- Each pair is "key:value" -->
 *   <def var="key">
 *     <tokenize delimiters=":">${pair}</tokenize>
 *   </def>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Trim vs no-trim</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="text" value="  apple  ,  banana  ,  cherry  "/>
 * 
 * <!-- With trimming (default) -->
 * <tokenize delimiters="," trimtokens="true">${text}</tokenize>
 * <!-- Result: ["apple", "banana", "cherry"] -->
 * 
 * <!-- Without trimming -->
 * <tokenize delimiters="," trimtokens="false">${text}</tokenize>
 * <!-- Result: ["  apple  ", "  banana  ", "  cherry  "] -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Allow empty tokens</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="data" value="apple,,cherry,,elderberry"/>
 * 
 * <!-- Skip empty tokens (default) -->
 * <tokenize delimiters="," allowemptytokens="false">${data}</tokenize>
 * <!-- Result: ["apple", "cherry", "elderberry"] -->
 * 
 * <!-- Keep empty tokens -->
 * <tokenize delimiters="," allowemptytokens="true">${data}</tokenize>
 * <!-- Result: ["apple", "", "cherry", "", "elderberry"] -->
 * </config>
 * }</pre>
 * 
 * <h3>Common Use Cases</h3>
 * <ul>
 *   <li><strong>CSV parsing:</strong> Split comma/tab delimited data</li>
 *   <li><strong>Line processing:</strong> Split multiline text into individual lines</li>
 *   <li><strong>URL lists:</strong> Process newline-separated URLs</li>
 *   <li><strong>Tag extraction:</strong> Split space-separated tags or keywords</li>
 *   <li><strong>Path parsing:</strong> Split file paths by / or \</li>
 * </ul>
 * 
 * <h3>Delimiter Syntax</h3>
 * <p>
 * The {@code delimiters} attribute can contain multiple delimiter characters.
 * Any of these characters will split the text:
 * </p>
 * <pre>{@code
 * <!-- Split by comma OR semicolon -->
 * <tokenize delimiters=",;">${text}</tokenize>
 * 
 * <!-- Split by newline (&#10; is line feed) -->
 * <tokenize delimiters="&#10;">${text}</tokenize>
 * 
 * <!-- Split by space, tab, newline -->
 * <tokenize delimiters=" &#9;&#10;">${text}</tokenize>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Default delimiters:</strong> Space, tab, newline, carriage return</li>
 *   <li><strong>Multiple delimiters:</strong> All delimiter chars split independently</li>
 *   <li><strong>Returns ListVariable:</strong> Result is a list, use with {@code <loop>}</li>
 *   <li><strong>Empty tokens:</strong> By default filtered out (set allowemptytokens=true to keep)</li>
 *   <li><strong>Trim by default:</strong> Leading/trailing whitespace removed (set trimtokens=false to keep)</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <regexp>} - Advanced text splitting with regex patterns</li>
 *   <li>{@code <loop>} - Iterate over tokenized results</li>
 *   <li>{@code <text>} - Simple text manipulation</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 2.0
 */
@CorePlugin(elementName = "tokenize")
public class TokenizePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.TokenizePlugin");
    
    private static final String DELIMITERS_ATT_NAME = "delimiters";
    private static final String TRIM_TOKENS_ATT_NAME = "trimtokens";
    private static final String ALLOW_EMPTY_TOKENS_ATT_NAME = "allowemptytokens";

    /**
     * Metadata for TokenizePlugin.
     * 
     * <p>Standalone plugin - splits text into tokens using delimiters for iteration.</p>
     */
    public static class TokenizePluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "tokenize";
        }

        @Override
        public String getName() {
            return "Tokenize";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Splits text content into individual tokens (words, lines, or custom-delimited parts) and returns them as a list for iteration with loop. Useful for processing CSV data, splitting lines, parsing structured text, or any scenario requiring text segmentation. Supports customizable delimiters, token trimming, and empty token handling.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = TokenizePlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "tokenize", "split", "text", "parsing", "csv"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return TokenizePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "tokenize";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new TokenizePluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-cut";
        }
        
        @Override
        public String getColor() {
            return "#10B981"; // Green for text processing
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
                new InputPort("text", "Text to tokenize (from body content)", PortType.STRING, false),
                new InputPort("delimiters", "Delimiter characters (from delimiters attribute, default: \\n\\r)", PortType.STRING, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "List of tokens for iteration", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Splits text content into individual tokens using delimiters. Returns tokens as a list for iteration with loop. Useful for processing CSV data, splitting lines, or parsing structured text.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+T", "Add Tokenize", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Tokenize Plugin",
                "Splits text content into individual tokens using delimiters. Returns tokens as a list for iteration.",
                new String[]{"Tab", "Enter", "Escape"}
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
            return new String[0]; // Can contain any child plugins
        }
    }
    
    /**
     * Configuration schema for TokenizePlugin.
     */
    public static class TokenizePluginConfigurationSchema implements PluginConfigurationSchema {
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
            return new String[0]; // Can contain any child plugins
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
            // No validation needed - all attributes are optional
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new TokenizePluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new TokenizePluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No validation needed - all attributes are optional
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
            text = bodyContent.toString(context.getCharset());
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
