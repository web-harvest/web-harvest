/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import org.webharvest.runtime.resource.ExecutionResourceAccess;
import org.webharvest.runtime.session.ExecutionOwner;

/** Worker-scoped immutable execution metadata and cancellation/resource capabilities. */
public interface PartitionContext {

    /** Returns the canonical session owner shared by all partitions of this plan. */
    ExecutionOwner getOwner();

    /** Returns resource access bound to {@link #getOwner()}. */
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
