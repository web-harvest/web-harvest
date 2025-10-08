package org.webharvest.definition;

import org.webharvest.runtime.processors.Processor;

/**
 * Simple implementation of IElementDef for testing purposes.
 * This class provides a basic implementation that can be used in tests.
 */
public class SimpleElementDef extends AbstractElementDef {

    public SimpleElementDef(XmlNode node) {
        super(node);
    }

    public String getPluginId() {
        return xmlNode.getQName();
    }

    public String getElementName() {
        return xmlNode.getName();
    }

    public String getUri() {
        return xmlNode.getUri();
    }

    public String getNamespaceURI() {
        return xmlNode.getUri();
    }

    public XmlNode getNode() {
        return xmlNode;
    }

    public Class<? extends Processor> getProcessorClass() {
        // Return a default processor class
        return org.webharvest.runtime.processors.EmptyProcessor.class;
    }

    public String getId() {
        return xmlNode.getAttribute("id");
    }

    public String getShortElementName() {
        return xmlNode.getQName();
    }

    public int getLineNumber() {
        return xmlNode.getLineNumber();
    }

    public int getColumnNumber() {
        return xmlNode.getColumnNumber();
    }

    @Override
    public Processor createPlugin() {
        // Create a simple processor for testing
        try {
            return getProcessorClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating processor: " + e.getMessage(), e);
        }
    }
}
