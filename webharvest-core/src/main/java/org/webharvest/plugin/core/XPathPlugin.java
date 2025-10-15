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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import net.sf.saxon.Configuration;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;
import org.webharvest.utils.XmlUtil;
import org.webharvest.exception.ScraperXPathException;

// Bug #13: jsoup for CSS selectors
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
// REMOVED in v2.2.x - RuntimeConfig no longer exists
// import org.webharvest.runtime.RuntimeConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.InputSource;

/**
 * XPath/CSS Selector plugin that provides expression evaluation functionality.
 * This is a core plugin migrated from the original XPathProcessor.
 * 
 * <p>
 * The XPath plugin evaluates XPath expressions OR CSS selectors against XML/HTML content.
 * It requires both content and expression as sub-elements.
 * </p>
 * 
 * <h3>Bug #13 Enhancement: CSS Selector Support (v2.2.0)</h3>
 * <p>
 * Added support for CSS selectors as a modern alternative to XPath for HTML querying.
 * Use the {@code type="css"} attribute to enable CSS selector mode.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>XPath Mode (default):</h4>
 * <pre>{@code
 * <!-- Extract single element text -->
 * <xpath expression="//title/text()">
 *   <html-to-xml>
 *     <http url="https://example.com"/>
 *   </html-to-xml>
 * </xpath>
 * }</pre>
 * 
 * <h4>CSS Selector Mode (Bug #13 - NEW!):</h4>
 * <pre>{@code
 * <!-- Extract links with class 'link' -->
 * <xpath type="css" expression="a.link">
 *   <html-to-xml>
 *     <http url="https://example.com"/>
 *   </html-to-xml>
 * </xpath>
 * 
 * <!-- Extract product titles -->
 * <xpath type="css" expression=".product h3">
 *   <html-to-xml>${response}</html-to-xml>
 * </xpath>
 * 
 * <!-- Use pseudo-selectors -->
 * <xpath type="css" expression="li:first-child">
 *   <html-to-xml>${content}</html-to-xml>
 * </xpath>
 * 
 * <!-- Extract all links -->
 * <xpath expression="//a/@href">
 *   <html-to-xml>
 *     <http url="https://example.com"/>
 *   </html-to-xml>
 * </xpath>
 * 
 * <!-- Extract specific div content -->
 * <xpath expression="//div[@class='content']//p">
 *   <html-to-xml>
 *     <http url="https://example.com"/>
 *   </html-to-xml>
 * </xpath>
 * 
 * <!-- Store result in variable -->
 * <def var="title">
 *   <xpath expression="//h1/text()">
 *     <html-to-xml>
 *       <http url="https://example.com"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>expression</strong> (required): The XPath expression OR CSS selector to evaluate</li>
 *   <li><strong>type</strong> (optional): Query type - "xpath" (default) or "css" (Bug #13)</li>
 *   <li><strong>xml-content</strong> (required): XML/HTML content to evaluate against (as sub-element)</li>
 * </ul>
 * 
 * <h3>CSS Selector Syntax (type="css")</h3>
 * <ul>
 *   <li><code>a.link</code> - Links with class 'link'</li>
 *   <li><code>.product h3</code> - H3 elements inside elements with class 'product'</li>
 *   <li><code>tr:first-child</code> - First row in table</li>
 *   <li><code>li:nth-child(2)</code> - Second list item</li>
 *   <li><code>input[type='text']</code> - Text input fields</li>
 *   <li><code>div > p</code> - Direct child paragraphs</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>XML Input Required</strong>: The plugin requires valid XML content as input</li>
 *   <li><strong>XPath Syntax</strong>: Uses standard XPath 1.0 syntax with Saxon engine</li>
 *   <li><strong>Namespace Support</strong>: Supports XML namespaces in XPath expressions</li>
 *   <li><strong>Error Handling</strong>: Invalid XPath expressions will throw PluginException</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <html-to-xml>} - Convert HTML to XML for XPath processing</li>
 *   <li>{@code <xq-expression>} - XQuery expressions for complex XML processing</li>
 *   <li>{@code <regexp-pattern>} - Regular expressions for text extraction</li>
 *   <li>{@code <xml>} - XML manipulation and transformation</li>
 * </ul>
 * 
 * @core This is a core plugin
 * @author WebHarvest Team
 * @since 2.1.0
 */
