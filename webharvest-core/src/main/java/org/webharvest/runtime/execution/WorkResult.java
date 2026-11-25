/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable aggregate of exactly one terminal outcome per partition, in original plan order.
 * The aggregate status does not imply a transaction spanning multiple partitions.
 */
public final class WorkResult<R> {

    public enum Status {
        SUCCEEDED,
        PARTIALLY_SUCCEEDED,
        FAILED,
        CANCELLED
    }

    private final String planId;
    private final Status status;
    private final List<PartitionOutcome<R>> outcomes;

    public WorkResult(String planId, List<PartitionOutcome<R>> outcomes) {
        if (planId == null || planId.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan ID cannot be null or empty");
        }
        Objects.requireNonNull(outcomes, "Partition outcomes cannot be null");
        if (outcomes.isEmpty()) {
            throw new IllegalArgumentException("A work result must contain partition outcomes");
        }
        List<PartitionOutcome<R>> snapshot = List.copyOf(outcomes);
        boolean succeeded = false;
        boolean unsuccessful = false;
        boolean failed = false;
        Set<String> partitionIds = new HashSet<>();
        for (PartitionOutcome<R> outcome : snapshot) {
            Objects.requireNonNull(outcome, "Partition outcome cannot be null");
            if (!partitionIds.add(outcome.getPartitionId())) {
                throw new IllegalArgumentException(
                    "Duplicate partition outcome: " + outcome.getPartitionId());
            }
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
        this.planId = planId;
        this.outcomes = snapshot;
    }

    public String getPlanId() {
        return planId;
    }

    public Status getStatus() {
        return status;
    }

    public List<PartitionOutcome<R>> getOutcomes() {
        return outcomes;
    }
}
