/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.resource;

import org.testng.annotations.Test;
import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/** Deterministic tests for the execution-bound resource facade. */
public class LedgerExecutionResourceContextTest {


    @Test(groups = {"unit", "runtime"})
    public void requestTerminationDoesNotWaitForBlockedProviderAbort() throws Exception {
        java.util.concurrent.ThreadPoolExecutor dispatcher = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(4),
            runnable -> {
                Thread thread = new Thread(runnable, "execution-context-abort-test");
                thread.setDaemon(true);
                return thread;
            }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            ResourceLedger ledger = new ResourceLedger("runtime-a", dispatcher);
            ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
            LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
            AtomicInteger closes = new AtomicInteger();
            ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
                "blocked-provider", () -> {
                    entered.countDown();
                    release.await();
                }, closes::incrementAndGet);

            context.requestTermination();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            java.util.List<LedgerExecutionResourceContext.DrainReport> reports =
                context.markOwnerQuiescentAndDrain(Duration.ZERO);
            assertEquals(reports.get(0).getAbortState(), ResourceLedger.AbortState.RUNNING);
            assertEquals(reports.get(0).getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
            assertEquals(closes.get(), 0);

            release.countDown();
            dispatcher.shutdown();
            assertTrue(dispatcher.awaitTermination(5, TimeUnit.SECONDS));
            reports = context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
            assertEquals(reports.get(0).getAbortState(), ResourceLedger.AbortState.SUCCEEDED);
            assertEquals(reports.get(0).getState(), ExecutionResourceAccess.ResourceState.CLOSED);
            assertEquals(closes.get(), 1);
        } finally {
            release.countDown();
            dispatcher.shutdownNow();
        }
    }

    @Test(groups = {"unit", "runtime"})
    public void lateAbortCompletionAutomaticallyRetriesDrainAfterOwnerExit() throws Exception {
        java.util.concurrent.ThreadPoolExecutor dispatcher = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(4),
            runnable -> {
                Thread thread = new Thread(runnable, "late-abort-completion-test");
                thread.setDaemon(true);
                return thread;
            }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        CountDownLatch abortEntered = new CountDownLatch(1);
        CountDownLatch releaseAbort = new CountDownLatch(1);
        AtomicInteger closes = new AtomicInteger();
        ResourceLedger ledger = new ResourceLedger("runtime-a", dispatcher);
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "late-abort-session", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        try {
            ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
                "late-abort-resource", () -> {
                    abortEntered.countDown();
                    releaseAbort.await();
                }, closes::incrementAndGet);

            context.requestTermination();
            assertTrue(abortEntered.await(5, TimeUnit.SECONDS),
                "The provider abort callback must be running asynchronously");

            java.util.List<LedgerExecutionResourceContext.DrainReport> firstDrain =
                context.markOwnerQuiescentAndDrain(Duration.ZERO);
            assertEquals(firstDrain.get(0).getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
            assertEquals(firstDrain.get(0).getAbortState(), ResourceLedger.AbortState.RUNNING);
            assertEquals(closes.get(), 0);

            // The owner has exited and no lease remains. Releasing abort must itself
            // trigger the retained drain; the test deliberately performs no second drain.
            releaseAbort.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (resource.getState() != ExecutionResourceAccess.ResourceState.CLOSED
                && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED,
                "Abort completion must automatically continue the quarantined drain");
            assertEquals(closes.get(), 1);
            assertEquals(ledger.size(), 0,
                "Automatically closed resources must retire their ledger registration");
        } finally {
            releaseAbort.countDown();
            dispatcher.shutdown();
            assertTrue(dispatcher.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test(groups = {"unit", "runtime"})
    public void sessionResourceRemainsQuarantinedUntilOwnerAndLeasesAreQuiescent() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger aborts = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();

        ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
            "jdbc/session-a", aborts::incrementAndGet, closes::incrementAndGet);
        ExecutionResourceAccess.Lease lease = resource.acquire();

        context.requestTermination();
        assertEquals(aborts.get(), 1);
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        expectThrows(IllegalStateException.class, () -> { resource.acquire(); });
        assertEquals(closes.get(), 0);

        LedgerExecutionResourceContext.DrainReport report =
            context.markOwnerQuiescentAndDrain(Duration.ZERO).get(0);
        assertEquals(report.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        assertEquals(report.getActiveLeases(), 1);
        assertEquals(closes.get(), 0);

        lease.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(closes.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void normalSessionExitClosesResourceWithoutInvokingAbort() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger aborts = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();

        ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
            "browser/session-a", aborts::incrementAndGet, closes::incrementAndGet);

        context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(aborts.get(), 0);
        assertEquals(closes.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void operationResourceClosesAfterItsLastLeaseIsReleased() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger closes = new AtomicInteger();

        ExecutionResourceAccess.OperationResource resource = context.registerOperationResource(
            "http/request-1", null, closes::incrementAndGet);
        ExecutionResourceAccess.Lease lease = resource.acquire();

        resource.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        assertEquals(closes.get(), 0);

        lease.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(closes.get(), 1);
        assertEquals(ledger.size(), 0);
    }


    @Test(groups = {"unit", "runtime"})
    public void interruptionDuringOneDrainDoesNotSkipLaterResources() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        ExecutionResourceAccess.ResourceHandle blocked = context.registerSessionResource(
            "blocked-first", null, null);
        ExecutionResourceAccess.Lease lease = blocked.acquire();
        AtomicInteger laterCloses = new AtomicInteger();
        ExecutionResourceAccess.ResourceHandle later = context.registerSessionResource(
            "ready-second", null, laterCloses::incrementAndGet);

        Thread.currentThread().interrupt();
        try {
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(5));
            fail("Expected the interrupted drain to be reported");
        } catch (InterruptedException expected) {
            assertEquals(later.getState(), ExecutionResourceAccess.ResourceState.CLOSED,
                "A later quiescent resource must still be closed after interruption");
            assertEquals(laterCloses.get(), 1);
            assertEquals(blocked.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        } finally {
            Thread.interrupted();
            lease.close();
        }
        assertEquals(blocked.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void fatalCloseFailureDoesNotSkipLaterResources() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AssertionError fatal = new AssertionError("first close failed");
        AssertionError secondary = new AssertionError("second close failed");
        context.registerSessionResource("fatal-first", null, () -> {
            throw fatal;
        });
        ExecutionResourceAccess.ResourceHandle secondFailure = context.registerSessionResource(
            "fatal-second", null, () -> {
                throw secondary;
            });
        context.registerSessionResource("same-fatal-third", null, () -> {
            throw fatal;
        });
        AtomicInteger laterCloses = new AtomicInteger();
        ExecutionResourceAccess.ResourceHandle later = context.registerSessionResource(
            "ready-third", null, laterCloses::incrementAndGet);

        try {
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
            fail("Expected the fatal close failure to be rethrown");
        } catch (AssertionError actual) {
            assertSame(actual, fatal);
            assertEquals(actual.getSuppressed().length, 1);
            assertSame(actual.getSuppressed()[0], secondary);
        }
        assertEquals(later.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(laterCloses.get(), 1);
        assertEquals(secondFailure.getState(), ExecutionResourceAccess.ResourceState.CLOSE_FAILED);
        assertEquals(ledger.size(), 3,
            "All failed registrations remain retained for diagnostics");
    }

    @Test(groups = {"unit", "runtime"})
    public void fatalCloseFailurePreservesInterruptionAndContinuesDraining() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        ExecutionResourceAccess.ResourceHandle blocked =
            context.registerSessionResource("blocked-first", null, null);
        ExecutionResourceAccess.Lease lease = blocked.acquire();
        AssertionError fatal = new AssertionError("close failed after interruption");
        context.registerSessionResource("fatal-second", null, () -> {
            throw fatal;
        });
        AtomicInteger laterCloses = new AtomicInteger();
        ExecutionResourceAccess.ResourceHandle later = context.registerSessionResource(
            "ready-third", null, laterCloses::incrementAndGet);

        Thread.currentThread().interrupt();
        try {
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(5));
            fail("Expected the fatal close failure to be rethrown");
        } catch (AssertionError actual) {
            assertSame(actual, fatal);
            assertEquals(actual.getSuppressed().length, 1);
            assertTrue(actual.getSuppressed()[0] instanceof InterruptedException,
                "The earlier interruption must remain attached to the fatal cleanup failure");
            assertTrue(Thread.currentThread().isInterrupted(),
                "A fatal cleanup failure must not consume the caller's interrupt signal");
            assertEquals(blocked.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
            assertEquals(later.getState(), ExecutionResourceAccess.ResourceState.CLOSED,
                "A later resource must still be drained after the fatal failure");
            assertEquals(laterCloses.get(), 1);
        } finally {
            Thread.interrupted();
            lease.close();
        }

        assertEquals(blocked.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(ledger.size(), 1,
            "The failed registration remains retained for diagnostics");
    }

    @Test(groups = {"unit", "runtime"})
    public void requestTerminationIsIdempotent() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger aborts = new AtomicInteger();
        ExecutionResourceAccess.ResourceHandle resource =
            context.registerSessionResource("idempotent", aborts::incrementAndGet, null);

        context.requestTermination();
        context.requestTermination();

        assertEquals(aborts.get(), 1);
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        context.markOwnerQuiescentAndDrain(Duration.ZERO);
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
    }

    @Test(groups = {"unit", "runtime"})
    public void requestTerminationAttemptsEveryAbortAndPreservesFatalFailures() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AssertionError first = new AssertionError("first abort failed");
        AssertionError second = new AssertionError("second abort failed");
        context.registerSessionResource("first-error", () -> {
            throw first;
        }, null);
        context.registerSessionResource("second-error", () -> {
            throw second;
        }, null);
        context.registerSessionResource("same-first-error", () -> {
            throw first;
        }, null);

        try {
            context.requestTermination();
            fail("Expected fatal abort failure to be rethrown after fan-out");
        } catch (AssertionError expected) {
            assertSame(expected, first);
            assertEquals(expected.getSuppressed().length, 1);
            assertSame(expected.getSuppressed()[0], second);
        }

        context.markOwnerQuiescentAndDrain(Duration.ZERO);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void sessionLeaseReleaseBeforeQuiescenceDoesNotDrainResource() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger closes = new AtomicInteger();
        ExecutionResourceAccess.ResourceHandle resource =
            context.registerSessionResource("early-release", null, closes::incrementAndGet);
        ExecutionResourceAccess.Lease lease = resource.acquire();

        lease.close();

        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.OPEN);
        assertEquals(closes.get(), 0);
        context.markOwnerQuiescentAndDrain(Duration.ZERO);
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(closes.get(), 1);
    }

    @Test(groups = {"unit", "runtime"})
    public void leaseAndOperationCloseAreIdempotent() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger closes = new AtomicInteger();

        ExecutionResourceAccess.OperationResource resource =
            context.registerOperationResource("idempotent-operation", null, closes::incrementAndGet);
        ExecutionResourceAccess.Lease lease = resource.acquire();

        resource.close();
        resource.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        assertEquals(closes.get(), 0);

        lease.close();
        lease.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(closes.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"}, expectedExceptions = IllegalArgumentException.class)
    public void rejectsNegativeOwnerDrainBudget() throws InterruptedException {
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(
            new ResourceLedger("runtime-a"), new ExecutionOwner("runtime-a", "session-a", 1));
        context.markOwnerQuiescentAndDrain(Duration.ofNanos(-1));
    }

    @Test(groups = {"unit", "runtime"})
    public void rejectsOverflowingDrainBudgetWithoutChangingLifecycleOrAdmission() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        ExecutionResourceAccess.ResourceHandle existing =
            context.registerSessionResource("existing", null, null);

        expectThrows(IllegalArgumentException.class, () -> {
            try {
                context.markOwnerQuiescentAndDrain(Duration.ofSeconds(Long.MAX_VALUE));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Budget validation must not interrupt the caller", interrupted);
            }
        });

        assertEquals(existing.getState(), ExecutionResourceAccess.ResourceState.OPEN,
            "An invalid budget must not partially transition existing registrations");
        ExecutionResourceAccess.ResourceHandle admittedAfterRejectedDrain =
            context.registerSessionResource("admitted-after-rejection", null, null);
        ExecutionResourceAccess.Lease lease = admittedAfterRejectedDrain.acquire();
        lease.close();

        context.markOwnerQuiescentAndDrain(Duration.ZERO);
        assertEquals(existing.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(admittedAfterRejectedDrain.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(ledger.size(), 0);
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable actual) {
            assertTrue(expected.isInstance(actual),
                "Expected " + expected.getName() + " but received " + actual.getClass().getName());
            return;
        }
        fail("Expected " + expected.getName() + " to be thrown");
    }

    @Test(groups = {"unit", "runtime"})
    public void rejectsRegistrationAfterSessionTerminationAndForeignRuntimeOwner() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);

        context.requestTermination();
        expectThrows(IllegalStateException.class,
            () -> { context.registerSessionResource("late", null, null); });

        expectThrows(IllegalArgumentException.class,
            () -> { new LedgerExecutionResourceContext(ledger,
                new ExecutionOwner("runtime-b", "session-b", 1)); });
    }
    @Test(groups = {"unit", "runtime"})
    public void requestTerminationClosesFacadeLeaseAdmissionBeforeAbortCallbacks() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        CountDownLatch firstAbortEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstAbort = new CountDownLatch(1);
        AtomicReference<Throwable> terminationFailure = new AtomicReference<>();

        context.registerSessionResource("first", () -> {
            firstAbortEntered.countDown();
            if (!releaseFirstAbort.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to release the first abort callback");
            }
        }, null);
        ExecutionResourceAccess.ResourceHandle second = context.registerSessionResource("second", null, null);

        Thread terminator = new Thread(() -> {
            try {
                context.requestTermination();
            } catch (Throwable failure) {
                terminationFailure.set(failure);
            }
        }, "resource-termination-test");

        terminator.start();
        try {
            assertTrue(firstAbortEntered.await(5, TimeUnit.SECONDS),
                "The first abort callback must block before the second lease admission is attempted");

            ExecutionResourceAccess.Lease unexpectedLease = null;
            try {
                unexpectedLease = second.acquire();
            } catch (IllegalStateException expected) {
                // Facade-level admission is already closed, even though the second
                // registration has not yet received its individual close request.
            }
            if (unexpectedLease != null) {
                unexpectedLease.close();
                fail("A later registration must not admit a lease during termination fan-out");
            }
        } finally {
            releaseFirstAbort.countDown();
            terminator.join(5000);
        }

        assertFalse(terminator.isAlive(), "Termination should finish after the blocked callback is released");
        assertNull(terminationFailure.get(), "Termination should not fail");

        context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
        assertEquals(second.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(ledger.size(), 0);
    }

}
