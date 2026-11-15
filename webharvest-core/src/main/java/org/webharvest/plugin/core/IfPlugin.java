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
import org.webharvest.plugin.annotations.MasterDetailConstraint;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
// import org.webharvest.utils.CommonUtil;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conditional execution processor - execute body content only if condition is true.
 * 
 * <p>
 * The {@code <if>} processor evaluates a boolean condition and executes its body content
 * only when the condition is true. It's essential for implementing conditional logic,
 * error handling, data validation, and dynamic behavior in scrapers.
 * </p>
 * 
 * <h3>Condition Expressions</h3>
 * <p>The {@code condition} attribute accepts:</p>
 * <ul>
 *   <li><strong>Boolean literals:</strong> {@code "true"}, {@code "false"}</li>
 *   <li><strong>Comparisons:</strong> {@code "${count > 10}"}, {@code "${status == 'OK'}"}</li>
 *   <li><strong>Empty checks:</strong> {@code "${empty(varname)}"}</li>
 *   <li><strong>Script expressions:</strong> Any JavaScript expression returning boolean</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Basic conditional execution</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="httpStatus" value="200"/>
 * 
 * <if condition="${httpStatus == '200'}">
 *   <file path="success.log" action="append">
 *     Request succeeded at ${_date}
 *   </file>
 * </if>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Error handling</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="response">
 *   <http url="https://api.example.com/data"/>
 * </def>
 * 
 * <if condition="${empty(response)}">
 *   <file path="errors.log" action="append">
 *     Failed to fetch data from API
 *   </file>
 *   <exit/>
 * </if>
 * 
 * <!-- Continue with non-empty response -->
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: If-else pattern with {@code <else>}</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="stock" value="5"/>
 * 
 * <if condition="${stock > 0}">
 *   <def var="status" value="In Stock"/>
 * </if>
 * <else>
 *   <def var="status" value="Out of Stock"/>
 * </else>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Numeric comparison</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="product" index="i" maxloops="100">
 *   <xpath expression="//product">...</xpath>
 *   
 *   <def var="price">
 *     <xpath expression=".//price/text()">${product}</xpath>
 *   </def>
 *   
 *   <!-- Only save expensive products -->
 *   <if condition="${price > 100}">
 *     <file path="expensive-products.txt" action="append">
 *       Product ${i}: $${price}
 *     </file>
 *   </if>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: String matching</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="category" value="electronics"/>
 * 
 * <if condition="${category == 'electronics'}">
 *   <http url="https://example.com/electronics"/>
 * </if>
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Variable existence check</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <if condition="${empty(apiKey)}">
 *   <def var="apiKey" value="default-key"/>
 * </if>
 * 
 * <http url="https://api.example.com?key=${apiKey}"/>
 * </config>
 * }</pre>
 * 
 * <h3>Condition Evaluation</h3>
 * <p>Conditions are evaluated using the configured scripting language (JavaScript by default):</p>
 * <ul>
 *   <li><strong>Truthy values:</strong> {@code "true"}, {@code "1"}, non-empty strings</li>
 *   <li><strong>Falsy values:</strong> {@code "false"}, {@code "0"}, {@code ""}, {@code "null"}</li>
 *   <li><strong>Operators:</strong> {@code ==, !=, <, >, <=, >=, &&, ||, !}</li>
 * </ul>
 * 
 * <h3>Common Patterns</h3>
 * <pre>{@code
 * <!-- Check if variable exists -->
 * <if condition="${!empty(varname)}">...</if>
 * 
 * <!-- Numeric comparison -->
 * <if condition="${count >= 10}">...</if>
 * 
 * <!-- String equality -->
 * <if condition="${status == 'success'}">...</if>
 * 
 * <!-- Complex boolean logic -->
 * <if condition="${(count > 0) && (status == 'OK')}">...</if>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Required attribute:</strong> {@code condition} is mandatory</li>
 *   <li><strong>Body execution:</strong> Body is only executed if condition is true</li>
 *   <li><strong>No return value:</strong> {@code <if>} doesn't return a value (use {@code <def>} inside)</li>
 *   <li><strong>Else support:</strong> Use {@code <else>} processor for alternative branch</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <else>} - Alternative execution path</li>
 *   <li>{@code <while>} - Conditional looping</li>
 *   <li>{@code <empty>} - Check for empty values</li>
 *   <li>{@code <script>} - Complex conditional logic</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "if",
    description = "If plugin that provides conditional processing",
    version = "2.1.0",
    category = "Control"
)
@MasterDetailConstraint(
    detailType = "else",
    maxOccurs = 1,
    minOccurs = 0,
    sectionTitle = "Else Branch",
    addLabel = "Add Else",
    emptyLabel = "No else branch",
    usesBody = true
)
@org.webharvest.plugin.annotations.UiField(
    attribute = "condition",
    kind = org.webharvest.plugin.annotations.UiFieldKind.EXPRESSION,
    group = "Condition",
    order = 1,
    helperText = "Boolean expression controlling the if branch",
    placeholder = "${flag} == true",
    expressionCapable = true,
    format = "template"
)
@org.webharvest.plugin.annotations.ExpressionCapable(attribute = "condition", context = "template")
public class IfPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.IfPlugin");

    /**
     * Metadata for IfPlugin.
     * 
     * <p>Master plugin - allows else as child. Conditional execution based on boolean condition.</p>
     */
    public static class IfPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "if";
        }

        @Override
        public String getName() {
            return "If";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes body content only if condition is true. Essential for conditional logic, error handling, and data validation. Supports boolean expressions, comparisons, and empty checks.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = IfPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "if", "conditional", "control", "master"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getElementName() {
            return "if";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String getClassName() {
            return IfPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new IfPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-question-circle";
        }
        
        @Override
        public String getColor() {
            return "#10B981"; // Green for control flow
        }
        
        @Override
        public String getGroup() {
            return "Control Flow";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 160;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("condition", "Boolean condition to evaluate (from condition attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Result from body execution (if condition is true)", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes body content only if condition is true. Essential for conditional logic, error handling, and data validation. Supports boolean expressions, comparisons, and empty checks. Allows else as child.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+I", "Add If", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "If Plugin",
                "Executes body content only if condition is true. Essential for conditional logic, error handling, and data validation.",
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
            return new String[]{"else"}; // Master plugin - allows else child
        }

        @Override
        public boolean acceptsArbitraryChildren() {
            return true;
        }
    }
    
    /**
     * Configuration schema for IfPlugin.
     */
    public static class IfPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public AttributeDefinition[] getAttributeDefinitions() {
            return new AttributeDefinition[]{
                new AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "condition";
                    }

                    @Override
                    public AttributeType getType() {
                        return AttributeType.STRING;
                    }

                    @Override
                    public boolean isRequired() {
                        return true;
                    }

                    @Override
                    public String getDefaultValue() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Condition expression to evaluate";
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
                        return "Optional identifier for the if element";
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
            return new String[]{"else"}; // Allows else child
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[]{"condition"};
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            // Additional validation can be added here if needed
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new IfPluginMetadata();
    }
    
    public PluginConfigurationSchema getConfigurationSchema() {
        return new IfPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        // Additional validation can be added here if needed
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("IfPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();

        // Get condition from configuration - XML schema defines 'condition' attribute
        String condition = config.getProperty("condition", "");
        
        LOGGER.debug("If plugin - Condition: '" + condition + "'");
        LOGGER.debug("If plugin - Config properties: " + config.getProperties());
        LOGGER.debug("If plugin - Config toString: " + config);

        try {
            // Evaluate condition (simplified - in real implementation this would be more complex)
            boolean conditionResult = evaluateCondition(condition, context);
            LOGGER.debug("IfPlugin.doExecute: conditionResult=" + conditionResult);
            
            if (conditionResult) {
                // Execute body content when condition is true
                Variable bodyResult = executeBody(context);
                LOGGER.debug("IfPlugin.doExecute: Condition true, executing body content");
                return bodyResult;
            } else {
                // Return EmptyVariable for false conditions
                LOGGER.debug("IfPlugin.doExecute: Condition false, returning EmptyVariable");
                return EmptyVariable.INSTANCE;
            }

        } catch (Exception e) {
            LOGGER.error("Error during if condition evaluation", e);
            return EmptyVariable.INSTANCE;
        }
    }
    
    private boolean evaluateCondition(String condition, DynamicScopeContext context) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }

        try {
            // Older callers can execute plugins with a bare ScraperContext that has
            // no scripting language configured. Preserve the historical variable /
            // comparison contract in that case instead of asking BaseTemplater to
            // return the unevaluated template as plain text.
            if (context.getScriptingLanguage() == null) {
                return evaluateWithoutConfiguredLanguage(condition, context);
            }

            // Use the same configured expression engine as templates and scripts.
            Variable evaluated = BaseTemplater.evaluateToVariable(
                    condition.trim(), context.getScriptingLanguage(), context);
            return isTruthyConditionValue(evaluated);
        } catch (RuntimeException e) {
            LOGGER.error("Error evaluating If condition: {}", condition, e);
            return false;
        }
    }

    /**
     * Preserves the legacy If behavior for direct plugin callers that have not
     * configured a scripting language on their context.
     */
    private boolean evaluateWithoutConfiguredLanguage(
            String condition, DynamicScopeContext context) {
        String expression = condition.trim();
        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1).trim();
        }

        for (String operator : new String[] {"==", "!="}) {
            int operatorIndex = expression.indexOf(operator);
            if (operatorIndex >= 0) {
                String leftName = expression.substring(0, operatorIndex).trim();
                String rightValue = unquote(expression.substring(operatorIndex + operator.length()).trim());
                Variable leftVariable = context.getVar(leftName);
                String leftValue = leftVariable == null ? "" : leftVariable.toString().trim();
                boolean equal = leftValue.equals(rightValue);
                return "==".equals(operator) ? equal : !equal;
            }
        }

        Variable variable = context.getVar(expression);
        if (variable != null) {
            return isTruthyConditionValue(variable);
        }
        return isTruthyConditionValue(new NodeVariable(expression));
    }

    private String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * Applies the documented If truthiness rules without changing the shared
     * Variable conversion contract used by other plugins.
     */
    private boolean isTruthyConditionValue(Variable value) {
        if (value == null || value.getWrappedObject() == null) {
            return false;
        }

        Object wrapped = value.getWrappedObject();
        if (wrapped instanceof Boolean) {
            return (Boolean) wrapped;
        }
        if (wrapped instanceof Number) {
            return ((Number) wrapped).doubleValue() != 0.0d;
        }

        String text = value.toString().trim();
        if (text.isEmpty() || "false".equalsIgnoreCase(text) || "null".equalsIgnoreCase(text)) {
            return false;
        }
        try {
            return Double.parseDouble(text) != 0.0d;
        } catch (NumberFormatException ignored) {
            // Per the If contract, every non-empty non-falsy string is truthy.
            return true;
        }
    }

}
