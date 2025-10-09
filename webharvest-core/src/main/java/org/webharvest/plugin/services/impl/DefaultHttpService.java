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

package org.webharvest.plugin.services.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.webharvest.plugin.services.HttpResponse;
import org.webharvest.plugin.services.HttpService;
import org.webharvest.plugin.services.HttpServiceException;
import org.webharvest.runtime.web.HttpClientManager;

/**
 * Default implementation of HttpService using the existing HttpClientManager.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public class DefaultHttpService implements HttpService {
    
    private final HttpClientManager httpClientManager;
    
    /**
     * Creates a new DefaultHttpService.
     * 
     * @param httpClientManager the HTTP client manager to use
     */
    public DefaultHttpService(HttpClientManager httpClientManager) {
        this.httpClientManager = httpClientManager;
    }
    
    @Override
    public HttpResponse get(String url, Map<String, String> headers) throws HttpServiceException {
        return request("GET", url, null, headers);
    }
    
    @Override
    public HttpResponse post(String url, String body, Map<String, String> headers) throws HttpServiceException {
        return request("POST", url, body, headers);
    }
    
    @Override
    public HttpResponse request(String method, String url, String body, Map<String, String> headers) throws HttpServiceException {
        try {
            HttpClient client = httpClientManager.getHttpClient();
            HttpMethod httpMethod = createHttpMethod(method, url, body);
            
            // Set headers
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    httpMethod.setRequestHeader(entry.getKey(), entry.getValue());
                }
            }
            
            // Execute request
            int statusCode = client.executeMethod(httpMethod);
            
            // Read response
            String responseBody = httpMethod.getResponseBodyAsString();
            Map<String, String> responseHeaders = new HashMap<>();
            
            // Extract headers
            org.apache.commons.httpclient.Header[] headersArray = httpMethod.getResponseHeaders();
            for (org.apache.commons.httpclient.Header header : headersArray) {
                responseHeaders.put(header.getName(), header.getValue());
            }
            
            return new DefaultHttpResponse(statusCode, responseBody, responseHeaders);
            
        } catch (IOException e) {
            throw new HttpServiceException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    private HttpMethod createHttpMethod(String method, String url, String body) throws HttpServiceException {
        switch (method.toUpperCase()) {
            case "GET":
                return new GetMethod(url);
            case "POST":
                PostMethod postMethod = new PostMethod(url);
                if (body != null) {
                    try {
                        postMethod.setRequestEntity(new StringRequestEntity(body, "text/plain", "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        throw new HttpServiceException("Unsupported encoding: " + e.getMessage(), e);
                    }
                }
                return postMethod;
            default:
                throw new HttpServiceException("Unsupported HTTP method: " + method);
        }
    }
    
    /**
     * Default implementation of HttpResponse.
     * 
     * Bug #39 Fix: Stores response as byte[] to support binary content.
     */
    private static class DefaultHttpResponse implements HttpResponse {
        private final int statusCode;
        private final byte[] binaryData;  // Bug #39: Store as binary
        private final Map<String, String> headers;
        
        public DefaultHttpResponse(int statusCode, String body, Map<String, String> headers) {
            this.statusCode = statusCode;
            this.binaryData = body != null ? body.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
            this.headers = headers;
        }
        
        @Override
        public int getStatusCode() {
            return statusCode;
        }
        
        @Override
        public String getBody() {
            // Convert binary to String
            if (binaryData == null || binaryData.length == 0) {
                return "";
            }
            return new String(binaryData, java.nio.charset.StandardCharsets.UTF_8);
        }
        
        @Override
        public byte[] getBodyAsBinary() {
            // Bug #39 Fix: Return binary data directly
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
                int charsetIndex = contentType.indexOf("charset=");
                return contentType.substring(charsetIndex + 8).trim();
            }
            return "UTF-8"; // Default charset
        }
    }
}
