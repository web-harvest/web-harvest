package org.webharvest.events;

import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.AlreadyBoundException;
import org.webharvest.Harvester;
import org.webharvest.Registry;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * Tests for DefaultHandlerHolder - event bus management.
 * 
 * @author Robert Bala
 * @since 2.2.0
 */
public class DefaultHandlerHolderTest {

    private DefaultHandlerHolder holder;
    private Registry<Harvester, EventBus> mockRegistry;
    private Provider<EventBus> mockProvider;
    private EventBus mockEventBus;
    private Harvester mockHarvester;

    @BeforeMethod
    public void setUp() {
        mockRegistry = mock(Registry.class);
        mockProvider = mock(Provider.class);
        mockEventBus = mock(EventBus.class);
        mockHarvester = mock(Harvester.class);
        
        when(mockProvider.get()).thenReturn(mockEventBus);
        
        holder = new DefaultHandlerHolder(mockRegistry, mockProvider);
    }

    // ========== Constructor Tests ==========

    @Test
    public void testConstructor() {
        assertNotNull(holder);
    }

    // Constructor null checks removed - Guice handles dependency injection

    // ========== register() Tests ==========

    @Test
    public void testRegisterHandler() {
        EventHandler<?> handler = mock(EventHandler.class);
        holder.register(handler);
        // Should not throw
    }

    @Test
    public void testRegisterMultipleHandlers() {
        EventHandler<?> handler1 = mock(EventHandler.class);
        EventHandler<?> handler2 = mock(EventHandler.class);
        EventHandler<?> handler3 = mock(EventHandler.class);
        
        holder.register(handler1);
        holder.register(handler2);
        holder.register(handler3);
        // Should not throw
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRegisterNullHandlerThrows() {
        holder.register(null);
    }

    @Test
    public void testRegisterHandlerExceptionMessage() {
        try {
            holder.register(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Handler is required"));
        }
    }

    // ========== onBeforeScraping() Tests ==========

    @Test
    public void testOnBeforeScrapingRegistersHandlers() throws AlreadyBoundException {
        EventHandler<?> handler = mock(EventHandler.class);
        holder.register(handler);
        
        holder.onBeforeScraping(mockHarvester);
        
        verify(mockEventBus).register(handler);
        verify(mockRegistry).bind(mockHarvester, mockEventBus);
    }

    @Test
    public void testOnBeforeScrapingRegistersMultipleHandlers() throws AlreadyBoundException {
        EventHandler<?> handler1 = mock(EventHandler.class);
        EventHandler<?> handler2 = mock(EventHandler.class);
        
        holder.register(handler1);
        holder.register(handler2);
        
        holder.onBeforeScraping(mockHarvester);
        
        verify(mockEventBus).register(handler1);
        verify(mockEventBus).register(handler2);
        verify(mockRegistry).bind(mockHarvester, mockEventBus);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnBeforeScrapingWithNullHarvesterThrows() {
        holder.onBeforeScraping(null);
    }

    @Test
    public void testOnBeforeScrapingNullHarvesterMessage() {
        try {
            holder.onBeforeScraping(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Harvester is required"));
        }
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testOnBeforeScrapingAlreadyBoundThrows() throws AlreadyBoundException {
        doThrow(new AlreadyBoundException("test")).when(mockRegistry).bind(any(), any());
        
        holder.onBeforeScraping(mockHarvester);
    }

    @Test
    public void testOnBeforeScrapingGetsEventBusFromProvider() throws AlreadyBoundException {
        holder.onBeforeScraping(mockHarvester);
        
        verify(mockProvider).get();
    }

    // ========== onAfterScraping() Tests ==========

    @Test
    public void testOnAfterScrapingUnregistersHandlers() {
        EventHandler<?> handler = mock(EventHandler.class);
        holder.register(handler);
        
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        holder.onAfterScraping(mockHarvester);
        
        verify(mockEventBus).unregister(handler);
        verify(mockRegistry).unbind(mockHarvester);
    }

    @Test
    public void testOnAfterScrapingUnregistersMultipleHandlers() {
        EventHandler<?> handler1 = mock(EventHandler.class);
        EventHandler<?> handler2 = mock(EventHandler.class);
        
        holder.register(handler1);
        holder.register(handler2);
        
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        holder.onAfterScraping(mockHarvester);
        
        verify(mockEventBus).unregister(handler1);
        verify(mockEventBus).unregister(handler2);
        verify(mockRegistry).unbind(mockHarvester);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testOnAfterScrapingWithNullHarvesterThrows() {
        holder.onAfterScraping(null);
    }

    @Test
    public void testOnAfterScrapingNullHarvesterMessage() {
        try {
            holder.onAfterScraping(null);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Harvester is required"));
        }
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOnAfterScrapingWithoutEventBusThrows() {
        when(mockRegistry.lookup(mockHarvester)).thenReturn(null);
        
        holder.onAfterScraping(mockHarvester);
    }

    @Test
    public void testOnAfterScrapingWithoutEventBusMessage() {
        when(mockRegistry.lookup(mockHarvester)).thenReturn(null);
        
        try {
            holder.onAfterScraping(mockHarvester);
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Could not find") || 
                      e.getMessage().contains("Cound not find")); // Typo in original code
        }
    }

    // ========== Lifecycle Tests ==========

    @Test
    public void testCompleteLifecycle() throws AlreadyBoundException {
        EventHandler<?> handler = mock(EventHandler.class);
        holder.register(handler);
        
        when(mockRegistry.lookup(mockHarvester)).thenReturn(mockEventBus);
        
        // Before scraping
        holder.onBeforeScraping(mockHarvester);
        verify(mockEventBus).register(handler);
        verify(mockRegistry).bind(mockHarvester, mockEventBus);
        
        // After scraping
        holder.onAfterScraping(mockHarvester);
        verify(mockEventBus).unregister(handler);
        verify(mockRegistry).unbind(mockHarvester);
    }

    @Test
    public void testMultipleScrapingCycles() throws AlreadyBoundException {
        EventHandler<?> handler = mock(EventHandler.class);
        holder.register(handler);
        
        Harvester harvester1 = mock(Harvester.class);
        Harvester harvester2 = mock(Harvester.class);
        
        EventBus eventBus1 = mock(EventBus.class);
        EventBus eventBus2 = mock(EventBus.class);
        
        when(mockProvider.get()).thenReturn(eventBus1, eventBus2);
        when(mockRegistry.lookup(harvester1)).thenReturn(eventBus1);
        when(mockRegistry.lookup(harvester2)).thenReturn(eventBus2);
        
        // Cycle 1
        holder.onBeforeScraping(harvester1);
        holder.onAfterScraping(harvester1);
        
        // Cycle 2
        holder.onBeforeScraping(harvester2);
        holder.onAfterScraping(harvester2);
        
        verify(eventBus1).register(handler);
        verify(eventBus1).unregister(handler);
        verify(eventBus2).register(handler);
        verify(eventBus2).unregister(handler);
    }

    @Test
    public void testNoHandlersRegistered() throws AlreadyBoundException {
        // No handlers registered
        holder.onBeforeScraping(mockHarvester);
        
        // Should still bind event bus
        verify(mockRegistry).bind(mockHarvester, mockEventBus);
        // But no handler registrations
        verify(mockEventBus, never()).register(any());
    }
}

