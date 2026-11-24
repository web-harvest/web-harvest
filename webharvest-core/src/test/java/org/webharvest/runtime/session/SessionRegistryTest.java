package org.webharvest.runtime.session;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link SessionRegistry} implementation.
 * 
 * <p>Tests registration, lookup, concurrent access, and cleanup operations.</p>
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class SessionRegistryTest {
    
    private SessionRegistry registry;
    private Config mockConfig;
    
    @BeforeMethod
    public void setUp() {
        registry = new SessionRegistryImpl();
        mockConfig = mock(Config.class);
    }
    
    @Test(groups = {"unit", "session"})
    public void testRegister_SingleSession() {
        ScraperSession session = createSession("session-1", "client-1");
        
        registry.register(session);
        
        Optional<ScraperSession> found = registry.getSession("session-1");
        assertTrue(found.isPresent());
        assertEquals(found.get().getSessionId(), "session-1");
    }
    
    @Test(groups = {"unit", "session"})
    public void testRegister_MultipleSessions() {
        ScraperSession session1 = createSession("session-1", "client-1");
        ScraperSession session2 = createSession("session-2", "client-2");
        ScraperSession session3 = createSession("session-3", "client-1");
        
        registry.register(session1);
        registry.register(session2);
        registry.register(session3);
        
        assertEquals(registry.size(), 3);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testRegister_NullSession_ThrowsException() {
        registry.register(null);
    }
    
    @Test(groups = {"unit", "session"})
    public void testRegister_DuplicateIdRejectedWithoutReplacing() {
        ScraperSession session1 = createSession("session-1", "client-1");
        ScraperSession session2 = createSession("session-1", "client-2");

        registry.register(session1);
        try {
            registry.register(session2);
            fail("Duplicate session IDs must be rejected");
        } catch (IllegalStateException expected) {
            // Expected: the original owner remains registered.
        }

        assertSame(registry.getSession("session-1").orElse(null), session1);
        assertEquals(registry.size(), 1);
    }

    @Test(groups = {"unit", "session"})
    public void testUnregister_StaleSessionCannotRemoveCurrentOwner() {
        ScraperSession original = createSession("session-1", "client-1");
        ScraperSession stale = createSession("session-1", "client-2");
        registry.register(original);

        assertFalse(registry.unregisterIfOwner(stale));
        assertSame(registry.getSession("session-1").orElse(null), original);
        assertEquals(registry.size(), 1);
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetSession_Found() {
        ScraperSession session = createSession("session-1", "client-1");
        registry.register(session);
        
        Optional<ScraperSession> found = registry.getSession("session-1");
        
        assertTrue(found.isPresent());
        assertEquals(found.get(), session);
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetSession_NotFound() {
        Optional<ScraperSession> found = registry.getSession("non-existent");
        
        assertFalse(found.isPresent());
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testGetSession_NullSessionId_ThrowsException() {
        registry.getSession(null);
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetSessionsByClientId_SingleClient() {
        ScraperSession session1 = createSession("session-1", "client-1");
        ScraperSession session2 = createSession("session-2", "client-1");
        ScraperSession session3 = createSession("session-3", "client-2");
        
        registry.register(session1);
        registry.register(session2);
        registry.register(session3);
        
        List<ScraperSession> client1Sessions = registry.getSessionsByClientId("client-1");
        
        assertEquals(client1Sessions.size(), 2);
        assertTrue(client1Sessions.contains(session1));
        assertTrue(client1Sessions.contains(session2));
        assertFalse(client1Sessions.contains(session3));
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetSessionsByClientId_NoSessions() {
        List<ScraperSession> sessions = registry.getSessionsByClientId("non-existent");
        
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetSessionsByClientId_NullClientId() {
        ScraperSession session1 = createSession("session-1", null);
        ScraperSession session2 = createSession("session-2", "client-1");
        
        registry.register(session1);
        registry.register(session2);
        
        // Searching for null clientId should return sessions without client
        List<ScraperSession> nullClientSessions = registry.getSessionsByClientId(null);
        
        assertEquals(nullClientSessions.size(), 1);
        assertTrue(nullClientSessions.contains(session1));
        assertFalse(nullClientSessions.contains(session2));
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetAllSessions_Empty() {
        List<ScraperSession> sessions = registry.getAllSessions();
        
        assertNotNull(sessions);
        assertTrue(sessions.isEmpty());
    }
    
    @Test(groups = {"unit", "session"})
    public void testGetAllSessions_MultipleSessions() {
        ScraperSession session1 = createSession("session-1", "client-1");
        ScraperSession session2 = createSession("session-2", "client-2");
        ScraperSession session3 = createSession("session-3", "client-1");
        
        registry.register(session1);
        registry.register(session2);
        registry.register(session3);
        
        List<ScraperSession> allSessions = registry.getAllSessions();
        
        assertEquals(allSessions.size(), 3);
        assertTrue(allSessions.contains(session1));
        assertTrue(allSessions.contains(session2));
        assertTrue(allSessions.contains(session3));
    }
    
    @Test(groups = {"unit", "session"})
    public void testUnregister_ExistingSession() {
        ScraperSession session = createSession("session-1", "client-1");
        registry.register(session);
        
        boolean removed = registry.unregister("session-1");
        
        assertTrue(removed);
        assertEquals(registry.size(), 0);
        assertFalse(registry.getSession("session-1").isPresent());
    }
    
    @Test(groups = {"unit", "session"})
    public void testUnregister_NonExistentSession() {
        boolean removed = registry.unregister("non-existent");
        
        assertFalse(removed);
    }
    
    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testUnregister_NullSessionId_ThrowsException() {
        registry.unregister(null);
    }
    
    @Test(groups = {"unit", "session"})
    public void testSize_Empty() {
        assertEquals(registry.size(), 0);
    }
    
    @Test(groups = {"unit", "session"})
    public void testSize_MultipleSessions() {
        registry.register(createSession("session-1", "client-1"));
        registry.register(createSession("session-2", "client-2"));
        registry.register(createSession("session-3", "client-1"));
        
        assertEquals(registry.size(), 3);
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsEmpty_WhenEmpty() {
        assertTrue(registry.isEmpty());
    }
    
    @Test(groups = {"unit", "session"})
    public void testIsEmpty_WhenNotEmpty() {
        registry.register(createSession("session-1", "client-1"));
        
        assertFalse(registry.isEmpty());
    }
    
    @Test(groups = {"unit", "session"})
    public void testClear_RemovesAllSessions() {
        registry.register(createSession("session-1", "client-1"));
        registry.register(createSession("session-2", "client-2"));
        registry.register(createSession("session-3", "client-1"));
        
        assertEquals(registry.size(), 3);
        
        registry.clear();
        
        assertEquals(registry.size(), 0);
        assertTrue(registry.isEmpty());
        assertTrue(registry.getAllSessions().isEmpty());
    }
    
    @Test(groups = {"unit", "session"})
    public void testConcurrentRegistration() throws InterruptedException {
        int threadCount = 10;
        int sessionsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Create threads that register sessions concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadNum = t;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for signal to start
                    for (int i = 0; i < sessionsPerThread; i++) {
                        String sessionId = "session-" + threadNum + "-" + i;
                        ScraperSession session = createSession(sessionId, "client-" + threadNum);
                        registry.register(session);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), 
            "All threads should complete within timeout");
        
        // Verify all sessions were registered
        assertEquals(registry.size(), threadCount * sessionsPerThread,
            "All sessions should be registered");
    }
    
    @Test(groups = {"unit", "session"})
    public void testConcurrentLookup() throws InterruptedException {
        // Pre-populate registry
        for (int i = 0; i < 100; i++) {
            registry.register(createSession("session-" + i, "client-" + (i % 10)));
        }
        
        int threadCount = 10;
        int lookupsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Create threads that lookup sessions concurrently
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < lookupsPerThread; i++) {
                        String sessionId = "session-" + (i % 100);
                        Optional<ScraperSession> found = registry.getSession(sessionId);
                        if (found.isPresent()) {
                            successCount.incrementAndGet();
                        }
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
        
        assertEquals(successCount.get(), threadCount * lookupsPerThread,
            "All lookups should succeed");
    }
    
    @Test(groups = {"unit", "session"})
    public void testConcurrentModification_GetAllSessions() {
        // Register initial sessions
        for (int i = 0; i < 10; i++) {
            registry.register(createSession("session-" + i, "client-" + i));
        }
        
        // Get snapshot
        List<ScraperSession> snapshot1 = registry.getAllSessions();
        
        // Modify registry
        registry.register(createSession("session-new", "client-new"));
        registry.unregister("session-0");
        
        // Original snapshot should be unaffected
        assertEquals(snapshot1.size(), 10, 
            "Snapshot should not be affected by later modifications");
    }
    
    // Helper method
    private ScraperSession createSession(String sessionId, String clientId) {
        return new ScraperSessionImpl(sessionId, clientId, mockConfig);
    }
}

