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
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webharvest.definition.Config;
import org.webharvest.runtime.DynamicScopeContext;
import org.webharvest.runtime.Scraper;
import org.webharvest.runtime.ScraperContext;
import org.webharvest.runtime.client.ClientContext;
import org.webharvest.runtime.session.ExecutionOwner;
import org.webharvest.runtime.resource.ExecutionResourceAccess.ResourceState;
import org.webharvest.runtime.resource.LedgerExecutionResourceCoordinator;
import org.webharvest.runtime.resource.ResourceLedger;
import org.webharvest.runtime.session.ScraperSession;
import org.webharvest.runtime.session.ScraperSessionImpl;
import org.webharvest.runtime.session.SessionRegistry;
import org.webharvest.runtime.session.SessionRegistryImpl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of {@link WebHarvestService}.
 *
 * <p>Every accepted session owns one retained task handle. Business session
 * completion is separate from observed worker exit. Cancellation interrupts
 * the retained worker where possible; it never treats Future cancellation
 * itself as proof of quiescence.</p>
 */
public class WebHarvestServiceImpl implements WebHarvestService {

    private static final Logger LOG = LoggerFactory.getLogger(WebHarvestServiceImpl.class);
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 60;
    private static final long CANCELLATION_GRACE_SECONDS = 5;
    private static final Duration RESOURCE_DRAIN_BUDGET = Duration.ofSeconds(5);

    private final ExecutorService executorService;
    private final SessionRegistry sessionRegistry;
    private final EventBus eventBus;
    private final ScheduledThreadPoolExecutor timeoutScheduler;
    private final ConcurrentMap<String, SessionTask> executionTasks = new ConcurrentHashMap<>();
    private final String runtimeId = UUID.randomUUID().toString();
    private final ResourceLedger resourceLedger = new ResourceLedger(runtimeId);
    private final AtomicLong executionGeneration = new AtomicLong();
    private final Object admissionLock = new Object();
    private int pendingSubmissions;
    private volatile boolean shutdown;

    /**
     * Creates a service using the EventBus owned by this Guice runtime.
     *
     * @param eventBus shared runtime EventBus
     */
    @Inject
    public WebHarvestServiceImpl(EventBus eventBus) {
        this(Executors.newCachedThreadPool(new SessionThreadFactory()),
            new SessionRegistryImpl(), eventBus);
    }

    /** Creates a service using the default cached session executor. */
    public WebHarvestServiceImpl() {
        this(Executors.newCachedThreadPool(new SessionThreadFactory()));
    }

    /**
     * Creates a service with a caller-supplied session executor.
     * The service owns its shutdown lifecycle after construction.
     *
     * @param executorService executor used for session workers
     */
    public WebHarvestServiceImpl(ExecutorService executorService) {
        this(executorService, new SessionRegistryImpl(), new EventBus());
    }

