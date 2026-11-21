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

import com.google.common.eventbus.EventBus;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.session.events.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe implementation of {@link ScraperSession}.
 * 
 * <p>This implementation provides:</p>
 * <ul>
 *   <li>Thread-safe status transitions using volatile and synchronized blocks</li>
 *   <li>Automatic metrics tracking on state changes</li>
 *   <li>Blocking wait operations for completion</li>
 *   <li>Cancellation support with terminal state protection</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>All public methods are thread-safe. Internal state is protected by:</p>
 * <ul>
 *   <li>Volatile fields for status and error</li>
 *   <li>Synchronized blocks for state transitions</li>
 *   <li>ReentrantLock for wait/notify coordination</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create session
 * ScraperSessionImpl session = new ScraperSessionImpl(
 *     UUID.randomUUID().toString(),
 *     "client-123",
 *     config
 * );
 * 
 * // Register in registry
 * registry.register(session);
 * 
 * // Start execution
 * executorService.submit(() -> {
 *     try {
 *         session.markRunning(context);
 *         scraper.execute(context);
 *         session.markCompleted();
 *     } catch (Exception e) {
 *         session.markFailed(e);
 *     }
 * });
 * 
 * // Monitor or wait
 * session.awaitCompletion();
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see ScraperSession
 * @see SessionStatus
 * @see SessionMetrics
 */
public class ScraperSessionImpl implements ScraperSession {
    
    private final String sessionId;
    private final String clientId;
    private final Config config;
    private final SessionMetrics metrics;
    private final EventBus eventBus;
    
    private volatile SessionStatus status;
    private volatile DynamicScopeContext context;
    private volatile Throwable error;
    
    private final ReentrantLock completionLock;
    private final Condition completionCondition;
    
    /**
     * Creates a new session instance.
     * 
     * @param sessionId unique identifier for this session (must not be null or empty)
     * @param clientId client identifier, or null if not associated with a client
     * @param config the configuration to execute (must not be null)
     * @throws IllegalArgumentException if sessionId is null/empty or config is null
     */
    public ScraperSessionImpl(String sessionId, String clientId, Config config) {
        this(sessionId, clientId, config, new EventBus());
    }
    
    /**
     * Creates a new session instance with custom EventBus.
     * 
     * @param sessionId unique identifier for this session (must not be null or empty)
     * @param clientId client identifier, or null if not associated with a client
     * @param config the configuration to execute (must not be null)
     * @param eventBus the EventBus for publishing session events (must not be null)
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public ScraperSessionImpl(String sessionId, String clientId, Config config, EventBus eventBus) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }
        
        this.sessionId = sessionId;
        this.clientId = clientId;
        this.config = config;
        this.metrics = new SessionMetrics();
        this.eventBus = eventBus;
        this.status = SessionStatus.PENDING;
        this.context = null;
        this.error = null;
        
        this.completionLock = new ReentrantLock();
        this.completionCondition = completionLock.newCondition();
        
        // Publish creation event
        eventBus.post(new SessionCreatedEvent(this));
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
    public SessionStatus getStatus() {
        return status;
    }
    
    @Override
    public SessionMetrics getMetrics() {
        return metrics;
    }
    
    @Override
    public Config getConfig() {
        return config;
    }
    
    @Override
    public DynamicScopeContext getContext() {
        return context;
    }
    
    @Override
    public Throwable getError() {
        return error;
    }
    
    @Override
    public synchronized boolean cancel() {
        if (status.isTerminal()) {
            return false;
        }
        
        status = SessionStatus.CANCELLED;
        metrics.markEnded();
        eventBus.post(new SessionCancelledEvent(this));
        signalCompletion();
        return true;
    }
    
    @Override
    public void awaitCompletion() throws InterruptedException {
        completionLock.lock();
        try {
            while (!status.isTerminal()) {
                completionCondition.await();
            }
        } finally {
            completionLock.unlock();
        }
    }
    
    @Override
    public boolean awaitCompletion(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative: " + timeoutMillis);
        }
        
        completionLock.lock();
        try {
            long remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while (!status.isTerminal() && remainingNanos > 0) {
                remainingNanos = completionCondition.awaitNanos(remainingNanos);
            }
            return status.isTerminal();
        } finally {
            completionLock.unlock();
        }
    }
    
    /**
     * Marks this session as running and associates it with an execution context.
     * 
     * <p>This method should be called when execution actually starts.
     * Status transitions from PENDING to RUNNING.</p>
     * 
     * @param context the execution context (must not be null)
     * @throws IllegalArgumentException if context is null
     * @throws IllegalStateException if session is not in PENDING state
     */
    public synchronized void markRunning(DynamicScopeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (status != SessionStatus.PENDING) {
            throw new IllegalStateException(
                "Cannot mark as running from state: " + status);
        }
        
        this.context = context;
        this.status = SessionStatus.RUNNING;
        eventBus.post(new SessionStartedEvent(this));
    }
    
    /**
     * Marks this session as successfully completed.
     * 
     * <p>Status transitions to COMPLETED and metrics end time is recorded.</p>
     * 
     * @throws IllegalStateException if session is not in RUNNING state
     */
    public synchronized void markCompleted() {
        if (status != SessionStatus.RUNNING) {
            throw new IllegalStateException(
                "Cannot mark as completed from state: " + status);
        }
        
        status = SessionStatus.COMPLETED;
        metrics.markEnded();
        eventBus.post(new SessionCompletedEvent(this));
        signalCompletion();
    }
    
    /**
     * Marks this session as failed due to an error.
     * 
     * <p>Status transitions to FAILED and the error is recorded.
     * If error is null, a generic exception is created.</p>
     * 
     * @param error the error that caused failure (may be null)
     */
    public synchronized void markFailed(Throwable error) {
        if (status.isTerminal()) {
            // Already in terminal state, don't change
            return;
        }
        
        this.error = error != null ? error : 
            new RuntimeException("Session failed with unknown error");
        this.status = SessionStatus.FAILED;
        metrics.markEnded();
        eventBus.post(new SessionFailedEvent(this, this.error));
        signalCompletion();
    }
    
    /**
     * Signals all threads waiting for completion.
     * Must be called while holding the instance lock.
     */
    private void signalCompletion() {
        completionLock.lock();
        try {
            completionCondition.signalAll();
        } finally {
            completionLock.unlock();
        }
    }
    
    @Override
    public String toString() {
        return "ScraperSession{" +
                "id='" + sessionId + '\'' +
                ", client='" + clientId + '\'' +
                ", status=" + status +
                ", metrics=" + metrics +
                '}';
    }
}

