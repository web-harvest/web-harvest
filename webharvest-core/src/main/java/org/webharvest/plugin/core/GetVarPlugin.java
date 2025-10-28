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
 * INTERRUPTION) HOWEVER CAUSED AND ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package org.webharvest.plugin.core;

import java.text.MessageFormat;

import org.webharvest.exception.VariableException;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variable retrieval processor - retrieves variable value from execution context.
 * 
 * <p>
 * The {@code <get-var>} processor retrieves the value of a previously defined variable
 * and returns it for use in the current execution flow. It's commonly used to pass
 * data between different parts of a scraper configuration while preserving the original
 * Variable type (binary data, XML structure, lists).
 * </p>
 * 
 * <p>
 * <strong>Note:</strong> In most cases, you can use the simpler {@code ${varname}} syntax
 * for variable interpolation within text content. The {@code <get-var>} processor is
 * specifically needed when you want to pass the actual Variable object (preserving type)
 * to another processor without String conversion.
 * </p>
 * 
 * <h3>get-var vs Template ${} Syntax</h3>
 * <table border="1">
 *   <tr>
 *     <th>Syntax</th>
 *     <th>Use Case</th>
 *     <th>Result Type</th>
 *   </tr>
 *   <tr>
 *     <td>{@code ${varname}}</td>
 *     <td>Text interpolation in attributes/content</td>
 *     <td>String (calls toString())</td>
 *   </tr>
 *   <tr>
 *     <td>{@code <get-var var="varname"/>}</td>
 *     <td>Pass Variable object to processor</td>
 *     <td>Original Variable type (binary, XML, list)</td>
 *   </tr>
 * </table>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Simple text retrieval (both syntaxes work)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="apiUrl" value="https://api.example.com"/>
 * 
 * <!-- Template syntax (simpler) -->
 * <http url="${apiUrl}/users"/>
 * 
 * <!-- Processor syntax (explicit) -->
 * <http>
 *   <get-var var="apiUrl"/>/users
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Preserving binary data (use get-var!)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Download image as binary -->
 * <def var="imageData">
 *   <http url="https://example.com/logo.png"/>
 * </def>
 * 
 * <!-- CORRECT: get-var preserves byte[] array -->
 * <file path="logo.png" action="write" type="binary">
 *   <get-var var="imageData"/>
 * </file>
 * 
 * <!-- WRONG: ${imageData} converts to String, corrupts binary! -->
 * <file path="broken.png" action="write" type="binary">
 *   ${imageData}  <!-- ❌ Corrupted! -->
 * </file>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Preserving XML structure</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="xmlData">
 *   <json-to-xml>
 *     <http url="https://api.example.com/data.json"/>
 *   </json-to-xml>
 * </def>
 * 
 * <!-- get-var preserves XML structure for XPath -->
 * <def var="username">
 *   <xpath expression="//user/name/text()">
 *     <get-var var="xmlData"/>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Working with lists</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="productIds">
 *   <xpath expression="//product/@id">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <!-- Loop needs Variable object, not String -->
 * <loop item="id">
 *   <get-var var="productIds"/>
 *   
 *   <http url="https://example.com/product/${id}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Dynamic variable names with template interpolation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="lang" value="en"/>
 * <def var="greeting_en" value="Hello"/>
 * <def var="greeting_pl" value="Cześć"/>
 * <def var="greeting_de" value="Hallo"/>
 * 
 * <!-- Dynamic variable name -->
 * <get-var var="greeting_${lang}"/>
 * <!-- Result: Hello (retrieves greeting_en) -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Reusing expensive HTTP calls</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Fetch API data once -->
 * <def var="apiData">
 *   <http url="https://api.example.com/data?key=${apiKey}"/>
 * </def>
 * 
 * <!-- Reuse the same response multiple times -->
 * <def var="userId">
 *   <xpath expression="//user/@id">
 *     <json-to-xml><get-var var="apiData"/></json-to-xml>
 *   </xpath>
 * </def>
 * 
 * <def var="userName">
 *   <xpath expression="//user/name">
 *     <json-to-xml><get-var var="apiData"/></json-to-xml>
 *   </xpath>
 * </def>
 * 
 * <def var="userEmail">
 *   <xpath expression="//user/email">
 *     <json-to-xml><get-var var="apiData"/></json-to-xml>
 *   </xpath>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>When to Use</h3>
 * 
 * <p><strong>Use {@code <get-var>} when you need to:</strong></p>
 * <ul>
 *   <li>Pass binary data to {@code <file>} (images, PDFs, ZIPs)</li>
 *   <li>Preserve XML/JSON structure for XPath/XQuery processing</li>
 *   <li>Work with ListVariable in {@code <loop>} iterations</li>
 *   <li>Use dynamic variable names (e.g., {@code var="item_${index}"})</li>
 *   <li>Chain processors that expect Variable objects, not Strings</li>
 * </ul>
 * 
 * <p><strong>Use {@code ${variable}} when you need to:</strong></p>
 * <ul>
 *   <li>Interpolate text in URLs, paths, or content</li>
 *   <li>Build strings from variables</li>
 *   <li>Simple text replacement (toString() is acceptable)</li>
 * </ul>
 * 
 * <h3>Error Handling</h3>
 * <p>
 * If the requested variable doesn't exist in the current scope or any parent scope,
 * {@code <get-var>} throws a RuntimeException:
 * </p>
 * <pre>
 * Error retrieving variable 'productId': Variable 'productId' is not defined!
 * </pre>
 * <p>
 * This strict behavior helps catch typos and missing variable definitions early in
 * the execution. To handle optional variables, use {@code <if>}:
 * </p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <if condition="${empty(optionalVar)}">
 *   <!-- Variable doesn't exist, use default -->
 *   <def var="optionalVar" value="default"/>
 * </if>
 * 
 * <get-var var="optionalVar"/>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Type preservation:</strong> Preserves original Variable type (NodeVariable, ListVariable, etc.)</li>
 *   <li><strong>Variable scope:</strong> Searches current scope, then parent scopes up to global</li>
 *   <li><strong>Template support:</strong> The {@code var} attribute itself supports ${} interpolation</li>
 *   <li><strong>No fallback:</strong> Missing variables throw error (unlike ${} which may return empty)</li>
 *   <li><strong>Performance:</strong> O(1) variable lookup, safe to use extensively</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <def>} - Define or update variables</li>
 *   <li>{@code <var>} - Alternative variable definition syntax</li>
 *   <li>{@code ${varname}} - Template variable interpolation</li>
 *   <li>{@code <empty>} - Check if variable is empty</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "get",
    description = "Retrieves a variable value from the context",
    version = "2.1.0",
    category = "Variables"
)
public class GetVarPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.GetVarPlugin");

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "get"; }
            @Override
            public String getName() { return "get"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Retrieves a variable value from the context"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
            public String getCategory() { return "Variables"; }
            @Override
            public String[] getTags() { return new String[0]; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return GetVarPlugin.this.getConfigurationSchema(); }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public String getElementName() { return "get"; }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getClassName() { return GetVarPlugin.this.getClass().getName(); }
        };
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[] {
                    new AttributeDefinition() {
                        @Override
                        public String getName() { return "var"; }
                        @Override
                        public String getDescription() { return "Name of the variable to retrieve"; }
                        @Override
                        public boolean isRequired() { return true; }
                        @Override
                        public AttributeType getType() { return AttributeType.STRING; }
                        @Override
                        public String getPattern() { return null; }
                        @Override
                        public Number getMaxValue() { return null; }
                        @Override
                        public Number getMinValue() { return null; }
                        @Override
                        public String[] getAllowedValues() { return null; }
                        @Override
                        public String getDefaultValue() { return null; }
                    }
                };
            }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public boolean allowsBody() { return false; }
            @Override
            public String[] getRequiredSubElements() { return new String[0]; }
            @Override
            public String[] getAllowedSubElements() { return new String[0]; }
            @Override
            public String[] getOptionalAttributes() { return new String[0]; }
            @Override
            public String[] getRequiredAttributes() { return new String[]{"var"}; }
            @Override
            public AttributeDefinition getAttribute(String name) { 
                if ("var".equals(name)) {
                    return getAttributeDefinitions()[0];
                }
                return null;
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
        // No initialization needed for GetVarPlugin
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        // GetVarPlugin should return the variable value directly, not use BodyProcessor
        return doExecute(context);
    }

    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String varName = config.getProperty("var", "");
        
        LOGGER.debug("GetVarPlugin: doExecute called with varName='" + varName + "'");
        LOGGER.debug("GetVarPlugin: config=" + config);
        
        if (varName.isEmpty()) {
            throw new RuntimeException("Variable name is required for get operation");
        }
        
        // Evaluate the var name as a template if it contains template expressions
        if (varName.contains("${")) {
            Variable evaluatedVarName = BaseTemplater.evaluateToVariable(varName, null, context);
            if (evaluatedVarName != null) {
                varName = evaluatedVarName.toString();
                LOGGER.debug("GetVarPlugin: evaluated varName to: '" + varName + "'");
            }
        }

               try {
                   Variable variable = context.getVar(varName);
                   LOGGER.debug("GetVarPlugin: context.getVar('" + varName + "') returned: " + variable);
                   
                   if (variable == null) {
                       throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': Variable ''{0}'' is not defined!", varName));
                   }
                   
                   LOGGER.debug("Retrieved variable '{}' with value: {}", varName, variable);
                   LOGGER.debug("GetVarPlugin: returning variable: " + variable);
                   return variable;
                   
               } catch (Exception e) {
                   LOGGER.error("Error retrieving variable '{}': {}", varName, e.getMessage());
                   LOGGER.debug("GetVarPlugin: Error retrieving variable '" + varName + "': " + e.getMessage());
                   if (e instanceof RuntimeException) {
                       // Check if this is already our formatted error message
                       if (e.getMessage() != null && e.getMessage().startsWith("Error retrieving variable")) {
                           throw e; // Re-throw as-is
                       }
                       // Wrap RuntimeException with our error message format
                       throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': {1}", varName, e.getMessage()), e);
                   }
                   throw new RuntimeException(MessageFormat.format("Error retrieving variable ''{0}'': {1}", varName, e.getMessage()), e);
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
}
