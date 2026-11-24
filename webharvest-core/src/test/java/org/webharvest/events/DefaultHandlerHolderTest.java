package org.webharvest.events;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.webharvest.AlreadyBoundException;
import org.webharvest.Harvester;
import org.webharvest.Registry;

import com.google.common.eventbus.EventBus;
import com.google.inject.Provider;

/** Tests runtime-wide registration and Harvester-scoped event routing. */
public class DefaultHandlerHolderTest {

    static class TestHarvesterEvent implements HarvesterEvent {
        private final Harvester harvester;

        TestHarvesterEvent(Harvester harvester) {
            this.harvester = harvester;
        }

        @Override
        public Harvester getHarvester() {
            return harvester;
        }
    }

    static class OtherHarvesterEvent implements HarvesterEvent {
        private final Harvester harvester;

        OtherHarvesterEvent(Harvester harvester) {
            this.harvester = harvester;
        }

        @Override
        public Harvester getHarvester() {
            return harvester;
        }
    }

    static class RecordingHandler implements EventHandler<TestHarvesterEvent> {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        @com.google.common.eventbus.Subscribe
        public void handle(TestHarvesterEvent event) {
            calls.incrementAndGet();
        }
    }

    private DefaultHandlerHolder holder;
    private Registry<Harvester, EventBus> registry;
    private Provider<EventBus> provider;
    private EventBus eventBus;

    @BeforeMethod
    public void setUp() {
        registry = mock(Registry.class);
        provider = mock(Provider.class);
        eventBus = mock(EventBus.class);
        when(provider.get()).thenReturn(eventBus);
        holder = new DefaultHandlerHolder(registry, provider);
    }

