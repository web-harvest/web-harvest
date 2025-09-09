/*  Copyright (c) 2006-2007, Vladimir Nikic
    All rights reserved.

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

    You can contact Vladimir Nikic by sending e-mail to
    nikic_vladimir@yahoo.com. Please include the word "Web-Harvest" in the
    subject line.
*/
package org.webharvest.plugin.core;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginConfigurationSchema;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.exceptions.PluginValidationException;
import org.webharvest.plugin.factory.DefaultPluginFactory;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.EmptyVariable;

import java.util.Arrays;
import java.util.List;

/**
 * HTTP plugin for Web-Harvest.
 * This is an example of how to refactor an existing processor into a plugin.
 * 
 * @author Web-Harvest Team
 * @since 2.1.0
 */
public class HttpPlugin extends AbstractPlugin {

    private static final String DEFAULT_NAMESPACE = "http://webharvest.org/plugins/core";
    private static final String DEFAULT_ELEMENT_NAME = "http";
    private static final String DEFAULT_PLUGIN_ID = "http-plugin";
    private static final String DEFAULT_PLUGIN_NAME = "HTTP Plugin";
    private static final String DEFAULT_PLUGIN_VERSION = "1.0.0";
    private static final String DEFAULT_PLUGIN_DESCRIPTION = "HTTP processor plugin for making HTTP requests";
    private static final String DEFAULT_PLUGIN_CATEGORY = "core";
    private static final String DEFAULT_PLUGIN_AUTHOR = "Web-Harvest Team";

    /**
     * Constructor for creating an HTTP plugin.
     * 
     * @param metadata plugin metadata
     */
    public HttpPlugin(PluginMetadata metadata) {
        super(metadata);
    }

    /**
     * Default constructor for creating an HTTP plugin with default metadata.
     */
    public HttpPlugin() {
        super(createDefaultMetadata());
    }

    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        // Get configuration properties
        String url = getProperty("url");
        String method = getProperty("method", "GET");
        String userAgent = getProperty("user-agent", "Web-Harvest/2.1.0");
        int timeout = getIntProperty("timeout", 30000);
        boolean followRedirects = getBooleanProperty("follow-redirects", true);
        
