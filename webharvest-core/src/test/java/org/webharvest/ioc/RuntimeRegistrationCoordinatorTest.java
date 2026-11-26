/*
 Copyright (c) 2006-2025 the original author or authors.

 Redistribution and use of this software in source and binary forms,
 with or without modification, are permitted provided that the following
 conditions are met:

 * Redistributions of source code must retain the above
   copyright notice, this list of conditions and the
   following disclaimer.

 * Redistributions in binary form must reproduce the above
   copyright notice, this list of conditions and the
   following disclaimer in the documentation and/or other
   materials provided with the distribution.

 * The name of Web-Harvest may not be used to endorse or promote
   products derived from this software without specific prior
   written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 POSSIBILITY OF SUCH DAMAGE.
 */

package org.webharvest.ioc;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.annotations.Test;
import org.webharvest.Harvester;
import org.webharvest.ScrapingAware;
import org.webharvest.events.EventBus;
import org.webharvest.runtime.service.WebHarvestService;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Guice;
import com.google.inject.Injector;

/** Runtime ownership and retirement tests for automatic Guice registrations. */
public class RuntimeRegistrationCoordinatorTest {

    static class Subscriber {
        private final AtomicInteger events = new AtomicInteger();

        @Subscribe
        public void onEvent(String event) {
            events.incrementAndGet();
        }
    }

    static class Listener implements ScrapingAware {
        private final AtomicInteger before = new AtomicInteger();
        private final AtomicInteger after = new AtomicInteger();

        @Override
        public void onBeforeScraping(Harvester harvester) {
            before.incrementAndGet();
        }

        @Override
        public void onAfterScraping(Harvester harvester) {
            after.incrementAndGet();
        }
    }

    @Test
    public void serviceShutdownRetiresOnlyItsRuntimeRegistrations() throws Exception {
        Injector first = Guice.createInjector(new ScraperModule("target"));
        Injector second = Guice.createInjector(new ScraperModule("target"));

        Subscriber firstSubscriber = first.getInstance(Subscriber.class);
        Subscriber secondSubscriber = second.getInstance(Subscriber.class);
        Listener firstListener = first.getInstance(Listener.class);
        Listener secondListener = second.getInstance(Listener.class);

        WebHarvestService firstService = first.getInstance(WebHarvestService.class);
        WebHarvestService secondService = second.getInstance(WebHarvestService.class);
        firstService.shutdown();

        first.getInstance(com.google.common.eventbus.EventBus.class).post("after-close");
        second.getInstance(com.google.common.eventbus.EventBus.class).post("still-open");

        ScrapingInterceptor.ScrapingAwareHelper firstHelper =
            first.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class);
        ScrapingInterceptor.ScrapingAwareHelper secondHelper =
            second.getInstance(ScrapingInterceptor.ScrapingAwareHelper.class);
        Harvester harvester = mock(Harvester.class);
        firstHelper.onBeforeScraping(harvester);
        secondHelper.onBeforeScraping(harvester);
        firstHelper.onAfterScraping(harvester);
        secondHelper.onAfterScraping(harvester);

        assertEquals(firstSubscriber.events.get(), 0,
            "Closing runtime A must unregister its EventBus subscriber");
        assertEquals(secondSubscriber.events.get(), 1,
            "Closing runtime A must not unregister runtime B's subscriber");
        assertEquals(firstListener.before.get(), 0,
            "Closing runtime A must remove its ScrapingAware listener");
        assertEquals(firstListener.after.get(), 0);
        assertEquals(secondListener.before.get(), 1);
        assertEquals(secondListener.after.get(), 1);

        secondService.shutdown();
    }

    @Test
    public void registrationRacingCloseIsEitherRetiredOrRejected() throws InterruptedException {
        com.google.common.eventbus.EventBus eventBus = new com.google.common.eventbus.EventBus();
        RuntimeRegistrationCoordinator coordinator =
            new RuntimeRegistrationCoordinator(eventBus,
                new ScrapingInterceptor.ScrapingAwareHelper());
        Subscriber subscriber = new Subscriber();
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> registrationFailure = new AtomicReference<Throwable>();

        Thread registration = new Thread(() -> {
            try {
                start.await();
                coordinator.registerSubscriber(subscriber);
            } catch (IllegalStateException closed) {
                // Close won admission; rejection is the other valid race outcome.
            } catch (Throwable failure) {
                registrationFailure.set(failure);
            }
        });
        Thread retirement = new Thread(() -> {
            try {
                start.await();
                coordinator.close();
            } catch (Throwable failure) {
                registrationFailure.set(failure);
            }
        });

        registration.start();
        retirement.start();
        start.countDown();
        registration.join(TimeUnit.SECONDS.toMillis(5));
        retirement.join(TimeUnit.SECONDS.toMillis(5));

        assertFalse(registration.isAlive(), "Registration race must terminate");
        assertFalse(retirement.isAlive(), "Retirement race must terminate");
        assertNull(registrationFailure.get());
        eventBus.post("after-race");
        assertEquals(subscriber.events.get(), 0,
            "A registration admitted before close must be retired; a later one rejected");
        assertEquals(coordinator.getSubscriberRegistrationCount(), 0);
    }

    @Test
    public void closeIsIdempotentAndRejectsNewRegistrations() {
        com.google.common.eventbus.EventBus eventBus = new com.google.common.eventbus.EventBus();
        RuntimeRegistrationCoordinator coordinator =
            new RuntimeRegistrationCoordinator(eventBus,
                new ScrapingInterceptor.ScrapingAwareHelper());
        Subscriber subscriber = new Subscriber();

        coordinator.registerSubscriber(subscriber);
        coordinator.close();
        coordinator.close();

        assertThrows(IllegalStateException.class,
            () -> coordinator.registerSubscriber(new Subscriber()));
        assertEquals(coordinator.getSubscriberRegistrationCount(), 0);
    }
}
