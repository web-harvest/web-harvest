/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.concurrent.CompletionStage;

/** Handle for an admitted plan; cancellation request is distinct from worker exit. */
public interface WorkExecution<R> {

    /**
     * Requests cancellation of this plan.
     *
     * @return true if this call initiated cancellation; false if already terminal or cancelling
     */
    boolean requestCancellation();

    /** Returns the admitted concurrency ceiling after runtime/session limits are applied. */
    int getEffectiveMaxConcurrentPartitions();

    /**
     * Completes only after every admitted worker has exited or queued work has
     * been positively removed before start. It does not mean resource close succeeded.
     */
    CompletionStage<WorkResult<R>> completion();
}
