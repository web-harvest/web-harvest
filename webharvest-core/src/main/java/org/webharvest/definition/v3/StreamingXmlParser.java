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

package org.webharvest.definition.v3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.ConfigParser;
import org.webharvest.definition.ConfigSource;
import org.webharvest.definition.IElementDef;
import org.webharvest.exception.ConfigurationException;
import org.webharvest.exception.ParserException;
import org.webharvest.plugin.DefaultPluginConfiguration;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.Plugin;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * New XML parser for Web-Harvest v3.0 - parses XML directly to PluginConfiguration.
 * 
 * <h3>Architecture Revolution - No More *Def Classes!</h3>
 * <p>
 * This parser eliminates the intermediate *Def layer (HttpDef, XPathDef, FileDef, etc.)
 * and creates PluginConfiguration objects directly from XML.
 * </p>
 * 
 * <h3>Comparison with Old Architecture</h3>
 * <pre>
 * OLD (v2.x):
 *   XML → SAXConfigParser → XmlNode → DefinitionResolver → *Def → Plugin
 *   (6 layers, 30+ *Def classes)
 * 
 * NEW (v3.0):
 *   XML → StreamingXmlParser → PluginConfiguration → Plugin
 *   (3 layers, no *Def classes!)
 * </pre>
 * 
 * <h3>How It Works</h3>
 * <pre>{@code
 * // XML:
 * <config>
 *     <http url="http://example.com" method="GET"/>
 * </config>
 * 
 * // OLD way creates:
 * XmlNode("http") → HttpDef → HttpPlugin
 * 
 * // NEW way creates:
 * PluginConfiguration("http", {url: "...", method: "GET"}) → HttpPlugin directly!
 * }</pre>
 * 
 * <h3>Usage</h3>
 * <pre>{@code
 * PluginRegistry registry = ...;  // injected
 * StreamingXmlParser parser = new StreamingXmlParser(registry);
 * PluginConfigurationTree tree = parser.parseToTree(configSource);
 * 
 * // Execute:
 * PluginConfiguration rootConfig = tree.getConfiguration();
 * Plugin rootPlugin = createPlugin(rootConfig);
 * rootPlugin.execute(context);
 * }</pre>
 * 
 * @author Web-Harvest Team
 * @since 3.0.0
 * @see PluginConfigurationTree
 * @see ConfigurationBuilder
 */
