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

import java.net.URLEncoder;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.webharvest.plugin.AbstractPlugin;
import org.webharvest.plugin.PluginConfiguration;
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
 * <h2>Binary Content Support (Bug #39 - Fixed in v2.2.0)</h2>
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
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <def var="url">https://httpbin.org/get</def>
 * <def var="response">
 *     <http url="${url}"/>
 * </def>
 * <file action="write" path="target/http_output.txt">
 *     <get var="response"/>
 * </file>
 * </config>
 * }</pre>
 * 
 * <h4>Example 2: POST with JSON</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <http url="https://api.example.com/users" method="POST">
 *   <http-header name="Content-Type">application/json</http-header>
 *   <http-header name="Authorization">Bearer ${apiToken}</http-header>
 *   <http-param name="data">{"name": "John", "age": 30}</http-param>
 * </http>
 * </config>
 * }</pre>
 * 
 * <h4>Example 3: Binary Content (Images, PDFs)</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <http url="http://example.com/image.jpg"/>
 * <file action="write" path="downloaded_image.jpg" type="binary">${http}</file>
 * </config>
 * }</pre>
 * 
 * <h4>Example 4: URL with Special Characters</h4>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <config xmlns="http://org.webharvest/schema/2.1/core">
 * <!-- Color parameter #FF0000 is automatically encoded as %23FF0000 -->
 * <http url="http://example.com/api?color=#FF0000&amp;size=large"/>
 * </config>
 * }</pre>
 * 
 * <h3>Parameters</h3>
 * <ul>
 *   <li><strong>url</strong> (required): The HTTP/HTTPS URL to request</li>
 *   <li><strong>method</strong> (optional): HTTP method (GET, POST, PUT, DELETE, etc.) - default: GET</li>
 *   <li><strong>timeout</strong> (optional): Response and connection-pool acquisition timeout in milliseconds - default: 30000</li>
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
 *   <li><strong>URL Encoding</strong>: Special characters (#, &amp;, =, etc.) are automatically encoded</li>
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
@CorePlugin(
    elementName = "http",
    description = "Performs HTTP requests to retrieve web pages",
    version = "2.2.0",
    category = "Network"
)
@org.webharvest.plugin.annotations.UiField(
    attribute = "url",
    kind = org.webharvest.plugin.annotations.UiFieldKind.URL,
    group = "Request",
    order = 1,
    helperText = "Absolute or template-capable HTTP URL",
    placeholder = "https://example.com",
    localizationKey = "plugin.http.url",
    expressionCapable = true,
    minLength = 1,
    maxLength = 4096
)
@org.webharvest.plugin.annotations.UiField(
    attribute = "method",
    kind = org.webharvest.plugin.annotations.UiFieldKind.SELECT,
    group = "Request",
    order = 2,
    helperText = "HTTP method"
)
@org.webharvest.plugin.annotations.UiField(
    attribute = "body",
    kind = org.webharvest.plugin.annotations.UiFieldKind.TEXTAREA,
    group = "Request",
    order = 3,
    helperText = "Optional request body for methods that send content",
    advanced = true
)
@org.webharvest.plugin.annotations.ExpressionCapable(attribute = "url", context = "template")
@org.webharvest.plugin.annotations.UiTool(
    value = org.webharvest.plugin.metadata.CatalogUiToolMarkers.HttpInspector.class,
    id = "http-inspector",
    label = "HTTP Inspector",
    description = "Inspect request and response metadata",
    icon = "fa-search",
    placement = "panel"
)
public class HttpPlugin extends AbstractPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("org.webharvest.plugin.core.HttpPlugin");

    @Inject
    private HttpService httpService;
    
    @Override
    protected void doInitialize(PluginConfiguration configuration) {
        // Plugin initialization logic
    }
    
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String TEXT_CONTENT_PROPERTY = "_textContent";
    private static final String CHILD_HTTP_HEADER = "http-header";
    private static final String CHILD_HTTP_PARAM = "http-param";
    private static final List<String> SUPPORTED_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE");
    private static final List<String> METHODS_USING_GET = List.of("GET", "HEAD");

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

            String url = urlVar != null ? urlVar.toString(context.getCharset()) : urlTemplate;
            String method = methodVar != null ? methodVar.toString(context.getCharset()) : methodTemplate;

            if (method == null) {
                method = "GET";
            }

            String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_METHODS.contains(normalizedMethod)) {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
            }

            Map<String, String> headers = collectHeaders(getConfiguration());
            List<Map.Entry<String, String>> queryParams = collectParameters(getConfiguration());
            String urlWithParams = appendQueryParameters(url, queryParams);

            String bodyTemplate = getConfiguration().getProperty("body", "");
            Variable bodyVar = BaseTemplater.evaluateToVariable(bodyTemplate, ScriptingLanguage.BEANSHELL, context);
            String body = bodyVar != null ? bodyVar.toString(context.getCharset()) : bodyTemplate;

            if (body == null) {
                body = "";
            }

            String timeoutValue = getConfiguration().getProperty("timeout", "30000");
            long timeoutMillis;
            try {
                timeoutMillis = Long.parseLong(timeoutValue.trim());
            } catch (NumberFormatException invalidTimeout) {
                throw new IllegalArgumentException("HTTP timeout must be a positive integer in milliseconds",
                    invalidTimeout);
            }
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("HTTP timeout must be a positive integer in milliseconds");
            }

            HttpResponse response = httpService.request(
                normalizedMethod,
                urlWithParams,
                body,
                headers,
                Duration.ofMillis(timeoutMillis),
                context.getResourceAccess());
            
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

    private Map<String, String> collectHeaders(PluginConfiguration configuration) {
        Map<String, String> headers = new LinkedHashMap<>();

        if (configuration == null || !configuration.hasChildren()) {
            return headers;
        }

        for (PluginConfiguration child : configuration.getChildren()) {
            if (child == null) {
                continue;
            }

            String elementName = child.getElementName();
            if (elementName == null) {
                continue;
            }

            if (CHILD_HTTP_HEADER.equalsIgnoreCase(elementName)) {
                String name = child.getProperty("name");
                if (name == null || name.trim().isEmpty()) {
                    LOGGER.warn("Skipping http-header child with empty name.");
                    continue;
                }

                String value = child.getProperty(TEXT_CONTENT_PROPERTY, "");
                addHeader(headers, name.trim(), value == null ? "" : value);
            }
        }

        return headers;
    }

    private List<Map.Entry<String, String>> collectParameters(PluginConfiguration configuration) {
        List<Map.Entry<String, String>> params = new ArrayList<>();

        if (configuration == null || !configuration.hasChildren()) {
            return params;
        }

        for (PluginConfiguration child : configuration.getChildren()) {
            if (child == null) {
                continue;
            }

            String elementName = child.getElementName();
            if (elementName == null) {
                continue;
            }

            if (CHILD_HTTP_PARAM.equalsIgnoreCase(elementName)) {
                String name = child.getProperty("name");
                if (name == null || name.trim().isEmpty()) {
                    LOGGER.warn("Skipping http-param child with empty name.");
                    continue;
                }

                String value = child.getProperty(TEXT_CONTENT_PROPERTY, "");
                params.add(new AbstractMap.SimpleEntry<>(name.trim(), value == null ? "" : value));
            }
        }

        return params;
    }

    private String appendQueryParameters(String originalUrl, List<Map.Entry<String, String>> params) {
        if (params.isEmpty()) {
            return originalUrl;
        }

        StringBuilder urlBuilder = new StringBuilder(originalUrl);
        char separator = originalUrl.contains("?") ? '&' : '?';

        for (Map.Entry<String, String> param : params) {
            if (param.getKey() == null || param.getKey().isEmpty()) {
                continue;
            }

            urlBuilder.append(separator);
            separator = '&';

            urlBuilder.append(encode(param.getKey()));
            urlBuilder.append('=');
            urlBuilder.append(encode(param.getValue() == null ? "" : param.getValue()));
        }

        return urlBuilder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void addHeader(Map<String, String> headers, String name, String value) {
        String existingKey = headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);

        if (existingKey == null) {
            headers.put(name, value);
        } else {
            String current = headers.get(existingKey);
            if (current == null || current.isEmpty()) {
                headers.put(existingKey, value);
            } else if (value != null && !value.isEmpty()) {
                headers.put(existingKey, current + ", " + value);
            }
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
            if (method == null || method.trim().isEmpty()) {
                throw new IllegalArgumentException("HTTP method is required");
            }

            String normalized = method.trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_METHODS.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }
    
    @Override
    public PluginMetadata getMetadata() {
        return new HttpPluginMetadata();
    }
    
    
    /**
     * Metadata for HttpPlugin.
     * 
     * <p>Master plugin - allows http-header and http-param as children.</p>
     */
    public static class HttpPluginMetadata implements VisualPluginMetadata {
        
        @Override
        public String getId() {
            return "http";
        }
        
        @Override
        public String getName() {
            return "HTTP Request";
        }
        
        @Override
        public String getDescription() {
            return "Performs HTTP/HTTPS requests with full support for headers, parameters, authentication, redirects, and binary content. Supports GET and POST methods.";
        }
        
        @Override
        public String getVersion() {
            return "2.3.0";
        }
        
        @Override
        public String[] getTags() {
            return new String[]{"http", "web", "request", "core", "network", "api"};
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
            CorePlugin annotation = HttpPlugin.class.getAnnotation(CorePlugin.class);
            return annotation != null ? annotation.category() : "Network";
        }
        
        // VisualPluginMetadata implementation
        @Override
        public String getIcon() {
            return "fas fa-globe";
        }
        
        @Override
        public String getColor() {
            return "#3B82F6"; // Blue for HTTP/Network
        }
        
        @Override
        public String getGroup() {
            return "HTTP & Network";
        }
        
        @Override
        public int getDefaultWidth() {
            return 220;
        }
        
        @Override
        public int getDefaultHeight() {
            return 120;
        }
        
        @Override
        public InputPort[] getInputPorts() {
            return new InputPort[]{
                new InputPort("url", "URL to request (from url attribute)", PortType.STRING, true)
            };
        }
        
        @Override
        public OutputPort[] getOutputPorts() {
            return new OutputPort[]{
                new OutputPort("output", "HTTP response (XML, JSON, or binary)", PortType.MIXED)
            };
        }
        
        @Override
        public PortType getPortType() {
            return PortType.MIXED;
        }
        
        @Override
        public String getTooltip() {
            return "Performs HTTP/HTTPS requests with full support for headers, parameters, authentication, redirects, and binary content. Supports GET and POST methods. Allows http-header and http-param as children.";
        }
        
        @Override
        public KeyboardShortcut[] getKeyboardShortcuts() {
            return new KeyboardShortcut[]{
                new KeyboardShortcut("Cmd+Shift+H", "Add HTTP Request", false)
            };
        }
        
        @Override
        public AccessibilityMetadata getAccessibility() {
            return new AccessibilityMetadata(
                "HTTP Request Plugin",
                "Performs HTTP/HTTPS requests with full support for headers, parameters, authentication, redirects, and binary content.",
                new String[]{"Tab", "Enter", "Escape"}
            );
        }
        
        @Override
        public LinkedServiceMetadata[] getLinkedServices() {
            return new LinkedServiceMetadata[0];
        }
        
        @Override
        public ParentChildRelationship getParentRelationship() {
            return null; // Master plugin - no parent required
        }
        
        @Override
        public String[] getAllowedChildPlugins() {
            return new String[]{"http-header", "http-param"}; // Master plugin - allows these children
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
                    public String getDescription() { return "HTTP method (GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, TRACE)"; }
                    @Override
                    public String[] getAllowedValues() { return SUPPORTED_METHODS.toArray(new String[0]); }
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
            return new String[]{"http-header", "http-param"}; // Master plugin - allows these children
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
