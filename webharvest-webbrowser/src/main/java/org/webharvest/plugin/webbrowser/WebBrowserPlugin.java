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
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISING OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.plugin.webbrowser;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.exceptions.PluginException;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.EmptyVariable;
import org.webharvest.runtime.variables.NodeVariable;
import org.webharvest.runtime.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebBrowser plugin for browser automation.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
@CorePlugin(elementName = "web-browser")
public class WebBrowserPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebBrowserPlugin.class);

    private static final String ACTION_ATT_NAME = "action";
    private static final String URL_ATT_NAME = "url";
    private static final String WIDTH_ATT_NAME = "width";
    private static final String HEIGHT_ATT_NAME = "height";

    private static final PluginMetadata METADATA = new PluginMetadata() {
        @Override
        public String getName() {
            return "Web Browser Plugin";
        }

        @Override
        public String getDescription() {
            return "Plugin for web browser automation";
        }

        @Override
        public String getVersion() {
            return "2.1.0";
        }

        @Override
        public String getClassName() {
            return WebBrowserPlugin.class.getName();
        }

        @Override
        public boolean isCorePlugin() {
            return true;
        }

        @Override
        public String getElementName() {
            return "webbrowser";
        }

        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }

        @Override
        public String[] getDependencies() {
            return new String[0];
        }

        @Override
        public String[] getTags() {
            return new String[0];
        }

        @Override
        public String getCategory() {
            return "Browser";
        }

        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }

        @Override
        public String getId() {
            return "WebBrowserPlugin";
        }

        @Override
        public PluginConfigurationSchema getConfigurationSchema() {
            return new PluginConfigurationSchema() {
                @Override
                public AttributeDefinition[] getAttributeDefinitions() {
                    return new AttributeDefinition[0];
                }

                @Override
                public String[] getRequiredAttributes() {
                    return new String[] { ACTION_ATT_NAME };
                }

                @Override
                public String[] getOptionalAttributes() {
                    return new String[] { URL_ATT_NAME, WIDTH_ATT_NAME, HEIGHT_ATT_NAME };
                }

                @Override
                public String getVersion() {
                    return "2.1.0";
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
                public AttributeDefinition getAttribute(String name) {
                    return null;
                }

                @Override
                public void validate(PluginConfiguration configuration) throws PluginValidationException {
                    if (configuration == null) {
                        throw new PluginValidationException("Configuration cannot be null");
                    }
                    
                    String[] required = getRequiredAttributes();
                    for (String attr : required) {
                        if (configuration.getProperty(attr) == null || configuration.getProperty(attr).trim().isEmpty()) {
                            throw new PluginValidationException("Required attribute '" + attr + "' is missing or empty");
                        }
                    }
                }
            };
        }
    };

    @Override
    public PluginMetadata getMetadata() {
        return METADATA;
    }

    public PluginConfigurationSchema getConfigurationSchema() {
        return METADATA.getConfigurationSchema();
    }

    @Override
    protected void doInitialize(PluginConfiguration configuration) throws PluginException {
        doValidateConfiguration(configuration);
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        if (configuration == null) {
            throw new PluginValidationException("Configuration cannot be null");
        }
        getConfigurationSchema().validate(configuration);
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        PluginConfiguration config = getConfiguration();
        
        String action = config.getProperty(ACTION_ATT_NAME);
        String url = config.getProperty(URL_ATT_NAME, "");
        String width = config.getProperty(WIDTH_ATT_NAME, "1024");
        String height = config.getProperty(HEIGHT_ATT_NAME, "768");
        
        System.out.println("WebBrowser Plugin - Action: " + action + ", URL: " + url);
        
        try {
            return executeAction(action, url, width, height, context);
        } catch (Exception e) {
            LOGGER.error("Unexpected error in webbrowser plugin", e);
            throw new RuntimeException("Unexpected error in webbrowser plugin: " + e.getMessage(), e);
        }
    }

    private Variable executeAction(String action, String url, String width, String height, DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        
        if ("load".equalsIgnoreCase(action)) {
            return handleLoad(url, width, height, context);
        } else if ("render".equalsIgnoreCase(action)) {
            return handleRender(url, width, height, context);
        } else if ("javascript".equalsIgnoreCase(action)) {
            return handleJavascript(url, context);
        } else {
            throw new PluginException("Unsupported WebBrowser action: " + action);
        }
    }

    private Variable handleLoad(String url, String width, String height, DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        System.out.println("WebBrowser load: " + url);
        
        if (url == null || url.trim().isEmpty()) {
            throw new PluginException("URL is required for load action");
        }
        
        // Validate URL format
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new PluginException("Invalid URL format: " + url);
        }
        
        // Simulate browser loading (in real implementation, this would use PhantomJS or similar)
        return new NodeVariable("Browser loaded: " + url + " (width: " + width + ", height: " + height + ")");
    }

    private Variable handleRender(String url, String width, String height, DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        System.out.println("WebBrowser render: " + url);
        
        if (url == null || url.trim().isEmpty()) {
            throw new PluginException("URL is required for render action");
        }
        
        // Validate dimensions
        try {
            int w = Integer.parseInt(width);
            int h = Integer.parseInt(height);
            if (w <= 0 || h <= 0) {
                throw new PluginException("Invalid dimensions: width=" + width + ", height=" + height);
            }
        } catch (NumberFormatException e) {
            throw new PluginException("Invalid dimension format: width=" + width + ", height=" + height);
        }
        
        // Simulate browser rendering (in real implementation, this would render to image/PDF)
        return new NodeVariable("Browser rendered: " + url + " to " + width + "x" + height);
    }

    private Variable handleJavascript(String url, DynamicScopeContext context) 
            throws PluginException, InterruptedException {
        System.out.println("WebBrowser javascript execution");
        
        // Get JavaScript code from body content
        Variable bodyVariable = super.executeBody(context);
        String javascriptCode = bodyVariable != null ? bodyVariable.toString() : "";
        
        if (javascriptCode.trim().isEmpty()) {
            throw new PluginException("JavaScript code is required for javascript action");
        }
        
        // Simulate JavaScript execution (in real implementation, this would execute on page)
        return new NodeVariable("JavaScript executed: " + javascriptCode.substring(0, Math.min(50, javascriptCode.length())) + "...");
    }
}

