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

package org.webharvest.runtime.session.events;

import org.webharvest.runtime.session.ScraperSession;

import java.time.Instant;

/**
 * Base class for all session-related events.
 * 
 * <p>Session events are published to Guava EventBus and can be subscribed to
 * for monitoring, logging, metrics collection, and custom business logic.</p>
 * 
 * <h2>Event Hierarchy:</h2>
 * <pre>
 * SessionEvent (abstract)
 *   ├── SessionCreatedEvent
 *   ├── SessionStartedEvent
 *   ├── SessionCompletedEvent
 *   ├── SessionFailedEvent
 *   └── SessionCancelledEvent
 * </pre>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Subscribe to events
 * EventBus eventBus = injector.getInstance(EventBus.class);
 * eventBus.register(new Object() {
 *     @Subscribe
 *     public void onSessionStarted(SessionStartedEvent event) {
 *         System.out.println("Session started: " + event.getSessionId());
 *     }
 *     
 *     @Subscribe
 *     public void onSessionCompleted(SessionCompletedEvent event) {
 *         System.out.println("Session completed in: " + 
 *             event.getSession().getMetrics().getDuration());
 *     }
 * });
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see ScraperSession
 * @see com.google.common.eventbus.EventBus
 */
public abstract class SessionEvent {
    
    private final ScraperSession session;
    private final Instant timestamp;
    
    /**
     * Creates a new session event.
     * 
     * @param session the session this event relates to (must not be null)
     * @throws IllegalArgumentException if session is null
     */
    protected SessionEvent(ScraperSession session) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        this.session = session;
        this.timestamp = Instant.now();
    }
    
    /**
     * Returns the session this event relates to.
     * 
     * @return the session (never null)
     */
    public ScraperSession getSession() {
        return session;
    }
    
    /**
     * Returns the session ID for convenience.
     * 
     * @return the session ID
     */
    public String getSessionId() {
        return session.getSessionId();
    }
    
    /**
     * Returns the client ID for convenience.
     * 
     * @return the client ID, or null if not associated with a client
     */
    public String getClientId() {
        return session.getClientId();
    }
    
    /**
     * Returns when this event was created.
     * 
     * @return the event timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "sessionId='" + getSessionId() + '\'' +
                ", clientId='" + getClientId() + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}






