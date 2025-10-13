package org.webharvest.runtime.tracking;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * Unit tests for {@link TokenTracker} implementation.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class TokenTrackerTest {
    
    private TokenTracker tracker;
    
    @BeforeMethod
    public void setUp() {
        tracker = new TokenTrackerImpl("session-123", "client-456");
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testCreation() {
        assertEquals(tracker.getSessionId(), "session-123");
        assertEquals(tracker.getClientId(), "client-456");
    }
    
    @Test(groups = {"unit", "tracking"}, expectedExceptions = IllegalArgumentException.class)
    public void testCreation_NullSessionId() {
        new TokenTrackerImpl(null, "client-456");
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testCreation_NullClientId() {
        TokenTracker t = new TokenTrackerImpl("session-123", null);
        assertNull(t.getClientId());
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testInitialUsage_AllZero() {
        TokenUsage usage = tracker.getUsage();
        
        assertEquals(usage.getHttpRequests(), 0L);
        assertEquals(usage.getHttpBytes(), 0L);
        assertEquals(usage.getCpuTimeMillis(), 0L);
        assertEquals(usage.getMemoryPeakBytes(), 0L);
        assertTrue(usage.isEmpty());
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_HttpRequest() {
        tracker.record(ResourceType.HTTP_REQUEST, 1);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getHttpRequests(), 1L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_HttpBytes() {
        tracker.record(ResourceType.HTTP_BYTES, 1024000);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getHttpBytes(), 1024000L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_CpuTime() {
        tracker.record(ResourceType.CPU_TIME, 5000);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getCpuTimeMillis(), 5000L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_MemoryPeak() {
        tracker.record(ResourceType.MEMORY_PEAK, 104857600);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getMemoryPeakBytes(), 104857600L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_Multiple() {
        tracker.record(ResourceType.HTTP_REQUEST, 5);
        tracker.record(ResourceType.HTTP_REQUEST, 3);
        tracker.record(ResourceType.HTTP_BYTES, 1000);
        tracker.record(ResourceType.HTTP_BYTES, 2000);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getHttpRequests(), 8L);
        assertEquals(usage.getHttpBytes(), 3000L);
    }
    
    @Test(groups = {"unit", "tracking"}, expectedExceptions = IllegalArgumentException.class)
    public void testRecord_NullType() {
        tracker.record(null, 100);
    }
    
    @Test(groups = {"unit", "tracking"}, expectedExceptions = IllegalArgumentException.class)
    public void testRecord_NegativeAmount() {
        tracker.record(ResourceType.HTTP_REQUEST, -1);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testRecord_ZeroAmount() {
        tracker.record(ResourceType.HTTP_REQUEST, 0);
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getHttpRequests(), 0L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testReset() {
        tracker.record(ResourceType.HTTP_REQUEST, 10);
        tracker.record(ResourceType.HTTP_BYTES, 5000);
        
        tracker.reset();
        
        TokenUsage usage = tracker.getUsage();
        assertTrue(usage.isEmpty());
        assertEquals(usage.getHttpRequests(), 0L);
        assertEquals(usage.getHttpBytes(), 0L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testConcurrentRecording() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < incrementsPerThread; i++) {
                        tracker.record(ResourceType.HTTP_REQUEST, 1);
                        tracker.record(ResourceType.HTTP_BYTES, 100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        
        TokenUsage usage = tracker.getUsage();
        assertEquals(usage.getHttpRequests(), threadCount * incrementsPerThread);
        assertEquals(usage.getHttpBytes(), threadCount * incrementsPerThread * 100);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testUsageSnapshot_Immutable() {
        tracker.record(ResourceType.HTTP_REQUEST, 5);
        
        TokenUsage snapshot1 = tracker.getUsage();
        assertEquals(snapshot1.getHttpRequests(), 5L);
        
        tracker.record(ResourceType.HTTP_REQUEST, 3);
        
        // Original snapshot should not change
        assertEquals(snapshot1.getHttpRequests(), 5L);
        
        TokenUsage snapshot2 = tracker.getUsage();
        assertEquals(snapshot2.getHttpRequests(), 8L);
    }
    
    @Test(groups = {"unit", "tracking"})
    public void testToString_ContainsInfo() {
        tracker.record(ResourceType.HTTP_REQUEST, 5);
        
        String str = tracker.toString();
        assertNotNull(str);
        assertTrue(str.contains("session-123"));
        assertTrue(str.contains("client-456"));
    }
}