        // Validate required properties
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("URL is required");
        }

        try {
            // Here would be the actual HTTP request logic
            // For now, we'll return a mock response
            String response = "HTTP/1.1 200 OK\n" +
                            "Content-Type: text/html\n" +
                            "Content-Length: 0\n\n" +
                            "Mock HTTP response for URL: " + url;
            
            return new MockVariable(response);
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) throws PluginValidationException {
        // Validate required properties
        validateRequiredProperty("url");
        
        // Validate URL format
        String url = getProperty("url");
        if (url != null && !url.matches("^https?://.*")) {
            throw new PluginValidationException("Invalid URL format: " + url);
        }
        
        // Validate method
        String method = getProperty("method", "GET");
        if (!Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS").contains(method.toUpperCase())) {
            throw new PluginValidationException("Invalid HTTP method: " + method);
        }
        
        // Validate timeout
        int timeout = getIntProperty("timeout", 30000);
        if (timeout < 1000 || timeout > 300000) {
            throw new PluginValidationException("Timeout must be between 1000 and 300000 milliseconds");
        }
    }

    /**
     * Creates default metadata for the HTTP plugin.
     * 
     * @return default metadata
     */
    private static PluginMetadata createDefaultMetadata() {
        return new DefaultPluginFactory.DefaultPluginMetadata(
            DEFAULT_PLUGIN_ID,
            DEFAULT_PLUGIN_NAME,
            DEFAULT_PLUGIN_VERSION,
            DEFAULT_PLUGIN_DESCRIPTION,
            DEFAULT_PLUGIN_AUTHOR,
            DEFAULT_PLUGIN_CATEGORY,
            Arrays.asList("http", "web", "request", "core"),
            java.util.Collections.emptyMap(),
            createConfigurationSchema(),
            DEFAULT_NAMESPACE,
            DEFAULT_ELEMENT_NAME,
            true, // core plugin
            HttpPlugin.class.getName()
        );
    }

    /**
     * Creates the configuration schema for the HTTP plugin.
     * 
     * @return configuration schema
     */
    private static PluginConfigurationSchema createConfigurationSchema() {
        return new DefaultPluginConfigurationSchema(
            Arrays.asList(
                new DefaultAttributeDefinition("url", PluginConfigurationSchema.AttributeType.URL, true, null, "The URL to request", null, null, null, null),
                new DefaultAttributeDefinition("method", PluginConfigurationSchema.AttributeType.ENUM, false, "GET", "HTTP method to use", Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS"), null, null, null),
                new DefaultAttributeDefinition("user-agent", PluginConfigurationSchema.AttributeType.STRING, false, "Web-Harvest/2.1.0", "User agent string", null, null, null, null),
                new DefaultAttributeDefinition("timeout", PluginConfigurationSchema.AttributeType.INTEGER, false, "30000", "Request timeout in milliseconds", null, 1000, 300000, null),
                new DefaultAttributeDefinition("follow-redirects", PluginConfigurationSchema.AttributeType.BOOLEAN, false, "true", "Whether to follow redirects", null, null, null, null)
            ),
            Arrays.asList("url"),
            Arrays.asList(),
            Arrays.asList(),
            Arrays.asList(),
            true,
            "1.0"
        );
    }


    /**
     * Default implementation of PluginConfigurationSchema.
     */
    private static class DefaultPluginConfigurationSchema implements PluginConfigurationSchema {
        private final List<AttributeDefinition> attributes;
        private final List<String> requiredAttributes;
        private final List<String> optionalAttributes;
        private final List<String> allowedSubElements;
        private final List<String> requiredSubElements;
        private final boolean allowsBody;
        private final String version;

        public DefaultPluginConfigurationSchema(List<AttributeDefinition> attributes,
                                              List<String> requiredAttributes,
                                              List<String> optionalAttributes,
                                              List<String> allowedSubElements,
                                              List<String> requiredSubElements,
                                              boolean allowsBody,
                                              String version) {
            this.attributes = attributes;
            this.requiredAttributes = requiredAttributes;
            this.optionalAttributes = optionalAttributes;
            this.allowedSubElements = allowedSubElements;
            this.requiredSubElements = requiredSubElements;
            this.allowsBody = allowsBody;
            this.version = version;
        }

        @Override
        public List<AttributeDefinition> getAttributes() { return attributes; }
        @Override
        public AttributeDefinition getAttribute(String name) { 
            return attributes.stream().filter(attr -> attr.getName().equals(name)).findFirst().orElse(null);
        }
        @Override
        public List<String> getRequiredAttributes() { return requiredAttributes; }
        @Override
        public List<String> getOptionalAttributes() { return optionalAttributes; }
        @Override
        public List<String> getAllowedSubElements() { return allowedSubElements; }
        @Override
        public List<String> getRequiredSubElements() { return requiredSubElements; }
        @Override
        public boolean allowsBody() { return allowsBody; }
        @Override
        public String getVersion() { return version; }
        @Override
        public void validate(PluginConfiguration configuration) throws PluginValidationException {
            // Basic validation - can be extended
        }
    }

    /**
     * Default implementation of AttributeDefinition.
     */
    private static class DefaultAttributeDefinition implements PluginConfigurationSchema.AttributeDefinition {
        private final String name;
        private final PluginConfigurationSchema.AttributeType type;
        private final boolean required;
        private final String defaultValue;
        private final String description;
        private final List<String> allowedValues;
        private final Number minValue;
        private final Number maxValue;
        private final String pattern;

        public DefaultAttributeDefinition(String name, PluginConfigurationSchema.AttributeType type,
                                        boolean required, String defaultValue, String description,
                                        List<String> allowedValues, Number minValue, Number maxValue,
                                        String pattern) {
            this.name = name;
            this.type = type;
            this.required = required;
            this.defaultValue = defaultValue;
            this.description = description;
            this.allowedValues = allowedValues;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.pattern = pattern;
        }

        @Override
        public String getName() { return name; }
        @Override
        public PluginConfigurationSchema.AttributeType getType() { return type; }
        @Override
        public boolean isRequired() { return required; }
        @Override
        public String getDefaultValue() { return defaultValue; }
        @Override
        public String getDescription() { return description; }
        @Override
        public List<String> getAllowedValues() { return allowedValues; }
        @Override
        public Number getMinValue() { return minValue; }
        @Override
        public Number getMaxValue() { return maxValue; }
        @Override
        public String getPattern() { return pattern; }
    }

    /**
     * Mock implementation of Variable for testing.
     */
    private static class MockVariable extends org.webharvest.runtime.variables.NodeVariable {
        
        public MockVariable(String value) {
            super(value);
        }
    }
}
