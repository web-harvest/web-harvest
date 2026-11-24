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

import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runtime-owned registry for resources whose closure must be coordinated with
 * execution quiescence and active-use leases.
 *
 * <p>A registration is not a lease. Registration establishes ownership;
 * {@link Registration#acquire(ExecutionOwner)} grants one attributed bounded use; and
 * {@link Registration#drainAndClose(Duration)} closes only after the owner
 * has independently reported quiescence and every use lease has been released.</p>
 *
 * <p>For RUNTIME scope the ledger's runtime ID is the owner identity and the
 * session execution owner is absent. SESSION and OPERATION resources require
 * an {@link ExecutionOwner} belonging to this ledger's runtime. Operation
 * identity is represented by the resource ID within that execution owner.</p>
 *
 * <p>This class does not infer worker exit from cancellation, interruption,
 * EventBus delivery or a terminal business status. The caller must invoke
 * {@link Registration#markOwnerQuiescent()} only after the execution
 * coordinator has observed the appropriate quiescence boundary.</p>
 */
public final class ResourceLedger {

    public enum Scope {
        RUNTIME,
        SESSION,
        OPERATION
    }

    public enum State {
        OPEN,
        CLOSE_REQUESTED,
        DRAINING,
        CLOSED,
        CLOSE_FAILED,
        QUARANTINED
    }

    @FunctionalInterface
    public interface AbortAction {
        void abort() throws Exception;
    }

    @FunctionalInterface
    public interface CloseAction {
        void close() throws Exception;
    }

    private final String runtimeId;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public ResourceLedger(String runtimeId) {
        if (runtimeId == null || runtimeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Runtime ID cannot be null or empty");
        }
        this.runtimeId = runtimeId;
    }

    public String getRuntimeId() {
        return runtimeId;
    }

    /**
     * Registers a runtime-scoped resource. The runtime ID is its owner identity.
     */
    public Registration registerRuntime(String resourceId, AbortAction abortAction,
                                         CloseAction closeAction) {
        return register(resourceId, Scope.RUNTIME, null, abortAction, closeAction);
    }

    /**
     * Registers a session- or operation-scoped resource against an immutable
     * execution owner. Runtime mismatches fail closed.
     */
    public Registration registerOwned(String resourceId, Scope scope, ExecutionOwner owner,
                                       AbortAction abortAction, CloseAction closeAction) {
        if (scope == Scope.RUNTIME) {
            throw new IllegalArgumentException("Use registerRuntime for runtime-scoped resources");
        }
        return register(resourceId, scope, owner, abortAction, closeAction);
    }

    private Registration register(String resourceId, Scope scope, ExecutionOwner owner,
                                  AbortAction abortAction, CloseAction closeAction) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }
        Objects.requireNonNull(scope, "Scope cannot be null");
        if (scope == Scope.RUNTIME) {
            if (owner != null) {
                throw new IllegalArgumentException("Runtime-scoped resources use the ledger runtime owner");
            }
        } else {
            Objects.requireNonNull(owner, "Execution owner cannot be null");
            if (!runtimeId.equals(owner.getRuntimeId())) {
                throw new IllegalArgumentException("Execution owner belongs to another runtime");
            }
        }

        Entry entry = new Entry(resourceId, scope, owner, abortAction, closeAction);
        Entry existing = entries.putIfAbsent(resourceId, entry);
        if (existing != null) {
            throw new IllegalStateException("Resource ID is already registered: " + resourceId);
        }
        return new Registration(this, entry);
    }

    /** Returns the number of retained registrations, including quarantined and failed closes. */
    public int size() {
        return entries.size();
    }

    private void retire(Entry entry) {
        synchronized (entry) {
            if (entry.state != State.CLOSED) {
                throw new IllegalStateException(
                    "Only successfully closed resources may be retired: " + entry.resourceId
                        + " (" + entry.state + ')');
            }
            if (entry.registrationRetired) {
                return;
            }
            entry.registrationRetired = true;
        }
        entries.remove(entry.resourceId, entry);
    }

    private static final class Entry {
        private final String resourceId;
        private final Scope scope;
        private final ExecutionOwner owner;
        private final AbortAction abortAction;
        private final CloseAction closeAction;
        private State state = State.OPEN;
        private final Map<Long, ExecutionOwner> activeLeases = new HashMap<>();
        private final AtomicLong nextLeaseId = new AtomicLong();
        private boolean ownerQuiescent;
        private boolean abortInvoked;
        private boolean abortInProgress;
        private boolean registrationRetired;
        private Throwable abortFailure;
        private Throwable closeFailure;

        private Entry(String resourceId, Scope scope, ExecutionOwner owner,
                      AbortAction abortAction, CloseAction closeAction) {
            this.resourceId = resourceId;
            this.scope = scope;
            this.owner = owner;
            this.abortAction = abortAction;
            this.closeAction = closeAction;
        }
    }

    public static final class Registration implements AutoCloseable {
        private final ResourceLedger ledger;
        private final Entry entry;

        private Registration(ResourceLedger ledger, Entry entry) {
            this.ledger = ledger;
            this.entry = entry;
        }

        public String getResourceId() {
            return entry.resourceId;
        }

        public String getRuntimeId() {
            return ledger.runtimeId;
        }

        public Scope getScope() {
            return entry.scope;
        }

        /** Null only for RUNTIME scope, whose owner is the ledger runtime ID. */
        public ExecutionOwner getExecutionOwner() {
            return entry.owner;
        }

        public State getState() {
            synchronized (entry) {
                return entry.state;
            }
        }

        public int getActiveLeaseCount() {
            synchronized (entry) {
                return entry.activeLeases.size();
            }
        }

        public boolean isOwnerQuiescent() {
            synchronized (entry) {
                return entry.ownerQuiescent;
            }
        }

        public Throwable getAbortFailure() {
            synchronized (entry) {
                return entry.abortFailure;
            }
        }

        public Throwable getCloseFailure() {
            synchronized (entry) {
                return entry.closeFailure;
            }
        }

        /**
         * Acquires a use lease attributed to the actual execution owner.
         * Every scope requires explicit attribution: a session-scoped
         * registration must not silently attribute a foreign caller to its owner.
         * The state check and lease insertion share the same monitor as close
         * admission, so no lease can escape CLOSE_REQUESTED.
         */
        /**
         * Acquires a lease for an explicit execution owner. Runtime-scoped
         * resources use this overload so every in-flight consumer is reportable.
         */
        public Lease acquire(ExecutionOwner useOwner) {
            Objects.requireNonNull(useOwner, "Lease execution owner cannot be null");
            if (!ledger.runtimeId.equals(useOwner.getRuntimeId())) {
                throw new IllegalArgumentException("Lease owner belongs to another runtime");
            }
            if (entry.scope != Scope.RUNTIME && !entry.owner.equals(useOwner)) {
                throw new IllegalArgumentException("Lease owner does not match resource owner");
            }
            synchronized (entry) {
                if (entry.state != State.OPEN) {
                    throw new IllegalStateException(
                        "Resource no longer admits use: " + entry.resourceId + " (" + entry.state + ')');
                }
                long leaseId = entry.nextLeaseId.incrementAndGet();
                entry.activeLeases.put(leaseId, useOwner);
                return new Lease(entry, leaseId, useOwner);
            }
        }

        /** Atomically prevents new leases; it does not abort or close the resource. */
        public void requestClose() {
            synchronized (entry) {
                if (entry.state == State.OPEN) {
                    entry.state = State.CLOSE_REQUESTED;
                }
                entry.notifyAll();
            }
        }

        /**
         * Requests best-effort interruption of active I/O. Abort is independent
         * from close and is invoked at most once; its failure remains observable.
         */
        public void abort() {
            AbortAction action;
            synchronized (entry) {
                if (entry.abortInvoked || entry.state == State.DRAINING
                    || entry.state == State.CLOSED || entry.state == State.CLOSE_FAILED) {
                    return;
                }
                entry.abortInvoked = true;
                action = entry.abortAction;
                if (action != null) {
                    entry.abortInProgress = true;
                }
            }
            if (action == null) {
                return;
            }
            try {
                action.abort();
            } catch (Throwable failure) {
                synchronized (entry) {
                    entry.abortFailure = failure;
                }
                if (failure instanceof Error) {
                    throw (Error) failure;
                }
            } finally {
                synchronized (entry) {
                    entry.abortInProgress = false;
                    entry.notifyAll();
                }
            }
        }

        /**
         * Records externally observed owner quiescence and closes lease admission
         * if it is still open. This must not be called merely because cancellation
         * was requested or business status is terminal.
         */
        public void markOwnerQuiescent() {
            synchronized (entry) {
                if (entry.state == State.OPEN) {
                    entry.state = State.CLOSE_REQUESTED;
                }
                entry.ownerQuiescent = true;
                entry.notifyAll();
            }
        }

        /**
         * Stops admission, waits up to the supplied budget for both owner
         * quiescence and lease drain, then invokes close exactly once.
         *
         * <p>Timeout retains the resource in QUARANTINED state. A later call may
         * complete the drain after the owner and all leases become quiescent.</p>
         */
        public DrainResult drainAndClose(Duration timeout) throws InterruptedException {
            Objects.requireNonNull(timeout, "Timeout cannot be null");
            if (timeout.isNegative()) {
                throw new IllegalArgumentException("Timeout cannot be negative");
            }
            final long timeoutNanos;
            try {
                timeoutNanos = timeout.toNanos();
            } catch (ArithmeticException overflow) {
                throw new IllegalArgumentException("Timeout is too large", overflow);
            }

            requestClose();
            boolean performClose = false;
            long started = System.nanoTime();
            synchronized (entry) {
                if (entry.state == State.CLOSED || entry.state == State.CLOSE_FAILED
                    || entry.state == State.DRAINING) {
                    return snapshot(entry);
                }
                while (!entry.ownerQuiescent || !entry.activeLeases.isEmpty()
                    || entry.abortInProgress) {
                    long remaining = timeoutNanos - (System.nanoTime() - started);
                    if (remaining <= 0) {
                        entry.state = State.QUARANTINED;
                        return snapshot(entry);
                    }
                    TimeUnit.NANOSECONDS.timedWait(entry, remaining);
                }
                entry.state = State.DRAINING;
                performClose = true;
            }

            if (performClose) {
                try {
                    if (entry.closeAction != null) {
                        entry.closeAction.close();
                    }
                    synchronized (entry) {
                        entry.state = State.CLOSED;
                        entry.notifyAll();
                    }
                } catch (Throwable failure) {
                    synchronized (entry) {
                        entry.closeFailure = failure;
                        entry.state = State.CLOSE_FAILED;
                        entry.notifyAll();
                    }
                    if (failure instanceof Error) {
                        throw (Error) failure;
                    }
                }
            }
            synchronized (entry) {
                return snapshot(entry);
            }
        }

        private static DrainResult snapshot(Entry entry) {
            List<OutstandingLease> outstanding = new ArrayList<>();
            for (Map.Entry<Long, ExecutionOwner> lease : entry.activeLeases.entrySet()) {
                outstanding.add(new OutstandingLease(lease.getKey(), lease.getValue()));
            }
            return new DrainResult(entry.state, outstanding, entry.ownerQuiescent,
                entry.abortInProgress, entry.abortFailure, entry.closeFailure);
        }

        /**
         * Retires this exact registration token. A resource that is merely
         * close-requested, quarantined or close-failed remains retained.
         */
        @Override
        public void close() {
            ledger.retire(entry);
        }
    }

    public static final class Lease implements AutoCloseable {
        private final Entry entry;
        private final long leaseId;
        private final ExecutionOwner owner;
        private final AtomicBoolean released = new AtomicBoolean();

        private Lease(Entry entry, long leaseId, ExecutionOwner owner) {
            this.entry = entry;
            this.leaseId = leaseId;
            this.owner = owner;
        }

        public long getLeaseId() {
            return leaseId;
        }

        public ExecutionOwner getOwner() {
            return owner;
        }

        @Override
        public void close() {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            synchronized (entry) {
                if (entry.activeLeases.remove(leaseId) == null) {
                    throw new IllegalStateException("Resource lease is not registered: " + leaseId);
                }
                entry.notifyAll();
            }
        }
    }

    /** Immutable diagnostic identity for a lease still using a resource. */
    public static final class OutstandingLease {
        private final long leaseId;
        private final ExecutionOwner owner;

        private OutstandingLease(long leaseId, ExecutionOwner owner) {
            this.leaseId = leaseId;
            this.owner = owner;
        }

        public long getLeaseId() {
            return leaseId;
        }

        public ExecutionOwner getOwner() {
            return owner;
        }
    }

    public static final class DrainResult {
        private final State state;
        private final List<OutstandingLease> outstandingLeases;
        private final boolean ownerQuiescent;
        private final boolean abortInProgress;
        private final Throwable abortFailure;
        private final Throwable closeFailure;

        private DrainResult(State state, List<OutstandingLease> outstandingLeases,
                            boolean ownerQuiescent, boolean abortInProgress,
                            Throwable abortFailure, Throwable closeFailure) {
            this.state = state;
            this.outstandingLeases = Collections.unmodifiableList(new ArrayList<>(outstandingLeases));
            this.ownerQuiescent = ownerQuiescent;
            this.abortInProgress = abortInProgress;
            this.abortFailure = abortFailure;
            this.closeFailure = closeFailure;
        }

        public State getState() {
            return state;
        }

        public int getActiveLeases() {
            return outstandingLeases.size();
        }

        public List<OutstandingLease> getOutstandingLeases() {
            return outstandingLeases;
        }

        public boolean isOwnerQuiescent() {
            return ownerQuiescent;
        }

        public boolean isAbortInProgress() {
            return abortInProgress;
        }

        public Throwable getAbortFailure() {
            return abortFailure;
        }

        public Throwable getCloseFailure() {
            return closeFailure;
        }

        public boolean isClosed() {
            return state == State.CLOSED;
        }
    }
}
