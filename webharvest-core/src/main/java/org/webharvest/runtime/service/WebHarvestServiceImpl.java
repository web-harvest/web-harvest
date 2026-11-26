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
import org.webharvest.ioc.RuntimeRegistrationCoordinator;
import org.webharvest.ioc.ScrapingInterceptor.ScrapingAwareHelper;

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
    private static final int RESOURCE_ABORT_WORKERS = 2;
    private static final int RESOURCE_ABORT_QUEUE_CAPACITY = 128;

    private final ExecutorService executorService;
    private final SessionRegistry sessionRegistry;
    private final EventBus eventBus;
    private final RuntimeRegistrationCoordinator registrationCoordinator;
    private final ScheduledThreadPoolExecutor timeoutScheduler;
    private final ConcurrentMap<String, SessionTask> executionTasks = new ConcurrentHashMap<>();
    private final String runtimeId = UUID.randomUUID().toString();
    private final ThreadPoolExecutor resourceAbortExecutor = createResourceAbortExecutor();
    private final ResourceLedger resourceLedger = new ResourceLedger(runtimeId, resourceAbortExecutor);
    private final AtomicLong executionGeneration = new AtomicLong();
    private final AtomicBoolean deferredFinalizationStarted = new AtomicBoolean();
    private final Object admissionLock = new Object();
    private int pendingSubmissions;
    private boolean executorShutdownInitiated;
    private final CountDownLatch executorShutdownCompleted = new CountDownLatch(1);
    private volatile boolean shutdown;

    /**
     * Creates a service using the EventBus owned by this Guice runtime.
     *
     * @param eventBus shared runtime EventBus
     * @param registrationCoordinator injector-owned registration lifecycle
     */
    @Inject
    public WebHarvestServiceImpl(EventBus eventBus,
                                 RuntimeRegistrationCoordinator registrationCoordinator) {
        this(Executors.newCachedThreadPool(new SessionThreadFactory()),
            new SessionRegistryImpl(), eventBus, registrationCoordinator);
    }

    /** Compatibility constructor for direct, non-Guice service composition. */
    public WebHarvestServiceImpl(EventBus eventBus) {
        this(Executors.newCachedThreadPool(new SessionThreadFactory()),
            new SessionRegistryImpl(), eventBus,
            new RuntimeRegistrationCoordinator(eventBus, new ScrapingAwareHelper()));
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
        this(executorService, sessionRegistry, eventBus,
            new RuntimeRegistrationCoordinator(eventBus, new ScrapingAwareHelper()));
    }

    /**
     * Creates a service with explicit runtime-owned session and registration components.
     */
    public WebHarvestServiceImpl(ExecutorService executorService,
                                 SessionRegistry sessionRegistry,
                                 EventBus eventBus,
                                 RuntimeRegistrationCoordinator registrationCoordinator) {
        if (executorService == null) {
            throw new IllegalArgumentException("ExecutorService cannot be null");
        }
        if (sessionRegistry == null) {
            throw new IllegalArgumentException("SessionRegistry cannot be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("EventBus cannot be null");
        }
        if (registrationCoordinator == null) {
            throw new IllegalArgumentException("RuntimeRegistrationCoordinator cannot be null");
        }

        this.executorService = executorService;
        this.sessionRegistry = sessionRegistry;
        this.eventBus = eventBus;
        this.registrationCoordinator = registrationCoordinator;
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

        // Construct without publishing. A creation event is observable only after
        // successful admission and registration, and subscriber code stays outside
        // the admission lock.
        ScraperSessionImpl session = new ScraperSessionImpl(
            sessionId, clientId, config, eventBus, owner, false);
        LedgerExecutionResourceCoordinator resourceCoordinator =
            new LedgerExecutionResourceCoordinator(resourceLedger, owner);
        SessionTask task = new SessionTask(session, opts, resourceCoordinator);
        session.attachCancellationHandler(task::requestCancellation);

        RuntimeException admissionFailure = null;
        boolean registered = false;
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
            boolean finishBeforeStart = task.rejectBeforeStart();
            if (finishBeforeStart) {
                task.finishBeforeStart();
            }
            session.markFailed(admissionFailure);
            throw admissionFailure;
        }

        try {
            // Admission and registry ownership are now established. Publish exactly
            // once before dispatch; a subscriber may cancel the accepted session.
            session.publishCreatedEvent();

            // Reserve under admissionLock, but call arbitrary executor code outside it.
            // The wrapper resolves the reservation even for direct/CallerRuns executors.
            FutureTask<Void> submittedTask = new FutureTask<>(() -> {
                task.run();
                return null;
            });
            task.attachFuture(submittedTask);
            if (session.getStatus().isTerminal() || task.isCancellationRequested()) {
                boolean finishBeforeStart = task.rejectBeforeStart();
                if (finishBeforeStart) {
                    task.finishBeforeStart();
                }
                task.resolveSubmission();
                return session;
            }
            executorService.execute(submittedTask);
            task.resolveSubmission();
            LOG.info("Accepted session {} for client {}", sessionId, clientId);
            return session;
        } catch (RuntimeException submissionFailure) {
            boolean finishBeforeStart = task.rejectBeforeStart();
            if (registered) {
                sessionRegistry.unregisterIfOwner(session);
            }
            task.resolveSubmission();
            if (finishBeforeStart) {
                task.finishBeforeStart();
            }
            if (!session.getStatus().isTerminal()) {
                session.markFailed(submissionFailure);
            }
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
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_TIMEOUT_SECONDS);
        List<SessionTask> tasks;
        synchronized (admissionLock) {
            shutdown = true;
            tasks = new ArrayList<>(executionTasks.values());
        }
        shutdownExecutorWhenReady();

        if (hasCurrentTaskThread(tasks)) {
            LOG.info("Shutdown initiated from a session task; finalization is deferred until task cleanup completes");
            return;
        }

        if (!awaitPendingSubmissions(deadline)) {
            logIncompleteShutdown("submission dispatch", tasks);
            throw new IllegalStateException("WebHarvestService shutdown deadline expired with "
                + getPendingSubmissionCount() + " accepted submission(s) not yet dispatched");
        }
        shutdownExecutorWhenReady();
        long shutdownSignalBudget = remainingNanos(deadline);
        if (shutdownSignalBudget == 0
            || !executorShutdownCompleted.await(shutdownSignalBudget, TimeUnit.NANOSECONDS)) {
            logIncompleteShutdown("executor shutdown signal", tasks);
            throw new IllegalStateException("WebHarvestService shutdown signal was not completed "
                + "within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds");
        }

        boolean executorTerminated = executorService.awaitTermination(
            remainingNanos(deadline), TimeUnit.NANOSECONDS);
        boolean taskRunsCompleted = executorTerminated && awaitTaskRunsComplete(tasks, deadline);
        if (!executorTerminated || !taskRunsCompleted) {
            markTasksUnresponsive(tasks, false);
            logIncompleteShutdown("worker exit or resource drain", tasks);
            throw new IllegalStateException("WebHarvestService did not terminate all accepted worker and "
                + "resource-drain paths within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds");
        }

        if (!shutdownResourceAbortExecutor(deadline)) {
            logIncompleteShutdown("resource abort callbacks", tasks);
            throw new IllegalStateException("WebHarvestService has resource abort callbacks that did not terminate "
                + "within " + SHUTDOWN_TIMEOUT_SECONDS + " seconds");
        }
        finalizeTimeoutSchedulerIfQuiescent();
        LOG.info("WebHarvestService shut down after all accepted tasks and resource-drain paths completed");
    }

    @Override
    public void shutdownNow() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        List<SessionTask> tasks;
        synchronized (admissionLock) {
            shutdown = true;
            executorShutdownInitiated = true;
            tasks = new ArrayList<>(executionTasks.values());
        }

        // Request resource abort and worker interruption before publishing cancellation
        // events. EventBus subscribers are synchronous; they must not prevent the
        // executor's forced-shutdown signal from being issued.
        for (SessionTask task : tasks) {
            task.requestKill();
        }

        List<Runnable> neverStarted;
        try {
            neverStarted = executorService.shutdownNow();
        } finally {
            executorShutdownCompleted.countDown();
        }
        for (Runnable pending : neverStarted) {
            for (SessionTask task : tasks) {
                task.markRemovedBeforeStart(pending);
            }
        }
        for (SessionTask task : tasks) {
            if (!task.isCompletionCommitted()) {
                task.session.cancel();
            }
        }

        if (hasCurrentTaskThread(tasks)) {
            LOG.info("shutdownNow initiated from a session task; finalization is deferred "
                + "until task cleanup completes");
            return;
        }

        boolean interrupted = false;
        boolean submissionsSettled = false;
        boolean executorTerminated = false;
        boolean taskRunsCompleted = false;
        boolean abortDispatcherTerminated = false;
        try {
            submissionsSettled = awaitPendingSubmissions(deadline);
            if (submissionsSettled) {
                executorTerminated = executorService.awaitTermination(
                    remainingNanos(deadline), TimeUnit.NANOSECONDS);
                if (executorTerminated) {
                    taskRunsCompleted = awaitTaskRunsComplete(tasks, deadline);
                    if (taskRunsCompleted) {
                        abortDispatcherTerminated = shutdownResourceAbortExecutor(deadline);
                    }
                }
            }
        } catch (InterruptedException interruption) {
            interrupted = true;
        }

        if (!submissionsSettled || !executorTerminated || !taskRunsCompleted) {
            markTasksUnresponsive(tasks, true);
            logIncompleteShutdown(!submissionsSettled ? "submission dispatch"
                : !executorTerminated ? "executor termination" : "worker exit or resource drain", tasks);
        } else if (!abortDispatcherTerminated) {
            logIncompleteShutdown("resource abort callbacks", tasks);
        } else {
            finalizeTimeoutSchedulerIfQuiescent();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
            LOG.warn("shutdownNow was interrupted; shutdown remains in progress");
        }
    }

    private boolean awaitPendingSubmissions(long deadline) throws InterruptedException {
        synchronized (admissionLock) {
            while (pendingSubmissions > 0) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    return false;
                }
                long millis = TimeUnit.NANOSECONDS.toMillis(remaining);
                int nanos = (int) (remaining - TimeUnit.MILLISECONDS.toNanos(millis));
                admissionLock.wait(millis, nanos);
            }
            return true;
        }
    }

    private int getPendingSubmissionCount() {
        synchronized (admissionLock) {
            return pendingSubmissions;
        }
    }

    private static long remainingNanos(long deadline) {
        return Math.max(0L, deadline - System.nanoTime());
    }

    private boolean awaitTaskRunsComplete(List<SessionTask> tasks, long deadline)
            throws InterruptedException {
        for (SessionTask task : tasks) {
            long remaining = remainingNanos(deadline);
            if (remaining == 0 || !task.awaitTaskCompletion(remaining, TimeUnit.NANOSECONDS)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasCurrentTaskThread(List<SessionTask> tasks) {
        for (SessionTask task : tasks) {
            if (task.isCurrentTaskThread()) {
                return true;
            }
        }
        return false;
    }

    private void markTasksUnresponsive(List<SessionTask> tasks, boolean killRequested) {
        for (SessionTask task : tasks) {
            if (!task.hasWorkerStarted() || task.hasWorkerExited()) {
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

    private void logIncompleteShutdown(String stage, List<SessionTask> tasks) {
        LOG.warn("WebHarvestService shutdown incomplete at stage {}; pending submissions: {}",
            stage, getPendingSubmissionCount());
        for (SessionTask task : tasks) {
            if (!task.isTaskCompleted()) {
                LOG.warn("Session {} remains in service cleanup (worker exited: {}, execution state: {})",
                    task.session.getSessionId(), task.hasWorkerExited(), task.session.getExecutionState());
            }
        }
    }

    private void shutdownExecutorWhenReady() {
        boolean initiateShutdown = false;
        synchronized (admissionLock) {
            if (shutdown && pendingSubmissions == 0 && !executorShutdownInitiated) {
                executorShutdownInitiated = true;
                initiateShutdown = true;
            }
        }
        if (initiateShutdown) {
            try {
                executorService.shutdown();
            } catch (RuntimeException failure) {
                LOG.error("Supplied executor failed while initiating graceful shutdown", failure);
            } finally {
                executorShutdownCompleted.countDown();
            }
        }
    }

    private boolean shutdownResourceAbortExecutor(long deadline) throws InterruptedException {
        resourceAbortExecutor.shutdown();
        boolean terminated = resourceAbortExecutor.awaitTermination(
            remainingNanos(deadline), TimeUnit.NANOSECONDS);
        if (terminated) {
            // Runtime registrations are retired only after workers, resource drains,
            // and asynchronous abort callbacks have all reached quiescence.
            registrationCoordinator.close();
        }
        return terminated;
    }

    private void finalizeTimeoutSchedulerIfQuiescent() {
        synchronized (admissionLock) {
            if (shutdown && pendingSubmissions == 0 && executionTasks.isEmpty()) {
                resourceAbortExecutor.shutdown();
                timeoutScheduler.shutdownNow();
            }
        }
    }

    private static void awaitTerminationUninterruptibly(ExecutorService executor) {
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    if (executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS)) {
                        return;
                    }
                } catch (InterruptedException interruption) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Completes runtime retirement when shutdown was initiated by one of its own
     * session workers. Such a worker cannot wait for its own executor to terminate;
     * the finalizer therefore runs on a separate daemon thread after the last task
     * has completed its cleanup and submission admission has settled.
     */
    private void scheduleDeferredRuntimeFinalizationIfQuiescent() {
        boolean startFinalizer = false;
        synchronized (admissionLock) {
            if (shutdown && pendingSubmissions == 0 && executionTasks.isEmpty()
                && executorShutdownCompleted.getCount() == 0
                && deferredFinalizationStarted.compareAndSet(false, true)) {
                startFinalizer = true;
            }
        }
        if (!startFinalizer) {
            return;
        }

        Thread finalizer = new Thread(() -> {
            try {
                // This background thread does not extend the caller's bounded shutdown wait.
                // It must, however, keep waiting: a slow abort callback may outlive the
                // public shutdown deadline, and registrations must remain until it exits.
                awaitTerminationUninterruptibly(executorService);
                resourceAbortExecutor.shutdown();
                awaitTerminationUninterruptibly(resourceAbortExecutor);
                registrationCoordinator.close();
                finalizeTimeoutSchedulerIfQuiescent();
                LOG.info("Deferred WebHarvestService runtime finalization completed after worker-initiated shutdown");
            } catch (RuntimeException failure) {
                LOG.error("Deferred runtime finalization failed; runtime registrations remain tracked", failure);
            }
        }, "webharvest-runtime-finalizer-" + runtimeId);
        finalizer.setDaemon(true);
        finalizer.start();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    private ThreadPoolExecutor createResourceAbortExecutor() {
        AtomicInteger sequence = new AtomicInteger();
        return new ThreadPoolExecutor(RESOURCE_ABORT_WORKERS, RESOURCE_ABORT_WORKERS,
            0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(RESOURCE_ABORT_QUEUE_CAPACITY), runnable -> {
                Thread thread = new Thread(runnable,
                    "webharvest-resource-abort-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
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
        private final CountDownLatch taskCompletion = new CountDownLatch(1);
        private Future<?> future;
        private ScheduledFuture<?> escalationFuture;
        private Thread worker;
        private Thread cleanupThread;
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
                worker = Thread.currentThread();
                if (cancellationRequested || exited) {
                    exited = true;
                    cancelEscalation();
                    skipExecution = true;
                } else {
                    started = true;
                    if (!session.markExecutionStarted()) {
                        exited = true;
                        cancelEscalation();
                        skipExecution = true;
                    } else {
                        skipExecution = false;
                    }
                }
            }

            if (skipExecution) {
                try {
                    session.markExecutionExited();
                    drainResources(Duration.ZERO);
                } finally {
                    completeTaskRun();
                }
                return;
            }

            try {
                executeSession(this);
            } finally {
                try {
                    synchronized (this) {
                        exited = true;
                        cancelEscalation();
                    }
                    // Worker exit and resource drain are separate lifecycle facts.
                    session.markExecutionExited();
                    drainResources(RESOURCE_DRAIN_BUDGET);
                } finally {
                    completeTaskRun();
                }
            }
        }

        private boolean attachFuture(Future<?> submittedFuture) {
            boolean finishBeforeStart = false;
            boolean cancelRunningFuture = false;
            synchronized (this) {
                future = submittedFuture;
                if (cancellationRequested && !exited) {
                    if (!started) {
                        finishBeforeStart = removeQueuedFutureLocked() && claimBeforeStartExitLocked();
                    } else {
                        cancelRunningFuture = true;
                    }
                }
            }
            if (cancelRunningFuture) {
                submittedFuture.cancel(true);
            }
            return finishBeforeStart;
        }

        private void resolveSubmission() {
            if (submissionResolved.compareAndSet(false, true)) {
                synchronized (admissionLock) {
                    if (pendingSubmissions <= 0) {
                        throw new IllegalStateException("Submission reservation underflow for session "
                            + session.getSessionId());
                    }
                    pendingSubmissions--;
                    admissionLock.notifyAll();
                }
                shutdownExecutorWhenReady();
                finalizeTimeoutSchedulerIfQuiescent();
                scheduleDeferredRuntimeFinalizationIfQuiescent();
            }
        }

        private synchronized boolean isCurrentTaskThread() {
            Thread current = Thread.currentThread();
            return taskCompletion.getCount() > 0
                && (worker == current || cleanupThread == current);
        }

        private synchronized boolean hasWorkerStarted() {
            return started;
        }

        private synchronized boolean isCompletionCommitted() {
            return completionCommitted;
        }

        private synchronized boolean hasWorkerExited() {
            return exited;
        }

        private boolean isTaskCompleted() {
            return taskCompletion.getCount() == 0;
        }

        private boolean awaitTaskCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return taskCompletion.await(timeout, unit);
        }

        private void completeTaskRun() {
            synchronized (this) {
                worker = null;
                cleanupThread = null;
            }
            taskCompletion.countDown();
            executionTasks.remove(session.getSessionId(), this);
            finalizeTimeoutSchedulerIfQuiescent();
            scheduleDeferredRuntimeFinalizationIfQuiescent();
        }

        private void requestCancellation() {
            boolean startedExecution;
            boolean finishBeforeStart = false;
            synchronized (this) {
                if (exited || cancellationRequested) {
                    return;
                }
                cancellationRequested = true;
                session.markGracefulTerminationRequested();
                if (!started && removeQueuedFutureLocked()) {
                    finishBeforeStart = claimBeforeStartExitLocked();
                    startedExecution = false;
                } else {
                    startedExecution = started;
                }
            }

            if (finishBeforeStart) {
                finishBeforeStart();
                return;
            }

            // Abort I/O before interrupting the worker so it cannot exit before
            // resource termination has been requested.
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

        private boolean removeQueuedFutureLocked() {
            if (future instanceof Runnable && executorService instanceof ThreadPoolExecutor) {
                boolean removed = ((ThreadPoolExecutor) executorService).remove((Runnable) future);
                if (removed) {
                    future.cancel(false);
                    return true;
                }
            }
            return false;
        }

        private boolean claimBeforeStartExitLocked() {
            if (started || exited) {
                return false;
            }
            exited = true;
            cancelEscalation();
            return true;
        }

        private void markRemovedBeforeStart(Runnable pending) {
            boolean finishBeforeStart = false;
            synchronized (this) {
                if (future == pending && !started && !exited) {
                    future.cancel(false);
                    finishBeforeStart = claimBeforeStartExitLocked();
                }
            }
            if (finishBeforeStart) {
                finishBeforeStart();
            }
        }

        private void requestKill() {
            boolean startedExecution;
            boolean finishBeforeStart = false;
            synchronized (this) {
                if (exited || completionCommitted) {
                    return;
                }
                cancellationRequested = true;
                session.markKillRequested();
                if (!started && removeQueuedFutureLocked()) {
                    finishBeforeStart = claimBeforeStartExitLocked();
                    startedExecution = false;
                } else {
                    startedExecution = started;
                }
            }

            if (finishBeforeStart) {
                finishBeforeStart();
                return;
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
        private boolean completeSuccessfully() {
            synchronized (this) {
                if (timedOut || cancellationRequested || exited || session.getStatus().isTerminal()) {
                    return false;
                }
                // Commit against the timeout gate before publishing SessionCompletedEvent.
                completionCommitted = true;
            }

            try {
                // EventBus subscribers must not run while the SessionTask monitor is held.
                session.markCompleted();
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

        private boolean rejectBeforeStart() {
            boolean finishBeforeStart = false;
            Thread workerToInterrupt = null;
            synchronized (this) {
                cancellationRequested = true;
                if (!started && !exited) {
                    // A custom executor may throw after it has already started the
                    // FutureTask. Claim pre-start exit only when cancellation wins.
                    boolean cancelledBeforeStart = future == null || future.cancel(false);
                    if (cancelledBeforeStart) {
                        finishBeforeStart = claimBeforeStartExitLocked();
                    }
                } else if (!exited) {
                    workerToInterrupt = worker;
                }
            }
            if (workerToInterrupt != null) {
                workerToInterrupt.interrupt();
            }
            return finishBeforeStart;
        }

        private void finishBeforeStart() {
            synchronized (this) {
                cleanupThread = Thread.currentThread();
            }
            try {
                // The executor has confirmed that this task will never be entered.
                session.markExecutionExited();
                drainResources(Duration.ZERO);
            } finally {
                completeTaskRun();
            }
        }

        private void drainResources(Duration budget) {
            try {
                for (org.webharvest.runtime.resource.LedgerExecutionResourceContext.DrainReport report
                    : resourceCoordinator.markOwnerQuiescentAndDrain(budget)) {
                    if (report.getState() != ResourceState.CLOSED) {
                        LOG.warn("Resource {} for session {} remains {} (active leases: {}, abort: {})",
                            report.getResourceId(), session.getSessionId(),
                            report.getState(), report.getActiveLeases(), report.getAbortState());
                    }
                    // Abort failure remains operationally relevant even when the
                    // resource's subsequent close succeeds and reaches CLOSED.
                    if (report.getAbortFailure() != null) {
                        LOG.warn("Resource abort failed for {}", report.getResourceId(), report.getAbortFailure());
                    }
                    if (report.getCloseFailure() != null) {
                        LOG.warn("Resource close failed for {}", report.getResourceId(), report.getCloseFailure());
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while draining resources for session {} (owner {})",
                    session.getSessionId(), session.getExecutionOwner(), interrupted);
            } catch (RuntimeException | Error drainFailure) {
                // submit() wraps task failures in FutureTask; without this log, a fatal
                // cleanup failure can be retained by an unread Future and become invisible.
                // Keep the failure visible with its owner identity, then preserve propagation.
                LOG.error("Fatal resource drain failure for session {} (owner {})",
                    session.getSessionId(), session.getExecutionOwner(), drainFailure);
                throw drainFailure;
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
