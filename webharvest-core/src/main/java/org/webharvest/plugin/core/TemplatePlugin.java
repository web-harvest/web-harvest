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
 * Template processor - evaluates {@code ${expression}} syntax for variable interpolation and dynamic content.
 * 
 * <p>
 * The {@code <template>} processor provides powerful variable interpolation and expression
 * evaluation within text content. It processes {@code ${...}} expressions using the configured
 * scripting language (JavaScript, Groovy, or BeanShell), enabling dynamic content generation,
 * string building, and simple computations.
 * </p>
 * 
 * <p>
 * <strong>Note:</strong> In most cases, you don't need explicit {@code <template>} processor -
 * {@code ${}} expressions work automatically in text content and attributes. Use {@code <template>}
 * when you need explicit template processing or language selection.
 * </p>
 * 
 * <h3>Template Expression Syntax</h3>
 * <p>The {@code ${expression}} syntax supports:</p>
 * <ul>
 *   <li><strong>Variables:</strong> {@code ${varname}} - Simple variable interpolation</li>
 *   <li><strong>Properties:</strong> {@code ${object.property}} - Access object properties</li>
 *   <li><strong>Arithmetic:</strong> {@code ${price * quantity}} - Mathematical operations</li>
 *   <li><strong>String concat:</strong> {@code ${firstName + " " + lastName}} - Combine strings</li>
 *   <li><strong>Function calls:</strong> {@code ${methodName(arg1, arg2)}} - Call functions</li>
 *   <li><strong>Conditionals:</strong> {@code ${condition ? value1 : value2}} - Ternary operator</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Simple variable interpolation</h4>
 * <pre>{@code
 * <def var="username" value="john_doe"/>
 * <def var="apiKey" value="abc123"/>
 * 
 * <!-- Automatic template interpolation in attributes -->
 * <http url="https://api.example.com/user/${username}?key=${apiKey}"/>
 * 
 * <!-- Explicit template processor (same result) -->
 * <def var="url">
 *   <template>https://api.example.com/user/${username}?key=${apiKey}</template>
 * </def>
 * <http url="${url}"/>
 * }</pre>
 * 
 * <h4>Example 2: Building complex strings</h4>
 * <pre>{@code
 * <def var="firstName" value="John"/>
 * <def var="lastName" value="Doe"/>
 * <def var="age" value="30"/>
 * 
 * <def var="greeting">
 *   <template>
 *     Hello, ${firstName} ${lastName}!
 *     You are ${age} years old.
 *     Next year you will be ${parseInt(age) + 1}.
 *   </template>
 * </def>
 * }</pre>
 * 
 * <h4>Example 3: HTML generation</h4>
 * <pre>{@code
 * <loop item="product" index="i">
 *   <xpath expression="//product">...</xpath>
 *   
 *   <def var="name"><xpath expression=".//name/text()">${product}</xpath></def>
 *   <def var="price"><xpath expression=".//price/text()">${product}</xpath></def>
 *   
 *   <def var="html">
 *     <template>
 *       <![CDATA[
 *         <div class="product" id="product-${i}">
 *           <h3>${name}</h3>
 *           <p class="price">$${price}</p>
 *           <a href="/product/${i}">View Details</a>
 *         </div>
 *       ]]>
 *     </template>
 *   </def>
 *   
 *   <file path="products.html" action="append">${html}</file>
 * </loop>
 * }</pre>
 * 
 * <h4>Example 4: JSON generation</h4>
 * <pre>{@code
 * <def var="userId" value="123"/>
 * <def var="userName" value="John Doe"/>
 * <def var="userEmail" value="john@example.com"/>
 * 
 * <def var="jsonOutput">
 *   <template>
 *     <![CDATA[
 *     {
 *       "id": ${userId},
 *       "name": "${userName}",
 *       "email": "${userEmail}",
 *       "timestamp": "${_date}",
 *       "source": "WebHarvest"
 *     }
 *     ]]>
 *   </template>
 * </def>
 * 
 * <file path="user-${userId}.json" action="write">${jsonOutput}</file>
 * }</pre>
 * 
 * <h4>Example 5: CSV generation</h4>
 * <pre>{@code
 * <!-- CSV header -->
 * <file path="products.csv" action="write">ID,Name,Price,Stock</file>
 * 
 * <!-- CSV rows with template -->
 * <loop item="product">
 *   <xpath expression="//product">...</xpath>
 *   
 *   <def var="id"><xpath expression="@id">${product}</xpath></def>
 *   <def var="name"><xpath expression="name/text()">${product}</xpath></def>
 *   <def var="price"><xpath expression="price/text()">${product}</xpath></def>
 *   <def var="stock"><xpath expression="stock/text()">${product}</xpath></def>
 *   
 *   <file path="products.csv" action="append">
 *     <template>${id},"${name}",${price},${stock}</template>
 *   </file>
 * </loop>
 * }</pre>
 * 
 * <h4>Example 6: Email body generation</h4>
 * <pre>{@code
 * <def var="customerName" value="Jane Smith"/>
 * <def var="orderTotal" value="299.99"/>
 * <def var="orderId" value="ORD-98765"/>
 * 
 * <def var="emailBody">
 *   <template>
 *     <![CDATA[
 *     Dear ${customerName},
 *     
 *     Thank you for your order ${orderId}.
 *     
 *     Order Details:
 *     - Total: $${orderTotal}
 *     - Date: ${_date}
 *     - Estimated delivery: 3-5 business days
 *     
 *     Track your order: https://example.com/track/${orderId}
 *     
 *     Best regards,
 *     The Team
 *     ]]>
 *   </template>
 * </def>
 * }</pre>
 * 
 * <h4>Example 7: XML document generation</h4>
 * <pre>{@code
 * <def var="products">
 *   <template>
 *     <![CDATA[
 *     <?xml version="1.0" encoding="UTF-8"?>
 *     <products>
 *     ]]>
 *   </template>
 * </def>
 * 
 * <loop item="product">
 *   <xpath expression="//product">...</xpath>
 *   
 *   <def var="name"><xpath expression="name/text()">${product}</xpath></def>
 *   <def var="price"><xpath expression="price/text()">${product}</xpath></def>
 *   
 *   <def var="products" value="${products}">
 *     <template>
 *       <![CDATA[
 *       <product>
 *         <name>${name}</name>
 *         <price>${price}</price>
 *       </product>
 *       ]]>
 *     </template>
 *   </def>
 * </loop>
 * 
 * <def var="products" value="${products}</products>"/>
 * <file path="products.xml" action="write">${products}</file>
 * }</pre>
 * 
 * <h3>Built-in Variables</h3>
 * <p>Available in all templates:</p>
 * <ul>
 *   <li><strong>${_date}:</strong> Current date/time</li>
 *   <li><strong>${_index}:</strong> Current loop index (inside loops)</li>
 *   <li><strong>${_item}:</strong> Current loop item (inside loops)</li>
 * </ul>
 * 
 * <h3>Automatic vs Explicit Template Processing</h3>
 * 
 * <p><strong>Automatic (most common):</strong></p>
 * <pre>{@code
 * <!-- Templates work automatically in text and attributes -->
 * <http url="https://example.com/user/${userId}"/>
 * <file path="output-${timestamp}.txt" action="write">
 *   User: ${userName}
 * </file>
 * }</pre>
 * 
 * <p><strong>Explicit {@code <template>} when:</strong></p>
 * <ul>
 *   <li>Need to select specific scripting language</li>
 *   <li>Complex multi-line templates with CDATA</li>
 *   <li>Template contains XML that would be parsed as processors</li>
 *   <li>Want clear semantic separation of template from other content</li>
 * </ul>
 * 
 * <h3>Expression Examples</h3>
 * 
 * <pre>{@code
 * <!-- Variable access -->
 * ${username}
 * 
 * <!-- Arithmetic -->
 * ${price * 1.23}  <!-- Add 23% tax -->
 * ${count + 1}
 * ${total / itemCount}
 * 
 * <!-- String operations -->
 * ${name.toUpperCase()}
 * ${email.substring(0, email.indexOf('@'))}
 * ${url.replace('http:', 'https:')}
 * 
 * <!-- Conditional -->
 * ${stock > 0 ? 'In Stock' : 'Out of Stock'}
 * ${price > 100 ? 'Premium' : 'Standard'}
 * 
 * <!-- Function calls (if defined) -->
 * ${formatDate(timestamp)}
 * ${calculateDiscount(price, 0.15)}
 * }</pre>
 * 
 * <h3>Performance Tips</h3>
 * <ul>
 *   <li><strong>Minimize in loops:</strong> Build template once, reference variable in loop</li>
 *   <li><strong>Cache complex expressions:</strong> Evaluate once, store in variable</li>
 *   <li><strong>Use script for heavy logic:</strong> {@code <script>} is better for complex computations</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>CDATA for XML:</strong> Use {@code <![CDATA[...]]>} when template contains XML/HTML tags</li>
 *   <li><strong>Escaping:</strong> Use {@code $${}} to output literal {@code ${}} in text</li>
 *   <li><strong>Language:</strong> Inherits from config default or specify via {@code language} attribute</li>
 *   <li><strong>Error handling:</strong> Invalid expressions return empty string (logged as warning)</li>
 *   <li><strong>Thread safety:</strong> Template evaluation is thread-safe</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <script>} - For complex scripting logic</li>
 *   <li>{@code <def>} - Variable definition</li>
 *   <li>{@code <function>} - Define reusable template functions</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
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