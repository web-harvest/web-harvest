/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.resource;

import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owner-bound facade over the runtime's single ResourceLedger.
 *
 * <p>The facade admits registrations only while the execution is open. The
 * service coordinator alone can request session termination and establish
 * session quiescence. Operation handles may establish quiescence only for
 * their own operation-scoped registration.</p>
 */
public final class LedgerExecutionResourceContext implements ExecutionResourceAccess {

    private final ResourceLedger ledger;
    private final ExecutionOwner owner;
    private final Object gate = new Object();
    private final List<Handle> registrations = new ArrayList<>();
    private Lifecycle lifecycle = Lifecycle.OPEN;

    private enum Lifecycle {
        OPEN,
        CLOSE_REQUESTED,
        QUIESCENT
    }

    LedgerExecutionResourceContext(ResourceLedger ledger, ExecutionOwner owner) {
        this.ledger = Objects.requireNonNull(ledger, "Resource ledger cannot be null");
        this.owner = Objects.requireNonNull(owner, "Execution owner cannot be null");
        if (!ledger.getRuntimeId().equals(owner.getRuntimeId())) {
            throw new IllegalArgumentException("Execution owner belongs to another runtime");
        }
    }

    @Override
    public ExecutionOwner getOwner() {
        return owner;
    }

    @Override
    public ResourceHandle registerSessionResource(String resourceId,
                                                  AbortAction abortAction,
                                                  CloseAction closeAction) {
        return register(resourceId, ResourceLedger.Scope.SESSION, abortAction, closeAction);
    }

    @Override
    public OperationResource registerOperationResource(String resourceId,
                                                        AbortAction abortAction,
                                                        CloseAction closeAction) {
        return (OperationHandle) register(resourceId, ResourceLedger.Scope.OPERATION,
            abortAction, closeAction);
    }

    private Handle register(String resourceId, ResourceLedger.Scope scope,
                            AbortAction abortAction, CloseAction closeAction) {
        synchronized (gate) {
            if (lifecycle != Lifecycle.OPEN) {
                throw new IllegalStateException("Execution no longer admits resource registration");
            }
            ResourceLedger.Registration registration = ledger.registerOwned(
                resourceId, scope, owner,
                abortAction == null ? null : abortAction::abort,
                closeAction == null ? null : closeAction::close);
            Handle handle = scope == ResourceLedger.Scope.OPERATION
                ? new OperationHandle(registration) : new Handle(registration);
            registrations.add(handle);
            return handle;
        }
    }

    /**
     * Host-coordinator operation. Stops registration and lease admission,
     * requests best-effort abort, and marks unresolved resources quarantined.
     * It does not declare the execution quiescent.
     */
    void requestTermination() {
        List<Handle> snapshot;
        synchronized (gate) {
            if (lifecycle != Lifecycle.OPEN) {
                return;
            }
            lifecycle = Lifecycle.CLOSE_REQUESTED;
            snapshot = new ArrayList<>(registrations);
        }
        Error fatalFailure = null;
        for (Handle handle : snapshot) {
            try {
                handle.requestTermination();
            } catch (Error failure) {
                if (fatalFailure == null) {
                    fatalFailure = failure;
                } else if (fatalFailure != failure) {
                    fatalFailure.addSuppressed(failure);
                }
            }
        }
        if (fatalFailure != null) {
            throw fatalFailure;
        }
    }

    /**
     * Host-coordinator operation. Call only after #354 has observed the task
     * exit (or confirmed removal before start). All registrations are marked
     * quiescent before a single shared drain budget is distributed.
     */
    List<DrainReport> markOwnerQuiescentAndDrain(Duration budget) throws InterruptedException {
        Objects.requireNonNull(budget, "Drain budget cannot be null");
        if (budget.isNegative()) {
            throw new IllegalArgumentException("Drain budget cannot be negative");
        }
        // Validate and normalize the complete budget before changing lifecycle
        // state. Duration.toNanos() can overflow; a rejected budget must leave
        // the facade OPEN so a later valid drain can establish quiescence.
        final long budgetNanos;
        try {
            budgetNanos = budget.toNanos();
        } catch (ArithmeticException overflow) {
            throw new IllegalArgumentException("Drain budget is too large", overflow);
        }

        final List<Handle> snapshot;
        synchronized (gate) {
            lifecycle = Lifecycle.QUIESCENT;
            snapshot = new ArrayList<>(registrations);
        }

        // Establish the ownership boundary for every registration before any
        // potentially blocking drain begins.
        for (Handle handle : snapshot) {
            handle.markOwnerQuiescent();
        }

        final long started = System.nanoTime();
        List<DrainReport> reports = new ArrayList<>();
        InterruptedException interruption = null;
        Throwable fatalFailure = null;
        for (Handle handle : snapshot) {
            long remaining = interruption == null
                ? Math.max(0L, budgetNanos - (System.nanoTime() - started)) : 0L;
            try {
                reports.add(handle.drain(Duration.ofNanos(remaining)));
            } catch (InterruptedException failure) {
                // Normalize this registration to a terminal diagnostic state
                // (CLOSED or QUARANTINED) before continuing with later handles.
                if (interruption == null) {
                    interruption = failure;
                } else {
                    interruption.addSuppressed(failure);
                }
                try {
                    reports.add(handle.drain(Duration.ZERO));
                } catch (Throwable retryFailure) {
                    if (fatalFailure == null) {
                        fatalFailure = retryFailure;
                    } else if (fatalFailure != retryFailure) {
                        fatalFailure.addSuppressed(retryFailure);
                    }
                }
            } catch (Throwable failure) {
                if (fatalFailure == null) {
                    fatalFailure = failure;
                } else if (fatalFailure != failure) {
                    fatalFailure.addSuppressed(failure);
                }
            }
        }

        if (fatalFailure != null) {
            if (interruption != null && fatalFailure != interruption) {
                fatalFailure.addSuppressed(interruption);
            }
            if (fatalFailure instanceof Error) {
                throw (Error) fatalFailure;
            }
            if (fatalFailure instanceof RuntimeException) {
                throw (RuntimeException) fatalFailure;
            }
            throw new IllegalStateException("Resource drain failed", fatalFailure);
        }
        if (interruption != null) {
            throw interruption;
        }
        return Collections.unmodifiableList(reports);
    }

