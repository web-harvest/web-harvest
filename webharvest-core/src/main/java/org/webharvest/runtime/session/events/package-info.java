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
 * Session lifecycle events for monitoring and integration.
 * 
 * <h2>Overview</h2>
 * <p>This package provides events published during session lifecycle transitions.
 * Events are published via Guava {@link com.google.common.eventbus.EventBus} and can
 * be subscribed to for:</p>
 * <ul>
 *   <li>Monitoring and logging</li>
 *   <li>Metrics collection</li>
 *   <li>Caller-defined event handling and logging</li>
 *   <li>Custom business logic</li>
 * </ul>
 * 
 * <h3>Event Types:</h3>
 * <ul>
 *   <li>{@link org.webharvest.runtime.session.events.SessionCreatedEvent} - Session instantiated</li>
 *   <li>{@link org.webharvest.runtime.session.events.SessionStartedEvent} - Execution started</li>
 *   <li>{@link org.webharvest.runtime.session.events.SessionCompletedEvent} - Execution completed successfully</li>
 *   <li>{@link org.webharvest.runtime.session.events.SessionFailedEvent} - Execution failed</li>
 *   <li>{@link org.webharvest.runtime.session.events.SessionCancelledEvent} - Session cancelled</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Subscribe to events
 * EventBus eventBus = new EventBus();
 * eventBus.register(new Object() {
 *     @Subscribe
 *     public void onSessionStarted(SessionStartedEvent event) {
 *         System.out.println("Session " + event.getSessionId() + " started");
 *     }
 *     
 *     @Subscribe
 *     public void onSessionCompleted(SessionCompletedEvent event) {
 *         SessionMetrics metrics = event.getMetrics();
 *         System.out.printf("Session completed in %s%n", metrics.getDuration());
 *     }
 *     
 *     @Subscribe
 *     public void onSessionFailed(SessionFailedEvent event) {
 *         System.err.println("Session failed: " + event.getError().getMessage());
 *     }
 * });
 * 
 * // Create session with this EventBus
 * ScraperSession session = new ScraperSessionImpl(sessionId, clientId, config, eventBus);
 * }</pre>
 * 
 * <h2>Integration boundary</h2>
 * <p>Consumers may subscribe to events on an {@link com.google.common.eventbus.EventBus}
 * supplied to a session implementation. The built-in {@code WebHarvestService}
 * does not expose its internal event bus, and no Kafka publisher or external
 * messaging adapter is included in this package.</p>
 * 
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.session.events;






