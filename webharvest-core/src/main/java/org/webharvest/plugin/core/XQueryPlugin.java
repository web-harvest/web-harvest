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
 * }</pre>
 * 
 * <h4>Example 2: XML transformation</h4>
 * <pre>{@code
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
 * }</pre>
 * 
 * <h4>Example 3: Aggregation (count, sum, avg)</h4>
 * <pre>{@code
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
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Saxon engine:</strong> Uses Saxon XQuery processor</li>
 *   <li><strong>CDATA required:</strong> Wrap XQuery in {@code <![CDATA[...]]>}</li>
 *   <li><strong>Input XML:</strong> Provided as child processor result</li>
 *   <li><strong>Returns ListVariable:</strong> Result can be single or multiple items</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
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
    version = "2.1.0"
)
public class XQueryPlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "xquery";
        }

        @Override
        public String getName() {
            return "XQuery Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "XQuery plugin that provides XQuery processing functionality";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            return "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xquery", "xml", "processing", "query", "expression"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
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
            return new PluginConfigurationSchema() {
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
                    return "2.1.0";
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
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
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
        LOGGER.debug("XQueryPlugin.doExecute: START");
        
        // Get XQuery expression and XML from body content
        String xqExpression = "";
        String xmlContent = "";
        
        try {
            // Execute body content to get sub-elements
            Variable bodyContent = executeBody(context);
            LOGGER.debug("XQueryPlugin.doExecute: bodyContent=" + bodyContent);
            
            if (bodyContent == null || bodyContent instanceof EmptyVariable) {
                LOGGER.warn("No body content available for XQuery evaluation");
                return EmptyVariable.INSTANCE;
            }
            
            // Parse the body content to extract XQuery expression and XML
            String bodyText = bodyContent.toString();
            LOGGER.debug("XQueryPlugin.doExecute: bodyText=" + bodyText);
            
            // Simple parsing - assume first line is XQuery expression, rest is XML
            String[] lines = bodyText.split("\n");
            if (lines.length >= 2) {
                xqExpression = lines[0].trim();
                xmlContent = lines[1].trim();
                LOGGER.debug("XQueryPlugin.doExecute: Extracted xqExpression=" + xqExpression);
                LOGGER.debug("XQueryPlugin.doExecute: Extracted xmlContent=" + xmlContent);
            } else if (lines.length == 1) {
                // Only one line - treat as XQuery expression
                xqExpression = lines[0].trim();
                LOGGER.debug("XQueryPlugin.doExecute: Extracted xqExpression=" + xqExpression);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error getting body content for XQuery evaluation", e);
            LOGGER.debug("XQueryPlugin.doExecute: ERROR: " + e.getMessage());
            return EmptyVariable.INSTANCE;
        }
        
        if (xqExpression == null || xqExpression.trim().isEmpty()) {
            LOGGER.warn("No XQuery expression provided");
            return EmptyVariable.INSTANCE;
        }
        
        LOGGER.debug("Evaluating XQuery expression with length: {}", xqExpression.length());
        
        try {
            // Use Saxon for XQuery evaluation - same as original XQueryProcessor
            Configuration saxonConfig = new Configuration();
            StaticQueryContext sqc = new StaticQueryContext(saxonConfig);
            
            XQueryExpression exp = sqc.compileQuery(xqExpression);
            DynamicQueryContext dynamicContext = new DynamicQueryContext(saxonConfig);
            
            // Set XML content as context for XQuery evaluation
            if (xmlContent != null && !xmlContent.trim().isEmpty()) {
                LOGGER.debug("XQueryPlugin.doExecute: Setting XML content as context");
                try {
                    // Parse XML content and set as context item
                    StringReader reader = new StringReader(xmlContent);
                    javax.xml.transform.stream.StreamSource streamSource = new javax.xml.transform.stream.StreamSource(reader);
                    net.sf.saxon.om.Item contextItem = sqc.buildDocument(streamSource);
                    dynamicContext.setContextItem(contextItem);
                    LOGGER.debug("XQueryPlugin.doExecute: Successfully set XML content as context");
                } catch (Exception contextException) {
                    LOGGER.debug("XQueryPlugin.doExecute: Failed to set XML content as context: " + contextException.getMessage());
                    // Continue without context - some XQuery expressions might work without it
                }
            }
            
            return XmlUtil.createListOfXmlNodes(exp, dynamicContext);
            
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate XQuery expression: {}", e.getMessage(), e);
            LOGGER.debug("XQueryPlugin.doExecute: ERROR: " + e.getMessage());
            throw new ScraperXQueryException("Error executing XQuery expression (XQuery = [" + xqExpression + "])!", e);
        }
    }
}
