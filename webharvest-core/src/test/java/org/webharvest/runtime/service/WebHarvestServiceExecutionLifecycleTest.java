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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED,
                "Cancellation must quarantine resources until the owner exits");
            assertEquals(listener.abortCount.get(), 1,
                "Resource abort must be requested before the worker is interrupted");
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS));
            assertFalse(tracked.awaitExecutionCompletion(0),
                "The non-cooperative callback must remain active after interruption");
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED,
                "A resource must remain quarantined while its execution owner is still active");

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertTrue(listener.closed.await(5, TimeUnit.SECONDS),
                "Resource cleanup follows the observed execution-exit signal");
            assertEquals(listener.closeCount.get(), 1);
            // The close callback signals before drainAndClose publishes CLOSED.
            // Wait for the ledger's post-callback state transition rather than
            // racing that final publication.
            long closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (listener.resource.getState() != ExecutionResourceAccess.ResourceState.CLOSED
                && System.nanoTime() < closeDeadline) {
                Thread.yield();
            }
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        } finally {
            listener.release.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testFatalResourceDrainFailureRemainsObservableThroughSubmittedFuture() throws InterruptedException {
        EventBus eventBus = new EventBus();
        FatalCloseResourceListener listener = new FatalCloseResourceListener();
        eventBus.register(listener);
        CapturingInlineExecutorService executor = new CapturingInlineExecutorService();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;

            assertTrue(tracked.awaitExecutionCompletion(0),
                "Worker exit is published before resource draining");
            assertNotNull(listener.resource, "SessionStarted must register the test resource");
            assertNotNull(executor.submittedFuture, "The executor must retain the submitted Future");

            try {
                executor.submittedFuture.get();
                fail("A fatal resource-close failure must remain attached to the submitted Future");
            } catch (java.util.concurrent.ExecutionException expected) {
                assertSame(expected.getCause(), listener.fatalFailure,
                    "The original fatal cleanup failure must be propagated unchanged");
            }

            assertEquals(session.getStatus(), SessionStatus.COMPLETED,
                "A post-execution cleanup failure must not rewrite the business outcome");
            assertEquals(listener.resource.getCloseFailure(), listener.fatalFailure);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testRuntimeCloseFailureIsRetainedInDrainReport() throws InterruptedException,
        java.util.concurrent.ExecutionException {
        EventBus eventBus = new EventBus();
        RuntimeCloseFailureListener listener = new RuntimeCloseFailureListener();
        eventBus.register(listener);
        CapturingInlineExecutorService executor = new CapturingInlineExecutorService();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            assertNotNull(executor.submittedFuture);
            assertNull(executor.submittedFuture.get(),
                "A non-fatal close failure is recorded in the drain report, not thrown from the task");
            assertEquals(session.getStatus(), SessionStatus.COMPLETED);
            assertNotNull(listener.resource);
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.CLOSE_FAILED);
            assertSame(listener.resource.getCloseFailure(), listener.closeFailure);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testAbortFailureIsRetainedEvenWhenResourceCloseSucceeds() throws InterruptedException {
        EventBus eventBus = new EventBus();
        AbortFailureResourceListener listener = new AbortFailureResourceListener();
        eventBus.register(listener);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            java.util.concurrent.Executors.newSingleThreadExecutor(), new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));
            assertTrue(session.cancel());
            assertTrue(listener.interrupted.await(5, TimeUnit.SECONDS));
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertNotNull(listener.resource);
            long closeDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (listener.resource.getState() != ExecutionResourceAccess.ResourceState.CLOSED
                && System.nanoTime() < closeDeadline) {
                Thread.yield();
            }
            assertEquals(listener.resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
            assertEquals(listener.closeCount.get(), 1);
            assertTrue(listener.resource.getAbortFailure() instanceof IllegalStateException,
                "Abort failure must remain observable even when the later close succeeds");
        } finally {
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

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (shutdownThread.getState() != Thread.State.BLOCKED
                && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(shutdownThread.getState(), Thread.State.BLOCKED,
                "Shutdown must wait for the in-flight admission critical section");

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

    private static class BlockingResourceListener {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger abortCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private final java.util.concurrent.atomic.AtomicInteger closeCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private volatile ExecutionResourceAccess.ResourceHandle resource;
        private volatile ExecutionResourceAccess.Lease lease;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/cancellable-session-resource", abortCount::incrementAndGet, () -> {
                    closeCount.incrementAndGet();
                    closed.countDown();
                });
            // Model active I/O: cancellation must abort it, but the lease stays held
            // until the simulated operation explicitly exits.
            lease = resource.acquire();
            started.countDown();
            boolean waiting = true;
            while (waiting) {
                try {
                    release.await();
                    waiting = false;
                } catch (InterruptedException expected) {
                    interrupted.countDown();
                    // Keep the simulated I/O operation active until explicitly released.
                }
            }
            lease.close();
        }
    }

    private static class RuntimeCloseFailureListener {
        private final IllegalStateException closeFailure =
            new IllegalStateException("recoverable resource close failure");
        private volatile ExecutionResourceAccess.ResourceHandle resource;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/runtime-close-failure", null, () -> {
                    throw closeFailure;
                });
        }
    }

    private static class AbortFailureResourceListener {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);
        private final java.util.concurrent.atomic.AtomicInteger closeCount =
            new java.util.concurrent.atomic.AtomicInteger();
        private volatile ExecutionResourceAccess.ResourceHandle resource;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/abort-failure",
                () -> { throw new IllegalStateException("resource abort failure"); },
                closeCount::incrementAndGet);
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                interrupted.countDown();
            }
        }
    }

    private static class FatalCloseResourceListener {
        private final AssertionError fatalFailure =
            new AssertionError("fatal resource close failure");
        private volatile ExecutionResourceAccess.ResourceHandle resource;

        @Subscribe
        public void onSessionStarted(SessionStartedEvent event) {
            resource = event.getSession().getContext().getResourceAccess().registerSessionResource(
                "test/fatal-close-resource", null, () -> {
                    throw fatalFailure;
                });
        }
    }

    private static class CapturingInlineExecutorService extends InlineExecutorService {
        private volatile java.util.concurrent.Future<?> submittedFuture;

        @Override
        public void execute(Runnable command) {
            if (command instanceof java.util.concurrent.Future<?>) {
                submittedFuture = (java.util.concurrent.Future<?>) command;
            }
            super.execute(command);
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
