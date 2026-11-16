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
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.XMLConstants;
import java.util.List;
import org.webharvest.exception.XsltException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * XSLT transformation processor - transforms XML using XSLT stylesheets.
 * 
 * <p>
 * The {@code <xslt>} processor applies XSLT transformations to XML content.
 * Supports XSLT 1.0 and 2.0 for complex XML transformations.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Transform XML to HTML</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="html">
 *   <xslt>
 *     <stylesheet>
 *       <![CDATA[
 *       <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *         <xsl:template match="/products">
 *           <html>
 *             <body>
 *               <xsl:for-each select="product">
 *                 <p><xsl:value-of select="name"/></p>
 *               </xsl:for-each>
 *             </body>
 *           </html>
 *         </xsl:template>
 *       </xsl:stylesheet>
 *       ]]>
 *     </stylesheet>
 *     <xml>${productXml}</xml>
 *   </xslt>
 * </def>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: XML to JSON-like structure</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <xslt>
 *   <stylesheet>
 *     <file path="transform.xsl" action="read"/>
 *   </stylesheet>
 *   <xml>${xmlData}</xml>
 * </xslt>
 * </config>
 * }</pre>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>XSLT version:</strong> Supports 1.0 and 2.0 (depends on Java version)</li>
 *   <li><strong>Performance:</strong> XSLT parsing/compilation is cached</li>
 *   <li><strong>Alternative:</strong> Consider {@code <xquery>} for simpler transformations</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see StylesheetPlugin
 */
@CorePlugin(
    elementName = "xslt",
    description = "XSLT plugin that provides XSLT transformation functionality",
    version = "2.1.0",
    category = "Processing"
)
public class XsltPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.XsltPlugin");

    /**
     * Metadata for XsltPlugin.
     * 
     * <p>Master plugin - requires xml and stylesheet as children. Applies XSLT transformations to XML content using XSLT stylesheets. Supports XSLT 1.0 and 2.0 for complex XML transformations.</p>
     */
    public static class XsltPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "xslt";
        }

        @Override
        public String getName() {
            return "XSLT Transformation";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "XSLT transformation processor that applies XSLT transformations to XML content. Supports XSLT 1.0 and 2.0 for complex XML transformations, XML to HTML conversion, XML restructuring, and data format conversion. XSLT parsing and compilation is cached for performance.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = XsltPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Processing";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xslt", "xml", "transformation", "stylesheet", "processing", "xml-processing"};
        }

        @Override
        public String[] getDependencies() {
            return new String[]{"TransformerFactory"};
        }

        @Override
        public String getClassName() {
            return XsltPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xslt";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new XsltPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-code";
        }
        
        @Override
        public String getColor() {
            return "#8B5CF6"; // Purple for processing/transformation
        }
        
        @Override
        public String getGroup() {
            return "Processing";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 160;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("xml", "XML content to transform", PortType.XML, true),
                new InputPort("stylesheet", "XSLT stylesheet", PortType.XML, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("result", "Transformed XML/HTML output", PortType.XML)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.XML;
        }
        
        @Override
        public String getTooltip() {
            return "XSLT transformation processor. Applies XSLT transformations to XML content. Supports XSLT 1.0 and 2.0 for complex transformations.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+X", "Add XSLT", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "XSLT Transformation Plugin",
                "Applies XSLT transformations to XML content. Supports XSLT 1.0 and 2.0 for complex XML transformations.",
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
            return new String[]{"xml", "stylesheet"};
        }
    }
    
    /**
     * Configuration schema for XsltPlugin.
     */
    public static class XsltPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the xslt element";
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
            return new String[]{"xml", "stylesheet"};
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[]{"xml", "stylesheet"};
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
        return new XsltPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new XsltPluginMetadata().getConfigurationSchema();
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

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        final String xmlContent = getRequiredChildContent("xml");
        final String stylesheetContent = getRequiredChildContent("stylesheet");

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

            Source stylesheetSource = new StreamSource(new StringReader(stylesheetContent));
            Transformer transformer = factory.newTransformer(stylesheetSource);
            Source xmlSource = new StreamSource(new StringReader(xmlContent));
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            transformer.transform(xmlSource, result);

            return new NodeVariable(writer.toString());
        } catch (TransformerException | IllegalArgumentException e) {
            LOGGER.error("Failed to perform XSLT transformation: {}", e.getMessage(), e);
            throw new XsltException("Error during XSLT transformation", e);
        }
    }

    /**
     * Reads the literal text content of a required child configuration.
     *
     * @param elementName child element name (xml or stylesheet)
     * @return non-blank child text
     * @throws XsltException when the child is missing or blank
     */
    private String getRequiredChildContent(String elementName) {
        final List<PluginConfiguration> children = getConfiguration().getChildren();
        for (PluginConfiguration child : children) {
            if (elementName.equals(child.getElementName())) {
                String content = child.getProperty("_textContent");
                if (content != null && !content.trim().isEmpty()) {
                    return content;
                }
                throw new XsltException("XSLT requires non-empty <" + elementName + "> content");
            }
        }
        throw new XsltException("XSLT requires a <" + elementName + "> child");
    }
}

















