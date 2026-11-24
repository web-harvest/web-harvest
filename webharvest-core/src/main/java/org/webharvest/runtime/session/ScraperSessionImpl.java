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

import java.util.concurrent.CountDownLatch;
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
public class ScraperSessionImpl implements ExecutionTrackedSession {
    
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
    private final CountDownLatch executionCompletion = new CountDownLatch(1);
    private volatile ExecutionState executionState = ExecutionState.NOT_SUBMITTED;
    private volatile TerminationDisposition terminationDisposition = TerminationDisposition.NOT_REQUESTED;
    private volatile Runnable cancellationHandler;
    
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
    
    /**
     * Attaches the service-owned task cancellation callback. If cancellation
     * happened during session-created event delivery, the callback is invoked
     * immediately after attachment.
     */
    public void attachCancellationHandler(Runnable handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Cancellation handler cannot be null");
        }
        boolean invoke;
        synchronized (this) {
            if (cancellationHandler != null) {
                throw new IllegalStateException("Cancellation handler already attached");
            }
            cancellationHandler = handler;
            invoke = status == SessionStatus.CANCELLED;
        }
        if (invoke) {
            handler.run();
        }
    }

    /** Marks the service-owned task as accepted by the execution coordinator. */
    public synchronized void markExecutionQueued() {
        if (executionState != ExecutionState.NOT_SUBMITTED) {
            throw new IllegalStateException("Execution was already submitted");
        }
        executionState = ExecutionState.QUEUED;
    }

    /** Returns true only when the queued task may enter its execution body. */
    public synchronized boolean markExecutionStarted() {
        if (executionState != ExecutionState.QUEUED || status.isTerminal()) {
            return false;
        }
        executionState = ExecutionState.RUNNING;
        return true;
    }

    /** Records the start of cooperative termination. */
    public synchronized void markGracefulTerminationRequested() {
        if (executionState != ExecutionState.EXITED
            && terminationDisposition == TerminationDisposition.NOT_REQUESTED) {
            terminationDisposition = TerminationDisposition.GRACE_IN_PROGRESS;
        }
    }

    /** Records an in-process kill escalation request; this does not kill a Java thread. */
    public synchronized void markKillRequested() {
        if (executionState != ExecutionState.EXITED
            && (terminationDisposition == TerminationDisposition.NOT_REQUESTED
                || terminationDisposition == TerminationDisposition.GRACE_IN_PROGRESS)) {
            terminationDisposition = TerminationDisposition.KILL_IN_PROGRESS;
        }
    }

    /** Records that in-process escalation did not make the worker exit. */
    public synchronized void markKillUnresponsive() {
        if (executionState != ExecutionState.EXITED
            && terminationDisposition == TerminationDisposition.KILL_IN_PROGRESS) {
            terminationDisposition = TerminationDisposition.KILL_UNRESPONSIVE;
        }
    }

    /** Records an interrupt request without claiming that the worker has exited. */
    public synchronized void markExecutionInterruptRequested() {
        if (executionState != ExecutionState.EXITED) {
            executionState = ExecutionState.INTERRUPT_REQUESTED;
        }
    }

    /** Records that the worker did not respond within the bounded grace period. */
    public synchronized void markExecutionUnresponsive() {
        if (executionState != ExecutionState.EXITED) {
            executionState = ExecutionState.UNRESPONSIVE;
        }
    }

    /** Records observed worker exit. This is the only transition proving quiescence. */
    public synchronized void markExecutionExited() {
        ExecutionState previousState = executionState;
        executionState = ExecutionState.EXITED;
        if (previousState == ExecutionState.NOT_SUBMITTED || previousState == ExecutionState.QUEUED) {
            if (terminationDisposition == TerminationDisposition.GRACE_IN_PROGRESS
                || terminationDisposition == TerminationDisposition.KILL_IN_PROGRESS
                || terminationDisposition == TerminationDisposition.KILL_UNRESPONSIVE) {
                terminationDisposition = TerminationDisposition.CANCELLED_BEFORE_START;
            }
        } else if (terminationDisposition == TerminationDisposition.GRACE_IN_PROGRESS) {
            terminationDisposition = TerminationDisposition.GRACEFUL;
        } else if (terminationDisposition == TerminationDisposition.KILL_IN_PROGRESS
            || terminationDisposition == TerminationDisposition.KILL_UNRESPONSIVE) {
            terminationDisposition = TerminationDisposition.WORKER_EXIT_CONFIRMED;
        }
        executionCompletion.countDown();
    }

    @Override
    public ExecutionState getExecutionState() {
        return executionState;
    }

    @Override
    public TerminationDisposition getTerminationDisposition() {
        return terminationDisposition;
    }

    @Override
    public void awaitExecutionCompletion() throws InterruptedException {
        if (executionState != ExecutionState.NOT_SUBMITTED) {
            executionCompletion.await();
        }
    }

    @Override
    public boolean awaitExecutionCompletion(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative: " + timeoutMillis);
        }
        if (executionState == ExecutionState.NOT_SUBMITTED || executionState == ExecutionState.EXITED) {
            return true;
        }
        return executionCompletion.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean cancel() {
        Runnable handler;
        synchronized (this) {
            if (status.isTerminal()) {
                return false;
            }
            status = SessionStatus.CANCELLED;
            metrics.markEnded();
            if (executionState == ExecutionState.NOT_SUBMITTED) {
                terminationDisposition = TerminationDisposition.CANCELLED_BEFORE_START;
            } else if (terminationDisposition == TerminationDisposition.NOT_REQUESTED) {
                terminationDisposition = TerminationDisposition.GRACE_IN_PROGRESS;
            }
            handler = cancellationHandler;
        }

        // Request worker interruption before notifying EventBus subscribers.
        // Neither this callback nor the event proves worker quiescence.
        if (handler != null) {
            handler.run();
        }
        signalCompletion();
        eventBus.post(new SessionCancelledEvent(this));
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
    public void markRunning(DynamicScopeContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        synchronized (this) {
            if (status != SessionStatus.PENDING) {
                throw new IllegalStateException(
                    "Cannot mark as running from state: " + status);
            }

            this.context = context;
            this.status = SessionStatus.RUNNING;
        }
        // Subscriber callbacks must not hold the session state monitor.
        eventBus.post(new SessionStartedEvent(this));
    }
    
    /**
     * Marks this session as successfully completed.
     * 
     * <p>Status transitions to COMPLETED and metrics end time is recorded.</p>
     * 
     * @throws IllegalStateException if session is not in RUNNING state
     */
    public void markCompleted() {
        synchronized (this) {
            if (status != SessionStatus.RUNNING) {
                throw new IllegalStateException(
                    "Cannot mark as completed from state: " + status);
            }

            status = SessionStatus.COMPLETED;
            metrics.markEnded();
        }
        signalCompletion();
        eventBus.post(new SessionCompletedEvent(this));
    }
    
    /**
     * Marks this session as failed due to an error.
     * 
     * <p>Status transitions to FAILED and the error is recorded.
     * If error is null, a generic exception is created.</p>
     * 
     * @param error the error that caused failure (may be null)
     */
    public void markFailed(Throwable error) {
        Throwable failure;
        synchronized (this) {
            if (status.isTerminal()) {
                // Already in terminal state, don't change
                return;
            }

            failure = error != null ? error
                : new RuntimeException("Session failed with unknown error");
            this.error = failure;
            this.status = SessionStatus.FAILED;
            metrics.markEnded();
        }
        signalCompletion();
        eventBus.post(new SessionFailedEvent(this, failure));
    }
    
    /**
     * Signals all threads waiting for completion.
     * Uses the independent completion condition; the session monitor is not required.
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