@CorePlugin(
    elementName = "xpath",
    description = "XPath plugin that provides XPath processing functionality",
    version = "2.1.0"
)
public class XPathPlugin extends AbstractPlugin {

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getId() {
            return "xpath";
        }

        @Override
        public String getName() {
            return "XPath Plugin";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "XPath plugin that provides XPath processing functionality";
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
            return new String[]{"core", "xpath", "xml", "expression", "evaluation", "query"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return XPathPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xpath";
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
                                return "Optional identifier for the xpath element";
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
                                return "expression";
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
                                return "XPath expression to evaluate";
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
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[]{"id", "expression"};
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
                    
                    // Check if expression is provided
                    String expression = configuration.getProperty("expression");
                    if (expression != null && !expression.trim().isEmpty()) {
                        return; // Valid - has expression
                    }
                    
                    // Check if any variable-based attributes are provided
                    for (String key : configuration.getAllProperties().keySet()) {
                        if (key.startsWith("v:")) {
                            return; // Valid - has variable-based attribute
                        }
                    }
                    
                    // Invalid - no expression and no variables
                    throw new PluginValidationException("XPath requires \"expression\" or at least one variable-based attribute!");
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
        // Validate using schema
        getConfigurationSchema().validate(configuration);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.XPathPlugin");

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        String expression = config.getProperty("expression");
        String type = config.getProperty("type");  // Bug #13: CSS selector support
        
        if (expression == null || expression.trim().isEmpty()) {
            LOGGER.warn("No XPath expression provided");
            return EmptyVariable.INSTANCE;
        }
        
        // Bug #13: Route to CSS selector if type="css"
        if ("css".equalsIgnoreCase(type)) {
            Variable html = getXmlContent(context);
            if (html == null || html instanceof EmptyVariable) {
                LOGGER.warn("No HTML content for CSS selector");
                return EmptyVariable.INSTANCE;
            }
            return evaluateCssSelector(html.toString(), expression);
        }
        
        // Get XML content from context - using the same logic as original XPathProcessor
        Variable xml = getXmlContent(context);
        if (xml == null || xml instanceof EmptyVariable) {
            LOGGER.warn("No XML content provided for XPath evaluation");
            return EmptyVariable.INSTANCE;
        }
        
        LOGGER.debug("Evaluating XPath expression: {} on XML content", expression);
        
        try {
            // v2.2.x ULTRA-LEAN: Create StaticQueryContext directly (RuntimeConfig removed)
            Configuration saxonConfig = new Configuration();
            StaticQueryContext sqc = new StaticQueryContext(saxonConfig);
            
            DynamicQueryContext dynamicContext = new DynamicQueryContext(saxonConfig);
            StringReader reader = new StringReader(xml.toString());
            
            dynamicContext.setContextItem(sqc.buildDocument(new StreamSource(reader)));
            
            // Process XPath variables if any  
            Map<String, String> varMap = getVariableMap(getConfiguration());
            Map<String, String> evaluatedVarMap = new HashMap<String, String>();
            for (Map.Entry<String, String> attEntry : varMap.entrySet()) {
                String varName = attEntry.getKey();
                String varValue = attEntry.getValue();
                evaluatedVarMap.put(varName, varValue);
                // Log debug information instead of setting properties
                LOGGER.debug("XPathPlugin: variable {} = {}", varName, varValue);
            }
            
            // Process XPath variables
            String xpathExpression = null;
            for (Map.Entry<String, String> attEntry : evaluatedVarMap.entrySet()) {
                String varName = attEntry.getKey();
                xpathExpression = attEntry.getValue();
                // v2.2.x ULTRA-LEAN: Compile XQuery directly (XQueryExpressionPool removed)
                XQueryExpression exp = sqc.compileQuery(xpathExpression);
                ListVariable xpathResult = XmlUtil.createListOfXmlNodes(exp, dynamicContext);
                context.setLocalVar(varName, xpathResult);
            }
            
            // Evaluate main XPath expression
            if (expression != null) {
                xpathExpression = expression;
                // v2.2.x ULTRA-LEAN: Compile XQuery directly (XQueryExpressionPool removed)
                XQueryExpression exp = sqc.compileQuery(xpathExpression);
                return XmlUtil.createListOfXmlNodes(exp, dynamicContext);
            } else {
                return EmptyVariable.INSTANCE;
            }
            
        } catch (XPathException e) {
            LOGGER.error("Failed to evaluate XPath expression: {}", e.getMessage(), e);
            throw new ScraperXPathException("Error parsing XPath expression (XPath = [" + expression + "])!", e);
        } catch (Exception e) {
            LOGGER.error("Failed to evaluate XPath expression: {}", e.getMessage(), e);
            throw new ScraperXPathException("Error parsing XPath expression (XPath = [" + expression + "])!", e);
        }
    }
    
    /**
     * Get XML content from context - using the same logic as original XPathProcessor
     */
    protected Variable getXmlContent(DynamicScopeContext context) {
        try {
            // Execute body content to get XML
            Variable bodyContent = executeBody(context);
            
            if (bodyContent == null || bodyContent instanceof EmptyVariable) {
                LOGGER.warn("No body content available for XPath evaluation");
                return EmptyVariable.INSTANCE;
            }
            
            LOGGER.debug("Retrieved XML content for XPath evaluation");
            return bodyContent;
            
        } catch (Exception e) {
            LOGGER.error("Error getting body content for XPath evaluation", e);
            return EmptyVariable.INSTANCE;
        }
    }
    
    // StaticQueryContext is now created directly in doExecute()
    // private RuntimeConfig getRuntimeConfig(DynamicScopeContext context) {
    //     return new RuntimeConfig();
    // }
    
    /**
     * Get variable map from configuration - placeholder for now
     * TODO: Implement proper variable map handling in new plugin architecture
     */
    private Map<String, String> getVariableMap(PluginConfiguration config) {
        // For now, return empty map
        // In a real implementation, this would extract variables from configuration
        return new HashMap<String, String>();
    }
    
    /**
     * Bug #13: Evaluate CSS selector against HTML content
     * TDD Cycle 2: Minimal implementation to make test pass
     * 
     * @param htmlContent HTML content to query
     * @param cssSelector CSS selector expression
     * @return Variable containing results (null for now - minimal impl)
     */
    protected Variable evaluateCssSelector(String htmlContent, String cssSelector) {
        // TDD GREEN Cycle 3: Real implementation with jsoup
        LOGGER.info("Bug #13: CSS Selector '{}' on HTML content", cssSelector);
        
        try {
            // Parse HTML with jsoup (use fully qualified name to avoid conflict with org.w3c.dom.Document)
            org.jsoup.nodes.Document document = Jsoup.parse(htmlContent);
            
            // Apply CSS selector
            Elements elements = document.select(cssSelector);
            
            if (elements.isEmpty()) {
                LOGGER.debug("No elements matched CSS selector: {}", cssSelector);
                return new NodeVariable("");
            }
            
            // Collect text from all matched elements
            List<Variable> results = new ArrayList<>();
            for (Element element : elements) {
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    results.add(new NodeVariable(text));
                }
            }
            
            LOGGER.debug("CSS Selector matched {} elements", results.size());
            return new ListVariable(results);
            
        } catch (Exception e) {
            LOGGER.error("Error applying CSS selector: " + cssSelector, e);
            throw new RuntimeException("CSS Selector error: " + e.getMessage(), e);
        }
    }
    
}