    /**
     * Creates a service with explicit runtime-owned session components.
     *
     * @param executorService executor used for session workers
     * @param sessionRegistry registry owned by this service runtime
     * @param eventBus shared runtime EventBus
     */
    public WebHarvestServiceImpl(ExecutorService executorService,
                                 SessionRegistry sessionRegistry,
                                 EventBus eventBus) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        if (sessionRegistry == null) {
            throw new IllegalArgumentException("SessionRegistry cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }

        this.executorService = executorService;
        this.sessionRegistry = sessionRegistry;
        this.eventBus = eventBus;
        this.timeoutScheduler = createTimeoutScheduler();
    }

    @Override
    public ScraperSession executeAsync(Config config, ClientContext client, SessionOptions options) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        SessionOptions opts = options != null ? options : SessionOptions.DEFAULT;
        String clientId = client != null ? client.getClientId() : null;

        String sessionId = UUID.randomUUID().toString();
        ExecutionOwner owner = new ExecutionOwner(runtimeId, sessionId, executionGeneration.incrementAndGet());

        // Session construction publishes SessionCreatedEvent synchronously.
        // Keep subscriber callbacks outside admissionLock.
        ScraperSessionImpl session = new ScraperSessionImpl(sessionId, clientId, config, eventBus, owner);
        LedgerExecutionResourceCoordinator resourceCoordinator =
            new LedgerExecutionResourceCoordinator(resourceLedger, owner);
        SessionTask task = new SessionTask(session, opts, resourceCoordinator);
        session.attachCancellationHandler(task::requestCancellation);

        boolean registered = false;
        RuntimeException admissionFailure = null;
        synchronized (admissionLock) {
            if (shutdown) {
                admissionFailure = new IllegalStateException("Service is shut down");
            } else {
                try {
                    session.markExecutionQueued();
                    sessionRegistry.register(session);
                    registered = true;
                    executionTasks.put(sessionId, task);
                    pendingSubmissions++;
                } catch (RuntimeException failure) {
                    admissionFailure = failure;
                }
            }
        }

        if (admissionFailure != null) {
            task.rejectBeforeStart();
            session.markFailed(admissionFailure);
            throw admissionFailure;
        }

        try {
            // Resolve the in-flight submission reservation when the executor
            // actually enters the wrapper (including direct/CallerRuns executors),
            // or when submit returns for a queued task. This lets shutdown wait
            // for dispatch without holding admissionLock across user execution.
            Future<?> future = executorService.submit(() -> {
                task.resolveSubmission();
                task.run();
            });
            task.attachFuture(future);
            LOG.info("Accepted session {} for client {}", sessionId, clientId);
            return session;
        } catch (RuntimeException submissionFailure) {
            executionTasks.remove(sessionId, task);
            task.rejectBeforeStart();
            if (registered) {
                sessionRegistry.unregisterIfOwner(session);
            }
            session.markFailed(submissionFailure);
            throw new IllegalStateException("Session submission was rejected", submissionFailure);
        } finally {
            task.resolveSubmission();
        }
    }

    private void executeSession(SessionTask task) {
        ScraperSessionImpl session = task.session;
        ScheduledFuture<?> timeoutFuture = null;
        try {
            if (task.isCancellationRequested()) {
                return;
            }

            DynamicScopeContext context = new ScraperContext();
            context.setConfig(session.getConfig());
            context.setEventBus(eventBus);
            context.setSession(session);
            context.setExecutionOwner(session.getExecutionOwner());
            context.setResourceAccess(task.resourceCoordinator.getAccess());

            if (task.options.hasTimeout()) {
                timeoutFuture = timeoutScheduler.schedule(
                    task::requestTimeout,
                    task.options.getTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
                );
            }

            session.markRunning(context);
            if (task.isCancellationRequested() || task.isTimedOut()
                || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Session cancellation or deadline reached before scraper entry");
            }

            Scraper scraper = new Scraper(eventBus);
            scraper.execute(context);

            if (task.isTimedOut()) {
                throw new TimeoutException("Session execution exceeded timeout: " + task.options.getTimeout());
            }

            if (!task.completeSuccessfully()) {
                if (task.isTimedOut()) {
                    throw new TimeoutException("Session execution exceeded timeout: " + task.options.getTimeout());
                }
                return;
            }
            LOG.info("Session {} completed successfully in {}",
                session.getSessionId(), session.getMetrics().getDuration());
        } catch (InterruptedException interrupted) {
            if (task.isTimedOut()) {
                session.markFailed(new TimeoutException(
                    "Session execution exceeded timeout: " + task.options.getTimeout()));
            } else {
                session.markFailed(interrupted);
            }
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            if (task.isTimedOut() && !(failure instanceof TimeoutException)) {
                session.markFailed(new TimeoutException(
                    "Session execution exceeded timeout: " + task.options.getTimeout()));
            } else {
                session.markFailed(failure);
            }
            LOG.error("Session {} failed", session.getSessionId(), failure);
        } finally {
            if (timeoutFuture != null) {
                timeoutFuture.cancel(false);
            }
        }
    }

    @Override
    public Optional<ScraperSession> getSession(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        return sessionRegistry.getSession(sessionId);
    }

    @Override
    public List<ScraperSession> listSessions(String clientId) {
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        return sessionRegistry.getSessionsByClientId(clientId);
    }

