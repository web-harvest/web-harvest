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

package org.webharvest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.Configuration;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.query.DynamicQueryContext;
import net.sf.saxon.query.StaticQueryContext;
import net.sf.saxon.query.XQueryExpression;
import net.sf.saxon.trans.XPathException;

import org.w3c.dom.Document;
import org.webharvest.definition.validation.ResourcePathToURITransformer;
import org.webharvest.definition.validation.SchemaComponentFactory;
import org.webharvest.definition.validation.SchemaResolver;
import org.webharvest.definition.validation.SchemaResourcesPostProcessor;
import org.webharvest.definition.validation.SchemaSource;
import org.webharvest.definition.validation.TransformerPair;
import org.webharvest.definition.validation.URIToSchemaSourceTransformer;
import org.webharvest.plugin.registry.PluginRegistryHolder;
import org.webharvest.plugin.schema.PluginSchemaPostProcessor;
// import org.webharvest.runtime.RuntimeConfig;
import org.webharvest.runtime.variables.ListVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * XML utils - contains common logic for XML handling
 * 
 * <h3>Bug #40: XML Schema Validation Toggle (v2.2.0)</h3>
 * <p>
 * Added system property {@code webharvest.schema.validation} to control
 * whether XML schema validation is enabled. Default is TRUE (enabled).
 * </p>
 * <pre>
 * // Disable schema validation:
 * java -Dwebharvest.schema.validation=false ...
 * 
 * // Or in code:
 * System.setProperty(XmlUtil.SCHEMA_VALIDATION_PROPERTY, "false");
 * </pre>
 * 
 * @author Robert Bala (2025 refactoring)
 */
