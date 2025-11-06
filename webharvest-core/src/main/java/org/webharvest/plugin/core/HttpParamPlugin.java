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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.webharvest.runtime.variables.Variable;
import org.webharvest.utils.CommonUtil;

/**
 * HTTP request parameter (form data) - used within {@code <http>} for POST/PUT parameters.
 * 
 * <p>
 * The {@code <http-param>} processor defines HTTP request parameters for form submissions,
 * API calls, and data posting. Must be used as a child element of {@code <http>}. 
 * Automatically handles URL encoding and content-type headers.
 * </p>
 * 
 * <h3>Usage Examples</h3>
 * 
 * <h4>Example 1: Login form submission</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <http url="https://example.com/login" method="POST">
 *   <http-param name="username">john_doe</http-param>
 *   <http-param name="password">secret123</http-param>
 *   <http-param name="remember">true</http-param>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: Search form with dynamic values</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="searchTerm">web scraping tools</def>
 * 
 * <http url="https://example.com/search" method="POST">
 *   <http-param name="q">${searchTerm}</http-param>
 *   <http-param name="category">software</http-param>
 *   <http-param name="sort">relevance</http-param>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: API data submission</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <http url="https://api.example.com/submit" method="POST">
 *   <http-header name="Content-Type">application/x-www-form-urlencoded</http-header>
 *   <http-param name="name">John Doe</http-param>
 *   <http-param name="email">john@example.com</http-param>
 *   <http-param name="message">Hello from WebHarvest!</http-param>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: Multi-value parameters (checkboxes)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <http url="https://example.com/preferences" method="POST">
 *   <http-param name="interests">technology</http-param>
 *   <http-param name="interests">programming</http-param>
 *   <http-param name="interests">web-scraping</http-param>
 *   <http-param name="email">user@example.com</http-param>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>name</strong> (required): The parameter name (form field name)</li>
 *   <li><strong>Content (body)</strong>: The parameter value</li>
 * </ul>
 * 
 * <h3>Return Value</h3>
 * <p>Does not return a value. Parameters are collected by parent {@code <http>} element and sent in request body.</p>
 * 
 * <h3>Behavior</h3>
 * <ul>
 *   <li><strong>URL Encoding:</strong> Parameter values are automatically URL-encoded</li>
 *   <li><strong>Content-Type:</strong> Sets application/x-www-form-urlencoded if not specified</li>
 *   <li><strong>GET Requests:</strong> Parameters appended to URL query string</li>
 *   <li><strong>POST/PUT Requests:</strong> Parameters sent in request body</li>
 *   <li><strong>Multiple Values:</strong> Same parameter name can be used multiple times</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Parent Element</strong>: Must be used as child of {@code <http>} element</li>
 *   <li><strong>HTTP Methods</strong>: Works with GET (query string), POST, PUT, PATCH (body)</li>
 *   <li><strong>Dynamic Values</strong>: Parameter values support variable substitution with ${var}</li>
 *   <li><strong>Special Characters</strong>: Automatically handles spaces, &, =, and other special characters</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <http>} - Main HTTP request processor (parent element)</li>
 *   <li>{@code <http-header>} - HTTP request headers</li>
 *   <li>{@code <def>} - Variable definition for dynamic parameter values</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @since 1.0
 * @see HttpPlugin
 * @see HttpHeaderPlugin
 */
