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

package org.webharvest.runtime.session;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe implementation of {@link SessionRegistry} using {@link ConcurrentHashMap}.
 * 
 * <p>This implementation provides:</p>
 * <ul>
 *   <li>Thread-safe concurrent access without external synchronization</li>
 *   <li>O(1) lookup by session ID</li>
 *   <li>Efficient filtering by client ID</li>
 *   <li>Snapshot semantics for bulk operations (getAllSessions, getSessionsByClientId)</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>All public methods are thread-safe and can be called concurrently from multiple threads.
 * Methods that return collections ({@link #getAllSessions()}, {@link #getSessionsByClientId(String)})
 * return snapshots that are safe from concurrent modification.</p>
 * 
 * <h3>Memory Management:</h3>
 * <p>Sessions remain in the registry until explicitly removed via {@link #unregister(String)}
 * or {@link #clear()}. Applications should implement cleanup strategies to prevent unbounded
 * memory growth:</p>
 * <ul>
 *   <li>Unregister sessions after completion</li>
 *   <li>Implement periodic cleanup of old completed sessions</li>
 *   <li>Use TTL-based eviction for long-running applications</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SessionRegistry registry = new SessionRegistryImpl();
 * 
 * // Register new session
 * ScraperSession session = new ScraperSessionImpl(sessionId, clientId, config);
 * registry.register(session);
 * 
 * // Lookup later
 * Optional<ScraperSession> found = registry.getSession(sessionId);
 * if (found.isPresent()) {
 *     System.out.println("Session status: " + found.get().getStatus());
 * }
 * 
 * // Cleanup when done
 * if (session.getStatus().isTerminal()) {
 *     registry.unregister(sessionId);
 * }
 * }</pre>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><b>register()</b>: O(1) average case</li>
 *   <li><b>getSession()</b>: O(1) average case</li>
 *   <li><b>unregister()</b>: O(1) average case</li>
 *   <li><b>getSessionsByClientId()</b>: O(n) where n = total sessions</li>
 *   <li><b>getAllSessions()</b>: O(n) where n = total sessions</li>
 * </ul>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see SessionRegistry
 * @see ScraperSession
 * @see ConcurrentHashMap
 */
public class SessionRegistryImpl implements SessionRegistry {
    
    private final ConcurrentHashMap<String, ScraperSession> sessions;
    
    /**
     * Creates a new empty registry.
     */
    public SessionRegistryImpl() {
        this.sessions = new ConcurrentHashMap<>();
    }
    
    /**
     * Creates a new registry with specified initial capacity.
     * Useful when expected number of sessions is known.
     * 
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if initialCapacity is negative
     */
    public SessionRegistryImpl(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException(
                "Initial capacity cannot be negative: " + initialCapacity);
        }
        this.sessions = new ConcurrentHashMap<>(initialCapacity);
    }
    
    @Override
    public void register(ScraperSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        sessions.put(session.getSessionId(), session);
    }
    
    @Override
    public Optional<ScraperSession> getSession(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        return Optional.ofNullable(sessions.get(sessionId));
    }
    
    @Override
    public List<ScraperSession> getSessionsByClientId(String clientId) {
        // Note: We allow null clientId (sessions without client)
        // but throw exception if parameter is explicitly null without intent
        // For consistency, we'll allow null and filter appropriately
        
        return sessions.values().stream()
            .filter(session -> {
                String sessionClientId = session.getClientId();
                // Match both null or equal clientId
                return (clientId == null && sessionClientId == null) ||
                       (clientId != null && clientId.equals(sessionClientId));
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ScraperSession> getAllSessions() {
        // Return a snapshot (new ArrayList) to prevent concurrent modification issues
        return new ArrayList<>(sessions.values());
    }
    
    @Override
    public boolean unregister(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        return sessions.remove(sessionId) != null;
    }
    
    @Override
    public int size() {
        return sessions.size();
    }
    
    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }
    
    @Override
    public void clear() {
        sessions.clear();
    }
    
    @Override
    public String toString() {
        return "SessionRegistry{" +
                "size=" + size() +
                ", sessions=" + sessions.keySet() +
                '}';
    }
}






