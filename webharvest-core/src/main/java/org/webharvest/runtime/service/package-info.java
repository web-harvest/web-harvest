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
 * High-level service API for WebHarvest session management.
 * 
 * <h2>Overview</h2>
 * <p>This package provides the main entry point for executing WebHarvest configurations
 * with session management, concurrent execution, and monitoring.</p>
 * 
 * <h3>Key Components:</h3>
 * <ul>
 *   <li>{@link org.webharvest.runtime.service.WebHarvestService} - Main service interface</li>
 *   <li>{@link org.webharvest.runtime.service.SessionOptions} - Execution configuration</li>
 * </ul>
 * 
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create service
 * WebHarvestService service = new WebHarvestServiceImpl();
 * 
 * // Prepare configuration
 * Config config = ConfigFactory.create(new File("scraper.xml"));
 * ClientContext client = ClientContext.of("my-app");
 * SessionOptions options = SessionOptions.DEFAULT;
 * 
 * // Execute asynchronously
 * ScraperSession session = service.executeAsync(config, client, options);
 * 
 * // Monitor progress
 * while (!session.getStatus().isTerminal()) {
 *     SessionMetrics metrics = session.getMetrics();
 *     System.out.printf("Progress: %d elements in %ds%n",
 *         metrics.getProcessedElements(),
 *         metrics.getDuration().getSeconds());
 *     Thread.sleep(1000);
 * }
 * 
 * // Check result
 * if (session.getStatus().isSuccessful()) {
 *     System.out.println("Completed successfully!");
 * }
 * 
 * // Cleanup
 * service.shutdown();
 * }</pre>
 * 
 * <h2>Multi-Session Execution</h2>
 * <pre>{@code
 * WebHarvestService service = new WebHarvestServiceImpl();
 * 
 * // Execute multiple configurations concurrently
 * List<ScraperSession> sessions = new ArrayList<>();
 * for (File configFile : configFiles) {
 *     Config config = ConfigFactory.create(configFile);
 *     ScraperSession session = service.executeAsync(config, client, options);
 *     sessions.add(session);
 * }
 * 
 * // Wait for all
 * for (ScraperSession session : sessions) {
 *     session.awaitCompletion();
 * }
 * 
 * service.shutdown();
 * }</pre>
 * 
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.service;


