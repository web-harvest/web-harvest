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
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.Variable;
// REMOVED in v2.2.x
import org.webharvest.definition.XmlNode;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import org.webharvest.utils.XmlUtil;
import org.webharvest.exception.ScraperXQueryException;
// import org.webharvest.runtime.RuntimeConfig; // Removed - will be handled via dependency injection
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;

/**
 * XQuery expression processor - powerful XML querying and transformation using XQuery language.
 * 
 * <p>
 * The {@code <xquery>} processor evaluates XQuery expressions against XML data, providing
 * advanced querying, filtering, transformation, and aggregation capabilities beyond what
 * XPath offers. XQuery is a full-featured query language (similar to SQL for databases)
 * designed specifically for XML data.
 * </p>
 * 
 * <h3>XQuery vs XPath</h3>
 * <table border="1">
 *   <tr>
 *     <th>Feature</th>
 *     <th>XPath</th>
 *     <th>XQuery</th>
 *   </tr>
 *   <tr>
 *     <td>Selection</td>
 *     <td>Simple paths</td>
 *     <td>Complex FLWOR expressions</td>
 *   </tr>
 *   <tr>
 *     <td>Transformation</td>
 *     <td>Limited</td>
 *     <td>Full XML reconstruction</td>
 *   </tr>
 *   <tr>
 *     <td>Joins</td>
 *     <td>Not supported</td>
 *     <td>Multi-document joins</td>
 *   </tr>
 *   <tr>
 *     <td>Aggregation</td>
 *     <td>Basic (count, sum)</td>
 *     <td>Advanced (group by, distinct)</td>
 *   </tr>
 * </table>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: FLWOR expression (For-Let-Where-Order-Return)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="expensiveProducts">
 *   <xquery>
 *     <![CDATA[
 *       for $product in //product
 *       where $product/price > 100
 *       order by $product/price descending
 *       return $product/name/text()
 *     ]]>
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xquery>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: XML transformation</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="transformedXml">
 *   <xquery>
 *     <![CDATA[
 *       <products>
 *       {
 *         for $p in //div[@class='product']
 *         return
 *           <product>
 *             <id>{data($p/@id)}</id>
 *             <name>{$p//h3/text()}</name>
 *             <price>{$p//span[@class='price']/text()}</price>
 *           </product>
 *       }
 *       </products>
 *     ]]>
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xquery>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Aggregation (count, sum, avg)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="totalPrice">
 *   <xquery>
 *     <![CDATA[
 *       sum(//product/price)
 *     ]]>
 *     <json-to-xml>
 *       <http url="https://api.example.com/products"/>
 *     </json-to-xml>
 *   </xquery>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: XQuery with node() parameter (v2.2.0 - Fixed r825)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Create sample XML data -->
 * <def var="productsXml">
 *   <template><![CDATA[
 *     <products>
 *       <product><name>Book A</name><price>25.50</price></product>
 *       <product><name>Book B</name><price>18.99</price></product>
 *     </products>
 *   ]]></template>
 * </def>
 * 
 * <!-- Process with XQuery using node() parameter -->
 * <def var="analysis">
 *   <xquery>
 *     <xq-param name="data" type="node()">
 *       <get var="productsXml"/>
 *     </xq-param>
 *     <xq-expression>
 *       <![CDATA[
 *       declare variable $data as node() external;
 *       
 *       let $count := count($data//product)
 *       let $total := sum($data//product/price)
 *       
 *       return
 *       <analysis>
 *         <total_products>{$count}</total_products>
 *         <total_value>{$total}</total_value>
 *       </analysis>
 *       ]]>
 *     </xq-expression>
 *   </xquery>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 5: XQuery with typed parameters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xquery>
 *   <xq-param name="a" type="integer">10</xq-param>
 *   <xq-param name="b" type="integer">5</xq-param>
 *   <xq-expression>
 *     <![CDATA[
 *     declare variable $a as xs:integer external;
 *     declare variable $b as xs:integer external;
 *     
 *     <calculations>
 *       <sum>{$a + $b}</sum>
 *       <product>{$a * $b}</product>
 *     </calculations>
 *     ]]>
 *   </xq-expression>
 * </xquery>
 * </config>
 * }</pre>
 * 
 * <h3>Child Elements (v2.2.0)</h3>
 * <ul>
 *   <li><strong>{@code <xq-expression>}:</strong> XQuery expression (required) - Use CDATA for complex queries</li>
 *   <li><strong>{@code <xq-param>}:</strong> XQuery parameter binding (optional, multiple allowed)</li>
 * </ul>
 * 
 * <h3>Parameter Types</h3>
 * <p>The {@code <xq-param>} element supports the following types:</p>
 * <ul>
 *   <li><strong>node():</strong> XML node from variable (use {@code <get var="..."/>})</li>
 *   <li><strong>integer:</strong> Integer value</li>
 *   <li><strong>long:</strong> Long integer value</li>
 *   <li><strong>double/float:</strong> Decimal value</li>
 *   <li><strong>boolean:</strong> Boolean value (true/false)</li>
 *   <li><strong>string:</strong> Text value (default)</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Saxon engine:</strong> Uses Saxon XQuery processor</li>
 *   <li><strong>CDATA required:</strong> Wrap XQuery in {@code <![CDATA[...]]>}</li>
 *   <li><strong>Child elements:</strong> v2.2.0 supports {@code <xq-param>} and {@code <xq-expression>}</li>
 *   <li><strong>Node parameters:</strong> Use {@code type="node()"} with {@code <get>} to pass XML variables</li>
 *   <li><strong>Returns ListVariable:</strong> Result can be single or multiple items</li>
 * </ul>
 * 
 * <h3>Bug Fixes</h3>
 * <ul>
 *   <li><strong>r825 (Oct 2025):</strong> Fixed XQParamPlugin to execute child elements (node() parameters)</li>
 *   <li><strong>r824 (Oct 2025):</strong> Added executeChildElements() for nested {@code <get>} support</li>
 *   <li><strong>r823 (Oct 2025):</strong> Refactored to parse {@code <xq-param>} and {@code <xq-expression>} children</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <xq-param>} - XQuery parameter definition</li>
 *   <li>{@code <xq-expression>} - XQuery expression definition</li>
 *   <li>{@code <xpath>} - Simpler XML querying</li>
 *   <li>{@code <xslt>} - XSLT transformations</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "xquery",
    description = "XQuery plugin that provides XQuery processing functionality",
    version = "2.1.0",
    category = "Extraction"
)
public class XQueryPlugin extends AbstractPlugin {

    /**
     * Metadata for XQueryPlugin.
     * 
     * <p>Master plugin - requires xq-expression and allows xq-param as children. Evaluates XQuery expressions against XML data, providing advanced querying, filtering, transformation, and aggregation capabilities beyond what XPath offers.</p>
     */
    public static class XQueryPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "xquery";
        }

        @Override
        public String getName() {
            return "XQuery";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "XQuery expression processor that evaluates XQuery expressions against XML data, providing advanced querying, filtering, transformation, and aggregation capabilities beyond what XPath offers. Supports FLWOR expressions (For-Let-Where-Order-Return), XML transformation, multi-document joins, grouping, and complex aggregations. XQuery is a full-featured query language (similar to SQL for databases) designed specifically for XML data.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = XQueryPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Extraction";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xquery", "xml", "processing", "query", "expression", "flwor", "extraction", "transformation"};
        }

        @Override
        public String[] getDependencies() {
            return new String[]{"Saxon"};
        }

        @Override
        public String getClassName() {
            return XQueryPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xquery";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new XQueryPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-search-plus";
        }
        
        @Override
        public String getColor() {
            return "#10B981"; // Green for extraction/query
        }
        
        @Override
        public String getGroup() {
            return "Extraction";
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
                new InputPort("xml", "XML data to query", PortType.XML, true),
                new InputPort("params", "XQuery parameters (via xq-param)", PortType.MIXED, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("result", "XQuery query results", PortType.XML)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.XML;
        }
        
        @Override
        public String getTooltip() {
            return "XQuery expression processor. Evaluates XQuery expressions against XML data, providing advanced querying, filtering, transformation, and aggregation capabilities beyond XPath.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+Q", "Add XQuery", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "XQuery Plugin",
                "Evaluates XQuery expressions against XML data. Supports FLWOR expressions, XML transformation, and complex aggregations.",
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
            return new String[]{"xq-expression", "xq-param"};
        }
    }
    
    /**
     * Configuration schema for XQueryPlugin.
     */
    public static class XQueryPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the xquery element";
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
            return new String[]{"xq-expression"};
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[]{"xq-param", "xq-expression"};
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new XQueryPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new XQueryPluginMetadata().getConfigurationSchema();
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
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.XQueryPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("XQueryPlugin.doExecute: START");
        
        PluginConfiguration config = getConfiguration();
        String xqExpression = "";
        Map<String, Object> xqParams = new HashMap<>();
        
        // Parse child elements: <xq-expression> and <xq-param>
        if (config.hasChildren()) {
            for (PluginConfiguration child : config.getChildren()) {
                String elementName = child.getElementName();
                LOGGER.debug("XQueryPlugin: Processing child element: {}", elementName);
                
                if ("xq-expression".equals(elementName)) {
                    // Get XQuery expression from <xq-expression> element
                    xqExpression = child.getProperty("_textContent", "");
                    LOGGER.debug("XQueryPlugin: XQuery expression length: {}", xqExpression.length());
                } else if ("xq-param".equals(elementName)) {
                    // Get parameter name and type
                    String paramName = child.getProperty("name", "");
                    String paramType = child.getProperty("type", "");
                    
                    LOGGER.debug("XQueryPlugin: Processing xq-param: name=" + paramName + ", type=" + paramType);
                    
                    if (!paramName.isEmpty()) {
                        Variable paramValue = null;
                        
                        // Try to execute child elements first (like <get>, <http>, etc.)
                        if (child.hasChildren()) {
                            paramValue = executeChildElements(child, context);
                            LOGGER.debug("XQueryPlugin: Parameter value from child execution: " + (paramValue != null ? paramValue.toString().substring(0, Math.min(100, paramValue.toString().length())) : "null"));
                        } 
                        
                        // If no children or empty result, use text content
                        if (paramValue == null || paramValue instanceof EmptyVariable) {
                            String textContent = child.getProperty("_textContent", "").trim();
                            if (!textContent.isEmpty()) {
                                paramValue = new org.webharvest.runtime.variables.NodeVariable(textContent);
                                LOGGER.debug("XQueryPlugin: Parameter value from text content: " + textContent);
                            }
                        }
                        
                        if (paramValue != null && !(paramValue instanceof EmptyVariable)) {
                            String stringValue = paramValue.toString().trim();
                            
                            // Auto-detect XML content if no type specified
                            boolean looksLikeXml = stringValue.startsWith("<") && stringValue.contains(">");
                            
                            // Convert based on type
                            if ("node()".equals(paramType) || paramType.startsWith("node()") || 
                                (paramType.isEmpty() && looksLikeXml)) {
                                // For node() type or XML content, pass the Variable directly
                                xqParams.put(paramName, paramValue);
                                LOGGER.debug("XQueryPlugin: Parameter {} will be bound as node()", paramName);
                            } else if (!paramType.isEmpty()) {
                                // For other explicit types, convert to string first, then to target type
                                xqParams.put(paramName, convertParamValue(stringValue, paramType));
                            } else {
                                // Default: string
                                xqParams.put(paramName, stringValue);
                            }
                        } else {
                            LOGGER.warn("XQueryPlugin: No value obtained for parameter: " + paramName);
                            xqParams.put(paramName, "");
                        }
                    }
                }
            }
        }
        
        if (xqExpression == null || xqExpression.trim().isEmpty()) {
            LOGGER.warn("No XQuery expression provided");
            return EmptyVariable.INSTANCE;
        }
        
        LOGGER.debug("Evaluating XQuery expression with {} parameters", xqParams.size());
        
        try {
            // Use Saxon for XQuery evaluation - same as original XQueryProcessor
            Configuration saxonConfig = new Configuration();
            StaticQueryContext sqc = new StaticQueryContext(saxonConfig);
            
            XQueryExpression exp = sqc.compileQuery(xqExpression);
            DynamicQueryContext dynamicContext = new DynamicQueryContext(saxonConfig);
            
            // Bind XQuery parameters
            for (Map.Entry<String, Object> entry : xqParams.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                
                LOGGER.debug("XQueryPlugin: Binding parameter: {} = {}", paramName, paramValue.getClass().getSimpleName());
                
                // Convert to Saxon value
                if (paramValue instanceof Variable) {
                    // Handle node() type - convert Variable to Saxon node
                    Variable var = (Variable) paramValue;
                    try {
                        String xmlContent = var.toString();
                        StringReader reader = new StringReader(xmlContent);
                        javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(reader);
                        net.sf.saxon.om.Item contextItem = sqc.buildDocument(streamSource);
                        dynamicContext.setParameter(paramName, contextItem);
                        LOGGER.debug("XQueryPlugin: Bound node() parameter: " + paramName);
                    } catch (Exception e) {
                        LOGGER.warn("XQueryPlugin: Failed to bind node() parameter " + paramName + ": " + e.getMessage());
                        // Fallback to string
                        dynamicContext.setParameter(paramName, new net.sf.saxon.value.StringValue(paramValue.toString()));
                    }
                } else if (paramValue instanceof java.util.List) {
                    // Handle sequences (integer*, double*, etc.)
                    java.util.List<?> sequence = (java.util.List<?>) paramValue;
                    java.util.List<net.sf.saxon.om.Item> saxonItems = new java.util.ArrayList<>();
                    
                    for (Object item : sequence) {
                        if (item instanceof Integer) {
                            saxonItems.add(net.sf.saxon.value.Int64Value.makeIntegerValue((Integer) item));
                        } else if (item instanceof Long) {
                            saxonItems.add(net.sf.saxon.value.Int64Value.makeIntegerValue((Long) item));
                        } else if (item instanceof Double) {
                            saxonItems.add(new net.sf.saxon.value.DoubleValue((Double) item));
                        } else if (item instanceof Boolean) {
                            saxonItems.add(net.sf.saxon.value.BooleanValue.get((Boolean) item));
                        } else {
                            saxonItems.add(new net.sf.saxon.value.StringValue(item.toString()));
                        }
                    }
                    
                    // Create Saxon sequence from items
                    net.sf.saxon.value.SequenceExtent saxonSequence = new net.sf.saxon.value.SequenceExtent(
                        saxonItems.toArray(new net.sf.saxon.om.Item[0])
                    );
                    dynamicContext.setParameter(paramName, saxonSequence);
                    LOGGER.debug("XQueryPlugin: Bound sequence parameter {} with {} items", paramName, saxonItems.size());
                } else if (paramValue instanceof String) {
                    dynamicContext.setParameter(paramName, new net.sf.saxon.value.StringValue((String) paramValue));
                } else if (paramValue instanceof Integer) {
                    dynamicContext.setParameter(paramName, net.sf.saxon.value.Int64Value.makeIntegerValue((Integer) paramValue));
                } else if (paramValue instanceof Long) {
                    dynamicContext.setParameter(paramName, net.sf.saxon.value.Int64Value.makeIntegerValue((Long) paramValue));
                } else if (paramValue instanceof Double) {
                    dynamicContext.setParameter(paramName, new net.sf.saxon.value.DoubleValue((Double) paramValue));
                } else if (paramValue instanceof Boolean) {
                    dynamicContext.setParameter(paramName, net.sf.saxon.value.BooleanValue.get((Boolean) paramValue));
                } else {
                    // Default: convert to string
                    dynamicContext.setParameter(paramName, new net.sf.saxon.value.StringValue(paramValue.toString()));
                }
            }
            
            return XmlUtil.createListOfXmlNodes(exp, dynamicContext);
            
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate XQuery expression: {}", e.getMessage(), e);
            throw new ScraperXQueryException("Error executing XQuery expression (XQuery = [" + xqExpression + "])!", e);
        }
    }
    
    /**
     * Execute child elements of a configuration to get their result.
     * 
     * <p>
     * Used for {@code <xq-param>} elements that contain nested processors like
     * {@code <get>}, {@code <http>}, {@code <xpath>}, etc.
     * </p>
     * 
     * <p>
     * This method creates an XQParamPlugin instance and executes it with the child
     * configuration. The XQParamPlugin in turn calls executeBody() which executes
     * all nested child elements and returns their result.
     * </p>
     * 
     * @param config The plugin configuration containing child elements
     * @param context The execution context
     * @return Variable result from child element execution, or EmptyVariable if execution fails
     * @throws InterruptedException If execution is interrupted
     * @since 2.2.0 (r824)
     */
    private Variable executeChildElements(PluginConfiguration config, DynamicScopeContext context) throws InterruptedException {
        if (!config.hasChildren()) {
            return EmptyVariable.INSTANCE;
        }
        
        try {
            // Create and execute XQParamPlugin instance to handle child elements
            XQParamPlugin paramPlugin = new XQParamPlugin();
            paramPlugin.initialize(config);
            Variable result = paramPlugin.execute(context);
            
            LOGGER.debug("XQueryPlugin: XQParamPlugin execution returned: " + (result != null ? result.getClass().getSimpleName() : "null"));
            
            return result != null ? result : EmptyVariable.INSTANCE;
            
        } catch (Exception e) {
            LOGGER.warn("XQueryPlugin: Failed to execute child elements: " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
    }
    
    /**
     * Convert parameter value string to appropriate Java type based on XQuery type.
     * 
     * <p>
     * Converts string parameter values to proper Java types for XQuery parameter binding.
     * Supports integer, long, double, float, boolean, and string types.
     * </p>
     * 
     * @param value The parameter value as string
     * @param type The XQuery type (e.g., "integer", "double", "boolean", "string")
     * @return Converted value as appropriate Java type (Integer, Double, Boolean, or String)
     * @since 2.2.0 (r823)
     */
    private Object convertParamValue(String value, String type) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        // Check if type is a sequence (ends with * or +)
        boolean isSequence = type.endsWith("*") || type.endsWith("+");
        String baseType = isSequence ? type.substring(0, type.length() - 1).trim() : type;
        
        if (isSequence) {
            // Split value by whitespace (spaces, tabs, newlines)
            String[] values = value.trim().split("\\s+");
            java.util.List<Object> sequence = new java.util.ArrayList<>();
            
            for (String val : values) {
                if (!val.isEmpty()) {
                    sequence.add(convertSingleValue(val, baseType));
                }
            }
            
            return sequence;
        } else {
            return convertSingleValue(value.trim(), baseType);
        }
    }
    
    /**
     * Convert a single value to the specified XQuery type.
     */
    private Object convertSingleValue(String value, String type) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        
        try {
            if (type.contains("integer")) {
                return Integer.parseInt(value);
            } else if (type.contains("long")) {
                return Long.parseLong(value);
            } else if (type.contains("double") || type.contains("float")) {
                return Double.parseDouble(value);
            } else if (type.contains("boolean")) {
                return Boolean.parseBoolean(value);
            } else {
                // Default: string
                return value;
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Failed to convert value '{}' to type '{}', using as string", value, type);
            return value;
        }
    }
}
