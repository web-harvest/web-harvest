package org.webharvest.runtime.service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.client.ClientContext;
import org.webharvest.runtime.session.ExecutionState;
import org.webharvest.runtime.session.ExecutionTrackedSession;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionRegistry;
import org.webharvest.runtime.session.SessionRegistryImpl;
import org.webharvest.runtime.session.SessionStatus;
import org.webharvest.runtime.session.events.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * Unit tests for {@link WebHarvestService}.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class WebHarvestServiceTest {
    
    private WebHarvestService service;
    private Config mockConfig;
    
    @BeforeMethod
    public void setUp() {
        service = new WebHarvestServiceImpl();
        mockConfig = mock(Config.class);
    }
    
    @AfterMethod
    public void tearDown() throws InterruptedException {
        if (service != null && !service.isShutdown()) {
            service.shutdown();
        }
    }
    
    @Test(groups = {"unit", "service"})
    public void testExecuteAsync_CreatesSession() {
        ClientContext client = ClientContext.of("test-client");
        
        ScraperSession session = service.executeAsync(mockConfig, client, null);
        
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(session.getClientId(), "test-client");
        assertEquals(session.getConfig(), mockConfig);
    }
    
    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testExecuteAsync_NullConfig_ThrowsException() {
        service.executeAsync(null, null, null);
    }
    
    @Test(groups = {"unit", "service"})
    public void testExecuteAsync_NullClient_Accepted() {
        ScraperSession session = service.executeAsync(mockConfig, null, null);
        
        assertNotNull(session);
        assertNull(session.getClientId());
    }
    
    @Test(groups = {"unit", "service"})
    public void testExecuteAsync_NullOptions_UsesDefaults() {
        ScraperSession session = service.executeAsync(mockConfig, null, null);
        
        assertNotNull(session);
        assertNotNull(session.getMetrics());
    }
    
    @Test(groups = {"unit", "service"})
    public void testExecuteAsync_CustomOptions() {
        SessionOptions options = SessionOptions.builder()
            .timeout(Duration.ofMinutes(5))
            .trackTokens(false)
            .build();
        
        ScraperSession session = service.executeAsync(mockConfig, null, options);
        
        assertNotNull(session);
    }
    
    @Test(groups = {"unit", "service"})
    public void testGetSession_ExistingSession() {
        ScraperSession created = service.executeAsync(mockConfig, null, null);
        
        Optional<ScraperSession> found = service.getSession(created.getSessionId());
        
        assertTrue(found.isPresent());
        assertEquals(found.get().getSessionId(), created.getSessionId());
    }
    
    @Test(groups = {"unit", "service"})
    public void testGetSession_NonExistent() {
        Optional<ScraperSession> found = service.getSession("non-existent");
        
        assertFalse(found.isPresent());
    }
    
    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testGetSession_NullSessionId() {
        service.getSession(null);
    }
    
    @Test(groups = {"unit", "service"})
    public void testListSessions_ByClient() {
        ClientContext client1 = ClientContext.of("client-1");
        ClientContext client2 = ClientContext.of("client-2");
        
        service.executeAsync(mockConfig, client1, null);
        service.executeAsync(mockConfig, client1, null);
        service.executeAsync(mockConfig, client2, null);
        
        List<ScraperSession> client1Sessions = service.listSessions("client-1");
        
        assertEquals(client1Sessions.size(), 2);
        assertTrue(client1Sessions.stream()
            .allMatch(s -> "client-1".equals(s.getClientId())));
    }
    
    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testListSessions_NullClientId() {
        service.listSessions(null);
    }
    
    @Test(groups = {"unit", "service"})
    public void testListAllSessions() {
        service.executeAsync(mockConfig, ClientContext.of("client-1"), null);
        service.executeAsync(mockConfig, ClientContext.of("client-2"), null);
        
        List<ScraperSession> allSessions = service.listAllSessions();
        
        assertEquals(allSessions.size(), 2);
    }
    
    @Test(groups = {"unit", "service"})
    public void testCancelSession_ExistingSession() throws InterruptedException {
        ScraperSession session = service.executeAsync(mockConfig, null, null);
        
        // Give it time to maybe start
        Thread.sleep(100);
        
        boolean cancelled = service.cancelSession(session.getSessionId());
        
        assertTrue(cancelled || session.getStatus().isTerminal(),
            "Should cancel or already be terminal");
    }
    
    @Test(groups = {"unit", "service"})
    public void testCancelBeforeStart_ExecutionQuiescenceObserved() throws InterruptedException {
        service.shutdown();

        ExecutorService executor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        boolean[] serviceOwnsExecutor = {false};
        executor.submit(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS),
                "The executor blocker should start before the session is submitted");
            service = new WebHarvestServiceImpl(executor, new SessionRegistryImpl(), new EventBus());
            serviceOwnsExecutor[0] = true;

            ScraperSession session = service.executeAsync(mockConfig, null, null);
            assertTrue(session.cancel(), "Queued session should accept cancellation");
            assertTrue(session.awaitCompletion(1000), "Business cancellation should be observable");

            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(tracked.awaitExecutionCompletion(1000),
                "A cancelled-before-start task must be known not to have entered the worker body");
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
        } finally {
            releaseBlocker.countDown();
            if (!serviceOwnsExecutor[0]) {
                executor.shutdownNow();
            }
        }

        service.shutdown();
    }

    @Test(groups = {"unit", "service"})
    public void testCancelDuringWorker_BusinessCompletionPrecedesQuiescence() throws InterruptedException {
        service.shutdown();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        service = new WebHarvestServiceImpl(executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(mockConfig, null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertTrue(listener.started.await(5, TimeUnit.SECONDS),
                "Worker should reach the controlled blocking point");
            assertTrue(session.cancel(), "Running session should accept cancellation");
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS),
                "Cancellation should interrupt the active worker");
            assertTrue(session.awaitCompletion(1000),
                "Business cancellation should be observable independently");
            assertFalse(tracked.awaitExecutionCompletion(50),
                "Worker quiescence must remain pending while the worker is still blocked");
            assertEquals(tracked.getExecutionState(), ExecutionState.INTERRUPT_REQUESTED);

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000),
                "Worker exit should be observed after the blocking operation is released");
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
        } finally {
            listener.release.countDown();
        }

        service.shutdown();
    }

    @Test(groups = {"unit", "service"})
    public void testCancelSession_NonExistent() {
        boolean cancelled = service.cancelSession("non-existent");
        
        assertFalse(cancelled);
    }
    
    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testCancelSession_NullSessionId() {
        service.cancelSession(null);
    }
    
    @Test(groups = {"unit", "service"})
    public void testShutdown_PreventsNewSubmissions() throws InterruptedException {
        service.shutdown();
        
        assertTrue(service.isShutdown());
        
        try {
            service.executeAsync(mockConfig, null, null);
            fail("Should throw exception after shutdown");
        } catch (IllegalStateException e) {
            // Expected
        }
    }
    
    @Test(groups = {"unit", "service"})
    public void testShutdownNow() {
        service.executeAsync(mockConfig, null, null);
        
        service.shutdownNow();
        
        assertTrue(service.isShutdown());
    }
    
    @Test(groups = {"unit", "service"})
    public void testEventsPublished() throws InterruptedException {
        EventBus eventBus = new EventBus();
        SessionRegistry registry = new SessionRegistryImpl();
        WebHarvestService customService = new WebHarvestServiceImpl(
            Executors.newSingleThreadExecutor(),
            registry,
            eventBus
        );
        
        TestEventCollector collector = new TestEventCollector();
        eventBus.register(collector);
        
        ScraperSession session = customService.executeAsync(mockConfig, null, null);
        
        // Wait for session creation event
        Thread.sleep(100);
        
        assertTrue(collector.createdEvents.size() >= 1,
            "Should have received SessionCreatedEvent");
        
        customService.shutdown();
    }
    
    private static class BlockingStartedListener {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch interrupted = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            started.countDown();
            boolean waiting = true;
            while (waiting) {
                try {
                    release.await();
                    waiting = false;
                } catch (InterruptedException cancellation) {
                    interrupted.countDown();
                    // Deliberately remain active until the test releases this operation.
                }
            }
        }
    }

    /**
     * Event collector for testing.
     */
    private static class TestEventCollector {
        final List<SessionCreatedEvent> createdEvents = new ArrayList<>();
        final List<SessionStartedEvent> startedEvents = new ArrayList<>();
        final List<SessionCompletedEvent> completedEvents = new ArrayList<>();
        
        @Subscribe
        public void onCreated(SessionCreatedEvent event) {
            createdEvents.add(event);
        }
        
        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            startedEvents.add(event);
        }
        
        @Subscribe
        public void onCompleted(SessionCompletedEvent event) {
            completedEvents.add(event);
        }
    }
}