public class XmlUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtil.class);
    private static final Map<Integer, SAXParserFactory> saxParserFactoryMap;
    
    /**
     * System property for controlling XML schema validation.
     * Default: true (enabled)
     * 
     * @since 2.2.0 (Bug #40)
     */
    public static final String SCHEMA_VALIDATION_PROPERTY = "webharvest.schema.validation";

    static {
        /* Initialize schema resolver lazily to avoid IOC issues */
        initializeSchemaResolver();

        final HashMap<Integer, SAXParserFactory> map = new HashMap<Integer, SAXParserFactory>();
        SAXParserFactory factory;

        // non-validating ns-unaware
        factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        map.put(0, factory);

        // validating ns-unaware
        /*factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(false);
        map.put(1, factory);*/

        // non-validating ns-aware
        factory = SAXParserFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        map.put(2, factory);

        // validating ns-aware
        /*factory = SAXParserFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        map.put(3, factory);*/

        saxParserFactoryMap = Collections.unmodifiableMap(map);
    }

    private static final ThreadLocal<DocumentBuilder> documentBuilderTL =
            new ThreadLocal<DocumentBuilder>() {
                @Override
                protected DocumentBuilder initialValue() {
                    try {
                        return DocumentBuilderFactory.newInstance().newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };

    private static final ThreadLocal<XPath> xPathBuilderTL =
            new ThreadLocal<XPath>() {
                @Override
                protected XPath initialValue() {
                    return XPathFactory.newInstance().newXPath();
                }
            };

    public static void prettyPrintXml(Document doc, Writer writer) {
        try {
            final Transformer serializer = TransformerFactory.newInstance().newTransformer();
            serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            serializer.setOutputProperty(OutputKeys.METHOD, "xml");
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T evaluateXPath(String xPathExpression, Document document) {
        try {
            return (T) xPathBuilderTL.get().evaluate(xPathExpression, document);
        } catch (XPathExpressionException e) {
            throw Assert.shouldNeverHappen(e);
        }
    }

    public static Document parse(InputSource is) {
        try {
            return documentBuilderTL.get().parse(is);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String prettyPrintXml(String xmlAsString) {
        final StringWriter writer = new StringWriter();
        prettyPrintXml(parse(new InputSource(new StringReader(xmlAsString))), writer);
        return writer.toString();
    }

    /**
     * Evaluates specified XPath expression against given XML text and using given runtime configuration.
     *
     * @param xpath
     * @param xml
     * @param runtimeConfig
     * @return Instance of ListVariable that contains results.
     * @throws XPathException
     */
    // public static ListVariable evaluateXPath(String xpath, String xml, RuntimeConfig runtimeConfig) throws XPathException {
    //     StaticQueryContext sqc = runtimeConfig.getStaticQueryContext();
    //     Configuration config = sqc.getConfiguration();
    //     XQueryExpression exp = runtimeConfig.getXQueryExpressionPool().getCompiledExpression(xpath);
    //     DynamicQueryContext dynamicContext = new DynamicQueryContext(config);
    //     StringReader reader = new StringReader(xml);
    //     dynamicContext.setContextItem(sqc.buildDocument(new StreamSource(reader)));
    //     return createListOfXmlNodes(exp, dynamicContext);
    // }

    /**
     * Creates list variable of resulting XML nodes.
     *
     * @param exp
     * @param dynamicContext
     * @return
     * @throws XPathException
     */
    public static ListVariable createListOfXmlNodes(XQueryExpression exp, DynamicQueryContext dynamicContext) throws XPathException {
        final SequenceIterator iter = exp.iterator(dynamicContext);
        final ListVariable listVariable = new ListVariable();

        for (Item item = iter.next(); item != null; item = iter.next()) {
            listVariable.addVariable(new NodeVariable(
                    new XmlNodeWrapper(item, exp.getExecutable().getDefaultOutputProperties())));
        }

        return listVariable;
    }

    /**
     * Gets a SAXParserFactory with the specified configuration.
     * 
     * <p>
     * <strong>Bug #40 Fix (v2.2.0):</strong> The validating parameter is now checked
     * against the system property {@code webharvest.schema.validation}. If validation
     * is disabled via system property, a non-validating factory is returned regardless
     * of the validating parameter.
     * </p>
     * 
     * @param validating whether validation is requested
     * @param nsAware whether namespace awareness is needed
     * @return configured SAXParserFactory
     * @since 2.2.0 (Bug #40 - validation toggle)
     */
    public static SAXParserFactory getSAXParserFactory(boolean validating, boolean nsAware) {
        // Bug #40 Fix: Check system property for schema validation
        boolean effectiveValidating = validating && isSchemaValidationEnabled();
        return saxParserFactoryMap.get((effectiveValidating ? 1 : 0) | (nsAware ? 1 : 0) << 1);
    }
    
    /**
     * Checks if XML schema validation is enabled.
     * 
     * <p>
     * <strong>Bug #40 Feature (v2.2.0):</strong> Reads system property 
     * {@code webharvest.schema.validation} to determine if schema validation
     * should be enabled. Default is TRUE (enabled).
     * </p>
     * 
     * <p>
     * Accepted values for enabling: "true", "TRUE", "yes", "YES", "1"<br>
     * Accepted values for disabling: "false", "FALSE", "no", "NO", "0"<br>
     * Default (if property not set): true (enabled)
     * </p>
     * 
     * @return true if schema validation is enabled, false otherwise
     * @since 2.2.0 (Bug #40)
     */
    public static boolean isSchemaValidationEnabled() {
        String propertyValue = System.getProperty(SCHEMA_VALIDATION_PROPERTY);
        
        // If property not set, default to TRUE (enabled)
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            return true;
        }
        
        // Parse property value
        String normalized = propertyValue.trim().toLowerCase();
        
        // True values: true, yes, 1
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
            return true;
        }
        
        // False values: false, no, 0
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized)) {
            return false;
        }
        
        // Unknown value - log warning and default to true
        LOGGER.warn("Unknown value for {}: '{}'. Defaulting to true (enabled)", 
                    SCHEMA_VALIDATION_PROPERTY, propertyValue);
        return true;
    }
    
    /**
     * Initialize schema resolver lazily to avoid IOC issues.
     * This method is called from static block but can be safely called multiple times.
     */
    private static void initializeSchemaResolver() {
        try {
            /* Resolving core XML schemas for version 1.0, 2.0 and 2.1. */
            final SchemaResolver schemaResolver =
                SchemaComponentFactory.getSchemaResolver();
            
            // Add static schema post-processor for core schemas
            schemaResolver.addPostProcessor(
                    new SchemaResourcesPostProcessor<String>(
                            new TransformerPair<String, URI, SchemaSource>(
                                    new ResourcePathToURITransformer(),
                                    new URIToSchemaSourceTransformer()),
                            "/config.xsd", "/wh-core-2.0.xsd",
                            "/wh-core-2.1-loose.xsd",
                            "/wh-jndi-2.1.xsd"));
            
            // Add dynamic plugin schema post-processor
            schemaResolver.addPostProcessor(
                    new PluginSchemaPostProcessor(PluginRegistryHolder.getInstance()));
            
            schemaResolver.refresh();
        } catch (Exception e) {
            // Log the error but don't fail the class loading
            // This allows the application to continue even if schema initialization fails
            LOGGER.error("Warning: Failed to initialize XML schema resolver: " + e.getMessage());
        }
    }
    
    /**
     * Force refresh of the schema resolver to pick up XSD changes.
     */
    public static void refreshSchemaResolver() {
        try {
            final SchemaResolver schemaResolver =
                SchemaComponentFactory.getSchemaResolver();
            schemaResolver.refresh();
        } catch (Exception e) {
            LOGGER.error("Warning: Failed to refresh XML schema resolver: " + e.getMessage());
        }
    }
}
