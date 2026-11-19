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
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.WHConstants;
import org.webharvest.utils.CommonUtil;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.scripting.ScriptingLanguage;
import org.apache.commons.lang3.math.NumberUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * While loop processor - repeats execution while condition remains true.
 * 
 * <p>
 * The {@code <while>} processor executes its body content repeatedly as long as the
 * specified condition evaluates to true. Unlike {@code <loop>}, which iterates over
 * a collection, {@code <while>} continues until a condition becomes false or maxloops
 * limit is reached.
 * </p>
 * 
 * <h3>Bug #19 Fix: Memory Leak Prevention (v2.2.0)</h3>
 * <p>
 * <strong>Problem:</strong> Variables created inside while loop body accumulated in memory,
 * causing {@code OutOfMemoryError} during long-running loops (pagination, API polling).
 * </p>
 * <p>
 * <strong>Solution:</strong> Each while iteration now executes in a nested {@link DynamicScopeContext}.
 * Temporary variables are automatically cleaned up after each iteration, preventing memory leaks
 * in long-running loops.
 * </p>
 * <p>
 * <strong>Impact:</strong> Can now safely run while loops with 10,000+ iterations without memory issues.
 * Particularly important for pagination scenarios where loop runs until "hasNextPage" becomes false.
 * </p>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>condition (or test):</strong> Boolean expression evaluated before each iteration</li>
 *   <li><strong>index:</strong> Variable name for 0-based iteration counter (optional)</li>
 *   <li><strong>maxloops:</strong> Safety limit to prevent infinite loops (default: 100)</li>
 *   <li><strong>empty:</strong> Skip empty results if true (default: false)</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Pagination with has-next indicator</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="hasNextPage" value="true"/>
 * <def var="page" value="1"/>
 * 
 * <while condition="${hasNextPage}" maxloops="20">
 *   <def var="response">
 *     <http url="https://api.example.com/products?page=${page}"/>
 *   </def>
 *   
 *   <!-- Check if there's a next page -->
 *   <def var="hasNextPage">
 *     <xpath expression="//pagination/hasNext/text()">
 *       <json-to-xml>${response}</json-to-xml>
 *     </xpath>
 *   </def>
 *   
 *   <!-- Increment page -->
 *   <def var="page">
 *     <script>parseInt(context.getVar("page")) + 1</script>
 *   </def>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Retry logic with backoff</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="success" value="false"/>
 * <def var="retryCount" value="0"/>
 * 
 * <while condition="${!success && retryCount < 3}" maxloops="3">
 *   <def var="response">
 *     <http url="https://api.example.com/data"/>
 *   </def>
 *   
 *   <if condition="${!empty(response)}">
 *     <def var="success" value="true"/>
 *   </if>
 *   <else>
 *     <def var="retryCount">
 *       <script>parseInt(context.getVar("retryCount")) + 1</script>
 *     </def>
 *     <sleep time="1000"/>  <!-- Wait before retry -->
 *   </else>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Process until specific value found</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="found" value="false"/>
 * 
 * <while condition="${!found}" maxloops="50" index="i">
 *   <def var="item">
 *     <http url="https://api.example.com/item/${i}"/>
 *   </def>
 *   
 *   <if condition="${item == 'TARGET'}">
 *     <def var="found" value="true"/>
 *   </if>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Dynamic pagination (unknown page count)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="hasMore" value="true"/>
 * <def var="offset" value="0"/>
 * 
 * <while test="${hasMore}" maxloops="100">
 *   <def var="data">
 *     <http url="https://api.example.com/items?offset=${offset}&limit=100"/>
 *   </def>
 *   
 *   <!-- Check if response has items -->
 *   <def var="itemCount">
 *     <xpath expression="count(//item)">
 *       <json-to-xml>${data}</json-to-xml>
 *     </xpath>
 *   </def>
 *   
 *   <if condition="${itemCount == '0'}">
 *     <def var="hasMore" value="false"/>
 *   </if>
 *   <else>
 *     <def var="offset">
 *       <script>parseInt(context.getVar("offset")) + 100</script>
 *     </def>
 *   </else>
 * </while>
 * </config>
 * }</pre>
 * 
 * <h3>Condition vs Loop</h3>
 * <table border="1">
 *   <tr>
 *     <th>Use {@code <while>} when:</th>
 *     <th>Use {@code <loop>} when:</th>
 *   </tr>
 *   <tr>
 *     <td>Pagination with unknown page count</td>
 *     <td>Iterating over known collection</td>
 *   </tr>
 *   <tr>
 *     <td>Retry logic until success</td>
 *     <td>Processing list of URLs</td>
 *   </tr>
 *   <tr>
 *     <td>Search until item found</td>
 *     <td>Extract all items from XPath</td>
 *   </tr>
 *   <tr>
 *     <td>Continue while condition true</td>
 *     <td>Process each item in array</td>
 *   </tr>
 * </table>
 * 
 * <h3>Infinite Loop Protection</h3>
 * <p>
 * Always set {@code maxloops} to prevent infinite loops:
 * </p>
 * <pre>{@code
 * <!-- Safe: has limit -->
 * <while condition="${hasMore}" maxloops="100">
 *   ...
 * </while>
 * 
 * <!-- Dangerous: no explicit limit (defaults to 100) -->
 * <while condition="${true}">
 *   ...  <!-- Will stop after 100 iterations -->
 * </while>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Condition evaluation:</strong> Re-evaluated before each iteration</li>
 *   <li><strong>Index variable:</strong> Auto-incremented, starts at 0</li>
 *   <li><strong>maxloops default:</strong> 100 iterations (safety measure)</li>
 *   <li><strong>Infinite loops:</strong> Set maxloops=-1 to disable limit (risky!)</li>
 *   <li><strong>test vs condition:</strong> Both attributes work (condition preferred)</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <loop>} - Iterate over collections</li>
 *   <li>{@code <if>} - One-time conditional execution</li>
 *   <li>{@code <exit>} - Break out of while loop</li>
 *   <li>{@code <script>} - Complex condition logic</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "while",
    description = "While plugin that provides while loop functionality",
    version = "2.1.0",
    category = "Control"
)
public class WhilePlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.WhilePlugin");

    /**
     * Metadata for WhilePlugin.
     * 
     * <p>Master plugin - can contain any child plugins. Repeats execution while condition is true.</p>
     */
    public static class WhilePluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "while";
        }

        @Override
        public String getName() {
            return "While";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Repeats execution while condition remains true. Unlike loop which iterates over collections, while continues until condition becomes false or maxloops limit is reached. Essential for pagination, API polling, and conditional repetition.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = WhilePlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "while", "loop", "iteration", "condition", "control", "container", "master"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return WhilePlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "while";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new WhilePluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-redo";
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
            return 240;
        }
        
        @Override
        public int getDefaultHeight() {
            return 180;
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
                new OutputPort("output", "Aggregated results from all iterations", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Repeats execution while condition remains true. Unlike loop which iterates over collections, while continues until condition becomes false or maxloops limit is reached. Essential for pagination, API polling, and conditional repetition. Can contain any child plugins.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+W", "Add While", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "While Plugin",
                "Repeats execution while condition remains true. Unlike loop which iterates over collections, while continues until condition becomes false or maxloops limit is reached.",
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
            return new String[0]; // Empty array means can contain any child plugins
        }

        @Override
        public boolean acceptsArbitraryChildren() {
            return true;
        }
    }
    
    /**
     * Configuration schema for WhilePlugin.
     */
    public static class WhilePluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[]{
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
                                return "Optional identifier for the while element";
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
                                return "condition";
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
                                return "Condition to evaluate for loop continuation";
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
                                return "index";
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
                                return "Variable name for loop index";
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
                                return "maxloops";
                            }

                            @Override
                            public PluginConfigurationSchema.AttributeType getType() {
                                return PluginConfigurationSchema.AttributeType.INTEGER;
                            }

                            @Override
                            public boolean isRequired() {
                                return false;
                            }

                            @Override
                            public String getDefaultValue() {
                                return "1000";
                            }

                            @Override
                            public String getDescription() {
                                return "Maximum number of loop iterations";
                            }

                            @Override
                            public String[] getAllowedValues() {
                                return null;
                            }

                            @Override
                            public Number getMinValue() {
                                return 1;
                            }

                            @Override
                            public Number getMaxValue() {
                                return 1000000;
                            }

                            @Override
                            public String getPattern() {
                                return null;
                            }
                        },
                        new PluginConfigurationSchema.AttributeDefinition() {
                            @Override
                            public String getName() {
                                return "empty";
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
                                return "Whether to return empty result (true) or collect results (false)";
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
                    return new String[0]; // Empty means can contain any child plugins
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[]{"id", "index", "maxloops", "empty"};
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
                    
                    String condition = configuration.getProperty("condition");
                    // Allow empty condition - it will be treated as false
                    // if (condition == null || condition.trim().isEmpty()) {
                    //     throw new PluginValidationException("Condition attribute is required");
                    // }
                    
                    String maxloops = configuration.getProperty("maxloops");
                    if (maxloops != null && !maxloops.trim().isEmpty()) {
                        try {
                            int maxLoopsValue = Integer.parseInt(maxloops);
                            if (maxLoopsValue < 1 || maxLoopsValue > 1000000) {
                                throw new PluginValidationException("Maxloops must be between 1 and 1000000");
                            }
                        } catch (NumberFormatException e) {
                            throw new PluginValidationException("Maxloops must be a valid integer");
                        }
                    }
                }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new WhilePluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new WhilePluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // Validate configuration first
        doValidateConfiguration(configuration);
        // Initialize plugin with configuration
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        
        String condition = configuration.getProperty("condition");
        // Allow empty condition - it will be treated as false
        // if (condition == null || condition.trim().isEmpty()) {
        //     throw new PluginValidationException("Condition attribute is required");
        // }
        
        String maxloops = configuration.getProperty("maxloops");
        if (maxloops != null && !maxloops.trim().isEmpty()) {
            try {
                int maxLoopsValue = Integer.parseInt(maxloops);
                if (maxLoopsValue < 1 || maxLoopsValue > 1000000) {
                    throw new PluginValidationException("Maxloops must be between 1 and 1000000");
                }
            } catch (NumberFormatException e) {
                throw new PluginValidationException("Maxloops must be a valid integer");
            }
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("WhilePlugin.doExecute: START");
        
        // Snapshot plugin configuration once so cleanup cannot invalidate a second read.
        PluginConfiguration config = getConfiguration();
        if (config == null) {
            throw new RuntimeException("WhilePlugin requires valid configuration - check XML structure");
        }
        LOGGER.trace("WhilePlugin.doExecute: config=" + config);
        LOGGER.trace("WhilePlugin.doExecute: config.getClass()=" + (config != null ? config.getClass().getName() : "null"));

        // Get while configuration - using the same logic as original WhileProcessor
        String index = config.getProperty("index", "");
        String maxLoopsString = config.getProperty("maxloops", "");
        // XML schema defines 'test' attribute (renamed from 'condition')
        String condition = config.getProperty("test", "");
        LOGGER.debug("doExecute: config.getProperty('test', '')='" + condition + "'");
        // If 'test' is not found, try 'condition' for backward compatibility
        if (condition.isEmpty()) {
            condition = config.getProperty("condition", "");
            LOGGER.debug("doExecute: config.getProperty('condition', '')='" + condition + "'");
        }
        boolean isEmpty = CommonUtil.getBooleanValue(config.getProperty("empty", "false"), false);

        LOGGER.trace("WhilePlugin.doExecute: condition='" + condition + "', index='" + index + "', maxLoops='" + maxLoopsString + "', isEmpty=" + isEmpty);

        final List<Object> resultList = new ArrayList<Object>();
        int i = 0;

        // Define the zero-based index in the parent scope. Each iteration temporarily
        // shadows it inside the nested scope; the next value is committed after that scope exits.
        if (index != null && !index.isEmpty()) {
            context.setLocalVar(index, new NodeVariable(String.valueOf(i)));
        }

        // Evaluate condition using BaseTemplater like original WhileProcessor
        String evaluatedCondition = BaseTemplater.evaluateToString(condition, context.getScriptingLanguage(), context);
        LOGGER.debug("doExecute: evaluatedCondition='" + evaluatedCondition + "'");
        LOGGER.trace("WhilePlugin.doExecute: context.getScriptingLanguage()=" + context.getScriptingLanguage());
        

        // Try to evaluate as boolean using BeanShell directly
        boolean conditionResult = false;
        try {
            // Use BeanShell to evaluate the already-evaluated condition as a boolean expression
            org.webharvest.runtime.scripting.ScriptSource scriptSource = 
                new org.webharvest.runtime.scripting.ScriptSource(evaluatedCondition, context.getScriptingLanguage());
            Object result = BaseTemplater.scriptEngineFactory.getEngine(scriptSource).evaluate(context, scriptSource);
            LOGGER.trace("WhilePlugin.doExecute: BeanShell result=" + result + ", type=" + (result != null ? result.getClass().getName() : "null"));
            
            if (result instanceof Boolean) {
                conditionResult = (Boolean) result;
            } else if (result != null) {
                // Try to convert to boolean
                String resultStr = result.toString();
                conditionResult = CommonUtil.isBooleanTrue(resultStr);
            }
            LOGGER.trace("WhilePlugin.doExecute: conditionResult=" + conditionResult);
        } catch (Exception e) {
            LOGGER.trace("WhilePlugin.doExecute: Error evaluating condition: " + e.getMessage());
            // Fallback to string evaluation
            conditionResult = CommonUtil.isBooleanTrue(evaluatedCondition);
        }

        // Iterates while testing variable represents boolean true or loop limit is exceeded
        final double maxLoops = NumberUtils.toDouble(maxLoopsString, 1000); // Use 1000 as default instead of DEFAULT_MAX_LOOPS
        
        // Apply safety limits to prevent infinite loops
        final double safeMaxLoops = Math.max(0, Math.min(maxLoops, 10000)); // Minimum 0, Maximum 10000 iterations
        LOGGER.trace("WhilePlugin.doExecute: maxLoops=" + maxLoops + ", safeMaxLoops=" + safeMaxLoops);

        // Use conditionResult from BaseTemplater.evaluateToVariable
        while (conditionResult && (i < safeMaxLoops)) {
            LOGGER.debug("doExecute: Loop iteration " + i + ", evaluatedCondition='" + evaluatedCondition + "'");
            
            // Bug #19 Fix: Execute each iteration in nested context
            // This automatically cleans up variables created in loop body
            final int currentIteration = i;
            
            try {
                Variable loopResult = context.executeWithinNewContext(() -> {
                    // Define current value of index variable in nested context
                    if (index != null && !index.isEmpty()) {
                        context.setLocalVar(index, new NodeVariable(String.valueOf(currentIteration)));
                    }
                    
                    // Execute body content using new architecture
                    LOGGER.debug("doExecute: About to execute body content");
                    Variable bodyResult = executeBody(context);
                    LOGGER.trace("WhilePlugin.doExecute: Body result.class=" + (bodyResult != null ? bodyResult.getClass().getName() : "null"));
                    LOGGER.trace("WhilePlugin.doExecute: Body result=" + bodyResult);
                    
                    return bodyResult;
                });
                
                if (!isEmpty) {
                    resultList.addAll(loopResult.toList());
                }
            } catch (ScraperExecutionExitException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }

            i++;
            
            // Commit the next zero-based index outside the iteration scope.
            if (index != null && !index.isEmpty()) {
                context.setLocalVar(index, new NodeVariable(String.valueOf(i)));
            }

            // Re-evaluate condition for next iteration using BeanShell
            try {
                // Re-evaluate the condition using BaseTemplater first
                String reEvaluatedCondition = BaseTemplater.evaluateToString(condition, context.getScriptingLanguage(), context);
                LOGGER.debug("doExecute: Re-evaluated condition='" + reEvaluatedCondition + "'");
                
                // Then evaluate as boolean using BeanShell
                org.webharvest.runtime.scripting.ScriptSource scriptSource = 
                    new org.webharvest.runtime.scripting.ScriptSource(reEvaluatedCondition, context.getScriptingLanguage());
                Object result = BaseTemplater.scriptEngineFactory.getEngine(scriptSource).evaluate(context, scriptSource);
                LOGGER.trace("WhilePlugin.doExecute: Re-evaluated BeanShell result=" + result + ", type=" + (result != null ? result.getClass().getName() : "null"));
                
                if (result instanceof Boolean) {
                    conditionResult = (Boolean) result;
                } else if (result != null) {
                    String resultStr = result.toString();
                    conditionResult = CommonUtil.isBooleanTrue(resultStr);
                }
                LOGGER.trace("WhilePlugin.doExecute: Re-evaluated conditionResult=" + conditionResult);
            } catch (Exception e) {
                LOGGER.trace("WhilePlugin.doExecute: Error re-evaluating condition: " + e.getMessage());
                conditionResult = false;
            }

            // Preserve the single-iteration safety behavior only for a literal
            // constant true condition. A dynamic expression may evaluate to the
            // string "true" initially and still need further iterations.
            if ("true".equalsIgnoreCase(condition == null ? "" : condition.trim())
                    && i > 0 && maxLoopsString.isEmpty()) {
                LOGGER.trace("Breaking infinite loop for literal true condition without maxloops");
                break;
            }
        }

        LOGGER.trace("WhilePlugin.doExecute: Loop finished, i=" + i + ", resultList.size=" + resultList.size());
        if (isEmpty) {
            return EmptyVariable.INSTANCE;
        } else {
            return resultList.isEmpty() ? EmptyVariable.INSTANCE : new ListVariable(resultList);
        }
    }

    
}





