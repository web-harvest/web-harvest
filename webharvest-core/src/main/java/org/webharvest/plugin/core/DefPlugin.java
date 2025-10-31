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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;
// REMOVED in v2.2.x
import org.webharvest.definition.XmlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variable definition plugin - defines or updates variables in the execution context.
 * 
 * <p>
 * The {@code <def>} element is one of the most fundamental processors in WebHarvest.
 * It creates or updates variables in the current scope, making data available for
 * subsequent processors through variable references ({@code ${varname}}).
 * </p>
 * 
 * <h3>Variable Value Sources</h3>
 * <p>Variable value can be provided in three ways (in order of precedence):</p>
 * <ol>
 *   <li><strong>value attribute:</strong> Direct value assignment with template interpolation</li>
 *   <li><strong>Child processors:</strong> Result of executing nested processors</li>
 *   <li><strong>Text content:</strong> Plain text or text with variable interpolation</li>
 * </ol>
 * 
 * <h3>Template Interpolation (v2.2.x Enhancement)</h3>
 * <p>
 * All value sources support template interpolation, allowing you to reference
 * other variables using {@code ${varname}} syntax. This applies to both the
 * {@code value} attribute and text content.
 * </p>
 * 
 * <h3>Variable Scope</h3>
 * <p>
 * Variables are stored in the current execution context and are accessible to:
 * </p>
 * <ul>
 *   <li>All subsequent processors in the same scope</li>
 *   <li>Child scopes (nested processors)</li>
 *   <li>Template expressions using {@code ${varname}} syntax</li>
 *   <li>Script processors via context API</li>
 * </ul>
 * <p>
 * Variables defined inside functions or loops are local to that scope unless
 * explicitly made global.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Simple string value</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="apiKey" value="abc123xyz"/>
 * 
 * <!-- Use the variable -->
 * <http url="https://api.example.com?key=${apiKey}"/>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Value from child processor (HTTP response)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="htmlContent">
 *   <http url="https://example.com"/>
 * </def>
 * 
 * <!-- Extract data from stored HTML -->
 * <def var="title">
 *   <xpath expression="//title/text()">
 *     <html-to-xml>${htmlContent}</html-to-xml>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Template interpolation in value attribute</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="baseUrl" value="https://example.com"/>
 * <def var="apiPath" value="/api/v1"/>
 * 
 * <!-- Combine variables -->
 * <def var="fullUrl" value="${baseUrl}${apiPath}/users"/>
 * <!-- Result: https://example.com/api/v1/users -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Text content with interpolation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="firstName" value="John"/>
 * <def var="lastName" value="Doe"/>
 * 
 * <def var="fullName">Hello, ${firstName} ${lastName}!</def>
 * <!-- Result: Hello, John Doe! -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Complex value from multiple processors</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="productData">
 *   <http url="https://api.example.com/product/123"/>
 *   <json-to-xml>${http}</json-to-xml>
 *   <xpath expression="//product/name/text()"/>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Updating existing variables</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="counter" value="0"/>
 * 
 * <loop item="product" index="i" maxloops="100">
 *   <http url="https://api.example.com/products"/>
 *   
 *   <!-- Update counter (concatenation, not arithmetic) -->
 *   <def var="counter" value="${counter}1"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Common Patterns</h3>
 * 
 * <h4>Configuration Variables</h4>
 * <pre>{@code
 * <config>
 *   <!-- Define constants at the top -->
 *   <def var="BASE_URL" value="https://production.example.com"/>
 *   <def var="API_KEY" value="secret_key_here"/>
 *   <def var="MAX_RETRIES" value="3"/>
 *   
 *   <!-- Rest of scraper logic -->
 *   <http url="${BASE_URL}/api/data?key=${API_KEY}"/>
 * </config>
 * }</pre>
 * 
 * <h4>Data Accumulation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="allProducts" value=""/>
 * 
 * <loop item="page" index="i" maxloops="10">
 *   <def var="pageData">
 *     <http url="https://api.example.com/products?page=${i}"/>
 *   </def>
 *   
 *   <!-- Accumulate results -->
 *   <def var="allProducts" value="${allProducts}${pageData}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Variable names:</strong> Must be valid identifiers (alphanumeric + underscore)</li>
 *   <li><strong>Overwriting:</strong> Defining a variable with existing name updates its value</li>
 *   <li><strong>Empty values:</strong> If no value is provided (no attribute, no body), variable is set to empty string</li>
 *   <li><strong>Type preservation:</strong> Variables store the actual Variable type (NodeVariable, ListVariable, etc.)</li>
 *   <li><strong>Performance:</strong> Variable lookup is O(1), safe to use extensively</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <var>} - Alternative syntax for variable definition</li>
 *   <li>{@code <get-var>} - Retrieve variable value</li>
 *   <li>{@code <set-var>} - Update existing variable (deprecated, use def instead)</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "def",
    description = "Defines variables with their values",
    version = "2.1.0",
    category = "Variables"
)
public class DefPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.DefPlugin");

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "def"; }
            @Override
            public String getName() { return "def"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Defines variables with their values"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
        public String getCategory() {
            CorePlugin annotation = DefPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Variables";
        }
            @Override
            public String[] getTags() { return new String[0]; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return DefPlugin.this.getConfigurationSchema(); }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public String getElementName() { return "def"; }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getClassName() { return DefPlugin.this.getClass().getName(); }
        };
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
                return new PluginConfigurationSchema.AttributeDefinition[] {
                    new PluginConfigurationSchema.AttributeDefinition() {
                        @Override
                        public String getName() { return "var"; }
                        @Override
                        public String getDescription() { return "Variable name to define"; }
                        @Override
                        public PluginConfigurationSchema.AttributeType getType() { return PluginConfigurationSchema.AttributeType.STRING; }
                        @Override
                        public boolean isRequired() { return true; }
                        @Override
                        public Number getMaxValue() { return null; }
                        @Override
                        public Number getMinValue() { return null; }
                        @Override
                        public String[] getAllowedValues() { return null; }
                        @Override
                        public String getDefaultValue() { return null; }
                        @Override
                        public String getPattern() { return null; }
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
                return new String[0];
            }

            @Override
            public String[] getRequiredAttributes() {
                return new String[]{"var"};
            }
            
            @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                // Validation is handled in doValidateConfiguration
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No initialization needed for DefPlugin
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.info("DefPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();
        LOGGER.info("DefPlugin.doExecute: config: {}", config);

        String varName = config.getProperty("var", "");
        LOGGER.info("DefPlugin.doExecute: varName = '{}'", varName);

        if (varName.isEmpty()) {
            LOGGER.error("DefPlugin.doExecute: Variable name is required for def operation");
            throw new RuntimeException("Variable name is required for def operation");
        }

        try {
            // Get value from "value" attribute or execute child elements
            String valueAttr = config.getProperty("value", "");
            Variable value;
            
            if (!valueAttr.isEmpty()) {
                // Use value from attribute WITH template interpolation (v2.2.x FIX)
                LOGGER.debug("DefPlugin.doExecute: Processing value attribute with template interpolation");
                // Check if BaseTemplater is available (it might be null in unit tests)
                if (BaseTemplater.scriptEngineFactory != null) {
                    Variable interpolated = BaseTemplater.evaluateToVariable(valueAttr, 
                                                                             context.getScriptingLanguage(), 
                                                                             context);
                    value = interpolated;
                    LOGGER.debug("DefPlugin.doExecute: Interpolated value attribute: {}", value);
                } else {
                    // Fallback for unit tests or when BaseTemplater is not initialized
                    value = new NodeVariable(valueAttr);
                    LOGGER.warn("DefPlugin.doExecute: BaseTemplater not available, using value attribute without interpolation");
                }
            } else {
                // Execute child elements to get the value
                value = super.executeBody(context);
            }
            LOGGER.info("DefPlugin.doExecute: Body content result: {}", value);
            
            // If no result from child processors, try XML node (text content)
            if (value == null || value instanceof EmptyVariable) {
                LOGGER.info("DefPlugin.doExecute: No child processors result, trying XML node");
                value = getBodyContentFromXmlNode(context);
                LOGGER.info("DefPlugin.doExecute: XML node result: {}", value);
            }

            // Set the variable in the context
            context.setLocalVar(varName, value);
            LOGGER.info("DefPlugin.doExecute: Set variable '{}' in context with value: {}", varName, value);

            LOGGER.info("DefPlugin.doExecute: Returning value: {}", value);
            return value;

        } catch (Exception e) {
            LOGGER.error("Error defining variable '" + varName + "': " + e.getMessage(), e);
            throw new RuntimeException("Error defining variable '" + varName + "': " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String varName = configuration.getProperty("var", "");
        if (varName.isEmpty()) {
            throw new PluginValidationException("Variable name 'var' is required");
        }
    }

    /**
     * Gets the body content directly from configuration (v2.2.x ULTRA-LEAN).
     * Uses _textContent property populated by StreamingXmlParser.
     * Applies template interpolation for ${variable} expressions.
     * 
     * @param context the execution context (needed for template interpolation)
     * @return the interpolated body content as Variable
     */
    private Variable getBodyContentFromXmlNode(DynamicScopeContext context) {
        // v2.2.x: Use _textContent property from PluginConfiguration
        String textContent = getConfiguration().getProperty("_textContent");
        if (textContent != null && !textContent.trim().isEmpty()) {
            // v2.2.x FIX: Apply template interpolation to resolve ${variable} expressions
            // Check if BaseTemplater is available (it might be null in unit tests)
            if (BaseTemplater.scriptEngineFactory != null) {
                LOGGER.debug("DefPlugin.getBodyContentFromXmlNode: Applying template interpolation to: {}", textContent);
                Variable interpolated = BaseTemplater.evaluateToVariable(textContent, 
                                                                         context.getScriptingLanguage(), 
                                                                         context);
                LOGGER.debug("DefPlugin.getBodyContentFromXmlNode: Interpolated result: {}", interpolated);
                return interpolated;
            } else {
                // Fallback for unit tests or when BaseTemplater is not initialized
                LOGGER.warn("DefPlugin.getBodyContentFromXmlNode: BaseTemplater not available, returning text content without interpolation");
                return new NodeVariable(textContent);
            }
        }
        
        return EmptyVariable.INSTANCE;
    }
}
