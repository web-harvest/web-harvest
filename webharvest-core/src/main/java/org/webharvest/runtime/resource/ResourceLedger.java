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

import org.webharvest.runtime.registry.RegistryGenerationId;
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
 * <p>RUNTIME resources are owned by the ledger runtime. GENERATION resources
 * are owned by a distinct {@link RegistryGenerationId}; their leases are
 * attributed to sessions pinned to that exact generation. SESSION and OPERATION
 * resources require an {@link ExecutionOwner} belonging to this runtime.</p>
 *
 * <p>This class does not infer worker exit from cancellation, interruption,
 * EventBus delivery or a terminal business status. The caller must invoke
 * {@link Registration#markOwnerQuiescent()} only after the execution
 * coordinator has observed the appropriate quiescence boundary.</p>
 */
public final class ResourceLedger {

    public enum Scope {
        RUNTIME,
        GENERATION,
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
    private final ConcurrentHashMap<RegistrationKey, Entry> entries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<RegistryGenerationId, GenerationEntry> generations =
        new ConcurrentHashMap<>();

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
        return register(resourceId, Scope.RUNTIME, null, null, abortAction, closeAction);
    }

    /**
     * Registers a generation-scoped resource against the exact immutable
     * registry generation. The generation belongs to this ledger's runtime.
     */
    public Registration registerGeneration(String resourceId, RegistryGenerationId generationId,
                                           AbortAction abortAction, CloseAction closeAction) {
        return register(resourceId, Scope.GENERATION, null, generationId, abortAction, closeAction);
    }

    /**
     * Registers a session- or operation-scoped resource against an immutable
     * execution owner. Runtime mismatches fail closed.
     */
    public Registration registerOwned(String resourceId, Scope scope, ExecutionOwner owner,
                                       AbortAction abortAction, CloseAction closeAction) {
        if (scope == Scope.RUNTIME || scope == Scope.GENERATION) {
            throw new IllegalArgumentException("Use the scope-specific registration method");
        }
        return register(resourceId, scope, owner, null, abortAction, closeAction);
    }

    private Registration register(String resourceId, Scope scope, ExecutionOwner owner,
                                  RegistryGenerationId generationId,
                                  AbortAction abortAction, CloseAction closeAction) {
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be null or empty");
        }
        Objects.requireNonNull(scope, "Scope cannot be null");
        if (scope == Scope.RUNTIME) {
            if (owner != null || generationId != null) {
                throw new IllegalArgumentException("Runtime resources use the ledger runtime owner");
            }
        } else if (scope == Scope.GENERATION) {
            Objects.requireNonNull(generationId, "Registry generation ID cannot be null");
            if (!runtimeId.equals(generationId.getRuntimeId())) {
                throw new IllegalArgumentException("Registry generation belongs to another runtime");
            }
            if (owner != null) {
                throw new IllegalArgumentException("Generation resources cannot use an execution owner");
            }
        } else {
            Objects.requireNonNull(owner, "Execution owner cannot be null");
            if (!runtimeId.equals(owner.getRuntimeId())) {
                throw new IllegalArgumentException("Execution owner belongs to another runtime");
            }
            if (generationId != null) {
                throw new IllegalArgumentException("Session and operation resources cannot own a generation ID");
            }
        }

        RegistrationKey key = new RegistrationKey(resourceId, scope, owner, generationId);
        Entry entry = new Entry(key, resourceId, scope, owner, generationId, abortAction, closeAction);
        Entry existing;
        if (scope == Scope.GENERATION) {
            GenerationEntry generation = generationEntry(generationId);
            synchronized (generation) {
                if (generation.retiring) {
                    throw new IllegalStateException("Registry generation is retiring: " + generationId);
                }
                existing = entries.putIfAbsent(key, entry);
            }
        } else {
            existing = entries.putIfAbsent(key, entry);
        }
        if (existing != null) {
            throw new IllegalStateException("Resource ID is already registered for this owner: "
                + resourceId + " (" + scope + ')');
        }
        return new Registration(this, entry);
    }

    /** Pins a generation for the full lifetime of one execution. Release only after worker exit is observed. */
    public GenerationPin pinGeneration(ExecutionOwner owner, RegistryGenerationId generationId) {
        Objects.requireNonNull(owner, "Execution owner cannot be null");
        Objects.requireNonNull(generationId, "Registry generation ID cannot be null");
        validateGenerationOwner(owner, generationId);
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            if (generation.retiring) {
                throw new IllegalStateException("Registry generation no longer admits pins: " + generationId);
            }
            if (generation.activePins.containsValue(owner)) {
                throw new IllegalStateException("Execution owner already pins this generation: " + owner);
            }
            long pinId = generation.nextPinId.incrementAndGet();
            generation.activePins.put(pinId, owner);
            return new GenerationPin(this, generationId, pinId, owner);
        }
    }

    /** Stops new pins atomically; existing executions retain access until their pins are released. */
    public void beginGenerationRetirement(RegistryGenerationId generationId) {
        Objects.requireNonNull(generationId, "Registry generation ID cannot be null");
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            generation.retiring = true;
            generation.notifyAll();
        }
    }

    public int getActiveGenerationPinCount(RegistryGenerationId generationId) {
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            return generation.activePins.size();
        }
    }

    private void validateGenerationOwner(ExecutionOwner owner, RegistryGenerationId generationId) {
        if (!runtimeId.equals(owner.getRuntimeId())
            || !runtimeId.equals(generationId.getRuntimeId())) {
            throw new IllegalArgumentException("Execution owner and generation must belong to this runtime");
        }
    }

    private GenerationEntry generationEntry(RegistryGenerationId generationId) {
        if (!runtimeId.equals(generationId.getRuntimeId())) {
            throw new IllegalArgumentException("Registry generation belongs to another runtime");
        }
        return generations.computeIfAbsent(generationId, ignored -> new GenerationEntry());
    }

    private boolean isGenerationRetiring(RegistryGenerationId generationId) {
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            return generation.retiring;
        }
    }

    private boolean awaitGenerationPins(RegistryGenerationId generationId, long timeoutNanos,
                                        long started) throws InterruptedException {
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            while (!generation.activePins.isEmpty()) {
                long remaining = timeoutNanos - (System.nanoTime() - started);
                if (remaining <= 0) {
                    return false;
                }
                TimeUnit.NANOSECONDS.timedWait(generation, remaining);
            }
            return true;
        }
    }

    private void releaseGenerationPin(RegistryGenerationId generationId, long pinId,
                                      ExecutionOwner owner) {
        GenerationEntry generation = generationEntry(generationId);
        synchronized (generation) {
            if (!generation.activePins.remove(pinId, owner)) {
                throw new IllegalStateException("Generation pin is not registered: " + pinId);
            }
            generation.notifyAll();
        }
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
        entries.remove(entry.key, entry);
    }

    /** Composite ownership key; the same local resource ID may exist in different owners/generations. */
    private static final class RegistrationKey {
        private final String resourceId;
        private final Scope scope;
        private final ExecutionOwner owner;
        private final RegistryGenerationId generationId;

        private RegistrationKey(String resourceId, Scope scope, ExecutionOwner owner,
                                RegistryGenerationId generationId) {
            this.resourceId = resourceId;
            this.scope = scope;
            this.owner = owner;
            this.generationId = generationId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof RegistrationKey)) {
                return false;
            }
            RegistrationKey that = (RegistrationKey) other;
            return resourceId.equals(that.resourceId) && scope == that.scope
                && Objects.equals(owner, that.owner) && Objects.equals(generationId, that.generationId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resourceId, scope, owner, generationId);
        }
    }

    private static final class GenerationEntry {
        private final Map<Long, ExecutionOwner> activePins = new HashMap<>();
        private final AtomicLong nextPinId = new AtomicLong();
        private boolean retiring;
    }

    private static final class Entry {
        private final RegistrationKey key;
        private final String resourceId;
        private final Scope scope;
        private final ExecutionOwner owner;
        private final RegistryGenerationId generationId;
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

        private Entry(RegistrationKey key, String resourceId, Scope scope, ExecutionOwner owner,
                      RegistryGenerationId generationId, AbortAction abortAction, CloseAction closeAction) {
            this.key = key;
            this.resourceId = resourceId;
            this.scope = scope;
            this.owner = owner;
            this.generationId = generationId;
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

        /** Null unless SESSION or OPERATION scope owns this exact execution. */
        public ExecutionOwner getExecutionOwner() {
            return entry.owner;
        }

        /** Non-null only for GENERATION scope. */
        public RegistryGenerationId getRegistryGenerationId() {
            return entry.generationId;
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
         * Every scope requires explicit attribution; a session-scoped
         * registration must not silently attribute a foreign caller to its owner.
         * Runtime-scoped resources use this overload to report every consumer.
         * The state check and lease insertion share the close-admission monitor,
         * so no lease can escape CLOSE_REQUESTED.
         */
        public Lease acquire(ExecutionOwner useOwner) {
            Objects.requireNonNull(useOwner, "Lease execution owner cannot be null");
            if (entry.scope == Scope.GENERATION) {
                throw new IllegalStateException("Generation resource requires an explicit generation pin");
            }
            validateRuntimeOwner(useOwner);
            if (entry.scope != Scope.RUNTIME && !entry.owner.equals(useOwner)) {
                throw new IllegalArgumentException("Lease owner does not match resource owner");
            }
            return acquireValidated(useOwner);
        }

        /**
         * Acquires a generation-resource lease only when the execution is pinned
         * to the exact generation that owns the resource.
         */
        public Lease acquire(ExecutionOwner useOwner, RegistryGenerationId pinnedGeneration) {
            Objects.requireNonNull(useOwner, "Lease execution owner cannot be null");
            Objects.requireNonNull(pinnedGeneration, "Pinned registry generation cannot be null");
            if (entry.scope != Scope.GENERATION) {
                throw new IllegalStateException("Explicit generation pin is valid only for GENERATION resources");
            }
            validateRuntimeOwner(useOwner);
            if (!entry.generationId.equals(pinnedGeneration)) {
                throw new IllegalArgumentException("Execution is pinned to a different registry generation");
            }
            return acquireGenerationValidated(useOwner);
        }

        private Lease acquireGenerationValidated(ExecutionOwner useOwner) {
            GenerationEntry generation = ledger.generationEntry(entry.generationId);
            synchronized (generation) {
                if (!generation.activePins.containsValue(useOwner)) {
                    throw new IllegalStateException(
                        "Execution owner has no active pin for generation: " + useOwner);
                }
                synchronized (entry) {
                    if (entry.state != State.OPEN && entry.state != State.QUARANTINED) {
                        throw new IllegalStateException(
                            "Generation resource no longer admits pinned use: " + entry.resourceId
                                + " (" + entry.state + ')');
                    }
                    long leaseId = entry.nextLeaseId.incrementAndGet();
                    entry.activeLeases.put(leaseId, useOwner);
                    return new Lease(entry, leaseId, useOwner);
                }
            }
        }

        private void validateRuntimeOwner(ExecutionOwner useOwner) {
            if (!ledger.runtimeId.equals(useOwner.getRuntimeId())) {
                throw new IllegalArgumentException("Lease owner belongs to another runtime");
            }
        }

        private Lease acquireValidated(ExecutionOwner useOwner) {
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
            if (entry.scope == Scope.GENERATION) {
                ledger.beginGenerationRetirement(entry.generationId);
                return;
            }
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
            boolean generationRetiring = entry.scope == Scope.GENERATION
                && ledger.isGenerationRetiring(entry.generationId);
            synchronized (entry) {
                if (entry.state == State.OPEN && !generationRetiring) {
                    throw new IllegalStateException(
                        "Resource close must be requested before abort: " + entry.resourceId);
                }
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
                if (failure instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
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
            if (entry.scope == Scope.GENERATION) {
                throw new IllegalStateException(
                    "Generation quiescence is established by releasing all generation pins");
            }
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

            long started = System.nanoTime();
            if (entry.scope == Scope.GENERATION) {
                ledger.beginGenerationRetirement(entry.generationId);
                if (!ledger.awaitGenerationPins(entry.generationId, timeoutNanos, started)) {
                    synchronized (entry) {
                        if (entry.state == State.OPEN || entry.state == State.CLOSE_REQUESTED) {
                            entry.state = State.QUARANTINED;
                        }
                        return snapshot(entry);
                    }
                }
                synchronized (entry) {
                    if (entry.state == State.OPEN || entry.state == State.QUARANTINED) {
                        entry.state = State.CLOSE_REQUESTED;
                    }
                }
            } else {
                requestClose();
            }
            boolean performClose = false;
            synchronized (entry) {
                if (entry.state == State.CLOSED || entry.state == State.CLOSE_FAILED
                    || entry.state == State.DRAINING) {
                    return snapshot(entry);
                }
                while ((entry.scope != Scope.GENERATION && !entry.ownerQuiescent)
                    || !entry.activeLeases.isEmpty() || entry.abortInProgress) {
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
                    if (failure instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
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

    /**
     * First-class execution ownership of one immutable registry generation.
     * Closing this token declares that the owning worker has actually exited.
     */
    public static final class GenerationPin implements AutoCloseable {
        private final ResourceLedger ledger;
        private final RegistryGenerationId generationId;
        private final long pinId;
        private final ExecutionOwner owner;
        private final AtomicBoolean released = new AtomicBoolean();

        private GenerationPin(ResourceLedger ledger, RegistryGenerationId generationId,
                              long pinId, ExecutionOwner owner) {
            this.ledger = ledger;
            this.generationId = generationId;
            this.pinId = pinId;
            this.owner = owner;
        }

        public RegistryGenerationId getRegistryGenerationId() {
            return generationId;
        }

        public ExecutionOwner getOwner() {
            return owner;
        }

        public boolean isReleased() {
            return released.get();
        }

        @Override
        public void close() {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            try {
                ledger.releaseGenerationPin(generationId, pinId, owner);
            } catch (RuntimeException failure) {
                released.set(false);
                throw failure;
            }
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
