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

package org.webharvest.plugin;

import org.webharvest.definition.IElementDef;
import org.webharvest.definition.WebHarvestPluginDef;
import org.webharvest.definition.CorePluginDef;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.processors.BodyProcessor;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.ioc.InjectorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executable implementation of BodyContentProvider that can execute child elements.
 * This implementation uses BodyProcessor to execute child elements, but handles
 * the ProcessorFactory injection issue by using a different approach.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class ExecutableBodyContentProvider implements BodyContentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExecutableBodyContentProvider.class);

    private final IElementDef elementDef;

    public ExecutableBodyContentProvider(IElementDef elementDef) {
        this.elementDef = elementDef;
    }

    @Override
    public Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("ExecutableBodyContentProvider.executeBody called");
        
        if (elementDef != null && elementDef.hasOperations()) {
            LOGGER.debug("ExecutableBodyContentProvider.executeBody - hasOperations: {}", elementDef.hasOperations());
            
            try {
                // Use BodyProcessor directly to execute child elements
                LOGGER.debug("ExecutableBodyContentProvider.executeBody - creating BodyProcessor via Builder");
                org.webharvest.runtime.processors.BodyProcessor bodyProcessor = 
                    new org.webharvest.runtime.processors.BodyProcessor.Builder(elementDef)
                        .setParentProcessor(null) // Set parent to null to avoid circular dependency
                        .build();
                
                LOGGER.debug("ExecutableBodyContentProvider.executeBody - executing BodyProcessor");
                Variable result = bodyProcessor.execute(context);
                LOGGER.debug("ExecutableBodyContentProvider.executeBody - BodyProcessor result: {}", result);
                return result;
                
            } catch (Exception e) {
                LOGGER.warn("ExecutableBodyContentProvider.executeBody - BodyProcessor failed: {}", e.getMessage());
                LOGGER.warn("Falling back to text content extraction");
                
                // Fallback to text content if PluginProcessorAdapter fails
                Variable fallbackResult = getBodyTextContent(context);
                LOGGER.debug("ExecutableBodyContentProvider.executeBody - fallback result: {}", fallbackResult);
                return fallbackResult;
            }
        }
        
        LOGGER.debug("No operations found in element definition or element definition is null");
        return EmptyVariable.INSTANCE;
    }

    @Override
    public Variable getBodyTextContent(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("ExecutableBodyContentProvider.getBodyTextContent called");
        LOGGER.debug("elementDef: {}", elementDef);
        LOGGER.debug("elementDef instanceof WebHarvestPluginDef: {}", elementDef instanceof WebHarvestPluginDef);
        
        if (elementDef != null) {
            org.webharvest.definition.XmlNode xmlNode = null;
            
            if (elementDef instanceof WebHarvestPluginDef) {
                WebHarvestPluginDef pluginDef = (WebHarvestPluginDef) elementDef;
                xmlNode = pluginDef.getNode();
            } else if (elementDef instanceof CorePluginDef) {
                CorePluginDef coreDef = (CorePluginDef) elementDef;
                xmlNode = coreDef.getNode();
            }
            
            LOGGER.debug("xmlNode: {}", xmlNode);

            if (xmlNode != null) {
                String textContent = extractTextContent(xmlNode);
                LOGGER.debug("extractTextContent result: '{}'", textContent);
                
                if (textContent != null && !textContent.trim().isEmpty()) {
                    LOGGER.debug("Extracted text content: {}", textContent);
                    return new NodeVariable(textContent.trim());
                }
            }
        }
        
        LOGGER.debug("No text content found");
        return EmptyVariable.INSTANCE;
    }

    /**
     * Extracts text content from XML node, including child elements as XML.
     * This method is optimized for template evaluation scenarios.
     * 
     * @param node the XML node
     * @return the text content
     */
    private String extractTextContent(org.webharvest.definition.XmlNode node) {
        if (node == null) {
            return null;
        }

        // First try to get direct text content
        String textContent = node.getText();
        
        // If no direct text content, try to extract from child elements
        if ((textContent == null || textContent.trim().isEmpty()) && 
            node.getElementList().size() > 0) {
            
            StringBuilder xmlContent = new StringBuilder();
            for (Object element : node.getElementList()) {
                if (element instanceof org.webharvest.definition.XmlNode) {
                    org.webharvest.definition.XmlNode childNode = (org.webharvest.definition.XmlNode) element;
                    xmlContent.append(constructXmlString(childNode));
                } else {
                    // This is text content
                    xmlContent.append(element.toString());
                }
            }
            textContent = xmlContent.toString();
        }

        return textContent;
    }

    /**
     * Recursively constructs XML string representation of a node.
     * This is optimized for template evaluation scenarios.
     * 
     * @param node the XML node
     * @return the XML string representation
     */
    private String constructXmlString(org.webharvest.definition.XmlNode node) {
        if (node == null) {
            return "";
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<").append(node.getName());
        
        // Add attributes
        if (node.getAttributes(node.getUri()) != null) {
            for (java.util.Map.Entry<String, String> entry : node.getAttributes(node.getUri()).entrySet()) {
                xml.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
        xml.append(">");

        // Add children - elementList contains both text and child nodes
        if (node.getElementList().size() > 0) {
            for (Object element : node.getElementList()) {
                if (element instanceof org.webharvest.definition.XmlNode) {
                    xml.append(constructXmlString((org.webharvest.definition.XmlNode) element));
                } else {
                    // This is text content
                    xml.append(element.toString());
                }
            }
        }

        xml.append("</").append(node.getName()).append(">");
        return xml.toString();
    }
}
