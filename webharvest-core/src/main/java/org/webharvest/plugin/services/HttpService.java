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

import java.util.Map;

/**
 * Service interface for HTTP operations.
 * 
 * <p>
 * This interface abstracts HTTP operations, making it easy to mock
 * and test plugins that depend on HTTP functionality.
 * </p>
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public interface HttpService {
    
    /**
     * Executes an HTTP GET request.
     * 
     * @param url the URL to request
     * @param headers optional headers to include
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    HttpResponse get(String url, Map<String, String> headers) throws HttpServiceException;
    
    /**
     * Executes an HTTP POST request.
     * 
     * @param url the URL to request
     * @param body the request body
     * @param headers optional headers to include
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    HttpResponse post(String url, String body, Map<String, String> headers) throws HttpServiceException;
    
    /**
     * Executes an HTTP request with custom method.
     * 
     * @param method the HTTP method
     * @param url the URL to request
     * @param body the request body (can be null)
     * @param headers optional headers to include
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    HttpResponse request(String method, String url, String body, Map<String, String> headers) throws HttpServiceException;


}


