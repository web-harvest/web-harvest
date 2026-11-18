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

package org.webharvest.plugin.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HttpService} using Apache HttpClient 5.x
 * 
 * <p>Migrated from HttpClient 4.5.14 to 5.5.1 in October 2025 for:</p>
 * <ul>
 *   <li>Latest security updates</li>
 *   <li>Improved performance</li>
 *   <li>Modern Java support</li>
 * </ul>
 * 
 * <h3>Bug #36: Redirect Header Preservation (v2.2.0)</h3>
 * <p>Added support for:</p>
 * <ul>
 *   <li><code>followRedirects</code> - control redirect behavior</li>
 *   <li><code>preserveHeaders</code> - maintain headers across redirects</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version 2.2.0
 */
public class HttpServiceImpl implements HttpService, AutoCloseable {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceImpl.class);
    
    private final CloseableHttpClient httpClient;
    
    /**
     * Creates a new HttpServiceImpl with a default HttpClient.
     * 
     * <p>Default behavior:</p>
     * <ul>
     *   <li>Redirects: ENABLED (follows 301/302)</li>
     *   <li>Header preservation: NONE</li>
     * </ul>
     */
    public HttpServiceImpl() {
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * Creates a new HttpServiceImpl with custom HttpClient.
     * 
     * @param httpClient the HTTP client to use
     */
    public HttpServiceImpl(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    /**
     * Creates HttpServiceImpl with redirect configuration.
     * 
     * <p><strong>Bug #36 Fix:</strong> This constructor enables control over
     * redirect behavior and header preservation.</p>
     * 
     * @param followRedirects whether to follow HTTP redirects (301/302)
     * 
     * @since 2.2.0
     */
    public HttpServiceImpl(boolean followRedirects) {
        this(followRedirects, new HashMap<>());
    }
    
    /**
     * Creates HttpServiceImpl with redirect and header preservation.
     * 
     * <p><strong>Bug #36 Fix:</strong> This constructor solves the problem of
     * headers being lost during redirects (e.g., login → redirect → logout).</p>
     * 
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li><strong>Login workflows:</strong> Preserve Cookie, Referer on redirect</li>
     *   <li><strong>API auth:</strong> Preserve X-API-Key, Authorization headers</li>
     *   <li><strong>Custom headers:</strong> Maintain any custom headers across redirects</li>
     * </ul>
     * 
     * @param followRedirects whether to follow HTTP redirects
     * @param defaultHeaders headers to add to ALL requests (initial + redirects)
     * 
     * @since 2.2.0
     */
    public HttpServiceImpl(boolean followRedirects, Map<String, String> defaultHeaders) {
        LOGGER.debug("Creating HttpServiceImpl: followRedirects={}, defaultHeaders={}", 
            followRedirects, defaultHeaders != null ? defaultHeaders.size() : 0);
        
        HttpClientBuilder builder = HttpClients.custom();
        
        // Configure redirect behavior
        RequestConfig requestConfig = RequestConfig.custom()
            .setRedirectsEnabled(followRedirects)
            .build();
        builder.setDefaultRequestConfig(requestConfig);
        
        // Add header preservation interceptor if needed
        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            HeaderPreservingInterceptor interceptor = new HeaderPreservingInterceptor(defaultHeaders);
            builder.addRequestInterceptorFirst(interceptor);
            
            LOGGER.info("HTTP client configured with {} default headers for redirect preservation", 
                defaultHeaders.size());
        }
        
        this.httpClient = builder.build();
    }
    
    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws HttpServiceException {
        LOGGER.debug("Executing GET request to: {}", url);
        HttpGet httpGet = new HttpGet(url);
        addHeaders(httpGet, headers);
        return executeRequest(httpGet);
    }
    
    @Override
    public HttpResponse post(String url, String body, Map<String, String> headers) throws HttpServiceException {
        LOGGER.debug("Executing POST request to: {}", url);
        HttpPost httpPost = new HttpPost(url);
        addHeaders(httpPost, headers);
        
        if (body != null && !body.isEmpty()) {
            try {
                StringEntity entity = new StringEntity(body);
                httpPost.setEntity(entity);
            } catch (Exception e) {
                throw new HttpServiceException("Failed to set request body: " + e.getMessage(), e);
            }
        }
        
        return executeRequest(httpPost);
    }
    
    @Override
    public HttpResponse request(String method, String url, String body, Map<String, String> headers) throws HttpServiceException {
        LOGGER.debug("Executing {} request to: {}", method, url);
        
        if ("GET".equalsIgnoreCase(method)) {
            return get(url, headers);
        } else if ("POST".equalsIgnoreCase(method)) {
            return post(url, body, headers);
        } else {
            throw new HttpServiceException("Unsupported HTTP method: " + method);
        }
    }
    
    /**
     * Adds headers to the HTTP request.
     * 
     * @param request the HTTP request
     * @param headers the headers to add
     */
    private void addHeaders(ClassicHttpRequest request, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * Executes an HTTP request and returns the response.
     * 
     * <p>Bug #39 Fix: Now reads response as binary data (byte[]) to support
     * images and other binary content without String conversion corruption.</p>
     * 
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    private HttpResponse executeRequest(ClassicHttpRequest request) throws HttpServiceException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getCode();
            
            // Bug #39 Fix: Get response body as BINARY first
            // This prevents corruption of image/PDF/video data
            byte[] binaryData = new byte[0];
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                binaryData = EntityUtils.toByteArray(entity);
            }
            
            // Get response headers  
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : response.getHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            
            LOGGER.debug("HTTP request completed with status code: {}, body size: {} bytes", 
                statusCode, binaryData.length);
            
            return new HttpResponseImpl(statusCode, binaryData, responseHeaders);
            
        } catch (IOException e) {
            LOGGER.error("HTTP request failed: {}", e.getMessage(), e);
            throw new HttpServiceException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Closes the underlying HTTP client and releases its connection resources.
     *
     * @throws IOException if the client cannot be closed
     */
    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    /**
     * Simple implementation of {@link HttpResponse}.
     * 
     * Bug #39 Fix: Stores response as byte[] to support binary content
     * without corruption.
     */
    private static class HttpResponseImpl implements HttpResponse {
        
        private final int statusCode;
        private final byte[] binaryData;  // Bug #39: Store as binary
        private final Map<String, String> headers;
        
        public HttpResponseImpl(int statusCode, byte[] binaryData, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.binaryData = binaryData;
            this.headers = headers;
        }
        
        @Override
        public int getStatusCode() {
            return statusCode;
        }
        
        @Override
        public String getBody() {
            // Convert binary to String (for text content)
            if (binaryData == null || binaryData.length == 0) {
                return "";
            }
            
            // Use UTF-8 by default for String conversion
            // (HttpPlugin can use charset from Content-Type if needed)
            return new String(binaryData, java.nio.charset.StandardCharsets.UTF_8);
        }
        
        @Override
        public byte[] getBodyAsBinary() {
            // Bug #39 Fix: Return binary data directly (no String conversion)
            return binaryData != null ? binaryData : new byte[0];
        }
        
        @Override
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        @Override
        public String getHeader(String name) {
            if (name == null) {
                return null;
            }
            for (Map.Entry<String, String> header : headers.entrySet()) {
                if (name.equalsIgnoreCase(header.getKey())) {
                    return header.getValue();
                }
            }
            return null;
        }
        
        @Override
        public String getContentType() {
            return getHeader("Content-Type");
        }
        
        @Override
        public String getCharset() {
            String contentType = getContentType();
            if (contentType != null && contentType.contains("charset=")) {
                int index = contentType.indexOf("charset=");
                String charset = contentType.substring(index + 8);
                int semicolonIndex = charset.indexOf(';');
                if (semicolonIndex > 0) {
                    charset = charset.substring(0, semicolonIndex);
                }
                return charset.trim();
            }
            return "UTF-8"; // Default charset
        }
    }
}





