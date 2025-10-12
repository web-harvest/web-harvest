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

package org.webharvest.runtime.tracking;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe implementation of {@link TokenTracker} using atomic counters.
 * 
 * <p>This implementation provides lock-free concurrent access using
 * {@link AtomicLong} for each resource type counter.</p>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><b>record()</b>: O(1) lock-free atomic operation</li>
 *   <li><b>getUsage()</b>: O(n) where n = number of resource types (typically 4)</li>
 *   <li><b>reset()</b>: O(n) where n = number of resource types</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see TokenTracker
 * @see TokenUsage
 */
public class TokenTrackerImpl implements TokenTracker {
    
    private final String sessionId;
    private final String clientId;
    private final Map<ResourceType, AtomicLong> counters;
    
    /**
     * Creates a new token tracker for a session.
     * 
     * @param sessionId the session identifier (must not be null)
     * @param clientId the client identifier (may be null)
     * @throws IllegalArgumentException if sessionId is null
     */
    public TokenTrackerImpl(String sessionId, String clientId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.counters = new EnumMap<>(ResourceType.class);
        
        // Initialize all counters
        for (ResourceType type : ResourceType.values()) {
            counters.put(type, new AtomicLong(0));
        }
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public String getClientId() {
        return clientId;
    }
    
    @Override
    public void record(ResourceType type, long amount) {
        if (type == null) {
            throw new IllegalArgumentException("Resource type cannot be null");
        }
        if (amount < 0) {
            throw new IllegalArgumentException(
                "Amount cannot be negative: " + amount);
        }
        
        counters.get(type).addAndGet(amount);
    }
    
    @Override
    public TokenUsage getUsage() {
        Map<ResourceType, Long> snapshot = new EnumMap<>(ResourceType.class);
        for (Map.Entry<ResourceType, AtomicLong> entry : counters.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return new TokenUsage(snapshot);
    }
    
    @Override
    public void reset() {
        for (AtomicLong counter : counters.values()) {
            counter.set(0);
        }
    }
    
    @Override
    public String toString() {
        return "TokenTracker{" +
                "sessionId='" + sessionId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", usage=" + getUsage() +
                '}';
    }
}


