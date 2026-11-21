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
 * Resource counter interfaces and immutable usage snapshots for WebHarvest.
 *
 * <h2>Overview</h2>
 * <p>This package defines resource categories and a counter API that stores
 * explicitly recorded values. These counters are not a billing or quota system.
 * The built-in execution path does not currently record real runtime resource
 * events into {@link TokenTracker}; see
 * <a href="https://github.com/web-harvest/web-harvest/issues/168">issue #168</a>.</p>
 *
 * <h3>Available Counter Types</h3>
 * <ul>
 *   <li>{@link ResourceType#HTTP_REQUEST} — HTTP request count</li>
 *   <li>{@link ResourceType#HTTP_BYTES} — HTTP byte count</li>
 *   <li>{@link ResourceType#CPU_TIME} — explicitly recorded CPU time in milliseconds</li>
 *   <li>{@link ResourceType#MEMORY_PEAK} — explicitly recorded peak memory in bytes</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TokenTracker tracker = new TokenTrackerImpl("session-123", "client-456");
 * tracker.record(ResourceType.HTTP_REQUEST, 1);
 * tracker.record(ResourceType.HTTP_BYTES, 52428);
 * TokenUsage usage = tracker.getUsage();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>The built-in {@link TokenTrackerImpl} uses atomic counters for concurrent
 * recording. A counter value represents only values explicitly supplied to it;
 * it is not evidence that a runtime resource was measured.</p>
 *
 * @since 2.2.0
 * @author Robert Bala
 */
package org.webharvest.runtime.tracking;






