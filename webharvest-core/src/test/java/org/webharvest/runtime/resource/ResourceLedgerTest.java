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
import java.util.concurrent.atomic.AtomicInteger;

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
        assertThrows(IllegalStateException.class, registration::acquire);

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
        assertThrows(IllegalStateException.class, registration::close);
    }

    @Test(groups = {"unit", "runtime"})
    public void testDuplicateIdentityAndCrossRuntimeOwnerFailClosed() {
        ResourceLedger ledger = new ResourceLedger("runtime-a");
        ResourceLedger.Registration first = ledger.registerRuntime("shared-client", null, null);

        assertThrows(IllegalStateException.class,
            () -> ledger.registerRuntime("shared-client", null, null));
        assertThrows(IllegalArgumentException.class,
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
