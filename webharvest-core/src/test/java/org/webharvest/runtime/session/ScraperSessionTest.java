package org.webharvest.runtime.session;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ScraperSession} implementation.
 * 
 * <p>Tests session lifecycle, status transitions, and thread safety.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class ScraperSessionTest {
    
    private Config mockConfig;
    private DynamicScopeContext mockContext;
    
    @BeforeMethod
    public void setUp() {
        mockConfig = mock(Config.class);
        mockContext = mock(DynamicScopeContext.class);
    }
    
    @Test(groups = {"unit", "session"})
    public void testSessionCreation_WithClientId() {
        String sessionId = "session-123";
        String clientId = "client-456";
        
        ScraperSession session = new ScraperSessionImpl(sessionId, clientId, mockConfig);
        
        assertEquals(session.getSessionId(), sessionId);
        assertEquals(session.getClientId(), clientId);
        assertEquals(session.getStatus(), SessionStatus.PENDING);
        assertNotNull(session.getMetrics());
        assertEquals(session.getConfig(), mockConfig);
        assertNull(session.getContext(), "Context should be null initially");
        assertNull(session.getError(), "Error should be null initially");
    }
    
    @Test(groups = {"unit", "session"})
    public void testSessionCreation_WithoutClientId() {
        String sessionId = "session-123";
        
        ScraperSession session = new ScraperSessionImpl(sessionId, null, mockConfig);
        
        assertEquals(session.getSessionId(), sessionId);
        assertNull(session.getClientId());
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testSessionCreation_NullSessionId_ThrowsException() {
        new ScraperSessionImpl(null, "client-123", mockConfig);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testSessionCreation_EmptySessionId_ThrowsException() {
        new ScraperSessionImpl("", "client-123", mockConfig);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testSessionCreation_NullConfig_ThrowsException() {
        new ScraperSessionImpl("session-123", "client-456", null);
    }
    
    @Test(groups = {"unit", "session"})
    public void testStatusTransition_PendingToRunning() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        
        assertEquals(session.getStatus(), SessionStatus.PENDING);
        
        session.markRunning(mockContext);
        
        assertEquals(session.getStatus(), SessionStatus.RUNNING);
        assertNotNull(session.getContext());
        assertEquals(session.getContext(), mockContext);
    }

    @Test(groups = {"unit", "session"})
    public void testMarkRunning_WhenNotPending_ThrowsIllegalStateException() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        assertEquals(session.getStatus(), SessionStatus.RUNNING);

        try {
            session.markRunning(mockContext);
            fail("Expected IllegalStateException when markRunning is called from RUNNING");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Cannot mark as running from state: RUNNING"),
                    expected.getMessage());
        }
        assertEquals(session.getStatus(), SessionStatus.RUNNING);
        assertEquals(session.getContext(), mockContext);
    }
    
    @Test(groups = {"unit", "session"})
    public void testStatusTransition_RunningToCompleted() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        session.markCompleted();
        
        assertEquals(session.getStatus(), SessionStatus.COMPLETED);
        assertNull(session.getError());
    }

    @Test(groups = {"unit", "session"})
    public void testMarkCompleted_WhenNotRunning_ThrowsIllegalStateException() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        assertEquals(session.getStatus(), SessionStatus.PENDING);

        try {
            session.markCompleted();
            fail("Expected IllegalStateException when markCompleted is called from PENDING");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Cannot mark as completed from state: PENDING"),
                    expected.getMessage());
        }
        assertEquals(session.getStatus(), SessionStatus.PENDING);
    }
    
    @Test(groups = {"unit", "session"})
    public void testStatusTransition_RunningToFailed() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        Exception error = new RuntimeException("Test error");
        session.markFailed(error);
        
        assertEquals(session.getStatus(), SessionStatus.FAILED);
        assertEquals(session.getError(), error);
    }
    
    @Test(groups = {"unit", "session"})
    public void testStatusTransition_PendingToCancelled() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        
        boolean cancelled = session.cancel();
        
        assertTrue(cancelled, "Should successfully cancel");
        assertEquals(session.getStatus(), SessionStatus.CANCELLED);
    }
    
    @Test(groups = {"unit", "session"})
    public void testCancel_TerminalState_ReturnsFalse() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        session.markCompleted();
        
        boolean cancelled = session.cancel();
        
        assertFalse(cancelled, "Cannot cancel completed session");
        assertEquals(session.getStatus(), SessionStatus.COMPLETED);
    }
    
    @Test(groups = {"unit", "session"})
    public void testAwaitCompletion_WaitsForCompletion() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        AtomicBoolean completed = new AtomicBoolean(false);
        
        // Start waiting thread
        Thread waiter = new Thread(() -> {
            try {
                session.awaitCompletion();
                completed.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();
        
        // Give waiter time to start waiting
        Thread.sleep(100);
        assertFalse(completed.get(), "Should still be waiting");
        
        // Complete the session
        session.markCompleted();
        
        // Wait for waiter thread
        waiter.join(1000);
        assertTrue(completed.get(), "Should have completed");
    }
    
    @Test(groups = {"unit", "session"})
    public void testAwaitCompletionWithTimeout_Success() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        // Complete in background after short delay
        new Thread(() -> {
            try {
                Thread.sleep(100);
                session.markCompleted();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        boolean completed = session.awaitCompletion(1000);
        
        assertTrue(completed, "Should complete within timeout");
        assertEquals(session.getStatus(), SessionStatus.COMPLETED);
    }
    
    @Test(groups = {"unit", "session"})
    public void testAwaitCompletionWithTimeout_Timeout() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        boolean completed = session.awaitCompletion(100);
        
        assertFalse(completed, "Should timeout");
        assertEquals(session.getStatus(), SessionStatus.RUNNING);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testAwaitCompletionWithTimeout_NegativeTimeout() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.awaitCompletion(-1);
    }
    
    @Test(groups = {"unit", "session"})
    public void testMetricsUpdated_OnStatusChanges() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        SessionMetrics metrics = session.getMetrics();
        
        assertNull(metrics.getEndTime(), "End time should be null initially");
        
        session.markRunning(mockContext);
        assertNull(metrics.getEndTime(), "End time should still be null when running");
        
        Thread.sleep(50); // Ensure measurable duration
        
        session.markCompleted();
        assertNotNull(metrics.getEndTime(), "End time should be set on completion");
        assertTrue(metrics.getDuration().toMillis() >= 50,
            "Duration should be at least 50ms");
    }
    
    @Test(groups = {"unit", "session"})
    public void testConcurrentStatusChecks() throws InterruptedException {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean hasError = new AtomicBoolean(false);
        
        // Multiple threads checking status concurrently
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        SessionStatus status = session.getStatus();
                        if (status == null) {
                            hasError.set(true);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await(5, TimeUnit.SECONDS);
        assertFalse(hasError.get(), "Status should always be non-null");
    }
    
    @Test(groups = {"unit", "session"})
    public void testToString_ContainsKeyInfo() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", "client-456", mockConfig);
        
        String str = session.toString();
        assertNotNull(str);
        assertTrue(str.contains("session-123"), "Should contain session ID");
        assertTrue(str.contains("client-456"), "Should contain client ID");
        assertTrue(str.contains("PENDING"), "Should contain status");
    }
    
    @Test(groups = {"unit", "session"})
    public void testMarkFailed_NullError_AcceptedWithFallback() {
        ScraperSessionImpl session = new ScraperSessionImpl("session-123", null, mockConfig);
        session.markRunning(mockContext);
        
        session.markFailed(null);
        
        assertEquals(session.getStatus(), SessionStatus.FAILED);
        // Implementation should handle null gracefully (e.g., create generic error)
        assertNotNull(session.getError(), "Should have a fallback error message");
    }
}






