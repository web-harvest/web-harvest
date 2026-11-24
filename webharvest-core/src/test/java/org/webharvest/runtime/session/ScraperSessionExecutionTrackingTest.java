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

package org.webharvest.runtime.session;

import com.google.common.eventbus.EventBus;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/** Tests the separation between session outcome and worker execution state. */
public class ScraperSessionExecutionTrackingTest {

    private ScraperSessionImpl createSession() {
        return new ScraperSessionImpl("execution-test", null,
            Mockito.mock(org.webharvest.definition.Config.class), new EventBus());
    }

    @Test(groups = {"unit", "session"})
    public void testUnsubmittedExecutionIsAlreadyQuiescent() throws InterruptedException {
        ExecutionTrackedSession session = createSession();

        assertEquals(session.getExecutionState(), ExecutionState.NOT_SUBMITTED);
        assertEquals(session.getTerminationDisposition(), TerminationDisposition.NOT_REQUESTED);
        session.awaitExecutionCompletion();
        assertTrue(session.awaitExecutionCompletion(0));
    }

    @Test(groups = {"unit", "session"})
    public void testQueuedExecutionRequiresObservedExit() throws InterruptedException {
        ScraperSessionImpl session = createSession();

        session.markExecutionQueued();
        assertEquals(session.getExecutionState(), ExecutionState.QUEUED);
        assertFalse(session.awaitExecutionCompletion(0));
        assertTrue(session.markExecutionStarted());
        assertFalse(session.markExecutionStarted());

        session.markExecutionInterruptRequested();
        assertEquals(session.getExecutionState(), ExecutionState.INTERRUPT_REQUESTED);
        session.markExecutionUnresponsive();
        assertEquals(session.getExecutionState(), ExecutionState.UNRESPONSIVE);
        assertFalse(session.awaitExecutionCompletion(0));

        session.markExecutionExited();
        session.awaitExecutionCompletion();
        assertTrue(session.awaitExecutionCompletion(0));
        session.markExecutionInterruptRequested();
        session.markExecutionUnresponsive();
        assertEquals(session.getExecutionState(), ExecutionState.EXITED);
    }

    @Test(groups = {"unit", "session"})
    public void testCancelledSessionCannotEnterExecutionAndLateHandlerIsInvoked() {
        ScraperSessionImpl session = createSession();
        AtomicInteger cancellations = new AtomicInteger();

        assertTrue(session.cancel());
        assertEquals(session.getTerminationDisposition(), TerminationDisposition.CANCELLED_BEFORE_START);
        session.attachCancellationHandler(cancellations::incrementAndGet);
        assertEquals(cancellations.get(), 1);

        session.markExecutionQueued();
        assertFalse(session.markExecutionStarted());
        assertEquals(session.getExecutionState(), ExecutionState.QUEUED);
        assertFalse(session.cancel());
        session.markExecutionExited();
        assertEquals(session.getTerminationDisposition(), TerminationDisposition.CANCELLED_BEFORE_START);
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testMarkRunningRejectsNullContext() {
        createSession().markRunning(null);
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testAttachNullCancellationHandlerRejected() {
        createSession().attachCancellationHandler(null);
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalStateException.class)
    public void testDuplicateCancellationHandlerRejected() {
        ScraperSessionImpl session = createSession();
        session.attachCancellationHandler(() -> { });
        session.attachCancellationHandler(() -> { });
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalStateException.class)
    public void testDuplicateExecutionQueueRejected() {
        ScraperSessionImpl session = createSession();
        session.markExecutionQueued();
        session.markExecutionQueued();
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testNegativeExecutionWaitRejected() throws InterruptedException {
        createSession().awaitExecutionCompletion(-1);
    }

    @Test(groups = {"unit", "session"}, expectedExceptions = IllegalArgumentException.class)
    public void testNullEventBusRejected() {
        new ScraperSessionImpl("execution-test", null,
            Mockito.mock(org.webharvest.definition.Config.class), null);
    }
}
