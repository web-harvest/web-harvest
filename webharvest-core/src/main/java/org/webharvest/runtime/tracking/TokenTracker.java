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

package org.webharvest.runtime.tracking;

/**
 * Tracks resource consumption for token-based usage metering.
 * 
 * <p>Token tracking enables monitoring and measuring resource consumption
 * during scraper execution. This data can be used for:</p>
 * <ul>
 *   <li>Usage analytics and optimization</li>
 *   <li>Billing and monetization (future)</li>
 *   <li>Quota enforcement (future)</li>
 *   <li>Performance monitoring</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>Implementations must be thread-safe. Multiple threads may record
 * resource usage concurrently during scraper execution.</p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * // Create tracker for session
 * TokenTracker tracker = new TokenTrackerImpl("session-123", "client-456");
 * 
 * // Record HTTP request
 * tracker.record(ResourceType.HTTP_REQUEST, 1);
 * tracker.record(ResourceType.HTTP_BYTES, 1024000); // 1MB
 * 
 * // Record CPU time
 * long start = System.currentTimeMillis();
 * doWork();
 * long duration = System.currentTimeMillis() - start;
 * tracker.record(ResourceType.CPU_TIME, duration);
 * 
 * // Get current usage
 * TokenUsage usage = tracker.getUsage();
 * System.out.println("Consumed: " + usage);
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see TokenUsage
 * @see ResourceType
 */
public interface TokenTracker {
    
    /**
     * Returns the session identifier this tracker is associated with.
     * 
     * @return the session ID (never null)
     */
    String getSessionId();
    
    /**
     * Returns the client identifier this tracker is associated with.
     * 
     * @return the client ID, or null if not associated with a client
     */
    String getClientId();
    
    /**
     * Records resource consumption.
     * 
     * <p>This method is thread-safe and can be called concurrently
     * from multiple threads.</p>
     * 
     * @param type the type of resource consumed
     * @param amount the amount consumed (must be non-negative)
     * @throws IllegalArgumentException if type is null or amount is negative
     */
    void record(ResourceType type, long amount);
    
    /**
     * Returns a snapshot of current resource usage.
     * 
     * <p>The returned {@link TokenUsage} is immutable and represents
     * the state at the time of the call.</p>
     * 
     * @return current usage snapshot (never null)
     */
    TokenUsage getUsage();
    
    /**
     * Resets all tracked usage to zero.
     * 
     * <p>This method is useful for testing or when reusing a tracker
     * for multiple operations.</p>
     */
    void reset();
}


