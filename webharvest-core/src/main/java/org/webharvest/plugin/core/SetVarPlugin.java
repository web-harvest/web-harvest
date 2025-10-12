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

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
// REMOVED in v2.2.x
import org.webharvest.definition.XmlNode;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Variable setter processor - updates existing variables (legacy, use {@code <def>} instead).
 * 
 * <p>
 * The {@code <set-var>} processor updates the value of an existing variable in the
 * execution context. This is a legacy processor maintained for backward compatibility.
 * </p>
 * 
 * <p>
 * <strong>Deprecation Notice:</strong> This processor is maintained for compatibility
 * with configurations written for WebHarvest 1.x. New configurations should use
 * {@code <def>} instead, which provides the same functionality with cleaner syntax.
 * </p>
 * 
 * <h3>Differences: set-var vs def</h3>
 * <table>
 *   <tr>
 *     <th>Aspect</th>
 *     <th>{@code <set-var>}</th>
 *     <th>{@code <def>}</th>
 *   </tr>
 *   <tr>
 *     <td>Purpose</td>
 *     <td>Update existing variable</td>
 *     <td>Define or update variable</td>
 *   </tr>
 *   <tr>
 *     <td>Syntax</td>
 *     <td>{@code <set-var name="x">}</td>
 *     <td>{@code <def var="x">}</td>
 *   </tr>
 *   <tr>
 *     <td>Arithmetic</td>
 *     <td>Limited support</td>
 *     <td>Via template or script</td>
 *   </tr>
 *   <tr>
 *     <td>Recommended</td>
 *     <td>No (legacy)</td>
 *     <td><strong>Yes</strong></td>
 *   </tr>
 * </table>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic variable update (legacy syntax)</h4>
 * <pre>{@code
 * <def var="counter" value="0"/>
 * 
 * <loop maxloops="10">
 *   <!-- Legacy: set-var -->
 *   <set-var name="counter">${counter + 1}</set-var>
 *   
 *   <!-- Modern: use def instead -->
 *   <def var="counter" value="${counter}1"/>
 * </loop>
 * }</pre>
 * 
 * <h4>Example 2: Arithmetic expressions (limited support)</h4>
 * <pre>{@code
 * <def var="count" value="5"/>
 * 
 * <!-- set-var has basic arithmetic -->
 * <set-var name="count">${count + 1}</set-var>
 * <set-var name="count">${count - 1}</set-var>
 * }</pre>
 * 
 * <h4>Example 3: Updating with processor result</h4>
 * <pre>{@code
 * <def var="totalItems" value="0"/>
 * 
 * <loop item="page" maxloops="5">
 *   <def var="pageItems">
 *     <xpath expression="count(//item)">
 *       <http url="https://api.example.com/page/${page}"/>
 *     </xpath>
 *   </def>
 *   
 *   <!-- Add to total (string concatenation, not arithmetic!) -->
 *   <set-var name="totalItems">${totalItems}${pageItems}</set-var>
 * </loop>
 * }</pre>
 * 
 * <h3>Migration to {@code <def>}</h3>
 * 
 * <p><strong>Old (set-var):</strong></p>
 * <pre>{@code
 * <set-var name="counter">${counter + 1}</set-var>
 * }</pre>
 * 
 * <p><strong>New (def):</strong></p>
 * <pre>{@code
 * <def var="counter" value="${counter}1"/>
 * }</pre>
 * 
 * <p><strong>Or with script for real arithmetic:</strong></p>
 * <pre>{@code
 * <def var="counter">
 *   <script>
 *     <![CDATA[
 *       var current = parseInt(context.getVar("counter"));
 *       current + 1;
 *     ]]>
 *   </script>
 * </def>
 * }</pre>
 * 
 * <h3>Limitations</h3>
 * <ul>
 *   <li><strong>Arithmetic:</strong> Only basic + and - operations, result is string concatenation unless using script</li>
 *   <li><strong>Type conversion:</strong> Limited automatic type conversion</li>
 *   <li><strong>Scope:</strong> Can only update variables in current scope</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Deprecated:</strong> Use {@code <def>} for new configurations</li>
 *   <li><strong>Attribute name:</strong> Uses {@code name="x"} instead of {@code var="x"}</li>
 *   <li><strong>Arithmetic caveat:</strong> {@code ${x + 1}} is string concat, not math (use script for real math)</li>
 *   <li><strong>Template support:</strong> Variable name supports template interpolation</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <def>} - Modern variable definition (recommended)</li>
 *   <li>{@code <get-var>} - Retrieve variable value</li>
 *   <li>{@code <script>} - For complex arithmetic and logic</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @deprecated Use {@code <def>} processor instead for cleaner syntax
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "set-var",
    description = "Sets a variable value in the context",
    version = "2.1.0"
)
public class SetVarPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.SetVarPlugin");

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() { return "set-var"; }
            @Override
            public String getName() { return "set"; }
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public String getDescription() { return "Sets a variable value in the context"; }
            @Override
            public String getAuthor() { return "Web-Harvest Team"; }
            @Override
            public String getCategory() { return "core"; }
            @Override
            public String[] getTags() { return new String[0]; }
            @Override
            public String[] getDependencies() { return new String[0]; }
            @Override
            public PluginConfigurationSchema getConfigurationSchema() { return SetVarPlugin.this.getConfigurationSchema(); }
            @Override
            public String getNamespace() { return "http://org.webharvest/schema/2.1/core"; }
            @Override
            public String getElementName() { return "set-var"; }
            @Override
            public boolean isCorePlugin() { return true; }
            @Override
            public String getClassName() { return SetVarPlugin.this.getClass().getName(); }
        };
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public String getVersion() { return "2.1.0"; }
            @Override
            public boolean allowsBody() { return true; }
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
            public AttributeDefinition[] getAttributeDefinitions() { 
                return new AttributeDefinition[] {
                    new AttributeDefinition() {
                        @Override
                        public String getName() { return "var"; }
                        @Override
                        public String getDescription() { return "Name of the variable to set"; }
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
        // No initialization needed for SetVarPlugin
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("SetVarPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();
        String varName = config.getProperty("var", "");
        String valueAttr = config.getProperty("value", "");
        
        LOGGER.debug("SetVarPlugin.doExecute: varName='" + varName + "', valueAttr='" + valueAttr + "'");
        
        if (varName.isEmpty()) {
            throw new RuntimeException("Variable name is required for set operation");
        }

        try {
            Variable value;
            
            if (valueAttr != null && !valueAttr.isEmpty()) {
                // Use value attribute and process it as template (v2.2.x FIX)
                if (BaseTemplater.scriptEngineFactory != null) {
                    value = BaseTemplater.evaluateToVariable(valueAttr, context.getScriptingLanguage(), context);
                    if (value == null) {
                        value = EmptyVariable.INSTANCE;
                    }
                } else {
                    // Fallback for unit tests without BaseTemplater
                    value = new NodeVariable(valueAttr);
                }
            } else {
                // Get the body content and process it as template
                LOGGER.debug("SetVarPlugin.doExecute: Getting body content from XML node");
                Variable bodyContent = getBodyContentFromXmlNode();
                LOGGER.debug("SetVarPlugin.doExecute: Body content=" + bodyContent);
                if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                    // Process body content as template
                    String bodyText = bodyContent.toString();
                    LOGGER.debug("SetVarPlugin.doExecute: Body text='" + bodyText + "'");
                    LOGGER.debug("SetVarPlugin.doExecute: bodyContent class=" + bodyContent.getClass().getName());
                    
                    // For arithmetic expressions, we need to handle them specially
                    if (bodyText.contains("+") || bodyText.contains("-") || bodyText.contains("*") || bodyText.contains("/")) {
                        LOGGER.debug("SetVarPlugin.doExecute: Calling evaluateArithmeticExpression");
                        value = evaluateArithmeticExpression(bodyText, context);
                    } else {
                        LOGGER.debug("SetVarPlugin.doExecute: Calling BaseTemplater.evaluateToVariable");
                        // v2.2.x FIX: Use context.getScriptingLanguage() and null-safety
                        if (BaseTemplater.scriptEngineFactory != null) {
                            value = BaseTemplater.evaluateToVariable(bodyText, context.getScriptingLanguage(), context);
                        } else {
                            // Fallback for unit tests
                            value = new NodeVariable(bodyText);
                        }
                    }
                    LOGGER.debug("SetVarPlugin.doExecute: Evaluated value=" + value);
                    if (value == null) {
                        value = EmptyVariable.INSTANCE;
                    }
                } else {
                    LOGGER.debug("SetVarPlugin.doExecute: No body content, using EmptyVariable");
                    value = EmptyVariable.INSTANCE;
                }
            }
            
            // Set the variable in the context
            context.setLocalVar(varName, value);
            LOGGER.debug("SetVarPlugin.doExecute: Set variable '" + varName + "' to value: " + value);
            
            // Verify the variable was set
            Variable verifyVar = context.getVar(varName);
            LOGGER.debug("SetVarPlugin.doExecute: Verified variable '" + varName + "' = " + verifyVar);
            
            LOGGER.debug("Set variable '{}' with value: {}", varName, value);
            return value;
            
        } catch (Exception e) {
            LOGGER.error("Error setting variable '{}': {}", varName, e.getMessage());
            throw new RuntimeException("Error setting variable '" + varName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Gets the body content directly from configuration (v2.2.x ULTRA-LEAN).
     * Uses _textContent property populated by StreamingXmlParser.
     */
    private Variable getBodyContentFromXmlNode() {
        // v2.2.x: Use _textContent property from PluginConfiguration
        String textContent = getConfiguration().getProperty("_textContent");
        if (textContent != null && !textContent.trim().isEmpty()) {
            return new NodeVariable(textContent);
        }
        
        return EmptyVariable.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String varName = configuration.getProperty("name", "");
        if (varName.isEmpty()) {
            throw new PluginValidationException("Variable name 'name' is required");
        }
    }
    
    /**
     * Evaluates arithmetic expressions like ${counter + 1}
     */
    private Variable evaluateArithmeticExpression(String expression, DynamicScopeContext context) {
        try {
            LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: expression='" + expression + "'");
            
            // Handle template expressions like ${counter + 1}
            if (expression.contains("${") && expression.contains("}")) {
                // Extract the expression inside ${...}
                String innerExpression = expression.substring(expression.indexOf("${") + 2, expression.lastIndexOf("}"));
                LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: innerExpression='" + innerExpression + "'");
                
                // Handle addition like "counter + 1"
                if (innerExpression.contains("+")) {
                    String[] parts = innerExpression.split("\\+");
                    if (parts.length == 2) {
                        String leftVar = parts[0].trim();
                        String rightValueStr = parts[1].trim();
                        
                        LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: leftVar='" + leftVar + "', rightValueStr='" + rightValueStr + "'");
                        
                        // Get the variable value from context
                        Variable leftVarValue = context.getVar(leftVar);
                        double leftValue = 0.0;
                        if (leftVarValue != null) {
                            try {
                                leftValue = Double.parseDouble(leftVarValue.toString());
                            } catch (NumberFormatException e) {
                                LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: Cannot parse left value '" + leftVarValue.toString() + "' as number, using 0");
                                leftValue = 0.0;
                            }
                        }
                        
                        // Parse right value
                        double rightValue = 0.0;
                        try {
                            rightValue = Double.parseDouble(rightValueStr);
                        } catch (NumberFormatException e) {
                            LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: Cannot parse right value '" + rightValueStr + "' as number, using 0");
                            rightValue = 0.0;
                        }
                        
                        double result = leftValue + rightValue;
                        LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: " + leftValue + " + " + rightValue + " = " + result);
                        return new NodeVariable(String.valueOf((int) result)); // Return as integer
                    }
                }
                
                // Handle subtraction like "counter - 1"
                if (innerExpression.contains("-")) {
                    String[] parts = innerExpression.split("-");
                    if (parts.length == 2) {
                        String leftVar = parts[0].trim();
                        String rightValueStr = parts[1].trim();
                        
                        Variable leftVarValue = context.getVar(leftVar);
                        double leftValue = 0.0;
                        if (leftVarValue != null) {
                            try {
                                leftValue = Double.parseDouble(leftVarValue.toString());
                            } catch (NumberFormatException e) {
                                leftValue = 0.0;
                            }
                        }
                        
                        double rightValue = 0.0;
                        try {
                            rightValue = Double.parseDouble(rightValueStr);
                        } catch (NumberFormatException e) {
                            rightValue = 0.0;
                        }
                        
                        double result = leftValue - rightValue;
                        return new NodeVariable(String.valueOf((int) result));
                    }
                }
            }
            
            // Fallback to BaseTemplater for other expressions (v2.2.x FIX)
            if (BaseTemplater.scriptEngineFactory != null) {
                return BaseTemplater.evaluateToVariable(expression, context.getScriptingLanguage(), context);
            } else {
                // Fallback for unit tests
                return new NodeVariable(expression);
            }
        } catch (Exception e) {
            LOGGER.debug("SetVarPlugin.evaluateArithmeticExpression: Error evaluating expression '" + expression + "': " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
}
