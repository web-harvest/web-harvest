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

import java.util.List;
import java.util.Optional;

/**
 * Central registry for managing active and completed scraper sessions.
 * 
 * <p>The registry provides thread-safe session storage and lookup capabilities.
 * It enables:</p>
 * <ul>
 *   <li>Session registration with unique IDs</li>
 *   <li>Session lookup by ID</li>
 *   <li>Listing sessions by client ID (multi-tenant support)</li>
 *   <li>Listing all sessions</li>
 *   <li>Session cleanup and lifecycle management</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * SessionRegistry registry = new SessionRegistryImpl();
 * 
 * // Register a new session
 * ScraperSession session = new ScraperSessionImpl(sessionId, clientId, config);
 * registry.register(session);
 * 
 * // Look up session by ID
 * Optional<ScraperSession> found = registry.getSession(sessionId);
 * if (found.isPresent()) {
 *     System.out.println("Session status: " + found.get().getStatus());
 * }
 * 
 * // Get all sessions for a client
 * List<ScraperSession> clientSessions = registry.getSessionsByClientId("client-123");
 * System.out.println("Client has " + clientSessions.size() + " sessions");
 * 
 * // Cleanup completed sessions
 * registry.unregister(sessionId);
 * }</pre>
 * 
 * <h3>Multi-tenant Scenarios:</h3>
 * <pre>{@code
 * // Track resource usage per client
 * for (String clientId : getActiveClientIds()) {
 *     List<ScraperSession> sessions = registry.getSessionsByClientId(clientId);
 *     
 *     long activeSessions = sessions.stream()
 *         .filter(s -> s.getStatus() == SessionStatus.RUNNING)
 *         .count();
 *         
 *     if (activeSessions > quotaForClient(clientId)) {
 *         // Enforce quota limits
 *         System.err.println("Client " + clientId + " exceeded session quota");
 *     }
 * }
 * }</pre>
 * 
 * <h3>Cleanup Strategies:</h3>
 * <p>Registries should implement cleanup to prevent memory leaks:</p>
 * <ul>
 *   <li><b>Manual cleanup:</b> Call {@link #unregister(String)} after session completion</li>
 *   <li><b>Automatic cleanup:</b> Background task removes old terminal sessions</li>
 *   <li><b>Size-based cleanup:</b> Evict oldest sessions when registry exceeds size limit</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>All implementations must be thread-safe. Multiple threads may register,
 * query, and unregister sessions concurrently without external synchronization.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see ScraperSession
 * @see SessionRegistryImpl
 * @see org.webharvest.runtime.service.WebHarvestService
 */
public interface SessionRegistry {
    
    /**
     * Registers a session in the registry.
     * 
     * <p>If a session with the same ID already exists, it will be replaced.
     * This method is typically called immediately after session creation.</p>
     * 
     * @param session the session to register
     * @throws IllegalArgumentException if session is null
     */
    void register(ScraperSession session);
    
    /**
     * Retrieves a session by its unique identifier.
     * 
     * @param sessionId the session ID to look up
     * @return an Optional containing the session if found, or empty if not found
     * @throws IllegalArgumentException if sessionId is null
     */
    Optional<ScraperSession> getSession(String sessionId);
    
    /**
     * Retrieves all sessions associated with a specific client.
     * 
     * <p>Useful for multi-tenant scenarios where you need to track all sessions
     * for a particular client for quota enforcement, billing, or monitoring.</p>
     * 
     * <p>Sessions are returned in no particular order. The returned list is a
     * snapshot at the time of the call.</p>
     * 
     * @param clientId the client ID to filter by
     * @return a list of sessions (never null, may be empty)
     * @throws IllegalArgumentException if clientId is null
     */
    List<ScraperSession> getSessionsByClientId(String clientId);
    
    /**
     * Retrieves all registered sessions.
     * 
     * <p>This includes sessions in all states (PENDING, RUNNING, COMPLETED, etc.).
     * Use with caution in production systems as the returned list may be large.</p>
     * 
     * <p>Sessions are returned in no particular order. The returned list is a
     * snapshot at the time of the call.</p>
     * 
     * @return a list of all sessions (never null, may be empty)
     */
    List<ScraperSession> getAllSessions();
    
    /**
     * Removes a session from the registry.
     * 
     * <p>Typically called after session completion to free memory. If the session
     * doesn't exist, this method does nothing.</p>
     * 
     * <p><b>Important:</b> Unregistering a running session doesn't cancel it;
     * it only removes it from the registry. Call {@link ScraperSession#cancel()}
     * first if you want to stop execution.</p>
     * 
     * @param sessionId the ID of the session to remove
     * @return true if the session was found and removed; false if not found
     * @throws IllegalArgumentException if sessionId is null
     */
    boolean unregister(String sessionId);
    
    /**
     * Returns the current number of registered sessions.
     * 
     * <p>This is a snapshot count at the time of the call. In highly concurrent
     * environments, the actual count may change immediately after this method returns.</p>
     * 
     * @return the number of sessions currently registered
     */
    int size();
    
    /**
     * Checks if the registry is empty.
     * 
     * @return true if no sessions are registered; false otherwise
     */
    boolean isEmpty();
    
    /**
     * Removes all sessions from the registry.
     * 
     * <p><b>Warning:</b> This does not cancel running sessions; it only
     * removes them from the registry. Use with caution.</p>
     */
    void clear();
}






