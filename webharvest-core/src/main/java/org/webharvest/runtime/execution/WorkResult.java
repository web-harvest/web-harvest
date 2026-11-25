/*
 * Copyright (c) 2006-2026 the original author or authors.
 * All rights reserved.
 */
package org.webharvest.runtime.execution;

import java.util.List;
import java.util.Objects;

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

    /**
     * Creates a result whose outcomes must exactly match the plan's partition IDs and order.
     *
     * @param plan the immutable plan that was admitted
     * @param outcomes exactly one terminal outcome for each plan partition, in plan order
     */
    public WorkResult(WorkPlan<?> plan, List<PartitionOutcome<R>> outcomes) {
        Objects.requireNonNull(plan, "Work plan cannot be null");
        Objects.requireNonNull(outcomes, "Partition outcomes cannot be null");
        if (outcomes.size() != plan.getPartitionCount()) {
            throw new IllegalArgumentException(
                "Expected " + plan.getPartitionCount() + " partition outcomes but received " + outcomes.size());
        }

        List<PartitionOutcome<R>> snapshot = List.copyOf(outcomes);
        boolean succeeded = false;
        boolean unsuccessful = false;
        boolean failed = false;
        for (int i = 0; i < snapshot.size(); i++) {
            PartitionOutcome<R> outcome = Objects.requireNonNull(snapshot.get(i), "Partition outcome cannot be null");
            String expectedPartitionId = plan.getPartitions().get(i).getPartitionId();
            if (!expectedPartitionId.equals(outcome.getPartitionId())) {
                throw new IllegalArgumentException(
                    "Outcome at index " + i + " must match plan partition " + expectedPartitionId
                        + " but was " + outcome.getPartitionId());
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
        this.planId = plan.getPlanId();
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
