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
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML content wrapper - returns XML text as a {@link NodeVariable}.
 *
 * <p>Literal XML must be supplied as text (normally CDATA). Raw nested element
 * tags are parsed as child plugin configurations by the streaming XML parser;
 * they are not treated as literal markup by this plugin. For variable
 * interpolation, use a {@code <template>} child whose content is CDATA.</p>
 *
 * <h3>Example: literal XML</h3>
 * <pre>{@code
 * <def var="xmlData">
 *   <xml><![CDATA[
 *     <products>
 *       <product id="1"><name>Laptop</name></product>
 *       <product id="2"><name>Mouse</name></product>
 *     </products>
 *   ]]></xml>
 * </def>
 * }</pre>
 *
 * <h3>Example: generated XML with variables</h3>
 * <pre>{@code
 * <def var="productName">WebHarvest Pro</def>
 * <def var="productXml">
 *   <xml>
 *     <template><![CDATA[
 *       <product><name>${productName}</name></product>
 *     ]]></template>
 *   </xml>
 * </def>
 * }</pre>
 *
 * <h3>Return value and limitations</h3>
 * <p>Returns a {@code NodeVariable} containing the supplied or generated XML
 * text. This wrapper does not parse, schema-validate or otherwise validate the
 * XML. Use an XML transformation/query plugin when actual XML parsing or
 * validation is required.</p>
 *
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 */
@CorePlugin(
    elementName = "xml",
    description = "XML plugin that provides XML processing functionality",
    version = "2.1.0"
)
public class XMLPlugin extends AbstractPlugin {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.XMLPlugin");

    /**
     * Metadata for XMLPlugin.
     * 
     * <p>Standalone plugin - wraps XML content for explicit declaration and processing.</p>
     */
    public static class XMLPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "xml";
        }

        @Override
        public String getName() {
            return "XML";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Wraps XML content for explicit declaration and processing. Rarely needed in modern configurations - most XML is processed directly without this wrapper. Useful when you need to ensure XML content is properly parsed and validated.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = XMLPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "xml", "processing", "container", "markup", "xml-json", "data-transformation"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return XMLPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "xml";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new XMLPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-file-code";
        }
        
        @Override
        public String getColor() {
            return "#F59E0B"; // Amber for XML/JSON transformation
        }
        
        @Override
        public String getGroup() {
            return "XML & JSON";
        }
        
        @Override
        public int getDefaultWidth() {
            return 200;
        }
        
        @Override
        public int getDefaultHeight() {
            return 120;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("content", "XML content to wrap (from body content)", PortType.XML, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Wrapped XML content", PortType.XML)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.XML;
        }
        
        @Override
        public String getTooltip() {
            return "Wraps XML content for explicit declaration and processing. Rarely needed in modern configurations - most XML is processed directly without this wrapper. Useful when you need to ensure XML content is properly parsed and validated.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+X", "Add XML", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "XML Plugin",
                "Wraps XML content for explicit declaration and processing. Useful when you need to ensure XML content is properly parsed and validated.",
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
            return new String[0]; // Can contain any child plugins for XML content generation
        }
    }
    
    /**
     * Configuration schema for XMLPlugin.
     */
    public static class XMLPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the xml element";
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
        public String[] getRequiredAttributes() {
            return new String[0];
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id"};
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[0]; // Can contain any child plugins for XML content generation
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
            return "2.3.0";
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
        return new XMLPluginMetadata();
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return new XMLPluginMetadata().getConfigurationSchema();
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

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("XMLPlugin.doExecute: START");
        PluginConfiguration config = getConfiguration();

        String xmlContent;
        if (config.hasChildren()) {
            // Child plugins (for example <template>) generate the XML text.
            Variable bodyContent = executeBody(context);
            if (bodyContent == null || bodyContent instanceof EmptyVariable) {
                return EmptyVariable.INSTANCE;
            }
            xmlContent = bodyContent.toString();
        } else {
            // The streaming parser stores literal text/CDATA here. Raw nested
            // elements are child configurations, not literal XML text.
            xmlContent = config.getProperty("_textContent", "");
        }

        if (xmlContent == null || xmlContent.trim().isEmpty()) {
            LOGGER.warn("No XML content provided for XML plugin");
            return EmptyVariable.INSTANCE;
        }

        LOGGER.debug("Returning XML content with length: {}", xmlContent.length());
        return new NodeVariable(xmlContent);
    }
}
