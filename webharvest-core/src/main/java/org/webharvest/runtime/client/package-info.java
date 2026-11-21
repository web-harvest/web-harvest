/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * Caller-supplied client identity and metadata for WebHarvest sessions.
 * 
 * <h2>Overview</h2>
 * <p>This package associates a session with a caller-provided client identifier and
 * immutable metadata. It supports grouping and lookup, but does not provide
 * authentication, authorization, tenant isolation, billing, or quota enforcement.</p>
 * 
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li>Group sessions by a caller-provided client ID</li>
 *   <li>Attach caller-defined labels for reporting</li>
 *   <li>Filter session listings by client ID</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create client context
 * ClientContext client = ClientContext.builder()
 *     .clientId("acme-corp")
 *     .metadata("tier", "premium")
 *     .metadata("region", "us-east")
 *     .metadata("accountId", "ACC-12345")
 *     .build();
 * 
 * // Use in session execution
 * ScraperSession session = service.executeAsync(config, client, options);
 * 
 * // Query all sessions for this client
 * List<ScraperSession> clientSessions = registry.getSessionsByClientId(client.getClientId());
 * }</pre>
 * 
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.client;






