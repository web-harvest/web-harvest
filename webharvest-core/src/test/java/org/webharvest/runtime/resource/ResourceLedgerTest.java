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

package org.webharvest.runtime.resource;

import org.testng.annotations.Test;
import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/** Deterministic tests for resource ownership, lease admission and drain. */
public class ResourceLedgerTest {

    @Test(groups = {"unit", "runtime"})
    public void testCloseRequestRejectsNewLeasesAndClosesOnlyAfterQuiescenceAndDrain() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        AtomicInteger closeCount = new AtomicInteger();
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        ResourceLedger.Registration registration = ledger.registerOwned(
            "jdbc/session-a", ResourceLedger.Scope.SESSION, owner, null, closeCount::incrementAndGet);
        ResourceLedger.Lease lease = registration.acquire();

        registration.requestClose();
        assertEquals(registration.getState(), ResourceLedger.State.CLOSE_REQUESTED);
        expectThrows(IllegalStateException.class, registration::acquire);

        ResourceLedger.DrainResult timedOut = registration.drainAndClose(Duration.ZERO);
        assertEquals(timedOut.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(timedOut.getActiveLeases(), 1);
        assertEquals(closeCount.get(), 0);

        lease.close();
        registration.markOwnerQuiescent();
        ResourceLedger.DrainResult drained = registration.drainAndClose(Duration.ofSeconds(1));
        assertTrue(drained.isClosed());
        assertEquals(closeCount.get(), 1);

        registration.close();
        registration.close();
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void testConcurrentAcquireAndCloseRequestHaveOneAtomicBoundary() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ResourceLedger.Registration registration = ledger.registerOwned(
            "jdbc/race", ResourceLedger.Scope.SESSION,
            new ExecutionOwner("runtime-a", "session-a", 1), null, null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<ResourceLedger.Lease> acquired = new AtomicReference<>();
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        Thread acquirer = new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                acquired.set(registration.acquire());
            } catch (IllegalStateException closeWon) {
                // Close admission won the same atomic boundary.
            } catch (Throwable failure) {
                unexpected.set(failure);
            }
        });
        Thread closer = new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                registration.requestClose();
            } catch (Throwable failure) {
                unexpected.set(failure);
            }
        });

        acquirer.start();
        closer.start();
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        acquirer.join(5000);
        closer.join(5000);

        assertFalse(acquirer.isAlive());
        assertFalse(closer.isAlive());
        assertNull(unexpected.get());
        assertEquals(registration.getState(), ResourceLedger.State.CLOSE_REQUESTED);
        assertEquals(registration.getActiveLeaseCount(), acquired.get() == null ? 0 : 1);
        expectThrows(IllegalStateException.class, registration::acquire);

        if (acquired.get() != null) {
            acquired.get().close();
        }
        registration.markOwnerQuiescent();
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testRuntimeResourceReportsOutstandingLeaseOwners() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner ownerA = new ExecutionOwner("runtime-a", "session-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner("runtime-a", "session-b", 2);
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "shared-http-client", null, closeCount::incrementAndGet);

        expectThrows(IllegalStateException.class, registration::acquire);
        ResourceLedger.Lease leaseA = registration.acquire(ownerA);
        ResourceLedger.Lease leaseB = registration.acquire(ownerB);
        registration.requestClose();
        registration.markOwnerQuiescent();

        ResourceLedger.DrainResult blocked = registration.drainAndClose(Duration.ZERO);
        assertEquals(blocked.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(blocked.getOutstandingLeases().size(), 2);
        assertTrue(blocked.getOutstandingLeases().stream()
            .anyMatch(lease -> ownerA.equals(lease.getOwner())));
        assertTrue(blocked.getOutstandingLeases().stream()
            .anyMatch(lease -> ownerB.equals(lease.getOwner())));
        assertEquals(closeCount.get(), 0);

        leaseA.close();
        ResourceLedger.DrainResult oneRemaining = registration.drainAndClose(Duration.ZERO);
        assertEquals(oneRemaining.getOutstandingLeases().size(), 1);
        assertTrue(oneRemaining.getOutstandingLeases().stream()
            .allMatch(lease -> ownerB.equals(lease.getOwner())));

        leaseB.close();
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertEquals(closeCount.get(), 1);
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testSessionResourceRejectsLeaseFromDifferentExecutionOwner() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner ownerA = new ExecutionOwner("runtime-a", "session-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner("runtime-a", "session-b", 2);
        ResourceLedger.Registration registration = ledger.registerOwned(
            "session-resource", ResourceLedger.Scope.SESSION, ownerA, null, null);

        expectThrows(IllegalArgumentException.class, () -> registration.acquire(ownerB));
        ResourceLedger.Lease lease = registration.acquire(ownerA);
        lease.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testOwnerQuiescenceClosesLeaseAdmission() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        ResourceLedger.Registration registration = ledger.registerOwned(
            "session-resource", ResourceLedger.Scope.SESSION, owner, null, null);

        registration.markOwnerQuiescent();
        assertEquals(registration.getState(), ResourceLedger.State.CLOSE_REQUESTED);
        expectThrows(IllegalStateException.class, registration::acquire);
    }

    @Test(groups = {"unit", "runtime"})
    public void testOwnerQuiescenceIsRequiredEvenWhenNoLeaseIsActive() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerOwned(
            "browser/session-a", ResourceLedger.Scope.SESSION,
            new ExecutionOwner("runtime-a", "session-a", 1), null, closeCount::incrementAndGet);

        ResourceLedger.DrainResult timedOut = registration.drainAndClose(Duration.ZERO);
        assertEquals(timedOut.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(closeCount.get(), 0);

        registration.markOwnerQuiescent();
        expectThrows(IllegalStateException.class, registration::acquire);
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertEquals(closeCount.get(), 1);
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testAbortIsSeparateIdempotentAndItsFailureIsPreserved() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        AtomicInteger abortCount = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        IllegalStateException abortFailure = new IllegalStateException("abort failed");
        ResourceLedger.Registration registration = ledger.registerOwned(
            "ftp/op-1", ResourceLedger.Scope.OPERATION,
            new ExecutionOwner("runtime-a", "session-a", 1),
            () -> {
                abortCount.incrementAndGet();
                throw abortFailure;
            }, closeCount::incrementAndGet);

        registration.requestClose();
        registration.abort();
        registration.abort();
        assertSame(registration.getAbortFailure(), abortFailure);
        assertEquals(abortCount.get(), 1);
        assertEquals(closeCount.get(), 0);

        registration.markOwnerQuiescent();
        ResourceLedger.DrainResult result = registration.drainAndClose(Duration.ofSeconds(1));
        assertTrue(result.isClosed());
        assertSame(result.getAbortFailure(), abortFailure);
        assertEquals(closeCount.get(), 1);
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testCloseFailureIsVisibleAndRegistrationIsRetained() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        IllegalArgumentException closeFailure = new IllegalArgumentException("close failed");
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "http/runtime", null, () -> { throw closeFailure; });

        registration.requestClose();
        registration.markOwnerQuiescent();
        ResourceLedger.DrainResult result = registration.drainAndClose(Duration.ofSeconds(1));

        assertEquals(result.getState(), ResourceLedger.State.CLOSE_FAILED);
        assertSame(result.getCloseFailure(), closeFailure);
        assertEquals(ledger.size(), 1);
        expectThrows(IllegalStateException.class, registration::close);
    }

    @Test(groups = {"unit", "runtime"})
    public void testDuplicateIdentityAndCrossRuntimeOwnerFailClosed() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ResourceLedger.Registration first = ledger.registerRuntime("shared-client", null, null);

        expectThrows(IllegalStateException.class,
            () -> ledger.registerRuntime("shared-client", null, null));
        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerOwned("wrong-runtime", ResourceLedger.Scope.SESSION,
                new ExecutionOwner("runtime-b", "session-b", 1), null, null));

        first.requestClose();
        first.markOwnerQuiescent();
        try {
            assertTrue(first.drainAndClose(Duration.ofSeconds(1)).isClosed());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            fail("Unexpected interruption", interrupted);
        }
        first.close();
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void testRejectsInvalidLedgerAndRegistrationInputs() {
        expectThrows(IllegalArgumentException.class, () -> new ResourceLedger(null));
        expectThrows(IllegalArgumentException.class, () -> new ResourceLedger("  "));

        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerRuntime(null, null, null));
        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerOwned("", ResourceLedger.Scope.SESSION, owner, null, null));
        expectThrows(NullPointerException.class,
            () -> ledger.registerOwned("null-scope", null, owner, null, null));
        expectThrows(NullPointerException.class,
            () -> ledger.registerOwned("missing-owner", ResourceLedger.Scope.SESSION, null, null, null));
        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerOwned("runtime-scope", ResourceLedger.Scope.RUNTIME, owner, null, null));
    }

    @Test(groups = {"unit", "runtime"})
    public void testDrainBudgetValidationAndRepeatedSuccessfulDrain() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "runtime-client", null, closeCount::incrementAndGet);

        expectThrows(NullPointerException.class, () -> registration.drainAndClose(null));
        expectThrows(IllegalArgumentException.class,
            () -> registration.drainAndClose(Duration.ofNanos(-1)));
        expectThrows(IllegalArgumentException.class,
            () -> registration.drainAndClose(Duration.ofSeconds(Long.MAX_VALUE)));

        registration.abort();
        registration.abort();
        registration.requestClose();
        registration.markOwnerQuiescent();
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        registration.abort();
        registration.close();
        registration.close();
        assertEquals(closeCount.get(), 1);
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void testDrainWaitsForInProgressAbortAction() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        CountDownLatch abortEntered = new CountDownLatch(1);
        CountDownLatch releaseAbort = new CountDownLatch(1);
        AtomicInteger closeCount = new AtomicInteger();
        AtomicReference<Throwable> abortFailure = new AtomicReference<>();
        ResourceLedger.Registration registration = ledger.registerOwned(
            "ftp/operation", ResourceLedger.Scope.OPERATION,
            new ExecutionOwner("runtime-a", "session-a", 1),
            () -> {
                abortEntered.countDown();
                releaseAbort.await();
            }, closeCount::incrementAndGet);
        registration.requestClose();
        registration.markOwnerQuiescent();

        Thread aborter = new Thread(() -> {
            try {
                registration.abort();
            } catch (Throwable failure) {
                abortFailure.set(failure);
            }
        });
        aborter.start();
        try {
            assertTrue(abortEntered.await(5, TimeUnit.SECONDS));
            ResourceLedger.DrainResult whileAborting = registration.drainAndClose(Duration.ZERO);
            assertEquals(whileAborting.getState(), ResourceLedger.State.QUARANTINED);
            assertEquals(closeCount.get(), 0);
        } finally {
            releaseAbort.countDown();
        }
        aborter.join(5000);

        assertFalse(aborter.isAlive());
        assertNull(abortFailure.get());
        assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertEquals(closeCount.get(), 1);
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testConcurrentDrainInvokesCloseOnceAndDoesNotAbortDuringClose() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        CountDownLatch closeEntered = new CountDownLatch(1);
        CountDownLatch releaseClose = new CountDownLatch(1);
        AtomicInteger abortCount = new AtomicInteger();
        AtomicInteger closeCount = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "runtime-client",
            abortCount::incrementAndGet,
            () -> {
                closeCount.incrementAndGet();
                closeEntered.countDown();
                releaseClose.await();
            });
        registration.requestClose();
        registration.markOwnerQuiescent();

        Thread closer = new Thread(() -> {
            try {
                registration.drainAndClose(Duration.ofSeconds(5));
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        });
        closer.start();
        try {
            assertTrue(closeEntered.await(5, TimeUnit.SECONDS));
            assertEquals(registration.getState(), ResourceLedger.State.DRAINING);
            registration.abort();
            assertEquals(abortCount.get(), 0);
        } finally {
            releaseClose.countDown();
        }
        closer.join(5000);

        assertFalse(closer.isAlive());
        assertNull(failure.get());
        assertEquals(closeCount.get(), 1);
        assertEquals(registration.getState(), ResourceLedger.State.CLOSED);
        registration.close();
    }

    private static <T extends Throwable> T expectThrows(Class<T> expectedType,
                                                              ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expectedType.isInstance(failure)) {
                return expectedType.cast(failure);
            }
            throw new AssertionError("Unexpected exception type: " + failure.getClass(), failure);
        }
        throw new AssertionError("Expected exception of type " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test(groups = {"unit", "runtime"})
    public void testStaleRegistrationTokenCannotRetireReplacement() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ResourceLedger.Registration old = ledger.registerRuntime("client", null, null);
        old.requestClose();
        old.markOwnerQuiescent();
        assertTrue(old.drainAndClose(Duration.ofSeconds(1)).isClosed());
        old.close();

        ResourceLedger.Registration replacement = ledger.registerRuntime("client", null, null);
        old.close();
        assertEquals(ledger.size(), 1);
        assertEquals(replacement.getState(), ResourceLedger.State.OPEN);
    }
}
