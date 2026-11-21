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
 * Session management for WebHarvest scraper execution.
 * 
 * <h2>Overview</h2>
 * <p>This package provides session-based execution tracking for WebHarvest scrapers.
 * Each scraper execution is represented by a {@link org.webharvest.runtime.session.ScraperSession}
 * with unique identifier, status tracking, and performance metrics.</p>
 * 
 * <h3>Key Concepts:</h3>
 * <ul>
 *   <li><b>Session:</b> A single execution of a WebHarvest configuration</li>
 *   <li><b>Session ID:</b> Identifier (UUID for sessions created by the built-in service)</li>
 *   <li><b>Status:</b> Current state (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED)</li>
 *   <li><b>Metrics:</b> Session timing and explicitly recorded counters</li>
 *   <li><b>Registry:</b> Central storage for all active sessions</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create and register session
 * SessionRegistry registry = new SessionRegistryImpl();
 * Config config = ConfigFactory.create(new File("scraper.xml"));
 * 
 * String sessionId = UUID.randomUUID().toString();
 * ScraperSession session = new ScraperSessionImpl(sessionId, "client-123", config);
 * registry.register(session);
 * 
 * // Execute asynchronously
 * ExecutorService executor = Executors.newCachedThreadPool();
 * executor.submit(() -> {
 *     try {
 *         DynamicScopeContext context = new ScraperContext();
 *         session.markRunning(context);
 *         
 *         Scraper scraper = new Scraper();
 *         scraper.execute(context);
 *         
 *         session.markCompleted();
 *     } catch (Exception e) {
 *         session.markFailed(e);
 *     }
 * });
 * 
 * // Monitor progress
 * while (!session.getStatus().isTerminal()) {
 *     System.out.println("Status: " + session.getStatus());
 *     System.out.println("Elapsed: " + session.getMetrics().getDuration().getSeconds() + " seconds");
 *     Thread.sleep(1000);
 * }
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>The {@link org.webharvest.runtime.session.ScraperSession} contract requires
 * implementations to be thread-safe. The built-in {@link org.webharvest.runtime.session.ScraperSessionImpl}
 * uses volatile state and synchronized lifecycle transitions; {@link org.webharvest.runtime.session.SessionMetrics}
 * uses atomic counters, and {@link org.webharvest.runtime.session.SessionRegistryImpl}
 * uses a concurrent map. These implementation details do not guarantee thread safety
 * for custom implementations.</p>
 * 
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.session;






