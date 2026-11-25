/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.Objects;

/** Caller-requested execution constraints; runtime limits may only reduce concurrency. */
public final class WorkExecutionPolicy {

    private final int maxConcurrentPartitions;
    private final WorkFailurePolicy failurePolicy;

    public WorkExecutionPolicy(int maxConcurrentPartitions, WorkFailurePolicy failurePolicy) {
        if (maxConcurrentPartitions < 1) {
            throw new IllegalArgumentException("Maximum concurrent partitions must be positive");
        }
        this.maxConcurrentPartitions = maxConcurrentPartitions;
        this.failurePolicy = Objects.requireNonNull(failurePolicy, "Failure policy cannot be null");
    }

    public int getMaxConcurrentPartitions() {
        return maxConcurrentPartitions;
    }

    public WorkFailurePolicy getFailurePolicy() {
        return failurePolicy;
    }
}
