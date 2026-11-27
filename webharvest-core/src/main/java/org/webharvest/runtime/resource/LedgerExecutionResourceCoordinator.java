/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.resource;

import org.webharvest.runtime.session.ExecutionOwner;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Host-side lifecycle controller for one execution's resource access facade.
 *
 * <p>The service retains this controller. Plugins receive only the narrower
 * {@link ExecutionResourceAccess} view and cannot declare owner quiescence.</p>
 */
public final class LedgerExecutionResourceCoordinator {

    private final LedgerExecutionResourceContext context;

    public LedgerExecutionResourceCoordinator(ResourceLedger ledger, ExecutionOwner owner) {
        this.context = new LedgerExecutionResourceContext(
            Objects.requireNonNull(ledger, "Resource ledger cannot be null"),
            Objects.requireNonNull(owner, "Execution owner cannot be null"));
    }

    /** Returns the plugin-facing capability without lifecycle authority. */
    public ExecutionResourceAccess getAccess() {
        return context;
    }

    /** Stops admission and requests best-effort abort without asserting quiescence. */
    public void requestTermination() {
        context.requestTermination();
    }

    /**
     * Establishes owner quiescence after observed task exit and drains resources
     * using one shared budget.
     */
    public List<LedgerExecutionResourceContext.DrainReport> markOwnerQuiescentAndDrain(
        Duration budget) throws InterruptedException {
        return context.markOwnerQuiescentAndDrain(budget);
    }
}
