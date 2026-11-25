/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.Objects;

/** Immutable terminal outcome for one partition. */
public final class PartitionOutcome<R> {

    public enum Status {
        SUCCEEDED,
        FAILED,
        CANCELLED,
        NOT_STARTED,
        UNKNOWN
    }

    private final String partitionId;
    private final Status status;
    private final R result;
    private final Throwable failure;

    public PartitionOutcome(String partitionId, Status status, R result, Throwable failure) {
        if (partitionId == null || partitionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Partition ID cannot be null or empty");
        }
        this.partitionId = partitionId;
        this.status = Objects.requireNonNull(status, "Partition status cannot be null");
        if (status == Status.FAILED && failure == null) {
            throw new IllegalArgumentException("A failed partition must include its failure");
        }
        if (status != Status.FAILED && status != Status.UNKNOWN && failure != null) {
            throw new IllegalArgumentException("Only failed or unknown outcomes may include a failure");
        }
        if (status != Status.SUCCEEDED && result != null) {
            throw new IllegalArgumentException("Only successful outcomes may include a result");
        }
        this.result = result;
        this.failure = failure;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public Status getStatus() {
        return status;
    }

    public R getResult() {
        return result;
    }

    public Throwable getFailure() {
        return failure;
    }
}
