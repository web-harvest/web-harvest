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

package org.webharvest.plugin.schema;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.plugin.Plugin;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition;
import org.webharvest.plugin.PluginConfigurationSchema.AttributeType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Dynamic XSD schema generator that creates XML schemas from registered plugins.
 * This generator maintains backward compatibility with existing XSD schemas
 * while supporting the new plugin architecture.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class DynamicSchemaGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicSchemaGenerator.class);

    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String WH_NAMESPACE = "http://org.webharvest/schema/2.1/core";
    private static final String WH_NAMESPACE_10 = "http://org.webharvest/schema/1.0/config";

    /**
     * Generates an XSD schema for the given plugins.
     * 
     * @param plugins the plugins to include in the schema
     * @param namespace the target namespace
     * @return the generated XSD schema as a string
     */
    public String generateSchema(Collection<Plugin> plugins, String namespace) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();

            // Create root schema element
            Element schema = doc.createElementNS(XSD_NAMESPACE, "xs:schema");
            schema.setAttribute("xmlns:xs", XSD_NAMESPACE);
            schema.setAttribute("targetNamespace", namespace);
            schema.setAttribute("elementFormDefault", "qualified");
            schema.setAttribute("attributeFormDefault", "unqualified");
            doc.appendChild(schema);

            // Group plugins by namespace
            Map<String, Collection<Plugin>> pluginsByNamespace = groupPluginsByNamespace(plugins);

            // Generate elements for each namespace
            for (Map.Entry<String, Collection<Plugin>> entry : pluginsByNamespace.entrySet()) {
                String pluginNamespace = entry.getKey();
                Collection<Plugin> namespacePlugins = entry.getValue();

                // Create complex type for the root element
                Element rootType = createComplexType(doc, "ConfigType");
                Element rootSequence = doc.createElementNS(XSD_NAMESPACE, "xs:sequence");
                rootType.appendChild(rootSequence);

                // Add choice element for all plugins
                Element choice = doc.createElementNS(XSD_NAMESPACE, "xs:choice");
                choice.setAttribute("minOccurs", "0");
                choice.setAttribute("maxOccurs", "unbounded");
                rootSequence.appendChild(choice);

                // Generate elements for each plugin
                for (Plugin plugin : namespacePlugins) {
                    generatePluginElement(doc, choice, plugin);
                }

                // Create root element
                Element rootElement = doc.createElementNS(XSD_NAMESPACE, "xs:element");
                rootElement.setAttribute("name", "config");
                rootElement.setAttribute("type", "ConfigType");
                schema.appendChild(rootElement);
            }

            // Convert to string
            return documentToString(doc);

        } catch (Exception e) {
            LOG.error("Failed to generate XSD schema", e);
            throw new RuntimeException("Failed to generate XSD schema", e);
        }
    }

    /**
     * Groups plugins by their namespace.
     */
    private Map<String, Collection<Plugin>> groupPluginsByNamespace(Collection<Plugin> plugins) {
        Map<String, Collection<Plugin>> grouped = new TreeMap<>();
        
        for (Plugin plugin : plugins) {
            PluginMetadata metadata = plugin.getMetadata();
            String namespace = metadata.getNamespace();
            
            if (!grouped.containsKey(namespace)) {
                grouped.put(namespace, new java.util.ArrayList<>());
            }
            grouped.get(namespace).add(plugin);
        }
        
        return grouped;
    }

    /**
     * Generates an XSD element for a plugin.
     */
    private void generatePluginElement(Document doc, Element parent, Plugin plugin) {
        PluginMetadata metadata = plugin.getMetadata();
        String elementName = metadata.getElementName();
        
        Element element = doc.createElementNS(XSD_NAMESPACE, "xs:element");
        element.setAttribute("name", elementName);
        parent.appendChild(element);

        // Create complex type for the plugin
        String typeName = elementName.substring(0, 1).toUpperCase() + elementName.substring(1) + "Type";
        Element complexType = createComplexType(doc, typeName);
        
        // Add attributes from configuration schema
        PluginConfigurationSchema schema = metadata.getConfigurationSchema();
        if (schema != null) {
            AttributeDefinition[] attributeDefinitions = schema.getAttributeDefinitions();
            if (attributeDefinitions != null) {
                for (AttributeDefinition attrDef : attributeDefinitions) {
                    Element attribute = doc.createElementNS(XSD_NAMESPACE, "xs:attribute");
                    attribute.setAttribute("name", attrDef.getName());
                    attribute.setAttribute("type", getXsdType(attrDef.getType()));
                    
                    if (attrDef.isRequired()) {
                        attribute.setAttribute("use", "required");
                    } else {
                        attribute.setAttribute("use", "optional");
                    }
                    
                    if (attrDef.getDefaultValue() != null) {
                        attribute.setAttribute("default", attrDef.getDefaultValue());
                    }
                    
                    complexType.appendChild(attribute);
                }
            }
        }

        // Add sequence for child elements
        Element sequence = doc.createElementNS(XSD_NAMESPACE, "xs:sequence");
        sequence.setAttribute("minOccurs", "0");
        sequence.setAttribute("maxOccurs", "unbounded");
        
        // Add choice for child plugins (simplified)
        Element choice = doc.createElementNS(XSD_NAMESPACE, "xs:choice");
        choice.setAttribute("minOccurs", "0");
        choice.setAttribute("maxOccurs", "unbounded");
        sequence.appendChild(choice);
        
        complexType.appendChild(sequence);

        element.setAttribute("type", typeName);
    }

    /**
     * Creates a complex type element.
     */
    private Element createComplexType(Document doc, String typeName) {
        Element complexType = doc.createElementNS(XSD_NAMESPACE, "xs:complexType");
        complexType.setAttribute("name", typeName);
        return complexType;
    }

    /**
     * Maps plugin attribute types to XSD types.
     */
    private String getXsdType(AttributeType type) {
        switch (type) {
            case STRING:
                return "xs:string";
            case INTEGER:
                return "xs:integer";
            case BOOLEAN:
                return "xs:boolean";
            case URL:
                return "xs:anyURI";
            case REGEX:
                return "xs:string";
            default:
                return "xs:string";
        }
    }

    /**
     * Converts a DOM document to a string.
     */
    private String documentToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}
