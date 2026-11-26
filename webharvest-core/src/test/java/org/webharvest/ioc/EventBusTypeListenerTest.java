package org.webharvest.ioc;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;

/** Tests injector-local EventBus subscriber registration. */
public class EventBusTypeListenerTest {

    static class Subscriber {
        private final AtomicInteger received = new AtomicInteger();

        @Subscribe
        public void onEvent(String event) {
            received.incrementAndGet();
        }
    }

    static class NonSubscriber {
        public void onEvent(String event) {
            // Deliberately not a subscriber.
        }
    }

    static class BaseSubscriber {
        private final AtomicInteger received = new AtomicInteger();

        @Subscribe
        public void onInheritedEvent(String event) {
            received.incrementAndGet();
        }
    }

    static class InheritedSubscriber extends BaseSubscriber {
        // Inherits the @Subscribe method from BaseSubscriber.
    }

    @Test
    public void subscriberUsesProviderCapturedFromItsTypeEncounter() {
        EventBusTypeListener listener = new EventBusTypeListener();
        @SuppressWarnings("unchecked")
        TypeEncounter<Subscriber> encounter = mock(TypeEncounter.class);
        @SuppressWarnings("unchecked")
        Provider<EventBus> provider = mock(Provider.class);
        EventBus eventBus = mock(EventBus.class);
        Subscriber subscriber = new Subscriber();
        when(encounter.getProvider(EventBus.class)).thenReturn(provider);
        when(provider.get()).thenReturn(eventBus);

        listener.hear(TypeLiteral.get(Subscriber.class), encounter);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<InjectionListener<Subscriber>> captor =
                org.mockito.ArgumentCaptor.forClass(InjectionListener.class);
        verify(encounter).register(captor.capture());
        captor.getValue().afterInjection(subscriber);

        verify(provider).get();
        verify(eventBus).register(subscriber);
    }

    @Test
    public void nonSubscriberDoesNotResolveOrRegisterEventBus() {
        EventBusTypeListener listener = new EventBusTypeListener();
        @SuppressWarnings("unchecked")
        TypeEncounter<NonSubscriber> encounter = mock(TypeEncounter.class);

        listener.hear(TypeLiteral.get(NonSubscriber.class), encounter);

        verify(encounter, never()).getProvider(EventBus.class);
        verify(encounter, never()).register(any(InjectionListener.class));
    }

    @Test
    public void inheritedSubscriberMethodsAreRegistered() {
        Injector injector = Guice.createInjector(new ScraperModule("target"));
        EventBus eventBus = injector.getInstance(EventBus.class);
        InheritedSubscriber subscriber = injector.getInstance(InheritedSubscriber.class);

        eventBus.post("inherited");

        assertEquals(subscriber.received.get(), 1,
            "Subscriber methods declared by a superclass must be discovered");
    }

    @Test
    public void independentInjectorsOwnIndependentBusesAndSubscribers() {
        Injector first = Guice.createInjector(new ScraperModule("target"));
        Injector second = Guice.createInjector(new ScraperModule("target"));

        EventBus firstBus = first.getInstance(EventBus.class);
        EventBus secondBus = second.getInstance(EventBus.class);
        assertNotSame(firstBus, secondBus,
            "Each injector/runtime must own a distinct EventBus instance");

        Subscriber firstSubscriber = first.getInstance(Subscriber.class);
        Subscriber secondSubscriber = second.getInstance(Subscriber.class);

        firstBus.post("first");
        assertEquals(firstSubscriber.received.get(), 1);
        assertEquals(secondSubscriber.received.get(), 0,
            "A subscriber created by another injector must not receive this runtime's event");

        secondBus.post("second");
        assertEquals(firstSubscriber.received.get(), 1);
        assertEquals(secondSubscriber.received.get(), 1);
    }
}
