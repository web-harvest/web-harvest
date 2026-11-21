/*
 * Copyright (c) 2006-2025 the original author or authors.
 */

package org.webharvest.plugin.services;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * HTTP request interceptor that ensures specified headers are present on ALL requests,
 * including redirect requests.
 * 
 * <h2>Bug #36: HTTP Redirect Header Preservation</h2>
 * 
 * <p><strong>Problem:</strong> When HttpClient follows redirects (HTTP 301/302),
 * it creates new requests but doesn't copy custom headers like Referer, Authorization,
 * or custom auth tokens. This causes authentication failures and session loss.</p>
 * 
 * <p><strong>Solution:</strong> This interceptor adds default headers to EVERY request,
 * ensuring they're present on both initial and redirect requests.</p>
 * 
 * <h4>Example Usage:</h4>
 * <pre>{@code
 * Map<String, String> defaultHeaders = new HashMap<>();
 * defaultHeaders.put("Referer", "https://example.com/login");
 * defaultHeaders.put("X-API-Key", "secret");
 * 
 * HeaderPreservingInterceptor interceptor = new HeaderPreservingInterceptor(defaultHeaders);
 * 
 * CloseableHttpClient client = HttpClients.custom()
 *     .addRequestInterceptorFirst(interceptor)
 *     .build();
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class HeaderPreservingInterceptor implements HttpRequestInterceptor {
    
    private static final Logger LOG = LoggerFactory.getLogger(HeaderPreservingInterceptor.class);
    
    private final Map<String, String> defaultHeaders;
    
    /**
     * Creates interceptor with no default headers.
     */
    public HeaderPreservingInterceptor() {
        this(new HashMap<>());
    }
    
    /**
     * Creates interceptor with specified default headers.
     * 
     * <p>These headers will be added to ALL requests (initial + redirects)
     * if they're not already present.</p>
     * 
     * @param defaultHeaders map of header name → value to add
     */
    public HeaderPreservingInterceptor(Map<String, String> defaultHeaders) {
        this.defaultHeaders = defaultHeaders != null ? 
            new HashMap<>(defaultHeaders) : new HashMap<>();
        
        if (!this.defaultHeaders.isEmpty()) {
            LOG.debug("Created HeaderPreservingInterceptor with {} default headers", 
                this.defaultHeaders.size());
        }
    }
    
    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context) 
            throws HttpException, IOException {
        
        if (defaultHeaders.isEmpty()) {
            return; // No headers to add
        }
        
        // Add default headers if not already present
        int addedCount = 0;
        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            String headerName = entry.getKey();
            String headerValue = entry.getValue();
            
            // Only add if not already present (don't override existing headers)
            if (!request.containsHeader(headerName)) {
                request.addHeader(headerName, headerValue);
                addedCount++;
                
                LOG.trace("Added default header: {} = {}", headerName, headerValue);
            }
        }
        
        if (addedCount > 0) {
            LOG.debug("Added {} default header(s) to request: {}", 
                addedCount, request.getRequestUri());
        }
    }
    
    /**
     * Returns copy of default headers.
     * 
     * @return immutable map of default headers
     */
    public Map<String, String> getDefaultHeaders() {
        return new HashMap<>(defaultHeaders);
    }
}

