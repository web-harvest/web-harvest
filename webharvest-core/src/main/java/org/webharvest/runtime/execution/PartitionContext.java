/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.session.ExecutionOwner;

/**
 * Worker-scoped immutable execution metadata and cancellation/resource capabilities.
 */
public interface PartitionContext {

    /** Returns the stable plan identifier. */
    String getPlanId();

    /** Returns the stable partition identifier, distinct from the session owner. */
    String getPartitionId();

    /** Returns the canonical session owner shared by all partitions of this plan. */
    ExecutionOwner getOwner();

    /**
     * Returns the owner-bound resource facade shared by partitions of this execution.
     *
     * <p>Concurrent partitions may call this facade concurrently; implementations used
     * for partition execution must support concurrent calls. Adapters must use unique
     * resource IDs. Use operation resources for partition-local lifetimes. Session
     * resources are owner-scoped and may be shared across partitions; an individual
     * partition must not treat them as partition-local or close them on its own exit.</p>
     */
    ExecutionResourceAccess getResourceAccess();

    /** Returns whether cancellation or fail-fast termination has been requested. */
    boolean isCancellationRequested();

    /**
     * Checks cooperative cancellation.
     *
     * @throws InterruptedException when cancellation has been requested or the worker interrupted
     */
    void throwIfCancellationRequested() throws InterruptedException;
}
