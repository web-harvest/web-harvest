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
import org.webharvest.runtime.registry.RegistryGenerationId;
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
        ResourceLedger.Lease lease = registration.acquire(owner);

        registration.requestClose();
        assertEquals(registration.getState(), ResourceLedger.State.CLOSE_REQUESTED);
        expectThrows(IllegalStateException.class, () -> registration.acquire(owner));

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
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
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
                acquired.set(registration.acquire(owner));
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
        expectThrows(IllegalStateException.class, () -> registration.acquire(owner));

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
        expectThrows(IllegalStateException.class, () -> registration.acquire(owner));
    }

    @Test(groups = {"unit", "runtime"})
    public void testOwnerQuiescenceIsRequiredEvenWhenNoLeaseIsActive() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerOwned(
            "browser/session-a", ResourceLedger.Scope.SESSION,
            new ExecutionOwner("runtime-a", "session-a", 1), null, closeCount::incrementAndGet);

        ResourceLedger.DrainResult timedOut = registration.drainAndClose(Duration.ZERO);
        assertEquals(timedOut.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(closeCount.get(), 0);

        registration.markOwnerQuiescent();
        expectThrows(IllegalStateException.class, () -> registration.acquire(owner));
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

        registration.requestClose();
        registration.abort();
        registration.abort();
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
    public void testAbortRequiresCloseAdmissionToBeStopped() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        AtomicInteger abortCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "runtime-client", abortCount::incrementAndGet, null);

        expectThrows(IllegalStateException.class, registration::abort);
        assertEquals(abortCount.get(), 0);
        assertEquals(registration.getState(), ResourceLedger.State.OPEN);

        registration.requestClose();
        registration.abort();
        registration.abort();
        assertEquals(abortCount.get(), 1);
    }

    @Test(groups = {"unit", "runtime"})
    public void testAbortActionRestoresInterruptStatus() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ResourceLedger.Registration registration = ledger.registerRuntime(
            "interruptible-client", () -> {
                throw new InterruptedException("abort interrupted");
            }, null);
        AtomicReference<Boolean> interrupted = new AtomicReference<>(false);
        registration.requestClose();

        Thread aborter = new Thread(() -> {
            registration.abort();
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        aborter.start();
        aborter.join(5000);

        assertFalse(aborter.isAlive());
        assertTrue(interrupted.get());
        assertTrue(registration.getAbortFailure() instanceof InterruptedException);
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
            assertTrue(whileAborting.isAbortInProgress());
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

    @Test(groups = {"unit", "runtime"})
    public void testGenerationResourcesRequireExactPinnedGenerationAndDrainAllSessions()
        throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId generationOne = new RegistryGenerationId("runtime-a", "snapshot-a");
        RegistryGenerationId generationTwo = new RegistryGenerationId("runtime-a", "snapshot-b");
        ExecutionOwner ownerA = new ExecutionOwner("runtime-a", "session-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner("runtime-a", "session-b", 2);
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration registration = ledger.registerGeneration(
            "provider-loader/generation-a", generationOne, null, closeCount::incrementAndGet);
        ResourceLedger.GenerationPin pinA = ledger.pinGeneration(ownerA, generationOne);
        ResourceLedger.GenerationPin pinB = ledger.pinGeneration(ownerB, generationOne);

        assertEquals(registration.getScope(), ResourceLedger.Scope.GENERATION);
        assertEquals(registration.getRegistryGenerationId(), generationOne);
        assertNull(registration.getExecutionOwner());
        expectThrows(IllegalStateException.class, () -> registration.acquire(ownerA));
        expectThrows(IllegalArgumentException.class, () -> registration.acquire(ownerA, generationTwo));
        expectThrows(IllegalArgumentException.class,
            () -> ledger.pinGeneration(new ExecutionOwner("runtime-b", "session-a", 1), generationOne));
        expectThrows(IllegalStateException.class, () -> ledger.pinGeneration(ownerA, generationOne));

        ResourceLedger.Lease leaseA = registration.acquire(ownerA, generationOne);
        ResourceLedger.Lease leaseB = registration.acquire(ownerB, generationOne);
        registration.requestClose();
        assertEquals(registration.getState(), ResourceLedger.State.CLOSE_REQUESTED);
        expectThrows(IllegalStateException.class,
            () -> registration.markOwnerQuiescent());
        expectThrows(IllegalStateException.class,
            () -> ledger.pinGeneration(new ExecutionOwner("runtime-a", "session-c", 3), generationOne));

        ResourceLedger.DrainResult blocked = registration.drainAndClose(Duration.ZERO);
        assertEquals(blocked.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(blocked.getActiveLeases(), 2);
        assertEquals(blocked.getActiveGenerationPins(), 2);
        assertFalse(blocked.isOwnerQuiescent());
        assertEquals(ledger.getActiveGenerationPinCount(generationOne), 2);
        assertEquals(closeCount.get(), 0);

        leaseA.close();
        ResourceLedger.DrainResult oneLeaseRemaining = registration.drainAndClose(Duration.ZERO);
        assertEquals(oneLeaseRemaining.getActiveLeases(), 1);
        assertEquals(closeCount.get(), 0);

        leaseB.close();
        pinA.close();
        assertEquals(ledger.getActiveGenerationPinCount(generationOne), 1);
        assertFalse(registration.drainAndClose(Duration.ZERO).isClosed());
        pinB.close();

        ResourceLedger.DrainResult closed = registration.drainAndClose(Duration.ofSeconds(1));
        assertTrue(closed.isClosed());
        assertTrue(closed.isOwnerQuiescent());
        assertEquals(closed.getActiveGenerationPins(), 0);
        assertEquals(closeCount.get(), 1);
        assertEquals(ledger.getActiveGenerationPinCount(generationOne), 0);
        registration.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testPinnedGenerationCanAcquireAfterResourceCloseRequestAndQuarantine()
        throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId generation = new RegistryGenerationId("runtime-a", "snapshot-close-requested");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-pinned", 1);
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration resource = ledger.registerGeneration(
            "provider-loader", generation, null, closeCount::incrementAndGet);
        ResourceLedger.GenerationPin pin = ledger.pinGeneration(owner, generation);

        resource.requestClose();
        assertEquals(resource.getState(), ResourceLedger.State.CLOSE_REQUESTED);

        ResourceLedger.DrainResult quarantined = resource.drainAndClose(Duration.ZERO);
        assertEquals(quarantined.getState(), ResourceLedger.State.QUARANTINED);
        assertFalse(quarantined.isOwnerQuiescent());
        assertEquals(closeCount.get(), 0);

        // Retirement rejects new generation pins, but an execution already
        // pinned to this exact generation must be able to finish its work.
        ResourceLedger.Lease lease = resource.acquire(owner, generation);
        assertEquals(resource.getActiveLeaseCount(), 1);
        lease.close();

        pin.close();
        assertTrue(resource.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertEquals(closeCount.get(), 1);
        resource.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testGenerationResourceStaysOpenForPinnedSessionWithoutResourceLease()
        throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId generation = new RegistryGenerationId("runtime-a", "snapshot-shared");
        ExecutionOwner ownerA = new ExecutionOwner("runtime-a", "session-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner("runtime-a", "session-b", 2);
        AtomicInteger closeCount = new AtomicInteger();
        ResourceLedger.Registration resource = ledger.registerGeneration(
            "provider-loader", generation, null, closeCount::incrementAndGet);
        ResourceLedger.GenerationPin pinA = ledger.pinGeneration(ownerA, generation);
        ResourceLedger.GenerationPin pinB = ledger.pinGeneration(ownerB, generation);
        ResourceLedger.Lease leaseA = resource.acquire(ownerA, generation);

        ledger.beginGenerationRetirement(generation);
        expectThrows(IllegalStateException.class,
            () -> ledger.pinGeneration(new ExecutionOwner("runtime-a", "session-c", 3), generation));
        expectThrows(IllegalStateException.class,
            () -> ledger.registerGeneration("late-resource", generation, null, null));

        ResourceLedger.Lease leaseDuringRetirement = resource.acquire(ownerA, generation);
        assertEquals(resource.getState(), ResourceLedger.State.OPEN);
        leaseDuringRetirement.close();
        assertFalse(resource.drainAndClose(Duration.ZERO).isClosed());
        assertEquals(resource.getState(), ResourceLedger.State.QUARANTINED);
        assertEquals(closeCount.get(), 0);

        leaseA.close();
        pinA.close();
        assertEquals(ledger.getActiveGenerationPinCount(generation), 1);
        assertFalse(resource.drainAndClose(Duration.ZERO).isClosed());
        assertEquals(closeCount.get(), 0,
            "A resource must remain open while any execution still pins its generation");

        pinB.close();
        assertTrue(resource.drainAndClose(Duration.ofSeconds(1)).isClosed());
        assertEquals(closeCount.get(), 1);
        resource.close();
    }

    @Test(groups = {"unit", "runtime"})
    public void testGenerationPinAdmissionIsAtomicWithRetirement() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId generation = new RegistryGenerationId("runtime-a", "snapshot-race");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-race", 1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<ResourceLedger.GenerationPin> pin = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread pinner = new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                pin.set(ledger.pinGeneration(owner, generation));
            } catch (IllegalStateException retirementWon) {
                // Retirement may win the same generation monitor.
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        });
        Thread retirer = new Thread(() -> {
            ready.countDown();
            try {
                start.await();
                ledger.beginGenerationRetirement(generation);
            } catch (Throwable unexpected) {
                failure.set(unexpected);
            }
        });

        pinner.start();
        retirer.start();
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pinner.join(5000);
        retirer.join(5000);

        assertFalse(pinner.isAlive());
        assertFalse(retirer.isAlive());
        assertNull(failure.get());
        expectThrows(IllegalStateException.class,
            () -> ledger.pinGeneration(new ExecutionOwner("runtime-a", "late-session", 2), generation));

        if (pin.get() != null) {
            assertEquals(ledger.getActiveGenerationPinCount(generation), 1);
            pin.get().close();
        }
        assertEquals(ledger.getActiveGenerationPinCount(generation), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void testSameLocalResourceIdCanCoexistAcrossOwnersAndGenerations()
        throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId generationOne = new RegistryGenerationId("runtime-a", "publication-1");
        RegistryGenerationId generationTwo = new RegistryGenerationId("runtime-a", "publication-2");
        ExecutionOwner ownerA = new ExecutionOwner("runtime-a", "session-a", 1);
        ExecutionOwner ownerB = new ExecutionOwner("runtime-a", "session-b", 2);

        ResourceLedger.Registration generationResourceOne =
            ledger.registerGeneration("provider-loader", generationOne, null, null);
        ResourceLedger.Registration generationResourceTwo =
            ledger.registerGeneration("provider-loader", generationTwo, null, null);
        ResourceLedger.Registration sessionResourceA =
            ledger.registerOwned("jdbc/pool", ResourceLedger.Scope.SESSION, ownerA, null, null);
        ResourceLedger.Registration sessionResourceB =
            ledger.registerOwned("jdbc/pool", ResourceLedger.Scope.SESSION, ownerB, null, null);

        assertEquals(ledger.size(), 4);
        expectThrows(IllegalStateException.class,
            () -> ledger.registerGeneration("provider-loader", generationOne, null, null));
        expectThrows(IllegalStateException.class,
            () -> ledger.registerOwned("jdbc/pool", ResourceLedger.Scope.SESSION, ownerA, null, null));

        for (ResourceLedger.Registration registration : new ResourceLedger.Registration[] {
            generationResourceOne, generationResourceTwo
        }) {
            registration.requestClose();
            assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
            registration.close();
        }
        for (ResourceLedger.Registration registration : new ResourceLedger.Registration[] {
            sessionResourceA, sessionResourceB
        }) {
            registration.markOwnerQuiescent();
            assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
            registration.close();
        }
        assertEquals(ledger.size(), 0);
    }

    @Test(groups = {"unit", "runtime"})
    public void testRegistryGenerationIdHasRuntimeScopedValueSemantics() {
        RegistryGenerationId first = new RegistryGenerationId("runtime-a", "snapshot-a");
        RegistryGenerationId same = new RegistryGenerationId("runtime-a", "snapshot-a");
        RegistryGenerationId otherRuntime = new RegistryGenerationId("runtime-b", "snapshot-a");
        RegistryGenerationId otherSnapshot = new RegistryGenerationId("runtime-a", "snapshot-b");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertFalse(first.equals(otherRuntime));
        assertFalse(first.equals(otherSnapshot));
        assertEquals(first.getRuntimeId(), "runtime-a");
        assertEquals(first.getValue(), "snapshot-a");
        assertTrue(first.toString().contains("snapshot-a"));

        expectThrows(IllegalArgumentException.class, () -> new RegistryGenerationId(null, "snapshot"));
        expectThrows(IllegalArgumentException.class, () -> new RegistryGenerationId("runtime-a", " "));
    }

    @Test(groups = {"unit", "runtime"})
    public void testGenerationRegistrationRejectsForeignRuntimeAndWrongRegistrationPath() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        RegistryGenerationId foreign = new RegistryGenerationId("runtime-b", "snapshot-b");
        RegistryGenerationId local = new RegistryGenerationId("runtime-a", "snapshot-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-a", 1);

        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerGeneration("foreign-generation", foreign, null, null));
        expectThrows(IllegalArgumentException.class,
            () -> ledger.registerOwned("generation-via-execution-owner", ResourceLedger.Scope.GENERATION,
                owner, null, null));
        expectThrows(NullPointerException.class,
            () -> ledger.registerGeneration("missing-generation", null, null, null));

        ResourceLedger.Registration registration =
            ledger.registerGeneration("local-generation", local, null, null);
        assertEquals(registration.getRegistryGenerationId(), local);
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
    public void asyncAbortDoesNotBlockCallerAndRetainsResourceUntilCallbackActuallyReturns() throws Exception {
        java.util.concurrent.ThreadPoolExecutor dispatcher = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(2),
            runnable -> {
                Thread thread = new Thread(runnable, "resource-ledger-abort-test");
                thread.setDaemon(true);
                return thread;
            }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch dispatchReturned = new CountDownLatch(1);
        AtomicReference<Throwable> dispatchFailure = new AtomicReference<>();
        try {
            ResourceLedger ledger = new ResourceLedger("runtime-a", dispatcher);
            CountDownLatch entered = new CountDownLatch(1);
            AtomicInteger closes = new AtomicInteger();
            ResourceLedger.Registration registration = ledger.registerRuntime(
                "blocked-abort", () -> {
                    entered.countDown();
                    release.await();
                }, closes::incrementAndGet);

            registration.requestClose();
            Thread caller = new Thread(() -> {
                try {
                    registration.abort();
                } catch (Throwable failure) {
                    dispatchFailure.set(failure);
                } finally {
                    dispatchReturned.countDown();
                }
            }, "resource-ledger-abort-caller");
            caller.start();
            assertTrue(dispatchReturned.await(2, TimeUnit.SECONDS),
                "Abort admission must return while provider callback remains blocked");
            assertNull(dispatchFailure.get());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(registration.getAbortState(), ResourceLedger.AbortState.RUNNING);

            registration.markOwnerQuiescent();
            ResourceLedger.DrainResult blocked = registration.drainAndClose(Duration.ZERO);
            assertTrue(blocked.isAbortInProgress());
            assertEquals(blocked.getAbortState(), ResourceLedger.AbortState.RUNNING);
            assertEquals(closes.get(), 0, "Close must not race a running abort callback");

            release.countDown();
            caller.join(5000);
            assertFalse(caller.isAlive());
            dispatcher.shutdown();
            assertTrue(dispatcher.awaitTermination(5, TimeUnit.SECONDS));
            assertEquals(registration.getAbortState(), ResourceLedger.AbortState.SUCCEEDED);
            assertTrue(registration.drainAndClose(Duration.ofSeconds(1)).isClosed());
            assertEquals(closes.get(), 1);
            registration.close();
        } finally {
            release.countDown();
            dispatcher.shutdownNow();
        }
    }

    @Test(groups = {"unit", "runtime"})
    public void asyncAbortDispatcherRejectionIsDistinctAndDoesNotPretendCallbackRan() throws Exception {
        java.util.concurrent.ThreadPoolExecutor dispatcher = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1),
            runnable -> {
                Thread thread = new Thread(runnable, "resource-ledger-rejection-test");
                thread.setDaemon(true);
                return thread;
            }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            ResourceLedger ledger = new ResourceLedger("runtime-a", dispatcher);
            ResourceLedger.Registration running = ledger.registerRuntime("running", () -> {
                entered.countDown();
                release.await();
            }, null);
            ResourceLedger.Registration queued = ledger.registerRuntime("queued", () -> { }, null);
            AtomicInteger rejectedCallbackRuns = new AtomicInteger();
            ResourceLedger.Registration rejected = ledger.registerRuntime("rejected",
                rejectedCallbackRuns::incrementAndGet, null);

            running.requestClose();
            queued.requestClose();
            rejected.requestClose();
            running.abort();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            queued.abort();
            rejected.abort();

            assertEquals(rejected.getAbortState(), ResourceLedger.AbortState.REJECTED);
            assertTrue(rejected.getAbortFailure() instanceof java.util.concurrent.RejectedExecutionException);
            assertEquals(rejectedCallbackRuns.get(), 0);
            assertEquals(running.getAbortState(), ResourceLedger.AbortState.RUNNING);
            release.countDown();
            dispatcher.shutdown();
            assertTrue(dispatcher.awaitTermination(5, TimeUnit.SECONDS));
            assertEquals(queued.getAbortState(), ResourceLedger.AbortState.SUCCEEDED);
            assertEquals(rejected.getAbortState(), ResourceLedger.AbortState.REJECTED);
        } finally {
            release.countDown();
            dispatcher.shutdownNow();
        }
    }


    @Test(groups = {"unit", "runtime"})
    public void asyncAbortCallbackFailureIsRecordedOnlyAfterCallbackCompletion() throws Exception {
        java.util.concurrent.ThreadPoolExecutor dispatcher = new java.util.concurrent.ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new java.util.concurrent.ArrayBlockingQueue<>(1),
            runnable -> {
                Thread thread = new Thread(runnable, "resource-ledger-failure-test");
                thread.setDaemon(true);
                return thread;
            }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        IllegalStateException expected = new IllegalStateException("provider abort failed");
        try {
            ResourceLedger ledger = new ResourceLedger("runtime-a", dispatcher);
            ResourceLedger.Registration registration = ledger.registerRuntime("failed-abort", () -> {
                entered.countDown();
                release.await();
                throw expected;
            }, null);
            registration.requestClose();
            registration.abort();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertEquals(registration.getAbortState(), ResourceLedger.AbortState.RUNNING);
            assertNull(registration.getAbortFailure());
            release.countDown();
            dispatcher.shutdown();
            assertTrue(dispatcher.awaitTermination(5, TimeUnit.SECONDS));
            assertEquals(registration.getAbortState(), ResourceLedger.AbortState.FAILED);
            assertSame(registration.getAbortFailure(), expected);
        } finally {
            release.countDown();
            dispatcher.shutdownNow();
        }
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

    @Test(groups = {"unit", "runtime"})
    public void testLeaseAndGenerationPinCloseAreIdempotent() throws InterruptedException {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ExecutionOwner owner = new ExecutionOwner("runtime-a", "session-idempotent", 1);
        RegistryGenerationId generation = new RegistryGenerationId("runtime-a", "generation-idempotent");
        ResourceLedger.Registration resource = ledger.registerGeneration(
            "provider-loader", generation, null, null);
        ResourceLedger.GenerationPin pin = ledger.pinGeneration(owner, generation);
        ResourceLedger.Lease lease = resource.acquire(owner, generation);

        lease.close();
        lease.close();
        assertEquals(resource.getActiveLeaseCount(), 0,
            "Closing the same lease token twice must release exactly one lease");

        pin.close();
        pin.close();
        assertEquals(ledger.getActiveGenerationPinCount(generation), 0,
            "Closing the same generation pin twice must release exactly one pin");

        assertTrue(resource.drainAndClose(Duration.ofSeconds(1)).isClosed());
        resource.close();
        assertEquals(ledger.size(), 0);
    }
}
