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
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of BodyContentProvider.
 * This class provides the standard behavior for processing plugin body content.
 * 
 * @author Robert Bala
 * @since 2.1.0
 */
public class DefaultBodyContentProvider implements BodyContentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBodyContentProvider.class);

    private final IElementDef elementDef;

    public DefaultBodyContentProvider(IElementDef elementDef) {
        this.elementDef = elementDef;
    }

    @Override
    public Variable executeBody(DynamicScopeContext context) throws InterruptedException {
        System.out.println("DefaultBodyContentProvider.executeBody called");
        if (elementDef != null) {
            LOGGER.debug("DefaultBodyContentProvider.executeBody - hasOperations: {}", elementDef.hasOperations());
            if (elementDef.hasOperations()) {
                LOGGER.warn("DefaultBodyContentProvider.executeBody - body operations detected but not supported in this implementation");
                LOGGER.warn("This is a simplified implementation that does not execute child elements");
                // For now, return empty variable as a fallback
                // In a full implementation, this would need proper BodyProcessor integration
                return EmptyVariable.INSTANCE;
            }
        }
        System.out.println("No operations found in element definition or element definition is null");
        return EmptyVariable.INSTANCE;
    }

    @Override
    public Variable getBodyTextContent(DynamicScopeContext context) throws InterruptedException {
        System.out.println("DefaultBodyContentProvider.getBodyTextContent called");
        // Get the element definition to extract text content
        if (elementDef != null && elementDef instanceof WebHarvestPluginDef) {
            WebHarvestPluginDef pluginDef = (WebHarvestPluginDef) elementDef;
            org.webharvest.definition.XmlNode xmlNode = pluginDef.getNode();
            
            // Try to get full XML content including child elements
            String textContent = xmlNode.getText();
            
            // If no direct text, try to get XML representation of children
            if ((textContent == null || textContent.trim().isEmpty()) && xmlNode.getElementList().size() > 0) {
                StringBuilder xmlContent = new StringBuilder();
                for (Object element : xmlNode.getElementList()) {
                    if (element instanceof org.webharvest.definition.XmlNode) {
                        org.webharvest.definition.XmlNode childNode = (org.webharvest.definition.XmlNode) element;
                        // Construct XML representation recursively
                        xmlContent.append(constructXmlString(childNode));
                    } else {
                        xmlContent.append(element.toString());
                    }
                }
                textContent = xmlContent.toString();
            }
            
            if (textContent != null && !textContent.trim().isEmpty()) {
                return new NodeVariable(textContent.trim());
            }
        }
        return EmptyVariable.INSTANCE;
    }

    /**
     * Recursively constructs XML string representation of a node
     */
    private String constructXmlString(org.webharvest.definition.XmlNode node) {
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
