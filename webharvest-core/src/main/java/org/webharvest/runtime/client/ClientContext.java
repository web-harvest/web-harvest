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

package org.webharvest.runtime.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents client context for multi-tenant session management.
 * 
 * <p>A client context identifies who is executing a scraping session and provides
 * metadata for tracking, monitoring, and future quota enforcement.</p>
 * 
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li><b>Multi-tenant Applications:</b> Isolate sessions by client/tenant</li>
 *   <li><b>Resource Tracking:</b> Track resource usage per client</li>
 *   <li><b>Billing:</b> Associate sessions with billing accounts</li>
 *   <li><b>Monitoring:</b> Filter logs and metrics by client</li>
 *   <li><b>Quota Enforcement:</b> Future support for client-specific limits</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create client context
 * ClientContext client = ClientContext.builder()
 *     .clientId("acme-corp")
 *     .metadata("tier", "premium")
 *     .metadata("region", "us-east")
 *     .metadata("accountId", "12345")
 *     .build();
 * 
 * // Use in session execution
 * ScraperSession session = service.executeAsync(config, client, options);
 * 
 * // Query sessions by client
 * List<ScraperSession> clientSessions = registry.getSessionsByClientId(client.getClientId());
 * }</pre>
 * 
 * <h3>Immutability:</h3>
 * <p>Instances are immutable after construction. Metadata map is defensively copied
 * and returned as unmodifiable.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see org.webharvest.runtime.session.ScraperSession
 */
public final class ClientContext {
    
    private final String clientId;
    private final Map<String, Object> metadata;
    
    /**
     * Creates a client context with specified ID and metadata.
     * Use {@link #builder()} for fluent construction.
     * 
     * @param clientId the client identifier (may be null)
     * @param metadata additional metadata (may be null or empty)
     */
    private ClientContext(String clientId, Map<String, Object> metadata) {
        this.clientId = clientId;
        this.metadata = metadata != null ? 
            Collections.unmodifiableMap(new HashMap<>(metadata)) : 
            Collections.emptyMap();
    }
    
    /**
     * Returns the client identifier.
     * 
     * @return the client ID, or null if not associated with a client
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Returns all metadata associated with this client.
     * 
     * <p>The returned map is unmodifiable.</p>
     * 
     * @return unmodifiable metadata map (never null, may be empty)
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Returns a specific metadata value.
     * 
     * @param key the metadata key
     * @return the metadata value, or null if not present
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Returns a specific metadata value as a string.
     * 
     * @param key the metadata key
     * @return the metadata value as string, or null if not present or not a string
     */
    public String getMetadataString(String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }
    
    /**
     * Checks if metadata contains a specific key.
     * 
     * @param key the metadata key
     * @return true if key exists; false otherwise
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }
    
    /**
     * Creates a new builder for constructing client contexts.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a simple client context with only an ID.
     * 
     * @param clientId the client identifier
     * @return a new client context
     */
    public static ClientContext of(String clientId) {
        return new Builder().clientId(clientId).build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientContext that = (ClientContext) o;
        return Objects.equals(clientId, that.clientId) &&
               Objects.equals(metadata, that.metadata);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(clientId, metadata);
    }
    
    @Override
    public String toString() {
        return "ClientContext{" +
                "clientId='" + clientId + '\'' +
                ", metadata=" + metadata +
                '}';
    }
    
    /**
     * Builder for creating {@link ClientContext} instances with fluent API.
     * 
     * <h3>Usage Example:</h3>
     * <pre>{@code
     * ClientContext client = ClientContext.builder()
     *     .clientId("my-client")
     *     .metadata("tier", "premium")
     *     .metadata("region", "us-west")
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private String clientId;
        private Map<String, Object> metadata = new HashMap<>();
        
        private Builder() {}
        
        /**
         * Sets the client identifier.
         * 
         * @param clientId the client ID (may be null)
         * @return this builder for chaining
         */
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }
        
        /**
         * Adds a metadata key-value pair.
         * 
         * @param key the metadata key (must not be null)
         * @param value the metadata value (may be null)
         * @return this builder for chaining
         * @throws IllegalArgumentException if key is null
         */
        public Builder metadata(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("Metadata key cannot be null");
            }
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Adds multiple metadata entries at once.
         * 
         * @param metadata map of metadata entries (may be null)
         * @return this builder for chaining
         */
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }
        
        /**
         * Builds the client context.
         * 
         * @return a new immutable client context
         */
        public ClientContext build() {
            return new ClientContext(clientId, metadata);
        }
    }
}

