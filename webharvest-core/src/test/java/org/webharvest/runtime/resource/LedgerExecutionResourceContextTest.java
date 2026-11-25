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
    public void requestTerminationIsIdempotent() {
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
    public void requestTerminationAttemptsEveryAbortAndPreservesFatalFailures() {
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

    @Test(groups = {"unit", "runtime"}, expectedExceptions = IllegalArgumentException.class)
    public void rejectsOwnerDrainBudgetThatOverflowsNanoseconds() throws InterruptedException {
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(
            new ResourceLedger("runtime-a"), new ExecutionOwner("runtime-a", "session-a", 1));
        context.markOwnerQuiescentAndDrain(Duration.ofSeconds(Long.MAX_VALUE));
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
