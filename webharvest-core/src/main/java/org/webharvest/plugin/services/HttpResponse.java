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
 * Represents an HTTP response.
 * 
 * @author Robert Bala
 * @since 2.1.0-SNAPSHOT
 */
public interface HttpResponse {
    
    /**
     * Gets the response status code.
     * 
     * @return the status code
     */
    int getStatusCode();
    
    /**
     * Gets the response body as a string.
     * 
     * @return the response body
     */
    String getBody();
    
    /**
     * Gets the response headers.
     * 
     * @return the response headers
     */
    Map<String, String> getHeaders();
    
    /**
     * Gets a specific header value.
     * 
     * @param name the header name
     * @return the header value, or null if not present
     */
    String getHeader(String name);
    
    /**
     * Gets the content type of the response.
     * 
     * @return the content type
     */
    String getContentType();
    
    /**
     * Gets the character encoding of the response.
     * 
     * @return the character encoding
     */
    String getCharset();
}


