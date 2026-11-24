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
            assertTrue(tracked.awaitExecutionCompletion(0));
        } finally {
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
            assertFalse(tracked.awaitExecutionCompletion(50),
                "The worker is still executing its non-cooperative callback");

            listener.release.countDown();
            assertTrue(tracked.awaitExecutionCompletion(5000));
            assertEquals(tracked.getExecutionState(), ExecutionState.EXITED);
            assertEquals(session.getStatus(), SessionStatus.FAILED);
            assertTrue(session.getError() instanceof TimeoutException);
        } finally {
            listener.release.countDown();
            service.shutdown();
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
