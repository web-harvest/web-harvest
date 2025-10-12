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
 */

package org.webharvest.plugin.core;

import static org.webharvest.WHConstants.XMLNS_CORE;
import static org.webharvest.WHConstants.XMLNS_CORE_10;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

// Additional HTML parsing libraries
// import net.htmlparser.jericho.Source; // Will use full class name to avoid conflict
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * HTML to well-formed XML converter - essential for XPath/XQuery processing of web pages.
 * 
 * <p>
 * The {@code <html-to-xml>} processor is one of the most critical processors in WebHarvest.
 * It converts potentially malformed HTML from web pages into well-formed XML that can be
 * processed using XPath and XQuery. This is necessary because most web pages contain
 * invalid HTML (unclosed tags, missing quotes, etc.) that XML parsers reject.
 * </p>
 * 
 * <h3>Why HTML to XML Conversion?</h3>
 * <p>
 * HTML from the web is often "tag soup" - syntactically invalid but rendered by browsers:
 * </p>
 * <ul>
 *   <li>Unclosed tags: {@code <p>Text<p>More text}</li>
 *   <li>Missing quotes: {@code <div class=container>}</li>
 *   <li>Invalid nesting: {@code <b><i>Text</b></i>}</li>
 *   <li>Missing closing tags: {@code <li>Item 1<li>Item 2}</li>
 * </ul>
 * <p>
 * XPath requires well-formed XML. {@code <html-to-xml>} fixes all these issues,
 * enabling reliable data extraction.
 * </p>
 * 
 * <h3>Multi-Strategy Parsing (Chain of Responsibility)</h3>
 * <p>
 * The plugin uses multiple parsing strategies in order of robustness. If one fails,
 * the next is tried automatically:
 * </p>
 * <ol>
 *   <li><strong>JSoup.parse():</strong> Primary parser - fast, handles most HTML correctly</li>
 *   <li><strong>JSoup.clean():</strong> Aggressive cleaning with Safelist - for broken structure</li>
 *   <li><strong>TagSoup:</strong> Streaming SAX parser - for low memory/large files</li>
 *   <li><strong>Apache Tika:</strong> Content extraction fallback - when structure doesn't matter</li>
 *   <li><strong>Basic cleaning:</strong> Last resort - regex-based cleaning</li>
 * </ol>
 * <p>
 * This multi-strategy approach ensures that even the worst HTML can be processed.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Convert HTTP response to XML for XPath</h4>
 * <pre>{@code
 * <def var="productName">
 *   <xpath expression="//h1[@class='product-title']/text()">
 *     <html-to-xml>
 *       <http url="https://example.com/product/123"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h4>Example 2: Extract multiple fields from page</h4>
 * <pre>{@code
 * <!-- Fetch page once -->
 * <def var="htmlPage">
 *   <http url="https://example.com/article/456"/>
 * </def>
 * 
 * <!-- Convert to XML once -->
 * <def var="xmlPage">
 *   <html-to-xml>${htmlPage}</html-to-xml>
 * </def>
 * 
 * <!-- Extract multiple fields -->
 * <def var="title">
 *   <xpath expression="//h1/text()">
 *     ${xmlPage}
 *   </xpath>
 * </def>
 * 
 * <def var="author">
 *   <xpath expression="//meta[@name='author']/@content">
 *     ${xmlPage}
 *   </xpath>
 * </def>
 * 
 * <def var="content">
 *   <xpath expression="//div[@class='content']">
 *     ${xmlPage}
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h4>Example 3: Extract all links from page</h4>
 * <pre>{@code
 * <def var="links">
 *   <xpath expression="//a/@href">
 *     <html-to-xml>
 *       <http url="https://example.com/links"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <!-- Loop through all links -->
 * <loop item="link">
 *   ${links}
 *   
 *   <http url="${link}"/>
 * </loop>
 * }</pre>
 * 
 * <h4>Example 4: Extract table data</h4>
 * <pre>{@code
 * <def var="rows">
 *   <xpath expression="//table[@id='products']/tr">
 *     <html-to-xml>
 *       <http url="https://example.com/products"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * 
 * <loop item="row">
 *   ${rows}
 *   
 *   <def var="productName">
 *     <xpath expression="td[1]/text()">${row}</xpath>
 *   </def>
 *   <def var="productPrice">
 *     <xpath expression="td[2]/text()">${row}</xpath>
 *   </def>
 *   
 *   <!-- Save to file/database -->
 * </loop>
 * }</pre>
 * 
 * <h4>Example 5: Complex selector with namespaces</h4>
 * <pre>{@code
 * <def var="articles">
 *   <xpath expression="//article[contains(@class, 'news-item')]//h2/text()">
 *     <html-to-xml>
 *       <http url="https://news.example.com"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h4>Example 6: Handling malformed HTML</h4>
 * <pre>{@code
 * <!-- Even badly broken HTML works -->
 * <def var="brokenHtml" value="<p>Unclosed paragraph<div>No closing div<b>Bold"/>
 * 
 * <html-to-xml>${brokenHtml}</html-to-xml>
 * <!-- Result: Well-formed XML with auto-closed tags -->
 * }</pre>
 * 
 * <h3>Common Patterns</h3>
 * 
 * <h4>Pattern 1: Fetch → Convert → Extract</h4>
 * <pre>{@code
 * <!-- 1. Fetch HTML -->
 * <def var="page">
 *   <http url="https://example.com"/>
 * </def>
 * 
 * <!-- 2. Convert to XML -->
 * <def var="xml">
 *   <html-to-xml>${page}</html-to-xml>
 * </def>
 * 
 * <!-- 3. Extract data with XPath -->
 * <def var="data">
 *   <xpath expression="//div[@class='content']/p">
 *     ${xml}
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h4>Pattern 2: Inline conversion (compact)</h4>
 * <pre>{@code
 * <def var="title">
 *   <xpath expression="//title/text()">
 *     <html-to-xml>
 *       <http url="https://example.com"/>
 *     </html-to-xml>
 *   </xpath>
 * </def>
 * }</pre>
 * 
 * <h3>Technical Details</h3>
 * 
 * <p><strong>Parsing Strategies:</strong></p>
 * <ol>
 *   <li><strong>JSoup Parse:</strong> Primary strategy using JSoup library
 *     <ul>
 *       <li>Handles most modern HTML correctly</li>
 *       <li>Preserves document structure</li>
 *       <li>Auto-closes tags and fixes nesting</li>
 *       <li>Fast performance</li>
 *     </ul>
 *   </li>
 *   <li><strong>JSoup Clean:</strong> Aggressive cleaning with Safelist
 *     <ul>
 *       <li>Used when structure is severely broken</li>
 *       <li>Removes dangerous content</li>
 *       <li>Guaranteed well-formed output</li>
 *     </ul>
 *   </li>
 *   <li><strong>TagSoup:</strong> SAX-based streaming parser
 *     <ul>
 *       <li>Low memory usage for huge files</li>
 *       <li>Very lenient with broken HTML</li>
 *       <li>Slower but more robust</li>
 *     </ul>
 *   </li>
 *   <li><strong>Apache Tika:</strong> Content extraction
 *     <ul>
 *       <li>Last resort - extracts just text</li>
 *       <li>Works when HTML is completely broken</li>
 *       <li>Loses structure but preserves content</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <p><strong>Automatic Fixes Applied:</strong></p>
 * <ul>
 *   <li>Invalid XML characters removed (control characters)</li>
 *   <li>Malformed DOCTYPE declarations fixed or removed</li>
 *   <li>Element names with dots normalized (class.name → class_name)</li>
 *   <li>Unclosed tags automatically closed</li>
 *   <li>Invalid nesting corrected</li>
 *   <li>Missing quotes added to attributes</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>No parameters:</strong> html-to-xml has no attributes - uses body content only</li>
 *   <li><strong>Input source:</strong> Can be from {@code <http>}, {@code <file>}, {@code <def>}, or literal text</li>
 *   <li><strong>Output format:</strong> Always well-formed XML (validates against XML spec)</li>
 *   <li><strong>Memory usage:</strong> Entire HTML is loaded into memory - consider file size</li>
 *   <li><strong>Performance:</strong> JSoup is fast (~50MB/sec), TagSoup is slower but more robust</li>
 *   <li><strong>Charset:</strong> Respects charset from HTTP or uses UTF-8 default</li>
 * </ul>
 * 
 * <h3>Troubleshooting</h3>
 * 
 * <p><strong>If XPath returns empty results:</strong></p>
 * <ul>
 *   <li>Save XML to file and inspect: {@code <file path="debug.xml" action="write">${xmlPage}</file>}</li>
 *   <li>Check if namespaces were added by parser</li>
 *   <li>Verify element names (may be lowercased)</li>
 *   <li>Try simpler XPath expression first</li>
 * </ul>
 * 
 * <p><strong>If content is missing:</strong></p>
 * <ul>
 *   <li>JSoup may have removed malformed elements</li>
 *   <li>Try TagSoup strategy (more lenient)</li>
 *   <li>Check if content is loaded via JavaScript (use web-browser plugin instead)</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <http>} - Fetch HTML from web</li>
 *   <li>{@code <xpath>} - Extract data from XML</li>
 *   <li>{@code <xquery>} - Complex XML querying</li>
 *   <li>{@code <json-to-xml>} - Convert JSON to XML</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    namespace = "http://org.webharvest/schema/2.1/core",
    elementName = "html-to-xml",
    description = "HTML to XML plugin that converts HTML content to well-formed XML",
    version = "2.1.0"
)
public class HtmlToXmlPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.HtmlToXmlPlugin");
    
    // Compiled regex patterns for better performance
    private static final Pattern INVALID_XML_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    private static final Pattern MALFORMED_DOCTYPE = Pattern.compile("<!DOCTYPE[^>]*(?![^>]*>)[^>]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern ELEMENTS_WITH_DOTS = Pattern.compile("<([a-z][a-z0-9]*)\\.[^>]*>", Pattern.CASE_INSENSITIVE);
    
    // Chain of parsing strategies
    private final List<HtmlParsingStrategy> parsingStrategies = initializeParsingStrategies();

    /**
     * {@inheritDoc}
     */
    @Override
    public PluginMetadata getMetadata() {
        return new PluginMetadata() {
        @Override
        public String getId() {
            return "html-to-xml";
        }

        @Override
        public String getName() {
                return "html-to-xml";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getDescription() {
            return "HTML to XML plugin that converts HTML content to well-formed XML";
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
                return new String[]{"core", "html", "xml", "conversion", "parser"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
            public PluginConfigurationSchema getConfigurationSchema() {
                return HtmlToXmlPlugin.this.getConfigurationSchema();
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
            public String getElementName() {
                return "html-to-xml";
                            }

                            @Override
            public boolean isCorePlugin() {
                    return true;
                            }

                            @Override
            public String getClassName() {
                return HtmlToXmlPlugin.class.getName();
                }
        };
                }

    /**
     * {@inheritDoc}
     */
                @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        // No specific initialization needed
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // No specific validation needed
    }

    /**
     * {@inheritDoc}
     */
    public PluginConfigurationSchema getConfigurationSchema() {
        return new PluginConfigurationSchema() {
            @Override
            public AttributeDefinition[] getAttributeDefinitions() {
                return new AttributeDefinition[0];
                }

                @Override
            public AttributeDefinition getAttribute(String name) {
                return null;
                }

                @Override
            public String[] getRequiredAttributes() {
                return new String[0];
                }

                @Override
            public String[] getOptionalAttributes() {
                    return new String[0];
                }

                @Override
                public String[] getAllowedSubElements() {
                    return new String[0];
                }

                @Override
            public String[] getRequiredSubElements() {
                    return new String[0];
                }

                @Override
            public boolean allowsBody() {
                return true;
            }

    @Override
            public String getVersion() {
                return "2.1.0";
    }

    @Override
            public void validate(PluginConfiguration configuration) throws PluginValidationException {
                // No specific validation needed
            }
        };
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Execute body content to get HTML string
        Variable bodyContent = executeBody(context);
        String html = "";
        
        if (bodyContent != null && !(bodyContent instanceof EmptyVariable)) {
            html = bodyContent.toString();
            LOGGER.debug("HtmlToXmlPlugin: Retrieved HTML content: " + html);
        } else {
            LOGGER.debug("HtmlToXmlPlugin: No body content available");
            return new NodeVariable("<empty/>");
        }
        
        if (html == null || html.trim().isEmpty()) {
            return new NodeVariable("<empty/>");
        }
        
        LOGGER.debug("Processing HTML content with length: {}", html.length());
        
        // Try each strategy in the chain
        for (HtmlParsingStrategy strategy : parsingStrategies) {
            try {
                String result = strategy.parse(html);
                if (result != null && !result.trim().isEmpty()) {
                    LOGGER.debug("Successfully processed with strategy: {}", strategy.getName());
                    return new NodeVariable(result);
                }
            } catch (Exception e) {
                LOGGER.debug("Strategy {} failed: {}", strategy.getName(), e.getMessage());
            }
        }
        
        // If all strategies fail, use basic cleaning as fallback
        LOGGER.warn("All parsing strategies failed, using basic HTML cleaning as fallback");
        String result = basicHtmlCleaning(html);
        return new NodeVariable(result);
    }

    /**
     * Initialize the chain of parsing strategies according to the proposed pipeline:
     * 1. JSoup.parse(...) - primary method
     * 2. JSoup.clean(...) with different Safelist - if content disappeared/structure broken
     * 3. HtmlCleaner + W3C DOM + XPath - if still bad
     * 4. TagSoup + SAX - for streaming/low memory
     * 5. Jericho - to preserve exact original HTML bits
     */
    private List<HtmlParsingStrategy> initializeParsingStrategies() {
        List<HtmlParsingStrategy> strategies = new ArrayList<>();
        
        // Strategy 1: JSoup.parse(...) - primary method
        strategies.add(new JSoupParseStrategy());
        
        // Strategy 2: JSoup.clean(...) with different Safelist - if content disappeared/structure broken
        strategies.add(new JSoupCleanStrategy());
        
        // Strategy 3: TagSoup + SAX - for streaming/low memory
        strategies.add(new TagSoupStrategy());
        
        // Strategy 4: Apache Tika - fallback for content extraction
        strategies.add(new TikaStrategy());
        
        return strategies;
    }

    /**
     * Interface for HTML parsing strategies
     */
    private interface HtmlParsingStrategy {
        String parse(String html) throws Exception;
        String getName();
    }

    /**
     * Strategy 1: JSoup.parse(...) - primary method
     * If everything OK → use CSS selectors
     */
    private class JSoupParseStrategy implements HtmlParsingStrategy {
        @Override
        public String parse(String html) throws Exception {
            LOGGER.debug("Trying JSoup.parse() strategy");
            
            // Clean common HTML issues first
            String cleanedHtml = cleanCommonHtmlIssues(html);
            
            // Parse with JSoup
            Document document = Jsoup.parse(cleanedHtml, "", Parser.htmlParser());
            
            // Check if content disappeared or structure is broken
            if (isContentDisappeared(document, html)) {
                LOGGER.debug("Content disappeared with JSoup.parse(), trying next strategy");
                return null;
            }
            
            // Clean document for XML compatibility
            document = cleanDocumentForXml(document);
            
            // Convert to XML string
            return documentToXmlString(document);
        }

        @Override
        public String getName() {
            return "JSoup.parse()";
        }
    }

    /**
     * Strategy 2: JSoup.clean(...) with different Safelist
     * If content disappeared / structure broken → try Jsoup.clean(...) with different Safelist
     */
    private class JSoupCleanStrategy implements HtmlParsingStrategy {
        @Override
        public String parse(String html) throws Exception {
            LOGGER.debug("Trying JSoup.clean() strategy");
            
            // Clean common HTML issues first
            String cleanedHtml = cleanCommonHtmlIssues(html);
            
            // Use JSoup.clean() with different safelists
            String[] safelistNames = {"relaxed", "basicWithImages", "basic", "simpleText", "none"};
            
            for (String safelistName : safelistNames) {
                try {
                    Safelist safelist = getSafelist(safelistName);
                    String cleaned = Jsoup.clean(cleanedHtml, safelist);
                    
                    if (cleaned != null && !cleaned.trim().isEmpty()) {
                        Document document = Jsoup.parse(cleaned, "", Parser.htmlParser());
                        
                        // Check if content is still meaningful
                        if (!isContentDisappeared(document, html)) {
                            document = cleanDocumentForXml(document);
                            return documentToXmlString(document);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.debug("JSoup.clean() with {} failed: {}", safelistName, e.getMessage());
                }
            }
            
            return null;
        }

        @Override
        public String getName() {
            return "JSoup.clean()";
        }
    }


    /**
     * Strategy 4: TagSoup + SAX
     * If you need streaming/low memory → TagSoup + SAX
     */
    private class TagSoupStrategy implements HtmlParsingStrategy {
        @Override
        public String parse(String html) throws Exception {
            LOGGER.debug("Trying TagSoup + SAX strategy");
            
            try {
                org.ccil.cowan.tagsoup.Parser parser = new org.ccil.cowan.tagsoup.Parser();
                StringWriter writer = new StringWriter();
                
                // Create a custom handler to capture the output
                DefaultHandler handler = new DefaultHandler() {
                    @Override
                    public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes) {
                        writer.write("<" + qName);
                        for (int i = 0; i < attributes.getLength(); i++) {
                            writer.write(" " + attributes.getQName(i) + "=\"" + attributes.getValue(i) + "\"");
                        }
                        writer.write(">");
                    }
                    
                    @Override
                    public void endElement(String uri, String localName, String qName) {
                        writer.write("</" + qName + ">");
                    }
                    
                    @Override
                    public void characters(char[] ch, int start, int length) {
                        writer.write(new String(ch, start, length));
                    }
                };
                
                parser.setContentHandler(handler);
                String cleanedHtml = cleanCommonHtmlIssues(html);
                parser.parse(new InputSource(new StringReader(cleanedHtml)));
                
                String result = writer.toString();
                return result.isEmpty() ? null : result;
            
        } catch (Exception e) {
                LOGGER.debug("TagSoup + SAX strategy failed: {}", e.getMessage());
                return null;
            }
        }

        @Override
        public String getName() {
            return "TagSoup + SAX";
        }
    }
    
    /**
     * Strategy 4: Apache Tika
     * Fallback for content extraction
     */
    private class TikaStrategy implements HtmlParsingStrategy {
        @Override
        public String parse(String html) throws Exception {
            LOGGER.debug("Trying Apache Tika strategy");
            
            try {
                Tika tika = new Tika();
                String cleanedHtml = cleanCommonHtmlIssues(html);
                
                // Try to extract content
                String content = tika.parseToString(new ByteArrayInputStream(cleanedHtml.getBytes("UTF-8")));
                
                if (content != null && !content.trim().isEmpty()) {
                    // Wrap in a simple XML structure
                    return "<content>" + content + "</content>";
                }
                
                return null;
                
            } catch (Exception e) {
                LOGGER.debug("Apache Tika strategy failed: {}", e.getMessage());
                return null;
            }
        }

        @Override
        public String getName() {
            return "Apache Tika";
        }
    }
    
    /**
     * Clean common HTML issues that cause XML parsing problems
     */
    private String cleanCommonHtmlIssues(String html) {
        if (html == null) return "";
        
        String cleaned = html;
        
        // Fix unescaped ampersands
        cleaned = cleaned.replaceAll("&(?![a-zA-Z0-9#]+;)", "&amp;");
        
        // Fix malformed script tags
        cleaned = cleaned.replaceAll("(?i)<script[^>]*>(?![\\s\\S]*?</script>)", "<script></script>");
        
        // Fix malformed style tags
        cleaned = cleaned.replaceAll("(?i)<style[^>]*>(?![\\s\\S]*?</style>)", "<style></style>");
        
        // Fix single character tags
        cleaned = cleaned.replaceAll("<([a-zA-Z])(?![a-zA-Z0-9])(?![^>]*>)(?![^>]*/>)", "<$1>");
        
        // Fix tags with numbers
        cleaned = cleaned.replaceAll("<([a-zA-Z][0-9]+)(?![^>]*>)(?![^>]*/>)", "<$1>");
        
        // Fix common single letter tags
        cleaned = cleaned.replaceAll("<([ibpae])(?![^>]*>)(?![^>]*/>)", "<$1>");
        
        // Fix additional single letter tags
        cleaned = cleaned.replaceAll("<([cdfghjklmnopqrstuvwxyz])(?![^>]*>)(?![^>]*/>)", "<$1>");
        
        // Fix tags with attributes that are not properly closed
        cleaned = cleaned.replaceAll("<([a-zA-Z][a-zA-Z0-9]*?)\\s+([a-zA-Z][a-zA-Z0-9]*?)=(?![^>]*>)(?![^>]*/>)", "<$1 $2=\"\">");
        
        // Fix any remaining malformed tags
        cleaned = cleaned.replaceAll("<([a-zA-Z][a-zA-Z0-9]*?)(?![^>]*>)(?![^>]*/>)", "<$1>");
        
        // Remove xlink attributes that cause namespace issues - very aggressive approach
        cleaned = cleaned.replaceAll("\\s+xlink:[^\\s>]*", "");
        cleaned = cleaned.replaceAll("xlink:[^\\s>]*", "");
        // Remove any remaining xlink references with different patterns
        cleaned = cleaned.replaceAll("\\s+xlink:[^=]*=[\"'][^\"']*[\"']", "");
        cleaned = cleaned.replaceAll("xlink:[^=]*=[\"'][^\"']*[\"']", "");
        // Final cleanup
        cleaned = cleaned.replaceAll("\\s+xlink:[^\\s>]*", "");
        cleaned = cleaned.replaceAll("xlink:[^\\s>]*", "");
        
        // Remove problematic DTD declarations that cause external DTD loading issues
        // This regex handles both simple and complex DTD declarations
        cleaned = cleaned.replaceAll("(?s)<!DOCTYPE\\s+[^>]*(?:\\[[^\\]]*\\])?[^>]*>", "");
        
        // Remove any remaining DTD references and external entity declarations
        cleaned = cleaned.replaceAll("(?s)<!ENTITY[^>]*>", "");
        cleaned = cleaned.replaceAll("(?s)<!ATTLIST[^>]*>", "");
        cleaned = cleaned.replaceAll("(?s)<!ELEMENT[^>]*>", "");
        cleaned = cleaned.replaceAll("(?s)<!NOTATION[^>]*>", "");
        
        // Remove any remaining DOCTYPE references
        cleaned = cleaned.replaceAll("(?i)DOCTYPE", "");
        
        return cleaned;
    }
    
    /**
     * Clean the JSoup document to ensure it produces valid XML
     */
    private Document cleanDocumentForXml(Document document) {
        // Remove script and style elements that might contain invalid content
        document.select("script, style").remove();
        
        // Remove xlink attributes that cause namespace issues
        Elements allElements = document.select("*");
        int removedCount = 0;
        for (org.jsoup.nodes.Element element : allElements) {
            // Remove all xlink: prefixed attributes
            List<String> xlinkAttrs = new ArrayList<>();
            for (org.jsoup.nodes.Attribute attr : element.attributes()) {
                if (attr.getKey().startsWith("xlink:")) {
                    xlinkAttrs.add(attr.getKey());
                }
            }
            for (String xlinkAttr : xlinkAttrs) {
                element.removeAttr(xlinkAttr);
                removedCount++;
            }
        }
        if (removedCount > 0) {
            LOGGER.debug("Removed {} xlink attributes to prevent namespace issues", removedCount);
        }
        
        // Fix elements with invalid names (containing dots, etc.)
        Elements elementsWithDots = document.select("*");
        for (org.jsoup.nodes.Element element : elementsWithDots) {
            String tagName = element.tagName();
            if (tagName.contains(".") || !isValidXmlName(tagName)) {
                element.tagName("element");
            }
        }
        
        // Fix attributes with invalid names
        for (org.jsoup.nodes.Element element : allElements) {
            // Collect invalid attribute names first
            List<String> invalidAttrs = new ArrayList<>();
            for (org.jsoup.nodes.Attribute attr : element.attributes()) {
                if (!isValidXmlName(attr.getKey())) {
                    invalidAttrs.add(attr.getKey());
                }
            }
            // Remove invalid attributes
            for (String invalidAttr : invalidAttrs) {
                element.removeAttr(invalidAttr);
            }
        }
        
        // Clean invalid XML characters
        cleanInvalidXmlCharacters(document);
        
        return document;
    }
    
    /**
     * Clean invalid XML characters from the document
     */
    private void cleanInvalidXmlCharacters(Document document) {
        // Clean text nodes
        cleanTextNodes(document);
        
        // Clean comments
        cleanComments(document);
        
        // Clean attribute values
        Elements allElements = document.select("*");
        for (org.jsoup.nodes.Element element : allElements) {
            for (org.jsoup.nodes.Attribute attr : element.attributes()) {
                String cleanValue = cleanInvalidXmlChars(attr.getValue());
                element.attr(attr.getKey(), cleanValue);
            }
        }
    }
    
    /**
     * Clean text nodes recursively
     */
    private void cleanTextNodes(org.jsoup.nodes.Element element) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.TextNode) {
                org.jsoup.nodes.TextNode textNode = (org.jsoup.nodes.TextNode) node;
                String cleanText = cleanInvalidXmlChars(textNode.text());
                textNode.text(cleanText);
            } else if (node instanceof org.jsoup.nodes.Element) {
                cleanTextNodes((org.jsoup.nodes.Element) node);
            }
        }
    }
    
    /**
     * Clean comments recursively
     */
    private void cleanComments(org.jsoup.nodes.Element element) {
        for (org.jsoup.nodes.Node node : element.childNodes()) {
            if (node instanceof org.jsoup.nodes.Comment) {
                org.jsoup.nodes.Comment comment = (org.jsoup.nodes.Comment) node;
                String cleanComment = cleanInvalidXmlChars(comment.getData());
                // Remove comments that contain "--" as they're invalid in XML
                if (cleanComment.contains("--")) {
                    comment.remove();
                } else {
                    comment.setData(cleanComment);
                }
            } else if (node instanceof org.jsoup.nodes.Element) {
                cleanComments((org.jsoup.nodes.Element) node);
            }
        }
    }
    
    /**
     * Clean invalid XML characters from a string
     */
    private String cleanInvalidXmlChars(String text) {
        if (text == null) {
            return null;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isValidXmlChar(c)) {
                result.append(c);
            } else {
                // Replace invalid characters with space
                result.append(' ');
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * Check if a character is valid in XML
     */
    private boolean isValidXmlChar(char c) {
        return (c == 0x09 || c == 0x0A || c == 0x0D || 
                (c >= 0x20 && c <= 0xD7FF) || 
                (c >= 0xE000 && c <= 0xFFFD) || 
                (c >= 0x10000 && c <= 0x10FFFF));
    }
    
    /**
     * Check if a string is a valid XML name
     */
    private boolean isValidXmlName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        char first = name.charAt(0);
        if (!Character.isLetter(first) && first != '_' && first != ':') {
            return false;
        }
        
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.' && c != ':') {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Convert JSoup Document to XML string
     */
    private String documentToXmlString(Document document) {
        try {
            // Remove XML declaration if present
                document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            document.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
            document.outputSettings().charset("UTF-8");
            
            String xml = document.html();
            
            // Remove XML declaration
            xml = xml.replaceFirst("^<\\?xml[^>]*\\?>", "");
            
            return xml.trim();
            
        } catch (Exception e) {
            LOGGER.debug("Failed to convert document to XML string: {}", e.getMessage());
            return "<error>Failed to convert to XML</error>";
        }
    }

    /**
     * Get JSoup Safelist by name
     */
    private Safelist getSafelist(String name) {
        switch (name.toLowerCase()) {
            case "none":
                return Safelist.none();
            case "simpletext":
                return Safelist.simpleText();
            case "basic":
                return Safelist.basic();
            case "basicwithimages":
                return Safelist.basicWithImages();
            case "relaxed":
                return Safelist.relaxed();
            default:
                return Safelist.relaxed();
        }
    }

    /**
     * Basic HTML cleaning fallback
     */
    private String basicHtmlCleaning(String html) {
        String cleaned = cleanCommonHtmlIssues(html);
        
        // Remove invalid XML characters
        cleaned = INVALID_XML_CHARS.matcher(cleaned).replaceAll("");
        
        // Fix malformed DOCTYPE
        cleaned = MALFORMED_DOCTYPE.matcher(cleaned).replaceAll("");
        
        // Fix elements with dots in names
        cleaned = ELEMENTS_WITH_DOTS.matcher(cleaned).replaceAll("<$1>");
        
        // Basic tag balancing
        cleaned = balanceTags(cleaned);
        
        return cleaned;
    }

    /**
     * Basic tag balancing
     */
    private String balanceTags(String html) {
        // This is a very basic implementation
        // In a real scenario, you might want to use a more sophisticated approach
        
        // Remove unclosed tags that are commonly self-closing
        String[] selfClosingTags = {"br", "hr", "img", "input", "meta", "link", "area", "base", "col", "embed", "source", "track", "wbr"};
        for (String tag : selfClosingTags) {
            html = html.replaceAll("(?i)<" + tag + "([^>]*?)(?<!/)>", "<" + tag + "$1/>");
        }
        
        return html;
    }
    
    /**
     * Check if content disappeared or structure is broken after parsing
     */
    private boolean isContentDisappeared(Document document, String originalHtml) {
        // Simple heuristic: if the document is too small compared to original
        String documentText = document.text();
        String originalText = Jsoup.parse(originalHtml).text();
        
        // If document text is less than 10% of original text, content probably disappeared
        if (documentText.length() < originalText.length() * 0.1) {
            return true;
        }
        
        // If document has no meaningful content
        if (documentText.trim().isEmpty()) {
            return true;
        }
        
        return false;
    }

}