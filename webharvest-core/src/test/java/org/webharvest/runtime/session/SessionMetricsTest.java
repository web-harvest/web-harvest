package org.webharvest.runtime.session;

import org.testng.annotations.Test;
import java.time.Duration;
import java.time.Instant;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link SessionMetrics}.
 * 
 * <p>Tests timing, counting, and thread-safety of session metrics.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class SessionMetricsTest {
    
    @Test(groups = {"unit", "session"})
    public void testDefaultConstructor_SetsStartTime() {
        Instant before = Instant.now();
        SessionMetrics metrics = new SessionMetrics();
        Instant after = Instant.now();
        
        assertNotNull(metrics.getStartTime());
        assertFalse(metrics.getStartTime().isBefore(before),
            "Start time should be at or after test start");
        assertFalse(metrics.getStartTime().isAfter(after),
            "Start time should be at or before test end");
    }
    
    @Test(groups = {"unit", "session"})
    public void testConstructorWithStartTime() {
        Instant startTime = Instant.now().minusSeconds(60);
        SessionMetrics metrics = new SessionMetrics(startTime);
        
        assertEquals(metrics.getStartTime(), startTime);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testConstructorWithNullStartTime_ThrowsException() {
        new SessionMetrics(null);
    }
    
    @Test(groups = {"unit", "session"})
    public void testEndTime_InitiallyNull() {
        SessionMetrics metrics = new SessionMetrics();
        assertNull(metrics.getEndTime());
    }
    
    @Test(groups = {"unit", "session"})
    public void testMarkEnded_SetsEndTime() {
        SessionMetrics metrics = new SessionMetrics();
        
        Instant before = Instant.now();
        metrics.markEnded();
        Instant after = Instant.now();
        
        assertNotNull(metrics.getEndTime());
        assertFalse(metrics.getEndTime().isBefore(before));
        assertFalse(metrics.getEndTime().isAfter(after));
    }
    
    @Test(groups = {"unit", "session"})
    public void testMarkEnded_Idempotent() {
        SessionMetrics metrics = new SessionMetrics();
        
        metrics.markEnded();
        Instant firstEnd = metrics.getEndTime();
        
        // Call again - should not change
        metrics.markEnded();
        Instant secondEnd = metrics.getEndTime();
        
        assertEquals(firstEnd, secondEnd,
            "Multiple calls to markEnded() should not change end time");
    }
    
    @Test(groups = {"unit", "session"})
    public void testMarkEndedWithTime() {
        Instant startTime = Instant.now().minusSeconds(60);
        SessionMetrics metrics = new SessionMetrics(startTime);
        
        Instant endTime = Instant.now();
        metrics.markEnded(endTime);
        
        assertEquals(metrics.getEndTime(), endTime);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testMarkEndedWithNullTime_ThrowsException() {
        SessionMetrics metrics = new SessionMetrics();
        metrics.markEnded(null);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testMarkEndedBeforeStartTime_ThrowsException() {
        Instant startTime = Instant.now();
        SessionMetrics metrics = new SessionMetrics(startTime);
        
        Instant endTime = startTime.minusSeconds(10);
        metrics.markEnded(endTime);
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetDuration_RunningSession() throws InterruptedException {
        SessionMetrics metrics = new SessionMetrics();
        
        // Sleep briefly to ensure measurable duration
        Thread.sleep(50);
        
        Duration duration = metrics.getDuration();
        assertNotNull(duration);
        assertTrue(duration.toMillis() >= 50,
            "Duration should be at least 50ms");
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetDuration_CompletedSession() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        SessionMetrics metrics = new SessionMetrics(startTime);
        metrics.markEnded(endTime);
        
        Duration duration = metrics.getDuration();
        assertTrue(duration.getSeconds() >= 60,
            "Duration should be at least 60 seconds");
    }
    
    @Test(groups = {"unit", "session"})
    public void testProcessedElements_InitiallyZero() {
        SessionMetrics metrics = new SessionMetrics();
        assertEquals(metrics.getProcessedElements(), 0L);
    }
    
    @Test(groups = {"unit", "session"})
    public void testIncrementProcessedElements_Single() {
        SessionMetrics metrics = new SessionMetrics();
        
        long newCount = metrics.incrementProcessedElements();
        assertEquals(newCount, 1L);
        assertEquals(metrics.getProcessedElements(), 1L);
    }
    
    @Test(groups = {"unit", "session"})
    public void testIncrementProcessedElements_Multiple() {
        SessionMetrics metrics = new SessionMetrics();
        
        metrics.incrementProcessedElements();
        metrics.incrementProcessedElements();
        long count = metrics.incrementProcessedElements();
        
        assertEquals(count, 3L);
        assertEquals(metrics.getProcessedElements(), 3L);
    }
    
    @Test(groups = {"unit", "session"})
    public void testIncrementProcessedElementsByCount() {
        SessionMetrics metrics = new SessionMetrics();
        
        long count = metrics.incrementProcessedElements(10);
        assertEquals(count, 10L);
        assertEquals(metrics.getProcessedElements(), 10L);
        
        count = metrics.incrementProcessedElements(5);
        assertEquals(count, 15L);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testIncrementProcessedElements_NegativeCount() {
        SessionMetrics metrics = new SessionMetrics();
        metrics.incrementProcessedElements(-5);
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetProcessingRate() {
        Instant startTime = Instant.now().minusSeconds(10);
        SessionMetrics metrics = new SessionMetrics(startTime);
        metrics.markEnded(startTime.plusSeconds(10));
        
        metrics.incrementProcessedElements(100);
        
        double rate = metrics.getProcessingRate();
        assertEquals(rate, 10.0, 0.1,
            "Should process 100 elements in 10 seconds = 10 elem/s");
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetProcessingRate_ZeroDuration() {
        SessionMetrics metrics = new SessionMetrics();
        // Immediately get rate - duration will be ~0
        
        double rate = metrics.getProcessingRate();
        assertEquals(rate, 0.0, "Rate should be 0 when no time has elapsed");
    }
    
    @Test(groups = {"unit", "session"})
    public void testConcurrentIncrements() throws InterruptedException {
        SessionMetrics metrics = new SessionMetrics();
        int threadCount = 10;
        int incrementsPerThread = 100;
        
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    metrics.incrementProcessedElements();
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(metrics.getProcessedElements(), 
            threadCount * incrementsPerThread,
            "All increments should be counted correctly");
    }
    
    @Test(groups = {"unit", "session"})
    public void testToString_ContainsKeyInfo() {
        SessionMetrics metrics = new SessionMetrics();
        metrics.incrementProcessedElements(42);
        
        String str = metrics.toString();
        assertNotNull(str);
        assertTrue(str.contains("SessionMetrics"));
        assertTrue(str.contains("startTime"));
        assertTrue(str.contains("processedElements"));
        assertTrue(str.contains("42"));
    }
}


