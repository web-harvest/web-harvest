/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.resource;

import org.webharvest.runtime.session.ExecutionOwner;

/**
 * Execution-bound access to resources whose lifetime must be coordinated with
 * the owning execution. Implementations capture one immutable owner; callers
 * cannot substitute an owner or declare session quiescence.
 */
public interface ExecutionResourceAccess {

    ExecutionOwner getOwner();

    ResourceHandle registerSessionResource(String resourceId,
                                            AbortAction abortAction,
                                            CloseAction closeAction);

    OperationResource registerOperationResource(String resourceId,
                                                AbortAction abortAction,
                                                CloseAction closeAction);

    @FunctionalInterface
    interface AbortAction {
        void abort() throws Exception;
    }

    @FunctionalInterface
    interface CloseAction {
        void close() throws Exception;
    }

    interface ResourceHandle {
        String getResourceId();
        Lease acquire();
        ResourceState getState();
        int getActiveLeaseCount();
        Throwable getAbortFailure();
        Throwable getCloseFailure();
    }

    interface OperationResource extends ResourceHandle, AutoCloseable {
        /** Marks this operation's resource owner quiescent and drains it. */
        @Override
        void close();
    }

    interface Lease extends AutoCloseable {
        /** Releases this use exactly once. */
        @Override
        void close();
    }

    enum ResourceState {
        OPEN,
        CLOSE_REQUESTED,
        DRAINING,
        CLOSED,
        CLOSE_FAILED,
        QUARANTINED
    }
}