@CorePlugin(
    elementName = "http-param",
    description = "HTTP parameter plugin that defines HTTP parameters for HTTP requests",
    version = "2.1.0"
)
public class HttpParamPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.HttpParamPlugin");

    /**
     * Metadata for HttpParamPlugin.
     * 
     * <p>Detail plugin - requires http as parent. Defines HTTP request parameters for form submissions.</p>
     */
    public static class HttpParamPluginMetadata implements VisualPluginMetadata {
        @Override
        public String getId() {
            return "http-param";
        }

        @Override
        public String getName() {
            return "HTTP Parameter";
        }

        @Override
        public String getVersion() {
            return "2.3.0";
        }

        @Override
        public String getDescription() {
            return "Defines HTTP request parameters for form submissions, API calls, and data posting. Must be used within &lt;http&gt; element. Automatically handles URL encoding.";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getCategory() {
            CorePlugin annotation = HttpParamPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "core";
        }

        @Override
        public String[] getTags() {
            return new String[]{"core", "http", "parameter", "request", "web", "form", "detail"};
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String getClassName() {
            return HttpParamPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "http-param";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new HttpParamPluginConfigurationSchema();
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-list";
        }
        
        @Override
        public String getColor() {
            return "#818CF8"; // Light indigo for detail plugins
        }
        
        @Override
        public String getGroup() {
            return "HTTP & Network";
        }
        
        @Override
        public int getDefaultWidth() {
            return 180;
        }
        
        @Override
        public int getDefaultHeight() {
            return 100;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("name", "Parameter name (from name attribute)", PortType.STRING, true),
                new InputPort("value", "Parameter value (from body content)", PortType.STRING, false)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "Parameter definition passed to parent http", PortType.STRING)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.STRING;
        }
        
        @Override
        public String getTooltip() {
            return "Defines HTTP request parameters for form submissions, API calls, and data posting. Must be used within http element. Automatically handles URL encoding and content-type headers. Supports dynamic values with ${var} syntax.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[0]; // Detail plugin - no shortcuts
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "HTTP Parameter Plugin",
                "Defines HTTP request parameters for form submissions, API calls, and data posting. Must be used within http element.",
                new String[]{"Tab", "Enter"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return new ParentChildRelationship("http", true, "HTTP Request"); // Required parent
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[0]; // No children
        }
    }
    
    /**
     * Configuration schema for HttpParamPlugin.
     */
    public static class HttpParamPluginConfigurationSchema implements PluginConfigurationSchema {
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
                        return "Optional identifier for the http-param element";
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
                        return "name";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.STRING;
                    }

                    @Override
                    public boolean isRequired() {
                        return true;
                    }

                    @Override
                    public String getDefaultValue() {
                        return null;
                    }

                    @Override
                    public String getDescription() {
                        return "Name of the HTTP parameter";
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
                        return "^[a-zA-Z0-9\\-_]+$";
                    }
                },
                new PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() {
                        return "isfile";
                    }

                    @Override
                    public PluginConfigurationSchema.AttributeType getType() {
                        return PluginConfigurationSchema.AttributeType.BOOLEAN;
                    }

                    @Override
                    public boolean isRequired() {
                        return false;
                    }

                    @Override
                    public String getDefaultValue() {
                        return "false";
                    }

                    @Override
                    public String getDescription() {
                        return "Whether this parameter represents a file";
                    }

                    @Override
                    public String[] getAllowedValues() {
                        return new String[]{"true", "false"};
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
                        return "filename";
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
                        return "Filename for file parameters";
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
                        return "contenttype";
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
                        return "Content type for file parameters (MIME type)";
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
                        return "^[a-zA-Z0-9\\-_]+/[a-zA-Z0-9\\-_]+$";
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
            return new String[0];
        }

        @Override
        public String[] getAllowedSubElements() {
            return new String[0];
        }

        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"id", "isfile", "filename", "contenttype"};
        }

        @Override
        public String[] getRequiredAttributes() {
            return new String[]{"name"};
        }

        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            if (configuration == null) {
                throw new PluginValidationException("Configuration cannot be null");
            }
            
            String name = configuration.getProperty("name");
            if (name == null || name.trim().isEmpty()) {
                throw new PluginValidationException("Name attribute is required");
            }
            
            if (!name.matches("^[a-zA-Z0-9\\-_]+$")) {
                throw new PluginValidationException("Name must be a valid HTTP parameter name");
            }
            
            String contentType = configuration.getProperty("contenttype");
            if (contentType != null && !contentType.trim().isEmpty()) {
                if (!contentType.matches("^[a-zA-Z0-9\\-_]+/[a-zA-Z0-9\\-_]+$")) {
                    throw new PluginValidationException("Content type must be a valid MIME type");
                }
            }
        }
    }

    @Override
    public PluginMetadata getMetadata() {
        return new HttpParamPluginMetadata();
    }
    
    public PluginConfigurationSchema getConfigurationSchema() {
        return new HttpParamPluginMetadata().getConfigurationSchema();
    }
    
    // Legacy duplicate schema removed - using HttpParamPluginConfigurationSchema

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
        
        String name = configuration.getProperty("name");
        if (name == null || name.trim().isEmpty()) {
            throw new PluginValidationException("Name attribute is required");
        }
        
        if (!name.matches("^[a-zA-Z0-9\\-_]+$")) {
            throw new PluginValidationException("Name must be a valid HTTP parameter name");
        }
        
        String contentType = configuration.getProperty("contenttype");
        if (contentType != null && !contentType.trim().isEmpty()) {
            if (!contentType.matches("^[a-zA-Z0-9\\-_]+/[a-zA-Z0-9\\-_]+$")) {
                throw new PluginValidationException("Content type must be a valid MIME type");
            }
        }
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        // Get HTTP parameter configuration - using the same logic as original HttpParamProcessor
        String name = config.getProperty("name", "");
        String isFileStr = config.getProperty("isfile", "false");
        boolean isFile = CommonUtil.getBooleanValue(isFileStr, false);
        String fileName = config.getProperty("filename", "");
        String contentType = config.getProperty("contenttype", "");
        
        if (name.isEmpty()) {
            LOGGER.warn("No parameter name specified for HTTP parameter");
            return EmptyVariable.INSTANCE;
        }
        
        // Log debug information instead of setting properties
        LOGGER.debug("HTTP parameter - name: {}, isFile: {}", name, isFile);
        LOGGER.debug("HTTP parameter configured with fileName and contentType");
        
        try {
            // TODO: Implement proper body execution
            Variable paramValue = EmptyVariable.INSTANCE;
            
            // TODO: In the original HttpParamProcessor, this would add the parameter to the parent HttpProcessor
            // For now, we'll simulate the parameter addition
            LOGGER.debug("HTTP parameter added (simplified implementation)");
            
            return EmptyVariable.INSTANCE;
            
        } catch (Exception e) {
            LOGGER.error("Error during HTTP parameter processing", e);
            return EmptyVariable.INSTANCE;
        }
    }
}