    @Override
    public List<ScraperSession> listAllSessions() {
        return sessionRegistry.getAllSessions();
    }

    @Override
    public boolean cancelSession(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID cannot be null");
        }
        Optional<ScraperSession> session = sessionRegistry.getSession(sessionId);
        return session.map(ScraperSession::cancel).orElse(false);
    }

    @Override
    public void shutdown() throws InterruptedException {
        boolean interrupted = false;
        List<SessionTask> tasks;
        synchronized (admissionLock) {
            shutdown = true;
            while (pendingSubmissions > 0) {
                try {
                    admissionLock.wait();
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
            tasks = new ArrayList<>(executionTasks.values());
        }
        executorService.shutdown();
        if (interrupted) {
            Thread.currentThread().interrupt();
            throw new InterruptedException("Interrupted while waiting for accepted sessions to reach the executor");
        }

        boolean terminated = executorService.awaitTermination(
            SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        boolean tasksExited = terminated
            && awaitTaskExits(tasks, SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS, true);
        if (!terminated || !tasksExited) {
            markTasksUnresponsive(tasks, false, true);
            throw new IllegalStateException(
                "WebHarvestService did not observe all accepted workers exit within "
                    + SHUTDOWN_TIMEOUT_SECONDS + " seconds");
        }
        timeoutScheduler.shutdownNow();
        LOG.info("WebHarvestService shut down after all accepted tasks exited");
    }

    @Override
    public void shutdownNow() {
        boolean interrupted = false;
        List<SessionTask> tasks;
        synchronized (admissionLock) {
            shutdown = true;
            while (pendingSubmissions > 0) {
                try {
                    admissionLock.wait();
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
            tasks = new ArrayList<>(executionTasks.values());
        }

        // Cancellation invokes resource abort actions and synchronous EventBus
        // subscribers. Never run those callbacks while holding admissionLock
        // or while carrying an interrupt flag from the admission wait.
        for (SessionTask task : tasks) {
            task.session.cancel();
            task.requestKill();
        }
        List<Runnable> neverStarted = executorService.shutdownNow();
        for (Runnable pending : neverStarted) {
            for (SessionTask task : tasks) {
                task.markRemovedBeforeStart(pending);
            }
        }

        boolean terminated;
        try {
            terminated = executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException interruption) {
            interrupted = true;
            terminated = false;
        }
        boolean tasksExited = false;
        if (terminated) {
            try {
                tasksExited = awaitTaskExits(tasks, 1, TimeUnit.SECONDS, true);
            } catch (InterruptedException interruption) {
                interrupted = true;
            }
        }
        if (!terminated || !tasksExited) {
            markTasksUnresponsive(tasks, true, true);
        }
        timeoutScheduler.shutdownNow();
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean awaitTaskExits(List<SessionTask> tasks, long timeout,
                                   TimeUnit unit, boolean skipCurrentWorker) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        for (SessionTask task : tasks) {
            if (skipCurrentWorker && task.isCurrentWorker()) {
                continue;
            }
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) {
                return false;
            }
            long remainingMillis = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
            if (!task.session.awaitExecutionCompletion(remainingMillis)) {
                return false;
            }
        }
        return true;
    }

    private void markTasksUnresponsive(List<SessionTask> tasks,
                                       boolean killRequested, boolean skipCurrentWorker) {
        for (SessionTask task : tasks) {
            if (skipCurrentWorker && task.isCurrentWorker()) {
                continue;
            }
            if (killRequested) {
                task.session.markKillRequested();
            }
            task.session.markExecutionUnresponsive();
            if (killRequested) {
                task.session.markKillUnresponsive();
            }
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    private static ScheduledThreadPoolExecutor createTimeoutScheduler() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1, new TimeoutThreadFactory());
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return scheduler;
    }

    private final class SessionTask implements Runnable {
        private final ScraperSessionImpl session;
        private final SessionOptions options;
        private final LedgerExecutionResourceCoordinator resourceCoordinator;
        private final AtomicBoolean submissionResolved = new AtomicBoolean();
        private Future<?> future;
        private ScheduledFuture<?> escalationFuture;
        private Thread worker;
        private boolean started;
        private boolean cancellationRequested;
        private boolean timedOut;
        private boolean completionCommitted;
        private boolean exited;

        private SessionTask(ScraperSessionImpl session, SessionOptions options,
                            LedgerExecutionResourceCoordinator resourceCoordinator) {
            this.session = session;
            this.options = options;
            this.resourceCoordinator = resourceCoordinator;
        }

        @Override
        public void run() {
            boolean skipExecution;
            synchronized (this) {
                if (cancellationRequested || exited) {
                    exited = true;
                    cancelEscalation();
                    skipExecution = true;
                } else {
                    started = true;
                    worker = Thread.currentThread();
                    if (!session.markExecutionStarted()) {
                        worker = null;
                        exited = true;
                        cancelEscalation();
                        skipExecution = true;
                    } else {
                        skipExecution = false;
                    }
                }
            }

            if (skipExecution) {
                executionTasks.remove(session.getSessionId(), this);
                session.markExecutionExited();
                drainResources(Duration.ZERO);
                return;
            }

            try {
                executeSession(this);
            } finally {
                synchronized (this) {
                    worker = null;
                    exited = true;
                    cancelEscalation();
                }
                executionTasks.remove(session.getSessionId(), this);
                // Publish quiescence only after task bookkeeping and lock release.
                session.markExecutionExited();
                drainResources(RESOURCE_DRAIN_BUDGET);
            }
        }

        private synchronized void attachFuture(Future<?> submittedFuture) {
            future = submittedFuture;
            if (!cancellationRequested || exited) {
                return;
            }
            if (!started) {
                removeQueuedFuture();
            } else {
                future.cancel(true);
            }
        }

        private void resolveSubmission() {
            if (submissionResolved.compareAndSet(false, true)) {
                synchronized (admissionLock) {
                    pendingSubmissions--;
                    admissionLock.notifyAll();
                }
            }
        }

        private synchronized boolean isCurrentWorker() {
            return !exited && worker == Thread.currentThread();
        }

        private void requestCancellation() {
            boolean startedExecution;
            synchronized (this) {
                if (exited) {
                    return;
                }
                cancellationRequested = true;
                session.markGracefulTerminationRequested();
                if (!started && removeQueuedFuture()) {
                    return;
                }
                startedExecution = started;
            }

            // Close admission and abort I/O before interrupting the worker, so
            // a fast worker exit cannot race ahead of resource abort dispatch.
            requestResourceTermination();
            if (startedExecution) {
                synchronized (this) {
                    if (!exited) {
                        session.markExecutionInterruptRequested();
                        if (worker != null) {
                            worker.interrupt();
                        }
                        if (future != null) {
                            future.cancel(true);
                        }
                        scheduleUnresponsiveEscalation();
                    }
                }
            }
        }

        /**
         * Removes a queued FutureTask only when the executor confirms it is
         * still in a ThreadPoolExecutor queue. A successful remove proves it
         * cannot enter the worker body; generic executors must run the
         * cancellation gate and report exit from the task itself.
         */
        private boolean removeQueuedFuture() {
            if (future instanceof Runnable && executorService instanceof ThreadPoolExecutor) {
                boolean removed = ((ThreadPoolExecutor) executorService).remove((Runnable) future);
                if (removed) {
                    future.cancel(false);
                    finishBeforeStart();
                    return true;
                }
            }
            return false;
        }

        private synchronized void markRemovedBeforeStart(Runnable pending) {
            // shutdownNow() returns only tasks that have not commenced execution.
            if (future == pending) {
                future.cancel(false);
                finishBeforeStart();
            }
        }

        private void requestKill() {
            boolean startedExecution;
            synchronized (this) {
                if (exited) {
                    return;
                }
                session.markKillRequested();
                if (!started && removeQueuedFuture()) {
                    return;
                }
                startedExecution = started;
            }

            requestResourceTermination();
            if (startedExecution) {
                synchronized (this) {
                    if (!exited) {
                        session.markExecutionInterruptRequested();
                        if (worker != null) {
                            worker.interrupt();
                        }
                        if (future != null) {
                            future.cancel(true);
                        }
                        scheduleUnresponsiveEscalation();
                    }
                }
            }
        }

        private void requestTimeout() {
            synchronized (this) {
                if (exited || !started || completionCommitted) {
                    return;
                }
                timedOut = true;
                session.markGracefulTerminationRequested();
            }

            requestResourceTermination();
            synchronized (this) {
                if (!exited && started && !completionCommitted) {
                    session.markExecutionInterruptRequested();
                    if (worker != null) {
                        worker.interrupt();
                    }
                    scheduleUnresponsiveEscalation();
                }
            }
        }

        private void requestResourceTermination() {
            try {
                resourceCoordinator.requestTermination();
            } catch (RuntimeException | Error failure) {
                // The ledger retains abort failures per resource; cancellation must still proceed.
                LOG.warn("Resource abort request failed for session {}", session.getSessionId(), failure);
            }
        }

        private synchronized boolean isCancellationRequested() {
            return cancellationRequested;
        }

        private synchronized boolean isTimedOut() {
            return timedOut;
        }

        /**
         * Commits successful business completion while holding the same gate
         * used by the timeout callback. A deadline cannot interrupt a worker
         * after success has been committed.
         */
        private synchronized boolean completeSuccessfully() {
            if (timedOut || cancellationRequested || exited || session.getStatus().isTerminal()) {
                return false;
            }
            try {
                session.markCompleted();
                completionCommitted = true;
                return true;
            } catch (IllegalStateException concurrentTermination) {
                if (session.getStatus().isTerminal()) {
                    return false;
                }
                throw concurrentTermination;
            }
        }

        private synchronized void scheduleUnresponsiveEscalation() {
            if (escalationFuture == null && !exited) {
                try {
                    escalationFuture = timeoutScheduler.schedule(() -> {
                        synchronized (SessionTask.this) {
                            if (!exited) {
                                session.markKillRequested();
                                session.markExecutionUnresponsive();
                                session.markKillUnresponsive();
                                if (worker != null) {
                                    worker.interrupt();
                                }
                            }
                        }
                    }, CANCELLATION_GRACE_SECONDS, TimeUnit.SECONDS);
                } catch (RejectedExecutionException ignored) {
                    // Service shutdown will perform the explicit unresponsive check.
                }
            }
        }

        private synchronized void cancelEscalation() {
            if (escalationFuture != null) {
                escalationFuture.cancel(false);
                escalationFuture = null;
            }
        }

        private synchronized void rejectBeforeStart() {
            cancellationRequested = true;
            if (!started && !exited) {
                finishBeforeStart();
            } else if (!exited && worker != null) {
                worker.interrupt();
            }
        }

        private void finishBeforeStart() {
            exited = true;
            cancelEscalation();
            executionTasks.remove(session.getSessionId(), this);
            // The executor has confirmed that this task will never be entered.
            session.markExecutionExited();
            drainResources(Duration.ZERO);
        }

        private void drainResources(Duration budget) {
            try {
                for (org.webharvest.runtime.resource.LedgerExecutionResourceContext.DrainReport report
                    : resourceCoordinator.markOwnerQuiescentAndDrain(budget)) {
                    if (report.getState() != ResourceState.CLOSED) {
                        LOG.warn("Resource {} for session {} remains {} (active leases: {})",
                            report.getResourceId(), session.getSessionId(),
                            report.getState(), report.getActiveLeases());
                        if (report.getAbortFailure() != null) {
                            LOG.warn("Resource abort failed for {}", report.getResourceId(), report.getAbortFailure());
                        }
                        if (report.getCloseFailure() != null) {
                            LOG.warn("Resource close failed for {}", report.getResourceId(), report.getCloseFailure());
                        }
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while draining resources for session {}", session.getSessionId(), interrupted);
            }
        }
    }

    private static class SessionThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("webharvest-session-" + threadNumber.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }

    private static class TimeoutThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "webharvest-session-timeouts");
            thread.setDaemon(true);
            return thread;
        }
    }
}
