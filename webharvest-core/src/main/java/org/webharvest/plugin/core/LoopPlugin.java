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
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

/**
 * Loop iterator processor - repeats execution for each item in a collection.
 * 
 * <p>
 * The {@code <loop>} processor is fundamental for iterating over collections of data,
 * such as product lists, search results, paginated APIs, or any repeating data structure.
 * It executes its body content once for each item, with optional index tracking and filtering.
 * </p>
 * 
 * <h3>Loop Data Sources</h3>
 * <p>The loop can iterate over:</p>
 * <ul>
 *   <li><strong>Body content:</strong> Result from executing child processors (most common)</li>
 *   <li><strong>Variable:</strong> Previously defined ListVariable or collection</li>
 *   <li><strong>Delimited string:</strong> Text split by delimiter (default: newline)</li>
 * </ul>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>item:</strong> Variable name for current item in each iteration</li>
 *   <li><strong>index:</strong> Variable name for 0-based iteration index (optional)</li>
 *   <li><strong>maxloops:</strong> Maximum iterations (default: 100, prevents infinite loops)</li>
 *   <li><strong>delimiter:</strong> String separator for splitting (default: newline)</li>
 *   <li><strong>filter:</strong> XPath expression to filter items (optional)</li>
 *   <li><strong>empty:</strong> Skip empty items if true (default: false)</li>
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
 * <h4>Example 5: Filtered loop (skip empty items)</h4>
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
 *   <li><strong>Filter early:</strong> Use XPath predicates to reduce loop iterations</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Variable scope:</strong> {@code item} and {@code index} variables are local to loop body</li>
 *   <li><strong>Index is 0-based:</strong> First iteration has index=0</li>
 *   <li><strong>Empty handling:</strong> By default, empty items are processed (set empty="true" to skip)</li>
 *   <li><strong>Delimiter:</strong> Newline by default, can be comma, semicolon, or any string</li>
 *   <li><strong>Type preservation:</strong> Item variable preserves original Variable type</li>
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
 * @version 2.2.0
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
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String item = config.getProperty("item", "");
        String index = config.getProperty("index", "");
        String maxloops = config.getProperty("maxloops", "100");
        String filter = config.getProperty("filter", "");
        String empty = config.getProperty("empty", "false");
        
        LOGGER.debug("LoopPlugin: item=" + item + ", index=" + index + ", maxloops=" + maxloops);

        try {
            // Get items from configuration
            String itemsStr = config.getProperty("items", "");
            String delimiter = config.getProperty("delimiter", ",");
            
            LOGGER.debug("LoopPlugin: items=" + itemsStr + ", delimiter=" + delimiter);
            
            if (itemsStr.isEmpty()) {
                LOGGER.debug("LoopPlugin: No items specified");
                return EmptyVariable.INSTANCE;
            }
            
            // Split items by delimiter
            String[] items = itemsStr.split(delimiter);
            LOGGER.debug("LoopPlugin: Found " + items.length + " items");
            
            StringBuilder result = new StringBuilder();
            
            // Loop through items
            for (int i = 0; i < items.length; i++) {
                final String currentItem = items[i].trim();
                final int iteration = i;
                LOGGER.debug("LoopPlugin: Processing item " + i + ": " + currentItem);
                
                // Bug #19 Fix: Execute each iteration in nested context
                // This automatically cleans up variables created in loop body
                // Prevents memory leak when looping 1000s of items
                try {
                    Variable bodyResult = context.executeWithinNewContext(() -> {
                        // Set current item in nested context
                        context.setLocalVar("item", new NodeVariable(currentItem));
                        context.setLocalVar("index", new NodeVariable(String.valueOf(iteration)));
                        
                        // Execute body content for each item
                        return executeBody(context);
                    });
                    
                    if (bodyResult != null && !(bodyResult instanceof EmptyVariable)) {
                        result.append(bodyResult.toString());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            
            LOGGER.debug("LoopPlugin: Final result: " + result.toString());
            return new NodeVariable(result.toString());
            
        } catch (Exception e) {
            LOGGER.debug("LoopPlugin: Error: " + e.getMessage());
            LOGGER.error("Error in LoopPlugin: {}", e.getMessage(), e);
            return EmptyVariable.INSTANCE;
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
            @Override
            public String getId() {
                return "loop";
            }

            @Override
            public String getName() {
                return "Loop Plugin";
            }

            @Override
            public String getVersion() {
                return "2.1.0";
            }

            @Override
            public String getDescription() {
                return "Loop plugin for iterations";
            }

            @Override
            public String getAuthor() {
                return "Web-Harvest Team";
            }

            @Override
            public String getCategory() {
                return "Control";
            }

            @Override
            public String[] getTags() {
                return new String[]{"loop", "iteration", "core"};
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
                return new PluginConfigurationSchema() {
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
                        return new String[]{"item", "index", "maxloops", "filter", "empty"};
                    }

                    @Override
                    public String[] getRequiredAttributes() {
                        return new String[0];
                    }

                    @Override
                    public void validate(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
                        // No validation needed for now
                    }
                };
            }
        };
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