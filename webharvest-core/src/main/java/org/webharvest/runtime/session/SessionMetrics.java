package org.webharvest.runtime.session;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics for a scraper session, tracking timing, progress, and performance.
 * 
 * <p>This class is thread-safe and can be accessed from multiple threads
 * while a session is executing.</p>
 * 
 * <h3>Metrics Tracked:</h3>
 * <ul>
 *   <li><b>Timing:</b> Start time, end time, duration</li>
 *   <li><b>Progress:</b> Number of elements processed</li>
 *   <li><b>Performance:</b> Elements per second</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * SessionMetrics metrics = session.getMetrics();
 * 
 * // Get timing information
 * Instant startTime = metrics.getStartTime();
 * Duration duration = metrics.getDuration();
 * 
 * // Get progress
 * long processed = metrics.getProcessedElements();
 * 
 * // Calculate performance
 * double rate = metrics.getElementsPerSecond();
 * }</pre>
 * 
 * @author Robert Bala
 * @since 2.2.0
 * @version %I%, %G%
 */
public class SessionMetrics {
    
    private final Instant startTime;
    private volatile Instant endTime;
    private final AtomicLong processedElements;
    
    /**
     * Creates metrics with current time as start time.
     */
    public SessionMetrics() {
        this(Instant.now());
    }
    
    /**
     * Creates metrics with specified start time.
     * 
     * @param startTime the start time (must not be null)
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
     * Returns the session start time.
     * 
     * @return the start time (never null)
     */
    public Instant getStartTime() {
        return startTime;
    }
    
    /**
     * Returns the session end time.
     * 
     * @return the end time, or null if session hasn't ended
     */
    public Instant getEndTime() {
        return endTime;
    }
    
    /**
     * Marks the session as ended with current time.
     * 
     * <p>This method is idempotent - calling it multiple times
     * will not change the end time after it's been set.</p>
     */
    public void markEnded() {
        markEnded(Instant.now());
    }
    
    /**
     * Marks the session as ended with specified time.
     * 
     * <p>This method is idempotent - calling it multiple times
     * will not change the end time after it's been set.</p>
     * 
     * @param endTime the end time (must not be null and must not be before start time)
     * @throws IllegalArgumentException if endTime is null or before start time
     */
    public synchronized void markEnded(Instant endTime) {
        if (endTime == null) {
            throw new IllegalArgumentException("End time cannot be null");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time");
        }
        if (this.endTime == null) {
            this.endTime = endTime;
        }
    }
    
    /**
     * Returns the session duration.
     * 
     * <p>If session has ended, returns the duration between start and end time.
     * If session is still running, returns the duration from start to now.</p>
     * 
     * @return the duration (never null)
     */
    public Duration getDuration() {
        Instant end = (endTime != null) ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }
    
    /**
     * Returns the number of elements processed.
     * 
     * @return the count of processed elements
     */
    public long getProcessedElements() {
        return processedElements.get();
    }
    
    /**
     * Increments the processed elements counter by one.
     * 
     * @return the new count after incrementing
     */
    public long incrementProcessedElements() {
        return processedElements.incrementAndGet();
    }
    
    /**
     * Increments the processed elements counter by specified amount.
     * 
     * @param count the number of elements to add
     * @return the new count after incrementing
     * @throws IllegalArgumentException if count is negative
     */
    public long incrementProcessedElements(long count) {
        if (count < 0) {
            throw new IllegalArgumentException("Count cannot be negative: " + count);
        }
        if (count > 0) {
            return processedElements.addAndGet(count);
        }
        return processedElements.get();
    }
    
    /**
     * Returns the processing rate in elements per second.
     * Alias for {@link #getElementsPerSecond()}.
     * 
     * @return the elements per second
     */
    public double getProcessingRate() {
        return getElementsPerSecond();
    }
    
    /**
     * Returns the processing rate in elements per second.
     * 
     * <p>Returns 0 if duration is less than 1 second or no elements processed.</p>
     * 
     * @return the elements per second
     */
    public double getElementsPerSecond() {
        long elements = processedElements.get();
        if (elements == 0) {
            return 0.0;
        }
        
        Duration duration = getDuration();
        long seconds = duration.getSeconds();
        if (seconds == 0) {
            return 0.0;
        }
        
        return (double) elements / seconds;
    }
    
    @Override
    public String toString() {
        return "SessionMetrics{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", duration=" + getDuration() +
                ", processedElements=" + processedElements.get() +
                ", elementsPerSecond=" + String.format("%.2f", getElementsPerSecond()) +
                '}';
    }
}
