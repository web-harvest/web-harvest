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

/** Concurrency and registration-token tests for ScrapingAwareHelper. */
public class ScrapingAwareHelperTest {

    @Test
    public void unregisterAffectsFutureSnapshotsButNotAnInFlightSnapshot() throws InterruptedException {
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        Harvester harvester = mock(Harvester.class);
        CountDownLatch callbackEntered = new CountDownLatch(1);
        CountDownLatch releaseCallback = new CountDownLatch(1);
        AtomicInteger removedCalls = new AtomicInteger();
        AtomicInteger removedAfterCalls = new AtomicInteger();
        AtomicInteger addedCalls = new AtomicInteger();
        AtomicInteger addedAfterCalls = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        ScrapingAware removedListener = new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                removedCalls.incrementAndGet();
                callbackEntered.countDown();
                try {
                    releaseCallback.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Callback interrupted", interrupted);
                }
            }

            @Override
            public void onAfterScraping(Harvester value) {
                removedAfterCalls.incrementAndGet();
            }
        };
        ScrapingInterceptor.ScrapingAwareHelper.ListenerRegistration token =
                helper.registerListener(removedListener);

        Thread notification = new Thread(() -> {
            try {
                helper.onBeforeScraping(harvester);
                helper.onAfterScraping(harvester);
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        });
        notification.start();
        try {
            assertTrue(callbackEntered.await(5, TimeUnit.SECONDS));
            token.close();
            token.close();
            assertTrue(token.isClosed());
            helper.addListener(new ScrapingAware() {
                @Override
                public void onBeforeScraping(Harvester value) {
                    addedCalls.incrementAndGet();
                }

                @Override
                public void onAfterScraping(Harvester value) {
                    addedAfterCalls.incrementAndGet();
                }
            });
        } finally {
            releaseCallback.countDown();
        }
        notification.join(5000);

        assertFalse(notification.isAlive());
        assertNull(failure.get());
        assertEquals(removedCalls.get(), 1,
            "An in-flight snapshot may finish after its token is closed");
        assertEquals(addedCalls.get(), 0,
            "A listener added after snapshot capture must not receive that notification");
        assertEquals(removedAfterCalls.get(), 1,
            "A listener removed mid-scope must still receive its matching after notification");
        assertEquals(addedAfterCalls.get(), 0,
            "A listener added mid-scope must not receive an unmatched after notification");

        helper.onBeforeScraping(harvester);
        helper.onAfterScraping(harvester);
        assertEquals(removedCalls.get(), 1);
        assertEquals(addedCalls.get(), 1);
        assertEquals(addedAfterCalls.get(), 1);
    }

    @Test
    public void nestedScopesReuseTheirOwnListenerSnapshotsInLifoOrder() {
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        Harvester harvester = mock(Harvester.class);
        AtomicInteger firstAfterCalls = new AtomicInteger();
        AtomicInteger secondAfterCalls = new AtomicInteger();
        ScrapingInterceptor.ScrapingAwareHelper.ListenerRegistration first =
                helper.registerListener(new ScrapingAware() {
                    @Override
                    public void onBeforeScraping(Harvester value) {
                        // No-op.
                    }
                    @Override
                    public void onAfterScraping(Harvester value) {
                        firstAfterCalls.incrementAndGet();
                    }
                });

        helper.onBeforeScraping(harvester);
        helper.registerListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                        // No-op.
                    }
            @Override
            public void onAfterScraping(Harvester value) {
                secondAfterCalls.incrementAndGet();
            }
        });
        helper.onBeforeScraping(harvester);
        first.close();

        helper.onAfterScraping(harvester);
        helper.onAfterScraping(harvester);
        assertEquals(firstAfterCalls.get(), 2,
                "Both nested and outer scopes must retain their captured first listener");
        assertEquals(secondAfterCalls.get(), 1,
                "The listener added after outer entry belongs only to the nested scope");
    }

    @Test
    public void sameListenerMayReceiveConcurrentCallbacksWithoutGlobalSerialization()
        throws InterruptedException {
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        Harvester harvester = mock(Harvester.class);
        CountDownLatch callbacksEntered = new CountDownLatch(2);
        CountDownLatch releaseCallbacks = new CountDownLatch(1);
        AtomicInteger activeCallbacks = new AtomicInteger();
        AtomicInteger maximumConcurrency = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                int active = activeCallbacks.incrementAndGet();
                maximumConcurrency.accumulateAndGet(active, Math::max);
                callbacksEntered.countDown();
                try {
                    releaseCallbacks.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Callback interrupted", interrupted);
                } finally {
                    activeCallbacks.decrementAndGet();
                }
            }

            @Override
            public void onAfterScraping(Harvester value) {
                // No-op.
            }
        });

        Runnable notify = () -> {
            try {
                helper.onBeforeScraping(harvester);
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        };
        Thread first = new Thread(notify);
        Thread second = new Thread(notify);
        first.start();
        second.start();
        try {
            assertTrue(callbacksEntered.await(5, TimeUnit.SECONDS),
                "Both sessions should enter the same listener concurrently");
        } finally {
            releaseCallbacks.countDown();
        }
        first.join(5000);
        second.join(5000);

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertNull(failure.get());
        assertEquals(maximumConcurrency.get(), 2);
    }

    @Test
    public void notificationContinuesAfterRuntimeExceptionAndPreservesFailures() {
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        Harvester harvester = mock(Harvester.class);
        RuntimeException firstFailure = new IllegalStateException("first listener failed");
        RuntimeException secondFailure = new IllegalArgumentException("second listener failed");
        RuntimeException afterFirstFailure = new IllegalStateException("first after-listener failed");
        RuntimeException afterSecondFailure = new IllegalArgumentException("second after-listener failed");
        AtomicInteger laterListenerCalls = new AtomicInteger();

        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                throw firstFailure;
            }

            @Override
            public void onAfterScraping(Harvester value) {
                throw afterFirstFailure;
            }
        });
        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                throw secondFailure;
            }

            @Override
            public void onAfterScraping(Harvester value) {
                throw afterSecondFailure;
            }
        });
        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                laterListenerCalls.incrementAndGet();
            }

            @Override
            public void onAfterScraping(Harvester value) {
                laterListenerCalls.incrementAndGet();
            }
        });

        try {
            helper.onBeforeScraping(harvester);
            fail("Expected aggregated before-listener failure");
        } catch (RuntimeException actual) {
            assertSame(actual, firstFailure);
            assertEquals(actual.getSuppressed().length, 1);
            assertSame(actual.getSuppressed()[0], secondFailure);
        }
        assertEquals(laterListenerCalls.get(), 1);

        try {
            helper.onAfterScraping(harvester);
            fail("Expected aggregated after-listener failure");
        } catch (RuntimeException actual) {
            assertSame(actual, afterFirstFailure);
            assertEquals(actual.getSuppressed().length, 1);
            assertSame(actual.getSuppressed()[0], afterSecondFailure);
        }
        assertEquals(laterListenerCalls.get(), 2);
    }
    @Test
    public void fatalAfterListenerFailureDoesNotPreventRemainingCleanupListeners() {
        ScrapingInterceptor.ScrapingAwareHelper helper =
                new ScrapingInterceptor.ScrapingAwareHelper();
        Harvester harvester = mock(Harvester.class);
        AssertionError fatalFailure = new AssertionError("fatal cleanup failure");
        AtomicInteger laterListenerCalls = new AtomicInteger();

        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                // No-op.
            }

            @Override
            public void onAfterScraping(Harvester value) {
                throw fatalFailure;
            }
        });
        helper.addListener(new ScrapingAware() {
            @Override
            public void onBeforeScraping(Harvester value) {
                // No-op.
            }

            @Override
            public void onAfterScraping(Harvester value) {
                laterListenerCalls.incrementAndGet();
            }
        });

        try {
            helper.onAfterScraping(harvester);
            fail("Expected the first listener's fatal failure");
        } catch (AssertionError actual) {
            assertSame(actual, fatalFailure);
        }

        assertEquals(laterListenerCalls.get(), 1,
                "A fatal listener failure must not skip remaining teardown listeners");
    }

}