    @Test
    public void registerRetainsHandlerWithoutPerSessionBusRegistration() {
        RecordingHandler handler = new RecordingHandler();
        holder.register(handler);

        verifyNoInteractions(eventBus);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void registerRejectsNullHandler() {
        holder.register(null);
    }

    @Test
    public void beforeScrapingBindsHarvesterToSharedRuntimeBus() throws AlreadyBoundException {
        Harvester harvester = mock(Harvester.class);

        holder.onBeforeScraping(harvester);

        verify(provider).get();
        verify(registry).bind(harvester, eventBus);
        verifyNoInteractions(eventBus);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void beforeScrapingRejectsNullHarvester() {
        holder.onBeforeScraping(null);
    }

    @Test
    public void beforeScrapingWrapsDuplicateHarvesterBinding() throws AlreadyBoundException {
        Harvester harvester = mock(Harvester.class);
        doThrow(new AlreadyBoundException("duplicate")).when(registry).bind(harvester, eventBus);

        try {
            holder.onBeforeScraping(harvester);
            fail("Expected duplicate binding failure");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("already has an active event route"));
        }
    }

    @Test
    public void afterScrapingUnbindsOnlyTheHarvesterRoute() {
        Harvester harvester = mock(Harvester.class);
        when(registry.lookup(harvester)).thenReturn(eventBus);

        holder.onAfterScraping(harvester);

        verify(registry).lookup(harvester);
        verify(registry).unbind(harvester);
        verifyNoInteractions(eventBus);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void afterScrapingRejectsNullHarvester() {
        holder.onAfterScraping(null);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void afterScrapingRejectsUnknownHarvester() {
        Harvester harvester = mock(Harvester.class);
        when(registry.lookup(harvester)).thenReturn(null);

        holder.onAfterScraping(harvester);
    }

    @Test
    public void dispatchesOnlyMatchingEventTypesForActiveHarvesterRoutes() {
        Harvester first = mock(Harvester.class);
        Harvester second = mock(Harvester.class);
        RecordingHandler handler = new RecordingHandler();
        holder.register(handler);
        when(registry.lookup(first)).thenReturn(eventBus);
        when(registry.lookup(second)).thenReturn(eventBus);

        holder.onHarvesterEvent(new TestHarvesterEvent(first));
        holder.onHarvesterEvent(new OtherHarvesterEvent(first));
        holder.onHarvesterEvent(new TestHarvesterEvent(second));

        assertEquals(handler.calls.get(), 2,
            "Only matching event types on active harvester routes should be dispatched");

        holder.onAfterScraping(first);
        doReturn(null).when(registry).lookup(first);
        holder.onHarvesterEvent(new TestHarvesterEvent(first));
        holder.onHarvesterEvent(new TestHarvesterEvent(second));
        assertEquals(handler.calls.get(), 3,
            "Ending one session route must not disable another active session");
    }

    @Test
    public void sharedEventBusPostDispatchesThroughHarvesterRoute() {
        EventBus sharedBus = new EventBus();
        Provider<EventBus> sharedProvider = () -> sharedBus;
        DefaultHandlerHolder dispatcher = new DefaultHandlerHolder(registry, sharedProvider);
        sharedBus.register(dispatcher);

        Harvester harvester = mock(Harvester.class);
        RecordingHandler handler = new RecordingHandler();
        dispatcher.register(handler);
        when(registry.lookup(harvester)).thenReturn(sharedBus);

        sharedBus.post(new TestHarvesterEvent(harvester));

        assertEquals(handler.calls.get(), 1,
            "The shared EventBus must dispatch through the active Harvester route");
    }

    @Test
    public void continuesDispatchAndAggregatesHandlerFailures() {
        Harvester harvester = mock(Harvester.class);
        when(registry.lookup(harvester)).thenReturn(eventBus);
        RuntimeException firstFailure = new IllegalStateException("first");
        RuntimeException secondFailure = new IllegalArgumentException("second");
        AtomicInteger laterCalls = new AtomicInteger();

        holder.register(new EventHandler<TestHarvesterEvent>() {
            @Override
            @com.google.common.eventbus.Subscribe
            public void handle(TestHarvesterEvent event) {
                throw firstFailure;
            }
        });
        holder.register(new EventHandler<TestHarvesterEvent>() {
            @Override
            @com.google.common.eventbus.Subscribe
            public void handle(TestHarvesterEvent event) {
                throw secondFailure;
            }
        });
        holder.register(new EventHandler<TestHarvesterEvent>() {
            @Override
            @com.google.common.eventbus.Subscribe
            public void handle(TestHarvesterEvent event) {
                laterCalls.incrementAndGet();
            }
        });

        try {
            holder.onHarvesterEvent(new TestHarvesterEvent(harvester));
            fail("Expected aggregated handler failure");
        } catch (RuntimeException actual) {
            assertSame(actual, firstFailure);
            assertEquals(actual.getSuppressed().length, 1);
            assertSame(actual.getSuppressed()[0], secondFailure);
        }
        assertEquals(laterCalls.get(), 1);
    }

    @Test
    public void sameHandlerMayReceiveEventsFromIndependentSessionsConcurrently()
        throws InterruptedException {
        Harvester first = mock(Harvester.class);
        Harvester second = mock(Harvester.class);
        when(registry.lookup(first)).thenReturn(eventBus);
        when(registry.lookup(second)).thenReturn(eventBus);
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maximum = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        holder.register(new EventHandler<TestHarvesterEvent>() {
            @Override
            @com.google.common.eventbus.Subscribe
            public void handle(TestHarvesterEvent event) {
                int current = active.incrementAndGet();
                maximum.accumulateAndGet(current, Math::max);
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                } finally {
                    active.decrementAndGet();
                }
            }
        });

        Thread one = new Thread(() -> dispatch(holder, new TestHarvesterEvent(first), failure));
        Thread two = new Thread(() -> dispatch(holder, new TestHarvesterEvent(second), failure));
        one.start();
        two.start();
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS),
                "Both sessions should dispatch concurrently to the shared handler");
        } finally {
            release.countDown();
        }
        one.join(5000);
        two.join(5000);

        assertFalse(one.isAlive());
        assertFalse(two.isAlive());
        assertNull(failure.get());
        assertEquals(maximum.get(), 2);
    }

    private static void dispatch(DefaultHandlerHolder holder, HarvesterEvent event,
                                 AtomicReference<Throwable> failure) {
        try {
            holder.onHarvesterEvent(event);
        } catch (Throwable unexpected) {
            failure.set(unexpected);
        }
    }
}
