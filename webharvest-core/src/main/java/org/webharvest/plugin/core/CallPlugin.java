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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.exception.ScraperExecutionExitException;
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
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.IncludeExecutionException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.definition.PluginConfigurationTree;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.PluginBodyExecutor;
import org.webharvest.plugin.core.CallParamPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Function invocation processor - calls user-defined functions with parameters.
 * 
 * <p>
 * The {@code <call>} processor invokes functions defined with {@code <function>},
 * passing parameters and receiving return values. This enables code reuse, modular
 * configurations, and abstraction of complex logic into reusable components.
 * </p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Example 1: Simple function call</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Define function -->
 * <function name="fetchUser">
 *   <def var="userId"><call-param name="userId"/></def>
 *   
 *   <http url="https://api.example.com/user/${userId}"/>
 * </function>
 * 
 * <!-- Call function -->
 * <def var="userData">
 *   <call name="fetchUser">
 *     <call-param name="userId">123</call-param>
 *   </call>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h3>Example 2: Function with multiple parameters</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="buildUrl">
 *   <def var="base"><call-param name="baseUrl"/></def>
 *   <def var="path"><call-param name="path"/></def>
 *   <def var="query"><call-param name="query"/></def>
 *   
 *   <template>${base}${path}?${query}</template>
 * </function>
 * 
 * <call name="buildUrl">
 *   <call-param name="baseUrl">https://api.example.com</call-param>
 *   <call-param name="path">/users</call-param>
 *   <call-param name="query">limit=10&offset=0</call-param>
 * </call>
 * </config>
 * }</pre>
 * 
 * <h3>Example 3: Reusable scraping function</h3>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <function name="scrapePage">
 *   <def var="url"><call-param name="url"/></def>
 *   
 *   <def var="title">
 *     <xpath expression="//h1/text()">
 *       <html-to-xml>
 *         <http url="${url}"/>
 *       </html-to-xml>
 *     </xpath>
 *   </def>
 *   
 *   ${title}
 * </function>
 * 
 * <!-- Use function multiple times -->
 * <loop item="pageUrl">
 *   <tokenize>${urlList}</tokenize>
 *   
 *   <def var="pageTitle">
 *     <call name="scrapePage">
 *       <call-param name="url">${pageUrl}</call-param>
 *     </call>
 *   </def>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Function scope:</strong> Functions must be defined before calling</li>
 *   <li><strong>Parameters:</strong> Passed via {@code <call-param>} child elements</li>
 *   <li><strong>Return value:</strong> Function returns last expression result</li>
 *   <li><strong>Variable scope:</strong> Function parameters are local to function body</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <function>} - Define functions</li>
 *   <li>{@code <call-param>} - Pass parameters to functions</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "call",
    description = "Call plugin that executes function calls",
    version = "2.2.0"
)
public class CallPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.CallPlugin");

    /**
     * Metadata for CallPlugin.
     * 
     * <p>Master plugin - allows call-param as children. Executes user-defined functions with parameters.</p>
     */
    public static class CallPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "call";
        }

        @Override
        public String getName() {
            return "Call Function";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Executes a user-defined function by name, passing parameters via call-param children. Functions must be defined using the function element before they can be called. Returns the function's return value or the result of its last expression.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = CallPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "call", "function", "execution", "invocation", "master"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return CallPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "call";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new CallPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-play-circle";
        }
        
        @Override
        public String getColor() {
            return "#8B5CF6"; // Purple for functions
        }
        
        @Override
        public String getGroup() {
            return "Functions & Scripting";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 140;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("name", "Function name to call (from name attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Function return value", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Executes a user-defined function by name, passing parameters via call-param children. Functions must be defined using the function element before they can be called. Returns the function's return value.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+C", "Add Call Function", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Call Function Plugin",
                "Executes a user-defined function by name, passing parameters via call-param children.",
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
            return new String[]{"call-param"}; // Master plugin - allows call-param
        }
    }
    
    /**
     * Configuration schema for CallPlugin.
     */
    public static class CallPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "name";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.STRING;
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
                        return "Name of the function to call";
                    }

                    @Override
                    public String[] getAllowedValues() {
                        return null;
                    }

                    @Override
                    public String getPattern() {
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
                },
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "id";
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
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Optional identifier for the call element";
                    }

                    @Override
                    public String[] getAllowedValues() {
                        return null;
                    }

                    @Override
                    public String getPattern() {
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
            return new String[]{"call-param"}; // Master plugin - allows call-param
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[]{"name"};
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            
            String name = configuration.getProperty("name");
            if (name == null || name.trim().isEmpty()) {
                throw new PluginValidationException("Name attribute is required");
            }
        }
    }

    private Map<String, Variable> functionParams = new HashMap<String, Variable>();
    private Variable functionResult = new NodeVariable("");

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        
        LOGGER.info("CallPlugin initialized with function: {}", configuration.getProperty("name"));
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String name = configuration.getProperty("name");
        if (name == null || name.trim().isEmpty()) {
            throw new PluginValidationException("Name attribute is required");
        }
    }

    /**
     * Normalizes a return value to handle NodeVariable wrapping List and ListVariable.
     * Converts List representations like [item1, item2] to space-separated strings.
     */
    private Variable normalizeReturnValue(Variable value) {
        if (value == null || value instanceof EmptyVariable) {
            LOGGER.debug("normalizeReturnValue: value is null or EmptyVariable");
            return value;
        }
        
        // Handle ListVariable - extract last non-empty item
        if (value instanceof ListVariable) {
            ListVariable listResult = (ListVariable) value;
            List<Variable> items = listResult.toList();
            LOGGER.debug("normalizeReturnValue: ListVariable with {} items", items.size());
            // Find the last non-empty item
            for (int i = items.size() - 1; i >= 0; i--) {
                Variable item = items.get(i);
                if (item != null && !(item instanceof EmptyVariable) && !item.isEmpty()) {
                    LOGGER.debug("normalizeReturnValue: Found non-empty item at index {}: '{}'", i, item.toString());
                    return normalizeReturnValue(item); // Recursively normalize
                }
            }
            LOGGER.debug("normalizeReturnValue: No non-empty items in ListVariable");
            return EmptyVariable.INSTANCE;
        }
        
        // Handle NodeVariable wrapping List
        if (value instanceof NodeVariable) {
            NodeVariable nodeVar = (NodeVariable) value;
            Object wrapped = nodeVar.getWrappedObject();
            if (wrapped instanceof java.util.List) {
                java.util.List<?> list = (java.util.List<?>) wrapped;
                if (list.isEmpty()) {
                    return EmptyVariable.INSTANCE;
                } else if (list.size() == 1) {
                    Object singleItem = list.get(0);
                    // If single item is also a List, normalize it recursively
                    if (singleItem instanceof java.util.List) {
                        return normalizeReturnValue(new NodeVariable(singleItem));
                    }
                    return new NodeVariable(singleItem);
                } else {
                    // Join list items with space (remove brackets)
                    StringBuilder sb = new StringBuilder();
                    for (Object obj : list) {
                        if (sb.length() > 0) {
                            sb.append(' ');
                        }
                        // Remove brackets from string representation if present
                        // Handle nested brackets like [[item]] -> item
                        String str = obj != null ? obj.toString() : "";
                        // Remove all leading and trailing brackets
                        while (str.startsWith("[") && str.endsWith("]")) {
                            str = str.substring(1, str.length() - 1);
                        }
                        sb.append(str);
                    }
                    return new NodeVariable(sb.toString());
                }
            }
            // Also check if toString() contains brackets that need to be removed
            // This handles cases where BaseTemplater returns "Hello [Alice]" as a string
            // or "[Hello][World]" -> "Hello World"
            String strValue = nodeVar.toString();
            if (strValue != null && strValue.contains("[") && strValue.contains("]")) {
                // Try to clean up bracket patterns:
                // - "Hello [Alice]" -> "Hello Alice" (single bracket pair with space before)
                // - "[Hello][World]" -> "Hello World" (multiple bracket pairs)
                // - "Hello[World]" -> "Hello World" (bracket pair without space, add space)
                // Use regex to replace bracket patterns, preserving spaces
                String cleaned = strValue.replaceAll("\\s*\\[([^\\]]+)\\]\\s*", " $1 ");
                // Clean up multiple spaces
                cleaned = cleaned.replaceAll("\\s+", " ").trim();
                // If the entire string is wrapped in brackets, remove them
                if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                if (!cleaned.equals(strValue)) {
                    return new NodeVariable(cleaned);
                }
            }
        }
        
        // For other types, return as-is
        return value;
    }
    
    /**
     * Capitalize first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + (str.length() > 1 ? str.substring(1) : "");
    }
    
    /**
     * Find function configuration in the configuration tree.
     */
    private PluginConfiguration findFunctionConfiguration(PluginConfigurationTree tree, String functionName) {
        if (tree == null) {
            return null;
        }
        
        PluginConfiguration config = tree.getConfiguration();
        if (config != null && "function".equals(config.getElementName())) {
            String name = config.getProperty("name");
            if (functionName.equals(name)) {
                return config;
            }
        }
        
        // Search in children
        for (PluginConfigurationTree child : tree.getChildren()) {
            PluginConfiguration found = findFunctionConfiguration(child, functionName);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * Execute call-param elements and collect parameter values.
     */
    private Map<String, Variable> collectCallParams(PluginConfiguration callConfig, DynamicScopeContext context) 
            throws InterruptedException {
        Map<String, Variable> params = new HashMap<>();
        
        if (callConfig == null || !callConfig.hasChildren()) {
            return params;
        }
        
        for (PluginConfiguration child : callConfig.getChildren()) {
            if ("call-param".equals(child.getElementName())) {
                String paramName = child.getProperty("name");
                if (paramName != null && !paramName.isEmpty()) {
                    try {
                        // Create and execute CallParamPlugin to get parameter value
                        CallParamPlugin paramPlugin = new CallParamPlugin();
                        paramPlugin.initialize(child);
                        Variable paramValue = paramPlugin.execute(context);
                        params.put(paramName, paramValue);
                        LOGGER.debug("CallPlugin: Collected parameter '{}' = {} chars", 
                            paramName, paramValue != null ? paramValue.toString().length() : 0);
                    } catch (ScraperExecutionExitException e) {
                        throw e;
                    } catch (Exception e) {
                        IncludeExecutionException.rethrowIfIncludeFailure(e);
                        LOGGER.warn("CallPlugin: Failed to execute call-param '{}': {}", paramName, e.getMessage());
                        params.put(paramName, EmptyVariable.INSTANCE);
                    }
                }
            }
        }
        
        return params;
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.info("CallPlugin.doExecute: START");
        
        PluginConfiguration config = getConfiguration();
        
        // Get function name
        String functionName = BaseTemplater.evaluateToString(config.getProperty("name", ""), null, context);
        LOGGER.info("CallPlugin.doExecute: Function name: {}", functionName);
        
        if (functionName.isEmpty()) {
            LOGGER.error("CallPlugin.doExecute: No function name specified");
            return EmptyVariable.INSTANCE;
        }

        try {
            // Step 1: Find function configuration in the configuration tree
            PluginConfigurationTree configTree = context.getConfig() != null ? 
                context.getConfig().getConfigurationTree() : null;
            
            if (configTree == null) {
                LOGGER.error("CallPlugin: Configuration tree is null, cannot find function '{}'", functionName);
                return EmptyVariable.INSTANCE;
            }
            
            PluginConfiguration functionConfig = findFunctionConfiguration(configTree, functionName);
            if (functionConfig == null) {
                LOGGER.error("CallPlugin: Function '{}' not found in configuration tree", functionName);
                return EmptyVariable.INSTANCE;
            }
            
            LOGGER.debug("CallPlugin: Found function configuration for '{}'", functionName);
            
            // Step 2: Collect call-param values
            Map<String, Variable> callParams = collectCallParams(config, context);
            LOGGER.debug("CallPlugin: Collected {} parameters", callParams.size());
            
            // Step 3: Execute function body in a new scope with parameters
            // Clear functionResult before execution (in outer scope)
            context.setLocalVar("functionResult", EmptyVariable.INSTANCE);
            
            // Execute function body in new scope with parameters available
            Variable functionResult = context.executeWithinNewContext(() -> {
                // Set call-param values in the new scope
                // These will be accessible via CallParamPlugin inside the function
                // Use a valid variable name pattern: callParamName (starts with letter)
                LOGGER.info("CallPlugin: Setting {} parameters in function scope", callParams.size());
                for (Map.Entry<String, Variable> param : callParams.entrySet()) {
                    // Store parameters with a special prefix so CallParamPlugin can find them
                    // Variable name must start with letter, so we use "callParam" prefix
                    String paramVarName = "callParam" + capitalize(param.getKey());
                    context.setLocalVar(paramVarName, param.getValue());
                    int paramLength = param.getValue() != null ? param.getValue().toString().length() : 0;
                    LOGGER.info("CallPlugin: Set parameter '{}' = {} chars in function scope (as '{}')", 
                        new Object[]{param.getKey(), paramLength, paramVarName});
                }
                
                // Clear functionResult in the new scope
                // Use a flag to detect if ReturnPlugin was called
                // Variable name must start with letter (VariableName validation)
                String returnFlagName = "returnCalled" + capitalize(functionName);
                context.setLocalVar(returnFlagName, new NodeVariable("false"));
                context.setLocalVar("functionResult", EmptyVariable.INSTANCE);
                
                // Execute function body using PluginBodyExecutor
                PluginBodyExecutor executor = new DefaultPluginBodyExecutor();
                Variable bodyResult = executor.executeBody(functionConfig, context);
                LOGGER.info("CallPlugin: Function '{}' body executed, bodyResult type: {}, isEmpty: {}, toString: '{}'", 
                    new Object[]{
                        functionName, 
                        bodyResult != null ? bodyResult.getClass().getSimpleName() : "null",
                        bodyResult != null ? bodyResult.isEmpty() : "null",
                        bodyResult != null ? bodyResult.toString() : "null"
                    });
                
                // Check if ReturnPlugin was called (it sets the flag)
                Variable returnFlag = context.getVar(returnFlagName);
                Variable genericFlag = context.getVar("returnPluginCalled");
                Variable explicitReturn = context.getVar("functionResult");
                
                LOGGER.info("CallPlugin: Function '{}' - returnFlag: {}, genericFlag: {}, explicitReturn: {} (type: {})", 
                    new Object[]{
                        functionName,
                        returnFlag != null ? returnFlag.toString() : "null",
                        genericFlag != null ? genericFlag.toString() : "null",
                        explicitReturn != null ? explicitReturn.toString() : "null",
                        explicitReturn != null ? explicitReturn.getClass().getSimpleName() : "null"
                    });
                
                boolean returnWasCalled = (returnFlag != null && returnFlag.toString().equals("true")) ||
                                        (genericFlag != null && genericFlag.toString().equals("true"));
                
                if (returnWasCalled) {
                    // ReturnPlugin was called, use its value (even if EmptyVariable)
                    // Normalize the return value to handle NodeVariable wrapping List
                    Variable normalizedReturn = normalizeReturnValue(explicitReturn);
                    String returnValue = normalizedReturn != null ? normalizedReturn.toString() : "null";
                    LOGGER.info("CallPlugin: Function '{}' returned explicit value via <return> (may be empty): '{}'", 
                        functionName, returnValue);
                    return normalizedReturn != null ? normalizedReturn : EmptyVariable.INSTANCE;
                } else if (explicitReturn != null && !(explicitReturn instanceof EmptyVariable)) {
                    // Fallback: explicitReturn has value but flag wasn't set (shouldn't happen)
                    Variable normalizedReturn = normalizeReturnValue(explicitReturn);
                    LOGGER.info("CallPlugin: Function '{}' returned value (flag not set): '{}'", 
                        functionName, normalizedReturn != null ? normalizedReturn.toString() : "null");
                    return normalizedReturn != null ? normalizedReturn : explicitReturn;
                }
                
                // Otherwise, return the last expression result
                // Normalize bodyResult to handle ListVariable and NodeVariable wrapping List
                Variable normalizedBodyResult = normalizeReturnValue(bodyResult);
                LOGGER.info("CallPlugin: Function '{}' returned last expression result: '{}' (normalized from '{}')", 
                    new Object[]{
                        functionName,
                        normalizedBodyResult != null ? normalizedBodyResult.toString() : "null",
                        bodyResult != null ? bodyResult.toString() : "null"
                    });
                return normalizedBodyResult != null ? normalizedBodyResult : EmptyVariable.INSTANCE;
            });
            
            LOGGER.info("CallPlugin.doExecute: Function '{}' executed, result: {} chars", 
                functionName, functionResult != null ? functionResult.toString().length() : 0);
            
            // Normalize final result
            Variable normalizedResult = normalizeReturnValue(functionResult);
            return normalizedResult != null ? normalizedResult : EmptyVariable.INSTANCE;
            
        } catch (ScraperExecutionExitException e) {
            throw e;
        } catch (Exception e) {
            IncludeExecutionException.rethrowIfIncludeFailure(e);
            LOGGER.error("CallPlugin.doExecute: Error executing function call '{}': {}", 
                functionName, e.getMessage());
            LOGGER.error("CallPlugin.doExecute: Exception details", e);
            return EmptyVariable.INSTANCE;
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new CallPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new CallPluginMetadata().getConfigurationSchema();
    }

    /**
     * Sets the function result.
     * 
     * @param result the result to set
     */
    public void setFunctionResult(Variable result) {
        this.functionResult = result;
    }

    /**
     * Adds a function parameter.
     * 
     * @param name parameter name
     * @param value parameter value
     */
    public void addFunctionParam(String name, Variable value) {
        functionParams.put(name, value);
        LOGGER.debug("CallPlugin: Added function parameter '{}' = {}", name, value);
    }
}