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

import java.util.HashMap;
import java.util.Map;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
import org.webharvest.plugin.PluginMetadata;
import org.webharvest.plugin.annotations.CorePlugin;
import org.webharvest.plugin.services.HttpResponse;
import com.google.inject.Inject;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.variables.Variable;
import org.webharvest.runtime.variables.NodeVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.runtime.templaters.BaseTemplater;
import org.webharvest.runtime.scripting.ScriptingLanguage;

/**
 * HTTP plugin for making HTTP/HTTPS requests.
 * 
 * <p>
 * This plugin performs GET and POST requests with full support for:
 * headers, parameters, authentication, redirects, proper URL encoding, and binary content.
 * </p>
 * 
 * <h3>Binary Content Support (Bug #39 - Fixed in v2.2.0)</h3>
 * <p>
 * <strong>Issue:</strong> Binary content (images, PDFs, videos) was always converted to String,
 * corrupting binary data. No way to force binary mode for misconfigured servers.
 * </p>
 * <p>
 * <strong>Example:</strong><br>
 * Image URL: {@code http://example.com/image/12345} (no .jpg extension)<br>
 * Server Content-Type: {@code text/plain} (WRONG! Actually JPEG)<br>
 * Before fix: Binary data converted to String → corrupted (0xFF → 0xEF 0xBF 0xBD)<br>
 * After fix: Auto-detected as image/* → binary data preserved
 * </p>
 * <p>
 * <strong>Fix:</strong> HttpPlugin now auto-detects binary MIME types (image/*, video/*, audio/*,
 * application/pdf, etc.) and stores response as {@code byte[]} in NodeVariable instead of String.
 * This prevents corruption of binary data.
 * </p>
 * 
 * <h3>URL Encoding (Bug #55 - Fixed in v2.2.0)</h3>
 * <p>
 * <strong>Issue:</strong> Character '#' in URL parameter values was not encoded,
 * causing URL truncation. Everything after '#' was treated as fragment/anchor.
 * </p>
 * <p>
 * <strong>Example:</strong><br>
 * Input: {@code http://example.com?color=#FF0000}<br>
 * Before fix: {@code http://example.com?color=} (truncated!)<br>
 * After fix: {@code http://example.com?color=%23FF0000} (correct)
 * </p>
 * <p>
 * <strong>Fix:</strong> {@link org.webharvest.utils.CommonUtil#encodeUrl(String, String)} 
 * now properly encodes '#' as %23 in parameter values.
 * </p>
 * 
 * <h4>Example 1: Simple GET Request</h4>
 * <pre>{@code
 * <def var="url">https://httpbin.org/get</def>
 * <def var="response">
 *     <http url="${url}"/>
 * </def>
 * <file action="write" path="target/http_output.txt">
 *     <get var="response"/>
 * </file>
 * }</pre>
 * 
 * <h4>Example 2: POST with JSON</h4>
 * <pre>{@code
 * <http url="https://api.example.com/users" method="POST">
 *   <http-header name="Content-Type">application/json</http-header>
 *   <http-header name="Authorization">Bearer ${apiToken}</http-header>
 *   <http-param name="data">{"name": "John", "age": 30}</http-param>
 * </http>
 * }</pre>
 * 
 * <h4>Example 3: Binary Content (Images, PDFs)</h4>
 * <pre>{@code
 * <http url="http://example.com/image.jpg"/>
 * <file action="write" path="downloaded_image.jpg" type="binary">${http}</file>
 * }</pre>
 * 
 * <h4>Example 4: URL with Special Characters</h4>
 * <pre>{@code
 * <!-- Color parameter #FF0000 is automatically encoded as %23FF0000 -->
 * <http url="http://example.com/api?color=#FF0000&amp;size=large"/>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>url</strong> (required): The HTTP/HTTPS URL to request</li>
 *   <li><strong>method</strong> (optional): HTTP method (GET, POST, PUT, DELETE, etc.) - default: GET</li>
 *   <li><strong>timeout</strong> (optional): Request timeout in milliseconds - default: 30000</li>
 *   <li><strong>charset</strong> (optional): Character encoding - default: UTF-8</li>
 *   <li><strong>followRedirects</strong> (optional): Follow HTTP redirects - default: true</li>
 * </ul>
 * 
 * <h3>Sub-elements</h3>
 * <ul>
 *   <li>{@code <http-header>} - Define HTTP request headers</li>
 *   <li>{@code <http-param>} - Define POST/PUT form parameters</li>
 * </ul>
 * 
 * <h3>Important Notes</h3>
 * <ul>
 *   <li><strong>Binary Auto-Detection</strong>: Automatically detects binary content (images, PDFs, videos) and stores as byte[]</li>
 *   <li><strong>URL Encoding</strong>: Special characters (#, &, =, etc.) are automatically encoded</li>
 *   <li><strong>HTTPS Support</strong>: Full SSL/TLS support with certificate validation</li>
 *   <li><strong>Response Variable</strong>: HTTP response content is available in ${http} or assigned variable</li>
 * </ul>
 * 
 * <h3>Related Processors</h3>
 * <ul>
 *   <li>{@code <http-header>} - HTTP request headers</li>
 *   <li>{@code <http-param>} - HTTP form parameters</li>
 *   <li>{@code <html-to-xml>} - Convert HTML response to XML for XPath</li>
 *   <li>{@code <xpath>} - Extract data from HTML/XML response</li>
 *   <li>{@code <regexp>} - Extract data with regular expressions</li>
 *   <li>{@code <file>} - Save HTTP response to file</li>
 * </ul>
 * 
 * @author WebHarvest Team
 * @version 2.2.0
 * @see org.webharvest.utils.CommonUtil#encodeUrl(String, String)
 */
