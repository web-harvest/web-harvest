package org.webharvest.runtime.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe accumulator for session timing and explicitly recorded counters.
 * 
 * <p>The built-in service records session start/end timing. It does not currently
 * connect processed-element, HTTP-request, or plugin-duration counters to runtime
 * events; those values change only when a caller explicitly records them. See
 * <a href="https://github.com/web-harvest/web-harvest/issues/168">issue #168</a>.</p>
 * 
 * <h3>Available Values:</h3>
 * <ul>
 *   <li><b>Timing:</b> Start time, end time, and duration (recorded by the session lifecycle)</li>
 *   <li><b>Element count:</b> Explicitly incremented counter; not populated by the built-in service</li>
 *   <li><b>HTTP and plugin counters:</b> Explicit accumulator methods; no built-in runtime recorder</li>
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
    
    // HTTP metrics
    private final AtomicLong httpRequestCount;
    private final AtomicLong httpBytesDownloaded;
    private final AtomicLong httpBytesUploaded;
    private final AtomicLong httpTotalTimeMs;
    
    // Plugin breakdown
    private final Map<String, AtomicLong> pluginExecutionTimes;
    
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
        
        // Initialize HTTP metrics
        this.httpRequestCount = new AtomicLong(0);
        this.httpBytesDownloaded = new AtomicLong(0);
        this.httpBytesUploaded = new AtomicLong(0);
        this.httpTotalTimeMs = new AtomicLong(0);
        
        // Initialize plugin breakdown
        this.pluginExecutionTimes = new ConcurrentHashMap<>();
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
     * Returns the explicitly recorded processed-element count.
     * 
     * <p>The built-in service does not currently increment this counter.</p>
     * 
     * @return the recorded count
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
     * Returns the rate derived from the explicitly recorded element count.
     * 
     * <p>The built-in service does not currently populate the element counter.
     * Returns 0 if duration is less than 1 second or no elements were recorded.</p>
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
    
    /**
     * Records an HTTP request in this accumulator.
     * 
     * <p>This method must be called by an instrumentation source; the built-in
     * runtime does not currently record HTTP events here.</p>
     * 
     * @param bytesDownloaded bytes downloaded
     * @param bytesUploaded bytes uploaded
     * @param responseTimeMs response time in milliseconds
     */
    public void recordHttpRequest(long bytesDownloaded, long bytesUploaded, long responseTimeMs) {
        httpRequestCount.incrementAndGet();
        httpBytesDownloaded.addAndGet(bytesDownloaded);
        httpBytesUploaded.addAndGet(bytesUploaded);
        httpTotalTimeMs.addAndGet(responseTimeMs);
    }
    
    /**
     * Records a plugin duration in this accumulator.
     * 
     * <p>This method must be called by an instrumentation source; the built-in
     * runtime does not currently record plugin lifecycle timings here.</p>
     * 
     * @param pluginName the plugin name
     * @param executionTimeMs execution time in milliseconds
     */
    public void recordPluginExecution(String pluginName, long executionTimeMs) {
        pluginExecutionTimes.computeIfAbsent(pluginName, k -> new AtomicLong(0))
                           .addAndGet(executionTimeMs);
    }
    
    /**
     * Returns HTTP request count.
     * 
     * @return number of HTTP requests
     */
    public long getHttpRequestCount() {
        return httpRequestCount.get();
    }
    
    /**
     * Returns total HTTP bytes downloaded.
     * 
     * @return bytes downloaded
     */
    public long getHttpBytesDownloaded() {
        return httpBytesDownloaded.get();
    }
    
    /**
     * Returns total HTTP bytes uploaded.
     * 
     * @return bytes uploaded
     */
    public long getHttpBytesUploaded() {
        return httpBytesUploaded.get();
    }
    
    /**
     * Returns average HTTP response time.
     * 
     * @return average response time in milliseconds, or 0 if no requests
     */
    public long getAverageHttpResponseTime() {
        long count = httpRequestCount.get();
        return count > 0 ? httpTotalTimeMs.get() / count : 0;
    }
    
    /**
     * Returns plugin execution time breakdown.
     * 
     * @return map of plugin name to execution time in milliseconds
     */
    public Map<String, Long> getPluginBreakdown() {
        Map<String, Long> breakdown = new ConcurrentHashMap<>();
        pluginExecutionTimes.forEach((name, time) -> breakdown.put(name, time.get()));
        return breakdown;
    }
}
