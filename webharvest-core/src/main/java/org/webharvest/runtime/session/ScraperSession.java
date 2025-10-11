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

import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;

/**
 * Represents a single scraper execution session with unique identifier and lifecycle management.
 * 
 * <p>A session encapsulates all state and metadata for one execution of a WebHarvest configuration.
 * Each session has:</p>
 * <ul>
 *   <li>A unique session ID (UUID)</li>
 *   <li>An optional client identifier for multi-tenant scenarios</li>
 *   <li>Execution status tracking ({@link SessionStatus})</li>
 *   <li>Performance metrics ({@link SessionMetrics})</li>
 *   <li>Configuration being executed</li>
 *   <li>Runtime execution context</li>
 *   <li>Error information (if failed)</li>
 * </ul>
 * 
 * <h3>Session Lifecycle:</h3>
 * <pre>
 * 1. Session created    → PENDING status
 * 2. Execution starts   → RUNNING status
 * 3. Execution completes → COMPLETED status (success)
 *                       → FAILED status (error)
 *                       → CANCELLED status (cancelled)
 * </pre>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Execute configuration asynchronously
 * ScraperSession session = webHarvestService.executeAsync(config, clientContext, options);
 * 
 * // Get session identifier for tracking
 * String sessionId = session.getSessionId();
 * System.out.println("Started session: " + sessionId);
 * 
 * // Monitor progress
 * while (session.getStatus() == SessionStatus.RUNNING) {
 *     SessionMetrics metrics = session.getMetrics();
 *     System.out.println("Progress: " + metrics.getProcessedElements() + " elements");
 *     Thread.sleep(1000);
 * }
 * 
 * // Check result
 * if (session.getStatus() == SessionStatus.COMPLETED) {
 *     DynamicScopeContext context = session.getContext();
 *     Variable result = context.getVar("result");
 *     System.out.println("Success: " + result);
 * } else if (session.getStatus() == SessionStatus.FAILED) {
 *     Throwable error = session.getError();
 *     System.err.println("Failed: " + error.getMessage());
 * }
 * }</pre>
 * 
 * <h3>Multi-tenant Support:</h3>
 * <p>Sessions can be associated with a client ID for multi-tenant scenarios:</p>
 * <pre>{@code
 * // All sessions for a specific client
 * List<ScraperSession> clientSessions = registry.getSessionsByClientId("client-123");
 * 
 * // Useful for:
 * // - Resource usage tracking per client
 * // - Quota enforcement per client
 * // - Billing and monetization
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>Implementations must be thread-safe. Multiple threads may query session state
 * while execution is in progress.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see SessionStatus
 * @see SessionMetrics
 * @see SessionRegistry
 * @see org.webharvest.runtime.service.WebHarvestService
 */
public interface ScraperSession {
    
    /**
     * Returns the unique identifier for this session.
     * 
     * <p>Session IDs are typically UUIDs and are guaranteed to be unique
     * across all sessions in the system.</p>
     * 
     * @return the session ID (never null)
     */
    String getSessionId();
    
    /**
     * Returns the client identifier associated with this session.
     * 
     * <p>Client IDs enable multi-tenant scenarios where sessions from different
     * clients need to be isolated and tracked separately.</p>
     * 
     * @return the client ID, or null if not associated with a client
     */
    String getClientId();
    
    /**
     * Returns the current execution status of this session.
     * 
     * @return the current status (never null)
     */
    SessionStatus getStatus();
    
    /**
     * Returns the execution metrics for this session.
     * 
     * <p>Metrics are updated in real-time during execution and include timing,
     * progress, and performance data.</p>
     * 
     * @return the session metrics (never null)
     */
    SessionMetrics getMetrics();
    
    /**
     * Returns the configuration being executed by this session.
     * 
     * @return the configuration (never null)
     */
    Config getConfig();
    
    /**
     * Returns the execution context for this session.
     * 
     * <p>The context contains all variables created during execution.
     * This method returns null until execution has started.</p>
     * 
     * <p><b>Important:</b> Variables are only accessible after plugins
     * have executed. For running sessions, the context reflects the
     * current state up to that point.</p>
     * 
     * @return the execution context, or null if execution hasn't started
     */
    DynamicScopeContext getContext();
    
    /**
     * Returns the error that caused this session to fail.
     * 
     * <p>Only populated if status is {@link SessionStatus#FAILED}.</p>
     * 
     * @return the error, or null if session didn't fail
     */
    Throwable getError();
    
    /**
     * Attempts to cancel this session.
     * 
     * <p>Cancellation is best-effort and may not succeed if the session is already
     * in a terminal state. If successful, status transitions to {@link SessionStatus#CANCELLED}.</p>
     * 
     * <p><b>Note:</b> Cancellation is cooperative and depends on the executing
     * plugins checking for interruption. Some long-running operations may take
     * time to respond to cancellation.</p>
     * 
     * @return true if cancellation was initiated; false if session is already terminal
     */
    boolean cancel();
    
    /**
     * Blocks until this session reaches a terminal state.
     * 
     * <p>Convenient method for synchronous waiting. Equivalent to:</p>
     * <pre>{@code
     * while (!session.getStatus().isTerminal()) {
     *     Thread.sleep(100);
     * }
     * }</pre>
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    void awaitCompletion() throws InterruptedException;
    
    /**
     * Blocks until this session reaches a terminal state or timeout expires.
     * 
     * @param timeoutMillis maximum time to wait in milliseconds
     * @return true if session completed within timeout; false if timed out
     * @throws InterruptedException if interrupted while waiting
     * @throws IllegalArgumentException if timeoutMillis is negative
     */
    boolean awaitCompletion(long timeoutMillis) throws InterruptedException;
}

