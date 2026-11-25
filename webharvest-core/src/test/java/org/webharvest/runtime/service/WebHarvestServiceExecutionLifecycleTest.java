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

package org.webharvest.runtime.service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.mockito.Mockito;
import org.testng.annotations.Test;
import org.webharvest.definition.Config;
import org.webharvest.runtime.session.ExecutionState;
import org.webharvest.runtime.session.ExecutionTrackedSession;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.SessionRegistryImpl;
import org.webharvest.runtime.session.SessionStatus;
import org.webharvest.runtime.session.TerminationDisposition;
import org.webharvest.runtime.session.events.SessionCreatedEvent;
import org.webharvest.runtime.session.events.SessionStartedEvent;
import org.webharvest.runtime.resource.ExecutionResourceAccess;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/** Deterministic tests for service task ownership, deadlines and shutdown. */
public class WebHarvestServiceExecutionLifecycleTest {

    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testServiceRejectsNullExecutor() {
        new WebHarvestServiceImpl(null, new SessionRegistryImpl(), new EventBus());
    }

    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testServiceRejectsNullRegistry() {
        new WebHarvestServiceImpl(new InlineExecutorService(), null, new EventBus());
    }

    @Test(groups = {"unit", "service"}, expectedExceptions = IllegalArgumentException.class)
    public void testServiceRejectsNullEventBus() {
        new WebHarvestServiceImpl(new InlineExecutorService(), new SessionRegistryImpl(), null);
    }

