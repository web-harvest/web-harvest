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

 You can contact Vladimir Nikic by sending e-mail to
 nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
 subject line.
*/
package org.webharvest.plugin.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.DefaultPluginBodyExecutor;
import org.webharvest.plugin.DefaultPluginConfiguration;
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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Loop iterator processor - repeats execution for each item in a collection.
 * 
 * <p>
 * The {@code <loop>} processor is fundamental for iterating over collections of data,
 * such as product lists, search results, paginated APIs, or any repeating data structure.
 * It executes its body content once for each item, with optional index tracking and empty-item handling.
 * </p>
 * 
 * <h3>Loop Input Sources</h3>
 * <p>Input is normalized once before iteration. Supported forms are:</p>
 * <ul>
 *   <li><strong>items attribute:</strong> A delimited string, or an exact ${variable} reference.
 *       A ListVariable is iterated element-by-element with its Variable types preserved;
 *       a scalar variable is split using the configured delimiter.</li>
 *   <li><strong>Leading list/xpath child:</strong> The first direct {@code <list>} or {@code <xpath>}
 *       child is evaluated once and its result becomes the input collection.</li>
 *   <li><strong>Inline text:</strong> Direct text content is split by newline by default.
 *       An exact ${variable} reference is resolved before splitting.</li>
 *   <li><strong>No source:</strong> The loop retains count-only behavior and runs up to maxloops
 *       with an empty item value.</li>
 * </ul>
 * <p>An optional {@code <body>} wrapper is unwrapped. Without it, the remaining direct child
 * processors form the body. If a source is present without body processors, the source items
 * themselves are returned in order.</p>
 *
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>items:</strong> Optional literal string or exact variable reference.</li>
 *   <li><strong>item:</strong> Variable name for current item in each iteration (default: _item).</li>
 *   <li><strong>index:</strong> Variable name for 0-based iteration index (default: _index).</li>
 *   <li><strong>maxloops:</strong> Maximum body executions (default: 100, prevents runaway loops).
 *       Items skipped by {@code empty="true"} do not consume this limit; the index retains the source position.</li>
 *   <li><strong>delimiter:</strong> Literal string separator; comma by default for items and newline for inline text.</li>
 *   <li><strong>empty:</strong> Skip values for which {@link Variable#isEmpty()} is true (default: false).
 *       Literal tokens are trimmed before they become Variables; typed collection values are preserved.</li>
 *   <li><strong>filter:</strong> Reserved but not implemented; non-empty values are logged and ignored.</li>
 * </ul>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Loop over XPath results</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="product" index="i" maxloops="50">
 *   <xpath expression="//div[@class='product']">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 *   
 *   <!-- Extract product details -->
 *   <def var="name">
 *     <xpath expression=".//h3/text()">${product}</xpath>
 *   </def>
 *   <def var="price">
 *     <xpath expression=".//span[@class='price']/text()">${product}</xpath>
 *   </def>
 *   
 *   <!-- Save product -->
 *   <file path="products/product-${i}.json" action="write">
 *     {"name": "${name}", "price": "${price}"}
 *   </file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Loop over delimited list</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="urls">
 *   http://example.com/page1
 *   http://example.com/page2
 *   http://example.com/page3
 * </def>
 * 
 * <loop item="url">
 *   ${urls}
 *   
 *   <http url="${url}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Pagination scraping</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="pageNumbers">1,2,3,4,5,6,7,8,9,10</def>
 * 
 * <loop item="pageNum" index="i" delimiter=",">
 *   ${pageNumbers}
 *   
 *   <def var="pageContent">
 *     <http url="https://example.com/products?page=${pageNum}"/>
 *   </def>
 *   
 *   <file path="pages/page-${pageNum}.html" action="write">
 *     ${pageContent}
 *   </file>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Loop with index and counter</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="totalCount" value="0"/>
 * 
 * <loop item="item" index="idx" maxloops="100">
 *   <xpath expression="//li">
 *     <html-to-xml>
 *       <http url="https://example.com/list"/>
 *     </html-to-xml>
 *   </xpath>
 *   
 *   <!-- Track iteration number -->
 *   <file path="scraping.log" action="append">
 *     Processing item ${idx}: ${item}
 *   </file>
 *   
 *   <!-- Update counter -->
 *   <def var="totalCount" value="${totalCount}1"/>
 * </loop>
 * 
 * <!-- Final count -->
 * <file path="summary.txt" action="write">
 *   Processed ${totalCount} items
 * </file>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: Skip empty items</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="link" empty="true">
 *   <xpath expression="//a/@href">
 *     <html-to-xml>
 *       <http url="https://example.com/links"/>
 *     </html-to-xml>
 *   </xpath>
 *   
 *   <!-- Only non-empty links are processed -->
 *   <http url="${link}"/>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h4>Example 6: Nested loops (products and variants)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="product" index="i">
 *   <xpath expression="//div[@class='product']">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 *   
 *   <def var="productName">
 *     <xpath expression=".//h3/text()">${product}</xpath>
 *   </def>
 *   
 *   <!-- Nested loop for product variants -->
 *   <loop item="variant" index="j">
 *     <xpath expression=".//div[@class='variant']">${product}</xpath>
 *     
 *     <def var="size">
 *       <xpath expression=".//span[@class='size']/text()">${variant}</xpath>
 *     </def>
 *     
 *     <file path="products/product-${i}-variant-${j}.txt" action="write">
 *       ${productName} - Size: ${size}
 *     </file>
 *   </loop>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Loop Control</h3>
 * 
 * <p><strong>maxloops:</strong> Safety limit to prevent infinite loops</p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Default limit: 100 iterations -->
 * <loop item="item">...</loop>
 * 
 * <!-- Custom limit: 1000 iterations -->
 * <loop item="item" maxloops="1000">...</loop>
 * 
 * <!-- No limit (use with caution!) -->
 * <loop item="item" maxloops="-1">...</loop>
 * </config>
 * }</pre>
 * 
 * <p><strong>Breaking out of loop:</strong></p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <loop item="item" maxloops="1000">
 *   <xpath expression="//item">...</xpath>
 *   
 *   <!-- Exit loop on condition -->
 *   <if condition="${item = 'STOP'}">
 *     <exit/>
 *   </if>
 * </loop>
 * </config>
 * }</pre>
 * 
 * <h3>Performance Considerations</h3>
 * <ul>
 *   <li><strong>Set realistic maxloops:</strong> Prevents runaway loops if data is larger than expected</li>
 *   <li><strong>Reuse HTTP responses:</strong> Fetch once outside loop, extract inside</li>
 *   <li><strong>Batch file writes:</strong> Accumulate data, write once after loop</li>
 *   <li><strong>Filter data before passing it to Loop:</strong> Use XPath predicates in the source expression</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Variable scope:</strong> Each body iteration gets an isolated scope; the final processed item/index remain visible after the loop</li>
 *   <li><strong>Index is 0-based:</strong> First iteration has index=0</li>
 *   <li><strong>Empty handling:</strong> By default, empty items are processed (set empty="true" to skip)</li>
 *   <li><strong>Delimiter:</strong> Delimiters are literal strings; items defaults to comma and inline text defaults to newline</li>
 *   <li><strong>Type preservation:</strong> ListVariable elements retain their Variable type; delimited text becomes NodeVariable</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <while>} - Conditional iteration (while condition is true)</li>
 *   <li>{@code <xpath>} - Extract lists for iteration</li>
 *   <li>{@code <exit>} - Break out of loop early</li>
 *   <li>{@code <if>} - Conditional processing within loop</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.3.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "loop",
    description = "Loop plugin for iterations",
    version = "2.1.0",
    category = "Control"
)
public class LoopPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.LoopPlugin");

    @Override
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    protected Variable doExecute(final DynamicScopeContext context) throws InterruptedException {
        final PluginConfiguration config = getConfiguration();
        final String item = config.getProperty("item", "");
        final String index = config.getProperty("index", "");
        final String maxloops = config.getProperty("maxloops", "100");
        final String filter = config.getProperty("filter", "");
        final boolean skipEmpty = Boolean.parseBoolean(config.getProperty("empty", "false"));

        final String itemVarName = item == null || item.isBlank() ? "_item" : item.trim();
        final String indexVarName = index == null || index.isBlank() ? "_index" : index.trim();

        try {
            if (filter != null && !filter.isBlank()) {
                LOGGER.warn("Loop filter is not yet supported and will be ignored: {}", filter);
            }

            final int maxIterations = resolveMaxLoops(maxloops, context);
            if (maxIterations == 0) {
                return new NodeVariable("");
            }

            final LoopInputSourceResolver.ResolvedInput input =
                LoopInputSourceResolver.resolve(config, context);
            final int availableItems = input.hasSource() ? input.getItems().size()
                : (maxIterations < 0 ? 0 : maxIterations);

            final DefaultPluginConfiguration bodyConfiguration = new DefaultPluginConfiguration(
                "loop-body", config.getNamespace(), "loop-body", new java.util.Properties());
            bodyConfiguration.setChildren(input.getBodyChildren());
            final DefaultPluginBodyExecutor bodyExecutor = new DefaultPluginBodyExecutor();
            final NodeVariable emptyItem = new NodeVariable("");
            final StringBuilder result = new StringBuilder();

            int executedIterations = 0;
            for (int i = 0; i < availableItems; i++) {
                if (maxIterations >= 0 && executedIterations >= maxIterations) {
                    break;
                }

                final int currentIndex = i;
                final Variable currentItem = input.hasSource()
                    ? input.getItems().get(i) : emptyItem;
                final NodeVariable currentIndexVariable =
                    new NodeVariable(String.valueOf(currentIndex));

                if (skipEmpty && (currentItem == null || currentItem.isEmpty())) {
                    continue;
                }

                try {
                    final Variable bodyResult = context.executeWithinNewContext(() -> {
                        context.setLocalVar(itemVarName, currentItem);
                        context.setLocalVar(indexVarName, currentIndexVariable);

                        if (input.requiresCustomBodyExecution()) {
                            if (input.getBodyChildren().isEmpty()) {
                                return input.hasSource() ? currentItem : EmptyVariable.INSTANCE;
                            }
                            return bodyExecutor.executeBody(bodyConfiguration, context);
                        }

                        final Variable iterationBodyResult = executeBody(context);
                        if (input.hasSource() && input.getBodyChildren().isEmpty()
                                && (iterationBodyResult == null
                                    || iterationBodyResult instanceof EmptyVariable)) {
                            return currentItem;
                        }
                        return iterationBodyResult;
                    });

                    if (bodyResult != null && !(bodyResult instanceof EmptyVariable)) {
                        result.append(bodyResult.toString());
                    }
                    executedIterations++;
                } finally {
                    // Keep the historical final item/index visibility even
                    // when a body fails, while body-local values are cleaned
                    // by executeWithinNewContext().
                    context.setLocalVar(itemVarName, currentItem);
                    context.setLocalVar(indexVarName, currentIndexVariable);
                }
            }

            LOGGER.debug("LoopPlugin: Final result: {}", result);
            return new NodeVariable(result.toString());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            LOGGER.debug("LoopPlugin: Error: {}", e.getMessage());
            LOGGER.error("Error in LoopPlugin: {}", e.getMessage(), e);
            return EmptyVariable.INSTANCE;
        }
    }

    /**
     * Resolves template expressions in the iteration limit against the current
     * execution context before parsing the numeric value.
     */
    private int resolveMaxLoops(String configuredValue, DynamicScopeContext context) {
        String resolvedValue = configuredValue;

        if (configuredValue != null && configuredValue.contains(BaseTemplater.VAR_START)) {
            if (BaseTemplater.scriptEngineFactory != null && context.getScriptingLanguage() != null) {
                resolvedValue = BaseTemplater.evaluateToString(
                    configuredValue, context.getScriptingLanguage(), context);
            }

            // Keep simple variable references usable in lightweight contexts where
            // the scripting engine has not been initialized (for example unit tests).
            if (resolvedValue != null && resolvedValue.contains(BaseTemplater.VAR_START)) {
                String expression = configuredValue.trim();
                if (expression.startsWith(BaseTemplater.VAR_START)
                        && expression.endsWith(BaseTemplater.VAR_END)
                        && expression.indexOf(BaseTemplater.VAR_START,
                            BaseTemplater.VAR_START.length()) < 0) {
                    String variableName = expression.substring(
                        BaseTemplater.VAR_START.length(),
                        expression.length() - BaseTemplater.VAR_END.length()).trim();
                    Variable variable = context.getVar(variableName);
                    if (variable != null) {
                        resolvedValue = variable.toString();
                    }
                }
            }
        }

        return parseMaxLoops(resolvedValue);
    }

    private int parseMaxLoops(String maxloops) {
        if (maxloops == null) {
            return 100;
        }

        try {
            return Integer.parseInt(maxloops.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("LoopPlugin: Invalid maxloops '{}', defaulting to 100", maxloops);
            return 100;
        }
    }

    /**
     * Metadata for LoopPlugin.
     * 
     * <p>Master plugin - can contain any child plugins. Iterates over collections.</p>
     */
    public static class LoopPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "loop";
        }

        @Override
        public String getName() {
            return "Loop";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Iterates over collections of data, executing body content once for each item. Supports XPath results, variables, and delimited strings. Essential for scraping paginated data, product lists, and repeating structures.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = LoopPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Control";
        }

        @Override
        public String[] getTags() {
            return new String[]{"loop", "iteration", "core", "control", "container"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return LoopPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "loop";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new LoopPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-sync-alt";
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
                new InputPort("collection", "Collection to iterate (from body content)", PortType.MIXED, false)
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
            return "Iterates over collections of data, executing body content once for each item. Supports XPath results, variables, and delimited strings. Can contain any child plugins. Essential for scraping paginated data and repeating structures.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+L", "Add Loop", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "Loop Plugin",
                "Iterates over collections of data, executing body content once for each item. Supports XPath results, variables, and delimited strings.",
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
     * Configuration schema for LoopPlugin.
     */
    public static class LoopPluginConfigurationSchema implements PluginConfigurationSchema {
        @Override
        public PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new PluginConfigurationSchema.AttributeDefinition[0];
        }

        @Override
        public PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
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
            return new String[]{"items", "item", "index", "maxloops", "filter", "empty", "delimiter"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
            // No validation needed for now
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new LoopPluginMetadata();
    }
    
    public PluginConfigurationSchema getConfigurationSchema() {
        return new LoopPluginMetadata().getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) {
        // No initialization needed
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        // No validation needed for now
    }
}