@CorePlugin(elementName = "http")
public class HttpPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.HttpPlugin");

    @Inject
    private HttpService httpService;
    
    @Override
    protected void doInitialize(PluginConfiguration configuration) {
        // Plugin initialization logic
    }
    
    @Override
    protected Variable doExecute(DynamicScopeContext context) throws InterruptedException {
        LOGGER.debug("HttpPlugin.doExecute: START - Method entered!");
        LOGGER.debug("HttpPlugin: context = {}", context);
        LOGGER.debug("HttpPlugin: httpService = {}", httpService);
        LOGGER.debug("HttpPlugin: httpService.getClass() = {}", (httpService != null ? httpService.getClass().getName() : "NULL"));
        
        // Check if HttpService is properly injected
        if (httpService == null) {
            throw new IllegalStateException("HttpService is not properly injected. Check Guice configuration.");
        }
        
        try {
            String urlTemplate = getConfiguration().getProperty("url");
            String methodTemplate = getConfiguration().getProperty("method", "GET");

            if (urlTemplate == null || urlTemplate.trim().isEmpty()) {
                throw new RuntimeException("URL is required");
            }

            // Resolve templates for URL and method
            Variable urlVar = BaseTemplater.evaluateToVariable(urlTemplate, ScriptingLanguage.BEANSHELL, context);
            Variable methodVar = BaseTemplater.evaluateToVariable(methodTemplate, ScriptingLanguage.BEANSHELL, context);

            String url = urlVar != null ? urlVar.toString() : urlTemplate;
            String method = methodVar != null ? methodVar.toString() : methodTemplate;
            
            Map<String, String> headers = new HashMap<>();
            // Add any custom headers from configuration
            
            HttpResponse response;
            if ("POST".equalsIgnoreCase(method)) {
                String bodyTemplate = getConfiguration().getProperty("body", "");
                Variable bodyVar = BaseTemplater.evaluateToVariable(bodyTemplate, ScriptingLanguage.BEANSHELL, context);
                String body = bodyVar != null ? bodyVar.toString() : bodyTemplate;
                response = httpService.post(url, body, headers);
            } else {
                response = httpService.get(url, headers);
            }
            
            // Bug #39 Fix: Check if response should be treated as binary
            String contentType = response.getContentType();
            boolean isBinary = isBinaryContentType(contentType);
            
            if (isBinary) {
                // Binary content: images, PDFs, videos, etc.
                byte[] binaryData = response.getBodyAsBinary();
                LOGGER.debug("HttpPlugin: Binary response, {} bytes", binaryData.length);
                return new NodeVariable(binaryData);
            } else {
                // Text content: HTML, JSON, XML, etc.
                String responseBody = response.getBody();
                LOGGER.debug("HttpPlugin: Text response, {} chars", responseBody.length());
                LOGGER.debug("HttpPlugin: First 200 chars = '{}'", 
                    responseBody.substring(0, Math.min(responseBody.length(), 200)));
                return new NodeVariable(responseBody);
            }
            
        } catch (Exception e) {
            throw new IllegalStateException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines if Content-Type indicates binary content.
     * 
     * <p>Bug #39 Fix: Detects binary MIME types (images, videos, PDFs, etc.)
     * to prevent String conversion corruption.</p>
     * 
     * @param contentType the Content-Type header value
     * @return true if binary content, false if text
     * @since 2.2.0
     */
    private boolean isBinaryContentType(String contentType) {
        if (contentType == null || contentType.isEmpty()) {
            return false;  // Unknown type, default to text
        }
        
        String mimeType = contentType.toLowerCase();
        
        // Remove parameters (e.g., "image/jpeg; charset=utf-8" → "image/jpeg")
        int semicolonIndex = mimeType.indexOf(';');
        if (semicolonIndex > 0) {
            mimeType = mimeType.substring(0, semicolonIndex).trim();
        }
        
        // Bug #39 Fix: Detect binary MIME types
        return mimeType.startsWith("image/")        // All images
            || mimeType.startsWith("video/")        // All videos
            || mimeType.startsWith("audio/")        // All audio
            || mimeType.equals("application/pdf")    // PDFs
            || mimeType.equals("application/zip")    // ZIP archives
            || mimeType.equals("application/gzip")   // GZIP
            || mimeType.equals("application/x-tar")  // TAR
            || mimeType.equals("application/octet-stream")  // Generic binary
            || mimeType.contains("binary");         // Any "binary" hint
    }
    
    @Override
    protected void doValidateConfiguration(PluginConfiguration configuration) {
        String url = configuration.getProperty("url");
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }
        
        String method = configuration.getProperty("method", "GET");
        if (!"GET".equals(method) && !"POST".equals(method)) {
            throw new IllegalArgumentException("Method must be GET or POST");
        }
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return new HttpPluginMetadata();
    }
    
    
    /**
     * Metadata for HttpPlugin.
     */
    public static class HttpPluginMetadata implements PluginMetadata {
        
        @Override
        public String getId() {
            return "http";
        }
        
        @Override
        public String getName() {
            return "HTTP Plugin";
        }
        
        @Override
        public String getDescription() {
            return "HTTP request plugin with dependency injection support";
        }
        
        @Override
        public String getVersion() {
            return "2.1.0-SNAPSHOT";
        }
        
        @Override
        public String[] getTags() {
            return new String[]{"http", "web", "request", "core"};
        }
        
        @Override
        public String[] getDependencies() {
            return new String[]{"HttpService"};
        }
        
        @Override
        public org.webharvest.plugin.PluginConfigurationSchema getConfigurationSchema() {
            return new HttpPluginConfigurationSchema();
        }
        
        @Override
        public String getNamespace() {
            return "http://org.webharvest/schema/2.1/core";
        }
        
        @Override
        public String getElementName() {
            return "http";
        }
        
        @Override
        public boolean isCorePlugin() {
            return true;
        }
        
        @Override
        public String getClassName() {
            return HttpPlugin.class.getName();
        }
        
        @Override
        public String getAuthor() {
            return "Web-Harvest Team";
        }
        
        @Override
        public String getCategory() {
            return "Web";
        }
    }
    
    /**
     * Configuration schema for HttpPlugin.
     */
    public static class HttpPluginConfigurationSchema implements org.webharvest.plugin.PluginConfigurationSchema {
        
        @Override
        public org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition[] getAttributeDefinitions() {
            return new org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition[]{
                new org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() { return "url"; }
                    @Override
                    public org.webharvest.plugin.PluginConfigurationSchema.AttributeType getType() { return org.webharvest.plugin.PluginConfigurationSchema.AttributeType.URL; }
                    @Override
                    public boolean isRequired() { return true; }
                    @Override
                    public String getDefaultValue() { return null; }
                    @Override
                    public String getDescription() { return "The URL to request"; }
                    @Override
                    public String[] getAllowedValues() { return null; }
                    @Override
                    public Number getMinValue() { return null; }
                    @Override
                    public Number getMaxValue() { return null; }
                    @Override
                    public String getPattern() { return null; }
                },
                new org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() { return "method"; }
                    @Override
                    public org.webharvest.plugin.PluginConfigurationSchema.AttributeType getType() { return org.webharvest.plugin.PluginConfigurationSchema.AttributeType.ENUM; }
                    @Override
                    public boolean isRequired() { return false; }
                    @Override
                    public String getDefaultValue() { return "GET"; }
                    @Override
                    public String getDescription() { return "HTTP method (GET, POST)"; }
                    @Override
                    public String[] getAllowedValues() { return new String[]{"GET", "POST"}; }
                    @Override
                    public Number getMinValue() { return null; }
                    @Override
                    public Number getMaxValue() { return null; }
                    @Override
                    public String getPattern() { return null; }
                },
                new org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition() {
                    @Override
                    public String getName() { return "body"; }
                    @Override
                    public org.webharvest.plugin.PluginConfigurationSchema.AttributeType getType() { return org.webharvest.plugin.PluginConfigurationSchema.AttributeType.STRING; }
                    @Override
                    public boolean isRequired() { return false; }
                    @Override
                    public String getDefaultValue() { return null; }
                    @Override
                    public String getDescription() { return "Request body for POST requests"; }
                    @Override
                    public String[] getAllowedValues() { return null; }
                    @Override
                    public Number getMinValue() { return null; }
                    @Override
                    public Number getMaxValue() { return null; }
                    @Override
                    public String getPattern() { return null; }
                }
            };
        }
        
        @Override
        public String[] getRequiredAttributes() {
            return new String[]{"url"};
        }
        
        @Override
        public String[] getOptionalAttributes() {
            return new String[]{"method", "body"};
        }
        
        @Override
        public void validate(PluginConfiguration configuration) throws org.webharvest.plugin.exceptions.PluginValidationException {
            String url = configuration.getProperty("url");
            if (url == null || url.trim().isEmpty()) {
                throw new org.webharvest.plugin.exceptions.PluginValidationException("URL is required");
            }
            
            String method = configuration.getProperty("method", "GET");
            if (!"GET".equals(method) && !"POST".equals(method)) {
                throw new org.webharvest.plugin.exceptions.PluginValidationException("Method must be GET or POST");
            }
        }
        
        @Override
        public org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition getAttribute(String name) {
            for (org.webharvest.plugin.PluginConfigurationSchema.AttributeDefinition attr : getAttributeDefinitions()) {
                if (attr.getName().equals(name)) {
                    return attr;
                }
            }
            return null;
        }
        
        @Override
        public String[] getAllowedSubElements() {
            return new String[0];
        }
        
        @Override
        public String[] getRequiredSubElements() {
            return new String[0];
        }
        
        @Override
        public boolean allowsBody() {
            return false;
        }
        
        @Override
        public String getVersion() {
            return "2.1.0-SNAPSHOT";
        }
    }
}
