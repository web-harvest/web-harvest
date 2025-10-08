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
     * @param request the HTTP request to execute
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    private HttpResponse executeRequest(HttpRequestBase request) throws HttpServiceException {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            
            // Get response body
            String body = "";
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                body = EntityUtils.toString(entity);
            }
            
            // Get response headers
            Map<String, String> responseHeaders = new HashMap<>();
            for (Header header : response.getAllHeaders()) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            
            LOGGER.debug("HTTP request completed with status code: {}", statusCode);
            
            return new HttpResponseImpl(statusCode, body, responseHeaders);
            
        } catch (IOException e) {
            LOGGER.error("HTTP request failed: {}", e.getMessage(), e);
            throw new HttpServiceException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Simple implementation of {@link HttpResponse}.
     */
    private static class HttpResponseImpl implements HttpResponse {
        
        private final int statusCode;
        private final String body;
        private final Map<String, String> headers;
        
        public HttpResponseImpl(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
        
        @Override
        public int getStatusCode() {
            return statusCode;
        }
        
        @Override
        public String getBody() {
            return body;
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