    @Test(groups = {"unit", "service"})
    public void testServiceAssignsRuntimeScopedMonotonicExecutionOwners() throws InterruptedException {
        WebHarvestServiceImpl firstRuntime = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), new EventBus());
        WebHarvestServiceImpl secondRuntime = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), new EventBus());

        try {
            ScraperSession first = firstRuntime.executeAsync(Mockito.mock(Config.class), null, null);
            ScraperSession second = firstRuntime.executeAsync(Mockito.mock(Config.class), null, null);
            ScraperSession otherRuntime = secondRuntime.executeAsync(Mockito.mock(Config.class), null, null);

            ExecutionTrackedSession firstTracked = (ExecutionTrackedSession) first;
            ExecutionTrackedSession secondTracked = (ExecutionTrackedSession) second;
            ExecutionTrackedSession otherTracked = (ExecutionTrackedSession) otherRuntime;

            assertEquals(firstTracked.getExecutionOwner().getSessionId(), first.getSessionId());
            assertEquals(secondTracked.getExecutionOwner().getSessionId(), second.getSessionId());
            assertEquals(firstTracked.getExecutionOwner().getRuntimeId(),
                secondTracked.getExecutionOwner().getRuntimeId());
            assertEquals(firstTracked.getExecutionOwner().getExecutionGeneration(), 1L);
            assertEquals(secondTracked.getExecutionOwner().getExecutionGeneration(), 2L);
            assertFalse(firstTracked.getExecutionOwner().getRuntimeId().equals(
                otherTracked.getExecutionOwner().getRuntimeId()),
                "Independent runtime instances must have distinct owner identities");
            assertEquals(otherTracked.getExecutionOwner().getExecutionGeneration(), 1L);
        } finally {
            firstRuntime.shutdown();
            secondRuntime.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testCancellationAbortsSessionResourcesBeforeWorkerExit() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingResourceListener listener = new BlockingResourceListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            java.util.concurrent.Executors.newSingleThreadExecutor(),
            new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));

            assertTrue(session.cancel());
            assertEquals(listener.abortCount.get(), 1,
                "Resource abort must be requested before the worker is interrupted");
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS));
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(listener.closeCount.get(), 1);
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        } finally {
            listener.release.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testSessionResourcesAreClosedAfterObservedWorkerExit() throws InterruptedException {
        EventBus eventBus = new EventBus();
        SessionResourceRegistrationListener listener = new SessionResourceRegistrationListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertTrue(tracked.awaitExecutionCompletion(0));
            assertNotNull(listener.resource);
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
            assertEquals(listener.closeCount.get(), 1);
            assertEquals(listener.abortCount.get(), 0);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownIsIdempotent() throws InterruptedException {
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), new EventBus());
        service.shutdown();
        service.shutdown();
        assertTrue(service.isShutdown());
    }

    @Test(groups = {"unit", "service"})
    public void testCancellationDuringSessionCreatedEventPreventsWorkerEntry() throws InterruptedException {
        EventBus eventBus = new EventBus();
        eventBus.register(new CancelOnCreatedListener());
        InlineExecutorService executor = new InlineExecutorService();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.CANCELLED_BEFORE_START);
            assertTrue(tracked.awaitExecutionCompletion(0));
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testCreatedEventCancellationCancelsFutureBeforeWorkerEntry() throws InterruptedException {
        EventBus eventBus = new EventBus();
        eventBus.register(new CancelOnCreatedListener());
        ExecutorService executor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        executor.submit(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.CANCELLED_BEFORE_START);
            assertTrue(tracked.awaitExecutionCompletion(0));
        } finally {
            releaseBlocker.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownNowMarksExecutorDrainedQueuedTaskExited() throws InterruptedException {
        java.util.concurrent.ThreadPoolExecutor delegate = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        CountDownLatch blockerStarted = new CountDownLatch(1);
        delegate.submit(() -> {
            blockerStarted.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        ExecutorService wrappedExecutor = new DelegatingExecutorService(delegate);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            wrappedExecutor, new SessionRegistryImpl(), new EventBus());

        try {
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            delegate.submit(() -> { });
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            service.shutdownNow();
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertTrue(tracked.awaitExecutionCompletion(0));
        } finally {
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testQueuedCancellationRunsResourceCloseOutsideTaskMonitor() throws Exception {
        java.util.concurrent.ThreadPoolExecutor executor = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        executor.execute(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), new EventBus());

        try {
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            CountDownLatch closeCompleted = new CountDownLatch(1);
            AtomicReference<Throwable> callbackFailure =
                registerCloseCallbackMonitorProbe(service, session, closeCompleted, false);

            assertTrue(session.cancel());
            assertTrue(closeCompleted.await(5, TimeUnit.SECONDS),
                "Queued-session resource close callback must finish");
            assertNull(callbackFailure.get(),
                "Resource close callback must be able to acquire the SessionTask monitor");
            assertTrue(((ExecutionTrackedSession) session).awaitExecutionCompletion(0));
        } finally {
            releaseBlocker.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownNowRemovalRunsResourceCloseOutsideTaskMonitor() throws Exception {
        java.util.concurrent.ThreadPoolExecutor delegate = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        delegate.execute(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new DelegatingExecutorService(delegate), new SessionRegistryImpl(), new EventBus());

        try {
            assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            CountDownLatch closeCompleted = new CountDownLatch(1);
            AtomicReference<Throwable> callbackFailure =
                registerCloseCallbackMonitorProbe(service, session, closeCompleted, false);

            service.shutdownNow();
            assertTrue(closeCompleted.await(5, TimeUnit.SECONDS),
                "shutdownNow-removed resource close callback must finish");
            assertNull(callbackFailure.get(),
                "shutdownNow resource close callback must run outside the SessionTask monitor");
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
        } finally {
            releaseBlocker.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testFutureAttachmentRemovalRunsResourceCloseOutsideTaskMonitor() throws Exception {
        QueueThenBlockThreadPoolExecutor executor = new QueueThenBlockThreadPoolExecutor();
        CountDownLatch blockerStarted = new CountDownLatch(1);
        CountDownLatch releaseBlocker = new CountDownLatch(1);
        executor.execute(() -> {
            blockerStarted.countDown();
            try {
                releaseBlocker.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        });
        assertTrue(blockerStarted.await(5, TimeUnit.SECONDS));
        executor.blockNextSubmission();

        EventBus eventBus = new EventBus();
        CapturingCreatedListener capture = new CapturingCreatedListener();
        eventBus.register(capture);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);
        AtomicReference<ScraperSession> submitted = new AtomicReference<>();
        AtomicReference<Throwable> submitFailure = new AtomicReference<>();
        Thread submitter = new Thread(() -> {
            try {
                submitted.set(service.executeAsync(Mockito.mock(Config.class), null, null));
            } catch (Throwable failure) {
                submitFailure.set(failure);
            }
        }, "future-attachment-submit");

        try {
            submitter.start();
            assertTrue(executor.queued.await(5, TimeUnit.SECONDS),
                "The submitted Future must be queued before cancellation");
            ScraperSession session = capture.session;
            assertNotNull(session);
            CountDownLatch closeCompleted = new CountDownLatch(1);
            AtomicReference<Throwable> callbackFailure =
                registerCloseCallbackMonitorProbe(service, session, closeCompleted, true);

            assertTrue(session.cancel());
            executor.releaseSubmission.countDown();
            submitter.join(5000);
            assertFalse(submitter.isAlive());
            assertNull(submitFailure.get());
            assertSame(submitted.get(), session);
            assertTrue(closeCompleted.await(5, TimeUnit.SECONDS),
                "Future-attachment resource close callback must finish");
            assertNull(callbackFailure.get(),
                "Future-attachment close callback must run outside the SessionTask monitor");
            assertTrue(((ExecutionTrackedSession) session).awaitExecutionCompletion(0));
        } finally {
            executor.releaseSubmission.countDown();
            releaseBlocker.countDown();
            submitter.join(5000);
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testCancellationBeforeFutureAttachmentInterruptsActiveWorker() throws InterruptedException {
        EventBus eventBus = new EventBus();
        CancelAndHoldStartedListener listener = new CancelAndHoldStartedListener();
        eventBus.register(listener);
        ExecutorService executor = new StartAndCancelExecutorService(listener.cancelled);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.GRACE_IN_PROGRESS);
            assertFalse(tracked.awaitExecutionCompletion(50),
                "The worker remains active in its controlled callback after cancellation");
            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.GRACEFUL);
        } finally {
            listener.release.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testRejectedSubmissionRollsBackRegistryAndExitsSession() throws InterruptedException {
        EventBus eventBus = new EventBus();
        CapturingCreatedListener capture = new CapturingCreatedListener();
        eventBus.register(capture);
        SessionRegistryImpl registry = new SessionRegistryImpl();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new RejectingExecutorService(), registry, eventBus);

        try {
            try {
                service.executeAsync(Mockito.mock(Config.class), null, null);
                fail("Rejected executor submission must not be accepted");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getCause() instanceof RejectedExecutionException);
            }

            assertEquals(registry.size(), 0);
            assertNotNull(capture.session);
            assertEquals(capture.session.getStatus(), SessionStatus.FAILED);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) capture.session;
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertTrue(tracked.awaitExecutionCompletion(0));
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testTimeoutDeadlineIsRetiredAfterSuccessfulCompletion() throws InterruptedException {
        WebHarvestServiceImpl service = new WebHarvestServiceImpl();
        try {
            SessionOptions options = SessionOptions.builder()
                .timeout(Duration.ofMinutes(1))
                .build();
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, options);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertTrue(session.awaitCompletion(5000));
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(session.getStatus(), SessionStatus.COMPLETED);
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.NOT_REQUESTED);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testTimeoutInterruptsWorkerAndWaitsForObservedExit() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            SessionOptions options = SessionOptions.builder()
                .timeout(Duration.ofMillis(200))
                .build();
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, options);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertTrue(listener.started.await(5, TimeUnit.SECONDS));
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS),
                "Deadline should interrupt the active worker");
            assertEquals(session.getStatus(), SessionStatus.RUNNING,
                "A timeout request is not itself a terminal business result");
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.GRACE_IN_PROGRESS);
            assertFalse(tracked.awaitExecutionCompletion(50),
                "The worker is still executing its non-cooperative callback");

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.FAILED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.GRACEFUL);
            assertTrue(session.getError() instanceof TimeoutException);
        } finally {
            listener.release.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testSubmitAndShutdownHaveAtomicAdmissionBoundary() throws InterruptedException {
        EventBus eventBus = new EventBus();
        CountDownLatch submitEntered = new CountDownLatch(1);
        CountDownLatch releaseSubmit = new CountDownLatch(1);
        CountDownLatch shutdownStarted = new CountDownLatch(1);
        BlockingSubmitExecutorService executor = new BlockingSubmitExecutorService(
            java.util.concurrent.Executors.newSingleThreadExecutor(), submitEntered, releaseSubmit);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);
        AtomicReference<ScraperSession> accepted = new AtomicReference<>();
        AtomicReference<Throwable> submitFailure = new AtomicReference<>();
        AtomicReference<Throwable> shutdownFailure = new AtomicReference<>();

        Thread submitter = new Thread(() -> {
            try {
                accepted.set(service.executeAsync(Mockito.mock(Config.class), null, null));
            } catch (Throwable failure) {
                submitFailure.set(failure);
            }
        });
        Thread shutdownThread = new Thread(() -> {
            shutdownStarted.countDown();
            try {
                service.shutdown();
            } catch (Throwable failure) {
                shutdownFailure.set(failure);
            }
        });

        try {
            submitter.start();
            assertTrue(submitEntered.await(5, TimeUnit.SECONDS));
            shutdownThread.start();
            assertTrue(shutdownStarted.await(5, TimeUnit.SECONDS));

            releaseSubmit.countDown();
            submitter.join(5000);
            shutdownThread.join(5000);
            assertFalse(submitter.isAlive());
            assertFalse(shutdownThread.isAlive());
            assertNull(submitFailure.get());
            assertNull(shutdownFailure.get());

            ScraperSession session = accepted.get();
            assertNotNull(session, "The submission must be either accepted and tracked or rejected");
            assertTrue(session.awaitCompletion(5000));
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertTrue(service.getSession(session.getSessionId()).isPresent());
        } finally {
            releaseSubmit.countDown();
            submitter.join(5000);
            shutdownThread.join(5000);
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testCreatedEventSubscriberDoesNotHoldAdmissionLockOrBlockShutdown() throws InterruptedException {
        EventBus eventBus = new EventBus();
        FirstCreatedBlockingListener listener = new FirstCreatedBlockingListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), eventBus);
        AtomicReference<ScraperSession> firstSession = new AtomicReference<>();
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        CountDownLatch shutdownFinished = new CountDownLatch(1);

        Thread firstSubmitter = new Thread(() -> {
            try {
                firstSession.set(service.executeAsync(Mockito.mock(Config.class), null, null));
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        }, "blocked-session-created-submit");
        Thread shutdownThread = new Thread(() -> {
            try {
                service.shutdown();
            } catch (Throwable failure) {
                firstFailure.compareAndSet(null, failure);
            } finally {
                shutdownFinished.countDown();
            }
        }, "shutdown-during-created-callback");

        try {
            firstSubmitter.start();
            assertTrue(listener.entered.await(5, TimeUnit.SECONDS));

            ScraperSession second = service.executeAsync(Mockito.mock(Config.class), null, null);
            assertNotNull(second);
            assertTrue(((ExecutionTrackedSession) second).awaitExecutionCompletion(0));

            shutdownThread.start();
            assertTrue(shutdownFinished.await(5, TimeUnit.SECONDS),
                "Shutdown must not wait for a SessionCreatedEvent subscriber that has not been admitted");

            listener.release.countDown();
            firstSubmitter.join(5000);
            assertFalse(firstSubmitter.isAlive());
            assertNull(firstSession.get(), "The first session must be rejected after shutdown closes admission");
            assertTrue(firstFailure.get() instanceof IllegalStateException);
            assertTrue(firstFailure.get().getMessage().contains("shut down"));
        } finally {
            listener.release.countDown();
            firstSubmitter.join(5000);
            shutdownThread.join(5000);
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testInlineExecutorRunsScraperOutsideAdmissionLock() throws InterruptedException {
        EventBus eventBus = new EventBus();
        FirstStartedBlockingListener listener = new FirstStartedBlockingListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), eventBus);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        Thread firstSubmitter = new Thread(() -> {
            try {
                service.executeAsync(Mockito.mock(Config.class), null, null);
            } catch (Throwable failure) {
                firstFailure.set(failure);
            }
        }, "inline-session-worker");

        try {
            firstSubmitter.start();
            assertTrue(listener.entered.await(5, TimeUnit.SECONDS));

            ScraperSession second = service.executeAsync(Mockito.mock(Config.class), null, null);
            assertNotNull(second,
                "A direct executor must not run scraper callbacks while admissionLock is held");
            assertTrue(((ExecutionTrackedSession) second).awaitExecutionCompletion(0));

            listener.release.countDown();
            firstSubmitter.join(5000);
            assertFalse(firstSubmitter.isAlive());
            assertNull(firstFailure.get());
        } finally {
            listener.release.countDown();
            firstSubmitter.join(5000);
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testSessionCompletedSubscriberRunsOutsideSessionTaskMonitor() throws Exception {
        EventBus eventBus = new EventBus();
        BlockingCompletedListener listener = new BlockingCompletedListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), eventBus);
        AtomicReference<Throwable> submitFailure = new AtomicReference<>();
        Thread submitter = new Thread(() -> {
            try {
                service.executeAsync(Mockito.mock(Config.class), null, null);
            } catch (Throwable failure) {
                submitFailure.set(failure);
            }
        }, "inline-session-completion");

        try {
            submitter.start();
            assertTrue(listener.entered.await(5, TimeUnit.SECONDS));

            Field tasksField = WebHarvestServiceImpl.class.getDeclaredField("executionTasks");
            tasksField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Object> tasks = (Map<String, Object>) tasksField.get(service);
            Object task = tasks.get(listener.session.getSessionId());
            assertNotNull(task, "The completing task remains registered during event delivery");

            CountDownLatch taskMonitorAcquired = new CountDownLatch(1);
            Thread monitorProbe = new Thread(() -> {
                synchronized (task) {
                    taskMonitorAcquired.countDown();
                }
            }, "session-task-monitor-probe");
            monitorProbe.start();

            assertTrue(taskMonitorAcquired.await(5, TimeUnit.SECONDS),
                "SessionCompletedEvent subscribers must not run while holding the SessionTask monitor");

            listener.release.countDown();
            submitter.join(5000);
            monitorProbe.join(5000);
            assertFalse(submitter.isAlive());
            assertFalse(monitorProbe.isAlive());
            assertNull(submitFailure.get());
            assertEquals(listener.session.getStatus(), SessionStatus.COMPLETED);
        } finally {
            listener.release.countDown();
            submitter.join(5000);
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testWorkerInitiatedShutdownDoesNotBlockQueuedSiblingOnSingleThreadExecutor()
        throws InterruptedException {
        EventBus eventBus = new EventBus();
        WorkerInitiatedShutdownListener listener = new WorkerInitiatedShutdownListener();
        eventBus.register(listener);
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);
        listener.service = service;

        try {
            ScraperSession first = service.executeAsync(Mockito.mock(Config.class), null, null);
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));

            // The first worker is held inside SessionStartedEvent, so this
            // second accepted task is deterministically queued behind it.
            ScraperSession second = service.executeAsync(Mockito.mock(Config.class), null, null);
            listener.allowShutdown.countDown();

            assertTrue(listener.shutdownReturned.await(5, TimeUnit.SECONDS),
                "Worker-initiated shutdown must return instead of waiting for its queued sibling");
            assertNull(listener.shutdownFailure.get());

            assertTrue(first.awaitCompletion(5000));
            assertTrue(second.awaitCompletion(5000));
            assertTrue(((ExecutionTrackedSession) first).awaitExecutionCompletion(5000));
            assertTrue(((ExecutionTrackedSession) second).awaitExecutionCompletion(5000));
            assertTrue(service.isShutdown());
        } finally {
            listener.allowShutdown.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testGracefulShutdownWaitsForInlineWorkerExitAfterExecutorTerminates() throws InterruptedException {
        EventBus eventBus = new EventBus();
        FirstStartedBlockingListener listener = new FirstStartedBlockingListener();
        eventBus.register(listener);
        CountDownLatch executorTerminationObserved = new CountDownLatch(1);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new SignallingInlineExecutorService(executorTerminationObserved), new SessionRegistryImpl(), eventBus);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch shutdownFinished = new CountDownLatch(1);

        Thread submitter = new Thread(() -> {
            try {
                service.executeAsync(Mockito.mock(Config.class), null, null);
            } catch (Throwable unexpected) {
                failure.compareAndSet(null, unexpected);
            }
        }, "caller-runs-session");
        Thread shutdownThread = new Thread(() -> {
            try {
                service.shutdown();
            } catch (Throwable unexpected) {
                failure.compareAndSet(null, unexpected);
            } finally {
                shutdownFinished.countDown();
            }
        }, "graceful-shutdown-caller-runs");

        try {
            submitter.start();
            assertTrue(listener.entered.await(5, TimeUnit.SECONDS));

            shutdownThread.start();
            assertTrue(executorTerminationObserved.await(5, TimeUnit.SECONDS),
                "Shutdown must observe executor termination before waiting for worker exit");
            assertEquals(shutdownFinished.getCount(), 1L,
                "Executor termination alone must not complete service shutdown");

            listener.release.countDown();
            submitter.join(5000);
            shutdownThread.join(5000);
            assertFalse(submitter.isAlive());
            assertFalse(shutdownThread.isAlive());
            assertNull(failure.get());
        } finally {
            listener.release.countDown();
            submitter.join(5000);
            shutdownThread.join(5000);
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testGracefulShutdownWaitsForInlineResourceDrainAfterSessionExit() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingCloseRegistrationListener listener = new BlockingCloseRegistrationListener();
        eventBus.register(listener);
        CountDownLatch executorTerminationObserved = new CountDownLatch(1);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new SignallingInlineExecutorService(executorTerminationObserved), new SessionRegistryImpl(), eventBus);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch shutdownFinished = new CountDownLatch(1);

        Thread submitter = new Thread(() -> {
            try {
                service.executeAsync(Mockito.mock(Config.class), null, null);
            } catch (Throwable unexpected) {
                failure.compareAndSet(null, unexpected);
            }
        }, "caller-runs-resource-drain");
        Thread shutdownThread = new Thread(() -> {
            try {
                service.shutdown();
            } catch (Throwable unexpected) {
                failure.compareAndSet(null, unexpected);
            } finally {
                shutdownFinished.countDown();
            }
        }, "graceful-shutdown-resource-drain");

        try {
            submitter.start();
            assertTrue(listener.closeStarted.await(5, TimeUnit.SECONDS));
            assertNotNull(listener.session);
            assertTrue(((ExecutionTrackedSession) listener.session).awaitExecutionCompletion(0),
                "Session worker exit is observed before physical resource close finishes");

            shutdownThread.start();
            assertTrue(executorTerminationObserved.await(5, TimeUnit.SECONDS),
                "Shutdown must observe executor termination before waiting for resource drain");
            assertEquals(shutdownFinished.getCount(), 1L);

            listener.releaseClose.countDown();
            submitter.join(5000);
            shutdownThread.join(5000);
            assertFalse(submitter.isAlive());
            assertFalse(shutdownThread.isAlive());
            assertNull(failure.get());
        } finally {
            listener.releaseClose.countDown();
            submitter.join(5000);
            if (shutdownThread.getState() != Thread.State.NEW) {
                shutdownThread.join(5000);
            }
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownNowCancellationSubscriberRunsOutsideAdmissionLock() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener worker = new BlockingStartedListener();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            java.util.concurrent.Executors.newSingleThreadExecutor(),
            new SessionRegistryImpl(), eventBus);
        AdmissionProbeCancelledListener probe = new AdmissionProbeCancelledListener(service);
        eventBus.register(worker);
        eventBus.register(probe);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(worker.started.await(5, TimeUnit.SECONDS));

            service.shutdownNow();

            assertTrue(probe.admissionRejected.await(5, TimeUnit.SECONDS),
                "A cancellation subscriber must be able to call back into service admission");
            assertNull(probe.failure.get());
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);

            worker.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
        } finally {
            worker.release.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownTimeoutIsSurfacedAndDoesNotClaimSuccess() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        FirstWaitTimesOutExecutorService executor = new FirstWaitTimesOutExecutorService(
            java.util.concurrent.Executors.newSingleThreadExecutor());
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));

            try {
                service.shutdown();
                fail("Shutdown must surface an executor that did not terminate");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("did not terminate"));
            }

            assertTrue(service.isShutdown());
            assertEquals(tracked.getExecutionState(), ExecutionState.UNRESPONSIVE);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.NOT_REQUESTED);
            assertEquals(session.getStatus(), SessionStatus.RUNNING);

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(session.getStatus(), SessionStatus.COMPLETED);
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            service.shutdown();
        } finally {
            listener.release.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testGraceEscalatesToKillUnresponsiveAndLaterConfirmsWorkerExit() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));
            assertTrue(session.cancel());
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS));

            assertTrue(tracked.awaitExecutionUnresponsive(7000),
                "The bounded GRACE period should escalate a non-cooperative worker");
            assertEquals(tracked.getExecutionState(), ExecutionState.UNRESPONSIVE);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.KILL_UNRESPONSIVE);
            assertFalse(tracked.awaitExecutionCompletion(0));

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.WORKER_EXIT_CONFIRMED);
        } finally {
            listener.release.countDown();
            service.shutdownNow();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownNowReportsUnresponsiveWorkerUntilItActuallyExits() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
        ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
        try {
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));
            service.shutdownNow();
            assertEquals(session.getStatus(), SessionStatus.CANCELLED);
            assertEquals(tracked.getExecutionState(), ExecutionState.UNRESPONSIVE);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.KILL_UNRESPONSIVE);
            assertFalse(tracked.awaitExecutionCompletion(0));

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.WORKER_EXIT_CONFIRMED);
        } finally {
            listener.release.countDown();
            service.shutdownNow();
        }
    }

    private static class BlockingCompletedListener {
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private volatile ScraperSession session;

        @Subscribe
        public void onCompleted(org.webharvest.runtime.session.events.SessionCompletedEvent event) {
            session = event.getSession();
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class FirstCreatedBlockingListener {
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Subscribe
        public void onCreated(SessionCreatedEvent event) {
            if (first.compareAndSet(true, false)) {
                entered.countDown();
                awaitRelease();
            }
        }

        private void awaitRelease() {
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class WorkerInitiatedShutdownListener {
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch allowShutdown = new CountDownLatch(1);
        private final CountDownLatch shutdownReturned = new CountDownLatch(1);
        private final AtomicReference<Throwable> shutdownFailure = new AtomicReference<>();
        private volatile WebHarvestServiceImpl service;

        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            if (!first.compareAndSet(true, false)) {
                return;
            }
            started.countDown();
            try {
                allowShutdown.await();
                service.shutdown();
            } catch (Throwable failure) {
                shutdownFailure.compareAndSet(null, failure);
            } finally {
                shutdownReturned.countDown();
            }
        }
    }

    private static class FirstStartedBlockingListener {
        private final AtomicBoolean first = new AtomicBoolean(true);
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            if (first.compareAndSet(true, false)) {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static class AdmissionProbeCancelledListener {
        private final WebHarvestServiceImpl service;
        private final CountDownLatch admissionRejected = new CountDownLatch(1);
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private AdmissionProbeCancelledListener(WebHarvestServiceImpl service) {
            this.service = service;
        }

        @Subscribe
        public void onCancelled(org.webharvest.runtime.session.events.SessionCancelledEvent event) {
            Thread probe = new Thread(() -> {
                try {
                    service.executeAsync(Mockito.mock(Config.class), null, null);
                    failure.set(new AssertionError("Admission unexpectedly succeeded after shutdown"));
                } catch (IllegalStateException expected) {
                    if (!expected.getMessage().contains("shut down")) {
                        failure.set(expected);
                    }
                } catch (Throwable unexpected) {
                    failure.set(unexpected);
                } finally {
                    admissionRejected.countDown();
                }
            }, "cancel-event-admission-probe");
            probe.start();
            try {
                if (!admissionRejected.await(5, TimeUnit.SECONDS)) {
                    failure.set(new AssertionError(
                        "Admission remained blocked while the cancellation subscriber was running"));
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failure.set(interrupted);
            }
        }
    }

    private static class BlockingCloseRegistrationListener {
        private final CountDownLatch closeStarted = new CountDownLatch(1);
        private final CountDownLatch releaseClose = new CountDownLatch(1);
        private volatile ScraperSession session;

        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            session = event.getSession();
            session.getContext().getResourceAccess().registerSessionResource(
                "test/blocking-close", null, () -> {
                    closeStarted.countDown();
                    releaseClose.await();
                });
        }
    }

    private static class BlockingResourceListener {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger abortCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger closeCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private volatile ExecutionResourceAccess.ResourceHandle resource;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/cancellable-session-resource", abortCount::incrementAndGet, closeCount::incrementAndGet);
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException expected) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class SessionResourceRegistrationListener {
        private final java.util.concurrent.atomic.AtomicInteger abortCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger closeCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private volatile ExecutionResourceAccess.ResourceHandle resource;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/session-resource", abortCount::incrementAndGet, closeCount::incrementAndGet);
        }
    }

    private static AtomicReference<Throwable> registerCloseCallbackMonitorProbe(
        WebHarvestServiceImpl service, ScraperSession session, CountDownLatch closeCompleted,
        boolean reenterShutdownNow) throws Exception {
        Field tasksField = WebHarvestServiceImpl.class.getDeclaredField("executionTasks");
        tasksField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> tasks = (Map<String, Object>) tasksField.get(service);
        Object task = tasks.get(session.getSessionId());
        assertNotNull(task, "The queued task must remain registered while its resource is registered");

        Field coordinatorField = task.getClass().getDeclaredField("resourceCoordinator");
        coordinatorField.setAccessible(true);
        org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator coordinator =
            (org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator) coordinatorField.get(task);
        AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
        ExecutionResourceAccess access = coordinator.getAccess();
        access.registerSessionResource("close-monitor-probe-" + session.getSessionId(), null, () -> {
            CountDownLatch monitorAcquired = new CountDownLatch(1);
            Thread probe = new Thread(() -> {
                synchronized (task) {
                    monitorAcquired.countDown();
                }
            }, "resource-close-task-monitor-probe");
            probe.start();
            try {
                if (!monitorAcquired.await(2, TimeUnit.SECONDS)) {
                    callbackFailure.compareAndSet(null, new AssertionError(
                        "SessionTask monitor remained held while resource close callback was running"));
                }
                probe.join(2000);
                if (probe.isAlive()) {
                    callbackFailure.compareAndSet(null, new AssertionError(
                        "SessionTask monitor probe did not terminate"));
                }

                if (reenterShutdownNow && callbackFailure.get() == null) {
                    Field pendingField = WebHarvestServiceImpl.class.getDeclaredField("pendingSubmissions");
                    pendingField.setAccessible(true);
                    int pendingSubmissions = pendingField.getInt(service);
                    java.lang.reflect.Method currentTaskMethod =
                        task.getClass().getDeclaredMethod("isCurrentTaskThread");
                    currentTaskMethod.setAccessible(true);
                    boolean currentTaskThread = (Boolean) currentTaskMethod.invoke(task);
                    if (pendingSubmissions != 0 || !currentTaskThread) {
                        callbackFailure.compareAndSet(null, new AssertionError(
                            "Reentrant shutdown must see a resolved submission and the current cleanup thread"));
                    } else {
                        service.shutdownNow();
                    }
                }
            } catch (Throwable failure) {
                callbackFailure.compareAndSet(null, failure);
            } finally {
                closeCompleted.countDown();
            }
        });
        return callbackFailure;
    }

    private static class CancelOnCreatedListener {
        @Subscribe
        public void onCreated(SessionCreatedEvent event) {
            event.getSession().cancel();
        }
    }

    private static class CapturingCreatedListener {
        private ScraperSession session;

        @Subscribe
        public void onCreated(SessionCreatedEvent event) {
            session = event.getSession();
        }
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
                    // Remain active until the test releases this operation.
                }
            }
        }
    }

    private static class CancelAndHoldStartedListener {
        final CountDownLatch cancelled = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        @Subscribe
        public void onStarted(SessionStartedEvent event) {
            event.getSession().cancel();
            cancelled.countDown();
            boolean waiting = true;
            while (waiting) {
                try {
                    release.await();
                    waiting = false;
                } catch (InterruptedException cancellation) {
                    // Remain active until the test releases this operation.
                }
            }
        }
    }

    private static class StartAndCancelExecutorService extends DelegatingExecutorService {
        private final CountDownLatch cancellationObserved;

        private StartAndCancelExecutorService(CountDownLatch cancellationObserved) {
            super(java.util.concurrent.Executors.newSingleThreadExecutor());
            this.cancellationObserved = cancellationObserved;
        }

        @Override
        public void execute(Runnable command) {
            super.execute(command);
            try {
                if (!cancellationObserved.await(5, TimeUnit.SECONDS)) {
                    throw new RejectedExecutionException("Worker did not reach cancellation callback");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Submission wait interrupted", interrupted);
            }
        }
    }

    private static class QueueThenBlockThreadPoolExecutor
        extends java.util.concurrent.ThreadPoolExecutor {
        private final CountDownLatch queued = new CountDownLatch(1);
        private final CountDownLatch releaseSubmission = new CountDownLatch(1);
        private final AtomicBoolean blockNextSubmission = new AtomicBoolean();

        private QueueThenBlockThreadPoolExecutor() {
            super(1, 1, 0, TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<>());
        }

        private void blockNextSubmission() {
            blockNextSubmission.set(true);
        }

        @Override
        public void execute(Runnable command) {
            super.execute(command);
            if (blockNextSubmission.compareAndSet(true, false)) {
                queued.countDown();
                try {
                    releaseSubmission.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Submission gate interrupted", interrupted);
                }
            }
        }
    }

    private static class DelegatingExecutorService extends AbstractExecutorService {
        private final ExecutorService delegate;

        private DelegatingExecutorService(ExecutorService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(command);
        }
    }

    private static class BlockingSubmitExecutorService extends DelegatingExecutorService {
        private final CountDownLatch entered;
        private final CountDownLatch release;

        private BlockingSubmitExecutorService(ExecutorService delegate,
                                              CountDownLatch entered,
                                              CountDownLatch release) {
            super(delegate);
            this.entered = entered;
            this.release = release;
        }

        @Override
        public void execute(Runnable command) {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Submission gate interrupted", interrupted);
            }
            super.execute(command);
        }
    }

    private static class FirstWaitTimesOutExecutorService extends DelegatingExecutorService {
        private boolean firstWait = true;

        private FirstWaitTimesOutExecutorService(ExecutorService delegate) {
            super(delegate);
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            if (firstWait) {
                firstWait = false;
                return false;
            }
            return super.awaitTermination(timeout, unit);
        }
    }

    private static class SignallingInlineExecutorService extends InlineExecutorService {
        private final CountDownLatch terminationObserved;

        private SignallingInlineExecutorService(CountDownLatch terminationObserved) {
            this.terminationObserved = terminationObserved;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            boolean terminated = super.awaitTermination(timeout, unit);
            terminationObserved.countDown();
            return terminated;
        }
    }

    private static class InlineExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return shutdown;
        }

        @Override
        public void execute(Runnable command) {
            if (shutdown) {
                throw new RejectedExecutionException("Executor is shut down");
            }
            command.run();
        }
    }

    private static class RejectingExecutorService extends InlineExecutorService {
        @Override
        public void execute(Runnable command) {
            throw new RejectedExecutionException("Test executor rejects all tasks");
        }
    }
}
