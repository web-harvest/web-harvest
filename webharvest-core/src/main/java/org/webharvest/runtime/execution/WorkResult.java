/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.List;
import java.util.Objects;

/** Immutable aggregate of all terminal partition outcomes for one work plan. */
public final class WorkResult<R> {

    public enum Status {
        SUCCEEDED,
        PARTIALLY_SUCCEEDED,
        FAILED,
        CANCELLED
    }

    private final Status status;
    private final List<PartitionOutcome<R>> outcomes;

    public WorkResult(List<PartitionOutcome<R>> outcomes) {
        Objects.requireNonNull(outcomes, "Partition outcomes cannot be null");
        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("A work result must contain partition outcomes");
        }
        List<PartitionOutcome<R>> snapshot = List.copyOf(outcomes);
        boolean succeeded = false;
        boolean unsuccessful = false;
        boolean failed = false;
        for (PartitionOutcome<R> outcome : snapshot) {
            Objects.requireNonNull(outcome, "Partition outcome cannot be null");
            if (outcome.getStatus() == PartitionOutcome.Status.SUCCEEDED) {
                succeeded = true;
            } else {
                unsuccessful = true;
                failed |= outcome.getStatus() == PartitionOutcome.Status.FAILED
                    || outcome.getStatus() == PartitionOutcome.Status.UNKNOWN;
            }
        }
        if (succeeded && unsuccessful) {
            status = Status.PARTIALLY_SUCCEEDED;
        } else if (succeeded) {
            status = Status.SUCCEEDED;
        } else if (failed) {
            status = Status.FAILED;
        } else {
            status = Status.CANCELLED;
        }
        this.outcomes = snapshot;
    }

    public Status getStatus() {
        return status;
    }

    public List<PartitionOutcome<R>> getOutcomes() {
        return outcomes;
    }
}
