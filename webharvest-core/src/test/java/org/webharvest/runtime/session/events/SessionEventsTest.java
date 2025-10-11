package org.webharvest.runtime.session.events;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.session.ScraperSessionImpl;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

/**
 * Tests for session event publishing.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class SessionEventsTest {
    
    private EventBus eventBus;
    private TestEventCollector collector;
    private Config mockConfig;
    
    @BeforeMethod
    public void setUp() {
        eventBus = new EventBus();
        collector = new TestEventCollector();
        eventBus.register(collector);
        mockConfig = mock(Config.class);
    }
    
    @Test(groups = {"unit", "events"})
    public void testSessionCreatedEvent() {
        new ScraperSessionImpl("session-1", "client-1", mockConfig, eventBus);
        
        assertEquals(collector.createdEvents.size(), 1);
        SessionCreatedEvent event = collector.createdEvents.get(0);
        assertEquals(event.getSessionId(), "session-1");
        assertEquals(event.getClientId(), "client-1");
    }
    
    @Test(groups = {"unit", "events"})
    public void testSessionStartedEvent() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        collector.createdEvents.clear(); // Reset
        
        DynamicScopeContext mockContext = mock(DynamicScopeContext.class);
        session.markRunning(mockContext);
        
        assertEquals(collector.startedEvents.size(), 1);
        SessionStartedEvent event = collector.startedEvents.get(0);
        assertEquals(event.getSessionId(), "session-1");
    }
    
    @Test(groups = {"unit", "events"})
    public void testSessionCompletedEvent() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        DynamicScopeContext mockContext = mock(DynamicScopeContext.class);
        session.markRunning(mockContext);
        collector.clear();
        
        session.markCompleted();
        
        assertEquals(collector.completedEvents.size(), 1);
        SessionCompletedEvent event = collector.completedEvents.get(0);
        assertEquals(event.getSessionId(), "session-1");
        assertNotNull(event.getMetrics());
    }
    
    @Test(groups = {"unit", "events"})
    public void testSessionFailedEvent() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        DynamicScopeContext mockContext = mock(DynamicScopeContext.class);
        session.markRunning(mockContext);
        collector.clear();
        
        Exception error = new RuntimeException("Test error");
        session.markFailed(error);
        
        assertEquals(collector.failedEvents.size(), 1);
        SessionFailedEvent event = collector.failedEvents.get(0);
        assertEquals(event.getSessionId(), "session-1");
        assertEquals(event.getError(), error);
    }
    
    @Test(groups = {"unit", "events"})
    public void testSessionCancelledEvent() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        collector.clear();
        
        session.cancel();
        
        assertEquals(collector.cancelledEvents.size(), 1);
        SessionCancelledEvent event = collector.cancelledEvents.get(0);
        assertEquals(event.getSessionId(), "session-1");
    }
    
    @Test(groups = {"unit", "events"})
    public void testFullSessionLifecycleEvents() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        
        assertEquals(collector.createdEvents.size(), 1);
        
        DynamicScopeContext mockContext = mock(DynamicScopeContext.class);
        session.markRunning(mockContext);
        assertEquals(collector.startedEvents.size(), 1);
        
        session.markCompleted();
        assertEquals(collector.completedEvents.size(), 1);
        
        // Total: 3 events published
        assertEquals(collector.allEventsCount(), 3);
    }
    
    @Test(groups = {"unit", "events"})
    public void testEventTimestamps() {
        ScraperSessionImpl session = new ScraperSessionImpl(
            "session-1", "client-1", mockConfig, eventBus);
        
        SessionCreatedEvent event = collector.createdEvents.get(0);
        assertNotNull(event.getTimestamp());
    }
    
    /**
     * Test event collector that subscribes to all session events.
     */
    private static class TestEventCollector {
        final List<SessionCreatedEvent> createdEvents = new ArrayList<>();
        final List<SessionStartedEvent> startedEvents = new ArrayList<>();
        final List<SessionCompletedEvent> completedEvents = new ArrayList<>();
        final List<SessionFailedEvent> failedEvents = new ArrayList<>();
        final List<SessionCancelledEvent> cancelledEvents = new ArrayList<>();
        
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
        
        @Subscribe
        public void onFailed(SessionFailedEvent event) {
            failedEvents.add(event);
        }
        
        @Subscribe
        public void onCancelled(SessionCancelledEvent event) {
            cancelledEvents.add(event);
        }
        
        int allEventsCount() {
            return createdEvents.size() + startedEvents.size() + 
                   completedEvents.size() + failedEvents.size() + 
                   cancelledEvents.size();
        }
        
        void clear() {
            createdEvents.clear();
            startedEvents.clear();
            completedEvents.clear();
            failedEvents.clear();
            cancelledEvents.clear();
        }
    }
}

