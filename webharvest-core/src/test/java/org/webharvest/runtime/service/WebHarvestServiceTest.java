package org.webharvest.runtime.service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.client.ClientContext;
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