    private void retryDrain(Handle handle) {
        Lifecycle current;
        synchronized (gate) {
            current = lifecycle;
        }
        if (current == Lifecycle.QUIESCENT || handle instanceof OperationHandle
            && ((OperationHandle) handle).isOperationComplete()) {
            try {
                handle.drain(Duration.ZERO);
            } catch (InterruptedException impossible) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static final class DrainReport {
        private final String resourceId;
        private final ResourceState state;
        private final int activeLeases;
        private final Throwable abortFailure;
        private final Throwable closeFailure;

        private DrainReport(String resourceId, ResourceState state, int activeLeases,
                            Throwable abortFailure, Throwable closeFailure) {
            this.resourceId = resourceId;
            this.state = state;
            this.activeLeases = activeLeases;
            this.abortFailure = abortFailure;
            this.closeFailure = closeFailure;
        }

        public String getResourceId() { return resourceId; }
        public ResourceState getState() { return state; }
        public int getActiveLeases() { return activeLeases; }
        public Throwable getAbortFailure() { return abortFailure; }
        public Throwable getCloseFailure() { return closeFailure; }
    }

    private class Handle implements ResourceHandle {
        protected final ResourceLedger.Registration registration;

        private Handle(ResourceLedger.Registration registration) {
            this.registration = registration;
        }

        @Override
        public String getResourceId() {
            return registration.getResourceId();
        }

        @Override
        public Lease acquire() {
            synchronized (gate) {
                if (lifecycle != Lifecycle.OPEN) {
                    throw new IllegalStateException("Execution no longer admits resource leases");
                }
                return new LeaseHandle(this, registration.acquire(owner));
            }
        }

        @Override
        public ResourceState getState() {
            return ResourceState.valueOf(registration.getState().name());
        }

        @Override
        public int getActiveLeaseCount() {
            return registration.getActiveLeaseCount();
        }

        @Override
        public Throwable getAbortFailure() {
            return registration.getAbortFailure();
        }

        @Override
        public Throwable getCloseFailure() {
            return registration.getCloseFailure();
        }

        private void requestTermination() {
            registration.requestClose();
            registration.abort();
            try {
                drain(Duration.ZERO);
            } catch (InterruptedException impossible) {
                Thread.currentThread().interrupt();
            }
        }

        private void markOwnerQuiescent() {
            if (registration.getScope() != ResourceLedger.Scope.GENERATION) {
                registration.requestClose();
                registration.markOwnerQuiescent();
            }
        }

        protected final DrainReport drain(Duration budget) throws InterruptedException {
            ResourceLedger.DrainResult result = registration.drainAndClose(budget);
            if (result.isClosed()) {
                registration.close();
            }
            return new DrainReport(getResourceId(), ResourceState.valueOf(result.getState().name()),
                result.getActiveLeases(), result.getAbortFailure(), result.getCloseFailure());
        }

        private void leaseReleased() {
            retryDrain(this);
        }
    }

    private final class OperationHandle extends Handle implements OperationResource {
        private final AtomicBoolean operationComplete = new AtomicBoolean();

        private OperationHandle(ResourceLedger.Registration registration) {
            super(registration);
        }

        @Override
        public void close() {
            if (!operationComplete.compareAndSet(false, true)) {
                return;
            }
            registration.requestClose();
            registration.markOwnerQuiescent();
            try {
                drain(Duration.ZERO);
            } catch (InterruptedException impossible) {
                Thread.currentThread().interrupt();
            }
        }

        private boolean isOperationComplete() {
            return operationComplete.get();
        }
    }

    private final class LeaseHandle implements Lease {
        private final Handle handle;
        private final ResourceLedger.Lease lease;
        private final AtomicBoolean released = new AtomicBoolean();

        private LeaseHandle(Handle handle, ResourceLedger.Lease lease) {
            this.handle = handle;
            this.lease = lease;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                lease.close();
                handle.leaseReleased();
            }
        }
    }
}
