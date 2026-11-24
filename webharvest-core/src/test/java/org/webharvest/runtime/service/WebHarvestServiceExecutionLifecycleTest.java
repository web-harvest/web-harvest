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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
    public void testShutdownIsIdempotent() throws InterruptedException {
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            new InlineExecutorService(), new SessionRegistryImpl(), new EventBus());
        service.shutdown();
        service.shutdown();
        assertTrue(service.isShutdown());
    }

    @Test(groups = {"unit", "service"})
    public void testSessionsShareRuntimeIdentityAndHaveDistinctGenerations() throws InterruptedException {
        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), new EventBus());

        try {
            ScraperSession first = service.executeAsync(Mockito.mock(Config.class), null, null);
            ScraperSession second = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession firstTracked = (ExecutionTrackedSession) first;
            ExecutionTrackedSession secondTracked = (ExecutionTrackedSession) second;

            assertTrue(firstTracked.awaitExecutionCompletion(5000));
            assertTrue(secondTracked.awaitExecutionCompletion(5000));
            assertEquals(firstTracked.getExecutionOwner().getRuntimeId(),
                secondTracked.getExecutionOwner().getRuntimeId());
            assertEquals(firstTracked.getExecutionOwner().getSessionId(), first.getSessionId());
            assertEquals(secondTracked.getExecutionOwner().getSessionId(), second.getSessionId());
            assertTrue(firstTracked.getExecutionOwner().getExecutionGeneration()
                != secondTracked.getExecutionOwner().getExecutionGeneration());
        } finally {
            service.shutdown();
        }
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
            assertEquals(tracked.getTerminationDisposition(), TerminationDisposition.GRACEFUL,
                "Observed exit after cooperative cancellation is a graceful outcome, not a KILL result");
        } finally {
            listener.release.countDown();
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testSubmitAndShutdownAreSerializedWithoutOrphanTask() throws InterruptedException {
        CountDownLatch submitEntered = new CountDownLatch(1);
        CountDownLatch allowSubmit = new CountDownLatch(1);
        BlockingSubmitExecutorService executor = new BlockingSubmitExecutorService(
            java.util.concurrent.Executors.newSingleThreadExecutor(), submitEntered, allowSubmit);
        SessionRegistryImpl registry = new SessionRegistryImpl();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(executor, registry, new EventBus());
        java.util.concurrent.atomic.AtomicReference<ScraperSession> accepted =
            new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> submitFailure =
            new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> shutdownFailure =
            new java.util.concurrent.atomic.AtomicReference<>();
        CountDownLatch submitFinished = new CountDownLatch(1);
        CountDownLatch shutdownAttempted = new CountDownLatch(1);
        CountDownLatch shutdownFinished = new CountDownLatch(1);

        Thread submitter = new Thread(() -> {
            try {
                accepted.set(service.executeAsync(Mockito.mock(Config.class), null, null));
            } catch (Throwable failure) {
                submitFailure.set(failure);
            } finally {
                submitFinished.countDown();
            }
        }, "test-session-submitter");
        Thread shutdown = new Thread(() -> {
            shutdownAttempted.countDown();
            try {
                service.shutdown();
            } catch (Throwable failure) {
                shutdownFailure.set(failure);
            } finally {
                shutdownFinished.countDown();
            }
        }, "test-service-shutdown");

        try {
            submitter.start();
            assertTrue(submitEntered.await(5, TimeUnit.SECONDS),
                "Submission must reach the executor while admission is held");
            shutdown.start();
            assertTrue(shutdownAttempted.await(5, TimeUnit.SECONDS));
            allowSubmit.countDown();

            assertTrue(submitFinished.await(5, TimeUnit.SECONDS));
            assertTrue(shutdownFinished.await(5, TimeUnit.SECONDS));
            assertNull(submitFailure.get());
            assertNull(shutdownFailure.get());
            assertNotNull(accepted.get());

            ExecutionTrackedSession tracked = (ExecutionTrackedSession) accepted.get();
            assertTrue(tracked.awaitExecutionCompletion(0));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(accepted.get().getStatus(), SessionStatus.COMPLETED);
            assertSame(registry.getSession(accepted.get().getSessionId()).orElse(null), accepted.get());
        } finally {
            allowSubmit.countDown();
            submitter.join(5000);
            shutdown.join(5000);
            service.shutdownNow();
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
    public void testUnrequestedWorkerInterruptDoesNotBecomeSuccessfulCompletion() throws InterruptedException {
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getConfigurationTree()).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return null;
        });
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), new EventBus());

        try {
            ScraperSession session = service.executeAsync(config, null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.FAILED);
            assertTrue(session.getError() instanceof InterruptedException);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testFatalWorkerFailureMarksBusinessSessionFailedBeforeExit() throws InterruptedException {
        Config config = Mockito.mock(Config.class);
        AssertionError fatal = new AssertionError("fatal plugin failure");
        Mockito.when(config.getConfigurationTree()).thenThrow(fatal);
        ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), new EventBus());

        try {
            ScraperSession session = service.executeAsync(config, null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.FAILED);
            assertSame(session.getError(), fatal);
        } finally {
            service.shutdown();
        }
    }

    @Test(groups = {"unit", "service"})
    public void testShutdownTimeoutIsSurfacedAndLateWorkerExitRemainsObservable() throws InterruptedException {
        EventBus eventBus = new EventBus();
        BlockingStartedListener listener = new BlockingStartedListener();
        eventBus.register(listener);
        NonTerminatingExecutorService executor = new NonTerminatingExecutorService();
        WebHarvestServiceImpl service = new WebHarvestServiceImpl(
            executor, new SessionRegistryImpl(), eventBus);

        try {
            ScraperSession session = service.executeAsync(Mockito.mock(Config.class), null, null);
            ExecutionTrackedSession tracked = (ExecutionTrackedSession) session;
            assertTrue(listener.started.await(5, TimeUnit.SECONDS));

            try {
                service.shutdown();
                fail("Shutdown must surface an executor that has not terminated");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("did not terminate"));
            }

            assertEquals(tracked.getExecutionState(), ExecutionState.UNRESPONSIVE);
            assertFalse(tracked.awaitExecutionCompletion(0),
                "A failed shutdown wait must not claim worker quiescence");

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.COMPLETED);
            assertTrue(executor.awaitWorkerTermination(5, TimeUnit.SECONDS));
            service.shutdown();
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
        } finally {
            listener.release.countDown();
            service.shutdownNow();
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

    private static class NonTerminatingExecutorService extends AbstractExecutorService {
        private final CountDownLatch terminated = new CountDownLatch(1);
        private volatile boolean shutdown;
        private volatile Thread worker;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            Thread currentWorker = worker;
            if (currentWorker != null) {
                currentWorker.interrupt();
            }
            return Collections.emptyList();
        }

        boolean awaitWorkerTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return terminated.await(timeout, unit);
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return terminated.getCount() == 0;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return isTerminated();
        }

        @Override
        public void execute(Runnable command) {
            worker = new Thread(() -> {
                try {
                    command.run();
                } finally {
                    terminated.countDown();
                }
            }, "test-non-terminating-worker");
            worker.start();
        }
    }

    private static class BlockingSubmitExecutorService extends DelegatingExecutorService {
        private final CountDownLatch submitEntered;
        private final CountDownLatch allowSubmit;

        private BlockingSubmitExecutorService(ExecutorService delegate,
                                              CountDownLatch submitEntered,
                                              CountDownLatch allowSubmit) {
            super(delegate);
            this.submitEntered = submitEntered;
            this.allowSubmit = allowSubmit;
        }

        @Override
        public void execute(Runnable command) {
            submitEntered.countDown();
            try {
                if (!allowSubmit.await(5, TimeUnit.SECONDS)) {
                    throw new RejectedExecutionException("Test submission gate timed out");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new RejectedExecutionException("Test submission was interrupted", interrupted);
            }
            super.execute(command);
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