public class StreamingXmlParser implements ConfigParser {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingXmlParser.class);
    
    private static final String CORE_NAMESPACE_21 = "http://org.webharvest/schema/2.1/core";
    private static final String CORE_NAMESPACE_10 = "http://org.webharvest/schema/1.0/config";
    
    // For POC, we'll use ConfigurableResolver to lookup plugins (like old parser)
    // In full v3.0, we'll use a proper PluginRegistry
    private final org.webharvest.definition.ConfigurableResolver resolver;

    /**
     * Creates a new streaming XML parser.
     * 
     * @param resolver the definition resolver for looking up plugins
     */
    public StreamingXmlParser(org.webharvest.definition.ConfigurableResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("ConfigurableResolver cannot be null");
        }
        this.resolver = resolver;
        LOG.info("StreamingXmlParser initialized");
    }

    /**
     * Parses XML configuration into PluginConfigurationTree.
     * 
     * @param source the configuration source
     * @return the parsed configuration tree
     * @throws ParserException if parsing fails
     */
    public PluginConfigurationTree parseToTree(ConfigSource source) throws ParserException {
        LOG.info("Parsing XML configuration from: {}", source.getLocation());
        
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(false);
            
            SAXParser parser = factory.newSAXParser();
            ConfigurationTreeHandler handler = new ConfigurationTreeHandler(resolver);
            
            org.xml.sax.InputSource inputSource = new org.xml.sax.InputSource(source.getReader());
            parser.parse(inputSource, handler);
            
            PluginConfigurationTree tree = handler.getConfigurationTree();
            LOG.info("Parsing completed successfully. Root: {}", tree);
            
            return tree;
            
        } catch (SAXException e) {
            throw new ParserException("SAX parsing error: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ParserException("IO error reading configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new ParserException("Unexpected error during parsing: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy method - returns IElementDef for backward compatibility.
     * 
     * @param source the configuration source
     * @return the root element definition
     * @throws ParserException if parsing fails
     * @deprecated Use {@link #parseToTree(ConfigSource)} instead
     */
    @Override
    @Deprecated
    public IElementDef parse(ConfigSource source) throws ParserException {
        // For now, throw UnsupportedOperationException
        // In full implementation, we would create an adapter: Tree → IElementDef
        throw new UnsupportedOperationException(
            "StreamingXmlParser does not support old IElementDef interface. " +
            "Use parseToTree() instead, or use SAXConfigParser for legacy support."
        );
    }

    /**
     * SAX handler that builds PluginConfigurationTree from XML events.
     */
    private static class ConfigurationTreeHandler extends DefaultHandler {
        
        private final org.webharvest.definition.ConfigurableResolver resolver;
        private final Stack<PluginConfigurationTree> treeStack;
        private final Stack<StringBuilder> textStack;
        private PluginConfigurationTree rootTree;
        private Locator locator;

        public ConfigurationTreeHandler(org.webharvest.definition.ConfigurableResolver resolver) {
            this.resolver = resolver;
            this.treeStack = new Stack<>();
            this.textStack = new Stack<>();
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        @Override
        public void startElement(String uri, String localName, String qName, 
                                Attributes attributes) throws SAXException {
            
            LOG.debug("Start element: {} ({})", localName, uri);
            
            // Normalize namespace (handle old 1.0 schema)
            String namespace = normalizeNamespace(uri);
            
            // Look up element in resolver (for now, using old system)
            // TODO v3.0: Replace with direct PluginRegistry lookup
            org.webharvest.definition.ElementInfo elementInfo = 
                resolver.getElementInfo(localName, namespace);
            
            // POC FALLBACK: If element not found in resolver (old *Def system),
            // still create PluginConfiguration (new @CorePlugin system doesn't use resolver)
            if (elementInfo == null && !"config".equals(localName)) {
                LOG.info("Element '{}' not found in resolver (old *Def system) - using fallback for POC", localName);
                // This is OK for POC - we're proving we can parse without *Def!
                // In full v3.0, we'll use PluginRegistry which has all @CorePlugin elements
            }
            
            // Extract attributes into map
            Map<String, String> attributesMap = extractAttributes(attributes, namespace);
            
            // Create plugin configuration directly!
            String configId = generateConfigId(localName);
            PluginConfiguration config = new DefaultPluginConfiguration(
                configId,
                namespace,
                localName,
                attributesMap
            );
            
            // Create tree node
            Class<?> pluginClass = elementInfo != null ? elementInfo.getProcessorClass() : null;
            PluginConfigurationTree tree = new PluginConfigurationTree(
                config,
                localName,
                pluginClass
            );
            
            // Set location for error reporting
            if (locator != null) {
                tree.setLocation(locator.getLineNumber(), locator.getColumnNumber());
            }
            
            // Add to parent or set as root
            if (treeStack.isEmpty()) {
                rootTree = tree;
            } else {
                PluginConfigurationTree parent = treeStack.peek();
                parent.addChild(tree);
            }
            
            treeStack.push(tree);
            textStack.push(new StringBuilder());
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (!textStack.isEmpty()) {
                textStack.peek().append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            LOG.debug("End element: {}", localName);
            
            // Get accumulated text content
            String textContent = textStack.pop().toString().trim();
            
            // If element had text content, add it as a property
            if (!textContent.isEmpty() && !treeStack.isEmpty()) {
                PluginConfigurationTree current = treeStack.peek();
                PluginConfiguration config = current.getConfiguration();
                
                // Add text content as special property
                if (config instanceof DefaultPluginConfiguration) {
                    ((DefaultPluginConfiguration) config).setProperty("_textContent", textContent);
                }
            }
            
            treeStack.pop();
        }

        /**
         * Normalizes namespace URI (handles old 1.0 schema aliases).
         */
        private String normalizeNamespace(String uri) {
            if (uri == null || uri.isEmpty()) {
                return CORE_NAMESPACE_10;  // Default to 1.0 for backward compatibility
            }
            if (CORE_NAMESPACE_10.equals(uri) || 
                "http://web-harvest.sourceforge.net/schema/1.0/config".equals(uri)) {
                return CORE_NAMESPACE_10;
            }
            return uri;
        }

        /**
         * Extracts XML attributes into a map.
         */
        private Map<String, String> extractAttributes(Attributes attributes, String namespace) {
            Map<String, String> map = new HashMap<>();
            
            for (int i = 0; i < attributes.getLength(); i++) {
                String attrName = attributes.getLocalName(i);
                String attrValue = attributes.getValue(i);
                
                if (attrName != null && !attrName.isEmpty()) {
                    map.put(attrName, attrValue != null ? attrValue : "");
                }
            }
            
            return map;
        }

        /**
         * Generates unique configuration ID.
         */
        private String generateConfigId(String elementName) {
            return elementName + "_" + System.nanoTime();
        }

        /**
         * Gets the parsed configuration tree.
         */
        public PluginConfigurationTree getConfigurationTree() {
            return rootTree;
        }
    }
}

