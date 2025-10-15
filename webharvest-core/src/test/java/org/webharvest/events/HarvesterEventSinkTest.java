package org.webharvest.events;

import com.google.common.eventbus.EventBus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.Harvester;
import org.webharvest.Registry;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for HarvesterEventSink - event publishing to scope-specific event bus.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class HarvesterEventSinkTest {

    private HarvesterEventSink sink;
    private Registry<Harvester, EventBus> mockRegistry;
    private EventBus mockEventBus;
    private Harvester mockHarvester;

    @BeforeMethod
    public void setUp() {
        mockRegistry = mock(Registry.class);
        mockEventBus = mock(EventBus.class);
        mockHarvester = mock(Harvester.class);
        
        sink = new HarvesterEventSink(mockRegistry);
    }

    // ========== Constructor Tests ==========

    @Test
    public void testConstructor() {
        assertNotNull(sink);
    }

    // ========== publish() Tests ==========

    @Test
    public void testPublishEvent() {
        HarvesterEvent event = mock(HarvesterEvent.class);
        when(event.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(mockEventBus).post(event);
    }

    @Test
    public void testPublishMultipleEvents() {
        HarvesterEvent event1 = mock(HarvesterEvent.class);
        HarvesterEvent event2 = mock(HarvesterEvent.class);
        
        when(event1.getHarvester()).thenReturn(mockHarvester);
        when(event2.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event1);
        sink.publish(event2);
        
        verify(mockEventBus).post(event1);
        verify(mockEventBus).post(event2);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPublishNullEventThrows() {
        sink.publish(null);
    }

    @Test
    public void testPublishNullEventMessage() {
        try {
            sink.publish(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Event is requried") || 
                      e.getMessage().contains("Event is required")); // Note: typo in original
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testPublishWhenEventBusNotFoundThrows() {
        HarvesterEvent event = mock(HarvesterEvent.class);
        when(event.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(null);
        
        sink.publish(event);
    }

    @Test
    public void testPublishEventBusNotFoundMessage() {
        HarvesterEvent event = mock(HarvesterEvent.class);
        when(event.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(null);
        
        try {
            sink.publish(event);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Could not find") || 
                      e.getMessage().contains("Cound not find")); // Note: typo in original
        }
    }

    // ========== Event Routing Tests ==========

    @Test
    public void testEventRoutedToCorrectHarvester() {
        Harvester harvester1 = mock(Harvester.class);
        Harvester harvester2 = mock(Harvester.class);
        EventBus eventBus1 = mock(EventBus.class);
        EventBus eventBus2 = mock(EventBus.class);
        
        HarvesterEvent event1 = mock(HarvesterEvent.class);
        HarvesterEvent event2 = mock(HarvesterEvent.class);
        
        when(event1.getHarvester()).thenReturn(harvester1);
        when(event2.getHarvester()).thenReturn(harvester2);
        when(mockRegistry.lookup(harvester1)).thenReturn(eventBus1);
        when(mockRegistry.lookup(harvester2)).thenReturn(eventBus2);
        
        sink.publish(event1);
        sink.publish(event2);
        
        // Each event goes to its own bus
        verify(eventBus1).post(event1);
        verify(eventBus2).post(event2);
        verify(eventBus1, never()).post(event2);
        verify(eventBus2, never()).post(event1);
    }

    @Test
    public void testLookupCallsRegistry() {
        HarvesterEvent event = mock(HarvesterEvent.class);
        when(event.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(mockRegistry).lookup(mockHarvester);
    }

    @Test
    public void testPublishGetsHarvesterFromEvent() {
        HarvesterEvent event = mock(HarvesterEvent.class);
        when(event.getHarvester()).thenReturn(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(event).getHarvester();
    }

    // ========== Concrete Event Types Tests ==========

    @Test
    public void testPublishScraperExecutionPausedEvent() {
        ScraperExecutionPausedEvent event = new ScraperExecutionPausedEvent(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(mockEventBus).post(event);
    }

    @Test
    public void testPublishScraperExecutionContinuedEvent() {
        ScraperExecutionContinuedEvent event = new ScraperExecutionContinuedEvent(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(mockEventBus).post(event);
    }

    @Test
    public void testPublishScraperExecutionStoppedEvent() {
        ScraperExecutionStoppedEvent event = new ScraperExecutionStoppedEvent(mockHarvester);
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        sink.publish(event);
        
        verify(mockEventBus).post(event);
    }

    // Note: ScraperExecutionExitEvent is NOT a HarvesterEvent (different interface)
    // It has String message instead of Harvester reference
}

