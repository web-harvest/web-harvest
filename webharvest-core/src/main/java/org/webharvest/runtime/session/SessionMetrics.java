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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks execution metrics for a scraper session.
 * 
 * <p>This class provides thread-safe access to session timing and progress information.
 * All metric updates use atomic operations to ensure consistency in multi-threaded
 * environments.</p>
 * 
 * <h3>Tracked Metrics:</h3>
 * <ul>
 *   <li><b>Start Time</b> - When session execution began</li>
 *   <li><b>End Time</b> - When session execution completed (null if running)</li>
 *   <li><b>Duration</b> - Elapsed time (live for running sessions)</li>
 *   <li><b>Processed Elements</b> - Number of plugins/processors executed</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ScraperSession session = service.executeAsync(config, client, options);
 * SessionMetrics metrics = session.getMetrics();
 * 
 * // Monitor progress
 * System.out.println("Started at: " + metrics.getStartTime());
 * System.out.println("Duration: " + metrics.getDuration().getSeconds() + "s");
 * System.out.println("Elements processed: " + metrics.getProcessedElements());
 * 
 * // After completion
 * if (session.getStatus().isTerminal()) {
 *     System.out.println("Finished at: " + metrics.getEndTime());
 *     System.out.println("Total duration: " + metrics.getDuration());
 * }
 * }</pre>
 * 
 * <h3>Thread Safety:</h3>
 * <p>All methods are thread-safe. Multiple threads can read metrics while
 * the session is executing without synchronization.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 * @see ScraperSession
 * @see SessionStatus
 */
public class SessionMetrics {
    
    private final Instant startTime;
    private volatile Instant endTime;
    private final AtomicLong processedElements;
    
    /**
     * Creates a new metrics instance with current time as start time.
     */
    public SessionMetrics() {
        this.startTime = Instant.now();
        this.endTime = null;
        this.processedElements = new AtomicLong(0);
    }
    
    /**
     * Creates a metrics instance with specified start time.
     * Used for testing or when recreating metrics from persistence.
     * 
     * @param startTime the session start time
     * @throws IllegalArgumentException if startTime is null
     */
    public SessionMetrics(Instant startTime) {
        if (startTime == null) {
            throw new IllegalArgumentException("Start time cannot be null");
        }
        this.startTime = startTime;
        this.endTime = null;
        this.processedElements = new AtomicLong(0);
    }
    
    /**
     * Returns the time when session execution started.
     * 
     * @return the start time (never null)
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Returns the time when session execution ended.
     * 
     * @return the end time, or null if session is still running
     */
    public Instant getEndTime() {
        return endTime;
    }
    
    /**
     * Marks the session as ended by setting the end time to now.
     * This method is idempotent - calling it multiple times has no effect
     * after the first call.
     */
    public void markEnded() {
        if (endTime == null) {
            endTime = Instant.now();
        }
    }
    
    /**
     * Marks the session as ended with a specific time.
     * Used for testing or when recreating metrics from persistence.
     * 
     * @param endTime the end time to set
     * @throws IllegalArgumentException if endTime is null or before startTime
     */
    public void markEnded(Instant endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        this.endTime = endTime;
    }
    
    /**
     * Returns the elapsed duration of the session.
     * 
     * <p>For running sessions, returns the time elapsed since start.
     * For completed sessions, returns the total execution time.</p>
     * 
     * @return the duration (never null)
     */
    public Duration getDuration() {
        Instant end = (endTime != null) ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }
    
    /**
     * Returns the number of elements (plugins/processors) that have been processed.
     * 
     * @return the count of processed elements (never negative)
     */
    public long getProcessedElements() {
        return processedElements.get();
    }
    
    /**
     * Increments the processed elements counter by one.
     * Thread-safe operation using atomic increment.
     * 
     * @return the new count after incrementing
     */
    public long incrementProcessedElements() {
        return processedElements.incrementAndGet();
    }
    
    /**
     * Increments the processed elements counter by a specified amount.
     * Thread-safe operation using atomic add.
     * 
     * @param count the number to add (must be non-negative)
     * @return the new count after incrementing
     * @throws IllegalArgumentException if count is negative
     */
    public long incrementProcessedElements(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative: " + count);
        }
        return processedElements.addAndGet(count);
    }
    
    /**
     * Calculates the average processing rate (elements per second).
     * 
     * @return elements per second, or 0 if no time has elapsed
     */
    public double getProcessingRate() {
        Duration duration = getDuration();
        long seconds = duration.getSeconds();
        if (seconds == 0) {
            return 0.0;
        }
        return (double) getProcessedElements() / seconds;
    }
    
    /**
     * Returns a string representation of these metrics.
     * 
     * @return a string describing the metrics
     */
    @Override
    public String toString() {
        return "SessionMetrics{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDuration() +
                ", processedElements=" + processedElements.get() +
                ", rate=" + String.format("%.2f", getProcessingRate()) + " elem/s" +
                '}';
    }
}


