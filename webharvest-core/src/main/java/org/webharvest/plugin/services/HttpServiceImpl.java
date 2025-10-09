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
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HttpService} using Apache HttpClient 4.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class HttpServiceImpl implements HttpService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceImpl.class);
    
    private final CloseableHttpClient httpClient;
    
    /**
     * Creates a new HttpServiceImpl with a default HttpClient.
     */
    public HttpServiceImpl() {
        this.httpClient = HttpClients.createDefault();
    }
    
    /**
     * Creates a new HttpServiceImpl with a custom HttpClient.
     * 
     * @param httpClient the HTTP client to use
     */
    public HttpServiceImpl(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
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
    private void addHeaders(HttpRequestBase request, Map<String, String> headers) {
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
    private HttpResponse executeRequest(HttpRequestBase request) throws HttpServiceException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            
            // Bug #39 Fix: Get response body as BINARY first
            // This prevents corruption of image/PDF/video data
            byte[] binaryData = new byte[0];
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                binaryData = EntityUtils.toByteArray(entity);
            }
            
            // Get response headers
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : response.getAllHeaders()) {
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
            return headers.get(name);
        }
        
        @Override
        public String getContentType() {
            return headers.get("Content-Type");
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





