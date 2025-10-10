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
 * Plugin that sets a variable value in the context.
 * This is equivalent to the old SetVarPlugin but using the new plugin architecture.
 *
 * @author Web-Harvest Team
 * @since 2.1.0
 * @version %I%, %G%
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
        System.out.println("SetVarPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();
        String varName = config.getProperty("var", "");
        String valueAttr = config.getProperty("value", "");
        
        System.out.println("SetVarPlugin.doExecute: varName='" + varName + "', valueAttr='" + valueAttr + "'");
        
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
                System.out.println("SetVarPlugin.doExecute: Getting body content from XML node");
                Variable bodyContent = getBodyContentFromXmlNode();
                System.out.println("SetVarPlugin.doExecute: Body content=" + bodyContent);
                if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
                    // Process body content as template
                    String bodyText = bodyContent.toString();
                    System.out.println("SetVarPlugin.doExecute: Body text='" + bodyText + "'");
                    System.out.println("SetVarPlugin.doExecute: bodyContent class=" + bodyContent.getClass().getName());
                    
                    // For arithmetic expressions, we need to handle them specially
                    if (bodyText.contains("+") || bodyText.contains("-") || bodyText.contains("*") || bodyText.contains("/")) {
                        System.out.println("SetVarPlugin.doExecute: Calling evaluateArithmeticExpression");
                        value = evaluateArithmeticExpression(bodyText, context);
                    } else {
                        System.out.println("SetVarPlugin.doExecute: Calling BaseTemplater.evaluateToVariable");
                        // v2.2.x FIX: Use context.getScriptingLanguage() and null-safety
                        if (BaseTemplater.scriptEngineFactory != null) {
                            value = BaseTemplater.evaluateToVariable(bodyText, context.getScriptingLanguage(), context);
                        } else {
                            // Fallback for unit tests
                            value = new NodeVariable(bodyText);
                        }
                    }
                    System.out.println("SetVarPlugin.doExecute: Evaluated value=" + value);
                    if (value == null) {
                        value = EmptyVariable.INSTANCE;
                    }
                } else {
                    System.out.println("SetVarPlugin.doExecute: No body content, using EmptyVariable");
                    value = EmptyVariable.INSTANCE;
                }
            }
            
            // Set the variable in the context
            context.setLocalVar(varName, value);
            System.out.println("SetVarPlugin.doExecute: Set variable '" + varName + "' to value: " + value);
            
            // Verify the variable was set
            Variable verifyVar = context.getVar(varName);
            System.out.println("SetVarPlugin.doExecute: Verified variable '" + varName + "' = " + verifyVar);
            
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
            System.out.println("SetVarPlugin.evaluateArithmeticExpression: expression='" + expression + "'");
            
            // Handle template expressions like ${counter + 1}
            if (expression.contains("${") && expression.contains("}")) {
                // Extract the expression inside ${...}
                String innerExpression = expression.substring(expression.indexOf("${") + 2, expression.lastIndexOf("}"));
                System.out.println("SetVarPlugin.evaluateArithmeticExpression: innerExpression='" + innerExpression + "'");
                
                // Handle addition like "counter + 1"
                if (innerExpression.contains("+")) {
                    String[] parts = innerExpression.split("\\+");
                    if (parts.length == 2) {
                        String leftVar = parts[0].trim();
                        String rightValueStr = parts[1].trim();
                        
                        System.out.println("SetVarPlugin.evaluateArithmeticExpression: leftVar='" + leftVar + "', rightValueStr='" + rightValueStr + "'");
                        
                        // Get the variable value from context
                        Variable leftVarValue = context.getVar(leftVar);
                        double leftValue = 0.0;
                        if (leftVarValue != null) {
                            try {
                                leftValue = Double.parseDouble(leftVarValue.toString());
                            } catch (NumberFormatException e) {
                                System.out.println("SetVarPlugin.evaluateArithmeticExpression: Cannot parse left value '" + leftVarValue.toString() + "' as number, using 0");
                                leftValue = 0.0;
                            }
                        }
                        
                        // Parse right value
                        double rightValue = 0.0;
                        try {
                            rightValue = Double.parseDouble(rightValueStr);
                        } catch (NumberFormatException e) {
                            System.out.println("SetVarPlugin.evaluateArithmeticExpression: Cannot parse right value '" + rightValueStr + "' as number, using 0");
                            rightValue = 0.0;
                        }
                        
                        double result = leftValue + rightValue;
                        System.out.println("SetVarPlugin.evaluateArithmeticExpression: " + leftValue + " + " + rightValue + " = " + result);
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
            System.out.println("SetVarPlugin.evaluateArithmeticExpression: Error evaluating expression '" + expression + "': " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
}
