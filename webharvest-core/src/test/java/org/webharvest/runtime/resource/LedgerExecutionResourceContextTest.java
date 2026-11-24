/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.resource;

import org.testng.annotations.Test;
import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void abortFailureRemainsObservableAndDoesNotPreventLaterClose() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger abortCalls = new AtomicInteger();
        AtomicInteger closeCalls = new AtomicInteger();

        ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
            "abort-failure", () -> {
                abortCalls.incrementAndGet();
                throw new Exception("abort failed");
            }, closeCalls::incrementAndGet);

        context.requestTermination();
        context.requestTermination();
        assertEquals(abortCalls.get(), 1);
        assertNotNull(resource.getAbortFailure());
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);

        LedgerExecutionResourceContext.DrainReport report =
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1)).get(0);
        assertEquals(report.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertNotNull(report.getAbortFailure());
        assertEquals(closeCalls.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void fatalAbortFailureDoesNotPreventOtherResourcesFromBeingAborted() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger laterAbortCalls = new AtomicInteger();

        context.registerSessionResource("fatal-abort", () -> {
            throw new AssertionError("fatal abort");
        }, null);
        context.registerSessionResource("later-abort", laterAbortCalls::incrementAndGet, null);

        expectThrows(AssertionError.class, context::requestTermination);
        assertEquals(laterAbortCalls.get(), 1);
        context.markOwnerQuiescentAndDrain(Duration.ZERO);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void closeFailureIsReportedAndRegistrationRemainsForDiagnostics() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);

        ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
            "close-failure", null, () -> {
                throw new Exception("close failed");
            });

        LedgerExecutionResourceContext.DrainReport report =
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1)).get(0);
        assertEquals(report.getState(), ExecutionResourceAccess.ResourceState.CLOSE_FAILED);
        assertNotNull(report.getCloseFailure());
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSE_FAILED);
        assertEquals(ledger.size(), 1);
    }

    @Test(groups = {"unit", "runtime"})
    public void operationResourceCloseIsIdempotentAndStopsFurtherLeaseAdmission() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger closeCalls = new AtomicInteger();

        ExecutionResourceAccess.OperationResource resource = context.registerOperationResource(
            "operation-idempotent", null, closeCalls::incrementAndGet);
        resource.close();
        resource.close();

        assertEquals(closeCalls.get(), 1);
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        expectThrows(IllegalStateException.class, () -> { resource.acquire(); });
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void rejectsInvalidDrainBudgetsAndDuplicateRegistrations() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);

        expectThrows(NullPointerException.class, () -> {
            try {
                context.markOwnerQuiescentAndDrain(null);
            } catch (InterruptedException interrupted) {
                throw new AssertionError(interrupted);
            }
        });
        expectThrows(IllegalArgumentException.class, () -> {
            try {
                context.markOwnerQuiescentAndDrain(Duration.ofNanos(-1));
            } catch (InterruptedException interrupted) {
                throw new AssertionError(interrupted);
            }
        });

        context.registerSessionResource("duplicate", null, null);
        expectThrows(IllegalStateException.class,
            () -> { context.registerSessionResource("duplicate", null, null); });
    }

    @Test(groups = {"unit", "runtime"})
    public void interruptedDrainKeepsResourceQuarantinedUntilLeaseRelease() throws Exception {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);
        AtomicInteger closeCalls = new AtomicInteger();

        ExecutionResourceAccess.ResourceHandle resource = context.registerSessionResource(
            "interrupted-drain", null, closeCalls::incrementAndGet);
        ExecutionResourceAccess.Lease lease = resource.acquire();
        context.requestTermination();

        Thread.currentThread().interrupt();
        try {
            context.markOwnerQuiescentAndDrain(Duration.ofSeconds(1));
            fail("Expected interrupted drain to preserve quarantine");
        } catch (InterruptedException expected) {
            assertFalse(Thread.currentThread().isInterrupted(), "Interrupted wait must clear the interrupt flag");
        }

        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.QUARANTINED);
        assertEquals(closeCalls.get(), 0);
        lease.close();
        assertEquals(resource.getState(), ExecutionResourceAccess.ResourceState.CLOSED);
        assertEquals(closeCalls.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void rejectsDrainBudgetThatCannotBeRepresentedInNanoseconds() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);

        expectThrows(IllegalArgumentException.class, () -> {
            try {
                context.markOwnerQuiescentAndDrain(Duration.ofSeconds(Long.MAX_VALUE));
            } catch (InterruptedException interrupted) {
                throw new AssertionError(interrupted);
            }
        });
    }

    @Test(groups = {"unit", "runtime"})
    public void terminationWithoutResourcesIsIdempotentAndClosesAdmission() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        LedgerExecutionResourceContext context = new LedgerExecutionResourceContext(ledger, owner);

        context.requestTermination();
        context.requestTermination();
        assertEquals(context.getOwner(), owner);
        expectThrows(IllegalStateException.class,
            () -> { context.registerSessionResource("late-empty", null, null); });
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
}
