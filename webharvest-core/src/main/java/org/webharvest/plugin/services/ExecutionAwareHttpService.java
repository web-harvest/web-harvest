/*
 * Copyright (c) 2006-2026 the original author or authors.
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

import java.time.Duration;
import java.util.Map;

import org.webharvest.runtime.resource.ExecutionResourceAccess;

/**
 * Optional HTTP provider capability for execution-owned cancellation.
 *
 * <p>Providers implementing this interface can register each active request
 * with the owning runtime execution and release its use lease when the request
 * completes. Legacy {@link HttpService} implementations remain valid for
 * standalone contexts but must not be used by a managed execution that
 * requires cancellation.</p>
 */
public interface ExecutionAwareHttpService extends HttpService {

    /**
     * Executes one request with response-inactivity and connection-pool
     * acquisition timeouts and execution-owned cancellation.
     *
     * @param method HTTP method
     * @param url request URL
     * @param body optional request body
     * @param headers optional request headers
     * @param timeout response inactivity and connection-pool acquisition timeout
     * @param resourceAccess execution-owned resource access
     * @return the HTTP response
     * @throws HttpServiceException if the request fails
     */
    HttpResponse request(String method, String url, String body,
                         Map<String, String> headers, Duration timeout,
                         ExecutionResourceAccess resourceAccess) throws HttpServiceException;
}
