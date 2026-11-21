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

package org.webharvest.runtime.service;

import org.webharvest.definition.Config;
import org.webharvest.runtime.client.ClientContext;
import org.webharvest.runtime.session.ScraperSession;

import java.util.List;
import java.util.Optional;

/**
 * High-level service for managing WebHarvest scraper sessions.
 * 
 * <p>This service provides asynchronous configuration execution and session
 * lifecycle management. Client IDs are used for session association and listing;
 * they are not an authentication or tenant-isolation boundary.</p>
 * 
 * <h3>Features:</h3>
 * <ul>
 *   <li>Asynchronous execution with session tracking</li>
 *   <li>Session lifecycle management and cancellation</li>
 *   <li>Multi-session concurrent execution</li>
 *   <li>Client-based session association and listing</li>
 *   <li>Session lifecycle and duration information</li>
 * </ul>
 * 
 * <h3>Basic Usage:</h3>
 * <pre>{@code
 * WebHarvestService service = new WebHarvestServiceImpl();
 * 
 * ClientContext client = ClientContext.of("my-app");
 * SessionOptions options = SessionOptions.DEFAULT;
 * Config config = ConfigFactory.create(new File("scraper.xml"));
 * 
 * // Execute asynchronously
 * ScraperSession session = service.executeAsync(config, client, options);
 * 
 * // Monitor progress
 * while (!session.getStatus().isTerminal()) {
 *     System.out.println("Progress: " + session.getMetrics());
 *     Thread.sleep(1000);
 * }
 * 
 * // Check result
 * if (session.getStatus().isSuccessful()) {
 *     System.out.println("Success!");
 * }
 * }</pre>
 * 
 * <h3>Multi-Session Example:</h3>
 * <pre>{@code
 * // Execute multiple configurations concurrently
 * List<ScraperSession> sessions = new ArrayList<>();
 * 
 * for (File configFile : configFiles) {
 *     Config config = ConfigFactory.create(configFile);
 *     ScraperSession session = service.executeAsync(config, client, options);
 *     sessions.add(session);
 * }
 * 
 * // Wait for all to complete
 * for (ScraperSession session : sessions) {
 *     session.awaitCompletion();
 * }
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see ScraperSession
 * @see ClientContext
 * @see SessionOptions
 */
public interface WebHarvestService {
    
    /**
     * Executes a configuration asynchronously.
     * 
     * <p>The configuration begins executing immediately in a background thread.
     * The returned session can be used to monitor progress and retrieve results.</p>
     * 
     * @param config the configuration to execute (must not be null)
     * @param client the client context (may be null for anonymous execution)
     * @param options execution options (may be null for defaults)
     * @return a session representing the execution (never null)
     * @throws IllegalArgumentException if config is null
     */
    ScraperSession executeAsync(Config config, ClientContext client, SessionOptions options);
    
    /**
     * Retrieves a session by its unique identifier.
     * 
     * @param sessionId the session ID to look up
     * @return an Optional containing the session if found
     * @throws IllegalArgumentException if sessionId is null
     */
    Optional<ScraperSession> getSession(String sessionId);
    
    /**
     * Retrieves all sessions for a specific client.
     * 
     * @param clientId the client ID
     * @return list of sessions (never null, may be empty)
     * @throws IllegalArgumentException if clientId is null
     */
    List<ScraperSession> listSessions(String clientId);
    
    /**
     * Retrieves all registered sessions.
     * 
     * <p>Use with caution - may return a large list in production systems.</p>
     * 
     * @return list of all sessions (never null, may be empty)
     */
    List<ScraperSession> listAllSessions();
    
    /**
     * Cancels a running session.
     * 
     * @param sessionId the session ID to cancel
     * @return true if session was found and cancelled; false if not found or already terminal
     * @throws IllegalArgumentException if sessionId is null
     */
    boolean cancelSession(String sessionId);
    
    /**
     * Shuts down the service gracefully.
     * 
     * <p>Waits for all running sessions to complete before shutting down.
     * New session submissions are rejected after shutdown is initiated.</p>
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    void shutdown() throws InterruptedException;
    
    /**
     * Shuts down the service immediately.
     * 
     * <p>Attempts to cancel all running sessions.
     * May not wait for sessions to complete.</p>
     */
    void shutdownNow();
    
    /**
     * Checks if the service has been shut down.
     * 
     * @return true if shutdown; false if still accepting sessions
     */
    boolean isShutdown();
}






