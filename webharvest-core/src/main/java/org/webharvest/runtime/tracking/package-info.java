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
 * Token-based resource usage tracking for WebHarvest sessions.
 * 
 * <h2>Overview</h2>
 * <p>This package provides tracking of resource consumption during scraper execution.
 * Resources are measured in "tokens" that can be used for:</p>
 * <ul>
 *   <li>Usage analytics and reporting</li>
 *   <li>Performance monitoring</li>
 *   <li>Billing and monetization (future)</li>
 *   <li>Quota enforcement (future)</li>
 * </ul>
 * 
 * <h3>Tracked Resources:</h3>
 * <ul>
 *   <li><b>HTTP_REQUEST:</b> Number of HTTP requests</li>
 *   <li><b>HTTP_BYTES:</b> Bytes downloaded</li>
 *   <li><b>CPU_TIME:</b> CPU time consumed (milliseconds)</li>
 *   <li><b>MEMORY_PEAK:</b> Peak memory usage (bytes)</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create tracker
 * TokenTracker tracker = new TokenTrackerImpl("session-123", "client-456");
 * 
 * // Record HTTP request
 * tracker.record(ResourceType.HTTP_REQUEST, 1);
 * tracker.record(ResourceType.HTTP_BYTES, 52428); // 51 KB
 * 
 * // Record CPU time
 * long start = System.currentTimeMillis();
 * doWork();
 * tracker.record(ResourceType.CPU_TIME, System.currentTimeMillis() - start);
 * 
 * // Get usage snapshot
 * TokenUsage usage = tracker.getUsage();
 * System.out.println("HTTP Requests: " + usage.getHttpRequests());
 * System.out.println("Bytes: " + usage.getHttpBytes());
 * System.out.println("CPU Time: " + usage.getCpuTimeMillis() + "ms");
 * }</pre>
 * 
 * <h2>Thread Safety</h2>
 * <p>{@link org.webharvest.runtime.tracking.TokenTrackerImpl} uses atomic counters
 * for lock-free concurrent recording from multiple threads.</p>
 * 
 * <h2>Future Integration</h2>
 * <p>v2.3+ will add:</p>
 * <ul>
 *   <li>Prometheus metrics export</li>
 *   <li>Quota enforcement based on token limits</li>
 *   <li>Pricing models and billing integration</li>
 * </ul>
 * 
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.tracking;


